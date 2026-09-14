# Quick Start Guide

## Prerequisites
- PostgreSQL running on WSL: `sudo service postgresql start`
- Database created: `CREATE DATABASE auth_service;`

## Run Locally

### Option 1: Direct Spring Boot
```bash
# Ensure .env has DB_HOST=localhost
./mvnw spring-boot:run
```

### Option 2: Docker
```bash
# Edit .env: Set DB_HOST=host.docker.internal
docker compose up -d --build
docker compose logs -f
```

## Deploy to Production (AWS EC2)

1. **Prepare .env.prod**
   ```bash
   cp .env.example .env.prod
   # Edit .env.prod:
   # - DB_HOST=YOUR_LAPTOP_PUBLIC_IP
   # - JWT_SECRET=$(openssl rand -base64 64)
   # - DB_PASSWORD=strong_password
   # - JWT_COOKIE_SECURE=true
   ```

2. **Configure PostgreSQL Access**
   ```bash
   # On laptop, edit /etc/postgresql/*/main/pg_hba.conf
   host auth_service postgres AWS_EC2_IP/32 scram-sha-256
   
   # Restart PostgreSQL
   sudo service postgresql restart
   ```

3. **Deploy**
   ```bash
   # On AWS EC2
   docker compose --env-file .env.prod up -d --build
   docker compose logs -f
   ```

## Verify
```bash
curl http://localhost:8080/actuator/health
```

## Environments

| Mode | DB_HOST | Command |
|------|---------|---------|
| Local Direct | `localhost` | `./mvnw spring-boot:run` |
| Local Docker | `host.docker.internal` | `docker compose up -d` |
| Production | `YOUR_LAPTOP_IP` | `docker compose --env-file .env.prod up -d` |

---

See **SETUP-SUMMARY.md** for complete details.
