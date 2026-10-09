CREATE TABLE IF NOT EXISTS offer_photo
(
    id UUID NOT NULL PRIMARY KEY,
    offer_id UUID NOT NULL REFERENCES offer (id) ON DELETE CASCADE,
    position INT NOT NULL CHECK (position >= 0),
    object_key VARCHAR(512),
    external_url VARCHAR(1024),
    CONSTRAINT offer_photo_one_source CHECK (
        (object_key IS NOT NULL AND object_key <> '') OR (external_url IS NOT NULL AND external_url <> '')
    ),
    CONSTRAINT uq_offer_photo_offer_position UNIQUE (offer_id, position)
);

CREATE INDEX IF NOT EXISTS idx_offer_photo_offer_position ON offer_photo (offer_id, position);

INSERT INTO offer_photo (id, offer_id, position, object_key, external_url)
SELECT gen_random_uuid(), id, 0, photo_object_key, NULL
FROM offer
WHERE photo_object_key IS NOT NULL AND photo_object_key <> ''
ON CONFLICT (offer_id, position) DO NOTHING;

INSERT INTO offer_photo (id, offer_id, position, object_key, external_url)
SELECT gen_random_uuid(), id, 0, NULL, external_photo_url
FROM offer
WHERE (photo_object_key IS NULL OR photo_object_key = '')
  AND external_photo_url IS NOT NULL AND external_photo_url <> ''
ON CONFLICT (offer_id, position) DO NOTHING;
