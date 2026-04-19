# 企业知识库系统 - 部署文档

## 1. 部署前准备

### 1.1 服务器要求

#### 生产环境

| 组件 | 最低配置 | 推荐配置 |
|------|----------|----------|
| CPU | 8核 | 16核+ |
| 内存 | 32GB | 64GB+ |
| 磁盘 | 500GB SSD | 1TB+ SSD |
| 带宽 | 100Mbps | 1Gbps |

**资源分配：**
- PostgreSQL: 8GB内存
- Elasticsearch: 16GB内存
- Redis: 4GB内存
- Kafka: 4GB内存
- MinIO: 4GB内存
- 应用: 8GB内存

#### 测试环境

| 组件 | 最低配置 | 推荐配置 |
|------|----------|----------|
| CPU | 4核 | 8核 |
| 内存 | 16GB | 32GB |
| 磁盘 | 200GB SSD | 500GB SSD |
| 带宽 | 100Mbps | 100Mbps |

### 1.2 网络规划

#### 架构图

```
                    Internet
                       │
                       │
                  ┌────▼────┐
                  │  Nginx  │ :80/443
                  │  (LB)   │
                  └────┬────┘
                       │
        ┌──────────────┼──────────────┐
        │              │              │
   ┌────▼────┐   ┌────▼────┐   ┌────▼────┐
   │ Frontend│   │ Backend │   │Backend  │
   │  :3000  │   │  :8080  │   │  :8080  │
   └─────────┘   └────┬────┘   └─────────┘
                      │
        ┌─────────────┼─────────────┬─────────────┐
        │             │             │             │
   ┌────▼────┐  ┌────▼────┐  ┌────▼────┐  ┌────▼────┐
   │   PG    │  │   ES    │  │  Redis  │  │  MinIO  │
   │  :5432  │  │  :9200  │  │  :6379  │  │  :9000  │
   └─────────┘  └─────────┘  └─────────┘  │  :9001  │
                                           └─────────┘
                      │
               ┌──────▼──────┐
               │   Kafka     │
               │   :9092     │
               └─────────────┘
```

#### 端口规划

| 服务 | 内部端口 | 外部端口 | 说明 |
|------|----------|----------|------|
| Nginx | 80/443 | 80/443 | 反向代理 |
| Frontend | 3000 | - | 静态资源 |
| Backend | 8080 | - | API服务 |
| PostgreSQL | 5432 | - | 数据库 |
| Elasticsearch | 9200 | - | 搜索引擎 |
| Redis | 6379 | - | 缓存 |
| Kafka | 9092 | - | 消息队列 |
| MinIO | 9000/9001 | - | 对象存储（API/控制台） |

### 1.3 安全检查清单

**部署前必须完成：**

- [ ] 修改所有默认密码
- [ ] 配置HTTPS证书（Let's Encrypt或商业证书）
- [ ] 限制数据库访问IP（仅允许应用服务器）
- [ ] 关闭调试模式（`spring.profiles.active=prod`）
- [ ] 配置防火墙规则
- [ ] 设置日志轮转
- [ ] 配置数据库备份
- [ ] 配置Elasticsearch快照
- [ ] 设置监控告警
- [ ] 配置DDoS防护
- [ ] 启用审计日志
- [ ] 定期安全更新

### 1.4 DNS配置

```bash
# A记录
kb.yourdomain.com      A    1.2.3.4    # 应用服务器

# CNAME记录（可选）
www.kb.yourdomain.com  CNAME  kb.yourdomain.com
api.kb.yourdomain.com  CNAME  kb.yourdomain.com
```

---

## 2. Docker Compose部署

### 2.1 准备部署文件

```bash
# 创建部署目录
mkdir -p /opt/knowledge-base
cd /opt/knowledge-base

# 复制部署文件
cp docker-compose.prod.yml docker-compose.yml
cp .env.prod .env
```

### 2.2 生产环境配置

#### docker-compose.prod.yml

