package com.county.vision.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity @Table(name = "screenings")
@Getter @Setter
public class Screening {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long batchId;
    private Long studentId;
    private Boolean absent;
    private String absentReason;
    private BigDecimal nakedOd;
    private BigDecimal nakedOs;
    private BigDecimal sphOd;
    private BigDecimal sphOs;
    private BigDecimal cylOd;
    private BigDecimal cylOs;
    private Integer axisOd;
    private Integer axisOs;
    private Boolean wearingGlasses;
    private BigDecimal hoursOutdoor;
    private BigDecimal screenHours;
    private Boolean readingDistanceOk;
    private Boolean eyeExerciseDaily;
    private Boolean parentalMyopia;
    private String questionnaireNote;
    private Boolean needRetest;
    private String deviceCalibNo;
    /** NORMAL/ABNORMAL/SUSPECTED */
    private String result;
    private Long screenerId;
    private LocalDateTime screeningTime;
    private LocalDateTime createdAt;
}
