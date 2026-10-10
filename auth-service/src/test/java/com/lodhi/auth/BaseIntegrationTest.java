package com.lodhi.auth;

import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public abstract class BaseIntegrationTest {
    @MockitoBean
    private LettuceConnectionFactory redisConnectionFactory;

    // RedisConfig is @Profile("!test"), so the typed template it defines must be
    // provided here for beans that inject RedisTemplate<String, Object>.
    @MockitoBean
    private RedisTemplate<String, Object> redisTemplate;

    @MockitoBean
    private com.lodhi.auth.services.TokenBlacklistService tokenBlacklistService;
}
