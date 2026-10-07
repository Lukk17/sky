import { readFileSync } from "node:fs";
import { join, dirname, basename } from "node:path";
import { fileURLToPath } from "node:url";

export const HERE = dirname(fileURLToPath(import.meta.url));
export const KEYCLOAK_BASE = (process.env.KEYCLOAK_BASE || "http://localhost:5777/auth").replace(/\/$/, "");
export const EDGE = (process.env.EDGE_BASE || "http://localhost:5777").replace(/\/$/, "");
export const CLIENT_ID = process.env.KEYCLOAK_CLIENT_ID || "sky-backend";
export const CLIENT_SECRET = process.env.KEYCLOAK_CLIENT_SECRET || "dev-only-change-in-prod";
export const REALM = "sky";

if (process.env.TLS_INSECURE === "1") process.env.NODE_TLS_REJECT_UNAUTHORIZED = "0";

export const failures = [];
export const load = (f) => JSON.parse(readFileSync(join(HERE, f), "utf8"));
export { join, basename };
export const ok = (s, extra = []) => [200, 201, 204, ...extra].includes(s);
export function fail(msg) { failures.push(msg); console.log(`FAIL ${msg}`); }

export async function req(method, url, { token, json, form, file } = {}) {
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

export async function userToken(username, password) {
  const r = await req("POST", `${KEYCLOAK_BASE}/realms/${REALM}/protocol/openid-connect/token`, {
    form: { grant_type: "password", client_id: CLIENT_ID, client_secret: CLIENT_SECRET, username, password },
  });
  if (!ok(r.status)) throw new Error(`token for ${username}: ${r.status} ${JSON.stringify(r.data)}`);
  return r.data.access_token;
}

export async function tokensFor(users) {
  const tokens = {};
  for (const u of users) {
    try { tokens[u.email] = await userToken(u.username, u.password); }
    catch (e) { fail(e.message); }
  }
  return tokens;
}
