package com.county.vision.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity @Table(name = "screening_batches")
@Getter @Setter
public class ScreeningBatch {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private Long schoolId;
    private String grade;
    private Long screenerOrgId;
    private LocalDate screeningDate;
    private String notes;
    private Long createdBy;
    private Integer totalCount;
    private Integer abnormalCount;
    private LocalDateTime createdAt;
}
