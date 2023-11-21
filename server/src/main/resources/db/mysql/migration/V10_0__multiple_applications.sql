CREATE TABLE `applications`
(
    `id`          bigint       NOT NULL AUTO_INCREMENT,
    `manage_id`   varchar(255) NOT NULL,
    `manage_type` varchar(255) NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE INDEX `applications_unique_manage` (`manage_id`,`manage_type`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE `roles_applications`
(
    `id`             bigint NOT NULL AUTO_INCREMENT,
    `role_id`        bigint NOT NULL,
    `application_id` bigint NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `user_roles_unique_user_role` (`user_id`, `role_id`),
    CONSTRAINT `fk_roles_applications_role` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_roles_applications_application` FOREIGN KEY (`application_id`) REFERENCES `applications` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  DEFAULT CHARSET = utf8mb4;

ALTER TABLE `roles` add `identifier` varchar(255) DEFAULT NULL;