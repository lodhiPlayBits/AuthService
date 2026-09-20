# Redis Production Deployment Checklist

## ✅ Pre-Deployment Checklist

### 1. Environment Setup
- [ ] `.env` file created from `.env.example`
- [ ] Strong password generated (32+ characters)
- [ ] Password set in `redis/.env`
- [ ] Same password set in `auth_service/.env`
- [ ] File permissions secured: `chmod 600 .env`

### 2. Network Setup
- [ ] Docker network `authnet` exists
  ```bash
  docker network create authnet
  ```

### 3. Directory Structure
- [ ] `data/` directory exists
- [ ] `backups/` directory exists
- [ ] `logs/` directory exists
- [ ] Proper permissions set (755)

### 4. Configuration Review
- [ ] `redis.conf` reviewed and customized if needed
- [ ] `maxmemory` appropriate for your server
- [ ] Dangerous commands disabled
- [ ] AOF and RDB persistence enabled

## 🚀 Deployment Steps

### Quick Deployment (Automated)

```bash
cd /home/ubuntu/backend/AuthService/redis
./setup.sh
```

The setup script will:
- ✅ Create `.env` with auto-generated password
- ✅ Set proper permissions
- ✅ Create Docker network if needed
- ✅ Build and start Redis
- ✅ Verify connection

### Manual Deployment

```bash
# 1. Navigate to redis directory
cd /home/ubuntu/backend/AuthService/redis

# 2. Create environment file
cp .env.example .env

# 3. Generate strong password
openssl rand -base64 32

# 4. Edit .env and set REDIS_PASSWORD
nano .env

# 5. Secure permissions
chmod 600 .env

# 6. Create directories
mkdir -p data backups logs

# 7. Create network (if not exists)
docker network create authnet || true

# 8. Build image
docker compose build

# 9. Start Redis
docker compose up -d

# 10. Verify
docker ps | grep redis-service
docker exec redis-service redis-cli -a "$(grep REDIS_PASSWORD .env | cut -d= -f2)" ping
```

## ✅ Post-Deployment Verification

### 1. Container Status
```bash
# Check if container is running
docker ps | grep redis-service

# Expected output:
# CONTAINER ID   IMAGE          ... STATUS                   PORTS
# xxxxxxxxxxxx   redis:7-alpine ... Up X minutes (healthy)   127.0.0.1:6379->6379/tcp
```

### 2. Health Check
```bash
# Check health status
docker inspect --format='{{.State.Health.Status}}' redis-service

# Expected output: healthy
```

### 3. Connection Test
```bash
# Ping Redis
source .env
docker exec redis-service redis-cli -a "${REDIS_PASSWORD}" --no-auth-warning ping

# Expected output: PONG
```

### 4. Configuration Verification
```bash
# Verify password is set
docker exec redis-service redis-cli -a "${REDIS_PASSWORD}" --no-auth-warning CONFIG GET requirepass

# Verify maxmemory
docker exec redis-service redis-cli -a "${REDIS_PASSWORD}" --no-auth-warning CONFIG GET maxmemory

# Verify AOF is enabled
docker exec redis-service redis-cli -a "${REDIS_PASSWORD}" --no-auth-warning CONFIG GET appendonly
```

### 5. Logs Check
```bash
# Check for any errors
docker logs redis-service --tail 50

# Expected: No error messages
```

### 6. Network Connectivity
```bash
# Verify container is on authnet
docker network inspect authnet | grep redis-service

# Test from another container
docker run --rm --network authnet redis:7-alpine \
  redis-cli -h redis-service -a "${REDIS_PASSWORD}" --no-auth-warning ping
```

### 7. Resource Usage
```bash
# Check CPU and memory usage
docker stats redis-service --no-stream

# Check Redis memory usage
docker exec redis-service redis-cli -a "${REDIS_PASSWORD}" --no-auth-warning INFO memory
```

### 8. Persistence Check
```bash
# Verify RDB file
docker exec redis-service ls -lh /data/dump.rdb

# Verify AOF file
docker exec redis-service ls -lh /data/appendonly.aof

# Check last save time
docker exec redis-service redis-cli -a "${REDIS_PASSWORD}" --no-auth-warning LASTSAVE
```

## 🔗 Application Integration Verification

### 1. Update Application Configuration
```bash
# Ensure auth_service/.env has matching password
cd /home/ubuntu/backend/AuthService/auth_service
grep REDIS_PASSWORD .env

# Should match redis/.env REDIS_PASSWORD
```

### 2. Test Application Connection
```bash
# Start application
cd /home/ubuntu/backend/AuthService/auth_service
mvn spring-boot:run

# Check logs for:
# - "Started AuthApplication in X seconds" (success)
# - No Redis connection errors
```

### 3. Verify Spring Boot Actuator
```bash
# Check health endpoint
curl http://localhost:8080/actuator/health

# Should show Redis status if configured
```

## 🔒 Security Verification

### 1. Password Protection
```bash
# Try connecting without password (should fail)
docker exec redis-service redis-cli ping

# Expected: (error) NOAUTH Authentication required.
```

