CREATE TABLE pages (
    id         BIGSERIAL    PRIMARY KEY,
    slug       VARCHAR(255) NOT NULL,
    title      VARCHAR(500) NOT NULL,
    version    INTEGER      NOT NULL DEFAULT 1,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pages_slug_unique UNIQUE (slug),
    CONSTRAINT pages_version_positive CHECK (version > 0)
);

CREATE INDEX idx_pages_slug ON pages(slug);

CREATE TABLE page_revisions (
    id          BIGSERIAL   PRIMARY KEY,
    page_id     BIGINT      NOT NULL,
    layout_data JSONB       NOT NULL,
    status      VARCHAR(20) NOT NULL,
    created_at  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_page_revisions_page FOREIGN KEY (page_id) REFERENCES pages(id) ON DELETE CASCADE,
    CONSTRAINT page_revisions_status_check CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED'))
);

CREATE INDEX idx_page_revisions_page_id ON page_revisions(page_id);
CREATE INDEX idx_page_revisions_status ON page_revisions(status);
CREATE INDEX idx_page_revisions_page_status ON page_revisions(page_id, status);

INSERT INTO permissions (name, description) VALUES
('cms:write', '{"en": "Can administer CMS pages"}')
ON CONFLICT (name) DO NOTHING;

INSERT INTO app_config (config_key, description, config_value, config_schema)
VALUES (
    'cms_slot_restrictions',
    '{"en": "Defines which widget types are allowed in each slot type"}',
    '{
        "hero": ["text", "image", "video"],
        "sidebar": ["text", "image"],
        "content": ["text", "image", "video", "embed"],
        "footer": ["text"]
    }',
    '{
        "type": "object",
        "patternProperties": {
            "^[a-zA-Z0-9_-]+$": {
                "type": "array",
                "items": {
                    "type": "string",
                    "pattern": "^[a-zA-Z0-9_-]+$"
                }
            }
        }
    }'
)
ON CONFLICT (config_key) DO NOTHING;
