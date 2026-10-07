package com.county.vision.domain.entity;

import com.county.vision.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "school")
public class School extends BaseEntity {
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    private Region region;

    private String address;
    private String contactPhone;
    /** 健康老师姓名（冗余展示） */
    private String healthTeacher;
}
