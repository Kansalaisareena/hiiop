-- :name add-org! :! :n
-- :doc Creates a new organizer record
INSERT INTO organization
(name)
VALUES (:name)

-- :name delete-org! :! :*
-- :doc Delete organizer record
DELETE FROM organization
WHERE name=:name

-- :name get-orgs :? :*
-- :doc Get all organizations
SELECT * FROM organization

-- :name create-virtual-user! :? :1
-- :doc creates a new user record
INSERT INTO users
(email)
VALUES (:email)
RETURNING id

-- :name update-user! :! :n
-- :doc update an existing user record
UPDATE users
SET name = :name, email = :email
WHERE id = :id

-- :name get-user-by-id :? :1 :uuid
-- :doc retrieve a user given the uuid.
SELECT
  id,
  name,
  email,
  moderator,
  last_login,
  is_active
FROM users
WHERE id = :id

-- :name get-user-by-email :? :1 :email
-- :doc retrieve a user given the email.
SELECT
  id,
  name,
  email,
  moderator,
  last_login,
  is_active
FROM users
WHERE email = :email

-- :name get-password-hash :? :1
-- doc retrieve a password hash by email
SELECT
  pass
FROM users
WHERE email = :email

-- :name delete-user! :! :n
-- :doc delete a user given the uuid
DELETE FROM users
WHERE id = :id

-- :name delete-user-by-email! :! :n
-- :doc delete user by email
DELETE FROM users
where email = :email

-- :name get-users :? :*
-- :doc get all users
SELECT * from users

-- :name get-user-id :? :1
-- :doc get user id by email
SELECT id FROM users
WHERE email = :email

-- :name add-unmoderated-quest! :? :1
-- :doc add a quest
INSERT INTO quests
(unmoderated_name,
 unmoderated_description,
 start_time,
 end_time,
 street_number,
 street,
 town,
 postal_code,
 country,
 latitude,
 longitude,
 google_maps_url,
 google_place_id,
 categories,
 max_participants,
 unmoderated_hashtags
 unmoderated_picture,
 unmoderated_organisation,
 unmoderated_organisation_description,
 is_open,
 owner)
VALUES
(:unmoderated_name,
 :unmoderated_description,
 :start_time,
 :end_time,
 :street_number,
 :street,
 :town,
 :postal_code,
 :country,
 :latitude,
 :longitude,
 :google_maps_url,
 :google_place_id,
 :categories,
 :max_participants,
 :unmoderated_hashtags,
 :unmoderated_picture,
 :unmoderated_organisation,
 :unmoderated_organisation_description,
 :is_open,
 :owner)
RETURNING id

-- :name add-moderated-quest! :? :1
-- :doc add a quest
INSERT INTO quests
(name,
 description,
 organisation,
 organisation_description,
 start_time,
 end_time,
 street_number,
 street,
 town,
 postal_code,
 country,
 latitude,
 longitude,
 google_maps_url,
 google_place_id,
 categories,
 max_participants,
 hashtags,
 picture,
 is_open,
 owner)
VALUES
(:name,
 :description,
 :organisation,
 :organisation_description,
 :start_time,
 :end_time,
 :street_number,
 :street,
 :town,
 :postal_code,
 :country,
 :latitude,
 :longitude,
 :google_maps_url,
 :google_place_id,
 :categories,
 :max_participants,
 :hashtags,
 :picture,
 :is_open,
 :owner)
RETURNING id

-- :name get-moderated-quest-by-id :? :1
-- :doc get quest by id
SELECT
  q.id as id,
  q.name as name,
  q.description as description,
  q.organisation as organisation,
  q.organisation_description as organisation_description,
  q.start_time as start_time,
  q.end_time as end_time,
  q.street_number as street_number,
  q.street as street,
  q.postal_code as postal_code,
  q.town as town,
  q.country as country,
  q.latitude as latitude,
  q.longitude as longitude,
  q.google_maps_url as google_maps_url,
  q.google_place_id as google_place_id,
  q.categories as categories,
  q.max_participants as max_participants,
  q.hashtags as hashtags,
  q.picture as picture,
  (SELECT url FROM pictures WHERE id = q.picture) as picture_url,
  q.is_open as is_open,
  q.owner as owner
FROM
  quests q
WHERE
  q.id = :id AND
  q.name IS NOT NULL AND
  q.is_rejected = FALSE;

