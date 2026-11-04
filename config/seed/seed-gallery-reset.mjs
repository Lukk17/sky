#!/usr/bin/env node
import { readFileSync, existsSync } from "node:fs";
import { HERE, EDGE, load, join, basename, req, userToken } from "./lib/http.mjs";
const users = load("users.json");
const offers = load("offers.json");
const rooms = ["rooms/lukk-mountain-cabin-room.jpg","rooms/lukk-seaside-flat-room.jpg","rooms/miami-bay-view-room.jpg","rooms/miami-palm-breeze-room.jpg","rooms/miami-sunshine-suites-room.jpg","rooms/warsaw-chopin-residence-room.jpg","rooms/warsaw-old-town-stay-room.jpg","rooms/warsaw-prestige-tower-room.jpg","rooms/warsaw-vistula-lofts-room.jpg"];
async function token(u){ return userToken(u.username, u.password); }
async function api(method,path,tok,body,filename){const r=await req(method,`${EDGE}${path}`,{token:tok,file:body?{bytes:body,filename}:undefined});return r;}
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
