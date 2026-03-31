INSERT INTO permissions (name, description) VALUES
('access:manage', '{"en": "Can manage roles and permissions"}')
ON CONFLICT (name) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p 
WHERE r.name = 'SUPER_ADMIN' AND p.name = 'access:manage'
ON CONFLICT (role_id, permission_id) DO NOTHING;
