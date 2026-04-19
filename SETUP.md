# 企业知识库系统 - 运行环境搭建文档

## 1. 环境要求

### 1.1 硬件要求

| 组件 | 最低配置 | 推荐配置 |
|------|----------|----------|
| CPU | 4核心 | 8核心+ |
| 内存 | 16GB | 32GB+ |
| 磁盘 | 50GB SSD | 100GB+ SSD |
| 网络 | 100Mbps | 1Gbps |

**资源分配建议：**
- PostgreSQL: 2GB内存
- Elasticsearch: 4GB内存
- Redis: 1GB内存
- Kafka: 2GB内存
- MinIO: 1GB内存
- 应用: 4GB内存

### 1.2 软件要求

| 软件 | 最低版本 | 推荐版本 | 用途 |
|------|----------|----------|------|
| JDK | 17 | 17 LTS | 后端运行环境 |
| Maven | 3.8.0 | 3.9.x | 后端构建 |
| Node.js | 18.x | 20 LTS | 前端运行环境 |
| Docker | 20.10.x | 24.x+ | 容器化部署 |
| Docker Compose | 2.0.x | 2.20.x+ | 服务编排 |
| Git | 2.30.x | Latest | 版本控制 |

### 1.3 操作系统支持

- **推荐**：Ubuntu 22.04 LTS / Windows 11 / macOS 13+
- **兼容**：CentOS 8+ / Debian 11+ / Windows 10+

---

## 2. 基础设施部署

### 2.1 快速开始（Docker Compose）

**一键启动所有服务：**
```bash
# 克隆项目
git clone <repository-url>
cd knowledge-base-system

# 启动所有基础设施服务
docker-compose up -d

# 查看服务状态
docker-compose ps

# 查看日志
docker-compose logs -f
```

**服务说明：**
- PostgreSQL: `localhost:5432`
- Elasticsearch: `localhost:9200`
- Redis: `localhost:6379`
- Kafka: `localhost:9092`
- MinIO: `localhost:9000` (API), `localhost:9001` (Console)

### 2.2 PostgreSQL数据库

#### 使用Docker启动

```bash
# 创建数据卷
docker volume create postgres_data

# 启动PostgreSQL
docker run -d \
  --name kb-postgres \
  -e POSTGRES_DB=knowledge_base \
  -e POSTGRES_USER=kb_user \
  -e POSTGRES_PASSWORD=kb_password \
  -e POSTGRES_INITDB_ARGS="-E UTF8 --locale=C" \
  -p 5432:5432 \
  -v postgres_data:/var/lib/postgresql/data \
  postgres:15.7-alpine

# 验证连接
docker exec -it kb-postgres psql -U kb_user -d knowledge_base -c "SELECT version();"
```

#### 手动安装

**Ubuntu/Debian:**
```bash
sudo apt update
sudo apt install postgresql-15 postgresql-contrib-15

# 启动服务
sudo systemctl start postgresql
sudo systemctl enable postgresql

# 创建数据库和用户
sudo -u postgres psql
```

```sql
-- 在psql中执行
CREATE DATABASE knowledge_base;
CREATE USER kb_user WITH PASSWORD 'kb_password';
GRANT ALL PRIVILEGES ON DATABASE knowledge_base TO kb_user;
\q
```

**Windows:**
1. 下载PostgreSQL安装包：https://www.postgresql.org/download/windows/
2. 运行安装程序，按提示安装
3. 使用pgAdmin创建数据库

**macOS:**
```bash
brew install postgresql@15
brew services start postgresql@15

# 创建数据库
createdb knowledge_base
```

#### 数据库初始化

```bash
# 连接到数据库
psql -U kb_user -d knowledge_base

# 创建扩展
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 验证
\dx
```

#### 创建索引（可选，性能优化）

