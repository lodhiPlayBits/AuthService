#!/bin/bash

# Create topics for AuthVolt notification system
# Run this after Kafka cluster is up and running

set -e

BROKER="localhost:9092"
REPLICATION_FACTOR=2
PARTITIONS=3
MIN_ISR=2

echo "Creating Kafka topics for AuthVolt..."

# User Events Topic
docker exec authvolt-kafka-node-1 kafka-topics \
  --create \
  --if-not-exists \
  --topic user-events \
  --bootstrap-server $BROKER \
  --partitions $PARTITIONS \
  --replication-factor $REPLICATION_FACTOR \
  --config min.insync.replicas=$MIN_ISR \
  --config retention.ms=604800000

echo "✓ Created topic: user-events"

# Email Notifications Topic
docker exec authvolt-kafka-node-1 kafka-topics \
  --create \
  --if-not-exists \
  --topic email-notifications \
  --bootstrap-server $BROKER \
  --partitions $PARTITIONS \
  --replication-factor $REPLICATION_FACTOR \
  --config min.insync.replicas=$MIN_ISR \
  --config retention.ms=604800000

echo "✓ Created topic: email-notifications"

# Password Change Events Topic
docker exec authvolt-kafka-node-1 kafka-topics \
  --create \
  --if-not-exists \
  --topic password-change-events \
  --bootstrap-server $BROKER \
  --partitions $PARTITIONS \
  --replication-factor $REPLICATION_FACTOR \
  --config min.insync.replicas=$MIN_ISR \
  --config retention.ms=604800000

echo "✓ Created topic: password-change-events"

# Login Events Topic
docker exec authvolt-kafka-node-1 kafka-topics \
  --create \
  --if-not-exists \
  --topic login-events \
  --bootstrap-server $BROKER \
  --partitions $PARTITIONS \
  --replication-factor $REPLICATION_FACTOR \
  --config min.insync.replicas=$MIN_ISR \
  --config retention.ms=604800000

echo "✓ Created topic: login-events"

# Audit Events Topic (with longer retention)
docker exec authvolt-kafka-node-1 kafka-topics \
  --create \
  --if-not-exists \
  --topic audit-events \
  --bootstrap-server $BROKER \
  --partitions $PARTITIONS \
  --replication-factor $REPLICATION_FACTOR \
  --config min.insync.replicas=$MIN_ISR \
  --config retention.ms=2592000000

echo "✓ Created topic: audit-events (30 days retention)"

# Dead Letter Queue Topic
docker exec authvolt-kafka-node-1 kafka-topics \
  --create \
  --if-not-exists \
  --topic dlq-notifications \
  --bootstrap-server $BROKER \
  --partitions $PARTITIONS \
  --replication-factor $REPLICATION_FACTOR \
  --config min.insync.replicas=$MIN_ISR \
  --config retention.ms=2592000000

echo "✓ Created topic: dlq-notifications (Dead Letter Queue)"

echo ""
echo "All topics created successfully!"
echo ""
echo "Listing all topics:"
docker exec authvolt-kafka-node-1 kafka-topics --list --bootstrap-server $BROKER

echo ""
echo "You can view topics in Kafka UI: http://localhost:8080"
