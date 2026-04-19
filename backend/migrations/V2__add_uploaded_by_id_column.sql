-- ============================================================================
-- Migration: Add uploaded_by_id column to documents table
-- Author: Knowledge Base Team
-- Date: 2026-04-10
-- Description:
--   添加uploaded_by_id冗余字段到documents表，避免JPA懒加载异常
--
--   原因：
--   - Document.uploadedBy是懒加载关系
--   - 在Kafka消费者异步处理时，Hibernate Session已关闭
--   - 序列化JSON时访问uploadedBy会触发懒加载异常
--   - 解决方案：添加冗余字段uploadedById存储上传者ID
-- ============================================================================

-- 添加uploaded_by_id列
ALTER TABLE documents ADD COLUMN IF NOT EXISTS uploaded_by_id BIGINT;

-- 为现有数据填充uploaded_by_id（从uploaded_by外键复制）
-- 注意：如果表中有现有数据，需要从users表查询并填充
-- UPDATE documents d
-- SET uploaded_by_id = (SELECT u.id FROM users u WHERE u.id = d.uploaded_by)
-- WHERE d.uploaded_by IS NOT NULL;

-- 暂时设置为NULL（新文档会自动填充）
-- ALTER TABLE documents ALTER COLUMN uploaded_by_id SET NOT NULL;

-- 创建索引（提升查询性能）
CREATE INDEX IF NOT EXISTS idx_documents_uploaded_by_id ON documents(uploaded_by_id);

-- 添加注释
COMMENT ON COLUMN documents.uploaded_by_id IS '上传者ID（冗余字段，避免懒加载异常）';

-- ============================================================================
-- 验证脚本
-- ============================================================================
-- 检查字段是否添加成功
-- SELECT column_name, data_type, is_nullable, column_default
-- FROM information_schema.columns
-- WHERE table_name = 'documents' AND column_name = 'uploaded_by_id';

-- 检查数据是否正确复制
-- SELECT id, title, uploaded_by_id, (SELECT id FROM users LIMIT 1) as user_id
-- FROM documents
-- LIMIT 5;

-- ============================================================================
-- 回滚脚本（如需要）
-- ============================================================================
-- ALTER TABLE documents DROP COLUMN IF EXISTS uploaded_by_id;
-- DROP INDEX IF EXISTS idx_documents_uploaded_by_id;
