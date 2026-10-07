package com.county.vision.domain.entity;

import com.county.vision.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 配镜记录（合作机构配镜 或 家长自行外配登记） */
@Getter
@Setter
@Entity
@Table(name = "glasses_record")
public class GlassesRecord extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Student student;

    private String opticianOrg;
    private LocalDate glassesDate;

    /** 是否合作机构配镜（false=家长自行外配） */
    private Boolean partnerOptician = true;

    private BigDecimal amount;

    /** 处方参数留痕 */
    @Column(length = 300)
    private String lensInfo;

    /** 配镜后是否不适 */
    private Boolean discomfort = false;
    @Column(length = 500)
    private String discomfortDetail;

    /** 是否已处理不适（复诊调整） */
    private Boolean discomfortResolved = false;
}
