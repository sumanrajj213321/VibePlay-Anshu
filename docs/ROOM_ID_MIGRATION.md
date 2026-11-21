# VibePlay six-digit Room ID migration

This migration adds a separate `publicRoomId` field to every existing room and reserves each code in `room_public_ids`. It does **not** rename Firestore room documents or move/delete their messages, seats, listeners, gifts, or sessions.

## Run safely

1. Use a trusted machine with Node.js 20+ and access to the Firebase project `vibeplay-anshu`.
2. Authenticate with Application Default Credentials using a project administrator/service account that has Firestore read/write access. Do not commit or share service-account JSON files.
3. From the repository root run:

   ```sh
   npm install
   npm run migrate:room-ids
   ```

   This is a dry run and writes nothing. Review the proposed room-to-code assignments.
4. If the plan is correct, run:

   ```sh
   npm run migrate:room-ids -- --apply
   ```

The script stops on duplicate existing codes or reservation conflicts rather than silently changing a room's existing valid code. It can be run again safely; already migrated rooms are skipped.

The Android app now displays `publicRoomId` (falling back to the Firestore document ID until migration is run), and new rooms reserve their six-digit code during creation.
