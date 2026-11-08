## 1. Scaffold the change

- [x] 1.1 Create `openspec/changes/phase-3-bruno-gaps/` mirroring the archived layout: `proposal.md`, `design.md`, `tasks.md`, `.openspec.yaml`, `README.md`.
- [x] 1.2 Confirm the gap by listing `docs/api/request/offer/*.yml` and `docs/api/request/auth/*.yml` and confirming no request exists for `GET /api/v1/offers/{offerId}`, `PUT /api/v1/owner/offers/{offerId}/photos/{photoId}/cover` or `GET /api/session`.

## 2. Read the existing style before writing

- [x] 2.1 Read `docs/api/request/offer/upload-gallery-photo-1.yml` and confirm it sets `galleryPhotoId1` from `res.body.gallery[0].id`.
- [x] 2.2 Read `docs/api/request/auth/get-token.yml` and confirm it sets `bearerToken` into the environment and uses `eq` / `isDefined` assertions.
- [x] 2.3 Read `docs/api/request/offer/create-offer.yml` and confirm it sets `offerId` from `res.body.id`.
- [x] 2.4 Read the gateway `SessionController` response shape and confirm it returns `email`, `csrfToken` and `logoutUrl`.
- [x] 2.5 Read the offer gallery cover endpoint signature and confirm it is `PUT /owner/offers/{offerId}/photos/{photoId}/cover` returning `OfferDTO`.

## 3. Write the three requests

- [x] 3.1 Create `docs/api/request/offer/get-offer.yml` as GET single offer by id reusing `{{offerId}}`, asserting status plus full body values including gallery.
- [x] 3.2 Create `docs/api/request/offer/set-gallery-cover.yml` as PUT cover reusing `{{galleryPhotoId1}}`, asserting status plus cover flags.
- [x] 3.3 Create `docs/api/request/auth/get-session.yml` as GET gateway session asserting status plus session shape.
- [x] 3.4 Add `gatewayUrl` to `docs/api/request/environments/local.yml` pointing at `http://localhost:5777`.

## 4. Run only the new requests against the local env

- [x] 4.1 Run the three new requests against the `local` environment from `docs/api/request` and confirm each passes.
- [x] 4.2 Do NOT run the full collection.

## 5. Check gate compliance

- [x] 5.1 Read `.github/workflows/ci.yaml` `verify-changelog` job and confirm `docs/api/request` changes mark no module affected.
- [x] 5.2 Add changelog bumps ONLY if the gate requires them for these changes. Confirm it does not.

## 6. Report

- [x] 6.1 Report files created, run output and gate compliance. Do NOT commit. Do NOT touch other phases.