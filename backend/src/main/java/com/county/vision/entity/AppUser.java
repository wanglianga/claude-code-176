package com.county.vision.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity @Table(name = "app_users")
@Getter @Setter
public class AppUser {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String username;
    private String passwordHash;
    private String realName;
    /** PLATFORM_ADMIN/EDU_BUREAU/HEALTH_COMMISSION/SCHOOL_HEALTH_TEACHER/HEAD_TEACHER/SCREENER/DOCTOR/OPTOMETRIST/OPTICIAN/PARENT */
    private String role;
    private String phone;
    private Long organizationId;
    private Boolean active;
    private LocalDateTime createdAt;
}
