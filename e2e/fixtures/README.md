# e2e fixtures

Small canary files used by upload-style tests. Each fixture holds distinctive content (no overlap with model
training data) so a passing test proves the answer came from retrieval or the attached document rather than
memorised knowledge.

## Conventions

- One-line canary phrase in a `.md` file: invented place name + unique numeric ID. Example:
  `The HELENA-DEDUP-CANARY village holds the 17th annual pierogi festival every August 14th.`
- Short PDFs with invented proper nouns and specific recent retail prices.
- DOCX recipes with distinctive rest times (`Rest the dough for 47 minutes`, not `Rest for an hour`).
- Small images (~100 KB) with a recognisable but uncommon subject, plus the canary phrase in a metadata text field
  (a PNG `tEXt` chunk, an EXIF comment) rather than only in the picture, because an assertion can read metadata out
  of the stored bytes and cannot read pixels.
- Audio clips ≤ 60 seconds with one or two clearly enunciated invented words.

Keep fixtures small. A test should be able to upload them in under 2 seconds.

## Per-fixture documentation

Each fixture's distinctive content goes in a table here:

| File | Used by | Distinctive content |
| --- | --- | --- |
| [offer-photo.png](offer-photo.png) | [../testing/2-offer-crud-test.md](../testing/2-offer-crud-test.md), [../testing/3-booking-flow-test.md](../testing/3-booking-flow-test.md) | 400x200 PNG carrying the canary marker `SKY-OFFER-PHOTO-CANARY-4471` in a `tEXt` chunk (keyword `Comment`) placed directly after `IHDR`. Chunk order is `IHDR, tEXt, sRGB, gAMA, pHYs, IDAT, IEND`. The marker sits in metadata, so the 8-byte PNG signature and the image data are untouched and `URLConnection.guessContentTypeFromStream`, which is what the `sky-offer` upload endpoint sniffs with, still reports `image/png`. [../../docs/api/request/offer/upload-photo.yml](../../docs/api/request/offer/upload-photo.yml) posts this file itself, through the relative path `../../../e2e/fixtures/offer-photo.png`: Bruno joins a multipart file path onto the collection root and lets it leave again, so one copy of the image serves both the Bruno collection and the e2e specs. The upload request fetches the presigned URL back and asserts the marker is in the stored bytes, which is what makes the assertion name this file rather than any image. |
