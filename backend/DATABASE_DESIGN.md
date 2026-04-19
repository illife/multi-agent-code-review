# 企业级知识库数据库设计方案

## 概述

这是一份为企业知识库和问答系统设计的完整数据库架构方案，支持多租户、审计日志、软删除、版本控制和性能优化。

## 架构原则

### 1. 多租户架构
- **策略**: 共享数据库、共享 Schema (Shared Database, Shared Schema)
- **实现**: 通过 `tenant_id` 列实现租户隔离
- **优势**: 资源利用率高、维护简单、备份方便
- **安全**: 使用 PostgreSQL 行级安全策略 (RLS) 确保租户隔离

### 2. 性能优化
- **索引策略**: 组合索引、部分索引、覆盖索引、GIN 索引
- **分区策略**: 按月分区审计日志和问答历史
- **物化视图**: 预聚合仪表盘查询数据
- **缓存策略**: Redis 缓存热点数据

### 3. 数据完整性
- **软删除**: 保留删除记录用于审计和恢复
- **版本控制**: 文档版本历史记录
- **审计日志**: 完整的操作追踪
- **级联操作**: 适当使用 CASCADE 和 RESTRICT

## 核心表结构

### 租户管理 (Tenants)

```sql
-- 租户表：管理多租户组织
CREATE TABLE tenants (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,              -- 组织名称
    slug VARCHAR(100) NOT NULL UNIQUE,       -- 唯一标识符
    domain VARCHAR(255),                     -- 自定义域名
    logo_url VARCHAR(500),                   -- 组织Logo
    settings JSONB DEFAULT '{}',             -- 租户特定设置
    subscription_tier VARCHAR(20),           -- 订阅级别
    max_users INTEGER DEFAULT 5,             -- 最大用户数
    max_storage_gb INTEGER DEFAULT 10,       -- 最大存储空间
    is_active BOOLEAN DEFAULT TRUE,          -- 是否激活
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);
```

**设计决策**:
- 使用 `slug` 作为租户的路由标识符
- `subscription_tier` 支持分级订阅模式
- `settings` 使用 JSONB 灵活存储租户配置
- 添加索引支持快速查找

### 用户管理 (Users)

```sql
-- 增强用户表：支持多租户和软删除
CREATE TABLE users_v2 (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenants(id),
    username VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100),
    department VARCHAR(100),
    profile_data JSONB DEFAULT '{}',        -- 灵活的用户配置
    is_active BOOLEAN DEFAULT TRUE,
    is_email_verified BOOLEAN DEFAULT FALSE,
    last_login_at TIMESTAMPTZ,
    deleted_at TIMESTAMPTZ,                -- 软删除时间戳
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    UNIQUE(tenant_id, username),
    UNIQUE(tenant_id, email)
);
```

**性能优化**:
```sql
-- 核心索引
CREATE INDEX idx_users_tenant_id ON users_v2(tenant_id);
CREATE INDEX idx_users_email ON users_v2(email) WHERE deleted_at IS NULL;

-- 部分索引（只索引活跃用户）
CREATE INDEX idx_users_active ON users_v2(is_active)
    WHERE is_active = TRUE AND deleted_at IS NULL;

-- JSONB 索引用于灵活查询
CREATE INDEX idx_users_profile ON users_v2 USING GIN(profile_data);

-- 全文搜索索引
CREATE INDEX idx_users_fulltext ON users_v2 USING GIN(
    to_tsvector('simple',
        COALESCE(full_name, '') || ' ' ||
        COALESCE(email, '') || ' ' ||
        COALESCE(department, '')
    )
) WHERE deleted_at IS NULL;
```

### 文档管理 (Documents)

```sql
-- 增强文档表：版本控制、权限管理、性能优化
CREATE TABLE documents_v2 (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenants(id),
    parent_version_id BIGINT REFERENCES documents_v2(id),  -- 版本链
    version_number INTEGER DEFAULT 1,                       -- 版本号
    title VARCHAR(500) NOT NULL,
    description TEXT,
    file_name VARCHAR(255) NOT NULL,
    file_type VARCHAR(50) NOT NULL,
    file_size BIGINT,
    file_md5 VARCHAR(64),                                  -- 秒传支持
    storage_type VARCHAR(20) NOT NULL,                     -- LOCAL/MINIO/S3/OSS
    storage_path VARCHAR(500),
    uploaded_by BIGINT NOT NULL REFERENCES users_v2(id),
    department VARCHAR(100),
    tags VARCHAR(500)[],                                   -- PostgreSQL 数组
    metadata JSONB DEFAULT '{}',                           -- 灵活元数据
    is_public BOOLEAN DEFAULT FALSE,
    visibility VARCHAR(20) DEFAULT 'PRIVATE',              -- 权限可见性
    status VARCHAR(20) DEFAULT 'PROCESSING',               -- 处理状态
    indexed_at TIMESTAMPTZ,
    last_accessed_at TIMESTAMPTZ,
    access_count INTEGER DEFAULT 0,                        -- 访问统计
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);
```

