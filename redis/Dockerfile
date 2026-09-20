# =============================================================================
# Redis Production Dockerfile
# =============================================================================
# Production-grade Redis container with security hardening
# =============================================================================

FROM redis:7-alpine

# Install required utilities
RUN apk add --no-cache \
    bash \
    tzdata \
    ca-certificates \
    && rm -rf /var/cache/apk/*

# Create necessary directories with proper permissions
RUN mkdir -p /data /var/log/redis /backups && \
    chown -R redis:redis /data /var/log/redis /backups && \
    chmod 755 /data /var/log/redis /backups

# Health check script
RUN echo '#!/bin/sh' > /usr/local/bin/healthcheck.sh && \
    echo 'redis-cli -a "${REDIS_PASSWORD}" --no-auth-warning ping | grep -q PONG' >> /usr/local/bin/healthcheck.sh && \
    chmod +x /usr/local/bin/healthcheck.sh

# Switch to redis user
USER redis

# Expose Redis port
EXPOSE 6379

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=30s --retries=3 \
    CMD /usr/local/bin/healthcheck.sh

# Default command (will be overridden by docker-compose)
CMD ["redis-server"]
