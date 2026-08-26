-- One-off data-repair script for corrupted `users.sub` duplicates.
--
-- Background: some `users` rows have a `sub` containing '@' while a sibling row holds the
-- corrected value with '_' in the same position (unique constraint `users_unique_sub` means
-- these ended up as two separate user rows instead of one). This script merges the '@' row
-- into the '_' row across every table with a foreign key to users(id) -- user_roles,
-- user_applications, remote_provisioned_users, api_tokens, invitations -- plus the
-- denormalized user_roles_audit table, then deletes the '@' row.
--
-- All FKs to users(id) are ON DELETE CASCADE, so simply deleting the '@' row without first
-- re-pointing these tables would silently cascade-delete that user's roles, applications,
-- provisioning records, api tokens and invitations. Run the dry-run section first and review
-- its output before running the transactional section.
--
-- Usage: mysql -u root invite < server/src/test/resources/dedupe_users.sql
--
-- Take a backup first, e.g.:
--   mysqldump -u root invite users user_roles user_applications remote_provisioned_users \
--     api_tokens invitations user_roles_audit > invite_dedupe_backup.sql

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

-- user_roles rows that will be DROPPED because the target user already has that role
SELECT ur.*
FROM user_roles ur
         JOIN user_dedup_map m ON ur.user_id = m.source_id
         JOIN user_roles ur2 ON ur2.user_id = m.target_id AND ur2.role_id = ur.role_id;

-- user_applications rows that will be DROPPED because the target user already has that application
SELECT ua.*
FROM user_applications ua
         JOIN user_dedup_map m ON ua.user_id = m.source_id
         JOIN user_applications ua2 ON ua2.user_id = m.target_id AND ua2.application_id = ua.application_id;

-- remote_provisioned_users rows that will be DROPPED because the target user already has that provisioning record
SELECT rpu.*
FROM remote_provisioned_users rpu
         JOIN user_dedup_map m ON rpu.user_id = m.source_id
         JOIN remote_provisioned_users rpu2
              ON rpu2.user_id = m.target_id AND rpu2.manage_provisioning_id = rpu.manage_provisioning_id;

-- Row counts before the fix, for a sanity check against the "after" counts at the bottom of this script
SELECT (SELECT COUNT(*) FROM users) AS users_before,
       (SELECT COUNT(*) FROM user_roles) AS user_roles_before,
       (SELECT COUNT(*) FROM user_applications) AS user_applications_before,
       (SELECT COUNT(*) FROM remote_provisioned_users) AS remote_provisioned_users_before,
       (SELECT COUNT(*) FROM api_tokens) AS api_tokens_before,
       (SELECT COUNT(*) FROM invitations) AS invitations_before;

-- ============================================================================
-- FIX -- only proceed once the dry-run output above has been reviewed
-- ============================================================================

START TRANSACTION;

-- user_roles (unique on user_id, role_id): drop the source's duplicate, then re-point the rest
DELETE ur FROM user_roles ur
    JOIN user_dedup_map m ON ur.user_id = m.source_id
    JOIN user_roles ur2 ON ur2.user_id = m.target_id AND ur2.role_id = ur.role_id;

UPDATE user_roles ur
    JOIN user_dedup_map m ON ur.user_id = m.source_id
    SET ur.user_id = m.target_id;

-- user_applications (unique on user_id, application_id)
DELETE ua FROM user_applications ua
    JOIN user_dedup_map m ON ua.user_id = m.source_id
    JOIN user_applications ua2 ON ua2.user_id = m.target_id AND ua2.application_id = ua.application_id;

UPDATE user_applications ua
    JOIN user_dedup_map m ON ua.user_id = m.source_id
    SET ua.user_id = m.target_id;

-- remote_provisioned_users (unique on user_id, manage_provisioning_id)
DELETE rpu FROM remote_provisioned_users rpu
    JOIN user_dedup_map m ON rpu.user_id = m.source_id
    JOIN remote_provisioned_users rpu2
        ON rpu2.user_id = m.target_id AND rpu2.manage_provisioning_id = rpu.manage_provisioning_id;

UPDATE remote_provisioned_users rpu
    JOIN user_dedup_map m ON rpu.user_id = m.source_id
    SET rpu.user_id = m.target_id;

-- api_tokens.owner_id (no unique constraint on this column)
UPDATE api_tokens t
    JOIN user_dedup_map m ON t.owner_id = m.source_id
    SET t.owner_id = m.target_id;

-- invitations.inviter_id (no unique constraint on this column)
UPDATE invitations i
    JOIN user_dedup_map m ON i.inviter_id = m.source_id
    SET i.inviter_id = m.target_id;

-- user_roles_audit.user_id (denormalized, no FK, kept consistent for historical reporting)
UPDATE user_roles_audit a
    JOIN user_dedup_map m ON a.user_id = m.source_id
    SET a.user_id = m.target_id;

-- finally remove the now-orphaned '@' duplicate user rows
DELETE u FROM users u
    JOIN user_dedup_map m ON u.id = m.source_id;

COMMIT;

DROP TEMPORARY TABLE user_dedup_map;

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
SELECT (SELECT COUNT(*) FROM users) AS users_after,
       (SELECT COUNT(*) FROM user_roles) AS user_roles_after,
       (SELECT COUNT(*) FROM user_applications) AS user_applications_after,
       (SELECT COUNT(*) FROM remote_provisioned_users) AS remote_provisioned_users_after,
       (SELECT COUNT(*) FROM api_tokens) AS api_tokens_after,
       (SELECT COUNT(*) FROM invitations) AS invitations_after;
