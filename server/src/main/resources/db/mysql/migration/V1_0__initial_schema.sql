CREATE TABLE `users`
(
    `id`                       bigint       NOT NULL AUTO_INCREMENT,
    `sub`                      varchar(255) NOT NULL,
    `super_user`               bool         DEFAULT 0,
    `eduperson_principal_name` varchar(255) NOT NULL,
    `given_name`               varchar(255) DEFAULT NULL,
    `family_name`              varchar(255) DEFAULT NULL,
    `schac_home_organization`  varchar(255) DEFAULT NULL,
    `email`                    varchar(255) DEFAULT NULL,
    `created_at`               datetime     DEFAULT CURRENT_TIMESTAMP,
    `last_activity`            datetime     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE INDEX `users_unique_sub` (`sub`),
    FULLTEXT KEY `full_text_index` (`given_name`, `family_name`, `email`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE `roles`
(
    `id`                     bigint       NOT NULL AUTO_INCREMENT,
    `name`                   varchar(255) NOT NULL,
    `short_name`             varchar(255) NOT NULL,
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
    UNIQUE KEY `roles_unique_short_name` (`short_name`, `manage_id`),
    FULLTEXT KEY `full_text_index` (`name`, `description`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE `user_roles`
(
    `id`                     bigint       NOT NULL AUTO_INCREMENT,
    `remote_scim_identifier` varchar(255) DEFAULT NULL,
    `inviter`                varchar(255) DEFAULT NULL,
    `end_date`               datetime     DEFAULT NULL,
    `created_at`             datetime     DEFAULT CURRENT_TIMESTAMP,
    `expiry_notifications`   int          DEFAULT 0,
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
    `edu_id_only`            bool         DEFAULT 0,
    `created_at`             datetime     DEFAULT CURRENT_TIMESTAMP,
    `expiry_date`            datetime     DEFAULT NULL,
    `role_expiry_date`       datetime     DEFAULT NULL,
    `inviter_id`             bigint       NOT NULL,
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
    PRIMARY KEY (`id`),
    UNIQUE KEY `invitation_roles_unique_user_role` (`invitation_id`, `role_id`),
    CONSTRAINT `fk_invitation_roles_invitation` FOREIGN KEY (`invitation_id`) REFERENCES `invitations` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_invitation_roles_role` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE `remote_provisioned_users`
(
    `id`                     bigint       NOT NULL AUTO_INCREMENT,
    `manage_provisioning_id` varchar(255) NOT NULL,
    `remote_scim_identifier` varchar(255) NOT NULL,
    `user_id`                bigint       NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `remote_provisioned_users_unique` (`user_id`, `manage_provisioning_id`),
    CONSTRAINT `fk_remote_provisioned_users_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE `remote_provisioned_groups`
(
    `id`                     bigint       NOT NULL AUTO_INCREMENT,
    `manage_provisioning_id` varchar(255) NOT NULL,
    `remote_scim_identifier` varchar(255) NOT NULL,
    `role_id`                bigint       NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `remote_provisioned_groups_unique` (`role_id`, `manage_provisioning_id`),
    CONSTRAINT `fk_remote_provisioned_groups_role` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE `provisionings`
(
    `id`                bigint       NOT NULL AUTO_INCREMENT,
    `provisioning_type` varchar(255) NOT NULL,
    `resource_type`     varchar(255) NOT NULL,
    `message`           json DEFAULT NULL,
    `method`            varchar(255) NOT NULL,
    `url`               varchar(255) NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  DEFAULT CHARSET = utf8mb4;
