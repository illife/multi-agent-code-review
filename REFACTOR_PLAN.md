# AI 智能代码导师系统 - 架构重构计划 v2.0

## 🎯 重构目标

将当前项目重构为符合**多智能体架构**的智能代码审查与教学系统，体现 AI 驱动、Agent 编排、智能教学的核心特性。

---

## 📊 当前命名问题分析

### 问题 1: 命名不符合系统定位
```
当前: codereview (代码审查)
定位: Multi-Agent AI 智能分析 + 教学

当前: knowledge-base (知识库)
定位: 智能教学导师系统
```

### 问题 2: 功能重合但未共享
```
重复模块:
├── User/Auth 认证系统        2 份
├── JWT/Security 安全配置      2 份
├── MinIO 文件存储            2 份
├── Kafka 消息队列            2 份
├── Redis 缓存                2 份
├── Qwen AI 调用              2 份
├── WebSocket 实时通信        2 份
└── 通用 Result/DTO           2 份
```

### 问题 3: 缺少 Agent 抽象层
```
当前: Agent 直接耦合具体实现
需要: Agent 接口抽象 + 多种实现
```

---

## 🏗️ 目标架构

### 整体架构

```
┌─────────────────────────────────────────────────────────────┐
│                     AI 智能代码导师系统                          │
│                (AI Code Mentor System)                        │
└─────────────────────────────────────────────────────────────┘
                              │
        ┌─────────────────────┼─────────────────────┐
        │                     │                     │
┌───────▼────────┐   ┌────────▼─────────┐   ┌──────▼──────────┐
│ shared-auth    │   │ shared-agent     │   │ shared-infra    │
│ (统一认证)      │   │ (Agent 核心)      │   │ (基础设施)      │
└────────┬────────┘   └────────┬─────────┘   └────────┬─────────┘
         │                     │                     │
┌────────▼────────┐   ┌────────▼─────────┐   ┌────────▼─────────┐
│ Auth Service    │   │ Agent Orchestration│   │ AI/Storage/Cache │
│ :8082           │   │ Service           │   │ Services          │
└─────────────────┘   └────────────────────┘   └──────────────────┘
         │                     │                     │
┌────────▼────────────────────────▼─────────────────▼─────────┐
│                       API Gateway (可选)                      │
│                   /km/*  /ci/*  /auth/*                         │
└─────────────────────────────┬───────────────────────────────┘
                              │
        ┌─────────────────────┴─────────────────────┐
        │                     │                     │
┌───────▼────────────┐  ┌───────▼─────────────┐  ┌──────▼──────────┐
│ Knowledge Mentor  │  │ Code Intelligence  │  │  Frontend       │
│ Service           │  │ Service            │  │  (React)        │
│ :8080             │  │ :8081              │  │  :5173           │
└───────────────────┘  └────────────────────┘  └──────────────────┘
```

### 新的模块命名

| 当前名称 | 新名称 | 说明 |
|---------|-------|------|
| codereview-api | code-intelligence-api | 体现 AI 智能分析 |
| codereview-domain | ci-domain | Code Intelligence 领域层 |
| codereview-infrastructure | ci-infrastructure | 基础设施层 |
| knowledge-base-api | knowledge-mentor-api | 体现导师/教学特性 |
| knowledge-base-core | km-domain | Knowledge Mentor 领域层 |

---

## 📦 共享模块详细设计

### 1. shared-auth (统一认证)

```
shared-auth/
├── auth-domain/                    # 领域层
│   ├── User.java                   # 用户实体
│   ├── Role.java                   # 角色实体
│   ├── UserRole.java               # 用户角色关联
│   └── Permission.java             # 权限实体
│
├── auth-security/                  # 安全层
│   ├── CustomUserDetails.java      # 用户详情
│   ├── CustomUserDetailsService.java
│   ├── JwtTokenProvider.java      # JWT 提供
│   ├── JwtAuthenticationFilter.java # JWT 过滤器
│   └── SecurityConfig.java         # 安全配置
│
└── auth-api/                       # 认证 API
    ├── AuthController.java
    ├── AuthRequest.java
    └── AuthResponse.java
```

### 2. shared-agent (Agent 核心) ⭐

