# 多智能体系统 - 存储层设计

## 项目定位
基于多智能体的智能代码审查与教学系统

## 核心表设计（14个）

### 1. 基础设施（2个）

#### users - 用户表
```sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100),
    role VARCHAR(20) DEFAULT 'USER'
        CHECK (role IN ('USER', 'ADMIN')),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);
```

#### refresh_tokens - 刷新令牌
```sql
CREATE TABLE refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    token VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_refresh_tokens_token ON refresh_tokens(token);
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id);
```

---

### 2. 多智能体任务系统（3个）⭐核心

#### agent_tasks - Agent任务主表
记录所有用户请求和Agent协作结果

```sql
CREATE TABLE agent_tasks (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    task_type VARCHAR(50) NOT NULL,  -- CODE_REVIEW, LEARNING_PATH, EXERCISE_GEN, QA
    request_data JSONB NOT NULL,      -- 请求参数
    result_data JSONB,                -- 最终结果
    status VARCHAR(20) DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED')),
    error_message TEXT,
    execution_time_ms INTEGER,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    completed_at TIMESTAMPTZ
);

CREATE INDEX idx_agent_tasks_user ON agent_tasks(user_id, created_at DESC);
CREATE INDEX idx_agent_tasks_type ON agent_tasks(task_type, status);
CREATE INDEX idx_agent_tasks_status ON agent_tasks(status);
```

**设计说明**：
- `task_type`: 任务类型
- `request_data`: 请求参数（JSONB灵活存储）
- `result_data`: Agent协作结果（JSONB）
- `execution_time_ms`: 性能分析

#### agent_messages - Agent消息传递
```sql
CREATE TABLE agent_messages (
    id BIGSERIAL PRIMARY KEY,
    task_id BIGINT NOT NULL REFERENCES agent_tasks(id),
    from_agent VARCHAR(50) NOT NULL,   -- 发送方
    to_agent VARCHAR(50),              -- 接收方（NULL=发给协调器）
    message_type VARCHAR(50) NOT NULL, -- REQUEST, RESPONSE
    content JSONB NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_agent_messages_task ON agent_messages(task_id, created_at);
```

#### agent_executions - Agent执行记录
```sql
CREATE TABLE agent_executions (
    id BIGSERIAL PRIMARY KEY,
    task_id BIGINT NOT NULL REFERENCES agent_tasks(id),
    agent_name VARCHAR(50) NOT NULL,
    action VARCHAR(100) NOT NULL,
    input_data JSONB,
    output_data JSONB,
    execution_time_ms INTEGER,
    token_usage INTEGER,
    status VARCHAR(20) DEFAULT 'SUCCESS',
    error_message TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_agent_executions_task ON agent_executions(task_id);
CREATE INDEX idx_agent_executions_agent ON agent_executions(agent_name, created_at DESC);
```

---

### 3. 代码审查模块（3个）⭐支持项目上传

#### projects - 项目表
```sql
CREATE TABLE projects (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    storage_path VARCHAR(500) NOT NULL,  -- MinIO路径
    file_count INTEGER DEFAULT 0,
    total_size BIGINT DEFAULT 0,
    language VARCHAR(50),  -- 主语言
    metadata JSONB DEFAULT '{}',
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_projects_user ON projects(user_id, created_at DESC);
CREATE INDEX idx_projects_language ON projects(language);
```

**设计说明**：
- 用户上传项目后创建记录
- `storage_path`: MinIO存储路径，如 `projects/user_123/project_456/`
- `language`: 项目主语言（自动检测）

#### project_files - 项目文件列表
```sql
CREATE TABLE project_files (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL REFERENCES projects(id),
    file_path VARCHAR(500) NOT NULL,  -- 相对路径: src/main/java/App.java
    file_name VARCHAR(255) NOT NULL,
    file_size BIGINT,
    language VARCHAR(50),
    storage_path VARCHAR(500) NOT NULL,  -- MinIO对象路径
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_project_files_project ON project_files(project_id);
CREATE INDEX idx_project_files_language ON project_files(language);
```

**设计说明**：
- 记录项目的每个文件
- `storage_path`: MinIO对象路径，如 `projects/user_123/project_456/src/main/java/App.java`
- 代码文件存MinIO，数据库只存元数据

#### code_review_requests - 审查请求
```sql
CREATE TABLE code_review_requests (
    id BIGSERIAL PRIMARY KEY,
    task_id BIGINT NOT NULL REFERENCES agent_tasks(id),
    project_id BIGINT REFERENCES projects(id),  -- 关联项目（单文件审查为NULL）
    user_id BIGINT NOT NULL REFERENCES users(id),
    review_scope VARCHAR(50) DEFAULT 'FULL',  -- FULL, PARTIAL
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_code_review_user ON code_review_requests(user_id, created_at DESC);
CREATE INDEX idx_code_review_project ON code_review_requests(project_id) WHERE project_id IS NOT NULL;
```

**审查结果存储**：
结果存在 `agent_tasks.result_data`：

```json
{
  "project_id": 456,
  "overall_score": 75,
  "summary": "项目整体质量良好",
  "file_results": [
    {
      "file_id": 1001,
      "file_path": "src/main/java/App.java",
      "issues": [{"severity": "HIGH", "line": 15, "message": "空指针风险"}],
      "score": 80
    }
  ],
  "suggestions": ["建议使用Optional"]
}
```

