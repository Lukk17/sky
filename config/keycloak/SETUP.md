# Keycloak

Everything about identity in this project: what the `sky` realm contains, how to bring a local Keycloak from zero to a verified, service-ready state, and how to get the JVM to trust the certificate it serves.

Follow the sections in order the first time. Every command has a PowerShell and a Unix shell version, and every path is relative to the repository root.

---

### Realm as code

The realm is defined by one file, [config/k8s/helm/infra/keycloak/files/sky-realm.json](../k8s/helm/infra/keycloak/files/sky-realm.json). That is the only copy in the repository. It lives inside the Keycloak Helm chart because the chart publishes it as a ConfigMap mounted at `/opt/keycloak/data/import`, and the local Docker run below mounts the same directory. There is no copy step and no generated duplicate to keep in sync.

What the file defines:

- Realm `sky`, enabled, login with email allowed, self-registration disabled.
- Realm roles `admin` and `user`.
- Confidential client `sky-backend` with the standard flow, direct access grants (password grant), and service accounts (client credentials). An audience mapper puts `sky-backend` into every access token's `aud` claim, which the services enforce when `OAUTH2_AUDIENCE=sky-backend` is set.
- Five redirect URIs on that client. Three are the originals: `http://localhost:8080/*`, `https://skycloud.luksarna.com/*`, and `https://skycloud.luksarna.com/oauth2/callback`. Two were added for the local Spring Cloud Gateway, which logs in on its own behalf once the `local` profile is off: `http://localhost:5777/oauth2/callback` for the oauth2-proxy-style path, and `http://localhost:5777/login/oauth2/code/keycloak` for the Spring Security default the gateway's `redirect-uri` template produces.
- Three demo users whose emails match the Flyway demo seed data.

Credentials are development-only, non-secret, intentionally committed values:

| Username | Email | Password | Realm roles |
|---|---|---|---|
| lukk | lukk@sky.dev | test1234 | admin, user |
| owner | owner@sky.dev | owner | admin, user |
| user | user@sky.dev | user | user |

| What | Value |
|---|---|
| Keycloak URL | https://keycloak.test:9443 |
| Admin console | https://keycloak.test:9443/admin, signed in with that instance's own administrator |
| Realm | sky |
| Client | sky-backend |
| Client secret | dev-only-change-in-prod |

Realm, client, roles and users all come out of that one file, so a Keycloak whose database is empty is fully seeded by mounting it. Two places get that for free and need no steps at all: the Helm chart, and the self-contained end-to-end Compose stack in [config/docker/docker-compose.ci.yaml](../docker/docker-compose.ci.yaml), which mounts the same directory at `/opt/keycloak/data/import` and starts with `--import-realm`.

---

### Which Keycloak, and which administrator

`admin / admin` is not one credential for one server. Four Keycloak instances serve this project and three of them get their administrator from somewhere this repository controls. One does not, and it is the one you are most likely sitting in front of.

