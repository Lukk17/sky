# Spec Delta

## Purpose

Maps the frontend offer photo flow onto the backend object-store photo endpoints through the gateway, so external photo links persist and stored-object upload and delete work from the UI.

## ADDED Requirements

### Requirement: External photo URL field with validation parity

The frontend offer forms SHALL send the external photo link as `externalPhotoUrl` and SHALL surface backend absolute-URL validation failures for that field.

#### Scenario: External URL persists through create and edit

- **WHEN** a user saves an offer with a valid absolute `http(s)` photo URL
- **THEN** the create/edit request carries `externalPhotoUrl` and the saved offer renders it

#### Scenario: Invalid photo URL is rejected visibly

- **WHEN** a user saves an offer with a non-absolute photo URL
- **THEN** the form shows a field-level error and no save request with that value succeeds

### Requirement: Stored-object photo upload and delete through the gateway

The frontend SHALL upload offer photos as multipart `file` to `POST /api/v1/owner/offers/{offerId}/photo` and SHALL delete the stored photo via `DELETE` on the same path, both through the gateway origin, rendering the returned `photoUrl` (presigned stored object when present, else the external URL, else empty).

#### Scenario: File upload shows the stored photo

- **WHEN** an owner uploads a photo file for an offer
- **THEN** the client posts multipart `file` to the gateway photo path and renders the returned `photoUrl`

#### Scenario: Stored photo delete clears the rendered photo

- **WHEN** an owner deletes the stored photo of an offer
- **THEN** the client calls the gateway photo delete path and the rendered `photoUrl` falls back to the external URL or empty

#### Scenario: No legacy photo field is sent

- **WHEN** any offer create, edit, upload, or display path runs
- **THEN** no request body or response mapping names `photoPath`
