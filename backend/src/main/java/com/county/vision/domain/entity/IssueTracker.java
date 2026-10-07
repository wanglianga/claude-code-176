package com.county.vision.domain.entity;

import com.county.vision.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 异常事项跟进台账：请假、电话不通、导入错误、结果差异大、外配眼镜、
 * 补助争议、配镜不适、复查未到场 —— 每项均须闭环
 */
@Getter
@Setter
@Entity
@Table(name = "issue_tracker")
public class IssueTracker extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Student student;

    /** 对应 ScreenFlag 名称 */
    private String issueType;

    @Column(length = 500)
    private String description;

    /** OPEN 待处理 / RESOLVED 已闭环 */
    private String status = "OPEN";

    private String raisedBy;
    private LocalDateTime raisedAt;

    private String resolvedBy;
    private LocalDateTime resolvedAt;

    @Column(length = 500)
    private String resolveNote;
}
