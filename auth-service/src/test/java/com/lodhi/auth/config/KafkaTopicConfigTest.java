package com.lodhi.auth.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KafkaTopicConfigTest {

    @Test
    void allTopicsUseConfiguredReplicationFactor() {
        KafkaTopicConfig config = new KafkaTopicConfig(2);

        List<NewTopic> topics = List.of(
                config.accountEventsTopic(),
                config.accountEventsDltTopic());

        for (NewTopic topic : topics) {
            assertEquals(2, topic.replicationFactor(), topic.name());
            assertEquals(3, topic.numPartitions(), topic.name());
        }
    }

    @Test
    void replicationFactorFollowsProperty() {
        KafkaTopicConfig config = new KafkaTopicConfig(3);

        assertEquals(3, config.accountEventsTopic().replicationFactor());
    }

    @Test
    void dltTopicNamesMatchDeadLetterPublishingRecovererDestination() {
        KafkaTopicConfig config = new KafkaTopicConfig(2);

        // DeadLetterPublishingRecoverer appends ".DLT" to the source topic
        assertEquals("account-events.DLT", config.accountEventsDltTopic().name());
    }
}
