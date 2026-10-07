package com.county.vision.domain.entity;

import com.county.vision.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/** 医院/门诊复诊结果，回写学生档案 */
@Getter
@Setter
@Entity
@Table(name = "visit_record")
public class VisitRecord extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Student student;

    /** 县医院 / 合作眼科门诊名称 */
    private String orgName;
    private String doctor;
    private LocalDate visitDate;

    /** 是否真性近视 */
    private Boolean trueMyopia;

    /** 是否需要散瞳 */
    private Boolean needMydriasis;

    /** 处方参数：球镜/柱镜/轴位/瞳距（左右） */
    private String rxSphereLeft;
    private String rxSphereRight;
    private String rxCylinderLeft;
    private String rxCylinderRight;
    private String rxAxisLeft;
    private String rxAxisRight;
    private String pd;

    /** 诊断结论 */
    @Column(length = 500)
    private String diagnosis;

    /** 复查周期（月），如 3、6 */
    private Integer reviewCycleMonths;

    /** 是否符合配镜补助（医生初判） */
    private Boolean subsidyEligible;

    /** 与筛查结果差异是否较大 */
    private Boolean bigDifference = false;
    @Column(length = 500)
    private String differenceNote;
}
