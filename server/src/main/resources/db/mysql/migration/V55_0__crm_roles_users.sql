ALTER TABLE `roles`
    add `crm_organisation_id` varchar(255) DEFAULT NULL,
    add `crm_organisation_code` varchar(255) DEFAULT NULL,
    add `crm_role_id` varchar(255) DEFAULT NULL,
    add `crm_role_name` varchar(255) DEFAULT NULL;
ALTER TABLE `users`
    add `crm_contact_id` varchar(255) DEFAULT NULL;
ALTER TABLE `invitations`
    add `crm_contact_id` varchar(255) DEFAULT NULL;
