-- Test Users
-- Password: testpass
INSERT INTO users (id, username, password_hash) VALUES (1, 'admin', '$2a$12$llGEJmpM5l3xhCORCr/tX.RrkU/GiJeYSjIcLZxmjZwMhtKzMwGya');
INSERT INTO users (id, username, password_hash) VALUES (2, 'user', '$2a$12$llGEJmpM5l3xhCORCr/tX.RrkU/GiJeYSjIcLZxmjZwMhtKzMwGya');

INSERT INTO roles (id, name) VALUES (1, 'ROLE_ADMIN');
INSERT INTO roles (id, name) VALUES (2, 'ROLE_USER');

INSERT INTO permissions (id, name) VALUES (1, 'event:write');
INSERT INTO permissions (id, name) VALUES (2, 'config:read');
INSERT INTO permissions (id, name) VALUES (3, 'config:write');

INSERT INTO role_permissions (role_id, permission_id) VALUES (1, 1);
INSERT INTO role_permissions (role_id, permission_id) VALUES (1, 2);
INSERT INTO role_permissions (role_id, permission_id) VALUES (1, 3);

INSERT INTO user_roles (user_id, role_id) VALUES (1, 1);

INSERT INTO event_types (id, name, custom_icon) VALUES (1, CAST('{"en": "Type 1"}' AS JSON), 'icon1');
INSERT INTO event_types (id, name, custom_icon) VALUES (2, CAST('{"en": "Type 2"}' AS JSON), 'icon2');

INSERT INTO unit_types (id, name, description) VALUES (1, CAST('{"en": "Alpha"}' AS JSON), CAST('{"en": "First Squad"}' AS JSON));
INSERT INTO unit_types (id, name, description) VALUES (2, CAST('{"en": "Bravo"}' AS JSON), CAST('{"en": "Second Squad"}' AS JSON));

-- Fix sequences because we inserted exact IDs
ALTER SEQUENCE IF EXISTS event_types_id_seq RESTART WITH 100;
ALTER SEQUENCE IF EXISTS unit_types_id_seq RESTART WITH 100;
ALTER SEQUENCE IF EXISTS users_id_seq RESTART WITH 10;
ALTER SEQUENCE IF EXISTS permissions_id_seq RESTART WITH 10;
ALTER SEQUENCE IF EXISTS roles_id_seq RESTART WITH 10;

-- App Config Test Data
INSERT INTO app_config (config_key, description, config_value, config_schema) 
VALUES ('test_config_1', CAST('{"en": "Test Config 1"}' AS JSON), CAST('{"enabled": true}' AS JSON), CAST('{"type": "object", "properties": {"enabled": {"type": "boolean"}}, "required": ["enabled"]}' AS JSON));

INSERT INTO app_config (config_key, description, config_value, config_schema) 
VALUES ('test_config_2', CAST('{"en": "Test Config 2"}' AS JSON), CAST('{"timeout": 5000}' AS JSON), CAST('{"type": "object", "properties": {"timeout": {"type": "integer"}}, "required": ["timeout"]}' AS JSON));

INSERT INTO app_config (config_key, description, config_value, config_schema) 
VALUES ('test_config_3', CAST('{"en": "Test Config 3"}' AS JSON), CAST('{"retries": 3}' AS JSON), CAST('{"type": "object", "properties": {"retries": {"type": "integer"}}, "required": ["retries"]}' AS JSON));

INSERT INTO app_config (config_key, description, config_value, config_schema) 
VALUES ('disabled_endpoints', CAST('{"en": "Disabled endpoints"}' AS JSON), CAST('["one","two"]' AS JSON), CAST('{"type": "array","items": {"type": "string","minLength": 1}}' AS JSON));