-- :name get-unmoderated-quest-by-id :? :1
-- :doc get quest by id
SELECT
  q.id as id,
  q.unmoderated_name as unmoderated_name,
  q.unmoderated_description as unmoderated_description,
  q.unmoderated_organisation as unmoderated_organisation,
  q.unmoderated_organisation_description as unmoderated_organisation_description,
  q.start_time as start_time,
  q.end_time as end_time,
  q.street_number as street_number,
  q.street as street,
  q.postal_code as postal_code,
  q.town as town,
  q.country as country,
  q.latitude as latitude,
  q.longitude as longitude,
  q.google_maps_url as google_maps_url,
  q.google_place_id as google_place_id,
  q.categories as categories,
  q.max_participants as max_participants,
  q.unmoderated_hashtags as unmoderated_hashtags,
  (SELECT url FROM pictures WHERE id = q.unmoderated_picture) as unmoderated_picture_url,
  q.is_open as is_open,
  q.owner as owner
FROM
  quests q
WHERE
  q.id = :id

-- :name get-moderated-quest-by-owner :? :1
-- :doc get quest by owner
SELECT
  q.id as id,
  q.name as name,
  q.description as description,
  q.organisation as organisation,
  q.organisation_description as organisation_description,
  q.start_time as start_time,
  q.end_time as end_time,
  q.street_number as street_number,
  q.street as street,
  q.postal_code as postal_code,
  q.town as town,
  q.country as country,
  q.latitude as latitude,
  q.longitude as longitude,
  q.google_maps_url as google_maps_url,
  q.google_place_id as google_place_id,
  q.categories as categories,
  q.max_participants as max_participants,
  q.hashtags as hashtags,
  (SELECT url FROM pictures WHERE id = q.picture) as picture_url,
  q.is_open as is_open,
  q.owner as owner
FROM
  quests q
WHERE
  q.owner = :owner;

-- :name update-moderated-quest! :? :1
-- :doc "Update moderated quest"
UPDATE
  quests
SET
  name = :name,
  description = :description,
  organisation = :organisation,
  organisation_description = :organisation_description,
  start_time = :start_time,
  end_time = :end_time,
  street_number = :street_number,
  street = :street,
  town = :town,
  postal_code = :postal_code,
  country = :country,
  latitude = :latitude,
  longitude = :longitude,
  google_maps_url = :google_maps_url,
  google_place_id = :google_place_id,
  categories = :categories,
  max_participants = :max_participants,
  hashtags = :hashtags,
  picture = :picture,
  is_open = :is_open
WHERE
  id = :id AND
  owner = :owner
RETURNING id

-- :name get-quest-secret-party-id :? :1
-- :doc get quest secret party id by id
SELECT
  secret_party
FROM
  quests
WHERE
  id = :id

-- :name delete-quest-by-id! :! :n
-- :doc Delete quest by id
DELETE FROM
  quests
WHERE
  id = :id

-- :name create-password-token! :? :1
-- :doc "Create new password token and return it"
INSERT INTO password_tokens (user_id, expires)
SELECT u.id, :expires
FROM users u
WHERE u.email = :email
ON CONFLICT (user_id) DO
  UPDATE
    SET expires = :expires,
        token = uuid_generate_v4()
RETURNING token

-- :name delete-password-token! :! :n
-- :doc "Delete a token"
DELETE FROM password_tokens
WHERE token = :token

-- :name get-token-by-user-id :? :1 :uuid
-- :doc "Get token by user id"
SELECT token
FROM password_tokens
WHERE user_id = :user_id

-- :name check-token-validity :? :1
-- :doc "Check if password token is valid"
SELECT EXISTS
  (SELECT 1 FROM password_tokens
    WHERE token = :token AND
          expires > now())

-- :name get-token-info :? :1 :uuid
-- :doc "Retrieve token's info and user email using token uuid"
SELECT
  t.token as token,
  u.id as user_id,
  u.email as email,
  t.expires as expires
FROM
  users u,
  password_tokens t
WHERE
  t.token = :token AND
  t.user_id = u.id AND
  t.expires > now()

-- :name activate-user! :! :1
-- :doc "Activate user with password token"
UPDATE users
  SET pass = :pass,
      email = :email,
      is_active = true
  WHERE is_active = false
    AND EXISTS (
      SELECT 1
      FROM password_tokens
        WHERE id = user_id AND
              expires > now() AND
              token = :token)

-- :name add-picture! :? :1
-- :doc "Add picture"
INSERT INTO pictures (url, owner)
VALUES (:url, :owner)
RETURNING id

-- :name update-picture-url! :? :1
-- :doc "Update picture url"
UPDATE pictures
SET url = :url
WHERE id = :id
RETURNING url
