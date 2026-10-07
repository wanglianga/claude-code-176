package com.county.vision.domain.entity;

import com.county.vision.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "grade_class",
        uniqueConstraints = @UniqueConstraint(columnNames = {"school_id", "grade", "class_name"}))
public class GradeClass extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private School school;

    /** 年级，如 一年级、七年级、高一 */
    private String grade;

    @Column(name = "class_name")
    private String className;
}
