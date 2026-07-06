CREATE TABLE `user_applications`
(
    `id`             bigint NOT NULL AUTO_INCREMENT,
    `user_id`        bigint NOT NULL,
    `application_id` bigint NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `user_applications_unique` (`user_id`, `application_id`),
    CONSTRAINT `fk_user_applications_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_user_applications_application` FOREIGN KEY (`application_id`) REFERENCES `applications` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  DEFAULT CHARSET = utf8mb4;
