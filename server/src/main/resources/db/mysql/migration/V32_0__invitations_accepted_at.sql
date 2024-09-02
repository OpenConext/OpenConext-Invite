ALTER TABLE `invitations`
    ADD `accepted_at` datetime DEFAULT NULL;

UPDATE `invitations`
SET `accepted_at` = DATE_ADD(created_at, INTERVAL 30 day)
WHERE status = 'ACCEPTED';