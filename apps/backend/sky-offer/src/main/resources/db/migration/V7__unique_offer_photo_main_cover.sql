DROP INDEX IF EXISTS idx_offer_photo_offer_main;

CREATE UNIQUE INDEX IF NOT EXISTS idx_offer_photo_offer_main ON offer_photo (offer_id) WHERE main = TRUE;
