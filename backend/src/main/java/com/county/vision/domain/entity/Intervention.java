package com.county.vision.domain.entity;

import com.county.vision.common.BaseEntity;
import com.county.vision.domain.enums.InterventionType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/** 学校日常干预：座位调整、户外活动、家长提醒、用眼指导 */
@Getter
@Setter
@Entity
@Table(name = "intervention")
public class Intervention extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Student student;

    @Enumerated(EnumType.STRING)
    private InterventionType type;

    private LocalDate execDate;
    private String executor;

    @Column(length = 500)
    private String content;

    /** 是否执行完成 */
    private Boolean executed = true;
}
