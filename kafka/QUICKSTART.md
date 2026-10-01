# Kafka Quick Start Guide

Get your Kafka cluster up and running in 3 minutes.

## Prerequisites

- Docker and Docker Compose installed
- Ports available: 2181, 9092, 9093, 8080, 9308

## Step 1: Start Kafka Cluster

```bash
cd /home/ubuntu/backend/AuthService/kafka
docker-compose up -d
```

Wait 30-60 seconds for all services to initialize.

## Step 2: Verify Health

```bash
./scripts/health-check.sh
```

You should see all checks passing ✓

## Step 3: Create Topics

```bash
./scripts/create-topics.sh
```

This creates all required topics for the AuthVolt notification system.

## Step 4: Access Kafka UI

Open http://localhost:8080 in your browser to:
- View topics and messages
- Monitor consumer groups
- Check broker health
- Manage configurations

## Quick Test

### Produce a message

```bash
docker exec -it authvolt-kafka-broker-1 kafka-console-producer \
  --topic user-events \
  --bootstrap-server localhost:9092

# Type a message and press Enter
# Press Ctrl+C to exit
```

### Consume messages

```bash
docker exec -it authvolt-kafka-broker-1 kafka-console-consumer \
  --topic user-events \
  --from-beginning \
  --bootstrap-server localhost:9092

# Press Ctrl+C to exit
```

## Connect from Your Application

Update your Spring Boot `application.yml`:

```yaml
spring:
  kafka:
    bootstrap-servers: kafka-broker-1:19092,kafka-broker-2:19093
```

**Note**: Use `kafka-broker-1:19092,kafka-broker-2:19093` when connecting from Docker containers on the same network.

## Stop Kafka

```bash
docker-compose down
```

To also remove data:

```bash
docker-compose down -v
rm -rf data/ logs/
```

## Troubleshooting

**Containers won't start?**
```bash
docker-compose logs
```

**Connection refused?**
- Wait 30-60 seconds after starting
- Run `./scripts/health-check.sh`
- Check `docker-compose ps` shows all services as healthy

**Port already in use?**
```bash
# Find what's using the port
sudo netstat -tuln | grep 9092

# Change ports in .env file
```

## Next Steps

- Read [README.md](README.md) for detailed documentation
- Configure your notification-service to connect to Kafka
- Set up monitoring with Prometheus
- Enable SASL/SSL for production
