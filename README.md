# AuthService · AuthVolt

[![CI](https://github.com/lodhiPlayBits/AuthService/actions/workflows/ecr-push.yml/badge.svg?branch=main)](https://github.com/lodhiPlayBits/AuthService/actions/workflows/ecr-push.yml)
[![Release](https://img.shields.io/github/v/release/lodhiPlayBits/AuthService)](https://github.com/lodhiPlayBits/AuthService/releases/latest)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

**Production-oriented authentication, authorization and real-time notification platform built with Spring Boot 4, PostgreSQL, Redis, Kafka and an OpenResty/Nginx gateway.**

AuthVolt provides secure user authentication: JWT access tokens, refresh-token rotation with token-family revocation, Google sign-in, role-based access control with fine-grained permissions, and gateway-level request protection. A separate notification service streams account events and admin announcements to users in real time over Kafka, SSE and WebSocket.

The system is built backend-first, with security, database safety, observability, rate limiting and automated production deployment in mind.

**Live:** [authvolt.fun](https://authvolt.fun) · **Changelog:** [CHANGELOG.md](CHANGELOG.md)

---

## Features

### Authentication

* JWT-based authentication (HS512, issuer and audience validated)
* Short-lived access tokens
* Long-lived refresh tokens in `HttpOnly` cookies scoped to `/api/v1/auth`
* Refresh-token rotation
* Refresh-token reuse detection
* Refresh-token family revocation
* Refresh tokens stored as SHA-256 hashes, never in plain text
* CSRF double-submit protection (`XSRF-TOKEN` cookie + `X-XSRF-TOKEN` header)
* Login with **email, username or phone number** (identifier type detected automatically)
* Google OAuth2 sign-in with a profile-completion step
* Logout and logout-all-devices
* Access-token blacklist in Redis, so logged-out tokens stop working immediately
* BCrypt password hashing on virtual threads, behind a concurrency limit

### Authorization / RBAC

* Role-based access control
* Fine-grained permissions
* Roles and permissions stored in the database and editable at runtime
* `@PreAuthorize` checks with **ownership rules** on user endpoints
* Custom `@RequiresPermission` aspect on admin endpoints
* Dynamic role creation and deletion
* Dynamic permission creation and deletion
* Assign or revoke permissions on roles
* Assign roles to users, enable or disable accounts
* Audit log of every security-relevant event

Example permissions:

```text
admin:create      admin:read        admin:update      admin:delete
user:read         user:update       user:delete
users:read        users:update      users:assign-roles
roles:create      roles:read        roles:update      roles:delete
permissions:create  permissions:read  permissions:delete
audit:read        audit:manage
```

### Real-time Notifications

* `auth-service` publishes account events to **Kafka** after the database transaction commits
* `notification-service` pushes them to the user over **Server-Sent Events**
* Events for offline users are buffered in **Redis** (last 50, kept 7 days) and replayed on reconnect
* Admin announcements over **STOMP/WebSocket**, with ack and dismiss
* JWT-authenticated WebSocket handshake and per-subscription authorization
* Dead-letter topic for events that keep failing

### Gateway Security

Requests pass through an **OpenResty/Nginx gateway** before reaching any Spring Boot service.

* TLS termination
* Let's Encrypt certificates (auto-renewed by `certbot`)
* Self-signed certificate fallback for local development
* Lua-based request validation
* Per-route rate limiting
* Per-IP connection limiting
* `X-Request-ID` injection for tracing
* Bad requests rejected before they reach the JVM

Rate-limit strategy:

```text
/api/v1/auth/login     → strict   (3 r/s per IP, burst 5)
/api/v1/auth/register  → strict   (3 r/s per IP, burst 5)
/api/v1/auth/refresh   → strict   (3 r/s per IP, burst 5)
all other APIs         → relaxed  (10 r/s per IP, burst 20)
connections            → max 20 per IP
```

### Observability

* Correlation ID generated at the gateway and propagated through every log line
* Structured request logging (`LoggingFilter`, Logback)
* Spring Boot Actuator with liveness and readiness probes
* Audit log table for security events
* Configurable log levels and log output
* Kafka UI and a Prometheus Kafka exporter

### CI/CD & DevSecOps

* Tests and JaCoCo coverage on every PR
* Trivy filesystem and image scans, failing on fixable CRITICAL vulnerabilities
* SonarCloud static analysis
* Images pushed to Amazon ECR, tagged by commit SHA
* Only changed services are rebuilt
* Automatic deploy on merge to `main` via AWS SSM
* Health checks, local and public smoke tests, and **automatic rollback**

---

# Architecture

<!-- Add the Eraser architecture image here, e.g. ![Architecture](docs/architecture.png) -->

```text
                                  ┌──────────────────────────────┐
                                  │       Client / Browser       │
                                  │          React SPA           │
                                  └──────────────┬───────────────┘
                                                 │
                                                 │ HTTPS
                                                 ▼
 ┌─────────────────────────────────────────────────────────────────────────────────────────────────┐
 │                       OpenResty / Nginx  ·  nginx-gateway  ·  :80 / :443                        │
 │                                                                                                 │
 │  • TLS termination (Let's Encrypt)          • Per-route rate limiting (login 3 r/s, API 10 r/s) │
 │  • Lua request validation                   • Per-IP connection limiting (20 per IP)            │
 │  • X-Request-ID injection                   • CORS policy                                       │
 └──────┬──────────────────────┬──────────────────────────────────────────────────────┬────────────┘
        │                      │                                                      │
        │                      │                                                      │
        │ /                    │ /api/v1/auth                  /api/v1/notifications  │
        │                      │ /api/v1/users                     /ws/announcements  │
        │                      │ /api/v1/admin                                        │
        ▼                      ▼                                                      ▼
 ┌──────────────┐  ┌────────────────────────┐     ┌─────────────────┐     ┌────────────────────────┐
 │client-service│  │      auth-service      │pub  │  Kafka (KRaft)  │ sub │  notification-service  │
 │ React + Vite │  │   Spring Boot :5001    ├────►│     2 nodes     ├────►│   Spring Boot :5002    │
 │ static build │  │  JWT · OAuth2 · RBAC   │     │ account-events  │     │    SSE · STOMP / WS    │
 └──────────────┘  └────┬──────────────┬────┘     └─────────────────┘     └───────────┬────────────┘
                        │              │                                              │
                        │ JDBC         │ cache · blacklist                            │ missed events
                        │              │                                              │
                        ▼              ▼                                              ▼
              ┌───────────────────┐  ┌─────────────────────────────────────────────────────────────┐
              │ PostgreSQL (RDS)  │  │                         Redis :6379                         │
              │   Flyway V1–V14   │  │ roles cache · JWT blacklist · per-user missed-event buffer  │
              └───────────────────┘  └─────────────────────────────────────────────────────────────┘
```

Every box is a separate Docker container, on the `authnet` and `authvolt-kafka-network` Docker networks. Only nginx exposes ports to the internet. PostgreSQL runs on **Amazon RDS**. `auth-service` also calls **Google** to verify ID tokens during Google sign-in.

## auth-service internals

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│                        auth-service  ·  Spring Boot :5001                        │
│                                                                                  │
│  Filters                                                                         │
│   ├── LoggingFilter               correlation ID, request logging                │
│   ├── CsrfCookieFilter            XSRF-TOKEN cookie                              │
│   └── JwtAuthenticationFilter     signature · iss · aud · exp · blacklist        │
│                                                                                  │
│  Controllers                                                                     │
│   ├── AuthController              /api/v1/auth                                   │
│   ├── OAuth2Controller            /api/v1/auth/oauth2                            │
│   ├── UserController              /api/v1/users    @PreAuthorize + ownership     │
│   └── AdminController             /api/v1/admin    @RequiresPermission           │
│                                                                                  │
│  Services                                                                        │
│   ├── AuthService                 login, logout, logout-all                      │
│   ├── RefreshTokenService         rotation + reuse detection                     │
│   ├── RefreshTokenFamilyService   family revocation                              │
│   ├── TokenBlacklistService       Redis blacklist by jti                         │
│   ├── GoogleOAuth2Service         Google ID-token sign-in                        │
│   ├── BCryptService               virtual threads + semaphore                    │
│   ├── UserService · RoleService · PermissionService · AdminService               │
│   ├── AccountEventProducer        Kafka, published after commit                  │
│   └── AuditService                security audit log                             │
│                                                                                  │
│  Security                                                                        │
│   ├── JwtService                  issue + parse HS512 tokens                     │
│   ├── TokenHashService            SHA-256 of refresh-token jti                   │
│   ├── CookieService               HttpOnly refresh cookie                        │
│   ├── CustomAuthenticationProvider · CustomUserDetailsService                    │
│   └── RequiresPermissionAspect    permission checks on admin APIs                │
│                                                                                  │
│  Repositories                                                                    │
│   └── User · Role · Permission · RefreshToken · AuditLog                         │
└──────────────────────────────────────────────────────────────────────────────────┘
```

## notification-service internals

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│                    notification-service  ·  Spring Boot :5002                    │
│                                                                                  │
│  Security                                                                        │
│   └── OAuth2 resource server       validates auth-service JWTs                   │
│                                                                                  │
│  Kafka consumers                                                                 │
│   ├── AccountEventConsumer         account-events → SSE or Redis buffer          │
│   └── AnnouncementConsumer         broadcast-announcements → STOMP topic         │
│                                                                                  │
│  HTTP                                                                            │
│   └── NotificationController       GET /api/v1/notifications/stream (SSE)        │
│                                                                                  │
│  WebSocket  (STOMP over /ws/announcements)                                       │
│   ├── JwtHandshakeInterceptor      JWT check on connect                          │
│   ├── SubscriptionAuthorizationInterceptor   who may subscribe where             │
│   └── AnnouncementStompController  /app/announcement.send · .ack · .dismiss      │
│                                                                                  │
│  Connection management                                                           │
│   ├── SseConnectionManager · SseHeartbeatScheduler                               │
│   └── WebSocketSessionManager                                                    │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

# Authentication Flow

## Login

```text
Client
   │
   │ POST /api/v1/auth/login   { identifier, password }
   ▼
OpenResty
   │
   │ Validate + Rate Limit (3 r/s)
   ▼
auth-service
   │
   ├── Detect identifier type (email / username / phone)
   ├── Verify password (BCrypt on virtual thread, semaphore-limited)
   ├── Generate access token  (HS512 · iss · aud · roles · permissions)
   ├── Generate refresh token (new token family)
   ├── Store SHA-256 hash of the refresh token
   └── Write LOGIN_SUCCESS audit event
   │
   ▼
Client
   │
   ├── Access Token            (response body)
   ├── HttpOnly Refresh Cookie (path /api/v1/auth)
   └── XSRF-TOKEN Cookie
```

## Refresh Token Rotation

```text
POST /api/v1/auth/refresh
   Cookie: refreshToken=#1
   Header: X-XSRF-TOKEN
          │
          ▼
   Validate CSRF + JWT
          │
          ▼
   Look up hash of #1
          │
          ├── Current → mark #1 used
          │                │
          │                └── Generate #2 (same family)
          │                          │
          │                          ▼
          │                  New Access Token
          │                  New Refresh Cookie
          │
          └── Already used → reuse detected (see below)
```

If a previously used refresh token is presented again:

```text
Compromised Refresh Token
          │
          ▼
Reuse detected
          │
          ├── Write TOKEN_REUSE audit event
          │
          ▼
Revoke entire token family
          │
          ▼
All tokens in the chain become invalid
(attacker and victim must both log in again)
```

This stops an attacker from continuing to use a stolen refresh-token chain.

## Logout

```text
POST /logout        (refresh cookie)       POST /logout/all     (Bearer)
       │                                          │
       ▼                                          ▼
Revoke current token family               Revoke ALL refresh tokens of user
       │                                          │
       └──────────────────┬───────────────────────┘
                          ▼
         Blacklist current access token in Redis
         (TTL = token's remaining lifetime)
                          │
                          ▼
                Clear refresh cookie
```

## Google Sign-in

```text
Client ── Google ID token ──► POST /api/v1/auth/oauth2/google
                                        │
                                        ▼
                           Verify token with Google
                                        │
                        ┌───────────────┴───────────────┐
                        ▼                               ▼
                 Existing user                      New user
                        │                               │
                        │                 Create account (provider = GOOGLE)
                        │                               │
                        │                               ▼
                        │              PATCH /oauth2/complete-profile
                        │                               │
                        └───────────────┬───────────────┘
                                        ▼
                         Access Token + Refresh Cookie
```

---

# Real-time Notification Flow

```text
Admin disables a user
        │
        ▼
auth-service ── commit DB transaction
        │
        │ afterCommit → publish AccountEvent (ACCOUNT_DISABLED)
        ▼
Kafka topic: account-events
        │
        ▼
notification-service (AccountEventConsumer)
        │
        ├── User online  → push over SSE  (GET /api/v1/notifications/stream)
        │
        └── User offline → store in Redis buffer (last 50, 7 days)
                                  │
                                  ▼
                       Replayed when the user reconnects
```

Account event types:

```text
ACCOUNT_DISABLED   ACCOUNT_ENABLED   ACCOUNT_DELETED
PASSWORD_CHANGED   ROLE_CHANGED      SESSION_REVOKED
```

Admin announcements:

```text
Admin ── STOMP SEND /app/announcement.send ──► notification-service
                                                     │
                                                     ▼
                                    Kafka topic: broadcast-announcements
                                                     │
                                                     ▼
                              every notification-service instance consumes
                                                     │
                                                     ▼
                         Clients subscribed to /topic/announcements
                                                     │
                              ack / dismiss ──► admin-responses topic ──► Admin
```

---

# RBAC Architecture

Authorization is based on:

```text
User
  │
  └── Roles
        │
        └── Permissions
```

Example:

```text
Admin User
    │
    └── ADMIN
          ├── admin:create
          ├── admin:read
          ├── admin:update
          ├── admin:delete
          ├── roles:* / permissions:*
          └── users:read / users:update / users:assign-roles
```

Roles and permissions are carried in the access token, so authorization doesn't need a database lookup per request. Role lookups that do hit the database are cached in Redis and evicted whenever a role changes.

User endpoints use `@PreAuthorize` with ownership checks:

```java
@PutMapping("/{id}")
@PreAuthorize("hasAuthority('admin:update') or " +
              "(hasAuthority('user:update') and #id == authentication.principal.userId)")
public ResponseEntity<UserResponseDTO> updateUser(@PathVariable Long id, ...) { ... }
```

Admin endpoints use a custom aspect:

```java
@GetMapping("/users")
@RequiresPermission("users:read")
public ResponseEntity<PagedResponse<CreateUserResponseDTO>> getAllUsers(...) { ... }
```

Ownership rules:

```text
Admin
 └── Can read, update and delete any user

Normal User
 ├── Can read, update and delete only their own account
 └── Can change only their own password
```

---

# Tech Stack

## Backend

* Java 21 (virtual threads)
* Spring Boot 4.1
* Spring Security
* Spring Data JPA / Hibernate
* Spring Kafka
* Spring WebSocket (STOMP)
* Maven

## Database & Cache

* PostgreSQL (Amazon RDS)
* HikariCP
* Flyway
* Redis

## Messaging

* Apache Kafka (KRaft mode, 2 nodes, no ZooKeeper)
* Kafka UI
* Kafka Prometheus exporter

## Security

* JWT (JJWT, HS512)
* Access + Refresh Tokens
* Refresh Token Rotation
* Token Family Revocation
* HTTP-only Cookies + CSRF
* Google OAuth2
* RBAC
* Fine-grained Permissions

## Gateway

* OpenResty
* Nginx
* Lua
* Let's Encrypt
* TLS

## Frontend

* React 19
* Vite
* React Router
* STOMP.js

## Infrastructure

* Docker
* Docker Compose
* AWS EC2
* AWS RDS
* Amazon ECR
* AWS Systems Manager (SSM)
* Linux

## CI/CD & Quality

* GitHub Actions
* JaCoCo
* SonarCloud
* Trivy

## Testing

* JUnit 5
* Spring Boot Test
* Integration tests (H2)
* Vitest + Testing Library
* k6 load tests

---

# Project Structure

```text
AuthService/
│
├── auth-service/                 # Authentication & authorization API
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/lodhi/auth/
│   │   │   │   ├── audit/          # Audit events and log
│   │   │   │   ├── config/         # Security, Kafka, Redis, BCrypt executor
│   │   │   │   ├── controllers/    # Auth, OAuth2, User, Admin
│   │   │   │   ├── filters/        # LoggingFilter
│   │   │   │   ├── security/       # JWT, cookies, @RequiresPermission
│   │   │   │   ├── services/       # Business logic
│   │   │   │   └── respositories/  # Spring Data repositories
│   │   │   └── resources/
│   │   │       ├── application.yml
│   │   │       └── db/migration/   # Flyway V1–V14
│   │   └── test/
│   ├── Dockerfile
│   ├── docker-compose.yml
│   └── .env.example
│
├── notification-service/         # Kafka consumer, SSE + STOMP push
├── notification-contract/        # Shared Kafka event classes
├── client/                       # React + Vite frontend
│
├── nginx/                        # OpenResty gateway
│   ├── nginx.conf
│   ├── conf.d/                   # Routing, rate limits, upstreams
│   ├── lua/                      # Request validation
│   └── Dockerfile
│
├── kafka/                        # 2-node KRaft cluster
├── redis/                        # Redis config and persistence
├── loadtest/                     # k6 scripts and results
│
├── .github/
│   ├── workflows/ecr-push.yml    # CI/CD pipeline
│   └── scripts/                  # SSM deploy + smoke test
│
├── CHANGELOG.md
└── README.md
```

---

# API Endpoints

## Authentication

Base URL:

```text
/api/v1/auth
```

| Method | Endpoint | Description | Authentication |
| ------ | -------- | ----------- | -------------- |
| `POST` | `/register` | Register user | Public |
| `POST` | `/login` | Login with email, username or phone | Public |
| `POST` | `/refresh` | Rotate refresh token | Refresh cookie + `X-XSRF-TOKEN` |
| `POST` | `/logout` | Revoke current session | Refresh cookie |
| `POST` | `/logout/all` | Revoke all user sessions | Bearer token |
| `POST` | `/oauth2/google` | Sign in with Google | Public |
| `PATCH` | `/oauth2/complete-profile` | Finish Google sign-up | Bearer token |

---

## Users

Base URL:

```text
/api/v1/users
```

| Method | Endpoint | Description | Authorization |
| ------ | -------- | ----------- | ------------- |
| `POST` | `/create-user` | Create user | `admin:create` |
| `GET` | `/getByEmail` | Find user by email | `admin:read` / `user:read` |
| `GET` | `/{id}` | Get user | `admin:read` or owner |
| `PUT` | `/{id}` | Update user | `admin:update` or owner |
| `DELETE` | `/{id}` | Delete user | `admin:delete` or owner |
| `POST` | `/{id}/change-password` | Change password | Owner only |

---

## Administration

Base URL:

```text
/api/v1/admin
```

| Method | Endpoint | Description | Permission |
| ------ | -------- | ----------- | ---------- |
| `GET` | `/roles` | List roles | `roles:read` |
| `POST` | `/roles` | Create role | `roles:create` |
| `DELETE` | `/roles/{roleId}` | Delete role | `roles:delete` |
| `POST` | `/roles/{roleId}/permissions` | Assign permissions | `roles:update` |
| `DELETE` | `/roles/{roleId}/permissions` | Revoke permissions | `roles:update` |
| `GET` | `/permissions` | List permissions | `permissions:read` |
| `POST` | `/permissions` | Create permission | `permissions:create` |
| `DELETE` | `/permissions/{permissionId}` | Delete permission | `permissions:delete` |
| `GET` | `/users?page=&size=&sortBy=` | List users (paginated) | `users:read` |
| `PUT` | `/users/{userId}/roles` | Assign roles | `users:assign-roles` |
| `PUT` | `/users/{userId}/status` | Enable / disable user | `users:update` |

---

## Notifications

| Type | Endpoint | Description |
| ---- | -------- | ----------- |
| SSE | `GET /api/v1/notifications/stream` | Live account events for the logged-in user |
| STOMP | `/ws/announcements` | WebSocket endpoint |
| STOMP | `SUBSCRIBE /topic/announcements` | Receive announcements |
| STOMP | `SEND /app/announcement.send` | Broadcast an announcement (admin) |
| STOMP | `SEND /app/announcement.ack` · `.dismiss` | Acknowledge or dismiss |

---

## Kafka Topics

| Topic | Producer | Consumer | Purpose |
| ----- | -------- | -------- | ------- |
| `account-events` | auth-service | notification-service | Account status, role, password and session changes |
| `account-events.DLT` | Spring Kafka | – | Dead-letter topic |
| `broadcast-announcements` | notification-service | all notification-service instances | Admin announcements |
| `admin-responses` | notification-service | notification-service | Ack and dismiss responses back to the admin |

---

# Environment Configuration

Copy the example environment file:

```bash
cp auth-service/.env.example auth-service/.env
```

Never commit `.env` or any file containing real secrets.

### Database

```text
DB_HOST
DB_PORT
DB_NAME
DB_USERNAME
DB_PASSWORD
```

### Admin Bootstrap

```text
ADMIN_EMAIL
ADMIN_USERNAME
ADMIN_PASSWORD
ADMIN_PHONE
```

### JWT

```text
JWT_SECRET
JWT_ISSUER
JWT_AUDIENCE
JWT_ACCESS_TTL_SECOND
JWT_REFRESH_TTL_SECOND
```

### Refresh Cookie

```text
JWT_REFRESH_TOKEN_COOKIE_NAME
JWT_COOKIE_HTTP_ONLY
JWT_COOKIE_SECURE
JWT_COOKIE_SAME_SITE
JWT_COOKIE_DOMAIN
JWT_COOKIE_PATH
```

### Google OAuth2

```text
GOOGLE_CLIENT_ID
```

### Redis

```text
REDIS_HOST
REDIS_PORT
REDIS_PASSWORD
CACHE_TTL
```

### Kafka

```text
KAFKA_BOOTSTRAP_SERVERS
KAFKA_TOPIC_REPLICATION_FACTOR
```

### HikariCP

```text
HIKARI_MAX_POOL_SIZE
HIKARI_MIN_IDLE
HIKARI_CONNECTION_TIMEOUT
HIKARI_IDLE_TIMEOUT
HIKARI_MAX_LIFETIME
HIKARI_LEAK_DETECTION_THRESHOLD
```

### Application

```text
SERVER_PORT
SERVER_CONNECTION_TIMEOUT
QUERY_TIMEOUT
LOCK_TIMEOUT
BCRYPT_MAX_CONCURRENT
```

### Logging / Actuator

```text
LOG_LEVEL_*
LOG_FILE
ACTUATOR_ENDPOINTS
ACTUATOR_HEALTH_SHOW_DETAILS
```

### Frontend (build time)

```text
VITE_API_URL
VITE_GOOGLE_CLIENT_ID
```

Development-only settings:

```text
JPA_SHOW_SQL
JPA_FORMAT_SQL
```

---

# Running Locally

## Prerequisites

Make sure you have:

* Java 21
* Maven 3.9+
* Docker
* Docker Compose
* Node.js 20
* PostgreSQL 15+

Clone the repository:

```bash
git clone https://github.com/lodhiPlayBits/AuthService.git
cd AuthService
```

Create the shared Docker network:

```bash
docker network create authnet
```

Start Redis and Kafka:

```bash
(cd redis && docker compose up -d)
(cd kafka && docker compose up -d)
```

Install the shared event contract (both services depend on it):

```bash
mvn -f notification-contract/pom.xml -DskipTests install
```

Configure the environment. `notification-service` reads the same file, so both services share the JWT secret, issuer and audience:

```bash
cp auth-service/.env.example auth-service/.env     # fill in the values
```

Build the jars, then start the services:

```bash
mvn -f auth-service/pom.xml -DskipTests package
mvn -f notification-service/pom.xml -DskipTests package

(cd auth-service && docker compose --env-file .env up -d --build)
(cd notification-service && docker compose up -d --build)
```

Start the frontend:

```bash
cd client
npm ci
npm run dev
```

Check running containers:

```bash
docker ps
```

View logs:

```bash
docker logs -f auth-service
```

Flyway creates the database schema on first start.

---

# Testing

Run the backend test suites:

```bash
mvn -f auth-service/pom.xml clean verify
mvn -f notification-service/pom.xml clean verify
```

Open the JaCoCo coverage report:

```text
auth-service/target/site/jacoco/index.html
```

Run the frontend tests:

```bash
cd client && npm test
```

The test suite includes:

* Unit tests
* Spring application-context tests
* Controller, filter and security tests
* Repository and integration tests
* Frontend component tests

Load tests live in [`loadtest/`](loadtest). Moving BCrypt onto virtual threads behind a semaphore gave **+28% throughput, −29% p95 login latency and zero timeouts** at 40 RPS on a 2-vCPU t3.large.

---

# CI/CD Pipeline

Everything runs from [`.github/workflows/ecr-push.yml`](.github/workflows/ecr-push.yml).

```text
Pull Request / Push
        │
        ├── test     (auth-service · notification-service · client) + JaCoCo
        ├── scan     Trivy filesystem scan (6 components)
        └── sonar    SonarCloud analysis
        │
        ▼
build-push
        │
        ├── Changed service   → build image → Trivy image scan → push to ECR
        └── Unchanged service → retag previous image in ECR (no rebuild)
        │
        ▼
ci-passed  (single required check)
        │
        │  main branch only
        ▼
deploy  ── AWS SSM ──► deploy.sh on EC2
                          │
                          ├── docker compose pull + up
                          ├── wait until all containers are healthy
                          ├── local smoke test
                          └── on failure → redeploy last good tag
        │
        ▼
smoke-test  (from the public internet: DNS → TLS → nginx → services)
        │
        └── on failure → rollback
                            ├── redeploy previous commit via SSM
                            └── open a revert PR for review
```

| Event | What runs |
| ----- | --------- |
| Pull request | Tests, scans, Sonar, image build (no push) |
| Push to `develop` | Everything above + push images to ECR |
| Push to `main` | Everything above + deploy + smoke test (+ rollback) |

`main` and `develop` are protected. Every change goes through a pull request.

---

# Production Deployment

```text
Internet
   │
   ▼
AWS EC2 (t3.large, Docker Compose)
   │
   ├── OpenResty / Nginx
   │       ├── TLS (Let's Encrypt)
   │       ├── Rate Limiting
   │       └── Request Validation
   │
   ├── client-service
   ├── auth-service ──────────┐
   ├── notification-service   │
   ├── Redis                  │
   └── Kafka (2 nodes)        │
                              ▼
                          AWS RDS
                         PostgreSQL

Images:   Amazon ECR (tagged by commit SHA)
Deploys:  GitHub Actions → AWS SSM → deploy.sh
```

Production configuration uses environment variables for:

* Database credentials
* JWT signing secret
* Admin credentials
* Google client ID
* Cookie configuration
* Connection pool configuration
* Logging configuration

Do **not** hardcode secrets into Java source code, Dockerfiles, Git history, or configuration files committed to the repository.

---

# Security Considerations

This project deliberately places security controls at several layers.

### Gateway

```text
TLS
 ↓
Request Validation (Lua)
 ↓
Rate Limiting
 ↓
Connection Limiting
```

### Application

```text
Authentication
 ↓
JWT Validation (signature · iss · aud · exp · blacklist)
 ↓
CSRF check (cookie endpoints)
 ↓
Authorization (permissions)
 ↓
Ownership Checks
 ↓
Audit Log
```

### Tokens

```text
Short-lived access token
 ↓
Rotating refresh token (hashed at rest)
 ↓
Reuse detection → family revocation
 ↓
Redis blacklist on logout
```

### Database

```text
Connection Pool Limits
 ↓
Query Timeout
 ↓
Lock Timeout
 ↓
PostgreSQL
```

### Supply Chain

```text
Trivy filesystem scan
 ↓
Trivy image scan
 ↓
SonarCloud
 ↓
Protected branches + PR-only changes
```

No single security mechanism is expected to protect the entire system.

---

# Design Goals

The main goals of this project are:

* Secure authentication
* Fine-grained authorization
* Stateless access-token authentication
* Stateful refresh-token management
* Refresh-token compromise detection
* Defense in depth at the gateway and application layers
* Event-driven communication between services
* Real-time user notifications
* Production-oriented database configuration
* Observable request flow
* Containerized, automated deployment with safe rollback
* Clean separation of responsibilities

---

# Future Improvements

* Email verification
* Password reset workflow
* Account lockout / suspicious-login detection
* MFA / TOTP
* Redis-backed distributed rate limiting
* Distributed tracing with OpenTelemetry
* Prometheus + Grafana dashboards
* Kubernetes deployment
* Secret management with AWS Secrets Manager
* Asymmetric JWT signing (RS256 / JWKS) for zero-trust verification across services

---

# Versioning

The project follows [Semantic Versioning](https://semver.org/). Releases are tagged `vX.Y.Z` on `main` and described in the [CHANGELOG](CHANGELOG.md). Commit messages follow [Conventional Commits](https://www.conventionalcommits.org/).

---

# Author

**Gaurav Lodhi**

Backend / Software Engineer focused on Java, Spring Boot, distributed systems, databases, and production backend infrastructure.

---

## Project

If you find the project useful, consider giving it a ⭐.

Built with **Java 21 + Spring Boot + PostgreSQL + Redis + Kafka + OpenResty**.

Licensed under [MIT](LICENSE).
