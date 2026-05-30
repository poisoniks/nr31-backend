CREATE TABLE app_config (
    config_key VARCHAR(100) PRIMARY KEY,
    description JSONB,
    config_value JSONB NOT NULL,
    config_schema JSONB
);
