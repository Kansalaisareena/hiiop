CREATE TABLE events
(id serial PRIMARY KEY,
 name varchar(300),
 owner uuid REFERENCES users(id) ON DELETE CASCADE,
 description TEXT NULL,
 unmoderated_description TEXT,
 max_participants INTEGER,
 picture VARCHAR(36),
 hashtags VARCHAR(1000),
 start_time TIME,
 end_time TIME
);
