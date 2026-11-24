# VibePlay Music Library Worker

Separate R2 music-file service. It does not connect to or modify Agora.

## Features
- Firebase ID-token verification for every library request.
- Central R2 library; clients fetch the same track independently.
- MP3, M4A, AAC, OGG, WAV and FLAC uploads, up to 15 MB per file.
- HTTP byte-range responses for audio seeking.
- Users can delete only tracks they uploaded.
- Conservative 9.5 GiB storage cap to leave headroom below R2's 10 GB free storage allowance.

## Cost warning
R2 is quota-based, not unlimited. The Worker enforces a storage cap, but cannot guarantee monthly Class A or Class B request allowances will never be exceeded. Do not enable paid billing or upgrade just to deploy this. Stop if Cloudflare requires paid activation you do not want.

## Setup
1. In Cloudflare, open R2 Object Storage and create a bucket named vibeplay-music.
2. In this folder, install Wrangler: npm install --save-dev wrangler
3. Authenticate: npx wrangler login
4. Deploy: npx wrangler deploy
5. Copy the deployed workers.dev URL. The health check is /health.
6. Never put R2 access keys or secrets in the Android app.

## API
- GET /v1/music — list tracks (Firebase bearer token required)
- POST /v1/music — multipart fields file, optional title, optional artist
- GET /v1/music/{key} — stream a track, supports Range
- DELETE /v1/music/{key} — uploader only

Upload only audio you own or have permission to distribute. The Android app still needs client integration and room playback-sync implementation before this service is usable inside VibePlay.
