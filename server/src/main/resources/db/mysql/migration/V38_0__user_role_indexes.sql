ALTER TABLE `user_roles`
    ADD INDEX `authority_index` (`authority`),
    ADD INDEX `guest_role_included_index` (`guest_role_included`);
