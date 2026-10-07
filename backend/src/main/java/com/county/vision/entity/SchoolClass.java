package com.county.vision.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity @Table(name = "school_classes")
@Getter @Setter
public class SchoolClass {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long schoolId;
    private String grade;
    private String className;
    private Long headTeacherId;
    private String headTeacherName;
    private String headTeacherPhone;
    private LocalDateTime createdAt;
}
