package com.county.vision.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity @Table(name = "glasses")
@Getter @Setter
public class Glasses {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long studentId;
    private Long diagnosisId;
    private Long orgId;
    private String orgName;
    /** SUBSIDY_SHOP/SELF_EXTERNAL */
    private String source;
    /** ORDERED/FITTED/DISCOMFORT_REPORTED/READJUSTED/CONFIRMED_OK */
    private String status;
    private BigDecimal price;
    private BigDecimal subsidyAmount;
    private LocalDate fittedDate;
    private LocalDate discomfortDate;
    private String discomfortNote;
    private String readjustNote;
    private LocalDate confirmedDate;
    private LocalDateTime createdAt;
}
