CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE users
(id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
 name VARCHAR(300) DEFAULT NULL,
 email VARCHAR(100) UNIQUE,
 moderator BOOLEAN DEFAULT false,
 last_login TIME,
 is_active BOOLEAN DEFAULT false,
 pass TEXT DEFAULT NULL);