```sql
-- 文档表索引
CREATE INDEX idx_documents_uploaded_by ON documents(uploaded_by);
CREATE INDEX idx_documents_status ON documents(status);
CREATE INDEX idx_documents_created_at ON documents(created_at DESC);

-- 权限表索引
CREATE INDEX idx_document_permissions_document_id ON document_permissions(document_id);
CREATE INDEX idx_document_permissions_user_id ON document_permissions(user_id);
CREATE INDEX idx_document_permissions_lookup ON document_permissions USING GIN (document_id, user_id);

-- 问答历史索引
CREATE INDEX idx_qa_history_user_id ON qa_history(user_id);
CREATE INDEX idx_qa_history_created_at ON qa_history(created_at DESC);
```

### 2.3 Elasticsearch

#### 使用Docker启动

```bash
# 创建数据卷
docker volume create es_data

# 启动Elasticsearch
docker run -d \
  --name kb-elasticsearch \
  -e discovery.type=single-node \
  -e xpack.security.enabled=false \
  -e "ES_JAVA_OPTS=-Xms1g -Xmx1g" \
  -e cluster.name=knowledge-base-cluster \
  -p 9200:9200 \
  -p 9300:9300 \
  -v es_data:/usr/share/elasticsearch/data \
  docker.elastic.co/elasticsearch/elasticsearch:8.11.0

# 等待ES启动（约30-60秒）
curl http://localhost:9200/_cluster/health
```

#### 验证安装

```bash
# 检查集群健康
curl http://localhost:9200/_cluster/health?pretty

# 检查节点信息
curl http://localhost:9200/_nodes/stats?pretty

# 应该返回类似：
# {
#   "cluster_name" : "knowledge-base-cluster",
#   "status" : "green",
#   "number_of_nodes" : 1,
#   "number_of_data_nodes" : 1
# }
```

#### 创建索引

```bash
# 方式1：通过应用自动创建（推荐）
# 应用启动时会自动创建索引

# 方式2：手动创建
curl -X PUT http://localhost:9200/kb_document_chunks \
  -H 'Content-Type: application/json' \
  -d '{
    "settings": {
      "number_of_shards": 3,
      "number_of_replicas": 1
    },
    "mappings": {
      "properties": {
        "content": {
          "type": "text",
          "fields": {
            "keyword": {
              "type": "keyword"
            }
          }
        },
        "content_vector": {
          "type": "dense_vector",
          "dims": 1536,
          "index": true,
          "similarity": "cosine"
        },
        "document_id": {
          "type": "long"
        },
        "chunk_id": {
          "type": "long"
        },
        "title": {
          "type": "text"
        },
        "file_name": {
          "type": "keyword"
        },
        "file_type": {
          "type": "keyword"
        },
        "uploaded_by": {
          "type": "long"
        },
        "is_public": {
          "type": "boolean"
        },
        "created_at": {
          "type": "date"
        }
      }
    }
  }'
```

#### 故障排查

**问题1：Elasticsearch启动失败**
```bash
# 检查内存
docker stats kb-elasticsearch

# 增加JVM堆内存
docker run -e "ES_JAVA_OPTS=-Xms2g -Xmx2g" ...

# 检查日志
docker logs kb-elasticsearch
```

**问题2：无法连接到Elasticsearch**
```bash
# 检查端口是否被占用
netstat -tuln | grep 9200

# 检查防火墙
sudo ufw allow 9200
```

### 2.4 Redis

#### 使用Docker启动

```bash
# 创建数据卷
docker volume create redis_data

# 启动Redis
docker run -d \
  --name kb-redis \
  -p 6379:6379 \
  -v redis_data:/data \
  redis:7.2.5-alpine \
  redis-server --appendonly yes

# 验证连接
docker exec -it kb-redis redis-cli ping
# 应该返回：PONG
```

#### 手动安装

**Ubuntu/Debian:**
```bash
sudo apt update
sudo apt install redis-server

# 启动服务
sudo systemctl start redis-server
sudo systemctl enable redis-server

# 测试连接
redis-cli ping
```

