UPDATE permissions
SET description = format('{"en": %L}', description);

ALTER TABLE permissions
ALTER COLUMN description TYPE JSONB USING description::JSONB;

INSERT INTO permissions (name, description) VALUES
('cache:clear', '{"en": "Can clear application cache"}'),
('event:write', '{"en": "Can create/edit/delete events"}'),
('config:read', '{"en": "Can read application configs"}'),
('config:write', '{"en": "Can write application configs"}')
ON CONFLICT (name) DO NOTHING;

INSERT INTO roles (name) VALUES
('SUPER_ADMIN')
ON CONFLICT (name) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p WHERE r.name = 'SUPER_ADMIN'
ON CONFLICT (role_id, permission_id) DO NOTHING;
