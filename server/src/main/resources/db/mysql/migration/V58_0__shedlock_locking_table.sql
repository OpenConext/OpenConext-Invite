CREATE TABLE shedlock (
    name        VARCHAR(64)  NOT NULL,
    lock_until  TIMESTAMP(3) NOT NULL,
    locked_at   TIMESTAMP(3) NOT NULL,
    locked_by   VARCHAR(255) NOT NULL,
    PRIMARY KEY (name)
) ENGINE=InnoDB
    DEFAULT CHARSET = utf8mb4;

-- Pre-insert your lock rows (critical for Galera safety)
INSERT INTO shedlock (name, lock_until, locked_at, locked_by)
VALUES ('resource_cleaner_user_level_lock', '2000-01-01 00:00:00.000', '2000-01-01 00:00:00.000', 'init');
INSERT INTO shedlock (name, lock_until, locked_at, locked_by)
VALUES ('role_expiration_notifier_user_level_lock', '2000-01-01 00:00:00.000', '2000-01-01 00:00:00.000', 'init');
