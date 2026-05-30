INSERT INTO app_config (config_key, description, config_value, config_schema)
VALUES (
  'fetch_scheduled_discord_events_guild_id',
  '{"en": "Discord Guild id to import events to calendar from"}',
  '{"guildId": ""}',
  '{
     "$schema": "http://json-schema.org/draft-07/schema#",
     "type": "object",
     "properties": {
       "guildId": {
         "type": ["string", "null"]
       }
     },
     "additionalProperties": false
   }'
)
ON CONFLICT (config_key) DO NOTHING;

INSERT INTO permissions (name, description) VALUES
('discord:manage', '{"en": "Can manage discord integration bot"}')
ON CONFLICT (name) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p WHERE r.name = 'SUPER_ADMIN' AND p.name = 'discord:manage'
ON CONFLICT (role_id, permission_id) DO NOTHING;
