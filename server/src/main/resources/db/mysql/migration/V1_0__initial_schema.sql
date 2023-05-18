CREATE TABLE `institutions`
(
    `id`         bigint       NOT NULL AUTO_INCREMENT,
    `manage_id`  varchar(255) NOT NULL,
    `created_by` varchar(255) NOT NULL,
    `created_at` datetime     DEFAULT CURRENT_TIMESTAMP,
    `updated_by` varchar(255) DEFAULT NULL,
    `updated_at` datetime     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `applications_unique_manage_id` (`manage_id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE `applications`
(
    `id`         bigint       NOT NULL AUTO_INCREMENT,
    `manage_id`  varchar(255) NOT NULL,
    `name`       varchar(255) NOT NULL,
    `created_by` varchar(255) NOT NULL,
    `created_at` datetime     DEFAULT CURRENT_TIMESTAMP,
    `updated_by` varchar(255) DEFAULT NULL,
    `updated_at` datetime     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `applications_unique_manage_id` (`manage_id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE `users`
(
    `id`                       bigint       NOT NULL AUTO_INCREMENT,
    `sub`                      varchar(255) NOT NULL,
    `super_user`               bool         DEFAULT 0,
    `eduperson_principal_name` varchar(255) NOT NULL,
    `given_name`               varchar(255) DEFAULT NULL,
    `family_name`              varchar(255) DEFAULT NULL,
    `email`                    varchar(255) DEFAULT NULL,
    `created_at`               datetime     DEFAULT CURRENT_TIMESTAMP,
    `last_activity`            datetime     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE INDEX `users_unique_eppn` (`eduperson_principal_name`),
    UNIQUE INDEX `users_unique_sub` (sub)
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE `roles`
(
    `id`                     bigint       NOT NULL AUTO_INCREMENT,
    `name`                   varchar(255) NOT NULL,
    `display_name`           text                  DEFAULT NULL,
    `remote_scim_identifier` varchar(255)          DEFAULT NULL,
    `authority`              varchar(255) NOT NULL DEFAULT 'INVITER',
    `landing_page`           varchar(255)          DEFAULT NULL,
    `instant_available`      bool                  DEFAULT 1,
    `default_expiry_days`    int                   DEFAULT NULL,
    `application_id`         bigint       NOT NULL,
    `created_by`             varchar(255) NOT NULL,
    `created_at`             datetime              DEFAULT CURRENT_TIMESTAMP,
    `updated_by`             varchar(255)          DEFAULT NULL,
    `updated_at`             datetime              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `roles_unique_application_name` (`name`, `application_id`),
    CONSTRAINT `fk_roles_application` FOREIGN KEY (`application_id`) REFERENCES `applications` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE `user_roles`
(
    `id`                     bigint NOT NULL AUTO_INCREMENT,
    `remote_scim_identifier` varchar(255) DEFAULT NULL,
    `inviter`                varchar(255) DEFAULT NULL,
    `end_date`               datetime     DEFAULT NULL,
    `user_id`                bigint NOT NULL,
    `role_id`                bigint NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `user_roles_unique_user_role` (`user_id`, `role_id`),
    CONSTRAINT `fk_user_roles_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_user_roles_role` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE `invitations`
(
    `id`                     bigint       NOT NULL AUTO_INCREMENT,
    `intended_authority`     varchar(255) NOT NULL,
    `status`                 varchar(255) DEFAULT NULL,
    `email`                  varchar(255) NOT NULL,
    `message`                varchar(255) DEFAULT NULL,
    `hash`                   varchar(255) DEFAULT NULL,
    `enforce_email_equality` bool         DEFAULT 0,
    `created_at`             datetime     DEFAULT CURRENT_TIMESTAMP,
    `expiry_date`            datetime     NOT NULL,
    `inviter_id`             bigint       NOT NULL,
    `institution_id`         bigint       NOT NULL,
    PRIMARY KEY (`id`),
    INDEX `index_invitation_hash` (`hash`),
    CONSTRAINT `fk_invitations_user` FOREIGN KEY (`inviter_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE `invitation_roles`
(
    `id`            bigint NOT NULL AUTO_INCREMENT,
    `invitation_id` bigint NOT NULL,
    `role_id`       bigint NOT NULL,
    `end_date`      datetime DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `invitation_roles_unique_user_role` (`invitation_id`, `role_id`),
    CONSTRAINT `fk_invitation_roles_invitation` FOREIGN KEY (`invitation_id`) REFERENCES `invitations` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_invitation_roles_role` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE `invitations_institutions`
(
    `id`             bigint NOT NULL AUTO_INCREMENT,
    `invitation_id`  bigint NOT NULL,
    `institution_id` bigint NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `invitations_institutions_unique` (`invitation_id`, `institution_id`),
    CONSTRAINT `fk_invitation` FOREIGN KEY (`invitation_id`) REFERENCES `invitations` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_institution` FOREIGN KEY (`institution_id`) REFERENCES `institutions` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  DEFAULT CHARSET = utf8mb4;

