ALTER TABLE
  quests
ADD COLUMN
  address TEXT,
DROP COLUMN
  google_place_id,
DROP COLUMN
  street_number,
DROP COLUMN
  street,
DROP COLUMN
  postal_code,
DROP COLUMN
  country,
DROP COLUMN
  latitude,
DROP COLUMN
  longitude,
DROP COLUMN
  google_maps_url;
