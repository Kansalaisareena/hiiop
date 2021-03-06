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
INSERT INTO
  users (email)
VALUES
  (lower(:email))
RETURNING id

-- :name update-user! :! :n
-- :doc update an existing user record
UPDATE
  users
SET
  name = :name,
  email = lower(:email),
  phone = :phone,
  locale = :locale
WHERE id = :id

-- :name edit-user! :! :1
-- :doc update user fields editable by user
UPDATE
  users
SET
  name = :name,
  phone = :phone,
  locale = :locale
WHERE id = :id AND
        (id = :user_id OR
        EXISTS (SELECT FROM users u
             WHERE u.id = :user_id AND
                   u.moderator = true))

-- :name get-public-user-by-id :? :1 :uuid
-- :doc retrieve a user given the uuid.
SELECT
  id,
  name
FROM users
WHERE
  id = :id

-- :name get-user-by-id :? :1 :uuid
-- :doc retrieve a user given the uuid.
SELECT
  id,
  name,
  email,
  phone,
  moderator,
  last_login,
  is_active,
  locale
FROM users
WHERE
  id = :id AND
  (id = :user_id OR
   EXISTS (SELECT FROM users u
           WHERE u.id = :user_id AND
                 u.moderator = true))


-- :name get-user-by-email :? :1 :email
-- :doc retrieve a user given the email.
SELECT
  id,
  name,
  email,
  phone,
  locale,
  moderator,
  last_login,
  is_active
FROM users
WHERE lower(email) = lower(:email)

-- :name get-password-hash :? :1
-- doc retrieve a password hash by email
SELECT
  pass
FROM users
WHERE lower(email) = lower(:email)

-- :name delete-user! :! :n
-- :doc delete a user given the uuid
DELETE FROM users
WHERE id = :id

-- :name delete-user-by-email! :! :n
-- :doc delete user by email
DELETE FROM users
WHERE lower(email) = lower(:email)

-- :name get-users :? :*
-- :doc get all users
SELECT * FROM users

-- :name get-user-name-and-id :? :1
-- :doc get user id by email
SELECT id, name
FROM users
WHERE lower(email) = lower(:email)

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
 unmoderated_hashtags,
 unmoderated_picture,
 unmoderated_organisation,
 unmoderated_organisation_description,
 is_open,
 owner)
VALUES
(:name,
 :description,
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
 :organisation,
 :organisation_description,
 :is_open,
 :owner)
RETURNING id

-- :name get-quest-limitations :? :1
-- :doc "Quest limitations"
SELECT
  id,
  start_time,
  end_time,
  max_participants,
  (SELECT COUNT(user_id) FROM parties WHERE quest_id = :id) as participant_count,
  is_open,
  secret_party
FROM
  quests
WHERE
  id = :id

-- :name get-quest-secret-party :? :1
-- :doc "Secret party!"
SELECT
  secret_party
FROM
  quests
WHERE
  id = :id

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
  q.is_rejected as is_rejected,
  (SELECT COUNT(user_id) FROM parties WHERE quest_id = :id) as participant_count,
  q.owner as owner
FROM
  quests q
WHERE
  q.id = :id AND
  q.name IS NOT NULL

-- :name get-moderated-secret-quest :? :1
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
  q.owner as owner,
  (SELECT COUNT(user_id) FROM parties WHERE quest_id = :id) as participant_count
FROM
  quests q
WHERE
  q.id = :id AND
  q.secret_party = :secret_party AND
  q.name IS NOT NULL

-- :name get-unmoderated-quest-by-id :? :1
-- :doc get quest by id
SELECT
  q.id as id,
  q.unmoderated_name as name,
  q.unmoderated_description as description,
  q.unmoderated_organisation as organisation,
  q.unmoderated_organisation_description as organisation_description,
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
  q.unmoderated_hashtags as hashtags,
  q.unmoderated_picture as picture,
  (SELECT url FROM pictures WHERE id = q.unmoderated_picture) as picture_url,
  q.is_open as is_open,
  q.is_rejected as is_rejected,
  q.owner as owner
FROM
  quests q
WHERE
  q.id = :id AND
  (q.owner = :owner OR
   EXISTS (SELECT FROM users u
           WHERE u.id = :owner AND
                 u.moderator = true))

