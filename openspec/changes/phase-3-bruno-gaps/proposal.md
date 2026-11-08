## Why

The Bruno collection under `docs/api/request` covers the offer gallery write path and the public offer list, but three REST endpoints have no collection request exercising them. The gaps are:

- `GET /api/v1/offers/{offerId}` on sky-offer: the single-offer read. `get-all-offers.yml` and `get-owned-offers.yml` walk the list endpoints, and `get-offer-owner.yml` walks the owner lookup, but no request reads a single offer by id and asserts its body, so a regression in the offer DTO, the gallery or the cover address is invisible to the collection.
- `PUT /api/v1/owner/offers/{offerId}/photos/{photoId}/cover` on sky-offer: the gallery cover setter. `upload-gallery-photo-1.yml` and `upload-gallery-photo-2.yml` upload two photos, `reorder-gallery-photo.yml` reorders them, and `delete-gallery-photo.yml` removes one, but the cover flag is never asserted on a write, so a cover that fails to promote or that leaves two mains is invisible.
- `GET /api/session` on sky-gateway: the gateway session owner. The gateway is the only component the collection reaches through, and no request reads the session shape back, so a change to the CSRF token delivery or the logout URL is invisible to the collection.

## What Changes

- Add `docs/api/request/offer/get-offer.yml` as a GET single offer by id reusing the `{{offerId}}` chain variable set by `create-offer.yml`, asserting status 200 plus the full body values including the gallery array.
- Add `docs/api/request/offer/set-gallery-cover.yml` as a PUT cover reusing the gallery photo id variable set by `upload-gallery-photo-1.yml`, asserting status 200 plus the cover flags.
- Add `docs/api/request/auth/get-session.yml` as a GET gateway session request asserting status 200 plus the session shape.
- No code, test, chart, compose file or published document is touched. This change is collection-only.

## Capabilities

### Modified Capabilities

None. No specification changes; this is a coverage gap in the Bruno collection, which is not governed by an OpenSpec capability.

## Impact

- Affected files: three new Bruno requests under `docs/api/request/`.
- The `e2e-collection` job in `.github/workflows/ci.yaml` runs the whole collection, so the three new requests run in CI once merged.
- The `verify-changelog` job in `.github/workflows/ci.yaml` does not demand a bump for `docs/api/request` changes: its changed-file switch has no case for `docs/`, so nothing is marked affected and the job exits 0 with no module to verify.
- Risk: low. Each request mirrors the variable and assertion style of the request that sets the variable it consumes, and each is run against the local stack before this change is merged.