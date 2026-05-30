CREATE TABLE supported_locales (
    id SERIAL PRIMARY KEY,
    code VARCHAR(10) UNIQUE NOT NULL,
    description VARCHAR(255)
);

INSERT INTO supported_locales (code, description) VALUES 
('en', 'English'),
('uk', 'Ukrainian');
