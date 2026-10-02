#!/usr/bin/env node
// Reset each offer gallery: delete all gallery photos, then upload the hotel
// exterior (seed/photos/hotels) as main cover plus two distinct room photos
// (seed/photos/rooms, round-robin without repeats inside one offer).
// Bookings and messages are untouched. Idempotent on rerun.
// Env: EDGE_BASE (k3d: http://localhost), KEYCLOAK_BASE
// (k3d: http://keycloak.127.0.0.1.nip.io), KEYCLOAK_CLIENT_ID/SECRET.
import { readFileSync, existsSync } from "node:fs";
import { basename, join, dirname } from "node:path";
import { fileURLToPath } from "node:url";
const HERE = dirname(fileURLToPath(import.meta.url));
const EDGE = (process.env.EDGE_BASE || "http://localhost:5777").replace(/\/$/, "");
const KEYCLOAK_BASE = (process.env.KEYCLOAK_BASE || "https://keycloak.test:9443").replace(/\/$/, "");
if (process.env.TLS_INSECURE === "1") process.env.NODE_TLS_REJECT_UNAUTHORIZED = "0";
const CLIENT_ID = process.env.KEYCLOAK_CLIENT_ID || "sky-backend";
const CLIENT_SECRET = process.env.KEYCLOAK_CLIENT_SECRET || "dev-only-change-in-prod";
const users = JSON.parse(readFileSync(join(HERE, "users.json"), "utf8"));
const offers = JSON.parse(readFileSync(join(HERE, "offers.json"), "utf8"));
const rooms = ["rooms/lukk-mountain-cabin-room.jpg","rooms/lukk-seaside-flat-room.jpg","rooms/miami-bay-view-room.jpg","rooms/miami-palm-breeze-room.jpg","rooms/miami-sunshine-suites-room.jpg","rooms/warsaw-chopin-residence-room.jpg","rooms/warsaw-old-town-stay-room.jpg","rooms/warsaw-prestige-tower-room.jpg","rooms/warsaw-vistula-lofts-room.jpg"];
async function token(u){const r=await fetch(`${KEYCLOAK_BASE}/realms/sky/protocol/openid-connect/token`,{method:"POST",headers:{"Content-Type":"application/x-www-form-urlencoded"},body:new URLSearchParams({grant_type:"password",client_id:CLIENT_ID,client_secret:CLIENT_SECRET,username:u.username,password:u.password})});const t=await r.text();let j={};try{j=JSON.parse(t);}catch{throw new Error(`token ${u.username}: ${r.status} ${t.slice(0,120)}`);}if(!j.access_token)throw new Error(`token ${u.username}: ${r.status} ${t.slice(0,120)}`);return j.access_token;}
async function api(method,path,tok,body,filename){const headers={Authorization:`Bearer ${tok}`};let payload;if(body){const b="r"+Date.now().toString(16)+Math.floor(Math.random()*1e6);headers["Content-Type"]=`multipart/form-data; boundary=${b}`;const head=Buffer.from(`--${b}\r\nContent-Disposition: form-data; name="file"; filename="${filename}"\r\nContent-Type: image/jpeg\r\n\r\n`);payload=Buffer.concat([head,body,Buffer.from(`\r\n--${b}--\r\n`)]);}const r=await fetch(`${EDGE}${path}`,{method,headers,body:payload});const t=await r.text();let j=null;try{j=t?JSON.parse(t):null;}catch{j=t;}return{status:r.status,data:j};}
const toks={};for(const u of users){toks[u.email]=await token(u);}
const lr=await fetch(`${EDGE}/api/v1/offers?size=100`,{headers:{Authorization:`Bearer ${toks[users[0].email]}`}});const live=(await lr.json()).content??[];
let failures=0;
for(let oi=0;oi<offers.length;oi++){
  const o=offers[oi];const found=live.find(x=>x.hotelName===o.hotelName);if(!found){console.log(`SKIP missing offer ${o.hotelName}`);continue;}
  const tok=toks[o.ownerEmail];
  for(const g of (found.gallery??[])){if(!g.id)continue;const d=await api("DELETE",`/api/v1/owner/offers/${found.id}/photos/${g.id}`,tok);console.log(`${o.hotelName} delete gallery ${g.id} -> ${d.status}`);if(![200,404].includes(d.status))failures++;}
  const ext=join(HERE,"photos",o.photoFile);if(!existsSync(ext)){console.log(`SKIP exterior missing ${o.photoFile}`);failures++;continue;}
  const up=await api("POST",`/api/v1/owner/offers/${found.id}/photos`,tok,readFileSync(ext),basename(ext));
  console.log(`${o.hotelName} upload exterior ${basename(ext)} -> ${up.status}`);if(up.status!==200){failures++;continue;}
  const r1=rooms[(oi*2)%rooms.length],r2=rooms[(oi*2+1)%rooms.length];
  for(const rp of [r1,r2]){const p=join(HERE,"photos",rp);const u2=await api("POST",`/api/v1/owner/offers/${found.id}/photos`,tok,readFileSync(p),basename(p));console.log(`${o.hotelName} upload room ${basename(p)} -> ${u2.status}`);if(u2.status!==200)failures++;}
  const cur=await (await fetch(`${EDGE}/api/v1/offers?size=100`,{headers:{Authorization:`Bearer ${toks[users[0].email]}`}})).json();
  const me=(cur.content??[]).find(x=>x.id===found.id);
  const mainPhoto=(me.gallery??[]).find(g=>g.main);
  console.log(`${o.hotelName}: gallery=${(me.gallery??[]).length} main=${mainPhoto?mainPhoto.id:"NONE"} coverSet=${me.coverPhotoUrl===mainPhoto?.url}`);
}
if(failures>0){console.error(`GALLERY RESET FAILED: ${failures} failures`);process.exit(1);}
console.log("GALLERY RESET OK");
