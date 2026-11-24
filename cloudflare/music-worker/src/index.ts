export interface Env {
  MUSIC_BUCKET: R2Bucket;
  FIREBASE_PROJECT_ID: string;
  MAX_STORAGE_BYTES?: string;
  MAX_UPLOAD_BYTES?: string;
  ALLOWED_ORIGINS?: string;
}
const DEFAULT_MAX_STORAGE = 9.5 * 1024 * 1024 * 1024;
const DEFAULT_MAX_UPLOAD = 15 * 1024 * 1024;
const TYPES = new Set(["audio/mpeg","audio/mp4","audio/aac","audio/ogg","audio/wav","audio/x-wav","audio/flac"]);
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
  const headers = new Headers(h); headers.set("Content-Type","application/json; charset=utf-8"); headers.set("Cache-Control","no-store");
  return new Response(JSON.stringify(data), {status,headers});
}
function b64(value: string): Uint8Array {
  const s=value.replace(/-/g,"+").replace(/_/g,"/");
  const raw=atob(s.padEnd(Math.ceil(s.length/4)*4,"="));
  return Uint8Array.from(raw,c=>c.charCodeAt(0));
}
async function verify(request: Request, env: Env): Promise<string> {
  const auth=request.headers.get("Authorization")||"", token=auth.startsWith("Bearer ")?auth.slice(7):"";
  const p=token.split("."); if(p.length!==3) throw new Error("Sign in is required.");
  const head=JSON.parse(new TextDecoder().decode(b64(p[0])));
  const claims=JSON.parse(new TextDecoder().decode(b64(p[1])));
  const now=Math.floor(Date.now()/1000), project=env.FIREBASE_PROJECT_ID;
  if(head.alg!=="RS256" || !head.kid || claims.aud!==project || claims.iss!=="https://securetoken.google.com/"+project ||
     typeof claims.sub!=="string" || !claims.sub || typeof claims.exp!=="number" || claims.exp<=now ||
     typeof claims.iat!=="number" || claims.iat>now+60) throw new Error("Invalid or expired sign-in token.");
  const response=await fetch("https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com",{cf:{cacheTtl:3600,cacheEverything:true}});
  if(!response.ok) throw new Error("Could not verify sign-in.");
  const jwks=await response.json() as {keys:(JsonWebKey & {kid?:string})[]};
  const jwk=jwks.keys.find(k=>k.kid===head.kid); if(!jwk) throw new Error("Sign-in key not found.");
  const key=await crypto.subtle.importKey("jwk",jwk,{name:"RSASSA-PKCS1-v1_5",hash:"SHA-256"},false,["verify"]);
  const ok=await crypto.subtle.verify("RSASSA-PKCS1-v1_5",key,b64(p[2]),new TextEncoder().encode(p[0]+"."+p[1]));
  if(!ok) throw new Error("Invalid sign-in token.");
  return claims.sub;
}
async function usedBytes(bucket: R2Bucket): Promise<number> {
  let cursor:string|undefined,total=0;
  do { const page=await bucket.list({prefix:"music/",limit:1000,cursor}); total+=page.objects.reduce((n,o)=>n+o.size,0); cursor=page.truncated?page.cursor:undefined; } while(cursor);
  return total;
}
function clean(value: FormDataEntryValue|null, fallback: string): string {
  return (typeof value==="string"?value:fallback).trim().replace(/[<>]/g,"").slice(0,100)||fallback;
}
function byteRange(value:string|null,size:number) {
  if(!value) return null;
  const m=/^bytes=(\d*)-(\d*)$/.exec(value.trim()); if(!m) return null;
  let start:number,end:number;
  if(!m[1]) { const suffix=Number(m[2]); if(!suffix) return null; start=Math.max(0,size-suffix); end=size-1; }
  else { start=Number(m[1]); end=m[2]?Math.min(Number(m[2]),size-1):size-1; }
  if(!Number.isSafeInteger(start)||!Number.isSafeInteger(end)||start<0||end<start||start>=size) return null;
  return {offset:start,length:end-start+1,contentRange:"bytes "+start+"-"+end+"/"+size};
}
export default {
 async fetch(request:Request,env:Env):Promise<Response> {
  const h=cors(request,env); if(request.method==="OPTIONS") return new Response(null,{status:204,headers:h});
  const url=new URL(request.url);
  if(url.pathname==="/health"&&request.method==="GET") return reply({ok:true,service:"vibeplay-music"},200,h);
  try {
   const uid=await verify(request,env);
   if(url.pathname==="/v1/music"&&request.method==="GET") {
    const page=await env.MUSIC_BUCKET.list({prefix:"music/",limit:1000});
    return reply({tracks:page.objects.map(o=>({id:o.key,title:o.customMetadata?.title||o.key.split("/").pop()||"Untitled",artist:o.customMetadata?.artist||"Unknown artist",size:o.size,contentType:o.httpMetadata?.contentType||"audio/mpeg",uploadedBy:o.customMetadata?.uploadedBy||""})),truncated:page.truncated},200,h);
   }
   if(url.pathname==="/v1/music"&&request.method==="POST") {
    const form=await request.formData(), file=form.get("file");
    if(!(file instanceof File)) return reply({error:"Choose an audio file."},400,h);
    const type=(file.type||"").toLowerCase().split(";")[0];
    if(!TYPES.has(type)) return reply({error:"Unsupported audio format."},415,h);
    const max=Number(env.MAX_UPLOAD_BYTES||DEFAULT_MAX_UPLOAD);
    if(file.size<=0||file.size>max) return reply({error:"Maximum upload size is 15 MB."},413,h);
    if(await usedBytes(env.MUSIC_BUCKET)+file.size>Number(env.MAX_STORAGE_BYTES||DEFAULT_MAX_STORAGE)) return reply({error:"Music storage is full. Remove a track before uploading."},507,h);
    const ext=type==="audio/mpeg"?"mp3":type==="audio/mp4"?"m4a":type==="audio/aac"?"aac":type==="audio/ogg"?"ogg":type==="audio/flac"?"flac":"wav";
    const key="music/"+crypto.randomUUID()+"."+ext;
    const title=clean(form.get("title"),file.name.replace(/\.[^.]+$/,"").slice(0,100)||"Untitled");
    const artist=clean(form.get("artist"),"Unknown artist");
    await env.MUSIC_BUCKET.put(key,file.stream(),{httpMetadata:{contentType:type,cacheControl:"private, max-age=3600"},customMetadata:{title,artist,uploadedBy:uid,originalName:file.name.slice(0,120)}});
    return reply({id:key,title,artist,size:file.size,contentType:type},201,h);
   }
   if(url.pathname.startsWith("/v1/music/")&&request.method==="GET") {
    const key=decodeURIComponent(url.pathname.slice("/v1/music/".length));
    if(!key.startsWith("music/")||key.includes("..")) return reply({error:"Invalid track."},400,h);
    const head=await env.MUSIC_BUCKET.head(key); if(!head) return reply({error:"Track not found."},404,h);
    const range=byteRange(request.headers.get("Range"),head.size);
    if(request.headers.has("Range")&&!range) { h.set("Content-Range","bytes */"+head.size); return new Response(null,{status:416,headers:h}); }
    const obj=await env.MUSIC_BUCKET.get(key,range?{range:{offset:range.offset,length:range.length}}:{});
    if(!obj) return reply({error:"Track not found."},404,h);
    obj.writeHttpMetadata(h); h.set("Accept-Ranges","bytes"); h.set("ETag",obj.httpEtag); h.set("Cache-Control","private, max-age=3600");
    if(range) { h.set("Content-Range",range.contentRange); h.set("Content-Length",String(range.length)); } else h.set("Content-Length",String(head.size));
    return new Response(obj.body,{status:range?206:200,headers:h});
   }
   if(url.pathname.startsWith("/v1/music/")&&request.method==="DELETE") {
    const key=decodeURIComponent(url.pathname.slice("/v1/music/".length));
    if(!key.startsWith("music/")||key.includes("..")) return reply({error:"Invalid track."},400,h);
    const obj=await env.MUSIC_BUCKET.head(key); if(!obj) return reply({error:"Track not found."},404,h);
    if(obj.customMetadata?.uploadedBy!==uid) return reply({error:"Only the uploader can remove this track."},403,h);
    await env.MUSIC_BUCKET.delete(key); return reply({ok:true},200,h);
   }
   return reply({error:"Not found."},404,h);
  } catch(e) {
   const message=e instanceof Error?e.message:"Request failed.";
   return reply({error:message},message.toLowerCase().includes("sign-in")||message.toLowerCase().includes("token")||message.toLowerCase().includes("key")?401:500,h);
  }
 }
} satisfies ExportedHandler<Env>;
