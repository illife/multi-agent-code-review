-- V1: Create base tables for Code Review AI
-- This migration creates the core tables needed for the code review functionality

-- Create roles table
CREATE TABLE IF NOT EXISTS roles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create user_roles junction table
CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
);

-- Create code_reviews table
CREATE TABLE IF NOT EXISTS code_reviews (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    code_content TEXT NOT NULL,
    language VARCHAR(50) NOT NULL,
    file_name VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    total_issues INTEGER,
    visibility VARCHAR(20) DEFAULT 'PRIVATE',
    shared_team_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_code_reviews_user_id ON code_reviews(user_id);
CREATE INDEX IF NOT EXISTS idx_code_reviews_status ON code_reviews(status);
CREATE INDEX IF NOT EXISTS idx_code_reviews_created_at ON code_reviews(created_at DESC);

-- Insert default roles
INSERT INTO roles (name, description) VALUES
    ('USER', 'Standard user role'),
    ('ADMIN', 'Administrator role with full permissions'),
    ('TEACHER', 'Teacher role with teaching permissions')
ON CONFLICT (name) DO NOTHING;

-- Create a sequence for code review IDs if needed
CREATE SEQUENCE IF NOT EXISTS code_review_seq;
