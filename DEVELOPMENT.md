# 企业知识库系统 - 开发文档

## 1. 项目结构详解

### 1.1 整体目录结构

```
knowledge-base-system/
├── backend/                                    # 后端项目
│   ├── knowledge-base-api/                     # API模块
│   │   ├── src/main/java/com/company/kb/
│   │   │   ├── config/                         # 配置类
│   │   │   ├── controller/                     # REST控制器
│   │   │   ├── security/                       # 安全相关
│   │   │   ├── websocket/                      # WebSocket处理
│   │   │   └── KbApiApplication.java          # 启动类
│   │   └── src/main/resources/
│   │       └── application.yml                 # 应用配置
│   │
│   ├── knowledge-base-core/                    # 核心业务模块
│   │   ├── src/main/java/com/company/kb/core/
│   │   │   ├── consumer/                       # Kafka消费者
│   │   │   ├── domain/                         # 领域模型
│   │   │   ├── repository/                     # 数据访问
│   │   │   ├── service/                        # 业务服务
│   │   │   └── service/cache/                  # 缓存服务
│   │
│   ├── knowledge-base-infrastructure/          # 基础设施模块
│   │   ├── src/main/java/com/company/kb/infra/
│   │   │   ├── elasticsearch/                  # ES集成
│   │   │   ├── kafka/                          # Kafka配置
│   │   │   ├── qwen/                           # 千问API
│   │   │   └── document/                       # 文档解析
│   │
│   └── knowledge-base-common/                  # 公共模块
│       ├── src/main/java/com/company/kb/common/
│       │   ├── dto/                            # 数据传输对象
│       │   ├── exception/                      # 异常定义
│       │   └── result/                         # 统一响应
│
├── frontend/                                   # 前端项目
│   ├── src/
│   │   ├── hooks/                              # React Hooks
│   │   ├── components/                         # React组件
│   │   ├── pages/                              # 页面组件
│   │   ├── services/                           # API服务
│   │   ├── store/                              # Redux状态
│   │   └── types/                              # TypeScript类型
│   ├── package.json
│   └── vite.config.ts
│
├── docker-compose.yml                          # Docker编排
├── pom.xml                                     # Maven父POM
├── .env.example                                # 环境变量模板
└── README.md                                   # 项目说明
```

### 1.2 后端模块详解

#### knowledge-base-api（API层）

**职责：**
- 暴露REST API
- 处理HTTP请求/响应
- WebSocket连接管理
- 安全认证和授权
- 请求参数验证

**关键类：**

| 类名 | 职责 |
|------|------|
| `KbApiApplication.java` | Spring Boot启动类 |
| `SecurityConfig.java` | Spring Security配置 |
| `WebSocketConfig.java` | WebSocket配置 |
| `RedisConfig.java` | Redis缓存配置 |
| `CorsConfig.java` | 跨域配置 |
| `JwtAuthenticationFilter.java` | JWT认证过滤器 |
| `CustomPermissionEvaluator.java` | 权限评估器 |
| `QAController.java` | 问答接口 |
| `DocumentController.java` | 文档管理接口 |
| `SearchController.java` | 搜索接口 |
| `AuthController.java` | 认证接口 |
| `QAWebSocketHandler.java` | WebSocket处理器 |
| `WebSocketSessionManager.java` | 会话管理器 |

**配置文件：application.yml**
```yaml
spring:
  application:
    name: knowledge-base-api
  datasource:
    url: jdbc:postgresql://localhost:5432/knowledge_base
    username: kb_user
    password: kb_password
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      acks: all
      retries: 3

elasticsearch:
  uris: http://localhost:9200
  index-name: kb_document_chunks

qwen:
  api-key: your_qwen_api_key
  chat-model: qwen-max

jwt:
  secret: your_secret_key
  expiration: 86400000
```

#### knowledge-base-core（业务层）

**职责：**
- 实现核心业务逻辑
- 领域模型定义
- 数据访问抽象
- Kafka消息消费
- 缓存管理

**关键类：**

| 类名 | 职责 |
|------|------|
| `User.java` | 用户实体 |
| `Role.java` | 角色实体 |
| `Document.java` | 文档实体 |
| `DocumentPermission.java` | 文档权限实体 |
| `QAHistory.java` | 问答历史实体 |
| `UserRepository.java` | 用户数据访问 |
| `DocumentRepository.java` | 文档数据访问 |
| `DocumentPermissionRepository.java` | 权限数据访问 |
| `QAService.java` | 问答服务接口 |
| `QAServiceImpl.java` | 问答服务实现 |
| `DocumentService.java` | 文档服务 |
| `PermissionService.java` | 权限服务 |
| `RankFusionService.java` | RRF融合服务 |
| `CacheService.java` | 缓存服务 |
| `DocumentProcessingConsumer.java` | 文档处理消费者 |
| `DeadLetterQueueConsumer.java` | 死信队列消费者 |

**领域模型示例：**

```java
@Entity
@Table(name = "documents")
@Data
public class Document {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false, length = 50)
    private String fileType;

    @Column(nullable = false)
    private Long fileSize;

    @Column(nullable = false)
    private Long uploadedBy;

    @Column(nullable = false)
    private Boolean isPublic;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private DocumentStatus status;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(columnDefinition = "JSONB")
    private Map<String, Object> metadata;

    @Column(name = "tags")
    private List<String> tags;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
```

#### knowledge-base-infrastructure（基础设施层）