**Windows:**
1. 下载Redis for Windows：https://github.com/microsoftarchive/redis/releases
2. 解压并运行redis-server.exe

**macOS:**
```bash
brew install redis
brew services start redis

# 测试连接
redis-cli ping
```

#### 配置Redis（可选）

```bash
# 编辑配置文件
sudo vim /etc/redis/redis.conf

# 设置密码
requirepass your_redis_password

# 最大内存
maxmemory 2gb

# 内存策略
maxmemory-policy allkeys-lru

# 重启服务
sudo systemctl restart redis-server
```

### 2.5 MinIO对象存储

#### 使用Docker启动

```bash
# 创建数据卷
docker volume create minio_data

# 启动MinIO
docker run -d \
  --name kb-minio \
  -e MINIO_ROOT_USER=minioadmin \
  -e MINIO_ROOT_PASSWORD=minioadmin \
  -p 9000:9000 \
  -p 9001:9001 \
  -v minio_data:/data \
  minio/minio:RELEASE.2024-04-18T00-36-38Z \
  server /data --console-address ":9001"

# 验证连接
curl -I http://localhost:9000/minio/health/live
```

#### 访问MinIO控制台

1. 打开浏览器访问：http://localhost:9001
2. 默认用户名：`minioadmin`
3. 默认密码：`minioadmin`
4. 登录后创建bucket：`knowledge-base-documents`

#### 创建Bucket

**方式1：通过控制台**
1. 登录MinIO控制台
2. 点击右侧 "Buckets" → "Create Bucket"
3. 输入bucket名称：`knowledge-base-documents`
4. 点击 "Create Bucket"

**方式2：通过MC客户端**
```bash
# 下载MC客户端
wget https://dl.min.io/client/mc/release/linux-amd64/mc
chmod +x mc
sudo mv mc /usr/local/bin/

# 配置MinIO别名
mc alias set local http://localhost:9000 minioadmin minioadmin

# 创建bucket
mc mb local/knowledge-base-documents

# 设置bucket策略（公共访问，生产环境不推荐）
mc anonymous set download local/knowledge-base-documents

# 列出bucket
mc ls local/
```

#### 验证MinIO配置

```bash
# 检查MinIO服务状态
docker ps | grep minio

# 查看MinIO日志
docker logs kb-minio

# 测试上传文件
echo "Hello MinIO" > test.txt
mc cp test.txt local/knowledge-base-documents/

# 列出文件
mc ls local/knowledge-base-documents/

# 删除测试文件
mc rm local/knowledge-base-documents/test.txt
```

#### 配置MinIO客户端（Java）

```yaml
# application.yml
minio:
  endpoint: ${MINIO_ENDPOINT:http://localhost:9000}
  access-key: ${MINIO_ACCESS_KEY:minioadmin}
  secret-key: ${MINIO_SECRET_KEY:minioadmin}
  bucket-name: ${MINIO_BUCKET_NAME:knowledge-base-documents}
  part-size: ${MINIO_PART_SIZE:5242880} # 5MB

# 分片上传配置
app:
  upload:
    chunked-upload:
      enabled: ${CHUNKED_UPLOAD_ENABLED:false}
      min-chunk-size: ${MIN_CHUNK_SIZE:5242880} # 5MB
      max-chunk-size: ${MAX_CHUNK_SIZE:10485760} # 10MB
      max-file-size: ${MAX_FILE_SIZE:1073741824} # 1GB
      expire-hours: ${UPLOAD_EXPIRE_HOURS:24}
      concurrent-chunks: ${CONCURRENT_CHUNKS:3}
      min-size-threshold: ${MIN_SIZE_THRESHOLD:52428800} # 50MB
```

#### 环境变量配置

