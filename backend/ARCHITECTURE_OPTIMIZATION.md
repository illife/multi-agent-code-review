# 架构优化方案
# Architecture Optimization Plan

## 当前架构问题分析

### 1. 多余的父 POM 模块
```
backend/
├── shared-parent/              ← 管理共享模块
├── code-intelligence-parent/   ← 管理代码审查服务
├── knowledge-mentor-parent/    ← 管理知识库服务
├── auth-service/               ← 独立认证服务
│   └── pom.xml (auth-service-parent)
└── codereview-ai/              ← 过时的 POM，引用不存在的模块
```

**问题：**
- 每个父 POM 都重复管理相同的依赖版本（Spring Boot, Lombok, JWT, PostgreSQL）
- `codereview-ai/pom.xml` 引用不存在的模块（codereview-common, codereview-domain, codereview-api）
- 缺少统一的根级父 POM 来管理所有版本

### 2. 模块依赖混乱
```
code-intelligence-api → shared-common, shared-security, shared-agent
                        ci-domain, ci-infrastructure

knowledge-mentor-api → shared-common, shared-security, shared-ai
                      km-domain, km-infrastructure

auth-service → auth-core, auth-infrastructure
              (但未使用 shared-security)
```

**问题：**
- auth-service 有自己的 JWT 配置，与 shared-security 重复
- ci-domain 和 km-domain 可能有共享的领域概念
- 缺少统一的依赖版本管理

### 3. 目录结构复杂
```
backend/
├── shared/                        (5个子模块)
├── code-intelligence-parent/      (父 POM，无代码)
├── code-intelligence-api/         (API 层)
├── ci-domain/                     (领域层)
├── ci-infrastructure/             (基础设施层)
├── knowledge-mentor-parent/       (父 POM，无代码)
├── knowledge-mentor-api/          (API 层)
├── km-domain/                     (领域层)
├── km-infrastructure/             (基础设施层)
├── auth-service/                  (独立服务，3个子模块)
└── codereview-ai/                 (过时)
```

## 优化方案

### 方案：统一根级父 POM + 扁平化模块结构

```
backend/
├── pom.xml (根级父 POM)
│
├── shared/                        (共享模块，无变化)
│   ├── shared-common/
│   ├── shared-security/
│   ├── shared-infrastructure/
│   ├── shared-agent/
│   └── shared-ai/
│
├── services/                      (所有服务)
│   ├── code-intelligence/         (代码审查服务)
│   │   ├── ci-api/
│   │   ├── ci-domain/
│   │   └── ci-infrastructure/
│   │
│   ├── knowledge-mentor/          (知识库服务)
│   │   ├── km-api/
│   │   ├── km-domain/
│   │   └── km-infrastructure/
│   │
│   └── auth/                      (认证服务)
│       ├── auth-api/
│       ├── auth-domain/
│       └── auth-infrastructure/
│
└── deployment/                    (部署配置)
    ├── docker/
    ├── kubernetes/
    └── migrations/
```

### 关键变更

#### 1. 创建根级父 POM

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project>
    <groupId>com.think.platform</groupId>
    <artifactId>think-platform-parent</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>pom</packaging>

    <modules>
        <module>shared</module>
        <module>services/code-intelligence</module>
        <module>services/knowledge-mentor</module>
        <module>services/auth</module>
    </modules>

    <properties>
        <java.version>17</java.version>
        <spring-boot.version>3.2.0</spring-boot.version>
        <lombok.version>1.18.30</lombok.version>
        <jjwt.version>0.12.3</jjwt.version>
        <!-- 所有版本统一管理 -->
    </properties>

    <dependencyManagement>
        <dependencies>
            <!-- 统一管理所有依赖版本 -->
        </dependencies>
    </dependencyManagement>
</project>
```

#### 2. 删除冗余文件

```bash
# 删除多余的父 POM
rm -rf code-intelligence-parent/
rm -rf knowledge-mentor-parent/
rm -rf codereview-ai/

