-- 创建代码审查记录表
CREATE TABLE IF NOT EXISTS code_reviews (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    code_content TEXT NOT NULL,
    language VARCHAR(50) NOT NULL,
    file_name VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    total_issues INT DEFAULT 0,
    visibility VARCHAR(20) DEFAULT 'PRIVATE',
    shared_team_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_review_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_review_status CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED')),
    CONSTRAINT chk_review_visibility CHECK (visibility IN ('PRIVATE', 'TEAM', 'PUBLIC'))
);

-- 创建代码问题表
CREATE TABLE IF NOT EXISTS code_issues (
    id BIGSERIAL PRIMARY KEY,
    review_id BIGINT NOT NULL,
    severity VARCHAR(20) NOT NULL,
    category VARCHAR(50),
    title VARCHAR(255),
    description TEXT,
    code_snippet TEXT,
    line_number INT,
    suggestion TEXT,
    agent_type VARCHAR(50),
    tool_name VARCHAR(100),
    rule_id VARCHAR(100),
    is_resolved BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_issue_review FOREIGN KEY (review_id)
        REFERENCES code_reviews(id) ON DELETE CASCADE,
    CONSTRAINT chk_issue_severity CHECK (severity IN ('CRITICAL', 'HIGH', 'MEDIUM', 'LOW', 'INFO'))
);

-- 创建索引优化查询性能
CREATE INDEX IF NOT EXISTS idx_review_user_id ON code_reviews(user_id);
CREATE INDEX IF NOT EXISTS idx_review_status ON code_reviews(status);
CREATE INDEX IF NOT EXISTS idx_review_language ON code_reviews(language);
CREATE INDEX IF NOT EXISTS idx_review_visibility ON code_reviews(visibility);
CREATE INDEX IF NOT EXISTS idx_review_created_at ON code_reviews(created_at DESC);

CREATE INDEX IF NOT EXISTS idx_issue_review_id ON code_issues(review_id);
CREATE INDEX IF NOT EXISTS idx_issue_severity ON code_issues(severity);
CREATE INDEX IF NOT EXISTS idx_issue_agent_type ON code_issues(agent_type);
CREATE INDEX IF NOT EXISTS idx_issue_tool_rule ON code_issues(tool_name, rule_id);
CREATE INDEX IF NOT EXISTS idx_issue_is_resolved ON code_issues(is_resolved);

-- 添加表注释
COMMENT ON TABLE code_reviews IS '代码审查记录表，存储代码审查请求和结果';
COMMENT ON COLUMN code_reviews.user_id IS '提交审查的用户ID';
COMMENT ON COLUMN code_reviews.code_content IS '待审查的代码内容';
COMMENT ON COLUMN code_reviews.language IS '编程语言：javascript, java, python, go';
COMMENT ON COLUMN code_reviews.status IS '审查状态：PENDING(待处理), PROCESSING(处理中), COMPLETED(已完成), FAILED(失败)';
COMMENT ON COLUMN code_reviews.visibility IS '可见性：PRIVATE(仅自己), TEAM(团队), PUBLIC(公开)';

COMMENT ON TABLE code_issues IS '代码问题表，存储审查发现的问题';
COMMENT ON COLUMN code_issues.severity IS '严重程度：CRITICAL(严重), HIGH(高), MEDIUM(中), LOW(低), INFO(信息)';
COMMENT ON COLUMN code_issues.category IS '问题类别：bug, security, performance, style, best-practice';
COMMENT ON COLUMN code_issues.agent_type IS 'Agent类型：STATIC_ANALYSIS, SECURITY, PERFORMANCE, BEST_PRACTICE, TEACHING';
COMMENT ON COLUMN code_issues.tool_name IS '检测工具名称：ESLint, Pylint, QwenAI等';
