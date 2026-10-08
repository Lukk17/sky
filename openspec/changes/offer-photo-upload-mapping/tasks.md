# Tasks

## 1. Field rename with validation parity

- [ ] 1.1 Rename frontend `photoPath` to `externalPhotoUrl` across the offer model, offer service mappings, add/edit forms, and display templates, and verify no `photoPath` reference remains in `apps/frontend/sky-view/src` outside change archives
- [ ] 1.2 Surface backend absolute-URL validation failures on the photo field with a matching client-side absolute-URL check, and verify an invalid URL shows a field-level error while a valid absolute URL saves

## 2. Upload wiring with tests

- [ ] 2.1 Add a frontend multipart `file` upload call to `POST /api/v1/owner/offers/{offerId}/photo` plus photo delete to `DELETE` on the same path through the gateway origin, and verify the request shape matches `docs/api/request/offer/upload-photo.yml`
- [ ] 2.2 Render the returned `photoUrl` (presigned stored object, else external URL, else empty) in offer displays, and verify stored, external, and empty states each render correctly
- [ ] 2.3 Cover the rename, validation display, and upload/delete paths with frontend tests, and verify the suite passes
