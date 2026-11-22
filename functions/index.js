const { initializeApp } = require("firebase-admin/app");
const { getFirestore, FieldValue } = require("firebase-admin/firestore");
const { onDocumentCreated } = require("firebase-functions/v2/firestore");
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
