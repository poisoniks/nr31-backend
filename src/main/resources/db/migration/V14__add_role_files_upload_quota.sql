ALTER TABLE roles ADD COLUMN files_upload_quota_bytes BIGINT;

INSERT INTO permissions (name, description) VALUES
('file:manage_quota', '{"en": "Can manage user file upload quotas by role"}')
ON CONFLICT (name) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p 
WHERE r.name = 'SUPER_ADMIN' AND p.name = 'file:manage_quota'
ON CONFLICT (role_id, permission_id) DO NOTHING;
