#!/bin/bash
# =============================================================================
# Load Test Cleanup Script
# =============================================================================
# Cleans database, logs, and Redis after load testing
# Preserves admin user by default
# =============================================================================

set -euo pipefail

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
AUTH_SERVICE_DIR="${PROJECT_ROOT}/auth_service"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

# Functions
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

log_section() {
    echo ""
    echo -e "${CYAN}=== $1 ===${NC}"
}

# Load environment variables
if [ -f "${AUTH_SERVICE_DIR}/.env" ]; then
    source "${AUTH_SERVICE_DIR}/.env"
else
    log_error "Environment file not found: ${AUTH_SERVICE_DIR}/.env"
    exit 1
fi

# Main menu
show_menu() {
    echo ""
    echo -e "${BLUE}╔═══════════════════════════════════════════════════╗${NC}"
    echo -e "${BLUE}║     Load Test Cleanup Options                     ║${NC}"
    echo -e "${BLUE}╚═══════════════════════════════════════════════════╝${NC}"
    echo ""
    echo "1) Clean everything (keep admin user)"
    echo "2) Clean everything (remove ALL users including admin)"
    echo "3) Clean logs only"
    echo "4) Clean Redis cache only"
    echo "5) Clean database only (keep admin)"
    echo "6) Show current statistics"
    echo "7) Exit"
    echo ""
}

# Get database statistics
show_stats() {
    log_section "Current Database Statistics"
    
    docker exec -i postgres-container psql -U "${DB_USERNAME}" -d "${DB_NAME}" <<EOF
SELECT 'Users' as table_name, COUNT(*) as count FROM user_table
UNION ALL
SELECT 'Roles', COUNT(*) FROM role
UNION ALL
SELECT 'Permissions', COUNT(*) FROM permission
UNION ALL
SELECT 'Refresh Tokens', COUNT(*) FROM refresh_token
UNION ALL
SELECT 'Audit Logs', COUNT(*) FROM audit_log;
EOF

    log_section "Redis Statistics"
    docker exec redis-service redis-cli -a "${REDIS_PASSWORD}" --no-auth-warning INFO keyspace
    docker exec redis-service redis-cli -a "${REDIS_PASSWORD}" --no-auth-warning DBSIZE
    
    log_section "Log Files Size"
    du -sh "${AUTH_SERVICE_DIR}/logs" 2>/dev/null || echo "No logs directory"
}

