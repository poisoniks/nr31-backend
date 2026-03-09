INSERT INTO app_config (config_key, description, config_value, config_schema)
VALUES (
  'feature_switches',
  '{"en": "Global feature switches for controlling application features"}',
  '[
    {"name": "calendar_feature", "enabled": true}
  ]',
  '{
    "$schema": "http://json-schema.org/draft-07/schema#",
    "type": "array",
    "items": {
      "type": "object",
      "properties": {
        "name": { "type": "string" },
        "enabled": { "type": "boolean" }
      },
      "required": ["name", "enabled"]
    }
  }'
)
ON CONFLICT (config_key) DO NOTHING;
