# 低资源服务器部署快速指南

## 服务器配置要求

| 资源 | 推荐值 | 最低值 |
|------|--------|--------|
| CPU | 2核心 | 1核心 |
| 内存 | 2GB | 1.5GB |
| 磁盘 | 20GB | 10GB |
| 系统 | Ubuntu 22.04 | Debian 11+ |

## 资源分配总览

```
┌─────────────────────────────────────────────────────────────┐
│                    2GB 内存分配                              │
├─────────────────────────────────────────────────────────────┤
│  应用服务              │    内存     │   CPU    │           │
├─────────────────────────────────────────────────────────────┤
│  PostgreSQL           │   200MB     │   0.5核   │           │
│  Redis                │   100MB     │   0.25核  │           │
│  Kafka + Zookeeper    │   230MB     │   0.75核  │           │
│  Elasticsearch        │   300MB     │   0.5核   │           │
│  API Gateway          │   256MB     │   0.5核   │           │
│  Auth Service         │   256MB     │   0.5核   │           │
│  Knowledge Mentor     │   384MB     │   0.75核  │           │
│  Code Intelligence    │   256MB     │   0.5核   │           │
│  前端 (Nginx)         │    64MB     │   0.25核  │           │
├─────────────────────────────────────────────────────────────┤
│  总计                 │  ~2046MB    │  ~4.5核  │           │
└─────────────────────────────────────────────────────────────┘

注: CPU使用是动态共享的，不会同时达到最大值
```

## 快速部署步骤

### 1. 服务器准备

```bash
# 更新系统
sudo apt update && sudo apt upgrade -y

# 安装 Docker
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker $USER

# 安装 Docker Compose
sudo apt install docker-compose -y

# 创建工作目录
mkdir -p ~/think-platform
cd ~/think-platform
```

### 2. 配置环境变量

```bash
# 复制环境变量模板
cp .env.prod.example .env.prod

# 编辑配置（重要！）
nano .env.prod
```

**必须修改的配置**:
```bash
POSTGRES_PASSWORD=your_secure_password_minimum_32_characters
JWT_SECRET=generate_with_openssl_rand_base64_32
QWEN_API_KEY=sk-your-actual-qwen-api-key
```

### 3. 部署

```bash
# 方式一：使用部署脚本（推荐）
chmod +x scripts/deploy-low-resource.sh
./scripts/deploy-low-resource.sh

# 方式二：手动部署
docker-compose -f docker-compose.low-resource.yml up -d
```

### 4. 验证部署

```bash
# 检查服务状态
docker-compose -f docker-compose.low-resource.yml ps

# 健康检查
curl http://localhost:8082/actuator/health
curl http://localhost:9200/_cluster/health

# 查看资源使用
docker stats --no-stream
```

## 熔断限流配置

### 已配置的熔断器

| 熔断器 | 失败率阈值 | 超时阈值 | 用途 |
|--------|-----------|----------|------|
| knowledgeService | 60% | 5秒 | 知识库API |
| codeIntelligenceService | 70% | 10秒 | 代码审查API |
| aiService | 80% | 30秒 | Qwen AI调用 |
| elasticsearchService | 50% | 2秒 | ES搜索 |
| kafkaService | 40% | - | Kafka消息 |

### 已配置的限流器

| 限流器 | 速率 | 周期 | 用途 |
|--------|------|------|------|
| apiGlobal | 50 req/s | 1秒 | 全局API |
| documentUpload | 5 req | 10秒 | 文档上传 |
| qaRequest | 10 req/s | 1秒 | AI问答 |
| codeReview | 3 req/s | 1秒 | 代码审查 |
| searchRequest | 30 req/s | 1秒 | 搜索请求 |

## 监控命令

```bash
# 实时查看日志
docker-compose -f docker-compose.low-resource.yml logs -f --tail=100

# 查看特定服务日志
docker logs -f kb-api-gateway
docker logs -f kb-knowledge-mentor

# 资源使用监控
watch -n 2 'docker stats --no-stream'

# 查看容器健康状态
docker inspect --format='{{.State.Health.Status}}' kb-api-gateway
```

## 常见问题解决

### 内存不足

```bash
# 创建 swap
sudo fallocate -l 1G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
```

### 服务启动失败

```bash
# 查看详细日志
docker-compose -f docker-compose.low-resource.yml logs [service-name]

# 重启服务
docker-compose -f docker-compose.low-resource.yml restart [service-name]

# 完全重建
docker-compose -f docker-compose.low-resource.yml down
docker-compose -f docker-compose.low-resource.yml up -d --force-recreate
```

### Elasticsearch 内存问题

编辑 `docker-compose.low-resource.yml`:
```yaml
elasticsearch:
  environment:
    ES_JAVA_OPTS: -Xms128m -Xmx128m  # 降低内存
```

## 性能优化建议

### 1. 数据库优化

```sql
-- 在 PostgreSQL 中设置
ALTER SYSTEM SET shared_buffers = '64MB';
ALTER SYSTEM SET effective_cache_size = '128MB';
ALTER SYSTEM SET work_mem = '8MB';
ALTER SYSTEM SET max_connections = 50;
```

### 2. Redis 优化

```yaml
# 已在 docker-compose 中配置
command: >
  redis-server
  --maxmemory 64mb
  --maxmemory-policy allkeys-lru
```

### 3. JVM 优化

已在各服务的环境变量中配置:
```bash
JAVA_OPTS=-Xms128m -Xmx256m -XX:+UseG1GC -XX:MaxGCPauseMillis=200
```

### 4. Nginx 优化

已启用 gzip 压缩和静态文件缓存。

## 生产环境建议

### SSL 证书配置

```bash
# 安装 certbot
sudo apt install certbot python3-certbot-nginx -y

# 获取证书
sudo certbot --nginx -d your-domain.com
```

### 自动备份

```bash
# 添加到 crontab
0 2 * * * /path/to/backup.sh
```

### 日志轮转

```bash
# 配置日志轮转
sudo nano /etc/logrotate.d/docker-containers
```

## 扩容方案

当业务增长时，可以考虑：

1. **垂直扩容**: 升级服务器到 4核4G
2. **水平扩容**: 将服务分离到多台服务器
3. **混合部署**: 将非核心服务移到云端

## 支持与帮助

- 查看日志: `docker-compose logs -f`
- 健康检查: `curl http://localhost:8082/actuator/health`
- 监控指标: `docker stats`
- 配置文件: `.env.prod`
