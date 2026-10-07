package com.county.vision.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity @Table(name = "parent_confirmations")
@Getter @Setter
public class ParentConfirmation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long screeningId;
    private Long studentId;
    /** PENDING/CONFIRMED/ON_HOLD */
    private String status;
    /** COUNTY_HOSPITAL/PARTNER_CLINIC/POSTPONE */
    private String choice;
    private String reason;
    private LocalDateTime notifiedAt;
    private LocalDateTime confirmedAt;
    private String decidedByName;
    /** PENDING/APPROVED/REJECTED */
    private String schoolReviewStatus;
    private Long schoolReviewBy;
    private LocalDateTime schoolReviewAt;
    private String schoolReviewNote;
    private LocalDateTime createdAt;
}
