const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_1";
const BOB_UID = "bob_uid_2";
const ADMIN_UID = "admin_uid_0";
const ADMIN_EMAIL = "sumanrajj213321@gmail.com";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastDate(msOffset = 2000) {
  return new Date(Date.now() - msOffset);
}

function makeUserPayload(uid, username, vibeId) {
  const ts = pastDate(5000);
  return {
    userId: uid,
    vibeId,
    username,
    bio: "Ready to vibe in voice rooms!",
    avatarUrl: "",
    coverUrl: "",
    gender: "UNSPECIFIED",
    age: 0,
    showAge: false,
    showGender: true,
    showOnlineStatus: true,
    online: true,
    level: 1,
    xp: 0,
    vipLevel: 0,
    coinBalance: 500,
    totalGiftsReceived: 0,
    totalGiftsSent: 0,
    followersCount: 0,
    followingCount: 0,
    accountStatus: "ACTIVE",
    role: "USER",
    lastDailyClaimDay: "",
    createdAt: ts,
    updatedAt: ts,
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated users cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").where("accountStatus", "==", "ACTIVE").get());
  await assertFails(unauthDb.collection("rooms").where("status", "==", "LIVE").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).set(makeUserPayload(ALICE_UID, "Alice", "VP-100001"))
  );
});

test("User profile creation, private subcollection isolation, daily reward, and VIP upgrade", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  // Create valid initial profile
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(makeUserPayload(ALICE_UID, "Alice", "VP-100001"))
  );

  // Reject creating profile with inflated coins or ADMIN role
  await assertFails(
    bobDb.collection("users").doc(BOB_UID).set({
      ...makeUserPayload(BOB_UID, "Bob", "VP-200002"),
      coinBalance: 99999,
    })
  );
  await assertFails(
    bobDb.collection("users").doc(BOB_UID).set({
      ...makeUserPayload(BOB_UID, "Bob", "VP-200002"),
      role: "ADMIN",
    })
  );

  // Private subcollection belongs strictly to owner
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("private").doc("account").set({
      userId: ALICE_UID,
      email: "alice@example.com",
      blockedUserIds: [],
      updatedAt: pastDate(),
    })
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("private").doc("account").get()
  );

  // Valid daily login reward claim (+150 coins)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      coinBalance: 650,
      lastDailyClaimDay: "2026-11-18",
      lastDailyClaimAt: pastDate(2 * 24 * 60 * 60 * 1000),
      xp: 50,
      level: 1,
      updatedAt: pastDate(),
    })
  );

  // Valid VIP 1 upgrade (650 - 200 = 450)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      coinBalance: 450,
      vipLevel: 1,
      vipExpiresAt: new Date(Date.now() + 30 * 24 * 60 * 60 * 1000),
      updatedAt: pastDate(),
    })
  );

  // Reject arbitrary self-coin inflation
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      coinBalance: 50000,
      updatedAt: pastDate(),
    })
  );

  // Reject future timestamp
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      bio: "Future tamper",
      updatedAt: new Date(Date.now() + 120000),
    })
  );
});

