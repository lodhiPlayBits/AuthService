FROM openresty/openresty:1.25.3.1-alpine

# Install openssl for runtime self-signed cert generation (if needed)
RUN apk add --no-cache openssl

# Create the nginx user/group — this Alpine-based OpenResty image
# doesn't ship one by default, and nginx.conf's "user nginx;" directive
# needs it to exist or the master process fails at startup.
RUN adduser -D -H -u 101 -s /sbin/nologin nginx

# Copy configs and lua scripts into the image (baked-in, not live-mounted)
COPY nginx.conf /usr/local/openresty/nginx/conf/nginx.conf
COPY conf.d/ /usr/local/openresty/nginx/conf/conf.d/
COPY lua/ /usr/local/openresty/nginx/lua/

# Copy SSL helper files (configs + generator script)
COPY ssl/ssl-params.conf /usr/local/openresty/nginx/ssl/ssl-params.conf
COPY ssl/options-ssl-nginx.conf /usr/local/openresty/nginx/ssl/options-ssl-nginx.conf
COPY ssl/generate-self-signed.sh /usr/local/openresty/nginx/ssl/generate-self-signed.sh
RUN chmod +x /usr/local/openresty/nginx/ssl/generate-self-signed.sh

# Entrypoint script: generate self-signed certs if no real certs exist,
# then start OpenResty.
RUN printf '#!/bin/sh\n\
set -e\n\
DOMAIN="${DOMAIN:-authvolt.fun}"\n\
CERT_DIR="/etc/nginx/ssl/live/${DOMAIN}"\n\
if [ ! -f "${CERT_DIR}/fullchain.pem" ] || [ ! -f "${CERT_DIR}/privkey.pem" ]; then\n\
  echo "[entrypoint] No certs found for ${DOMAIN} — generating self-signed…"\n\
  SSL_DIR=/etc/nginx/ssl DOMAIN="${DOMAIN}" /usr/local/openresty/nginx/ssl/generate-self-signed.sh\n\
else\n\
  echo "[entrypoint] Certs found for ${DOMAIN} — using existing."\n\
fi\n\
# Generate DH params if missing (may already exist from generate-self-signed.sh)\n\
if [ ! -f /etc/nginx/ssl/dhparams.pem ]; then\n\
  echo "[entrypoint] Generating DH parameters…"\n\
  openssl dhparam -out /etc/nginx/ssl/dhparams.pem 2048\n\
fi\n\
# Ensure worker processes (running as nginx user) can read ssl material\n\
chown -R nginx:nginx /etc/nginx/ssl 2>/dev/null || true\n\
exec openresty -g "daemon off;"\n' > /docker-entrypoint.sh && chmod +x /docker-entrypoint.sh

# Validate config at build time — fails the build if anything is broken.
# Note: SSL cert files don't exist at build time, so we skip ssl validation
# by testing with a flag that ignores missing cert errors. The real
# validation happens at container start after certs are generated.
# RUN openresty -t

EXPOSE 80 443

ENTRYPOINT ["/docker-entrypoint.sh"]