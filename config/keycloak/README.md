# Keycloak realm (realm-as-code)

`sky-realm.json` is the single source of truth for the `sky` Keycloak realm. It is imported
into the running Keycloak locally and, for the cluster, synced into the Keycloak Helm chart at
deploy time (the `helm-app-deploy` script copies it to `config/k8s/helm/infra/keycloak/files/`,
which is gitignored), so the realm comes up pre-configured. Edit only this file; never edit the
generated chart copy.

---

### Local development endpoints and credentials

These are local-development-only, non-secret, intentionally committed values for the local Docker
stack. Real environments inject secrets via Kubernetes sealed-secrets; none of the values below
exist in any production system.

| What | Value |
|---|---|
| Keycloak URL | https://keycloak.test:9443 |
| Admin user | admin / admin |
| Realm | sky |
| Client | sky-backend |
| Client secret | dev-only-change-in-prod |
| User: owner | owner / owner (realm role: user) |
| User: user | user / user (realm role: user) |
| User: lukk | lukk / test1234 (realm role: admin) |

For a step-by-step guide to importing the realm, trusting the self-signed certificate, verifying
users and roles, and minting tokens, see SETUP.md.

---

### What the realm defines

- Realm `sky` (login with email allowed, registration disabled).
- Client `sky-backend` (confidential): standard flow + direct access grants
  (password grant) + service accounts (client credentials). It carries an
  audience mapper so every access token's `aud` includes `sky-backend`, which the
  services validate when `OAUTH2_AUDIENCE=sky-backend` is set.
- Three demo users whose credentials match the seed data:
  - `owner` / `owner` (`owner@sky.dev`) with realm role `user`
  - `user` / `user` (`user@sky.dev`) with realm role `user`
  - `lukk` / `test1234` with realm role `admin`

---

### Import into a running Keycloak (quick reference)

For the full runbook with PowerShell and Unix commands, see SETUP.md. Quick reference below.

Get an admin token:

```shell
curl -sk -d "grant_type=password&client_id=admin-cli&username=admin&password=admin" \
  https://keycloak.test:9443/realms/master/protocol/openid-connect/token
```

Import the realm (run from the repo root):

```shell
curl -sk -H "Authorization: Bearer <token>" -H "Content-Type: application/json" \
  -d @config/keycloak/sky-realm.json \
  https://keycloak.test:9443/admin/realms
```

For a fresh Keycloak you can instead start it with the import flag and the file
mounted under `/opt/keycloak/data/import/`:

```shell
docker run -p 9443:9443 \
  -e KEYCLOAK_ADMIN=admin \
  -e KEYCLOAK_ADMIN_PASSWORD=admin \
  -v "$(pwd)/config/keycloak:/opt/keycloak/data/import" \
  quay.io/keycloak/keycloak:26.0 start-dev --import-realm
```

---

### Mint a token (for the Bruno collection or manual testing)

```shell
curl -s -X POST https://keycloak.test:9443/realms/sky/protocol/openid-connect/token \
  -d grant_type=password \
  -d client_id=sky-backend \
  -d client_secret=dev-only-change-in-prod \
  -d username=owner \
  -d password=owner
```

Take the `access_token` from the response and paste it into the Bruno environment
variable `bearerToken`. Pass `-k` to curl if you have not yet trusted the self-signed
certificate (see SETUP.md for the trust step).

---

### Services

Each service reads `OAUTH2_ISSUER_URI` and validates the JWT. The application defaults point
at `https://keycloak.test:9443/realms/sky` so a bare local run works without setting any
environment variable. Production and Helm overrides inject the real issuer via the env var.
Set `OAUTH2_AUDIENCE=sky-backend` to additionally enforce the audience claim.
