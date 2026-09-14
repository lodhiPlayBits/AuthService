#!/bin/bash
# Run auth service locally (not in Docker)

set -e

# Check PostgreSQL
if ! sudo service postgresql status | grep -q "online\|active\|running"; then
    sudo service postgresql start
    sleep 2
fi

echo "Starting locally..."

# Check if database exists and has Flyway issues
if sudo -u postgres psql -lqt | cut -d \| -f 1 | grep -qw auth_service; then
    echo "Database exists. Checking for Flyway issues..."
    
    # Try to detect checksum mismatch (simplified check)
    CHECKSUM_COUNT=$(sudo -u postgres psql -d auth_service -t -c "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '12';" 2>/dev/null || echo "0")
    
    if [ "$CHECKSUM_COUNT" != "0" ]; then
        echo "Found Flyway migration V12. Resetting database for clean state..."
        sudo -u postgres psql -c "DROP DATABASE auth_service;"
        sudo -u postgres psql -c "CREATE DATABASE auth_service;"
        echo "Database reset complete."
    fi
fi

# Load environment and run
set -a
source .env
set +a
./mvnw spring-boot:run
