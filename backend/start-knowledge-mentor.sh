#!/bin/bash
# 启动知识库服务 (Knowledge Mentor Service)
# Port: 8080

cd "$(dirname "$0")"
SERVICE_DIR="services/knowledge-mentor/km-api"

echo "=================================="
echo "  Starting Knowledge Mentor Service"
echo "  Port: 8080"
echo "=================================="

if [ ! -d "$SERVICE_DIR" ]; then
    echo "Error: Service directory not found: $SERVICE_DIR"
    exit 1
fi

cd "$SERVICE_DIR"

# 检查端口是否被占用
if netstat -ano | grep ":8080" | grep -q "LISTENING"; then
    echo "Warning: Port 8080 is already in use"
    echo "Please stop the process using port 8080 first"
    exit 1
fi

echo "Compiling and starting Knowledge Mentor Service..."
mvn spring-boot:run

# 或者使用已编译的 JAR 启动：
# mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Dserver.port=8080"
# 或者：java -jar target/km-api-1.0.0-SNAPSHOT.jar --server.port=8080
