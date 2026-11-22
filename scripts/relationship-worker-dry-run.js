"use strict";

// READ-ONLY relationship migration probe.
// This file intentionally never writes to Firestore.
const admin = require("firebase-admin");

function loadServiceAccount() {
  const raw = process.env.FIREBASE_SERVICE_ACCOUNT_JSON;
  if (!raw) throw new Error("FIREBASE_SERVICE_ACCOUNT_JSON secret is not configured.");
  const parsed = JSON.parse(raw);
  if (!parsed.project_id) throw new Error("Service-account JSON has no project_id.");
  return parsed;
}

async function main() {
  if (process.env.RELATIONSHIP_WORKER_MODE !== "dry-run") {
    throw new Error("Refusing to run: RELATIONSHIP_WORKER_MODE must equal dry-run.");
  }

  const serviceAccount = loadServiceAccount();
  if (serviceAccount.project_id !== "vibeplay-anshu") {
    throw new Error("Refusing unexpected Firebase project.");
  }

  admin.initializeApp({
    credential: admin.credential.cert(serviceAccount),
    projectId: serviceAccount.project_id
  });
  const db = admin.firestore();

  const giftSnapshot = await db.collection("gift_transactions")
    .orderBy("createdAt", "desc")
    .limit(500)
    .get();

  let validUnprocessed = 0;
  let malformed = 0;
  let selfGifts = 0;
  let alreadyProcessed = 0;
  const pairTotals = new Map();

  for (const doc of giftSnapshot.docs) {
    const gift = doc.data();
    const senderId = gift.senderId;
    const recipientId = gift.recipientId;
    const coinCost = gift.coinCost;

    if (typeof senderId !== "string" || !senderId ||
        typeof recipientId !== "string" || !recipientId ||
        !Number.isSafeInteger(coinCost) || coinCost <= 0) {
      malformed += 1;
      continue;
    }
    if (senderId === recipientId) {
      selfGifts += 1;
      continue;
    }

    const processed = await db.collection("relationship_gift_events").doc(doc.id).get();
    if (processed.exists) {
      alreadyProcessed += 1;
      continue;
    }

    validUnprocessed += 1;
    const ids = [senderId, recipientId].sort();
    const key = ids.join(":");
    const current = pairTotals.get(key) || { coinCost: 0, gifts: 0 };
    current.coinCost += coinCost;
    current.gifts += 1;
    pairTotals.set(key, current);
  }

  const cutoff = admin.firestore.Timestamp.fromMillis(Date.now() - 48 * 60 * 60 * 1000);
  const expirySnapshot = await db.collection("relationship_invites")
    .where("createdAt", "<=", cutoff)
    .limit(500)
    .get();

  let expiredPending = 0;
  let otherInviteStatuses = 0;
  for (const doc of expirySnapshot.docs) {
    const invite = doc.data();
    if (invite.status !== "PENDING") {
      otherInviteStatuses += 1;
      continue;
    }
    const expiry = invite.expiresAt && typeof invite.expiresAt.toMillis === "function"
      ? invite.expiresAt.toMillis()
      : invite.createdAt && typeof invite.createdAt.toMillis === "function"
        ? invite.createdAt.toMillis() + 48 * 60 * 60 * 1000
        : 0;
    if (expiry > 0 && expiry <= Date.now()) expiredPending += 1;
  }

  const report = {
    mode: "DRY_RUN_READ_ONLY",
    projectId: serviceAccount.project_id,
    giftTransactionsScanned: giftSnapshot.size,
    validUnprocessedGiftTransactions: validUnprocessed,
    alreadyProcessedGiftTransactions: alreadyProcessed,
    malformedGiftTransactions: malformed,
    selfGiftTransactionsIgnored: selfGifts,
    distinctPairsWithUnprocessedGifts: pairTotals.size,
    projectedCoinCostByPair: Array.from(pairTotals.values()).reduce((sum, item) => sum + item.coinCost, 0),
    oldInvitationsScanned: expirySnapshot.size,
    invitationsEligibleForExpiry: expiredPending,
    nonPendingInvitationsIgnored: otherInviteStatuses,
    writesPerformed: 0
  };
  console.log(JSON.stringify(report, null, 2));
  await admin.app().delete();
}

main().catch((error) => {
  console.error("Read-only relationship probe failed:", error.message);
  process.exitCode = 1;
});
