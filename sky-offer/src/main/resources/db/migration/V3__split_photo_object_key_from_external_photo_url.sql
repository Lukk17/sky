ALTER TABLE offer
    ADD COLUMN photo_object_key VARCHAR(512);

ALTER TABLE offer
    RENAME COLUMN photo_path TO external_photo_url;

ALTER TABLE offer
    ALTER COLUMN external_photo_url TYPE VARCHAR(1024);
