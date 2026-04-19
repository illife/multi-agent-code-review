# 包名映射指南

## 🎯 重构后的包结构

```
com.think.platform
├── shared                   # 共享模块
│   ├── common              # 通用类
│   │   ├── result          # Result, ResultCode
│   │   └── exception       # 异常类
│   ├── security            # 安全认证
│   │   ├── JwtTokenProvider
│   │   ├── JwtAuthenticationFilter
│   │   ├── SharedUserDetails
│   │   └── SecurityUtils
│   ├── infrastructure      # 基础设施
│   │   └── minio          # MinIO 存储
│   ├── agent              # Agent 框架
│   │   ├── core           # Agent 接口
│   │   └── orchestration  # 编排器
│   └── ai                 # AI 服务
│       └── llm            # LLM 提供者
│
├── ci                      # Code Intelligence (代码审查)
│   ├── api                # REST API
│   ├── domain             # 领域模型
│   │   ├── entity         # 实体类
│   │   ├── repository     # 仓库接口
│   │   └── service        # 业务服务
│   └── infrastructure     # 基础设施实现
│
└── km                      # Knowledge Mentor (知识库教学)
    ├── api                # REST API
    ├── domain             # 领域模型
    │   ├── entity         # 实体类
    │   ├── repository     # 仓库接口
    │   └── service        # 业务服务
    └── infrastructure     # 基础设施实现
```

---

## 📋 详细包名映射表

### 1. Code Intelligence (原 codereview)

| 原包名 | 新包名 | 说明 |
|--------|--------|------|
| `com.codereview.ai.api.controller` | `com.think.platform.ci.api.controller` | REST 控制器 |
| `com.codereview.ai.api.service` | `com.think.platform.ci.api.service` | API 服务 |
| `com.codereview.ai.api.config` | `com.think.platform.ci.config` | 配置类 |
| `com.codereview.ai.api.security` | `com.think.platform.ci.security` | 安全类 |
| `com.codereview.ai.domain.entity` | `com.think.platform.ci.domain.entity` | 实体类 |
| `com.codereview.ai.domain.repository` | `com.think.platform.ci.domain.repository` | 仓库接口 |
| `com.codereview.ai.domain.service` | `com.think.platform.ci.domain.service` | 业务服务 |
| `com.codereview.ai.infrastructure` | `com.think.platform.ci.infrastructure` | 基础设施 |
| `com.codereview.ai.common` | `com.think.platform.ci.common` | CI 通用类 |
| `com.codereview.ai.agent` | `com.think.platform.ci.agent` | Agent 实现 |

### 2. Knowledge Mentor (原 knowledge-base)

| 原包名 | 新包名 | 说明 |
|--------|--------|------|
| `com.company.kb.api.controller` | `com.think.platform.km.api.controller` | REST 控制器 |
| `com.company.kb.api.service` | `com.think.platform.km.api.service` | API 服务 |
| `com.company.kb.api.config` | `com.think.platform.km.config` | 配置类 |
| `com.company.kb.api.security` | `com.think.platform.km.security` | 安全类 |
| `com.company.kb.api.dto` | `com.think.platform.km.api.dto` | DTO 类 |
| `com.company.kb.core.domain` | `com.think.platform.km.domain.entity` | 实体类 |
| `com.company.kb.core.repository` | `com.think.platform.km.domain.repository` | 仓库接口 |
| `com.company.kb.core.service` | `com.think.platform.km.domain.service` | 业务服务 |
| `com.company.kb.infrastructure` | `com.think.platform.km.infrastructure` | 基础设施 |
| `com.company.kb.common` | `com.think.platform.km.common` | KM 通用类 |

### 3. 共享模块 (新创建)

| 功能 | 包名 | 类 |
|------|------|-----|
| 统一响应 | `com.think.platform.shared.common.result` | Result, ResultCode |
| 异常类 | `com.think.platform.shared.common.exception` | BaseException, BusinessException |
| JWT | `com.think.platform.shared.security` | JwtTokenProvider, JwtAuthenticationFilter |
| 用户详情 | `com.think.platform.shared.security` | SharedUserDetails |
| 工具类 | `com.think.platform.shared.security` | SecurityUtils |
| Agent | `com.think.platform.shared.agent.core` | Agent, AgentType, AgentRequest |
| 编排 | `com.think.platform.shared.agent.orchestration` | AgentOrchestrator |
| LLM | `com.think.platform.shared.ai.llm` | LlmProvider, ChatRequest |
| MinIO | `com.think.platform.shared.infra.minio` | MinioStorageService |

---

## 🔍 具体文件位置示例

### Code Intelligence API

