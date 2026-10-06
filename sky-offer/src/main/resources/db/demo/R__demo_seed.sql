-- Demo seed for sky-offer (LOCAL profile only).
-- Repeatable migration: Flyway re-runs this whenever the checksum changes.
-- All inserts are idempotent via ON CONFLICT (id) DO NOTHING.

INSERT INTO offer (id, hotel_name, description, comment, price, owner_email, room_capacity, city, country, external_photo_url)
VALUES ('11111111-1111-4111-8111-111111111111', 'Sopot Beach Hotel',
        'Charming beachside hotel in Sopot with direct access to the Baltic Sea.',
        'Breakfast included', 320.00, 'owner@sky.dev', 2, 'Sopot', 'Poland',
        'https://images.pexels.com/photos/1134176/pexels-photo-1134176.jpeg?auto=compress&cs=tinysrgb&dpr=2&w=500'),
       ('22222222-2222-4222-8222-222222222222', 'Warsaw City Suites',
        'Modern suites in the heart of Warsaw, walking distance from the Old Town.',
        'Free parking', 480.00, 'owner@sky.dev', 3, 'Warsaw', 'Poland',
        'https://images.pexels.com/photos/2373201/pexels-photo-2373201.jpeg?auto=compress&cs=tinysrgb&h=750&w=1260'),
       ('33333333-3333-4333-8333-333333333333', 'Miami Ocean Resort',
        'Luxury resort on Miami Beach with ocean views and pool access.',
        'Late check-out available', 950.00, 'owner@sky.dev', 4, 'Miami', 'USA',
        'https://images.pexels.com/photos/1179156/pexels-photo-1179156.jpeg?auto=compress&cs=tinysrgb&h=750&w=1260')
ON CONFLICT
    (id)
    DO NOTHING;

INSERT INTO offer_event (id, offer_id, sequence_number, event_type, payload, timestamp)
VALUES ('aaaaaaaa-1111-4111-8111-111111111111', '11111111-1111-4111-8111-111111111111', 1, 'OFFER_CREATED',
        '{"id":"11111111-1111-4111-8111-111111111111","hotelName":"Sopot Beach Hotel","ownerEmail":"owner@sky.dev","city":"Sopot","country":"Poland","price":320.00}',
        now()),
       ('aaaaaaaa-2222-4222-8222-222222222222', '22222222-2222-4222-8222-222222222222', 1, 'OFFER_CREATED',
        '{"id":"22222222-2222-4222-8222-222222222222","hotelName":"Warsaw City Suites","ownerEmail":"owner@sky.dev","city":"Warsaw","country":"Poland","price":480.00}',
        now()),
       ('aaaaaaaa-3333-4333-8333-333333333333', '33333333-3333-4333-8333-333333333333', 1, 'OFFER_CREATED',
        '{"id":"33333333-3333-4333-8333-333333333333","hotelName":"Miami Ocean Resort","ownerEmail":"owner@sky.dev","city":"Miami","country":"USA","price":950.00}',
        now())
ON CONFLICT
    (id)
    DO NOTHING;
