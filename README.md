# AuthService

A Spring Boot authentication and authorization microservice: JWT-based login with refresh token rotation, role/permission-based access control (RBAC), and an Nginx/OpenResty gateway in front of it handling TLS, per-endpoint rate limiting, and request validation.

## Tech Stack

**Backend**
- Java 21
- Spring Boot (Spring Security, Spring Data JPA)
- JWT access + refresh tokens, refresh token rotation and family revocation
- Flyway for schema migrations
- Maven (`mvnw` included, no Gradle build)

**Database**
- PostgreSQL, HikariCP connection pooling (pool size and timeouts configurable via env vars)

**Gateway**
- OpenResty (Nginx + Lua)
- Lua-based request validation (`access_by_lua_file`)
- Per-route rate limiting: strict limits on `/auth/login`, `/auth/register`, `/auth/refresh`; looser general-API limit elsewhere
- Connection limiting (slow-loris mitigation)
- TLS via Let's Encrypt (with a self-signed fallback for local/dev)

**Testing**
- JUnit 5, Spring Boot Test, integration tests against a real Spring context and DB
- JaCoCo for coverage reporting

## Architecture

```
                         ┌─────────────────────────────────────────────┐
                         │              OpenResty / Nginx               │
  Client ── HTTPS ──────▶│  TLS termination                             │
                         │  Lua request validation (access_by_lua_file) │
                         │  Rate limiting (per-route, per-IP)           │
                         │  Connection limiting                         │
                         └───────────────────┬───────────────────────────┘
                                              │ HTTP (internal)
                                              ▼
                         ┌─────────────────────────────────────────────┐
                         │              AuthService (Spring Boot)       │
                         │                                               │
                         │  Filters        JwtAuthenticationFilter,     │
                         │                 LoggingFilter (correlation   │
                         │                 ID injection)                │
                         │                       │                      │
                         │  Controllers    AuthController                │
                         │                 UserController                │
                         │                 AdminController                │
                         │                       │                      │
                         │  Services       AuthService, UserService,    │
                         │                 RoleService, PermissionService│
                         │                 RefreshTokenService           │
                         │                       │                      │
                         │  Security       JwtService, CookieService,   │
                         │                 CustomUserDetailsService      │
                         │                       │                      │
                         │  Repositories   UserRepository,               │
                         │                 TokenRepository, ...          │
                         └───────────────────────┬───────────────────────┘
                                                  │ JDBC
                                                  ▼
                                     ┌───────────────────────┐
                                     │   PostgreSQL (RDS)     │
                                     └───────────────────────┘
```

## Key Features

- **JWT auth with refresh rotation** — short-lived access tokens, rotating refresh tokens, and refresh token *family* revocation (a compromised/reused refresh token invalidates the whole chain, not just itself).
- **RBAC** — fine-grained permissions (`admin:create`, `user:update`, etc.) enforced with `@PreAuthorize`, including ownership checks (e.g., a user can update their own record without an admin permission).
- **Gateway-level protection** — rate limiting and request validation happen at Nginx/Lua before a request ever reaches the JVM.
- **Correlation ID logging** — every request is tagged for end-to-end tracing across filters, services, and logs.
- **Admin bootstrap** — an initial admin user/role/permission set is created on startup (`AdminInitializer`), configured via env vars, not hardcoded.

## Prerequisites

- Java 21
- Maven (or use the bundled `./mvnw`)
- Docker & Docker Compose (for running Postgres + Nginx locally)
- PostgreSQL 15+ if not using Docker

## Environment Variables

Copy `.env.example` to `.env` and fill in real values. **Never commit `.env` or any file containing real secrets.**

| Variable | Purpose |
|---|---|
| `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD` | Postgres connection |
| `ADMIN_EMAIL`, `ADMIN_USERNAME`, `ADMIN_PASSWORD` | Bootstrap admin account created on first startup |
| `JWT_SECRET`, `JWT_ISSUER`, `JWT_AUDIENCE` | Token signing and validation |
| `JWT_ACCESS_TTL_SECOND`, `JWT_REFRESH_TTL_SECOND` | Token lifetimes |
| `JWT_REFRESH_TOKEN_COOKIE_NAME`, `JWT_COOKIE_HTTP_ONLY`, `JWT_COOKIE_SECURE`, `JWT_COOKIE_SAME_SITE`, `JWT_COOKIE_DOMAIN`, `JWT_COOKIE_PATH` | Refresh token cookie config |
| `HIKARI_MAX_POOL_SIZE`, `HIKARI_MIN_IDLE`, `HIKARI_CONNECTION_TIMEOUT`, `HIKARI_IDLE_TIMEOUT`, `HIKARI_MAX_LIFETIME`, `HIKARI_LEAK_DETECTION_THRESHOLD` | Connection pool tuning |
| `SERVER_PORT`, `SERVER_CONNECTION_TIMEOUT` | Server config |
| `QUERY_TIMEOUT`, `LOCK_TIMEOUT` | DB query safety limits |
| `JPA_SHOW_SQL`, `JPA_FORMAT_SQL` | Hibernate SQL logging (dev only) |
| `LOG_LEVEL_*` | Per-package log verbosity |
| `ACTUATOR_ENDPOINTS`, `ACTUATOR_HEALTH_SHOW_DETAILS` | Spring Actuator exposure |
| `LOG_FILE` | Log output path |

## Running Locally

```bash
git clone https://github.com/lodhiPlayBits/AuthService.git
cd AuthService/auth_service
cp .env.example .env   # fill in real values
docker-compose up --build -d
```

To run the service directly (without Docker), start Postgres separately, then:

```bash
./mvnw spring-boot:run
```

## API Endpoints

**Auth** — `/api/v1/auth`

| Method | Path | Description | Auth Required |
|---|---|---|---|
| POST | `/login` | Authenticate, issue access + refresh tokens | No |
| POST | `/register` | Register a new user | No |
| POST | `/refresh` | Rotate refresh token, issue new access token | Refresh cookie |
| POST | `/logout` | Revoke current refresh token | Yes |
| POST | `/logout/all` | Revoke all refresh tokens for the user (all devices) | Yes |

**Users** — `/api/v1/users`

| Method | Path | Description | Permission |
|---|---|---|---|
| POST | `/create-user` | Create a user | `admin:create` |
| GET | `/getByEmail` | Look up a user by email | `admin:read` or `user:read` |
| GET | `/{id}` | Get user by ID | `admin:read`, or `user:read` + self |
| PUT | `/{id}` | Update user | `admin:update`, or `user:update` + self |
| DELETE | `/{id}` | Delete user | `admin:delete`, or `user:delete` + self |
| POST | `/{id}/change-password` | Change password | self only |

**Admin** — `/api/v1/admin`

| Method | Path | Description |
|---|---|---|
| GET | `/roles` | List roles |
| POST | `/roles` | Create role |
| DELETE | `/roles/{roleId}` | Delete role |
| POST | `/roles/{roleId}/permissions` | Assign permissions to role |
| DELETE | `/roles/{roleId}/permissions` | Revoke permissions from role |
| GET | `/permissions` | List permissions |
| POST | `/permissions` | Create permission |
| DELETE | `/permissions/{permissionId}` | Delete permission |
| GET | `/users` | List users |
| PUT | `/users/{userId}/roles` | Assign roles to user |

## Testing

```bash
./mvnw clean test
```

Coverage report (JaCoCo):

```bash
./mvnw clean test
# open target/site/jacoco/index.html
```

## License

No license file is currently included — all rights reserved by default. Add a `LICENSE` file if you intend this to be reused by others.
