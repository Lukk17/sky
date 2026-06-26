# offer-crud: run tasks template

Spec: [`2-offer-crud-test.md`](2-offer-crud-test.md)

Copy this file to `runs/<UTC-timestamp>_2-offer-crud-tasks.md` before
starting a run. Tick boxes as you go. Add anything you did beyond the spec
under Additional tasks I did.

## Tasks

### Prerequisites

- [ ] Bruno CLI is installed (`bru --version` prints a version, exits 0).
- [ ] Gateway reachable: `/actuator/health` returns 200.

### Reset state

- [ ] None. The run creates its own data and deletes it in the teardown requests (self-cleaning, re-runnable).

### Run

- [ ] Single Bruno run: `cd docs/api/request && bru run auth offer teardown/delete-offer.yml --env local --insecure`.

### Expected

- [ ] Run summary reports Status PASS, all requests passed, all assertions passed: 39/39.
- [ ] Created offer returns HTTP 201 with UUID `id`, `ownerEmail` `lukk@sky.dev`, and the submitted `hotelName`, `city`, `country`, `roomCapacity`, and `price`.
- [ ] Public offer list returns HTTP 200 with a `content` array and numeric `totalElements`.
- [ ] Search returns the offer by `hotelName` LIKE.
- [ ] Owner page returns the offer (Postgres `sky.offer`).
- [ ] Owner lookup returns HTTP 200 with body `lukk@sky.dev`.
- [ ] Photo upload returns HTTP 200 with UUID `id` and a non-empty presigned `photoUrl` referencing the `sky-offers` store (MinIO and `offer_photo`).
- [ ] Edit returns HTTP 200 and a follow-up read reflects the renamed `hotelName` and reprice.
- [ ] Teardown delete returns HTTP 204 (removal from `sky.offer` / `sky.offer_photo` and `sky-offers`).

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
