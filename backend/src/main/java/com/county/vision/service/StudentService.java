package com.county.vision.service;

import com.county.vision.common.BusinessException;
import com.county.vision.domain.entity.*;
import com.county.vision.domain.enums.EventType;
import com.county.vision.domain.enums.ScreenFlag;
import com.county.vision.domain.repository.*;
import com.county.vision.dto.RecordDtos;
import com.county.vision.dto.StudentDtos;
import com.county.vision.security.LoginUser;
import com.county.vision.security.SecurityUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class StudentService {

    private final StudentRepository studentRepo;
    private final SchoolRepository schoolRepo;
    private final GradeClassRepository classRepo;
    private final TimelineService timeline;
    private final IssueTrackerRepository issueRepo;

    public StudentService(StudentRepository studentRepo, SchoolRepository schoolRepo,
                          GradeClassRepository classRepo, TimelineService timeline,
                          IssueTrackerRepository issueRepo) {
        this.studentRepo = studentRepo;
        this.schoolRepo = schoolRepo;
        this.classRepo = classRepo;
        this.timeline = timeline;
        this.issueRepo = issueRepo;
    }

    /* ---------------- 查询 ---------------- */

    @Transactional(readOnly = true)
    public List<StudentDtos.StudentListItem> search(Long schoolId, String grade, Long regionId,
                                                    String stage, String keyword, boolean onlyImportError) {
        LoginUser me = SecurityUtils.current();
        // 学校角色只能看本校
        if (me.getSchoolId() != null) {
            schoolId = me.getSchoolId();
        }
        var stageEnum = stage == null || stage.isBlank() ? null : Enum.valueOf(
                com.county.vision.domain.enums.StudentStage.class, stage);
        List<Student> list = studentRepo.search(schoolId, blankToNull(grade), regionId,
                stageEnum, blankToNull(keyword), onlyImportError);
        return list.stream().map(this::toListItem).toList();
    }

    private StudentDtos.StudentListItem toListItem(Student s) {
        StudentDtos.StudentListItem i = new StudentDtos.StudentListItem();
        i.setId(s.getId());
        i.setStudentNo(s.getStudentNo());
        i.setName(s.getName());
        i.setGender(s.getGender());
        i.setBirthDate(s.getBirthDate());
        i.setSchoolId(s.getSchool().getId());
        i.setSchoolName(s.getSchool().getName());
        i.setRegionName(s.getSchool().getRegion() != null ? s.getSchool().getRegion().getName() : null);
        i.setGrade(s.getGradeClass().getGrade());
        i.setClassName(s.getGradeClass().getClassName());
        i.setParentName(s.getParentName());
        i.setParentPhone(s.getParentPhone());
        i.setStage(s.getStage().name());
        i.setStageLabel(s.getStage().getLabel());
        i.setFlags(TimelineService.splitFlags(s.getFlags()));
        i.setFlagLabels(TimelineService.splitFlags(s.getFlags()).stream().map(TimelineService::labelOf).toList());
        i.setImportError(s.getImportError());
        i.setScreenYear(s.getScreenYear());
        return i;
    }

    /* ---------------- 手工新增/编辑 ---------------- */

    @Transactional
    public Student saveStudent(StudentDtos.StudentSaveRequest req) {
        School school = resolveSchool(req.getSchoolId());
        GradeClass gc = resolveClass(school, req.getGrade(), req.getClassName());

        Student s = req.getId() == null ? new Student()
                : studentRepo.findById(req.getId()).orElseThrow(() -> new BusinessException("学生不存在"));
        if (req.getId() == null) {
            s.setScreenYear(LocalDate.now().getYear());
            s.setImportBatch("manual-" + System.currentTimeMillis());
        }
        s.setSchool(school);
        s.setGradeClass(gc);
        s.setStudentNo(req.getStudentNo());
        s.setName(req.getName());
        s.setGender(req.getGender());
        s.setBirthDate(req.getBirthDate());
        s.setParentName(req.getParentName());
        s.setParentPhone(req.getParentPhone());
        s.setPriorVisionArchive(req.getPriorVisionArchive());
        Student saved = studentRepo.save(s);
        timeline.event(saved, com.county.vision.domain.enums.EventType.IMPORT,
                req.getId() == null ? "手工新增学生档案" : "编辑学生档案");
        return saved;
    }

    /* ---------------- 批量导入 ---------------- */

    @Transactional
    public StudentDtos.ImportResult importStudents(MultipartFile file, Long schoolIdParam) {
        School school = resolveSchool(schoolIdParam);
        String filename = Optional.ofNullable(file.getOriginalFilename()).orElse("").toLowerCase();
        List<Map<String, String>> rows = filename.endsWith(".csv")
                ? parseCsv(file) : parseExcel(file);

        String batch = "B" + System.currentTimeMillis();
        StudentDtos.ImportResult result = new StudentDtos.ImportResult();
        result.setTotal(rows.size());
        result.setBatch(batch);
        result.setErrors(new ArrayList<>());
        int imported = 0, updated = 0, errorCount = 0;
        int year = LocalDate.now().getYear();

        int rowNum = 1;
        for (Map<String, String> row : rows) {
            rowNum++;
            String grade = trim(row.get("年级"));
            String className = trim(row.get("班级"));
            String name = trim(row.get("姓名"));
            String phone = trim(row.get("家长电话"));
            String studentNo = trim(row.get("学号"));

            if (name == null || grade == null || className == null) {
                result.getErrors().add(new StudentDtos.ImportErrorRow(rowNum, row.toString(),
                        "缺少必填项（年级/班级/姓名）"));
                errorCount++;
                continue;
            }
            String rowProblem = validateSoft(row, phone);

            try {
                GradeClass gc = resolveClass(school, grade, className);
                Student s = studentNo != null
                        ? studentRepo.findByStudentNoAndScreenYear(studentNo, year).orElse(null)
                        : null;
                boolean isNew = s == null;
                if (isNew) {
                    s = new Student();
                    s.setScreenYear(year);
                    s.setImportBatch(batch);
                }
                s.setSchool(school);
                s.setGradeClass(gc);
                s.setStudentNo(studentNo);
                s.setName(name);
                s.setGender(trim(row.get("性别")));
                s.setBirthDate(parseDate(trim(row.get("出生日期"))));
                s.setParentName(trim(row.get("家长姓名")));
                s.setParentPhone(phone);
                s.setPriorVisionArchive(trim(row.get("既往视力档案")));

                if (rowProblem != null) {
                    s.setImportError(true);
                    s.setImportErrorDetail(rowProblem);
                    Student saved = studentRepo.save(s);
                    timeline.raiseIssue(saved, ScreenFlag.IMPORT_ERROR,
                            "批量导入信息异常（第" + rowNum + "行）：" + rowProblem);
                } else {
                    s.setImportError(false);
                    s.setImportErrorDetail(null);
                    studentRepo.save(s);
                }
                timeline.event(s, EventType.IMPORT,
                        "名单导入（批次 " + batch + "，第" + rowNum + "行）");
                if (isNew) imported++; else updated++;
            } catch (Exception ex) {
                result.getErrors().add(new StudentDtos.ImportErrorRow(rowNum, row.toString(), ex.getMessage()));
                errorCount++;
            }
        }
        result.setImported(imported);
        result.setUpdated(updated);
        result.setErrorCount(errorCount);
        return result;
    }

    /** 修正导入错误信息并闭环该异常事项 */
    @Transactional
    public void fixImportError(Long studentId, RecordDtos.FixImportRequest req) {
        Student s = studentRepo.findById(studentId).orElseThrow(() -> new BusinessException("学生不存在"));
        if (req.getParentPhone() != null) s.setParentPhone(req.getParentPhone());
        if (req.getName() != null) s.setName(req.getName());
        if (req.getStudentNo() != null) s.setStudentNo(req.getStudentNo());
        if (req.getGender() != null) s.setGender(req.getGender());
        if (req.getGrade() != null && req.getClassName() != null) {
            s.setGradeClass(resolveClass(s.getSchool(), req.getGrade(), req.getClassName()));
        }
        boolean phoneOk = s.getParentPhone() != null && s.getParentPhone().matches("\\d{11}");
        if (phoneOk) {
            s.setImportError(false);
            s.setImportErrorDetail(null);
        }
        studentRepo.save(s);
        // 闭环该学生未处理的导入错误事项
        issueRepo.findByStudentIdOrderByRaisedAtDesc(studentId).stream()
                .filter(i -> "IMPORT_ERROR".equals(i.getIssueType()) && "OPEN".equals(i.getStatus()))
                .findFirst()
                .ifPresent(i -> timeline.resolveIssue(i.getId(),
                        "信息已修正：" + (req.getParentPhone() != null ? "家长电话 " + req.getParentPhone() : "档案信息")));
        timeline.event(s, EventType.RESOLVE, "班主任修正批量导入的错误信息");
    }

    /* ---------------- 解析 ---------------- */

    private static final List<String> HEADERS = List.of(
            "年级", "班级", "学号", "姓名", "性别", "出生日期", "家长姓名", "家长电话", "既往视力档案");

    private List<Map<String, String>> parseExcel(MultipartFile file) {
        try (Workbook wb = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            List<Map<String, String>> rows = new ArrayList<>();
            DataFormatter fmt = new DataFormatter();
            int last = sheet.getLastRowNum();
            if (last < 1) return rows;
            Row header = sheet.getRow(0);
            Map<Integer, String> col = new HashMap<>();
            for (Cell c : header) {
                col.put(c.getColumnIndex(), fmt.formatCellValue(c).trim());
            }
            for (int r = 1; r <= last; r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                Map<String, String> m = new HashMap<>();
                col.forEach((idx, name) -> {
                    Cell cell = row.getCell(idx);
                    m.put(name, cell == null ? "" : fmt.formatCellValue(cell).trim());
                });
                if (m.values().stream().anyMatch(v -> v != null && !v.isBlank())) rows.add(m);
            }
            return rows;
        } catch (Exception e) {
            throw new BusinessException("Excel 解析失败：" + e.getMessage());
        }
    }

    private List<Map<String, String>> parseCsv(MultipartFile file) {
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            List<Map<String, String>> rows = new ArrayList<>();
            String headerLine = br.readLine();
            if (headerLine == null) return rows;
            String[] heads = splitCsvLine(headerLine);
            String line;
            while ((line = br.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] vals = splitCsvLine(line);
                Map<String, String> m = new HashMap<>();
                for (int i = 0; i < heads.length && i < vals.length; i++) {
                    m.put(heads[i].trim(), vals[i].trim());
                }
                rows.add(m);
            }
            return rows;
        } catch (Exception e) {
            throw new BusinessException("CSV 解析失败：" + e.getMessage());
        }
    }

    private String[] splitCsvLine(String line) {
        // 支持带引号的简单 CSV
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean quote = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (quote && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    cur.append('"');
                    i++;
                } else quote = !quote;
            } else if (ch == ',' && !quote) {
                out.add(cur.toString());
                cur.setLength(0);
            } else cur.append(ch);
        }
        out.add(cur.toString());
        return out.toArray(new String[0]);
    }

    private String validateSoft(Map<String, String> row, String phone) {
        List<String> problems = new ArrayList<>();
        if (phone == null || !phone.matches("\\d{11}")) {
            problems.add("家长电话格式不正确（应为11位手机号）");
        }
        if (trim(row.get("性别")) == null) problems.add("缺性别");
        if (parseDate(trim(row.get("出生日期"))) == null && trim(row.get("出生日期")) != null) {
            problems.add("出生日期格式无法识别");
        }
        return problems.isEmpty() ? null : String.join("；", problems);
    }

    private LocalDate parseDate(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            String norm = s.replace("/", "-").replace(".", "-");
            if (norm.matches("\\d{4}-\\d{1,2}-\\d{1,2}")) {
                return LocalDate.parse(norm, DateTimeFormatter.ofPattern("yyyy-M-d"));
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private School resolveSchool(Long schoolIdParam) {
        LoginUser me = SecurityUtils.current();
        Long sid = me.getSchoolId() != null ? me.getSchoolId() : schoolIdParam;
        if (sid == null) {
            throw new BusinessException("请指定学校（或使用学校账号登录后导入）");
        }
        return schoolRepo.findById(sid).orElseThrow(() -> new BusinessException("学校不存在"));
    }

    private GradeClass resolveClass(School school, String grade, String className) {
        return classRepo.findBySchoolAndGradeAndClassName(school, grade, className)
                .orElseGet(() -> {
                    GradeClass gc = new GradeClass();
                    gc.setSchool(school);
                    gc.setGrade(grade);
                    gc.setClassName(className);
                    return classRepo.save(gc);
                });
    }

    private String trim(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }

    public List<String> headerNames() {
        return HEADERS;
    }
}
