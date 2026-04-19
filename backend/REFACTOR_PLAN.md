# Backend Refactoring Plan

**Date**: 2026-04-13
**Status**: Draft
**Author**: Software Architect Agent

---

## Executive Summary

This document outlines a comprehensive refactoring plan to consolidate three separate backend modules (codereview-*, knowledge-base-*, auth-service) into a unified platform architecture with two core domains: **code-intelligence** and **knowledge-mentor**.

### Current State
- 3 independent module groups with overlapping dependencies
- Inconsistent package naming conventions
- Duplicate common utilities across modules
- No shared infrastructure layer

### Target State
- Unified platform with shared common/infrastructure modules
- Clear domain boundaries (code-intelligence, knowledge-mentor)
- Consistent package naming and organization
- Single source of truth for cross-cutting concerns

---

## 1. Current Module Inventory

### 1.1 CodeReview AI Modules (codereview-*)
**Group ID**: `com.codereview.ai`
**Parent**: `codereview-ai-parent` (codereview-ai/pom.xml)
**Version**: 1.0.0-SNAPSHOT
**Java Files**: ~140

| Module | Artifact ID | Purpose | Java Files | Package |
|--------|-------------|---------|------------|---------|
| Parent | codereview-ai-parent | Parent POM | - | - |
| Common | codereview-common | Shared utilities, DTOs, exceptions | 29 | com.codereview.ai.common |
| Domain | codereview-domain | Domain models, agents, business logic | 102 | com.codereview.ai.domain |
| Infrastructure | codereview-infrastructure | External integrations | 4 | com.codereview.ai.infrastructure |
| API | codereview-api | REST layer, entry point | 5 | com.codereview.ai.api |

**Entry Point**: `com.codereview.ai.api.CodeReviewApplication`
**Main Port**: 8080 (default)

**Key Domain Classes**:
- `com.codereview.ai.domain.agent` - Agent framework
- `com.codereview.ai.domain.agent.inspector` - Code inspection agents
- `com.codereview.ai.domain.agent.mentor` - Teaching/mentor agents
- `com.codereview.ai.domain.model` - CodeReview, Project, Issue models

### 1.2 Knowledge Base Modules (knowledge-base-*)
**Group ID**: `com.company.kb`
**Parent**: `knowledge-base-system` (implicit root POM)
**Version**: 1.0.0-SNAPSHOT
**Java Files**: ~131

| Module | Artifact ID | Purpose | Java Files | Package |
|--------|-------------|---------|------------|---------|
| Common | knowledge-base-common | Shared utilities, DTOs | 21 | com.company.kb.common |
| Core | knowledge-base-core | Business logic, services | 58 | com.company.kb.core |
| Infrastructure | knowledge-base-infrastructure | ES, Qwen, document parsing | 21 | com.company.kb.infrastructure |
| API | knowledge-base-api | REST layer, entry point | 31 | com.company.kb.api |

**Entry Point**: `com.company.kb.KbApiApplication`
**Main Port**: 8080 (conflicts with codereview-api)

**Key Domain Classes**:
- `com.company.kb.core.domain` - Document, Chunk, Embedding models
- `com.company.kb.core.service` - DocumentService, QAService
- `com.company.kb.infrastructure` - Elasticsearch, Qwen AI integration

### 1.3 Auth Service Modules (auth-service/*)
**Group ID**: `com.codereview.auth`
**Parent**: `auth-service-parent` (auth-service/pom.xml)
**Version**: 1.0.0-SNAPSHOT
**Java Files**: ~10

| Module | Artifact ID | Purpose | Java Files | Package |
|--------|-------------|---------|------------|---------|
| Parent | auth-service-parent | Parent POM | - | - |
| API | auth-api | Authentication REST API | - | com.codereview.auth.api |
| Core | auth-core | Auth business logic | - | com.codereview.auth.core |
| Infrastructure | auth-infrastructure | Auth persistence | - | com.codereview.auth.infrastructure |

**Entry Point**: `com.codereview.auth.api.AuthApplication`

---

