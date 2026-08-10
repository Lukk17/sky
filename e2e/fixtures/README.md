# e2e fixtures

Small canary files used by upload-style tests. Each fixture holds distinctive content (no overlap with model
training data) so a passing test proves the answer came from retrieval or the attached document rather than
memorised knowledge.

## Conventions

- One-line canary phrase in a `.md` file: invented place name + unique numeric ID. Example:
  `The HELENA-DEDUP-CANARY village holds the 17th annual pierogi festival every August 14th.`
- Short PDFs with invented proper nouns and specific recent retail prices.
- DOCX recipes with distinctive rest times (`Rest the dough for 47 minutes`, not `Rest for an hour`).
- Small images (~100 KB) with a recognisable but uncommon subject.
- Audio clips ≤ 60 seconds with one or two clearly enunciated invented words.

Keep fixtures small. A test should be able to upload them in under 2 seconds.

## Per-fixture documentation

Each fixture's distinctive content goes in a table here:

| File | Used by | Distinctive content |
| --- | --- | --- |
| `offer-photo.png` | `2-offer-crud-test.md`, `3-booking-flow-test.md` | 16x16 PNG carrying a `tEXt` chunk with `SKY-E2E-CANARY-OFFER-PHOTO-7F3A`. Real PNG magic bytes so the offer service photo-upload magic-byte validator accepts it; the canary string is unique enough that finding it in the MinIO `sky-offers` bucket proves the byte stream round-tripped rather than a memorised placeholder. |
