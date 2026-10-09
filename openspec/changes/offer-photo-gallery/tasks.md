# Tasks

## 1. Persistence and migration

- [ ] 1.1 Add `offer_photo` table migration plus backfill of legacy single-slot values into position 0, and verify with `./apps/backend/gradlew :sky-offer:flywayMigrate` equivalent or Testcontainers repository test showing legacy values land exactly once in order
- [ ] 1.2 Add gallery repository queries (ordered by position, cover as position 0) and verify with a repository test asserting order, empty gallery, and stored-object precedence
- [ ] 1.3 Document the S3 key layout `offers/{offerId}/{photoId}-{filename}` in the sky-offer module docs and verify the documented layout matches the upload path in code review

## 2. Gallery API

- [ ] 2.1 Implement gallery reads on offer responses (full ordered gallery plus derived cover) and verify with a `@WebMvcTest` or integration test asserting order, cover, and empty state
- [ ] 2.2 Implement photo upload appending to gallery end and photo delete removing both row and S3 object, and verify with an integration test using a fake object store asserting URLs are retrievable and deletes remove objects
- [ ] 2.3 Implement reorder and set-cover with invalid-reference rejection leaving the gallery unchanged, and verify with tests asserting the new order, the new cover, and the error case
- [ ] 2.4 Enforce the gallery cap (10, decided per design.md) with a 413 on overflow and verify with a test uploading past the cap

## 3. Frontend rendering

- [ ] 3.1 Normalize the gallery once at the API client boundary (cover fallback, empty state) and verify with a client unit test on sample payloads
- [ ] 3.2 Render cover plus thumbnails with selection on the offer detail view and cover-only on cards, and verify with a component test or manual screenshot of three-photo, one-photo, and empty galleries

## 4. Runbook and seed integration

- [ ] 4.1 Extend `e2e/testing/2-offer-crud-test.md` and its run-record template to two-photo upload, reorder, and delete per the delta spec, and verify with `openspec validate --strict`
- [ ] 4.2 Run the full gallery flow against a live stack and verify the two presigned URLs resolve, the reorder swaps the cover, and the delete removes the object from the `sky-offers` bucket
