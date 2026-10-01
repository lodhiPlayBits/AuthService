#!/bin/bash

# Format storage if not already formatted
if [ ! -f /var/lib/kafka/data/meta.properties ]; then
  echo 'Formatting Kafka storage directory...'
  kafka-storage format -t ${CLUSTER_ID:-MkU3OEVBNTcwNTJENDM2Qk} -c /etc/kafka/kafka.properties
fi

# Start Kafka
exec /etc/confluent/docker/run
