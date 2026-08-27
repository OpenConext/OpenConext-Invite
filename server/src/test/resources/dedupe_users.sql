-- One-off data-repair script for corrupted `users.sub` duplicates.
--
-- Background: some `users` rows have a `sub` containing '@' while a sibling row holds the
-- corrected value with '_' in the same position (unique constraint `users_unique_sub` means
-- these ended up as two separate user rows instead of one). This script merges the '@' row
-- into the '_' row across every table with a foreign key to users(id) -- user_roles,
-- remote_provisioned_users, api_tokens, invitations -- plus the
-- denormalized user_roles_audit table, then deletes the '@' row.
--
-- All FKs to users(id) are ON DELETE CASCADE, so simply deleting the '@' row without first
-- re-pointing these tables would silently cascade-delete that user's roles, applications,
-- provisioning records, api tokens and invitations. Run the dry-run section first and review
-- its output before running the transactional section.
--
-- NOTE: this environment does not yet have a user_applications table, so it is intentionally
-- not handled here. If/when that table exists, add the same drop-conflict-then-repoint pattern
-- used below for user_roles / remote_provisioned_users.
--
-- The dry run also builds `change_log`, a row-by-row snapshot of every DELETE/UPDATE this
-- script will perform. The FIX section executes strictly from that snapshot (not by
-- re-deriving the conflict logic again), so the "STATEMENTS TO BE EXECUTED" section -- printed
-- before FIX runs -- is guaranteed to match exactly what FIX will do. Review it manually
-- before proceeding.
--
-- Usage: mysql -u root invite_prd < server/src/test/resources/dedupe_users.sql

-- ============================================================================
-- DRY RUN -- read-only, run this first and review before proceeding
-- ============================================================================

CREATE TEMPORARY TABLE user_dedup_map AS
SELECT u1.id AS source_id, u1.sub AS source_sub, u2.id AS target_id, u2.sub AS target_sub
FROM users u1
         JOIN users u2 ON REPLACE(u1.sub, '@', '_') = u2.sub AND u1.id <> u2.id
WHERE u1.sub LIKE '%@%';

-- The pairs that will be merged (source '@' row -> surviving '_' row)
SELECT *
FROM user_dedup_map;

-- Duplicate groups that could NOT be resolved automatically because no clean '_' variant
-- exists to merge into (both/all variants still contain '@'). These need manual review.
SELECT REPLACE(sub, '@', '_') AS normalized_sub,
       GROUP_CONCAT(sub SEPARATOR ' | ')      AS variants,
       COUNT(*)                               AS cnt
FROM users
GROUP BY normalized_sub
HAVING cnt > 1
   AND normalized_sub NOT IN (SELECT target_sub FROM user_dedup_map);

-- Snapshot of every individual row-level action this script will perform, taken before
-- anything is modified. table_name/row_id identify the row; action is DELETE or UPDATE;
-- user_column is the FK column being repointed (NULL for DELETEs); new_user_id is the value
-- it will be set to (NULL for DELETEs). The FIX section below executes from this table.
CREATE TEMPORARY TABLE change_log
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    table_name  VARCHAR(64) NOT NULL,
    action      VARCHAR(10) NOT NULL,
    row_id      BIGINT      NOT NULL,
    user_column VARCHAR(32) NULL,
    old_user_id BIGINT      NOT NULL,
    new_user_id BIGINT      NULL
);

-- user_roles: rows dropped because the target already has that role
INSERT INTO change_log (table_name, action, row_id, user_column, old_user_id, new_user_id)
SELECT 'user_roles', 'DELETE', ur.id, NULL, ur.user_id, NULL
FROM user_roles ur
         JOIN user_dedup_map m ON ur.user_id = m.source_id
         JOIN user_roles ur2 ON ur2.user_id = m.target_id AND ur2.role_id = ur.role_id;

-- user_roles: rows re-pointed to the surviving user
INSERT INTO change_log (table_name, action, row_id, user_column, old_user_id, new_user_id)
SELECT 'user_roles', 'UPDATE', ur.id, 'user_id', ur.user_id, m.target_id
FROM user_roles ur
         JOIN user_dedup_map m ON ur.user_id = m.source_id
WHERE NOT EXISTS (SELECT 1
                   FROM user_roles ur2
                   WHERE ur2.user_id = m.target_id
                     AND ur2.role_id = ur.role_id);

-- remote_provisioned_users: rows dropped because the target already has that provisioning record
INSERT INTO change_log (table_name, action, row_id, user_column, old_user_id, new_user_id)
SELECT 'remote_provisioned_users', 'DELETE', rpu.id, NULL, rpu.user_id, NULL
FROM remote_provisioned_users rpu
         JOIN user_dedup_map m ON rpu.user_id = m.source_id
         JOIN remote_provisioned_users rpu2
              ON rpu2.user_id = m.target_id AND rpu2.manage_provisioning_id = rpu.manage_provisioning_id;

-- remote_provisioned_users: rows re-pointed to the surviving user
INSERT INTO change_log (table_name, action, row_id, user_column, old_user_id, new_user_id)
SELECT 'remote_provisioned_users', 'UPDATE', rpu.id, 'user_id', rpu.user_id, m.target_id
FROM remote_provisioned_users rpu
         JOIN user_dedup_map m ON rpu.user_id = m.source_id
WHERE NOT EXISTS (SELECT 1
                   FROM remote_provisioned_users rpu2
                   WHERE rpu2.user_id = m.target_id
                     AND rpu2.manage_provisioning_id = rpu.manage_provisioning_id);

