# Redis Quick Start Guide

## 🚀 First Time Setup

```bash
# 1. Run setup script (auto-generates password)
./setup.sh

# 2. Note the generated password or check .env file
cat .env | grep REDIS_PASSWORD
```

## 📦 Manual Setup

```bash
# 1. Copy environment file
cp .env.example .env

# 2. Generate password
openssl rand -base64 32

# 3. Edit .env and set REDIS_PASSWORD
nano .env

# 4. Create authnet network (if not exists)
docker network create authnet

# 5. Start Redis
docker compose up -d
```

## ✅ Verify Installation

```bash
# Check container status
docker ps | grep redis-service

# Test connection
source .env
docker exec redis-service redis-cli -a "${REDIS_PASSWORD}" ping
# Expected: PONG

# Check health
docker inspect --format='{{.State.Health.Status}}' redis-service
# Expected: healthy
```

## 🔧 Common Commands

### Container Management
```bash
# Start
docker compose up -d

# Stop
docker compose down

# Restart
docker compose restart

# View logs
docker logs -f redis-service

# Shell access
docker exec -it redis-service sh
```

### Redis CLI
```bash
# Connect to Redis CLI
source .env
docker exec -it redis-service redis-cli -a "${REDIS_PASSWORD}"

# Quick commands
docker exec redis-service redis-cli -a "${REDIS_PASSWORD}" INFO
docker exec redis-service redis-cli -a "${REDIS_PASSWORD}" DBSIZE
docker exec redis-service redis-cli -a "${REDIS_PASSWORD}" PING
```

### Monitoring
```bash
# Memory usage
docker exec redis-service redis-cli -a "${REDIS_PASSWORD}" INFO memory

# Stats
docker exec redis-service redis-cli -a "${REDIS_PASSWORD}" INFO stats

# Connected clients
docker exec redis-service redis-cli -a "${REDIS_PASSWORD}" CLIENT LIST

# Slow queries
docker exec redis-service redis-cli -a "${REDIS_PASSWORD}" SLOWLOG GET 10
```

### Backup
```bash
# Manual backup
docker exec redis-service redis-cli -a "${REDIS_PASSWORD}" BGSAVE

# Copy backup file
docker cp redis-service:/data/dump.rdb ./backups/backup_$(date +%Y%m%d).rdb
```

## 🔗 Spring Boot Integration

### 1. Add dependency (already in pom.xml)
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

### 2. Configure application.yml
```yaml
spring:
  data:
    redis:
      host: redis-service
      port: 6379
      password: ${REDIS_PASSWORD}
```

### 3. Use in code
```java
@Autowired
private StringRedisTemplate redisTemplate;

// Set value
redisTemplate.opsForValue().set("key", "value", Duration.ofMinutes(10));

// Get value
String value = redisTemplate.opsForValue().get("key");
```

## 🆘 Troubleshooting

### Container won't start
```bash
# Check logs
docker logs redis-service

# Validate config
docker run --rm -v $(pwd)/redis.conf:/redis.conf redis:7-alpine \
  redis-server /redis.conf --test-config
```

### Authentication error
```bash
# Verify password
cat .env | grep REDIS_PASSWORD

# Test with explicit password
docker exec redis-service redis-cli -a "your_password_here" ping
```

### Connection refused from app
```bash
# Verify app is on authnet network
docker network inspect authnet

# Test connection from app container
docker exec your-app-container ping redis-service
```

## 📚 More Information

See [README.md](README.md) for complete documentation including:
- Security best practices
- Performance tuning
- Backup & recovery procedures
- Monitoring guide
- Troubleshooting

## 🔒 Security Reminders

- ✅ Never commit `.env` to git
- ✅ Use strong passwords (32+ characters)
- ✅ Secure `.env` permissions: `chmod 600 .env`
- ✅ Don't expose Redis port to internet
- ✅ Use Docker network for container communication