| Instance | Address | Administrator |
|---|---|---|
| The throwaway container from [Start Keycloak with the realm already imported](#start-keycloak-with-the-realm-already-imported) | https://keycloak.test:9443 | admin / admin, because that `docker run` passes `KC_BOOTSTRAP_ADMIN_USERNAME` and `KC_BOOTSTRAP_ADMIN_PASSWORD` itself |
| The shared local-dev stack, from the neighbouring `InstallationHelper` checkout | https://keycloak.test:9443 | whatever was bootstrapped on its database, which nothing in this repository sets |
| The Keycloak the Helm chart deploys in a cluster | http://keycloak.127.0.0.1.nip.io:5777 | admin / admin, read from the `keycloak-admin` and `keycloak-admin-password` entries of the `sky-secrets` Secret |
| The end-to-end Compose stack, [config/docker/docker-compose.ci.yaml](../docker/docker-compose.ci.yaml) | inside its own network only | admin / admin, set on the service |

The first two share an address, and that is the whole trap. They both answer on `keycloak.test:9443`, only one can hold the port at a time, and every `admin / admin` in the sections below is a fact for the throwaway container and a guess for the shared stack. If a password grant against `master` with `admin` and `admin` does not hand back a token, you are probably talking to the shared stack, and the next section is what you need first.

---

### The shared local-dev Keycloak has no administrator until somebody creates one

The `keycloak` service in `InstallationHelper/local-dev/local-dev-docker-compose.yaml` sets its database variables and its hostname variables and nothing else. No `KC_BOOTSTRAP_ADMIN_USERNAME`, no `KC_BOOTSTRAP_ADMIN_PASSWORD`, and its store is a persistent PostgreSQL rather than an in-memory one, so a database that has never been bootstrapped comes up with no administrator at all. Until one exists, every route in the three import sections below fails, because all of them authenticate as an admin first.

Create one against the running container. The command is identical in PowerShell and in a Unix shell:

```bash
docker exec -e KC_BOOTSTRAP_ADMIN_PASSWORD=admin keycloak /opt/keycloak/bin/kc.sh bootstrap-admin user --username admin --password:env KC_BOOTSTRAP_ADMIN_PASSWORD --no-prompt --optimized
```

`--optimized` belongs there because that image is pre-built with `kc.sh build`. Leave it off and kc.sh re-augments the server inside the running container for no benefit. Success logs `Created temporary admin user with username admin`, and temporary is Keycloak's word for an unrestricted account it wants you to replace with a named one in the console. On a development stack it is reasonable to leave it, and leaving it is also what makes every `admin / admin` below true for this instance.

Closing that gap properly belongs to the `InstallationHelper` project, not to this one. The compose file lives there and nothing in this repository can add a variable to it.

Importing the `sky` realm into that instance, on the other hand, is a normal step rather than a workaround. The shared stack deliberately carries no project data, because owning a realm is each project's job, and it imports only its own `local-realm-export.json`. Seeding `sky` there is this project's work to do.

---

### Prerequisites

`keycloak.test` must resolve to `127.0.0.1` on the developer machine.

PowerShell, run as Administrator:

```powershell
Get-Content C:\Windows\System32\drivers\etc\hosts | Select-String keycloak.test
```

Unix shell:

```bash
grep keycloak.test /etc/hosts
```

The expected line is:

```text
127.0.0.1 keycloak.test
```

If it is absent, add it. PowerShell, run as Administrator:

```powershell
Add-Content C:\Windows\System32\drivers\etc\hosts "`n127.0.0.1 keycloak.test"
```

Unix shell:

```bash
echo "127.0.0.1 keycloak.test" | sudo tee -a /etc/hosts
```

The other two host services the stack expects are in [config/local-dev/local_README.md](../local-dev/local_README.md): PostgreSQL on 5432 and the object store on 9070.

---

### Start Keycloak with the realm already imported

This is the shortest path. The container serves HTTPS on host port 9443 with the development keypair the `local-dev` stack generates at `local-dev/auth/certificates/localhost/`, and imports the realm on first boot. The keypair's subject alternative names cover `keycloak.test`, `localhost`, `host.docker.internal`, `keycloak`, `sky.test`, and `sky`.

The commands assume the `InstallationHelper` checkout sits beside this one, so `../InstallationHelper` resolves from the repository root. The private key stays in that project and is never copied here.

PowerShell:

```powershell
docker run -d --name keycloak -p 9443:8443 -e KC_BOOTSTRAP_ADMIN_USERNAME=admin -e KC_BOOTSTRAP_ADMIN_PASSWORD=admin -e KC_HOSTNAME=https://keycloak.test:9443 -e KC_HTTPS_CERTIFICATE_FILE=/opt/keycloak/conf/tls.crt -e KC_HTTPS_CERTIFICATE_KEY_FILE=/opt/keycloak/conf/tls.key -v "${PWD}/../InstallationHelper/local-dev/auth/certificates/localhost/localhost.crt:/opt/keycloak/conf/tls.crt:ro" -v "${PWD}/../InstallationHelper/local-dev/auth/certificates/localhost/localhost.key:/opt/keycloak/conf/tls.key:ro" -v "${PWD}/config/k8s/helm/infra/keycloak/files:/opt/keycloak/data/import:ro" quay.io/keycloak/keycloak:26.5.7 start-dev --import-realm
```

Unix shell:

```bash
docker run -d --name keycloak -p 9443:8443 -e KC_BOOTSTRAP_ADMIN_USERNAME=admin -e KC_BOOTSTRAP_ADMIN_PASSWORD=admin -e KC_HOSTNAME=https://keycloak.test:9443 -e KC_HTTPS_CERTIFICATE_FILE=/opt/keycloak/conf/tls.crt -e KC_HTTPS_CERTIFICATE_KEY_FILE=/opt/keycloak/conf/tls.key -v "$(pwd)/../InstallationHelper/local-dev/auth/certificates/localhost/localhost.crt:/opt/keycloak/conf/tls.crt:ro" -v "$(pwd)/../InstallationHelper/local-dev/auth/certificates/localhost/localhost.key:/opt/keycloak/conf/tls.key:ro" -v "$(pwd)/config/k8s/helm/infra/keycloak/files:/opt/keycloak/data/import:ro" quay.io/keycloak/keycloak:26.5.7 start-dev --import-realm
```

`start-dev` keeps everything in an in-memory database, so deleting the container throws the realm away and the next start re-imports it. That is what you want while iterating. The startup log ends with `Realm 'sky' imported` followed by `Import finished successfully`.

Confirm the discovery endpoint. Pass `-k` until you have trusted the issuing authority.

PowerShell:

```powershell
Invoke-RestMethod -SkipCertificateCheck -Uri "https://keycloak.test:9443/realms/sky/.well-known/openid-configuration" | Select-Object issuer
```

Unix shell:

```bash
curl -sk https://keycloak.test:9443/realms/sky/.well-known/openid-configuration
```

The `issuer` field must read `https://keycloak.test:9443/realms/sky`, which is the default the four services fall back to when `OAUTH2_ISSUER_URI` is unset.

---

### The cluster Keycloak imports itself

None of the import sections below apply to a Kubernetes cluster. The chart at [config/k8s/helm/infra/keycloak/](../k8s/helm/infra/keycloak/) ships the same realm file as a ConfigMap, mounts it at `/opt/keycloak/data/import`, and starts Keycloak with `--import-realm`, so the realm and its users exist on pod startup. The cluster runbook is [config/k8s/local_README.md](../k8s/local_README.md), and the cluster issuer is `http://keycloak.127.0.0.1.nip.io/realms/sky`, not the `keycloak.test` one.

It imports once, and only once. `--import-realm` skips a realm that already exists in Keycloak's database, and in the cluster that database is a PostgreSQL StatefulSet on the `keycloak-postgres-pvc` PersistentVolumeClaim which outlives the pod. So a change to the realm file reaches a fresh cluster and is silently ignored by one that has already booted: `helm upgrade` rolls a new pod, the new ConfigMap is mounted, the import runs, and it finds realm `sky` present and does nothing. Three ways to pick the change up, cheapest first:

1. Edit the same thing by hand in the admin console. Right for one value, such as adding a redirect URI.
2. Delete the realm, then let the import run again. Realm settings, Action, Delete realm, then restart the pod with `kubectl rollout restart deployment/keycloak-deployment`. Throws away anything created through the console.
3. Delete the Keycloak database volume. `kubectl delete statefulset keycloak-postgres` then `kubectl delete pvc keycloak-postgres-pvc`, then reinstall the chart. The full reset, and the only one that also clears Keycloak's own internal state.

The local `start-dev` container from the next section has none of this problem, because it keeps its database in memory and re-imports on every start.

---

### Obtain an admin token from the master realm

Every import and verification call below uses this token. It expires quickly, so re-run this step whenever you start getting 401 responses.

The `admin` and `admin` in both commands is the administrator of the throwaway container above. Against the shared local-dev stack, substitute whatever was bootstrapped on its database, and if nothing was, go back and do that first. The two instances answer on the same address, so nothing in the response tells you which one you reached.

PowerShell:

```powershell
$token = (Invoke-RestMethod -SkipCertificateCheck -Uri "https://keycloak.test:9443/realms/master/protocol/openid-connect/token" -Method Post -ContentType "application/x-www-form-urlencoded" -Body "grant_type=password&client_id=admin-cli&username=admin&password=admin").access_token
```

Unix shell:

```bash
TOKEN=$(curl -sk -d "grant_type=password&client_id=admin-cli&username=admin&password=admin" https://keycloak.test:9443/realms/master/protocol/openid-connect/token | python3 -c "import sys,json; print(json.load(sys.stdin)['access_token'])")
```

---

### Import the realm into a Keycloak that is already running

Use this when the instance was started without `--import-realm`, or when you deleted the realm and want it back.

PowerShell:

```powershell
Invoke-RestMethod -SkipCertificateCheck -Uri "https://keycloak.test:9443/admin/realms" -Method Post -Headers @{ Authorization = "Bearer $token" } -ContentType "application/json" -InFile "config\k8s\helm\infra\keycloak\files\sky-realm.json"
```

Unix shell:

```bash
curl -sk -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d @config/k8s/helm/infra/keycloak/files/sky-realm.json https://keycloak.test:9443/admin/realms
```

A 201 with an empty body means the realm was created. A 409 means it already exists, which is safe to ignore. There is no in-place overwrite: delete the realm first if you want a clean re-import.

---

### Import through kcadm.sh

An alternative when you have shell access to the container. Replace `keycloak` with your container name if it differs, and replace the `--user` and `--password` values with that instance's administrator.

```bash
docker cp config/k8s/helm/infra/keycloak/files/sky-realm.json keycloak:/tmp/sky-realm.json
```

```bash
docker exec keycloak /opt/keycloak/bin/kcadm.sh config credentials --server https://localhost:8443 --realm master --user admin --password admin
```

```bash
docker exec keycloak /opt/keycloak/bin/kcadm.sh create realms -f /tmp/sky-realm.json
```

---

### Import through the admin console

For a click-through import instead of the terminal:

1. Open https://keycloak.test:9443/admin and sign in as that instance's administrator, which is admin / admin for the throwaway container and whatever was bootstrapped for the shared local-dev stack.
2. Open the realm dropdown in the top left (it shows the current realm, usually "master") and click Create realm.
3. Under Resource file, click Browse and select [config/k8s/helm/infra/keycloak/files/sky-realm.json](../k8s/helm/infra/keycloak/files/sky-realm.json).
4. The realm name fills in as "sky" from the file. Click Create.
5. Verify the result under Realm settings, Clients, Realm roles, and Users.

To re-import over an existing realm, delete it first under Realm settings, Action, Delete realm, then repeat.

---

### Verify the realm

PowerShell:

```powershell
Invoke-RestMethod -SkipCertificateCheck -Uri "https://keycloak.test:9443/admin/realms/sky" -Headers @{ Authorization = "Bearer $token" } | Select-Object realm, enabled
```

Unix shell:

```bash
curl -sk -H "Authorization: Bearer $TOKEN" https://keycloak.test:9443/admin/realms/sky
```

Expected: realm `sky`, enabled true.

---

### Verify the sky-backend client

PowerShell:

```powershell
Invoke-RestMethod -SkipCertificateCheck -Uri "https://keycloak.test:9443/admin/realms/sky/clients" -Headers @{ Authorization = "Bearer $token" } | Where-Object { $_.clientId -eq "sky-backend" } | Select-Object clientId, enabled, secret
```

Unix shell:

```bash
curl -sk -H "Authorization: Bearer $TOKEN" "https://keycloak.test:9443/admin/realms/sky/clients?clientId=sky-backend"
```

---

### Verify realm roles

PowerShell:

```powershell
Invoke-RestMethod -SkipCertificateCheck -Uri "https://keycloak.test:9443/admin/realms/sky/roles" -Headers @{ Authorization = "Bearer $token" } | Select-Object name
```

Unix shell:

```bash
curl -sk -H "Authorization: Bearer $TOKEN" https://keycloak.test:9443/admin/realms/sky/roles
```

Expected roles include `admin` and `user`.

---

### Verify the demo users

PowerShell:

```powershell
Invoke-RestMethod -SkipCertificateCheck -Uri "https://keycloak.test:9443/admin/realms/sky/users" -Headers @{ Authorization = "Bearer $token" } | Select-Object username, email, enabled
```

Unix shell:

```bash
curl -sk -H "Authorization: Bearer $TOKEN" https://keycloak.test:9443/admin/realms/sky/users
```

Expected: `lukk`, `owner`, and `user`, matching the credentials table at the top of this document.

---

### Mint a token

This is the same password grant the Bruno collection runs in `auth/get-token.yml`, so if this works the collection works.

PowerShell:

```powershell
Invoke-RestMethod -SkipCertificateCheck -Uri "https://keycloak.test:9443/realms/sky/protocol/openid-connect/token" -Method Post -ContentType "application/x-www-form-urlencoded" -Body "grant_type=password&client_id=sky-backend&client_secret=dev-only-change-in-prod&username=owner&password=owner" | Select-Object access_token, token_type, expires_in
```

Unix shell:

```bash
curl -sk -d "grant_type=password&client_id=sky-backend&client_secret=dev-only-change-in-prod&username=owner&password=owner" https://keycloak.test:9443/realms/sky/protocol/openid-connect/token
```

A non-empty `access_token` confirms the realm issues tokens. `expires_in` is 300 seconds, which is why the collection re-runs the token request rather than caching one.

---

### Add a user

PowerShell:

```powershell
Invoke-RestMethod -SkipCertificateCheck -Uri "https://keycloak.test:9443/admin/realms/sky/users" -Method Post -Headers @{ Authorization = "Bearer $token" } -ContentType "application/json" -Body '{"username":"newuser","email":"newuser@sky.dev","enabled":true,"credentials":[{"type":"password","value":"newpass","temporary":false}]}'
```

Unix shell:

```bash
curl -sk -w "\n%{http_code}\n" -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d '{"username":"newuser","email":"newuser@sky.dev","enabled":true,"credentials":[{"type":"password","value":"newpass","temporary":false}]}' https://keycloak.test:9443/admin/realms/sky/users
```

HTTP 201 confirms the user was created. A user added this way disappears on the next container restart, because `start-dev` keeps no state. Add it to the realm file instead if you want it to survive.

---

### Reset a user's password

Look up the internal id first.

PowerShell:

```powershell
$userId = (Invoke-RestMethod -SkipCertificateCheck -Uri "https://keycloak.test:9443/admin/realms/sky/users?username=owner" -Headers @{ Authorization = "Bearer $token" })[0].id
```

Unix shell:

```bash
USER_ID=$(curl -sk -H "Authorization: Bearer $TOKEN" "https://keycloak.test:9443/admin/realms/sky/users?username=owner" | python3 -c "import sys,json; print(json.load(sys.stdin)[0]['id'])")
```

Then set the password.

PowerShell:

```powershell
Invoke-RestMethod -SkipCertificateCheck -Uri "https://keycloak.test:9443/admin/realms/sky/users/$userId/reset-password" -Method Put -Headers @{ Authorization = "Bearer $token" } -ContentType "application/json" -Body '{"type":"password","value":"newpass","temporary":false}'
```

Unix shell:

```bash
curl -sk -w "\n%{http_code}\n" -X PUT -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d '{"type":"password","value":"newpass","temporary":false}' "https://keycloak.test:9443/admin/realms/sky/users/$USER_ID/reset-password"
```

HTTP 204 confirms success.

---

### Trust the local development certificate authority

The Spring Security OAuth2 resource server fetches the JWKS from `https://keycloak.test:9443`. The certificate served there is a leaf issued by the local development certificate authority, so the JVM refuses the connection with a PKIX path validation error until that authority is trusted. Do not disable TLS validation in committed code, trust the authority explicitly instead.

Every service image already does this, the gateway included: each `docker/Dockerfile` imports [config/keycloak/certs/localhost-ca.crt](certs/localhost-ca.crt) under the alias `local-dev-ca` into the JRE `cacerts` store at build time, alongside the public certificate authorities that ship with the JRE. A container built from this repository therefore needs nothing extra, and that covers the whole Docker Compose stack.

That file is a copy of the authority the `local-dev` stack generates at `local-dev/auth/certificates/localhost/localhost-ca.crt`, regenerated by `generate-certificates.sh` in that directory. Only the authority is copied here, never either private key. Because the images trust the authority rather than the leaf, reissuing the leaf needs no image rebuild; reissuing the authority does.

The two approaches below are for a service started with `./gradlew bootRun` on the host, which runs on your own JDK and knows nothing about the image's `cacerts`. Even then you only need them when the host run is not under the `local` profile: under `local`, `LocalSecurityAutoConfiguration` in `sky-common` installs `UnverifiedJwtDecoder`, nothing ever fetches the JWKS, and the certificate never comes up. Approach B is the one to pick, because it is the only one that adds to the JDK's trust store rather than replacing it.

There used to be a third approach here: generate a PKCS12 truststore under [config/docker/certs/](../docker/certs/) and point the compose services at it with `-Djavax.net.ssl.trustStore` through `JAVA_TOOL_OPTIONS`. It is gone, and deliberately so. That property replaces the JVM trust store outright rather than adding to it, so those containers ended up trusting the local Keycloak certificate and no public authority at all, which breaks every outbound TLS call to anything else. [config/docker/docker-compose.yaml](../docker/docker-compose.yaml) no longer mounts a truststore or sets `JAVA_TOOL_OPTIONS`, and neither should be reintroduced. The leftover `config/docker/certs/sky-truststore.p12` on your disk is unused and gitignored; nothing reads it.

#### Approach A, a local truststore passed to the JVM

This is the same `-Djavax.net.ssl.trustStore` property the compose stack no longer uses, and it carries the same cost: the store you point at replaces the JDK's, so that JVM trusts Keycloak and no public authority. Acceptable for a single service you are debugging against local infrastructure, wrong for anything that calls out to the internet.

Extract the certificate from the running Keycloak. PowerShell:

```powershell
'' | openssl s_client -connect keycloak.test:9443 -showcerts 2>$null | openssl x509 -outform PEM -out keycloak-local.crt
```

Unix shell:

```bash
openssl s_client -connect keycloak.test:9443 -showcerts </dev/null 2>/dev/null | openssl x509 -outform PEM -out keycloak-local.crt
```

You can skip that step entirely and use the committed [config/keycloak/certs/localhost-ca.crt](certs/localhost-ca.crt), which is the authority that issued the certificate the container serves. Trusting the authority also covers a reissued leaf.

Import it into a new truststore. The password is your choice, the examples use `changeit`.

PowerShell:

```powershell
keytool -importcert -noprompt -alias local-dev-ca -file config\keycloak\certs\localhost-ca.crt -keystore local-truststore.jks -storepass changeit
```

Unix shell:

```bash
keytool -importcert -noprompt -alias local-dev-ca -file config/keycloak/certs/localhost-ca.crt -keystore local-truststore.jks -storepass changeit
```

Point the service at it at startup. The profile is left off on purpose: under `local` there would be no JWKS fetch to secure. PowerShell:

```powershell
.\gradlew.bat :sky-offer:bootRun -Djavax.net.ssl.trustStore=local-truststore.jks -Djavax.net.ssl.trustStorePassword=changeit
```

Unix shell:

```bash
./gradlew :sky-offer:bootRun -Djavax.net.ssl.trustStore=local-truststore.jks -Djavax.net.ssl.trustStorePassword=changeit
```

#### Approach B, the JDK cacerts store

Trusts the certificate for every process on that JDK, so no JVM arguments are needed afterwards. Locate the store first.

PowerShell:

```powershell
$cacerts = Join-Path ((Get-Command java).Source | Split-Path | Split-Path) "lib\security\cacerts"
```

Unix shell:

```bash
CACERTS="$(dirname "$(dirname "$(readlink -f "$(which java)")")")/lib/security/cacerts"
```

Import into it. The default store password is `changeit`.

PowerShell, run as Administrator:

```powershell
keytool -importcert -noprompt -alias local-dev-ca -file config\keycloak\certs\localhost-ca.crt -keystore $cacerts -storepass changeit
```

Unix shell:

```bash
sudo keytool -importcert -noprompt -alias local-dev-ca -file config/keycloak/certs/localhost-ca.crt -keystore "$CACERTS" -storepass changeit
```

#### Docker Compose needs no pre-step

The images trust the certificate already, so there is nothing to generate first.

```bash
docker compose -f config/docker/docker-compose.yaml up --build -d
```

---

### Verify a service can reach the JWKS endpoint

Once a service is running with the certificate trusted, its readiness probe is the end-to-end check.

PowerShell:

```powershell
Invoke-RestMethod -Uri "http://localhost:5552/actuator/health/readiness" | Select-Object status
```

Unix shell:

```bash
curl -s http://localhost:5552/actuator/health/readiness
```

Expected: `{"status":"UP"}`. If it is DOWN and the logs show `PKIX path validation failed`, the truststore has not taken effect, go back to the trust step.

---

### How the services use the realm

Each service reads `OAUTH2_ISSUER_URI` and validates the JWT itself as an OAuth2 resource server. The application defaults point at `https://keycloak.test:9443/realms/sky`, so a bare local run needs no environment variable. Helm overlays inject the cluster or production issuer. Set `OAUTH2_AUDIENCE=sky-backend` to additionally enforce the audience claim that the realm's audience mapper writes.

The environment variable name is deliberately provider-neutral, so pointing the stack at a different OIDC provider is a configuration change rather than a code change.

---

### Docs map

| Document | What it covers |
|---|---|
| [README.md](../../README.md) | Platform overview, modules, build, ports |
| [config/local-dev/local_README.md](../local-dev/local_README.md) | Running locally without Kubernetes: Gradle and Docker Compose |
| [config/local-dev/e2e-stack_README.md](../local-dev/e2e-stack_README.md) | The self-contained Compose stack and the Bruno gate CI runs on it |
| [config/k8s/local_README.md](../k8s/local_README.md) | Local Kubernetes cluster on k3d: bring-up, verification, teardown |
| [config/k8s/helm/helm_README.md](../k8s/helm/helm_README.md) | Chart-by-chart reference, secret key inventory, upgrades |
| [config/k8s/_deployment-scripts/deployment_README.md](../k8s/_deployment-scripts/deployment_README.md) | Deploying to the GCP cluster, sealed secrets, deployment scripts |
| [docs/api/README.md](../../docs/api/README.md) | Bruno collection and OpenAPI specs |