```
shared-agent/
├── agent-domain/                   # Agent 领域
│   ├── Agent.java                  # Agent 接口
│   ├── AgentType.java              # Agent 类型枚举
│   ├── AgentRequest.java           # Agent 请求
│   └── AgentResult.java            # Agent 结果
│
├── agent-orchestration/            # Agent 编排
│   ├── AgentOrchestrator.java      # 编排器
│   ├── PipelineOrchestrator.java   # 流水线编排 (并行)
│   └── ExecutionStrategy.java      # 执行策略
│
├── inspector-agents/               # 检查员 Agents
│   ├── CodeStandardsInspector.java  # 代码规范
│   ├── ArchitectureGuardian.java    # 架构审查
│   └── SecurityAuditor.java        # 安全审计
│
└── mentor-agents/                  # 导师 Agents
    ├── TeachingMentor.java         # 教学导师
    ├── SkillAssessor.java          # 技能评估
    └── ExerciseCoach.java          # 练习教练
```

### 3. shared-ai (AI 服务抽象)

```
shared-ai/
├── llm/
│   ├── LlmProvider.java            # LLM 提供者接口
│   ├── QwenProvider.java           # 通义千问
│   └── TokenCounter.java           # Token 计数
│
└── rag/
    ├── VectorStore.java            # 向量存储
    ├── RagService.java             # RAG 检索
    └── DocumentChunker.java       # 文档分块
```

### 4. shared-infra (基础设施)

```
shared-infra/
├── storage/
│   ├── StorageService.java         # 存储接口
│   └── MinioStorageService.java    # MinIO 实现
│
├── messaging/
│   ├── MessageProducer.java        # 消息生产者
│   └── KafkaProducerService.java   # Kafka 实现
│
├── cache/
│   ├── CacheService.java           # 缓存接口
│   └── RedisCacheService.java      # Redis 实现
│
└── websocket/
    └── WebSocketHandler.java       # WebSocket 处理
```

---

## 📋 详细重构步骤

### 阶段 1: 创建共享模块 (1-2天)

```bash
# 创建共享模块目录结构
mkdir -p shared/shared-auth/src/main/java/com/codereview/shared/auth
mkdir -p shared/shared-agent/src/main/java/com/codereview/shared/agent
mkdir -p shared/shared-ai/src/main/java/com/codereview/shared/ai
mkdir -p shared/shared-infra/src/main/java/com/codereview/shared/infra
mkdir -p shared/shared-common/src/main/java/com/codereview/shared/common
```

### 阶段 2: 重命名业务模块 (1天)

```bash
# 重命名 codereview → code-intelligence
mv codereview-api code-intelligence-api
mv codereview-domain ci-domain
mv codereview-infrastructure ci-infrastructure

# 重命名 knowledge-base → knowledge-mentor
mv knowledge-base-api knowledge-mentor-api
mv knowledge-base-core km-domain
```

### 阶段 3: 抽取重复代码 (1-2天)

- 认证代码 → shared-auth
- AI 服务 → shared-ai
- Agent 逻辑 → shared-agent
- 基础设施 → shared-infra

### 阶段 4: 更新依赖关系 (1天)

- 两个业务模块依赖共享模块
- 移除重复依赖
- 统一版本管理

### 阶段 5: 前端重构 (1天)

- 重命名组件和页面
- 更新 API 路径
- 更新路由配置

### 阶段 6: 配置统一 (0.5天)

- 统一环境变量
- 更新代理配置
- 端口标准化

---

## 📅 执行时间表

| 阶段 | 任务 | 时间 | 优先级 |
|------|------|------|--------|
| 1 | 创建 shared-* 模块 | 1-2天 | 高 |
| 2 | 重命名业务模块 | 1天 | 高 |
| 3 | 抽取重复代码 | 1-2天 | 高 |
| 4 | 更新依赖关系 | 1天 | 高 |
| 5 | 前端重构 | 1天 | 中 |
| 6 | 配置统一 | 0.5天 | 中 |
| 7 | 集成测试 | 1天 | 高 |

**总计**: 7-9 天 (单人) 或 3-4 天 (Agent Team)

---

## 🔧 配置与依赖分析 (Integration Lead)

### 当前配置清单

#### 端口配置

| 服务 | 端口 | 说明 |
|------|------|------|
| knowledge-base-api | 8080 | 知识库后端服务 |
| codereview-api | 8081 | 代码审查后端服务 |
| frontend (vite) | 5173 | 前端开发服务器 |

