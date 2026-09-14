# Auth Service

Spring Boot authentication service with JWT, RBAC, and PostgreSQL.

## Prerequisites

- Java 21+
- Maven 3.9+
- PostgreSQL 16+
- Docker

## Setup

1. **Start PostgreSQL**
   ```bash
   sudo service postgresql start
   ```

2. **Create database**
   ```bash
   psql -U postgres -c "CREATE DATABASE auth_service;"
   ```

3. **Generate new JWT secret** (current one was exposed)
   ```bash
   openssl rand -base64 64
   ```
   
4. **Update .env**
   ```bash
   JWT_SECRET=<paste-your-new-secret-here>
   ```

## Run

**Local (without Docker):**
```bash
./run.sh
```

**Docker:**
```bash
docker compose up --build
```

**If Flyway checksum error:**
```bash
# Reset database (dev only)
psql -U postgres -c "DROP DATABASE IF EXISTS auth_service; CREATE DATABASE auth_service;"
./run.sh
```

Simple! Local uses `localhost`, Docker uses `host.docker.internal`.

## Endpoints

- Health: `http://localhost:8080/actuator/health`
- Auth: `http://localhost:8080/api/v1/auth/*`
- Admin: `http://localhost:8080/api/v1/admin/*`

## Troubleshooting

**Flyway checksum mismatch?**
```bash
docker compose down
psql -U postgres -c "DROP DATABASE auth_service; CREATE DATABASE auth_service;"
./run.sh
```

**Can't connect to DB?**
```bash
sudo service postgresql status
psql -U postgres -d auth_service
```

## Important

- **Never commit `.env`** - In `.gitignore`
- **Generate new JWT secret** - Current one was exposed
- **Setup**:
  - Local: `.env` has `localhost` → `./run.sh`
  - Docker: docker-compose overrides to `host.docker.internal` → `docker compose up`

## License

[Your License]
