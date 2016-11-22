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


-- :name get-user :? :1 :uuid
-- :doc retrieve a user given the uuid.
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

-- :name add-unmoderated-event! :! :n
-- :doc add an event
INSERT INTO events
(name,
 owner,
 unmoderated_description,
 max_participants,
 picture,
 hashtags,
 start_time,
 end_time)
 VALUES
 (:name,
  :owner,
  :unmoderated_description,
  :max_participants,
  :picture,
  :hasthags,
  :start_time,
  :end_time)

-- :name add-full-user! :? :1
-- :doc "add a new registered user"
INSERT INTO users (id, email, name, pass)
VALUES (DEFAULT, :email, :name, :pass )
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

  
