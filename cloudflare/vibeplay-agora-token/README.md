# VibePlay Agora token Worker

Firebase-authenticated token endpoint for VibePlay voice rooms.

## Security
- Verifies Firebase ID tokens with Google's Secure Token JWKS, issuer and audience.
- Reads the LIVE room, caller's room session and seat documents using the caller's Firebase ID token. Firestore Security Rules are enforced.
- Never trusts a client-supplied UID or speaker flag.
- Grants audio-publish privilege only to the authenticated occupant of an unmuted, unlocked seat 0..8.
- App Certificate is a Cloudflare secret and must never be committed.

## Required Cloudflare secret
Set `AGORA_APP_CERTIFICATE` to the Agora Primary Certificate using Worker Settings > Variables and Secrets. Keep it as a Secret. Do not add it to wrangler.toml.

## Deploy from this directory
`npm install`
`npx wrangler login`
`npm run deploy`

If deploying to the already-created Worker, preserve the name `vibeplay-agora-token`. Ensure the secret is configured on that Worker.

## Request
POST `{"roomId":"<room-id>"}` with `Authorization: Bearer <Firebase-ID-token>`.

## Important Agora limitation
Agora's publish-audio token privilege is enforced only when co-host authentication is enabled for the Agora project. Without that project setting, a listener token may not prevent publishing. Also, an already-issued speaker publish privilege can remain valid for up to 5 minutes; the Android client must immediately mute/unpublish when Firestore moderation changes arrive.
