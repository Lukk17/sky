# Spec Delta

## Purpose

Lets owners attach an ordered set of photos to an offer so listings show the room, the view, and details instead of a single image.

## ADDED Requirements

### Requirement: Offer carries an ordered photo gallery

Each offer SHALL expose an ordered gallery of zero or more photos. The gallery order defines display order, position 0 is the cover photo, and an empty gallery SHALL render the empty state with no cover. Adding, removing, or reordering photos SHALL NOT change any other offer field.

#### Scenario: Gallery order defines the cover

- **WHEN** an offer has three photos in a defined order
- **THEN** the gallery lists all three in that order and position 0 is reported as the cover photo

#### Scenario: Empty gallery has no cover

- **WHEN** an offer has no photos
- **THEN** the gallery is empty and no cover photo is reported

### Requirement: Gallery migration preserves the existing single photo

Existing single-slot photo values SHALL be preserved exactly once as gallery position 0. When both a stored object key and an external URL exist, the stored object wins. Offers with neither SHALL migrate to an empty gallery.

#### Scenario: Stored object takes precedence over external URL

- **WHEN** an offer has both a stored photo object and an external photo URL
- **THEN** the migrated gallery position 0 references the stored object and the external URL is recorded as a fallback source

#### Scenario: Offer without any photo migrates empty

- **WHEN** an offer has no stored photo object and no external photo URL
- **THEN** the migrated gallery is empty

### Requirement: Gallery API shape supports list, upload, reorder, and delete

Offer reads SHALL return the full ordered gallery including each photo identifier, its position, and a retrievable URL. Clients SHALL be able to upload a new photo to the end of the gallery, move a photo to a new position, set any photo as cover, and delete a photo. Deleting a photo SHALL remove its stored object. Invalid photo references SHALL be rejected with a client error and SHALL NOT change the gallery.

#### Scenario: Upload appends and delete removes the stored object

- **WHEN** a client uploads a photo and later deletes it
- **THEN** the upload appends it at the end with a retrievable URL and the delete removes it from the gallery and its stored object

#### Scenario: Reorder changes the cover

- **WHEN** a client moves the photo at position 2 to position 0
- **THEN** the gallery reflects the new order and the moved photo becomes the cover

#### Scenario: Invalid photo reference is rejected

- **WHEN** a client reorders or deletes a photo identifier the offer does not have
- **THEN** the API answers a client error and the gallery is unchanged

### Requirement: Frontend renders the gallery with cover, thumbnails, and empty state

Offer views SHALL render the gallery cover prominently with thumbnails for the remaining photos, allow selecting any thumbnail as the viewed photo, and render an empty state when the gallery is empty. Card or list views SHALL use the cover photo.

#### Scenario: Viewer navigates thumbnails

- **WHEN** a visitor opens an offer with three photos and selects the second thumbnail
- **THEN** the viewer shows the second photo with all three thumbnails visible

#### Scenario: Empty gallery shows a placeholder

- **WHEN** a visitor opens an offer with no photos
- **THEN** the view shows an empty-state placeholder instead of a broken image
