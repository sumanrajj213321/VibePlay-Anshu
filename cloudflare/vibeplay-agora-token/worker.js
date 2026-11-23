import { jwtVerify, createRemoteJWKSet } from "jose";
import { RtcTokenBuilder } from "agora-token";

const PROJECT_ID = "vibeplay-anshu";
const DATABASE_ID = "(default)";
const FIREBASE_JWKS = createRemoteJWKSet(
  new URL("https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com")
);
const JOIN_TOKEN_TTL = 3600;
const AUDIO_PUBLISH_TTL = JOIN_TOKEN_TTL;

function json(data, status = 200) {
  return new Response(JSON.stringify(data), {
    status,
    headers: {
      "content-type": "application/json; charset=utf-8",
      "cache-control": "no-store",
      "access-control-allow-origin": "*",
      "access-control-allow-methods": "POST, OPTIONS",
      "access-control-allow-headers": "authorization, content-type"
    }
  });
}
function getField(doc, name) { return doc?.fields?.[name]; }
function stringField(doc, name) { return getField(doc, name)?.stringValue ?? ""; }
function boolField(doc, name, fallback = false) {
  const value = getField(doc, name)?.booleanValue;
  return typeof value === "boolean" ? value : fallback;
}
async function firestoreGet(path, idToken) {
  const url = "https://firestore.googleapis.com/v1/projects/" + PROJECT_ID +
    "/databases/" + encodeURIComponent(DATABASE_ID) + "/documents/" +
    path.split("/").map(encodeURIComponent).join("/");
  const response = await fetch(url, { headers: { authorization: "Bearer " + idToken } });
  if (response.status === 404) return null;
  if (!response.ok) throw new Error("Firestore read failed: " + response.status);
  return response.json();
}
async function authenticate(request) {
  const header = request.headers.get("authorization") || "";
  if (!header.startsWith("Bearer ")) throw new Response("Authentication required", { status: 401 });
  const idToken = header.slice(7).trim();
  if (!idToken || idToken.length > 8192) throw new Response("Invalid authentication token", { status: 401 });
  const { payload } = await jwtVerify(idToken, FIREBASE_JWKS, {
    algorithms: ["RS256"],
    issuer: "https://securetoken.google.com/" + PROJECT_ID,
    audience: PROJECT_ID
  });
  const now = Math.floor(Date.now() / 1000);
  if (typeof payload.sub !== "string" || !payload.sub ||
      typeof payload.iat !== "number" || payload.iat > now + 60 ||
      typeof payload.auth_time !== "number" || payload.auth_time > now + 60) {
    throw new Response("Invalid Firebase identity", { status: 401 });
  }
  return { uid: payload.sub, idToken };
}
async function numericAgoraUid(firebaseUid) {
  const digest = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(firebaseUid));
  return new DataView(digest).getUint32(0, false) || 1;
}
export default {
  async fetch(request, env) {
    if (request.method === "OPTIONS") return json({ ok: true });
    if (request.method !== "POST") return json({ error: "Method not allowed" }, 405);
    try {
      if (!env.AGORA_APP_ID || !env.AGORA_APP_CERTIFICATE) {
        return json({ error: "Agora Worker secrets are not configured" }, 503);
      }
      const { uid, idToken } = await authenticate(request);
      let body;
      try { body = await request.json(); } catch { return json({ error: "Expected JSON body" }, 400); }
      const roomId = typeof body?.roomId === "string" ? body.roomId.trim() : "";
      if (!/^[A-Za-z0-9_-]{1,64}$/.test(roomId)) return json({ error: "Invalid roomId" }, 400);

      const roomPath = "rooms/" + roomId;
      const [room, session] = await Promise.all([
        firestoreGet(roomPath, idToken),
        firestoreGet(roomPath + "/room_sessions/" + uid, idToken)
      ]);
      if (!room || stringField(room, "status") !== "LIVE" || !session) {
        return json({ error: "Active membership in a LIVE room is required" }, 403);
      }

      const seats = await Promise.all(Array.from({ length: 9 }, (_, index) =>
        firestoreGet(roomPath + "/seats/" + index, idToken)
      ));
      let seatIndex = -1;
      let muted = true;
      let locked = true;
      for (let index = 0; index < seats.length; index++) {
        if (stringField(seats[index], "occupantId") === uid) {
          seatIndex = index;
          muted = boolField(seats[index], "muted", true);
          locked = boolField(seats[index], "seatLocked", false);
          break;
        }
      }
      const canPublishAudio = seatIndex >= 0 && !muted && !locked;
      const rtcUid = await numericAgoraUid(uid);
      const channelName = "vp_" + roomId;
      const token = RtcTokenBuilder.buildTokenWithUidAndPrivilege(
        env.AGORA_APP_ID, env.AGORA_APP_CERTIFICATE, channelName, rtcUid,
        JOIN_TOKEN_TTL, JOIN_TOKEN_TTL,
        canPublishAudio ? AUDIO_PUBLISH_TTL : 0,
        0, 0
      );
      if (!token) return json({ error: "Agora token generation failed" }, 500);
      return json({
        appId: env.AGORA_APP_ID,
        channelName,
        uid: rtcUid,
        token,
        role: canPublishAudio ? "speaker" : "listener",
        seatIndex: canPublishAudio ? seatIndex : null,
        expiresIn: JOIN_TOKEN_TTL,
        publishPrivilegeExpiresIn: canPublishAudio ? AUDIO_PUBLISH_TTL : 0
      });
    } catch (error) {
      if (error instanceof Response) return json({ error: await error.text() }, error.status);
      console.error("Agora token request failed", error?.message || "unknown error");
      return json({ error: "Unable to authorize voice room access" }, 401);
    }
  }
};