## 2. Proposed New Module Structure

### 2.1 Root Platform POM

```
backend/
  pom.xml (NEW - root parent POM)
  Group ID: com.think.platform
  Artifact ID: think-platform
  Version: 1.0.0-SNAPSHOT
```

### 2.2 Module Hierarchy

```
think-platform/
├── shared-modules/
│   ├── shared-common/          # Unified common module
│   ├── shared-infrastructure/  # Unified infrastructure
│   └── shared-security/        # Security & auth (NEW)
├── code-intelligence/          # Renamed from codereview-*
│   ├── ci-api/
│   ├── ci-domain/
│   └── ci-application/         # NEW - orchestrates domain + infra
├── knowledge-mentor/           # Renamed from knowledge-base-*
│   ├── km-api/
│   ├── km-domain/
│   └── km-application/         # NEW - orchestrates domain + infra
└── platform-gateway/           # NEW - API Gateway
```

---

## 3. Module Renaming Matrix

### 3.1 CodeReview AI -> Code Intelligence

| Old Module | New Module | Old Package | New Package |
|------------|------------|-------------|-------------|
| codereview-common | → shared-common | com.codereview.ai.common | → com.think.platform.common |
| codereview-domain | → ci-domain | com.codereview.ai.domain | → com.think.platform.ci.domain |
| codereview-infrastructure | → shared-infrastructure | com.codereview.ai.infrastructure | → com.think.platform.infrastructure |
| codereview-api | → ci-api | com.codereview.ai.api | → com.think.platform.ci.api |
| codereview-ai | → (removed, merged into root) | - | - |

### 3.2 Knowledge Base -> Knowledge Mentor

| Old Module | New Module | Old Package | New Package |
|------------|------------|-------------|-------------|
| knowledge-base-common | → shared-common | com.company.kb.common | → com.think.platform.common |
| knowledge-base-core | → km-domain | com.company.kb.core | → com.think.platform.km.domain |
| knowledge-base-infrastructure | → shared-infrastructure | com.company.kb.infrastructure | → com.think.platform.infrastructure |
| knowledge-base-api | → km-api | com.company.kb.api | → com.think.platform.km.api |

### 3.3 Auth Service -> Shared Security

| Old Module | New Module | Old Package | New Package |
|------------|------------|-------------|-------------|
| auth-service-parent | → (merged into root) | - | - |
| auth-api | → platform-gateway | com.codereview.auth.api | → com.think.platform.gateway |
| auth-core | → shared-security | com.codereview.auth.core | → com.think.platform.security |
| auth-infrastructure | → shared-security | com.codereview.auth.infrastructure | → com.think.platform.security.infrastructure |

---

## 4. Java Package Refactoring Plan

### 4.1 Unified Package Structure

