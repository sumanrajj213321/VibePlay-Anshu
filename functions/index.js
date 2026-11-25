const { initializeApp } = require("firebase-admin/app");
const { getFirestore, FieldValue, FieldPath } = require("firebase-admin/firestore");
const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { onSchedule } = require("firebase-functions/v2/scheduler");
const { Timestamp } = require("firebase-admin/firestore");
const logger = require("firebase-functions/logger");
const { createHash } = require("node:crypto");

initializeApp();
const db = getFirestore();

const PUBLIC_GIFT_WALL_PAGE_SIZE = 250;

function toPublicGiftWallProjection(gift, transactionId) {
  if (!gift || typeof transactionId !== "string" || !transactionId ||
      typeof gift.senderId !== "string" || !gift.senderId ||
      typeof gift.recipientId !== "string" || !gift.recipientId ||
      typeof gift.giftId !== "string" || !gift.giftId ||
      typeof gift.giftName !== "string" || !gift.giftName ||
      !Number.isSafeInteger(gift.coinCost) || gift.coinCost <= 0 ||
      !Number.isSafeInteger(gift.quantity) || gift.quantity < 1) {
    return null;
  }
  return {
    giftTransactionId: transactionId,
    senderId: gift.senderId,
    recipientId: gift.recipientId,
    giftId: gift.giftId,
    giftName: gift.giftName.slice(0, 60),
    coinCost: gift.coinCost,
    quantity: Math.min(gift.quantity, 999),
    createdAt: gift.createdAt || FieldValue.serverTimestamp()
  };
}

// Keep the public projection in sync even when an older app version writes
// a gift transaction without creating its public Gift Wall document.
exports.publishGiftWallEntry = onDocumentCreated(
  {
    document: "gift_transactions/{transactionId}",
    region: "asia-south1",
    retry: true
  },
  async (event) => {
    const gift = event.data && event.data.data();
    const projection = toPublicGiftWallProjection(gift, event.params.transactionId);
    if (!projection) {
      logger.warn("Skipping malformed gift transaction for public Gift Wall", {
        transactionId: event.params.transactionId
      });
      return;
    }
    await db.collection("public_gift_wall")
      .doc(event.params.transactionId)
      .set(projection, { merge: true });
    logger.info("Published public Gift Wall entry", {
      transactionId: event.params.transactionId,
      recipientId: projection.recipientId
    });
  }
);

