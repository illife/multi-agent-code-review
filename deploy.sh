#!/bin/bash

###############################################################################
# Production Deployment Script
# This script helps deploy the Knowledge Base platform to production
###############################################################################

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Functions
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

check_prerequisites() {
    log_info "Checking prerequisites..."

    # Check if Docker is installed
    if ! command -v docker &> /dev/null; then
        log_error "Docker is not installed. Please install Docker first."
        exit 1
    fi

    # Check if Docker Compose is installed
    if ! command -v docker-compose &> /dev/null; then
        log_error "Docker Compose is not installed. Please install Docker Compose first."
        exit 1
    fi

    # Check if .env file exists
    if [ ! -f .env ]; then
        log_warn ".env file not found. Creating from .env.production.example..."
        if [ -f .env.production.example ]; then
            cp .env.production.example .env
            log_info ".env file created. Please edit it with your production values before continuing."
            log_warn "Update the following values in .env:"
            log_warn "  - SPRING_DATASOURCE_PASSWORD (generate strong password)"
            log_warn "  - MINIO_ACCESS_KEY and MINIO_SECRET_KEY"
            log_warn "  - QWEN_API_KEY (get from Aliyun)"
            log_warn "  - JWT_SECRET (generate with: openssl rand -base64 32)"
            read -p "Press Enter after updating .env file..."
        else
            log_error ".env.production.example not found. Cannot create .env file."
            exit 1
        fi
    fi

    log_info "Prerequisites check passed."
}

generate_secrets() {
    log_info "Generating secure secrets..."

    # Generate JWT secret
    JWT_SECRET=$(openssl rand -base64 32)
    log_info "Generated JWT_SECRET: $JWT_SECRET"

    # Generate database password
    DB_PASSWORD=$(openssl rand -base64 16)
    log_info "Generated database password"

    # Generate MinIO credentials
    MINIO_ACCESS_KEY=$(openssl rand -base64 12 | tr -dc 'a-zA-Z0-9')
    MINIO_SECRET_KEY=$(openssl rand -base64 24)
    log_info "Generated MinIO credentials"

    log_warn "Please update your .env file with these values"
}

build_images() {
    log_info "Building Docker images..."

    docker-compose -f docker-compose.prod.yml build --no-cache

    log_info "Docker images built successfully."
}

start_services() {
    log_info "Starting services..."

    docker-compose -f docker-compose.prod.yml up -d

    log_info "Services starting. Waiting for health checks..."
    sleep 30

    # Check service status
    docker-compose -f docker-compose.prod.yml ps
}

stop_services() {
    log_info "Stopping services..."

    docker-compose -f docker-compose.prod.yml down

    log_info "Services stopped."
}

restart_services() {
    log_info "Restarting services..."

    docker-compose -f docker-compose.prod.yml restart

    log_info "Services restarted."
}

view_logs() {
    log_info "Viewing logs (Ctrl+C to exit)..."

    docker-compose -f docker-compose.prod.yml logs -f
}

backup_data() {
    log_info "Creating data backup..."

    BACKUP_DIR="backups/$(date +%Y%m%d_%H%M%S)"
    mkdir -p "$BACKUP_DIR"

    # Backup volumes
    docker run --rm \
        -v kb_postgres_data:/data/postgres \
        -v kb_elasticsearch_data:/data/elasticsearch \
        -v kb_redis_data:/data/redis \
        -v kb_kafka_data:/data/kafka \
        -v kb_minio_data:/data/minio \
        -v "$(pwd)/$BACKUP_DIR":/backup \
        alpine tar czf /backup/data_backup.tar.gz /data

    log_info "Backup created at: $BACKUP_DIR/data_backup.tar.gz"
}

restore_data() {
    if [ -z "$1" ]; then
        log_error "Please specify the backup directory to restore from."
        exit 1
    fi

    log_warn "This will replace all existing data. Are you sure? (yes/no)"
    read -r CONFIRM

    if [ "$CONFIRM" != "yes" ]; then
        log_info "Restore cancelled."
        exit 0
    fi

    log_info "Restoring data from $1..."

    docker run --rm \
        -v kb_postgres_data:/data/postgres \
        -v kb_elasticsearch_data:/data/elasticsearch \
        -v kb_redis_data:/data/redis \
        -v kb_kafka_data:/data/kafka \
        -v kb_minio_data:/data/minio \
        -v "$(pwd)/$1":/backup \
        alpine sh -c "rm -rf /data/* && tar xzf /backup/data_backup.tar.gz -C /"

    log_info "Data restored. Please restart services."
}

show_status() {
    log_info "Service status:"

    docker-compose -f docker-compose.prod.yml ps

    echo ""
    log_info "Health checks:"

    # Backend health
    echo -n "Backend: "
    curl -sf http://localhost:8080/api/actuator/health && echo "Healthy" || echo "Unhealthy"

    # Elasticsearch health
    echo -n "Elasticsearch: "
    curl -sf http://localhost:9200/_cluster/health && echo "Healthy" || echo "Unhealthy"

    # Redis health
    echo -n "Redis: "
    docker exec kb-redis redis-cli ping || echo "Unhealthy"

    # Frontend health
    echo -n "Frontend: "
    curl -sf http://localhost/health && echo "Healthy" || echo "Unhealthy"
}

print_usage() {
    echo "Usage: $0 [command]"
    echo ""
    echo "Commands:"
    echo "  check       Check prerequisites for deployment"
    echo "  secrets     Generate secure secrets for configuration"
    echo "  build       Build Docker images"
    echo "  start       Start all services"
    echo "  stop        Stop all services"
    echo "  restart     Restart all services"
    echo "  logs        View logs from all services"
    echo "  backup      Backup all data volumes"
    echo "  restore     Restore data from backup (specify backup directory)"
    echo "  status      Show status of all services"
    echo "  help        Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0 check"
    echo "  $0 build"
    echo "  $0 start"
    echo "  $0 restore backups/20240101_120000"
}

# Main script logic
case "${1:-help}" in
    check)
        check_prerequisites
        ;;
    secrets)
        generate_secrets
        ;;
    build)
        check_prerequisites
        build_images
        ;;
    start)
        check_prerequisites
        start_services
        show_status
        ;;
    stop)
        stop_services
        ;;
    restart)
        restart_services
        ;;
    logs)
        view_logs
        ;;
    backup)
        backup_data
        ;;
    restore)
        restore_data "$2"
        ;;
    status)
        show_status
        ;;
    help|--help|-h)
        print_usage
        ;;
    *)
        log_error "Unknown command: $1"
        print_usage
        exit 1
        ;;
esac
