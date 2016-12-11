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

-- :name delete-user-by-email! :? :*
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
(name,
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
 unmoderated_description,
 max_participants,
 hashtags,
 picture,
 owner,
 is_open)
VALUES
(:name,
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
 :unmoderated_description,
 :max_participants,
 :hashtags,
 :picture,
 :owner,
 :is_open)
RETURNING id

-- :name get-quest-by-id :? :1
-- :doc get quest by id
SELECT
  q.id as id,
  q.name as name,
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
  q.description as description,
  q.unmoderated_description as unmoderated_description,
  q.max_participants as max_participants,
  q.hashtags as hashtags,
  (SELECT url FROM pictures WHERE id = q.picture) as picture_url,
  q.owner as owner,
  q.is_open as is_open
FROM
  quests q
WHERE
  q.id = :id

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

-- :name check-token-validity :? :1
-- :doc "Check if password token is valid"
SELECT EXISTS
  (SELECT 1 FROM password_tokens
    WHERE token = :token AND
          expires > now())

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