**职责：**
- 外部系统集成
- Elasticsearch操作
- Kafka配置
- 千问API调用
- 文档解析

**关键类：**

| 类名 | 职责 |
|------|------|
| `ElasticsearchConfig.java` | ES配置 |
| `ElasticsearchService.java` | ES服务接口 |
| `ElasticsearchServiceImpl.java` | ES服务实现 |
| `DocumentChunk.java` | 文档块实体 |
| `KafkaConfig.java` | Kafka配置 |
| `EmbeddingService.java` | 文本向量化服务接口 |
| `EmbeddingServiceImpl.java` | 文本向量化服务实现 |
| `ChatService.java` | 对话生成服务接口 |
| `ChatServiceImpl.java` | 对话生成服务实现 |
| `DocumentParserService.java` | 文档解析服务 |

**Elasticsearch服务实现：**

```java
@Service
@RequiredArgsConstructor
public class ElasticsearchServiceImpl implements ElasticsearchService {

    private final ElasticsearchClient client;

    @Value("${elasticsearch.index-name}")
    private String indexName;

    @Override
    public List<SearchHit> searchByBM25(String query, List<Long> accessibleDocIds) {
        // BM25全文搜索
        BoolQuery.Builder boolBuilder = new BoolQuery.Builder();
        boolBuilder.must(m -> m.Match(match -> match
            .field("content")
            .query(FieldValue.of(query))
        ));

        // 权限过滤
        if (accessibleDocIds != null && !accessibleDocIds.isEmpty()) {
            boolBuilder.filter(f -> f.Terms(t -> t
                .field("document_id")
                .terms(terms -> terms.value(
                    accessibleDocIds.stream()
                        .map(id -> FieldValue.of(id))
                        .collect(Collectors.toList())
                ))
            ));
        }

        SearchRequest searchRequest = new SearchRequest.Builder()
            .index(indexName)
            .size(20)
            .query(boolBuilder.build()._toQuery())
            .build();

        SearchResponse<DocumentChunk> response = client.search(
            searchRequest,
            DocumentChunk.class
        );

        return response.hits().hits().stream()
            .map(SearchHit::from)
            .collect(Collectors.toList());
    }

    @Override
    public List<SearchHit> searchByKNN(float[] queryVector, List<Long> accessibleDocIds) {
        // KNN向量搜索
        Query knnQuery = KnnQuery.of(k -> k
            .field("content_vector")
            .queryVector(queryVector)
            .k(20)
            .numCandidates(100)
        )._toQuery();

        // 权限过滤
        BoolQuery.Builder boolBuilder = new BoolQuery.Builder()
            .must(knnQuery);

        if (accessibleDocIds != null && !accessibleDocIds.isEmpty()) {
            boolBuilder.filter(f -> f.Terms(t -> t
                .field("document_id")
                .terms(terms -> terms.value(
                    accessibleDocIds.stream()
                        .map(id -> FieldValue.of(id))
                        .collect(Collectors.toList())
                ))
            ));
        }

        SearchRequest searchRequest = new SearchRequest.Builder()
            .index(indexName)
            .size(20)
            .query(boolBuilder.build()._toQuery())
            .build();

        SearchResponse<DocumentChunk> response = client.search(
            searchRequest,
            DocumentChunk.class
        );

        return response.hits().hits().stream()
            .map(SearchHit::from)
            .collect(Collectors.toList());
    }

    @Override
    public void bulkIndexChunks(List<DocumentChunk> chunks) throws Exception {
        List<BulkOperation> operations = new ArrayList<>();
        for (DocumentChunk chunk : chunks) {
            chunk.setCreatedAt(System.currentTimeMillis());
            operations.add(BulkOperation.of(b -> b
                .index(i -> i
                    .index(indexName)
                    .id(chunk.getDocumentId() + "_" + chunk.getChunkId())
                    .document(chunk)
                )
            ));
        }

        BulkRequest bulkRequest = new BulkRequest.Builder()
            .operations(operations)
            .build();

        BulkResponse bulkResponse = client.bulk(bulkRequest);

        if (bulkResponse.errors()) {
            log.error("批量索引存在错误");
            bulkResponse.items().forEach(item -> {
                if (item.error() != null) {
                    log.error("索引失败: id={}, error={}",
                        item.id(), item.error().reason());
                }
            });
        } else {
            log.info("批量索引成功: count={}", chunks.size());
        }
    }
}
```

#### knowledge-base-common（公共模块）

**职责：**
- 定义DTO（数据传输对象）
- 统一响应格式
- 异常定义
- 工具类

**关键类：**

| 类名 | 职责 |
|------|------|
| `Result.java` | 统一响应封装 |
| `ResultCode.java` | 响应码定义 |
| `BusinessException.java` | 业务异常 |
| `AuthResponse.java` | 认证响应 |
| `LoginRequest.java` | 登录请求 |
| `RegisterRequest.java` | 注册请求 |
| `QARequest.java` | 问答请求 |

