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

## GitHub Actions deployment (no billing)

A manual deployment workflow is available at **Actions → Deploy VibePlay Music Worker → Run workflow**. Before running it, add these repository Actions secrets under **Settings → Secrets and variables → Actions**:

- `CLOUDFLARE_API_TOKEN` — a Cloudflare API token limited to Workers Scripts edit for this account.
- `CLOUDFLARE_ACCOUNT_ID` — the Cloudflare account ID.
- `DBBYTE_API_KEY` — the bucket-scoped key for `vibeplay-music`.

The workflow deploys this Worker and sets the DBByte key as a Worker secret. It does not add a payment method, upgrade a plan, or enable billing. If Cloudflare requires billing/payment to proceed, stop and do not enable it.

After deployment, set the repository Actions secret `MUSIC_API_BASE_URL` to the exact HTTPS Worker URL shown by Wrangler (for example, the workers.dev URL). The APK workflow passes this value into the app. Do not put the DBByte key in `MUSIC_API_BASE_URL`, Android source, or the app.

## DBByte API behavior
The Worker uses DBByte's documented REST object routes: multipart `POST /storage/{bucket}/upload`, paginated `GET /storage/{bucket}/list`, `GET /storage/{bucket}/download/{name}`, and `DELETE /storage/{bucket}/delete/{name}`. DBByte returns a storage-generated `key` and the uploaded filename as `name`; the Worker stores and addresses tracks by that returned filename. Listing is paginated and filters the filename prefix in the Worker because DBByte's list prefix filter applies to its generated key, not the uploaded filename.

## API
All library endpoints except health require a valid Firebase ID token:
- GET `/v1/music` — list tracks
- POST `/v1/music` — multipart fields `file`, optional `title`, optional `artist`
- GET `/v1/music/{name}` — stream a track; forwards byte-range requests
- DELETE `/v1/music/{name}` — uploader only

Upload only audio you own or have permission to distribute.

## Remaining app work
The Android client and Firestore room playback state still need integration and testing. Do not claim the feature is live until the Worker is deployed, app integration is committed, and multi-device playback has been tested.
