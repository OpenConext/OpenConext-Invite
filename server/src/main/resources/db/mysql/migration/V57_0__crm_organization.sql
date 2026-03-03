ALTER TABLE `roles`
    DROP `crm_organisation_id`
    DROP `crm_organisation_name`,
    DROP `crm_organisation_code`;

ALTER TABLE `roles` DROP INDEX `roles_crm_organisation_id_index`;

ALTER TABLE `users`
    DROP `crm_organisation_id`;

CREATE TABLE `organisations`
(
    `id`                     bigint       NOT NULL AUTO_INCREMENT,
    `crm_organisation_id` varchar(255)  NOT NULL,
    `crm_organisation_name` varchar(255)  NOT NULL,
    `crm_organisation_abbrevation` varchar(255)  NULL
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  DEFAULT CHARSET = utf8mb4;

ALTER TABLE users
ADD COLUMN organisation_id BIGINT NULL,
ADD CONSTRAINT fk_users_organisation
    FOREIGN KEY (organisation_id)
    REFERENCES organisations(id)
    ON DELETE SET NULL
    ON UPDATE CASCADE;

ALTER TABLE roles
ADD COLUMN organisation_id BIGINT NULL,
ADD CONSTRAINT fk_roles_organisation
    FOREIGN KEY (organisation_id)
    REFERENCES organisations(id)
    ON DELETE SET NULL
    ON UPDATE CASCADE;
