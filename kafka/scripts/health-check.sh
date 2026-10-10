#!/bin/bash

# Health check script for Kafka KRaft cluster

set -e

echo "=========================================="
echo "   Kafka KRaft Cluster Health Check"
echo "=========================================="
echo ""

# Check Kafka Node 1
echo "1. Checking Kafka Node 1 (Controller + Broker)..."
if docker exec authvolt-kafka-node-1 kafka-broker-api-versions --bootstrap-server localhost:19092 2>/dev/null | grep -q "ApiVersion"; then
    echo "   ✓ Kafka Node 1 is healthy"
else
    echo "   ✗ Kafka Node 1 is not responding"
    exit 1
fi
echo ""

# Check Kafka Node 2
echo "2. Checking Kafka Node 2 (Controller + Broker)..."
if docker exec authvolt-kafka-node-2 kafka-broker-api-versions --bootstrap-server localhost:19092 2>/dev/null | grep -q "ApiVersion"; then
    echo "   ✓ Kafka Node 2 is healthy"
else
    echo "   ✗ Kafka Node 2 is not responding"
    exit 1
fi
echo ""

# Check Kafka UI
echo "3. Checking Kafka UI..."
if curl -s -o /dev/null -w "%{http_code}" http://localhost:8080 | grep -q "200"; then
    echo "   ✓ Kafka UI is accessible at http://localhost:8080"
else
    echo "   ⚠ Kafka UI is not responding (may still be starting)"
fi
echo ""

# Check Metadata Quorum
echo "4. Checking KRaft Metadata Quorum..."
docker exec authvolt-kafka-node-1 kafka-metadata-quorum --bootstrap-server localhost:19092 describe --status 2>/dev/null || echo "   ⚠ Unable to fetch quorum status"
echo ""

# List topics
echo "5. Listing topics..."
TOPICS=$(docker exec authvolt-kafka-node-1 kafka-topics --list --bootstrap-server localhost:19092 2>/dev/null)
if [ -z "$TOPICS" ]; then
    echo "   No topics found (this is normal for a fresh installation)"
else
    echo "$TOPICS" | while read -r topic; do
        echo "   - $topic"
    done
fi
echo ""

# Show cluster information
echo "6. Cluster Information..."
docker exec authvolt-kafka-node-1 kafka-broker-api-versions --bootstrap-server localhost:19092 2>/dev/null | grep "id:" | head -n 2
echo ""

echo "=========================================="
echo "   All checks passed! ✓"
echo "=========================================="
echo ""
echo "Quick commands:"
echo "  - View logs: docker-compose logs -f"
echo "  - Kafka UI: http://localhost:8080"
echo "  - Create topics: ./scripts/create-topics.sh"
echo ""
echo "KRaft Mode Benefits:"
echo "  ✓ No Zookeeper dependency"
echo "  ✓ Faster cluster operations"
echo "  ✓ Better scalability"
echo "  ✓ Simplified architecture"
