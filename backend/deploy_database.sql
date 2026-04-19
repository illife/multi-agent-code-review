-- ============================================================================
-- Code Review AI + Knowledge Base Database Schema
-- PostgreSQL 15.7
-- 生成时间: 2026-04-12
-- ============================================================================

-- 设置编码
SET client_encoding = 'UTF8';

-- ============================================================================
-- 1. 用户和权限表
-- ============================================================================

-- 用户表
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100),
    department VARCHAR(100),
    is_active BOOLEAN NOT NULL DEFAULT true,
    last_login_at TIMESTAMP(6),
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 角色表
CREATE TABLE IF NOT EXISTS roles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description TEXT
);

-- 用户角色关联表
CREATE TABLE IF NOT EXISTS user_roles (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
    UNIQUE(user_id, role_id)
);

-- ============================================================================
-- 2. 文档管理表 (Knowledge Base)
-- ============================================================================

-- 文档表
CREATE TABLE IF NOT EXISTS documents (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(500) NOT NULL,
    file_name VARCHAR(255),
    file_path VARCHAR(500),
    file_size BIGINT,
    mime_type VARCHAR(100),
    uploaded_by BIGINT NOT NULL,
    status VARCHAR(50) DEFAULT 'PROCESSING',
    summary TEXT,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (uploaded_by) REFERENCES users(id)
);

-- 文档分块表
CREATE TABLE IF NOT EXISTS document_chunks (
    id BIGSERIAL PRIMARY KEY,
    document_id BIGINT NOT NULL,
    chunk_index INTEGER NOT NULL,
    content TEXT NOT NULL,
    metadata JSONB,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE
);

-- 文档权限表
CREATE TABLE IF NOT EXISTS document_permissions (
    id BIGSERIAL PRIMARY KEY,
    document_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    granted_by BIGINT,
    permission_type VARCHAR(50) NOT NULL, -- READ, WRITE, ADMIN
    granted_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP(6),
    FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (granted_by) REFERENCES users(id)
);

-- ============================================================================
-- 3. 代码审查表 (Code Review)
-- ============================================================================

