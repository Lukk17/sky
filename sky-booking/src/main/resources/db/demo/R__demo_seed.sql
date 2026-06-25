-- Demo seed for sky-booking (LOCAL profile only).
-- Repeatable migration: Flyway re-runs this whenever the checksum changes.
-- All inserts are idempotent via ON CONFLICT (id) DO NOTHING.
-- offer_id is stored as VARCHAR(255) in the booking table.

INSERT INTO booking (id, offer_id, booked_date, booking_user, owner_email)
VALUES
    (1, '1', '2027-07-15', 'user@sky.dev', 'owner@sky.dev'),
    (2, '2', '2027-08-20', 'user@sky.dev', 'owner@sky.dev')
ON CONFLICT (id) DO NOTHING;

SELECT setval(pg_get_serial_sequence('booking', 'id'), COALESCE((SELECT MAX(id) FROM booking), 1), (SELECT COUNT(*) FROM booking) > 0);

INSERT INTO booking_event (id, booking_id, sequence_number, event_type, payload, timestamp)
VALUES
    (1, 1, 1, 'BOOKED', '{"bookingId":1,"offerId":"1","bookingUser":"user@sky.dev","ownerEmail":"owner@sky.dev","bookedDate":"2027-07-15"}', now()),
    (2, 2, 1, 'BOOKED', '{"bookingId":2,"offerId":"2","bookingUser":"user@sky.dev","ownerEmail":"owner@sky.dev","bookedDate":"2027-08-20"}', now())
ON CONFLICT (id) DO NOTHING;

SELECT setval(pg_get_serial_sequence('booking_event', 'id'), COALESCE((SELECT MAX(id) FROM booking_event), 1), (SELECT COUNT(*) FROM booking_event) > 0);
