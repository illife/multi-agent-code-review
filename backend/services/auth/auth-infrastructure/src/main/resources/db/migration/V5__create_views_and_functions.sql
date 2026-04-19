-- ========================================
-- 企业知识库 MVP - V5: 视图和辅助函数
-- 用户统计 + 文档访问统计
-- ========================================

-- ========================================
-- 用户统计视图
-- ========================================
CREATE OR REPLACE VIEW user_stats AS
SELECT
    u.id as user_id,
    u.username,
    u.full_name,
    u.role,
    u.primary_org_tag,
    COUNT(DISTINCT d.id) as document_count,
    COUNT(DISTINCT qa.id) as qa_count,
    SUM(d.file_size) as total_file_size,
    MAX(d.last_accessed_at) as last_document_access,
    MAX(qa.created_at) as last_qa_activity
FROM users u
LEFT JOIN documents d ON d.uploaded_by = u.id
LEFT JOIN qa_history qa ON qa.user_id = u.id
GROUP BY u.id, u.username, u.full_name, u.role, u.primary_org_tag;

COMMENT ON VIEW user_stats IS '用户统计视图 - 仪表盘用';

-- ========================================
-- 文档统计视图
-- ========================================
CREATE OR REPLACE VIEW document_stats AS
SELECT
    d.org_tag,
    d.file_type,
    d.status,
    d.is_public,
    COUNT(*) as document_count,
    SUM(d.file_size) as total_file_size,
    AVG(d.file_size) as avg_file_size,
    SUM(d.access_count) as total_access_count,
    COUNT(DISTINCT d.uploaded_by) as unique_uploaders
FROM documents d
GROUP BY d.org_tag, d.file_type, d.status, d.is_public;

COMMENT ON VIEW document_stats IS '文档统计视图';

-- ========================================
-- 辅助函数

-- 增加文档访问次数
CREATE OR REPLACE FUNCTION increment_document_access(p_document_id BIGINT)
RETURNS void AS $$
BEGIN
    UPDATE documents
    SET access_count = access_count + 1,
        last_accessed_at = NOW()
    WHERE id = p_document_id;
END;
$$ LANGUAGE plpgsql;

-- 获取用户的组织标签（数组）
CREATE OR REPLACE FUNCTION get_user_org_tags(p_user_id BIGINT)
RETURNS VARCHAR(50][] AS $$
DECLARE
    v_org_tags_text VARCHAR(255);
    v_org_tags_array VARCHAR(50)[];
BEGIN
    SELECT org_tags INTO v_org_tags_text
    FROM users
    WHERE id = p_user_id;

    -- 将逗号分隔字符串转为数组
    IF v_org_tags_text IS NOT NULL THEN
        SELECT string_to_array(v_org_tags_text, ',') INTO v_org_tags_array;
    END IF;

    RETURN v_org_tags_array;
END;
$$ LANGUAGE plpgsql;

-- 检查用户是否有权限访问文档
CREATE OR REPLACE FUNCTION can_access_document(p_user_id BIGINT, p_document_id BIGINT)
RETURNS BOOLEAN AS $$
DECLARE
    v_doc_org_tag VARCHAR(50);
    v_user_org_tags VARCHAR(50)[];
    v_is_public BOOLEAN;
BEGIN
    -- 获取文档信息
    SELECT org_tag, is_public INTO v_doc_org_tag, v_is_public
    FROM documents
    WHERE id = p_document_id;

    -- 公开文档所有人可访问
    IF v_is_public = TRUE THEN
        RETURN TRUE;
    END IF;

    -- 获取用户的组织标签
    v_user_org_tags := get_user_org_tags(p_user_id);

    -- 检查用户是否有文档的组织标签
    IF v_doc_org_tag IS NOT NULL AND v_user_org_tags IS NOT NULL THEN
        RETURN v_doc_org_tag = ANY(v_user_org_tags);
    END IF;

    -- 检查是否是文档上传者
    IF EXISTS (
        SELECT 1 FROM documents
        WHERE id = p_document_id AND uploaded_by = p_user_id
    ) THEN
        RETURN TRUE;
    END IF;

    -- 管理员可访问所有文档
    IF EXISTS (
        SELECT 1 FROM users
        WHERE id = p_user_id AND role = 'ADMIN'
    ) THEN
        RETURN TRUE;
    END IF;

    RETURN FALSE;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION can_access_document IS '检查用户是否有权限访问文档';
COMMENT ON FUNCTION get_user_org_tags IS '获取用户的组织标签数组';
COMMENT ON FUNCTION increment_document_access IS '增加文档访问次数';
