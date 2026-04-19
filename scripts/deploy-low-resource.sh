#!/bin/bash
# ============================================
# 低资源服务器部署脚本
# 适用于 2核2G 服务器
# ============================================

set -e

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 日志函数
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 检查系统资源
check_resources() {
    log_info "检查系统资源..."

    # 检查内存
    total_mem=$(free -m | awk '/Mem:/ {print $2}')
    if [ "$total_mem" -lt 1800 ]; then
        log_warn "内存低于2GB，当前: ${total_mem}MB"
        log_warn "建议至少2GB内存"
    else
        log_info "内存检查通过: ${total_mem}MB"
    fi

    # 检查CPU
    cpu_cores=$(nproc)
    if [ "$cpu_cores" -lt 2 ]; then
        log_warn "CPU核心数少于2，当前: ${cpu_cores}"
    else
        log_info "CPU检查通过: ${cpu_cores}核心"
    fi

    # 检查磁盘空间
    disk_avail=$(df -m / | awk 'NR==2 {print $4}')
    if [ "$disk_avail" -lt 5000 ]; then
        log_error "磁盘空间不足5GB，当前可用: ${disk_avail}MB"
        exit 1
    else
        log_info "磁盘检查通过: ${disk_avail}MB可用"
    fi
}

# 检查Docker
check_docker() {
    log_info "检查Docker环境..."

    if ! command -v docker &> /dev/null; then
        log_error "Docker未安装"
        log_info "请运行: curl -fsSL https://get.docker.com | sh"
        exit 1
    fi

    if ! command -v docker-compose &> /dev/null; then
        log_error "Docker Compose未安装"
        log_info "请运行: sudo apt install docker-compose"
        exit 1
    fi

    log_info "Docker版本: $(docker --version)"
    log_info "Docker Compose版本: $(docker-compose --version)"
}

# 配置环境变量
setup_env() {
    log_info "配置环境变量..."

    if [ ! -f .env.prod ]; then
        if [ -f .env.prod.example ]; then
            log_info "从模板创建 .env.prod"
            cp .env.prod.example .env.prod
            log_warn "请编辑 .env.prod 并填写正确的配置"
            log_warn "特别注意: POSTGRES_PASSWORD, JWT_SECRET, QWEN_API_KEY"
            exit 1
        else
            log_error "未找到 .env.prod.example"
            exit 1
        fi
    fi

    source .env.prod

    # 验证必需的环境变量
    required_vars=("POSTGRES_PASSWORD" "JWT_SECRET" "QWEN_API_KEY")
    for var in "${required_vars[@]}"; do
        if [ -z "${!var}" ]; then
            log_error "环境变量 $var 未设置"
            exit 1
        fi
    done

    log_info "环境变量检查通过"
}

# 创建必要的目录
create_directories() {
    log_info "创建必要的目录..."

    mkdir -p logs
    mkdir -p uploads
    mkdir -p projects
    mkdir -p postgres_data
    mkdir -p es_data
    mkdir -p kafka_data

    log_info "目录创建完成"
}

# 优化系统参数
optimize_system() {
    log_info "优化系统参数..."

    # 设置swap
    swap_total=$(free -m | awk '/Swap:/ {print $2}')
    if [ "$swap_total" -lt 1000 ]; then
        log_warn "Swap空间较小，建议至少1GB"
        log_info "如需增加swap: sudo fallocate -l 1G /swapfile && sudo chmod 600 /swapfile && sudo mkswap /swapfile && sudo swapon /swapfile"
    fi

    # 调整vm.overcommit_memory
    sysctl vm.overcommit_memory=1 || echo "vm.overcommit_memory=1" | sudo tee -a /etc/sysctl.conf

    log_info "系统参数优化完成"
}

# 构建镜像
build_images() {
    log_info "构建Docker镜像..."

    # 检查是否有自定义Dockerfile
    if [ -f Dockerfile.backend ]; then
        log_info "构建后端镜像..."
        docker build -f Dockerfile.backend -t think-platform/api-gateway:latest .
        docker build -f Dockerfile.backend -t think-platform/auth-service:latest .
        docker build -f Dockerfile.backend -t think-platform/knowledge-mentor:latest .
        docker build -f Dockerfile.backend -t think-platform/code-intelligence:latest .
    else
        log_info "使用默认构建方式..."
        cd backend
        mvn clean package -DskipTests
        docker-compose -f ../docker-compose.low-resource.yml build
        cd ..
    fi

    # 构建前端
    if [ -f Dockerfile.frontend ]; then
        log_info "构建前端镜像..."
        docker build -f Dockerfile.frontend -t think-platform/frontend:latest .
    fi

    log_info "镜像构建完成"
}

# 启动服务
start_services() {
    log_info "启动服务..."

    # 停止旧服务
    docker-compose -f docker-compose.low-resource.yml down

    # 启动新服务
    docker-compose -f docker-compose.low-resource.yml up -d

    log_info "服务启动完成"
}

# 等待服务健康
wait_for_healthy() {
    log_info "等待服务启动..."

    max_attempts=60
    attempt=0

    while [ $attempt -lt $max_attempts ]; do
        attempt=$((attempt + 1))

        # 检查PostgreSQL
        if docker exec kb-postgres pg_isready -U kb_user &> /dev/null; then
            log_info "PostgreSQL 就绪"
            break
        fi

        echo -n "."
        sleep 2
    done

    if [ $attempt -eq $max_attempts ]; then
        log_error "服务启动超时"
        exit 1
    fi

    # 等待应用服务
    log_info "等待应用服务..."
    sleep 30
}

# 健康检查
health_check() {
    log_info "执行健康检查..."

    services=(
        "kb-api-gateway:8082"
        "kb-auth-service:8083"
        "kb-knowledge-mentor:8080"
        "kb-code-intelligence:8088"
    )

    for service in "${services[@]}"; do
        IFS=':' read -ra PARTS <<< "$service"
        container="${PARTS[0]}"
        port="${PARTS[1]}"

        if docker exec $container curl -f http://localhost:$port/actuator/health &> /dev/null; then
            log_info "$container 健康检查通过"
        else
            log_warn "$container 健康检查失败"
        fi
    done

    # Elasticsearch检查
    if curl -f http://localhost:9200/_cluster/health &> /dev/null; then
        log_info "Elasticsearch 健康检查通过"
    fi
}

# 显示服务状态
show_status() {
    log_info "服务状态:"
    docker-compose -f docker-compose.low-resource.yml ps
}

# 显示资源使用
show_resources() {
    log_info "资源使用:"
    docker stats --no-stream --format "table {{.Container}}\t{{.CPUPerc}}\t{{.MemUsage}}\t{{.MemPerc}}"
}

# 主函数
main() {
    log_info "开始部署流程..."

    check_resources
    check_docker
    setup_env
    create_directories
    optimize_system

    # 询问是否构建镜像
    read -p "是否构建新镜像? (y/N): " build_choice
    if [[ $build_choice =~ ^[Yy]$ ]]; then
        build_images
    fi

    start_services
    wait_for_healthy
    health_check
    show_status
    show_resources

    log_info "部署完成!"
    log_info "访问地址:"
    log_info "  - 前端: http://localhost"
    log_info "  - 网关: http://localhost:8082"
    log_info "  - API文档: http://localhost:8082/doc.html"
    log_info ""
    log_info "查看日志: docker-compose -f docker-compose.low-resource.yml logs -f [service-name]"
    log_info "查看状态: docker-compose -f docker-compose.low-resource.yml ps"
}

# 执行主函数
main "$@"