```yaml
version: '3.8'

services:
  # PostgreSQL数据库
  postgres:
    image: postgres:15.7-alpine
    container_name: kb-postgres
    environment:
      POSTGRES_DB: ${POSTGRES_DB}
      POSTGRES_USER: ${POSTGRES_USER}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
      POSTGRES_INITDB_ARGS: "-E UTF8 --locale=C"
    ports:
      - "127.0.0.1:5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./backups:/backups
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${POSTGRES_USER} -d ${POSTGRES_DB}"]
      interval: 10s
      timeout: 5s
      retries: 5
    restart: unless-stopped
    networks:
      - kb-network

  # Elasticsearch
  elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:8.11.0
    container_name: kb-elasticsearch
    environment:
      - discovery.type=single-node
      - xpack.security.enabled=false
      - "ES_JAVA_OPTS=-Xms4g -Xmx4g"
      - cluster.name=knowledge-base-cluster
    ports:
      - "127.0.0.1:9200:9200"
    volumes:
      - es_data:/usr/share/elasticsearch/data
      - ./backups/es:/backups
    healthcheck:
      test: ["CMD-SHELL", "curl -f http://localhost:9200/_cluster/health || exit 1"]
      interval: 30s
      timeout: 10s
      retries: 5
      start_period: 40s
    restart: unless-stopped
    networks:
      - kb-network

  # Redis
  redis:
    image: redis:7-alpine
    container_name: kb-redis
    command: redis-server --appendonly yes --requirepass ${REDIS_PASSWORD}
    ports:
      - "127.0.0.1:6379:6379"
    volumes:
      - redis_data:/data
    healthcheck:
      test: ["CMD", "redis-cli", "--raw", "incr", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5
    restart: unless-stopped
    networks:
      - kb-network

  # Zookeeper
  zookeeper:
    image: confluentinc/cp-zookeeper:7.5.0
    container_name: kb-zookeeper
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
      ZOOKEEPER_TICK_TIME: 2000
    volumes:
      - zookeeper_data:/var/lib/zookeeper/data
      - zookeeper_log:/var/lib/zookeeper/log
    restart: unless-stopped
    networks:
      - kb-network

  # Kafka
  kafka:
    image: confluentinc/cp-kafka:7.5.3
    container_name: kb-kafka
    depends_on:
      - zookeeper
    ports:
      - "127.0.0.1:9092:9092"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_LOG_RETENTION_HOURS: 168
    volumes:
      - kafka_data:/var/lib/kafka/data
    healthcheck:
      test: ["CMD-SHELL", "kafka-broker-api-versions --bootstrap-server localhost:9092"]
      interval: 30s
      timeout: 10s
      retries: 5
      start_period: 60s
    restart: unless-stopped
    networks:
      - kb-network

  # 后端应用
  backend:
    image: knowledge-base-backend:latest
    container_name: kb-backend
    depends_on:
      - postgres
      - elasticsearch
      - redis
      - kafka
    ports:
      - "127.0.0.1:8080:8080"
    env_file:
      - .env
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - JAVA_OPTS=-Xms2g -Xmx2g
    volumes:
      - ./uploads:/app/uploads
      - ./logs:/app/logs
    healthcheck:
      test: ["CMD-SHELL", "curl -f http://localhost:8080/api/actuator/health || exit 1"]
      interval: 30s
      timeout: 10s
      retries: 5
      start_period: 60s
    restart: unless-stopped
    networks:
      - kb-network

  # 前端应用
  frontend:
    image: knowledge-base-frontend:latest
    container_name: kb-frontend
    ports:
      - "127.0.0.1:3000:80"
    restart: unless-stopped
    networks:
      - kb-network

  # Nginx
  nginx:
    image: nginx:alpine
    container_name: kb-nginx
    depends_on:
      - backend
      - frontend
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx/nginx.conf:/etc/nginx/nginx.conf:ro
      - ./nginx/conf.d:/etc/nginx/conf.d:ro
      - ./nginx/ssl:/etc/nginx/ssl:ro
      - ./nginx/logs:/var/log/nginx
    restart: unless-stopped
    networks:
      - kb-network

volumes:
  postgres_data:
    driver: local
  es_data:
    driver: local
  redis_data:
    driver: local
  zookeeper_data:
    driver: local
  zookeeper_log:
    driver: local
  kafka_data:
    driver: local

networks:
  kb-network:
    driver: bridge
```

#### .env.prod

