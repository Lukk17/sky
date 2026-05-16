-- Baseline schema for sky-booking. Reverse-engineered from
-- com.lukk.sky.booking.domain.model.{Booking, Event} (Hibernate 6 +
-- SpringPhysicalNamingStrategy → snake_case columns, lower_case table
-- names, explicit @Table for booking_event).
--
-- Verify against a clean MySQL on first deploy. Mismatches surface at
-- startup because spring.jpa.hibernate.ddl-auto is `validate`.

CREATE TABLE IF NOT EXISTS booking (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    offer_id      VARCHAR(255) NOT NULL,
    booked_date   DATE         NOT NULL,
    booking_user  VARCHAR(255) NOT NULL,
    owner_email   VARCHAR(255) NOT NULL,
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS booking_event (
    id              BIGINT      NOT NULL AUTO_INCREMENT,
    booking_id      BIGINT,
    sequence_number INT         NOT NULL,
    event_type      VARCHAR(255),
    payload         TEXT,
    timestamp       DATETIME(6),
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
