ALTER TABLE offer_photo ADD COLUMN IF NOT EXISTS main BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE offer_photo SET main = TRUE WHERE position = 0;

CREATE INDEX IF NOT EXISTS idx_offer_photo_offer_main ON offer_photo (offer_id) WHERE main = TRUE;
