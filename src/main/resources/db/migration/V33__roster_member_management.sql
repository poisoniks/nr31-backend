-- Add new roster permissions
INSERT INTO permissions (name, description) VALUES
('roster:read', '{"en": "Can read roster member data"}'),
('roster:write', '{"en": "Can manage roster member data"}')
ON CONFLICT (name) DO NOTHING;

-- Grant to SUPER_ADMIN
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'SUPER_ADMIN' AND p.name IN ('roster:read', 'roster:write')
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- AppConfig for export template
INSERT INTO app_config (config_key, description, config_value, config_schema)
VALUES (
  'roster_export_template_file_id',
  '{"en": "File ID of the roster export Excel template"}',
  'null',
  '{
    "$schema": "http://json-schema.org/draft-07/schema#",
    "type": ["integer", "null"],
    "description": "File metadata ID of the roster export template"
  }'
)
ON CONFLICT (config_key) DO NOTHING;

-- Reference: Specialties
CREATE TABLE specialties (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    sort_order  INTEGER NOT NULL DEFAULT 0
);

-- Reference: Ranks
CREATE TABLE ranks (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    abbreviation    VARCHAR(10) NOT NULL UNIQUE,
    tag             VARCHAR(100),
    category        VARCHAR(20) NOT NULL,
    specialty_id    BIGINT REFERENCES specialties(id),
    sort_order      INTEGER NOT NULL DEFAULT 0
);

-- Reference: Awards
CREATE TABLE awards (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(200) NOT NULL,
    abbreviation    VARCHAR(50) NOT NULL UNIQUE,
    sort_order      INTEGER NOT NULL DEFAULT 0
);

-- Reference: Training disciplines
CREATE TABLE training_disciplines (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(100) NOT NULL UNIQUE,
    is_mandatory    BOOLEAN NOT NULL DEFAULT false,
    sort_order      INTEGER NOT NULL DEFAULT 0
);

-- Core: Roster members
CREATE TABLE roster_members (
    id              BIGSERIAL PRIMARY KEY,
    sequence_number INTEGER,
    mb_nickname     VARCHAR(100) NOT NULL,
    nationality     VARCHAR(100),
    discord_nickname VARCHAR(100),
    discord_id      VARCHAR(50),
    specialty_id    BIGINT REFERENCES specialties(id),
    rank_id         BIGINT REFERENCES ranks(id),
    unit_type_id    BIGINT REFERENCES unit_types(id),
    join_date       DATE,
    penalties       DECIMAL(5,2) DEFAULT 0,
    is_archived     BOOLEAN NOT NULL DEFAULT false,
    training_notes  TEXT,
    user_id         BIGINT REFERENCES users(id),
    created_at      TIMESTAMP DEFAULT NOW(),
    updated_at      TIMESTAMP DEFAULT NOW()
);

-- Junction: Member awards
CREATE TABLE member_awards (
    id              BIGSERIAL PRIMARY KEY,
    member_id       BIGINT NOT NULL REFERENCES roster_members(id) ON DELETE CASCADE,
    award_id        BIGINT NOT NULL REFERENCES awards(id) ON DELETE CASCADE,
    awarded_date    DATE,
    UNIQUE(member_id, award_id)
);

-- Monthly attendance records
CREATE TABLE attendance_records (
    id              BIGSERIAL PRIMARY KEY,
    member_id       BIGINT NOT NULL REFERENCES roster_members(id) ON DELETE CASCADE,
    year            INTEGER NOT NULL,
    month           INTEGER NOT NULL,
    attendance_count INTEGER,
    status          VARCHAR(30),
    UNIQUE(member_id, year, month)
);

-- Monthly event counts
CREATE TABLE monthly_event_counts (
    id              BIGSERIAL PRIMARY KEY,
    year            INTEGER NOT NULL,
    month           INTEGER NOT NULL,
    event_count     INTEGER NOT NULL,
    UNIQUE(year, month)
);

-- Junction: Member training scores
CREATE TABLE member_training_scores (
    id              BIGSERIAL PRIMARY KEY,
    member_id       BIGINT NOT NULL REFERENCES roster_members(id) ON DELETE CASCADE,
    discipline_id   BIGINT NOT NULL REFERENCES training_disciplines(id) ON DELETE CASCADE,
    score           INTEGER NOT NULL CHECK (score >= 1 AND score <= 5),
    UNIQUE(member_id, discipline_id)
);

-- Indexes
CREATE INDEX idx_roster_members_discord_id ON roster_members(discord_id);
CREATE INDEX idx_roster_members_mb_nickname ON roster_members(mb_nickname);
CREATE INDEX idx_roster_members_unit_type ON roster_members(unit_type_id);
CREATE INDEX idx_attendance_records_year_month ON attendance_records(year, month);
CREATE INDEX idx_attendance_records_member ON attendance_records(member_id);
