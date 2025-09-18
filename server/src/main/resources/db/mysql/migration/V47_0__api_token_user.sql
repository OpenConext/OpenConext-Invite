ALTER TABLE api_tokens
    ADD COLUMN owner_id BIGINT NULL,
ADD CONSTRAINT fk_api_tokens_user
    FOREIGN KEY (owner_id)
    REFERENCES users(id)
    ON DELETE CASCADE;