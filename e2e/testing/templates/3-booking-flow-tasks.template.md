# booking-flow: run tasks template

Spec: [../3-booking-flow-test.md](../3-booking-flow-test.md)

Copy this file to `e2e/testing/runs/<UTC-timestamp>_3-booking-flow-tasks.md` before
starting a run. Tick boxes as you go. Add anything you did beyond the spec
under Additional tasks I did.

## Tasks

### Prerequisites

- [ ] Bruno CLI is installed (`bru --version` prints a version, exits 0).
- [ ] Gateway reachable: `/actuator/health` returns 200.
- [ ] Object store answers on the host: `curl -s -o /dev/null -w '%{http_code}\n' http://s3.localhost:9070/` prints `200`.

### Reset state

- [ ] None. The run creates its own data and deletes it in the cleanup requests (self-cleaning, re-runnable).

### Run

- [ ] Single Bruno run: `cd docs/api/request && bru run auth offer booking cleanup/delete-booking.yml cleanup/delete-offer.yml --env local --insecure`.

### Expected

- [ ] Run summary reports Status PASS, all requests passed, all assertions passed (record the total you saw, the collection grows).
- [ ] Seeded offer returns HTTP 201 with UUID `id` and `ownerEmail` `lukk@sky.dev`.
- [ ] Offer folder photo upload returns the presigned `photoUrl` (contains `X-Amz-Algorithm`), and fetching that URL returns HTTP 200 with the canary marker `SKY-OFFER-PHOTO-CANARY-4471` in the stored bytes.
- [ ] Offer folder photo lifecycle passes too: the edit leaves the object alone, and each replaced or deleted object's address answers 404.
- [ ] Booking returns HTTP 201 with UUID `id`, `offerId` equal to the `offerId` variable the seeded-offer request saved, a `bookedDate` carrying `2027-08-15`, and `bookingUser` `lukk@sky.dev` (Kafka event consumed by sky-notify).
- [ ] User bookings page returns the booking (Postgres `public.booking`).
- [ ] Owner lookup returns HTTP 200 with body `lukk@sky.dev`.
- [ ] Teardown deletes return HTTP 204, removing the booking from `public.booking` and the seeded offer from `public.offer`.

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
