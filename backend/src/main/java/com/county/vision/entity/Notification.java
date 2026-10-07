package com.county.vision.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity @Table(name = "notifications")
@Getter @Setter
public class Notification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long userId;
    private String phone;
    private String channel;
    private String title;
    private String content;
    private Long studentId;
    private String bizType;
    /** PENDING/SENT/READ */
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime sentAt;
}
