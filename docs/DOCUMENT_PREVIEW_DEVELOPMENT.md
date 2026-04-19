# 文档预览与下载功能开发总结

## 功能概述

实现了企业知识库系统的文档预览和下载功能，支持：
- 文本文件在线预览（txt, md, json, xml, log 及代码文件）
- PDF文件在线预览
- 所有类型文件下载
- 文件大小限制（最大1MB预览）

## 一、核心功能点

### 1.1 文件下载功能
- 直接浏览器下载
- 文件名正确编码（支持中文）
- Content-Type正确设置

### 1.2 文件预览功能
- 文本文件：直接显示内容
- PDF文件：iframe嵌入显示
- 其他文件：提示下载

### 1.3 安全性
- JWT token认证
- 文件类型校验
- 文件大小限制

## 二、技术架构

### 2.1 后端架构

```
Controller层 (DocumentController)
    ↓
Service层 (DocumentService)
    ↓
Infrastructure层 (MinioService)
    ↓
MinIO对象存储
```

### 2.2 前端架构

```
DocumentDetailPage.tsx
    ↓
documentService.ts
    ↓
axios (api.ts)
    ↓
后端API
```

## 三、开发痛点与解决方案

### 痛点1: 文件下载时的中文文件名编码问题

**问题描述：**
```
下载的中文文件名显示为乱码或下载失败
```

**解决方案：**
```java
// DocumentController.java
public ResponseEntity<Resource> downloadDocument(@PathVariable Long documentId) {
    DocumentService.FileDownloadResult downloadResult = documentService.downloadDocument(documentId);

    // URL编码文件名（关键步骤）
    String encodedFileName = URLEncoder.encode(downloadResult.getFileName(), StandardCharsets.UTF_8)
            .replaceAll("\\+", "%20");

    return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .header(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"" + downloadResult.getFileName() + "\"; " +
                    "filename*=UTF-8''" + encodedFileName)  // RFC 5987标准
            .body(downloadResult.getResource());
}
```

**关键技术点：**
- 使用 `URLEncoder.encode()` 对文件名进行编码
- `filename*=UTF-8''` 是 RFC 5987 标准
- 同时提供 `filename` 和 `filename*` 以兼容不同浏览器

### 痛点2: Resource类型的正确实现

**问题描述：**
```java
error: cannot find symbol: class Resource
error: InputStreamResource() has protected access in InputStreamResource
```

**解决方案：**
```java
// MinioService.java - 使用ByteArrayResource
public Resource downloadFile(String objectPath) throws Exception {
    try (InputStream stream = minioClient.getObject(...)) {
        // 将InputStream读取到byte数组
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[16384];
        int nRead;
        while ((nRead = stream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        byte[] bytes = buffer.toByteArray();

        String filename = objectPath.substring(objectPath.lastIndexOf("/") + 1);

        return new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return filename;
            }
        };
    }
}
```

**关键点：**
- 使用 `ByteArrayResource` 而非 `InputStreamResource`
- 需要重写 `getFilename()` 方法
- 将流内容读取到内存（注意大文件限制）

### 痛点3: axios响应拦截器导致的数据提取问题

**问题描述：**
```typescript
// 前端代码
const response = await api.post('/documents/36')
console.log(response.data)  // undefined
```

**原因分析：**
```typescript
// api.ts 响应拦截器
api.interceptors.response.use(
  (response) => {
    if (code !== undefined && data !== undefined) {
      return data  // 已经提取了data字段
    }
    return response.data
  }
)
```

**解决方案：**
```typescript
// 正确使用方式
const response = await api.get(`/documents/${id}`) as any
// response 已经是业务数据本身，不是axios响应对象
return response

// 或者直接使用
const data = await api.get(`/documents/${id}`)
console.log(data)  // 直接是数据
```

**关键点：**
- 响应拦截器改变了返回值结构
- 需要在代码中明确这种变化
- 使用 `as any` 类型断言避免TypeScript错误

### 痛点4: Spring Boot异步文件流处理

**问题描述：**
```java
// 使用InputStreamResource时
return new InputStreamResource(stream) {
    @Override
    public InputStream getInputStream() {
        // 这时stream可能已经关闭
    }
}
```

**解决方案：**
```java
// 使用ByteArrayResource，将内容读取到内存
public Resource downloadFile(String objectPath) throws Exception {
    byte[] bytes;
    try (InputStream stream = minioClient.getObject(...)) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        // 读取流到byte数组
        byte[] data = new byte[16384];
        int nRead;
        while ((nRead = stream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        bytes = buffer.toByteArray();
    }
    // stream在try-with-resources中自动关闭
    // 但bytes数组已经保存了内容
    return new ByteArrayResource(bytes);
}
```

