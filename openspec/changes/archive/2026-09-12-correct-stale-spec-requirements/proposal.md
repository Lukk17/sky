## Why

Two merged specifications under `openspec/specs/` state requirements that the code contradicts, so they no longer
describe the system and cannot be used to judge whether a change regressed anything.

`offer-crud-e2e` requires assertions against a Postgres table named `sky.offer_photo`. No migration in this
repository has ever created that table: `sky-offer/src/main/resources/db/migration/V1__init.sql` creates `offer` and
`offer_event` only, and the photo has always been a column on the offer row.
`sky-offer/src/main/resources/db/migration/V3__split_photo_object_key_from_external_photo_url.sql` then split that
column in two, a server-owned `photo_object_key` and a client-writable `external_photo_url`, which the specification
does not mention at all. The same sentence also names MinIO, when the runbook it governs asserts against the object
store through a presigned URL and never against a MinIO-specific interface.

`test-strategy` requires that a controller rejection be asserted as "400 or 401 per current contract". The handlers
in `sky-booking` and `sky-offer` now return five more statuses than that, so a test asserting 400 or 401 against an
endpoint that answers 409 or 503 would be written to a contract the code does not honour.

## What Changes

- Restate the `offer-crud-e2e` requirement so its persisted-state assertion targets are the ones the runbook at
  `e2e/testing/2-offer-crud-test.md` actually checks: the `offer` table with its server-owned `photo_object_key`
  column, and the `sky-offers` bucket in the object store. Drop `sky.offer_photo`, which never existed, and drop the
  MinIO product name in favour of the object store.
- Restate the `test-strategy` requirement on authenticated and unauthenticated paths so the rejection statuses are
  the full set the handlers return, read out of
  `sky-booking/src/main/java/com/lukk/sky/booking/domain/exception/GlobalExceptionHandler.java` and
  `sky-offer/src/main/java/com/lukk/sky/offer/domain/exception/GlobalExceptionHandler.java`.
- Correct, in the same requirement block, the test authentication mechanism it names. It says `X-Forwarded-User`
  today with JWT to follow, and `X-Forwarded-User` appears nowhere in the repository: the controller tests drive the
  JWT chain. This sentence sits inside the block being restated, so it cannot be carried over unchanged.
- Correct the example path in that requirement's scenario from `/api/bookings` to `/api/v1/bookings`, which is what
  `sky.apiPrefix` plus `BookingController` produce.

Nothing here changes code. Both requirements are being aligned to behaviour that already ships and is already
verified, so no test and no endpoint moves.

## Capabilities

### New Capabilities

None. Both capabilities already exist.

### Modified Capabilities

- `offer-crud-e2e`: the persisted-state assertion targets in the behaviour-only runbook requirement.
- `test-strategy`: the rejection status set and the authentication mechanism in the authenticated and
  unauthenticated paths requirement.

## Impact

- Affected files: `openspec/specs/offer-crud-e2e/spec.md` and `openspec/specs/test-strategy/spec.md`, rewritten at
  archive time from the delta specs in this change.
- No source, chart, compose file, migration, Bruno request or OpenAPI contract is touched, so there is nothing to
  build and nothing to deploy.
- The Purpose placeholder both files carry is out of scope here and is handled by the
  `replace-spec-purpose-placeholders` change, so that eighteen cosmetic rewrites do not obscure this correctness fix.
- Risk: low. The specifications move toward the committed runbook and the committed handlers, both of which are the
  evidence rather than the thing being changed.
