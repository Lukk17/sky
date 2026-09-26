## 1. Environments

- [x] 1.1 Collapse the three per-service base addresses into one `apiBaseUrl` per environment file (gateway origin for default and production, direct ports only in `localDev`), and verify no production or default file names port 5552 to 5555
- [x] 1.2 Rewrite every path fragment to its published `/api/v1/...` form, and verify a search for `/offer/api`, `/booking/api` and `/msg/api` in `apps/frontend/sky-view/src` returns zero matches outside change archives

## 2. Services

- [x] 2.1 Update `offer.service.ts` to the published offer paths (list, owned, add, edit, delete with id, search), and verify each method builds the exact gateway-routed URL
- [x] 2.2 Update `booking.service.ts` to `GET /api/v1/user/bookings`, `POST /api/v1/bookings` and `DELETE /api/v1/bookings/{bookingId}`, and verify each method builds the exact gateway-routed URL
- [x] 2.3 Rewrite `message.service.ts` send as `POST /api/v1/messages` with a body and delete as `DELETE /api/v1/messages/{messageId}`, keeping received and sent as `GET /api/v1/messages/received` and `/sent`, and verify each method builds the exact gateway-routed URL
- [x] 2.4 Build the `StompService` socket URL from the environment origin plus `/notifyWebsocket`, and verify no hardcoded `localhost:5554` string remains in `apps/frontend/sky-view/src`
- [x] 2.5 Cover every secured call with the bearer header from `sky-auth.service.ts` while leaving anonymous list and search calls bare when no token is present, and verify the header logic has no per-service exception

## 3. Verification

- [ ] 3.1 Serve the app against the compose stack and click through offers, search, owned offers, bookings, messages and auth, and verify every screen loads data with no edge 404
- [ ] 3.2 Repeat the click-through against the cluster host, and verify secured screens pass the ingress auth chain while anonymous screens stay public
- [ ] 3.3 Trigger a booking event with the app open and verify the notification arrives over the gateway WebSocket URL
- [ ] 3.4 Record the photo upload gap as a follow-up task, and verify it is tracked rather than silently dropped
  - NOTE (2026-09-24, read-only investigation): frontend `apps/frontend/sky-view/src/app/services/offer.service.ts:28-38,105-116`
    builds `Offer` with `photoPath` from the `photoPath` form field (`add-offer.component.html:50-58`,
    `edit-offer.component.html:55-59`) and renders it (`offers.component.html:5`, `offer-details.component.html:4`).
    Backend `sky-offer/src/main/java/com/lukk/sky/offer/adapters/dto/OfferDTO.java:51-57` exposes only
    `externalPhotoUrl` (absolute-URL constrained) plus derived `photoUrl`, so a `photoPath` JSON member reaches
    no writable field and the typed link never persists. Stored-object upload lives on a separate contract,
    `POST /api/v1/owner/offers/{offerId}/photo` multipart `file` (`OfferApiController.java:240`,
    `docs/api/request/offer/upload-photo.yml:6-21`), which the frontend never calls. Minimal fix exceeds a
    small change: rename `photoPath` to `externalPhotoUrl` across model, forms, and templates, plus add a
    multipart upload call with tests. Follow-up proposal: one change mapping the form field to
    `externalPhotoUrl` with validation parity, plus a second change wiring file upload to the photo endpoint;
    this box stays open and the gap is tracked here, not dropped.
