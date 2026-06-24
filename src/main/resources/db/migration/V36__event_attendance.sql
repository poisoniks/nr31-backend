-- Add attendance weight to event types
ALTER TABLE event_types ADD COLUMN attendance_weight INTEGER NOT NULL DEFAULT 1;

-- Rename attendance_records.attendance_count for clarity
ALTER TABLE attendance_records RENAME COLUMN attendance_count TO manual_attendance_count;

-- Rename monthly_event_counts.event_count for clarity
ALTER TABLE monthly_event_counts RENAME COLUMN event_count TO manual_event_count;

-- New: per-event attendance records
CREATE TABLE event_attendance (
    id              BIGSERIAL PRIMARY KEY,
    member_id       BIGINT NOT NULL REFERENCES roster_members(id) ON DELETE CASCADE,
    event_id        BIGINT NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    occurrence_date TIMESTAMPTZ NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(member_id, event_id, occurrence_date)
);

CREATE INDEX idx_event_attendance_event_occurrence ON event_attendance(event_id, occurrence_date);
CREATE INDEX idx_event_attendance_member ON event_attendance(member_id);
CREATE INDEX idx_event_attendance_occurrence_date ON event_attendance(occurrence_date);

-- New permission
INSERT INTO permissions (name, description) VALUES
('attendance:write', '{"en": "Can record event attendance"}')
ON CONFLICT (name) DO NOTHING;

-- Grant to SUPER_ADMIN
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'SUPER_ADMIN' AND p.name = 'attendance:write'
ON CONFLICT (role_id, permission_id) DO NOTHING;
