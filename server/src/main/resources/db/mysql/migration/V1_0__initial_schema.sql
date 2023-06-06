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
    UNIQUE INDEX `users_unique_sub` (`sub`),
    FULLTEXT KEY `full_text_index` (`given_name`,`family_name`,`email`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE `roles`
(
    `id`                     bigint       NOT NULL AUTO_INCREMENT,
    `name`                   varchar(255) NOT NULL,
    `description`            text         DEFAULT NULL,
    `remote_scim_identifier` varchar(255) DEFAULT NULL,
    `landing_page`           varchar(255) DEFAULT NULL,
    `default_expiry_days`    int          DEFAULT NULL,
    `manage_id`              varchar(255) NOT NULL,
    `manage_type`            varchar(255) NOT NULL,
    `created_by`             varchar(255) NOT NULL,
    `created_at`             datetime     DEFAULT CURRENT_TIMESTAMP,
    `updated_by`             varchar(255) DEFAULT NULL,
    `updated_at`             datetime     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `roles_unique_name` (`name`, `manage_id`),
    FULLTEXT KEY `full_text_index` (`name`,`description`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE `user_roles`
(
    `id`                     bigint       NOT NULL AUTO_INCREMENT,
    `remote_scim_identifier` varchar(255) DEFAULT NULL,
    `inviter`                varchar(255) DEFAULT NULL,
    `end_date`               datetime     DEFAULT NULL,
    `user_id`                bigint       NOT NULL,
    `role_id`                bigint       NOT NULL,
    `authority`              varchar(255) NOT NULL,
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
    `message`                text         DEFAULT NULL,
    `hash`                   varchar(255) DEFAULT NULL,
    `enforce_email_equality` bool         DEFAULT 0,
    `scope_wayf`             bool         DEFAULT 0,
    `edu_id_only`            bool         DEFAULT 0,
    `created_at`             datetime     DEFAULT CURRENT_TIMESTAMP,
    `expiry_date`            datetime     DEFAULT NULL,
    `role_expiry_date`       datetime     DEFAULT NULL,
    `inviter_id`             bigint       NOT NULL,
    PRIMARY KEY (`id`),
    INDEX                    `index_invitation_hash` (`hash`),
    CONSTRAINT `fk_invitations_user` FOREIGN KEY (`inviter_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE `invitation_roles`
(
    `id`            bigint NOT NULL AUTO_INCREMENT,
    `end_date`      datetime DEFAULT NULL,
    `invitation_id` bigint NOT NULL,
    `role_id`       bigint NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `invitation_roles_unique_user_role` (`invitation_id`, `role_id`),
    CONSTRAINT `fk_invitation_roles_invitation` FOREIGN KEY (`invitation_id`) REFERENCES `invitations` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_invitation_roles_role` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE `identity_providers`
(
    `id`              bigint       NOT NULL AUTO_INCREMENT,
    `manage_id`       varchar(255) NOT NULL,
    `manage_entityid` varchar(255) NOT NULL,
    `manage_name`     varchar(255) NOT NULL,
    `invitation_id`   bigint       NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `identity_providers_unique_invitation` (`invitation_id`, `manage_id`),
    CONSTRAINT `fk_identity_providers_invitation` FOREIGN KEY (`invitation_id`) REFERENCES `invitations` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  DEFAULT CHARSET = utf8mb4;