```bash
# .env
MINIO_ENDPOINT=http://localhost:9000
MINIO_ACCESS_KEY=minioadmin
MINIO_SECRET_KEY=minioadmin
MINIO_BUCKET_NAME=knowledge-base-documents

# 分片上传配置
CHUNKED_UPLOAD_ENABLED=true
MIN_CHUNK_SIZE=5242880
MAX_CHUNK_SIZE=10485760
MAX_FILE_SIZE=1073741824
UPLOAD_EXPIRE_HOURS=24
CONCURRENT_CHUNKS=3
MIN_SIZE_THRESHOLD=52428800
```

#### 故障排查

**问题1：MinIO启动失败**
```bash
# 检查端口占用
netstat -tuln | grep 9000

# 检查磁盘空间
df -h

# 查看详细日志
docker logs kb-minio --tail 100
```

**问题2：无法访问MinIO控制台**
```bash
# 检查防火墙
sudo ufw allow 9001

# 检查容器状态
docker ps | grep minio

# 重启MinIO
docker restart kb-minio
```

**问题3：分片上传失败**
```bash
# 检查bucket是否存在
mc ls local/

# 检查bucket权限
mc anonymous get local/knowledge-base-documents

# 检查Redis BitMap状态
redis-cli
> KEYS upload:status:*
> GETBIT upload:status:xxx 0
```

### 2.5 Apache Kafka

#### 使用Docker启动（推荐）

```bash
# 创建数据卷
docker volume create zookeeper_data
docker volume create zookeeper_log
docker volume create kafka_data

# 启动Zookeeper
docker run -d \
  --name kb-zookeeper \
  -e ZOOKEEPER_CLIENT_PORT=2181 \
  -e ZOOKEEPER_TICK_TIME=2000 \
  -p 2181:2181 \
  -v zookeeper_data:/var/lib/zookeeper/data \
  -v zookeeper_log:/var/lib/zookeeper/log \
  confluentinc/cp-zookeeper:7.5.0

# 等待Zookeeper启动
sleep 10

# 启动Kafka
docker run -d \
  --name kb-kafka \
  --link kb-zookeeper:zookeeper \
  -e KAFKA_BROKER_ID=1 \
  -e KAFKA_ZOOKEEPER_CONNECT=zookeeper:2181 \
  -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9092 \
  -e KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1 \
  -p 9092:9092 \
  -v kafka_data:/var/lib/kafka/data \
  confluentinc/cp-kafka:7.5.3

# 验证Kafka
docker exec -it kb-kafka kafka-topics.sh --list \
  --bootstrap-server localhost:9092
```

#### 创建Kafka主题

```bash
# 创建文档处理主题
docker exec -it kb-kafka kafka-topics.sh --create \
  --topic document-processing \
  --bootstrap-server localhost:9092 \
  --partitions 3 \
  --replication-factor 1

# 创建死信队列主题
docker exec -it kb-kafka kafka-topics.sh --create \
  --topic document-processing-dlq \
  --bootstrap-server localhost:9092 \
  --partitions 1 \
  --replication-factor 1

# 列出所有主题
docker exec -it kb-kafka kafka-topics.sh --list \
  --bootstrap-server localhost:9092

# 查看主题详情
docker exec -it kb-kafka kafka-topics.sh --describe \
  --topic document-processing \
  --bootstrap-server localhost:9092
```

#### 测试Kafka

```bash
# 生产者
docker exec -it kb-kafka kafka-console-producer.sh \
  --topic document-processing \
  --bootstrap-server localhost:9092

# 消费者
docker exec -it kb-kafka kafka-console-consumer.sh \
  --topic document-processing \
  --bootstrap-server localhost:9092 \
  --from-beginning
```

#### 故障排查

**问题1：Kafka无法启动**
```bash
# 检查Zookeeper是否启动
docker logs kb-zookeeper

# 检查Kafka日志
docker logs kb-kafka

# 检查网络连接
docker exec -it kb-kafka ping kb-zookeeper
```

**问题2：消息发送失败**
```bash
# 检查主题是否存在
docker exec -it kb-kafka kafka-topics.sh --list \
  --bootstrap-server localhost:9092

# 检查消费者组
docker exec -it kb-kafka kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 \
  --list
```

