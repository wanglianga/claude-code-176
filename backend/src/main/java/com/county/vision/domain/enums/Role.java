package com.county.vision.domain.enums;

/**
 * 系统角色
 */
public enum Role {
    GOV_EDU("县教体部门"),
    GOV_HEALTH("县卫健部门"),
    SCHOOL_DOCTOR("学校健康老师"),
    TEACHER("班主任"),
    SCREEN_ORG("筛查机构"),
    HOSPITAL("县医院"),
    CLINIC("合作眼科门诊"),
    PARENT("家长"),
    ADMIN("平台管理员");

    private final String label;

    Role(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
