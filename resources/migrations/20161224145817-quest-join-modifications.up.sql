ALTER TABLE
  parties
DROP CONSTRAINT parties_quest_id_fkey,
ADD CONSTRAINT parties_quest_id_fkey
   FOREIGN KEY (quest_id)
   REFERENCES quests(id)
   ON DELETE CASCADE,
DROP CONSTRAINT parties_user_id_fkey,
ADD CONSTRAINT parties_user_id_fkey
   FOREIGN KEY (user_id)
   REFERENCES users(id)
   ON DELETE CASCADE,
ADD CONSTRAINT
  join_only_once UNIQUE (quest_id, user_id);

ALTER TABLE
  users
ADD COLUMN
  phone TEXT NULL;
