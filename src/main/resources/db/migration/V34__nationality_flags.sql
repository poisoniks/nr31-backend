CREATE TABLE nationality_flags (
    id          BIGSERIAL PRIMARY KEY,
    flag_file_id UUID NOT NULL REFERENCES files_metadata(id),
    country_code VARCHAR(3),
    UNIQUE(flag_file_id)
);

-- Replace nationality VARCHAR with FK reference
ALTER TABLE roster_members
    ADD COLUMN nationality_flag_id BIGINT REFERENCES nationality_flags(id);

ALTER TABLE roster_members
    DROP COLUMN nationality;
