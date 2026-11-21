#!/usr/bin/env node
"use strict";

const { applicationDefault, initializeApp } = require("firebase-admin/app");
const { FieldPath, getFirestore, Timestamp } = require("firebase-admin/firestore");

const PROJECT_ID = process.env.GCLOUD_PROJECT || "vibeplay-anshu";
const SIX_DIGITS = /^[0-9]{6}$/;
const APPLY = process.argv.includes("--apply");

initializeApp({
  credential: applicationDefault(),
  projectId: PROJECT_ID,
});
const db = getFirestore();

function makeCode() {
  return String(100000 + Math.floor(Math.random() * 900000));
}

async function readAllRooms() {
  const all = [];
  let cursor = null;
  for (;;) {
    let query = db.collection("rooms").orderBy(FieldPath.documentId()).limit(400);
    if (cursor) query = query.startAfter(cursor);
    const page = await query.get();
    if (page.empty) break;
    all.push(...page.docs);
    cursor = page.docs[page.docs.length - 1];
    if (page.size < 400) break;
  }
  return all;
}

async function main() {
  const rooms = await readAllRooms();
  const reserved = new Map();

  // Reserve existing public codes and six-digit room document IDs first.
  for (const doc of rooms) {
    const existing = doc.get("publicRoomId");
    const codes = [];
    if (typeof existing === "string" && SIX_DIGITS.test(existing)) codes.push(existing);
    if (SIX_DIGITS.test(doc.id)) codes.push(doc.id);
    for (const code of new Set(codes)) {
      const owner = reserved.get(code);
      if (owner && owner !== doc.id) {
        throw new Error(`Duplicate six-digit room code ${code}: ${owner} and ${doc.id}. No rooms were changed.`);
      }
      reserved.set(code, doc.id);
    }
  }

  const plan = [];
  for (const doc of rooms) {
    const existing = doc.get("publicRoomId");
    if (typeof existing === "string" && SIX_DIGITS.test(existing)) {
      plan.push({ doc, code: existing, needsUpdate: false });
      continue;
    }
    let code = SIX_DIGITS.test(doc.id) ? doc.id : "";
    if (!code || (reserved.has(code) && reserved.get(code) !== doc.id)) {
      do { code = makeCode(); } while (reserved.has(code));
    }
    reserved.set(code, doc.id);
    plan.push({ doc, code, needsUpdate: true });
  }

  const pending = plan.filter((item) => item.needsUpdate);
  console.log(`Found ${rooms.length} rooms; ${pending.length} need a six-digit public ID.`);
  if (!APPLY) {
    for (const item of pending.slice(0, 30)) console.log(`DRY RUN  ${item.doc.id} -> ${item.code}`);
    if (pending.length > 30) console.log(`... and ${pending.length - 30} more`);
    console.log("No changes made. Re-run with --apply to write the migration.");
    return;
  }

  let migrated = 0;
  for (const item of pending) {
    const roomRef = db.collection("rooms").doc(item.doc.id);
    const reservationRef = db.collection("room_public_ids").doc(item.code);
    await db.runTransaction(async (tx) => {
      const [roomSnap, reservationSnap] = await Promise.all([
        tx.get(roomRef),
        tx.get(reservationRef),
      ]);
      if (!roomSnap.exists) throw new Error(`Room disappeared during migration: ${item.doc.id}`);
      const current = roomSnap.get("publicRoomId");
      if (typeof current === "string" && SIX_DIGITS.test(current)) {
        if (current !== item.code) throw new Error(`Room changed during migration: ${item.doc.id}`);
      } else {
        if (reservationSnap.exists && reservationSnap.get("roomId") !== item.doc.id) {
          throw new Error(`Code collision for ${item.code}; stopped at room ${item.doc.id}.`);
        }
        tx.update(roomRef, { publicRoomId: item.code });
      }
      if (!reservationSnap.exists) {
        tx.create(reservationRef, {
          roomId: item.doc.id,
          createdAt: Timestamp.now(),
        });
      } else if (reservationSnap.get("roomId") !== item.doc.id) {
        throw new Error(`Reservation ${item.code} belongs to another room.`);
      }
    });
    migrated += 1;
    if (migrated % 50 === 0) console.log(`Migrated ${migrated}/${pending.length}`);
  }

  console.log(`Migration complete. Updated ${migrated} rooms; room document IDs and subcollections were preserved.`);
}

main().catch((error) => {
  console.error(error.message || error);
  process.exitCode = 1;
});
