-- Migration: Create project_reports table and fix project_files table
-- Date: 2026-04-16
-- Description: Creates the project_reports table and adds missing columns to project_files

-- Create project_reports table
CREATE TABLE IF NOT EXISTS project_reports (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL,
    summary TEXT,
    overall_score INTEGER,
    risk_level VARCHAR(20),
    metrics JSONB,
    recommendations TEXT,
    file_statistics JSONB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_project_reports_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
);

-- Create index for faster lookups
CREATE INDEX IF NOT EXISTS idx_project_reports_project_id ON project_reports(project_id);

-- Fix project_files table - add missing columns
ALTER TABLE project_files
ADD COLUMN IF NOT EXISTS line_count INTEGER;

ALTER TABLE project_files
ADD COLUMN IF NOT EXISTS is_analyzed BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE project_files
ADD COLUMN IF NOT EXISTS minio_path VARCHAR(500);

ALTER TABLE project_files
ADD COLUMN IF NOT EXISTS review_id BIGINT;

ALTER TABLE project_files
ADD COLUMN IF NOT EXISTS analysis_priority INTEGER NOT NULL DEFAULT 0;

-- Make storage_path nullable in project_files
ALTER TABLE project_files
ALTER COLUMN storage_path DROP NOT NULL;
