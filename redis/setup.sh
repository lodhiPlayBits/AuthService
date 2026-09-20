#!/bin/bash
# =============================================================================
# Redis Setup Script
# =============================================================================
# Initializes Redis for first-time deployment
# =============================================================================

set -euo pipefail

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}╔═══════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║         Redis Production Setup                    ║${NC}"
echo -e "${BLUE}╚═══════════════════════════════════════════════════╝${NC}"
echo ""

# Check if .env exists
if [ -f ".env" ]; then
    echo -e "${YELLOW}⚠${NC} .env file already exists"
    read -p "Do you want to regenerate it? (yes/no): " regenerate
    
    if [ "$regenerate" != "yes" ]; then
        echo -e "${GREEN}✓${NC} Using existing .env file"
    else
        rm .env
    fi
fi

# Create .env if it doesn't exist
if [ ! -f ".env" ]; then
    echo -e "${BLUE}→${NC} Creating .env file..."
    cp .env.example .env
    
    # Generate strong password
    NEW_PASSWORD=$(openssl rand -base64 32)
    
    # Update .env with generated password
    sed -i "s/REDIS_PASSWORD=.*/REDIS_PASSWORD=${NEW_PASSWORD}/" .env
    
    echo -e "${GREEN}✓${NC} .env file created with auto-generated password"
    echo -e "${YELLOW}⚠${NC} Password: ${NEW_PASSWORD}"
    echo -e "${YELLOW}⚠${NC} Save this password securely!"
fi

# Set proper permissions
echo -e "${BLUE}→${NC} Setting file permissions..."
chmod 600 .env
chmod 755 data backups logs 2>/dev/null || mkdir -p data backups logs && chmod 755 data backups logs

echo -e "${GREEN}✓${NC} Permissions set"

# Check if authnet network exists
echo -e "${BLUE}→${NC} Checking Docker network..."
if ! docker network ls | grep -q "authnet"; then
    echo -e "${YELLOW}⚠${NC} Creating authnet network..."
    docker network create authnet
    echo -e "${GREEN}✓${NC} Network created"
else
    echo -e "${GREEN}✓${NC} Network already exists"
fi

# Build and start Redis
echo -e "${BLUE}→${NC} Building Redis image..."
docker compose build

echo -e "${BLUE}→${NC} Starting Redis service..."
docker compose up -d

# Wait for Redis to be ready
echo -e "${BLUE}→${NC} Waiting for Redis to be ready..."
sleep 5

# Test connection
if docker exec redis-service redis-cli -a "$(grep REDIS_PASSWORD .env | cut -d= -f2)" ping &>/dev/null; then
    echo -e "${GREEN}✓${NC} Redis is running and responding"
else
    echo -e "${RED}✗${NC} Redis is not responding"
    exit 1
fi

# Show status
echo ""
echo -e "${GREEN}╔═══════════════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║         Setup Complete!                           ║${NC}"
echo -e "${GREEN}╚═══════════════════════════════════════════════════╝${NC}"
echo ""
echo "Redis Configuration:"
echo "  Host: redis-service (on authnet network)"
echo "  Port: 6379"
echo "  Password: Check .env file"
echo ""
echo "Useful Commands:"
echo "  Status:  docker ps | grep redis-service"
echo "  Logs:    docker logs redis-service"
echo "  Stop:    docker compose down"
echo "  Monitor: docker exec redis-service redis-cli -a \$REDIS_PASSWORD INFO"
echo ""
echo -e "${YELLOW}⚠ Remember to update your application's REDIS_PASSWORD environment variable${NC}"
echo ""
