# Kafka KRaft Cluster Setup

Production-grade Apache Kafka cluster with **KRaft mode** (no Zookeeper!) - 2 nodes for the AuthVolt notification system.

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│              Kafka KRaft Cluster                         │
│         (No Zookeeper Required!)                         │
├─────────────────────────────────────────────────────────┤
│                                                           │
│  ┌────────────────┐     ┌────────────────┐             │
│  │   Node 1       │     │   Node 2       │             │
│  │ ┌────────────┐ │     │ ┌────────────┐ │             │
│  │ │ Controller │ │◄────┤ │ Controller │ │             │
│  │ └────────────┘ │     │ └────────────┘ │             │
│  │ ┌────────────┐ │     │ ┌────────────┐ │             │
│  │ │   Broker   │ │     │ │   Broker   │ │             │
│  │ │   :9092    │ │     │ │   :9094    │ │             │
│  │ └────────────┘ │     │ └────────────┘ │             │
│  └────────────────┘     └────────────────┘             │
│                                                           │
│  ┌──────────────┐                                        │
│  │  Kafka UI    │  ← Management Web Interface            │
│  │  :8080       │                                        │
│  └──────────────┘                                        │
│                                                           │
│  ┌──────────────┐                                        │
│  │   Exporter   │  ← Prometheus Metrics                  │
│  │   :9308      │                                        │
│  └──────────────┘                                        │
└─────────────────────────────────────────────────────────┘
         │
         │ kafka-node-1:19092, kafka-node-2:19092
         │
    ┌────▼────────────────┐
    │  Notification       │
    │  Service            │
    └─────────────────────┘
```

## Features

✅ **KRaft Mode**: No Zookeeper dependency - simpler, faster, more scalable  
✅ **High Availability**: 2 nodes with replication factor 2  
✅ **Data Durability**: `min.insync.replicas=2` ensures no data loss  
✅ **Monitoring**: Kafka UI for management, Prometheus exporter for metrics  
✅ **Production Ready**: Proper JVM tuning, health checks, and restart policies  
✅ **Secure Configuration**: Environment-based configuration  
✅ **Auto Topic Creation**: Enabled for development convenience  

## Why KRaft?

KRaft (Kafka Raft Metadata mode) replaces Zookeeper with Kafka's built-in consensus protocol:

- **Simpler Architecture**: One less system to manage
- **Faster Operations**: Metadata operations are 10x faster
- **Better Scaling**: Supports millions of partitions
- **Easier Operations**: Simplified deployment and monitoring
- **Future-Proof**: Zookeeper mode is deprecated in Kafka 3.x+  

## Quick Start

### 1. Copy Environment Variables

```bash
cp .env.example .env
# Edit .env if needed
```

### 2. Start Kafka Cluster

```bash
docker-compose up -d
```

### 3. Verify Cluster Health

```bash
# Check all containers are running
docker-compose ps

# Check broker 1 health
docker exec authvolt-kafka-broker-1 kafka-broker-api-versions --bootstrap-server localhost:9092

# Check broker 2 health
docker exec authvolt-kafka-broker-2 kafka-broker-api-versions --bootstrap-server localhost:9093
```

### 4. Access Kafka UI

Open http://localhost:8080 in your browser to manage topics, view messages, and monitor the cluster.

## Connecting from Services

### Spring Boot Configuration (notification-service)

```yaml
spring:
  kafka:
    bootstrap-servers: kafka-broker-1:19092,kafka-broker-2:19093
    producer:
      acks: all
      retries: 3
    consumer:
      auto-offset-reset: earliest
      enable-auto-commit: false
```

### Connection Strings

- **From Docker containers (same network)**: `kafka-broker-1:19092,kafka-broker-2:19093`
- **From host machine**: `localhost:9092,localhost:9093`
- **From external network**: `<your-domain>:9092,<your-domain>:9093`

## Topic Management

### Create a Topic

```bash
docker exec authvolt-kafka-broker-1 kafka-topics \
  --create \
  --topic user-events \
  --bootstrap-server localhost:9092 \
  --partitions 3 \
  --replication-factor 2 \
  --config min.insync.replicas=2
```

### List Topics

```bash
docker exec authvolt-kafka-broker-1 kafka-topics \
  --list \
  --bootstrap-server localhost:9092
```

### Describe a Topic

```bash
docker exec authvolt-kafka-broker-1 kafka-topics \
  --describe \
  --topic user-events \
  --bootstrap-server localhost:9092
```

### Delete a Topic

```bash
docker exec authvolt-kafka-broker-1 kafka-topics \
  --delete \
  --topic user-events \
  --bootstrap-server localhost:9092
