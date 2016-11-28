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

-- :name create-virtual-user! :! :n
-- :doc creates a new user record
INSERT INTO users
(email)
VALUES (:email)

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

-- :name add-full-user! :? :1
-- :doc "add a new registered user"
INSERT INTO users (id, email, name, pass)
VALUES (DEFAULT, :email, :name, :pass)
ON CONFLICT (email) DO
  UPDATE
    SET pass = :pass
    WHERE users.name IS NULL
RETURNING id

-- :name delete-user! :? :*
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

-- :name add-unmoderated-open-quest! :? :1
-- :doc add an unmoderated open quest
INSERT INTO quests
(name,
 start_time,
 end_time,
 address,
 town,
 categories,
 unmoderated_description,
 max_participants,
 hashtags,
 picture,
 owner)
VALUES
(:name,
 :start_time,
 :end_time,
 :address,
 :town,
 :categories,
 :unmoderated_description,
 :max_participants,
 :hashtags,
 :picture,
 :owner)
RETURNING id

-- :name add-unmoderated-secret-quest! :? :1
-- :doc add an unmoderated secret quest
INSERT INTO quests
(name,
 start_time,
 end_time,
 address,
 town,
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
 :address,
 :town,
 :categories,
 :unmoderated_description,
 :max_participants,
 :hashtags,
 :picture,
 :owner
 FALSE)
RETURNING id

-- :name get-quest-by-id :? :1
-- :doc get quest by id
SELECT
  q.id as id,
  q.name as name,
  q.start_time as start_time,
  q.end_time as end_time,
  q.address as address,
  q.town as town,
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
INSERT INTO password_tokens
(user_id, token, expires)
VALUES
(:user_id, DEFAULT, :expires)
ON CONFLICT (user_id) DO
  UPDATE
    SET expires = :expires,
        token = uuid_generate_v4()
RETURNING token

-- :name check-token-validity :?
-- :doc "Check if password token is valid"
SELECT EXISTS
  (SELECT 1 FROM password_tokens
    WHERE token = :token AND
          expires > now())

-- :name activate-user! :!
-- :doc "Activate user with password token"
UPDATE users
  SET pass = :pass,
      email = :email,
      is_active = true
  WHERE EXISTS (
    SELECT 1
    FROM password_tokens
      WHERE id = user_id AND
            expires > now() AND
            token = :token)
