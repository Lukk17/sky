# Keycloak Local Setup Runbook

Step-by-step runbook for an AI agent or new developer to bring the local Keycloak environment from zero
to a verified, service-ready state. Follow the sections in order. Every command has a PowerShell and a
Unix (bash) version.

---

### Prerequisites

The following services must be running on the host before you start:

| Service | Address | Notes |
|---|---|---|
| Keycloak | https://keycloak.test:9443 | HTTPS, self-signed cert, KC_HOSTNAME=keycloak.test |
| PostgreSQL | localhost:5432 | database sky, user sky_user, password sky_pass |
| MinIO | http://localhost:9070 | access key admin, secret key password |

`keycloak.test` must resolve to `127.0.0.1` on the developer machine. Confirm the hosts file entry:

PowerShell (run as Administrator):

```powershell
Get-Content C:\Windows\System32\drivers\etc\hosts | Select-String keycloak.test
```

Unix:

```bash
grep keycloak.test /etc/hosts
```

The expected line is:

```
127.0.0.1 keycloak.test
```

If it is absent, add it. PowerShell (run as Administrator):

```powershell
Add-Content C:\Windows\System32\drivers\etc\hosts "`n127.0.0.1 keycloak.test"
```

Unix:

```bash
echo "127.0.0.1 keycloak.test" | sudo tee -a /etc/hosts
```

---

### Confirm Keycloak is reachable

Keycloak exposes its OIDC discovery endpoint at
`https://keycloak.test:9443/realms/master/.well-known/openid-configuration`.
Because it uses a self-signed certificate you must either pass `-k` (insecure) for the smoke-check,
or trust the cert first (see the Self-signed cert trust section).

PowerShell:

```powershell
Invoke-WebRequest -Uri "https://keycloak.test:9443/realms/master/.well-known/openid-configuration" -SkipCertificateCheck | Select-Object -ExpandProperty StatusCode
```

Unix:

```bash
curl -sk https://keycloak.test:9443/realms/master/.well-known/openid-configuration | python3 -m json.tool | head -5
```

Expected result: HTTP 200 and a JSON body with an `issuer` field.

---

### Obtain an admin token from the master realm

All realm import and verification calls use this token. The token expires after 60 seconds by default;
re-run this step if you get 401 errors later.

PowerShell:

```powershell
$token = (Invoke-RestMethod -SkipCertificateCheck `
  -Uri "https://keycloak.test:9443/realms/master/protocol/openid-connect/token" `
  -Method Post `
  -ContentType "application/x-www-form-urlencoded" `
  -Body "grant_type=password&client_id=admin-cli&username=admin&password=admin").access_token
echo $token
```

Unix:

```bash
TOKEN=$(curl -sk \
  -d "grant_type=password&client_id=admin-cli&username=admin&password=admin" \
  https://keycloak.test:9443/realms/master/protocol/openid-connect/token \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['access_token'])")
echo $TOKEN
```

---

### Import the sky realm via the admin REST API

The sky realm definition lives at `config/keycloak/sky-realm.json`. Run the command from the repository
root (the path is relative to the project root).

PowerShell:

```powershell
Invoke-RestMethod -SkipCertificateCheck `
  -Uri "https://keycloak.test:9443/admin/realms" `
  -Method Post `
  -Headers @{ Authorization = "Bearer $token" } `
  -ContentType "application/json" `
  -InFile "config\keycloak\sky-realm.json"
```

Unix:

```bash
curl -sk \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d @config/keycloak/sky-realm.json \
  https://keycloak.test:9443/admin/realms
```

A 201 response (empty body) means the realm was created. A 409 means the realm already exists, which is
safe to ignore.

---

### Import via kcadm.sh (alternative)

If you have direct shell access to the running Keycloak container, `kcadm.sh` is an alternative.
Replace `keycloak` with your container name if it differs.

Copy the realm file into the container:

```bash
docker cp config/keycloak/sky-realm.json keycloak:/tmp/sky-realm.json
```

Authenticate the admin CLI:

```bash
docker exec keycloak /opt/keycloak/bin/kcadm.sh config credentials \
  --server https://localhost:9443 \
  --realm master \
  --user admin \
  --password admin
