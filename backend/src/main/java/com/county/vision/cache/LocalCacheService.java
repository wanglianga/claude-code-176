package com.county.vision.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** 本地（无 Redis）缓存实现：带 TTL 的进程内缓存 */
@Service
@ConditionalOnProperty(name = "vision.redis.enabled", havingValue = "false", matchIfMissing = true)
public class LocalCacheService implements CacheService {

    private record Entry(String json, Instant expireAt) {}

    private final Map<String, Entry> store = new ConcurrentHashMap<>();
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        Entry e = store.get(key);
        if (e == null) return null;
        if (e.expireAt().isBefore(Instant.now())) {
            store.remove(key);
            return null;
        }
        try {
            return mapper.readValue(e.json(), type);
        } catch (Exception ex) {
            return null;
        }
    }

    @Override
    public void put(String key, Object value, Duration ttl) {
        try {
            store.put(key, new Entry(mapper.writeValueAsString(value), Instant.now().plus(ttl)));
        } catch (Exception ignored) {
        }
    }

    @Override
    public void evict(String key) {
        store.remove(key);
    }

    @Override
    public void evictByPrefix(String prefix) {
        store.keySet().removeIf(k -> k.startsWith(prefix));
    }
}
