# 真实API测试指南 - 会调用AI模型

## 前提条件

确保所有服务正在运行：
```bash
# 1. 启动基础设施
docker-compose up -d

# 2. 启动后端
cd backend
mvn spring-boot:run

# 3. 启动前端
cd frontend
npm run dev
```

## 测试方法

### 方法1：使用前端界面测试

1. 打开浏览器访问 `http://localhost:5173`
2. 登录系统
3. 上传一个真实文件
4. 观察AI模型用量变化

**会触发的真实调用**：
- ✅ 上传文件到MinIO
- ✅ 保存文档到PostgreSQL
- ✅ 发送Kafka消息
- ✅ 调用Qwen API进行文档解析
- ✅ 生成embedding向量
- ✅ 索引到Elasticsearch

### 方法2：使用curl命令测试

#### 2.1 登录获取Token
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

保存返回的token。

#### 2.2 上传文档
```bash
curl -X POST http://localhost:8080/api/documents/upload \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -F "file=@test.pdf" \
  -F "title=测试文档" \
  -F "description=这是一个测试文档"
```

#### 2.3 查看文档处理状态
```bash
curl -X GET http://localhost:8080/api/documents \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### 方法3：使用Postman测试

1. **导入API集合**
   - 创建新Collection
   - 添加登录请求
   - 添加上传请求
   - 设置环境变量 `{{token}}`

2. **测试流程**
   ```
   登录 → 获取token → 上传文档 → 查看状态 → 查看AI用量
   ```

3. **上传请求配置**
   ```
   Method: POST
   URL: http://localhost:8080/api/documents/upload
   Headers:
     Authorization: Bearer {{token}}
   Body (form-data):
     file: [选择文件]
     title: 测试文档
     description: 测试描述
   ```

## 验证AI调用

### 检查后端日志

```bash
# 查看AI调用日志
cd backend
tail -f knowledge-base-api/logs/application.log | grep -i "qwen\|embedding"
```

应该看到类似：
```
2026-04-09 20:15:30.123 INFO  [qwen] Calling Qwen API: https://dashscope.aliyuncs.com/...
2026-04-09 20:15:35.456 INFO  [embedding] Generated embedding for chunk 1/10, tokens: 150
2026-04-09 20:15:40.789 INFO  [qwen] Document processing completed, total tokens: 1500
```

### 检查Qwen控制台

1. 登录阿里云百炼平台：https://bailian.console.aliyun.com/
2. 查看「用量统计」
3. 应该看到调用次数和token消耗增加

### 检查数据库

```bash
# 连接PostgreSQL
docker exec -it kb-postgres psql -U kb_user -d knowledge_base

# 查看文档记录
SELECT id, title, status, created_at FROM documents ORDER BY created_at DESC LIMIT 5;

# 查看文档分片
SELECT COUNT(*) FROM document_chunks WHERE document_id = 1;
```

### 检查Kafka消息

```bash
# 查看Kafka消息
docker exec -it kb-kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic document-processing \
  --from-beginning \
  --max-messages 1
```

## 性能监控

### 查看API调用统计

```bash
# 使用Actuator端点
curl http://localhost:8080/api/actuator/metrics/http.server.requests

# 查看所有可用指标
curl http://localhost:8080/api/actuator/metrics
```

## 预期结果

真实的文档上传流程会：

1. **上传阶段**
   - 文件上传到MinIO ✅
   - 数据库创建文档记录（状态：PROCESSING）✅
   - Kafka发送处理消息 ✅

2. **处理阶段**
   - Consumer接收消息 ✅
   - 调用Qwen API解析文档 ✅
   - 生成embedding向量 ✅
   - 索引到Elasticsearch ✅
   - 更新文档状态（INDEXED）✅

3. **用量变化**
   - Qwen API调用次数 +1 ✅
   - Token消耗增加 ✅
   - 向量生成次数增加 ✅

## 故障排查

### 如果AI没有被调用

1. **检查Kafka是否运行**
   ```bash
   docker-compose ps kafka
   ```

2. **检查Consumer是否启动**
   - 查看后端日志是否有 "Starting Kafka consumer"
   - 检查 `DocumentConsumer` 是否在运行

3. **检查配置**
   ```bash
   # 查看.env中的AI配置
   cat .env | grep QWEN
   ```

4. **手动触发处理**
   ```bash
   # 直接调用处理接口（如果有）
   curl -X POST http://localhost:8080/api/documents/process/1
   ```

## 成本估算

真实调用Qwen API会产生费用：

| 文档类型 | 预估Token | 预估费用（元） |
|---------|----------|---------------|
| 1页PDF | ~500 | ¥0.01 |
| 10页PDF | ~5,000 | ¥0.10 |
| 100页PDF | ~50,000 | ¥1.00 |

> 注意：实际费用以Qwen官方定价为准

---

**建议**：使用小文件（1-2页PDF）进行测试，控制成本。
