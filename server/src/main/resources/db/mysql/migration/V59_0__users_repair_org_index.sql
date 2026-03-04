ALTER TABLE `users`
    DROP CONSTRAINT `users_unique_crm_contact_profile`;

ALTER TABLE `users`
    ADD CONSTRAINT `users_unique_crm_contact_profile`
    UNIQUE (`crm_contact_id`, `organisation_id`);

