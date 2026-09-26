# Design

## Context

See proposal.md Why. Frontend (`offer.service.ts`, add/edit offer forms, offer display templates) speaks `photoPath`; backend `OfferDTO` writes only `externalPhotoUrl` (absolute-URL constrained) and reads back derived `photoUrl`; stored-object upload/delete lives on `POST|DELETE /api/v1/owner/offers/{offerId}/photo` multipart `file` (see `OfferApiController`, `docs/api/request/offer/upload-photo.yml`). The gateway already forwards `/api/v1` unchanged, so no edge change is needed.

## Goals / Non-Goals

**Goals:**

- Rename the frontend photo field to `externalPhotoUrl` with backend validation parity.
- Wire file upload and photo delete through the gateway photo endpoints with `photoUrl` rendering.
- Cover both parts with frontend tests.

**Non-Goals:**

- No backend DTO, controller, storage, or validation change.
- No gateway route or ingress change.
- No migration, Helm, Kafka, or auth change.

## Decisions

- Two-part sequencing (rename first, upload second): the rename alone restores typed-link persistence and is independently testable; upload builds on the saved offer id. Alternative of one combined change was rejected because a rename regression would be indistinguishable from an upload regression.
- Frontend-only change: the backend contract already supports both halves, so no new endpoint or field is introduced. Alternative of adding a writable `photoPath` alias was rejected because it would create a second writable photo field against the module rule of one.
- Reuse the gateway origin (`apiBaseUrl`) for the multipart calls with no new environment key: photo paths are published `/api/v1` like every other offer path. Alternative of a separate photo host was rejected because it would bypass the edge auth chain.
- Render `photoUrl` as returned (presigned stored object, else external URL, else empty) rather than composing a display URL client-side: matches the server-owned key rule and the Bruno canary assertions. Alternative of preferring `externalPhotoUrl` always was rejected because it would hide the stored object.
- Validation parity by surfacing the backend absolute-URL error on the field plus a matching client-side absolute-URL check: keeps the message source of truth on the backend while failing fast in the form.

## Risks / Trade-offs

- [Presigned URL expiry in rendered views] → Mitigation: re-fetch the offer to refresh `photoUrl` rather than caching it.
- [Large file rejected by the 5 MB / 6 MB limits with a 413] → Mitigation: surface the error text and keep the previous photo rendered.
- [Sibling compose work in progress] → Mitigation: proposal only, no containers touched, no verification run claimed.

## Migration Plan

- Frontend-only rollout, no data migration. Rollback is revert of the frontend change; stored objects and external URLs are untouched.
