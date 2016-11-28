CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE password_tokens
(token uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
 user_id uuid UNIQUE REFERENCES users(id) ON DELETE CASCADE,
 expires timestamp with time zone NOT NULL
);





