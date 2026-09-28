# Redis Service - Production Configuration

Production-grade Redis cache service for the AuthService backend with enterprise security, performance tuning, and operational best practices.

## 📋 Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Prerequisites](#prerequisites)
- [Quick Start](#quick-start)
- [Configuration](#configuration)
- [Security](#security)
- [Monitoring](#monitoring)
- [Backup & Recovery](#backup--recovery)
- [Performance Tuning](#performance-tuning)
- [Troubleshooting](#troubleshooting)

## 🎯 Overview

This Redis setup provides:
- **Security**: Password authentication, command renaming, network isolation
- **Persistence**: Both RDB snapshots and AOF (Append-Only File)
- **High Performance**: Optimized memory management and lazy freeing
- **Production Ready**: Health checks, resource limits, logging
- **Docker Integration**: Runs on shared `authnet` network

## ✨ Features

### Security Features
- ✅ Strong password authentication required
- ✅ Dangerous commands disabled/renamed (FLUSHDB, FLUSHALL, KEYS, CONFIG, DEBUG)
- ✅ Protected mode enabled
- ✅ Non-root container execution (UID 999)
- ✅ Read-only root filesystem
- ✅ Resource limits (CPU & Memory)
- ✅ Network isolation via Docker network

### Performance Features
- ✅ Optimized memory management (512MB default with LRU eviction)
- ✅ Lazy freeing for better performance
- ✅ Active defragmentation
- ✅ Client output buffer limits
- ✅ TCP keepalive for connection health

### Reliability Features
- ✅ Dual persistence (RDB + AOF)
- ✅ Automatic health checks
- ✅ Graceful restart policy
- ✅ Structured logging (JSON format)
- ✅ Slow query logging

## 📦 Prerequisites

- Docker 20.10+
- Docker Compose 2.0+
- `authnet` Docker network created:
  ```bash
  docker network create authnet
  ```

## 🚀 Quick Start

### 1. Environment Setup

```bash
# Copy environment template
cp .env.example .env

# Generate a strong password
openssl rand -base64 32

# Edit .env and set REDIS_PASSWORD
nano .env
```

### 2. Create Required Directories

```bash
mkdir -p data backups logs
chmod 755 data backups logs
```

### 3. Start Redis

```bash
# Using Docker Compose
docker compose up -d

# Or build and run manually
docker build -t redis-custom .
docker run -d --name redis-service \
  --network authnet \
  --env-file .env \
  -v $(pwd)/data:/data \
  redis-custom
```

### 4. Verify Installation

```bash
# Check container status
docker ps | grep redis-service

# Test connection
docker exec redis-service redis-cli -a "${REDIS_PASSWORD}" ping
# Expected output: PONG

# Check health
docker inspect --format='{{.State.Health.Status}}' redis-service
# Expected output: healthy
```

## ⚙️ Configuration

### Directory Structure

```
redis/
├── Dockerfile              # Production-optimized Redis image
├── docker-compose.yml      # Container orchestration
├── redis.conf             # Redis server configuration
├── .env                   # Environment variables (DO NOT COMMIT)
├── .env.example           # Environment template
├── .gitignore             # Git exclusions
├── README.md              # This file
├── data/                  # Redis data (auto-created)
├── backups/               # Backup storage (auto-created)
└── logs/                  # Application logs (auto-created)
```

### Key Configuration Files

#### `.env` - Environment Variables
Contains sensitive credentials and configuration:
- `REDIS_PASSWORD`: Authentication password (required)
- `REDIS_MAX_MEMORY`: Maximum memory allocation
- Resource limits and tuning parameters

**⚠️ CRITICAL**: Never commit `.env` to version control!

#### `redis.conf` - Redis Server Configuration
Production-optimized settings:
- Memory: 512MB max with LRU eviction
- Persistence: RDB + AOF enabled
- Security: Password auth + dangerous commands disabled
- Performance: Lazy freeing + active defragmentation

#### `docker-compose.yml` - Container Configuration
- Health checks every 30 seconds
- Resource limits (1 CPU core, 1GB RAM)
- Security options (no-new-privileges, read-only root)
- Logging (JSON format, 10MB max per file)

## 🔒 Security

### Authentication

Redis requires password authentication:

```bash
# Connect with password
docker exec -it redis-service redis-cli -a "${REDIS_PASSWORD}"

# Or set in environment
export REDIS_PASSWORD="your_password_here"
docker exec -it redis-service redis-cli -a "${REDIS_PASSWORD}"
```

### Disabled/Renamed Commands

For security, dangerous commands are protected:

| Command     | Status                      | Risk Level |
|-------------|----------------------------|------------|
| FLUSHDB     | ❌ Disabled                | Critical   |
| FLUSHALL    | ❌ Disabled                | Critical   |
| KEYS        | ❌ Disabled                | High       |
| CONFIG      | 🔒 Renamed with password   | High       |
| DEBUG       | ❌ Disabled                | High       |
| SHUTDOWN    | 🔒 Renamed with password   | Critical   |

### Network Security

- Redis port bound to `127.0.0.1` only (not exposed to host)
- Communication only via Docker `authnet` network
- Protected mode enabled

### File Permissions

```bash
# Secure environment file
chmod 600 .env

# Verify permissions
ls -la .env
# Should show: -rw------- (600)
```

## 📊 Monitoring

### Health Checks

```bash
# Container health status
docker inspect --format='{{.State.Health.Status}}' redis-service

# Detailed health log
docker inspect --format='{{json .State.Health}}' redis-service | jq
```

### Performance Metrics

```bash
# Memory usage
docker exec redis-service redis-cli -a "${REDIS_PASSWORD}" INFO memory

# Statistics
docker exec redis-service redis-cli -a "${REDIS_PASSWORD}" INFO stats

# Client connections
docker exec redis-service redis-cli -a "${REDIS_PASSWORD}" INFO clients

# Slow queries
docker exec redis-service redis-cli -a "${REDIS_PASSWORD}" SLOWLOG GET 10

# Database size
docker exec redis-service redis-cli -a "${REDIS_PASSWORD}" DBSIZE
```

### Real-time Monitoring

```bash
# Monitor all commands (be careful in production)
docker exec -it redis-service redis-cli -a "${REDIS_PASSWORD}" MONITOR

# Watch stats
watch -n 1 'docker exec redis-service redis-cli -a "${REDIS_PASSWORD}" INFO stats'
```

### Logs

```bash
# View container logs
docker logs redis-service

# Follow logs
docker logs -f redis-service

# Last 100 lines
docker logs --tail 100 redis-service

# With timestamps
docker logs -t redis-service
```

## 💾 Backup & Recovery

### Persistence Strategy

Redis uses **dual persistence** for data durability:

1. **RDB (Snapshotting)**
   - Point-in-time snapshots
   - Faster restarts
   - Scheduled: 900s/1 change, 300s/10 changes, 60s/10000 changes

2. **AOF (Append-Only File)**
   - Every operation logged
   - Higher durability
   - fsync every second

### Manual Backup

```bash
# Trigger immediate snapshot
docker exec redis-service redis-cli -a "${REDIS_PASSWORD}" BGSAVE

# Check last save time
docker exec redis-service redis-cli -a "${REDIS_PASSWORD}" LASTSAVE

# Copy data files
docker cp redis-service:/data/dump.rdb ./backups/dump_$(date +%Y%m%d_%H%M%S).rdb
docker cp redis-service:/data/appendonly.aof ./backups/appendonly_$(date +%Y%m%d_%H%M%S).aof
```

### Automated Backups

Set up a cron job:

```bash
# Edit crontab
crontab -e

# Add daily backup at 2 AM
0 2 * * * cd /path/to/redis && docker exec redis-service redis-cli -a "$REDIS_PASSWORD" BGSAVE && docker cp redis-service:/data/dump.rdb ./backups/dump_$(date +\%Y\%m\%d).rdb
```

### Restore from Backup

```bash
# Stop Redis
docker compose down

# Replace data files
cp ./backups/dump_YYYYMMDD.rdb ./data/dump.rdb
cp ./backups/appendonly_YYYYMMDD.aof ./data/appendonly.aof

# Ensure correct permissions
chmod 644 ./data/dump.rdb ./data/appendonly.aof

# Start Redis
docker compose up -d

# Verify data
docker exec redis-service redis-cli -a "${REDIS_PASSWORD}" DBSIZE
```

## ⚡ Performance Tuning

### Memory Optimization

```bash
# Check current memory usage
docker exec redis-service redis-cli -a "${REDIS_PASSWORD}" INFO memory | grep used_memory_human

# Check eviction stats
docker exec redis-service redis-cli -a "${REDIS_PASSWORD}" INFO stats | grep evicted

# Monitor fragmentation
docker exec redis-service redis-cli -a "${REDIS_PASSWORD}" INFO memory | grep mem_fragmentation_ratio
```

### Configuration Tuning

Adjust `redis.conf` for your workload:

```properties
# For high-throughput, low-latency:
maxmemory 1gb
maxmemory-policy allkeys-lru
lazyfree-lazy-eviction yes

# For data durability:
appendonly yes
appendfsync everysec
save 900 1
save 300 10

# For memory-constrained:
maxmemory 256mb
maxmemory-policy volatile-lru
activedefrag yes
```

### Benchmark

```bash
# Run performance benchmark
docker exec redis-service redis-benchmark -a "${REDIS_PASSWORD}" -q -n 100000

# Specific operations
docker exec redis-service redis-benchmark -a "${REDIS_PASSWORD}" -t set,get -n 100000 -q

# Latency test
docker exec redis-service redis-cli -a "${REDIS_PASSWORD}" --latency
```

## 🔧 Troubleshooting

### Common Issues

#### 1. Container Won't Start

```bash
# Check logs
docker logs redis-service

# Common causes:
# - Invalid redis.conf syntax
# - Missing .env file
# - Port already in use
# - Insufficient permissions on data directory

# Validate configuration
docker run --rm -v $(pwd)/redis.conf:/redis.conf redis:7-alpine redis-server /redis.conf --test-config
```

#### 2. Authentication Failures

```bash
# Verify password is set
docker exec redis-service redis-cli CONFIG GET requirepass

# Check environment variable
docker exec redis-service env | grep REDIS_PASSWORD

# Test with explicit password
docker exec redis-service redis-cli -a "your_password" ping
```

#### 3. Out of Memory

```bash
# Check memory usage
docker exec redis-service redis-cli -a "${REDIS_PASSWORD}" INFO memory

# Check eviction policy
docker exec redis-service redis-cli -a "${REDIS_PASSWORD}" CONFIG GET maxmemory-policy

# Flush expired keys
docker exec redis-service redis-cli -a "${REDIS_PASSWORD}" --scan --pattern '*' | xargs -L 1 redis-cli -a "${REDIS_PASSWORD}" DEL
```

#### 4. High Latency

```bash
# Check slow log
docker exec redis-service redis-cli -a "${REDIS_PASSWORD}" SLOWLOG GET 10

# Monitor latency
docker exec redis-service redis-cli -a "${REDIS_PASSWORD}" --latency-history

# Check CPU usage
docker stats redis-service

# Review persistence settings (AOF fsync may cause spikes)
```

#### 5. Connection Refused

```bash
# Check container is running
docker ps | grep redis-service

# Check network
docker network inspect authnet

# Verify service is listening
docker exec redis-service netstat -tlnp | grep 6379

# Test from another container on authnet
docker run --rm --network authnet redis:7-alpine redis-cli -h redis-service -p 6379 -a "${REDIS_PASSWORD}" ping
```

### Useful Commands

```bash
# Get all configuration
docker exec redis-service redis-cli -a "${REDIS_PASSWORD}" CONFIG GET '*'

# Get specific config
docker exec redis-service redis-cli -a "${REDIS_PASSWORD}" CONFIG GET maxmemory

# Set config at runtime (not persistent)
docker exec redis-service redis-cli -a "${REDIS_PASSWORD}" CONFIG SET maxmemory 1gb

# Get server info
docker exec redis-service redis-cli -a "${REDIS_PASSWORD}" INFO

# Check keyspace
docker exec redis-service redis-cli -a "${REDIS_PASSWORD}" INFO keyspace

# Debug memory usage
docker exec redis-service redis-cli -a "${REDIS_PASSWORD}" MEMORY STATS

# Check latency events
docker exec redis-service redis-cli -a "${REDIS_PASSWORD}" LATENCY DOCTOR
```

## 🔗 Integration with Spring Boot

### Application Configuration

Add to `application.properties` or `application.yml`:

```properties
# application.properties
spring.data.redis.host=redis-service
spring.data.redis.port=6379
spring.data.redis.password=${REDIS_PASSWORD}
spring.data.redis.timeout=60000
spring.data.redis.lettuce.pool.max-active=8
spring.data.redis.lettuce.pool.max-idle=8
spring.data.redis.lettuce.pool.min-idle=0
```

```yaml
# application.yml
spring:
  data:
    redis:
      host: redis-service
      port: 6379
      password: ${REDIS_PASSWORD}
      timeout: 60000
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 0
```

### Maven Dependency

Already added to `auth_service/pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

### Test Connection

```java
@SpringBootTest
class RedisConnectionTest {
    
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    @Test
    void testRedisConnection() {
        redisTemplate.opsForValue().set("test:key", "Hello Redis!");
        String value = redisTemplate.opsForValue().get("test:key");
        assertEquals("Hello Redis!", value);
    }
}
```

## 📚 Additional Resources

- [Redis Official Documentation](https://redis.io/documentation)
- [Redis Security Guide](https://redis.io/topics/security)
- [Redis Persistence](https://redis.io/topics/persistence)
- [Spring Data Redis](https://spring.io/projects/spring-data-redis)
- [Redis Best Practices](https://redis.io/topics/best-practices)

## 📄 License

This configuration is part of the AuthService backend project.

## 🤝 Support

For issues or questions:
1. Check the [Troubleshooting](#troubleshooting) section
2. Review container logs: `docker logs redis-service`
3. Contact the DevOps team

---

**⚠️ Security Reminder**: Never commit `.env` file or expose Redis port publicly in production!
