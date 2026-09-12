## 1. Establish the evidence

- [x] 1.1 Confirm no `offer_photo` table exists by reading every file in
  `sky-offer/src/main/resources/db/migration/` and verifying that `CREATE TABLE` appears only for `offer` and
  `offer_event`
- [x] 1.2 Confirm the real photo columns by reading
  `sky-offer/src/main/resources/db/migration/V3__split_photo_object_key_from_external_photo_url.sql` and verifying it
  adds `photo_object_key` and renames `photo_path` to `external_photo_url`
- [x] 1.3 Confirm `photo_object_key` is server-owned by grepping the `sky-offer` main sources and verifying the field
  appears in no DTO and is written only in `OfferServicePrimary`
- [x] 1.4 Confirm the assertion targets the runbook really checks by reading the What this verifies section of
  `e2e/testing/2-offer-crud-test.md` and verifying it names `public.offer`, `photo_object_key` and the `sky-offers`
  bucket and never names a database table for photos
- [x] 1.5 Record the full rejection status set by reading
  `sky-booking/src/main/java/com/lukk/sky/booking/domain/exception/GlobalExceptionHandler.java`,
  `sky-offer/src/main/java/com/lukk/sky/offer/domain/exception/GlobalExceptionHandler.java`,
  `sky-common/src/main/java/com/lukk/sky/common/web/SkyRestExceptionHandler.java` and
  `sky-common/src/main/java/com/lukk/sky/common/web/SpringDataExceptionHandler.java`, and verifying every status named
  in the delta maps to a handler that returns it
- [x] 1.6 Confirm `X-Forwarded-User` is absent from the repository by grepping the five module source trees and
  verifying zero matches

## 2. Write the delta specifications

- [x] 2.1 Write `openspec/changes/correct-stale-spec-requirements/specs/offer-crud-e2e/spec.md` under
  `## MODIFIED Requirements`, carrying the entire requirement block including its unchanged scenario, and verify the
  requirement header matches the merged file character for character
- [x] 2.2 Write `openspec/changes/correct-stale-spec-requirements/specs/test-strategy/spec.md` under
  `## MODIFIED Requirements`, carrying the entire requirement block, and verify the requirement header matches the
  merged file character for character
- [x] 2.3 Verify both deltas hold no em dash, no en dash, no semicolon joining two clauses, and no bold or italic,
  using a byte-exact matcher first validated against a fixture containing an em dash, an en dash, an arrow and a
  bullet
- [x] 2.4 Verify each delta respects the wrap width of the merged file it targets: 120 columns for `offer-crud-e2e`,
  one physical line per paragraph for `test-strategy`
- [x] 2.5 Run `openspec validate correct-stale-spec-requirements --strict` and verify it reports no error

## 3. Archive and sync

- [x] 3.1 Archive the change, take the sync when the archive step offers it, and verify
  `openspec/specs/offer-crud-e2e/spec.md` no longer contains `offer_photo` or `MinIO`
- [x] 3.2 Verify `openspec/specs/test-strategy/spec.md` no longer contains the string `400 or 401` and now lists
  every status from task 1.5
- [x] 3.3 Verify the change directory moved to `openspec/changes/archive/2026-09-12-correct-stale-spec-requirements`
  and that `openspec list` reports it as no longer active
- [x] 3.4 Verify `git status` shows exactly the two merged specifications as modified, plus the archived change tree as
  added, and no file outside `openspec/changes` and `openspec/specs`

## 4. Notes from the run

- The merged-file rewrite was performed by `openspec archive correct-stale-spec-requirements --yes`, which reported
  `~ 1 modified` against each of the two capabilities. The `openspec-sync-specs` skill the archive skill delegates to
  is absent from this checkout, but the CLI did the sync itself, so no hand edit of a merged specification was needed
  for either requirement.
- Task 3.4 found one file beyond the expected set: `openspec/config.yaml`, untracked, holding `schema: spec-driven`.
  The CLI created it when the change was scaffolded. Nothing in this change asked for it and it is left in place for
  the owner to decide on.
