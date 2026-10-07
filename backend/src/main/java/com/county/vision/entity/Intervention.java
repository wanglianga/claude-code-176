package com.county.vision.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity @Table(name = "interventions")
@Getter @Setter
public class Intervention {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long studentId;
    private Long classId;
    private Long schoolId;
    /** SEAT_ADJUSTMENT/OUTDOOR_ACTIVITY/EYE_HABIT_REMINDER/PARENT_NOTICE/PERIODIC_CHECK */
    private String type;
    private String title;
    private String detail;
    private LocalDate plannedDate;
    /** PLANNED/DONE/VERIFIED */
    private String status;
    private Long operatorId;
    private LocalDateTime executedAt;
    private String effectNote;
    private LocalDateTime createdAt;
}
