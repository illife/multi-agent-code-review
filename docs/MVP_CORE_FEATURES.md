# 企业知识库MVP核心功能总结

## 已完成的核心功能 ✅

### 1. 文档上传管理 ✅
- 普通上传
- 秒传功能（MD5去重）
- 分片上传（大文件支持）
- 文档列表查询
- 文档删除
- 文档详情查看

### 2. 混合搜索引擎 ✅
- BM25关键词搜索
- KNN向量语义搜索
- RRF融合算法
- 高级筛选（部门、标签、文件类型、日期范围）
- 搜索历史记录
- 热门搜索统计
- 完整分页支持
- Elasticsearch原生高亮

### 3. 文档预览下载 ✅
- 文本文件在线预览
- PDF文件预览
- 所有文件类型下载
- 中文文件名支持

### 4. AI智能问答 ✅
- WebSocket流式问答
- RAG检索增强生成
- 来源引用展示
- 实时答案生成

---

## 剩余核心功能介绍

### 5. 用户认证与权限管理

#### 功能描述
- JWT token认证
- 基于角色的访问控制（RBAC）
- 文档级权限管理
- 用户注册登录

#### 核心API

```java
// 用户登录
POST /api/auth/login
Request: { "username": "user1", "password": "password" }
Response: { "code": 200, "data": { "token": "jwt_token", "user": {...} } }

// 获取当前用户信息
GET /api/auth/me
Headers: Authorization: Bearer {token}

// 用户注册
POST /api/auth/register
Request: { "username": "newuser", "password": "pass", "email": "user@example.com" }
```

#### 前端实现
```typescript
// 登录表单提交
const handleLogin = async (values: LoginFormValues) => {
  const response = await authService.login(values.username, values.password)
  localStorage.setItem('token', response.token)
  setUser(response.user)
  navigate('/')
}
```

#### 权限模型
```java
// 文档权限
public enum Permission {
    OWNER,      // 所有者：完全控制
    VIEW,       // 查看者：只能查看
    EDIT,       // 编辑者：可编辑
    DELETE      // 删除者：可删除
}

// 权限检查
@PreAuthorize("hasPermission(#documentId, 'VIEW')")
public Document getDocument(Long documentId) {
    return documentService.getDocumentById(documentId);
}
```

#### 面试考点
1. **JWT原理与流程**
   - Header.Payload.Signature 结构
   - 无状态认证的优势
   - Token刷新机制

2. **RBAC权限模型**
   - 用户-角色-权限三层模型
   - @PreAuthorize注解原理
   - 数据级权限控制

3. **Spring Security配置**
   - SecurityFilterChain配置
   - 自定义认证过滤器
   - CORS跨域处理

---

### 6. 文档处理流程（异步化）

#### 功能描述
基于Kafka的异步文档处理流程：
1. 文档上传后立即返回
2. 发布消息到Kafka队列
3. Consumer异步处理：解析→分块→向量化→索引
4. WebSocket通知前端处理进度

#### 处理流程图

```
┌─────────┐
│ 用户上传 │
└────┬────┘
     │
     ▼
┌─────────────┐
│ 返回文档ID  │ 立即返回
└─────┬───────┘
      │
      ▼ (发送Kafka消息)
┌──────────────┐
│ Kafka Queue  │
└───────┬──────┘
        │
        ▼ (异步消费)
┌──────────────────┐
│ DocumentConsumer  │
└────┬─────────┘
     │
     ├─→ 文件解析
     ├─→ 文本分块
     ├─→ 向量化
     └─→ ES索引
```

#### 核心代码

**生产者（上传时）：**
```java
@PostMapping("/upload")
public Result<Map<String, Object>> uploadDocument(...) {
    // 1. 创建文档记录
    Document document = documentService.uploadDocument(...);

    // 2. 发送Kafka消息（异步处理）
    kafkaTemplate.send("document-processing", document.getId().toString());

    // 3. 立即返回（不等待处理完成）
    return Result.success(result);
}
```

**消费者（异步处理）：**
```java
@KafkaListener(topics = "document-processing")
public void handleDocumentUpload(String documentIdStr) {
    Long documentId = Long.parseLong(documentIdStr);

    // 1. 解析文档
    String content = documentService.parseDocument(document);

    // 2. 分块处理
    List<DocumentChunk> chunks = documentService.chunkDocument(content, document);

    // 3. 向量化
    float[][] embeddings = vectorEmbeddingService.generateEmbeddingsBatch(...);

    // 4. 索引到Elasticsearch
    elasticsearchService.bulkIndexChunks(chunks);

    // 5. 更新文档状态
    documentService.updateDocumentStatus(documentId, DocumentStatus.INDEXED);
}
```

