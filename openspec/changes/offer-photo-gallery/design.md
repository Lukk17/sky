# Design

## Context

See proposal.md Why. Current state: `Offer.photoObjectKey` (server-owned column `photo_object_key`) plus `externalPhotoUrl` form the single photo slot in sky-offer, with objects in the `sky-offers` bucket. Reads return one photo. This design adds a gallery without breaking in-flight clients. The new spec is `specs/offer-photo-gallery/spec.md`, and the runbook delta is `specs/offer-crud-e2e/spec.md`.

## Goals / Non-Goals

**Goals:**

- Ordered gallery per offer with cover derived from position 0.
- Lossless one-shot migration of the single slot into gallery row 0.
- Bounded S3 growth: delete removes the object.

**Non-Goals:**

- Image transcoding, thumbnails generation, or CDN changes.
- Reordering across offers or sharing one photo between offers.
- Backfilling gallery data into historical event topics.

## Decisions

- New `offer_photo` table (FK to offer, `position`, `object_key` nullable, `external_url` nullable, unique per offer and position) over a Postgres array column. Rationale: ordering, per-photo delete, and FK cleanup are relational. Alternative (array of keys on `offer`) rejected: no per-photo metadata and painful partial updates.
- S3 key layout `offers/{offerId}/{photoId}-{filename}` over reusing the flat single-slot keys. Rationale: collision-free per photo and deletable per photo. Alternative (keep flat `{offerId}` keys) rejected: overwrites on second upload.
- Expand-contract migration: add table and dual-write reads (gallery first, fall back to legacy slot), backfill legacy values once, then remove legacy columns in a later migration. Rationale: zero-downtime cutover with rollback by reading the old slot. Alternative (flag-day column drop) rejected: breaks reads mid-deploy.
- Gallery endpoints under the offer resource (`POST/DELETE /photos`, `PATCH` order/cover) returning the full gallery on offer reads, with `coverPhoto` kept as a derived convenience field during deprecation. Rationale: keeps disjoint top-level edge resources unchanged. Alternative (new top-level `/photos` resource) rejected: requires gateway and ingress additions per repo edge rules.
- Frontend normalizes gallery once at the API client boundary; detail view shows cover plus thumbnails, cards use cover only. Rationale: one mapping point instead of per-component fallbacks.

## Risks / Trade-offs

- [Risk] Legacy slot and gallery diverge during dual-write window → Mitigation: gallery is write authority, legacy slot read-only fallback, backfill verified by count comparison before column drop.
- [Risk] Orphaned S3 objects on failed deletes → Mitigation: delete DB row only after object delete succeeds, plus periodic orphan reconciliation job.
- [Risk] Large galleries slow offer reads → Mitigation: cap gallery size at 10 with a 413 on overflow, paginate only if measured read latency regresses.

## Migration Plan

1. Deploy: new table, gallery write paths, dual reads.
2. Backfill: one Flyway migration inserting row 0 from `photo_object_key` (win on conflict) else `externalPhotoUrl`.
3. Verify: row counts match legacy non-null counts, canary offer round-trips.
4. Cut over reads to gallery-only, deprecate `coverPhoto` field, drop legacy columns in a follow-up release.
5. Rollback: before step 4, revert to legacy-slot reads; gallery rows are ignored.

## Decided

- Gallery cap is 10 photos per offer.
- Only stored uploads are allowed, no external URLs for new photos.
