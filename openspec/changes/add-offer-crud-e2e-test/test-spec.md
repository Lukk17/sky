# offer-crud: e2e test

## What this verifies

- POST `/offer/api/owner/offers` returns HTTP 201 with a server-assigned numeric `id`, `ownerEmail` equal to
  `lukk@sky.dev`, and the submitted `hotelName`, `city`, `country`, `price`, and `roomCapacity` echoed.
- GET `/offer/api/offers` (public) returns HTTP 200 with a paginated body (`content` array and numeric
  `totalElements`).
- POST `/offer/api/search` with the canary token returns HTTP 200 and the created offer appears in `content`
  (retrieval by `hotelName` LIKE).
- GET `/offer/api/owner/offers` returns HTTP 200 and the created offer appears in the owner's page.
- GET `/offer/api/offers/{id}/owner` returns HTTP 200 with body `lukk@sky.dev`.
- POST `/offer/api/owner/offers/{id}/photo` (multipart) returns HTTP 200 with a numeric `id` and a non-empty
  `photoUrl` presigned URL pointing at the `sky-offers` object store (persisted state in MinIO and `offer_photo`).
- PUT `/offer/api/owner/offers` returns HTTP 200; a follow-up read reflects the edited `hotelName` and `price`.
- DELETE `/offer/api/owner/offers/{id}` returns HTTP 204 and the offer no longer appears in the owner's page
  (persisted-state removal in Postgres `sky.offer` / `sky.offer_photo`).

## Prerequisites

The runner needs curl and jq on PATH, a reachable gateway, a Keycloak token for user lukk, and the fixture image on
disk. Run all commands from the repository root so the fixture relative path resolves.

Check curl is installed.

```bash
curl --version
```

Expect a version banner and exit code 0.

Check jq is installed.

```bash
jq --version
```

Expect a version string such as `jq-1.7` and exit code 0.

Check the gateway is reachable through its public offer endpoint.

```bash
curl -s -o /dev/null -w '%{http_code}\n' 'http://localhost:5777/offer/api/offers?page=0&size=1'
```

Expect `200`.

Check the fixture image is present and is a real PNG.

```bash
head -c 8 e2e/fixtures/offer-photo.png | od -An -tx1
```

Expect the PNG magic bytes `89 50 4e 47 0d 0a 1a 0a`.

Obtain a Keycloak access token for user lukk and export it.

```bash
export TOKEN=$(curl -sk -X POST https://keycloak.test:9443/realms/sky/protocol/openid-connect/token -d grant_type=password -d client_id=sky-backend -d client_secret=dev-only-change-in-prod -d username=lukk -d password=test1234 | jq -r .access_token)
```

Expect `$TOKEN` to be a non-empty JWT. Verify with the next command.

```bash
printf '%s' "$TOKEN" | grep -Eq '^[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+$' && echo token-ok
```

Expect `token-ok`.

## Reset state

Remove any canary offers left by a previous aborted run. Deleting an offer also removes its `offer_photo` rows and
the photo object in the `sky-offers` bucket. This lists the owner's offers and deletes those whose hotel name carries
the canary marker.

```bash
curl -s 'http://localhost:5777/offer/api/owner/offers?page=0&size=100' -H "Authorization: Bearer $TOKEN" | jq -r '.content[] | select(.hotelName | startswith("SKY-E2E-OFFER-CANARY")) | .id' | while read -r id; do curl -s -o /dev/null -X DELETE "http://localhost:5777/offer/api/owner/offers/$id" -H "Authorization: Bearer $TOKEN"; done
```

## Run

Each step is one API call. Wait for the documented success status before moving to the next step.

1. Create the canary offer and capture its id. Wait for HTTP 201.

```bash
export OFFER_ID=$(curl -s -X POST 'http://localhost:5777/offer/api/owner/offers' -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' -H 'Accept: application/json' -d '{"hotelName":"SKY-E2E-OFFER-CANARY Hotel 7F3A","description":"Automated e2e canary offer, please ignore.","comment":"Breakfast included.","price":249.99,"roomCapacity":2,"city":"Paris","country":"France","photoPath":"/images/canary.jpg"}' | jq -r .id)
```

2. List all offers (public). Wait for HTTP 200.

```bash
curl -s -w '\n%{http_code}\n' 'http://localhost:5777/offer/api/offers?page=0&size=20' -H 'Accept: application/json'
```

3. Search for the canary token and confirm the created offer is returned. Wait for HTTP 200.

```bash
curl -s 'http://localhost:5777/offer/api/search' -H 'Content-Type: text/plain' -H 'Accept: application/json' -d 'SKY-E2E-OFFER-CANARY' | jq --arg id "$OFFER_ID" '.content[] | select((.id|tostring) == $id) | {id, hotelName}'
```

4. List the owner's offers and confirm the created offer is present. Wait for HTTP 200.

