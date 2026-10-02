#!/usr/bin/env node
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
async function upload(token_,id,path){const bytes=readFileSync(path);const b="g"+Date.now().toString(16);const head=Buffer.from(`--${b}\r\nContent-Disposition: form-data; name="file"; filename="${basename(path)}"\r\nContent-Type: image/jpeg\r\n\r\n`);const tail=Buffer.from(`\r\n--${b}--\r\n`);const r=await fetch(`${EDGE}/api/v1/owner/offers/${id}/photos`,{method:"POST",headers:{Authorization:`Bearer ${token_}`,"Content-Type":`multipart/form-data; boundary=${b}`},body:Buffer.concat([head,bytes,tail])});return r.status;}
const toks={};for(const u of users){toks[u.email]=await token(u);}
const lr=await fetch(`${EDGE}/api/v1/offers?size=100`,{headers:{Authorization:`Bearer ${toks[users[0].email]}`}});const lj=await lr.json();const live=lj.content??lj;
let ri=0;
for(const o of offers){
  const found=live.find(x=>x.hotelName===o.hotelName);if(!found)continue;
  const count=(found.gallery??[]).length;
  const need=Math.max(0,2-count);
  for(let i=0;i<need;i++){const p=join(HERE,"photos",rooms[ri++%rooms.length]);if(!existsSync(p))continue;const s=await upload(toks[o.ownerEmail],found.id,p);console.log(`${o.hotelName} extra ${basename(p)} -> ${s}`);}
}
const lr2=await fetch(`${EDGE}/api/v1/offers?size=100`,{headers:{Authorization:`Bearer ${toks[users[0].email]}`}});const lj2=await lr2.json();const live2=lj2.content??lj2;
for(const x of live2){console.log(`${x.hotelName}: ${(x.gallery??[]).length} photos`);}