---

### 4. AI教学模块（4个）

#### learning_requests - 学习请求
```sql
CREATE TABLE learning_requests (
    id BIGSERIAL PRIMARY KEY,
    task_id BIGINT REFERENCES agent_tasks(id),
    user_id BIGINT NOT NULL REFERENCES users(id),
    request_type VARCHAR(50) NOT NULL,  -- LEARNING_PATH, EXERCISE, QA
    target_skill VARCHAR(100),
    current_level VARCHAR(50),
    description TEXT,
    preference JSONB DEFAULT '{}',
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_learning_user ON learning_requests(user_id, created_at DESC);
```

#### learning_paths - 学习路径
```sql
CREATE TABLE learning_paths (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    task_id BIGINT REFERENCES agent_tasks(id),
    title VARCHAR(255) NOT NULL,
    target_skill VARCHAR(100),
    weeks INTEGER,
    steps JSONB DEFAULT '[]',
    status VARCHAR(20) DEFAULT 'ACTIVE',
    progress INTEGER DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_learning_paths_user ON learning_paths(user_id);
```

#### exercises - 练习题库
```sql
CREATE TABLE exercises (
    id BIGSERIAL PRIMARY KEY,
    task_id BIGINT REFERENCES agent_tasks(id),
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    difficulty VARCHAR(20) CHECK (difficulty IN ('EASY', 'MEDIUM', 'HARD')),
    skill_tag VARCHAR(100),
    language VARCHAR(50),
    starter_code TEXT,
    solution_code TEXT,
    test_cases JSONB DEFAULT '[]',
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_exercises_skill ON exercises(skill_tag);
```

#### exercise_submissions - 练习提交
```sql
CREATE TABLE exercise_submissions (
    id BIGSERIAL PRIMARY KEY,
    exercise_id BIGINT NOT NULL REFERENCES exercises(id),
    user_id BIGINT NOT NULL REFERENCES users(id),
    code TEXT NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING',
    score INTEGER,
    feedback JSONB DEFAULT '{}',
    submitted_at TIMESTAMPTZ DEFAULT NOW(),
    graded_at TIMESTAMPTZ
);

CREATE INDEX idx_exercise_submissions_user ON exercise_submissions(user_id, submitted_at DESC);
```

---

### 5. 知识库模块（2个）

#### documents - 文档存储
```sql
CREATE TABLE documents (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id),
    title VARCHAR(500) NOT NULL,
    content TEXT,
    file_type VARCHAR(50),
    tags VARCHAR(500)[],
    metadata JSONB DEFAULT '{}',
    is_public BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_documents_tags ON documents USING GIN(tags);
```

#### document_chunks - 文档分块
```sql
CREATE TABLE document_chunks (
    id BIGSERIAL PRIMARY KEY,
    document_id BIGINT NOT NULL REFERENCES documents(id),
    chunk_index INT NOT NULL,
    text_content TEXT NOT NULL,
    es_doc_id VARCHAR(100),  -- Elasticsearch文档ID
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_chunks_document ON document_chunks(document_id);
```

---

## 表关系图

```
users (用户)
  │
  ├── refresh_tokens
  │
  ├── agent_tasks (任务中心) ←────┐
  │     │                         │
  │     ├── agent_messages        │
  │     └── agent_executions      │
  │                                 │
  ├── projects (项目) ←───────────┤ 代码审查
  │     └── project_files          │
  │                                 │
  ├── code_review_requests ────────┘
  │
  ├── learning_requests
  │
  ├── learning_paths
  │
  ├── exercises
  │     └── exercise_submissions
  │
  └── documents
        └── document_chunks
```

---

## MinIO存储结构

```
projects/
├── user_123/
│   ├── project_456/
│   │   ├── src/
│   │   │   └── main/
│   │   │       ├── java/
│   │   │       │   └── App.java
│   │   │       └── resources/
│   │   │           └── application.yml
│   │   ├── pom.xml
│   │   └── README.md
│   └── project_789/
│       └── ...
└── user_456/
    └── ...
```

---

## 数据流：项目代码审查

```
1. 用户多文件选择
   ↓
2. 前端分片上传 → MinIO存储
   ↓
3. 创建项目记录
   INSERT INTO projects (storage_path='projects/user_123/project_456/')
   ↓
4. 创建文件记录
   INSERT INTO project_files (file_path='src/main/java/App.java', ...)
   ↓
5. 创建审查任务
   INSERT INTO agent_tasks (task_type='CODE_REVIEW')
   INSERT INTO code_review_requests (project_id=456)
   ↓
6. Agent协作审查
   INSERT INTO agent_executions (agent_name='CODE_ANALYZER')
   INSERT INTO agent_messages (from_agent='CODE_ANALYZER', ...)
   ↓
7. 保存结果
   UPDATE agent_tasks (result_data={file_results:[...]})
```

---

## 设计决策总结

| 决策点 | 选择 | 原因 |
|--------|------|------|
| **项目上传** | 多文件选择 | 简单直接 |
| **代码存储** | MinIO | 代码文件不适合存数据库 |
| **增量审查** | 暂不支持 | 初期全量审查，简单可靠 |
| **Agent记录** | 可选 | agent_messages可后期开启 |
| **结果存储** | JSONB | 灵活，支持多文件结构 |
