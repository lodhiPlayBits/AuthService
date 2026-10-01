package com.lodhi.auth.config;

import com.lodhi.auth.events.AccountEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EnableKafka
@Configuration
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public ConsumerFactory<String, AccountEvent> accountEventConsumerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        
        // Unique group ID per instance for broadcast pattern (all instances receive all messages)
        config.put(ConsumerConfig.GROUP_ID_CONFIG, "sse-service-" + UUID.randomUUID());
        
        // Start from latest to avoid replaying old events
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
        config.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, 1000);
        
        // JsonDeserializer settings
        config.put(JsonDeserializer.TRUSTED_PACKAGES, "com.lodhi.auth.events");
        config.put(JsonDeserializer.VALUE_DEFAULT_TYPE, AccountEvent.class.getName());
        config.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        
        return new DefaultKafkaConsumerFactory<>(
            config,
            new StringDeserializer(),
            new JsonDeserializer<>(AccountEvent.class, false)
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, AccountEvent> accountEventKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, AccountEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(accountEventConsumerFactory());
        factory.setConcurrency(1); // Single consumer thread per instance
        return factory;
    }
}