**关键点：**
- try-with-resources确保流正确关闭
- 将内容读入内存避免流关闭问题
- 限制文件大小避免内存溢出

### 痛点5: 文件类型判断与预览策略

**问题描述：**
- 如何判断文件是否支持预览？
- 不同文件类型使用什么预览方式？

**解决方案：**
```java
// 后端：文件类型校验
String fileType = document.getFileType().toLowerCase();
Set<String> previewableTypes = Set.of(
    ".txt", ".md", ".json", ".xml", ".log",
    ".java", ".js", ".ts", ".py", ".c", ".cpp"
);

if (!previewableTypes.contains(fileType)) {
    throw new UnsupportedOperationException("不支持预览: " + fileType);
}
```

```typescript
// 前端：预览策略判断
const isPreviewable = (fileType: string) => {
  const type = fileType.toLowerCase()
  return ['.txt', '.md', '.json', '.xml', '.log'].includes(type)
}

const isPdfPreviewable = (fileType: string) => {
  return fileType.toLowerCase() === '.pdf'
}

// 渲染逻辑
{isPreviewable(fileType) ? (
  <TextPreview content={textContent} />
) : isPdfPreviewable(fileType) ? (
  <iframe src={previewUrl} />
) : (
  <DownloadPrompt />
)}
```

### 痛点6: 大文件预览的性能问题

**问题描述：**
- 文本文件太大导致浏览器卡顿
- 加载时间过长

**解决方案：**
```java
// 限制文件大小
int maxBytes = 1024 * 1024; // 1MB
int totalBytes = 0;
byte[] buffer = new byte[8192];
int nRead;

while ((nRead = stream.read(buffer, 0, buffer.length)) != -1) {
    totalBytes += nRead;
    if (totalBytes > maxBytes) {
        throw new IOException("文件过大，不支持预览（最大1MB）");
    }
    byteBuffer.write(buffer, 0, nRead);
}
```

**前端优化：**
```typescript
// 分页显示大文件
const [content, setContent] = useState('')

// 只显示前10000个字符，超出部分截断
const displayContent = content.length > 10000
  ? content.substring(0, 10000) + '\n\n... (内容过长，已截断)'
  : content
```

## 四、面试考点

### 4.1 HTTP协议相关

**考点1: Content-Disposition的作用**
```java
// 下载文件
header("Content-Disposition: attachment; filename=\"file.txt\"")

// 预览文件
header("Content-Disposition: inline")
```

**考点2: 文件名编码标准**
- RFC 2231 vs RFC 5987
- 浏览器兼容性问题
- 中文文件名处理

**考点3: Content-Type的重要性**
```java
// 根据文件扩展名确定Content-Type
MediaType mediaType = switch (extension) {
    case "pdf" -> MediaType.APPLICATION_PDF;
    case "txt" -> MediaType.TEXT_PLAIN;
    case "json" -> MediaType.APPLICATION_JSON;
    default -> MediaType.APPLICATION_OCTET_STREAM;
};
```

### 4.2 Java IO与NIO

**考点1: try-with-resources**
```java
// 自动关闭资源
try (InputStream stream = minioClient.getObject(...)) {
    // 使用stream
} // 自动关闭
```

**考点2: InputStream转换为byte数组**
```java
// 方法1: ByteArrayOutputStream
ByteArrayOutputStream buffer = new ByteArrayOutputStream();
byte[] data = new byte[1024];
int nRead;
while ((nRead = stream.read(data)) != -1) {
    buffer.write(data, 0, nRead);
}
byte[] bytes = buffer.toByteArray();

// 方法2: Apache Commons IO
byte[] bytes = IOUtils.toByteArray(stream);
```

**考点3: 字符编码处理**
```java
// 指定字符编码读取文本
String content = new String(bytes, StandardCharsets.UTF_8);
```

### 4.3 Spring Boot相关

**考点1: ResponseEntity的用法**
```java
return ResponseEntity.ok()
    .contentType(mediaType)
    .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
    .body(resource);
```

**考点2: Resource接口**
```java
public interface Resource extends InputStreamSource {
    String getFilename();
    long contentLength() throws IOException;
}
```

**考点3: 异常处理**
```java
try {
    return documentService.downloadDocument(documentId);
} catch (Exception e) {
    log.error("下载失败", e);
    return ResponseEntity.notFound().build();
}
```

### 4.4 前端相关

