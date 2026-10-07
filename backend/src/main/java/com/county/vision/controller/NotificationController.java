package com.county.vision.controller;

import com.county.vision.common.LoginUser;
import com.county.vision.common.NotifyService;
import com.county.vision.entity.Notification;
import com.county.vision.repo.NotificationRepository;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationRepository repo;
    private final NotifyService notify;

    public NotificationController(NotificationRepository repo, NotifyService notify) {
        this.repo = repo;
        this.notify = notify;
    }

    @GetMapping
    public List<Notification> mine() {
        return repo.findByUserIdOrderByCreatedAtDesc(LoginUser.get().id());
    }

    @GetMapping("/unread-count")
    public Map<String, Object> unread() {
        long dbUnread = repo.countByUserIdAndStatus(LoginUser.get().id(), "SENT");
        long redisUnread = notify.unreadCount(LoginUser.get().id());
        return Map.of("count", Math.max(dbUnread, redisUnread));
    }

    @PostMapping("/{id}/read")
    public Notification read(@PathVariable Long id) {
        Notification n = repo.findById(id).orElseThrow();
        n.setStatus("READ");
        return repo.save(n);
    }

    @PostMapping("/read-all")
    public Map<String, Object> readAll() {
        repo.findByUserIdOrderByCreatedAtDesc(LoginUser.get().id()).stream()
                .filter(n -> "SENT".equals(n.getStatus()))
                .forEach(n -> { n.setStatus("READ"); repo.save(n); });
        notify.clearUnread(LoginUser.get().id());
        return Map.of("ok", true);
    }
}