// Backfill old gift transactions into the minimal public Gift Wall collection.
// Existing projections are left untouched; full/private transaction fields are never copied.
exports.backfillPublicGiftWall = onSchedule(
  {
    schedule: "every 15 minutes",
    timeZone: "Asia/Kolkata",
    region: "asia-south1",
    retryCount: 3
  },
  async () => {
    let cursor = null;
    let scanned = 0;
    let published = 0;
    while (true) {
      let query = db.collection("gift_transactions")
        .orderBy(FieldPath.documentId(), "asc")
        .limit(PUBLIC_GIFT_WALL_PAGE_SIZE);
      if (cursor) query = query.startAfter(cursor);
      const page = await query.get();
      if (page.empty) break;

      const projectionRefs = page.docs.map((doc) =>
        db.collection("public_gift_wall").doc(doc.id)
      );
      const existingProjections = await db.getAll(...projectionRefs);
      const batch = db.batch();
      let writes = 0;
      for (let i = 0; i < page.docs.length; i += 1) {
        scanned += 1;
        if (existingProjections[i].exists) continue;
        const source = page.docs[i];
        const projection = toPublicGiftWallProjection(source.data(), source.id);
        if (!projection) continue;
        batch.create(projectionRefs[i], projection);
        writes += 1;
      }
      if (writes > 0) {
        await batch.commit();
        published += writes;
      }
      cursor = page.docs[page.docs.length - 1];
      if (page.size < PUBLIC_GIFT_WALL_PAGE_SIZE) break;
    }
    logger.info("Public Gift Wall backfill completed", { scanned, published });
  }
);


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

      const pairData = pair.exists ? pair.data() : {};
      const previousCoins = Number(pairData.totalGiftCoins || 0);
      const totalGiftCoins = previousCoins + coinCost;
      const familiarityPoints = Math.floor(totalGiftCoins / 5);
      const pairUpdate = {
        userIds: [senderId, recipientId].sort(),
        totalGiftCoins,
        familiarityPoints,
        level: levelFor(familiarityPoints),
        updatedAt: FieldValue.serverTimestamp()
      };

      // An accepted tag remains locked until the pair reaches 10,000 familiarity.
      const activatesTag = pairData.tagStatus === "WAITING_FAMILIARITY" &&
        familiarityPoints >= 10000 &&
        ["BUDDIES", "BRO", "CONFIDANT", "BESTIE"].includes(pairData.acceptedTagType);
      if (activatesTag) {
        pairUpdate.tagStatus = "ACTIVE";
        pairUpdate.tagType = pairData.acceptedTagType;
        pairUpdate.tagStartedAt = FieldValue.serverTimestamp();
        pairUpdate.activatedAtFamiliarity = familiarityPoints;
        pairUpdate.acceptedTagType = FieldValue.delete();
        const [senderProfile, recipientProfile] = await Promise.all([
          tx.get(db.collection("users").doc(senderId)),
          tx.get(db.collection("users").doc(recipientId))
        ]);
        const tagType = pairData.acceptedTagType;
        tx.set(db.collection("relationship_links").doc(senderId), {
          partnerId: recipientId,
          partnerName: recipientProfile.get("username") || recipientProfile.get("displayName") || "VibePlay user",
          partnerAvatarUrl: recipientProfile.get("avatarUrl") || "",
          tagType,
          updatedAt: FieldValue.serverTimestamp()
        });
        tx.set(db.collection("relationship_links").doc(recipientId), {
          partnerId: senderId,
          partnerName: senderProfile.get("username") || senderProfile.get("displayName") || "VibePlay user",
          partnerAvatarUrl: senderProfile.get("avatarUrl") || "",
          tagType,
          updatedAt: FieldValue.serverTimestamp()
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
          updatedAt: FieldValue.serverTimestamp()
        });
      }

      tx.set(pairRef, pairUpdate, { merge: true });

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


// Backfill public cards for active tags created before public-profile publishing existed.
exports.syncActiveRelationshipPublicTags = onSchedule(
  {
    schedule: "every 60 minutes",
    timeZone: "Asia/Kolkata",
    region: "asia-south1"
  },
  async () => {
    const activePairs = await db.collection("relationship_pairs")
      .where("tagStatus", "==", "ACTIVE")
      .get();
    let batch = db.batch();
    let writes = 0;
    let total = 0;

    for (const pairDoc of activePairs.docs) {
      const pair = pairDoc.data();
      const userIds = Array.isArray(pair.userIds)
        ? [...new Set(pair.userIds.filter((id) => typeof id === "string" && id))]
        : [];
      if (userIds.length !== 2) continue;

      const [userAId, userBId] = userIds.sort();
      const [userA, userB] = await Promise.all([
        db.collection("users").doc(userAId).get(),
        db.collection("users").doc(userBId).get()
      ]);
      if (!userA.exists || !userB.exists) continue;

      batch.set(db.collection("relationship_public_tags").doc(pairDoc.id), {
        userIds: [userAId, userBId],
        userAName: userA.get("username") || userA.get("displayName") || "VibePlay user",
        userAAvatarUrl: userA.get("avatarUrl") || "",
        userBName: userB.get("username") || userB.get("displayName") || "VibePlay user",
        userBAvatarUrl: userB.get("avatarUrl") || "",
        tagType: pair.tagType || "",
        updatedAt: FieldValue.serverTimestamp()
      }, { merge: true });
      writes += 1;
      total += 1;

      if (writes === 450) {
        await batch.commit();
        batch = db.batch();
        writes = 0;
      }
    }

    if (writes > 0) await batch.commit();
    logger.info("Active relationship public-tag sync completed", {
      activePairsScanned: activePairs.size,
      publicTagsWritten: total
    });
  }
);