-- api_tokens: no unique constraint on owner_id, always re-pointed
INSERT INTO change_log (table_name, action, row_id, user_column, old_user_id, new_user_id)
SELECT 'api_tokens', 'UPDATE', t.id, 'owner_id', t.owner_id, m.target_id
FROM api_tokens t
         JOIN user_dedup_map m ON t.owner_id = m.source_id;

-- invitations: no unique constraint on inviter_id, always re-pointed
INSERT INTO change_log (table_name, action, row_id, user_column, old_user_id, new_user_id)
SELECT 'invitations', 'UPDATE', i.id, 'inviter_id', i.inviter_id, m.target_id
FROM invitations i
         JOIN user_dedup_map m ON i.inviter_id = m.source_id;

-- user_roles_audit: denormalized, no FK, kept consistent for historical reporting
INSERT INTO change_log (table_name, action, row_id, user_column, old_user_id, new_user_id)
SELECT 'user_roles_audit', 'UPDATE', a.id, 'user_id', a.user_id, m.target_id
FROM user_roles_audit a
         JOIN user_dedup_map m ON a.user_id = m.source_id;

-- users: the duplicate '@' rows themselves, to be deleted
INSERT INTO change_log (table_name, action, row_id, user_column, old_user_id, new_user_id)
SELECT 'users', 'DELETE', m.source_id, NULL, m.source_id, NULL
FROM user_dedup_map m;

-- user_roles rows that will be DROPPED because the target user already has that role
SELECT *
FROM change_log
WHERE table_name = 'user_roles'
  AND action = 'DELETE';

-- remote_provisioned_users rows that will be DROPPED because the target user already has that provisioning record
SELECT *
FROM change_log
WHERE table_name = 'remote_provisioned_users'
  AND action = 'DELETE';

-- Row counts before the fix, for a sanity check against the "after" counts further down
SELECT (SELECT COUNT(*) FROM users)                     AS users_before,
       (SELECT COUNT(*) FROM user_roles)                AS user_roles_before,
       (SELECT COUNT(*) FROM remote_provisioned_users)  AS remote_provisioned_users_before,
       (SELECT COUNT(*) FROM api_tokens)                AS api_tokens_before,
       (SELECT COUNT(*) FROM invitations)                AS invitations_before;

-- ============================================================================
-- STATEMENTS TO BE EXECUTED -- individual DELETE/UPDATE statements, for manual/visual
-- inspection before FIX runs. This is exactly what FIX (below) will do, row by row.
-- ============================================================================

SELECT
    CASE action
        WHEN 'DELETE' THEN CONCAT('DELETE FROM ', table_name, ' WHERE id = ', row_id, ';')
        WHEN 'UPDATE' THEN CONCAT('UPDATE ', table_name, ' SET ', user_column, ' = ', new_user_id, ' WHERE id = ', row_id, ';')
        END AS statement
FROM change_log
ORDER BY table_name, action, row_id;

-- ============================================================================
-- FIX -- only proceed once the dry-run and statement output above have been reviewed
-- ============================================================================

START TRANSACTION;

DELETE ur FROM user_roles ur
    JOIN change_log c ON c.table_name = 'user_roles' AND c.action = 'DELETE' AND c.row_id = ur.id;

UPDATE user_roles ur
    JOIN change_log c ON c.table_name = 'user_roles' AND c.action = 'UPDATE' AND c.row_id = ur.id
    SET ur.user_id = c.new_user_id;

DELETE rpu FROM remote_provisioned_users rpu
    JOIN change_log c ON c.table_name = 'remote_provisioned_users' AND c.action = 'DELETE' AND c.row_id = rpu.id;

UPDATE remote_provisioned_users rpu
    JOIN change_log c ON c.table_name = 'remote_provisioned_users' AND c.action = 'UPDATE' AND c.row_id = rpu.id
    SET rpu.user_id = c.new_user_id;

UPDATE api_tokens t
    JOIN change_log c ON c.table_name = 'api_tokens' AND c.action = 'UPDATE' AND c.row_id = t.id
    SET t.owner_id = c.new_user_id;

UPDATE invitations i
    JOIN change_log c ON c.table_name = 'invitations' AND c.action = 'UPDATE' AND c.row_id = i.id
    SET i.inviter_id = c.new_user_id;

UPDATE user_roles_audit a
    JOIN change_log c ON c.table_name = 'user_roles_audit' AND c.action = 'UPDATE' AND c.row_id = a.id
    SET a.user_id = c.new_user_id;

DELETE u FROM users u
    JOIN change_log c ON c.table_name = 'users' AND c.action = 'DELETE' AND c.row_id = u.id;

COMMIT;

DROP TEMPORARY TABLE user_dedup_map;
DROP TEMPORARY TABLE change_log;

-- ============================================================================
-- VERIFY
-- ============================================================================

-- Should return zero rows (or only the genuinely-unresolved groups already reported above)
SELECT REPLACE(sub, '@', '_') AS normalized_sub,
       GROUP_CONCAT(sub SEPARATOR ' | ') AS variants,
       COUNT(*)                          AS cnt
FROM users
GROUP BY normalized_sub
HAVING cnt > 1;

-- Row counts after the fix, to compare against the "before" counts above
SELECT (SELECT COUNT(*) FROM users)                     AS users_after,
       (SELECT COUNT(*) FROM user_roles)                AS user_roles_after,
       (SELECT COUNT(*) FROM remote_provisioned_users)  AS remote_provisioned_users_after,
       (SELECT COUNT(*) FROM api_tokens)                AS api_tokens_after,
       (SELECT COUNT(*) FROM invitations)                AS invitations_after;
