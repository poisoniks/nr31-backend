CREATE TABLE event_exception_participating_units (
    event_exception_id BIGINT REFERENCES event_exceptions(id) ON DELETE CASCADE,
    unit_id BIGINT REFERENCES unit_types(id) ON DELETE CASCADE,
    PRIMARY KEY (event_exception_id, unit_id)
);
