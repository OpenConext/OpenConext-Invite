-- One-off data-repair script: normalize remaining `users.sub` values that still contain '@'
-- by replacing '@' with '_', for users that had no '_' twin to merge into via dedupe_users.sql.
--
-- Usage: mysql -u root invite_prd < server/src/test/resources/normalize_user_subs.sql

-- ============================================================================
-- DRY RUN -- read-only, run this first and review before proceeding
-- ============================================================================

-- Rows that will be updated
SELECT id, sub, email, REPLACE(sub, '@', '_') AS new_sub
FROM users
WHERE sub LIKE '%@%';

-- Safety check: normalized values that would collide with an existing user's sub
-- (either another '@' row normalizing to the same value, or an existing '_' row --
-- the latter shouldn't happen if dedupe_users.sql already ran, but this guards against it).
-- Should return an empty set; if it doesn't, resolve those rows manually before proceeding.
SELECT REPLACE(sub, '@', '_') AS new_sub, COUNT(*) AS cnt, GROUP_CONCAT(id) AS colliding_ids
FROM users
WHERE REPLACE(sub, '@', '_') IN (
    SELECT REPLACE(sub, '@', '_') FROM users WHERE sub LIKE '%@%'
)
GROUP BY new_sub
HAVING cnt > 1;

-- ============================================================================
-- FIX -- only proceed once the dry-run output above shows no collisions
-- ============================================================================

UPDATE users
SET sub = REPLACE(sub, '@', '_')
WHERE sub LIKE '%@%';

-- ============================================================================
-- VERIFY
-- ============================================================================

-- Should return zero rows
SELECT id, sub, email FROM users WHERE sub LIKE '%@%';
