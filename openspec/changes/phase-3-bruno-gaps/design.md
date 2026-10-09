## Context

The Bruno collection is the contract the running stack is checked against in CI, and it is the thing a caller's saved requests hold. Three endpoints have no request, so three pieces of the published surface have no automated assertion. The fix is to write the three requests in the style the collection already uses, reusing the variables the existing requests set rather than minting new ones.

## Variable reuse

- `{{offerId}}` is set by `create-offer.yml` (seq 1) in its after-response script, and consumed by every subsequent offer request. `get-offer.yml` reuses it unchanged.
- `{{galleryPhotoId1}}` is set by `upload-gallery-photo-1.yml` (seq 11) from `res.body.gallery[0].id`, and consumed by `delete-gallery-photo.yml` (seq 14). `set-gallery-cover.yml` reuses it unchanged, which is the variable name the upload requests set first.
- `{{bearerToken}}` is set by `auth/get-token.yml` (seq 1) into the environment, and consumed by every authenticated request. `get-session.yml` reuses it unchanged.

## Request shapes

- `get-offer.yml` is a GET at `{{offerUrl}}/api/v1/offers/{{offerId}}` with the `Authorization` and `Accept` headers the other authenticated offer requests carry. Its assertions pin status 200 and the full offer body: `id`, `hotelName`, `city`, `country`, `roomCapacity`, `price`, `ownerEmail`, `externalPhotoUrl`, `photoUrl`, the absence of `photoPath` and `photoObjectKey`, and the gallery array with its `id`, `position`, `url` and `main` fields plus `coverPhotoUrl`.
- `set-gallery-cover.yml` is a PUT at `{{offerUrl}}/api/v1/owner/offers/{{offerId}}/photos/{{galleryPhotoId1}}/cover` with the same headers. Its assertions pin status 200, that the returned gallery photo with id `{{galleryPhotoId1}}` carries `main: true`, that no other gallery photo carries `main: true`, and that `coverPhotoUrl` equals that photo's `url`.
- `get-session.yml` is a GET at `{{gatewayUrl}}/api/session` with the `Authorization` and `Accept` headers. The gateway session endpoint returns `Map<String, String>` with `email`, `csrfToken` and `logoutUrl` when a session exists, so the assertions pin status 200 and those three keys. The `local` environment already publishes `offerUrl`, `bookingUrl` and `messageUrl` at `http://localhost:5777`; this request names the gateway address through a new `gatewayUrl` variable added to `local.yml` rather than reusing one of the three, so the request reads as a gateway request and not as a service request.

## Ordering

The three requests are inserted into the existing sequence by their seq numbers. `get-offer.yml` is seq 5, which sits between `get-owned-offers.yml` (seq 4) and `get-offer-owner.yml` (seq 5). To avoid renumbering the whole offer folder, the new requests take the next free seq values after the highest existing one: `get-offer.yml` is seq 15, `set-gallery-cover.yml` is seq 16, and `get-session.yml` is seq 17 in a new `auth` sub-request rather than a new folder, because the gateway session is an auth-shaped endpoint and the `auth` folder already exists with `get-token.yml` at seq 1. Renaming the existing `get-offer-owner.yml` seq is not done: the collection is run by seq, and renumbering a working request would only move a green assertion.

## What was deliberately left out

- No assertion pins the `logoutUrl` value literally. It is built from `issuerUri`, `clientId` and the first allowed frontend URL, and its exact form is an environment detail. The request pins that the key is present and is a string, which is the shape the controller publishes.
- No request asserts the CSRF token value. It is session-bound and opaque; the collection has no way to submit it back, and the gateway validates it on mutating requests the collection does not make.
- The `local` environment is the only one updated. `direct.yml`, `k8s.yml` and `ci.yml` name their own gateway or service addresses and would need their own `gatewayUrl`; adding it there is a separate change once those environments are exercised by this request.