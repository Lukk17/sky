#!/usr/bin/env node
import { readFileSync, existsSync } from "node:fs";
import { HERE, EDGE, KEYCLOAK_BASE, CLIENT_ID, CLIENT_SECRET, load, join, basename, ok, req, userToken } from "./lib/http.mjs";
const users = load("users.json");
const offers = load("offers.json");
const rooms = ["rooms/lukk-mountain-cabin-room.jpg","rooms/lukk-seaside-flat-room.jpg","rooms/miami-bay-view-room.jpg","rooms/miami-palm-breeze-room.jpg","rooms/miami-sunshine-suites-room.jpg","rooms/warsaw-chopin-residence-room.jpg","rooms/warsaw-old-town-stay-room.jpg","rooms/warsaw-prestige-tower-room.jpg","rooms/warsaw-vistula-lofts-room.jpg"];
async function token(u){ return userToken(u.username, u.password); }
async function upload(token_,id,path){const bytes=readFileSync(path);const r=await req("POST",`${EDGE}/api/v1/owner/offers/${id}/photos`,{token:token_,file:{bytes,filename:basename(path)}});return r.status;}
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
