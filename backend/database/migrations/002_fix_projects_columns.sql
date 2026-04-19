-- Fix projects table column names to match JPA entity
-- Rename columns to match the Project entity field names

DO $$
BEGIN
    -- Rename 'name' to 'project_name'
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'projects'
        AND column_name = 'name'
    ) THEN
        ALTER TABLE projects RENAME COLUMN name TO project_name_name;

        -- Add new project_name column
        ALTER TABLE projects ADD COLUMN project_name VARCHAR(255);

        -- Copy data from old column
        UPDATE projects SET project_name = project_name_name;

        -- Make it NOT NULL
        ALTER TABLE projects ALTER COLUMN project_name SET NOT NULL;

        -- Drop old column
        ALTER TABLE projects DROP COLUMN project_name_name;
    END IF;

    -- Rename 'file_count' to 'total_files'
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'projects'
        AND column_name = 'file_count'
    ) THEN
        ALTER TABLE projects RENAME COLUMN file_count TO total_files;
    END IF;

    RAISE NOTICE 'Projects table columns renamed successfully';
END $$;

-- Update index names
DROP INDEX IF EXISTS idx_projects_user;
CREATE INDEX idx_projects_user_id ON projects(user_id);
CREATE INDEX idx_projects_created_at ON projects(created_at DESC);

-- Add comments
COMMENT ON COLUMN projects.project_name IS 'Name of the uploaded project';
COMMENT ON COLUMN projects.total_files IS 'Total number of files in the project';
