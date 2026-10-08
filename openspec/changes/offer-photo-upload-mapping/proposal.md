# Proposal

## Why

The frontend builds offers with a `photoPath` form field that reaches no writable backend field, so the typed photo link never persists, and it never calls the multipart photo endpoint, so stored-object upload is unreachable from the UI. Mapping the flow onto the object-store photo endpoints through the gateway restores photo persistence for both external URLs and uploaded files.

## What Changes

- Part 1, field rename with validation parity: rename frontend `photoPath` to `externalPhotoUrl` across the offer model, add/edit forms, and display templates; surface backend `@ExternalPhotoUrl` absolute-URL validation errors in the forms.
- Part 2, upload wiring: add a frontend multipart file-upload call to `POST /api/v1/owner/offers/{offerId}/photo` (and photo delete to `DELETE` on the same path) routed through the gateway; render the returned `photoUrl` (presigned stored object, else external URL, else empty).
- Tests: unit coverage for the renamed field mapping and validation display, plus upload-path tests (multipart call shape, `photoUrl` rendering, gateway URL).
- No backend contract change; no gateway route change (photo endpoints already published under `/api/v1`).

## Capabilities

### New Capabilities

- `offer-photo-upload`: frontend offer photo flow mapping onto the object-store photo endpoints (external URL field plus multipart upload/delete through the gateway).

### Modified Capabilities

None.

## Impact

- Affected: `apps/frontend/sky-view` offer model, services, add/edit forms, display templates, and frontend tests.
- Referenced only: `sky-offer` `OfferDTO`/`OfferApiController` photo contract, `docs/api/request/offer/upload-photo.yml`, gateway `/api/v1` forwarding.
- No backend, gateway, Helm, migration, or Kafka changes.
