CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE spaces (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    -- BaseEntity
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Entity Specific
    space_name VARCHAR(255),
    description VARCHAR(500),
    profile_pic VARCHAR(255)

);