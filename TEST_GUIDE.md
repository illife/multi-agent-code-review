# 代码审查系统测试指南

## 前置条件

1. 启动 Docker Desktop
2. 启动基础设施服务：
```bash
cd C:\Users\HP\Desktop\think
docker-compose up -d
```

## 测试步骤

### 1. 启动后端服务

```bash
cd backend/knowledge-base-api
mvn spring-boot:run
```

**预期结果**：
- 服务启动在 `http://localhost:8080`
- 控制台显示 "CodeReviewConsumer 已启动"

### 2. 启动前端服务

```bash
cd frontend
npm run dev
```

**预期结果**：
- 前端运行在 `http://localhost:5173`
- 可以访问 http://localhost:5173/review

### 3. API 测试（使用 curl）

#### 3.1 登录获取 Token

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"username\": \"admin\", \"password\": \"admin123\"}"
```

**保存返回的 token**

#### 3.2 提交代码审查（同步）

```bash
curl -X POST http://localhost:8080/api/review/submit \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "code": "function test() {\n  var x = 1;\n  console.log(x);\n  return x == 1;\n}",
    "language": "javascript",
    "fileName": "test.js"
  }'
```

**预期结果**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "reviewId": 1,
    "status": "COMPLETED",
    "totalIssues": 3,
    "issues": [...]
  }
}
```

#### 3.3 获取审查详情

```bash
curl -X GET http://localhost:8080/api/review/1 \
  -H "Authorization: Bearer <TOKEN>"
```

### 4. WebSocket 测试

连接：`ws://localhost:8080/api/ws/review?token=<TOKEN>`

发送消息：
```json
{
  "type": "submit",
  "code": "function test() { var x = 1; }",
  "language": "javascript",
  "fileName": "test.js",
  "visibility": "PRIVATE"
}
```

**预期消息流**：
1. `{"type": "start", "reviewId": 1}`
2. `{"type": "agent_progress", ...}`
3. `{"type": "issue_found", "issue": {...}}`
4. `{"type": "agent_complete", ...}`
5. `{"type": "complete", ...}`

## 验证清单

- [ ] 后端编译通过
- [ ] 前端编译通过
- [ ] 数据库表创建成功
- [ ] 登录获取 token 成功
- [ ] 提交代码审查成功
- [ ] 返回问题列表
- [ ] WebSocket 连接成功
- [ ] 实时进度更新正常

## 常见问题

### 后端启动失败
- 检查 Docker 服务是否运行
- 检查 PostgreSQL 是否在 5432 端口
- 检查配置文件 `.env`

### WebSocket 连接失败
- 检查 token 是否有效
- 检查防火墙设置
- 查看后端日志

### Agent 未执行
- 检查 Kafka 是否运行
- 查看 CodeReviewConsumer 日志
- 检查 QWEN_API_KEY 配置
