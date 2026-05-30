INSERT INTO permissions (name, description) VALUES
('roster:write', '{"en": "Can change roster"}')
ON CONFLICT (name) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p WHERE r.name = 'SUPER_ADMIN' AND p.name = 'roster:write'
ON CONFLICT (role_id, permission_id) DO NOTHING;

ALTER TABLE unit_types ADD COLUMN custom_icon VARCHAR(255);
