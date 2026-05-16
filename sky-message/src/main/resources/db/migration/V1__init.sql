-- Baseline schema for sky-message. Reverse-engineered from
-- com.lukk.sky.message.domain.model.Message.

CREATE TABLE IF NOT EXISTS message (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    text           TEXT         NOT NULL,
    created_time   DATETIME(6),
    is_read        BIT(1)       NOT NULL,
    receiver_email VARCHAR(255) NOT NULL,
    sender_email   VARCHAR(255) NOT NULL,
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
