CREATE TABLE `api_tokens`
(
    `id`                bigint       NOT NULL AUTO_INCREMENT,
    `organization_guid` varchar(255) NOT NULL,
    `hashed_value`      varchar(255) NOT NULL,
    `description`       varchar(255) DEFAULT NULL,
    `created_at`        datetime     DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE INDEX `api_tokens_unique_hashed_value` (`hashed_value`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  DEFAULT CHARSET = utf8mb4;
