#!/bin/bash

# 前端上传功能测试脚本
# 用于验证前端上传功能是否正常工作

echo "========================================"
echo "前端上传功能测试"
echo "========================================"
echo ""

# 颜色定义
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 检查计数器
PASS_COUNT=0
FAIL_COUNT=0

# 检查函数
check_pass() {
    echo -e "${GREEN}✓${NC} $1"
    ((PASS_COUNT++))
}

check_fail() {
    echo -e "${RED}✗${NC} $1"
    ((FAIL_COUNT++))
}

check_warn() {
    echo -e "${YELLOW}⚠${NC} $1"
}

check_info() {
    echo -e "${BLUE}ℹ${NC} $1"
}

echo "1. 检查前端项目结构"
echo "----------------------------------------"

# 检查前端目录
if [ -d "frontend" ]; then
    check_pass "前端目录存在"
else
    check_fail "前端目录不存在"
    exit 1
fi

# 检查上传组件
if [ -f "frontend/src/components/upload/SmartFileUploader.tsx" ]; then
    check_pass "SmartFileUploader组件存在"
else
    check_fail "SmartFileUploader组件不存在"
fi

if [ -f "frontend/src/components/upload/ChunkedFileUploader.tsx" ]; then
    check_pass "ChunkedFileUploader组件存在"
else
    check_fail "ChunkedFileUploader组件不存在"
fi

# 检查服务层
if [ -f "frontend/src/services/documentService.ts" ]; then
    check_pass "documentService服务存在"
else
    check_fail "documentService服务不存在"
fi

# 检查测试页面
if [ -f "frontend/src/pages/TestUploadPage.tsx" ]; then
    check_pass "TestUploadPage测试页面存在"
else
    check_fail "TestUploadPage测试页面不存在"
fi

# 检查路由配置
if grep -q "test-upload" "frontend/src/App.tsx"; then
    check_pass "测试页面路由已配置"
else
    check_fail "测试页面路由未配置"
fi

echo ""
echo "2. 检查前端依赖"
echo "----------------------------------------"

# 检查package.json
if [ -f "frontend/package.json" ]; then
    check_pass "package.json存在"

    # 检查关键依赖
    if grep -q "spark-md5" "frontend/package.json"; then
        check_pass "spark-md5依赖已安装"
    else
        check_fail "spark-md5依赖未安装"
    fi

    if grep -q "antd" "frontend/package.json"; then
        check_pass "antd依赖已安装"
    else
        check_fail "antd依赖未安装"
    fi

    if grep -q "axios" "frontend/package.json"; then
        check_pass "axios依赖已安装"
    else
        check_fail "axios依赖未安装"
    fi

    # 检查node_modules
    if [ -d "frontend/node_modules" ]; then
        check_pass "node_modules目录存在"
    else
        check_warn "node_modules目录不存在，需要运行: cd frontend && npm install"
    fi
else
    check_fail "package.json不存在"
fi

echo ""
echo "3. 检查上传组件功能"
echo "----------------------------------------"

# 检查SmartFileUploader功能
if grep -q "calculateMD5" "frontend/src/components/upload/SmartFileUploader.tsx"; then
    check_pass "SmartFileUploader支持MD5计算"
else
    check_fail "SmartFileUploader不支持MD5计算"
fi

if grep -q "chunkedUpload.init" "frontend/src/components/upload/SmartFileUploader.tsx"; then
    check_pass "SmartFileUploader支持分片上传初始化"
else
    check_fail "SmartFileUploader不支持分片上传初始化"
fi

if grep -q "chunkedUpload.uploadChunk" "frontend/src/components/upload/SmartFileUploader.tsx"; then
    check_pass "SmartFileUploader支持分片上传"
else
    check_fail "SmartFileUploader不支持分片上传"
fi

if grep -q "chunkedUpload.complete" "frontend/src/components/upload/SmartFileUploader.tsx"; then
    check_pass "SmartFileUploader支持完成上传"
else
    check_fail "SmartFileUploader不支持完成上传"
fi

# 检查documentService API
if grep -q "chunkedUpload:" "frontend/src/services/documentService.ts"; then
    check_pass "documentService包含分片上传API"
else
    check_fail "documentService不包含分片上传API"
fi

echo ""
echo "4. 检查后端服务状态"
echo "----------------------------------------"

# 检查后端是否运行
BACKEND_URL="http://localhost:8080"
if curl -s -o /dev/null -w "%{http_code}" "${BACKEND_URL}/api/actuator/health" | grep -q "200\|401"; then
    check_pass "后端服务正在运行"

    # 检查分片上传端点
    if curl -s -o /dev/null -w "%{http_code}" "${BACKEND_URL}/api/files/chunk/init" | grep -q "405\|401"; then
        check_pass "分片上传端点可用"
    else
        check_warn "分片上传端点可能不可用"
    fi

    # 检查普通上传端点
    if curl -s -o /dev/null -w "%{http_code}" "${BACKEND_URL}/api/documents/upload" | grep -q "405\|401"; then
        check_pass "普通上传端点可用"
    else
        check_warn "普通上传端点可能不可用"
    fi
else
    check_warn "后端服务未运行，请先启动后端"
fi

echo ""
echo "5. 功能验证建议"
echo "----------------------------------------"

check_info "建议按以下顺序测试上传功能："
echo ""
echo "  1. 启动后端服务："
echo "     cd backend && mvn spring-boot:run"
echo ""
echo "  2. 启动前端服务："
echo "     cd frontend && npm run dev"
echo ""
echo "  3. 访问测试页面："
echo "     http://localhost:5173/test-upload"
echo ""
echo "  4. 测试流程："
echo "     a) 先测试"调试上传器" - 验证基础功能"
echo "     b) 再测试"简化测试上传器" - 验证文件上传"
echo "     c) 最后测试"智能上传组件" - 验证完整功能"
echo ""
echo "  5. 测试不同大小的文件："
echo "     - 小文件（< 50MB）：应该使用普通上传"
echo "     - 大文件（≥ 50MB）：应该使用分片上传"
echo ""
echo "  6. 测试高级功能："
echo "     - 秒传：上传相同文件两次，第二次应该秒传"
echo "     - 断点续传：上传大文件时暂停后继续"
echo "     - 并发上传：同时上传多个文件"

echo ""
echo "========================================"
echo "验证结果汇总"
echo "========================================"
echo -e "${GREEN}通过检查: $PASS_COUNT${NC}"
echo -e "${RED}失败检查: $FAIL_COUNT${NC}"
echo ""

if [ $FAIL_COUNT -eq 0 ]; then
    echo -e "${GREEN}✓ 前端上传功能配置完整！${NC}"
    echo ""
    echo "下一步操作："
    echo "1. 启动后端: cd backend && mvn spring-boot:run"
    echo "2. 启动前端: cd frontend && npm run dev"
    echo "3. 访问测试页面: http://localhost:5173/test-upload"
    echo "4. 按照测试流程验证功能"
    exit 0
else
    echo -e "${RED}✗ 前端上传功能配置存在问题，请修复后重试${NC}"
    exit 1
fi
