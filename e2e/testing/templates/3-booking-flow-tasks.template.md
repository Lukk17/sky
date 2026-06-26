# booking-flow: run tasks template

Spec: [`3-booking-flow-test.md`](3-booking-flow-test.md)

Copy this file to `runs/<UTC-timestamp>_3-booking-flow-tasks.md` before
starting a run. Tick boxes as you go. Add anything you did beyond the spec
under Additional tasks I did.

## Tasks

### Prerequisites

- [ ] curl is installed (`curl --version` exits 0).
- [ ] jq is installed (`jq --version` prints a version).
- [ ] Gateway reachable: public offers endpoint returns 200.
- [ ] Keycloak token obtained and exported as `$TOKEN`.
- [ ] `$TOKEN` is a well-formed JWT (`token-ok`).

### Reset state

- [ ] Canary bookings from prior runs removed (matched by `bookedDate` 2027-08-15).
- [ ] Canary offers from prior runs removed from the owner's collection.

### Run

- [ ] Step 1: create canary offer, captured `OFFER_ID` (HTTP 201).
- [ ] Step 2: create booking against the offer, captured `BOOKING_ID` (HTTP 201).
- [ ] Step 3: list user bookings, booking present (HTTP 200).
- [ ] Step 4: resolve offer owner (HTTP 200).
- [ ] Step 5: delete booking (HTTP 204).
- [ ] Step 6: list user bookings, booking absent.
- [ ] Step 7: delete seeded offer (HTTP 204).

### Expected

- [ ] Step 1 returns HTTP 201 with numeric `id` and `ownerEmail` `lukk@sky.dev`.
- [ ] Step 2 returns HTTP 201 with numeric `id`, `offerId` equal to `OFFER_ID`, a `bookedDate` carrying `2027-08-15`, and `bookingUser` `lukk@sky.dev`.
- [ ] Step 3 prints `1` (booking present in the user's page in `sky.booking`).
- [ ] Step 4 returns HTTP 200 with body `lukk@sky.dev`.
- [ ] Step 5 returns HTTP 204.
- [ ] Step 6 prints `0` (booking removed from `sky.booking`).
- [ ] Step 7 returns HTTP 204 (seeded offer removed from `sky.offer`).

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
