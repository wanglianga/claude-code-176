package com.county.vision.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity @Table(name = "organizations")
@Getter @Setter
public class Organization {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    /** SCHOOL/SCREENING_AGENCY/HOSPITAL/OPTOMETRY_CLINIC/OPTICAL_SHOP/EDU_BUREAU/HEALTH_COMMISSION */
    private String orgType;
    private String area;
    private String contactName;
    private String contactPhone;
    private String address;
    private LocalDateTime createdAt;
}
