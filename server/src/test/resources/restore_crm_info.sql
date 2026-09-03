-- One-off data-repair script: restore `users.crm_contact_id` / `users.organisation_id` values
-- lost by a bug in dedupe_users.sql. That script deleted the '@' duplicate `users` rows
-- without carrying over crm_contact_id/organisation_id to the surviving '_' user, so any
-- values that only existed on the deleted '@' row are gone from the live database. This
-- script recovers them from a backup taken before dedupe_users.sql ran, and reapplies them to
-- the surviving user.
--
-- ASSUMPTION: the backup lives in a sibling database on the same MariaDB instance, named
-- `invite_prd_backup` (per the mysqldump-based backup taken before dedupe_users.sql ran), and
-- it still contains the original '@' user rows with their original crm_contact_id /
-- organisation_id values intact. Adjust the `invite_prd_backup.users` references below if your
-- backup is named or structured differently.
--
-- `users` has a UNIQUE constraint `users_unique_crm_contact_profile` on
-- (crm_contact_id, organisation_id) (see V55_0__crm_roles_users.sql). Existing non-NULL values
-- on the target are never overwritten, and a fill is skipped (and flagged for manual review)
-- if it would produce a (crm_contact_id, organisation_id) pair that already belongs to a
-- different user, to avoid violating that constraint.
--
-- Usage: mysql -u root invite_prd < server/src/test/resources/restore_crm_info.sql

-- ============================================================================
-- DRY RUN -- read-only, run this first and review before proceeding
-- ============================================================================

-- Snapshot of every backup '@' user that had CRM info, matched to its surviving '_' user,
-- with the value each column would end up with (existing value wins if already set).
CREATE TEMPORARY TABLE crm_restore_map AS
SELECT b.id                                            AS backup_source_id,
       b.sub                                            AS backup_source_sub,
       u.id                                             AS target_id,
       u.sub                                             AS target_sub,
       b.crm_contact_id                                 AS backup_crm_contact_id,
       b.organisation_id                                AS backup_organisation_id,
       u.crm_contact_id                                 AS current_crm_contact_id,
       u.organisation_id                                AS current_organisation_id,
       COALESCE(u.crm_contact_id, b.crm_contact_id)     AS new_crm_contact_id,
       COALESCE(u.organisation_id, b.organisation_id)   AS new_organisation_id
FROM invite_prd_backup.users b
         JOIN users u ON u.sub = REPLACE(b.sub, '@', '_')
WHERE b.sub LIKE '%@%'
  AND (b.crm_contact_id IS NOT NULL OR b.organisation_id IS NOT NULL);

-- Flag rows where filling in the NULLs would produce a (crm_contact_id, organisation_id) pair
-- that a DIFFERENT user already has -- applying these would violate the unique constraint.
ALTER TABLE crm_restore_map
    ADD COLUMN has_unique_conflict BOOLEAN DEFAULT FALSE;

UPDATE crm_restore_map m
SET has_unique_conflict = EXISTS (SELECT 1
                                   FROM users u2
                                   WHERE u2.id <> m.target_id
                                     AND m.new_crm_contact_id IS NOT NULL
                                     AND m.new_organisation_id IS NOT NULL
                                     AND u2.crm_contact_id = m.new_crm_contact_id
                                     AND u2.organisation_id = m.new_organisation_id);

-- All candidate rows found in the backup
SELECT *
FROM crm_restore_map;

-- Backup '@' rows with CRM info but no matching current '_' user at all -- shouldn't happen
-- (every merged pair should still have its target), investigate manually if this is non-empty.
SELECT b.id, b.sub, b.crm_contact_id, b.organisation_id
FROM invite_prd_backup.users b
WHERE b.sub LIKE '%@%'
  AND (b.crm_contact_id IS NOT NULL OR b.organisation_id IS NOT NULL)
  AND NOT EXISTS (SELECT 1 FROM users u WHERE u.sub = REPLACE(b.sub, '@', '_'));

