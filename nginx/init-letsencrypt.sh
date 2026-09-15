#!/bin/sh
# Run this ONCE before the first `docker compose up`.
# Nginx needs certs to start on 443, but certbot needs a running web
# server on port 80 to prove domain ownership. This script breaks
# that chicken-and-egg loop by temporarily running a bare nginx on
# port 80 only, issuing the cert, then letting the real stack start.

#!/bin/sh
set -e

DOMAIN="authvolt.fun"
EMAIL="gauravlodhi983@gmail.com"

echo "== Requesting certificate via docker compose (correct volume namespace) =="
docker compose run --rm --entrypoint "\
  certbot certonly --webroot -w /var/www/certbot \
  -d $DOMAIN --email $EMAIL --agree-tos --no-eff-email" certbot

echo "Done. Reload nginx to pick up the cert:"
docker compose exec nginx nginx -s reload