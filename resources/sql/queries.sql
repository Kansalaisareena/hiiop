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
(id, email)
VALUES (:id, :email)

-- :name update-user! :! :n
-- :doc update an existing user record
UPDATE users
SET name = :name, email = :email
WHERE id = :id

-- :name get-user :? :uuid
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

-- :name delete-user! :! :n
-- :doc delete a user given the uuid
DELETE FROM users
WHERE id = :id

-- :name add-event! :! :n
-- :doc add an event
INSERT INTO events
(
