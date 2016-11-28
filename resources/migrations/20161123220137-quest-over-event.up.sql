CREATE TABLE IF NOT EXISTS pictures
(id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
 url TEXT NOT NULL);

CREATE TABLE IF NOT EXISTS quests
(id SERIAL PRIMARY KEY,
 name VARCHAR(300),
 start_time TIMESTAMP WITH TIME ZONE,
 end_time TIMESTAMP WITH TIME ZONE,
 address TEXT,
 town TEXT,
 categories json,
 description TEXT NULL,
 unmoderated_description TEXT,
 max_participants INTEGER,
 hashtags json,
 picture uuid REFERENCES pictures(id) DEFAULT NULL,
 owner uuid REFERENCES users(id) ON DELETE CASCADE,
 is_open BOOLEAN DEFAULT true,
 secret_party uuid DEFAULT uuid_generate_v4());

CREATE TABLE IF NOT EXISTS parties
(id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
 quest_id INTEGER REFERENCES quests(id),
 user_id uuid REFERENCES users(id),
 days INTEGER NOT NULL DEFAULT 1);
