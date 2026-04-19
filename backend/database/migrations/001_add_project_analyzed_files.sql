-- Fix projects table structure
-- Add analyzed_files column if it doesn't exist

DO $$
BEGIN
    -- Add analyzed_files column
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'projects'
        AND column_name = 'analyzed_files'
    ) THEN
        ALTER TABLE projects ADD COLUMN analyzed_files INTEGER NOT NULL DEFAULT 0;
    END IF;

    -- Add file_filter_config column if it doesn't exist
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'projects'
        AND column_name = 'file_filter_config'
    ) THEN
        ALTER TABLE projects ADD COLUMN file_filter_config JSONB;
    END IF;

    -- Add source_url column if it doesn't exist
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'projects'
        AND column_name = 'source_url'
    ) THEN
        ALTER TABLE projects ADD COLUMN source_url VARCHAR(500);
    END IF;

    -- Add storage_path column if it doesn't exist
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'projects'
        AND column_name = 'storage_path'
    ) THEN
        ALTER TABLE projects ADD COLUMN storage_path VARCHAR(500);
    END IF;

    -- Add total_size column if it doesn't exist
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'projects'
        AND column_name = 'total_size'
    ) THEN
        ALTER TABLE projects ADD COLUMN total_size BIGINT;
    END IF;

    -- Add total_issues column if it doesn't exist
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'projects'
        AND column_name = 'total_issues'
    ) THEN
        ALTER TABLE projects ADD COLUMN total_issues INTEGER NOT NULL DEFAULT 0;
    END IF;

    -- Add language column if it doesn't exist
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'projects'
        AND column_name = 'language'
    ) THEN
        ALTER TABLE projects ADD COLUMN language VARCHAR(50);
    END IF;

    -- Add total_files column if it doesn't exist
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'projects'
        AND column_name = 'total_files'
    ) THEN
        ALTER TABLE projects ADD COLUMN total_files INTEGER NOT NULL DEFAULT 0;
    END IF;

    -- Add upload_type column if it doesn't exist
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'projects'
        AND column_name = 'upload_type'
    ) THEN
        ALTER TABLE projects ADD COLUMN upload_type VARCHAR(20) NOT NULL DEFAULT 'ZIP';
    END IF;

    -- Add visibility column if it doesn't exist
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'projects'
        AND column_name = 'visibility'
    ) THEN
        ALTER TABLE projects ADD COLUMN visibility VARCHAR(20) NOT NULL DEFAULT 'PRIVATE';
    END IF;

    -- Add updated_at column if it doesn't exist
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'projects'
        AND column_name = 'updated_at'
    ) THEN
        ALTER TABLE projects ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
    END IF;

    RAISE NOTICE 'Projects table structure updated successfully';
END $$;