// Select which active relationship partner is featured on the user's main profile.
exports.setMainRelationshipPartner = onCall(
  { region: "asia-south1" },
  async (request) => {
    if (!request.auth) throw new HttpsError("unauthenticated", "Please sign in first.");
    const userId = request.auth.uid;
    const partnerId = request.data && request.data.partnerId;
    if (typeof partnerId !== "string" || !partnerId || partnerId === userId || partnerId.length > 128) {
      throw new HttpsError("invalid-argument", "Choose a valid relationship partner.");
    }

    const pairId = pairKey(userId, partnerId);
    const pairRef = db.collection("relationship_pairs").doc(pairId);
    const pairSnap = await pairRef.get();
    if (!pairSnap.exists) throw new HttpsError("not-found", "Relationship tag was not found.");
    const pair = pairSnap.data();
    if (pair.tagStatus !== "ACTIVE" || !Array.isArray(pair.userIds) ||
        !pair.userIds.includes(userId) || !pair.userIds.includes(partnerId)) {
      throw new HttpsError("failed-precondition", "Only an active relationship tag can be featured.");
    }

    await db.collection("relationship_profile_selection").doc(userId).set({
      userId,
      partnerId,
      pairId,
      updatedAt: FieldValue.serverTimestamp()
    });
    return { success: true, partnerId };
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
      tx.delete(db.collection("relationship_links").doc(userId));
      tx.delete(db.collection("relationship_links").doc(otherUserId));
      tx.delete(db.collection("relationship_public_tags").doc(pairId));
    });

    return {
      success: true,
      cooldownUntil: cooldownUntil.toDate().toISOString(),
      familiarityPreserved: true
    };
  }
);


// Create a pending relationship invitation and enforce pair cooldown server-side.
exports.sendRelationshipInvite = onCall(
  { region: "asia-south1" },
  async (request) => {
    if (!request.auth) throw new HttpsError("unauthenticated", "Please sign in first.");
    const senderId = request.auth.uid;
    const otherUserId = request.data && request.data.otherUserId;
    const tagType = request.data && request.data.tagType;
    const allowedTags = ["BUDDIES", "BRO", "CONFIDANT", "BESTIE"];
    if (typeof otherUserId !== "string" || !otherUserId || otherUserId === senderId ||
        otherUserId.length > 128 || !allowedTags.includes(tagType)) {
      throw new HttpsError("invalid-argument", "Choose a valid user and relationship tag.");
    }

    const senderRef = db.collection("users").doc(senderId);
    const recipientRef = db.collection("users").doc(otherUserId);
    const pairId = pairKey(senderId, otherUserId);
    const pairRef = db.collection("relationship_pairs").doc(pairId);
    const inviteRef = db.collection("relationship_invites").doc();
    const notificationRef = db.collection("notifications").doc();

    await db.runTransaction(async (tx) => {
      const senderFollowingRef = senderRef.collection("following").doc(otherUserId);
      const [senderSnap, recipientSnap, pairSnap, followingSnap] = await Promise.all([
        tx.get(senderRef), tx.get(recipientRef), tx.get(pairRef), tx.get(senderFollowingRef)
      ]);
      if (!senderSnap.exists || !recipientSnap.exists ||
          senderSnap.get("accountStatus") !== "ACTIVE" ||
          recipientSnap.get("accountStatus") !== "ACTIVE") {
        throw new HttpsError("not-found", "The selected user is unavailable.");
      }
      if (!followingSnap.exists) {
        throw new HttpsError("permission-denied", "You can invite only users you follow.");
      }
      const pair = pairSnap.exists ? pairSnap.data() : {};
      if (pair.tagStatus === "ACTIVE") {
        throw new HttpsError("already-exists", "You already have an active relationship tag with this user.");
      }
      if (pair.pendingInviteId) {
        const oldInviteRef = db.collection("relationship_invites").doc(pair.pendingInviteId);
        const oldInviteSnap = await tx.get(oldInviteRef);
        const oldInvite = oldInviteSnap.exists ? oldInviteSnap.data() : null;
        const oldExpiry = oldInvite && oldInvite.expiresAt && typeof oldInvite.expiresAt.toMillis === "function"
          ? oldInvite.expiresAt.toMillis()
          : oldInvite && oldInvite.createdAt && typeof oldInvite.createdAt.toMillis === "function"
            ? oldInvite.createdAt.toMillis() + 48 * 60 * 60 * 1000
            : Date.now() + 48 * 60 * 60 * 1000;
        if (oldInvite && oldInvite.status === "PENDING" && oldExpiry > Date.now()) {
          throw new HttpsError("already-exists", "A relationship invitation is already pending.");
        }
        if (oldInvite && oldInvite.status === "PENDING") {
          tx.update(oldInviteRef, {
            status: "REJECTED", autoRejected: true, rejectionReason: "NO_RESPONSE_48_HOURS",
            respondedAt: FieldValue.serverTimestamp()
          });
          if (oldInvite.notificationId) {
            tx.set(db.collection("notifications").doc(oldInvite.notificationId), { read: true }, { merge: true });
          }
        }
        tx.set(pairRef, {
          tagStatus: "NONE", pendingInviteId: FieldValue.delete(),
          pendingTagType: FieldValue.delete(), invitedBy: FieldValue.delete(),
          invitedTo: FieldValue.delete(), pendingAt: FieldValue.delete()
        }, { merge: true });
      }
      if (pair.tagCooldownUntil && pair.tagCooldownUntil.toMillis() > Date.now()) {
        throw new HttpsError("failed-precondition", "You can invite this user again after the seven-day cooldown.");
      }

      const senderName = String(senderSnap.get("username") || "VibeUser").slice(0, 60);
      const recipientName = String(recipientSnap.get("username") || "VibeUser").slice(0, 60);
      const senderAvatar = String(senderSnap.get("avatarUrl") || "");
      const recipientAvatar = String(recipientSnap.get("avatarUrl") || "");
      tx.create(inviteRef, {
        pairId, inviterId: senderId, invitedTo: otherUserId,
        inviterName: senderName, invitedName: recipientName,
        inviterAvatarUrl: senderAvatar, invitedAvatarUrl: recipientAvatar,
        tagType, status: "PENDING", notificationId: notificationRef.id,
        createdAt: FieldValue.serverTimestamp(),
        expiresAt: Timestamp.fromMillis(Date.now() + 48 * 60 * 60 * 1000)
      });
      tx.set(pairRef, {
        userIds: [senderId, otherUserId].sort(),
        userDisplayNames: { [senderId]: senderName, [otherUserId]: recipientName },
        userAvatarUrls: { [senderId]: senderAvatar, [otherUserId]: recipientAvatar },
        tagStatus: "PENDING", pendingInviteId: inviteRef.id,
        pendingTagType: tagType, invitedBy: senderId, invitedTo: otherUserId,
        pendingAt: FieldValue.serverTimestamp(), updatedAt: FieldValue.serverTimestamp()
      }, { merge: true });
      tx.create(notificationRef, {
        recipientId: otherUserId, senderId, senderName,
        type: "RELATIONSHIP_INVITE", title: "Relationship invitation",
        body: senderName + " invited you to become " + tagType.toLowerCase() + ".",
        targetId: inviteRef.id, read: false, createdAt: FieldValue.serverTimestamp()
      });
    });
    return { success: true, inviteId: inviteRef.id };
  }
);

