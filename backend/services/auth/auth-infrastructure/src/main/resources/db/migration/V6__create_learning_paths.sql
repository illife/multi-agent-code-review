-- ========================================
-- 企业知识库 MVP - V6: 技能提升平台
-- 学习路径 + 练习题 + 教学文档
-- ========================================

-- ========================================
-- 学习路径表（AI生成）
-- ========================================
CREATE TABLE learning_paths (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    title VARCHAR(255) NOT NULL,
    description TEXT,
    target_skill VARCHAR(100),  -- 目标技能：Java高级、架构师、Go入门
    current_level VARCHAR(50),   -- 当前水平：初级、中级、高级
    target_level VARCHAR(50),    -- 目标水平
    duration_weeks INTEGER,      -- 预计周数
    steps JSONB DEFAULT '[]',    -- 学习步骤JSON：[{"week":1,"topic":"...","resources":[...]}]
    metadata JSONB DEFAULT '{}', -- 扩展信息
    status VARCHAR(20) DEFAULT 'IN_PROGRESS'
        CHECK (status IN ('IN_PROGRESS', 'COMPLETED', 'PAUSED')),
    progress INTEGER DEFAULT 0 CHECK (progress >= 0 AND progress <= 100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_learning_paths_user ON learning_paths(user_id, status);
CREATE INDEX idx_learning_paths_skill ON learning_paths(target_skill);

COMMENT ON TABLE learning_paths IS 'AI生成的学习路径';
COMMENT ON COLUMN learning_paths.steps IS '学习步骤JSON数组';

-- ========================================
-- 练习题表（AI生成+批改）
-- ========================================
CREATE TABLE exercises (
    id BIGSERIAL PRIMARY KEY,
    created_by BIGINT REFERENCES users(id),  -- AI或管理员创建
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    difficulty VARCHAR(20) CHECK (difficulty IN ('EASY', 'MEDIUM', 'HARD', 'EXPERT')),
    skill_tag VARCHAR(100),  -- 技能标签：Java并发、Go协程、算法
    language VARCHAR(50),    -- 编程语言
    starter_code TEXT,       -- 初始代码
    solution_code TEXT,      -- 参考答案
    test_cases JSONB DEFAULT '[]',  -- 测试用例
    hints JSONB DEFAULT '[]',       -- 提示
    metadata JSONB DEFAULT '{}',
    is_public BOOLEAN DEFAULT FALSE,
    usage_count INTEGER DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_exercises_skill ON exercises(skill_tag, difficulty);
CREATE INDEX idx_exercises_language ON exercises(language);
CREATE INDEX idx_exercises_public ON exercises(is_public) WHERE is_public = TRUE;

COMMENT ON TABLE exercises IS 'AI生成的编程练习题';
COMMENT ON COLUMN exercises.test_cases IS '测试用例JSON：[{input:"",expected:""}]';

-- ========================================
-- 练习提交表（AI批改）
-- ========================================
CREATE TABLE exercise_submissions (
    id BIGSERIAL PRIMARY KEY,
    exercise_id BIGINT NOT NULL REFERENCES exercises(id),
    user_id BIGINT NOT NULL REFERENCES users(id),
    code TEXT NOT NULL,
    language VARCHAR(50) NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'GRADING', 'PASSED', 'FAILED')),
    score INTEGER CHECK (score >= 0 AND score <= 100),
    feedback JSONB DEFAULT '{}',  -- AI批改反馈
    test_results JSONB DEFAULT '[]',  -- 测试结果
    execution_time_ms INTEGER,
    memory_used_mb INTEGER,
    submitted_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    graded_at TIMESTAMPTZ
);

CREATE INDEX idx_exercise_submissions_user ON exercise_submissions(user_id, submitted_at DESC);
CREATE INDEX idx_exercise_submissions_exercise ON exercise_submissions(exercise_id);
CREATE INDEX idx_exercise_submissions_status ON exercise_submissions(status);

COMMENT ON TABLE exercise_submissions IS '练习提交记录（AI批改）';
COMMENT ON COLUMN exercise_submissions.feedback IS 'AI批改反馈JSON：{suggestions:[...],analysis:"..."}';

-- ========================================
-- 教学文档表（AI生成）
-- ========================================
CREATE TABLE teaching_documents (
    id BIGSERIAL PRIMARY KEY,
    created_by BIGINT REFERENCES users(id),  -- AI生成
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    skill_tag VARCHAR(100),  -- 技能标签
    difficulty VARCHAR(20) CHECK (difficulty IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED')),
    document_type VARCHAR(50) CHECK (document_type IN ('TUTORIAL', 'GUIDE', 'REFERENCE', 'BEST_PRACTICE')),
    tags VARCHAR(500)[],
    code_examples JSONB DEFAULT '[]',  -- 代码示例
    metadata JSONB DEFAULT '{}',
    is_published BOOLEAN DEFAULT FALSE,
    view_count INTEGER DEFAULT 0,
    helpful_count INTEGER DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_teaching_docs_skill ON teaching_documents(skill_tag, difficulty);
CREATE INDEX idx_teaching_docs_type ON teaching_documents(document_type);
CREATE INDEX idx_teaching_docs_tags ON teaching_documents USING GIN(tags);
CREATE INDEX idx_teaching_docs_published ON teaching_documents(is_published) WHERE is_published = TRUE;

COMMENT ON TABLE teaching_documents IS 'AI生成的教学文档';
COMMENT ON COLUMN teaching_documents.code_examples IS '代码示例JSON数组';

-- ========================================
-- 触发器
-- ========================================
CREATE TRIGGER update_learning_paths_updated_at
    BEFORE UPDATE ON learning_paths
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_teaching_documents_updated_at
    BEFORE UPDATE ON teaching_documents
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- ========================================
-- 辅助函数：增加练习题使用次数
-- ========================================
CREATE OR REPLACE FUNCTION increment_exercise_usage(p_exercise_id BIGINT)
RETURNS void AS $$
BEGIN
    UPDATE exercises
    SET usage_count = usage_count + 1
    WHERE id = p_exercise_id;
END;
$$ LANGUAGE plpgsql;

-- ========================================
-- 辅助函数：增加教学文档浏览次数
-- ========================================
CREATE OR REPLACE FUNCTION increment_teaching_doc_views(p_doc_id BIGINT)
RETURNS void AS $$
BEGIN
    UPDATE teaching_documents
    SET view_count = view_count + 1
    WHERE id = p_doc_id;
END;
$$ LANGUAGE plpgsql;
