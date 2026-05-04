INSERT INTO app_config (config_key, description, config_value, config_schema)
VALUES (
  'cms.richtext.max_size_bytes',
  '{"en": "Maximum size in bytes for RichTextWidget content per locale"}',
  '1048576',
  '{
    "$schema": "http://json-schema.org/draft-07/schema#",
    "type": "integer",
    "minimum": 1,
    "maximum": 10485760,
    "description": "Maximum size in bytes (default: 1MB = 1048576, max: 10MB)"
  }'
)
ON CONFLICT (config_key) DO NOTHING;

INSERT INTO app_config (config_key, description, config_value, config_schema)
VALUES (
  'cms.newsfeed.max_items',
  '{"en": "Maximum number of items allowed in NewsFeedWidget"}',
  '50',
  '{
    "$schema": "http://json-schema.org/draft-07/schema#",
    "type": "integer",
    "minimum": 1,
    "maximum": 100,
    "description": "Maximum number of news items (default: 50, max: 100)"
  }'
)
ON CONFLICT (config_key) DO NOTHING;

UPDATE app_config
SET config_value = '{
    "hero": ["hero"],
    "sidebar": ["nextevent", "newsfeed", "richtext"],
    "content": ["richtext", "hero", "nextevent", "newsfeed"],
    "footer": ["richtext"]
}'
WHERE config_key = 'cms_slot_restrictions';
