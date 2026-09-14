# Environment Setup - Quick Reference

## Overview

One `application.yml` + one `.env` (local) + one `.env.prod` (production).

**Key Principle:** Only `DB_HOST` changes between environments.

## Files

1. **`application.yml`** - Single config file, constructs URL from `${DB_HOST}:${DB_PORT}/${DB_NAME}`
2. **`.env`** - Local development settings (verbose logging)
3. **`.env.prod`** - Production settings (minimal logging, secure cookies)
4. **`.env.example`** - Template
5. **`docker-compose.yml`** - Works with `.env` or `--env-file .env.prod`

## Main Changes

- DB URL: Now built from `DB_HOST + DB_PORT + DB_NAME` instead of full `DB_URL`
- JPA: `ddl-auto: none` (was `validate`)
- Flyway: `baseline-on-migrate: false` (was `true`)
- JWT cookies: Nested under `cookie:` section

## Environment Comparison

| Environment | DB_HOST | File | Command |
|------------|---------|------|---------|
| **Local Direct** | `localhost` | `.env` | `./mvnw spring-boot:run` |
| **Local Docker** | `host.docker.internal` | `.env` | `docker compose up -d` |
| **Production** | `YOUR_LAPTOP_IP` | `.env.prod` | `docker compose --env-file .env.prod up -d` |

**Note:** For Docker, edit `.env` to set `DB_HOST=host.docker.internal` before running.

## Usage

### 1. Local - Spring Boot Direct
```bash
# Ensure .env has DB_HOST=localhost
./mvnw spring-boot:run
```

### 2. Local - Docker
```bash
# Edit .env: Set DB_HOST=host.docker.internal
docker compose up -d --build
docker compose logs -f auth-service
```

### 3. Production - AWS EC2
```bash
# Use .env.prod: Set DB_HOST=YOUR_LAPTOP_PUBLIC_IP
docker compose --env-file .env.prod up -d --build
docker compose logs -f auth-service
```

## PostgreSQL Setup

Edit `/etc/postgresql/*/main/pg_hba.conf`:
```
# For local Docker
host auth_service postgres 172.20.0.0/16 scram-sha-256

# For AWS EC2 (add when deploying)
host auth_service postgres AWS_EC2_IP/32 scram-sha-256
```

Edit `/etc/postgresql/*/main/postgresql.conf`:
```
listen_addresses = '*'
```

Restart: `sudo service postgresql restart`

## Production Checklist

- [ ] Create `.env.prod` from `.env.example`
- [ ] Update `DB_HOST` in `.env.prod` (your laptop's public IP or VPN IP)
- [ ] Generate new `JWT_SECRET`: `openssl rand -base64 64`
- [ ] Set strong `DB_PASSWORD`
- [ ] Verify `JWT_COOKIE_SECURE=true`
- [ ] Add AWS EC2 IP to `pg_hba.conf`
- [ ] Test connection: `telnet YOUR_LAPTOP_IP 5432` from AWS
- [ ] Never commit `.env` or `.env.prod` to git

## Troubleshooting

**Connection refused:**
- Check: `sudo service postgresql status`
- Verify `pg_hba.conf` and `postgresql.conf`
- Restart: `sudo service postgresql restart`

**Docker can't reach PostgreSQL:**
- Verify `extra_hosts` in `docker-compose.yml`
- Test: `docker exec -it auth-service bash` then `apt update && apt install -y postgresql-client` then `psql -h host.docker.internal -U postgres`
