-- Create projects table for multi-file code review
-- Drop table if exists (for clean setup)
-- DROP TABLE IF EXISTS projects CASCADE;

CREATE TABLE IF NOT EXISTS projects (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    project_name VARCHAR(255) NOT NULL,
    description TEXT,
    upload_type VARCHAR(20) NOT NULL DEFAULT 'ZIP',
    source_url VARCHAR(500),
    storage_path VARCHAR(500),
    total_files INTEGER NOT NULL DEFAULT 0,
    total_size BIGINT,
    language VARCHAR(50),
    analyzed_files INTEGER NOT NULL DEFAULT 0,
    total_issues INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    visibility VARCHAR(20) NOT NULL DEFAULT 'PRIVATE',
    file_filter_config JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_projects_user_id ON projects(user_id);
CREATE INDEX IF NOT EXISTS idx_projects_status ON projects(status);
CREATE INDEX IF NOT EXISTS idx_projects_created_at ON projects(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_projects_visibility ON projects(visibility);

-- Add foreign key constraint to users table
-- ALTER TABLE projects
--     ADD CONSTRAINT fk_projects_user_id
--     FOREIGN KEY (user_id) REFERENCES users(id)
--     ON DELETE CASCADE;

-- Add comments
COMMENT ON TABLE projects IS 'Stores uploaded projects for multi-file code review';
COMMENT ON COLUMN projects.upload_type IS 'Type of upload: ZIP, MULTIFILE, or GIT';
COMMENT ON COLUMN projects.status IS 'Analysis status: PENDING, ANALYZING, COMPLETED, or FAILED';
COMMENT ON COLUMN projects.visibility IS 'Who can access: PRIVATE, PUBLIC, or TEAM';
COMMENT ON COLUMN projects.analyzed_files IS 'Number of files that have been analyzed';
COMMENT ON COLUMN projects.total_issues IS 'Total number of issues found across all files';

-- Create trigger to auto-update updated_at
CREATE OR REPLACE FUNCTION update_projects_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trigger_projects_updated_at ON projects;
CREATE TRIGGER trigger_projects_updated_at
    BEFORE UPDATE ON projects
    FOR EACH ROW
    EXECUTE FUNCTION update_projects_updated_at();