```env
# 生产环境配置
SPRING_PROFILES_ACTIVE=prod

# 数据库
POSTGRES_DB=knowledge_base
POSTGRES_USER=kb_user
POSTGRES_PASSWORD=CHANGE_THIS_PASSWORD

# Elasticsearch
ELASTICSEARCH_URIS=http://elasticsearch:9200
ELASTICSEARCH_INDEX_NAME=kb_document_chunks

# Redis
REDIS_HOST=redis
REDIS_PORT=6379
REDIS_PASSWORD=CHANGE_THIS_PASSWORD

# Kafka
SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:9092

# 千问API
QWEN_API_KEY=your_qwen_api_key

# JWT
JWT_SECRET=CHANGE_THIS_TO_A_SECURE_256_BIT_SECRET_KEY

# 日志
LOGGING_LEVEL_ROOT=WARN
LOGGING_LEVEL_COM_COMPANY_KB=INFO
```

### 2.3 构建镜像

#### 后端镜像

```bash
# Dockerfile.backend
FROM openjdk:17-jdk-alpine

WORKDIR /app

# 复制JAR文件
COPY backend/knowledge-base-api/target/knowledge-base-api-1.0.0-SNAPSHOT.jar app.jar

# 创建日志目录
RUN mkdir -p /app/logs

# 暴露端口
EXPOSE 8080

# 启动应用
ENTRYPOINT ["java", "-jar", "-Xms2g", "-Xmx2g", "app.jar"]
```

```bash
# 构建镜像
docker build -f Dockerfile.backend -t knowledge-base-backend:latest .

# 推送到镜像仓库
docker tag knowledge-base-backend:latest your-registry/knowledge-base-backend:latest
docker push your-registry/knowledge-base-backend:latest
```

#### 前端镜像

```bash
# Dockerfile.frontend
FROM node:20-alpine AS builder

WORKDIR /app

# 复制package文件
COPY frontend/package*.json ./

# 安装依赖
RUN npm ci --only=production

# 复制源代码
COPY frontend/ ./

# 构建
RUN npm run build

# 生产镜像
FROM nginx:alpine

# 复制构建产物
COPY --from=builder /app/dist /usr/share/nginx/html

# 复制nginx配置
COPY frontend/nginx.conf /etc/nginx/nginx.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
```

```bash
# 构建镜像
docker build -f Dockerfile.frontend -t knowledge-base-frontend:latest .

# 推送到镜像仓库
docker tag knowledge-base-frontend:latest your-registry/knowledge-base-frontend:latest
docker push your-registry/knowledge-base-frontend:latest
```

### 2.4 Nginx配置

#### nginx/nginx.conf

```nginx
user nginx;
worker_processes auto;
error_log /var/log/nginx/error.log warn;
pid /var/run/nginx.pid;

events {
    worker_connections 10240;
}

http {
    include /etc/nginx/mime.types;
    default_type application/octet-stream;

    log_format main '$remote_addr - $remote_user [$time_local] "$request" '
                    '$status $body_bytes_sent "$http_referer" '
                    '"$http_user_agent" "$http_x_forwarded_for"';

    access_log /var/log/nginx/access.log main;

    sendfile on;
    tcp_nopush on;
    tcp_nodelay on;
    keepalive_timeout 65;
    types_hash_max_size 2048;

    # Gzip压缩
    gzip on;
    gzip_vary on;
    gzip_proxied any;
    gzip_comp_level 6;
    gzip_types text/plain text/css text/xml text/javascript
               application/json application/javascript application/xml+rss;

    # 限流配置
    limit_req_zone $binary_remote_addr zone=api_limit:10m rate=100r/s;
    limit_req_zone $binary_remote_addr zone=general_limit:10m rate=50r/s;

    # 包含站点配置
    include /etc/nginx/conf.d/*.conf;
}
```

#### nginx/conf.d/kb.conf

