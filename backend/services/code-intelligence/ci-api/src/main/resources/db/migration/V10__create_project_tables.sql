-- ================================================================
-- Multi-Agent AI Code Review System - Database Migration
-- Version: V10
-- Description: Create tables for project upload and batch analysis
-- ================================================================

-- ================================================================
-- code_projects: Main project table for multi-file code review
-- ================================================================
CREATE TABLE IF NOT EXISTS code_projects (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    project_name VARCHAR(255) NOT NULL,
    description TEXT,
    upload_type VARCHAR(20) NOT NULL CHECK (upload_type IN ('ZIP', 'MULTIFILE', 'GIT')),
    source_url VARCHAR(500),
    total_files INTEGER DEFAULT 0,
    analyzed_files INTEGER DEFAULT 0,
    total_issues INTEGER DEFAULT 0,
    status VARCHAR(20) DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'ANALYZING', 'COMPLETED', 'FAILED')),
    visibility VARCHAR(20) DEFAULT 'PRIVATE' CHECK (visibility IN ('PRIVATE', 'PUBLIC', 'TEAM')),
    file_filter_config JSONB,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_projects_user_id ON code_projects(user_id);
CREATE INDEX IF NOT EXISTS idx_projects_status ON code_projects(status);
CREATE INDEX IF NOT EXISTS idx_projects_visibility ON code_projects(visibility);
CREATE INDEX IF NOT EXISTS idx_projects_created_at ON code_projects(created_at DESC);

-- ================================================================
-- project_files: File mapping table linking projects to code reviews
-- ================================================================
CREATE TABLE IF NOT EXISTS project_files (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL,
    review_id BIGINT,
    file_path VARCHAR(500) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    language VARCHAR(50),
    file_size BIGINT,
    line_count INTEGER,
    is_analyzed BOOLEAN DEFAULT false,
    analysis_priority INTEGER DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (project_id) REFERENCES code_projects(id) ON DELETE CASCADE,
    FOREIGN KEY (review_id) REFERENCES code_reviews(id) ON DELETE SET NULL,
    UNIQUE(project_id, file_path)
);

CREATE INDEX IF NOT EXISTS idx_project_files_project_id ON project_files(project_id);
CREATE INDEX IF NOT EXISTS idx_project_files_review_id ON project_files(review_id);
CREATE INDEX IF NOT EXISTS idx_project_files_analyzed ON project_files(is_analyzed);
CREATE INDEX IF NOT EXISTS idx_project_files_priority ON project_files(analysis_priority DESC);

-- ================================================================
-- project_reports: Project-level analysis reports
-- ================================================================
CREATE TABLE IF NOT EXISTS project_reports (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL,
    summary TEXT,
    overall_score INTEGER CHECK (overall_score >= 0 AND overall_score <= 100),
    risk_level VARCHAR(20) CHECK (risk_level IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    metrics JSONB,
    recommendations TEXT,
    file_statistics JSONB,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (project_id) REFERENCES code_projects(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_project_reports_project_id ON project_reports(project_id);
CREATE INDEX IF NOT EXISTS idx_project_reports_score ON project_reports(overall_score);
CREATE INDEX IF NOT EXISTS idx_project_reports_risk ON project_reports(risk_level);

-- ================================================================
-- Add minio_path column to project_files for storing file location
-- ================================================================
ALTER TABLE project_files
ADD COLUMN IF NOT EXISTS minio_path VARCHAR(500);
