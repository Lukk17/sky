-- Demo seed for sky-booking (LOCAL profile only).
-- Repeatable migration: Flyway re-runs this whenever the checksum changes.
-- All inserts are idempotent via ON CONFLICT (id) DO NOTHING.

INSERT INTO booking (id, offer_id, booked_date, booking_user, owner_email)
VALUES ('00000000-0000-0000-0000-00000000b001', '00000000-0000-0000-0000-00000000f001',
        '2027-07-15', 'user@sky.dev', 'owner@sky.dev'),
       ('00000000-0000-0000-0000-00000000b002', '00000000-0000-0000-0000-00000000f002',
        '2027-08-20', 'user@sky.dev', 'owner@sky.dev')
ON CONFLICT (id) DO NOTHING;

INSERT INTO booking_event (id, booking_id, sequence_number, event_type, payload, timestamp)
VALUES ('00000000-0000-0000-0000-00000000e001', '00000000-0000-0000-0000-00000000b001', 1, 'BOOKED',
        '{"id":"00000000-0000-0000-0000-00000000b001","offerId":"00000000-0000-0000-0000-00000000f001","bookedDate":"2027-07-15","bookingUser":"user@sky.dev","ownerEmail":"owner@sky.dev"}',
        now()),
       ('00000000-0000-0000-0000-00000000e002', '00000000-0000-0000-0000-00000000b002', 1, 'BOOKED',
        '{"id":"00000000-0000-0000-0000-00000000b002","offerId":"00000000-0000-0000-0000-00000000f002","bookedDate":"2027-08-20","bookingUser":"user@sky.dev","ownerEmail":"owner@sky.dev"}',
        now())
ON CONFLICT (id) DO NOTHING;