```nginx
# 上游服务器
upstream backend {
    least_conn;
    server backend:8080 max_fails=3 fail_timeout=30s;
}

upstream frontend {
    server frontend:80;
}

# HTTP重定向到HTTPS
server {
    listen 80;
    server_name kb.yourdomain.com;
    return 301 https://$server_name$request_uri;
}

# HTTPS配置
server {
    listen 443 ssl http2;
    server_name kb.yourdomain.com;

    # SSL证书
    ssl_certificate /etc/nginx/ssl/cert.pem;
    ssl_certificate_key /etc/nginx/ssl/key.pem;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;
    ssl_prefer_server_ciphers on;

    # 安全头
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;

    # 前端静态资源
    location / {
        limit_req zone=general_limit burst=20 nodelay;
        proxy_pass http://frontend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # API代理
    location /api/ {
        limit_req zone=api_limit burst=50 nodelay;
        proxy_pass http://backend/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        # WebSocket支持
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";

        # 超时配置
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 300s;
    }

    # 健康检查
    location /health {
        access_log off;
        return 200 "healthy\n";
        add_header Content-Type text/plain;
    }
}
```

### 2.5 SSL证书配置

#### Let's Encrypt（免费）

```bash
# 安装certbot
sudo apt install certbot

# 生成证书
sudo certbot certonly --standalone -d kb.yourdomain.com

# 复制证书
sudo cp /etc/letsencrypt/live/kb.yourdomain.com/fullchain.pem nginx/ssl/cert.pem
sudo cp /etc/letsencrypt/live/kb.yourdomain.com/privkey.pem nginx/ssl/key.pem

# 设置自动续期
sudo certbot renew --dry-run
```

#### 自签名证书（测试）

```bash
# 创建证书目录
mkdir -p nginx/ssl

# 生成自签名证书
openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
  -keyout nginx/ssl/key.pem \
  -out nginx/ssl/cert.pem \
  -subj "/C=CN/ST=Beijing/L=Beijing/O=Company/CN=kb.yourdomain.com"
```

### 2.6 启动服务

```bash
# 创建必要目录
mkdir -p uploads logs backups nginx/ssl

# 启动所有服务
docker-compose up -d

# 查看服务状态
docker-compose ps

# 查看日志
docker-compose logs -f

# 查看特定服务日志
docker-compose logs -f backend
```

### 2.7 验证部署

```bash
# 健康检查
curl https://kb.yourdomain.com/health

# API健康检查
curl https://kb.yourdomain.com/api/actuator/health

# 检查前端
curl -I https://kb.yourdomain.com
```

---

## 3. Kubernetes部署

### 3.1 创建命名空间

```yaml
# namespace.yaml
apiVersion: v1
kind: Namespace
metadata:
  name: knowledge-base
  labels:
    name: knowledge-base
```

```bash
kubectl apply -f namespace.yaml
```

### 3.2 ConfigMap和Secret

#### configmap.yaml

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: kb-config
  namespace: knowledge-base
data:
  SPRING_PROFILES_ACTIVE: "prod"
  ELASTICSEARCH_URIS: "http://elasticsearch:9200"
  REDIS_HOST: "redis"
  REDIS_PORT: "6379"
  SPRING_KAFKA_BOOTSTRAP_SERVERS: "kafka:9092"
```

#### secret.yaml

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: kb-secret
  namespace: knowledge-base
type: Opaque
data:
  POSTGRES_PASSWORD: a2JfcGFzc3dvcmQ=  # base64编码
  REDIS_PASSWORD: cmVkaXNfcGFzc3dvcmQ=
  JWT_SECRET: eW91cl9zZWNyZXRfa2V5
  QWEN_API_KEY: your_base64_encoded_api_key
```

```bash
kubectl apply -f configmap.yaml
kubectl apply -f secret.yaml
```

### 3.3 PostgreSQL部署

#### postgres-statefulset.yaml

```yaml
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: postgres
  namespace: knowledge-base
spec:
  serviceName: postgres
  replicas: 1
  selector:
    matchLabels:
      app: postgres
  template:
    metadata:
      labels:
        app: postgres
    spec:
      containers:
      - name: postgres
        image: postgres:15.7-alpine
        env:
        - name: POSTGRES_DB
          value: knowledge_base
        - name: POSTGRES_USER
          value: kb_user
        - name: POSTGRES_PASSWORD
          valueFrom:
            secretKeyRef:
              name: kb-secret
              key: POSTGRES_PASSWORD
        ports:
        - containerPort: 5432
        volumeMounts:
        - name: postgres-storage
          mountPath: /var/lib/postgresql/data
        resources:
          requests:
            memory: "2Gi"
            cpu: "1000m"
          limits:
            memory: "4Gi"
            cpu: "2000m"
  volumeClaimTemplates:
  - metadata:
      name: postgres-storage
    spec:
      accessModes: ["ReadWriteOnce"]
      resources:
        requests:
          storage: 50Gi
---
apiVersion: v1
kind: Service
metadata:
  name: postgres
  namespace: knowledge-base
spec:
  selector:
    app: postgres
  ports:
  - port: 5432
    targetPort: 5432
  clusterIP: None
```

