CREATE TABLE `organisations`
(
    `id`                     bigint       NOT NULL AUTO_INCREMENT,
    `crm_organisation_id` VARCHAR(255)  NOT NULL,
    `crm_organisation_name` VARCHAR(255)  NOT NULL,
    `crm_organisation_abbrevation` VARCHAR(255)  NULL,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  DEFAULT CHARSET = utf8mb4;

ALTER TABLE roles
    ADD COLUMN crm_role_abbrevation VARCHAR(255) NULL,
    ADD COLUMN crm_role_id VARCHAR(255) DEFAULT NULL,
    ADD COLUMN crm_role_name VARCHAR(255) DEFAULT NULL,
    ADD COLUMN organisation_id BIGINT NULL,
    ADD CONSTRAINT fk_roles_organisation
        FOREIGN KEY (organisation_id)
        REFERENCES organisations(id)
        ON DELETE SET NULL
        ON UPDATE CASCADE;

ALTER TABLE `invitations`
    ADD `crm_contact_id` VARCHAR(255) DEFAULT NULL,
    ADD COLUMN crm_organisation_id VARCHAR(255) DEFAULT NULL;

ALTER TABLE `users`
    ADD `crm_contact_id` VARCHAR(255) DEFAULT NULL,
    ADD COLUMN organisation_id BIGINT NULL,
    ADD CONSTRAINT fk_users_organisation
        FOREIGN KEY (organisation_id)
        REFERENCES organisations(id)
        ON DELETE SET NULL
        ON UPDATE CASCADE;

ALTER TABLE `users`
    ADD CONSTRAINT `users_unique_crm_contact_profile`
    UNIQUE (`crm_contact_id`, `organisation_id`);