### 2.6 MinIO（对象存储，可选）

#### 添加到docker-compose.yml

```yaml
minio:
  image: minio/minio:RELEASE.2024-04-18T00-36-38Z
  container_name: kb-minio
  environment:
    MINIO_ROOT_USER: minioadmin
    MINIO_ROOT_PASSWORD: minioadmin
  ports:
    - "9000:9000"
    - "9001:9001"
  volumes:
    - minio_data:/data
  command: server /data --console-address ":9001"
  networks:
    - kb-network

volumes:
  minio_data:
    driver: local
```

#### 启动MinIO

```bash
# 启动MinIO
docker-compose up -d minio

# 访问控制台
open http://localhost:9001

# 登录凭据
# 用户名: minioadmin
# 密码: minioadmin
```

#### 创建Bucket

```bash
# 安装mc客户端
brew install minio/stable/mc  # macOS
# 或下载：https://dl.min.io/client/mc/release/

# 配置别名
mc alias set local http://localhost:9000 minioadmin minioadmin

# 创建bucket
mc mb local/knowledge-base

# 设置公开访问
mc anonymous set download local/knowledge-base

# 验证
mc ls local/
```

---

## 3. 后端环境搭建

### 3.1 配置环境变量

```bash
# 复制环境变量模板
cp .env.example .env

# 编辑.env文件
vim .env
```

#### 必需配置项

```env
# ===== 数据库配置 =====
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/knowledge_base
SPRING_DATASOURCE_USERNAME=kb_user
SPRING_DATASOURCE_PASSWORD=kb_password

# ===== Elasticsearch配置 =====
ELASTICSEARCH_URIS=http://localhost:9200
ELASTICSEARCH_INDEX_NAME=kb_document_chunks

# ===== Redis配置 =====
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=

# ===== Kafka配置 =====
SPRING_KAFKA_BOOTSTRAP_SERVERS=localhost:9092

# ===== 千问API配置 =====
QWEN_API_KEY=your_qwen_api_key_here
QWEN_API_URL=https://dashscope.aliyuncs.com/api/v1
QWEN_EMBEDDING_MODEL=text-embedding-v2
QWEN_CHAT_MODEL=qwen-max

# ===== JWT配置 =====
JWT_SECRET=your_256_bit_secret_key_change_this_in_production_minimum_32_characters
JWT_EXPIRATION=86400000
JWT_REFRESH_EXPIRATION=604800000

# ===== 文件上传配置 =====
SPRING_SERVLET_MULTIPART_MAX_FILE_SIZE=50MB
SPRING_SERVLET_MULTIPART_MAX_REQUEST_SIZE=50MB
UPLOAD_DIR=./uploads

# ===== WebSocket配置 =====
WEBSOCKET_ALLOWED_ORIGINS=http://localhost:5173,http://localhost:3000
WEBSOCKET_HEARTBEAT_INTERVAL=30000

# ===== 缓存配置 =====
CACHE_USER_PERMISSIONS_TTL=300000
CACHE_QA_ANSWER_TTL=3600000
CACHE_DOCUMENT_METADATA_TTL=600000

# ===== 文档处理配置 =====
DOCUMENT_CHUNK_SIZE=500
DOCUMENT_CHUNK_OVERLAP=50
DOCUMENT_PROCESSING_TIMEOUT=300000

# ===== 检索配置 =====
SEARCH_TOP_K=20
SEARCH_RRF_K=60
SEARCH_NUM_CANDIDATES=100

# ===== 日志配置 =====
LOGGING_LEVEL_ROOT=INFO
LOGGING_LEVEL_COM_COMPANY_KB=DEBUG
```

#### 获取千问API Key

1. 访问阿里云控制台：https://dashscope.console.aliyun.com/
2. 开通服务并创建API Key
3. 将API Key配置到`QWEN_API_KEY`

### 3.2 构建和启动

#### 使用Maven构建