**统一响应格式：**

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {
    private Integer code;
    private String message;
    private T data;
    private Long timestamp;

    public static <T> Result<T> success(T data) {
        return Result.<T>builder()
            .code(200)
            .message("success")
            .data(data)
            .timestamp(System.currentTimeMillis())
            .build();
    }

    public static <T> Result<T> error(ResultCode resultCode) {
        return Result.<T>builder()
            .code(resultCode.getCode())
            .message(resultCode.getMessage())
            .timestamp(System.currentTimeMillis())
            .build();
    }
}
```

### 1.3 前端模块详解

#### 目录结构

```
frontend/
├── src/
│   ├── hooks/                              # 自定义Hooks
│   │   ├── useWebSocketQA.ts               # WebSocket问答Hook
│   │   └── redux.ts                        # Redux Hook
│   │
│   ├── components/                         # 组件
│   │   ├── layout/                         # 布局组件
│   │   │   └── MainLayout.tsx              # 主布局
│   │   └── qa/                             # Q&A组件
│   │       ├── ChatInterface.tsx           # 聊天界面
│   │       └── MessageRenderer.tsx         # 消息渲染
│   │
│   ├── pages/                              # 页面组件
│   │   ├── Login.tsx                       # 登录页
│   │   ├── HomePage.tsx                    # 首页
│   │   ├── DocumentsPage.tsx               # 文档管理页
│   │   └── QAPage.tsx                      # 问答页
│   │
│   ├── services/                           # API服务
│   │   ├── api.ts                          # Axios实例
│   │   ├── authService.ts                  # 认证服务
│   │   ├── documentService.ts              # 文档服务
│   │   └── qaService.ts                    # 问答服务
│   │
│   ├── store/                              # Redux Store
│   │   ├── slices/
│   │   │   ├── authSlice.ts                # 认证状态
│   │   │   ├── documentSlice.ts            # 文档状态
│   │   │   ├── qaSlice.ts                  # 问答状态
│   │   │   └── uiSlice.ts                  # UI状态
│   │   └── index.ts                        # Store配置
│   │
│   ├── types/                              # TypeScript类型
│   │   └── index.ts                        # 类型定义
│   │
│   ├── App.tsx                             # 根组件
│   └── main.tsx                            # 入口文件
│
├── package.json
├── tsconfig.json
├── vite.config.ts
└── index.html
```

#### 核心组件说明

**1. WebSocket Hook（useWebSocketQA.ts）**

```typescript
export const useWebSocketQA = () => {
  const [isConnected, setIsConnected] = useState(false);
  const [messages, setMessages] = useState<Message[]>([]);
  const wsRef = useRef<WebSocket | null>(null);

  const connect = useCallback(() => {
    const ws = new WebSocket('ws://localhost:8080/api/ws/qa');

    ws.onopen = () => {
      setIsConnected(true);
      console.log('WebSocket connected');
    };

    ws.onmessage = (event) => {
      const data = JSON.parse(event.data);

      if (data.type === 'token') {
        // 追加token到当前消息
        setMessages(prev => {
          const newMessages = [...prev];
          const lastMessage = newMessages[newMessages.length - 1];
          if (lastMessage && lastMessage.role === 'assistant') {
            lastMessage.content += data.content;
          }
          return newMessages;
        });
      } else if (data.type === 'complete') {
        // 消息完成，添加引用
        setMessages(prev => {
          const newMessages = [...prev];
          const lastMessage = newMessages[newMessages.length - 1];
          if (lastMessage && lastMessage.role === 'assistant') {
            lastMessage.sources = data.sources;
          }
          return newMessages;
        });
      }
    };

    ws.onerror = (error) => {
      console.error('WebSocket error:', error);
    };

    ws.onclose = () => {
      setIsConnected(false);
      console.log('WebSocket closed');
    };

    wsRef.current = ws;
  }, []);

  const sendMessage = useCallback((question: string) => {
    if (wsRef.current?.readyState === WebSocket.OPEN) {
      // 添加用户消息
      setMessages(prev => [...prev, {
        role: 'user',
        content: question,
        timestamp: new Date()
      }]);

      // 添加助手消息占位
      setMessages(prev => [...prev, {
        role: 'assistant',
        content: '',
        timestamp: new Date()
      }]);

      // 发送问题
      wsRef.current.send(JSON.stringify({
        type: 'query',
        question: question
      }));
    }
  }, []);

  const disconnect = useCallback(() => {
    wsRef.current?.close();
  }, []);

  return { isConnected, messages, connect, disconnect, sendMessage };
};
```

**2. Redux Slices（qaSlice.ts）**

```typescript
import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import { qaService } from '../services/qaService';

interface QAState {
  currentAnswer: string;
  sources: Source[];
  isLoading: boolean;
  error: string | null;
}

const initialState: QAState = {
  currentAnswer: '',
  sources: [],
  isLoading: false,
  error: null,
};

export const submitQuestion = createAsyncThunk(
  'qa/submitQuestion',
  async (question: string, { rejectWithValue }) => {
    try {
      const response = await qaService.submitQuestion(question);
      return response.data;
    } catch (error: any) {
      return rejectWithValue(error.response?.data || error.message);
    }
  }
);

