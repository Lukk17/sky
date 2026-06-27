CREATE TABLE IF NOT EXISTS booking
(
    id UUID NOT NULL,
    offer_id UUID NOT NULL,
    booked_date  DATE         NOT NULL,
    booking_user VARCHAR(255) NOT NULL,
    owner_email  VARCHAR(255) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS booking_event
(
    id UUID NOT NULL,
    booking_id UUID NOT NULL,
    sequence_number INT NOT NULL,
    event_type      VARCHAR(32) CHECK (event_type IN ('BOOKED', 'CANCELED', 'RESERVED')),
    payload         TEXT,
    timestamp TIMESTAMPTZ,
    PRIMARY KEY (id)
);
