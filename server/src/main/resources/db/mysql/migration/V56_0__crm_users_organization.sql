ALTER TABLE `users`
    ADD `crm_organisation_id` varchar(255) DEFAULT NULL;

CREATE INDEX `users_crm_organisation_id_index` ON `users` (`crm_organisation_id`);

ALTER TABLE `invitations`
    ADD `crm_organisation_id` varchar(255) DEFAULT NULL;

CREATE INDEX `invitations_crm_organisation_id_index` ON `invitations` (`crm_organisation_id`);

ALTER TABLE `users`
    ADD CONSTRAINT `users_unique_crm_contact_profile`
    UNIQUE (`crm_contact_id`, `crm_organisation_id`);