# 删除重构脚本备份
rm -f refactor-packages.py
rm -f refactor-packages.sh
rm -f restore-backup.py
```

#### 3. 重构 auth-service 使用 shared-security

**当前：**
```xml
<!-- auth-service/pom.xml -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.11.5</version>  <!-- 版本不一致！ -->
</dependency>
```

**优化后：**
```xml
<!-- services/auth/auth-api/pom.xml -->
<dependency>
    <groupId>com.think.platform</groupId>
    <artifactId>shared-security</artifactId>
</dependency>
```

#### 4. 统一服务结构

每个服务都采用三层结构：
- `*-api`: REST API 层，包含 controllers, WebSocket handlers
- `*-domain`: 业务逻辑层，包含 entities, services, repositories
- `*-infrastructure`: 基础设施层，包含外部集成（数据库, Elasticsearch, Kafka）

## 实施步骤

### Phase 1: 创建根级父 POM (5分钟)
1. 创建 `backend/pom.xml`
2. 定义所有依赖版本
3. 配置子模块引用

### Phase 2: 重组服务目录 (10分钟)
1. 创建 `services/` 目录
2. 移动 code-intelligence-* → services/code-intelligence/
3. 移动 knowledge-mentor-* → services/knowledge-mentor/
4. 移动 auth-service/* → services/auth/
5. 更新所有 pom.xml 中的路径引用

### Phase 3: 更新 auth-service (5分钟)
1. 移除 auth-service 中的重复 JWT 配置
2. 添加 shared-security 依赖
3. 删除重复的认证代码

### Phase 4: 清理冗余文件 (2分钟)
1. 删除多余的父 POM 目录
2. 删除过时的 codereview-ai 模块
3. 删除重构脚本备份

### Phase 5: 验证编译 (5分钟)
```bash
cd backend
mvn clean install
```

## 预期效果

### 简化前
- 18 个模块目录
- 5 个父 POM，重复管理依赖
- 多个版本的相同依赖（JWT: 0.11.5, 0.12.3）
- 认证逻辑分散在多处

### 简化后
- 12 个模块目录
- 1 个根级父 POM，统一管理所有版本
- 所有依赖版本统一
- 认证逻辑集中在 shared-security

### 依赖关系图

```
think-platform-parent (根)
│
├── shared (共享模块)
│   ├── shared-common ←── 所有服务依赖
│   ├── shared-security ←── 所有服务依赖
│   ├── shared-infrastructure ←── 所有服务依赖
│   ├── shared-agent ←── code-intelligence
│   └── shared-ai ←── knowledge-mentor
│
├── code-intelligence (服务)
│   ├── ci-api
│   ├── ci-domain
│   └── ci-infrastructure
│
├── knowledge-mentor (服务)
│   ├── km-api
│   ├── km-domain
│   └── km-infrastructure
│
└── auth (服务)
    ├── auth-api
    ├── auth-domain
    └── auth-infrastructure
```

## 风险评估

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| 目录移动导致路径问题 | 中 | 使用相对路径，更新所有 pom.xml |
| auth-service 重构影响功能 | 低 | 保持接口不变，只替换内部实现 |
| 编译错误 | 低 | 逐步迁移，每步验证编译 |

## 后续优化建议

1. **统一端口配置**
   - code-intelligence: 8081
   - knowledge-mentor: 8080
   - auth: 8082 (新增)

2. **统一 API 路由前缀**
   - `/api/ci/*` - 代码审查相关
   - `/api/km/*` - 知识库相关
   - `/api/auth/*` - 认证相关

3. **考虑合并 ci-domain 和 km-domain**
   - 如果有共享的领域概念（如 User, Project）
   - 创建 shared-domain 模块

## 总结

此优化方案将：
- ✅ 减少 33% 的模块目录数量
- ✅ 消除所有重复的依赖管理
- ✅ 统一认证逻辑
- ✅ 简化新开发者理解成本
- ✅ 保持向后兼容，不影响现有功能
