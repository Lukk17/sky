# offer-crud: run tasks template

Spec: [`2-offer-crud-test.md`](2-offer-crud-test.md)

Copy this file to `runs/<UTC-timestamp>_2-offer-crud-tasks.md` before
starting a run. Tick boxes as you go. Add anything you did beyond the spec
under Additional tasks I did.

## Tasks

### Prerequisites

- [ ] curl is installed (`curl --version` exits 0).
- [ ] jq is installed (`jq --version` prints a version).
- [ ] Gateway reachable: public offers endpoint returns 200.
- [ ] Fixture present: `e2e/fixtures/offer-photo.png` shows PNG magic bytes.
- [ ] Keycloak token obtained and exported as `$TOKEN`.
- [ ] `$TOKEN` is a well-formed JWT (`token-ok`).

### Reset state

- [ ] Canary offers from prior runs removed from the owner's collection (with their photos).

### Run

- [ ] Step 1: create canary offer, captured `OFFER_ID` (HTTP 201).
- [ ] Step 2: list all offers, public (HTTP 200).
- [ ] Step 3: search canary token, offer returned (HTTP 200).
- [ ] Step 4: list owner offers, offer present (HTTP 200).
- [ ] Step 5: resolve offer owner (HTTP 200).
- [ ] Step 6: upload canary photo (HTTP 200).
- [ ] Step 7: edit offer (HTTP 200).
- [ ] Step 8: read owner page, edit reflected (HTTP 200).
- [ ] Step 9: delete offer (HTTP 204).
- [ ] Step 10: read owner page, offer absent.

### Expected

- [ ] Step 1 returns HTTP 201 with numeric `id`, `ownerEmail` `lukk@sky.dev`, and the submitted `hotelName`, `city`, `country`, `roomCapacity`, and `price`.
- [ ] Step 2 returns HTTP 200 with a `content` array and numeric `totalElements`.
- [ ] Step 3 returns HTTP 200 and prints exactly one object whose `id` equals `OFFER_ID` and whose `hotelName` is the canary name.
- [ ] Step 4 prints `1` (offer present in the owner's page).
- [ ] Step 5 returns HTTP 200 with body `lukk@sky.dev`.
- [ ] Step 6 returns HTTP 200 with numeric `id` and a non-empty presigned `photoUrl` referencing the `sky-offers` store.
- [ ] Step 7 returns HTTP 200.
- [ ] Step 8 prints `hotelName` `SKY-E2E-OFFER-CANARY Hotel 7F3A (Renovated)` and `price` `299.99`.
- [ ] Step 9 returns HTTP 204.
- [ ] Step 10 prints `0` (offer and photo removed from `sky.offer` / `sky.offer_photo` and `sky-offers`).

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
