# AuthVolt

Authentication and notification platform built on Spring Boot. It provides JWT auth with rotating refresh tokens, Google sign-in, dynamic RBAC, and real-time notifications over Kafka, SSE and WebSocket.

Live at **https://authvolt.fun** · [Changelog](CHANGELOG.md)

## Architecture

```
            ┌──────────── nginx (TLS, rate limiting) ────────────┐
 browser ──►│  /api/v1/auth, /users, /admin   /api/v1/notifications, /ws │
            └───────────┬───────────────────────────┬───────────┘
                        ▼                           ▼
                  auth-service  ── account events ─► notification-service
                  │      │            (Kafka)            │      │
              Postgres  Redis                        Redis   SSE / STOMP
```

| Module | What it is |
|---|---|
| [`auth-service`](auth-service) | Spring Boot 4 / Java 21. Handles login, registration, refresh rotation, logout, Google OAuth2, and role/permission admin. Flyway manages the schema. |
| [`notification-service`](notification-service) | Consumes account events from Kafka and pushes them to clients over SSE and STOMP/WebSocket. |
| [`notification-contract`](notification-contract) | Shared Kafka event types used by both services. |
| [`client`](client) | React 19 + Vite frontend. |
| [`kafka`](kafka/README.md), [`redis`](redis/README.md), [`nginx`](nginx/README.md) | Infrastructure. Each has its own README. |
| [`loadtest`](loadtest) | Load test scripts and recorded runs. |

## Run locally

Requirements: Java 21, Maven, Docker, Node 20.

```bash
# 1. Infrastructure
(cd redis && docker compose up -d)
(cd kafka && docker compose up -d)

# 2. Shared contract (both services depend on it)
mvn -f notification-contract/pom.xml -DskipTests install

# 3. Services (each reads its config from a .env file; see its docker-compose.yml)
(cd auth-service && docker compose --env-file .env up -d --build)
(cd notification-service && docker compose --env-file .env up -d --build)

# 4. Frontend
(cd client && npm ci && npm run dev)
```

## API overview

| Endpoint | Auth |
|---|---|
| `POST /api/v1/auth/register`, `/login` | public |
| `POST /api/v1/auth/refresh` | `refreshToken` cookie + `X-XSRF-TOKEN` header |
| `POST /api/v1/auth/logout`, `/logout/all` | cookie / Bearer |
| `POST /api/v1/auth/oauth2/google` | public |
| `/api/v1/users/**` | Bearer |
| `/api/v1/admin/**` | Bearer + permission (e.g. `users:read`) |

## CI/CD

[`.github/workflows/ecr-push.yml`](.github/workflows/ecr-push.yml) runs on every PR and on every push to `develop` or `main`:

1. Tests and JaCoCo coverage (both services and the client)
2. Trivy filesystem scan, then SonarCloud analysis
3. Docker build, Trivy image scan, and push to ECR. Unchanged services reuse their previous image.
4. **On `main` only:** deploy to EC2 via SSM, run a public smoke test, and on failure roll back and open a revert PR

Merging to `main` deploys to production.

## Versioning

[Semantic Versioning](https://semver.org/). Releases are tagged `vX.Y.Z` on `main`. Commits follow [Conventional Commits](https://www.conventionalcommits.org/) (`feat:`, `fix:`, `chore:`, `ci:`, `docs:`).

## License

[MIT](LICENSE)