**性能索引**:
```sql
-- 组合索引（覆盖常见查询）
CREATE INDEX idx_documents_tenant_status ON documents_v2(tenant_id, status)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_documents_tenant_visibility ON documents_v2(tenant_id, visibility)
    WHERE deleted_at IS NULL;

-- 覆盖索引（包含列，支持索引扫描）
CREATE INDEX idx_documents_list_covering ON documents_v2(tenant_id, status, created_at DESC)
    INCLUDE (title, file_type, uploaded_by, visibility)
    WHERE deleted_at IS NULL;

-- GIN 索引用于数组和 JSONB
CREATE INDEX idx_documents_tags ON documents_v2 USING GIN(tags);
CREATE INDEX idx_documents_metadata ON documents_v2 USING GIN(metadata);

-- 部分索引（只索引公开文档）
CREATE INDEX idx_documents_public ON documents_v2(tenant_id, is_public, created_at DESC)
    WHERE is_public = TRUE AND deleted_at IS NULL;

-- 全文搜索索引
CREATE INDEX idx_documents_fulltext ON documents_v2 USING GIN(
    to_tsvector('simple',
        COALESCE(title, '') || ' ' ||
        COALESCE(description, '')
    )
) WHERE deleted_at IS NULL;
```

### 文档块 (Document Chunks)

```sql
-- 优化的文档块存储，支持 RAG 和父子分块策略
CREATE TABLE document_chunks_v2 (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenants(id),
    document_id BIGINT NOT NULL REFERENCES documents_v2(id),
    parent_chunk_id BIGINT REFERENCES document_chunks_v2(id),  -- 父子关系
    chunk_type VARCHAR(10) NOT NULL CHECK (chunk_type IN ('PARENT', 'CHILD')),
    content TEXT NOT NULL,
    position INTEGER NOT NULL,
    char_count INTEGER,
    token_count INTEGER,                                    -- Token 统计
    embedding_vector BYTEA,                                 -- 向量存储
    embedding_model VARCHAR(50),
    vector_dimensions INTEGER,
    embedding_hash VARCHAR(64),                             -- 去重哈希
    language VARCHAR(20),
    metadata JSONB DEFAULT '{}',
    access_count INTEGER DEFAULT 0,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);
```

**RAG 优化索引**:
```sql
-- RAG 查询优化：组合索引
CREATE INDEX idx_chunks_document_type ON document_chunks_v2(document_id, chunk_type)
    WHERE deleted_at IS NULL;

-- 向量搜索候选者
CREATE INDEX idx_chunks_for_embedding ON document_chunks_v2(document_id, position)
    WHERE chunk_type = 'CHILD'
    AND deleted_at IS NULL
    AND embedding_vector IS NOT NULL;

-- 语言和元数据过滤
CREATE INDEX idx_chunks_language ON document_chunks_v2(language)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_chunks_metadata ON document_chunks_v2 USING GIN(metadata);
```

### 权限管理 (Permissions)

```sql
-- 细粒度文档权限控制
CREATE TABLE document_permissions_v2 (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenants(id),
    document_id BIGINT NOT NULL REFERENCES documents_v2(id),
    user_id BIGINT REFERENCES users_v2(id),
    role_id BIGINT REFERENCES roles_v2(id),
    permission_type VARCHAR(20) NOT NULL
        CHECK (permission_type IN ('READ', 'WRITE', 'DELETE', 'ADMIN')),
    granted_by BIGINT NOT NULL REFERENCES users_v2(id),
    granted_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMPTZ,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    UNIQUE(tenant_id, document_id, user_id, permission_type)
);
```

**权限检查优化**:
```sql
-- 覆盖索引用于快速权限检查
CREATE INDEX idx_doc_perm_check ON document_permissions_v2(
    document_id, user_id, permission_type
)
INCLUDE (expires_at)
WHERE deleted_at IS NULL
AND (expires_at IS NULL OR expires_at > NOW());

-- 过期权限管理
CREATE INDEX idx_doc_perm_expires ON document_permissions_v2(expires_at)
    WHERE expires_at IS NOT NULL AND deleted_at IS NULL;
```

