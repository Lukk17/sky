# seed/photos

Layout:

- `hotels/<offer-slug>-exterior.jpg`: live photo. One per offer in `seed/offers.json`. The `photoFile` value points here. The backend has a single photo slot per offer, so this is the only image uploaded by `seed/seed.mjs`.
- `rooms/<offer-slug>-room.jpg`: room view, kept for the future. Rooms have nowhere to upload today. The backend accepts exactly one photo per offer, so these files are not referenced by any seed data or script.
- `unused/`: spare exterior images, never referenced by seed data or scripts.

Gallery follow-up: add a multi-photo gallery to the backend and the seed script, then wire each `rooms/` file to its offer.

Mapping note: all dropped exteriors show palm-style resort buildings, so city matching is by best fit, not by real look. Daylight pool views went to the three Miami offers. Night and dome views went to the Warsaw tower and residence offers. The remaining views went to Gdansk and Gorzow Wielkopolski.