-- :name get-moderated-or-unmoderated-quest-by-id :? :1
-- :doc get quest by id regardless of moderated state
SELECT
  q.id as id,
  COALESCE(q.unmoderated_name, q.name) as name,
  COALESCE(q.unmoderated_description, q.description) as description,
  COALESCE(q.unmoderated_organisation, q.organisation) as organisation,
  COALESCE(q.unmoderated_organisation_description, q.organisation_description) as organisation_description,
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
  COALESCE(q.unmoderated_hashtags, q.hashtags) as hashtags,
  COALESCE(q.unmoderated_picture, q.picture) as picture,
  (SELECT url FROM pictures WHERE id = COALESCE(q.unmoderated_picture, q.picture)) as picture_url,
  q.is_open as is_open,
  q.secret_party as secret_party,
  q.owner as owner
FROM
  quests q
WHERE
  q.id = :id AND
  (q.owner = :user_id OR
   EXISTS (SELECT FROM users u
           WHERE u.id = :user_id AND
                 u.moderator = true))

-- :name get-all-participating-quests :? :*
-- :doc "Get a list of user's participating quests"
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
  (SELECT COUNT(user_id) FROM parties WHERE quest_id = q.id) as participant_count,
  q.is_open as is_open,
  q.is_rejected as is_rejected,
  q.owner as owner,
  TRUE as moderated
FROM quests q
WHERE
  q.name IS NOT NULL AND
  EXISTS (SELECT FROM parties p
          WHERE p.user_id = :user_id AND
                p.quest_id = q.id)

-- :name get-all-moderated-quests :? :*
-- :doc get all moderated quests
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
  (SELECT COUNT(user_id) FROM parties WHERE quest_id = q.id) as participant_count,
  q.is_open as is_open,
  q.is_rejected as is_rejected,
  q.owner as owner
FROM
  quests q
WHERE
  q.name IS NOT NULL

-- :name get-all-unmoderated-quests :? :*
-- :doc get unmoderated quests
SELECT
q.id as id,
q.unmoderated_name as name,
q.unmoderated_description as description,
q.unmoderated_organisation as organisation,
q.unmoderated_organisation_description as organisation_description,
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
q.unmoderated_hashtags as hashtags,
(SELECT url FROM pictures WHERE id = q.unmoderated_picture) as picture_url,
(SELECT COUNT(user_id) FROM parties WHERE quest_id = q.id) as participant_count,
q.is_open as is_open,
q.is_rejected as is_rejected,
CASE WHEN q.name IS NULL THEN FALSE ELSE TRUE END as is_edit,
q.owner as owner
FROM
  quests q
WHERE q.unmoderated_name IS NOT NULL AND
      is_rejected = false AND
      EXISTS (SELECT FROM users u
              WHERE u.id = :user_id AND u.moderator = true)

-- :name moderate-accept-quest! :! :n
-- :doc accept unmoderated changes to a quest
UPDATE quests
SET
  name = unmoderated_name,
  unmoderated_name = null,
  description = unmoderated_description,
  unmoderated_description = null,
  organisation = unmoderated_organisation,
  unmoderated_organisation = null,
  organisation_description = unmoderated_organisation_description,
  unmoderated_organisation_description = null,
  hashtags = unmoderated_hashtags,
  unmoderated_hashtags = null,
  picture = unmoderated_picture,
  unmoderated_picture = null,
  is_rejected = false
WHERE id = :id AND
      EXISTS (SELECT FROM users u
              WHERE u.id = :user_id AND u.moderator = true)

-- :name moderate-reject-quest! :! :n
-- :doc reject unmoderated changes to a quest
UPDATE quests
SET is_rejected = true
where id = :id AND
      EXISTS (SELECT FROM users u
              WHERE u.id = :user_id AND u.moderator = true)

-- :name get-unmoderated-quests-by-owner :? :*
-- :doc get all unmoderated quests by owner
SELECT
  q.id as id,
  q.unmoderated_name as name,
  q.unmoderated_description as description,
  q.unmoderated_organisation as organisation,
  q.unmoderated_organisation_description as organisation_description,
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
  q.unmoderated_hashtags as hashtags,
  q.unmoderated_picture as picture,
  (SELECT url FROM pictures WHERE id = q.unmoderated_picture) as picture_url,
  (SELECT COUNT(user_id) FROM parties WHERE quest_id = q.id) as participant_count,
  q.is_open as is_open,
  q.owner as owner,
  q.is_rejected as is_rejected,
  FALSE as moderated
FROM
  quests q
WHERE
  q.owner = :owner AND q.unmoderated_name IS NOT NULL

-- :name get-moderated-quests-by-owner :? :*
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
  q.picture as picture,
  (SELECT url FROM pictures WHERE id = q.picture) as picture_url,
  (SELECT COUNT(user_id) FROM parties WHERE quest_id = q.id) as participant_count,
  q.is_open as is_open,
  q.owner as owner,
  q.is_rejected as is_rejected,
  TRUE as moderated