// Only the invited user can accept or reject a pending invitation.
exports.respondRelationshipInvite = onCall(
  { region: "asia-south1" },
  async (request) => {
    if (!request.auth) throw new HttpsError("unauthenticated", "Please sign in first.");
    const userId = request.auth.uid;
    const inviteId = request.data && request.data.inviteId;
    const accept = request.data && request.data.accept;
    if (typeof inviteId !== "string" || !inviteId || inviteId.length > 128 ||
        typeof accept !== "boolean") {
      throw new HttpsError("invalid-argument", "Invalid invitation response.");
    }
    const inviteRef = db.collection("relationship_invites").doc(inviteId);
    let inviterId = "";
    let pairId = "";
    let expired = false;
    await db.runTransaction(async (tx) => {
      const inviteSnap = await tx.get(inviteRef);
      if (!inviteSnap.exists) throw new HttpsError("not-found", "Invitation not found.");
      const invite = inviteSnap.data();
      if (invite.invitedTo !== userId) {
        throw new HttpsError("permission-denied", "Only the invited user can respond.");
      }
      if (invite.status !== "PENDING") {
        throw new HttpsError("failed-precondition", "This invitation has already been answered.");
      }
      pairId = invite.pairId;
      inviterId = invite.inviterId;
      const pairRef = db.collection("relationship_pairs").doc(pairId);
      const pairSnap = await tx.get(pairRef);
      if (!pairSnap.exists || pairSnap.get("pendingInviteId") !== inviteId) {
        throw new HttpsError("failed-precondition", "This invitation is no longer active.");
      }
      const expiryMs = invite.expiresAt && typeof invite.expiresAt.toMillis === "function"
        ? invite.expiresAt.toMillis()
        : invite.createdAt && typeof invite.createdAt.toMillis === "function"
          ? invite.createdAt.toMillis() + 48 * 60 * 60 * 1000
          : 0;
      const now = FieldValue.serverTimestamp();
      if (expiryMs > 0 && expiryMs <= Date.now()) {
        tx.update(inviteRef, {
          status: "REJECTED", autoRejected: true,
          rejectionReason: "NO_RESPONSE_48_HOURS", respondedAt: now
        });
        if (invite.notificationId) {
          tx.set(db.collection("notifications").doc(invite.notificationId), { read: true }, { merge: true });
        }
        tx.update(pairRef, {
          tagStatus: "NONE", pendingInviteId: FieldValue.delete(),
          pendingTagType: FieldValue.delete(), invitedBy: FieldValue.delete(),
          invitedTo: FieldValue.delete(), pendingAt: FieldValue.delete(),
          updatedAt: now
        });
        expired = true;
        return;
      }
      tx.update(inviteRef, {
        status: accept ? "ACCEPTED" : "REJECTED",
        respondedAt: now
      });
      if (accept) {
        const familiarityPoints = Number(pairSnap.get("familiarityPoints") || 0);
        const canActivate = familiarityPoints >= 10000;
        if (canActivate) {
          const [inviterProfile, recipientProfile] = await Promise.all([
            tx.get(db.collection("users").doc(inviterId)),
            tx.get(db.collection("users").doc(userId))
          ]);
          tx.set(db.collection("relationship_links").doc(inviterId), {
            partnerId: userId,
            partnerName: recipientProfile.get("username") || recipientProfile.get("displayName") || "VibePlay user",
            partnerAvatarUrl: recipientProfile.get("avatarUrl") || "",
            tagType: invite.tagType,
            updatedAt: now
          });
          tx.set(db.collection("relationship_links").doc(userId), {
            partnerId: inviterId,
            partnerName: inviterProfile.get("username") || inviterProfile.get("displayName") || "VibePlay user",
            partnerAvatarUrl: inviterProfile.get("avatarUrl") || "",
            tagType: invite.tagType,
            updatedAt: now
          });
          const [userAId, userBId] = [inviterId, userId].sort();
          const userAProfile = userAId === inviterId ? inviterProfile : recipientProfile;
          const userBProfile = userBId === inviterId ? inviterProfile : recipientProfile;
          tx.set(db.collection("relationship_public_tags").doc(pairId), {
            userIds: [userAId, userBId],
            userAName: userAProfile.get("username") || userAProfile.get("displayName") || "VibePlay user",
            userAAvatarUrl: userAProfile.get("avatarUrl") || "",
            userBName: userBProfile.get("username") || userBProfile.get("displayName") || "VibePlay user",
            userBAvatarUrl: userBProfile.get("avatarUrl") || "",
            tagType: invite.tagType,
            updatedAt: now
          });
        }
        tx.update(pairRef, {
          tagStatus: canActivate ? "ACTIVE" : "WAITING_FAMILIARITY",
          ...(canActivate ? {
            tagType: invite.tagType,
            tagStartedAt: now,
            activatedAtFamiliarity: familiarityPoints
          } : {
            acceptedTagType: invite.tagType
          }),
          activeInviteId: inviteId,
          pendingInviteId: FieldValue.delete(),
          pendingTagType: FieldValue.delete(),
          invitedBy: FieldValue.delete(), invitedTo: FieldValue.delete(),
          pendingAt: FieldValue.delete(), updatedAt: now
        });
      } else {
        tx.update(pairRef, {
          tagStatus: "NONE",
          pendingInviteId: FieldValue.delete(),
          pendingTagType: FieldValue.delete(),
          invitedBy: FieldValue.delete(), invitedTo: FieldValue.delete(),
          pendingAt: FieldValue.delete(), updatedAt: now
        });
      }
    });

    if (expired) {
      throw new HttpsError("deadline-exceeded", "This invitation expired after 48 hours without a response.");
    }
    const responder = await db.collection("users").doc(userId).get();
    const responderName = String(responder.get("username") || "VibeUser").slice(0, 60);
    await db.collection("notifications").add({
      recipientId: inviterId, senderId: userId, senderName: responderName,
      type: "SYSTEM",
      title: accept ? "Relationship invitation accepted" : "Relationship invitation declined",
      body: responderName + (accept ? " accepted your relationship invitation." : " declined your relationship invitation."),
      targetId: pairId, read: false, createdAt: FieldValue.serverTimestamp()
    });
    return { success: true, status: accept ? "ACCEPTED" : "REJECTED" };
  }
);