### 3.4 Elasticsearch部署

#### elasticsearch-deployment.yaml

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: elasticsearch
  namespace: knowledge-base
spec:
  replicas: 1
  selector:
    matchLabels:
      app: elasticsearch
  template:
    metadata:
      labels:
        app: elasticsearch
    spec:
      containers:
      - name: elasticsearch
        image: elasticsearch:8.11.0
        env:
        - name: discovery.type
          value: single-node
        - name: ES_JAVA_OPTS
          value: "-Xms4g -Xmx4g"
        ports:
        - containerPort: 9200
        resources:
          requests:
            memory: "4Gi"
            cpu: "2000m"
          limits:
            memory: "8Gi"
            cpu: "4000m"
        volumeMounts:
        - name: es-storage
          mountPath: /usr/share/elasticsearch/data
  volumes:
  - name: es-storage
    persistentVolumeClaim:
      claimName: es-pvc
---
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: es-pvc
  namespace: knowledge-base
spec:
  accessModes:
    - ReadWriteOnce
  resources:
    requests:
      storage: 100Gi
---
apiVersion: v1
kind: Service
metadata:
  name: elasticsearch
  namespace: knowledge-base
spec:
  selector:
    app: elasticsearch
  ports:
  - port: 9200
    targetPort: 9200
```

### 3.5 应用部署

#### backend-deployment.yaml

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: backend
  namespace: knowledge-base
spec:
  replicas: 3
  selector:
    matchLabels:
      app: backend
  template:
    metadata:
      labels:
        app: backend
    spec:
      containers:
      - name: backend
        image: knowledge-base-backend:latest
        envFrom:
        - configMapRef:
            name: kb-config
        - secretRef:
            name: kb-secret
        ports:
        - containerPort: 8080
        env:
        - name: JAVA_OPTS
          value: "-Xms2g -Xmx2g"
        livenessProbe:
          httpGet:
            path: /api/actuator/health
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /api/actuator/health
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 5
        resources:
          requests:
            memory: "2Gi"
            cpu: "1000m"
          limits:
            memory: "4Gi"
            cpu: "2000m"
---
apiVersion: v1
kind: Service
metadata:
  name: backend
  namespace: knowledge-base
spec:
  selector:
    app: backend
  ports:
  - port: 8080
    targetPort: 8080
  type: ClusterIP
```

### 3.6 Ingress配置

#### ingress.yaml

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: knowledge-base-ingress
  namespace: knowledge-base
  annotations:
    cert-manager.io/cluster-issuer: letsencrypt-prod
    nginx.ingress.kubernetes.io/ssl-redirect: "true"
    nginx.ingress.kubernetes.io/proxy-body-size: "50m"
    nginx.ingress.kubernetes.io/websocket-services: "backend"
spec:
  ingressClassName: nginx
  tls:
  - hosts:
    - kb.yourdomain.com
    secretName: kb-tls
  rules:
  - host: kb.yourdomain.com
    http:
      paths:
      - path: /api
        pathType: Prefix
        backend:
          service:
            name: backend
            port:
              number: 8080
      - path: /
        pathType: Prefix
        backend:
          service:
            name: frontend
            port:
              number: 80
```

### 3.7 部署到Kubernetes

```bash
# 应用所有配置
kubectl apply -f k8s/

# 查看部署状态
kubectl get all -n knowledge-base

# 查看日志
kubectl logs -f deployment/backend -n knowledge-base

# 进入Pod
kubectl exec -it deployment/backend -n knowledge-base -- /bin/bash
```

---

## 4. CI/CD配置

### 4.1 GitLab CI

#### .gitlab-ci.yml

```yaml
stages:
  - build
  - test
  - deploy

