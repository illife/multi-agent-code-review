-- ============================================
-- Code Review AI Database Tables
-- ============================================

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

-- Create code_reviews table (for single code review submissions)
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

-- Create code_issues table (for storing code review issues)
CREATE TABLE IF NOT EXISTS code_issues (
    id BIGSERIAL PRIMARY KEY,
    review_id BIGINT NOT NULL,
    severity VARCHAR(20) NOT NULL,
    category VARCHAR(50),
    title VARCHAR(255),
    description TEXT,
    code_snippet TEXT,
    line_number INTEGER,
    suggestion TEXT,
    agent_type VARCHAR(50),
    tool_name VARCHAR(100),
    rule_id VARCHAR(100),
    teaching_explanation TEXT,
    related_lesson_id BIGINT,
    is_resolved BOOLEAN DEFAULT FALSE,
    metadata TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (review_id) REFERENCES code_reviews(id) ON DELETE CASCADE
);

-- Create indexes for code_reviews
CREATE INDEX IF NOT EXISTS idx_code_reviews_user_id ON code_reviews(user_id);
CREATE INDEX IF NOT EXISTS idx_code_reviews_status ON code_reviews(status);
CREATE INDEX IF NOT EXISTS idx_code_reviews_created_at ON code_reviews(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_code_reviews_visibility ON code_reviews(visibility);

-- Create indexes for code_issues
CREATE INDEX IF NOT EXISTS idx_code_issues_review_id ON code_issues(review_id);
CREATE INDEX IF NOT EXISTS idx_code_issues_severity ON code_issues(severity);
CREATE INDEX IF NOT EXISTS idx_code_issues_category ON code_issues(category);
CREATE INDEX IF NOT EXISTS idx_code_issues_resolved ON code_issues(is_resolved);

-- Insert default roles
INSERT INTO roles (name, description) VALUES
    ('USER', 'Standard user role'),
    ('ADMIN', 'Administrator role with full permissions'),
    ('TEACHER', 'Teacher role with teaching permissions')
ON CONFLICT (name) DO NOTHING;

-- Verify tables created
SELECT 'Tables created successfully' as status, COUNT(*) as table_count
FROM pg_tables
WHERE schemaname='public'
AND tablename IN ('code_reviews', 'code_issues', 'roles', 'user_roles');