### 问答历史 (QA History)

```sql
-- 增强的问答历史，支持分析和追踪
CREATE TABLE qa_history_v2 (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenants(id),
    user_id BIGINT NOT NULL REFERENCES users_v2(id),
    session_id VARCHAR(100),                                -- 会话分组
    question TEXT NOT NULL,
    answer TEXT NOT NULL,
    search_type VARCHAR(20) CHECK (search_type IN ('BM25', 'KNN', 'HYBRID')),
    sources JSONB,
    context_used JSONB,
    chunks_accessed JSONB,                                  -- 追踪使用的文档块
    model_used VARCHAR(50),
    tokens_consumed INTEGER,                                -- 成本追踪
    response_time_ms INTEGER,                               -- 性能追踪
    feedback INTEGER CHECK (feedback >= 1 AND feedback <= 5),
    feedback_comment TEXT,
    is_bookmarked BOOLEAN DEFAULT FALSE,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL
);
```

**分析索引**:
```sql
-- 用户活动分析
CREATE INDEX idx_qa_user_created ON qa_history_v2(user_id, created_at DESC)
    WHERE deleted_at IS NULL;

-- 反馈分析
CREATE INDEX idx_qa_feedback ON qa_history_v2(feedback)
    WHERE feedback IS NOT NULL AND deleted_at IS NULL;

-- 书签查询
CREATE INDEX idx_qa_bookmarked ON qa_history_v2(is_bookmarked)
    WHERE is_bookmarked = TRUE AND deleted_at IS NULL;

-- 全文搜索（问题）
CREATE INDEX idx_qa_question_fulltext ON qa_history_v2 USING GIN(
    to_tsvector('simple', question)
) WHERE deleted_at IS NULL;
```

### 代码审查 (Code Reviews)

```sql
-- 代码审查跟踪和分析
CREATE TABLE code_reviews_v2 (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenants(id),
    user_id BIGINT NOT NULL REFERENCES users_v2(id),
    project_id BIGINT,
    code_content TEXT NOT NULL,
    language VARCHAR(50) NOT NULL,
    file_name VARCHAR(255),
    file_path VARCHAR(500),
    total_issues INTEGER DEFAULT 0,
    critical_issues INTEGER DEFAULT 0,                      -- 严重程度统计
    high_issues INTEGER DEFAULT 0,
    medium_issues INTEGER DEFAULT 0,
    low_issues INTEGER DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    visibility VARCHAR(20) DEFAULT 'PRIVATE',
    analysis_summary TEXT,
    overall_score INTEGER CHECK (overall_score >= 0 AND overall_score <= 100),
    metrics JSONB DEFAULT '{}',
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);
```

## 高级特性

### 1. 行级安全 (Row Level Security)

```sql
-- 启用 RLS
ALTER TABLE users_v2 ENABLE ROW LEVEL SECURITY;
ALTER TABLE documents_v2 ENABLE ROW LEVEL SECURITY;
ALTER TABLE document_chunks_v2 ENABLE ROW LEVEL SECURITY;

-- 租户隔离策略
CREATE POLICY users_tenant_policy ON users_v2
    USING (tenant_id = current_setting('app.current_tenant_id')::BIGINT);

CREATE POLICY documents_tenant_policy ON documents_v2
    USING (tenant_id = current_setting('app.current_tenant_id')::BIGINT);
```

### 2. 审计日志

```sql
-- 审计日志表（按月分区）
CREATE TABLE audit_logs_partitioned (
    id BIGSERIAL,
    tenant_id BIGINT NOT NULL,
    user_id BIGINT,
    action VARCHAR(50) NOT NULL,
    resource_type VARCHAR(50) NOT NULL,
    resource_id BIGINT,
    old_values JSONB,
    new_values JSONB,
    ip_address INET,
    user_agent TEXT,
    request_id VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (id, created_at)
) PARTITION BY RANGE (created_at);

-- 自动审计触发器
CREATE FUNCTION audit_trigger_func() RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        INSERT INTO audit_logs (tenant_id, user_id, action, resource_type, resource_id, new_values)
        VALUES (NEW.tenant_id, current_setting('app.current_user_id')::BIGINT, 'CREATE', TG_TABLE_NAME, NEW.id, to_jsonb(NEW));
        RETURN NEW;
    ELSIF TG_OP = 'UPDATE' THEN
        INSERT INTO audit_logs (tenant_id, user_id, action, resource_type, resource_id, old_values, new_values)
        VALUES (NEW.tenant_id, current_setting('app.current_user_id')::BIGINT, 'UPDATE', TG_TABLE_NAME, NEW.id, to_jsonb(OLD), to_jsonb(NEW));
        RETURN NEW;
    ELSIF TG_OP = 'DELETE' THEN
        INSERT INTO audit_logs (tenant_id, user_id, action, resource_type, resource_id, old_values)
        VALUES (OLD.tenant_id, current_setting('app.current_user_id')::BIGINT, 'DELETE', TG_TABLE_NAME, OLD.id, to_jsonb(OLD));
        RETURN OLD;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;
```

