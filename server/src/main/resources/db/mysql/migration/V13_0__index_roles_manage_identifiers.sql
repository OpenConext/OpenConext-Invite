ALTER TABLE `roles`
    ADD INDEX roles_manage_identifiers( (CAST(applications->'$[*].manageId' AS char(255) ARRAY)) );