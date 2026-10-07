package com.county.vision.controller;

import com.county.vision.common.GlobalExceptionHandler;
import com.county.vision.common.LoginUser;
import com.county.vision.entity.Issue;
import com.county.vision.entity.Organization;
import com.county.vision.entity.SchoolClass;
import com.county.vision.entity.Student;
import com.county.vision.entity.TimelineEvent;
import com.county.vision.repo.IssueRepository;
import com.county.vision.repo.OrganizationRepository;
import com.county.vision.repo.SchoolClassRepository;
import com.county.vision.repo.StudentRepository;
import com.county.vision.repo.TimelineEventRepository;
import com.county.vision.service.TimelineService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** 学生档案、班主任批量导入名单（年级班级/既往视力档案/家长联系方式） */
@RestController
@RequestMapping("/api/students")
public class StudentController {

    private final StudentRepository studentRepo;
    private final SchoolClassRepository classRepo;
    private final OrganizationRepository orgRepo;
    private final IssueRepository issueRepo;
    private final TimelineEventRepository timelineRepo;
    private final TimelineService timeline;

    public StudentController(StudentRepository studentRepo, SchoolClassRepository classRepo,
                             OrganizationRepository orgRepo, IssueRepository issueRepo,
                             TimelineEventRepository timelineRepo, TimelineService timeline) {
        this.studentRepo = studentRepo;
        this.classRepo = classRepo;
        this.orgRepo = orgRepo;
        this.issueRepo = issueRepo;
        this.timelineRepo = timelineRepo;
        this.timeline = timeline;
    }

    @GetMapping
    public Map<String, Object> list(@RequestParam(required = false) Long schoolId,
                                    @RequestParam(required = false) Long classId,
                                    @RequestParam(required = false) String grade,
                                    @RequestParam(required = false) String status,
                                    @RequestParam(required = false) String kw,
                                    @RequestParam(defaultValue = "1") int page,
                                    @RequestParam(defaultValue = "20") int size) {
        LoginUser me = LoginUser.get();
        // 学校账号只能看本校
        Long effectiveSchoolId = schoolId;
        Organization org = me.orgId() == null ? null : orgRepo.findById(me.orgId()).orElse(null);
        if (org != null && "SCHOOL".equals(org.getOrgType())) {
            effectiveSchoolId = org.getId();
        }
        Page<Student> p = studentRepo.search(effectiveSchoolId, classId, grade, status, kw,
                PageRequest.of(Math.max(0, page - 1), Math.min(200, size)));
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("total", p.getTotalElements());
        res.put("items", p.getContent());
        return res;
    }

    @GetMapping("/{id}")
    public Map<String, Object> detail(@PathVariable Long id) {
        Student s = studentRepo.findById(id).orElseThrow(() -> GlobalExceptionHandler.notFound("学生不存在"));
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("student", s);
        res.put("schoolName", s.getSchoolId() == null ? null :
                orgRepo.findById(s.getSchoolId()).map(Organization::getName).orElse(null));
        res.put("className", s.getClassId() == null ? null :
                classRepo.findById(s.getClassId()).map(c -> c.getGrade() + c.getClassName()).orElse(null));
        res.put("timeline", timelineRepo.findByStudentIdOrderByEventTimeDescIdDesc(id));
        res.put("issues", issueRepo.findByStudentIdOrderByCreatedAtDesc(id));
        return res;
    }

    @PostMapping
    public Student create(@RequestBody Student body) {
        if (body.getSchoolId() == null) {
            LoginUser me = LoginUser.get();
            body.setSchoolId(me.orgId());
        }
        if (body.getSchoolId() == null) throw GlobalExceptionHandler.badRequest("缺少学校");
        body.setStatus("PENDING_SCREENING");
        body.setCreatedAt(LocalDateTime.now());
        body.setUpdatedAt(LocalDateTime.now());
        if (body.getClassId() != null) {
            classRepo.findById(body.getClassId()).ifPresent(c -> body.setGrade(c.getGrade()));
        }
        Student s = studentRepo.save(body);
        timeline.event(s.getId(), "ARCHIVE_CREATE", "档案建立",
                "班主任导入学生档案：" + s.getName(), "系统", "HEAD_TEACHER", "STUDENT", s.getId());
        return s;
    }

    /** 手工修正批量导入错误的学生信息 */
    @PutMapping("/{id}")
    public Student update(@PathVariable Long id, @RequestBody Student body) {
        Student s = studentRepo.findById(id).orElseThrow(() -> GlobalExceptionHandler.notFound("学生不存在"));
        String oldPhone = s.getParentPhone();
        if (body.getName() != null) s.setName(body.getName());
        if (body.getGender() != null) s.setGender(body.getGender());
        if (body.getBirthDate() != null) s.setBirthDate(body.getBirthDate());
        if (body.getStudentNo() != null) s.setStudentNo(body.getStudentNo());
        if (body.getParentName() != null) s.setParentName(body.getParentName());
        if (body.getParentPhone() != null) s.setParentPhone(body.getParentPhone());
        if (body.getPastVisionNote() != null) s.setPastVisionNote(body.getPastVisionNote());
        if (body.getClassId() != null) {
            s.setClassId(body.getClassId());
            classRepo.findById(body.getClassId()).ifPresent(c -> s.setGrade(c.getGrade()));
        }
        s.setUpdatedAt(LocalDateTime.now());
        Student saved = studentRepo.save(s);
        if (!Objects.equals(oldPhone, saved.getParentPhone()) || (body.getName() != null)) {
            timeline.event(id, "IMPORT_FIX", "导入信息修正",
                    "修正家长联系方式/名单信息：" + saved.getParentPhone(),
                    LoginUser.get().realName(), LoginUser.get().role(), "STUDENT", id);
        }
        return saved;
    }