```bash
# 进入后端目录
cd backend

# 清理并构建
mvn clean install

# 跳过测试构建
mvn clean install -DskipTests

# 查看依赖树
mvn dependency:tree
```

#### 启动应用

**方式1：使用Maven插件（开发环境）**
```bash
cd backend/knowledge-base-api
mvn spring-boot:run
```

**方式2：使用JAR包（生产环境）**
```bash
# 打包
cd backend
mvn clean package -DskipTests

# 启动
java -jar knowledge-base-api/target/knowledge-base-api-1.0.0-SNAPSHOT.jar

# 指定配置文件
java -jar knowledge-base-api/target/knowledge-base-api-1.0.0-SNAPSHOT.jar \
  --spring.profiles.active=prod

# 指定JVM参数
java -Xms1g -Xmx2g \
  -jar knowledge-base-api/target/knowledge-base-api-1.0.0-SNAPSHOT.jar
```

**方式3：使用IDE（开发环境）**
- IntelliJ IDEA：右键`KbApiApplication.java` -> Run
- Eclipse：右键`KbApiApplication.java` -> Run As -> Java Application

### 3.3 验证后端

#### 健康检查

```bash
# 检查应用健康状态
curl http://localhost:8080/api/actuator/health

# 预期返回：
# {
#   "status": "UP"
# }
```

#### 测试Elasticsearch连接

```bash
# 获取ES信息
curl http://localhost:8080/api/es/info

# 检查索引是否存在
curl http://localhost:8080/api/es/index/exists
```

#### 测试Kafka连接

```bash
# 查看Kafka主题
curl http://localhost:8080/api/kafka/topics
```

#### 测试认证接口

```bash
# 注册用户
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "test@example.com",
    "password": "password123",
    "email": "test@example.com"
  }'

# 登录
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "test@example.com",
    "password": "password123"
  }'
```

### 3.4 常见问题排查

#### 问题1：数据库连接失败

**症状：**
```
java.sql.SQLException: Connection refused
```

**解决方案：**
```bash
# 检查数据库是否启动
docker-compose ps postgres

# 测试连接
psql -U kb_user -d knowledge_base -h localhost

# 检查防火墙
sudo ufw allow 5432

# 检查配置
cat .env | grep DATASOURCE
```

#### 问题2：Elasticsearch连接失败

**症状：**
```
Connection refused: localhost/127.0.0.1:9200
```

**解决方案：**
```bash
# 检查ES是否启动
curl http://localhost:9200/_cluster/health

# 检查ES日志
docker logs kb-elasticsearch

# 检查配置
cat .env | grep ELASTICSEARCH
```

#### 问题3：Kafka消息发送失败

**症状：**
```
TimeoutException: Topic 'document-processing' not present
```

**解决方案：**
```bash
# 创建主题
docker exec -it kb-kafka kafka-topics.sh --create \
  --topic document-processing \
  --bootstrap-server localhost:9092 \
  --partitions 3 \
  --replication-factor 1

# 验证主题
docker exec -it kb-kafka kafka-topics.sh --list \
  --bootstrap-server localhost:9092
```

#### 问题4：千问API调用失败

**症状：**
```
401 Unauthorized: Invalid API Key
```

**解决方案：**
```bash
# 检查API Key配置
cat .env | grep QWEN_API_KEY

# 测试API Key
curl -X POST https://dashscope.aliyuncs.com/api/v1/services/embeddings/text-embedding/text-embedding \
  -H "Authorization: Bearer YOUR_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "text-embedding-v2",
    "input": {
      "texts": ["测试"]
    }
  }'
```

---

## 4. 前端环境搭建

### 4.1 安装依赖

```bash
# 进入前端目录
cd frontend

# 使用npm安装
npm install

# 或使用pnpm（推荐）
npm install -g pnpm
pnpm install

# 或使用yarn
npm install -g yarn
yarn install
```

### 4.2 配置环境变量

