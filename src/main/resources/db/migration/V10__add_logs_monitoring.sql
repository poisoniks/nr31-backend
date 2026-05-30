INSERT INTO permissions (name, description) VALUES
('logs:read', '{"en": "Can access application logs"}')
ON CONFLICT (name) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p WHERE r.name = 'SUPER_ADMIN' AND p.name = 'logs:read'
ON CONFLICT (role_id, permission_id) DO NOTHING;
