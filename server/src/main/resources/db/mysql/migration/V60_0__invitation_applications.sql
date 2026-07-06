CREATE TABLE `invitation_applications`
(
    `id`             bigint NOT NULL AUTO_INCREMENT,
    `invitation_id`  bigint NOT NULL,
    `application_id` bigint NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `invitation_applications_unique` (`invitation_id`, `application_id`),
    CONSTRAINT `fk_invitation_applications_invitation` FOREIGN KEY (`invitation_id`) REFERENCES `invitations` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_invitation_applications_application` FOREIGN KEY (`application_id`) REFERENCES `applications` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  DEFAULT CHARSET = utf8mb4;
