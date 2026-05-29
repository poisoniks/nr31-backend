UPDATE app_config 
SET config_value = config_value || '[{"name": "hibernate_metrics", "enabled": false}]'::jsonb 
WHERE config_key = 'feature_switches';
