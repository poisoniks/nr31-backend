CREATE TABLE media_folders (
    id         UUID         PRIMARY KEY,
    name       VARCHAR(255) NOT NULL,
    parent_id  UUID         REFERENCES media_folders(id) ON DELETE RESTRICT,
    created_at TIMESTAMP    NOT NULL,
    CONSTRAINT unique_folder_name_per_parent UNIQUE (name, parent_id)
);

CREATE INDEX idx_media_folders_parent_id ON media_folders(parent_id);

ALTER TABLE files_metadata
    ADD COLUMN folder_id UUID REFERENCES media_folders(id) ON DELETE RESTRICT;

CREATE INDEX idx_files_metadata_folder_id ON files_metadata(folder_id);