#### 前端 API 代理配置 (vite.config.ts)

```typescript
// 知识库服务路由 (port 8080)
/api/documents  → http://localhost:8080
/api/qa         → http://localhost:8080
/api/search     → http://localhost:8080
/api/ws         → http://localhost:8080 (WebSocket)

// 代码审查服务路由 (port 8081)
/api/*          → http://localhost:8081 (所有其他请求)
```

#### 后端配置对比

| 配置项 | knowledge-base-api | codereview-api | 状态 |
|--------|-------------------|----------------|------|
| 数据库名 | knowledge_base | knowledge_base | ✅ 共享 |
| Kafka端口 | 9093 | 9093 | ✅ 共享 |
| Redis端口 | 6379 | 6379 | ✅ 共享 |
| Elasticsearch | 9200 | - | ⚠️ 仅 KB |
| MinIO | 9000 | 9000 | ✅ 共享 |
| JWT_SECRET | 7xC6H... | 默认值 | ❌ 不一致 |
| Qwen API | ✅ | ✅ | ✅ 共享 |

### 配置冲突点

#### 1. JWT Secret 不一致

```yaml
# knowledge-base-api
jwt:
  secret: ${JWT_SECRET:7xC6H4acpIyLKfg1yKZzZZlbOrrbFhEQGIqh8kYhRfxtX+f7xWGyFcGyRjsfUVGHVm/dgysYInm33Bmidfc1mw==}

# codereview-api
jwt:
  secret: ${JWT_SECRET:your-very-secure-secret-key-at-least-32-characters-long-for-security}
```

**影响**: 两个服务生成的 JWT token 不互通，用户无法跨服务访问

#### 2. MinIO Bucket 名称不一致

```yaml
# knowledge-base-api
minio:
  bucket-name: ${MINIO_BUCKET_NAME:knowledge-base-documents}

# codereview-api
minio:
  project-bucket-name: ${MINIO_PROJECT_BUCKET:code-review-projects}
```

**影响**: 文件存储隔离，无法共享

#### 3. Kafka Topic 名称差异

```yaml
# knowledge-base-api
kafka:
  topic:
    document-processing: document-processing
    code-review-processing: code-review-processing

# codereview-api
kafka:
  topic:
    code-review: code-review-processing
```

**影响**: Topic 名称不统一

### 建议的统一配置方案

#### 新的 .env 结构

```bash
# ===== 服务端口配置 =====
SERVER_PORT_KB=8080
SERVER_PORT_CI=8081
SERVER_PORT_FRONTEND=5173

# ===== 数据库配置 (共享) =====
DB_HOST=localhost
DB_PORT=5432
DB_NAME=ai_mentor_system
DB_USER=ai_user
DB_PASSWORD=ai_password

# ===== 共享基础设施配置 =====
KAFKA_BOOTSTRAP_SERVERS=localhost:9093
KAFKA_TOPIC_DOCUMENT_PROCESSING=document-processing
KAFKA_TOPIC_CODE_REVIEW=code-review-processing
KAFKA_TOPIC_PROJECT_ANALYSIS=project-analysis

REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=

ELASTICSEARCH_URIS=http://localhost:9200
ELASTICSEARCH_INDEX_DOCUMENTS=ai_mentor_documents
ELASTICSEARCH_INDEX_CODE_CHUNKS=ai_mentor_code_chunks

MINIO_ENDPOINT=http://localhost:9000
MINIO_ACCESS_KEY=minioadmin
MINIO_SECRET_KEY=minioadmin
MINIO_BUCKET_DOCUMENTS=ai-mentor-documents
MINIO_BUCKET_PROJECTS=ai-mentor-projects

# ===== AI 配置 (共享) =====
QWEN_API_KEY=sk-xxx
QWEN_API_URL=https://dashscope.aliyuncs.com/compatible-mode/v1
QWEN_EMBEDDING_MODEL=text-embedding-v4
QWEN_CHAT_MODEL=qwen-turbo-2025-04-28

# ===== 安全配置 (共享) =====
JWT_SECRET=7xC6H4acpIyLKfg1yKZzZZlbOrrbFhEQGIqh8kYhRfxtX+f7xWGyFcGyRjsfUVGHVm/dgysYInm33Bmidfc1mw==
JWT_EXPIRATION=86400000

# ===== CORS 配置 =====
CORS_ALLOWED_ORIGINS=http://localhost:5173,http://localhost:3000
WEBSOCKET_ALLOWED_ORIGINS=http://localhost:5173,http://localhost:3000

# ===== 前端配置 =====
VITE_API_BASE_URL=http://localhost:8080
VITE_CI_API_BASE_URL=http://localhost:8081
```

