CREATE TABLE user_roles_audit
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    user_id    BIGINT       NOT NULL,
    user_email VARCHAR(255) NOT NULL,
    role_id    BIGINT       NOT NULL,
    role_name  VARCHAR(255) NOT NULL,
    end_date   DATETIME DEFAULT NULL,
    action     VARCHAR(255) NOT NULL,
    authority  VARCHAR(255) NOT NULL,
    created_by varchar(255) NOT NULL
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  DEFAULT CHARSET = utf8mb4;