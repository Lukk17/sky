# offer-crud: e2e capability test proposal

## Why

sky-offer (port 5552) is the inventory the rest of the platform books against, and it is the only service that spans
three backing stores in one flow: Postgres (`offer`, `offer_photo`), MinIO (`sky-offers` bucket), and the gateway
auth edge. The full owner lifecycle, create, browse, search, resolve owner, attach a photo, edit, delete, has no
single end-to-end assertion. The captured Bruno `responses.json` shows the authenticated calls returning 401 because
no token was injected, so nothing proves the authenticated CRUD path or that an uploaded photo comes back as a
usable presigned URL. This test pins the whole owner lifecycle for lukk@sky.dev and asserts retrieval of the exact
canary offer through public search.

## What this will verify

- POST `/offer/api/owner/offers` returns HTTP 201 with a server-assigned UUID `id`, `ownerEmail` equal to
  `lukk@sky.dev`, and the submitted `hotelName`, `city`, `country`, `price`, and `roomCapacity` echoed.
- GET `/offer/api/offers` (public) returns HTTP 200 with a paginated body (`content` array and numeric
  `totalElements`).
- POST `/offer/api/search` with the canary token returns HTTP 200 and the created offer appears in `content`
  (retrieval by `hotelName` LIKE).
- GET `/offer/api/owner/offers` returns HTTP 200 and the created offer appears in the owner's page.
- GET `/offer/api/offers/{id}/owner` returns HTTP 200 with body `lukk@sky.dev`.
- POST `/offer/api/owner/offers/{id}/photo` (multipart) returns HTTP 200 with a UUID `id` and a non-empty
  `photoUrl` presigned URL pointing at the `sky-offers` object store (persisted state in MinIO and `offer_photo`).
- PUT `/offer/api/owner/offers` returns HTTP 200; a follow-up read reflects the edited `hotelName` and `price`.
- DELETE `/offer/api/owner/offers/{id}` returns HTTP 204 and the offer no longer appears in the owner's page
  (persisted-state removal in Postgres `sky.offer` / `sky.offer_photo`).

## Setup cost class

- [ ] 1: No state to reset, no fixtures, hits a single endpoint or MCP tool.
- [ ] 2: Single fixture upload OR vision-capable model OR PDF parsing.
- [ ] 3: Single-service reset (e.g. Redis only).
- [x] 4: Multi-service reset (DB + Redis + Qdrant + MinIO).
- [ ] 5: Seeded state + observation of an async background process.

This test uploads a fixture and writes to two backing stores (Postgres and MinIO), so it costs more to set up and
reset than the messaging test and runs after it.

## Fixtures needed

- `e2e/fixtures/offer-photo.png` — a 16x16 PNG with a `tEXt` chunk carrying `SKY-E2E-CANARY-OFFER-PHOTO-7F3A`. Real
  PNG magic bytes so the photo-upload validator (JPEG/PNG/GIF/WebP magic-byte check) accepts it; the canary string
  proves the byte stream round-tripped to MinIO rather than a memorised placeholder.

## Concurrency profile

- Mutates: Postgres `sky.offer` and `sky.offer_photo`, rows owned by `lukk@sky.dev`; MinIO bucket `sky-offers`,
  objects for `lukk@sky.dev` offers (the canary offer and its photo this test creates and deletes).
- Conflicts with: `3-booking-flow-test.md`, which also creates and deletes offers owned by `lukk@sky.dev` in
  `sky.offer`. The overlapping Mutates already forces serialisation; this names it for clarity.
- Serial: false.

## API client invocation

- Token: `docs/api/request/auth/get-token.yml`, or the equivalent curl shown in the spec.
- Flow: `docs/api/request/offer/create-offer.yml`, `get-all-offers.yml`, `search-offers.yml`, `get-owned-offers.yml`,
  `get-offer-owner.yml`, `upload-photo.yml`, `edit-offer.yml`, and `offer-teardown/delete-offer.yml`. The spec drives
  the same endpoints with self-contained curl through the gateway.

## Number assignment

N reflects relative setup cost across the suite (lowest first). Offer CRUD is more expensive than messaging (fixture
upload plus a second backing store) and cheaper than booking-flow (no dependent service or async events), so it
takes the middle prefix.

N: 2

## Next steps

After this proposal is approved:

- Generate `e2e/testing/2-offer-crud-test.md` from the `test-spec` artifact.
- Generate `e2e/testing/templates/2-offer-crud-tasks.template.md` from the `tasks-template` artifact.
- Each execution generates a `run` record under `e2e/testing/runs/`.
