-- Test Users
-- Password: testpass
INSERT INTO users (username, password_hash) VALUES ('admin', '$2a$12$llGEJmpM5l3xhCORCr/tX.RrkU/GiJeYSjIcLZxmjZwMhtKzMwGya') ON CONFLICT (username) DO NOTHING;
INSERT INTO users (username, password_hash) VALUES ('user', '$2a$12$llGEJmpM5l3xhCORCr/tX.RrkU/GiJeYSjIcLZxmjZwMhtKzMwGya') ON CONFLICT (username) DO NOTHING;

INSERT INTO roles (name) VALUES ('ROLE_ADMIN') ON CONFLICT (name) DO NOTHING;
INSERT INTO roles (name) VALUES ('ROLE_USER') ON CONFLICT (name) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id) 
SELECT r.id, p.id FROM roles r, permissions p WHERE r.name = 'ROLE_ADMIN' AND p.name IN ('event:write', 'config:read', 'config:write')
ON CONFLICT (role_id, permission_id) DO NOTHING;

INSERT INTO user_roles (user_id, role_id) 
SELECT u.id, r.id FROM users u, roles r WHERE u.username = 'admin' AND r.name = 'ROLE_ADMIN'
ON CONFLICT (user_id, role_id) DO NOTHING;

INSERT INTO event_types (name, custom_icon) SELECT CAST('{"en": "Type 1"}' AS JSONB), 'icon1' WHERE NOT EXISTS (SELECT 1 FROM event_types WHERE name->>'en' = 'Type 1');
INSERT INTO event_types (name, custom_icon) SELECT CAST('{"en": "Type 2"}' AS JSONB), 'icon2' WHERE NOT EXISTS (SELECT 1 FROM event_types WHERE name->>'en' = 'Type 2');

INSERT INTO unit_types (name, description) SELECT CAST('{"en": "Alpha"}' AS JSONB), CAST('{"en": "First Squad"}' AS JSONB) WHERE NOT EXISTS (SELECT 1 FROM unit_types WHERE name->>'en' = 'Alpha');
INSERT INTO unit_types (name, description) SELECT CAST('{"en": "Bravo"}' AS JSONB), CAST('{"en": "Second Squad"}' AS JSONB) WHERE NOT EXISTS (SELECT 1 FROM unit_types WHERE name->>'en' = 'Bravo');

-- App Config Test Data
INSERT INTO app_config (config_key, description, config_value, config_schema) 
VALUES ('test_config_1', CAST('{"en": "Test Config 1"}' AS JSON), CAST('{"enabled": true}' AS JSON), CAST('{"type": "object", "properties": {"enabled": {"type": "boolean"}}, "required": ["enabled"]}' AS JSON)) ON CONFLICT (config_key) DO NOTHING;

INSERT INTO app_config (config_key, description, config_value, config_schema) 
VALUES ('test_config_2', CAST('{"en": "Test Config 2"}' AS JSON), CAST('{"timeout": 5000}' AS JSON), CAST('{"type": "object", "properties": {"timeout": {"type": "integer"}}, "required": ["timeout"]}' AS JSON)) ON CONFLICT (config_key) DO NOTHING;

INSERT INTO app_config (config_key, description, config_value, config_schema) 
VALUES ('test_config_3', CAST('{"en": "Test Config 3"}' AS JSON), CAST('{"retries": 3}' AS JSON), CAST('{"type": "object", "properties": {"retries": {"type": "integer"}}, "required": ["retries"]}' AS JSON)) ON CONFLICT (config_key) DO NOTHING;

INSERT INTO app_config (config_key, description, config_value, config_schema) 
VALUES ('disabled_endpoints', CAST('{"en": "Disabled endpoints"}' AS JSON), CAST('["one","two"]' AS JSON), CAST('{"type": "array","items": {"type": "string","minLength": 1}}' AS JSON)) ON CONFLICT (config_key) DO NOTHING;
