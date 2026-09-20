# Redis Usage Guide for AuthService

## Overview

Redis is now integrated into the AuthService for caching and high-performance data operations. This guide shows how to use Redis in your application.

## Configuration

Redis is configured in `application.yml` and `.env`:

```yaml
spring:
  data:
    redis:
      host: redis-service
      port: 6379
      password: ${REDIS_PASSWORD}
  cache:
    type: redis
```

## Usage Examples

### 1. Using Spring Cache Annotations

The easiest way to cache data is using Spring's cache annotations:

```java
@Service
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    /**
     * Cache user by ID
     * First call: fetches from database and caches
     * Subsequent calls: returns from cache
     */
    @Cacheable(value = "users", key = "#id")
    public User getUserById(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new UserNotFoundException(id));
    }
    
    /**
     * Cache with complex key
     */
    @Cacheable(value = "users", key = "#email")
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new UserNotFoundException(email));
    }
    
    /**
     * Update cache when user is updated
     */
    @CachePut(value = "users", key = "#user.id")
    public User updateUser(User user) {
        return userRepository.save(user);
    }
    
    /**
     * Remove from cache when user is deleted
     */
    @CacheEvict(value = "users", key = "#id")
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }
    
    /**
     * Clear entire cache
     */
    @CacheEvict(value = "users", allEntries = true)
    public void clearUserCache() {
        // All users removed from cache
    }
}
```

### 2. Using RedisTemplate Directly

For more control, use `RedisTemplate`:

```java
@Service
public class SessionService {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    /**
     * Store session data
     */
    public void createSession(String sessionId, SessionData data) {
        String key = "session:" + sessionId;
        redisTemplate.opsForValue().set(key, data, Duration.ofHours(24));
    }
    
    /**
     * Get session data
     */
    public SessionData getSession(String sessionId) {
        String key = "session:" + sessionId;
        return (SessionData) redisTemplate.opsForValue().get(key);
    }
    
    /**
     * Delete session
     */
    public void deleteSession(String sessionId) {
        String key = "session:" + sessionId;
        redisTemplate.delete(key);
    }
    
    /**
     * Check if session exists
     */
    public boolean sessionExists(String sessionId) {
        String key = "session:" + sessionId;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
    
    /**
     * Extend session expiration
     */
    public void extendSession(String sessionId, Duration duration) {
        String key = "session:" + sessionId;
        redisTemplate.expire(key, duration);
    }
}
```

### 3. Token Blacklisting

Example for JWT token blacklisting:

```java
@Service
public class TokenBlacklistService {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    /**
     * Blacklist a token until it expires
     */
    public void blacklistToken(String token, Date expirationDate) {
        String key = "blacklist:" + token;
        long ttl = expirationDate.getTime() - System.currentTimeMillis();
        
        if (ttl > 0) {
            redisTemplate.opsForValue().set(key, "blacklisted", Duration.ofMillis(ttl));
        }
    }
    
    /**
     * Check if token is blacklisted
     */
    public boolean isTokenBlacklisted(String token) {
        String key = "blacklist:" + token;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
}
```

### 4. Rate Limiting

Example for API rate limiting:

```java
@Service
public class RateLimitService {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    /**
     * Check if user has exceeded rate limit
     * 
     * @param userId User identifier
     * @param maxRequests Maximum requests allowed
     * @param windowSeconds Time window in seconds
     * @return true if within limit, false if exceeded
     */
    public boolean checkRateLimit(String userId, int maxRequests, int windowSeconds) {
        String key = "rate_limit:" + userId;
        
        // Increment counter
        Long count = redisTemplate.opsForValue().increment(key);
        
        if (count == null) {
            return false;
        }
        
        // Set expiration on first request
        if (count == 1) {
            redisTemplate.expire(key, Duration.ofSeconds(windowSeconds));
        }
        
        return count <= maxRequests;
    }
    
    /**
     * Get remaining requests for user
     */
    public long getRemainingRequests(String userId, int maxRequests) {
        String key = "rate_limit:" + userId;
        Long count = (Long) redisTemplate.opsForValue().get(key);
        
        if (count == null) {
            return maxRequests;
        }
        
        return Math.max(0, maxRequests - count);
    }
}
```

