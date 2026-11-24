export interface Env {
  FIREBASE_PROJECT_ID: string;
  DBBYTE_API_KEY: string;
  DBBYTE_BUCKET: string;
  MAX_STORAGE_BYTES?: string;
  MAX_UPLOAD_BYTES?: string;
  ALLOWED_ORIGINS?: string;
}

const DEFAULT_MAX_STORAGE = 4.5 * 1024 * 1024 * 1024;
const DEFAULT_MAX_UPLOAD = 15 * 1024 * 1024;
const TYPES = new Set(["audio/mpeg","audio/mp4","audio/aac","audio/ogg","audio/wav","audio/x-wav","audio/wave","audio/vnd.wave","audio/flac","audio/x-flac","audio/x-aac","audio/m4a","audio/x-m4a","audio/opus","audio/x-opus+ogg","application/ogg","video/mp4","application/mp4","application/octet-stream"]);

function cors(request: Request, env: Env): Headers {
  const origin = request.headers.get("Origin") || "";
  const allowed = (env.ALLOWED_ORIGINS || "*").split(",").map(v => v.trim());
  const h = new Headers();
  h.set("Access-Control-Allow-Origin", allowed.includes("*") ? "*" : allowed.includes(origin) ? origin : "null");
  h.set("Access-Control-Allow-Methods", "GET, POST, DELETE, OPTIONS");
  h.set("Access-Control-Allow-Headers", "Authorization, Content-Type, Range");
  h.set("Access-Control-Expose-Headers", "Content-Length, Content-Range, Accept-Ranges, ETag");
  h.set("Vary", "Origin");
  return h;
}
function reply(data: unknown, status: number, h: Headers): Response {
  const headers = new Headers(h);
  headers.set("Content-Type", "application/json; charset=utf-8");
  headers.set("Cache-Control", "no-store");
  return new Response(JSON.stringify(data), { status, headers });
}
function b64(value: string): Uint8Array {
  const s = value.replace(/-/g, "+").replace(/_/g, "/");
  const raw = atob(s.padEnd(Math.ceil(s.length / 4) * 4, "="));
  return Uint8Array.from(raw, c => c.charCodeAt(0));
}
async function verify(request: Request, env: Env): Promise<string> {
  const auth = request.headers.get("Authorization") || "";
  const token = auth.startsWith("Bearer ") ? auth.slice(7) : "";
  const p = token.split(".");
  if (p.length !== 3) throw new Error("Sign in is required.");
  const head = JSON.parse(new TextDecoder().decode(b64(p[0])));
  const claims = JSON.parse(new TextDecoder().decode(b64(p[1])));
  const now = Math.floor(Date.now() / 1000), project = env.FIREBASE_PROJECT_ID;
  if (head.alg !== "RS256" || !head.kid || claims.aud !== project ||
      claims.iss !== "https://securetoken.google.com/" + project ||
      typeof claims.sub !== "string" || !claims.sub ||
      typeof claims.exp !== "number" || claims.exp <= now ||
      typeof claims.iat !== "number" || claims.iat > now + 60) {
    throw new Error("Invalid or expired sign-in token.");
  }
  const response = await fetch("https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com", {
    cf: { cacheTtl: 3600, cacheEverything: true }
  });
  if (!response.ok) throw new Error("Could not verify sign-in.");
  const jwks = await response.json() as { keys: (JsonWebKey & { kid?: string })[] };
  const jwk = jwks.keys.find(k => k.kid === head.kid);
  if (!jwk) throw new Error("Sign-in key not found.");
  const key = await crypto.subtle.importKey("jwk", jwk, { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" }, false, ["verify"]);
  const ok = await crypto.subtle.verify("RSASSA-PKCS1-v1_5", key, b64(p[2]), new TextEncoder().encode(p[0] + "." + p[1]));
  if (!ok) throw new Error("Invalid sign-in token.");
  return claims.sub;
}
function safeName(value: string): string {
  return value.replace(/[^a-zA-Z0-9._-]/g, "_").slice(0, 100);
}
function encodeName(value: string): string {
  return Array.from(new TextEncoder().encode(value), byte => byte.toString(16).padStart(2, "0")).join("");
}
function decodeName(value: string): string {
  try {
    if (!value || value.length % 2 !== 0 || !/^[0-9a-f]+$/i.test(value)) return value || "Untitled";
    const bytes = new Uint8Array(value.match(/.{2}/g)!.map(pair => parseInt(pair, 16)));
    return new TextDecoder().decode(bytes);
  } catch {
    return value || "Untitled";
  }
}
function apiUrl(env: Env, path: string): string {
  return "https://api.dbbyte.com/storage/" + encodeURIComponent(env.DBBYTE_BUCKET) + path;
}
async function dbbyte(env: Env, path: string, init: RequestInit = {}): Promise<Response> {
  const headers = new Headers(init.headers);
  headers.set("Authorization", "Bearer " + env.DBBYTE_API_KEY);
  return fetch(apiUrl(env, path), { ...init, headers });
}
async function listFiles(env: Env): Promise<any[]> {
  const all: any[] = [];
  let cursor = "";
  do {
    const query = cursor ? "&cursor=" + encodeURIComponent(cursor) : "";
    const response = await dbbyte(env, "/list?limit=1000" + query);
    if (!response.ok) throw new Error("Music library could not be listed (" + response.status + ").");
    const data = await response.json() as { files?: any[]; nextCursor?: string | null; hasMore?: boolean };
    if (Array.isArray(data.files)) all.push(...data.files);
    cursor = data.hasMore && data.nextCursor ? data.nextCursor : "";
  } while (cursor);
  return all;
}
export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    const h = cors(request, env);
    if (request.method === "OPTIONS") return new Response(null, { status: 204, headers: h });
    const url = new URL(request.url);
    if (url.pathname === "/health" && request.method === "GET") return reply({ ok: true, service: "vibeplay-music-dbbyte" }, 200, h);
    try {
      const uid = await verify(request, env);
      if (!env.DBBYTE_API_KEY || !env.DBBYTE_BUCKET) return reply({ error: "Music storage is not configured." }, 503, h);

      if (url.pathname === "/v1/music" && request.method === "GET") {
        const files = await listFiles(env);
        const tracks = files.filter(f => typeof f.name === "string" && f.name.startsWith("music_"))
          .map(f => {
            const parts = f.name.replace(/\.[^.]+$/, "").split("_");
            const extension = f.name.split(".").pop()?.toLowerCase();
            const title = decodeName(parts.slice(4).join("_")) || "Untitled";
            const artist = decodeName(parts[3] || "") || "Unknown artist";
            const contentType = extension === "mp3" ? "audio/mpeg" : extension === "m4a" ? "audio/mp4" :
              extension === "aac" ? "audio/aac" : extension === "ogg" ? "audio/ogg" :
              extension === "flac" ? "audio/flac" : extension === "mp4" ? "audio/mp4" : "audio/wav";
            return { id: f.name, title, artist, size: Number(f.size || 0), contentType, uploadedBy: parts[1] || "" };
          });
        return reply({ tracks, truncated: false }, 200, h);
      }

      if (url.pathname === "/v1/music" && request.method === "POST") {
        const incoming = await request.formData();
        const file = incoming.get("file");
        if (!(file instanceof File)) return reply({ error: "Choose an audio file." }, 400, h);
        const reportedType = (file.type || "").toLowerCase().split(";")[0].trim();
        const extension = (file.name.split(".").pop() || "").toLowerCase();
        const extensionType = extension === "mp3" ? "audio/mpeg" :
          extension === "m4a" || extension === "mp4" ? "audio/mp4" :
          extension === "aac" ? "audio/aac" :
          ["ogg", "oga", "opus"].includes(extension) ? "audio/ogg" :
          extension === "flac" ? "audio/flac" : extension === "wav" ? "audio/wav" : "";
        const aliases: Record<string, string> = {
          "video/mp4": "audio/mp4", "application/mp4": "audio/mp4",
          "audio/m4a": "audio/mp4", "audio/x-m4a": "audio/mp4",
          "audio/x-wav": "audio/wav", "audio/wave": "audio/wav", "audio/vnd.wave": "audio/wav",
          "audio/x-flac": "audio/flac", "audio/x-aac": "audio/aac",
          "application/ogg": "audio/ogg", "audio/opus": "audio/ogg", "audio/x-opus+ogg": "audio/ogg"
        };
        const type = aliases[reportedType] || (reportedType === "application/octet-stream" || !reportedType
          ? extensionType : reportedType);
        if ((reportedType !== "" && !TYPES.has(reportedType)) || !["audio/mpeg","audio/mp4","audio/aac","audio/ogg","audio/wav","audio/flac"].includes(type)) {
          return reply({ error: "Unsupported audio format. Choose MP3, M4A, AAC, OGG, WAV or FLAC." }, 415, h);
        }
        const maxUpload = Number(env.MAX_UPLOAD_BYTES || DEFAULT_MAX_UPLOAD);
        if (file.size <= 0 || file.size > maxUpload) return reply({ error: "Maximum upload size is 15 MB." }, 413, h);
        const files = await listFiles(env);
        const used = files.reduce((sum, f) => sum + Number(f.size || 0), 0);
        if (used + file.size > Number(env.MAX_STORAGE_BYTES || DEFAULT_MAX_STORAGE)) {
          return reply({ error: "Music storage is full. Remove a track before uploading." }, 507, h);
        }
        const ext = type === "audio/mpeg" ? "mp3" : type === "audio/mp4" ? (extension === "mp4" ? "mp4" : "m4a") :
          type === "audio/aac" ? "aac" : type === "audio/ogg" ? (["oga","opus"].includes(extension) ? extension : "ogg") :
          type === "audio/flac" ? "flac" : "wav";
        const title = String(incoming.get("title") || file.name).trim().slice(0, 120) || "Untitled";
        const artist = String(incoming.get("artist") || "Unknown artist").trim().slice(0, 80) || "Unknown artist";
        const storedName = "music_" + uid.slice(0, 32) + "_" + crypto.randomUUID() + "_" + encodeName(artist) + "_" + encodeName(title) + "." + ext;
        const uploadFile = new File([file], storedName, { type });
        const body = new FormData();
        body.set("file", uploadFile);
        const uploaded = await dbbyte(env, "/upload", { method: "POST", body });
        const result = await uploaded.json().catch(() => ({})) as any;
        if (!uploaded.ok) return reply({ error: result.error || "Upload failed (" + uploaded.status + ")." }, uploaded.status, h);
        return reply({
          id: result.name || storedName,
          title: title,
          artist,
          size: Number(result.size || file.size),
          contentType: result.mimeType || type,
          uploadedBy: uid
        }, 201, h);
      }

      if (url.pathname.startsWith("/v1/music/") && request.method === "GET") {
        const name = decodeURIComponent(url.pathname.slice("/v1/music/".length));
        if (!/^music_[A-Za-z0-9_-]{1,32}_[A-Fa-f0-9-]{36}_[A-Za-z0-9._-]+_[A-Za-z0-9._-]+\.(mp3|m4a|mp4|aac|ogg|oga|opus|flac|wav)$/i.test(name) || name.includes("..")) return reply({ error: "Invalid track." }, 400, h);
        const response = await dbbyte(env, "/download/" + encodeURIComponent(name), {
          method: "GET",
          headers: request.headers.has("Range") ? { Range: request.headers.get("Range")! } : {}
        });
        const headers = new Headers(h);
        for (const key of ["Content-Type", "Content-Length", "Content-Range", "Accept-Ranges", "ETag"]) {
          const value = response.headers.get(key);
          if (value) headers.set(key, value);
        }
        headers.set("Cache-Control", "private, no-store");
        return new Response(response.body, { status: response.status, headers });
      }

      if (url.pathname.startsWith("/v1/music/") && request.method === "DELETE") {
        const name = decodeURIComponent(url.pathname.slice("/v1/music/".length));
        if (!name.startsWith("music_" + uid.slice(0, 32) + "_") || name.includes("..") || name.includes("/")) {
          return reply({ error: "Only the uploader can remove this track." }, 403, h);
        }
        const deleted = await dbbyte(env, "/delete/" + encodeURIComponent(name), { method: "DELETE" });
        const result = await deleted.json().catch(() => ({})) as any;
        return reply(deleted.ok ? { ok: true } : { error: result.error || "Delete failed." }, deleted.status, h);
      }
      return reply({ error: "Not found." }, 404, h);
    } catch (e) {
      const message = e instanceof Error ? e.message : "Request failed.";
      const authError = message.toLowerCase().includes("sign-in") || message.toLowerCase().includes("token") || message.toLowerCase().includes("key");
      return reply({ error: message }, authError ? 401 : 500, h);
    }
  }
} satisfies ExportedHandler<Env>;