### 3. 物化视图（预聚合查询）

```sql
-- 文档分析物化视图
CREATE MATERIALIZED VIEW document_analytics_mv AS
SELECT
    d.tenant_id,
    d.uploaded_by,
    d.department,
    d.status,
    d.visibility,
    d.file_type,
    COUNT(*) as document_count,
    SUM(d.file_size) as total_file_size,
    AVG(d.file_size) as avg_file_size,
    SUM(d.access_count) as total_access_count,
    COUNT(DISTINCT d.tags) as unique_tags,
    MIN(d.created_at) as earliest_document,
    MAX(d.created_at) as latest_document
FROM documents_v2 d
WHERE d.deleted_at IS NULL
GROUP BY d.tenant_id, d.uploaded_by, d.department, d.status, d.visibility, d.file_type;

-- 用户活动物化视图
CREATE MATERIALIZED VIEW user_activity_mv AS
SELECT
    u.tenant_id,
    u.id as user_id,
    u.department,
    COUNT(DISTINCT d.id) as document_count,
    COUNT(DISTINCT qa.id) as qa_count,
    COUNT(DISTINCT cr.id) as code_review_count,
    SUM(d.access_count) as total_document_access,
    AVG(d.file_size) as avg_document_size,
    MAX(u.last_login_at) as last_login,
    MAX(qa.created_at) as last_qa_activity,
    MAX(cr.created_at) as last_review_activity
FROM users_v2 u
LEFT JOIN documents_v2 d ON d.uploaded_by = u.id AND d.deleted_at IS NULL
LEFT JOIN qa_history_v2 qa ON qa.user_id = u.id AND qa.deleted_at IS NULL
LEFT JOIN code_reviews_v2 cr ON cr.user_id = u.id AND cr.deleted_at IS NULL
WHERE u.deleted_at IS NULL
GROUP BY u.tenant_id, u.id, u.department;
```

### 4. 分区策略

**审计日志分区**:
```sql
-- 按月分区
CREATE TABLE audit_logs_2024_04 PARTITION OF audit_logs_partitioned
    FOR VALUES FROM ('2024-04-01') TO ('2024-05-01');

-- 自动创建未来分区
CREATE FUNCTION create_monthly_partition(table_name text, start_date date)
RETURNS void AS $$
DECLARE
    partition_name text;
    end_date date;
BEGIN
    partition_name := table_name || '_' || to_char(start_date, 'YYYY_MM');
    end_date := start_date + interval '1 month';

    EXECUTE format(
        'CREATE TABLE IF NOT EXISTS %I PARTITION OF %I FOR VALUES FROM (%L) TO (%L)',
        partition_name,
        table_name,
        start_date,
        end_date
    );
END;
$$ LANGUAGE plpgsql;
```

## 与 Elasticsearch 协同设计

### 1. 数据同步策略

**同步内容**:
```json
{
  "document_chunks": {
    "sync_condition": "status = 'INDEXED' AND deleted_at IS NULL'",
    "fields": [
      "id", "tenant_id", "document_id", "content",
      "chunk_type", "embedding_vector", "metadata"
    ]
  },
  "documents": {
    "sync_condition": "status = 'INDEXED' AND deleted_at IS NULL'",
    "fields": [
      "id", "tenant_id", "title", "description", "tags"
    ]
  }
}
```

**同步时机**:
- 文档状态变更时（触发器）
- 定时批量同步（每 5 分钟）
- 手动强制同步

### 2. ES 索引设计

