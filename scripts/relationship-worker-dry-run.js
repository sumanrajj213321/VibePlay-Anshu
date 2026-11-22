"use strict";

const admin = require("firebase-admin");
const { createHash } = require("node:crypto");

const LEVELS = [10000, 30000, 60000, 100000, 150000, 220000, 300000, 400000, 550000, 750000];
const ALLOWED_TAGS = ["BUDDIES", "BRO", "CONFIDANT", "BESTIE"];
const PAGE_SIZE = 250;
const EXPIRY_MS = 48 * 60 * 60 * 1000;

function pairIdFor(a, b) {
  return createHash("sha256").update([a, b].sort().join(":")).digest("hex");
}

function levelFor(points) {
  let level = 0;
  for (let i = 0; i < LEVELS.length; i += 1) {
    if (points >= LEVELS[i]) level = i + 1;
    else break;
  }
  return level;
}

function loadServiceAccount() {
  const raw = process.env.FIREBASE_SERVICE_ACCOUNT_JSON;
  if (!raw) throw new Error("FIREBASE_SERVICE_ACCOUNT_JSON secret is not configured.");
  const parsed = JSON.parse(raw);
  if (!parsed.project_id) throw new Error("Service-account JSON has no project_id.");
  return parsed;
}

function validateGift(gift) {
  return typeof gift.senderId === "string" && gift.senderId &&
    typeof gift.recipientId === "string" && gift.recipientId &&
    gift.senderId !== gift.recipientId &&
    Number.isSafeInteger(gift.coinCost) && gift.coinCost > 0;
}