variables:
  DOCKER_REGISTRY: your-registry.com
  BACKEND_IMAGE: ${DOCKER_REGISTRY}/knowledge-base-backend
  FRONTEND_IMAGE: ${DOCKER_REGISTRY}/knowledge-base-frontend

# 构建后端
build-backend:
  stage: build
  image: maven:3.9-openjdk-17
  script:
    - cd backend
    - mvn clean package -DskipTests
    - docker build -f ../Dockerfile.backend -t ${BACKEND_IMAGE}:${CI_COMMIT_SHA} .
    - docker tag ${BACKEND_IMAGE}:${CI_COMMIT_SHA} ${BACKEND_IMAGE}:latest
    - docker push ${BACKEND_IMAGE}:${CI_COMMIT_SHA}
    - docker push ${BACKEND_IMAGE}:latest
  only:
    - main
    - develop

# 构建前端
build-frontend:
  stage: build
  image: node:20-alpine
  script:
    - cd frontend
    - npm ci
    - npm run build
    - docker build -f ../Dockerfile.frontend -t ${FRONTEND_IMAGE}:${CI_COMMIT_SHA} .
    - docker tag ${FRONTEND_IMAGE}:${CI_COMMIT_SHA} ${FRONTEND_IMAGE}:latest
    - docker push ${FRONTEND_IMAGE}:${CI_COMMIT_SHA}
    - docker push ${FRONTEND_IMAGE}:latest
  only:
    - main
    - develop

# 测试
test:
  stage: test
  image: maven:3.9-openjdk-17
  script:
    - cd backend
    - mvn test
  coverage: '/Total.*?([0-9]{1,3})%/'
  only:
    - main
    - develop
    - merge_requests

# 部署到测试环境
deploy-staging:
  stage: deploy
  image: bitnami/kubectl:latest
  script:
    - kubectl config use-context staging
    - kubectl set image deployment/backend backend=${BACKEND_IMAGE}:${CI_COMMIT_SHA} -n knowledge-base
    - kubectl set image deployment/frontend frontend=${FRONTEND_IMAGE}:${CI_COMMIT_SHA} -n knowledge-base
    - kubectl rollout status deployment/backend -n knowledge-base
    - kubectl rollout status deployment/frontend -n knowledge-base
  environment:
    name: staging
    url: https://staging.kb.yourdomain.com
  only:
    - develop

# 部署到生产环境
deploy-production:
  stage: deploy
  image: bitnami/kubectl:latest
  script:
    - kubectl config use-context production
    - kubectl set image deployment/backend backend=${BACKEND_IMAGE}:${CI_COMMIT_SHA} -n knowledge-base
    - kubectl set image deployment/frontend frontend=${FRONTEND_IMAGE}:${CI_COMMIT_SHA} -n knowledge-base
    - kubectl rollout status deployment/backend -n knowledge-base
    - kubectl rollout status deployment/frontend -n knowledge-base
  environment:
    name: production
    url: https://kb.yourdomain.com
  when: manual
  only:
    - main
```

### 4.2 GitHub Actions

#### .github/workflows/deploy.yml

```yaml
name: Build and Deploy

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

env:
  REGISTRY: your-registry.com
  BACKEND_IMAGE: knowledge-base-backend
  FRONTEND_IMAGE: knowledge-base-frontend

