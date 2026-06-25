# Keycloak realm (realm-as-code)

`sky-realm.json` is the single source of truth for the `sky` Keycloak realm. It is imported
into the running Keycloak locally and, for the cluster, synced into the Keycloak Helm chart at
deploy time (the `helm-app-deploy` script copies it to `config/k8s/helm/infra/keycloak/files/`,
which is gitignored), so the realm comes up pre-configured. Edit only this file; never edit the
generated chart copy.

### What it defines

- Realm `sky` (login with email allowed, registration disabled).
- Client `sky-backend` (confidential): standard flow + direct access grants
  (password grant) + service accounts (client credentials). It carries an
  audience mapper so every access token's `aud` includes `sky-backend`, which the
  services validate when `OAUTH2_AUDIENCE=sky-backend` is set.
- Two demo users whose emails match the seed data:
  - `owner` / `owner` (`owner@sky.dev`)
  - `user` / `user` (`user@sky.dev`)

The client secret in the file (`dev-only-change-in-prod`) is a local-development
default. In the cluster the real secret lives in the sealed secret; rotate it there
and update the client after import.

### Import into a running Keycloak (local)

Copy the realm file into the container:

```shell
docker cp config/keycloak/sky-realm.json keycloak:/tmp/sky-realm.json
```

Authenticate kcadm against the running server:

```shell
docker exec keycloak /opt/keycloak/bin/kcadm.sh config credentials --server http://localhost:8080 --realm master --user admin --password admin
```

Create the realm from the file:

```shell
docker exec keycloak /opt/keycloak/bin/kcadm.sh create realms -f /tmp/sky-realm.json
```

For a fresh Keycloak you can instead start it with the import flag and the file
mounted under `/opt/keycloak/data/import/`:

```shell
docker run -p 8080:8080 -e KEYCLOAK_ADMIN=admin -e KEYCLOAK_ADMIN_PASSWORD=admin -v "$(pwd)/config/keycloak:/opt/keycloak/data/import" quay.io/keycloak/keycloak:26.0 start-dev --import-realm
```

### Mint a token (for the Bruno collection or manual testing)

```shell
curl -s -X POST http://localhost:8080/realms/sky/protocol/openid-connect/token -d grant_type=password -d client_id=sky-backend -d client_secret=dev-only-change-in-prod -d username=owner -d password=owner
```

Take the `access_token` from the response and paste it into the Bruno environment
variable `bearerToken`.

### Services

Each service reads `OAUTH2_ISSUER_URI` (for example
`http://localhost:8080/realms/sky` locally, `https://keycloak.luksarna.com/realms/sky`
in prod) and validates the JWT. Set `OAUTH2_AUDIENCE=sky-backend` to additionally
enforce the audience claim.
