ALTER TABLE
  quests
DROP COLUMN
  address,
ADD COLUMN
  google_place_id VARCHAR(50) NULL,
ADD COLUMN
  street_number INTEGER,
ADD COLUMN
  street TEXT NULL,
ADD COLUMN
  postal_code VARCHAR(20) NOT NULL,
ADD COLUMN
  country TEXT NOT NULL,
ADD COLUMN
  latitude TEXT NULL,
ADD COLUMN
  longitude TEXT NULL,
ADD COLUMN
  google_maps_url TEXT NULL;