```
com.think.platform
├── common                    # shared-common
│   ├── dto/
│   │   ├── ApiResponse.java
│   │   ├── PageRequest.java
│   │   └── PageResponse.java
│   ├── exception/
│   │   ├── BusinessException.java
│   │   └── GlobalExceptionHandler.java
│   ├── result/
│   │   └── Result.java
│   └── util/
│       ├── JsonUtil.java
│       └── ValidationUtil.java
├── infrastructure            # shared-infrastructure
│   ├── ai/                  # AI provider integrations
│   │   ├── qwen/
│   │   │   ├── QwenClient.java
│   │   │   └── QwenConfig.java
│   │   └── embedding/
│   │       └── EmbeddingService.java
│   ├── cache/               # Redis, Caffeine
│   │   └── RedisConfig.java
│   ├── kafka/               # Messaging
│   │   ├── KafkaConfig.java
│   │   └── KafkaProducer.java
│   ├── persistence/         # Database
│   │   └── JpaConfig.java
│   ├── storage/             # MinIO/S3
│   │   └── MinioService.java
│   └── search/              # Elasticsearch
│       ├── ElasticsearchConfig.java
│       └── ElasticsearchClient.java
├── security                 # shared-security
│   ├── auth/
│   │   ├── AuthService.java
│   │   └── JwtAuthenticationFilter.java
│   ├── jwt/
│   │   ├── JwtTokenProvider.java
│   │   └── JwtTokenValidator.java
│   └── oauth/
│       └── OAuth2Service.java
├── ci                       # code-intelligence domain
│   ├── domain/
│   │   ├── agent/
│   │   │   ├── inspector/
│   │   │   │   ├── CodeInspectorAgent.java
│   │   │   │   └── SecurityAnalyzer.java
│   │   │   ├── mentor/
│   │   │   │   ├── TeachingAgent.java
│   │   │   │   └── LearningPathGenerator.java
│   │   │   └── orchestrator/
│   │   │       └── AgentOrchestrator.java
│   │   ├── model/
│   │   │   ├── CodeReview.java
│   │   │   ├── Project.java
│   │   │   ├── Issue.java
│   │   │   └── SkillProfile.java
│   │   ├── repository/
│   │   │   ├── CodeReviewRepository.java
│   │   │   └── ProjectRepository.java
│   │   └── service/
│   │       ├── CodeAnalysisService.java
│   │       └── TeachingService.java
│   ├── application/
│   │   ├── service/
│   │   └── facade/
│   └── api/
│       ├── controller/
│       │   ├── CodeIntelligenceController.java
│       │   ├── ProjectAnalysisController.java
│       │   └── LearningController.java
│       ├── dto/
│       │   ├── CodeReviewRequest.java
│       │   ├── ProjectUploadRequest.java
│       │   └── TeachingContentRequest.java
│       └── config/
│           └── CIConfig.java
└── km                       # knowledge-mentor domain
    ├── domain/
    │   ├── document/
    │   │   ├── Document.java
    │   │   ├── DocumentType.java
    │   │   └── DocumentStatus.java
    │   ├── chunk/
    │   │   ├── DocumentChunk.java
    │   │   └── ChunkingStrategy.java
    │   ├── embedding/
    │   │   └── EmbeddingVector.java
    │   ├── retrieval/
    │   │   ├── SearchRequest.java
    │   │   └── HybridSearchService.java
    │   ├── repository/
    │   │   ├── DocumentRepository.java
    │   │   └── ChunkRepository.java
    │   └── service/
    │       ├── DocumentService.java
    │       ├── QaService.java
    │       └── SearchService.java
    ├── application/
    │   ├── service/
    │   └── facade/
    └── api/
        ├── controller/
        │   ├── DocumentController.java
        │   ├── QAController.java
        │   └── SearchController.java
        ├── dto/
        │   ├── DocumentUploadRequest.java
        │   ├── QuestionRequest.java
        │   └── SearchQueryRequest.java
        └── config/
            └── KMConfig.java
```

### 4.2 Package Migration Steps

1. **Phase 1**: Create new package structure under `com.think.platform`
2. **Phase 2**: Move common classes to `shared-common`
3. **Phase 3**: Move infrastructure to `shared-infrastructure`
4. **Phase 4**: Move security to `shared-security`
5. **Phase 5**: Refactor domain modules
6. **Phase 6**: Update API modules

---

## 5. Dependency Relationship Diagram

### 5.1 Current Dependencies

```
codereview-api
  ├─> codereview-domain
  │    └─> codereview-common
  └─> codereview-infrastructure
       └─> codereview-common

knowledge-base-api
  ├─> knowledge-base-core
  │    ├─> knowledge-base-common
  │    └─> knowledge-base-infrastructure
  │         └─> knowledge-base-common
  └─> knowledge-base-infrastructure
       └─> knowledge-base-common

auth-api
  ├─> auth-core
  │    └─> auth-infrastructure
  └─> auth-infrastructure
```

### 5.2 Target Dependencies

```
ci-api (code-intelligence/api)
  └─> ci-application
       ├─> ci-domain
       │    └─> shared-common
       └─> shared-infrastructure
            ├─> shared-common
            └─> shared-security

km-api (knowledge-mentor/api)
  └─> km-application
       ├─> km-domain
       │    └─> shared-common
       └─> shared-infrastructure
            ├─> shared-common
            └─> shared-security

platform-gateway
  ├─> shared-security
  └─> shared-infrastructure
```