```

Import the realm:

```bash
docker exec keycloak /opt/keycloak/bin/kcadm.sh create realms \
  -f /tmp/sky-realm.json
```

---

### Import via the admin console (GUI)

For a click-through import instead of the terminal:

1. Open the admin console at https://keycloak.test:9443/admin and sign in with admin / admin.
2. Open the realm dropdown in the top-left (it shows the current realm, usually "master") and click
   Create realm.
3. Under Resource file, click Browse and select config/keycloak/sky-realm.json from this repo.
4. The realm name fills in as "sky" from the file. Click Create.
5. The realm, the sky-backend client, the admin and user roles, and the demo users (including lukk)
   are created from the file. Verify under Realm settings, Clients, Realm roles, and Users.

To re-import over an existing realm, delete it first under Realm settings, Action, Delete realm, then
repeat. There is no in-place overwrite in the console.

---

### Note: the k3d in-cluster Keycloak imports automatically

The above (REST, kcadm, or GUI) is for your host Keycloak used by Docker Compose. The k3d cluster does
not need any of it: its Keycloak chart ships the realm as a ConfigMap mounted at
`/opt/keycloak/data/import` and starts Keycloak with `--import-realm`, so the sky realm and the lukk
user are imported on pod startup. The chart's copy of the realm is
`config/k8s/helm/infra/keycloak/files/sky-realm.json`. See config/k8s/local_README.md for the k3d flow.

---

### Verify the realm

Confirm the sky realm exists and is enabled:

PowerShell:

```powershell
Invoke-RestMethod -SkipCertificateCheck `
  -Uri "https://keycloak.test:9443/admin/realms/sky" `
  -Headers @{ Authorization = "Bearer $token" } `
  | Select-Object realm, enabled
```

Unix:

```bash
curl -sk \
  -H "Authorization: Bearer $TOKEN" \
  https://keycloak.test:9443/admin/realms/sky \
  | python3 -c "import sys,json; r=json.load(sys.stdin); print(r['realm'], r['enabled'])"
```

Expected: `sky True`.

---

### Verify the sky-backend client

PowerShell:

```powershell
Invoke-RestMethod -SkipCertificateCheck `
  -Uri "https://keycloak.test:9443/admin/realms/sky/clients" `
  -Headers @{ Authorization = "Bearer $token" } `
  | Where-Object { $_.clientId -eq "sky-backend" } `
  | Select-Object clientId, enabled, secret
```

Unix:

```bash
curl -sk \
  -H "Authorization: Bearer $TOKEN" \
  "https://keycloak.test:9443/admin/realms/sky/clients" \
  | python3 -c "
import sys, json
clients = json.load(sys.stdin)
for c in clients:
    if c.get('clientId') == 'sky-backend':
        print('clientId:', c['clientId'], '| enabled:', c['enabled'], '| secret:', c.get('secret','(fetch separately)'))
"
```

---

### Verify realm roles

PowerShell:

```powershell
Invoke-RestMethod -SkipCertificateCheck `
  -Uri "https://keycloak.test:9443/admin/realms/sky/roles" `
  -Headers @{ Authorization = "Bearer $token" } `
  | Select-Object name
```

Unix:

```bash
curl -sk \
  -H "Authorization: Bearer $TOKEN" \
  https://keycloak.test:9443/admin/realms/sky/roles \
  | python3 -c "import sys,json; [print(r['name']) for r in json.load(sys.stdin)]"
```

Expected roles include: `admin`, `user`.

---

### Verify the three demo users

PowerShell:

```powershell
Invoke-RestMethod -SkipCertificateCheck `
  -Uri "https://keycloak.test:9443/admin/realms/sky/users" `
  -Headers @{ Authorization = "Bearer $token" } `
  | Select-Object username, email, enabled
```

Unix:

```bash
curl -sk \
  -H "Authorization: Bearer $TOKEN" \
  https://keycloak.test:9443/admin/realms/sky/users \
  | python3 -c "import sys,json; [print(u['username'], u.get('email',''), u['enabled']) for u in json.load(sys.stdin)]"
```

Expected users: `owner` (owner@sky.dev), `user` (user@sky.dev), `lukk` (no email in realm file).

