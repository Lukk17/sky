#!/usr/bin/env node
// Local seed script. Zero dependencies, Node 18+.
// Requires compose or k3d running first (Keycloak plus the edge reachable).
// Env: KEYCLOAK_BASE (default https://keycloak.test:9443), EDGE_BASE (default http://localhost:5777),
//   TLS_INSECURE=1 to skip TLS verification (self-signed Keycloak cert),
//   KEYCLOAK_ADMIN_USER / KEYCLOAK_ADMIN_PASSWORD (default admin/admin),
//   KEYCLOAK_CLIENT_ID / KEYCLOAK_CLIENT_SECRET (default sky-backend/dev-only-change-in-prod).
import { readFileSync, existsSync } from "node:fs";
import { basename, dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const HERE = dirname(fileURLToPath(import.meta.url));
const KEYCLOAK_BASE = (process.env.KEYCLOAK_BASE || "https://keycloak.test:9443").replace(/\/$/, "");
const EDGE = (process.env.EDGE_BASE || "http://localhost:5777").replace(/\/$/, "");
const INSECURE = process.env.TLS_INSECURE === "1";
const ADMIN_USER = process.env.KEYCLOAK_ADMIN_USER || "admin";
const ADMIN_PASSWORD = process.env.KEYCLOAK_ADMIN_PASSWORD || "admin";
const CLIENT_ID = process.env.KEYCLOAK_CLIENT_ID || "sky-backend";
const CLIENT_SECRET = process.env.KEYCLOAK_CLIENT_SECRET || "dev-only-change-in-prod";
const REALM = "sky";

if (INSECURE) process.env.NODE_TLS_REJECT_UNAUTHORIZED = "0";

const failures = [];
const load = (f) => JSON.parse(readFileSync(join(HERE, f), "utf8"));

async function req(method, url, { token, json, form, file } = {}) {
  const headers = {};
  if (token) headers.Authorization = `Bearer ${token}`;
  let body;
  if (json !== undefined) {
    headers["Content-Type"] = "application/json";
    headers.Accept = "application/json";
    body = JSON.stringify(json);
  } else if (form !== undefined) {
    headers["Content-Type"] = "application/x-www-form-urlencoded";
    body = new URLSearchParams(form).toString();
  } else if (file !== undefined) {
    const { bytes, filename } = file;
    const boundary = "seed" + Date.now().toString(16);
    headers["Content-Type"] = `multipart/form-data; boundary=${boundary}`;
    const head = Buffer.from(
      `--${boundary}\r\nContent-Disposition: form-data; name="file"; filename="${filename}"\r\nContent-Type: image/jpeg\r\n\r\n`,
      "utf8",
    );
    const tail = Buffer.from(`\r\n--${boundary}--\r\n`, "utf8");
    body = Buffer.concat([head, bytes, tail]);
  }
  const res = await fetch(url, { method, headers, body });
  const text = await res.text();
  let data = null;
  try { data = text ? JSON.parse(text) : null; } catch { data = text; }
  return { status: res.status, data };
}
const ok = (s, extra = []) => [200, 201, 204, ...extra].includes(s);
function fail(msg) { failures.push(msg); console.log(`FAIL ${msg}`); }

async function adminToken() {
  const r = await req("POST", `${KEYCLOAK_BASE}/realms/master/protocol/openid-connect/token`, {
    form: { grant_type: "password", client_id: "admin-cli", username: ADMIN_USER, password: ADMIN_PASSWORD },
  });
  if (!ok(r.status)) throw new Error(`admin login ${r.status}: ${JSON.stringify(r.data)}`);
  return r.data.access_token;
}
async function userToken(username, password) {
  const r = await req("POST", `${KEYCLOAK_BASE}/realms/${REALM}/protocol/openid-connect/token`, {
    form: { grant_type: "password", client_id: CLIENT_ID, client_secret: CLIENT_SECRET, username, password },
  });
  if (!ok(r.status)) throw new Error(`token for ${username}: ${r.status} ${JSON.stringify(r.data)}`);
  return r.data.access_token;
}

async function ensureUser(at, u) {
  const found = await req("GET", `${KEYCLOAK_BASE}/admin/realms/${REALM}/users?username=${encodeURIComponent(u.username)}`, { token: at });
  if (!ok(found.status)) return fail(`lookup user ${u.username}: ${found.status}`);
  if (Array.isArray(found.data) && found.data.length > 0) {
    const id = found.data[0]?.id;
    if (!id) return fail(`user id missing for ${u.username}`);
    const pw = await req("PUT", `${KEYCLOAK_BASE}/admin/realms/${REALM}/users/${id}/reset-password`, {
      token: at, json: { type: "password", value: u.password, temporary: false },
    });
    if (!ok(pw.status)) return fail(`password for ${u.username}: ${pw.status}`);
    const roles = await req("GET", `${KEYCLOAK_BASE}/admin/realms/${REALM}/roles/user`, { token: at });
    if (ok(roles.status) && roles.data?.id) {
      await req("POST", `${KEYCLOAK_BASE}/admin/realms/${REALM}/users/${id}/role-mappings/realm`, { token: at, json: [roles.data] });
    }
    console.log(`UPDATE user: ${u.email}`);
    return found.data[0];
  }
  const created = await req("POST", `${KEYCLOAK_BASE}/admin/realms/${REALM}/users`, {
    token: at,
    json: { username: u.username, email: u.email, firstName: u.firstName, lastName: u.lastName, enabled: true, emailVerified: true },
  });
  if (!ok(created.status)) return fail(`create user ${u.username}: ${created.status} ${JSON.stringify(created.data)}`);
  const again = await req("GET", `${KEYCLOAK_BASE}/admin/realms/${REALM}/users?username=${encodeURIComponent(u.username)}`, { token: at });
  const id = again.data?.[0]?.id;
  if (!id) return fail(`user id missing for ${u.username}`);
  const pw = await req("PUT", `${KEYCLOAK_BASE}/admin/realms/${REALM}/users/${id}/reset-password`, {
    token: at, json: { type: "password", value: u.password, temporary: false },
  });
  if (!ok(pw.status)) return fail(`password for ${u.username}: ${pw.status}`);
  const roles = await req("GET", `${KEYCLOAK_BASE}/admin/realms/${REALM}/roles/user`, { token: at });
  if (ok(roles.status) && roles.data?.id) {
    await req("POST", `${KEYCLOAK_BASE}/admin/realms/${REALM}/users/${id}/role-mappings/realm`, { token: at, json: [roles.data] });
  }
  console.log(`CREATE user: ${u.email}`);
}

async function listOffers(token) {
  const r = await req("GET", `${EDGE}/api/v1/offers`, { token });
  if (!ok(r.status)) throw new Error(`list offers: ${r.status}`);
  return Array.isArray(r.data) ? r.data : (r.data?.content ?? []);
}
async function listBookings(token) {
  const r = await req("GET", `${EDGE}/api/v1/user/bookings`, { token });
  if (!ok(r.status)) throw new Error(`list bookings: ${r.status}`);
  return Array.isArray(r.data) ? r.data : (r.data?.content ?? []);
}
async function listSent(token) {
  const r = await req("GET", `${EDGE}/api/v1/messages/sent`, { token });
  if (!ok(r.status)) return [];
  return Array.isArray(r.data) ? r.data : (r.data?.content ?? []);
}

function datePlus(offset) {
  const d = new Date();
  d.setDate(d.getDate() + offset);
  return d.toISOString().slice(0, 10);
}

async function main() {
  const users = load("users.json");
  const offers = load("offers.json");
  const bookings = load("bookings.json");
  const messages = load("messages.json");

  const at = await adminToken().catch((e) => { fail(e.message); return null; });
  if (!at) throw new Error("no admin token");
  for (const u of users) await ensureUser(at, u);

  const tokens = {};
  for (const u of users) {
    try { tokens[u.email] = await userToken(u.username, u.password); }
    catch (e) { fail(e.message); }
  }
  if (failures.length > 0) throw new Error("user setup failed");

  const existing = await listOffers(tokens[users[0].email]);
  const byHotel = new Map(existing.map((o) => [o.hotelName, o]));
  const offerIds = {};
  for (const o of offers) {
    if (byHotel.has(o.hotelName)) {
      console.log(`SKIP offer exists: ${o.hotelName}`);
      offerIds[o.slug] = byHotel.get(o.hotelName).id;
      continue;
    }
    const r = await req("POST", `${EDGE}/api/v1/owner/offers`, {
      token: tokens[o.ownerEmail],
      json: { hotelName: o.hotelName, description: o.description, comment: o.comment, price: o.price, roomCapacity: o.roomCapacity, city: o.city, country: o.country, externalPhotoUrl: o.externalPhotoUrl },
    });
    if (r.status === 201) { console.log(`CREATE offer: ${o.hotelName}`); offerIds[o.slug] = r.data.id; }
    else fail(`create offer ${o.hotelName}: ${r.status} ${JSON.stringify(r.data)}`);
  }

  for (const o of offers) {
    const photoPath = join(HERE, "photos", o.photoFile);
    if (!existsSync(photoPath)) { console.log(`SKIP photo missing file, kept externalPhotoUrl: ${o.hotelName}`); continue; }
    if (!offerIds[o.slug]) continue;
    const bytes = readFileSync(photoPath);
    const r = await req("POST", `${EDGE}/api/v1/owner/offers/${offerIds[o.slug]}/photo`, {
      token: tokens[o.ownerEmail], file: { bytes, filename: basename(photoPath) },
    });
    if (ok(r.status)) console.log(`UPLOAD photo: ${o.hotelName}`);
    else fail(`upload photo ${o.hotelName}: ${r.status} ${JSON.stringify(r.data)}`);
  }

  for (const b of bookings) {
    const offerId = offerIds[b.offerSlug];
    if (!offerId) { fail(`booking: unknown offer slug ${b.offerSlug}`); continue; }
    const mine = await listBookings(tokens[b.bookerEmail]);
    const date = datePlus(b.dayOffset);
    if (mine.some((x) => String(x.offerId) === String(offerId) && x.bookedDate === date)) {
      console.log(`SKIP booking exists: ${b.bookerEmail} ${b.offerSlug} ${date}`);
      continue;
    }
    const r = await req("POST", `${EDGE}/api/v1/bookings`, { token: tokens[b.bookerEmail], json: { offerId, dateToBook: date } });
    if (r.status === 201) console.log(`CREATE booking: ${b.bookerEmail} ${b.offerSlug} ${date}`);
    else fail(`create booking ${b.bookerEmail} ${b.offerSlug}: ${r.status} ${JSON.stringify(r.data)}`);
  }

  for (const m of messages) {
    const sent = await listSent(tokens[m.senderEmail]).catch(() => []);
    if (sent.some((x) => x.receiverEmail === m.receiverEmail && x.text === m.text)) {
      console.log(`SKIP message exists: ${m.senderEmail} -> ${m.receiverEmail}`);
      continue;
    }
    const r = await req("POST", `${EDGE}/api/v1/messages`, { token: tokens[m.senderEmail], json: { receiverEmail: m.receiverEmail, text: m.text } });
    if (r.status === 201) console.log(`CREATE message: ${m.senderEmail} -> ${m.receiverEmail}`);
    else fail(`create message ${m.senderEmail}: ${r.status} ${JSON.stringify(r.data)}`);
  }

  if (failures.length > 0) throw new Error(`${failures.length} failures`);

  const mismatches = [];
  const liveOffers = await listOffers(tokens[users[0].email]).catch(() => null);
  if (!liveOffers) mismatches.push(`offers: unable to list (expected ${offers.length})`);
  else if (liveOffers.length < offers.length) mismatches.push(`offers: found ${liveOffers.length}, expected ${offers.length}`);
  const expectedBookings = new Map();
  for (const b of bookings) expectedBookings.set(b.bookerEmail, (expectedBookings.get(b.bookerEmail) ?? 0) + 1);
  for (const [email, expected] of expectedBookings) {
    const mine = await listBookings(tokens[email]).catch(() => null);
    if (!mine) mismatches.push(`bookings ${email}: unable to list (expected ${expected})`);
    else if (mine.length < expected) mismatches.push(`bookings ${email}: found ${mine.length}, expected ${expected}`);
  }
  const expectedSent = new Map();
  for (const m of messages) expectedSent.set(m.senderEmail, (expectedSent.get(m.senderEmail) ?? 0) + 1);
  for (const [email, expected] of expectedSent) {
    const sent = await listSent(tokens[email]).catch(() => null);
    if (!sent) mismatches.push(`sent messages ${email}: unable to list (expected ${expected})`);
    else if (sent.length < expected) mismatches.push(`sent messages ${email}: found ${sent.length}, expected ${expected}`);
  }
  if (mismatches.length > 0) {
    for (const m of mismatches) fail(`verify: ${m}`);
    throw new Error(`SEED MISMATCH: ${mismatches.join("; ")}`);
  }
  console.log(`SEED VERIFIED: ${offers.length} offers, ${bookings.length} bookings, ${messages.length} messages`);
  console.log("SEED OK");
}

main().catch((e) => { console.error(`SEED FAILED: ${e.message}`); process.exit(1); });
