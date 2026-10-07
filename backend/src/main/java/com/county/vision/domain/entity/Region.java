package com.county.vision.domain.entity;

import com.county.vision.common.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** 县域片区（如城区、东乡、西乡），用于片区维度趋势分析 */
@Getter
@Setter
@Entity
@Table(name = "region")
public class Region extends BaseEntity {
    private String name;
    private String code;
}