Credentials are local-development-only, non-secret, intentionally committed values:

| Username | Password | Realm role |
|---|---|---|
| owner | owner | user |
| user | user | user |
| lukk | test1234 | admin |

---

### Mint a token to verify end-to-end authentication

PowerShell:

```powershell
Invoke-RestMethod -SkipCertificateCheck `
  -Uri "https://keycloak.test:9443/realms/sky/protocol/openid-connect/token" `
  -Method Post `
  -ContentType "application/x-www-form-urlencoded" `
  -Body "grant_type=password&client_id=sky-backend&client_secret=dev-only-change-in-prod&username=owner&password=owner" `
  | Select-Object access_token, token_type, expires_in
```

Unix:

```bash
curl -sk \
  -d "grant_type=password&client_id=sky-backend&client_secret=dev-only-change-in-prod&username=owner&password=owner" \
  https://keycloak.test:9443/realms/sky/protocol/openid-connect/token \
  | python3 -m json.tool | grep -E '"access_token"|"token_type"|"expires_in"'
```

A non-empty `access_token` confirms Keycloak is issuing tokens correctly.

---

### Add a new user

PowerShell:

```powershell
Invoke-RestMethod -SkipCertificateCheck `
  -Uri "https://keycloak.test:9443/admin/realms/sky/users" `
  -Method Post `
  -Headers @{ Authorization = "Bearer $token" } `
  -ContentType "application/json" `
  -Body '{"username":"newuser","email":"newuser@sky.dev","enabled":true,"credentials":[{"type":"password","value":"newpass","temporary":false}]}'
```

Unix:

```bash
curl -sk -w "\n%{http_code}" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"username":"newuser","email":"newuser@sky.dev","enabled":true,"credentials":[{"type":"password","value":"newpass","temporary":false}]}' \
  https://keycloak.test:9443/admin/realms/sky/users
```

HTTP 201 confirms the user was created.

---

### Reset a user's password

First look up the user's internal ID:

PowerShell:

```powershell
$userId = (Invoke-RestMethod -SkipCertificateCheck `
  -Uri "https://keycloak.test:9443/admin/realms/sky/users?username=owner" `
  -Headers @{ Authorization = "Bearer $token" })[0].id
echo $userId
```

Unix:

```bash
USER_ID=$(curl -sk \
  -H "Authorization: Bearer $TOKEN" \
  "https://keycloak.test:9443/admin/realms/sky/users?username=owner" \
  | python3 -c "import sys,json; print(json.load(sys.stdin)[0]['id'])")
echo $USER_ID
```

Then set the new password:

PowerShell:

```powershell
Invoke-RestMethod -SkipCertificateCheck `
  -Uri "https://keycloak.test:9443/admin/realms/sky/users/$userId/reset-password" `
  -Method Put `
  -Headers @{ Authorization = "Bearer $token" } `
  -ContentType "application/json" `
  -Body '{"type":"password","value":"newpass","temporary":false}'
```

Unix:

```bash
curl -sk -w "\n%{http_code}" -X PUT \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"type":"password","value":"newpass","temporary":false}' \
  "https://keycloak.test:9443/admin/realms/sky/users/$USER_ID/reset-password"
```

HTTP 204 confirms success.

---

### Trust the self-signed Keycloak certificate

The Spring Security OAuth2 resource server fetches the JWKS from `https://keycloak.test:9443`. Because
the certificate is self-signed the JVM will refuse the connection with a PKIX path validation error
unless the certificate is trusted. Do not disable TLS validation in committed code; trust the cert
explicitly instead.

There are two approaches.

#### Approach A: Add the cert to a local truststore and pass it to the JVM at startup

Step 1: Extract the certificate from Keycloak.

PowerShell:

```powershell
'' | openssl s_client -connect keycloak.test:9443 -showcerts 2>$null | openssl x509 -outform PEM -out keycloak-local.crt
```

Unix:

```bash
openssl s_client -connect keycloak.test:9443 -showcerts </dev/null 2>/dev/null \
  | openssl x509 -outform PEM -out keycloak-local.crt
```

Step 2: Import the certificate into a local truststore. This creates `local-truststore.jks` in the
current directory. Choose any password (the example uses `changeit`).

