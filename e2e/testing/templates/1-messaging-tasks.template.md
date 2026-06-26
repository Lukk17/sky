# messaging: run tasks template

Spec: [`1-messaging-test.md`](1-messaging-test.md)

Copy this file to `runs/<UTC-timestamp>_1-messaging-tasks.md` before
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

- [ ] Canary messages from prior runs removed from the sender's sent collection.

### Run

- [ ] Step 1: send canary message, captured `MSG_ID` (HTTP 201).
- [ ] Step 2: list received messages (HTTP 200).
- [ ] Step 3: list sent messages, created id present.
- [ ] Step 4: delete the created message (HTTP 204).
- [ ] Step 5: list sent messages again, id absent.

### Expected

- [ ] Step 1 returns HTTP 201 with numeric `id`, `senderEmail` `lukk@sky.dev`, `receiverEmail` `owner@example.com`, and the canary `text`.
- [ ] Step 2 returns HTTP 200 with a `content` array and numeric `totalElements`.
- [ ] Step 3 returns HTTP 200 and prints exactly one message whose `id` equals `MSG_ID` and whose `text` is the canary string.
- [ ] Step 4 returns HTTP 204.
- [ ] Step 5 prints `0` (message removed from the sender's sent collection).

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
