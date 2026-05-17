INSERT INTO app_config (config_key, description, config_value, config_schema)
VALUES (
  'allowed_mime_types',
  '{"en": "Allowed MIME types for uploaded files"}',
  '["image/png", "image/jpeg", "image/webp", "application/pdf", "application/zip", "text/plain"]',
  '{
    "$schema": "http://json-schema.org/draft-07/schema#",
    "type": "array",
    "items": {
      "type": "string"
    },
    "uniqueItems": true,
    "description": "Allowed MIME types list"
  }'
)
ON CONFLICT (config_key) DO NOTHING;
