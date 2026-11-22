const { initializeApp } = require("firebase-admin/app");
const { getFirestore, FieldValue } = require("firebase-admin/firestore");
const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { Timestamp } = require("firebase-admin/firestore");
const logger = require("firebase-functions/logger");
const { createHash } = require("node:crypto");

initializeApp();
const db = getFirestore();

const LEVELS = [
  10000, 30000, 60000, 100000, 150000,
  220000, 300000, 400000, 550000, 750000
];

function pairKey(uidA, uidB) {
  return createHash("sha256").update([uidA, uidB].sort().join(":")).digest("hex");
}

function levelFor(points) {
  let level = 0;
  for (let i = 0; i < LEVELS.length; i += 1) {
    if (points >= LEVELS[i]) level = i + 1;
    else break;
  }
  return level;
}

exports.recordRelationshipFamiliarity = onDocumentCreated(
  {
    document: "gift_transactions/{giftId}",
    region: "asia-south1",
    retry: true
  },
  async (event) => {
    const gift = event.data && event.data.data();
    if (!gift) return;

    const senderId = gift.senderId;
    const recipientId = gift.recipientId;
    const coinCost = gift.coinCost;

    // Self-gifts and malformed gift records never build relationship familiarity.
    if (typeof senderId !== "string" || typeof recipientId !== "string" ||
        !senderId || !recipientId || senderId === recipientId ||
        !Number.isSafeInteger(coinCost) || coinCost <= 0) {
      return;
    }

    const pairId = pairKey(senderId, recipientId);
    const pairRef = db.collection("relationship_pairs").doc(pairId);
    const eventRef = db.collection("relationship_gift_events").doc(event.params.giftId);

    await db.runTransaction(async (tx) => {
      const [processed, pair] = await Promise.all([
        tx.get(eventRef),
        tx.get(pairRef)
      ]);
      if (processed.exists) return;

      const previousCoins = pair.exists ? (pair.get("totalGiftCoins") || 0) : 0;
      const totalGiftCoins = previousCoins + coinCost;
      const familiarityPoints = Math.floor(totalGiftCoins / 5);

      tx.set(pairRef, {
        userIds: [senderId, recipientId].sort(),
        totalGiftCoins,
        familiarityPoints,
        level: levelFor(familiarityPoints),
        updatedAt: FieldValue.serverTimestamp()
      }, { merge: true });

      tx.create(eventRef, {
        giftId: event.params.giftId,
        pairId,
        coinCost,
        processedAt: FieldValue.serverTimestamp()
      });
    });

    logger.info("Relationship familiarity updated", {
      pairId,
      giftId: event.params.giftId,
      coinCost
    });
  }
);


// Break an active relationship tag without deleting or resetting familiarity.
// The pair remains locked from creating another tag for seven full days.
exports.breakRelationshipTag = onCall(
  { region: "asia-south1" },
  async (request) => {
    if (!request.auth) {
      throw new HttpsError("unauthenticated", "Please sign in first.");
    }

    const userId = request.auth.uid;
    const otherUserId = request.data && request.data.otherUserId;
    if (typeof otherUserId !== "string" || !otherUserId ||
        otherUserId === userId || otherUserId.length > 128) {
      throw new HttpsError("invalid-argument", "A valid other user is required.");
    }

    const pairId = pairKey(userId, otherUserId);
    const pairRef = db.collection("relationship_pairs").doc(pairId);
    const cooldownUntil = Timestamp.fromMillis(Date.now() + 7 * 24 * 60 * 60 * 1000);

    await db.runTransaction(async (tx) => {
      const pairSnap = await tx.get(pairRef);
      if (!pairSnap.exists) {
        throw new HttpsError("not-found", "Relationship pair was not found.");
      }

      const pair = pairSnap.data();
      if (!Array.isArray(pair.userIds) || !pair.userIds.includes(userId) ||
          !pair.userIds.includes(otherUserId)) {
        throw new HttpsError("permission-denied", "You are not part of this relationship.");
      }
      if (pair.tagStatus !== "ACTIVE") {
        throw new HttpsError("failed-precondition", "There is no active tag to break.");
      }

      tx.update(pairRef, {
        tagStatus: "BROKEN",
        tagBrokenAt: FieldValue.serverTimestamp(),
        tagBrokenBy: userId,
        tagCooldownUntil: cooldownUntil
      });
    });

    return {
      success: true,
      cooldownUntil: cooldownUntil.toDate().toISOString(),
      familiarityPreserved: true
    };
  }
);