jobs:
  build:
    runs-on: ubuntu-latest
    permissions:
      contents: read
      packages: write

    steps:
    - name: Checkout code
      uses: actions/checkout@v3

    - name: Set up JDK 17
      uses: actions/setup-java@v3
      with:
        java-version: '17'
        distribution: 'temurin'
        cache: maven

    - name: Build backend
      run: |
        cd backend
        mvn clean package -DskipTests

    - name: Log in to registry
      uses: docker/login-action@v2
      with:
        registry: ${{ env.REGISTRY }}
        username: ${{ secrets.DOCKER_USERNAME }}
        password: ${{ secrets.DOCKER_PASSWORD }}

    - name: Build backend image
      run: |
        docker build -f Dockerfile.backend -t ${{ env.REGISTRY }}/${{ env.BACKEND_IMAGE }}:${{ github.sha }} .
        docker push ${{ env.REGISTRY }}/${{ env.BACKEND_IMAGE }}:${{ github.sha }}

    - name: Build frontend image
      run: |
        cd frontend
        npm ci
        npm run build
        cd ..
        docker build -f Dockerfile.frontend -t ${{ env.REGISTRY }}/${{ env.FRONTEND_IMAGE }}:${{ github.sha }} .
        docker push ${{ env.REGISTRY }}/${{ env.FRONTEND_IMAGE }}:${{ github.sha }}

  deploy:
    needs: build
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'

    steps:
    - name: Deploy to Kubernetes
      uses: azure/k8s-deploy@v4
      with:
        manifests: |
          k8s/backend-deployment.yaml
          k8s/frontend-deployment.yaml
        images: |
          ${{ env.REGISTRY }}/${{ env.BACKEND_IMAGE }}:${{ github.sha }}
          ${{ env.REGISTRY }}/${{ env.FRONTEND_IMAGE }}:${{ github.sha }}
        kubeconfig: ${{ secrets.KUBE_CONFIG }}
```

---

## 5. 监控和告警

### 5.1 Prometheus配置

#### prometheus.yml

```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  # Spring Boot应用
  - job_name: 'knowledge-base'
    kubernetes_sd_configs:
      - role: pod
        namespaces:
          names:
            - knowledge-base
    relabel_configs:
      - source_labels: [__meta_kubernetes_pod_label_app]
        action: keep
        regex: backend
      - source_labels: [__meta_kubernetes_pod_ip]
        target_label: __address__
        replacement: $1:8080
      - source_labels: [__meta_kubernetes_pod_name]
        target_label: pod

  # PostgreSQL
  - job_name: 'postgres'
    static_configs:
      - targets: ['postgres-exporter:9187']

  # Redis
  - job_name: 'redis'
    static_configs:
      - targets: ['redis-exporter:9121']

  # Elasticsearch
  - job_name: 'elasticsearch'
    static_configs:
      - targets: ['elasticsearch-exporter:9114']

  # Kafka
  - job_name: 'kafka'
    static_configs:
      - targets: ['kafka-exporter:9308']
```

### 5.2 Grafana仪表板

导入以下仪表板：
- Spring Boot 2.x Dashboard (ID: 11378)
- PostgreSQL Dashboard (ID: 9628)
- Redis Dashboard (ID: 11835)
- Elasticsearch Dashboard (ID: 2322)
- Kafka Dashboard (ID: 721)

### 5.3 告警规则

#### alerts.yml

```yaml
groups:
  - name: knowledge-base
    interval: 30s
    rules:
      # 高错误率
      - alert: HighErrorRate
        expr: rate(http_requests_total{status=~"5.."}[5m]) > 0.05
        for: 5m
        annotations:
          summary: "High error rate detected"
          description: "Error rate is {{ $value }} errors/sec"

      # 高响应时间
      - alert: HighResponseTime
        expr: histogram_quantile(0.99, rate(http_request_duration_seconds_bucket[5m])) > 1
        for: 5m
        annotations:
          summary: "P99 latency is too high"
          description: "P99 latency is {{ $value }} seconds"

      # 数据库连接池耗尽
      - alert: DatabasePoolExhausted
        expr: hikaricp_connections_active / hikaricp_connections_max > 0.9
        for: 5m
        annotations:
          summary: "Database connection pool nearly exhausted"
          description: "Connection pool usage is {{ $value }}%"

      # Kafka消费延迟
      - alert: KafkaConsumerLag
        expr: kafka_consumergroup_lag > 1000
        for: 10m
        annotations:
          summary: "Kafka consumer lag detected"
          description: "Consumer lag is {{ $value }} messages"

      # Elasticsearch集群健康
      - alert: ElasticsearchClusterNotHealthy
        expr: elasticsearch_cluster_health_status{color!="green"} > 0
        for: 5m
        annotations:
          summary: "Elasticsearch cluster not healthy"
          description: "Cluster status is {{ $labels.color }}"
```

---

## 6. 备份和恢复

### 6.1 数据库备份

#### 自动备份脚本

```bash
#!/bin/bash
# backup-db.sh

BACKUP_DIR="/backups/postgres"
DATE=$(date +"%Y%m%d_%H%M%S")
BACKUP_FILE="$BACKUP_DIR/kb_$DATE.sql.gz"

