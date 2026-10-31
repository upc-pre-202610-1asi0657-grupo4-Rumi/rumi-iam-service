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
