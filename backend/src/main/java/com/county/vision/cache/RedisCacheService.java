package com.county.vision.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/** Redis 缓存实现（容器 postgres profile）。Redis 故障时降级为不缓存，不影响业务。 */
@Slf4j
@Service
@ConditionalOnProperty(name = "vision.redis.enabled", havingValue = "true")
public class RedisCacheService implements CacheService {

    private final StringRedisTemplate redis;
    private final ObjectMapper mapper = new ObjectMapper();

    public RedisCacheService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public <T> T get(String key, Class<T> type) {
        try {
            String json = redis.opsForValue().get(key);
            if (json == null) return null;
            return mapper.readValue(json, type);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void put(String key, Object value, Duration ttl) {
        try {
            redis.opsForValue().set(key, mapper.writeValueAsString(value), ttl);
        } catch (Exception e) {
            log.debug("Redis 写入失败，已降级: {}", e.getMessage());
        }
    }

    @Override
    public void evict(String key) {
        try {
            redis.delete(key);
        } catch (Exception ignored) {
        }
    }

    @Override
    public void evictByPrefix(String prefix) {
        try {
            var keys = redis.keys(prefix + "*");
            if (keys != null && !keys.isEmpty()) {
                redis.delete(keys);
            }
        } catch (Exception ignored) {
        }
    }
}
