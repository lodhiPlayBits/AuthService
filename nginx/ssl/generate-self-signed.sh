#!/bin/sh
# ═══════════════════════════════════════════════════════════════════
# generate-self-signed.sh
# ═══════════════════════════════════════════════════════════════════
# Generates a self-signed TLS certificate + key and DH parameters
# for local development / first-boot before Let's Encrypt is set up.
#
# The generated files mirror the path layout that certbot uses, so
# auth-service.conf works identically whether you're using the
# self-signed fallback or real Let's Encrypt certs.
#
# Run from the repo root:
#   ./ssl/generate-self-signed.sh
#
# Or inside the container at build time (see Dockerfile).
# ═══════════════════════════════════════════════════════════════════

set -e

DOMAIN="${DOMAIN:-authvolt.fun}"
SSL_DIR="${SSL_DIR:-/etc/nginx/ssl}"
CERT_DIR="${SSL_DIR}/live/${DOMAIN}"
DH_PARAMS="${SSL_DIR}/dhparams.pem"

echo "══════════════════════════════════════════════════"
echo "  Generating self-signed cert for: ${DOMAIN}"
echo "  Output directory: ${CERT_DIR}"
echo "══════════════════════════════════════════════════"

mkdir -p "${CERT_DIR}"

# ── Self-signed certificate (valid 365 days) ─────────────────────
if [ ! -f "${CERT_DIR}/fullchain.pem" ] || [ ! -f "${CERT_DIR}/privkey.pem" ]; then
    openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
        -keyout "${CERT_DIR}/privkey.pem" \
        -out    "${CERT_DIR}/fullchain.pem" \
        -subj   "/CN=${DOMAIN}/O=Self-Signed/C=US" \
        -addext "subjectAltName=DNS:${DOMAIN},DNS:*.${DOMAIN},IP:127.0.0.1"
    echo "✓ Certificate and key generated."
else
    echo "• Certificate already exists — skipping generation."
fi

# ── DH parameters (2048-bit) ────────────────────────────────────
# Used by ssl-params.conf for Perfect Forward Secrecy with DHE
# cipher suites.  This takes ~30s on a modern machine; pre-generate
# so nginx doesn't block on startup.
if [ ! -f "${DH_PARAMS}" ]; then
    echo "Generating DH parameters (2048 bit) — this takes a moment…"
    openssl dhparam -out "${DH_PARAMS}" 2048
    echo "✓ DH parameters generated."
else
    echo "• DH parameters already exist — skipping generation."
fi

echo ""
echo "Done. Files created:"
echo "  ${CERT_DIR}/fullchain.pem"
echo "  ${CERT_DIR}/privkey.pem"
echo "  ${DH_PARAMS}"
echo ""
echo "To use with docker compose, mount ${SSL_DIR} as a volume or"
echo "run this script inside the container's entrypoint."
