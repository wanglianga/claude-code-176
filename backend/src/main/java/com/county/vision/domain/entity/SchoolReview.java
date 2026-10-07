package com.county.vision.domain.entity;

import com.county.vision.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** 学校健康老师对筛查异常学生的复核 */
@Getter
@Setter
@Entity
@Table(name = "school_review")
public class SchoolReview extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Student student;

    private String reviewer;
    private LocalDateTime reviewTime;

    /** 复核结论：CONFIRM 确认异常 / REJECT 误判排除 / RETEST 建议复测 */
    private String result;

    @Column(length = 500)
    private String opinion;
}