```bash
curl -s 'http://localhost:5777/offer/api/owner/offers?page=0&size=100' -H "Authorization: Bearer $TOKEN" | jq --arg id "$OFFER_ID" '[.content[] | select((.id|tostring) == $id)] | length'
```

5. Resolve the offer owner. Wait for HTTP 200.

```bash
curl -s -w '\n%{http_code}\n' "http://localhost:5777/offer/api/offers/$OFFER_ID/owner" -H "Authorization: Bearer $TOKEN" -H 'Accept: application/json'
```

6. Upload the canary photo to the offer. Wait for HTTP 200.

```bash
curl -s -w '\n%{http_code}\n' -X POST "http://localhost:5777/offer/api/owner/offers/$OFFER_ID/photo" -H "Authorization: Bearer $TOKEN" -H 'Accept: application/json' -F 'file=@e2e/fixtures/offer-photo.png;type=image/png'
```

7. Edit the offer (rename and reprice). Wait for HTTP 200.

```bash
curl -s -w '\n%{http_code}\n' -X PUT 'http://localhost:5777/offer/api/owner/offers' -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' -H 'Accept: application/json' -d "{\"id\":$OFFER_ID,\"hotelName\":\"SKY-E2E-OFFER-CANARY Hotel 7F3A (Renovated)\",\"description\":\"Freshly renovated canary offer.\",\"comment\":\"Breakfast and spa access included.\",\"price\":299.99,\"roomCapacity\":2,\"city\":\"Paris\",\"country\":\"France\",\"photoPath\":\"/images/canary-new.jpg\"}"
```

8. Confirm the edit persisted by reading the owner's page. Wait for HTTP 200.

```bash
curl -s 'http://localhost:5777/offer/api/owner/offers?page=0&size=100' -H "Authorization: Bearer $TOKEN" | jq --arg id "$OFFER_ID" '.content[] | select((.id|tostring) == $id) | {hotelName, price}'
```

9. Delete the offer. Wait for HTTP 204.

```bash
curl -s -o /dev/null -w '%{http_code}\n' -X DELETE "http://localhost:5777/offer/api/owner/offers/$OFFER_ID" -H "Authorization: Bearer $TOKEN"
```

10. Confirm the offer is gone from the owner's page.

```bash
curl -s 'http://localhost:5777/offer/api/owner/offers?page=0&size=100' -H "Authorization: Bearer $TOKEN" | jq --arg id "$OFFER_ID" '[.content[] | select((.id|tostring) == $id)] | length'
```

## Expected

Step 1 returns HTTP 201. The response body has a numeric `id`, `ownerEmail` equal to `lukk@sky.dev`, `hotelName`
equal to `SKY-E2E-OFFER-CANARY Hotel 7F3A`, `city` `Paris`, `country` `France`, `roomCapacity` `2`, and `price`
`249.99`. `OFFER_ID` is a non-empty number.

Step 2 returns HTTP 200 with a `content` array and a numeric `totalElements`.

Step 3 returns HTTP 200 and prints exactly one object whose `id` equals `OFFER_ID` and whose `hotelName` is the
canary name, confirming the offer is retrievable through public search by `hotelName` LIKE.

Step 4 prints `1`, confirming the offer is in the owner's page (persisted to Postgres `sky.offer`).

Step 5 returns HTTP 200 with body `lukk@sky.dev`.

Step 6 returns HTTP 200. The response body has a numeric `id` and a non-empty `photoUrl`. `photoUrl` is a presigned
URL referencing the `sky-offers` object store, confirming the photo persisted to MinIO and `offer_photo`.

Step 7 returns HTTP 200.

Step 8 prints `hotelName` equal to `SKY-E2E-OFFER-CANARY Hotel 7F3A (Renovated)` and `price` `299.99`, confirming
the edit persisted.

Step 9 returns HTTP 204.

Step 10 prints `0`, confirming the offer and its photo were removed (persisted-state removal in Postgres `sky.offer`
/ `sky.offer_photo` and the `sky-offers` bucket).

## Fixtures

- `e2e/fixtures/offer-photo.png` — a 16x16 PNG whose `tEXt` chunk carries `SKY-E2E-CANARY-OFFER-PHOTO-7F3A`. The PNG
  magic bytes make the offer service photo-upload validator accept it; the canary string is unique enough that a
  passing upload proves the byte stream reached MinIO rather than a memorised placeholder.

## Concurrency

- Mutates: Postgres `sky.offer` and `sky.offer_photo`, rows owned by `lukk@sky.dev`; MinIO bucket `sky-offers`,
  objects for `lukk@sky.dev` offers (the canary offer and its photo this test creates and deletes).
- Conflicts with: `3-booking-flow-test.md`, which also creates and deletes offers owned by `lukk@sky.dev` in
  `sky.offer`. The overlapping Mutates already forces serialisation; this names it for clarity.
- Serial: false.