```

## Testing

### Produce Test Messages

```bash
docker exec -it authvolt-kafka-broker-1 kafka-console-producer \
  --topic test-topic \
  --bootstrap-server localhost:9092
```

### Consume Test Messages

```bash
docker exec -it authvolt-kafka-broker-1 kafka-console-consumer \
  --topic test-topic \
  --from-beginning \
  --bootstrap-server localhost:9092
```

## Monitoring

### Kafka UI
- **URL**: http://localhost:8080
- **Features**: Topic management, message browsing, consumer groups

### Prometheus Metrics
- **URL**: http://localhost:9308/metrics
- **Metrics**: Broker health, topic stats, consumer lag

### Health Checks

```bash
# Check Zookeeper
docker exec authvolt-zookeeper nc -z localhost 2181 && echo "Zookeeper OK"

# Check Broker 1
docker exec authvolt-kafka-broker-1 kafka-broker-api-versions --bootstrap-server localhost:9092

# Check Broker 2
docker exec authvolt-kafka-broker-2 kafka-broker-api-versions --bootstrap-server localhost:9093
```

## Production Configuration

### For Production Deployment

Update `.env`:

```bash
# Use your actual domain/IP
KAFKA_EXTERNAL_HOST=kafka.yourdomain.com

# Increase heap size for production workload
KAFKA_HEAP_OPTS=-Xmx4G -Xms4G

# Adjust based on your retention requirements
KAFKA_LOG_RETENTION_HOURS=168  # 7 days
KAFKA_LOG_RETENTION_BYTES=10737418240  # 10GB per partition
```

### Security (Production)

For production, enable SASL/SSL:

1. Generate SSL certificates
2. Update docker-compose.yml with SASL_SSL listeners
3. Configure authentication mechanisms
4. Update application connection strings

## Backup & Recovery

### Backup Kafka Data

```bash
# Stop Kafka cluster
docker-compose down

# Backup data directories
tar -czf kafka-backup-$(date +%Y%m%d).tar.gz data/

# Restart cluster
docker-compose up -d
```

### Restore from Backup

```bash
# Stop cluster
docker-compose down

# Restore data
tar -xzf kafka-backup-YYYYMMDD.tar.gz

# Start cluster
docker-compose up -d
```

## Troubleshooting

### Container won't start

```bash
# Check logs
docker-compose logs kafka-broker-1
docker-compose logs kafka-broker-2
docker-compose logs zookeeper

# Ensure ports are available
netstat -tuln | grep -E '9092|9093|2181|8080'
```

### Connection refused

```bash
# Verify network
docker network ls
docker network inspect authvolt-kafka-network

# Check if brokers are advertising correct addresses
docker exec authvolt-kafka-broker-1 env | grep KAFKA_ADVERTISED_LISTENERS
```

### Out of disk space

```bash
# Check disk usage
docker exec authvolt-kafka-broker-1 du -sh /var/lib/kafka/data

# Reduce retention
# Edit .env: KAFKA_LOG_RETENTION_HOURS=24
docker-compose up -d
```

## Maintenance

### View Logs

```bash
# Tail all logs
docker-compose logs -f

# Specific service
docker-compose logs -f kafka-broker-1
```

### Restart Cluster

```bash
# Graceful restart
docker-compose restart

# Full restart with rebuild
docker-compose down && docker-compose up -d
```

### Clean Up

```bash
# Stop and remove containers
docker-compose down

# Remove all data (DESTRUCTIVE)
docker-compose down -v
rm -rf data/ logs/
```

## Scaling

To add a third broker:

1. Copy broker-2 configuration in `docker-compose.yml`
2. Change:
   - `KAFKA_BROKER_ID: 3`
   - Ports: `9094:9094` and `19094:19094`
   - Container name: `authvolt-kafka-broker-3`
3. Update replication factors if needed
4. Run `docker-compose up -d`

## Integration with Services

### Auth Service (Producer)
- Produces user registration events
- Produces password change events
- Produces login events

### Notification Service (Consumer)
- Consumes user events
- Sends email notifications
- Manages notification templates

## Useful Commands

```bash
# List consumer groups
docker exec authvolt-kafka-broker-1 kafka-consumer-groups \
  --list --bootstrap-server localhost:9092

# Check consumer lag
docker exec authvolt-kafka-broker-1 kafka-consumer-groups \
  --describe --group notification-service-group \
  --bootstrap-server localhost:9092

# Get cluster metadata
docker exec authvolt-kafka-broker-1 kafka-metadata \
  --bootstrap-server localhost:9092
```

## Support

For issues or questions:
1. Check the logs: `docker-compose logs`
2. Verify health checks are passing
3. Check Kafka UI for cluster status
4. Review [Confluent Platform Documentation](https://docs.confluent.io/)
