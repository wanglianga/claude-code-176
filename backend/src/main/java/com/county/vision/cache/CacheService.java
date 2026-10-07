package com.county.vision.cache;

import java.time.Duration;

/** 简单缓存抽象：local 用内存实现，postgres(容器) 用 Redis */
public interface CacheService {
    <T> T get(String key, Class<T> type);

    void put(String key, Object value, Duration ttl);

    void evict(String key);

    void evictByPrefix(String prefix);
}
