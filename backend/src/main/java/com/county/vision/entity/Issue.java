package com.county.vision.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity @Table(name = "issues")
@Getter @Setter
public class Issue {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long studentId;
    private Long screeningId;
    /** ABSENT/PHONE_UNREACHABLE/IMPORT_ERROR/RESULT_DIVERGENCE/SELF_GLASSES/SUBSIDY_DISPUTE/GLASSES_DISCOMFORT/MISSED_REVIEW */
    private String issueType;
    private String title;
    private String description;
    /** OPEN/PROCESSING/RESOLVED */
    private String status;
    private String resolution;
    private Long createdBy;
    private String createdByName;
    private String createdByRole;
    private Long resolvedBy;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;
}