---

## 6. Files to Rename

### 6.1 Directory Renames (9 directories)

```
codereview-ai/           → (DELETE - merge into root)
codereview-api/          → code-intelligence/ci-api/
codereview-domain/       → code-intelligence/ci-domain/
codereview-common/       → shared-modules/shared-common/
codereview-infrastructure/ → shared-modules/shared-infrastructure/

knowledge-base-api/      → knowledge-mentor/km-api/
knowledge-base-core/     → knowledge-mentor/km-domain/
knowledge-base-common/   → shared-modules/shared-common/ (MERGE)
knowledge-base-infrastructure/ → shared-modules/shared-infrastructure/ (MERGE)

auth-service/            → (SPLIT into shared-security/ + platform-gateway/)
```

### 6.2 Key File Renames

#### Application Classes
```
com.codereview.ai.api.CodeReviewApplication
  → com.think.platform.ci.api.CodeIntelligenceApplication

com.company.kb.KbApiApplication
  → com.think.platform.km.api.KnowledgeMentorApplication

com.codereview.auth.api.AuthApplication
  → com.think.platform.gateway.GatewayApplication
```

#### POM Files
```
codereview-ai/pom.xml                 → (DELETE)
codereview-api/pom.xml                → code-intelligence/ci-api/pom.xml
codereview-domain/pom.xml             → code-intelligence/ci-domain/pom.xml
codereview-common/pom.xml             → shared-modules/shared-common/pom.xml
codereview-infrastructure/pom.xml     → shared-modules/shared-infrastructure/pom.xml

knowledge-base-api/pom.xml            → knowledge-mentor/km-api/pom.xml
knowledge-base-core/pom.xml           → knowledge-mentor/km-domain/pom.xml
knowledge-base-common/pom.xml         → shared-modules/shared-common/pom.xml (MERGE)
knowledge-base-infrastructure/pom.xml → shared-modules/shared-infrastructure/pom.xml (MERGE)

auth-service/pom.xml                  → (DELETE)
auth-service/auth-api/pom.xml         → platform-gateway/pom.xml
auth-service/auth-core/pom.xml        → shared-modules/shared-security/pom.xml
auth-service/auth-infrastructure/pom.xml → shared-modules/shared-security/infrastructure/pom.xml
```

---

## 7. Maven Configuration Changes

