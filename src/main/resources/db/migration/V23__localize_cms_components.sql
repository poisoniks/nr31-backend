ALTER TABLE pages 
    ALTER COLUMN title TYPE JSONB 
    USING jsonb_build_object('en', title);
