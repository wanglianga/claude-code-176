package com.county.vision.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity @Table(name = "timeline_events")
@Getter @Setter
public class TimelineEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long studentId;
    private String eventType;
    private String title;
    private String detail;
    private String actorName;
    private String actorRole;
    private String refType;
    private Long refId;
    private LocalDateTime eventTime;
}
