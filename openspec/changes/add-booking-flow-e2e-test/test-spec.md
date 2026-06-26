# booking-flow: e2e test

## What this verifies

- POST `/offer/api/owner/offers` returns HTTP 201 with a server-assigned numeric `id` and `ownerEmail` equal to
  `lukk@sky.dev` (the offer this flow books against).
- POST `/booking/api/bookings` with `{offerId, dateToBook}` returns HTTP 201 with a server-assigned numeric `id`,
  the booked `offerId`, a `bookedDate`, and `bookingUser` equal to `lukk@sky.dev`.
- GET `/booking/api/user/bookings` returns HTTP 200 and the created booking appears in the user's page (persisted
  state in Postgres `sky.booking`).
- GET `/offer/api/offers/{offerId}/owner` returns HTTP 200 with body `lukk@sky.dev` (the owner sky-booking resolves
  internally).
- DELETE `/booking/api/bookings/{bookingId}` returns HTTP 204 and the booking no longer appears in the user's page
  (persisted-state removal).
- DELETE `/offer/api/owner/offers/{offerId}` returns HTTP 204 (teardown of the seeded offer).

## Prerequisites

The runner needs curl and jq on PATH, a reachable gateway, and a Keycloak token for user lukk. Both sky-offer and
sky-booking must be up because the flow seeds an offer in one and books it in the other.

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

Remove leftovers from a previous aborted run, bookings first (so an offer with a dangling booking can be deleted),
then the seeded canary offer. Bookings are matched by the canary `bookedDate`; offers by the canary hotel name.

```bash
curl -s 'http://localhost:5777/booking/api/user/bookings?page=0&size=100' -H "Authorization: Bearer $TOKEN" | jq -r '.content[] | select((.bookedDate|tostring) | startswith("2027-08-15")) | .id' | while read -r id; do curl -s -o /dev/null -X DELETE "http://localhost:5777/booking/api/bookings/$id" -H "Authorization: Bearer $TOKEN"; done
```

```bash
curl -s 'http://localhost:5777/offer/api/owner/offers?page=0&size=100' -H "Authorization: Bearer $TOKEN" | jq -r '.content[] | select(.hotelName | startswith("SKY-E2E-BOOK-CANARY")) | .id' | while read -r id; do curl -s -o /dev/null -X DELETE "http://localhost:5777/offer/api/owner/offers/$id" -H "Authorization: Bearer $TOKEN"; done
```

## Run

Each step is one API call. Wait for the documented success status before moving to the next step.

1. Create the canary offer to book against and capture its id. Wait for HTTP 201.

```bash
export OFFER_ID=$(curl -s -X POST 'http://localhost:5777/offer/api/owner/offers' -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' -H 'Accept: application/json' -d '{"hotelName":"SKY-E2E-BOOK-CANARY Hotel 7F3A","description":"Automated e2e booking canary offer, please ignore.","comment":"Breakfast included.","price":249.99,"roomCapacity":2,"city":"Paris","country":"France","photoPath":"/images/canary.jpg"}' | jq -r .id)
```

2. Create a booking against the offer and capture its id. Wait for HTTP 201.

```bash
export BOOKING_ID=$(curl -s -X POST 'http://localhost:5777/booking/api/bookings' -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' -H 'Accept: application/json' -d "{\"offerId\":\"$OFFER_ID\",\"dateToBook\":\"2027-08-15\"}" | jq -r .id)
```

3. List the user's bookings and confirm the created booking is present. Wait for HTTP 200.

```bash
curl -s 'http://localhost:5777/booking/api/user/bookings?page=0&size=100' -H "Authorization: Bearer $TOKEN" | jq --arg id "$BOOKING_ID" '[.content[] | select((.id|tostring) == $id)] | length'
```

4. Resolve the offer owner. Wait for HTTP 200.

```bash
curl -s -w '\n%{http_code}\n' "http://localhost:5777/offer/api/offers/$OFFER_ID/owner" -H "Authorization: Bearer $TOKEN" -H 'Accept: application/json'
```

5. Delete the booking. Wait for HTTP 204.

```bash
curl -s -o /dev/null -w '%{http_code}\n' -X DELETE "http://localhost:5777/booking/api/bookings/$BOOKING_ID" -H "Authorization: Bearer $TOKEN"
```

6. Confirm the booking is gone from the user's page.

```bash
curl -s 'http://localhost:5777/booking/api/user/bookings?page=0&size=100' -H "Authorization: Bearer $TOKEN" | jq --arg id "$BOOKING_ID" '[.content[] | select((.id|tostring) == $id)] | length'
```

7. Delete the seeded offer. Wait for HTTP 204.

```bash
curl -s -o /dev/null -w '%{http_code}\n' -X DELETE "http://localhost:5777/offer/api/owner/offers/$OFFER_ID" -H "Authorization: Bearer $TOKEN"
```

## Expected

Step 1 returns HTTP 201. The response body has a numeric `id` and `ownerEmail` equal to `lukk@sky.dev`. `OFFER_ID`
is a non-empty number.

Step 2 returns HTTP 201. The response body has a numeric `id`, an `offerId` equal to `OFFER_ID`, a `bookedDate`
carrying `2027-08-15`, and `bookingUser` equal to `lukk@sky.dev`. `BOOKING_ID` is a non-empty number. Creating the
booking produces a Kafka event consumed by sky-notify.

Step 3 prints `1`, confirming the booking persisted to the user's collection in Postgres `sky.booking`.

Step 4 returns HTTP 200 with body `lukk@sky.dev`, confirming the offer owner sky-booking resolves matches the
creator.

Step 5 returns HTTP 204.

Step 6 prints `0`, confirming the booking was removed from the user's collection (persisted-state removal in
Postgres `sky.booking`).

Step 7 returns HTTP 204, tearing down the seeded offer in Postgres `sky.offer`.

## Fixtures

- None. The offer is created with inline canary fields (`SKY-E2E-BOOK-CANARY Hotel 7F3A`) and no photo is uploaded,
  so this test reads no file and does not touch the MinIO `sky-offers` bucket.

## Concurrency

- Mutates: Postgres `sky.booking`, rows where `booking_user = lukk@sky.dev`; Postgres `sky.offer`, the seeded canary
  offer owned by `lukk@sky.dev` (created and deleted here); Kafka, booking and offer events this flow produces and
  sky-notify consumes.
- Conflicts with: `2-offer-crud-test.md`, which also creates and deletes offers owned by `lukk@sky.dev` in
  `sky.offer`. The overlapping Mutates already forces serialisation; this names it for clarity.
- Serial: false.
