CREATE TABLE files_metadata (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    original_name VARCHAR(255) NOT NULL,
    stored_name VARCHAR(255) NOT NULL UNIQUE,
    content_type VARCHAR(100) NOT NULL,
    size_bytes BIGINT NOT NULL,
    uploader_id BIGINT NOT NULL REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_files_metadata_uploader ON files_metadata(uploader_id);

INSERT INTO permissions (name, description) VALUES
('file:upload', '{"en": "Can upload files"}'),
('file:delete', '{"en": "Can delete files"}')
ON CONFLICT (name) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p WHERE r.name = 'SUPER_ADMIN' AND p.name IN ('file:upload', 'file:delete')
ON CONFLICT (role_id, permission_id) DO NOTHING;
