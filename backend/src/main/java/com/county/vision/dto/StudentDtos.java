package com.county.vision.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class StudentDtos {

    @Data
    public static class StudentListItem {
        private Long id;
        private String studentNo;
        private String name;
        private String gender;
        private LocalDate birthDate;
        private Long schoolId;
        private String schoolName;
        private String regionName;
        private String grade;
        private String className;
        private String parentName;
        private String parentPhone;
        private String stage;
        private String stageLabel;
        private List<String> flags;
        private List<String> flagLabels;
        private Boolean importError;
        private Integer screenYear;
    }

    @Data
    public static class StudentDetail {
        private Long id;
        private String studentNo;
        private String name;
        private String gender;
        private LocalDate birthDate;
        private Long schoolId;
        private String schoolName;
        private String regionName;
        private Long classId;
        private String grade;
        private String className;
        private String parentName;
        private String parentPhone;
        private String priorVisionArchive;
        private Integer screenYear;
        private String stage;
        private String stageLabel;
        private List<String> flags;
        private List<String> flagLabels;
        private Boolean importError;
        private String importErrorDetail;
        private LocalDateTime createdAt;

        private Object latestScreen;
        private Object confirmation;
        private Object schoolReview;
        private List<?> visits;
        private List<?> glasses;
        private List<?> subsidies;
        private List<?> followups;
        private List<?> interventions;
        private List<?> issues;
        private List<?> events;
    }

    /** 导入模板行 / 手工新增 */
    @Data
    public static class StudentSaveRequest {
        private Long id;
        private Long schoolId;
        private String grade;
        private String className;
        private String studentNo;
        private String name;
        private String gender;
        private LocalDate birthDate;
        private String parentName;
        private String parentPhone;
        private String priorVisionArchive;
    }

    @Data
    @lombok.AllArgsConstructor
    public static class ImportErrorRow {
        private int row;
        private String raw;
        private String reason;
    }

    @Data
    public static class ImportResult {
        private int total;
        private int imported;
        private int updated;
        private int errorCount;
        private String batch;
        private List<ImportErrorRow> errors;
    }
}
