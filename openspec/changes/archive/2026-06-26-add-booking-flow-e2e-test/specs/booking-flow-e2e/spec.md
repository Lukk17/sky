## ADDED Requirements

### Requirement: Booking flow capability has a behaviour-only e2e runbook

The suite MUST carry an immutable spec at `e2e/testing/3-booking-flow-test.md` and its run-record template at
`e2e/testing/templates/3-booking-flow-tasks.template.md` that drive the cross-service booking flow (seed an offer in
sky-offer, book it in sky-booking, list the user's bookings, resolve the offer owner, delete the booking, delete the
offer) for the authenticated user lukk@sky.dev through the gateway at `http://localhost:5777`. Assertions MUST be
observable behaviour only: HTTP status codes, response body content, and persisted state in Postgres `sky.booking`
and `sky.offer`. The runbook MUST NOT assert on log substrings.

#### Scenario: Book a seeded offer then tear both down

- **WHEN** the runner seeds a canary offer as lukk@sky.dev, books it for a future date, then deletes the booking and
  the offer
- **THEN** the offer create returns HTTP 201 with `ownerEmail` `lukk@sky.dev`, the booking create returns HTTP 201
  with the booked `offerId` and `bookingUser` `lukk@sky.dev`, the booking appears in the user's page, the offer
  owner lookup returns `lukk@sky.dev`, the booking delete returns HTTP 204 and the booking no longer appears, and the
  offer delete returns HTTP 204