test("Strict 9-seat Voice Room batch creation, seat 9 rejection, seat 0 protection, and listener isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = pastDate(5000);

  const roomRef = aliceDb.collection("rooms").doc("room_alpha");
  const lockRef = aliceDb.collection("room_creation_locks").doc(ALICE_UID);
  const batch = aliceDb.batch();

  batch.set(lockRef, {
    userId: ALICE_UID,
    roomId: "room_alpha",
    createdAt: ts,
  });

  batch.set(roomRef, {
    creatorId: ALICE_UID,
    hostId: ALICE_UID,
    hostName: "Alice",
    hostAvatarUrl: "",
    title: "Neon Night Stage",
    announcement: "Welcome everyone!",
    category: "Music",
    coverKey: "party_stage",
    tags: ["Music", "Live"],
    listenerCount: 0,
    activeSpeakersCount: 1,
    locked: false,
    hasPassword: false,
    passcodeHash: "",
    status: "LIVE",
    featured: false,
    createdAt: ts,
    updatedAt: ts,
  });

  // Seat 0 = Host Seat
  batch.set(roomRef.collection("seats").doc("0"), {
    seatIndex: 0,
    role: "HOST",
    occupantId: ALICE_UID,
    occupantName: "Alice",
    occupantAvatarUrl: "",
    occupantVipLevel: 0,
    muted: false,
    seatLocked: false,
    speaking: false,
    updatedAt: ts,
  });

  // Seats 1..8 = 8 Regular Speaker Seats
  for (let i = 1; i <= 8; i++) {
    batch.set(roomRef.collection("seats").doc(String(i)), {
      seatIndex: i,
      role: "SPEAKER",
      occupantId: "",
      occupantName: "",
      occupantAvatarUrl: "",
      occupantVipLevel: 0,
      muted: true,
      seatLocked: false,
      speaking: false,
      updatedAt: ts,
    });
  }

  await assertSucceeds(batch.commit());

  // Creating a 10th seat (index "9") MUST be rejected by rules
  await assertFails(
    aliceDb.collection("rooms").doc("room_alpha").collection("seats").doc("9").set({
      seatIndex: 9,
      role: "SPEAKER",
      occupantId: "",
      occupantName: "",
      occupantAvatarUrl: "",
      occupantVipLevel: 0,
      muted: true,
      seatLocked: false,
      speaking: false,
      updatedAt: ts,
    })
  );

  // Filtered 9-seat query matches RoomRepository.observeNineSeats
  await assertSucceeds(
    bobDb
      .collection("rooms")
      .doc("room_alpha")
      .collection("seats")
      .where("seatIndex", ">=", 0)
      .where("seatIndex", "<=", 8)
      .get()
  );

  // Bob joins as listener (matching RoomRepository.joinAsListener)
  await assertSucceeds(
    bobDb.collection("rooms").doc("room_alpha").collection("listeners").doc(BOB_UID).set({
      userId: BOB_UID,
      username: "Bob",
      avatarUrl: "",
      vipLevel: 0,
      role: "LISTENER",
      joinedAt: pastDate(),
    })
  );

  // Bob CANNOT take Host Seat 0
  await assertFails(
    bobDb.collection("rooms").doc("room_alpha").collection("seats").doc("0").update({
      occupantId: BOB_UID,
      occupantName: "Bob",
      updatedAt: pastDate(),
    })
  );

  // Bob joins Speaker Seat 1 and removes himself from listeners (never in both)
  const joinSeatBatch = bobDb.batch();
  joinSeatBatch.update(
    bobDb.collection("rooms").doc("room_alpha").collection("seats").doc("1"),
    {
      occupantId: BOB_UID,
      occupantName: "Bob",
      occupantAvatarUrl: "",
      occupantVipLevel: 0,
      muted: false,
      speaking: false,
      updatedAt: pastDate(),
    }
  );
  joinSeatBatch.delete(
    bobDb.collection("rooms").doc("room_alpha").collection("listeners").doc(BOB_UID)
  );
  await assertSucceeds(joinSeatBatch.commit());

  // Safe ownership transfer from Alice to Bob (seated in seat 1)
  const transferBatch = aliceDb.batch();
  const transferTs = pastDate();
  transferBatch.update(roomRef, {
    hostId: BOB_UID,
    hostName: "Bob",
    hostAvatarUrl: "",
    updatedAt: transferTs,
  });
  transferBatch.update(roomRef.collection("seats").doc("0"), {
    occupantId: BOB_UID,
    occupantName: "Bob",
    occupantAvatarUrl: "",
    occupantVipLevel: 0,
    muted: false,
    speaking: false,
    updatedAt: transferTs,
  });
  transferBatch.update(roomRef.collection("seats").doc("1"), {
    occupantId: "",
    occupantName: "",
    occupantAvatarUrl: "",
    occupantVipLevel: 0,
    muted: true,
    speaking: false,
    updatedAt: transferTs,
  });
  await assertSucceeds(transferBatch.commit());
});

