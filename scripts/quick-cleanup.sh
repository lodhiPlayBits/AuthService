#!/bin/bash
# =============================================================================
# Quick Cleanup Script
# =============================================================================
# Fast cleanup: logs + Redis + DB (preserves admin)
# For when you just need a quick reset between load tests
# =============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
AUTH_SERVICE_DIR="${PROJECT_ROOT}/auth_service"

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

echo -e "${CYAN}╔═══════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║         Quick Cleanup (Keep Admin)                ║${NC}"
echo -e "${CYAN}╚═══════════════════════════════════════════════════╝${NC}"
echo ""

# Load env
source "${AUTH_SERVICE_DIR}/.env"

# 1. Clean logs
echo -e "${GREEN}[1/3]${NC} Cleaning logs..."
rm -rf "${AUTH_SERVICE_DIR}/logs"/* 2>/dev/null || true
echo "      ✓ Logs cleaned"

# 2. Clean Redis
echo -e "${GREEN}[2/3]${NC} Flushing Redis cache..."
docker exec redis-service redis-cli -a "${REDIS_PASSWORD}" --no-auth-warning --scan | \
while read -r key; do
    docker exec redis-service redis-cli -a "${REDIS_PASSWORD}" --no-auth-warning DEL "$key" >/dev/null 2>&1
done
echo "      ✓ Redis flushed"

# 3. Clean database (keep admin)
echo -e "${GREEN}[3/3]${NC} Cleaning database (preserving admin)..."
docker exec -i postgres-container psql -U "${DB_USERNAME}" -d "${DB_NAME}" -q <<'EOF'
DO $$
DECLARE
    admin_user_id BIGINT;
BEGIN
    SELECT user_id INTO admin_user_id FROM user_table WHERE email = 'Admin@admin.com';
    DELETE FROM refresh_token WHERE user_id != admin_user_id OR user_id IS NULL;
    DELETE FROM audit_log WHERE user_id != admin_user_id OR user_id IS NULL;
    DELETE FROM user_table WHERE user_id != admin_user_id;
END $$;

ALTER SEQUENCE user_table_user_id_seq RESTART WITH 2;
ALTER SEQUENCE refresh_token_token_id_seq RESTART WITH 1;
ALTER SEQUENCE audit_log_log_id_seq RESTART WITH 1;
VACUUM ANALYZE;
EOF
echo "      ✓ Database cleaned"

echo ""
echo -e "${GREEN}✓ Cleanup complete!${NC}"
echo ""

# Show stats
echo -e "${YELLOW}Current state:${NC}"
docker exec -i postgres-container psql -U "${DB_USERNAME}" -d "${DB_NAME}" -t <<EOF
SELECT 'Users: ' || COUNT(*) FROM user_table
UNION ALL
SELECT 'Tokens: ' || COUNT(*) FROM refresh_token
UNION ALL
SELECT 'Audit logs: ' || COUNT(*) FROM audit_log;
EOF

redis_keys=$(docker exec redis-service redis-cli -a "${REDIS_PASSWORD}" --no-auth-warning DBSIZE 2>/dev/null | grep -o '[0-9]*')
echo "Redis keys: $redis_keys"

echo ""
echo -e "${GREEN}Ready for next load test!${NC}"