**考点1: axios响应拦截器**
```typescript
api.interceptors.response.use(
  (response) => {
    // 统一处理Result包装格式
    if (response.data?.code !== undefined) {
      return response.data.data
    }
    return response.data
  }
)
```

**考点2: TypeScript类型断言**
```typescript
const response = await api.get('/api/data') as any
// 使用as any绕过类型检查
```

**考点3: React状态管理**
```typescript
const [content, setContent] = useState('')
const [loading, setLoading] = useState(false)

useEffect(() => {
  loadContent()
}, [documentId])
```

### 4.5 系统设计相关

**考点1: 为什么使用MinIO而非本地文件系统？**
- 可扩展性：MinIO支持分布式存储
- 高可用性：支持多副本
- 对象存储特性：天然支持元数据
- 兼容性：S3 API兼容

**考点2: 文件预览的技术选型**
- 文本文件：直接显示（最简单）
- PDF：浏览器原生支持（最便捷）
- Office文档：需转换（最复杂）
- 图片：直接显示（最直观）

**考点3: 如何处理大文件预览？**
- 限制预览文件大小
- 分页加载
- 服务端渲染
- 客户端流式处理

## 五、完整代码示例

### 5.1 后端完整接口

```java
@GetMapping("/{documentId}/download")
public ResponseEntity<Resource> downloadDocument(@PathVariable Long documentId) {
    try {
        DocumentService.FileDownloadResult result = documentService.downloadDocument(documentId);

        String encodedFileName = URLEncoder.encode(result.getFileName(), StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + result.getFileName() + "\"; " +
                        "filename*=UTF-8''" + encodedFileName)
                .body(result.getResource());
    } catch (Exception e) {
        return ResponseEntity.notFound().build();
    }
}
```

### 5.2 前端完整组件

```typescript
// 文本预览组件
const TextPreview: React.FC<{ content: string }> = ({ content }) => {
  return (
    <div style={{
      background: '#f5f5f5',
      padding: '16px',
      borderRadius: '4px',
      maxHeight: '500px',
      overflow: 'auto',
      whiteSpace: 'pre-wrap',
      fontFamily: 'monospace',
      fontSize: '14px',
      lineHeight: '1.6'
    }}>
      {content}
    </div>
  )
}
```

## 六、最佳实践

### 6.1 后端最佳实践

1. **文件大小限制**：预览限制在1MB以内
2. **类型白名单**：只允许安全的文件类型预览
3. **异常处理**：所有IO操作都要try-catch
4. **日志记录**：记录关键操作和异常
5. **资源管理**：使用try-with-resources

### 6.2 前端最佳实践

1. **用户体验**：显示加载状态
2. **错误处理**：友好的错误提示
3. **性能优化**：大文件截断显示
4. **类型安全**：TypeScript类型定义
5. **代码复用**：提取公共组件

## 七、扩展思考

### 7.1 如何支持Office文档预览？

**方案1：使用在线服务**
- Microsoft Office Online Viewer
- Google Docs Viewer
- 优点：无需后端处理
- 缺点：依赖外部服务

**方案2：服务端转换**
- 使用LibreOffice/POI转换为PDF
- 优点：完全自主可控
- 缺点：服务器资源消耗大

**方案3：前端转换**
- 使用mammoth.js（docx）
- 使用sheetjs（xlsx）
- 优点：减轻服务器压力
- 缺点：格式可能不完全一致

### 7.2 如何实现大文件的断点续传？

```java
// 使用Range头支持
String range = request.getHeader("Range");
if (range != null) {
    // 解析 Range: bytes=0-1023
    // 返回 206 Partial Content
}
```

### 7.3 如何实现文件预览权限控制？

```java
@GetMapping("/{documentId}/preview")
public ResponseEntity<Resource> previewDocument(
    @PathVariable Long documentId,
    Authentication authentication) {

    // 检查用户权限
    if (!permissionService.canView(documentId, authentication.getName())) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    // 返回文件内容
    return ResponseEntity.ok()...;
}
```

## 八、总结

这个功能虽然看似简单，但涉及的知识点非常全面：

1. **HTTP协议**：Content-Type, Content-Disposition, Range请求
2. **Java IO**：流处理、字符编码、资源管理
3. **Spring Boot**：ResponseEntity, Resource, 异常处理
4. **前端技术**：React状态管理、axios拦截器、TypeScript
5. **对象存储**：MinIO API、S3兼容协议
6. **系统设计**：文件存储策略、预览方案选型、性能优化

从面试角度看，这个功能可以作为很好的"项目经验"来讲，体现了：
- 全栈开发能力
- 问题解决能力
- 技术选型思考
- 代码质量意识
