# messaging: run tasks template

Spec: [`1-messaging-test.md`](1-messaging-test.md)

Copy this file to `runs/<UTC-timestamp>_1-messaging-tasks.md` before
starting a run. Tick boxes as you go. Add anything you did beyond the spec
under Additional tasks I did.

## Tasks

### Prerequisites

- [ ] Bruno CLI is installed (`bru --version` prints a version, exits 0).
- [ ] Gateway reachable: `/actuator/health` returns 200.

### Reset state

- [ ] None. The run creates its own data and deletes it in the teardown requests (self-cleaning, re-runnable).

### Run

- [ ] Single Bruno run: `cd docs/api/request && bru run auth message teardown/delete-message.yml --env local --insecure`.

### Expected

- [ ] Run summary reports Status PASS, all requests passed, all assertions passed: 17/17.
- [ ] Sent message returns HTTP 201 with UUID `id`, `senderEmail` `lukk@sky.dev`, the submitted `receiverEmail`, and the canary `text`.
- [ ] Received page returns HTTP 200 with a `content` array and numeric `totalElements`.
- [ ] Sent page returns HTTP 200 with the created message present (Postgres `sky.message`).
- [ ] Teardown delete returns HTTP 204.

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