### 2. Command Protection
```bash
# Try dangerous commands (should fail)
docker exec redis-service redis-cli -a "${REDIS_PASSWORD}" --no-auth-warning FLUSHALL

# Expected: (error) ERR unknown command
```

### 3. File Permissions
```bash
# Check .env permissions
ls -la .env

# Expected: -rw------- (600)
```

### 4. Port Binding
```bash
# Check port is bound to localhost only
docker port redis-service

# Expected: 6379/tcp -> 127.0.0.1:6379
```

## 📊 Monitoring Setup

### 1. Basic Monitoring Script
```bash
# Create monitoring cron job
crontab -e

# Add (checks every 5 minutes):
*/5 * * * * docker exec redis-service redis-cli -a "PASSWORD" --no-auth-warning ping > /dev/null 2>&1 || echo "Redis is down!" | mail -s "Redis Alert" admin@example.com
```

### 2. Log Monitoring
```bash
# Monitor logs in real-time
docker logs -f redis-service

# Or check for errors
docker logs redis-service 2>&1 | grep -i error
```

### 3. Performance Monitoring
```bash
# Watch stats
watch -n 5 'docker exec redis-service redis-cli -a "${REDIS_PASSWORD}" --no-auth-warning INFO stats'

# Monitor slow queries
docker exec redis-service redis-cli -a "${REDIS_PASSWORD}" --no-auth-warning SLOWLOG GET 10
```

## 💾 Backup Verification

### 1. Test Manual Backup
```bash
# Trigger backup
docker exec redis-service redis-cli -a "${REDIS_PASSWORD}" --no-auth-warning BGSAVE

# Wait a few seconds, then copy
docker cp redis-service:/data/dump.rdb ./backups/test_backup.rdb

# Verify backup file exists
ls -lh ./backups/test_backup.rdb
```

### 2. Setup Automated Backups
```bash
# Create backup script in crontab
crontab -e

# Add daily backup at 2 AM:
0 2 * * * cd /home/ubuntu/backend/AuthService/redis && docker exec redis-service redis-cli -a "$(grep REDIS_PASSWORD .env | cut -d= -f2)" --no-auth-warning BGSAVE && sleep 10 && docker cp redis-service:/data/dump.rdb ./backups/backup_$(date +\%Y\%m\%d).rdb && find ./backups -name "backup_*.rdb" -mtime +7 -delete
```

## 🔄 Common Operations

### Restart Redis
```bash
docker compose restart
```

### Stop Redis
```bash
docker compose down
```

### View Logs
```bash
docker logs -f redis-service
```

### Access CLI
```bash
source .env
docker exec -it redis-service redis-cli -a "${REDIS_PASSWORD}"
```

### Check Database Size
```bash
docker exec redis-service redis-cli -a "${REDIS_PASSWORD}" --no-auth-warning DBSIZE
```

### Flush Database (CAREFUL!)
```bash
# This is disabled by default for safety
# If you really need to flush, you must restore the command first
```

## 🆘 Troubleshooting

### Container Won't Start
```bash
# Check logs
docker logs redis-service

# Validate config
docker run --rm -v $(pwd)/redis.conf:/redis.conf redis:7-alpine \
  redis-server /redis.conf --test-config

# Check permissions
ls -la data/ backups/ logs/
```

### Connection Refused
```bash
# Verify container is running
docker ps | grep redis-service

# Check network
docker network inspect authnet

# Test connectivity
docker exec redis-service redis-cli -a "${REDIS_PASSWORD}" --no-auth-warning ping
```

### Authentication Errors
```bash
# Verify password in .env
cat .env | grep REDIS_PASSWORD

# Test with explicit password
docker exec redis-service redis-cli -a "YOUR_PASSWORD" ping
```

### High Memory Usage
```bash
# Check memory stats
docker exec redis-service redis-cli -a "${REDIS_PASSWORD}" --no-auth-warning INFO memory

# Check key count
docker exec redis-service redis-cli -a "${REDIS_PASSWORD}" --no-auth-warning DBSIZE

# Review maxmemory setting
docker exec redis-service redis-cli -a "${REDIS_PASSWORD}" --no-auth-warning CONFIG GET maxmemory
```

## 📝 Deployment Notes

### Important Reminders
- ✅ Never commit `.env` to version control
- ✅ Use strong passwords (32+ characters)
- ✅ Keep passwords in sync between redis and auth_service
- ✅ Monitor memory usage regularly
- ✅ Set up automated backups
- ✅ Review logs periodically
- ✅ Test disaster recovery procedures

### Production Best Practices
- Set up monitoring and alerting
- Configure log aggregation
- Implement backup rotation policy
- Document recovery procedures
- Plan for scaling (sentinel/cluster)
- Regular security audits
- Performance baseline testing

## ✅ Deployment Complete

Once all checks pass, your Redis deployment is ready for production use!

For detailed usage examples, see: `REDIS_USAGE.md`
For operational guides, see: `README.md`
For quick reference, see: `QUICKSTART.md`

---

**Last Updated**: Deploy with confidence! 🚀
