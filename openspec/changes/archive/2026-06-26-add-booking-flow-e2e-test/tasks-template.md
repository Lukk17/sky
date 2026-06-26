# booking-flow: run tasks template

Spec: [`3-booking-flow-test.md`](3-booking-flow-test.md)

Copy this file to `runs/<UTC-timestamp>_3-booking-flow-tasks.md` before
starting a run. Tick boxes as you go. Add anything you did beyond the spec
under Additional tasks I did.

## Tasks

### Prerequisites

- [ ] Bruno CLI is installed (`bru --version` prints a version, exits 0).
- [ ] Gateway reachable: `/actuator/health` returns 200.

### Reset state

- [ ] None. The run creates its own data and deletes it in the teardown requests (self-cleaning, re-runnable).

### Run

- [ ] Single Bruno run: `cd docs/api/request && bru run auth offer booking teardown/delete-booking.yml teardown/delete-offer.yml --env local --insecure`.

### Expected

- [ ] Run summary reports Status PASS, all requests passed, all assertions passed: 52/52.
- [ ] Seeded offer returns HTTP 201 with UUID `id` and `ownerEmail` `lukk@sky.dev`.
- [ ] Offer folder photo upload returns the presigned `photoUrl` (contains `X-Amz-Algorithm`).
- [ ] Booking returns HTTP 201 with UUID `id`, `offerId` equal to `OFFER_ID`, a `bookedDate` carrying `2027-08-15`, and `bookingUser` `lukk@sky.dev` (Kafka event consumed by sky-notify).
- [ ] User bookings page returns the booking (Postgres `sky.booking`).
- [ ] Owner lookup returns HTTP 200 with body `lukk@sky.dev`.
- [ ] Teardown deletes return HTTP 204, removing the booking from `sky.booking` and the seeded offer from `sky.offer`.

### Verdict

- [ ] Verdict: PASS / FAIL (delete the wrong one)

## Result summary

<!-- One-paragraph narrative of what happened during this run. Anchor to the
Expected assertions. Write your summary above this line, then fill the
fields below. -->

Input tokens:

Output tokens:

Start (UTC):

End (UTC):

Duration:

---

## Additional tasks I did

<!-- Optional. List anything outside the spec, e.g. diagnostic curls, manual
log inspection, retries with different inputs. Leave empty if nothing
extra. -->