test("Room chat, speaker requests, virtual gift batch, and filtered repository queries succeed", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = pastDate(5000);

  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const admin = ctx.firestore();
    await admin.collection("users").doc(ALICE_UID).set(makeUserPayload(ALICE_UID, "Alice", "VP-100001"));
    await admin.collection("users").doc(BOB_UID).set(makeUserPayload(BOB_UID, "Bob", "VP-200002"));
    await admin.collection("rooms").doc("room_1").set({
      hostId: ALICE_UID,
      hostName: "Alice",
      hostAvatarUrl: "",
      title: "Chill Vibes",
      announcement: "Hi",
      category: "Chill",
      coverKey: "chill_lounge",
      tags: ["Chill"],
      listenerCount: 1,
      activeSpeakersCount: 1,
      locked: true,
      hasPassword: false,
      passcodeHash: "",
      status: "LIVE",
      featured: false,
      createdAt: ts,
      updatedAt: ts,
    });
  });

  // Bob requests to speak in locked room
  await assertSucceeds(
    bobDb.collection("rooms").doc("room_1").collection("speaker_requests").doc(BOB_UID).set({
      userId: BOB_UID,
      hostId: ALICE_UID,
      username: "Bob",
      avatarUrl: "",
      vipLevel: 0,
      status: "PENDING",
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );

  // Host approves request
  await assertSucceeds(
    aliceDb.collection("rooms").doc("room_1").collection("speaker_requests").doc(BOB_UID).update({
      status: "APPROVED",
      updatedAt: pastDate(),
    })
  );

  // A room session is refreshed on every join and limits chat visibility.
  const roomRef = bobDb.collection("rooms").doc("room_1");
  const joinedAt = pastDate(5000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("rooms").doc("room_1").collection("messages").doc("old_msg").set({
      senderId: ALICE_UID,
      senderName: "Alice",
      text: "Before Bob joined",
      type: "TEXT",
      createdAt: pastDate(10000),
    });
  });
  await assertSucceeds(
    roomRef.collection("room_sessions").doc(BOB_UID).set({
      userId: BOB_UID,
      joinedAt,
    })
  );

  // Bob sends a room chat message after joining.
  await assertSucceeds(
    roomRef.collection("messages").doc("msg_1").set({
      senderId: BOB_UID,
      senderName: "Bob",
      senderVipLevel: 0,
      text: "Great room!",
      type: "TEXT",
      createdAt: pastDate(1000),
    })
  );
  const visibleRoomMessages = await assertSucceeds(
    roomRef.collection("messages")
      .where("createdAt", ">", joinedAt)
      .where("type", "in", ["TEXT", "GIFT", "SYSTEM"])
      .get()
  );
  if (visibleRoomMessages.docs.some((doc) => doc.id === "old_msg")) {
    throw new Error("Messages from before the latest room join must not be visible.");
  }
  await assertFails(roomRef.collection("messages").get());

  // Leaving deletes the session, immediately revoking chat history access.
  await assertSucceeds(roomRef.collection("room_sessions").doc(BOB_UID).delete());
  await assertFails(
    roomRef.collection("messages")
      .where("createdAt", ">", joinedAt)
      .where("type", "in", ["TEXT", "GIFT", "SYSTEM"])
      .get()
  );

  // Bob sends a virtual gift to Alice (matching RoomRepository.sendVirtualGift)
  const giftBatch = bobDb.batch();
  giftBatch.update(bobDb.collection("users").doc(BOB_UID), {
    coinBalance: 450,
    totalGiftsSent: 1,
    xp: 25,
    level: 1,
    updatedAt: pastDate(),
  });
  giftBatch.update(bobDb.collection("users").doc(ALICE_UID), {
    totalGiftsReceived: 1,
    xp: 50,
    level: 1,
    updatedAt: pastDate(),
  });
  giftBatch.set(bobDb.collection("gift_transactions").doc("tx_1"), {
    senderId: BOB_UID,
    senderName: "Bob",
    recipientId: ALICE_UID,
    recipientName: "Alice",
    roomId: "room_1",
    giftId: "gift_fire",
    giftName: "Vibe Fire",
    coinCost: 50,
    quantity: 1,
    participantIds: [BOB_UID, ALICE_UID],
    createdAt: pastDate(),
  });
  await assertSucceeds(giftBatch.commit());
});

