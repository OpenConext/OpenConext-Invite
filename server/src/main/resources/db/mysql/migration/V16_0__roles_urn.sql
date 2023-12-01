ALTER TABLE `roles`
    add `urn` varchar(255) DEFAULT NULL;
ALTER TABLE `roles`
    add `teams_origin` bool DEFAULT 0;
