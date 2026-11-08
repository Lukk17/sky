# config/seed/photos

Layout:

- `hotels/<offer-slug>-exterior.png`: the cover photo. `photoFile` points here, and `config/seed/seed.mjs` uploads it to the single-photo slot and to the gallery, where it is the main photo.
- `hotels/<offer-slug>-interior.png`: the gallery photo. `photoFile2` points here, and the seed uploads it to the gallery.
- `rooms/<offer-slug>-room.jpg`: legacy room views, not referenced by the current seed data or scripts.

Every offer carries two photos. The seed skips an offer whose gallery already holds at least two photos, so a rerun is idempotent, and it fails when a referenced file is missing. Both PNG files come from `e2e/fixtures/offer-photo.png`, a neutral placeholder.
