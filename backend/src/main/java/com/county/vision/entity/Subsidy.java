package com.county.vision.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity @Table(name = "subsidies")
@Getter @Setter
public class Subsidy {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long studentId;
    private Long glassesId;
    private Long diagnosisId;
    private BigDecimal amount;
    /** PENDING/APPROVED/REJECTED/DISPUTED/PAID */
    private String status;
    private String applyNote;
    private String decisionNote;
    private Long decidedBy;
    private String decidedByName;
    private LocalDateTime decidedAt;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
}
