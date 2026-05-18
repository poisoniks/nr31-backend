CREATE EXTENSION IF NOT EXISTS pg_trgm;

INSERT INTO permissions (name, description) VALUES
('kb:write', '{"en": "Can create and edit own articles in standard folders"}'),
('kb:admin', '{"en": "Can manage all folders, edit any article, and access restricted folders"}')
ON CONFLICT (name) DO NOTHING;

-- Extracts all values from a JSONB map into a single space-separated string
CREATE OR REPLACE FUNCTION extract_localized_text(data JSONB)
RETURNS TEXT AS $$
    SELECT string_agg(val, ' ' ORDER BY key)
    FROM jsonb_each_text(data) AS t(key, val);
$$ LANGUAGE sql IMMUTABLE PARALLEL SAFE;

-- Dynamically builds a tsvector, applying 'english' to 'en' and 'simple' to everything else
CREATE OR REPLACE FUNCTION build_localized_tsvector(data JSONB, weight "char")
RETURNS tsvector AS $$
DECLARE
    result tsvector := ''::tsvector;
    k text;
    v text;
    dict regconfig;
BEGIN
    FOR k, v IN SELECT * FROM jsonb_each_text(data) LOOP
        IF k = 'en' THEN
            dict := 'english'::regconfig;
        ELSE
            dict := 'simple'::regconfig;
        END IF;

        result := result || setweight(to_tsvector(dict, coalesce(v, '')), weight);
    END LOOP;
    RETURN result;
END;
$$ LANGUAGE plpgsql IMMUTABLE PARALLEL SAFE;

CREATE TABLE kb_folders (
    id            BIGSERIAL PRIMARY KEY,
    name          JSONB NOT NULL,
    slug          VARCHAR(255) NOT NULL,
    parent_id     BIGINT REFERENCES kb_folders(id) ON DELETE RESTRICT,
    is_restricted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT kb_folders_slug_parent_unique UNIQUE (slug, parent_id)
);

CREATE UNIQUE INDEX idx_kb_folders_slug_root ON kb_folders(slug) WHERE parent_id IS NULL;
CREATE INDEX idx_kb_folders_parent_id ON kb_folders(parent_id);

CREATE TABLE kb_articles (
    id                 BIGSERIAL PRIMARY KEY,
    folder_id          BIGINT NOT NULL REFERENCES kb_folders(id) ON DELETE RESTRICT,
    author_id          BIGINT NOT NULL REFERENCES users(id),
    title              JSONB NOT NULL,
    slug               VARCHAR(255) NOT NULL UNIQUE,
    content            JSONB NOT NULL,
    plain_text_content JSONB NOT NULL,

    search_vector      tsvector GENERATED ALWAYS AS (
                           build_localized_tsvector(title, 'A') ||
                           build_localized_tsvector(plain_text_content, 'B')
                       ) STORED,

    created_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_kb_articles_slug ON kb_articles(slug);
CREATE INDEX idx_kb_articles_folder_id ON kb_articles(folder_id);
CREATE INDEX idx_kb_articles_search ON kb_articles USING GIN (search_vector);
CREATE INDEX idx_kb_articles_trgm_title ON kb_articles
USING GIN (extract_localized_text(title) gin_trgm_ops);
