CREATE TABLE `applications`
(
    `id`           bigint       NOT NULL AUTO_INCREMENT,
    `manage_id`    varchar(255) NOT NULL,
    `manage_type`  varchar(255) NOT NULL,
    `landing_page` varchar(255) DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `applications_unique` (`manage_id`, `manage_type`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE `roles_applications`
(
    `id`             bigint NOT NULL AUTO_INCREMENT,
    `role_id`        bigint NOT NULL,
    `application_id` bigint NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `roles_applications_unique` (`role_id`, `application_id`),
    CONSTRAINT `fk_roles_applications_role` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_roles_applications_application` FOREIGN KEY (`application_id`) REFERENCES `applications` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  DEFAULT CHARSET = utf8mb4;
