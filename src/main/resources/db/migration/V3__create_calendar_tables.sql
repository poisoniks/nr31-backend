CREATE TABLE event_types (
    id BIGSERIAL PRIMARY KEY,
    name JSONB NOT NULL,
    custom_icon VARCHAR(255)
);

CREATE TABLE unit_types (
    id BIGSERIAL PRIMARY KEY,
    name JSONB NOT NULL,
    description JSONB
);

CREATE TABLE events (
    id BIGSERIAL PRIMARY KEY,
    series_id VARCHAR(255),
    title JSONB,
    description JSONB,
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    timezone VARCHAR(64),
    type_id BIGINT REFERENCES event_types(id),
    server_name VARCHAR(255),
    rrule VARCHAR(512)
);

CREATE TABLE event_participating_units (
    event_id BIGINT REFERENCES events(id) ON DELETE CASCADE,
    unit_id BIGINT REFERENCES unit_types(id) ON DELETE CASCADE,
    PRIMARY KEY (event_id, unit_id)
);

CREATE TABLE event_exceptions (
    id BIGSERIAL PRIMARY KEY,
    original_event_id BIGINT REFERENCES events(id) ON DELETE CASCADE,
    exception_date TIMESTAMP NOT NULL,
    timezone VARCHAR(64),
    is_cancelled BOOLEAN NOT NULL DEFAULT FALSE,
    new_title JSONB,
    new_description JSONB,
    new_start_time TIMESTAMP,
    new_end_time TIMESTAMP,
    new_type_id BIGINT REFERENCES event_types(id),
    new_server_name VARCHAR(255)
);

CREATE INDEX idx_events_start_time ON events(start_time);
CREATE INDEX idx_events_end_time ON events(end_time);
CREATE INDEX idx_event_exceptions_date ON event_exceptions(exception_date);
CREATE INDEX idx_event_exceptions_original_id ON event_exceptions(original_event_id);
