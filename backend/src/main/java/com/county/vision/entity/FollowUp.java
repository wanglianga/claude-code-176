package com.county.vision.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity @Table(name = "follow_ups")
@Getter @Setter
public class FollowUp {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long studentId;
    private Long diagnosisId;
    private LocalDate dueDate;
    /** PENDING/NOTIFIED/DONE/MISSED */
    private String status;
    private LocalDateTime remindedAt;
    private LocalDate doneDate;
    private String resultNote;
    private LocalDateTime createdAt;
}
