ALTER TABLE `roles`
    ADD `crm_organisation_id` varchar(255) DEFAULT NULL,
    ADD `crm_organisation_code` varchar(255) DEFAULT NULL,
    ADD `crm_role_id` varchar(255) DEFAULT NULL,
    ADD `crm_role_name` varchar(255) DEFAULT NULL;

CREATE INDEX `roles_crm_organisation_id_index` ON `roles` (`crm_organisation_id`);
CREATE INDEX `roles_crm_role_id_index` ON `roles` (`crm_role_id`);

ALTER TABLE `users`
    ADD `crm_contact_id` varchar(255) DEFAULT NULL;

CREATE INDEX `users_crm_contact_id_index` ON `users` (`crm_contact_id`);

ALTER TABLE `invitations`
    ADD `crm_contact_id` varchar(255) DEFAULT NULL;

CREATE INDEX `invitations_crm_contact_id_index` ON `invitations` (`crm_contact_id`);