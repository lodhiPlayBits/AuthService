FROM confluentinc/cp-kafka:7.6.0

# ==================== KRaft Configuration ====================
ENV KAFKA_PROCESS_ROLES='controller,broker' \
    KAFKA_CONTROLLER_QUORUM_VOTERS='1@kafka-node-1:9093,2@kafka-node-2:9093' \
    KAFKA_CONTROLLER_LISTENER_NAMES='CONTROLLER' \
    CLUSTER_ID='MkU3OEVBNTcwNTJENDM2Qk' \
    KAFKA_LISTENER_SECURITY_PROTOCOL_MAP='CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT,EXTERNAL:PLAINTEXT' \
    KAFKA_INTER_BROKER_LISTENER_NAME='PLAINTEXT'

# ==================== Replication & Durability ====================
ENV KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=2 \
    KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR=2 \
    KAFKA_TRANSACTION_STATE_LOG_MIN_ISR=2 \
    KAFKA_DEFAULT_REPLICATION_FACTOR=2 \
    KAFKA_MIN_INSYNC_REPLICAS=2

# ==================== Topic Configuration ====================
ENV KAFKA_AUTO_CREATE_TOPICS_ENABLE='true' \
    KAFKA_DELETE_TOPIC_ENABLE='true' \
    KAFKA_NUM_PARTITIONS=3

# ==================== Log Configuration ====================
ENV KAFKA_LOG_DIRS='/var/lib/kafka/data' \
    KAFKA_LOG_RETENTION_HOURS=168 \
    KAFKA_LOG_RETENTION_BYTES=1073741824 \
    KAFKA_LOG_SEGMENT_BYTES=1073741824 \
    KAFKA_LOG_RETENTION_CHECK_INTERVAL_MS=300000 \
    KAFKA_LOG_CLEANUP_POLICY='delete'

# ==================== Performance Tuning ====================
ENV KAFKA_NUM_NETWORK_THREADS=8 \
    KAFKA_NUM_IO_THREADS=8 \
    KAFKA_SOCKET_SEND_BUFFER_BYTES=102400 \
    KAFKA_SOCKET_RECEIVE_BUFFER_BYTES=102400 \
    KAFKA_SOCKET_REQUEST_MAX_BYTES=104857600

# ==================== Group Coordinator ====================
ENV KAFKA_GROUP_INITIAL_REBALANCE_DELAY_MS=3000

# ==================== JVM Settings ====================
ENV KAFKA_HEAP_OPTS='-Xmx1G -Xms1G' \
    KAFKA_JVM_PERFORMANCE_OPTS='-XX:+UseG1GC -XX:MaxGCPauseMillis=20 -XX:InitiatingHeapOccupancyPercent=35'

COPY start-kafka.sh /start-kafka.sh
USER root
RUN chmod +x /start-kafka.sh
USER appuser

CMD ["/start-kafka.sh"]
