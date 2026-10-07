package com.county.vision.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity @Table(name = "revisit_diagnoses")
@Getter @Setter
public class RevisitDiagnosis {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long studentId;
    private Long referralId;
    private Long orgId;
    private Long doctorId;
    private String doctorName;
    private LocalDate visitDate;
    private Boolean trueMyopia;
    private Boolean needMydriasis;
    private Boolean mydriasisDone;
    /** NONE/LOW/MEDIUM/HIGH */
    private String degree;
    private BigDecimal rxSphOd;
    private BigDecimal rxSphOs;
    private BigDecimal rxCylOd;
    private BigDecimal rxCylOs;
    private Integer rxAxisOd;
    private Integer rxAxisOs;
    private BigDecimal pd;
    private Integer revisitCycleMonths;
    private Boolean subsidyEligible;
    private String subsidyNote;
    private String divergenceNote;
    private String advice;
    private LocalDateTime createdAt;
}
