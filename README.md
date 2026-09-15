# AuthService

**Production-oriented authentication and authorization microservice built with Spring Boot, PostgreSQL, JWT and an OpenResty/Nginx gateway.**

AuthService provides secure user authentication, JWT access and refresh tokens, refresh-token rotation with token-family revocation, role-based access control (RBAC), fine-grained permissions, and gateway-level request protection.

The service is designed as a backend-focused system with security, database safety, observability, rate limiting, and production deployment in mind.

---

##  Features

### Authentication

* JWT-based authentication
* Short-lived access tokens
* Long-lived refresh tokens
* Refresh-token rotation
* Refresh-token reuse detection
* Refresh-token family revocation
* Secure HTTP-only refresh-token cookies
* Logout and logout-all functionality
* Password hashing using Spring Security
* Configurable token issuer and audience

### Authorization / RBAC

* Role-based access control
* Fine-grained permissions
* Permission-based method authorization using `@PreAuthorize`
* User ownership checks
* Admin-only operations
* Dynamic role creation
* Dynamic permission creation
* Assign/revoke permissions from roles
* Assign roles to users

Example permissions:

```text
admin:create
admin:read
admin:update
admin:delete

user:create
user:read
user:update
user:delete
```

### Gateway Security

Requests pass through an **OpenResty/Nginx gateway** before reaching the Spring Boot application.

* TLS termination
* Let's Encrypt certificates
* Self-signed certificate fallback for local development
* Lua-based request validation
* Per-route rate limiting
* Per-IP rate limiting
* Connection limiting
* Slow-loris protection
* Request filtering before reaching the JVM

Example rate-limit strategy:

```text
/auth/login       → strict
/auth/register    → strict
/auth/refresh     → strict
/general APIs     → relaxed
```

### Observability

* Correlation ID generation and propagation
* Structured request logging
* Spring Boot Actuator
* Configurable log levels
* Configurable log output
* Health endpoints

Every request can be traced through the gateway, filters, services, and database operations using a correlation ID.

---

# 🏗️ Architecture

```text
                         ┌──────────────────────────────────────┐
                         │          Client / Browser            │
                         └──────────────────┬───────────────────┘
                                            │
                                         HTTPS
                                            │
                                            ▼
                  ┌─────────────────────────────────────────────────┐
                  │              OpenResty / Nginx                  │
                  │                                                 │
                  │  • TLS termination                              │
                  │  • Lua request validation                       │
                  │  • Per-route rate limiting                      │
                  │  • Connection limiting                          │
                  │  • Request filtering                            │
                  └──────────────────────┬──────────────────────────┘
                                         │
                                  Internal HTTP
                                         │
                                         ▼
              ┌──────────────────────────────────────────────────────┐
              │                 Spring Boot API                      │
              │                                                      │
              │  Filters                                             │
              │   ├── JwtAuthenticationFilter                        │
              │   └── LoggingFilter                                  │
              │                                                      │
              │  Controllers                                         │
              │   ├── AuthController                                 │
              │   ├── UserController                                 │
              │   └── AdminController                                │
              │                                                      │
              │  Services                                            │
              │   ├── AuthService                                    │
              │   ├── UserService                                    │
              │   ├── RoleService                                    │
              │   ├── PermissionService                              │
              │   └── RefreshTokenService                            │
              │                                                      │
              │  Security                                            │
              │   ├── JwtService                                     │
              │   ├── CookieService                                  │
              │   └── CustomUserDetailsService                       │
              │                                                      │
              │  Repositories                                        │
              │   ├── UserRepository                                 │
              │   ├── TokenRepository                                │
              │   └── ...                                            │
              └──────────────────────┬───────────────────────────────┘
                                     │
                                    JDBC
                                     │
                                     ▼
                         ┌────────────────────────┐
                         │      PostgreSQL        │
                         │         / RDS          │
                         └────────────────────────┘
```

---

# Authentication Flow

## Login

```text
Client
   │
   │ POST /api/v1/auth/login
   ▼
OpenResty
   │
   │ Validate + Rate Limit
   ▼
Spring Boot
   │
   ├── Validate credentials
   ├── Generate access token
   ├── Generate refresh token
   └── Store refresh-token metadata
   │
   ▼
Client
   │
   ├── Access Token
   └── HttpOnly Refresh Cookie
```