#### 更新后的前端代理配置

```typescript
// vite.config.ts (重构后)
export default defineConfig({
  server: {
    port: 5173,
    proxy: {
      // Knowledge Mentor API (documents, qa, lessons, exercises)
      '/api/km': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        ws: true,
        rewrite: (path) => path.replace(/^\/api\/km/, '/api')
      },

      // Code Intelligence API (review, project, teaching)
      '/api/ci': {
        target: 'http://localhost:8081',
        changeOrigin: true,
        ws: true,
        rewrite: (path) => path.replace(/^\/api\/ci/, '/api')
      },

      // Shared Auth Service (统一认证)
      '/api/auth': {
        target: 'http://localhost:8082',
        changeOrigin: true,
      },

      // WebSocket (统一入口)
      '/api/ws': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        ws: true,
      },
    },
  },
})
```

### 环境变量清单

#### 必需变量 (生产环境)

| 变量名 | 说明 | 默认值 |
|--------|------|--------|
| DB_PASSWORD | 数据库密码 | ❌ 必填 |
| QWEN_API_KEY | 通义千问 API Key | ❌ 必填 |
| JWT_SECRET | JWT 签名密钥 | ❌ 必填 (生产) |

#### 可选变量 (开发环境)

| 变量名 | 说明 | 默认值 |
|--------|------|--------|
| SERVER_PORT_KB | Knowledge Mentor 端口 | 8080 |
| SERVER_PORT_CI | Code Intelligence 端口 | 8081 |
| REDIS_PASSWORD | Redis 密码 | (空) |
| ELASTICSEARCH_USERNAME | ES 用户名 | (空) |

### 测试验证计划

#### 阶段 1: 配置迁移验证

```bash
# 1. 备份当前配置
cp .env .env.backup

# 2. 应用新配置
cp env.production .env

# 3. 验证服务启动
cd backend/knowledge-base-api && mvn spring-boot:run
cd backend/codereview-api && mvn spring-boot:run

# 4. 检查健康状态
curl http://localhost:8080/api/actuator/health
curl http://localhost:8081/api/actuator/health
```

#### 阶段 2: 跨服务认证测试

```bash
# 1. 在 Knowledge Base 登录
LOGIN_RESPONSE=$(curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"password123"}')

TOKEN=$(echo $LOGIN_RESPONSE | jq -r '.token')

# 2. 使用同一 Token 访问 Code Review
curl http://localhost:8081/api/review/history \
  -H "Authorization: Bearer $TOKEN"

# 预期: 200 OK (无 401/403)
```

#### 阶段 3: 前端集成测试

```bash
# 1. 启动前端
cd frontend && npm run dev

# 2. 测试流程
# - 登录 → Token 保存
# - 访问知识库页面 → /api/documents (8080)
# - 访问代码审查页面 → /api/review (8081)
# - 上传项目文件 → MinIO bucket
# - AI 分析 → Kafka 消息处理

# 3. 检查网络面板
# 验证所有 API 请求正确路由到对应端口
```

#### 阶段 4: 共享服务测试

```bash
# 测试共享 MinIO
curl http://localhost:8080/api/documents/upload
curl http://localhost:8081/api/project/upload
# 验证: 同一 MinIO 实例，不同 bucket

# 测试共享 Redis
redis-cli keys "*"
# 验证: 缓存键以服务名前缀区分

# 测试共享 Kafka
docker exec kb-kafka kafka-topics.sh --list \
  --bootstrap-server localhost:9092
# 验证: Topic 统一命名
```

---

## 🚀 下一步

重构计划已生成完毕。现在你可以选择：

1. **开始执行重构** - 我可以协助逐步执行
2. **创建可执行的脚本** - 生成自动化脚本
3. **先修复当前问题** - 继续解决 teaching 403 问题

你希望先做哪个？