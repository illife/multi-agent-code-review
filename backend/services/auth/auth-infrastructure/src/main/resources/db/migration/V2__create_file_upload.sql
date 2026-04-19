-- ========================================
-- 企业知识库 MVP - V2: 文件上传和分片
-- MD5去重 + 分片上传支持
-- ========================================

-- ========================================
-- 文件上传记录表
-- ========================================
CREATE TABLE file_upload (
    id BIGSERIAL PRIMARY KEY,
    file_md5 VARCHAR(32) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_size BIGINT NOT NULL,
    file_type VARCHAR(50),
    status VARCHAR(20) NOT NULL DEFAULT 'UPLOADING'
        CHECK (status IN ('UPLOADING', 'MERGED', 'FAILED', 'PROCESSING', 'COMPLETED')),
    user_id BIGINT NOT NULL REFERENCES users(id),
    is_public BOOLEAN DEFAULT FALSE,
    storage_path VARCHAR(500),
    metadata JSONB DEFAULT '{}',
    merged_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(file_md5, user_id)
);

CREATE INDEX idx_file_upload_md5 ON file_upload(file_md5);
CREATE INDEX idx_file_upload_user ON file_upload(user_id);
CREATE INDEX idx_file_upload_status ON file_upload(status);

COMMENT ON TABLE file_upload IS '文件上传记录表 - MD5去重';
COMMENT ON COLUMN file_upload.file_md5 IS '文件MD5，用于秒传';
COMMENT ON COLUMN file_upload.status IS '上传状态: UPLOADING, MERGED, PROCESSING, COMPLETED';

-- ========================================
-- 分片上传信息表
-- ========================================
CREATE TABLE chunk_info (
    id BIGSERIAL PRIMARY KEY,
    file_md5 VARCHAR(32) NOT NULL,
    chunk_index INT NOT NULL,
    chunk_md5 VARCHAR(32) NOT NULL,
    chunk_size BIGINT NOT NULL,
    storage_path VARCHAR(255) NOT NULL,
    uploaded_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_chunk_info_file ON chunk_info(file_md5, chunk_index);
CREATE UNIQUE INDEX idx_chunk_info_unique ON chunk_info(file_md5, chunk_index);

COMMENT ON TABLE chunk_info IS '分片上传信息表';

-- ========================================
-- 触发器
-- ========================================
CREATE TRIGGER update_file_upload_updated_at
    BEFORE UPDATE ON file_upload
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
