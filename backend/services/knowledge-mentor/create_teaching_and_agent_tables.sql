-- 教学文档表 (teaching_documents)
-- 用于存储根据测试结果和知识点生成的教学MD文档
CREATE TABLE IF NOT EXISTS teaching_documents (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    user_id VARCHAR(100) NOT NULL,
    document_type VARCHAR(50) NOT NULL,
    knowledge_point_ids TEXT,
    test_result_id BIGINT,
    content TEXT,
    metadata TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    priority INTEGER NOT NULL DEFAULT 2,
    tags TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100)
);

-- 为教学文档表创建索引
CREATE INDEX IF NOT EXISTS idx_teaching_documents_user_id ON teaching_documents(user_id);
CREATE INDEX IF NOT EXISTS idx_teaching_documents_document_type ON teaching_documents(document_type);
CREATE INDEX IF NOT EXISTS idx_teaching_documents_status ON teaching_documents(status);
CREATE INDEX IF NOT EXISTS idx_teaching_documents_test_result_id ON teaching_documents(test_result_id);
CREATE INDEX IF NOT EXISTS idx_teaching_documents_created_at ON teaching_documents(created_at DESC);

-- 为教学文档表添加注释
COMMENT ON TABLE teaching_documents IS '教学文档表：存储根据测试结果和知识点生成的教学MD文档';
COMMENT ON COLUMN teaching_documents.document_type IS '文档类型: LESSON(课时), PRACTICE(练习), REVIEW(复习), KNOWLEDGE_GAP(知识缺口), CUSTOM(自定义)';
COMMENT ON COLUMN teaching_documents.status IS '文档状态: DRAFT(草稿), PUBLISHED(已发布), ARCHIVED(已归档)';
COMMENT ON COLUMN teaching_documents.priority IS '优先级: 1(高), 2(中), 3(低)';

-- 智能体任务表 (agent_tasks)
-- 用于管理后台异步执行的智能体任务
CREATE TABLE IF NOT EXISTS agent_tasks (
    id BIGSERIAL PRIMARY KEY,
    task_name VARCHAR(255) NOT NULL,
    task_type VARCHAR(50) NOT NULL,
    user_id VARCHAR(100) NOT NULL,
    config TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    priority INTEGER NOT NULL DEFAULT 2,
    result TEXT,
    error_message VARCHAR(1000),
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    retry_count INTEGER NOT NULL DEFAULT 0,
    max_retries INTEGER NOT NULL DEFAULT 3,
    next_run_at TIMESTAMP,
    cron_expression VARCHAR(100),
    metadata TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100)
);

-- 为智能体任务表创建索引
CREATE INDEX IF NOT EXISTS idx_agent_tasks_user_id ON agent_tasks(user_id);
CREATE INDEX IF NOT EXISTS idx_agent_tasks_task_type ON agent_tasks(task_type);
CREATE INDEX IF NOT EXISTS idx_agent_tasks_status ON agent_tasks(status);
CREATE INDEX IF NOT EXISTS idx_agent_tasks_priority ON agent_tasks(priority);
CREATE INDEX IF NOT EXISTS idx_agent_tasks_next_run_at ON agent_tasks(next_run_at);
CREATE INDEX IF NOT EXISTS idx_agent_tasks_created_at ON agent_tasks(created_at DESC);

-- 为智能体任务表添加复合索引（用于查询待执行任务）
CREATE INDEX IF NOT EXISTS idx_agent_tasks_status_next_run ON agent_tasks(status, next_run_at, priority);

-- 为智能体任务表添加注释
COMMENT ON TABLE agent_tasks IS '智能体任务表：管理后台异步执行的智能体任务';
COMMENT ON COLUMN agent_tasks.task_type IS '任务类型: KNOWLEDGE_EXTRACTION, CONTENT_GENERATION, DOCUMENT_ANALYSIS, CODE_REVIEW, TEST_GENERATION, LEARNING_PATH, KNOWLEDGE_GAP_ANALYSIS, CUSTOM';
COMMENT ON COLUMN agent_tasks.status IS '任务状态: PENDING(待执行), RUNNING(执行中), COMPLETED(已完成), FAILED(失败), CANCELLED(已取消)';
COMMENT ON COLUMN agent_tasks.priority IS '优先级: 1(高), 2(中), 3(低)';
COMMENT ON COLUMN agent_tasks.next_run_at IS '下次执行时间（定时任务）';
COMMENT ON COLUMN agent_tasks.cron_expression IS 'Cron表达式（定时任务）';
COMMENT ON COLUMN agent_tasks.retry_count IS '重试次数';
COMMENT ON COLUMN agent_tasks.max_retries IS '最大重试次数';

-- 创建更新时间触发器函数（如果不存在）
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 为teaching_documents表添加更新时间触发器
DROP TRIGGER IF EXISTS update_teaching_documents_updated_at ON teaching_documents;
CREATE TRIGGER update_teaching_documents_updated_at
    BEFORE UPDATE ON teaching_documents
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- 为agent_tasks表添加更新时间触发器
DROP TRIGGER IF EXISTS update_agent_tasks_updated_at ON agent_tasks;
CREATE TRIGGER update_agent_tasks_updated_at
    BEFORE UPDATE ON agent_tasks
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
