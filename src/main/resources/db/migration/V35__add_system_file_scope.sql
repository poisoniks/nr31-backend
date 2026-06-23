INSERT INTO permissions (name, description) VALUES
('system-file:read', '{"en": "Can download system files"}')
ON CONFLICT (name) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p WHERE r.name = 'SUPER_ADMIN' AND p.name = 'system-file:read'
ON CONFLICT (role_id, permission_id) DO NOTHING;
