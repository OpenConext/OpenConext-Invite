-- Step 1 of 2: EXTRACT. Run this against the BACKUP database (invite_prd_backup) ONLY.
--
-- It never touches the live database -- it purely dumps a self-contained SQL script (a
-- staging table plus INSERT statements) built from the backup's original '@' user rows. That
-- avoids the earlier bug where an unqualified `users` reference silently resolved to whatever
-- database the script happened to be connected to: run against the backup, every check ended
-- up comparing the backup's own un-migrated duplicate against itself, so every row looked
-- like a conflict. This script only ever reads from the backup and only ever emits literal
-- SQL text as its result set -- it does not update anything itself.
--
-- Usage:
--   mysql -N -u root invite_prd_backup < server/src/test/resources/restore_crm_info_extract.sql > crm_restore_staging_dump.sql
--
-- (-N suppresses the column header so the captured output is pure, directly runnable SQL.)
-- Then continue with step 2: run crm_restore_staging_dump.sql against the LIVE database
-- (invite_prd) to populate the staging table, followed by restore_crm_info_apply.sql.

SELECT 'DROP TABLE IF EXISTS crm_restore_staging;' AS statement
UNION ALL
SELECT 'CREATE TABLE crm_restore_staging (target_sub VARCHAR(255) NOT NULL PRIMARY KEY, crm_contact_id VARCHAR(255) NULL, organisation_id BIGINT NULL);'
UNION ALL
SELECT CONCAT('INSERT INTO crm_restore_staging (target_sub, crm_contact_id, organisation_id) VALUES (',
              QUOTE(REPLACE(sub, '@', '_')), ', ',
              COALESCE(QUOTE(crm_contact_id), 'NULL'), ', ',
              COALESCE(CAST(organisation_id AS CHAR), 'NULL'), ');')
FROM users
WHERE sub LIKE '%@%'
  AND (crm_contact_id IS NOT NULL OR organisation_id IS NOT NULL);
