-- 创建文档权限表
CREATE TABLE IF NOT EXISTS document_permissions (
    id BIGSERIAL PRIMARY KEY,
    document_id BIGINT NOT NULL,
    user_id BIGINT,
    role_id BIGINT,
    permission_type VARCHAR(20) NOT NULL,
    granted_by BIGINT,
    granted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP,
    CONSTRAINT fk_doc_perm_document FOREIGN KEY (document_id)
        REFERENCES documents(id) ON DELETE CASCADE,
    CONSTRAINT fk_doc_perm_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_doc_perm_role FOREIGN KEY (role_id)
        REFERENCES roles(id) ON DELETE CASCADE,
    CONSTRAINT fk_doc_perm_granted_by FOREIGN KEY (granted_by)
        REFERENCES users(id) ON DELETE SET NULL
);

-- 创建索引优化查询性能
CREATE INDEX IF NOT EXISTS idx_doc_perm_doc_id ON document_permissions(document_id);
CREATE INDEX IF NOT EXISTS idx_doc_perm_user_id ON document_permissions(user_id);
CREATE INDEX IF NOT EXISTS idx_doc_perm_role_id ON document_permissions(role_id);
CREATE INDEX IF NOT EXISTS idx_doc_perm_granted_by ON document_permissions(granted_by);
CREATE INDEX IF NOT EXISTS idx_doc_perm_type ON document_permissions(permission_type);

-- 添加唯一约束，避免重复授权
-- 注意：由于同一个用户对同一文档可能有多种权限类型，所以唯一约束包含permission_type
CREATE UNIQUE INDEX IF NOT EXISTS idx_doc_perm_unique ON document_permissions(document_id, user_id, permission_type)
WHERE user_id IS NOT NULL;

-- 为现有文档添加权限（假设所有者是上传者）
-- 这个脚本会在系统首次运行时执行，为现有数据初始化权限
INSERT INTO document_permissions (document_id, user_id, permission_type, granted_by, granted_at)
SELECT d.id, d.uploaded_by, 'ADMIN', d.uploaded_by, d.created_at
FROM documents d
WHERE d.uploaded_by IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM document_permissions dp
    WHERE dp.document_id = d.id
      AND dp.user_id = d.uploaded_by
      AND dp.permission_type = 'ADMIN'
)
ON CONFLICT (document_id, user_id, permission_type) DO NOTHING;

-- 添加注释
COMMENT ON TABLE document_permissions IS '文档权限表，控制用户对文档的访问权限';
COMMENT ON COLUMN document_permissions.document_id IS '文档ID';
COMMENT ON COLUMN document_permissions.user_id IS '用户ID（角色权限时为NULL）';
COMMENT ON COLUMN document_permissions.role_id IS '角色ID（用户权限时为NULL）';

COMMENT ON COLUMN document_permissions.permission_type IS '权限类型：READ, WRITE, DELETE, ADMIN';
COMMENT ON COLUMN document_permissions.granted_by IS '授权者用户ID';
COMMENT ON COLUMN document_permissions.granted_at IS '授权时间';
COMMENT ON COLUMN document_permissions.expires_at IS '权限过期时间（可选）';