### 7.1 Root POM (NEW)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.think.platform</groupId>
    <artifactId>think-platform</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>pom</packaging>

    <name>Think Platform</name>
    <description>Unified platform for code intelligence and knowledge mentorship</description>

    <modules>
        <!-- Shared Modules -->
        <module>shared-modules/shared-common</module>
        <module>shared-modules/shared-infrastructure</module>
        <module>shared-modules/shared-security</module>

        <!-- Domain Modules -->
        <module>code-intelligence/ci-api</module>
        <module>code-intelligence/ci-domain</module>
        <module>code-intelligence/ci-application</module>

        <module>knowledge-mentor/km-api</module>
        <module>knowledge-mentor/km-domain</module>
        <module>knowledge-mentor/km-application</module>

        <!-- Gateway -->
        <module>platform-gateway</module>
    </modules>

    <properties>
        <java.version>17</java.version>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>

        <!-- Spring Boot -->
        <spring-boot.version>3.2.0</spring-boot.version>

        <!-- Dependencies -->
        <lombok.version>1.18.30</lombok.version>
        <jjwt.version>0.11.5</jjwt.version>
        <spring-kafka.version>3.1.0</spring-kafka.version>
        <mapstruct.version>1.5.5.Final</mapstruct.version>
        <postgresql.version>42.6.0</postgresql.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <!-- Spring Boot BOM -->
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-dependencies</artifactId>
                <version>${spring-boot.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>

            <!-- Internal Modules -->
            <dependency>
                <groupId>com.think.platform</groupId>
                <artifactId>shared-common</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>com.think.platform</groupId>
                <artifactId>shared-infrastructure</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>com.think.platform</groupId>
                <artifactId>shared-security</artifactId>
                <version>${project.version}</version>
            </dependency>
        </dependencies>
    </dependencyManagement>
</project>
```

### 7.2 Group ID Migrations

| Old Group ID | New Group ID |
|--------------|--------------|
| com.codereview.ai | com.think.platform |
| com.company.kb | com.think.platform |
| com.codereview.auth | com.think.platform |

---

## 8. Port Allocation

| Service | Current Port | Target Port | Notes |
|---------|--------------|-------------|-------|
| ci-api (codereview-api) | 8080 | 8081 | Internal service |
| km-api (knowledge-base-api) | 8080 | 8082 | Internal service |
| platform-gateway (auth-api) | ? | 8080 | Public entry point |
| shared modules | N/A | N/A | Library modules |

---

## 9. Implementation Phases

### Phase 1: Preparation (Week 1)
- [ ] Create root POM with dependency management
- [ ] Set up new directory structure
- [ ] Create shared-modules skeleton
- [ ] Establish CI/CD pipeline updates

### Phase 2: Shared Modules (Week 2-3)
- [ ] Merge codereview-common + knowledge-base-common → shared-common
- [ ] Merge codereview-infrastructure + knowledge-base-infrastructure → shared-infrastructure
- [ ] Extract auth-core → shared-security
- [ ] Update all dependencies to use shared modules

### Phase 3: Domain Refactoring (Week 4-5)
- [ ] Rename codereview-* → code-intelligence
- [ ] Rename knowledge-base-* → knowledge-mentor
- [ ] Update package names
- [ ] Refactor domain boundaries
- [ ] Create ci-application and km-application modules

### Phase 4: Gateway Creation (Week 6)
- [ ] Create platform-gateway from auth-api
- [ ] Implement routing to ci-api and km-api
- [ ] Set up unified authentication

### Phase 5: Testing & Validation (Week 7)
- [ ] Integration tests
- [ ] Performance tests
- [ ] Documentation updates

### Phase 6: Deployment (Week 8)
- [ ] Database migration scripts
- [ ] Environment configuration updates
- [ ] Production deployment

---

## 10. Risk Assessment

| Risk | Impact | Mitigation |
|------|--------|------------|
| Breaking changes to existing APIs | High | Version APIs, maintain compatibility layer |
| Database migration conflicts | High | Plan migration scripts carefully, test in staging |
| Package refactoring tool errors | Medium | Use automated refactoring tools, manual verification |
| Dependency conflicts | Medium | Thorough testing, staged rollout |
| Team productivity impact | Low | Clear documentation, phased approach |

---

## 11. Success Criteria

1. All modules under unified `com.think.platform` group ID
2. Zero code duplication between modules
3. Clear domain boundaries (code-intelligence vs knowledge-mentor)
4. Shared modules properly abstracted
5. All tests passing after refactoring
6. Zero production incidents during migration

---

## Appendix A: File Count Summary

| Module Group | Java Files | Test Files | Resources | Total |
|--------------|------------|------------|-----------|-------|
| codereview-* | 140 | ~10 | ~15 | ~165 |
| knowledge-base-* | 131 | ~15 | ~20 | ~166 |
| auth-service | 10 | ~5 | ~5 | ~20 |
| **Total** | **281** | **~30** | **~40** | **~351** |

## Appendix B: Module Dependency Analysis

### Current Duplicate Dependencies
- Spring Boot dependencies (duplicated in 3 parent POMs)
- Lombok configuration (duplicated)
- JWT libraries (duplicated)
- PostgreSQL drivers (duplicated)
- MapStruct (duplicated)

### Post-Refactoring Benefits
- Single source of truth for dependency versions
- Consistent configuration across all modules
- Easier dependency upgrades
- Reduced build time
- Simpler onboarding for new developers

---

**Document Version**: 2.0
**Last Updated**: 2026-04-13
**Next Review**: After Phase 2 completion