FROM
  quests q
WHERE
  q.owner = :owner AND q.unmoderated_name IS NULL

-- :name get-quest-owner :? :1
-- :doc get quest owner
SELECT
  u.id as id,
  u.name as name,
  u.email as email,
  u.phone as phone,
  u.locale as locale
FROM
  quests q,
  users u
WHERE
  u.id = q.owner AND
  q.id = :id

-- :name update-quest! :? :1
-- :doc "Update a quest"
UPDATE
  quests
SET
  unmoderated_name = :name,
  unmoderated_description = :description,
  unmoderated_organisation = :organisation,
  unmoderated_organisation_description = :organisation_description,
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
  unmoderated_hashtags = :hashtags,
  unmoderated_picture = :picture,
  is_open = :is_open,
  is_rejected = false
WHERE
  id = :id AND
  (owner = :owner OR
   EXISTS (SELECT FROM users u
           WHERE u.id = :owner AND u.moderator = true))
RETURNING ID

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
WHERE lower(u.email) = lower(:email)
ON CONFLICT (user_id) DO
  UPDATE
    SET expires = :expires,
        token = uuid_generate_v4()
RETURNING token

-- :name delete-password-token! :! :n
-- :doc "Delete a token"
DELETE FROM password_tokens
WHERE token = :token

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
  u.locale as locale,
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
      is_active = true
  WHERE lower(email) = lower(:email) AND
        is_active = false
    AND EXISTS (
      SELECT 1
      FROM password_tokens
        WHERE id = user_id AND
              expires > now() AND
              token = :token)

-- :name change-password! :! :1
-- :doc "Change user password with password token"
UPDATE users
  SET
    pass = :pass,
    is_active = true
  WHERE lower(email) = lower(:email) AND
    EXISTS (
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

-- :name get-party-member :? :1
-- :doc "Get party member"
SELECT
  id as member_id,
  quest_id,
  user_id,
  days
FROM
  parties
WHERE
  id = :id

-- :name join-quest! :? :1
-- :doc "Join an open quest"
INSERT INTO
  parties (quest_id, user_id, days)
VALUES
  (:quest_id, :user_id, :days)
RETURNING id

-- :name can-join-open-quest? :? :1
-- :doc "Can join open quest?"
SELECT EXISTS(
  SELECT
    FROM
      quests
    WHERE
      end_time > NOW() AND
      id = :quest_id AND
      is_open = true AND
      name IS NOT NULL AND
      max_participants > (SELECT
                            COUNT(user_id)
                          FROM
                            parties
                          WHERE quest_id = :quest_id))

-- :name can-join-secret-quest? :? :1
-- :doc "Can join secret quest?"
SELECT EXISTS(
  SELECT
    FROM
      quests
    WHERE
      end_time > NOW() AND
      id = :quest_id AND
      is_open = false AND
      secret_party = :secret_party AND
      name IS NOT NULL AND
      max_participants > (SELECT
                            COUNT(user_id)
                          FROM
                            parties
                          WHERE quest_id = :quest_id))

-- :name get-party-member-info :? :1
-- :doc get party member info for user and quest
SELECT
  p.id as member_id,
  p.quest_id,
  p.user_id,
  p.days
FROM
  parties p
WHERE
  p.quest_id = :quest_id AND
  p.user_id = :user_id

-- :name get-quest-party-members :? :*
-- :doc get quest party
SELECT
  p.id as member_id,
  u.name as name,
  u.email as email,
  u.phone as phone,
  u.locale as locale
FROM
  quests q,
  parties p,
  users u
WHERE
  q.id = :quest_id AND
  p.quest_id = :quest_id AND
  p.user_id = u.id AND
  ((q.owner = :user_id) OR
    (u.id = :user_id AND u.moderator = true))
ORDER BY
  u.name ASC

-- :name remove-member-from-party! :! :*
-- :doc Remove member from party
DELETE FROM
  parties p
WHERE
  p.id = :member_id

-- :name make-moderator! :! :*
-- :doc Make a user a moderator
UPDATE users
SET moderator = true
WHERE id = :id

-- :name add-quest-days-worked! :! :*
-- :doc Get days worked for quest (0 if still in the future)
INSERT INTO deleted_quest_days
VALUES ((
       SELECT sum(p.days)
       FROM parties p,
            quests q
       WHERE q.id = p.quest_id AND
             q.id = :id AND
             q.name IS NOT NULL AND
             q.end_time < NOW()
       ))
