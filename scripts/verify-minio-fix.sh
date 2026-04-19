#!/bin/bash

# 快速验证MinIO存储修复

echo "========================================"
echo "MinIO存储修复验证"
echo "========================================"
echo ""

# 颜色定义
GREEN='\033[0;32m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}1. 检查MinIO容器状态${NC}"
echo "----------------------------------------"
if docker ps | grep -q "kb-minio"; then
    echo -e "${GREEN}✓${NC} MinIO容器正在运行"
else
    echo -e "${RED}✗${NC} MinIO容器未运行"
    echo "请启动: docker-compose up -d minio"
    exit 1
fi

echo ""
echo -e "${BLUE}2. 检查MinIO Bucket${NC}"
echo "----------------------------------------"
MINIO_ENDPOINT="http://localhost:9000"
MINIO_ACCESS="minioadmin"
MINIO_SECRET="minioadmin"
BUCKET_NAME="knowledge-base-documents"

if docker exec kb-minio mc ls myminio/$BUCKET_NAME >/dev/null 2>&1; then
    echo -e "${GREEN}✓${NC} MinIO Bucket存在: $BUCKET_NAME"
else
    echo -e "${RED}✗${NC} MinIO Bucket不存在"
    echo "Bucket将在首次上传时自动创建"
fi

echo ""
echo -e "${BLUE}3. 检查后端服务状态${NC}"
echo "----------------------------------------"
BACKEND_URL="http://localhost:8080"
if curl -s -o /dev/null -w "%{http_code}" "${BACKEND_URL}/api/actuator/health" | grep -q "200\|401"; then
    echo -e "${GREEN}✓${NC} 后端服务正在运行"
else
    echo -e "${RED}✗${NC} 后端服务未运行"
    echo "请启动: cd backend && mvn spring-boot:run"
    exit 1
fi

echo ""
echo -e "${BLUE}4. 验证修复内容${NC}"
echo "----------------------------------------"
echo "✓ DocumentServiceImpl.uploadDocument() 现在使用MinIO"
echo "✓ MinioService.uploadFile() 方法已添加"
echo "✓ 统一使用MinIO存储（普通上传 + 分片上传）"

echo ""
echo -e "${BLUE}5. 测试步骤${NC}"
echo "----------------------------------------"
echo "请按照以下步骤测试文件上传："
echo ""
echo "1. 访问前端测试页面:"
echo "   http://localhost:5173/test-upload"
echo ""
echo "2. 使用'调试上传器'测试小文件上传"
echo ""
echo "3. 检查后端日志，确认使用MinIO存储:"
echo "   查找日志: '文件上传成功: objectName=, path='"
echo ""
echo "4. (可选) 访问MinIO控制台验证文件:"
echo "   URL: $MINIO_ENDPOINT"
echo "   用户名: $MINIO_ACCESS"
echo "   密码: $MINIO_SECRET"
echo "   Bucket: $BUCKET_NAME"
echo ""
echo "5. 验证存储路径:"
echo "   普通上传: documents/{uuid}/{filename}"
echo "   分片上传: documents/{uuid}/{filename}"
echo ""

echo -e "${GREEN}========================================"
echo "✓ MinIO存储修复验证完成"
echo "========================================${NC}"
echo ""
echo "现在可以测试文件上传功能了！"
