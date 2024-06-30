ALTER TABLE `api_tokens` CHANGE `organization_guid` `organization_guid` VARCHAR(255) DEFAULT NULL;
ALTER TABLE `api_tokens` ADD `super_user_token` bool DEFAULT 0;
