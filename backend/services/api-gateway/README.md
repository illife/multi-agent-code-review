# API Gateway

API Gateway is the single HTTP entry point for Think Platform. It owns request routing, CORS, authentication filters, admin checks, and API documentation aggregation for local development.

## Ports

| Service | Port | Notes |
| --- | --- | --- |
| API Gateway | 8082 | Public backend entry point |
| Auth API | 8083 | Authentication and users |
| Code Intelligence API | 8081 | Code review, project, learning, teaching |
| Knowledge Mentor API | 8080 | Documents, search, QA |

Do not make all services use the gateway port. Each service listens on its own port; clients should call the gateway.

## Routes

| Path | Target |
| --- | --- |
| `/api/auth/**` | `auth-api:8083` |
| `/api/users/**` | `auth-api:8083` |
| `/api/agent/**` | `code-intelligence-api:8081` |
| `/api/learning/**` | `code-intelligence-api:8081` |
| `/api/project/**`, `/api/projects/**` | `code-intelligence-api:8081` |
| `/api/review/**` | `code-intelligence-api:8081` |
| `/api/analysis/**` | `code-intelligence-api:8081` |
| `/api/teaching/**` | `code-intelligence-api:8081` |
| `/api/architecture/**` | `code-intelligence-api:8081` |
| `/api/documents/**` | `knowledge-mentor-api:8080` |
| `/api/qa/**` | `knowledge-mentor-api:8080` |
| `/api/search/**` | `knowledge-mentor-api:8080` |
| `/api/lessons/**` | `knowledge-mentor-api:8080` |
| `/api/exercises/**` | `knowledge-mentor-api:8080` |
| `/api/ws/**` | `knowledge-mentor-api:8080` |

## Local Run

```bash
cd backend/services/api-gateway
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

With Docker Compose:

```bash
docker compose up -d api-gateway
```

## Configuration

The gateway defaults match the Docker Compose service names:

- `AUTH_SERVICE_URL=http://auth-api:8083`
- `CODE_INTELLIGENCE_SERVICE_URL=http://code-intelligence-api:8081`
- `KNOWLEDGE_MENTOR_SERVICE_URL=http://knowledge-mentor-api:8080`

`JWT_SECRET` must be supplied through the environment.

## Production Notes

- Keep Swagger/Knife4j disabled in production.
- Keep CORS origins explicit.
- Route all browser traffic through the gateway instead of exposing individual services.
