package com.lodhi.notification_service.config;

import com.lodhi.notification.contract.events.BroadcastAnnouncementEvent;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.TopicConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.DeserializationException;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Kafka plumbing for the {@code broadcast-announcements} flow the WebSocket
 * stack is built on: topic declarations, the producer template used by the
 * admin STOMP gateway, and the broadcast consumer factory (one group per
 * instance so every replica fans out to its own connected clients) with
 * retry-then-DLT error handling.
 */
@Configuration
public class KafkaAnnouncementConfig {

    public static final String BROADCAST_ANNOUNCEMENTS_TOPIC = "broadcast-announcements";

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${kafka.topics.replication-factor:1}")
    private int replicationFactor;

    @Bean
    public NewTopic broadcastAnnouncementsTopic() {
        return TopicBuilder.name(BROADCAST_ANNOUNCEMENTS_TOPIC)
                .partitions(3)
                .replicas(replicationFactor)
                .config(TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_DELETE)
                .config(TopicConfig.RETENTION_MS_CONFIG, "604800000") // 7 days
                .build();
    }

    @Bean
    public NewTopic broadcastAnnouncementsDltTopic() {
        return TopicBuilder.name(BROADCAST_ANNOUNCEMENTS_TOPIC + ".DLT")
                .partitions(3)
                .replicas(replicationFactor)
                .build();
    }

    public static final String ADMIN_RESPONSES_TOPIC = "admin-responses";

    @Bean
    public NewTopic adminResponsesTopic() {
        return TopicBuilder.name(ADMIN_RESPONSES_TOPIC)
                .partitions(3)
                .replicas(replicationFactor)
                .config(TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_DELETE)
                .config(TopicConfig.RETENTION_MS_CONFIG, "604800000") // 7 days
                .build();
    }

    @Bean
    public ProducerFactory<String, Object> announcementProducerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        config.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);

        config.put(ProducerConfig.ACKS_CONFIG, "all");
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        config.put(ProducerConfig.RETRIES_CONFIG, 3);

        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, Object> announcementKafkaTemplate() {
        return new KafkaTemplate<>(announcementProducerFactory());
    }

    @Bean
    public ConsumerFactory<String, BroadcastAnnouncementEvent> announcementConsumerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);

        // Unique group ID per instance for broadcast pattern
        config.put(ConsumerConfig.GROUP_ID_CONFIG, "ws-announcement-" + UUID.randomUUID());
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        config.put(JsonDeserializer.TRUSTED_PACKAGES, "com.lodhi.notification.contract.events");
        config.put(JsonDeserializer.VALUE_DEFAULT_TYPE, BroadcastAnnouncementEvent.class.getName());
        config.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);

        return new DefaultKafkaConsumerFactory<>(
            config,
            new StringDeserializer(),
            new JsonDeserializer<>(BroadcastAnnouncementEvent.class, false)
        );
    }

    @Bean
    public DefaultErrorHandler announcementErrorHandler(KafkaTemplate<String, Object> announcementKafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(announcementKafkaTemplate);
        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer,
            new FixedBackOff(1000L, 3)); // 3 retries, 1s apart
        handler.addNotRetryableExceptions(DeserializationException.class);
        return handler;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, BroadcastAnnouncementEvent> announcementKafkaListenerContainerFactory(
            DefaultErrorHandler announcementErrorHandler) {
        ConcurrentKafkaListenerContainerFactory<String, BroadcastAnnouncementEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(announcementConsumerFactory());
        factory.setConcurrency(1);
        factory.setCommonErrorHandler(announcementErrorHandler);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
        return factory;
    }

    @Bean
    public ConsumerFactory<String, java.util.Map<String, Object>> adminResponseConsumerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, "ws-admin-responses-" + UUID.randomUUID());
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        config.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        return new DefaultKafkaConsumerFactory<>(config, new StringDeserializer(), new JsonDeserializer<>(Map.class, false));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, java.util.Map<String, Object>> adminResponseKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, java.util.Map<String, Object>> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(adminResponseConsumerFactory());
        factory.setConcurrency(1);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
        return factory;
    }
}
