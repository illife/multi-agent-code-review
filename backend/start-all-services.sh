#!/bin/bash
# ========================================
#   启动所有后端服务
#   Linux/Mac 启动脚本
# ========================================

cd "$(dirname "$0")"

echo ""
echo "========================================"
echo "  Starting All Backend Services"
echo "========================================"
echo ""

# 检查基础设施服务
echo "Checking infrastructure services..."
if ! curl -s http://localhost:9200 >/dev/null 2>&1; then
    echo "Warning: Elasticsearch may not be running (port 9200)"
fi
if ! curl -s http://localhost:5432 >/dev/null 2>&1; then
    echo "Warning: PostgreSQL may not be running (port 5432)"
fi

# 启动 Auth Service (Port 8083)
echo "[1/3] Starting Auth Service (Port 8083)..."
cd services/auth/auth-api
mvn spring-boot:run > ../../logs/auth-service.log 2>&1 &
AUTH_PID=$!
echo "Auth Service started with PID: $AUTH_PID"

sleep 5

# 启动 Knowledge Mentor Service (Port 8080)
echo "[2/3] Starting Knowledge Mentor Service (Port 8080)..."
cd ../knowledge-mentor/km-api
mvn spring-boot:run > ../../logs/km-service.log 2>&1 &
KM_PID=$!
echo "Knowledge Mentor Service started with PID: $KM_PID"

sleep 5

# 启动 Code Intelligence Service (Port 8081)
echo "[3/3] Starting Code Intelligence Service (Port 8081)..."
cd ../code-intelligence/ci-api
mvn spring-boot:run > ../../logs/ci-service.log 2>&1 &
CI_PID=$!
echo "Code Intelligence Service started with PID: $CI_PID"

echo ""
echo "========================================"
echo "  All services started!"
echo "========================================"
echo ""
echo "Service PIDs:"
echo "  - Auth Service: $AUTH_PID"
echo "  - Knowledge Mentor: $KM_PID"
echo "  - Code Intelligence: $CI_PID"
echo ""
echo "Service URLs:"
echo "  - Auth Service:        http://localhost:8083"
echo "  - Knowledge Mentor:    http://localhost:8080"
echo "  - Code Intelligence:    http://localhost:8081"
echo ""
echo "Logs: logs/*.log"
echo ""
echo "To stop all services:"
echo "  kill $AUTH_PID $KM_PID $CI_PID"
echo ""
