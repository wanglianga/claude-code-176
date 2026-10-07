package com.county.vision.common;

import org.springframework.stereotype.Service;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

/** 通知服务：落库通知记录，同时推入 Redis Stream 供模拟短信网关消费，并写未读计数 */
@Service
public class NotifyService {

    private final StringRedisTemplate redis;

    public NotifyService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /** 未读通知计数（供前端角标） */
    public long unreadCount(Long userId) {
        try {
            String v = redis.opsForValue().get("notify:unread:" + userId);
            return v == null ? 0 : Long.parseLong(v);
        } catch (Exception e) {
            return 0;
        }
    }

    public void incrementUnread(Long userId) {
        try {
            redis.opsForValue().increment("notify:unread:" + userId);
            redis.expire("notify:unread:" + userId, Duration.ofDays(30));
        } catch (Exception ignored) {
        }
    }

    public void clearUnread(Long userId) {
        try {
            redis.delete("notify:unread:" + userId);
        } catch (Exception ignored) {
        }
    }

    /**
     * 发送一条通知：写入 Redis Stream（模拟短信/微信网关队列），失败不影响主业务。
     */
    public void pushToGateway(String phone, String content, Long studentId, String bizType) {
        try {
            java.util.Map<String, String> payload = new java.util.HashMap<>();
            payload.put("phone", phone == null ? "" : phone);
            payload.put("content", content);
            payload.put("studentId", studentId == null ? "" : studentId.toString());
            payload.put("bizType", bizType == null ? "" : bizType);
            payload.put("ts", String.valueOf(System.currentTimeMillis()));
            redis.opsForStream().add("notify:sms-gateway", payload);
        } catch (Exception ignored) {
            // Redis 不可用时主流程仍可继续
        }
    }
}
