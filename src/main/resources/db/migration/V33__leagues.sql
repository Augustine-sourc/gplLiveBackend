-- Leagues: a group of users with their own scoped leaderboard (predictions
-- and Fantasy points, both computed live from data that already exists
-- elsewhere - nothing duplicated here). A league is either public
-- (discoverable by name search, join instantly) or private (hidden from
-- search, joinable only by invite code, and joining creates a PENDING
-- request the creator must accept before it counts as membership).
CREATE TABLE leagues (
    id SERIAL PRIMARY KEY,
    name VARCHAR(60) NOT NULL,
    is_public BOOLEAN NOT NULL DEFAULT TRUE,
    -- Short shareable code, e.g. "KTK4X9" - how private leagues get joined,
    -- and a convenient alternate way into a public one too.
    invite_code VARCHAR(12) NOT NULL,
    member_limit INT NOT NULL DEFAULT 20,
    creator_id INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT uq_leagues_invite_code UNIQUE (invite_code)
);

-- One row per user per league. PENDING = requested to join a private league
-- and awaiting the creator's decision (doesn't count toward member_limit
-- until accepted); ACTIVE = an actual member (counts toward member_limit,
-- shows up on the leaderboard). The creator gets an ACTIVE row the moment
-- their league is created.
CREATE TABLE league_memberships (
    id SERIAL PRIMARY KEY,
    league_id INT NOT NULL REFERENCES leagues(id) ON DELETE CASCADE,
    user_id INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status VARCHAR(10) NOT NULL DEFAULT 'ACTIVE',
    requested_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT uq_league_memberships_league_user UNIQUE (league_id, user_id)
);

CREATE INDEX idx_league_memberships_league ON league_memberships (league_id);
CREATE INDEX idx_league_memberships_user ON league_memberships (user_id);

-- Own notification category for join-request/accept/reject messages,
-- alongside the existing DEADLINE/RANK/GOAL/CAPTAIN types (see
-- V14__notification.sql for the enum type this extends).
ALTER TYPE notification_type_enum ADD VALUE IF NOT EXISTS 'LEAGUE';
