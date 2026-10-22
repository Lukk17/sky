# Phase 4c no-wire decision: sky-offer stays on hand DTOs

Date: 2026-10-22. Scope: `openspec/changes/phase-4c-server-stubs/`. No Java or contract change in this record.

## 1. sky-offer is not wired to generated DTOs

The generated `OfferDTO` in `contracts/sky-api/src/openapi.yaml` carries only `id`, `name`, `ownerEmail`, `photoUrl`, `price` (plus `city` in the bundled shape). The hand DTOs in `apps/backend/sky-offer/src/main/java/com/lukk/sky/offer/adapters/dto/` carry `hotelName` (not `name`) plus the full field set: `description`, `comment`, `roomCapacity`, `country`, `externalPhotoUrl`, `gallery`, `coverPhotoUrl`, with Bean Validation at the edge (`@NotBlank`/`@Size` on create, `@NullOrNotBlank` on edit, `@ExternalPhotoUrl`).

`PUT /owner/offers` is a partial update: absent means keep the stored value. `OfferEditDTO.applyTo` writes each field only when supplied via `applyIfSupplied`, and names neither `photoObjectKey` nor `ownerEmail`. The generated schema has no nullable-partial-update semantics, so wiring it drops fields and weakens edge validation. The wiring attempt broke 4 edit suites and was reverted. Working tree confirms the revert: no modified files under `apps/backend` or `contracts`.

Per `springboot-patterns` (validated DTO at the edge) and `hexagonal-architecture` (mapping stays in the inbound adapter), the hand DTOs remain the contract of the adapter. Behavior-neutral wiring is not possible with the current schema.

## 2. Booking and message need no wiring

Both are complete against the contract with no partial-update risk, verified green:

- Booking: `BookingDTO` (`id`, `offerId`, `bookedDate`, `bookingUser`, `ownerEmail`) and `BookingPayload` (`dateToBook`, `offerId`) match the contract schemas field for field. No edit endpoint, no merge semantics.
- Message: `MessageDTO` (`id`, `text`, `receiverEmail`, `senderEmail`, `createdTime`, `read`) matches the contract schema. Send is create-only, no edit endpoint.

Root `tasks.md` records `finish-phase-4c-booking-server-stubs` and `finish-phase-4c-message-server-stubs` as done. Nothing to wire, nothing to revert.

## 3. What contract expansion is needed to revisit

Do not retry wiring until the contract carries all of the following, then regenerate and re-run the 4 edit suites plus the ArchUnit hexagonal rules green:

- `hotelName` (not `name`) with the same `@Size(max = 255)` bounds, plus `description`, `comment`, `roomCapacity`, `country`, `externalPhotoUrl`, `gallery`, `coverPhotoUrl`.
- An `OfferEdit` schema with every field nullable, explicit absent-means-keep semantics, and no `photoObjectKey`/`ownerEmail` writability.
- Validation parity: required-vs-optional per operation matching `OfferDTO`/`OfferEditDTO` constraints, including the absolute-URL rule for `externalPhotoUrl`.
- No new required field without a migration path for sparse stored rows (the `NullPointerException`-on-null-merge class of defect).