PowerShell:

```powershell
keytool -importcert -noprompt `
  -alias keycloak-local `
  -file keycloak-local.crt `
  -keystore local-truststore.jks `
  -storepass changeit
```

Unix:

```bash
keytool -importcert -noprompt \
  -alias keycloak-local \
  -file keycloak-local.crt \
  -keystore local-truststore.jks \
  -storepass changeit
```

Step 3: Pass the truststore to the service at startup. Add these JVM arguments to the Gradle `bootRun`
task or to the IntelliJ run configuration.

PowerShell:

```powershell
.\gradlew.bat :sky-offer:bootRun `
  --args='--spring.profiles.active=local' `
  -Djavax.net.ssl.trustStore=local-truststore.jks `
  -Djavax.net.ssl.trustStorePassword=changeit
```

Unix:

```bash
./gradlew :sky-offer:bootRun \
  --args='--spring.profiles.active=local' \
  -Djavax.net.ssl.trustStore=local-truststore.jks \
  -Djavax.net.ssl.trustStorePassword=changeit
```

#### Approach B: Add the cert to the JDK cacerts store (trust for all JVM processes)

Locate the cacerts file. On a standard JDK 21/25 install:

PowerShell:

```powershell
$javaHome = (Get-Command java).Source | Split-Path | Split-Path
$cacerts = Join-Path $javaHome "lib\security\cacerts"
echo $cacerts
```

Unix:

```bash
JAVA_HOME=$(dirname $(dirname $(readlink -f $(which java))))
CACERTS=$JAVA_HOME/lib/security/cacerts
echo $CACERTS
```

Import into cacerts (the default password is `changeit`):

PowerShell (run as Administrator):

```powershell
keytool -importcert -noprompt `
  -alias keycloak-local `
  -file keycloak-local.crt `
  -keystore $cacerts `
  -storepass changeit
```

Unix:

```bash
sudo keytool -importcert -noprompt \
  -alias keycloak-local \
  -file keycloak-local.crt \
  -keystore $CACERTS \
  -storepass changeit
```

After this, services started with that JDK trust the certificate without extra JVM arguments.

#### Approach C: Generate the truststore for the Docker Compose e2e stack

The `config/docker/docker-compose.yaml` stack mounts a PKCS12 truststore into the four JWT-validating
services (offer, booking, message, notify) at `/certs/sky-truststore.p12` and points the JVM at it via
`JAVA_TOOL_OPTIONS`. Generate that file at `config/docker/certs/sky-truststore.p12` before running
`docker compose up`. The store password is `changeit`, a local-only non-secret value. The file is
git-ignored because it is derived from your local Keycloak certificate.

Run from the repository root.

PowerShell:

```powershell
'' | openssl s_client -connect keycloak.test:9443 -showcerts 2>$null | openssl x509 -outform PEM -out keycloak-local.crt
```

```powershell
keytool -importcert -noprompt -alias keycloak-local -file keycloak-local.crt -keystore config\docker\certs\sky-truststore.p12 -storetype PKCS12 -storepass changeit
```

Unix:

```bash
openssl s_client -connect keycloak.test:9443 -showcerts </dev/null 2>/dev/null | openssl x509 -outform PEM -out keycloak-local.crt
```

```bash
keytool -importcert -noprompt -alias keycloak-local -file keycloak-local.crt -keystore config/docker/certs/sky-truststore.p12 -storetype PKCS12 -storepass changeit
```

Then build and start the stack from the repository root.

PowerShell:

```powershell
docker compose -f config/docker/docker-compose.yaml up --build -d
```

Unix:

```bash
docker compose -f config/docker/docker-compose.yaml up --build -d
```

---

### Verify a service can fetch the JWKS endpoint

Once a service is running with the truststore configured, confirm it resolved the issuer:

PowerShell:

```powershell
Invoke-RestMethod -Uri "http://localhost:5552/actuator/health/readiness" | Select-Object status
```

Unix:

```bash
curl -s http://localhost:5552/actuator/health/readiness | python3 -m json.tool
```

Expected: `{"status":"UP"}`. If the status is DOWN and the logs show `PKIX path validation failed`, the
truststore is not yet applied correctly, go back to the trust step above.
