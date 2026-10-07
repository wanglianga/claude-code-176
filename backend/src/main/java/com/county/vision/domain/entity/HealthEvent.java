package com.county.vision.domain.entity;

import com.county.vision.common.BaseEntity;
import com.county.vision.domain.enums.EventType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 学生健康管理时间线事件：
 * 把筛查、告知、复诊、处方、配镜、补助、复查、学校干预连成一条完整记录
 */
@Getter
@Setter
@Entity
@Table(name = "health_event", indexes = @Index(columnList = "student_id, eventTime"))
public class HealthEvent extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Student student;

    @Enumerated(EnumType.STRING)
    private EventType type;

    private LocalDateTime eventTime;

    @Column(length = 1000)
    private String content;

    /** 操作角色/机构/人 */
    private String actor;
}
