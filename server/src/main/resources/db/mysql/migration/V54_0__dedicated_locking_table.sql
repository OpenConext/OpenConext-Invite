CREATE TABLE distributed_locks (
    lock_name VARCHAR(255) PRIMARY KEY,
    acquired_at TIMESTAMP
) ENGINE=InnoDB
  DEFAULT CHARSET = utf8mb4;