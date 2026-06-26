# offer-crud: e2e test

## What this verifies

- POST `/offer/api/owner/offers` returns HTTP 201 with a server-assigned UUID `id`, `ownerEmail` equal to
  `lukk@sky.dev`, and the submitted `hotelName`, `city`, `country`, `price`, and `roomCapacity` echoed.
- GET `/offer/api/offers` (public) returns HTTP 200 with a paginated body (`content` array and numeric
  `totalElements`).
- POST `/offer/api/search` with the canary token returns HTTP 200 and the created offer appears in `content`
  (retrieval by `hotelName` LIKE).
- GET `/offer/api/owner/offers` returns HTTP 200 and the created offer appears in the owner's page.
- GET `/offer/api/offers/{id}/owner` returns HTTP 200 with body `lukk@sky.dev`.
- POST `/offer/api/owner/offers/{id}/photo` (multipart) returns HTTP 200 with a UUID `id` and a non-empty
  `photoUrl` presigned URL pointing at the `sky-offers` object store (persisted state in MinIO and `offer_photo`).
- PUT `/offer/api/owner/offers` returns HTTP 200; a follow-up read reflects the edited `hotelName` and `price`.
- DELETE `/offer/api/owner/offers/{id}` returns HTTP 204 and the offer no longer appears in the owner's page
  (persisted-state removal in Postgres `sky.offer` / `sky.offer_photo`).

## Prerequisites

Two checks: the Bruno CLI is installed and the gateway is reachable.

Check the Bruno CLI is installed.

```bash
bru --version
```

Expect a version number and exit code 0.

Check the gateway is reachable.

```bash
curl -s -o /dev/null -w '%{http_code}\n' http://localhost:5777/actuator/health
```

Expect `200`.

## Reset state

None. The run below creates its own data and deletes it in the teardown requests, so it is self-cleaning and
re-runnable.

## Run

One step: a single Bruno invocation from the collection directory.

```bash
cd docs/api/request && bru run auth offer teardown/delete-offer.yml --env local --insecure
```

The auth folder mints the token, the offer folder runs the create/read/assert requests (including the photo upload)
with IDs chained automatically by the collection's scripts, and the teardown request deletes what was created. Bruno
evaluates every assertion in each request.

## Expected

The run summary reports Status PASS with all requests passed and all assertions passed: 39/39 assertions.

The assertion groups cover: the created offer returns HTTP 201 with a UUID `id`, `ownerEmail` equal to
`lukk@sky.dev`, and the submitted `hotelName`, `city`, `country`, `price`, and `roomCapacity`; the public offer list
returns HTTP 200 with a `content` array and a numeric `totalElements`; the search returns the offer by `hotelName`
LIKE; the owner page returns the offer (persisted to Postgres `sky.offer`); the owner lookup returns HTTP 200 with
body `lukk@sky.dev`; the photo upload returns HTTP 200 with a UUID `id` and a non-empty presigned `photoUrl`
referencing the `sky-offers` object store (MinIO and `offer_photo`); the edit returns HTTP 200 and a follow-up read
reflects the renamed `hotelName` and reprice; and the teardown delete returns HTTP 204 (persisted-state removal in
Postgres `sky.offer` / `sky.offer_photo` and the `sky-offers` bucket).

## Fixtures

- `docs/api/request/sample.png` — the canary image carrying the text `SKY E2E CANARY` (identical to
  `e2e/fixtures/offer-photo.png`). The photo-upload request sends it; a passing presigned-URL assertion proves the
  byte stream reached MinIO rather than a memorised placeholder.

## Concurrency

- Mutates: Postgres `sky.offer` and `sky.offer_photo`, rows owned by `lukk@sky.dev`; MinIO bucket `sky-offers`,
  objects for `lukk@sky.dev` offers (the canary offer and its photo this test creates and deletes).
- Conflicts with: `3-booking-flow-test.md`, which also creates and deletes offers owned by `lukk@sky.dev` in
  `sky.offer`. The overlapping Mutates already forces serialisation; this names it for clarity.
- Serial: false.
