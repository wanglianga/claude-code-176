package com.county.vision.domain.enums;

/**
 * 学校干预措施类型
 */
public enum InterventionType {
    SEAT_ADJUST("座位调整"),
    OUTDOOR_ACTIVITY("户外活动"),
    PARENT_REMIND("家长提醒"),
    BEHAVIOR_GUIDE("用眼行为指导"),
    OTHER("其他干预");

    private final String label;

    InterventionType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
