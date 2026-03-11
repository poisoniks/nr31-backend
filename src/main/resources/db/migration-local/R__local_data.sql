-- Password: testpass
INSERT INTO users (username, password_hash) VALUES ('admin', '$2a$12$llGEJmpM5l3xhCORCr/tX.RrkU/GiJeYSjIcLZxmjZwMhtKzMwGya') ON CONFLICT (username) DO NOTHING;

INSERT INTO user_roles (user_id, role_id) SELECT u.id, r.id FROM users u, roles r WHERE u.username = 'admin' AND r.name = 'SUPER_ADMIN' ON CONFLICT (user_id, role_id) DO NOTHING;

INSERT INTO event_types (name) SELECT CAST('{"en": "Flagspawn"}' AS JSONB) WHERE NOT EXISTS (SELECT 1 FROM event_types WHERE name->>'en' = 'Flagspawn');
INSERT INTO event_types (name) SELECT CAST('{"en": "Line battle"}' AS JSONB) WHERE NOT EXISTS (SELECT 1 FROM event_types WHERE name->>'en' = 'Line battle');
INSERT INTO event_types (name) SELECT CAST('{"en": "Clan war"}' AS JSONB) WHERE NOT EXISTS (SELECT 1 FROM event_types WHERE name->>'en' = 'Clan war');

INSERT INTO unit_types (name) SELECT CAST('{"en": "Infantry"}' AS JSONB) WHERE NOT EXISTS (SELECT 1 FROM unit_types WHERE name->>'en' = 'Infantry');
INSERT INTO unit_types (name) SELECT CAST('{"en": "Artilery"}' AS JSONB) WHERE NOT EXISTS (SELECT 1 FROM unit_types WHERE name->>'en' = 'Artilery');
INSERT INTO unit_types (name) SELECT CAST('{"en": "rifles"}' AS JSONB) WHERE NOT EXISTS (SELECT 1 FROM unit_types WHERE name->>'en' = 'rifles');
