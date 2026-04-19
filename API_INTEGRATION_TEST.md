# 前后端 API 对接测试指南

## 修复内容

### 1. Token 存储不一致问题
**问题**: 前端在不同地方使用了不同的 token key (`token` vs `accessToken`)

**修复**:
- `api.ts` - 统一使用 `localStorage.getItem('token')`
- `authSlice.ts` - 正确处理后端返回的 `accessToken`，转换为 `token`

### 2. API 路径不一致问题
**问题**: 前端调用 `/api/auth/login` 但实际应该是 `/auth/login`

**修复**:
- `authSlice.ts` - 移除多余的 `/api` 前缀（api.ts 已配置 baseURL）

### 3. 用户数据持久化
**修复**:
- 登录时保存用户信息到 `localStorage.setItem('user', JSON.stringify(user))`
- 初始状态从 localStorage 读取用户信息

---

## 启动服务进行测试

### 1. 启动基础设施
```bash
cd C:\Users\HP\Desktop\think
docker-compose up -d postgres redis elasticsearch kafka zookeeper
```

### 2. 启动后端服务
```bash
# 终端1 - API Gateway (端口8082)
cd C:\Users\HP\Desktop\think\backend\services\api-gateway
mvn spring-boot:run

# 终端2 - Auth Service (端口8083)
cd C:\Users\HP\Desktop\think\backend\services\auth\auth-api
mvn spring-boot:run

# 终端3 - Knowledge Mentor (端口8080)
cd C:\Users\HP\Desktop\think\backend\services\knowledge-mentor\km-api
mvn spring-boot:run
```

### 3. 启动前端
```bash
cd C:\Users\HP\Desktop\think\frontend
npm run dev
```

---

## API 测试步骤

### 第一步：测试登录 API

```bash
curl -X POST http://localhost:8082/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"admin\",\"password\":\"admin123\"}"
```

**期望响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "...",
    "userId": 1,
    "username": "admin",
    "role": "ADMIN"
  }
}
```

### 第二步：测试获取文档列表 API

```bash
# 1. 先登录获取 token
TOKEN=$(curl -s -X POST http://localhost:8082/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"admin\",\"password\":\"admin123\"}" \
  | jq -r '.data.accessToken')

# 2. 使用 token 调用文档列表 API
curl -X GET "http://localhost:8082/documents?page=0&size=20" \
  -H "Authorization: Bearer $TOKEN"
```

### 第三步：浏览器测试

1. 打开 `http://localhost:5173`
2. 登录用户: `admin` / `admin123`
3. 进入"文档管理"页面

---

## 常见问题排查

### 问题1: 401 Unauthorized
**原因**: Token 无效或未携带

**解决**:
```bash
# 检查 localStorage 中的 token
# 浏览器控制台执行:
console.log(localStorage.getItem('token'))
```

### 问题2: 500 Internal Server Error
**可能原因**:
- 后端服务未启动
- 数据库连接失败
- Elasticsearch 未启动

**排查**:
```bash
# 检查服务状态
curl http://localhost:8082/actuator/health
curl http://localhost:8083/actuator/health
curl http://localhost:8080/actuator/health

# 检查数据库
docker exec kb-postgres pg_isready -U kb_user

# 检查 Elasticsearch
curl http://localhost:9200/_cluster/health
```

### 问题3: CORS 错误
**解决**: 确保 API Gateway 已配置允许前端域名

检查 `application.yml` 中的 CORS 配置包含:
```yaml
allowedOrigins:
  - http://localhost:5173
```

---

## 完整的认证流程

```
1. 用户输入账号密码
   ↓
2. 前端调用 POST /auth/login
   ↓
3. 后端验证成功，返回 { accessToken, refreshToken, ... }
   ↓
4. 前端存储 token 到 localStorage.setItem('token', accessToken)
   ↓
5. 后续所有 API 请求自动在 header 添加: Authorization: Bearer {token}
   ↓
6. 后端通过 API Gateway 的 AuthenticationFilter 验证 token
   ↓
7. 验证成功，请求转发到具体服务处理
```

---

## 服务端口映射

| 服务 | 端口 | 说明 |
|------|------|------|
| 前端 | 5173 | Vite 开发服务器 |
| API Gateway | 8082 | 统一入口 |
| Auth Service | 8083 | 认证服务 |
| Knowledge Mentor | 8080 | 知识库服务 |
| Code Intelligence | 8088 | 代码审查服务 |
| PostgreSQL | 5432 | 数据库 |
| Elasticsearch | 9200 | 搜索引擎 |
| Redis | 6379 | 缓存 |
| Kafka | 9092 | 消息队列 |