const qaSlice = createSlice({
  name: 'qa',
  initialState,
  reducers: {
    clearAnswer: (state) => {
      state.currentAnswer = '';
      state.sources = [];
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(submitQuestion.pending, (state) => {
        state.isLoading = true;
        state.error = null;
      })
      .addCase(submitQuestion.fulfilled, (state, action) => {
        state.isLoading = false;
        state.currentAnswer = action.payload.answer;
        state.sources = action.payload.sources;
      })
      .addCase(submitQuestion.rejected, (state, action) => {
        state.isLoading = false;
        state.error = action.payload as string;
      });
  },
});

export const { clearAnswer } = qaSlice.actions;
export default qaSlice.reducer;
```

**3. API服务（qaService.ts）**

```typescript
import api from './api';

export const qaService = {
  submitQuestion: (question: string) =>
    api.post('/qa/query', { question }),

  retrieveDocuments: (query: string) =>
    api.post('/qa/retrieve', { query }),

  searchBM25: (query: string) =>
    api.post('/search/bm25', { query }),

  searchKNN: (query: string) =>
    api.post('/search/knn', { query }),

  hybridSearch: (query: string) =>
    api.post('/search/hybrid', { query }),
};
```

---

## 2. 核心业务流程

### 2.1 文档上传流程

#### 2.1.1 传统上传流程

```
┌─────────┐
│  用户   │
└────┬────┘
     │ 上传文档
     ▼
┌─────────────────────────────────────────────────────────┐
│                    DocumentController                    │
│  1. 验证文件格式和大小                                   │
│  2. 创建Document记录（状态=PROCESSING）                  │
│  3. 发送Kafka消息（DocumentUploaded事件）                │
│  4. 立即返回文档ID给用户                                 │
└────────────────────┬────────────────────────────────────┘
                     │
                     │ Kafka消息
                     ▼
┌─────────────────────────────────────────────────────────┐
│           DocumentProcessingConsumer                     │
│  1. 接收Kafka消息                                        │
│  2. 下载文件                                            │
│  3. 解析文档内容（POI/PDFBox）                           │
│  4. 文本分块（500字符/块，重叠50字符）                    │
│  5. 调用千问API生成向量                                  │
│  6. 批量索引到Elasticsearch                              │
│  7. 更新Document状态（PROCESSING→COMPLETED）             │
│  8. WebSocket推送完成通知                                │
└─────────────────────────────────────────────────────────┘
```

#### 2.1.2 分片上传流程（推荐）

**特性：**
- 支持大文件上传（最大1GB）
- 断点续传：网络中断后可继续上传
- 秒传：相同文件无需重复上传
- 并发上传：3个分片同时上传，速度提升5倍

**API接口：**

##### 1. 初始化分片上传

```http
POST /api/documents/chunk/init
Content-Type: application/json

{
  "fileName": "large-file.pdf",
  "fileSize": 104857600,
  "fileMd5": "5d41402abc4b2a76b9719d911017c592",
  "contentType": "application/pdf",
  "title": "大文件文档",
  "description": "这是一个大文件",
  "tags": ["技术", "文档"],
  "isPublic": false
}
```

**响应：**
```json
{
  "code": 200,
  "data": {
    "uploadId": "5d41402abc4b2a76b9719d911017c592_123456",
    "alreadyExists": false,
    "chunkSize": 5242880,
    "totalChunks": 20,
    "uploadedChunks": 0,
    "uploadToken": "abc123..."
  }
}
```

**秒传场景：**
```json
{
  "code": 200,
  "data": {
    "uploadId": "existing_upload_id",
    "alreadyExists": true,
    "existingDocumentId": 123,
    "chunkSize": 5242880,
    "totalChunks": 20,
    "uploadedChunks": 20
  }
}
```

##### 2. 上传分片

```http
POST /api/documents/chunk/upload
Content-Type: multipart/form-data

uploadId: 5d41402abc4b2a76b9719d911017c592_123456
chunkNumber: 0
chunk: [binary data]
chunkMd5: "d41d8cd98f00b204e9800998ecf8427e" (可选)
```

**响应：**
```json
{
  "code": 200,
  "data": {
    "uploadId": "5d41402abc4b2a76b9719d911017c592_123456",
    "chunkNumber": 0,
    "success": true,
    "duplicate": false,
    "uploadedChunks": 1,
    "totalChunks": 20,
    "progress": 5,
    "allChunksUploaded": false,
    "message": "分片上传成功"
  }
}
```

##### 3. 完成分片上传

```http
POST /api/documents/chunk/complete
Content-Type: application/json

{
  "uploadId": "5d41402abc4b2a76b9719d911017c592_123456",
  "fileMd5": "5d41402abc4b2a76b9719d911017c592",
  "title": "最终文档标题",
  "description": "最终描述",
  "tags": ["技术"],
  "isPublic": false
}
```

**响应：**
```json
{
  "code": 200,
  "data": {
    "uploadId": "5d41402abc4b2a76b9719d911017c592_123456",
    "success": true,
    "documentId": 124,
    "documentTitle": "最终文档标题",
    "filePath": "documents/uuid/filename.pdf",
    "storageType": "MINIO",
    "fileSize": 104857600,
    "message": "上传完成"
  }
}
```

##### 4. 检查上传状态（断点续传）

```http
POST /api/documents/chunk/check
Content-Type: application/json

{
  "fileMd5": "5d41402abc4b2a76b9719d911017c592",
  "fileName": "large-file.pdf",
  "fileSize": 104857600
}
```

**响应（秒传）：**
```json
{
  "code": 200,
  "data": {
    "exists": true,
    "documentId": 123,
    "documentTitle": "相同文件已存在",
    "message": "文件已存在，无需重复上传"
  }
}
```

**响应（断点续传）：**
```json
{
  "code": 200,
  "data": {
    "exists": false,
    "uploadId": "5d41402abc4b2a76b9719d911017c592_123456",
    "status": "UPLOADING",
    "totalChunks": 20,
    "uploadedChunks": 8,
    "progress": 40,
    "uploadedChunkNumbers": [0, 1, 2, 3, 4, 5, 6, 7],
    "missingChunkNumbers": [8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19],
    "message": "找到未完成的上传会话，可以断点续传"
  }
}
```

##### 5. 取消上传

```http
DELETE /api/documents/chunk/{uploadId}
```

**响应：**
```json
{
  "code": 200,
  "data": "上传已取消",
  "message": "success"
}
```

**分片上传完整流程：**

```
┌─────────────┐
│   前端      │
│  文件分片    │
└──────┬──────┘
       │ 1. POST /chunk/init
       ▼
┌─────────────────────────────────────────────────────────┐
│          ChunkUploadController.initUpload()             │
│  - 计算文件MD5                                           │
│  - 检查是否已存在（秒传）                                 │
│  - 生成uploadId                                          │
│  - 初始化Redis BitMap                                    │
│  - 创建UploadSession记录                                 │
└────────────────────┬────────────────────────────────────┘
                     │
       ┌─────────────┴─────────────┐
       │ 并发上传（3个分片）        │
       └─────────────┬─────────────┘
                     │
       ┌─────────────┴─────────────┐
       │ 2. POST /chunk/upload      │
       ▼                             │
┌─────────────────────────────────────┐
│    ChunkUploadController.uploadChunk() │
│  - 上传分片到MinIO                  │
│  - 更新Redis BitMap                │
│  - 返回上传进度                    │
└─────────────────────────────────────┘
                     │
                     │ 3. POST /chunk/complete
                     ▼
┌─────────────────────────────────────────────────────────┐
│         ChunkUploadController.completeUpload()          │
│  - 验证所有分片已上传                                     │
│  - MinIO Compose API合并分片                             │
│  - 创建Document记录                                      │
│  - 发送Kafka消息（异步处理）                              │
│  - 清理Redis临时状态                                      │
└─────────────────────────────────────────────────────────┘
```

**前端实现示例：**

```typescript
// 1. 初始化上传
const initResponse = await documentService.chunkedUpload.init({
  fileName: file.name,
  fileSize: file.size,
  fileMd5: md5,
  title: '文档标题',
  isPublic: false
});

// 2. 并发上传分片
const CHUNK_SIZE = 5 * 1024 * 1024; // 5MB
const totalChunks = Math.ceil(file.size / CHUNK_SIZE);
const concurrentUploads = 3;

for (let i = 0; i < totalChunks; i += concurrentUploads) {
  const uploads = [];
  for (let j = 0; j < concurrentUploads && i + j < totalChunks; j++) {
    const chunkIndex = i + j;
    const start = chunkIndex * CHUNK_SIZE;
    const end = Math.min(start + CHUNK_SIZE, file.size);
    const chunkData = file.slice(start, end);

    uploads.push(
      documentService.chunkedUpload.uploadChunk(
        initResponse.uploadId,
        chunkIndex,
        chunkData
      )
    );
  }
  await Promise.all(uploads);
}

// 3. 完成上传
const completeResponse = await documentService.chunkedUpload.complete({
  uploadId: initResponse.uploadId,
  fileMd5: md5
});
```

**断点续传实现：**

```typescript
// 上传前检查
const checkResponse = await documentService.chunkedUpload.check({
  fileMd5: md5,
  fileName: file.name,
  fileSize: file.size
});

if (checkResponse.exists) {
  // 秒传
  console.log('文件已存在，秒传成功');
  return;
}

if (checkResponse.uploadId) {
  // 断点续传
  const { missingChunkNumbers } = checkResponse;
  console.log(`继续上传，还需上传${missingChunkNumbers.length}个分片`);

  // 只上传缺失的分片
  for (const chunkIndex of missingChunkNumbers) {
    // ... 上传逻辑
  }
}
```

**代码实现：**

```java
// DocumentController.java
@PostMapping("/upload")
public Result<DocumentUploadResponse> uploadDocument(
        @RequestParam("file") MultipartFile file,
        @RequestParam("isPublic") Boolean isPublic,
        @AuthenticationPrincipal CustomUserDetails userDetails) {

    // 1. 验证文件
    validateFile(file);

    // 2. 创建文档记录
    Document document = Document.builder()
        .fileName(file.getOriginalFilename())
        .fileType(getFileType(file.getOriginalFilename()))
        .fileSize(file.getSize())
        .uploadedBy(userDetails.getId())
        .isPublic(isPublic)
        .status(DocumentStatus.PROCESSING)
        .build();

    document = documentRepository.save(document);

    // 3. 发送Kafka消息
    kafkaTemplate.send("document-processing",
        DocumentProcessingMessage.builder()
            .documentId(document.getId())
            .fileName(file.getOriginalFilename())
            .uploadedBy(userDetails.getId())
            .build()
    );

    // 4. 立即返回
    return Result.success(DocumentUploadResponse.builder()
        .documentId(document.getId())
        .status(DocumentStatus.PROCESSING)
        .message("文档上传成功，正在处理中")
        .build());
}

// DocumentProcessingConsumer.java
@KafkaListener(
    topics = "document-processing",
    groupId = "document-processor-group",
    concurrency = "3"
)
public void processDocument(
        DocumentProcessingMessage message,
        Acknowledgment acknowledgment) {

    try {
        log.info("开始处理文档: documentId={}", message.getDocumentId());

        // 1. 获取文档记录
        Document document = documentRepository.findById(message.getDocumentId())
            .orElseThrow(() -> new BusinessException("文档不存在"));

        // 2. 解析文档
        String content = documentParserService.parseDocument(message.getFilePath());

        // 3. 分块
        List<String> chunks = splitContent(content, 500, 50);

        // 4. 生成向量并索引
        List<DocumentChunk> chunkEntities = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            float[] vector = qwenAIService.generateEmbedding(chunks.get(i));

            chunkEntities.add(DocumentChunk.builder()
                .documentId(document.getId())
                .chunkId((long) i)
                .content(chunks.get(i))
                .contentVector(vector)
                .title(document.getTitle())
                .fileName(document.getFileName())
                .fileType(document.getFileType())
                .uploadedBy(document.getUploadedBy())
                .isPublic(document.getIsPublic())
                .build()
            );
        }

        // 5. 批量索引
        elasticsearchService.bulkIndexChunks(chunkEntities);

        // 6. 更新文档状态
        document.setStatus(DocumentStatus.COMPLETED);
        document.setContent(content);
        document.setChunkCount(chunks.size());
        documentRepository.save(document);

        // 7. 推送完成通知
        webSocketSessionManager.sendMessageToUser(
            message.getUploadedBy(),
            "文档处理完成: " + document.getFileName()
        );

        // 8. 手动提交
        acknowledgment.acknowledge();

        log.info("文档处理完成: documentId={}", message.getDocumentId());

    } catch (Exception e) {
        log.error("文档处理失败: documentId={}", message.getDocumentId(), e);

        // 发送到死信队列
        kafkaTemplate.send("document-processing-dlq", message);

        // 更新状态为失败
        documentRepository.findById(message.getDocumentId()).ifPresent(doc -> {
            doc.setStatus(DocumentStatus.FAILED);
            doc.setErrorMessage(e.getMessage());
            documentRepository.save(doc);
        });
    }
}
```

### 2.2 问答流程

```
┌─────────┐
│  用户   │
└────┬────┘
     │ 提交问题
     ▼
