-- Schema of iamdb (PostgreSQL), the database of rumi-iam-service.
-- The default profile runs Hibernate with ddl-auto=validate, so these tables must exist.
-- Create the database first:  CREATE DATABASE iamdb;

CREATE TABLE users (
    id            uuid         PRIMARY KEY,
    email         varchar(254) NOT NULL UNIQUE,
    full_name     varchar(100) NOT NULL,
    password_hash varchar(60)  NOT NULL,
    role          varchar(20)  NOT NULL CHECK (role IN ('ADMINISTRATOR', 'RESIDENT')),
    created_at    timestamp    NOT NULL
);

-- Buildings a user belongs to; copied into the buildingIds claim of the token. Empty until a later story fills it.
CREATE TABLE user_building_ids (
    user_id     uuid NOT NULL REFERENCES users (id),
    building_id uuid NOT NULL,
    PRIMARY KEY (user_id, building_id)
);

-- Single-use password reset tokens (US03). Only the SHA-256 of the token is stored, never the token itself.
CREATE TABLE password_reset_tokens (
    id          uuid        PRIMARY KEY,
    user_id     uuid        NOT NULL REFERENCES users (id),
    token_hash  varchar(64) NOT NULL UNIQUE,
    expires_at  timestamp   NOT NULL,
    used_at     timestamp,
    created_at  timestamp   NOT NULL
);
CREATE INDEX idx_password_reset_tokens_user ON password_reset_tokens (user_id);

-- Firebase tokens of the devices of each user, used by Notification to push alerts (US14).
CREATE TABLE device_tokens (
    id            uuid          PRIMARY KEY,
    user_id       uuid          NOT NULL REFERENCES users (id),
    token         varchar(1024) NOT NULL UNIQUE,
    platform      varchar(10)   NOT NULL CHECK (platform IN ('ANDROID', 'IOS', 'WEB')),
    registered_at timestamp     NOT NULL,
    updated_at    timestamp     NOT NULL
);
CREATE INDEX idx_device_tokens_user ON device_tokens (user_id);
