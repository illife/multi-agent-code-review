-- Migration: Fix project_reports JSONB columns
-- Date: 2026-04-16
-- Description: Change JSONB columns to TEXT to avoid type mismatch errors

-- Change JSONB columns to TEXT for project_reports
ALTER TABLE project_reports
ALTER COLUMN metrics TYPE TEXT USING metrics::TEXT;

ALTER TABLE project_reports
ALTER COLUMN file_statistics TYPE TEXT USING file_statistics::TEXT;
