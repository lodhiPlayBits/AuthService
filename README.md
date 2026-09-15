# AuthService

AuthService is a production-grade authentication and authorization microservice designed to provide secure, robust identity management for distributed applications. It handles user registration, login, role-based access control (RBAC), and session management via JWT with refresh token rotation. Intended for enterprise environments, it ensures high performance and security through strict rate limiting, connection management, and comprehensive structured logging.

## Tech Stack

**Backend**
*   **Language:** Java 21
*   **Framework:** Spring Boot
*   **Authentication:** JWT (JSON Web Tokens) with refresh token rotation

**Database**
*   **Relational Database:** PostgreSQL (managed via AWS RDS)

**Gateway & Infrastructure**
*   **Reverse Proxy / API Gateway:** OpenResty (Nginx)
*   **Request Validation:** Lua-based scripting in OpenResty
*   **Security:** HTTPS enabled via Let's Encrypt
*   **Deployment:** AWS EC2 + RDS

## Key Features

*   **Secure Authentication Flow:** Robust JWT-based authentication featuring short-lived access tokens and secure refresh token rotation to mitigate token theft.
*   **Advanced Rate Limiting:** Granular, per-endpoint rate limiting implemented at the OpenResty layer. Strict limits are enforced on critical endpoints (e.g., login, register, refresh), while general API routes have more permissive thresholds.
*   **Role-Based Access Control (RBAC):** Comprehensive role and permission management system enabling fine-grained access control for administrative and user actions.
*   **User Management:** Full lifecycle management for user accounts.
*   **Resilience & Protection:** Connection limiting and Lua-based request validation prevent abuse and overload.
*   **Observability:** End-to-end correlation ID logging for seamless request tracing, coupled with structured exception handling for clear, actionable error reporting.

## Architecture

```text
+--------+       HTTPS      +--------------------+       HTTP        +-----------------+
|        | ---------------->|                    | ----------------> |                 |
| Client |                  | OpenResty (Nginx)  |                   |  AuthService    |
|        | <----------------| (Rate Limiting,    | <---------------- |  (Spring Boot)  |
+--------+                  |  Lua Validation)   |                   +-----------------+
                                                                             |
                                                                             | TCP
                                                                             v
                                                                     +-----------------+
                                                                     |                 |
                                                                     | PostgreSQL (RDS)|
                                                                     |                 |
                                                                     +-----------------+
```

## Prerequisites

*   Java 21 or higher
*   Docker & Docker Compose (for local development and infrastructure)
*   Maven or Gradle (depending on the build tool used)
*   PostgreSQL 15+ (if running locally without Docker)

## Setup & Run Instructions

### Environment Variables

The application requires several environment variables to be set. **Do not commit actual secrets to version control.**

*   `DB_HOST`
*   `DB_PORT`
*   `DB_NAME`
*   `DB_USER`
*   `DB_PASSWORD`
*   `JWT_SECRET`
*   `JWT_ACCESS_EXPIRATION`
*   `JWT_REFRESH_EXPIRATION`
*   `SPRING_PROFILES_ACTIVE`

### Running via Docker Compose

To spin up the entire stack locally (OpenResty, AuthService, and a local PostgreSQL instance):

1.  Clone the repository.
2.  Ensure your `.env` file is populated with appropriate local values.
3.  Run the following command from the project root:

```bash
docker-compose up --build -d
```

## API Endpoints

| Method | Path | Description | Auth Required |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/auth/register` | Register a new user | No |
| `POST` | `/api/v1/auth/login` | Authenticate user and return JWTs | No |
| `POST` | `/api/v1/auth/refresh` | Issue new access token using refresh token | Yes (Refresh) |
| `POST` | `/api/v1/auth/logout` | Invalidate user session/tokens | Yes |
| `GET` | `/api/v1/users/me` | Get current authenticated user details | Yes |
| `GET` | `/api/v1/admin/users` | List all users (paginated) | Yes (Admin) |
| `PUT` | `/api/v1/admin/users/{id}/roles`| Update user roles | Yes (Admin) |

## Testing

The project uses JUnit 5 and Testcontainers for integration testing.

To execute the test suite:

```bash
./mvnw clean test
```
*(or `./gradlew test` if using Gradle)*

Test coverage reports (e.g., JaCoCo) will be generated in `target/site/jacoco/index.html`.

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the LICENSE file for details.
