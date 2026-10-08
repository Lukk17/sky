# Proposal

## Why

An offer today carries exactly one photo slot (server-owned `Offer.photoObjectKey` plus `externalPhotoUrl`). Owners cannot show a room, a view, and a detail shot together, so listings look thin next to real booking sites.

## What Changes

- Add an ordered photo gallery per offer (0..N photos) replacing the single photo slot as the source of truth for rendering.
- Migrate existing single-slot values (`photo_object_key`, external URL) into gallery row 0 with defined precedence.
- Define S3 key layout for gallery objects, upload and delete rules, and ordering semantics.
- Change offer API shape: gallery array in responses, multi-photo upload input, cover photo derivation.
- Change frontend rendering: gallery viewer with cover, thumbnails, and empty state.
- Keep backward compatibility during migration (read old slot until backfill completes, then remove).

## Capabilities

### New Capabilities

- `offer-photo-gallery`: ordered multi-photo gallery per offer covering storage, migration, API shape, and rendering.

### Modified Capabilities

- `offer-crud-e2e`: the owner lifecycle runbook covers one photo upload today and must cover gallery upload, reorder, delete, and cover derivation.

## Impact

- sky-offer: new gallery table and migration, S3 key layout in `sky-offers` bucket, REST API shape, photo upload and delete paths.
- sky-booking: read-only consumers of offer photo data (cover photo for booking views) pick up the new shape.
- Frontend (Sky-View): offer detail and card rendering switch from single image to gallery.
- Seed and e2e fixtures: gallery-aware seed data and runbook fixture usage.
