package com.county.vision.domain.entity;

import com.county.vision.common.BaseEntity;
import com.county.vision.domain.enums.SubsidyStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 配镜补助申请与核准 */
@Getter
@Setter
@Entity
@Table(name = "subsidy_application")
public class SubsidyApplication extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    private GlassesRecord glasses;

    private BigDecimal applyAmount;
    private BigDecimal approvedAmount;

    @Enumerated(EnumType.STRING)
    private SubsidyStatus status = SubsidyStatus.ELIGIBLE;

    private String applicant;
    private LocalDateTime applyTime;
    private String approver;
    private LocalDateTime approveTime;

    /** 争议原因 / 审批意见 */
    @Column(length = 500)
    private String opinion;
}
