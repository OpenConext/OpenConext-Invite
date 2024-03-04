UPDATE users SET created_at = CURRENT_TIMESTAMP WHERE created_at IS NULL;
UPDATE users SET last_activity = CURRENT_TIMESTAMP WHERE last_activity IS NULL;
