package com.county.vision.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity @Table(name = "students")
@Getter @Setter
public class Student {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String gender;
    private LocalDate birthDate;
    private Long schoolId;
    private Long classId;
    private String studentNo;
    private String grade;
    private String parentName;
    private String parentPhone;
    private String pastVisionNote;
    /** PENDING_SCREENING/SCREENED_ABNORMAL/AWAIT_CONFIRM/AWAIT_REVIEW/AWAIT_REVISIT/IN_REVISIT/REVISIT_DONE/GLASSES_FITTED/SUBSIDY_PAID/ON_HOLD/MISSED_REVIEW/CLOSED */
    private String status;
    private Long latestScreeningId;
    private Long latestDiagnosisId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