# 创建备份目录
mkdir -p $BACKUP_DIR

# 备份数据库
docker exec kb-postgres pg_dump -U kb_user knowledge_base | gzip > $BACKUP_FILE

# 保留最近7天
find $BACKUP_DIR -name "kb_*.sql.gz" -mtime +7 -delete

echo "Backup completed: $BACKUP_FILE"
```

#### 定时任务

```bash
# 添加到crontab
crontab -e

# 每天凌晨2点备份
0 2 * * * /opt/knowledge-base/scripts/backup-db.sh
```

### 6.2 Elasticsearch快照

#### 创建快照仓库

```bash
curl -X PUT "localhost:9200/_snapshot/backup" -H 'Content-Type: application/json' -d'
{
  "type": "fs",
  "settings": {
    "location": "/backups/elasticsearch"
  }
}'
```

#### 创建快照

```bash
curl -X PUT "localhost:9200/_snapshot/backup/snapshot_1?wait_for_completion=true"
```

### 6.3 恢复流程

#### 恢复数据库

```bash
# 停止应用
docker-compose stop backend

# 恢复数据库
gunzip -c /backups/postgres/kb_20260408_020000.sql.gz |
  docker exec -i kb-postgres psql -U kb_user knowledge_base

# 重启应用
docker-compose start backend
```

#### 恢复Elasticsearch

```bash
# 关闭索引
curl -X POST "localhost:9200/kb_document_chunks/_close"

# 恢复快照
curl -X POST "localhost:9200/_snapshot/backup/snapshot_1/_restore" -H 'Content-Type: application/json' -d'
{
  "indices": "kb_document_chunks"
}'

# 打开索引
curl -X POST "localhost:9200/kb_document_chunks/_open"
```

---

## 7. 性能优化

### 7.1 数据库优化

```sql
-- 创建索引
CREATE INDEX CONCURRENTLY idx_documents_created_at
ON documents(created_at DESC);

CREATE INDEX CONCURRENTLY idx_documents_status
ON documents(status) WHERE status = 'PROCESSING';

-- 分区表
CREATE TABLE qa_history_partitioned (
    id BIGSERIAL,
    user_id BIGINT,
    question TEXT,
    answer TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) PARTITION BY RANGE (created_at);

-- 按月分区
CREATE TABLE qa_history_2024_01 PARTITION OF qa_history_partitioned
    FOR VALUES FROM ('2024-01-01') TO ('2024-02-01');
```

### 7.2 JVM调优

```bash
# 堆内存设置
-Xms2g -Xmx2g

# GC配置
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200
-XX:G1ReservePercent=20

# 性能监控
-XX:+PrintGCDetails
-XX:+PrintGCDateStamps
-Xloggc:$APP_HOME/logs/gc.log
```

### 7.3 Nginx优化

```nginx
# 工作进程数
worker_processes auto;

# 连接数
worker_connections 10240;

# 启用缓存
proxy_cache_path /var/cache/nginx levels=1:2 keys_zone=api_cache:10m max_size=1g;

# 压缩
gzip on;
gzip_types text/plain application/json application/javascript text/css;
```

---

## 8. 日志管理

### 8.1 日志轮转

#### /etc/logrotate.d/knowledge-base

```
/opt/knowledge-base/logs/*.log {
    daily
    missingok
    rotate 30
    compress
    delaycompress
    notifempty
    create 0640 kb-user kb-group
    sharedscripts
    postrotate
        docker-compose kill -s USR1 backend
    endscript
}
```

### 8.2 集中日志

使用ELK Stack或Loki收集日志：

```yaml
# fluentd-config.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: fluentd-config
data:
  fluent.conf: |
    <source>
      @type tail
      path /var/log/containers/*backend*.log
      pos_file /var/log/fluentd-backend.log.pos
      tag kubernetes.*
      read_from_head true
      <parse>
        @type json
      </parse>
    </source>

    <match **>
      @type elasticsearch
      host elasticsearch
      port 9200
      logstash_format true
      logstash_prefix kb-logs
    </match>
```

---

**文档版本**：v1.0
**最后更新**：2026-04-08
**维护者**：DevOps团队
