CREATE TABLE IF NOT EXISTS offer (
    id            UUID          NOT NULL,
    hotel_name    VARCHAR(255)  NOT NULL,
    description   VARCHAR(3000),
    comment       VARCHAR(1000),
    price         NUMERIC(12,2) NOT NULL,
    owner_email   VARCHAR(100)  NOT NULL,
    room_capacity BIGINT        NOT NULL,
    city          VARCHAR(255)  NOT NULL,
    country       VARCHAR(255)  NOT NULL,
    photo_path    VARCHAR(255),
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS offer_event (
    id              UUID        NOT NULL,
    offer_id        UUID        NOT NULL,
    sequence_number INT         NOT NULL,
    event_type      VARCHAR(32) CHECK (event_type IN ('OFFER_CREATED', 'OFFER_UPDATED', 'OFFER_DELETED')),
    payload         TEXT,
    timestamp       TIMESTAMPTZ,
    PRIMARY KEY (id)
);
