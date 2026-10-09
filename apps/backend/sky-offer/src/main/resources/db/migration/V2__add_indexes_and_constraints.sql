CREATE INDEX idx_offer_owner_email ON offer (owner_email);

ALTER TABLE offer_event
    ADD CONSTRAINT uq_offer_event_offer_seq UNIQUE (offer_id, sequence_number);