-- Field-level conflicts: the target already has a non-NULL value that differs from the
-- backup's value for that column. Never overwritten -- review manually.
SELECT *
FROM crm_restore_map
WHERE (current_crm_contact_id IS NOT NULL AND current_crm_contact_id <> backup_crm_contact_id)
   OR (current_organisation_id IS NOT NULL AND current_organisation_id <> backup_organisation_id);

-- Unique-constraint conflicts: would collide with a different user's existing
-- (crm_contact_id, organisation_id) pair. Excluded from FIX -- review manually.
SELECT *
FROM crm_restore_map
WHERE has_unique_conflict = TRUE;

-- Rows that will actually be updated by FIX
SELECT *
FROM crm_restore_map
WHERE has_unique_conflict = FALSE
  AND ((current_crm_contact_id IS NULL AND backup_crm_contact_id IS NOT NULL)
    OR (current_organisation_id IS NULL AND backup_organisation_id IS NOT NULL));

-- Counts before the fix, for a sanity check against the "after" counts in VERIFY
SELECT COUNT(*) AS users_with_crm_info_before
FROM users
WHERE crm_contact_id IS NOT NULL
   OR organisation_id IS NOT NULL;

-- ============================================================================
-- STATEMENTS TO BE EXECUTED -- individual UPDATE statements, for manual/visual inspection
-- before FIX runs. This is exactly what FIX (below) will do, row by row.
-- ============================================================================

SELECT CONCAT('UPDATE users SET crm_contact_id = ', COALESCE(QUOTE(new_crm_contact_id), 'NULL'),
              ', organisation_id = ', COALESCE(CAST(new_organisation_id AS CHAR), 'NULL'),
              ' WHERE id = ', target_id, ';') AS statement
FROM crm_restore_map
WHERE has_unique_conflict = FALSE
  AND ((current_crm_contact_id IS NULL AND backup_crm_contact_id IS NOT NULL)
    OR (current_organisation_id IS NULL AND backup_organisation_id IS NOT NULL))
ORDER BY target_id;

-- ============================================================================
-- FIX -- only proceed once the dry-run and statement output above have been reviewed
-- ============================================================================

UPDATE users u
    JOIN crm_restore_map m ON u.id = m.target_id
    SET u.crm_contact_id  = m.new_crm_contact_id,
        u.organisation_id = m.new_organisation_id
WHERE m.has_unique_conflict = FALSE
  AND ((m.current_crm_contact_id IS NULL AND m.backup_crm_contact_id IS NOT NULL)
    OR (m.current_organisation_id IS NULL AND m.backup_organisation_id IS NOT NULL));

DROP TEMPORARY TABLE crm_restore_map;

-- ============================================================================
-- VERIFY
-- ============================================================================

-- Any backup '@' row's CRM info that still isn't reflected on its target. Rows here that were
-- already listed under "Field-level conflicts" or "Unique-constraint conflicts" above are
-- expected (intentionally not overwritten) -- anything else needs investigation.
SELECT b.id                AS backup_source_id,
       b.sub                AS backup_source_sub,
       u.id                 AS target_id,
       u.sub                AS target_sub,
       b.crm_contact_id     AS backup_crm_contact_id,
       u.crm_contact_id     AS current_crm_contact_id,
       b.organisation_id    AS backup_organisation_id,
       u.organisation_id    AS current_organisation_id
FROM invite_prd_backup.users b
         JOIN users u ON u.sub = REPLACE(b.sub, '@', '_')
WHERE b.sub LIKE '%@%'
  AND ((b.crm_contact_id IS NOT NULL AND
        (u.crm_contact_id IS NULL OR u.crm_contact_id <> b.crm_contact_id))
    OR (b.organisation_id IS NOT NULL AND
        (u.organisation_id IS NULL OR u.organisation_id <> b.organisation_id)));

-- Counts after the fix, to compare against the "before" count above
SELECT COUNT(*) AS users_with_crm_info_after
FROM users
WHERE crm_contact_id IS NOT NULL
   OR organisation_id IS NOT NULL;
