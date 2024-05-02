ALTER TABLE `invitations` ADD `language` VARCHAR(255) DEFAULT NULL;
UPDATE `invitations` SET `language` = 'en';
ALTER TABLE `invitations` MODIFY `language` VARCHAR(255) NOT NULL;