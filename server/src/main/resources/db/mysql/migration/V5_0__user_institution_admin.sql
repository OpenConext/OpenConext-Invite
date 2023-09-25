ALTER TABLE `users`
    add `institution_admin` bool DEFAULT 0;
ALTER TABLE `users`
    add `organization_guid` varchar(255) DEFAULT NULL;
