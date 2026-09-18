# offer-crud: run tasks template

Spec: [../2-offer-crud-test.md](../2-offer-crud-test.md)

Copy this file to `e2e/testing/runs/<UTC-timestamp>_2-offer-crud-tasks.md` before
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

- [ ] Single Bruno run: `cd docs/api/request && bru run auth offer cleanup/delete-offer.yml --env local --insecure`.

### Expected

- [ ] Run summary reports Status PASS, all requests passed, all assertions passed (record the total you saw, the collection grows).
- [ ] Created offer returns HTTP 201 with UUID `id`, `ownerEmail` `lukk@sky.dev`, and the submitted `hotelName`, `city`, `country`, `roomCapacity`, and `price`.
- [ ] Public offer list returns HTTP 200 with a `content` array and numeric `totalElements`.
- [ ] Search returns the offer by `hotelName` LIKE.
- [ ] Owner page returns the offer (Postgres `public.offer`).
- [ ] Owner lookup returns HTTP 200 with body `lukk@sky.dev`.
- [ ] Photo upload returns HTTP 200 with UUID `id` and a non-empty presigned `photoUrl` referencing the `sky-offers` bucket (object key in the server-owned `offer.photo_object_key`, absent from every response body), and fetching that URL returns HTTP 200 with the canary marker `SKY-OFFER-PHOTO-CANARY-4471` in the stored bytes.
- [ ] Edit returns HTTP 200, a follow-up read reflects the renamed `hotelName` and reprice, and refetching `photoUrl` still returns the canary bytes.
- [ ] Photo replace returns HTTP 200 with a new `photoUrl` that fetches the canary, and the replaced object's address answers 404.
- [ ] Photo delete returns HTTP 204 and the address it cleared answers 404.
- [ ] Restore upload returns HTTP 200, so the teardown has a photo to remove.
- [ ] Teardown delete returns HTTP 204 (removal from `public.offer`, and the object it held answers 404).

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
