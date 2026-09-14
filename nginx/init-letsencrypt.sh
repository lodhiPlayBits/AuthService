#!/bin/sh
# Run this ONCE before the first `docker compose up`.
# Nginx needs certs to start on 443, but certbot needs a running web
# server on port 80 to prove domain ownership. This script breaks
# that chicken-and-egg loop by temporarily running a bare nginx on
# port 80 only, issuing the cert, then letting the real stack start.

set -e

DOMAIN="yourdomain.com"
EMAIL="you@email.com"

if [ "$DOMAIN" = "yourdomain.com" ]; then
    echo "Edit DOMAIN and EMAIL at the top of this script first."
    exit 1
fi

echo "== Step 1: starting temporary nginx on port 80 for the ACME challenge =="
docker run --rm -d --name temp-nginx \
    -p 80:80 \
    -v certbot-webroot:/var/www/certbot \
    nginx:1.27-alpine sh -c \
    "mkdir -p /usr/share/nginx/html/.well-known/acme-challenge && \
     ln -sf /var/www/certbot/.well-known/acme-challenge /usr/share/nginx/html/.well-known/acme-challenge && \
     nginx -g 'daemon off;'"

sleep 2

echo "== Step 2: requesting certificate from Let's Encrypt =="
docker run --rm \
    -v certbot-certs:/etc/letsencrypt \
    -v certbot-webroot:/var/www/certbot \
    certbot/certbot certonly \
    --webroot -w /var/www/certbot \
    -d "$DOMAIN" \
    --email "$EMAIL" \
    --agree-tos \
    --no-eff-email

echo "== Step 3: stopping temporary nginx =="
docker stop temp-nginx

echo "Done. Cert issued for $DOMAIN. You can now run: docker compose up -d"
echo "Verify auto-renewal works with:"
echo "  docker compose run --rm certbot certbot renew --dry-run"
