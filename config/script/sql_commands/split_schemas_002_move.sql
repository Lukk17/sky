-- Step 2 of 3: copy each service's tables from the legacy `sky` schema into its
-- per-service schema.
--
-- PRE-RUN CHECKLIST (do these BEFORE running this script):
--   1. Take a full backup of `sky`:
--        mysqldump --single-transaction --routines --triggers sky > sky_backup.sql
--   2. Stop sky-booking, sky-offer, sky-message (cleanest if no writes are in
--      flight while data moves). sky-notify is unaffected (no DB).
--   3. Run split_schemas_001_create.sql to make sure target schemas exist.
--
-- POST-RUN CHECKLIST:
--   1. Verify row counts match (see the SELECT count(*) blocks at the bottom of
--      this script — uncomment to run).
--   2. Verify AUTO_INCREMENT values are aligned (the ALTER TABLE statements at
--      the end of each service's block do this).
--   3. Reconfigure the three services to point at their new JDBC URLs:
--        sky-booking: jdbc:mysql://<host>:3306/sky_booking
--        sky-offer:   jdbc:mysql://<host>:3306/sky_offer
--        sky-message: jdbc:mysql://<host>:3306/sky_message
--      (Helm values + env vars; Helm chart already templates these per-service.)
--   4. Restart the services. Each Flyway run will baseline its own schema.
--   5. Once smoke-tests pass, run split_schemas_003_cleanup.sql to drop `sky`.

-- ============================================================================
-- sky-booking
-- ============================================================================

CREATE TABLE IF NOT EXISTS sky_booking.booking LIKE sky.booking;
INSERT INTO sky_booking.booking SELECT * FROM sky.booking;
SET @max_booking_id := (SELECT COALESCE(MAX(id), 0) + 1 FROM sky_booking.booking);
SET @stmt := CONCAT('ALTER TABLE sky_booking.booking AUTO_INCREMENT = ', @max_booking_id);
PREPARE s FROM @stmt; EXECUTE s; DEALLOCATE PREPARE s;

CREATE TABLE IF NOT EXISTS sky_booking.booking_event LIKE sky.booking_event;
INSERT INTO sky_booking.booking_event SELECT * FROM sky.booking_event;
SET @max_booking_event_id := (SELECT COALESCE(MAX(id), 0) + 1 FROM sky_booking.booking_event);
SET @stmt := CONCAT('ALTER TABLE sky_booking.booking_event AUTO_INCREMENT = ', @max_booking_event_id);
PREPARE s FROM @stmt; EXECUTE s; DEALLOCATE PREPARE s;

-- ============================================================================
-- sky-offer
-- ============================================================================

CREATE TABLE IF NOT EXISTS sky_offer.offer LIKE sky.offer;
INSERT INTO sky_offer.offer SELECT * FROM sky.offer;
SET @max_offer_id := (SELECT COALESCE(MAX(id), 0) + 1 FROM sky_offer.offer);
SET @stmt := CONCAT('ALTER TABLE sky_offer.offer AUTO_INCREMENT = ', @max_offer_id);
PREPARE s FROM @stmt; EXECUTE s; DEALLOCATE PREPARE s;

CREATE TABLE IF NOT EXISTS sky_offer.offer_event LIKE sky.offer_event;
INSERT INTO sky_offer.offer_event SELECT * FROM sky.offer_event;
SET @max_offer_event_id := (SELECT COALESCE(MAX(id), 0) + 1 FROM sky_offer.offer_event);
SET @stmt := CONCAT('ALTER TABLE sky_offer.offer_event AUTO_INCREMENT = ', @max_offer_event_id);
PREPARE s FROM @stmt; EXECUTE s; DEALLOCATE PREPARE s;

-- ============================================================================
-- sky-message
-- ============================================================================

CREATE TABLE IF NOT EXISTS sky_message.message LIKE sky.message;
INSERT INTO sky_message.message SELECT * FROM sky.message;
SET @max_message_id := (SELECT COALESCE(MAX(id), 0) + 1 FROM sky_message.message);
SET @stmt := CONCAT('ALTER TABLE sky_message.message AUTO_INCREMENT = ', @max_message_id);
PREPARE s FROM @stmt; EXECUTE s; DEALLOCATE PREPARE s;

-- ============================================================================
-- Row-count verification (uncomment to run after the moves)
-- ============================================================================
--
-- SELECT 'booking'        AS tbl, (SELECT COUNT(*) FROM sky.booking)        AS old_count, (SELECT COUNT(*) FROM sky_booking.booking)        AS new_count
-- UNION ALL SELECT 'booking_event',  (SELECT COUNT(*) FROM sky.booking_event),  (SELECT COUNT(*) FROM sky_booking.booking_event)
-- UNION ALL SELECT 'offer',          (SELECT COUNT(*) FROM sky.offer),          (SELECT COUNT(*) FROM sky_offer.offer)
-- UNION ALL SELECT 'offer_event',    (SELECT COUNT(*) FROM sky.offer_event),    (SELECT COUNT(*) FROM sky_offer.offer_event)
-- UNION ALL SELECT 'message',        (SELECT COUNT(*) FROM sky.message),        (SELECT COUNT(*) FROM sky_message.message);
