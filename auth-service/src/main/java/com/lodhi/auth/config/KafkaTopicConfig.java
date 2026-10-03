package com.lodhi.auth.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    public static final String ACCOUNT_EVENTS_TOPIC = "account-events";

    private final int replicationFactor;

    public KafkaTopicConfig(@Value("${kafka.topics.replication-factor:1}") int replicationFactor) {
        this.replicationFactor = replicationFactor;
    }

    @Bean
    public NewTopic accountEventsTopic() {
        return TopicBuilder.name(ACCOUNT_EVENTS_TOPIC)
                .partitions(3)
                .replicas(replicationFactor)
                .config(TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_DELETE)
                .config(TopicConfig.RETENTION_MS_CONFIG, "604800000") // 7 days
                .build();
    }

    @Bean
    public NewTopic accountEventsDltTopic() {
        return TopicBuilder.name(ACCOUNT_EVENTS_TOPIC + ".DLT")
                .partitions(3)
                .replicas(replicationFactor)
                .build();
    }
}