-- 代码审查记录表
CREATE TABLE IF NOT EXISTS code_reviews (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    code_content TEXT NOT NULL,
    file_name VARCHAR(255),
    language VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, PROCESSING, COMPLETED, FAILED
    total_issues INTEGER DEFAULT 0,
    visibility VARCHAR(20) DEFAULT 'PRIVATE', -- PRIVATE, PUBLIC, SHARED_TEAM
    shared_team_id BIGINT,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- 代码问题表
CREATE TABLE IF NOT EXISTS code_issues (
    id BIGSERIAL PRIMARY KEY,
    review_id BIGINT NOT NULL,
    severity VARCHAR(20) NOT NULL, -- CRITICAL, HIGH, MEDIUM, LOW, INFO
    category VARCHAR(50), -- security, performance, best-practice, teaching
    title VARCHAR(255) NOT NULL,
    description TEXT,
    code_snippet TEXT,
    line_number INTEGER,
    suggestion TEXT,
    agent_type VARCHAR(50), -- STATIC_ANALYSIS, SECURITY, PERFORMANCE, SMART_ANALYSIS, etc.
    tool_name VARCHAR(100),
    rule_id VARCHAR(100),
    is_resolved BOOLEAN DEFAULT false,
    teaching_explanation TEXT,
    related_lesson_id BIGINT,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (review_id) REFERENCES code_reviews(id) ON DELETE CASCADE
);

-- ============================================================================
-- 4. 教学系统表
-- ============================================================================

-- 成就表
CREATE TABLE IF NOT EXISTS achievements (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    icon_url VARCHAR(500),
    category VARCHAR(50),
    requirements JSONB,
    xp_reward INTEGER DEFAULT 0,
    badge_color VARCHAR(20) DEFAULT '#3B82F6',
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 用户技能画像表
CREATE TABLE IF NOT EXISTS skill_profiles (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    overall_score INTEGER DEFAULT 0,
    java_skill INTEGER DEFAULT 0,
    python_skill INTEGER DEFAULT 0,
    javascript_skill INTEGER DEFAULT 0,
    typescript_skill INTEGER DEFAULT 0,
    go_skill INTEGER DEFAULT 0,
    security_skill INTEGER DEFAULT 0,
    performance_skill INTEGER DEFAULT 0,
    best_practices_skill INTEGER DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 用户进度表
CREATE TABLE IF NOT EXISTS user_progress (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    total_xp INTEGER DEFAULT 0,
    completed_reviews INTEGER DEFAULT 0,
    completed_exercises INTEGER DEFAULT 0,
    current_level INTEGER DEFAULT 1,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 用户成就关联表
CREATE TABLE IF NOT EXISTS user_achievements (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    achievement_id BIGINT NOT NULL,
    unlocked_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (achievement_id) REFERENCES achievements(id) ON DELETE CASCADE,
    UNIQUE(user_id, achievement_id)
);

-- 学习内容表
CREATE TABLE IF NOT EXISTS learning_content (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(500) NOT NULL,
    content TEXT NOT NULL,
    content_type VARCHAR(50) NOT NULL, -- LESSON, EXERCISE, BEST_PRACTICE
    category VARCHAR(50),
    difficulty_level VARCHAR(20), -- BEGINNER, INTERMEDIATE, ADVANCED
    language VARCHAR(50),
    tags JSONB,
    is_published BOOLEAN DEFAULT false,
    order_index INTEGER DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 练习题表
CREATE TABLE IF NOT EXISTS exercises (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(500) NOT NULL,
    description TEXT,
    problem_code TEXT NOT NULL,
    solution_code TEXT,
    language VARCHAR(50) NOT NULL,
    difficulty VARCHAR(20) DEFAULT 'BEGINNER',
    category VARCHAR(50),
    related_lesson_id BIGINT,
    xp_reward INTEGER DEFAULT 10,
    time_limit_seconds INTEGER DEFAULT 30,
    max_code_length INTEGER DEFAULT 10000,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (related_lesson_id) REFERENCES learning_content(id)
);

-- 练习尝试记录表
CREATE TABLE IF NOT EXISTS exercise_attempts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    exercise_id BIGINT NOT NULL,
    submitted_code TEXT,
    is_passed BOOLEAN NOT NULL DEFAULT false,
    attempts_count INTEGER DEFAULT 1,
    time_spent_seconds INTEGER,
    xp_earned INTEGER DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (exercise_id) REFERENCES exercises(id) ON DELETE CASCADE
);

-- ============================================================================
-- 5. 其他功能表
-- ============================================================================

-- QA 对话历史表
CREATE TABLE IF NOT EXISTS qa_history (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    question TEXT NOT NULL,
    answer TEXT,
    sources JSONB,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- 搜索历史表
CREATE TABLE IF NOT EXISTS search_history (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    query TEXT NOT NULL,
    results_count INTEGER,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- 上传会话表
CREATE TABLE IF NOT EXISTS upload_sessions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    file_name VARCHAR(255),
    total_size BIGINT,
    chunk_size BIGINT,
    total_chunks INTEGER,
    uploaded_chunks INTEGER DEFAULT 0,
    status VARCHAR(50) DEFAULT 'IN_PROGRESS',
    minio_path VARCHAR(500),
    expires_at TIMESTAMP(6),
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- 密码重置令牌表
CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP(6) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- ============================================================================
-- 6. 索引
-- ============================================================================

-- users 表索引
CREATE INDEX IF NOT EXISTS idx_users_last_login ON users(last_login_at);
CREATE INDEX IF NOT EXISTS idx_users_department ON users(department);
CREATE INDEX IF NOT EXISTS idx_users_is_active ON users(is_active);

-- code_reviews 表索引
CREATE INDEX IF NOT EXISTS idx_code_reviews_user_id ON code_reviews(user_id);
CREATE INDEX IF NOT EXISTS idx_code_reviews_status ON code_reviews(status);
CREATE INDEX IF NOT EXISTS idx_code_reviews_language ON code_reviews(language);
CREATE INDEX IF NOT EXISTS idx_code_reviews_created_at ON code_reviews(created_at);
CREATE INDEX IF NOT EXISTS idx_code_reviews_visibility ON code_reviews(visibility);

-- code_issues 表索引
CREATE INDEX IF NOT EXISTS idx_code_issues_review_id ON code_issues(review_id);
CREATE INDEX IF NOT EXISTS idx_code_issues_severity ON code_issues(severity);
CREATE INDEX IF NOT EXISTS idx_code_issues_category ON code_issues(category);
CREATE INDEX IF NOT EXISTS idx_code_issues_agent_type ON code_issues(agent_type);
CREATE INDEX IF NOT EXISTS idx_code_issues_is_resolved ON code_issues(is_resolved);
CREATE INDEX IF NOT EXISTS idx_code_issues_related_lesson ON code_issues(related_lesson_id);

-- learning_content 表索引
CREATE INDEX IF NOT EXISTS idx_learning_content_type ON learning_content(content_type);
CREATE INDEX IF NOT EXISTS idx_learning_content_category ON learning_content(category);
CREATE INDEX IF NOT EXISTS idx_learning_content_difficulty ON learning_content(difficulty_level);
CREATE INDEX IF NOT EXISTS idx_learning_content_language ON learning_content(language);
CREATE INDEX IF NOT EXISTS idx_learning_content_published ON learning_content(is_published);

-- skill_profiles 表索引
CREATE INDEX IF NOT EXISTS idx_skill_profiles_overall ON skill_profiles(overall_score);

-- user_progress 表索引
CREATE INDEX IF NOT EXISTS idx_user_progress_xp ON user_progress(total_xp);

-- ============================================================================
-- 7. 初始数据
-- ============================================================================

-- 插入默认角色
INSERT INTO roles (id, name, description) VALUES
(1, 'USER', '普通用户'),
(2, 'ADMIN', '管理员'),
(3, 'TEACHER', '教师')
ON CONFLICT (name) DO NOTHING;

-- 插入默认管理员用户 (密码: admin123)
INSERT INTO users (id, username, email, password_hash, full_name, is_active)
VALUES (1, 'admin', 'admin@codereview.ai', '$2a$10$N.zmdr9k7uOCQb376NoUnuHJ8QVPW9NNgPL8XqJ5QL5bF9dG5qW6xO', '系统管理员', true)
ON CONFLICT (username) DO NOTHING;

-- 分配管理员角色
INSERT INTO user_roles (user_id, role_id)
VALUES (1, 2)
ON CONFLICT DO NOTHING;

-- ============================================================================
-- 8. 完整性约束
-- ============================================================================

-- code_reviews 检查约束
ALTER TABLE code_reviews DROP CONSTRAINT IF EXISTS code_reviews_status_check;
ALTER TABLE code_reviews ADD CONSTRAINT code_reviews_status_check
    CHECK (status::text = ANY (ARRAY['PENDING'::text, 'PROCESSING'::text, 'COMPLETED'::text, 'FAILED'::text]));

-- code_issues 检查约束
ALTER TABLE code_issues DROP CONSTRAINT IF EXISTS code_issues_severity_check;
ALTER TABLE code_issues ADD CONSTRAINT code_issues_severity_check
    CHECK (severity::text = ANY (ARRAY['CRITICAL'::text, 'HIGH'::text, 'MEDIUM'::text, 'LOW'::text, 'INFO'::text]));

-- ============================================================================
-- 9. Flyway 迁移历史表（如果不存在）
-- ============================================================================

CREATE TABLE IF NOT EXISTS flyway_schema_history_codereview (
    installed_rank INTEGER NOT NULL,
    version VARCHAR(50) NOT NULL,
    description VARCHAR(200) NOT NULL,
    type VARCHAR(20) NOT NULL,
    script VARCHAR(1000) NOT NULL,
    checksum INTEGER,
    installed_by VARCHAR(100) NOT NULL,
    installed_on TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    execution_time INTEGER NOT NULL,
    success BOOLEAN NOT NULL,
    PRIMARY KEY (installed_rank)
);
