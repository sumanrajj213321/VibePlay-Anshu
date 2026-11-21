const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const { onCall, HttpsError } = require("firebase-functions/v2/https");
const admin = require("firebase-admin");
const crypto = require("crypto");

admin.initializeApp();
const db = admin.firestore();
const BONUS = 1000000;
const ADMIN_UID = "1fLIHgOyAJV6LNbHBXNeVg9JR6v1";
const BOT_UID = "VIBEPLAY_OFFICIAL_BOT";
const BOT_NAME = "VibePlay Official";
const BOT_TEXT = "🎉 10 LAKH COINS WELCOME BONUS!\n\nCongratulations! 🥳 VibePlay ki taraf se aapke account mein 10,00,000 bonus coins add kiye gaye hain.\n\n❤️ Enjoy VibePlay, send gifts and have fun!\n\n— Team VibePlay";

function conversationId(uid) {
  return "bot_" + crypto.createHash("sha256").update(uid).digest("hex").slice(0, 40);
}

async function grantOne(uid) {
  const userRef = db.collection("users").doc(uid);
  const convRef = db.collection("conversations").doc(conversationId(uid));
  const msgRef = convRef.collection("messages").doc("welcome_10_lakh_bonus");
  return db.runTransaction(async (tx) => {
    const userSnap = await tx.get(userRef);
    if (!userSnap.exists) return false;
    if (userSnap.get("welcomeBonusGranted") === true) return false;

    const username = userSnap.get("username") || "VibeStar";
    const avatar = userSnap.get("avatarUrl") || "";
    const currentCoins = Number(userSnap.get("coinBalance") || 0);
    if (!Number.isSafeInteger(currentCoins) || currentCoins < 0) {
      throw new Error("Invalid coin balance for " + uid);
    }

    tx.update(userRef, {
      coinBalance: currentCoins + BONUS,
      welcomeBonusGranted: true,
      welcomeBonusGrantedAt: admin.firestore.FieldValue.serverTimestamp(),
      updatedAt: admin.firestore.FieldValue.serverTimestamp()
    });

    tx.set(convRef, {
      conversationType: "DIRECT",
      groupName: "",
      groupAvatarUrl: "",
      ownerId: "",
      adminIds: [],
      participantIds: [uid, BOT_UID],
      participantNames: { [uid]: username, [BOT_UID]: BOT_NAME },
      participantAvatars: { [uid]: avatar, [BOT_UID]: "" },
      lastMessage: "🎉 10 lakh coins welcome bonus credited!",
      lastSenderId: BOT_UID,
      unreadBy: [uid],
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
      updatedAt: admin.firestore.FieldValue.serverTimestamp()
    }, { merge: true });

    tx.set(msgRef, {
      participantIds: [uid, BOT_UID],
      senderId: BOT_UID,
      senderName: BOT_NAME,
      senderAvatarUrl: "",
      type: "SYSTEM",
      text: BOT_TEXT,
      read: false,
      createdAt: admin.firestore.FieldValue.serverTimestamp()
    }, { merge: true });

    return true;
  });
}

// Newly-created account: grant once and then send its official Inbox message.
exports.grantWelcomeBonusOnNewUser = onDocumentCreated({ document: "users/{uid}", region: "asia-south1" }, async (event) => {
  await grantOne(event.params.uid);
});

// Existing-user migration. Only the configured official admin account can invoke it.
// Call repeatedly with nextCursor until hasMore is false.
exports.grantWelcomeBonusToExistingUsers = onCall(
  { timeoutSeconds: 540, memory: "512MiB", region: "asia-south1" },
  async (request) => {
    if (!request.auth || request.auth.uid !== ADMIN_UID) {
      throw new HttpsError("permission-denied", "Only VibePlay Official can run this migration.");
    }

    const cursor = typeof request.data?.cursor === "string" ? request.data.cursor : null;
    let query = db.collection("users").orderBy(admin.firestore.FieldPath.documentId()).limit(200);
    if (cursor) query = query.startAfter(cursor);

    const page = await query.get();
    let granted = 0;
    for (const doc of page.docs) {
      if (await grantOne(doc.id)) granted++;
    }

    const nextCursor = page.docs.length ? page.docs[page.docs.length - 1].id : cursor;
    return {
      processed: page.size,
      granted,
      nextCursor,
      hasMore: page.size === 200
    };
  }
);
