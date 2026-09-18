## ADDED Requirements

### Requirement: Offer CRUD capability has a behaviour-only e2e runbook

The suite MUST carry an immutable spec at `e2e/testing/2-offer-crud-test.md` and its run-record template at
`e2e/testing/templates/2-offer-crud-tasks.template.md` that drive the owner lifecycle (create, list, search, list
owned, resolve owner, upload photo, edit, delete) for the authenticated user lukk@sky.dev through the gateway at
`http://localhost:5777`. Assertions MUST be observable behaviour only: HTTP status codes, response body content, and
persisted state in Postgres `sky.offer` / `sky.offer_photo` and the MinIO `sky-offers` bucket. The runbook MUST
upload the canary fixture `e2e/fixtures/offer-photo.png` and MUST NOT assert on log substrings.

#### Scenario: Full owner lifecycle round-trips with a retrievable canary offer

- **WHEN** the runner creates a canary offer as lukk@sky.dev, searches for it, attaches the canary photo, edits it,
  and deletes it
- **THEN** the create returns HTTP 201 with a UUID `id` and `ownerEmail` `lukk@sky.dev`, public search returns
  the same offer by `hotelName`, the owner lookup returns `lukk@sky.dev`, the photo upload returns HTTP 200 with a
  non-empty presigned `photoUrl`, the edit returns HTTP 200 and is reflected on a follow-up read, the delete returns
  HTTP 204, and the offer no longer appears in the owner's page
