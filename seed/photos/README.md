# seed/photos

Naming convention: `<offer-slug>-1.jpg` (required, one photo per offer). `<offer-slug>-2.jpg` is optional material for the replace flow.

The backend accepts exactly one photo per offer (single `photoUrl` slot, replace semantics). Drop one JPEG per slug listed in [../offers.json](../offers.json), using the `photoFile` field as the file name.

No binary photos are committed here. As a fallback texture for manual uploads use [../../e2e/fixtures/offer-photo.png](../../e2e/fixtures/offer-photo.png).
