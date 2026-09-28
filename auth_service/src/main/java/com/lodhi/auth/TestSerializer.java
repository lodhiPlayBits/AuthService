package com.lodhi.auth;

import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JavaType;
import java.util.Set;

public class TestSerializer {
    public void test() {
        ObjectMapper mapper = new ObjectMapper();
        JavaType type = mapper.getTypeFactory().constructCollectionType(Set.class, String.class);
        Jackson2JsonRedisSerializer<Set<String>> serializer1 = new Jackson2JsonRedisSerializer<>(mapper, type);
        Jackson2JsonRedisSerializer<String> serializer2 = new Jackson2JsonRedisSerializer<>(mapper, String.class);
    }
}
