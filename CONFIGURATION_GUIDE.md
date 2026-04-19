# 环境变量配置指南

## 🚀 快速开始

### 第1步：复制配置文件

如果还没有 `.env` 文件：
```bash
# Windows (Git Bash)
cp .env.template .env

# 或手动创建 .env 文件，内容参考下方
```

### 第2步：填写必要配置

**最小必需配置**（必须修改）：
```bash
# ===== 必须修改的配置 =====

# Qwen API密钥（获取地址：https://bailian.console.aliyun.com/）
QWEN_API_KEY=your_qwen_api_key_here

# JWT密钥（生成方法：openssl rand -base64 64）
JWT_SECRET=change_this_to_a_strong_random_secret
```

**可选配置**（使用默认值即可）：
```bash
# 其他配置使用默认值，一般不需要修改
```

## 📋 完整配置模板

复制以下内容到 `.env` 文件：

```bash
# ========================================
# 企业知识库系统 - 环境变量配置
# ========================================
# ⚠️ 警告：此文件包含敏感信息，已在 .gitignore 中
# ========================================

# ===== 数据库配置 =====
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/knowledge_base
SPRING_DATASOURCE_USERNAME=kb_user
SPRING_DATASOURCE_PASSWORD=kb_password

# ===== Elasticsearch配置 =====
ELASTICSEARCH_URIS=http://localhost:9200

# ===== Redis配置 =====
REDIS_HOST=localhost
REDIS_PORT=6379

# ===== Kafka配置 ⚠️ 重要 =====
# 使用String序列化（已修复，不要改回JsonSerializer）
SPRING_KAFKA_BOOTSTRAP_SERVERS=localhost:9093
SPRING_KAFKA_PRODUCER_VALUE_SERIALIZER=org.springframework.kafka.support.serializer.StringSerializer
SPRING_KAFKA_CONSUMER_VALUE_DESERIALIZER=org.springframework.kafka.support.serializer.StringDeserializer

# ===== AI配置 =====
# 获取API密钥：https://bailian.console.aliyun.com/
QWEN_API_KEY=your_qwen_api_key_here
QWEN_API_URL=https://dashscope.aliyuncs.com/compatible-mode/v1
QWEN_EMBEDDING_MODEL=text-embedding-v4

# ===== JWT配置 ⚠️ 必须修改 =====
# 生成强密钥：openssl rand -base64 64
JWT_SECRET=change_this_to_a_strong_random_secret
JWT_EXPIRATION=86400000

# ===== MinIO配置 =====
MINIO_ENDPOINT=http://localhost:9000
MINIO_ACCESS_KEY=minioadmin
MINIO_SECRET_KEY=minioadmin

# ===== 应用配置 =====
# 开发环境建议禁用的功能
CHUNKED_UPLOAD_ENABLED=false
RATE_LIMIT_ENABLED=false

# 调试日志
LOGGING_LEVEL_COM_COMPANY_KB=DEBUG
```

## ⚠️ 安全注意事项

### 1. 敏感信息保护

```bash
# ✅ .gitignore 已配置，.env 不会被提交
git status  # 应该看不到 .env 文件
```

### 2. 密钥要求

**JWT Secret**：
- 必须至少256位（64个base64字符）
- 生成方法：`openssl rand -base64 64`
- 当前密钥长度：88字符 ✅

**Qwen API Key**：
- 从阿里云控制台获取
- 不要泄露或提交到git

### 3. 生产环境部署

如果部署到生产环境：
```bash
# 1. 更换所有默认密码
# 2. 使用环境变量或密钥管理服务
# 3. 启用安全功能（RATE_LIMIT_ENABLED=true）
# 4. 调整日志级别（LOGGING_LEVEL_ROOT=WARN）
```

## 🔧 配置验证

### 验证Kafka配置（重要！）

```bash
# 检查序列化配置
grep "STRING.*SERIALIZER" .env

# 应该输出：
# SPRING_KAFKA_PRODUCER_VALUE_SERIALIZER=org.springframework.kafka.support.serializer.StringSerializer
# SPRING_KAFKA_CONSUMER_VALUE_DESERIALIZER=org.springframework.kafka.support.serializer.StringDeserializer
```

### 验证JWT密钥强度

```bash
# 检查密钥长度
echo $JWT_SECRET | wc -c

# 应该 >= 64
```

## 📊 配置说明

### 开发环境推荐配置

| 配置项 | 推荐值 | 说明 |
|--------|--------|------|
| CHUNKED_UPLOAD_ENABLED | false | 小文件不需要分片 |
| RATE_LIMIT_ENABLED | false | 方便测试 |
| LOGGING_LEVEL_COM_COMPANY_KB | DEBUG | 便于调试 |
| CHUNKED_UPLOAD_ENABLED | false | 简化上传流程 |

### 生产环境推荐配置

| 配置项 | 推荐值 | 说明 |
|--------|--------|------|
| CHUNKED_UPLOAD_ENABLED | true | 支持大文件 |
| RATE_LIMIT_ENABLED | true | 防止滥用 |
| LOGGING_LEVEL_ROOT | WARN | 减少日志 |
| JWT_SECRET | 强随机值 | 必须256位+ |

## 🐛 常见问题

### Q1: .env 文件在哪里？

应该位于项目根目录：
```
think/
├── .env              # ← 这里（已创建）
├── .gitignore        # 包含 .env，不会被提交
└── backend/
```

### Q2: 配置不生效？

1. 检查文件名是否正确（必须是 `.env`，不是 `.env.txt`）
2. 重启应用
3. 检查 application.yml 中的默认值

### Q3: Kafka还是不工作？

**必须检查以下配置**：
```bash
# 必须是 StringSerializer，不能是 JsonSerializer！
SPRING_KAFKA_PRODUCER_VALUE_SERIALIZER=org.springframework.kafka.support.serializer.StringSerializer
SPRING_KAFKA_CONSUMER_VALUE_DESERIALIZER=org.springframework.kafka.support.serializer.StringDeserializer
```

## 📝 配置清单

使用前请确认：

- [ ] `.env` 文件已创建
- [ ] `QWEN_API_KEY` 已填写真实密钥
- [ ] `JWT_SECRET` 已使用强随机值
- [ ] Kafka序列化配置为 `StringSerializer`
- [ ] 应用已重启
- [ ] `.env` 不会被提交到git（检查 .gitignore）

---

**更新日期**：2026-04-10
**适用版本**：v1.0.0
