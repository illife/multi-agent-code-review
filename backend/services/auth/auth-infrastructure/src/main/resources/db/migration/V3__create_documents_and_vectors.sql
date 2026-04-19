-- ========================================
-- 企业知识库 MVP - V3: 文档和向量
-- 文档管理 + Elasticsearch向量存储
-- ========================================

-- ========================================
-- 文档表（知识库核心）
-- ========================================
CREATE TABLE documents (
    id BIGSERIAL PRIMARY KEY,
    file_upload_id BIGINT REFERENCES file_upload(id),
    title VARCHAR(500) NOT NULL,
    description TEXT,
    file_name VARCHAR(255) NOT NULL,
    file_type VARCHAR(50) NOT NULL,
    file_size BIGINT,
    file_md5 VARCHAR(32) NOT NULL,
    uploaded_by BIGINT NOT NULL REFERENCES users(id),
    org_tag VARCHAR(50) REFERENCES organization_tags(tag_id),
    is_public BOOLEAN DEFAULT FALSE,
    status VARCHAR(20) DEFAULT 'PROCESSING'
        CHECK (status IN ('PROCESSING', 'INDEXED', 'FAILED')),
    tags VARCHAR(500)[],  -- PostgreSQL 数组类型
    metadata JSONB DEFAULT '{}',
    access_count INTEGER DEFAULT 0,
    last_accessed_at TIMESTAMPTZ,
    indexed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 文档表索引
CREATE INDEX idx_documents_file_md5 ON documents(file_md5);
CREATE INDEX idx_documents_uploaded_by ON documents(uploaded_by);
CREATE INDEX idx_documents_org_tag ON documents(org_tag) WHERE org_tag IS NOT NULL;
CREATE INDEX idx_documents_status ON documents(status);
CREATE INDEX idx_documents_public ON documents(is_public) WHERE is_public = TRUE;

-- GIN索引用于数组和JSONB
CREATE INDEX idx_documents_tags ON documents USING GIN(tags);
CREATE INDEX idx_documents_metadata ON documents USING GIN(metadata);

-- 全文搜索索引
CREATE INDEX idx_documents_fulltext ON documents USING GIN(
    to_tsvector('simple',
        COALESCE(title, '') || ' ' ||
        COALESCE(description, '')
    )
);

COMMENT ON TABLE documents IS '文档表 - 知识库核心';
COMMENT ON COLUMN documents.file_md5 IS '关联文件上传的MD5';
COMMENT ON COLUMN documents.org_tag IS '组织标签，用于数据隔离';
COMMENT ON COLUMN documents.tags IS '文档标签数组';
COMMENT ON COLUMN documents.status IS '处理状态: PROCESSING, INDEXED, FAILED';

-- ========================================
-- 文档分块表（RAG检索）
-- ========================================
CREATE TABLE document_chunks (
    id BIGSERIAL PRIMARY KEY,
    document_id BIGINT NOT NULL REFERENCES documents(id),
    file_md5 VARCHAR(32) NOT NULL,
    chunk_index INT NOT NULL,
    text_content TEXT NOT NULL,
    char_count INTEGER,
    token_count INTEGER,
    embedding_model VARCHAR(50),  -- 如: text-embedding-v3
    es_doc_id VARCHAR(100),  -- Elasticsearch中的文档ID
    metadata JSONB DEFAULT '{}',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_chunks_document ON document_chunks(document_id);
CREATE INDEX idx_chunks_file_md5 ON document_chunks(file_md5);
CREATE INDEX idx_chunks_index ON document_chunks(file_md5, chunk_index);

COMMENT ON TABLE document_chunks IS '文档分块表 - 向量存ES，这里存元数据';
COMMENT ON COLUMN document_chunks.es_doc_id IS 'Elasticsearch中的向量文档ID';
COMMENT ON COLUMN document_chunks.chunk_index IS '分块序号，用于RAG检索';

-- ========================================
-- 问答历史表
-- ========================================
CREATE TABLE qa_history (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    session_id VARCHAR(100),
    question TEXT NOT NULL,
    answer TEXT NOT NULL,
    search_type VARCHAR(20) CHECK (search_type IN ('BM25', 'KNN', 'HYBRID')),
    sources JSONB,  -- 引用的文档块
    model_used VARCHAR(50),
    tokens_consumed INTEGER,
    response_time_ms INTEGER,
    feedback INTEGER CHECK (feedback >= 1 AND feedback <= 5),
    is_bookmarked BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_qa_user ON qa_history(user_id, created_at DESC);
CREATE INDEX idx_qa_session ON qa_history(session_id) WHERE session_id IS NOT NULL;
CREATE INDEX idx_qa_feedback ON qa_history(feedback) WHERE feedback IS NOT NULL;
CREATE INDEX idx_qa_bookmarked ON qa_history(is_bookmarked) WHERE is_bookmarked = TRUE;

-- 全文搜索
CREATE INDEX idx_qa_question_fulltext ON qa_history USING GIN(
    to_tsvector('simple', question)
);

COMMENT ON TABLE qa_history IS '问答历史表';
COMMENT ON COLUMN qa_history.search_type IS '搜索类型: BM25关键词, KNN向量, HYBRID混合';
COMMENT ON COLUMN qa_history.sources IS '引用的文档块JSON';

-- ========================================
-- 代码审查表
-- ========================================
CREATE TABLE code_reviews (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    project_name VARCHAR(100),
    code_content TEXT NOT NULL,
    language VARCHAR(50) NOT NULL,
    file_name VARCHAR(255),
    issues JSONB DEFAULT '[]',  -- 问题列表，不单独建表
    analysis_summary TEXT,
    overall_score INTEGER CHECK (overall_score >= 0 AND overall_score <= 100),
    metrics JSONB DEFAULT '{}',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_code_reviews_user ON code_reviews(user_id, created_at DESC);
CREATE INDEX idx_code_reviews_language ON code_reviews(language);
CREATE INDEX idx_code_reviews_score ON code_reviews(overall_score);

COMMENT ON TABLE code_reviews IS '代码审查表 - issues存JSONB';
COMMENT ON COLUMN code_reviews.issues IS '问题列表JSON: [{"severity":"HIGH","message":"..."}]';

-- ========================================
-- 触发器
-- ========================================
CREATE TRIGGER update_documents_updated_at
    BEFORE UPDATE ON documents
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
