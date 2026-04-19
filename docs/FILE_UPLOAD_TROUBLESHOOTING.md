# 文件上传与管理问题排查与解决方案

本文档记录了企业知识库系统在文件上传功能开发过程中遇到的问题及其解决方案，供学习和参考。

## 目录

1. [配置类问题](#配置类问题)
2. [前端类问题](#前端类问题)
3. [后端类问题](#后端类问题)
4. [消息队列问题](#消息队列问题)
5. [算法类问题](#算法类问题)
6. [功能增强](#功能增强)

---

## 配置类问题

### 问题1：文件大小限制错误

**错误现象**：
```
org.apache.tomcat.util.http.fileupload.impl.SizeLimitExceededException:
the request was rejected because its size (212931703) exceeds the configured maximum (52428800)
```

上传约200MB的文件时被拒绝，提示超过50MB限制。

**根本原因**：
1. Spring Boot的`multipart.max-file-size`默认为1MB，项目配置为50MB
2. **更隐蔽的问题**：Tomcat连接器有内部的`maxSwallowSize`限制，默认50MB
3. Tomcat在Spring Boot处理之前就会拦截大请求

**解决方案**：

修改 `application.yml`：

```yaml
# Spring Boot 多部分上传配置
servlet:
  multipart:
    max-file-size: 500MB        # 单文件最大大小
    max-request-size: 500MB     # 整个请求最大大小
    enabled: true

# Tomcat 连接器配置
server:
  tomcat:
    max-http-form-post-size: -1  # POST请求大小无限制
    max-swallow-size: -1         # 关键：取消Tomcat的内部限制
    connection-timeout: 60000
    threads:
      max: 200
      min-spare: 10
```

**关键点**：
- `max-swallow-size: -1` 是关键，很多开发者只改Spring配置而忽略Tomcat限制
- 两个限制都要调整，任何一个都会导致上传失败

**经验教训**：
- 遇到大小限制问题时，检查所有可能存在的限制点
- Spring Boot内部的Tomcat有独立的配置
- 错误信息中的数字（52428800 = 50MB）是重要线索

---

## 前端类问题

### 问题2：分片上传未触发

**错误现象**：
上传200MB的文件，但前端日志显示"使用普通上传"而非"使用分片上传"。

**根本原因**：
前端代码中`SIZE_THRESHOLD`设置错误：

```typescript
// 错误的配置
const SIZE_THRESHOLD = 1024 * 1024 * 1024  // 1GB

// 实际应该是
const SIZE_THRESHOLD = 50 * 1024 * 1024    // 50MB
```

**解决方案**：

修改 `SmartFileUploader.tsx` 和 `IntelligentFileUploader.tsx`：

```typescript
// 第39行左右
const SIZE_THRESHOLD = 50 * 1024 * 1024  // 50MB，与后端配置一致
```

**经验教训**：
- 前后端的阈值配置必须保持一致
- 单位转换要清晰（使用注释说明MB数）
- 配置常量应该集中管理，避免散落在代码中

### 问题3：并发分片上传数据丢失

**错误现象**：
上传完成后，MinIO中缺失部分分片（如缺失1-20号分片），导致合并失败。

**根本原因**：
前端并发上传逻辑存在严重bug：

```typescript
// 错误的实现
const uploadPromises: Promise<boolean>[] = []
let activeUploads = 0

for (let i = uploadedChunks; i < totalChunks; i++) {
  if (activeUploads >= concurrentChunks) {
    await Promise.race(uploadPromises)
    uploadPromises.length = 0  // BUG: 清空数组导致所有Promise丢失！
    activeUploads = 0
  }
  // ... 创建上传Promise并加入数组 ...
}
```

`Promise.race()`只等待最快完成的Promise，清空数组后其他正在进行的Promise引用丢失，无法追踪其完成状态。

**解决方案**：

采用工作线程模式，每个线程负责连续的分片范围：

```typescript
// 正确的实现
const uploadChunkWithLimit = async (startIndex: number) => {
  for (let i = startIndex; i < totalChunks; i++) {
    const start = i * chunkSize
    const end = Math.min(start + chunkSize, file.size)
    const chunkData = file.slice(start, end)

    await documentService.chunkedUpload.uploadChunk(
      uploadId, i, chunkData,
      (progress) => {
        // 进度回调
      }
    )

    // 顺序等待，确保每个分片完成
  }
}

// 计算每个工作线程的分片数量
const chunksPerWorker = Math.ceil((totalChunks - uploadedChunks) / concurrentChunks)
const workers: Promise<void>[] = []

for (let w = 0; w < concurrentChunks; w++) {
  const startChunk = uploadedChunks + w * chunksPerWorker
  if (startChunk >= totalChunks) break

  workers.push(uploadChunkWithLimit(startChunk))
}

// 等待所有工作线程完成
await Promise.all(workers)
```

**关键点**：
- 每个worker顺序处理自己的分片范围，避免Promise管理混乱
- 范围划分确保所有分片都被分配
- `Promise.all()`等待所有worker完成，不丢失任何Promise

**经验教训**：
- `Promise.race()`只返回最快完成的结果，不保证其他Promise完成
- 并发任务的引用管理很重要，丢失引用会导致无法等待完成
- 对于分片上传这种有序任务，按范围划分比随机分配更可靠

---

## 后端类问题

### 问题4：Redis状态与MinIO不一致

**错误现象**：
- Redis显示已上传30个分片
- MinIO中只有部分分片存在
- 合并时报告缺少分片

**根本原因**：
网络中断或上传失败后，Redis状态已更新但MinIO上传未完成，导致状态不一致。

**解决方案**：

在 `FileUploadServiceImpl.initUpload()` 中添加一致性检查：

```java
// 验证Redis状态与MinIO是否一致
boolean redisConsistent = true;
for (int i = 0; i < uploadedChunks; i++) {
    String chunkPath = String.format("documents/%s/chunks/%d", uploadId, i);
    if (!minioService.objectExists(chunkPath)) {
        log.warn("Redis不一致：分片{}标记为已上传但MinIO中不存在", i);
        redisConsistent = false;
        break;
    }
}

// 如果不一致，重置Redis状态
if (!redisConsistent) {
    log.warn("由于状态不一致重置Redis: uploadId={}", uploadId);
    chunkStatusService.cleanupUploadSession(uploadId);
    uploadedChunks = 0;
    session.setUploadedChunks(0);
    uploadSessionRepository.save(session);
}
```

在合并前再次验证：

```java
// 合并前验证所有分片都在MinIO中
List<Integer> missingChunks = new ArrayList<>();
for (int i = 0; i < session.getTotalChunks(); i++) {
    String chunkPath = String.format("documents/%s/chunks/%d", request.getUploadId(), i);
    if (!minioService.objectExists(chunkPath)) {
        log.error("MinIO中缺少分片: uploadId={}, chunk={}, path={}",
            request.getUploadId(), i, chunkPath);
        missingChunks.add(i);
    }
}

if (!missingChunks.isEmpty()) {
    throw new IllegalStateException(
        String.format("无法合并：MinIO中缺少%d个分片: %s",
            missingChunks.size(), missingChunks));
}
```

**经验教训**：
- 分布式系统中多个状态存储（Redis、MinIO、数据库）需要保持一致
- 关键操作前应该验证所有依赖的状态
- 失败时需要清理或重置状态，避免脏数据

### 问题5：秒传功能只对分片上传有效

**错误现象**：
- 分片上传相同文件会提示"文件已存在"
- 普通上传相同文件每次都重新上传
- 用户反馈：只有特定文件（C.pdf）显示已存在

**根本原因**：
- 分片上传流程包含MD5计算和检查
- 普通上传没有发送MD5参数，无法实现秒传

**解决方案**：

**1. 前端添加MD5计算**：

在 `documentService.ts` 中添加MD5计算方法：

```typescript
import SparkMD5 from 'spark-md5'

// 计算文件MD5（用于秒传）
calculateMD5: (file: File): Promise<string> => {
  return new Promise((resolve, reject) => {
    const spark = new SparkMD5.ArrayBuffer()
    const fileReader = new FileReader()
    const chunkSize = 2 * 1024 * 1024  // 2MB分块读取
    let currentChunk = 0
    const chunks = Math.ceil(file.size / chunkSize)

    fileReader.onload = (e) => {
      if (e.target?.result) {
        spark.append(e.target.result as ArrayBuffer)
        currentChunk++

        if (currentChunk < chunks) {
          loadNext()
        } else {
          const md5 = spark.end()
          resolve(md5)
        }
      }
    }

    fileReader.onerror = () => {
      reject(new Error('文件MD5计算失败'))
    }

    const loadNext = () => {
      const start = currentChunk * chunkSize
      const end = Math.min(start + chunkSize, file.size)
      fileReader.readAsArrayBuffer(file.slice(start, end))
    }

    loadNext()
  })
}
```

**2. 上传时携带MD5**：

```typescript
upload: async (
  file: File,
  metadata: UploadDocumentRequest,
  onProgress?: (progress: number) => void
): Promise<Document> => {
  // 计算文件MD5
  const fileMd5 = await documentService.calculateMD5(file)
  console.log('[Upload] 文件MD5:', fileMd5, 'fileName:', file.name)

  const formData = new FormData()
  formData.append('file', file)
  formData.append('title', metadata.title)
  formData.append('fileMd5', fileMd5)  // 添加MD5参数

  // ... 其他字段 ...

  const response = await api.post('/documents/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    onUploadProgress: (progressEvent) => {
      if (onProgress && progressEvent.total) {
        const progress = Math.round((progressEvent.loaded * 100) / progressEvent.total)
        onProgress(progress)
      }
    },
  })

  // 检查是否为秒传
  if (response.alreadyExists) {
    console.log('[Upload] 秒传成功! existingDocumentId:', response.id)
  }

  return response
}
```

**3. 后端检查MD5**：

在 `DocumentController.uploadDocument()` 中：

```java
@PostMapping("/upload")
public Result<Map<String, Object>> uploadDocument(
        @RequestParam("file") MultipartFile file,
        @RequestParam(value = "fileMd5", required = false) String fileMd5,
        // ... 其他参数 ...
) {
    // 1. 检查是否可以秒传
    if (fileMd5 != null && !fileMd5.isEmpty()) {
        Document existingDocument = documentService.findExistingDocument(fileMd5, username);
        if (existingDocument != null) {
            log.info("文件已存在，秒传成功: documentId={}", existingDocument.getId());

            Map<String, Object> result = new HashMap<>();
            result.put("id", existingDocument.getId());
            result.put("alreadyExists", true);  // 标记为秒传
            result.put("message", "文件已存在，秒传成功");

            return Result.success(result);
        }
    }

    // 2. 正常上传流程
    Document document = documentService.uploadDocument(file, title, description, username, fileMd5);
    // ...
}
```

**4. 添加MD5查询方法**：

在 `DocumentRepository` 中：

```java
/**
 * 根据用户、MD5和状态查询文档（用于秒传）
 */
List<Document> findByUploadedByAndFileMd5AndStatus(
    User uploadedBy,
    String fileMd5,
    Document.DocumentStatus status
);
```

在 `DocumentServiceImpl` 中实现：

```java
@Override
public Document findExistingDocument(String fileMd5, String username) {
    // 查找用户
    User user = userRepository.findByUsername(username).orElse(null);
    if (user == null) {
        return null;
    }

    // 查找相同MD5的已索引文档
    List<Document> existingDocs = documentRepository
        .findByUploadedByAndFileMd5AndStatus(
            user, fileMd5, Document.DocumentStatus.INDEXED);

    if (!existingDocs.isEmpty()) {
        log.info("找到已存在文档: documentId={}", existingDocs.get(0).getId());
        return existingDocs.get(0);
    }

    return null;
}
```

**5. 保存MD5到数据库**：

在 `Document` 实体中添加字段：

```java
// 文件MD5（用于秒传）
@Column(length = 64)
private String fileMd5;
```

在保存文档时记录MD5：

```java
Document document = Document.builder()
    // ... 其他字段 ...
    .fileMd5(fileMd5)  // 保存MD5
    .build();
```

**经验教训**：
- MD5是文件去重的有效手段
- 秒传应作为所有上传方式的通用功能
- 前端计算MD5需要分块处理，避免大文件内存溢出

---

## 消息队列问题

### 问题6：Kafka消费者未被调用

**错误现象**：
- 文档上传成功，Kafka消息已发送
- `DocumentProcessingConsumer.processDocument()` 从未被调用
- 消息直接进入死信队列（DLQ）

**根本原因**：
消费者方法签名过于复杂，Spring Kafka无法正确匹配：

```java
// 问题签名
@KafkaListener(topics = "document-processing", groupId = "document-processor-group")
public void processDocument(
        ConsumerRecord<String, String> record,
        Acknowledgment acknowledgment) {
    // ...
}
```

**解决方案**：

**1. 简化方法签名**：

```java
@KafkaListener(
    topics = "document-processing",
    groupId = "document-processor-group",
    concurrency = "3",
    containerFactory = "kafkaListenerContainerFactory"  // 指定容器工厂
)
public void processDocument(String message) {
    log.info("===== Kafka Consumer 被调用 =====");
    log.info("Message (raw): '{}'", message);

    Long documentId = Long.parseLong(message);
    log.info("成功解析documentId: {}", documentId);

    // 处理文档...
}
```

**2. 启用自动提交**：

在 `KafkaConfig.java` 中：

```java
@Bean
public ConsumerFactory<String, String> consumerFactory() {
    Map<String, Object> config = new HashMap<>();
    config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    config.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
    config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);  // 启用自动提交

    return new DefaultKafkaConsumerFactory<>(config);
}
```

**3. 指定容器工厂**：

```java
@Bean
@Primary
public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
        ConsumerFactory<String, String> consumerFactory,
        KafkaTemplate<String, String> kafkaTemplate) {

    DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
            kafkaTemplate,
            (record, ex) -> new TopicPartition("document-processing-dlt", record.partition()));

    // 直接发送到DLQ，不重试（便于调试）
    DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer);

    ConcurrentKafkaListenerContainerFactory<String, String> factory =
            new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(consumerFactory);
    factory.setCommonErrorHandler(errorHandler);
    factory.setConcurrency(3);

    return factory;
}
```

**关键点**：
- 简单的String参数比ConsumerRecord更可靠
- 明确指定`containerFactory`避免歧义
- 调试时禁用重试可以更快发现问题

**经验教训**：
- Kafka消费者方法签名要简单明了
- 当监听器不被调用时，检查方法签名和容器工厂配置
- 死信队列是重要的调试工具

### 问题7：Kafka消息重复处理

**错误现象**：
- 同一个文档ID被处理多次
- 日志显示消息进入DLQ后又被消费
- 实际上是Kafka的至少一次语义

**根本原因**：
这是Kafka的正常行为，不是bug：
- Kafka保证至少一次投递，可能重复
- 消息处理失败后会重试或进入DLQ
- 重试成功后会再次处理

**解决方案**：

添加幂等性检查，跳过已处理的文档：

```java
@KafkaListener(topics = "document-processing", ...)
public void processDocument(String message) {
    Long documentId = Long.parseLong(message);

    // ========== 早期检查：防止重复处理 ==========
    Document existingDocument = documentRepository.findById(documentId).orElse(null);
    if (existingDocument != null && existingDocument.getStatus() == Document.DocumentStatus.INDEXED) {
        log.warn("文档已索引，跳过处理: documentId={}, status={}",
            documentId, existingDocument.getStatus());
        return;
    }
    // =============================================

    // 继续处理...
}
```

**经验教训**：
- 分布式消息系统必须考虑幂等性
- 在业务逻辑开始前先检查状态
- "进入DLQ后又出来"可能是重试成功的正常现象

---

## 算法类问题

### 问题8：文档分块无限循环

**错误现象**：
```
java.lang.OutOfMemoryError: Java heap space
```

文档处理时内存溢出，实际上是分块逻辑进入无限循环。

**根本原因**：
分块算法中`start`位置计算错误，导致原地踏步：

```java
// 错误的实现
while (start < contentLength) {
    int end = Math.min(start + chunkSize, contentLength);

    // ... 处理分片 ...

    int overlapChars = overlap * 4;
    start = end - overlapChars;  // 当end较小时，start可能不变或倒退

    // 没有检查start是否真正前进
}
```

**解决方案**：

```java
private List<DocumentChunk> chunkDocument(String content, Document document) {
    List<DocumentChunk> chunks = new ArrayList<>();
    int contentLength = content.length();
    int chunkIndex = 0;

    // 短内容特殊处理
    if (contentLength <= chunkSize * 4) {
        // 直接创建单个chunk
        DocumentChunk chunk = DocumentChunk.builder()
            .content(content)
            .documentId(document.getId())
            .position(0)
            .build();
        chunks.add(chunk);
        return chunks;
    }

    int start = 0;
    int previousStart = -1;  // 关键：追踪上一次的位置

    while (start < contentLength) {
        // 检测无限循环
        if (start == previousStart) {
            log.warn("检测到分块无限循环，停止分块: documentId={}, start={}",
                document.getId(), start);
            break;
        }
        previousStart = start;

        // ... 处理当前分片 ...

        // 计算下一个起始位置
        int overlapChars = overlap * 4;
        start = end - overlapChars;

        // 防止负数
        if (start < 0) {
            start = 0;
        }

        // 检查是否有进展
        if (start >= end) {
            break;
        }

        // 检查是否到达末尾
        if (start >= contentLength) {
            break;
        }

        // 限制最大chunk数量，防止OOM
        if (chunks.size() >= 1000) {
            log.warn("达到最大chunk数量限制(1000)，停止分块: documentId={}", document.getId());
            break;
        }
    }

    return chunks;
}
```

**关键点**：
- `previousStart`追踪检测位置不变
- 多个边界条件检查（负数、到达末尾、无进展）
- 最大chunk数量限制作为最后防线

**经验教训**：
- 循环中必须有明确的进度检测
- 多重边界检查是必要的
- 资源限制（如最大数量）是防御性编程的重要手段

### 问题9：除零错误

**错误现象**：
```
java.lang.ArithmeticException: / by zero
    at VectorEmbeddingServiceImpl.generateEmbeddingsBatch:62
```

**根本原因**：
当文档内容为空时，chunkCount=0，计算平均值时除零：

```java
long duration = System.currentTimeMillis() - startTime;
log.info("Batch embeddings generated: textCount={}, duration={}ms, avgPerText={}ms",
    texts.size(), duration, duration / texts.size());  // texts.size() = 0
```

**解决方案**：

```java
@Override
public List<float[]> generateEmbeddingsBatch(List<String> texts) throws Exception {
    log.info("Batch generate embeddings: provider={}, textCount={}",
        activeProvider.getProviderName(), texts.size());

    // 添加空值检查
    if (texts == null || texts.isEmpty()) {
        log.warn("No texts to generate embeddings for");
        return new ArrayList<>();
    }

    long startTime = System.currentTimeMillis();
    List<float[]> embeddings = activeProvider.generateEmbeddingsBatch(texts);

    long duration = System.currentTimeMillis() - startTime;
    // 现在安全了，texts.size() > 0
    log.info("Batch embeddings generated: textCount={}, duration={}ms, avgPerText={}ms",
        texts.size(), duration, duration / texts.size());

    return embeddings;
}
```

**经验教训**：
- 除法运算前必须检查除数不为零
- 空集合/数组是常见的边界情况
- 日志输出也可能触发异常

### 问题10：字符串越界

**错误现象**：
```
java.lang.StringIndexOutOfBoundsException: Range [-82, 118) out of bounds for length 118
```

**根本原因**：
重叠计算导致负数起始位置：

```java
int overlapChars = overlap * 4;  // 可能很大
start = end - overlapChars;      // 可能变成负数
String chunkContent = content.substring(start, end);  // 越界
```

**解决方案**：

```java
int overlapChars = overlap * 4;
start = end - overlapChars;

// 防止负数
if (start < 0) {
    start = 0;
}

// 防止无进展
if (start >= end) {
    break;
}

// 确保不超过内容长度
if (start >= contentLength) {
    break;
}

// 现在安全地使用substring
String chunkContent = content.substring(start, end);
```

**经验教训**：
- 字符串/数组操作前验证索引范围
- 负数索引在Java中会导致异常
- 多重验证比单一验证更安全

### 问题11：空文档内容

**错误现象**：
```
PDF解析成功: pageCount=53
PDF原始内容: contentLength=790, hasContent=true
PDF trim后内容: contentLength=0, hasContent=false
chunkCount=0
```

PDF有790个字符但trim后为空，无法分块。

**根本原因**：
PDF是扫描版或图片版，只包含空白字符（空格、换行等），没有实际文字内容。

**解决方案**：

**1. 增强日志输出**：

```java
private String parsePDF(File file) throws Exception {
    log.info("开始解析PDF: {}", file.getName());

    try (PDDocument document = Loader.loadPDF(file)) {
        log.info("PDF加载成功: pageCount={}", document.getNumberOfPages());

        PDFTextStripper textStripper = new PDFTextStripper();
        textStripper.setSortByPosition(true);

        String content = textStripper.getText(document);
        log.info("PDF原始内容: contentLength={}, hasContent={}",
            content.length(), !content.isEmpty());

        String trimmed = content.trim();
        log.info("PDF trim后内容: contentLength={}, hasContent={}",
            trimmed.length(), !trimmed.isEmpty());

        if (trimmed.length() == 0 && content.length() > 0) {
            log.warn("PDF内容全是空白字符: originalLength={}", content.length());
            log.warn("这可能是扫描版PDF或图片PDF，需要OCR处理");
        }

        return trimmed;
    }
}
```

**2. 添加空内容检测**：

```java
@KafkaListener(topics = "document-processing", ...)
public void processDocument(String message) {
    // ... 前面的代码 ...

    String content = documentService.parseDocument(document);

    // 检查内容是否为空
    if (content == null || content.trim().isEmpty()) {
        log.warn("⚠️ 文档内容为空，无法处理: documentId={}, fileName={}",
            documentId, document.getFileName());

        // 更新文档状态为失败
        document.setStatus(Document.DocumentStatus.FAILED);
        document.setErrorMessage(
            "文档内容为空或无法解析。可能是扫描版PDF、图片PDF或加密文档。" +
            "建议：1)使用文字版PDF 2)集成OCR进行文字识别");
        documentRepository.save(document);

        log.warn("文档已标记为失败: documentId={}", documentId);
        return;
    }

    // 继续处理...
}
```

**经验教训**：
- PDF有多种类型（文字、扫描、图片）
- 文字提取成功不代表有实际内容
- 明确的错误信息能帮助用户理解问题
- OCR是处理扫描PDF的方案（但不在当前范围）

---

## 功能增强

### 秒传功能的完整实现

**功能描述**：
用户上传已存在的文件时，无需重新上传数据和重新处理，直接引用已有文档。

**技术要点**：

1. **MD5计算**：前端使用SparkMD5分块计算，避免大文件内存溢出
2. **去重范围**：同一用户下相同MD5的已索引文档
3. **状态标记**：返回`alreadyExists: true`标识秒传
4. **适用范围**：普通上传和分片上传都支持

**实现位置**：
- 前端：`documentService.ts` 的 `calculateMD5()` 和 `upload()` 方法
- 后端：`DocumentController` 的 `uploadDocument()` 方法
- 数据层：`DocumentRepository` 的 `findByUploadedByAndFileMd5AndStatus()` 方法

**注意事项**：
- MD5字段长度为64字符
- 只匹配已索引完成的文档（状态=INDEXED）
- 秒传不复制权限设置，需要单独处理

---

## 总结

### 问题分类统计

| 类别 | 问题数 | 主要原因 |
|------|--------|----------|
| 配置类 | 1 | 隐藏的Tomcat限制 |
| 前端类 | 2 | 阈值配置错误、并发逻辑bug |
| 后端类 | 2 | 状态不一致、功能缺失 |
| 消息队列 | 2 | 方法签名复杂、缺少幂等性 |
| 算法类 | 4 | 边界检查不足、无限循环 |

### 关键经验

1. **配置完整性**：检查所有配置点，不只看表面
2. **并发控制**：Promise/异步任务的引用管理很重要
3. **状态一致性**：多存储系统间的状态需要验证
4. **幂等性设计**：分布式系统必须考虑重复处理
5. **边界检查**：算法实现要检查所有边界条件
6. **详细日志**：问题排查依赖详细的日志输出

### 推荐阅读

- Spring Boot Multipart配置文档
- Kafka消费者配置和模式
- SparkMD5使用指南
- MinIO分片上传最佳实践
- Elasticsearch文档分块策略

---

*文档版本：1.0*
*创建日期：2026-04-10*
*作者：Knowledge Base Development Team*
