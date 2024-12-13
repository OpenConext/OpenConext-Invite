ALTER TABLE `invitations`
    ADD INDEX `status_index` (`status`);
ALTER TABLE `invitations`
    ADD INDEX `created_at_index` (`created_at`);
ALTER TABLE `invitations`
    ADD INDEX `expiry_date_index` (`expiry_date`);
ALTER TABLE `invitations`
    ADD FULLTEXT INDEX `email_index` (`email`);

