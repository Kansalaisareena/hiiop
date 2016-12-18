ALTER TABLE
  quests
ADD COLUMN
  unmoderated_name TEXT NULL,
ADD COLUMN
  unmoderated_organisation TEXT NULL,
ADD COLUMN
  unmoderated_organisation_description TEXT NULL,
ADD COLUMN
  unmoderated_hashtags json NULL,
ADD COLUMN
  unmoderated_picture uuid REFERENCES pictures(id) DEFAULT NULL,
ADD COLUMN
  is_rejected BOOLEAN DEFAULT false;
