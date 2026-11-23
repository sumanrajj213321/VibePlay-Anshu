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

## Deployment health check
Open `https://vibeplay-agora-token.sumanrajj213321.workers.dev/health`. A deployed Worker returns `{"ok":true,"service":"vibeplay-agora-token","version":1}`.

## Request
POST `{"roomId":"<room-id>"}` with `Authorization: Bearer <Firebase-ID-token>`.

## Important Agora limitation
Agora's publish-audio token privilege is enforced only when co-host authentication is enabled for the Agora project. Without that project setting, a listener token may not prevent publishing. An issued speaker publish privilege can remain valid until its one-hour expiry. The Android client mutes locally as soon as Firestore moderation changes arrive and requests a refreshed token to rejoin with the updated role.

## GitHub Actions deployment setup (one-time)

This repository includes a manual workflow at **Actions → Deploy Agora Token Worker**.
To authorize it, add two repository Actions secrets (never paste either value into chat or commit them):

1. In Cloudflare, open **Manage Account → API Tokens → Create Token**. Choose the **Edit Cloudflare Workers** template and restrict it to the account containing the existing `vibeplay-agora-token` Worker. The token must be able to edit/deploy that existing Worker.
2. In GitHub, open **VibePlay-Anshu → Settings → Secrets and variables → Actions → New repository secret**.
3. Add `CLOUDFLARE_API_TOKEN` with the token value.
4. Add `CLOUDFLARE_ACCOUNT_ID` with the Cloudflare account ID shown on the account overview page.
5. In GitHub **Actions**, select **Deploy Agora Token Worker → Run workflow**. The workflow deploys the checked-in Worker source and checks `/health`.

The existing `AGORA_APP_CERTIFICATE` must remain configured as a Worker Secret in Cloudflare. Do not put it in GitHub, this repository, or workflow logs. The deployment workflow does not upload or print the certificate.

Cloudflare's documented CI deployment requirements: https://developers.cloudflare.com/workers/ci-cd/external-cicd/github-actions/
