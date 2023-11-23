ALTER TABLE `roles` MODIFY `identifier` varchar(255) NOT NULL;
ALTER TABLE `roles` MODIFY `applications` json NOT NULL;
ALTER TABLE `roles` DROP COLUMN `manage_id`;
ALTER TABLE `roles` DROP COLUMN `manage_type`;