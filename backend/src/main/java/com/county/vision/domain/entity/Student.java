package com.county.vision.domain.entity;

import com.county.vision.common.BaseEntity;
import com.county.vision.domain.enums.StudentStage;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "student")
public class Student extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private School school;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private GradeClass gradeClass;

    /** 学籍号/校内学号 */
    private String studentNo;

    private String name;

    /** 男 / 女 */
    private String gender;

    private LocalDate birthDate;

    private String parentName;

    private String parentPhone;

    /** 既往视力档案（导入的历史摘要文本） */
    @Column(length = 1000)
    private String priorVisionArchive;

    /** 本学年筛查年度，如 2026 */
    private Integer screenYear;

    @Enumerated(EnumType.STRING)
    private StudentStage stage = StudentStage.PENDING_SCREEN;

    /** 当前累计异常标记，逗号分隔 ScreenFlag 名称 */
    @Column(length = 500)
    private String flags = "";

    /** 数据来源批次（导入批次号） */
    private String importBatch;

    /** 名单是否存在导入错误待修正 */
    private Boolean importError = false;

    @Column(length = 500)
    private String importErrorDetail;
}
