# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is an enterprise knowledge base and Q&A system (企业知识库与问答系统) built with:
- **Backend**: Java 17, Spring Boot 3.2, modular Maven architecture
- **Frontend**: React 18, TypeScript, Vite, Ant Design
- **Infrastructure**: PostgreSQL 15.7, Elasticsearch 8.11.0, Redis 7.2.5, Apache Kafka 7.5.3, MinIO 2024-04-18
- **AI**: Qwen (通义千问) for embeddings and chat completion with RAG

The system supports document upload/management, hybrid search (BM25 + KNN), AI-powered Q&A, and role-based access control.

## Development Commands

### Starting the Full Stack

```bash
# Start all infrastructure services (PostgreSQL, Elasticsearch, Redis, Kafka, MinIO)
docker-compose up -d

# Backend - from project root
cd backend
mvn clean install
mvn spring-boot:run

# Frontend - separate terminal
cd frontend
npm install
npm run dev
```

Backend runs on `http://localhost:8080` - frontend on `http://localhost:5173`

### Backend Development

```bash
# Build all modules
cd backend && mvn clean install

# Skip tests during build
mvn clean install -DskipTests

# Run single module
cd backend/knowledge-base-api && mvn spring-boot:run

# Run tests
mvn test

# Package for deployment
mvn clean package -DskipTests
```

### Frontend Development

```bash
cd frontend

# Install dependencies
npm install

# Development server
npm run dev

# Build for production
npm run build

# Preview production build
npm run preview

# Lint code
npm run lint
```

## Architecture

### Backend Module Structure

The backend follows a **layered modular architecture**:

- **knowledge-base-api** - Presentation layer
  - REST controllers (`controller/`)
  - WebSocket handlers (`websocket/`)
  - Spring Security config (`security/`, `config/SecurityConfig.java`)
  - Entry point: `KbApiApplication.java`

- **knowledge-base-core** - Business logic layer
  - Domain entities (`domain/`)
  - Business services (`service/`)
  - Data repositories (`repository/`)
  - Kafka consumers for async document processing (`consumer/`)

- **knowledge-base-infrastructure** - External integrations
  - Elasticsearch client (`elasticsearch/`)
  - Kafka configuration (`kafka/`)
  - Qwen AI service (`qwen/`)
  - Document parsers (PDF, Office, etc.) (`document/`)

- **knowledge-base-common** - Shared utilities
  - DTOs for all modules (`dto/`)
  - Custom exceptions (`exception/`)
  - Unified API response wrapper (`result/`)

### Frontend Structure

- `src/pages/` - Route-level page components
- `src/components/` - Reusable UI components
- `src/store/` - Redux Toolkit slices (auth, document, qa, ui)
- `src/services/` - API clients with axios
- `src/hooks/` - Custom React hooks (including WebSocket)

### Key Data Flows

**Document Upload Flow**:
1. Frontend uploads document (supports chunked upload for large files via MinIO)
2. Backend publishes `DocumentUploaded` event to Kafka
3. Kafka consumer asynchronously processes: parse → chunk → embed → index
4. WebSocket notifies frontend of processing status

**Hybrid Search Flow**:
1. User query is embedded (Qwen embedding API, 1536 dimensions)
2. Parallel execution: BM25 keyword search + KNN vector search in Elasticsearch
3. RRF (Reciprocal Rank Fusion) merges results
4. Top chunks sent to Qwen for answer generation
5. Answer streamed via WebSocket

## Configuration

### Environment Variables

Copy `.env.example` to `.env` and configure:

**Required for basic functionality:**
- `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` - PostgreSQL connection
- `ELASTICSEARCH_URIS` - Elasticsearch endpoint
- `REDIS_HOST`, `REDIS_PORT` - Redis connection
- `QWEN_API_KEY` - Qwen AI API key (get from https://bailian.console.aliyun.com/)

**Important defaults:**
- Backend port: 8080 (configurable via `server.port`)
- Elasticsearch index: `kb_document_chunks` (auto-created on startup)
- Kafka topic: `document-processing` (auto-created)
- JWT secret: Must be set in production, minimum 32 characters

### Disabling Chunked Upload

By default, chunked upload is disabled. Set in `.env`:
```bash
CHUNKED_UPLOAD_ENABLED=true
```

When enabled, files >= 50MB trigger chunked upload flow with MinIO multipart upload.

## Testing

### Backend Testing

```bash
# Run all tests
cd backend && mvn test

# Run specific test class
mvn test -Dtest=DocumentServiceTest

# Run specific test method
mvn test -Dtest=DocumentServiceTest#testUploadDocument
```

### Frontend Testing

The frontend uses ESLint for code quality:
```bash
cd frontend && npm run lint
```

## Important Patterns

### Backend

- **JWT Authentication**: All API endpoints (except `/api/auth/login`) require JWT token in `Authorization: Bearer <token>` header
- **Async Processing**: Document processing is async via Kafka - upload returns immediately
- **Caching**: Redis for distributed cache, `@Cacheable` annotations used extensively
- **Pagination**: All list endpoints use `Pageable` with `page` and `size` parameters
- **Error Handling**: `GlobalExceptionHandler` maps exceptions to standardized error responses

### Frontend

- **API Base URL**: Configured via `VITE_API_BASE_URL` in `.env`
- **State Management**: Redux Toolkit with slices for auth, documents, QA, and UI state
- **WebSocket**: Custom hook `useWebSocketQA.ts` for streaming Q&A responses
- **File Upload**: `ChunkedFileUploader.tsx` handles both regular and chunked uploads

## Troubleshooting

### Common Issues

**Elasticsearch connection fails:**
- Verify ES is running: `curl http://localhost:9200/_cluster/health`
- Check `ELASTICSEARCH_URIS` in `.env`
- Ensure enough heap memory: ES requires at least 1GB (configured in docker-compose.yml)

**Kafka messages not processing:**
- Check Kafka is running: `docker-compose ps kafka`
- Verify topic exists: `docker exec -it kb-kafka kafka-topics.sh --list --bootstrap-server localhost:9092`
- Check consumer logs for errors

**Qwen API errors:**
- Verify `QWEN_API_KEY` is set correctly
- Test API key: `curl -X POST https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions -H "Authorization: Bearer YOUR_KEY" ...`

**Frontend can't connect to backend:**
- Check CORS configuration in `CorsConfig.java`
- Verify backend is running on port 8080
- Check `VITE_API_BASE_URL` in frontend `.env`

### Health Checks

```bash
# Backend health endpoint
curl http://localhost:8080/api/actuator/health

# Elasticsearch cluster health
curl http://localhost:9200/_cluster/health

# Redis connection
docker exec kb-redis redis-cli ping

# All services status
docker-compose ps
```

## File Locations

- **Backend config**: `backend/knowledge-base-api/src/main/resources/application.yml`
- **Frontend config**: `frontend/vite.config.ts`
- **Docker services**: `docker-compose.yml`
- **Environment template**: `.env.example`
- **Main documentation**: `README.md`, `DEVELOPMENT.md`, `TECHNOLOGY.md`, `SETUP.md`
