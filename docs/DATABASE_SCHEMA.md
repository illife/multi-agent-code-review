# 数据库设计文档

## 概述

系统采用微服务架构，每个服务拥有独立的数据库schema。所有服务共享同一个PostgreSQL实例，但通过不同的schema进行隔离。

## 数据库实例信息

- **数据库名**: `multi_agent_platform` (生产环境) / `knowledge_base` (开发环境)
- **字符集**: UTF-8
- **时区**: UTC

## Schema 架构

```
multi_agent_platform (Database)
├── public (默认schema) - 共享表
├── auth (认证服务)
├── kb (知识库服务)
└── ci (代码审查服务)
```

## 1. 认证服务 (auth)

Schema: `public` 和 `auth`

### 1.1 用户表 (users)
```sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    full_name VARCHAR(100),
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    last_login_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

**索引**:
- `idx_users_username` on (username)
- `idx_users_email` on (email)
- `idx_users_role` on (role)

### 1.2 组织表 (organizations)
```sql
CREATE TABLE organizations (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    owner_id BIGINT NOT NULL REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

### 1.3 组织成员表 (organization_members)
```sql
CREATE TABLE organization_members (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL REFERENCES organizations(id),
    user_id BIGINT NOT NULL REFERENCES users(id),
    role VARCHAR(50) NOT NULL DEFAULT 'MEMBER',
    joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(organization_id, user_id)
);
```

### 1.4 Token表 (tokens)
```sql
CREATE TABLE tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    token_type VARCHAR(50) NOT NULL,
    token VARCHAR(500) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

### 1.5 文件上传表 (file_uploads)
```sql
CREATE TABLE file_uploads (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    filename VARCHAR(255) NOT NULL,
    original_filename VARCHAR(255),
    file_path VARCHAR(500),
    file_size BIGINT,
    mime_type VARCHAR(100),
    storage_type VARCHAR(50) DEFAULT 'LOCAL',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

### 1.6 学习路径表 (learning_paths)
```sql
CREATE TABLE learning_paths (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    task_id BIGINT,
    title VARCHAR(200) NOT NULL,
    target_skill VARCHAR(100),
    weeks INTEGER NOT NULL,
    steps JSONB,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    progress INTEGER DEFAULT 0,
    current_step INTEGER,
    completed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

## 2. 知识库服务 (knowledge-mentor)

Schema: `kb` (在代码中使用 `public` schema)

### 2.1 文档表 (documents)
```sql
CREATE TABLE documents (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(500),
    description TEXT,
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500),
    file_size BIGINT,
    file_type VARCHAR(100),
    file_md5 VARCHAR(32),
    uploaded_by BIGINT NOT NULL,
    is_public BOOLEAN DEFAULT FALSE,
    status VARCHAR(20) DEFAULT 'UPLOADED',
    indexed_chunk_count INTEGER DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

**索引**:
- `idx_documents_user_id` on (uploaded_by)
- `idx_documents_status` on (status)
- `idx_documents_is_public` on (is_public)
- `idx_documents_created_at` on (created_at DESC)

### 2.2 文档块表 (document_chunks)
```sql
CREATE TABLE document_chunks (
    id BIGSERIAL PRIMARY KEY,
    document_id BIGINT NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    chunk_index INTEGER NOT NULL,
    content TEXT NOT NULL,
    metadata JSONB,
    embedding_id VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

**索引**:
- `idx_chunks_document_id` on (document_id)
- `idx_chunks_chunk_index` on (document_id, chunk_index)

### 2.3 文档权限表 (document_permissions)
```sql
CREATE TABLE document_permissions (
    id BIGSERIAL PRIMARY KEY,
    document_id BIGINT NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL,
    permission VARCHAR(20) NOT NULL,
    granted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    granted_by BIGINT,
    UNIQUE(document_id, user_id)
);
```

### 2.4 代码审查关联表 (code_reviews)
```sql
CREATE TABLE code_reviews (
    id BIGSERIAL PRIMARY KEY,
    document_id BIGINT REFERENCES documents(id),
    user_id BIGINT NOT NULL,
    code_content TEXT,
    language VARCHAR(50),
    file_name VARCHAR(255),
    status VARCHAR(20) DEFAULT 'PENDING',
    total_issues INTEGER DEFAULT 0,
    visibility VARCHAR(20) DEFAULT 'PRIVATE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

## 3. 代码审查服务 (code-intelligence)

Schema: `ci` (在代码中使用 `public` schema)

### 3.1 代码审查表 (code_reviews)
```sql
CREATE TABLE code_reviews (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    code_content TEXT NOT NULL,
    language VARCHAR(50) NOT NULL,
    file_name VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    total_issues INTEGER DEFAULT 0,
    visibility VARCHAR(20) DEFAULT 'PRIVATE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

**状态枚举**: `PENDING`, `PROCESSING`, `COMPLETED`, `FAILED`

**索引**:
- `idx_code_reviews_user_id` on (user_id)
- `idx_code_reviews_status` on (status)
- `idx_code_reviews_created_at` on (created_at DESC)

### 3.2 代码问题表 (code_issues)
```sql
CREATE TABLE code_issues (
    id BIGSERIAL PRIMARY KEY,
    review_id BIGINT NOT NULL REFERENCES code_reviews(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    severity VARCHAR(20) NOT NULL,
    category VARCHAR(100),
    line_number INTEGER,
    code_snippet TEXT,
    suggestion TEXT,
    teaching_explanation TEXT,
    agent_type VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

**严重程度**: `CRITICAL`, `HIGH`, `MEDIUM`, `LOW`, `INFO`

**索引**:
- `idx_issues_review_id` on (review_id)
- `idx_issues_severity` on (severity)
- `idx_issues_category` on (category)
- `idx_issues_agent_type` on (agent_type)

### 3.3 教学报告表 (teaching_reports)
```sql
CREATE TABLE teaching_reports (
    id BIGSERIAL PRIMARY KEY,
    review_id BIGINT NOT NULL REFERENCES code_reviews(id) ON DELETE CASCADE,
    language VARCHAR(50),
    code_summary TEXT,
    summary TEXT,
    knowledge_gaps_json JSONB,
    key_findings_json JSONB,
    learning_resources_json JSONB,
    priority_actions_json JSONB,
    encouragement TEXT,
    total_issues INTEGER,
    critical_issues INTEGER DEFAULT 0,
    high_issues INTEGER DEFAULT 0,
    medium_issues INTEGER DEFAULT 0,
    low_issues INTEGER DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

### 3.4 项目表 (projects)
```sql
CREATE TABLE projects (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    project_name VARCHAR(255) NOT NULL,
    description TEXT,
    upload_type VARCHAR(20) NOT NULL,
    source_url VARCHAR(500),
    storage_path VARCHAR(500),
    total_files INTEGER DEFAULT 0,
    total_size BIGINT,
    language VARCHAR(50),
    analyzed_files INTEGER DEFAULT 0,
    total_issues INTEGER DEFAULT 0,
    status VARCHAR(20) DEFAULT 'PENDING',
    visibility VARCHAR(20) DEFAULT 'PRIVATE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

**状态枚举**: `PENDING`, `ANALYZING`, `COMPLETED`, `FAILED`

### 3.5 项目文件表 (project_files)
```sql
CREATE TABLE project_files (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    file_path VARCHAR(1000) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    language VARCHAR(50),
    file_size BIGINT,
    line_count INTEGER,
    is_analyzed BOOLEAN DEFAULT FALSE,
    analysis_priority INTEGER,
    review_id BIGINT REFERENCES code_reviews(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

### 3.6 项目报告表 (project_reports)
```sql
CREATE TABLE project_reports (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    summary TEXT,
    overall_score INTEGER,
    risk_level VARCHAR(20),
    metrics JSONB,
    recommendations TEXT,
    file_statistics JSONB,
    full_markdown_report TEXT,
    file_issue_details JSONB,
    severity_distribution JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

### 3.7 智能体任务表 (agent_tasks)
```sql
CREATE TABLE agent_tasks (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    task_type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    input_data JSONB,
    status VARCHAR(20) DEFAULT 'PENDING',
    result JSONB,
    error_message TEXT,
    scheduled_at TIMESTAMP,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

### 3.8 学习进度表 (user_progress)
```sql
CREATE TABLE user_progress (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    total_xp INTEGER DEFAULT 0,
    level INTEGER DEFAULT 1,
    current_path_id BIGINT,
    paths_completed INTEGER DEFAULT 0,
    exercises_completed INTEGER DEFAULT 0,
    streak_days INTEGER DEFAULT 0,
    last_activity_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

### 3.9 用户成就表 (user_achievements)
```sql
CREATE TABLE user_achievements (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    achievement_id VARCHAR(100) NOT NULL,
    unlocked_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    progress INTEGER,
    metadata JSONB,
    UNIQUE(user_id, achievement_id)
);
```

## 表关系图

```
users (1) ----< (N) organization_members
  |                     |
  |                  organizations
  |
  +----< (1) code_reviews
  |                     |
  +----< (1) learning_paths      (1) ----< projects ----< (N) project_files
  |                                          |
  +----< (1) file_uploads                     (1) ----< project_reports
  |
  +----< (1) user_progress
  |
  +----< (1) user_achievements


documents (1) ----< (N) document_chunks
  |
  +----< (N) document_permissions
  |
  +----< (N) code_reviews
```

## 数据字典

### 状态枚举值

#### code_reviews.status
- `PENDING` - 待处理
- `PROCESSING` - 处理中
- `COMPLETED` - 已完成
- `FAILED` - 失败

#### code_issues.severity
- `CRITICAL` - 严重
- `HIGH` - 高危
- `MEDIUM` - 中等
- `LOW` - 低危
- `INFO` - 信息

#### documents.status
- `UPLOADED` - 已上传
- `PROCESSING` - 处理中
- `INDEXED` - 已索引
- `FAILED` - 失败

#### projects.status
- `PENDING` - 待处理
- `ANALYZING` - 分析中
- `COMPLETED` - 已完成
- `FAILED` - 失败

#### agent_tasks.status
- `PENDING` - 待处理
- `SCHEDULED` - 已调度
- `RUNNING` - 运行中
- `COMPLETED` - 已完成
- `FAILED` - 失败

## 性能优化

### 索引策略
- 所有外键列都创建索引
- 频繁查询的字段创建复合索引
- 时间戳字段创建降序索引（用于分页）

### 分区策略
- 考虑对 `document_chunks` 表按时间分区
- 考虑对 `code_issues` 表按review_id分区

### 缓存策略
- 用户信息缓存在Redis
- 文档元数据缓存在Redis
- 搜索结果缓存在Redis (TTL: 5分钟)

## 数据库迁移管理

使用Flyway进行数据库版本管理：

```bash
# 开发环境
mvn flyway:migrate

# 生产环境
mvn flyway:info -Dflyway.configs=production
```

## 备份策略

### 每日备份
- 全量备份: 每天凌晨2点
- 增量备份: 每4小时一次
- 保留期: 30天

### 恢复流程
1. 停止所有服务
2. 恢复最近的完整备份
3. 应用增量备份
4. 验证数据完整性
5. 重启服务

## 监控指标

- 数据库连接池使用率
- 慢查询日志 (> 1秒)
- 表大小监控
- 索引效率监控
- 死锁检测

## 相关文档

- [部署指南](../DEPLOYMENT.md)
- [配置指南](../CONFIGURATION_GUIDE.md)
- [开发指南](../DEVELOPMENT.md)
