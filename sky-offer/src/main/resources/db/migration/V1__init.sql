-- Baseline schema for sky-offer. Reverse-engineered from
-- com.lukk.sky.offer.domain.model.{Offer, Event}.

CREATE TABLE IF NOT EXISTS offer (
    id            BIGINT         NOT NULL AUTO_INCREMENT,
    hotel_name    VARCHAR(255)   NOT NULL,
    description   VARCHAR(3000),
    comment       VARCHAR(1000),
    price         DECIMAL(38, 2) NOT NULL,
    owner_email   VARCHAR(100)   NOT NULL,
    room_capacity BIGINT         NOT NULL,
    city          VARCHAR(255)   NOT NULL,
    country       VARCHAR(255)   NOT NULL,
    photo_path    VARCHAR(255),
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS offer_event (
    id              BIGINT      NOT NULL AUTO_INCREMENT,
    offer_id        BIGINT      NOT NULL,
    sequence_number INT         NOT NULL,
    event_type      VARCHAR(255),
    payload         TEXT,
    timestamp       DATETIME(6),
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
