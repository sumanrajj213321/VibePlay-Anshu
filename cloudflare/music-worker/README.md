# VibePlay Music Library Worker (DBByte)

Separate music-file API using DBByte object storage. It does not connect to or modify Agora.

## Free-tier guardrails
- Bucket: `vibeplay-music`
- Worker storage ceiling: 4.5 GiB, leaving headroom below the advertised 5 GB bucket limit.
- Maximum upload: 15 MB per file.
- The Worker never enables billing or upgrades a plan.
- DBByte's current docs show a free allowance of 5 GB storage, 100 GB/month bandwidth and 10,000 requests/month. Limits can refuse further requests; monitor the dashboard and stop uploads before approaching limits. No service is unlimited.
- Never place the DBByte API key in Android or commit it to GitHub. Store it as a Worker secret named `DBBYTE_API_KEY`.

## Required DBByte API key
Create a bucket-scoped API key for `vibeplay-music` with only:
- read
- write
- delete

The key is used only by this server-side Worker. Do not paste the key into chat or GitHub.

## Deploy
1. Create the DBByte bucket named `vibeplay-music`.
2. Create the bucket-scoped API key described above.
3. In Cloudflare Workers, deploy this folder with Wrangler.
4. Set the secret: `npx wrangler secret put DBBYTE_API_KEY` and paste the key into the local terminal prompt only.
5. The Worker health endpoint is `/health`.

Cloudflare Workers has a free usage tier, but check the dashboard and do not add a payment method or enable billing. If Cloudflare blocks deployment until billing is enabled, stop; do not upgrade.

## API
All library endpoints except health require a valid Firebase ID token:
- GET `/v1/music` — list tracks
- POST `/v1/music` — multipart fields `file`, optional `title`, optional `artist`
- GET `/v1/music/{name}` — stream a track; forwards byte-range requests
- DELETE `/v1/music/{name}` — uploader only

Upload only audio you own or have permission to distribute.

## Remaining app work
The Android client and Firestore room playback state still need integration and testing. Do not claim the feature is live until the Worker is deployed, app integration is committed, and multi-device playback has been tested.