#### WebSocket进度通知
```java
@Component
public class DocumentProcessingNotifier {
    private final SimpMessagingTemplate messagingTemplate;

    public void notifyProgress(Long documentId, String status, int progress) {
        messagingTemplate.convertAndSend(
            "/topic/document-progress/" + documentId,
            new ProgressMessage(documentId, status, progress)
        );
    }
}
```

#### 面试考点
1. **Kafka消息队列**
   - 生产者-消费者模型
   - 消息顺序性保证
   - 消费者组管理
   - 死信队列（DLQ）配置

2. **异步处理模式**
   - 同步vs异步的选择
   - 最终一致性保证
   - 补偿机制设计

3. **WebSocket实时通信**
   - WebSocket协议握手
   - STOMP消息协议
   - 消息广播与点对点

---

### 7. 向量嵌入与相似度计算

#### 功能描述
- 使用Qwen（通义千问）API生成文本向量
- 1536维向量表示（v2模型）或1024维（v4模型）
- 余弦相似度计算
- 批量向量化优化

#### 核心实现

**向量生成服务：**
```java
@Service
public class VectorEmbeddingServiceImpl implements VectorEmbeddingService {

    @Value("${qwen.embedding-model:text-embedding-v2}")
    private String model;

    @Override
    public float[] generateEmbedding(String text) throws Exception {
        // 1. 构建请求
        Map<String, Object> request = Map.of(
            "model", model,
            "input", text
        );

        // 2. 调用千问API
        String response = webClientBuilder.build()
            .post()
            .uri(apiUrl + "/embeddings")
            .header("Authorization", "Bearer " + apiKey)
            .bodyValue(request)
            .retrieve()
            .bodyToMono(String.class)
            .block();

        // 3. 解析响应
        JsonNode root = objectMapper.readTree(response);
        JsonNode embedding = root.path("data").get(0).path("embedding");

        // 4. 转换为float数组
        float[] vector = new float[embedding.size()];
        for (int i = 0; i < embedding.size(); i++) {
            vector[i] = (float) embedding.get(i).asDouble();
        }

        return vector;
    }

    @Override
    public List<float[]> generateEmbeddingsBatch(List<String> texts) {
        // 批量处理，提高效率
        Map<String, Object> request = Map.of(
            "model", model,
            "input", texts  // 一次处理多个文本
        );
        // ... 批量API调用
    }
}
```

**余弦相似度：**
```java
public double cosineSimilarity(float[] vector1, float[] vector2) {
    double dotProduct = 0.0;
    double norm1 = 0.0;
    double norm2 = 0.0;

    for (int i = 0; i < vector1.length; i++) {
        dotProduct += vector1[i] * vector2[i];
        norm1 += vector1[i] * vector1[i];
        norm2 += vector2[i] * vector2[i];
    }

    return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
}
```

#### 向量数据库选型对比

| 方案 | 优点 | 缺点 | 适用场景 |
|------|------|------|----------|
| Elasticsearch | 集成搜索+向量 | 性能不如专用向量DB | 中小规模（<1000万） |
| Milvus | 高性能、可扩展 | 运维复杂 | 大规模（>1000万） |
| Pinecone | 云原生、易扩展 | 成本高 | 云原生架构 |
| pgvector | PostgreSQL插件 | 性能一般 | 小规模、简单场景 |

#### 面试考点
1. **向量化原理**
   - Word2Vec vs BERT vs Embedding API
   - 向量维度选择
   - 相似度计算方法

2. **Elasticsearch KNN搜索**
   - dense_vector字段类型
   - kNN查询参数配置
   - HNSW索引算法

3. **RAG架构设计**
   - Retrieval（检索）阶段
   - Augmentation（增强）阶段
   - Generation（生成）阶段

---

### 8. 系统监控与性能优化

#### 功能描述
- Spring Boot Actuator健康检查
- Redis分布式缓存
- 日志系统配置
- API访问频率限制

#### 健康检查端点

```java
// application.yml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  health:
    kafka:
      enabled: true
    redis:
      enabled: true
```

```bash
# 健康检查
curl http://localhost:8080/api/actuator/health

# 返回示例
{
  "status": "UP",
  "components": {
    "kafka": { "status": "UP" },
    "redis": { "status": "UP" },
    "elasticsearch": { "status": "UP" }
  }
}
```

#### Redis缓存配置

```java
@Cacheable(value = "documents", key = "#documentId")
public Document getDocumentById(Long documentId) {
    return documentRepository.findById(documentId);
}

@CacheEvict(value = "documents", key = "#documentId")
public void deleteDocument(Long documentId) {
    documentRepository.deleteById(documentId);
}
```

