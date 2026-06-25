-- Demo seed for sky-message (LOCAL profile only).
-- Repeatable migration: Flyway re-runs this whenever the checksum changes.
-- All inserts are idempotent via ON CONFLICT (id) DO NOTHING.

INSERT INTO message (id, text, created_time, is_read, receiver_email, sender_email)
VALUES
    (1, 'Hi! I am interested in booking the Sopot Beach Hotel. Is it available in July?', now(), true,  'owner@sky.dev', 'user@sky.dev'),
    (2, 'Hello! Yes, the Sopot Beach Hotel is available in July. I have reserved it for you.', now(), true,  'user@sky.dev',  'owner@sky.dev'),
    (3, 'Thank you! Looking forward to the stay.', now(), false, 'owner@sky.dev', 'user@sky.dev')
ON CONFLICT (id) DO NOTHING;

SELECT setval(pg_get_serial_sequence('message', 'id'), COALESCE((SELECT MAX(id) FROM message), 1), (SELECT COUNT(*) FROM message) > 0);