## Refresh Token Rotation

```text
Refresh Token #1
       │
       ▼
Validate token
       │
       ├── Valid → revoke #1
       │
       └── Generate #2
                │
                ▼
        New Access Token
        New Refresh Token
```

If a previously-used refresh token is detected:

```text
Compromised Refresh Token
          │
          ▼
Reuse detected
          │
          ▼
Revoke entire token family
          │
          ▼
All tokens in chain become invalid
```

This prevents an attacker from continuing to use a stolen refresh-token chain.

---

#  RBAC Architecture

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
          └── admin:delete
```

Authorization is enforced at the service/controller level using Spring Security.

Example:

```java
@PreAuthorize("hasAuthority('admin:create')")
public UserResponse createUser(...) {
    ...
}
```

Ownership checks are also supported.

For example:

```text
Admin
 └── Can update any user

Normal User
 └── Can update only their own account
```

---

#  Tech Stack

## Backend

* Java 21
* Spring Boot
* Spring Security
* Spring Data JPA
* Hibernate
* Maven

## Database

* PostgreSQL
* HikariCP
* Flyway

## Security

* JWT
* Access + Refresh Tokens
* Refresh Token Rotation
* Token Family Revocation
* HTTP-only Cookies
* RBAC
* Fine-grained Permissions

## Gateway

* OpenResty
* Nginx
* Lua
* Let's Encrypt
* TLS

## Infrastructure

* Docker
* Docker Compose
* AWS EC2
* AWS RDS
* Linux

## Testing

* JUnit 5
* Spring Boot Test
* Integration Testing
* JaCoCo

---

#  Project Structure

```text
AuthService/
│
├── auth_service/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   │   └── ...
│   │   │   └── resources/
│   │   │       ├── application.yml
│   │   │       └── db/
│   │   │           └── migration/
│   │   │
│   │   └── test/
│   │
│   ├── Dockerfile
│   ├── docker-compose.yml
│   ├── pom.xml
│   └── .env.example
│
├── nginx/
│   ├── nginx.conf
│   ├── lua/
│   └── Dockerfile
│
└── README.md
```

---

# API Endpoints

## Authentication

Base URL:

```text
/api/v1/auth
```

| Method | Endpoint      | Description                  | Authentication |
| ------ | ------------- | ---------------------------- | -------------- |
| `POST` | `/login`      | Authenticate user            | Public         |
| `POST` | `/register`   | Register user                | Public         |
| `POST` | `/refresh`    | Rotate refresh token         | Refresh cookie |
| `POST` | `/logout`     | Revoke current refresh token | Required       |
| `POST` | `/logout/all` | Revoke all user sessions     | Required       |

---

## Users

Base URL:

```text
/api/v1/users
```

| Method   | Endpoint                | Description        | Authorization              |
| -------- | ----------------------- | ------------------ | -------------------------- |
| `POST`   | `/create-user`          | Create user        | `admin:create`             |
| `GET`    | `/getByEmail`           | Find user by email | `admin:read` / `user:read` |
| `GET`    | `/{id}`                 | Get user           | Admin or owner             |
| `PUT`    | `/{id}`                 | Update user        | Admin or owner             |
| `DELETE` | `/{id}`                 | Delete user        | Admin or owner             |
| `POST`   | `/{id}/change-password` | Change password    | Owner                      |

---

## Administration

Base URL:

```text
/api/v1/admin
```

| Method   | Endpoint                      | Description        |
| -------- | ----------------------------- | ------------------ |
| `GET`    | `/roles`                      | List roles         |
| `POST`   | `/roles`                      | Create role        |
| `DELETE` | `/roles/{roleId}`             | Delete role        |
| `POST`   | `/roles/{roleId}/permissions` | Assign permissions |
| `DELETE` | `/roles/{roleId}/permissions` | Revoke permissions |
| `GET`    | `/permissions`                | List permissions   |
| `POST`   | `/permissions`                | Create permission  |
| `DELETE` | `/permissions/{permissionId}` | Delete permission  |
| `GET`    | `/users`                      | List users         |
| `PUT`    | `/users/{userId}/roles`       | Assign roles       |

---

#  Environment Configuration

Copy the example environment file:

```bash
cp .env.example .env
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
```

### Logging / Actuator

```text
LOG_LEVEL_*
ACTUATOR_ENDPOINTS
ACTUATOR_HEALTH_SHOW_DETAILS
LOG_FILE
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
* Docker
* Docker Compose
* PostgreSQL 15+ if running PostgreSQL outside Docker

