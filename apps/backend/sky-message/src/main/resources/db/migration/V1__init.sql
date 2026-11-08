CREATE TABLE IF NOT EXISTS message
(
    id UUID NOT NULL,
    text           TEXT         NOT NULL,
    created_time TIMESTAMPTZ,
    is_read        BOOLEAN      NOT NULL DEFAULT FALSE,
    receiver_email VARCHAR(255) NOT NULL,
    sender_email   VARCHAR(255) NOT NULL,
    PRIMARY KEY (id)
);