#### 频率限制实现

```java
@Component
public class RateLimitService {
    private final LoadingCache<String, RateLimiter> cache;

    public RateLimitService() {
        this.cache = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.HOURS)
            .build(new CacheLoader<String, RateLimiter>() {
                @Override
                public RateLimiter load(String key) {
                    return RateLimiter.create(10.0); // 每秒10个请求
                }
            });
    }

    public boolean tryAcquire(String key) {
        return cache.get(key).tryAcquire();
    }
}
```

#### 面试考点
1. **分布式缓存**
   - Redis vs Memcached
   - 缓存穿透、击穿、雪崩
   - 缓存更新策略

2. **性能优化**
   - 数据库索引设计
   - SQL慢查询优化
   - 连接池配置

3. **监控告警**
   - Prometheus指标采集
   - Grafana可视化
   - 告警规则配置

---

## 技术栈总览

### 后端技术栈
```
核心框架: Spring Boot 3.2
  ├─ Spring Data JPA (持久化)
  ├─ Spring Security (认证授权)
  ├─ Spring WebSocket (实时通信)
  ├─ Spring Kafka (消息队列)
  └─ Spring Cache (缓存)

数据存储:
  ├─ PostgreSQL 15.7 (关系数据库)
  ├─ Elasticsearch 8.11 (搜索引擎)
  ├─ Redis 7.2.5 (缓存)
  └─ MinIO (对象存储)

中间件:
  ├─ Kafka 7.5.3 (消息队列)
  └─ Qwen AI (AI服务)

工具库:
  ├─ Lombok (简化代码)
  ├─ MapStruct (对象映射)
  └─ Hutool (工具类)
```

### 前端技术栈
```
框架: React 18 + TypeScript
  ├─ Vite (构建工具)
  └─ Ant Design (UI组件)

状态管理: Redux Toolkit
  ├─ @reduxjs/toolkit
  └─ react-redux

HTTP客户端: Axios
  └─ 拦截器封装

实时通信: WebSocket
  └─ 自定义Hook封装

路由: React Router v6
```

### 基础设施
```
容器化: Docker + Docker Compose
  ├─ PostgreSQL
  ├─ Elasticsearch
  ├─ Redis
  ├─ Kafka
  └─ MinIO

构建工具: Maven
  ├─ 多模块项目结构
  └─ 依赖管理
```

---

## MVP功能完成度

| 功能模块 | 完成度 | 说明 |
|---------|-------|------|
| 用户认证 | ✅ 100% | JWT登录、注册、权限验证 |
| 文档管理 | ✅ 100% | 上传、列表、详情、删除 |
| 搜索功能 | ✅ 100% | BM25、KNN、混合搜索、筛选、历史 |
| 文档预览 | ✅ 100% | 文本、PDF预览、下载 |
| AI问答 | ✅ 100% | WebSocket流式问答、RAG |
| 异步处理 | ✅ 100% | Kafka异步处理文档 |
| 权限管理 | ✅ 80% | 基础RBAC，文档级权限部分实现 |
| 用户管理 | ✅ 60% | 基础CRUD，缺少用户管理界面 |

---

## 剩余优化方向

### 短期优化（1-2周）
1. **用户管理界面** - 完善用户CRUD操作
2. **权限管理界面** - 可视化权限分配
3. **文档编辑功能** - 在线编辑元数据

### 中期优化（1个月）
1. **Office文档预览** - 支持Word/Excel/PPT
2. **全文搜索优化** - 搜索结果相关性调优
3. **批量操作** - 批量上传、删除、权限设置

### 长期优化（2-3个月）
1. **多租户支持** - 企业级隔离
2. **审计日志** - 完整操作记录
3. **数据统计** - 使用情况分析
4. **移动端适配** - 响应式布局优化

---

## 总结

这个企业知识库系统已经具备了完整的MVP功能，核心流程全部打通：

```
用户上传文档 → 异步处理索引 → 用户搜索 → AI问答 → 查看文档
     ↓                                              ↑
  Kafka消息队列                                  RAG检索
     ↓                                              ↑
  Elasticsearch向量索引 ←───── Qwen AI服务
```

**系统特点：**
1. **高可用** - 异步处理、分布式存储
2. **智能化** - AI问答、语义搜索
3. **易扩展** - 微服务架构、模块化设计
4. **生产就绪** - 完善的监控、日志、缓存

这个项目作为面试案例，可以展示：
- 全栈开发能力（Java + React）
- 系统设计能力（架构选型、性能优化）
- 问题解决能力（异步处理、搜索优化）
- 工程实践能力（代码质量、工程化）