Clone the repository:

```bash
git clone https://github.com/lodhiPlayBits/AuthService.git
cd AuthService/auth_service
```

Create your environment file:

```bash
cp .env.example .env
```

Configure the required values in `.env`.

Start the application:

```bash
docker compose up --build -d
```

Check running containers:

```bash
docker compose ps
```

View logs:

```bash
docker compose logs -f
```

---

# 🧪 Testing

Run the complete test suite:

```bash
./mvnw clean test
```

Generate the JaCoCo coverage report:

```bash
./mvnw clean test
```

Then open:

```text
target/site/jacoco/index.html
```

The test suite includes:

* Unit tests
* Spring application-context tests
* Integration tests
* Database-backed tests

---

# 🐳 Docker

Build and start the complete stack:

```bash
docker compose up --build -d
```

Stop the stack:

```bash
docker compose down
```

Rebuild after code changes:

```bash
docker compose up --build -d
```

The production architecture is designed around:

```text
Internet
   │
   ▼
OpenResty / Nginx
   │
   ▼
Spring Boot
   │
   ▼
PostgreSQL / RDS
```

---

# ☁️ Production Deployment

The service can be deployed using:

```text
AWS EC2
   │
   ├── OpenResty / Nginx
   │       └── TLS
   │       └── Rate Limiting
   │       └── Request Validation
   │
   └── Docker
           └── Spring Boot
                   │
                   ▼
               AWS RDS
               PostgreSQL
```

Production configuration should use environment variables for:

* Database credentials
* JWT signing secret
* Admin credentials
* Cookie configuration
* Connection pool configuration
* Logging configuration

Do **not** hardcode secrets into Java source code, Dockerfiles, Git history, or configuration files committed to the repository.

---

#  Security Considerations

This project intentionally places multiple security controls at different layers.

### Gateway

```text
TLS
 ↓
Request Validation
 ↓
Rate Limiting
 ↓
Connection Limiting
```

### Application

```text
Authentication
 ↓
JWT Validation
 ↓
Authorization
 ↓
Ownership Checks
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

No single security mechanism is expected to protect the entire system.

---

# Design Goals

The main goals of this project are:

* Secure authentication
* Fine-grained authorization
* Stateless access-token authentication
* Stateful refresh-token management
* Refresh-token compromise detection
* Defense-in-depth at the gateway and application layers
* Production-oriented database configuration
* Observable request flow
* Containerized deployment
* Clean separation of responsibilities

---

# Future Improvements

Potential improvements include:

* Redis-backed distributed rate limiting
* Redis session/token metadata
* Kafka-based authentication/security event streaming
* Email verification
* Password reset workflow
* Account lockout / suspicious-login detection
* MFA / TOTP
* OAuth2 / OpenID Connect integration
* Distributed tracing with OpenTelemetry
* Prometheus + Grafana monitoring
* Kubernetes deployment
* Automated CI/CD deployment pipeline
* Secret management through a dedicated secret-management system

---

# 📜 License

This project is licensed under the **MIT License**.

You are free to use, modify, distribute, and sublicense this software, subject to the terms of the license.

See the [`LICENSE`](LICENSE) file for the complete license text.

## MIT License

```text
MIT License

Copyright (c) 2026 Gaurav Lodhi

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

---

# 👤 Author

**Gaurav Lodhi**

Backend / Software Engineer focused on Java, Spring Boot, distributed systems, databases, and production backend infrastructure.

---

## ⭐ Project

If you find the project useful, consider giving it a star.

Built with **Java 21 + Spring Boot + PostgreSQL + OpenResty**.
