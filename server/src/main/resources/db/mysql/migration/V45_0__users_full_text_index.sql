ALTER TABLE users
DROP INDEX full_text_index,
  ADD FULLTEXT INDEX full_text_index (given_name, family_name, email, schac_home_organization);