async function processGift(db, giftDoc, dryRun, stats) {
  const gift = giftDoc.data();
  if (!validateGift(gift)) {
    if (gift.senderId === gift.recipientId && gift.senderId) stats.selfGiftsIgnored += 1;
    else stats.malformedGiftTransactions += 1;
    return;
  }

  const senderId = gift.senderId;
  const recipientId = gift.recipientId;
  const pairId = pairIdFor(senderId, recipientId);
  const pairRef = db.collection("relationship_pairs").doc(pairId);
  const eventRef = db.collection("relationship_gift_events").doc(giftDoc.id);

  if (dryRun) {
    const [eventSnap, pairSnap] = await Promise.all([eventRef.get(), pairRef.get()]);
    if (eventSnap.exists) {
      stats.alreadyProcessedGiftTransactions += 1;
      return;
    }
    const pair = pairSnap.exists ? pairSnap.data() : {};
    const oldCoins = Number.isSafeInteger(pair.totalGiftCoins) && pair.totalGiftCoins >= 0
      ? pair.totalGiftCoins : 0;
    stats.validUnprocessedGiftTransactions += 1;
    stats.projectedGiftCoins += gift.coinCost;
    const existing = stats.pairs.get(pairId) || {
      pairId, userIds: [senderId, recipientId].sort(), newGiftTransactions: 0,
      newGiftCoins: 0, existingGiftCoins: oldCoins
    };
    existing.newGiftTransactions += 1;
    existing.newGiftCoins += gift.coinCost;
    stats.pairs.set(pairId, existing);
    return;
  }

  const outcome = await db.runTransaction(async (tx) => {
    const [eventSnap, pairSnap] = await Promise.all([tx.get(eventRef), tx.get(pairRef)]);
    if (eventSnap.exists) return "already_processed";

    const pair = pairSnap.exists ? pairSnap.data() : {};
    const oldCoins = Number.isSafeInteger(pair.totalGiftCoins) && pair.totalGiftCoins >= 0
      ? pair.totalGiftCoins : 0;
    const totalGiftCoins = oldCoins + gift.coinCost;
    const familiarityPoints = Math.floor(totalGiftCoins / 5);
    const pairUpdate = {
      userIds: [senderId, recipientId].sort(),
      totalGiftCoins,
      familiarityPoints,
      level: levelFor(familiarityPoints),
      updatedAt: admin.firestore.FieldValue.serverTimestamp()
    };

    const activatesTag = pair.tagStatus === "WAITING_FAMILIARITY" &&
      familiarityPoints >= 10000 && ALLOWED_TAGS.includes(pair.acceptedTagType);
    let profiles = null;
    if (activatesTag) {
      const senderRef = db.collection("users").doc(senderId);
      const recipientRef = db.collection("users").doc(recipientId);
      const [senderProfile, recipientProfile] = await Promise.all([tx.get(senderRef), tx.get(recipientRef)]);
      profiles = { senderProfile, recipientProfile };
      pairUpdate.tagStatus = "ACTIVE";
      pairUpdate.tagType = pair.acceptedTagType;
      pairUpdate.tagStartedAt = admin.firestore.FieldValue.serverTimestamp();
      pairUpdate.activatedAtFamiliarity = familiarityPoints;
      pairUpdate.acceptedTagType = admin.firestore.FieldValue.delete();
    }

    tx.set(pairRef, pairUpdate, { merge: true });
    tx.create(eventRef, {
      giftId: giftDoc.id,
      pairId,
      coinCost: gift.coinCost,
      processedAt: admin.firestore.FieldValue.serverTimestamp()
    });

    if (activatesTag && profiles) {
      const tagType = pair.acceptedTagType;
      const senderProfile = profiles.senderProfile;
      const recipientProfile = profiles.recipientProfile;
      tx.set(db.collection("relationship_links").doc(senderId), {
        partnerId: recipientId,
        partnerName: recipientProfile.get("username") || recipientProfile.get("displayName") || "VibePlay user",
        partnerAvatarUrl: recipientProfile.get("avatarUrl") || "",
        tagType,
        updatedAt: admin.firestore.FieldValue.serverTimestamp()
      });
      tx.set(db.collection("relationship_links").doc(recipientId), {
        partnerId: senderId,
        partnerName: senderProfile.get("username") || senderProfile.get("displayName") || "VibePlay user",
        partnerAvatarUrl: senderProfile.get("avatarUrl") || "",
        tagType,
        updatedAt: admin.firestore.FieldValue.serverTimestamp()
      });
      const [userAId, userBId] = [senderId, recipientId].sort();
      const userAProfile = userAId === senderId ? senderProfile : recipientProfile;
      const userBProfile = userBId === senderId ? senderProfile : recipientProfile;
      tx.set(db.collection("relationship_public_tags").doc(pairId), {
        userIds: [userAId, userBId],
        userAName: userAProfile.get("username") || userAProfile.get("displayName") || "VibePlay user",
        userAAvatarUrl: userAProfile.get("avatarUrl") || "",
        userBName: userBProfile.get("username") || userBProfile.get("displayName") || "VibePlay user",
        userBAvatarUrl: userBProfile.get("avatarUrl") || "",
        tagType,
        updatedAt: admin.firestore.FieldValue.serverTimestamp()
      });
    }
    return activatesTag ? "activated" : "processed";
  });

  if (outcome === "already_processed") stats.alreadyProcessedGiftTransactions += 1;
  else {
    stats.appliedGiftTransactions += 1;
    if (outcome === "activated") stats.waitingTagsActivated += 1;
  }
}