```bash
# 创建.env文件
cat > .env << EOF
VITE_API_BASE_URL=http://localhost:8080/api
VITE_WS_URL=ws://localhost:8080/api/ws/qa
EOF
```

### 4.3 启动开发服务器

```bash
# 使用npm
npm run dev

# 或使用pnpm
pnpm dev

# 或使用yarn
yarn dev

# 访问应用
# 默认地址：http://localhost:5173
```

### 4.4 构建生产版本

```bash
# 构建
npm run build

# 预览构建结果
npm run preview

# 输出目录：dist/
```

### 4.5 验证前端

#### 访问应用

```bash
# 打开浏览器
open http://localhost:5173
```

#### 检查网络请求

1. 打开浏览器开发者工具（F12）
2. 切换到Network标签
3. 刷新页面
4. 检查API请求是否成功

#### 测试WebSocket连接

1. 打开浏览器控制台（Console）
2. 输入以下代码：

```javascript
const ws = new WebSocket('ws://localhost:8080/api/ws/qa');

ws.onopen = () => {
  console.log('WebSocket连接成功');
};

ws.onerror = (error) => {
  console.error('WebSocket连接失败', error);
};
```

### 4.6 常见问题排查

#### 问题1：npm install失败

**症状：**
```
npm ERR! network request failed
```

**解决方案：**
```bash
# 使用国内镜像
npm config set registry https://registry.npmmirror.com

# 或使用cnpm
npm install -g cnpm --registry=https://registry.npmmirror.com
cnpm install
```

#### 问题2：无法连接后端API

**症状：**
```
net::ERR_CONNECTION_REFUSED
```

**解决方案：**
```bash
# 检查后端是否启动
curl http://localhost:8080/api/actuator/health

# 检查环境变量
cat .env

# 检查Vite配置
cat vite.config.ts

# 检查CORS配置
# 在application.yml中确认allowed-origins
```

#### 问题3：WebSocket连接失败

**症状：**
```
WebSocket connection failed
```

**解决方案：**
```bash
# 检查WebSocket配置
cat .env | grep WS_URL

# 检查后端WebSocket配置
curl -i -N \
  -H "Connection: Upgrade" \
  -H "Upgrade: websocket" \
  http://localhost:8080/api/ws/qa

# 检查CORS配置
cat backend/knowledge-base-api/src/main/resources/application.yml | grep websocket
```

---

## 5. 开发工具配置

### 5.1 IntelliJ IDEA

#### 安装插件

推荐安装以下插件：
- Lombok Plugin
- MyBatis Plugin
- Maven Helper
- String Manipulation
- Rainbow Brackets
- GitToolBox

#### 配置Maven

```bash
# Settings -> Build, Execution, Deployment -> Build Tools -> Maven
# Maven home directory: /usr/local/Cellar/maven/3.9.x/libexec
# User settings file: ~/.m2/settings.xml
```

#### 配置代码模板

```bash
# Settings -> Editor -> Live Templates
# 添加自定义模板，例如：
# psvm: public static void main(String[] args) {}
# sout: System.out.println();
```

### 5.2 VS Code

#### 安装扩展

推荐安装以下扩展：
- ESLint
- Prettier
- TypeScript Vue Plugin (Volar)
- Vue Language Features (Volar)
- Auto Rename Tag
- Bracket Pair Colorizer
- GitLens
- Material Icon Theme

#### 配置settings.json

```json
{
  "editor.formatOnSave": true,
  "editor.defaultFormatter": "esbenp.prettier-vscode",
  "editor.codeActionsOnSave": {
    "source.fixAll.eslint": true
  },
  "typescript.tsdk": "node_modules/typescript/lib",
  "vite.devServer.port": 5173
}
```

---

## 6. 完整启动流程

### 6.1 首次启动

