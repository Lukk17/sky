-- Step 3 of 3: drop the legacy shared `sky` schema.
--
-- RUN ONLY AFTER:
--   1. split_schemas_002_move.sql completed and row counts verified.
--   2. All services restarted against their new JDBC URLs.
--   3. Smoke test (Postman collection or live traffic) confirmed end-to-end
--      flows are healthy.
--   4. A backup of the legacy schema is on disk (split_schemas_002_move.sql
--      pre-run checklist item #1).
--
-- This is destructive. After this runs, the old `sky` schema is gone and the
-- only way back is the backup.

DROP DATABASE IF EXISTS sky;