async function processExpiredInvite(db, inviteDoc, dryRun, stats, nowMs) {
  const initial = inviteDoc.data();
  if (initial.status !== "PENDING") {
    stats.nonPendingInvitationsIgnored += 1;
    return;
  }
  const expiry = initial.expiresAt && typeof initial.expiresAt.toMillis === "function"
    ? initial.expiresAt.toMillis()
    : initial.createdAt && typeof initial.createdAt.toMillis === "function"
      ? initial.createdAt.toMillis() + EXPIRY_MS : 0;
  if (!expiry || expiry > nowMs) return;
  if (dryRun) {
    stats.invitationsEligibleForExpiry += 1;
    return;
  }

  const inviteRef = inviteDoc.ref;
  const pairRef = db.collection("relationship_pairs").doc(initial.pairId);
  const notificationId = "relationship-expired-" + inviteDoc.id;
  const notificationRef = db.collection("notifications").doc(notificationId);

  const expired = await db.runTransaction(async (tx) => {
    const [freshInvite, freshPair, notification] = await Promise.all([
      tx.get(inviteRef), tx.get(pairRef), tx.get(notificationRef)
    ]);
    if (!freshInvite.exists || freshInvite.get("status") !== "PENDING") return false;
    const current = freshInvite.data();
    const currentExpiry = current.expiresAt && typeof current.expiresAt.toMillis === "function"
      ? current.expiresAt.toMillis()
      : current.createdAt && typeof current.createdAt.toMillis === "function"
        ? current.createdAt.toMillis() + EXPIRY_MS : 0;
    if (!currentExpiry || currentExpiry > Date.now()) return false;

    tx.update(inviteRef, {
      status: "REJECTED",
      autoRejected: true,
      rejectionReason: "NO_RESPONSE_48_HOURS",
      respondedAt: admin.firestore.FieldValue.serverTimestamp()
    });
    if (current.notificationId) {
      tx.set(db.collection("notifications").doc(current.notificationId), { read: true }, { merge: true });
    }
    if (freshPair.exists && freshPair.get("pendingInviteId") === inviteDoc.id) {
      tx.update(pairRef, {
        tagStatus: "NONE",
        pendingInviteId: admin.firestore.FieldValue.delete(),
        pendingTagType: admin.firestore.FieldValue.delete(),
        invitedBy: admin.firestore.FieldValue.delete(),
        invitedTo: admin.firestore.FieldValue.delete(),
        pendingAt: admin.firestore.FieldValue.delete(),
        updatedAt: admin.firestore.FieldValue.serverTimestamp()
      });
    }
    if (!notification.exists && current.inviterId) {
      tx.create(notificationRef, {
        recipientId: current.inviterId,
        senderId: current.invitedTo || "VIBEPLAY_SYSTEM",
        senderName: "VibePlay",
        type: "SYSTEM",
        title: "Relationship invitation expired",
        body: "Your relationship invitation was automatically rejected because there was no response within 48 hours.",
        targetId: current.pairId || "",
        read: false,
        createdAt: admin.firestore.FieldValue.serverTimestamp()
      });
    }
    return true;
  });
  if (expired) stats.invitationsAutoRejected += 1;
}

async function scanPages(query, handler) {
  let cursor = null;
  let scanned = 0;
  while (true) {
    let pageQuery = query.limit(PAGE_SIZE);
    if (cursor) pageQuery = pageQuery.startAfter(cursor);
    const page = await pageQuery.get();
    if (page.empty) break;
    for (const doc of page.docs) await handler(doc);
    scanned += page.size;
    cursor = page.docs[page.docs.length - 1];
    if (page.size < PAGE_SIZE) break;
  }
  return scanned;
}

async function syncActivePublicTags(db, dryRun, stats) {
  const query = db.collection("relationship_pairs")
    .orderBy(admin.firestore.FieldPath.documentId(), "asc");
  let pendingBatch = db.batch();
  let pendingWrites = 0;

  stats.activeRelationshipPairsScanned = await scanPages(query, async (pairDoc) => {
    const pair = pairDoc.data();
    if (pair.tagStatus !== "ACTIVE" || !ALLOWED_TAGS.includes(pair.tagType)) return;
    const ids = Array.isArray(pair.userIds)
      ? [...new Set(pair.userIds.filter((id) => typeof id === "string" && id))]
      : [];
    if (ids.length !== 2) return;

    const [userAId, userBId] = ids.sort();
    const publicRef = db.collection("relationship_public_tags").doc(pairDoc.id);
    const [publicSnap, userA, userB] = await Promise.all([
      publicRef.get(),
      db.collection("users").doc(userAId).get(),
      db.collection("users").doc(userBId).get()
    ]);
    const next = {
      userIds: [userAId, userBId],
      userAName: userA.get("username") || userA.get("displayName") || "VibePlay user",
      userAAvatarUrl: userA.get("avatarUrl") || "",
      userBName: userB.get("username") || userB.get("displayName") || "VibePlay user",
      userBAvatarUrl: userB.get("avatarUrl") || "",
      tagType: pair.tagType
    };
    const current = publicSnap.exists ? publicSnap.data() : {};
    const unchanged = JSON.stringify(current.userIds || []) === JSON.stringify(next.userIds) &&
      current.userAName === next.userAName &&
      current.userAAvatarUrl === next.userAAvatarUrl &&
      current.userBName === next.userBName &&
      current.userBAvatarUrl === next.userBAvatarUrl &&
      current.tagType === next.tagType;
    if (unchanged) return;

    stats.publicTagsNeedingSync += 1;
    if (dryRun) return;
    pendingBatch.set(publicRef, {
      ...next,
      updatedAt: admin.firestore.FieldValue.serverTimestamp()
    }, { merge: true });
    pendingWrites += 1;
    if (pendingWrites >= 450) {
      await pendingBatch.commit();
      pendingBatch = db.batch();
      pendingWrites = 0;
    }
  });

  if (!dryRun && pendingWrites > 0) await pendingBatch.commit();
}