```json
{
  "kb_document_chunks": {
    "settings": {
      "number_of_shards": 3,
      "number_of_replicas": 1,
      "refresh_interval": "30s"
    },
    "mappings": {
      "properties": {
        "tenant_id": {"type": "long"},
        "document_id": {"type": "long"},
        "content": {
          "type": "text",
          "analyzer": "ik_max_word",
          "search_analyzer": "ik_smart"
        },
        "embedding_vector": {
          "type": "dense_vector",
          "dims": 1536,
          "index": true,
          "similarity": "cosine"
        },
        "chunk_type": {"type": "keyword"},
        "metadata": {"type": "object"}
      }
    }
  }
}
```

### 3. 一致性保证

**双写策略**:
1. 先写 PostgreSQL（主库）
2. 后写 Elasticsearch（通过 Kafka 异步）
3. 使用 CDC（Change Data Capture）监控变更
4. 定期一致性检查和修复

**补偿机制**:
```sql
-- ES 同步失败记录表
CREATE TABLE elasticsearch_sync_failures (
    id BIGSERIAL PRIMARY KEY,
    entity_type VARCHAR(50) NOT NULL,
    entity_id BIGINT NOT NULL,
    operation VARCHAR(20) NOT NULL,
    error_message TEXT,
    retry_count INTEGER DEFAULT 0,
    next_retry_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_es_sync_entity ON elasticsearch_sync_failures(entity_type, entity_id);
CREATE INDEX idx_es_sync_retry ON elasticsearch_sync_failures(next_retry_at)
    WHERE retry_count < 5;
```

## Redis 缓存设计

### 1. 缓存策略

```java
// 用户权限缓存（5 分钟 TTL）
String key = "user:permissions:" + userId;
redisTemplate.set(key, permissions, 5, TimeUnit.MINUTES);

// 文档元数据缓存（10 分钟 TTL）
String key = "document:metadata:" + documentId;
redisTemplate.set(key, metadata, 10, TimeUnit.MINUTES);

// 搜索结果缓存（2 分钟 TTL）
String key = "search:" + DigestUtils.md5Hex(query + filters);
redisTemplate.set(key, results, 2, TimeUnit.MINUTES);

// 热点文档内容缓存（1 小时 TTL）
String key = "document:content:" + documentId;
redisTemplate.set(key, content, 1, TimeUnit.HOURS);
```

### 2. 缓存更新策略

**写穿透（Write-Through）**:
```java
@CachePut(value = "documents", key = "#document.id")
public Document updateDocument(Document document) {
    return documentRepository.save(document);
}

@CacheEvict(value = "documents", key = "#documentId")
public void deleteDocument(Long documentId) {
    documentRepository.softDelete(documentId);
}
```

**失效事件**:
```java
@EventListener
public void handleDocumentChanged(DocumentChangedEvent event) {
    // 清除相关缓存
    redisTemplate.delete("document:metadata:" + event.getDocumentId());
    redisTemplate.delete("document:content:" + event.getDocumentId());

    // 发布 ES 同步事件
    kafkaTemplate.send("elasticsearch-sync", event);
}
```

### 3. 缓存预热

```sql
-- 热点文档识别
CREATE OR REPLACE FUNCTION get_hot_documents(p_tenant_id BIGINT, p_days INTEGER DEFAULT 7)
RETURNS TABLE(document_id BIGINT) AS $$
BEGIN
    RETURN QUERY
    SELECT d.id
    FROM documents_v2 d
    WHERE d.tenant_id = p_tenant_id
    AND d.deleted_at IS NULL
    AND d.last_accessed_at > NOW() - (p_days || ' days')::INTERVAL
    ORDER BY d.access_count DESC
    LIMIT 1000;
END;
$$ LANGUAGE plpgsql;
```

## 性能监控

### 1. 慢查询监控

```sql
-- 启用 pg_stat_statements
CREATE EXTENSION pg_stat_statements;

-- 慢查询视图
CREATE VIEW slow_queries AS
SELECT
    query,
    calls,
    total_exec_time / 1000 / 60 as total_exec_time_minutes,
    mean_exec_time as avg_exec_time_ms,
    stddev_exec_time as stddev_exec_time_ms,
    max_exec_time as max_exec_time_ms
FROM pg_stat_statements
WHERE calls > 100
ORDER BY mean_exec_time DESC
LIMIT 20;
```

### 2. 表大小监控

```sql
CREATE VIEW table_sizes AS
SELECT
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS total_size,
    pg_total_relation_size(schemaname||'.'||tablename) AS size_bytes
FROM pg_tables
WHERE schemaname = 'public'
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;
```

### 3. 索引使用监控

