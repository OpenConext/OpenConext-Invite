ALTER TABLE roles
  DROP CONSTRAINT fk_roles_organisation;
ALTER TABLE roles ADD CONSTRAINT fk_roles_organisation
    FOREIGN KEY (organisation_id) REFERENCES organisations (id)
    ON DELETE CASCADE
    ON UPDATE CASCADE;

ALTER TABLE users
  DROP CONSTRAINT fk_users_organisation;
ALTER TABLE users
  ADD CONSTRAINT fk_users_organisation
    FOREIGN KEY (organisation_id) REFERENCES organisations (id)
    ON DELETE CASCADE
    ON UPDATE CASCADE;