test("Private 1-to-1 conversations, images, notifications, and unfiltered query rejection", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext("charlie_uid").firestore();
  const ts = pastDate(5000);
  const convId = `${ALICE_UID}_${BOB_UID}`;

  await assertSucceeds(
    aliceDb.collection("conversations").doc(convId).set({
      participantIds: [ALICE_UID, BOB_UID],
      participantNames: { [ALICE_UID]: "Alice", [BOB_UID]: "Bob" },
      participantAvatars: { [ALICE_UID]: "", [BOB_UID]: "" },
      lastMessage: "Conversation started",
      lastSenderId: ALICE_UID,
      unreadBy: [],
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Send image message
  await assertSucceeds(
    aliceDb.collection("conversations").doc(convId).collection("messages").doc("dm_1").set({
      participantIds: [ALICE_UID, BOB_UID],
      senderId: ALICE_UID,
      type: "IMAGE",
      text: "Check this out",
      imageUri: "content://media/picker/0/photo.jpg",
      read: false,
      createdAt: pastDate(),
    })
  );

  // Charlie (non-participant) cannot read or query Alice & Bob's chat
  await assertFails(charlieDb.collection("conversations").doc(convId).get());
  await assertFails(
    charlieDb.collection("conversations").doc(convId).collection("messages").get()
  );
  await assertFails(
    charlieDb
      .collection("conversations")
      .doc(convId)
      .collection("messages")
      .where("participantIds", "array-contains", "charlie_uid")
      .get()
  );

  // Bob can query his conversations & messages
  await assertSucceeds(
    bobDb.collection("conversations").where("participantIds", "array-contains", BOB_UID).get()
  );
  await assertSucceeds(
    bobDb
      .collection("conversations")
      .doc(convId)
      .collection("messages")
      .where("participantIds", "array-contains", BOB_UID)
      .get()
  );

  // Unfiltered top-level queries must fail for normal users
  await assertFails(aliceDb.collection("conversations").get());
  await assertFails(aliceDb.collection("notifications").get());
  await assertFails(aliceDb.collection("reports").get());
});

test("Admin bootstrap email requires email_verified=true and blocks spoofing", async () => {
  const spoofedAdminDb = testEnv
    .authenticatedContext("spoof_uid", { email: ADMIN_EMAIL, email_verified: false })
    .firestore();
  const realAdminDb = testEnv
    .authenticatedContext(ADMIN_UID, { email: ADMIN_EMAIL, email_verified: true })
    .firestore();

  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("users").doc(BOB_UID).set(makeUserPayload(BOB_UID, "Bob", "VP-200002"));
  });

  // Unverified email claim MUST fail admin actions
  await assertFails(
    spoofedAdminDb.collection("users").doc(BOB_UID).update({
      accountStatus: "BANNED",
      updatedAt: pastDate(),
    })
  );
  await assertFails(spoofedAdminDb.collection("reports").get());

  // Verified bootstrapped admin succeeds in moderating user & adding catalog gift
  await assertSucceeds(
    realAdminDb.collection("users").doc(BOB_UID).update({
      accountStatus: "SUSPENDED",
      vipLevel: 3,
      coinBalance: 2000,
      updatedAt: pastDate(),
    })
  );
  await assertSucceeds(
    realAdminDb.collection("gifts_catalog").doc("gift_custom").set({
      name: "Diamond Mic",
      category: "Luxury",
      coinCost: 800,
      emoji: "💎",
      accentColorHex: "#EC4899",
      active: true,
      updatedAt: pastDate(),
    })
  );
});