    /** CSV 批量导入。表头：姓名,性别,出生日期,年级,班级,学籍号,家长姓名,家长电话,既往视力档案 */
    @PostMapping("/import")
    public Map<String, Object> importCsv(@RequestParam("file") MultipartFile file,
                                         @RequestParam(required = false) Long schoolId) {
        LoginUser me = LoginUser.get();
        Long sid = schoolId;
        if (sid == null) sid = me.orgId();
        if (sid == null) throw GlobalExceptionHandler.badRequest("请选择学校");
        Long finalSid = sid;
        Organization school = orgRepo.findById(sid)
                .orElseThrow(() -> GlobalExceptionHandler.badRequest("学校不存在"));

        List<Map<String, String>> rows = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        int lineNo = 0;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                lineNo++;
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] c = parseCsvLine(line);
                if (lineNo == 1 && c[0].contains("姓名")) continue; // 表头
                if (c.length < 8) {
                    errors.add("第" + lineNo + "行列数不足（至少8列）");
                    continue;
                }
                Map<String, String> row = new LinkedHashMap<>();
                row.put("name", c[0].trim());
                row.put("gender", c[1].trim());
                row.put("birthDate", c[2].trim());
                row.put("grade", c[3].trim());
                row.put("className", c[4].trim());
                row.put("studentNo", c[5].trim());
                row.put("parentName", c[6].trim());
                row.put("parentPhone", c[7].trim());
                row.put("pastVisionNote", c.length > 8 ? c[8].trim() : "");
                rows.add(row);
            }
        } catch (Exception e) {
            throw GlobalExceptionHandler.badRequest("CSV 解析失败：" + e.getMessage());
        }

        int success = 0;
        for (int i = 0; i < rows.size(); i++) {
            Map<String, String> r = rows.get(i);
            try {
                String grade = r.get("grade");
                String className = r.get("className");
                if (grade.isEmpty() || className.isEmpty() || r.get("name").isEmpty()) {
                    errors.add("第" + (i + 2) + "行：姓名/年级/班级不能为空");
                    continue;
                }
                SchoolClass sc = classRepo.findBySchoolIdAndGrade(finalSid, grade).stream()
                        .filter(x -> className.equals(x.getClassName())).findFirst()
                        .orElseGet(() -> {
                            SchoolClass nc = new SchoolClass();
                            nc.setSchoolId(finalSid);
                            nc.setGrade(grade);
                            nc.setClassName(className);
                            return classRepo.save(nc);
                        });
                Student s = new Student();
                s.setName(r.get("name"));
                s.setGender(r.get("gender"));
                if (!r.get("birthDate").isEmpty()) {
                    try {
                        s.setBirthDate(LocalDate.parse(r.get("birthDate")));
                    } catch (Exception ignore) {
                        s.setBirthDate(null);
                    }
                }
                s.setSchoolId(finalSid);
                s.setClassId(sc.getId());
                s.setGrade(grade);
                s.setStudentNo(r.get("studentNo"));
                s.setParentName(r.get("parentName"));
                s.setParentPhone(r.get("parentPhone"));
                s.setPastVisionNote(r.get("pastVisionNote"));
                s.setStatus("PENDING_SCREENING");
                s.setCreatedAt(LocalDateTime.now());
                s.setUpdatedAt(LocalDateTime.now());
                studentRepo.save(s);
                timeline.event(s.getId(), "ARCHIVE_CREATE", "名单导入",
                        "班主任导入学生档案，既往档案：" + (r.get("pastVisionNote").isEmpty() ? "无" : r.get("pastVisionNote")),
                        me.realName(), me.role(), "STUDENT", s.getId());
                success++;
            } catch (Exception ex) {
                errors.add("第" + (i + 2) + "行导入异常：" + ex.getMessage());
            }
        }

        // 批量导入信息错误：若存在错误行，登记一条待处理问题，提醒学校核对
        if (!errors.isEmpty()) {
            Issue issue = new Issue();
            issue.setIssueType("IMPORT_ERROR");
            issue.setTitle(school.getName() + " 名单导入存在 " + errors.size() + " 行错误");
            issue.setDescription(String.join("；", errors.size() > 10 ? errors.subList(0, 10) : errors)
                    + (errors.size() > 10 ? " 等" : ""));
            issue.setStatus("OPEN");
            issue.setCreatedBy(me.id());
            issue.setCreatedByName(me.realName());
            issue.setCreatedByRole(me.role());
            issueRepo.save(issue);
        }

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("total", rows.size());
        res.put("success", success);
        res.put("failed", errors.size());
        res.put("errors", errors);
        return res;
    }

    /** 简易 CSV 行解析（支持引号包裹与逗号转义） */
    private String[] parseCsvLine(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean quote = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (quote && i + 1 < line.length() && line.charAt(i + 1) == '"') { sb.append('"'); i++; }
                else quote = !quote;
            } else if (ch == ',' && !quote) {
                out.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(ch);
            }
        }
        out.add(sb.toString());
        return out.toArray(new String[0]);
    }

    @GetMapping("/timeline/{id}")
    public List<TimelineEvent> timeline(@PathVariable Long id) {
        return timelineRepo.findByStudentIdOrderByEventTimeDescIdDesc(id);
    }

    static String fmt(LocalDateTime t) {
        return t == null ? "" : t.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }
}