```bash
# 1. 启动基础设施
docker-compose up -d

# 2. 等待服务就绪（约30-60秒）
docker-compose ps

# 3. 配置环境变量
cp .env.example .env
vim .env  # 配置千问API Key等

# 4. 启动后端
cd backend
mvn clean install
mvn spring-boot:run

# 5. 等待后端启动（约10秒）
# 新开一个终端

# 6. 启动前端
cd frontend
npm install
npm run dev

# 7. 访问应用
# 后端：http://localhost:8080
# 前端：http://localhost:5173
```

### 6.2 日常开发启动

```bash
# 启动基础设施（如果未启动）
docker-compose up -d

# 启动后端
cd backend
mvn spring-boot:run

# 启动前端（新终端）
cd frontend
npm run dev
```

### 6.3 停止服务

```bash
# 停止前端（Ctrl+C）

# 停止后端（Ctrl+C）

# 停止基础设施
docker-compose down

# 停止基础设施并删除数据卷
docker-compose down -v
```

---

## 7. 健康检查脚本

### check-health.sh

```bash
#!/bin/bash

echo "===== 企业知识库系统健康检查 ====="

# 检查PostgreSQL
echo "检查PostgreSQL..."
if docker exec kb-postgres pg_isready -U kb_user > /dev/null 2>&1; then
  echo "✓ PostgreSQL运行正常"
else
  echo "✗ PostgreSQL未运行"
fi

# 检查Elasticsearch
echo "检查Elasticsearch..."
if curl -s http://localhost:9200/_cluster/health | grep -q '"status":"green"\|"status":"yellow"'; then
  echo "✓ Elasticsearch运行正常"
else
  echo "✗ Elasticsearch未运行"
fi

# 检查Redis
echo "检查Redis..."
if docker exec kb-redis redis-cli ping > /dev/null 2>&1; then
  echo "✓ Redis运行正常"
else
  echo "✗ Redis未运行"
fi

# 检查Kafka
echo "检查Kafka..."
if docker exec kb-kafka kafka-broker-api-versions --bootstrap-server localhost:9092 > /dev/null 2>&1; then
  echo "✓ Kafka运行正常"
else
  echo "✗ Kafka未运行"
fi

# 检查后端
echo "检查后端..."
if curl -s http://localhost:8080/api/actuator/health | grep -q '"status":"UP"'; then
  echo "✓ 后端运行正常"
else
  echo "✗ 后端未运行"
fi

# 检查前端
echo "检查前端..."
if curl -s http://localhost:5173 > /dev/null 2>&1; then
  echo "✓ 前端运行正常"
else
  echo "✗ 前端未运行"
fi

echo "===== 健康检查完成 ====="
```

**使用方法：**
```bash
chmod +x check-health.sh
./check-health.sh
```

---

## 8. 附录

### A. 端口占用列表

| 服务 | 端口 | 用途 |
|------|------|------|
| PostgreSQL | 5432 | 数据库 |
| Elasticsearch | 9200 | HTTP API |
| Elasticsearch | 9300 | 节点通信 |
| Redis | 6379 | 缓存 |
| Zookeeper | 2181 | Kafka协调 |
| Kafka | 9092 | 消息队列 |
| Kafka | 9093 | 外部访问 |
| MinIO | 9000 | 对象存储API |
| MinIO Console | 9001 | 管理控制台 |
| 后端应用 | 8080 | REST API |
| 前端应用 | 5173 | Web界面 |

### B. 默认账号密码

| 服务 | 用户名 | 密码 |
|------|--------|------|
| PostgreSQL | kb_user | kb_password |
| MinIO | minioadmin | minioadmin |
| Elasticsearch | - | - (无认证) |

### C. 参考资源

- [PostgreSQL官方文档](https://www.postgresql.org/docs/)
- [Elasticsearch官方文档](https://www.elastic.co/guide/en/elasticsearch/reference/current/index.html)
- [Redis官方文档](https://redis.io/documentation)
- [Apache Kafka官方文档](https://kafka.apache.org/documentation/)
- [Spring Boot官方文档](https://spring.io/projects/spring-boot)

---

**文档版本**：v1.0
**最后更新**：2026-04-08
**维护者**：DevOps团队
