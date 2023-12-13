CREATE TABLE `application_usages`
(
    `id`             bigint NOT NULL AUTO_INCREMENT,
    `landing_page`   varchar(255) DEFAULT NULL,
    `role_id`        bigint NOT NULL,
    `application_id` bigint NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `application_usages_unique` (`role_id`, `application_id`),
    CONSTRAINT `fk_application_usages_role` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_application_usages_application` FOREIGN KEY (`application_id`) REFERENCES `applications` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  DEFAULT CHARSET = utf8mb4;
