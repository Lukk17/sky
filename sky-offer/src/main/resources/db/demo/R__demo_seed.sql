-- Demo seed for sky-offer (LOCAL profile only).
-- Repeatable migration: Flyway re-runs this whenever the checksum changes.
-- All inserts are idempotent via ON CONFLICT (id) DO NOTHING.

INSERT INTO offer (id, hotel_name, description, comment, price, owner_email, room_capacity, city, country, photo_path)
VALUES
    (1, 'Sopot Beach Hotel', 'Charming beachside hotel in Sopot with direct access to the Baltic Sea.', 'Breakfast included', 320.00, 'owner@sky.dev', 2, 'Sopot', 'Poland', 'https://images.pexels.com/photos/1134176/pexels-photo-1134176.jpeg?auto=compress&cs=tinysrgb&dpr=2&w=500'),
    (2, 'Warsaw City Suites', 'Modern suites in the heart of Warsaw, walking distance from the Old Town.', 'Free parking', 480.00, 'owner@sky.dev', 3, 'Warsaw', 'Poland', 'https://images.pexels.com/photos/2373201/pexels-photo-2373201.jpeg?auto=compress&cs=tinysrgb&h=750&w=1260'),
    (3, 'Miami Ocean Resort', 'Luxury resort on Miami Beach with ocean views and pool access.', 'Late check-out available', 950.00, 'owner@sky.dev', 4, 'Miami', 'USA', 'https://images.pexels.com/photos/1179156/pexels-photo-1179156.jpeg?auto=compress&cs=tinysrgb&h=750&w=1260')
ON CONFLICT (id) DO NOTHING;

SELECT setval(pg_get_serial_sequence('offer', 'id'), (SELECT MAX(id) FROM offer));

INSERT INTO offer_event (id, offer_id, sequence_number, event_type, payload, timestamp)
VALUES
    (1, 1, 1, 'OFFER_CREATED', '{"id":1,"hotelName":"Sopot Beach Hotel","ownerEmail":"owner@sky.dev","city":"Sopot","country":"Poland","price":320.00}', now()),
    (2, 2, 1, 'OFFER_CREATED', '{"id":2,"hotelName":"Warsaw City Suites","ownerEmail":"owner@sky.dev","city":"Warsaw","country":"Poland","price":480.00}', now()),
    (3, 3, 1, 'OFFER_CREATED', '{"id":3,"hotelName":"Miami Ocean Resort","ownerEmail":"owner@sky.dev","city":"Miami","country":"USA","price":950.00}', now())
ON CONFLICT (id) DO NOTHING;

SELECT setval(pg_get_serial_sequence('offer_event', 'id'), (SELECT MAX(id) FROM offer_event));