### 5. Working with Hash Data Structures

```java
@Service
public class UserPreferencesService {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    /**
     * Store user preferences as hash
     */
    public void setPreference(Long userId, String key, String value) {
        String hashKey = "preferences:" + userId;
        redisTemplate.opsForHash().put(hashKey, key, value);
    }
    
    /**
     * Get specific preference
     */
    public String getPreference(Long userId, String key) {
        String hashKey = "preferences:" + userId;
        return (String) redisTemplate.opsForHash().get(hashKey, key);
    }
    
    /**
     * Get all preferences for user
     */
    public Map<Object, Object> getAllPreferences(Long userId) {
        String hashKey = "preferences:" + userId;
        return redisTemplate.opsForHash().entries(hashKey);
    }
    
    /**
     * Delete specific preference
     */
    public void deletePreference(Long userId, String key) {
        String hashKey = "preferences:" + userId;
        redisTemplate.opsForHash().delete(hashKey, key);
    }
}
```

### 6. Working with Sets

```java
@Service
public class OnlineUsersService {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    private static final String ONLINE_USERS_KEY = "online_users";
    
    /**
     * Mark user as online
     */
    public void markUserOnline(Long userId) {
        redisTemplate.opsForSet().add(ONLINE_USERS_KEY, userId.toString());
    }
    
    /**
     * Mark user as offline
     */
    public void markUserOffline(Long userId) {
        redisTemplate.opsForSet().remove(ONLINE_USERS_KEY, userId.toString());
    }
    
    /**
     * Check if user is online
     */
    public boolean isUserOnline(Long userId) {
        return Boolean.TRUE.equals(
            redisTemplate.opsForSet().isMember(ONLINE_USERS_KEY, userId.toString())
        );
    }
    
    /**
     * Get all online users
     */
    public Set<Object> getAllOnlineUsers() {
        return redisTemplate.opsForSet().members(ONLINE_USERS_KEY);
    }
    
    /**
     * Get count of online users
     */
    public long getOnlineUserCount() {
        Long size = redisTemplate.opsForSet().size(ONLINE_USERS_KEY);
        return size != null ? size : 0;
    }
}
```

### 7. Working with Sorted Sets (Leaderboards)

```java
@Service
public class LeaderboardService {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    private static final String LEADERBOARD_KEY = "leaderboard:score";
    
    /**
     * Update user score
     */
    public void updateScore(Long userId, double score) {
        redisTemplate.opsForZSet().add(LEADERBOARD_KEY, userId.toString(), score);
    }
    
    /**
     * Get user rank (0-based, lowest score = 0)
     */
    public Long getUserRank(Long userId) {
        return redisTemplate.opsForZSet().reverseRank(LEADERBOARD_KEY, userId.toString());
    }
    
    /**
     * Get top N users
     */
    public Set<Object> getTopUsers(int count) {
        return redisTemplate.opsForZSet()
            .reverseRange(LEADERBOARD_KEY, 0, count - 1);
    }
    
    /**
     * Get user score
     */
    public Double getUserScore(Long userId) {
        return redisTemplate.opsForZSet().score(LEADERBOARD_KEY, userId.toString());
    }
}
```

## Cache Configuration

### Default TTL

Default cache TTL is configured in `.env`:

```properties
CACHE_TTL=3600000  # 1 hour in milliseconds
```

### Per-Cache TTL

Configure different TTL for different caches in `RedisConfig.java`:

```java
@Bean
public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
    Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
    
    // Users cache: 1 hour
    cacheConfigurations.put("users", 
        RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofHours(1))
    );
    
    // Roles cache: 24 hours (rarely changes)
    cacheConfigurations.put("roles", 
        RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofHours(24))
    );
    
    // Sessions cache: 30 minutes
    cacheConfigurations.put("sessions", 
        RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(30))
    );
    
    return RedisCacheManager.builder(connectionFactory)
        .cacheDefaults(defaultConfig())
        .withInitialCacheConfigurations(cacheConfigurations)
        .transactionAware()
        .build();
}
```

## Testing Redis Connection

### Simple Connection Test

