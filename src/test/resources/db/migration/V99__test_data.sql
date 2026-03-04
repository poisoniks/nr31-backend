-- Test Users
-- Password: testpass
INSERT INTO users (id, username, password_hash) VALUES (1, 'admin', '$2a$12$llGEJmpM5l3xhCORCr/tX.RrkU/GiJeYSjIcLZxmjZwMhtKzMwGya');
INSERT INTO users (id, username, password_hash) VALUES (2, 'user', '$2a$12$llGEJmpM5l3xhCORCr/tX.RrkU/GiJeYSjIcLZxmjZwMhtKzMwGya');

INSERT INTO roles (id, name) VALUES (1, 'ROLE_ADMIN');
INSERT INTO roles (id, name) VALUES (2, 'ROLE_USER');

INSERT INTO permissions (id, name) VALUES (1, 'event:write');

INSERT INTO role_permissions (role_id, permission_id) VALUES (1, 1);

INSERT INTO user_roles (user_id, role_id) VALUES (1, 1);

INSERT INTO event_types (id, name, custom_icon) VALUES (1, CAST('{"en": "Type 1"}' AS JSON), 'icon1');
INSERT INTO event_types (id, name, custom_icon) VALUES (2, CAST('{"en": "Type 2"}' AS JSON), 'icon2');

INSERT INTO unit_types (id, name, description) VALUES (1, CAST('{"en": "Alpha"}' AS JSON), CAST('{"en": "First Squad"}' AS JSON));
INSERT INTO unit_types (id, name, description) VALUES (2, CAST('{"en": "Bravo"}' AS JSON), CAST('{"en": "Second Squad"}' AS JSON));

-- Fix sequences because we inserted exact IDs
ALTER SEQUENCE IF EXISTS event_types_id_seq RESTART WITH 100;
ALTER SEQUENCE IF EXISTS unit_types_id_seq RESTART WITH 100;
ALTER SEQUENCE IF EXISTS users_id_seq RESTART WITH 10;