┌─────────────────────────────────────────────────────────┐
│                      QAWebSocketHandler                  │
│  1. 接收WebSocket消息                                    │
│  2. 获取用户身份（JWT Token）                            │
│  3. 获取用户权限                                         │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│                      QAServiceImpl                       │
│  1. 获取用户可访问文档ID列表                             │
│  2. 生成问题向量                                         │
│  3. 并行执行BM25和KNN检索                                │
│  4. RRF融合结果                                          │
│  5. 提取Top-K块作为上下文                                │
│  6. 构建RAG Prompt                                       │
│  7. 调用千问API流式生成答案                              │
│  8. WebSocket推送token                                   │
│  9. 完成后返回引用                                       │
└─────────────────────────────────────────────────────────┘
```

**代码实现：**

```java
// QAServiceImpl.java
@Override
public void processQueryWithStream(
        String question,
        String userId,
        StreamCallback callback) throws Exception {

    try {
        // 1. 获取用户权限
        List<Long> accessibleDocIds = getAccessibleDocumentIds(userId);

        // 2. 生成问题向量
        float[] queryVector = qwenAIService.generateEmbedding(question);

        // 3. 并行检索
        CompletableFuture<List<ElasticsearchService.SearchHit>> bm25Future =
            CompletableFuture.supplyAsync(() -> {
                try {
                    return elasticsearchService.searchByBM25(question, accessibleDocIds);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

        CompletableFuture<List<ElasticsearchService.SearchHit>> knnFuture =
            CompletableFuture.supplyAsync(() -> {
                try {
                    return elasticsearchService.searchByKNN(queryVector, accessibleDocIds);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

        // 等待检索完成
        CompletableFuture.allOf(bm25Future, knnFuture).join();

        // 4. RRF融合
        List<ElasticsearchService.SearchHit> combinedResults =
            rankFusionService.reciprocalRankFusion(
                bm25Future.get(),
                knnFuture.get()
            );

        // 5. 提取Top-K
        List<ElasticsearchService.SearchHit> contextChunks =
            extractTopChunks(combinedResults, 5);

        // 6. 构建上下文
        String context = buildContext(contextChunks);

        // 7. 流式生成答案
        chatService.streamAnswer(question, context, new ChatService.StreamCallback() {
            @Override
            public void onToken(String token) {
                callback.onToken(token);
            }

            @Override
            public void onComplete() {
                List<Map<String, Object>> sources = buildSources(contextChunks);
                callback.onComplete(sources);
            }

            @Override
            public void onError(Throwable error) {
                callback.onError(error);
            }
        });

    } catch (Exception e) {
        log.error("处理查询失败: userId={}, question={}", userId, question, e);
        callback.onError(e);
    }
}

// RankFusionServiceImpl.java
@Override
public List<ElasticsearchService.SearchHit> reciprocalRankFusion(
        List<ElasticsearchService.SearchHit> bm25Results,
        List<ElasticsearchService.SearchHit> knnResults) {

    // 合并并去重
    Map<String, ElasticsearchService.SearchHit> hitMap = new HashMap<>();

    // 添加BM25结果
    for (int i = 0; i < bm25Results.size(); i++) {
        ElasticsearchService.SearchHit hit = bm25Results.get(i);
        String key = hit.getDocumentId() + "_" + hit.getChunkId();
        hit.setBm25Rank(i + 1);
        hitMap.put(key, hit);
    }

    // 合并KNN结果
    for (int i = 0; i < knnResults.size(); i++) {
        ElasticsearchService.SearchHit hit = knnResults.get(i);
        String key = hit.getDocumentId() + "_" + hit.getChunkId();
        if (hitMap.containsKey(key)) {
            hitMap.get(key).setKnnRank(i + 1);
        } else {
            hit.setKnnRank(i + 1);
            hitMap.put(key, hit);
        }
    }

    // 计算RRF分数
    int k = 60;
    List<ElasticsearchService.SearchHit> results = new ArrayList<>(hitMap.values());

    for (ElasticsearchService.SearchHit hit : results) {
        double score = 0.0;

        if (hit.getBm25Rank() > 0) {
            score += 1.0 / (k + hit.getBm25Rank());
        }

        if (hit.getKnnRank() > 0) {
            score += 1.0 / (k + hit.getKnnRank());
        }

        hit.setRrfScore(score);
    }

    // 按RRF分数排序
    results.sort((a, b) -> Double.compare(b.getRrfScore(), a.getRrfScore()));

    return results;
}
```

### 2.3 权限检查流程

```
┌─────────┐
│  用户   │
└────┬────┘
     │ 访问资源
     ▼
┌─────────────────────────────────────────────────────────┐
│              JwtAuthenticationFilter                     │
│  1. 提取JWT Token                                        │
│  2. 验证Token有效性                                      │
│  3. 解析用户身份                                         │
│  4. 设置SecurityContext                                  │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│              CustomPermissionEvaluator                   │
│  1. 检查功能权限（角色权限）                             │
│  2. 检查数据权限（文档权限）                             │
│  3. 返回权限结果                                         │
└─────────────────────────────────────────────────────────┘
```

**代码实现：**

```java
// CustomPermissionEvaluator.java
@Component
@RequiredArgsConstructor
public class CustomPermissionEvaluator {

    private final PermissionService permissionService;
    private final CacheService cacheService;

    public boolean checkPermission(Authentication authentication,
                                 Object permissionId,
                                 Object targetType,
                                 Object targetId) {

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long userId = userDetails.getId();

        // 1. 检查功能权限
        if (targetType == null) {
            return checkFunctionPermission(userId, (String) permissionId);
        }

        // 2. 检查数据权限
        if ("document".equals(targetType)) {
            return checkDocumentPermission(userId, (Long) targetId, (String) permissionId);
        }

        return false;
    }

    private boolean checkFunctionPermission(Long userId, String permission) {
        // 从缓存获取用户权限
        List<String> permissions = cacheService.getUserPermissions(userId);

        // 缓存未命中，从数据库查询
        if (permissions == null) {
            permissions = permissionService.getUserPermissions(userId);
            cacheService.cacheUserPermissions(userId, permissions);
        }

        return permissions.contains(permission);
    }

    private boolean checkDocumentPermission(Long userId, Long documentId, String permission) {
        // 检查文档权限
        return permissionService.hasDocumentPermission(userId, documentId, permission);
    }
}

// 使用注解
@PreAuthorize("@customPermissionEvaluator.checkPermission(authentication, 'document:read', 'document', #documentId)")
public Document getDocument(@PathVariable Long documentId) {
    return documentService.findById(documentId);
}
```

---

## 3. 开发规范

### 3.1 代码规范

#### 命名约定

| 类型 | 约定 | 示例 |
|------|------|------|
| 类名 | 大驼峰（PascalCase） | `DocumentService` |
| 方法名 | 小驼峰（camelCase） | `getDocumentById` |
| 常量 | 全大写下划线分隔 | `MAX_FILE_SIZE` |
| 变量 | 小驼峰（camelCase） | `documentId` |
| 包名 | 全小写点分隔 | `com.company.kb.service` |

#### 注释规范

**类注释：**
```java
/**
 * 文档服务实现类
 *
 * <p>负责文档的CRUD操作、权限管理和上传处理</p>
 *
 * @author 张三
 * @since 1.0.0
 */
@Service
public class DocumentServiceImpl implements DocumentService {
    // ...
}
```

**方法注释：**
```java
/**
 * 上传文档并异步处理
 *
 * @param file 上传的文件
 * @param isPublic 是否公开
 * @param userDetails 当前用户
 * @return 文档上传响应，包含文档ID和处理状态
 * @throws BusinessException 文件验证失败时抛出
 */
public DocumentUploadResponse uploadDocument(
        MultipartFile file,
        Boolean isPublic,
        CustomUserDetails userDetails) {
    // ...
}
```

**关键逻辑注释：**
```java
// RRF算法：结合BM25和KNN检索结果
// 公式：score(d) = Σ 1/(k + rank_i(d))
// k=60是经验值，无需调参
Map<String, Double> rrfScores = new HashMap<>();
int k = 60;

for (SearchHit hit : results) {
    double score = 0.0;
    if (hit.getBm25Rank() > 0) {
        score += 1.0 / (k + hit.getBm25Rank());
    }
    if (hit.getKnnRank() > 0) {
        score += 1.0 / (k + hit.getKnnRank());
    }
    rrfScores.put(hit.getId(), score);
}
```

#### 日志规范

**日志级别：**
- ERROR：错误信息，需要立即处理
- WARN：警告信息，需要关注
- INFO：关键业务流程信息
- DEBUG：调试信息，开发环境使用

**日志格式：**
```java
// 关键操作日志
log.info("开始处理文档: documentId={}, fileName={}", documentId, fileName);
log.info("文档处理完成: documentId={}, chunkCount={}", documentId, chunkCount);

// 异常日志
log.error("文档处理失败: documentId={}", documentId, e);

// 性能日志
long startTime = System.currentTimeMillis();
// ... 业务逻辑
log.info("检索耗时: {}ms", System.currentTimeMillis() - startTime);
```

### 3.2 Git规范

#### 分支策略

```
main (生产环境)
  ↑
  ├── develop (开发环境)
  │     ↑
  │     ├── feature/user-authentication (功能分支)
  │     ├── feature/document-upload
  │     └── feature/qa-search
  │
  └── hotfix/security-patch (紧急修复)
```

**分支命名：**
- `main`：生产环境分支
- `develop`：开发环境分支
- `feature/功能名`：新功能开发
- `hotfix/问题名`：紧急修复
- `release/版本号`：发布准备

#### 提交规范

**提交信息格式：**
```
<type>(<scope>): <subject>

<body>

<footer>
```

**Type类型：**
- `feat`：新功能
- `fix`：修复bug
- `docs`：文档更新
- `style`：代码格式调整
- `refactor`：代码重构
- `test`：测试相关
- `chore`：构建/工具变更

**示例：**
```
feat(document): add document upload feature

- Add document upload endpoint
- Implement Kafka async processing
- Add file validation logic

Closes #123
```

### 3.3 API接口文档

#### 认证接口

**POST /api/auth/login**
- 描述：用户登录
- 请求体：
```json
{
  "username": "user@example.com",
  "password": "password123"
}
```
- 响应：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "user": {
      "id": 1,
      "username": "user@example.com",
      "role": "USER"
    }
  },
  "timestamp": 1704729600000
}
```

**POST /api/auth/register**
- 描述：用户注册
- 请求体：
```json
{
  "username": "user@example.com",
  "password": "password123",
  "email": "user@example.com"
}
```

#### 文档接口

**POST /api/documents/upload**
- 描述：上传文档
- 请求：Multipart/form-data
  - file: 文件
  - isPublic: 是否公开
- 响应：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "documentId": 1,
    "status": "PROCESSING",
    "message": "文档上传成功，正在处理中"
  },
  "timestamp": 1704729600000
}
```

**GET /api/documents/{id}**
- 描述：获取文档详情
- 响应：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "title": "示例文档",
    "fileName": "example.pdf",
    "fileType": "pdf",
    "fileSize": 1024000,
    "uploadedBy": 1,
    "isPublic": true,
    "status": "COMPLETED",
    "createdAt": "2026-04-08T10:00:00"
  },
  "timestamp": 1704729600000
}
```

**GET /api/documents/my**
- 描述：获取我的文档列表
- 响应：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "documents": [...],
    "total": 10,
    "page": 1,
    "pageSize": 20
  }
}
```

**DELETE /api/documents/{id}**
- 描述：删除文档
- 响应：
```json
{
  "code": 200,
  "message": "文档删除成功",
  "timestamp": 1704729600000
}
```

#### 问答接口

**POST /api/qa/query**
- 描述：提交问题（非流式）
- 请求体：
```json
{
  "question": "如何使用Spring Boot？"
}
```
- 响应：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "answer": "Spring Boot是一个简化Spring应用开发的框架...",
    "sources": [
      {
        "id": "1_0",
        "title": "Spring Boot入门指南",
        "fileName": "spring-boot-guide.pdf",
        "documentId": 1,
        "score": 0.85,
        "index": 1
      }
    ]
  },
  "timestamp": 1704729600000
}
```

**POST /api/qa/retrieve**
- 描述：检索相关文档块
- 请求体：
```json
{
  "query": "Spring Boot配置"
}
```

#### 搜索接口

**POST /api/search/bm25**
- 描述：BM25关键词搜索
- 请求体：
```json
{
  "query": "Spring Boot",
  "topK": 20
}
```

**POST /api/search/knn**
- 描述：KNN向量搜索
- 请求体：
```json
{
  "query": "Spring Boot配置",
  "topK": 20
}
```

**POST /api/search/hybrid**
- 描述：混合搜索（BM25+KNN）
- 请求体：
```json
{
  "query": "Spring Boot自动配置",
  "topK": 20
}
```

---

## 4. 开发工具推荐

### 4.1 后端开发

| 工具 | 用途 | 推荐版本 |
|------|------|----------|
| IntelliJ IDEA | IDE | 2023.3+ |
| Postman | API测试 | Latest |
| DBeaver | 数据库管理 | Latest |
| RedisInsight | Redis管理 | Latest |
| Kibana | ES可视化 | 8.11+ |

### 4.2 前端开发

| 工具 | 用途 | 推荐版本 |
|------|------|----------|
| VS Code | IDE | Latest |
| React DevTools | React调试 | Latest |
| Redux DevTools | Redux调试 | Latest |

---

## 5. 常见问题

### Q1: 如何添加新的文档格式支持？

1. 在`DocumentParserService`中添加解析逻辑
2. 添加对应的Maven依赖
3. 更新`application.yml`中的`allowed-extensions`配置

### Q2: 如何自定义检索算法？

1. 实现`ElasticsearchService`接口
2. 在`QAServiceImpl`中调用新的检索方法
3. 调整`RankFusionService`以支持新的结果格式

### Q3: 如何添加新的AI服务？

1. 创建新的服务接口（如`OpenAIService`）
2. 实现`generateEmbedding`和`generateAnswer`方法
3. 在配置文件中添加AI服务配置
4. 在`QAServiceImpl`中切换AI服务

---

**文档版本**：v1.0
**最后更新**：2026-04-08
**维护者**：开发团队
