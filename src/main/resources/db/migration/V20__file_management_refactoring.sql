ALTER TABLE files_metadata DROP COLUMN status;
ALTER TABLE files_metadata ADD COLUMN scope VARCHAR(20) NOT NULL DEFAULT 'ATTACHMENT';
CREATE INDEX idx_files_metadata_scope ON files_metadata(scope);

ALTER TABLE files_metadata DROP CONSTRAINT IF EXISTS files_metadata_stored_name_key;

ALTER TABLE unit_types DROP COLUMN IF EXISTS custom_icon;
ALTER TABLE unit_types ADD COLUMN custom_icon_id UUID REFERENCES files_metadata(id) ON DELETE SET NULL;
CREATE INDEX idx_unit_types_custom_icon ON unit_types(custom_icon_id);

ALTER TABLE event_types DROP COLUMN IF EXISTS custom_icon;
ALTER TABLE event_types ADD COLUMN custom_icon_id UUID REFERENCES files_metadata(id) ON DELETE SET NULL;
CREATE INDEX idx_event_types_custom_icon ON event_types(custom_icon_id);

UPDATE permissions SET name = 'file:upload:public' WHERE name = 'file:upload';

INSERT INTO permissions (name, description) VALUES
('file:upload:attachment', '{"en": "Can upload file attachments"}')
ON CONFLICT (name) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p WHERE r.name = 'SUPER_ADMIN' AND p.name = 'file:upload:attachment'
ON CONFLICT (role_id, permission_id) DO NOTHING;
