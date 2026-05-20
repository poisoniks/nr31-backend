-- Trigram index on body plain text for fuzzy + ILIKE substring matching
CREATE INDEX idx_kb_articles_trgm_content ON kb_articles
    USING GIN (extract_localized_text(plain_text_content) gin_trgm_ops);

-- Add kb_search_precision config (default 'full' = level 3)
INSERT INTO app_config (config_key, description, config_value, config_schema)
VALUES (
  'kb_search_precision',
  '{"en": "Controls KB search depth: basic (FTS only), standard (FTS + prefix + fuzzy), full (all + substring)"}',
  '"full"',
  '{
    "$schema": "http://json-schema.org/draft-07/schema#",
    "type": "string",
    "enum": ["basic", "standard", "full"]
  }'
)
ON CONFLICT (config_key) DO NOTHING;
