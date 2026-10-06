# booking-flow: e2e capability test proposal

## Why

Booking is the cross-service flow with the highest setup cost: sky-booking (port 5555) cannot book against nothing,
so the test must first stand up an offer in sky-offer, then book it, and sky-booking resolves the offer owner over
an internal REST call while emitting Kafka events that sky-notify consumes. The captured Bruno `responses.json`
shows the authenticated booking calls returning 401, so nothing today proves the authenticated create-book-resolve
path end to end, nor that a deleted booking actually leaves the user's bookings. This test pins the dependent flow
for lukk@sky.dev and tears down both the booking and the seeded offer.

## What this will verify

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
- DELETE `/offer/api/owner/offers/{offerId}` returns HTTP 204 (cleanup of the seeded offer).

## Setup cost class

- [ ] 1: No state to reset, no fixtures, hits a single endpoint or MCP tool.
- [ ] 2: Single fixture upload OR vision-capable model OR PDF parsing.
- [ ] 3: Single-service reset (e.g. Redis only).
- [x] 4: Multi-service reset (DB + Redis + Qdrant + MinIO).
- [ ] 5: Seeded state + observation of an async background process.

This test seeds an offer in one service to exercise another, resets two Postgres tables across two services, and
produces Kafka events as a side effect, so it is the most expensive test in the suite to set up and runs last.

## Fixtures needed

- None. This flow creates the offer with inline canary fields and does not upload a photo; the offer photo path is a
  plain string. The MinIO bucket is therefore not touched by this test.

## Concurrency profile

- Mutates: Postgres `sky.booking`, rows where `booking_user = lukk@sky.dev`; Postgres `sky.offer`, the seeded canary
  offer owned by `lukk@sky.dev` (created and deleted here); Kafka, booking and offer events this flow produces and
  sky-notify consumes.
- Conflicts with: `2-offer-crud-test.md`, which also creates and deletes offers owned by `lukk@sky.dev` in
  `sky.offer`. The overlapping Mutates already forces serialisation; this names it for clarity.
- Serial: false.

## API client invocation

- Token: `docs/api/request/auth/get-token.yml`, or the equivalent curl shown in the spec.
- Flow: `docs/api/request/offer/create-offer.yml`, `docs/api/request/booking/create-booking.yml`,
  `get-user-bookings.yml`, `docs/api/request/offer/get-offer-owner.yml`, `booking/delete-booking.yml`, and
  `offer-cleanup/delete-offer.yml`. The spec drives the same endpoints with self-contained curl through the gateway.

## Number assignment

N reflects relative setup cost across the suite (lowest first). Booking-flow is the most expensive: it spans two
services, depends on a seeded offer, and produces Kafka events, so it takes the highest prefix.

N: 3

## Next steps

After this proposal is approved:

- Generate `e2e/testing/3-booking-flow-test.md` from the `test-spec` artifact.
- Generate `e2e/testing/templates/3-booking-flow-tasks.template.md` from the `tasks-template` artifact.
- Each execution generates a `run` record under `e2e/testing/runs/`.