// Automatically reject unanswered relationship invitations after 48 hours.
// Scan by document ID so legacy records without createdAt are not silently skipped.
exports.expireRelationshipInvites = onSchedule(
  {
    schedule: "every 15 minutes",
    timeZone: "Asia/Kolkata",
    region: "asia-south1",
    retryCount: 3
  },
  async () => {
    const nowMs = Date.now();
    const pageSize = 250;
    let cursor = null;
    let expiredCount = 0;

    while (true) {
      let query = db.collection("relationship_invites")
        .orderBy(FieldPath.documentId(), "asc")
        .limit(pageSize);
      if (cursor) query = query.startAfter(cursor);
      const page = await query.get();
      if (page.empty) break;

      for (const inviteDoc of page.docs) {
        const invite = inviteDoc.data();
        if (invite.status !== "PENDING") continue;
        const expiresAtMs = invite.expiresAt && typeof invite.expiresAt.toMillis === "function"
          ? invite.expiresAt.toMillis()
          : invite.createdAt && typeof invite.createdAt.toMillis === "function"
            ? invite.createdAt.toMillis() + 48 * 60 * 60 * 1000
            : 0;
        if (!expiresAtMs || expiresAtMs > nowMs) continue;

        const inviteRef = inviteDoc.ref;
        const pairRef = db.collection("relationship_pairs").doc(invite.pairId);
        const result = await db.runTransaction(async (tx) => {
          const [freshInvite, freshPair] = await Promise.all([tx.get(inviteRef), tx.get(pairRef)]);
          if (!freshInvite.exists || freshInvite.get("status") !== "PENDING") return null;
          const current = freshInvite.data();
          const currentExpiry = current.expiresAt && typeof current.expiresAt.toMillis === "function"
            ? current.expiresAt.toMillis()
            : current.createdAt && typeof current.createdAt.toMillis === "function"
              ? current.createdAt.toMillis() + 48 * 60 * 60 * 1000
              : 0;
          if (!currentExpiry || currentExpiry > Date.now()) return null;

          tx.update(inviteRef, {
            status: "REJECTED", autoRejected: true,
            rejectionReason: "NO_RESPONSE_48_HOURS",
            respondedAt: FieldValue.serverTimestamp()
          });
          if (current.notificationId) {
            tx.set(db.collection("notifications").doc(current.notificationId), { read: true }, { merge: true });
          }
          if (freshPair.exists && freshPair.get("pendingInviteId") === inviteDoc.id) {
            tx.update(pairRef, {
              tagStatus: "NONE", pendingInviteId: FieldValue.delete(),
              pendingTagType: FieldValue.delete(), invitedBy: FieldValue.delete(),
              invitedTo: FieldValue.delete(), pendingAt: FieldValue.delete(),
              updatedAt: FieldValue.serverTimestamp()
            });
          }
          return { inviterId: current.inviterId, invitedTo: current.invitedTo, pairId: current.pairId };
        });

        if (result && result.inviterId) {
          await db.collection("notifications").add({
            recipientId: result.inviterId,
            senderId: result.invitedTo || "VIBEPLAY_SYSTEM",
            senderName: "VibePlay",
            type: "SYSTEM",
            title: "Relationship invitation expired",
            body: "Your relationship invitation was automatically rejected because there was no response within 48 hours.",
            targetId: result.pairId || "",
            read: false,
            createdAt: FieldValue.serverTimestamp()
          });
          expiredCount += 1;
        }
      }

      cursor = page.docs[page.docs.length - 1];
      if (page.size < pageSize) break;
    }

    logger.info("Relationship invitation expiry scan completed", { expiredCount });
  }
);
