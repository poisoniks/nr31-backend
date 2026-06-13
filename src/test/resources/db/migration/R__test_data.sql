-- Test Users
-- Password: testpass
INSERT INTO users (username, password_hash, email, email_verified) VALUES ('admin', '$2a$12$llGEJmpM5l3xhCORCr/tX.RrkU/GiJeYSjIcLZxmjZwMhtKzMwGya', 'admin@example.com', TRUE) ON CONFLICT (username) DO NOTHING;
INSERT INTO users (username, password_hash, email, email_verified) VALUES ('user', '$2a$12$llGEJmpM5l3xhCORCr/tX.RrkU/GiJeYSjIcLZxmjZwMhtKzMwGya', 'user@example.com', TRUE) ON CONFLICT (username) DO NOTHING;

INSERT INTO roles (name) VALUES ('ROLE_ADMIN') ON CONFLICT (name) DO NOTHING;
INSERT INTO roles (name) VALUES ('ROLE_USER') ON CONFLICT (name) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id) 
SELECT r.id, p.id FROM roles r, permissions p WHERE r.name = 'ROLE_ADMIN' AND p.name IN ('event:write', 'config:read', 'config:write', 'roster:read', 'roster:write', 'file:manage_quota', 'access:manage', 'file:upload:public', 'file:upload:attachment', 'file:delete', 'cms:write')
ON CONFLICT (role_id, permission_id) DO NOTHING;

UPDATE roles SET files_upload_quota_bytes = 10485760 WHERE name = 'ROLE_ADMIN';

INSERT INTO user_roles (user_id, role_id) 
SELECT u.id, r.id FROM users u, roles r WHERE u.username = 'admin' AND r.name = 'ROLE_ADMIN'
ON CONFLICT (user_id, role_id) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id) 
SELECT r.id, p.id FROM roles r, permissions p WHERE r.name = 'ROLE_USER' AND p.name IN ('file:upload:attachment')
ON CONFLICT (role_id, permission_id) DO NOTHING;

UPDATE roles SET files_upload_quota_bytes = 10485760 WHERE name = 'ROLE_USER';

INSERT INTO user_roles (user_id, role_id) 
SELECT u.id, r.id FROM users u, roles r WHERE u.username = 'user' AND r.name = 'ROLE_USER'
ON CONFLICT (user_id, role_id) DO NOTHING;

INSERT INTO event_types (name) SELECT CAST('{"en": "Type 1"}' AS JSONB) WHERE NOT EXISTS (SELECT 1 FROM event_types WHERE name->>'en' = 'Type 1');
INSERT INTO event_types (name) SELECT CAST('{"en": "Type 2"}' AS JSONB) WHERE NOT EXISTS (SELECT 1 FROM event_types WHERE name->>'en' = 'Type 2');

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

