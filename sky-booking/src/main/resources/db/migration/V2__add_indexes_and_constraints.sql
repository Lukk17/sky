CREATE INDEX idx_booking_booking_user ON booking (booking_user);
CREATE INDEX idx_booking_offer_id ON booking (offer_id);
ALTER TABLE booking_event ADD CONSTRAINT uq_booking_event_booking_seq UNIQUE (booking_id, sequence_number);
