ALTER TABLE
  parties
DROP CONSTRAINT
  join_only_once;

ALTER TABLE
  users
DROP COLUMN
  phone;