```sql
CREATE VIEW index_usage_stats AS
SELECT
    schemaname,
    tablename,
    indexname,
    idx_tup_read,
    idx_tup_fetch,
    idx_scan,
    pg_size_pretty(pg_relation_size(indexrelid)) AS index_size
FROM pg_stat_user_indexes
WHERE schemaname = 'public'
ORDER BY idx_scan DESC;
```

## 扩展性考虑

### 1. 水平分片策略

**按租户分片**:
```sql
-- 为大型租户创建专用表
CREATE TABLE documents_large_tenant (
    LIKE documents_v2 INCLUDING ALL
) INHERITS (documents_v2);

-- 使用应用层路由或 PostgreSQL foreign data wrapper
```

**按时间分片**:
```sql
-- 旧数据归档
CREATE TABLE documents_archive (
    LIKE documents_v2 INCLUDING ALL
);

-- 自动归档函数
CREATE OR REPLACE FUNCTION archive_old_documents(p_months INTEGER DEFAULT 24)
RETURNS BIGINT AS $$
DECLARE
    v_archived_count BIGINT;
BEGIN
    WITH archived AS (
        INSERT INTO documents_archive
        SELECT * FROM documents_v2
        WHERE created_at < NOW() - (p_months || ' months')::INTERVAL
        AND deleted_at IS NOT NULL
        RETURNING *
    )
    SELECT COUNT(*) INTO v_archived_count FROM archived;

    DELETE FROM documents_v2
    WHERE created_at < NOW() - (p_months || ' months')::INTERVAL
    AND deleted_at IS NOT NULL;

    RETURN v_archived_count;
END;
$$ LANGUAGE plpgsql;
```

### 2. 读写分离

**主从复制设置**:
```bash
# PostgreSQL 主从复制
# 主库配置 (postgresql.conf)
wal_level = replica
max_wal_senders = 5
wal_keep_size = 1GB

# 从库配置
standby_mode = on
primary_conninfo = 'host=primary port=5432 user=replicator'
```

**应用层读写分离**:
```java
@Primary
@Bean(name = "writeDataSource")
public DataSource writeDataSource() {
    return DataSourceBuilder.create().url(writeUrl).build();
}

@Bean(name = "readDataSource")
public DataSource readDataSource() {
    return DataSourceBuilder.create().url(readUrl).build();
}
```

## 备份与恢复

### 1. 备份策略

```bash
# 全量备份（每周）
pg_dump -Fc -f backup_$(date +%Y%m%d).dump knowledge_base

# 增量备份（每天）
pg_backup_start(label='daily_backup')
# 备份 WAL 日志
pg_backup_stop(label='daily_backup')

# 备份特定表
pg_dump -t documents_v2 -t document_chunks_v2 knowledge_base
```

### 2. 恢复策略

```bash
# 恢复全量备份
pg_restore -d knowledge_base backup_20240414.dump

# 时间点恢复（PITR）
# 1. 恢复基础备份
# 2. 重放 WAL 日志到指定时间点
recovery_target_time = '2024-04-14 10:00:00'
```

## 维护任务

### 1. 日常维护

```sql
-- 更新统计信息
ANALYZE documents_v2;
ANALYZE document_chunks_v2;

-- 清理死元组
VACUUM ANALYZE documents_v2;

-- 重建碎片索引
REINDEX INDEX CONCURRENTLY idx_documents_tenant_status;
```

### 2. 定期维护

```sql
-- 清理旧审计日志
SELECT cleanup_old_audit_logs(12);  -- 保留 12 个月

-- 刷新物化视图
REFRESH MATERIALIZED VIEW CONCURRENTLY document_analytics_mv;
REFRESH MATERIALIZED VIEW CONCURRENTLY user_activity_mv;

-- 收集性能指标
SELECT collect_performance_metrics();
```

## 总结

这个数据库设计方案提供了：

1. **多租户支持**: 通过 `tenant_id` 和 RLS 实现完全隔离
2. **性能优化**: 分区、索引、物化视图、缓存
3. **数据完整性**: 软删除、版本控制、审计日志
4. **可扩展性**: 水平分片、读写分离、归档策略
5. **可维护性**: 监控视图、维护函数、备份恢复

设计权衡：
- **存储空间**: 牺牲存储空间换取查询性能（冗余字段、索引）
- **写入性能**: 牺牲写入性能换取读取性能（大量索引）
- **复杂度**: 增加系统复杂度换取功能完整性（分区、物化视图）

这个方案适合中大型企业知识库系统，可以根据实际业务需求进行调整。