async function main() {
  const mode = process.env.RELATIONSHIP_WORKER_MODE;
  if (!["dry-run", "apply"].includes(mode)) {
    throw new Error("Set RELATIONSHIP_WORKER_MODE to dry-run or apply.");
  }
  const dryRun = mode === "dry-run";
  if (!dryRun && process.env.RELATIONSHIP_WORKER_CONFIRM !== "APPLY") {
    throw new Error("Production writes blocked: set RELATIONSHIP_WORKER_CONFIRM=APPLY explicitly.");
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
  const stats = {
    giftTransactionsScanned: 0,
    validUnprocessedGiftTransactions: 0,
    alreadyProcessedGiftTransactions: 0,
    malformedGiftTransactions: 0,
    selfGiftsIgnored: 0,
    appliedGiftTransactions: 0,
    waitingTagsActivated: 0,
    projectedGiftCoins: 0,
    pairs: new Map(),
    oldInvitationsScanned: 0,
    invitationsEligibleForExpiry: 0,
    invitationsAutoRejected: 0,
    nonPendingInvitationsIgnored: 0,
    activeRelationshipPairsScanned: 0,
    publicTagsNeedingSync: 0
  };

  // Use document ID ordering so legacy gift documents without createdAt are not skipped.
  // Idempotency is enforced by relationship_gift_events inside each transaction.
  const giftQuery = db.collection("gift_transactions")
    .orderBy(admin.firestore.FieldPath.documentId(), "asc");
  stats.giftTransactionsScanned = await scanPages(giftQuery, async (doc) => {
    await processGift(db, doc, dryRun, stats);
  });

  // Scan by document ID so legacy invitations without createdAt are also checked.
  // processExpiredInvite handles both expiresAt and the createdAt + 48h fallback.
  const inviteQuery = db.collection("relationship_invites")
    .orderBy(admin.firestore.FieldPath.documentId(), "asc");
  stats.oldInvitationsScanned = await scanPages(inviteQuery, async (doc) => {
    await processExpiredInvite(db, doc, dryRun, stats, Date.now());
  });

  await syncActivePublicTags(db, dryRun, stats);

  const pairProjections = [];
  for (const pair of stats.pairs.values()) {
    const projectedCoins = pair.existingGiftCoins + pair.newGiftCoins;
    const projectedFamiliarity = Math.floor(projectedCoins / 5);
    pairProjections.push({
      newGiftTransactions: pair.newGiftTransactions,
      newGiftCoins: pair.newGiftCoins,
      existingGiftCoins: pair.existingGiftCoins,
      projectedTotalGiftCoins: projectedCoins,
      projectedFamiliarity,
      projectedLevel: levelFor(projectedFamiliarity)
    });
  }

  const report = {
    mode: dryRun ? "DRY_RUN_READ_ONLY" : "APPLY_PRODUCTION_WRITES",
    projectId: serviceAccount.project_id,
    ...Object.fromEntries(Object.entries(stats).filter(([key]) => key !== "pairs")),
    distinctPairsWithUnprocessedGifts: stats.pairs.size,
    pairProjections,
    writesPerformed: dryRun ? 0 : stats.appliedGiftTransactions + stats.invitationsAutoRejected + stats.publicTagsNeedingSync
  };
  console.log(JSON.stringify(report, null, 2));
  await admin.app().delete();
}

main().catch((error) => {
  console.error("Relationship worker failed:", error.message);
  process.exitCode = 1;
});
