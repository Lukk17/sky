# booking-flow: e2e test

## What this verifies

- POST `/offer/api/owner/offers` returns HTTP 201 with a server-assigned UUID `id` and `ownerEmail` equal to
  `lukk@sky.dev` (the offer this flow books against).
- POST `/booking/api/bookings` with `{offerId, dateToBook}` returns HTTP 201 with a server-assigned UUID `id`,
  the booked `offerId`, a `bookedDate`, and `bookingUser` equal to `lukk@sky.dev`.
- GET `/booking/api/user/bookings` returns HTTP 200 and the created booking appears in the user's page (persisted
  state in Postgres `sky.booking`).
- GET `/offer/api/offers/{offerId}/owner` returns HTTP 200 with body `lukk@sky.dev` (the owner sky-booking resolves
  internally).
- DELETE `/booking/api/bookings/{bookingId}` returns HTTP 204 and the booking no longer appears in the user's page
  (persisted-state removal).
- DELETE `/offer/api/owner/offers/{offerId}` returns HTTP 204 (teardown of the seeded offer).

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
cd docs/api/request && bru run auth offer booking teardown/delete-booking.yml teardown/delete-offer.yml --env local --insecure
```

The auth folder mints the token, the offer and booking folders run the create/read/assert requests with IDs chained
automatically by the collection's scripts, and the teardown requests delete the booking and then the seeded offer.
Bruno evaluates every assertion in each request.

## Expected

The run summary reports Status PASS with all requests passed and all assertions passed: 52/52 assertions.

The assertion groups cover: the seeded offer returns HTTP 201 with a UUID `id` and `ownerEmail` equal to
`lukk@sky.dev`; the offer folder includes the photo upload, so the presigned-URL assertion (`photoUrl` contains
`X-Amz-Algorithm`) is part of the run; the booking returns HTTP 201 with a UUID `id`, the booked `offerId`, a
`bookedDate` carrying `2027-08-15`, and `bookingUser` equal to `lukk@sky.dev` (and a Kafka event consumed by
sky-notify); the user bookings page returns the booking (persisted to Postgres `sky.booking`); the owner lookup
returns HTTP 200 with body `lukk@sky.dev`; and the two teardown deletes return HTTP 204, removing the booking from
Postgres `sky.booking` and the seeded offer from Postgres `sky.offer`.

## Fixtures

- `docs/api/request/sample.png` — the canary image carrying the text `SKY E2E CANARY` (identical to
  `e2e/fixtures/offer-photo.png`). The offer folder's photo-upload request sends it; a passing presigned-URL
  assertion proves the byte stream reached MinIO rather than a memorised placeholder.

## Concurrency

- Mutates: Postgres `sky.booking`, rows where `booking_user = lukk@sky.dev`; Postgres `sky.offer`, the seeded canary
  offer owned by `lukk@sky.dev` (created and deleted here); Kafka, booking and offer events this flow produces and
  sky-notify consumes.
- Conflicts with: `2-offer-crud-test.md`, which also creates and deletes offers owned by `lukk@sky.dev` in
  `sky.offer`. The overlapping Mutates already forces serialisation; this names it for clarity.
- Serial: false.