```
code-intelligence-api/src/main/java/com/think/platform/ci/
├── CodeIntelligenceApplication.java     # 主类
├── api/
│   ├── controller/
│   │   ├── AuthController.java          # 认证控制器
│   │   ├── CodeReviewController.java    # 代码审查控制器
│   │   ├── ProjectController.java       # 项目控制器
│   │   └── TeachingController.java     # 教学控制器
│   ├── service/
│   │   └── ReviewService.java           # 审查服务
│   └── config/
│       └── SecurityConfig.java          # 安全配置
└── security/
    ├── JwtTokenProvider.java            # (可以用 shared)
    └── CustomUserDetailsService.java    # (可以用 shared)
```

### Knowledge Mentor API

```
knowledge-mentor-api/src/main/java/com/think/platform/km/
├── KnowledgeMentorApplication.java     # 主类
├── api/
│   ├── controller/
│   │   ├── DocumentController.java     # 文档控制器
│   │   ├── QAController.java           # 问答控制器
│   │   ├── LessonController.java       # 课程控制器
│   │   └── ExerciseController.java     # 练习控制器
│   ├── dto/
│   │   ├── LoginRequest.java           # DTO 类
│   │   └── AuthResponse.java
│   └── config/
│       ├── SecurityConfig.java         # 安全配置
│       └── ElasticsearchConfig.java    # ES 配置
└── security/
    └── (建议使用 shared-security)
```

### 共享模块

```
shared/shared-common/src/main/java/com/think/platform/shared/common/
├── result/
│   ├── Result.java                      # 统一响应
│   └── ResultCode.java                 # 响应码
└── exception/
    ├── BaseException.java              # 基础异常
    ├── BusinessException.java           # 业务异常
    └── ResourceNotFoundException.java   # 资源不存在异常

shared/shared-security/src/main/java/com/think/platform/shared/security/
├── JwtTokenProvider.java               # JWT 提供者
├── JwtAuthenticationFilter.java        # JWT 过滤器
├── SharedUserDetails.java              # 用户详情
└── SecurityUtils.java                 # 安全工具类

shared/shared-agent/src/main/java/com/think/platform/shared/agent/
├── core/
│   ├── Agent.java                      # Agent 接口
│   ├── AgentType.java                  # Agent 类型
│   ├── AgentRequest.java               # Agent 请求
│   └── AgentResult.java                # Agent 结果
└── orchestration/
    ├── AgentOrchestrator.java          # Agent 编排器
    └── ExecutionStrategy.java          # 执行策略

shared/shared-ai/src/main/java/com/think/platform/shared/ai/
└── llm/
    ├── LlmProvider.java                # LLM 提供者接口
    ├── LlmProviderType.java            # LLM 类型
    ├── ChatRequest.java                # 对话请求
    ├── ChatResponse.java               # 对话响应
    └── ModelInfo.java                  # 模型信息

shared/shared-infrastructure/src/main/java/com/think/platform/shared/infra/
└── minio/
    ├── MinioProperties.java            # MinIO 配置
    ├── MinioConfig.java                # MinIO Bean
    └── MinioStorageService.java        # MinIO 服务
```

---

## 📝 import 语句示例

### 使用共享模块的 import

```java
// 统一响应
import com.think.platform.shared.common.result.Result;
import com.think.platform.shared.common.result.ResultCode;
import com.think.platform.shared.common.exception.BusinessException;

// 安全认证
import com.think.platform.shared.security.JwtTokenProvider;
import com.think.platform.shared.security.SharedUserDetails;
import com.think.platform.shared.security.SecurityUtils;

// Agent 框架
import com.think.platform.shared.agent.core.Agent;
import com.think.platform.shared.agent.core.AgentType;
import com.think.platform.shared.agent.orchestration.AgentOrchestrator;

// AI 服务
import com.think.platform.shared.ai.llm.LlmProvider;
import com.think.platform.shared.ai.llm.ChatRequest;

// 基础设施
import com.think.platform.shared.infra.minio.MinioStorageService;
```

### Code Intelligence 内部 import

```java
// 实体类
import com.think.platform.ci.domain.entity.Review;
import com.think.platform.ci.domain.entity.Project;

// 仓库
import com.think.platform.ci.domain.repository.ReviewRepository;

// 服务
import com.think.platform.ci.domain.service.ReviewService;
```

### Knowledge Mentor 内部 import

```java
// 实体类
import com.think.platform.km.domain.entity.Document;
import com.think.platform.km.domain.entity.Lesson;

// 仓库
import com.think.platform.km.domain.repository.DocumentRepository;

// 服务
import com.think.platform.km.domain.service.DocumentService;
```

---

## ⚠️ 注意事项

1. **共享模块优先**: 如果功能在 shared-* 模块中已实现，优先使用共享模块
2. **避免循环依赖**: CI 和 KM 模块不应相互依赖
3. **包名规范**: 全部使用 `com.think.platform` 作为基础包名
4. **目录结构**: 理想情况下目录应与包名一致（但目前包名已更新，目录可后续调整）
