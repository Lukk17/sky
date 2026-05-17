-- Step 1 of 3: create per-service schemas.
--
-- Idempotent. Safe to run repeatedly. Run as a user with CREATE DATABASE rights
-- (typically the same root user that owns the legacy `sky` schema today).
--
-- See split_schemas_002_move.sql for the data-move step and
-- split_schemas_003_cleanup.sql for the final drop of the old `sky` schema.

CREATE DATABASE IF NOT EXISTS sky_booking
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

CREATE DATABASE IF NOT EXISTS sky_offer
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

CREATE DATABASE IF NOT EXISTS sky_message
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;