```java
@SpringBootTest
class RedisConnectionTest {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Test
    void testRedisConnection() {
        // Test basic set/get
        redisTemplate.opsForValue().set("test:key", "test:value");
        String value = (String) redisTemplate.opsForValue().get("test:key");
        
        assertEquals("test:value", value);
        
        // Clean up
        redisTemplate.delete("test:key");
    }
    
    @Test
    void testRedisPing() {
        String pong = redisTemplate.getConnectionFactory()
            .getConnection()
            .ping();
        
        assertEquals("PONG", pong);
    }
}
```

## Best Practices

### 1. Use Appropriate Data Structures
- **String**: Simple key-value pairs, counters
- **Hash**: Object storage, user preferences
- **Set**: Unique collections, tags, online users
- **Sorted Set**: Leaderboards, rankings, time-series
- **List**: Queues, recent items

### 2. Key Naming Convention
Use a consistent pattern:
```
resource:identifier:attribute
```

Examples:
- `user:123:profile`
- `session:abc123`
- `cache:users:email:john@example.com`
- `rate_limit:user:123`

### 3. Set Appropriate TTL
Always set TTL to prevent memory issues:
```java
// Good
redisTemplate.opsForValue().set(key, value, Duration.ofHours(1));

// Bad - never expires
redisTemplate.opsForValue().set(key, value);
```

### 4. Handle Cache Misses
```java
public User getUserById(Long id) {
    String key = "user:" + id;
    User user = (User) redisTemplate.opsForValue().get(key);
    
    if (user == null) {
        // Cache miss - fetch from database
        user = userRepository.findById(id)
            .orElseThrow(() -> new UserNotFoundException(id));
        
        // Store in cache
        redisTemplate.opsForValue().set(key, user, Duration.ofHours(1));
    }
    
    return user;
}
```

### 5. Batch Operations
Use pipeline for multiple operations:
```java
redisTemplate.executePipelined(new SessionCallback<Object>() {
    @Override
    public Object execute(RedisOperations operations) throws DataAccessException {
        for (User user : users) {
            operations.opsForValue().set("user:" + user.getId(), user);
        }
        return null;
    }
});
```

## Monitoring

### Check Redis Health

```bash
# From host
docker exec redis-service redis-cli -a "${REDIS_PASSWORD}" INFO

# From Spring Boot Actuator
curl http://localhost:8080/actuator/health
```

### View Cache Statistics

```bash
# Memory usage
docker exec redis-service redis-cli -a "${REDIS_PASSWORD}" INFO memory

# Key count
docker exec redis-service redis-cli -a "${REDIS_PASSWORD}" DBSIZE

# Hit/miss ratio
docker exec redis-service redis-cli -a "${REDIS_PASSWORD}" INFO stats | grep keyspace
```

## Troubleshooting

### Connection Issues

```java
@Component
public class RedisHealthCheck {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @PostConstruct
    public void checkRedisConnection() {
        try {
            String pong = redisTemplate.getConnectionFactory()
                .getConnection()
                .ping();
            log.info("Redis connection successful: {}", pong);
        } catch (Exception e) {
            log.error("Redis connection failed", e);
        }
    }
}
```

### Common Issues

1. **Connection refused**: Redis container not running
   ```bash
   cd redis && docker compose up -d
   ```

2. **Authentication failed**: Wrong password in `.env`
   ```bash
   # Check password matches in both files
   cat redis/.env | grep REDIS_PASSWORD
   cat auth_service/.env | grep REDIS_PASSWORD
   ```

3. **Network issues**: App not on authnet network
   ```bash
   docker network connect authnet auth-service
   ```

## Performance Tips

1. **Use connection pooling** (already configured in `application.yml`)
2. **Set appropriate TTL** for all cached data
3. **Use pipeline** for bulk operations
4. **Monitor memory usage** and adjust `maxmemory` if needed
5. **Use appropriate data structures** for your use case
6. **Avoid storing large objects** (keep under 100KB)

## Resources

- [Spring Data Redis Documentation](https://docs.spring.io/spring-data/redis/docs/current/reference/html/)
- [Redis Commands Reference](https://redis.io/commands)
- [Redis Best Practices](https://redis.io/topics/best-practices)

---

For Redis server configuration, see: `redis/README.md`