-- Default File Metadata for Tests
INSERT INTO files_metadata (id, original_name, stored_name, content_type, size_bytes, scope, uploader_id, created_at)
VALUES ('550e8400-e29b-41d4-a716-446655440000'::uuid, 'test-background.jpg', 'test-background.jpg', 'image/jpeg', 1024, 'LIBRARY', 1, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

-- CMS Pages for Cucumber tests
-- version=1 so that the first PUT /draft call uses version=1 (no conflict).
INSERT INTO pages (slug, title, version, created_at, updated_at) VALUES
    ('home',            '{"en": "Home Page"}',              1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('about',           '{"en": "About Page"}',             1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('events',          '{"en": "Events Page"}',            1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('news',            '{"en": "News Page"}',              1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('large-content',   '{"en": "Large Content Page"}',     1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('news-feed',       '{"en": "News Feed Page"}',         1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('hero-page',       '{"en": "Hero Page"}',              1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('restricted',      '{"en": "Restricted Page"}',        1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('publish-test',    '{"en": "Publish Test Page"}',      1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('public-widgets',  '{"en": "Public Widgets Page"}',    1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('test-page',       '{"en": "Test Page"}',              1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('conflict-page',   '{"en": "Conflict Page"}',          1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('restricted-page', '{"en": "Restricted Page"}',        1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('public-page',     '{"en": "Public Page"}',            1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('admin-page',      '{"en": "Admin Page"}',             1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('permission-page', '{"en": "Permission Page"}',        1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('copy-page',       '{"en": "Copy Page"}',              1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('invalid-page',    '{"en": "Invalid Page"}',           1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('draft-page',      '{"en": "Draft Page"}',             1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('update-page',     '{"en": "Update Page"}',            1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('archive-page',    '{"en": "Archive Page"}',           1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('draft-only-page', '{"en": "Draft Only Page"}',        1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (slug) DO NOTHING;

-- Seed a minimal PUBLISHED revision for every test page.
-- Required so that GET /draft can auto-create a draft by copying from published.
-- Without this, both GET /draft and PUT /draft return 404 for fresh pages.
INSERT INTO page_revisions (page_id, layout_data, status, created_at)
SELECT p.id,
       '{"slots": [{"slotType": "content", "widgets": [{"type": "richtext", "bodyContent": {"en": {"type": "doc", "content": [{"type": "paragraph", "content": [{"type": "text", "text": "Initial content"}]}]}}}]}]}'::jsonb,
       'PUBLISHED',
       CURRENT_TIMESTAMP
FROM pages p
WHERE p.slug IN (
    'home', 'about', 'events', 'news', 'large-content', 'news-feed', 'hero-page',
    'restricted', 'publish-test', 'public-widgets', 'test-page', 'conflict-page',
    'restricted-page', 'public-page', 'admin-page', 'permission-page', 'copy-page',
    'invalid-page', 'draft-page', 'update-page', 'archive-page'
)
AND NOT EXISTS (
    SELECT 1 FROM page_revisions pr WHERE pr.page_id = p.id
);

-- KB Test Permissions and Roles
INSERT INTO role_permissions (role_id, permission_id) 
SELECT r.id, p.id FROM roles r, permissions p 
WHERE r.name = 'ROLE_ADMIN' AND p.name = 'kb:admin'
ON CONFLICT (role_id, permission_id) DO NOTHING;

INSERT INTO users (username, password_hash, email, email_verified) VALUES ('kbauthor', '$2a$12$llGEJmpM5l3xhCORCr/tX.RrkU/GiJeYSjIcLZxmjZwMhtKzMwGya', 'kbauthor@example.com', TRUE) ON CONFLICT (username) DO NOTHING;
INSERT INTO roles (name) VALUES ('ROLE_KB_AUTHOR') ON CONFLICT (name) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p 
WHERE r.name = 'ROLE_KB_AUTHOR' AND p.name = 'kb:write'
ON CONFLICT (role_id, permission_id) DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r 
WHERE u.username = 'kbauthor' AND r.name = 'ROLE_KB_AUTHOR'
ON CONFLICT (user_id, role_id) DO NOTHING;

-- Seed KB Folders
-- Root folder
INSERT INTO kb_folders (id, name, slug, parent_id, is_restricted, created_at)
VALUES (100, '{"en": "General Support"}', 'general-support', NULL, FALSE, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

-- Another root folder (restricted)
INSERT INTO kb_folders (id, name, slug, parent_id, is_restricted, created_at)
VALUES (101, '{"en": "Admin Only"}', 'admin-only', NULL, TRUE, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

-- Sub-folder
INSERT INTO kb_folders (id, name, slug, parent_id, is_restricted, created_at)
VALUES (102, '{"en": "User Guides"}', 'user-guides', 100, FALSE, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

-- Empty folder for deletion tests
INSERT INTO kb_folders (id, name, slug, parent_id, is_restricted, created_at)
VALUES (103, '{"en": "Empty Folder"}', 'empty-folder', NULL, FALSE, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

-- Seed KB Articles
-- Article in support folder authored by admin
INSERT INTO kb_articles (id, folder_id, author_id, title, slug, content, plain_text_content, created_at, updated_at)
VALUES (
    200, 
    100, 
    (SELECT id FROM users WHERE username = 'admin'), 
    '{"en": "How to Reset Password"}', 
    'how-to-reset-password', 
    '{"en": {"type": "doc", "content": [{"type": "paragraph", "content": [{"type": "text", "text": "To reset password, go to settings."}]}]}}'::jsonb, 
    '{"en": "To reset password, go to settings."}'::jsonb, 
    CURRENT_TIMESTAMP, 
    CURRENT_TIMESTAMP
) ON CONFLICT (id) DO NOTHING;

-- Article authored by kbauthor (used to test that kbauthor can update/delete their own articles)
INSERT INTO kb_articles (id, folder_id, author_id, title, slug, content, plain_text_content, created_at, updated_at)
VALUES (
    201, 
    100, 
    (SELECT id FROM users WHERE username = 'kbauthor'), 
    '{"en": "Author Guide"}', 
    'author-guide', 
    '{"en": {"type": "doc", "content": [{"type": "paragraph", "content": [{"type": "text", "text": "This is a guide for authors."}]}]}}'::jsonb, 
    '{"en": "This is a guide for authors."}'::jsonb, 
    CURRENT_TIMESTAMP, 
    CURRENT_TIMESTAMP
) ON CONFLICT (id) DO NOTHING;

