package com.county.vision.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity @Table(name = "referrals")
@Getter @Setter
public class Referral {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long studentId;
    private Long screeningId;
    private Long confirmationId;
    private Long targetOrgId;
    /** COUNTY_HOSPITAL/PARTNER_CLINIC */
    private String targetType;
    /** PENDING/COMPLETED/CANCELLED */
    private String status;
    private Long issuedBy;
    private LocalDateTime issuedAt;
    private LocalDateTime completedAt;
}