test("Speaker requests: prevent duplicates, allow cancel, support reject/re-request/approve, prevent overlapping seat occupation, and reuse vacated seat", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieUid = "charlie_uid_3";
  const charlieDb = testEnv.authenticatedContext(charlieUid).firestore();
  const ts = pastDate(5000);

  const roomRef = aliceDb.collection("rooms").doc("room_seats_tx");
  const setupBatch = aliceDb.batch();
  setupBatch.set(aliceDb.collection("room_creation_locks").doc(ALICE_UID), {
    userId: ALICE_UID,
    roomId: "room_seats_tx",
    createdAt: ts,
  });
  setupBatch.set(roomRef, {
    creatorId: ALICE_UID,
    hostId: ALICE_UID,
    hostName: "Alice",
    hostAvatarUrl: "",
    title: "Seat Governance Stage",
    announcement: "Ask to speak!",
    category: "Music",
    coverKey: "party_stage",
    tags: ["Music"],
    listenerCount: 2,
    activeSpeakersCount: 1,
    locked: true,
    hasPassword: false,
    passcodeHash: "",
    status: "LIVE",
    featured: false,
    createdAt: ts,
    updatedAt: ts,
  });
  setupBatch.set(roomRef.collection("seats").doc("0"), {
    seatIndex: 0,
    role: "HOST",
    occupantId: ALICE_UID,
    occupantName: "Alice",
    occupantAvatarUrl: "",
    occupantVipLevel: 0,
    muted: false,
    seatLocked: false,
    speaking: false,
    updatedAt: ts,
  });
  for (let i = 1; i <= 8; i++) {
    setupBatch.set(roomRef.collection("seats").doc(String(i)), {
      seatIndex: i,
      role: "SPEAKER",
      occupantId: "",
      occupantName: "",
      occupantAvatarUrl: "",
      occupantVipLevel: 0,
      muted: true,
      seatLocked: false,
      speaking: false,
      updatedAt: ts,
    });
  }
  await assertSucceeds(setupBatch.commit());

  // 1. Listener Bob sends Request to Speak with preferredSeatIndex = 3
  const bobReqRef = bobDb.collection("rooms").doc("room_seats_tx").collection("speaker_requests").doc(BOB_UID);
  await assertSucceeds(
    bobReqRef.set({
      userId: BOB_UID,
      hostId: ALICE_UID,
      username: "Bob",
      avatarUrl: "",
      vipLevel: 0,
      preferredSeatIndex: 3,
      status: "PENDING",
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );

  // 2. Prevent duplicate request documents under another ID
  await assertFails(
    bobDb.collection("rooms").doc("room_seats_tx").collection("speaker_requests").doc("dup_req_id").set({
      userId: BOB_UID,
      hostId: ALICE_UID,
      username: "Bob",
      avatarUrl: "",
      vipLevel: 0,
      preferredSeatIndex: 4,
      status: "PENDING",
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );

  // 3. Prevent duplicate PENDING->PENDING update by Bob while still pending
  await assertFails(
    bobReqRef.update({
      status: "PENDING",
      preferredSeatIndex: 4,
      updatedAt: pastDate(),
    })
  );

  // 4. Bob cancels his pending request
  await assertSucceeds(bobReqRef.delete());

  // 5. Bob submits a new request, and Host Alice rejects it
  await assertSucceeds(
    bobReqRef.set({
      userId: BOB_UID,
      hostId: ALICE_UID,
      username: "Bob",
      avatarUrl: "",
      vipLevel: 0,
      preferredSeatIndex: 3,
      status: "PENDING",
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );
  await assertSucceeds(
    aliceDb.collection("rooms").doc("room_seats_tx").collection("speaker_requests").doc(BOB_UID).update({
      status: "REJECTED",
      updatedAt: pastDate(),
    })
  );

  // 6. After rejection, Bob can submit a fresh PENDING request for Seat 2
  await assertSucceeds(
    bobReqRef.update({
      status: "PENDING",
      preferredSeatIndex: 2,
      username: "Bob",
      avatarUrl: "",
      vipLevel: 0,
      updatedAt: pastDate(),
    })
  );

  // 7. Host Alice approves Bob's request and assigns Bob to Seat 2
  const approveBatch = aliceDb.batch();
  approveBatch.update(roomRef.collection("seats").doc("2"), {
    occupantId: BOB_UID,
    occupantName: "Bob",
    occupantAvatarUrl: "",
    occupantVipLevel: 0,
    muted: false,
    speaking: false,
    updatedAt: pastDate(),
  });
  approveBatch.update(roomRef.collection("speaker_requests").doc(BOB_UID), {
    status: "APPROVED",
    updatedAt: pastDate(),
  });
  approveBatch.update(roomRef, {
    activeSpeakersCount: 2,
    listenerCount: 1,
    updatedAt: pastDate(),
  });
  await assertSucceeds(approveBatch.commit());

  // 8. Prevent two users occupying Seat 2 (even Host cannot overwrite occupied Seat 2 without vacating first)
  await assertFails(
    charlieDb.collection("rooms").doc("room_seats_tx").collection("seats").doc("2").update({
      occupantId: charlieUid,
      occupantName: "Charlie",
      occupantAvatarUrl: "",
      occupantVipLevel: 0,
      muted: false,
      speaking: false,
      updatedAt: pastDate(),
    })
  );
  await assertFails(
    roomRef.collection("seats").doc("2").update({
      occupantId: charlieUid,
      occupantName: "Charlie",
      occupantAvatarUrl: "",
      occupantVipLevel: 0,
      muted: false,
      speaking: false,
      updatedAt: pastDate(),
    })
  );

  // 9. Bob vacates Seat 2 -> Seat 2 becomes empty and reusable
  await assertSucceeds(
    bobDb.collection("rooms").doc("room_seats_tx").collection("seats").doc("2").update({
      occupantId: "",
      occupantName: "",
      occupantAvatarUrl: "",
      occupantVipLevel: 0,
      muted: true,
      speaking: false,
      updatedAt: pastDate(),
    })
  );

  // 10. Host assigns Charlie into newly available Seat 2
  await assertSucceeds(
    roomRef.collection("seats").doc("2").update({
      occupantId: charlieUid,
      occupantName: "Charlie",
      occupantAvatarUrl: "",
      occupantVipLevel: 0,
      muted: false,
      speaking: false,
      updatedAt: pastDate(),
    })
  );
});

test("Exact age, pinned chats, cross-user followers/following list reads, room owner exit with active admin vs no active admin, and group chats", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieUid = "charlie_uid_3";
  const charlieDb = testEnv.authenticatedContext(charlieUid).firestore();
  const ts = pastDate(5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      ...makeUserPayload(ALICE_UID, "Alice", "VP-100001"),
      age: 22,
      gender: "FEMALE",
    })
  );
  await assertSucceeds(
    bobDb.collection("users").doc(BOB_UID).set({
      ...makeUserPayload(BOB_UID, "Bob", "VP-200002"),
      age: 25,
      gender: "MALE",
    })
  );

  // Follow edge + cross-user followers/following read
  await assertSucceeds(
    bobDb.collection("users").doc(BOB_UID).collection("following").doc(ALICE_UID).set({
      peerId: ALICE_UID,
      peerName: "Alice",
      peerAvatarUrl: "",
      peerGender: "FEMALE",
      createdAt: pastDate(),
    })
  );
  await assertSucceeds(
    bobDb.collection("users").doc(ALICE_UID).collection("followers").doc(BOB_UID).set({
      peerId: BOB_UID,
      peerName: "Bob",
      peerAvatarUrl: "",
      peerGender: "MALE",
      createdAt: pastDate(),
    })
  );
  await assertSucceeds(
    charlieDb.collection("users").doc(ALICE_UID).collection("followers").get()
  );

  // Pinning conversations in private account
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("private").doc("account").set({
      userId: ALICE_UID,
      email: "alice@example.com",
      blockedUserIds: [],
      pinnedConversationIds: ["grp_1"],
      updatedAt: pastDate(),
    })
  );

  // Group chat creation, messaging, member addition/removal
  const groupRef = aliceDb.collection("conversations").doc("grp_1");
  await assertSucceeds(
    groupRef.set({
      conversationType: "GROUP",
      groupName: "Late Night Music",
      groupAvatarUrl: "",
      ownerId: ALICE_UID,
      adminIds: [ALICE_UID],
      participantIds: [ALICE_UID, BOB_UID],
      participantNames: { [ALICE_UID]: "Alice", [BOB_UID]: "Bob" },
      participantAvatars: { [ALICE_UID]: "", [BOB_UID]: "" },
      lastMessage: "Group created",
      lastSenderId: ALICE_UID,
      unreadBy: [BOB_UID],
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    bobDb.collection("conversations").doc("grp_1").collection("messages").doc("gm_1").set({
      participantIds: [ALICE_UID, BOB_UID],
      senderId: BOB_UID,
      senderName: "Bob",
      senderAvatarUrl: "",
      senderGender: "MALE",
      type: "TEXT",
      text: "Hey team!",
      imageUri: "",
      read: false,
      createdAt: pastDate(),
    })
  );
  // Charlie cannot read messages before joining
  await assertFails(charlieDb.collection("conversations").doc("grp_1").collection("messages").get());
  // Owner adds Charlie
  await assertSucceeds(
    groupRef.update({
      participantIds: [ALICE_UID, BOB_UID, charlieUid],
      participantNames: { [ALICE_UID]: "Alice", [BOB_UID]: "Bob", [charlieUid]: "Charlie" },
      participantAvatars: { [ALICE_UID]: "", [BOB_UID]: "", [charlieUid]: "" },
      lastMessage: "Charlie joined",
      lastSenderId: ALICE_UID,
      unreadBy: [BOB_UID, charlieUid],
      updatedAt: pastDate(),
    })
  );
  await assertSucceeds(charlieDb.collection("conversations").doc("grp_1").collection("messages").get());
  // Charlie leaves group
  await assertSucceeds(
    charlieDb.collection("conversations").doc("grp_1").update({
      participantIds: [ALICE_UID, BOB_UID],
      participantNames: { [ALICE_UID]: "Alice", [BOB_UID]: "Bob" },
      participantAvatars: { [ALICE_UID]: "", [BOB_UID]: "" },
      adminIds: [ALICE_UID],
      lastMessage: "Charlie left the group",
      lastSenderId: charlieUid,
      unreadBy: [ALICE_UID, BOB_UID],
      updatedAt: pastDate(),
    })
  );

  // Room owner exit with active admin keeps room LIVE & hides host seat avatar while keeping name
  const roomRef = aliceDb.collection("rooms").doc("room_admin_test");
  const roomBatch = aliceDb.batch();
  roomBatch.set(aliceDb.collection("room_creation_locks").doc(ALICE_UID), {
    userId: ALICE_UID,
    roomId: "room_admin_test",
    createdAt: ts,
  });
  roomBatch.update(aliceDb.collection("users").doc(ALICE_UID), {
    hasCreatedRoom: true,
    createdRoomId: "room_admin_test",
    updatedAt: ts,
  });
  roomBatch.set(roomRef, {
    creatorId: ALICE_UID,
    hostId: ALICE_UID,
    hostName: "Alice",
    hostAvatarUrl: "https://example.com/alice.png",
    ownerId: ALICE_UID,
    ownerName: "Alice",
    ownerOnline: true,
    adminIds: [BOB_UID],
    activeAdminIds: [BOB_UID],
    title: "Admin Continuity Room",
    announcement: "",
    category: "Music",
    coverKey: "chill_lounge",
    tags: ["Music"],
    listenerCount: 1,
    activeSpeakersCount: 1,
    locked: false,
    hasPassword: false,
    passcodeHash: "",
    status: "LIVE",
    featured: false,
    createdAt: ts,
    updatedAt: ts,
  });
  roomBatch.set(roomRef.collection("seats").doc("0"), {
    seatIndex: 0,
    role: "HOST",
    occupantId: ALICE_UID,
    occupantName: "Alice",
    occupantAvatarUrl: "https://example.com/alice.png",
    occupantVipLevel: 0,
    occupantGender: "FEMALE",
    muted: false,
    seatLocked: false,
    speaking: false,
    updatedAt: ts,
  });
  roomBatch.set(roomRef.collection("seats").doc("1"), {
    seatIndex: 1,
    role: "SPEAKER",
    occupantId: "",
    occupantName: "",
    occupantAvatarUrl: "",
    occupantVipLevel: 0,
    occupantGender: "UNSPECIFIED",
    muted: true,
    seatLocked: false,
    speaking: false,
    updatedAt: ts,
  });
  await assertSucceeds(roomBatch.commit());

  // Owner exits while Bob (active admin) stays -> Room stays LIVE, Seat 0 keeps ownerName, clears avatar
  const ownerExitBatch = aliceDb.batch();
  const exitTs = pastDate();
  ownerExitBatch.update(roomRef, {
    ownerOnline: false,
    status: "LIVE",
    updatedAt: exitTs,
  });
  ownerExitBatch.update(roomRef.collection("seats").doc("0"), {
    occupantId: ALICE_UID,
    occupantName: "Alice",
    occupantAvatarUrl: "",
    muted: true,
    speaking: false,
    updatedAt: exitTs,
  });
  await assertSucceeds(ownerExitBatch.commit());

  // Active Admin Bob manages seat 1 even while owner is offline
  await assertSucceeds(
    bobDb.collection("rooms").doc("room_admin_test").collection("seats").doc("1").update({
      seatLocked: true,
      updatedAt: pastDate(),
    })
  );

  // Last active admin Bob leaves when owner is offline -> permanent room stays LIVE and joinable
  await assertSucceeds(
    bobDb.collection("rooms").doc("room_admin_test").update({
      activeAdminIds: [],
      listenerCount: 0,
      status: "LIVE",
      updatedAt: pastDate(),
    })
  );
});

test("One permanent room per user: enforces lifetime lock across close, delete, transfer, reset attempts, and legacy backfill", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const adminDb = testEnv
    .authenticatedContext(ADMIN_UID, { email: ADMIN_EMAIL, email_verified: true })
    .firestore();
  const ts = pastDate(5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(makeUserPayload(ALICE_UID, "Alice", "VP-100001"))
  );
  await assertSucceeds(
    bobDb.collection("users").doc(BOB_UID).set(makeUserPayload(BOB_UID, "Bob", "VP-200002"))
  );

  // 1. Attempting to create a room without claiming room_creation_locks MUST fail
  await assertFails(
    aliceDb.collection("rooms").doc("room_no_lock").set({
      creatorId: ALICE_UID,
      hostId: ALICE_UID,
      hostName: "Alice",
      ownerId: ALICE_UID,
      ownerName: "Alice",
      title: "Unlocked Attempt",
      category: "Music",
      locked: false,
      hasPassword: false,
      status: "LIVE",
      featured: false,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // 2. Atomic first room creation succeeds
  const room1Ref = aliceDb.collection("rooms").doc("room_first");
  const lock1Ref = aliceDb.collection("room_creation_locks").doc(ALICE_UID);
  const firstBatch = aliceDb.batch();
  firstBatch.set(lock1Ref, {
    userId: ALICE_UID,
    roomId: "room_first",
    createdAt: ts,
  });
  firstBatch.update(aliceDb.collection("users").doc(ALICE_UID), {
    hasCreatedRoom: true,
    createdRoomId: "room_first",
    updatedAt: ts,
  });
  firstBatch.set(room1Ref, {
    creatorId: ALICE_UID,
    hostId: ALICE_UID,
    hostName: "Alice",
    ownerId: ALICE_UID,
    ownerName: "Alice",
    ownerOnline: true,
    title: "Alice Permanent Stage",
    category: "Music",
    locked: false,
    hasPassword: false,
    status: "LIVE",
    featured: false,
    createdAt: ts,
    updatedAt: ts,
  });
  await assertSucceeds(firstBatch.commit());

  // 3. Second room creation while room_first is LIVE MUST fail
  const attemptSecond = async (roomId) => {
    const b = aliceDb.batch();
    b.set(lock1Ref, { userId: ALICE_UID, roomId, createdAt: pastDate() });
    b.set(aliceDb.collection("rooms").doc(roomId), {
      creatorId: ALICE_UID,
      hostId: ALICE_UID,
      hostName: "Alice",
      ownerId: ALICE_UID,
      ownerName: "Alice",
      ownerOnline: true,
      title: "Second Stage Attempt",
      category: "Chill",
      locked: false,
      hasPassword: false,
      status: "LIVE",
      featured: false,
      createdAt: pastDate(),
      updatedAt: pastDate(),
    });
    return b.commit();
  };
  await assertFails(attemptSecond("room_second_live"));

  // 4. Transfer ownership to Bob -> Alice STILL cannot create another room
  await assertSucceeds(
    room1Ref.update({
      hostId: BOB_UID,
      hostName: "Bob",
      ownerId: BOB_UID,
      ownerName: "Bob",
      updatedAt: pastDate(),
    })
  );
  await assertFails(attemptSecond("room_after_transfer"));

  // 5. Close & Delete room_first (by new owner Bob) -> Alice STILL cannot create another room
  await assertSucceeds(
    bobDb.collection("rooms").doc("room_first").update({
      status: "ENDED",
      updatedAt: pastDate(),
    })
  );
  await assertFails(attemptSecond("room_after_end"));
  await assertSucceeds(bobDb.collection("rooms").doc("room_first").delete());
  await assertFails(attemptSecond("room_after_delete"));

  // 6. Lock document and user flag cannot be deleted or reset (not even by admin)
  await assertFails(lock1Ref.delete());
  await assertFails(lock1Ref.update({ roomId: "another_room" }));
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      hasCreatedRoom: false,
      createdRoomId: "",
      updatedAt: pastDate(),
    })
  );
  await assertFails(
    adminDb.collection("users").doc(ALICE_UID).update({
      hasCreatedRoom: false,
      updatedAt: pastDate(),
    })
  );

  // 7. Existing user with legacy room prior to lock collection can backfill once, then is locked permanently
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("rooms").doc("room_legacy_bob").set({
      hostId: BOB_UID,
      hostName: "Bob",
      ownerId: BOB_UID,
      ownerName: "Bob",
      title: "Bob Legacy Room",
      category: "Chill",
      locked: false,
      hasPassword: false,
      status: "ENDED",
      featured: false,
      createdAt: ts,
      updatedAt: ts,
    });
  });

  const bobLockRef = bobDb.collection("room_creation_locks").doc(BOB_UID);
  const bobBackfill = bobDb.batch();
  bobBackfill.set(bobLockRef, {
    userId: BOB_UID,
    roomId: "room_legacy_bob",
    createdAt: pastDate(),
  });
  bobBackfill.update(bobDb.collection("users").doc(BOB_UID), {
    hasCreatedRoom: true,
    createdRoomId: "room_legacy_bob",
    updatedAt: pastDate(),
  });
  await assertSucceeds(bobBackfill.commit());

  // After backfill, Bob cannot create a new room
  const bobNewRoom = bobDb.batch();
  bobNewRoom.set(bobDb.collection("rooms").doc("room_bob_second"), {
    creatorId: BOB_UID,
    hostId: BOB_UID,
    hostName: "Bob",
    ownerId: BOB_UID,
    ownerName: "Bob",
    title: "Bob Second Attempt",
    category: "Music",
    locked: false,
    hasPassword: false,
    status: "LIVE",
    featured: false,
    createdAt: pastDate(),
    updatedAt: pastDate(),
  });
  await assertFails(bobNewRoom.commit());
});