# Clean logs
clean_logs() {
    log_section "Cleaning Logs"
    
    if [ -d "${AUTH_SERVICE_DIR}/logs" ]; then
        log_info "Removing log files..."
        rm -rf "${AUTH_SERVICE_DIR}/logs"/*
        log_info "✓ Logs cleaned"
    else
        log_warn "Logs directory not found"
    fi
    
    # Clean nginx logs if needed
    if [ -d "${PROJECT_ROOT}/nginx/logs" ]; then
        log_info "Cleaning nginx logs..."
        sudo truncate -s 0 "${PROJECT_ROOT}/nginx/logs"/*.log 2>/dev/null || true
        log_info "✓ Nginx logs cleaned"
    fi
}

# Clean Redis
clean_redis() {
    log_section "Cleaning Redis Cache"
    
    log_info "Flushing all Redis data..."
    
    # Since dangerous commands are disabled, we need to scan and delete
    local key_count=$(docker exec redis-service redis-cli -a "${REDIS_PASSWORD}" --no-auth-warning DBSIZE)
    
    if [ "$key_count" -gt 0 ]; then
        log_info "Found $key_count keys in Redis"
        
        # Delete keys by pattern
        docker exec redis-service redis-cli -a "${REDIS_PASSWORD}" --no-auth-warning --scan | \
        while read -r key; do
            docker exec redis-service redis-cli -a "${REDIS_PASSWORD}" --no-auth-warning DEL "$key" >/dev/null
        done
        
        log_info "✓ Redis cache cleaned"
    else
        log_info "Redis cache is already empty"
    fi
}

# Clean database (keep admin)
clean_database_keep_admin() {
    log_section "Cleaning Database (Preserving Admin User)"
    
    log_warn "This will delete all data EXCEPT the admin user!"
    read -p "Are you sure? (yes/no): " confirm
    
    if [ "$confirm" != "yes" ]; then
        log_info "Database cleanup cancelled"
        return
    fi
    
    log_info "Cleaning database tables..."
    
    docker exec -i postgres-container psql -U "${DB_USERNAME}" -d "${DB_NAME}" <<'EOF'
-- Get admin user ID
DO $$
DECLARE
    admin_user_id BIGINT;
BEGIN
    -- Find admin user
    SELECT user_id INTO admin_user_id FROM user_table WHERE email = 'Admin@admin.com';
    
    -- Delete non-admin refresh tokens
    DELETE FROM refresh_token WHERE user_id != admin_user_id;
    
    -- Delete audit logs for non-admin users
    DELETE FROM audit_log WHERE user_id != admin_user_id;
    
    -- Delete non-admin users (cascade will handle user_roles)
    DELETE FROM user_table WHERE user_id != admin_user_id;
    
    RAISE NOTICE 'Cleanup completed. Admin user preserved.';
END $$;

-- Reset sequences
ALTER SEQUENCE user_table_user_id_seq RESTART WITH 2;
ALTER SEQUENCE refresh_token_token_id_seq RESTART WITH 1;
ALTER SEQUENCE audit_log_log_id_seq RESTART WITH 1;

-- Vacuum to reclaim space
VACUUM ANALYZE user_table;
VACUUM ANALYZE refresh_token;
VACUUM ANALYZE audit_log;
EOF
    
    log_info "✓ Database cleaned (admin preserved)"
}

# Clean database (remove everything)
clean_database_all() {
    log_section "Cleaning Database (Remove ALL Users)"
    
    log_error "WARNING: This will delete EVERYTHING including admin user!"
    log_error "You will need to restart the application to recreate admin"
    read -p "Are you ABSOLUTELY sure? (type 'DELETE ALL'): " confirm
    
    if [ "$confirm" != "DELETE ALL" ]; then
        log_info "Database cleanup cancelled"
        return
    fi
    
    log_info "Deleting all database data..."
    
    docker exec -i postgres-container psql -U "${DB_USERNAME}" -d "${DB_NAME}" <<'EOF'
-- Disable foreign key checks temporarily
SET session_replication_role = 'replica';

-- Truncate all tables
TRUNCATE TABLE refresh_token CASCADE;
TRUNCATE TABLE audit_log CASCADE;
TRUNCATE TABLE user_roles CASCADE;
TRUNCATE TABLE user_table CASCADE;
TRUNCATE TABLE role_permissions CASCADE;
TRUNCATE TABLE role CASCADE;
TRUNCATE TABLE permission CASCADE;

-- Re-enable foreign key checks
SET session_replication_role = 'origin';

-- Reset all sequences
ALTER SEQUENCE user_table_user_id_seq RESTART WITH 1;
ALTER SEQUENCE role_role_id_seq RESTART WITH 1;
ALTER SEQUENCE permission_permission_id_seq RESTART WITH 1;
ALTER SEQUENCE refresh_token_token_id_seq RESTART WITH 1;
ALTER SEQUENCE audit_log_log_id_seq RESTART WITH 1;

-- Vacuum to reclaim space
VACUUM ANALYZE;
EOF
    
    log_info "✓ All database data deleted"
    log_warn "You must restart auth-service to recreate admin and default roles/permissions"
    
    read -p "Restart auth-service now? (yes/no): " restart_confirm
    if [ "$restart_confirm" = "yes" ]; then
        log_info "Restarting auth-service..."
        docker restart auth-service
        log_info "✓ Auth-service restarted. Admin user will be recreated."
    fi
}

# Full cleanup (keep admin)
full_cleanup_keep_admin() {
    log_section "Full Cleanup (Preserving Admin User)"
    
    clean_logs
    clean_redis
    clean_database_keep_admin
    
    log_section "Cleanup Summary"
    log_info "✓ Logs cleaned"
    log_info "✓ Redis cache cleared"
    log_info "✓ Database cleaned (admin preserved)"
    echo ""
    show_stats
}

# Full cleanup (remove all)
full_cleanup_all() {
    log_section "Full Cleanup (Remove Everything)"
    
    clean_logs
    clean_redis
    clean_database_all
    
    log_section "Cleanup Summary"
    log_info "✓ Logs cleaned"
    log_info "✓ Redis cache cleared"
    log_info "✓ Database fully cleaned"
    echo ""
}

# Main execution
main() {
    echo -e "${BLUE}╔═══════════════════════════════════════════════════╗${NC}"
    echo -e "${BLUE}║      Load Test Cleanup Utility                    ║${NC}"
    echo -e "${BLUE}╚═══════════════════════════════════════════════════╝${NC}"
    
    # Check if containers are running
    if ! docker ps | grep -q "auth-service"; then
        log_error "auth-service container is not running"
        exit 1
    fi
    
    if ! docker ps | grep -q "redis-service"; then
        log_error "redis-service container is not running"
        exit 1
    fi
    
    # Show current stats first
    show_stats
    
    while true; do
        show_menu
        read -p "Select option (1-7): " choice
        
        case $choice in
            1)
                full_cleanup_keep_admin
                ;;
            2)
                full_cleanup_all
                ;;
            3)
                clean_logs
                ;;
            4)
                clean_redis
                ;;
            5)
                clean_database_keep_admin
                ;;
            6)
                show_stats
                ;;
            7)
                log_info "Exiting..."
                exit 0
                ;;
            *)
                log_error "Invalid option. Please select 1-7."
                ;;
        esac
        
        echo ""
        read -p "Press Enter to continue..."
    done
}

# Run main function
main "$@"
