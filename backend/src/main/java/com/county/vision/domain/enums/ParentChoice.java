package com.county.vision.domain.enums;

/**
 * 家长对异常结果的处置选择
 */
public enum ParentChoice {
    COUNTY_HOSPITAL("县医院复诊"),
    PARTNER_CLINIC("合作眼科门诊复诊"),
    DEFER("暂缓处理");

    private final String label;

    ParentChoice(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
