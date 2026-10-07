package com.county.vision.controller;

import com.county.vision.common.GlobalExceptionHandler;
import com.county.vision.common.LoginUser;
import com.county.vision.entity.*;
import com.county.vision.repo.*;
import com.county.vision.service.TimelineService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 学校日常干预：座位调整、户外活动、用眼提醒、家长提醒、定期检查。
 *  与医疗复诊连接——干预执行与效果进入学生健康管理记录 */
@RestController
@RequestMapping("/api/interventions")
public class InterventionController {

    private final InterventionRepository repo;
    private final StudentRepository studentRepo;
    private final SchoolClassRepository classRepo;
    private final OrganizationRepository orgRepo;
    private final TimelineService timeline;

    public InterventionController(InterventionRepository repo, StudentRepository studentRepo,
                                  SchoolClassRepository classRepo, OrganizationRepository orgRepo,
                                  TimelineService timeline) {
        this.repo = repo;
        this.studentRepo = studentRepo;
        this.classRepo = classRepo;
        this.orgRepo = orgRepo;
        this.timeline = timeline;
    }

    @GetMapping
    public List<Map<String, Object>> list(@RequestParam(required = false) Long studentId,
                                          @RequestParam(required = false) Long classId,
                                          @RequestParam(required = false) Long schoolId,
                                          @RequestParam(required = false) String status,
                                          @RequestParam(required = false) String type) {
        List<Intervention> all;
        if (studentId != null) all = repo.findByStudentIdOrderByCreatedAtDesc(studentId);
        else if (classId != null) all = repo.findByClassIdOrderByCreatedAtDesc(classId);
        else if (schoolId != null && status != null) all = repo.findBySchoolIdAndStatus(schoolId, status);
        else all = repo.findAll();
        List<Map<String, Object>> out = new ArrayList<>();
        for (Intervention iv : all) {
            if (status != null && !status.equals(iv.getStatus())) continue;
            if (type != null && !type.equals(iv.getType())) continue;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("intervention", iv);
            if (iv.getStudentId() != null) {
                studentRepo.findById(iv.getStudentId()).ifPresent(s -> {
                    m.put("studentName", s.getName());
                    m.put("grade", s.getGrade());
                });
            }
            if (iv.getSchoolId() != null) {
                orgRepo.findById(iv.getSchoolId()).ifPresent(o -> m.put("schoolName", o.getName()));
            }
            out.add(m);
        }
        return out;
    }

    public record InterventionReq(String type, String title, String detail, LocalDate plannedDate,
                                  Long studentId, Long classId, List<Long> studentIds) {}

    /** 创建干预；studentIds 非空时按学生批量创建（如全班异常学生座位调整） */
    @PostMapping
    @Transactional
    public Map<String, Object> create(@RequestBody InterventionReq req) {
        LoginUser me = LoginUser.get();
        if (req.type() == null || req.title() == null) {
            throw GlobalExceptionHandler.badRequest("干预类型与标题必填");
        }
        List<Long> ids = req.studentIds() != null && !req.studentIds().isEmpty()
                ? req.studentIds()
                : (req.studentId() != null ? List.of(req.studentId()) : List.of());
        if (ids.isEmpty()) throw GlobalExceptionHandler.badRequest("请选择干预对象学生");

        List<Long> created = new ArrayList<>();
        for (Long sid : ids) {
            Student s = studentRepo.findById(sid).orElse(null);
            if (s == null) continue;
            Intervention iv = new Intervention();
            iv.setType(req.type());
            iv.setTitle(req.title());
            iv.setDetail(req.detail());
            iv.setPlannedDate(req.plannedDate() == null ? LocalDate.now() : req.plannedDate());
            iv.setStatus("PLANNED");
            iv.setStudentId(sid);
            iv.setClassId(s.getClassId());
            iv.setSchoolId(s.getSchoolId());
            iv.setOperatorId(me.id());
            repo.save(iv);
            created.add(iv.getId());
            timeline.event(sid, "INTERVENTION_PLANNED", typeName(req.type()) + "已安排",
                    req.title() + (req.detail() == null ? "" : "：" + req.detail()),
                    me.realName(), me.role(), "INTERVENTION", iv.getId());
        }
        return Map.of("created", created.size(), "ids", created);
    }

    public record ExecuteReq(String effectNote) {}

    /** 班主任/健康老师执行干预 */
    @PostMapping("/{id}/execute")
    @Transactional
    public Intervention execute(@PathVariable Long id, @RequestBody(required = false) ExecuteReq req) {
        Intervention iv = repo.findById(id).orElseThrow(() -> GlobalExceptionHandler.notFound("干预记录不存在"));
        iv.setStatus("DONE");
        iv.setExecutedAt(LocalDateTime.now());
        if (req != null && req.effectNote() != null) iv.setEffectNote(req.effectNote());
        Intervention saved = repo.save(iv);
        timeline.event(iv.getStudentId(), "INTERVENTION_DONE", typeName(iv.getType()) + "已执行",
                iv.getTitle() + (req != null && req.effectNote() != null ? "；效果记录：" + req.effectNote() : ""),
                LoginUser.get().realName(), LoginUser.get().role(), "INTERVENTION", iv.getId());
        return saved;
    }

    /** 卫健/教育部门抽查核验干预落实情况 */
    @PostMapping("/{id}/verify")
    @Transactional
    public Intervention verify(@PathVariable Long id, @RequestBody(required = false) ExecuteReq req) {
        Intervention iv = repo.findById(id).orElseThrow(() -> GlobalExceptionHandler.notFound("干预记录不存在"));
        if (!"DONE".equals(iv.getStatus())) throw GlobalExceptionHandler.badRequest("仅已执行的干预可核验");
        iv.setStatus("VERIFIED");
        if (req != null && req.effectNote() != null) {
            iv.setEffectNote((iv.getEffectNote() == null ? "" : iv.getEffectNote() + " | ")
                    + "核验意见：" + req.effectNote());
        }
        Intervention saved = repo.save(iv);
        timeline.event(iv.getStudentId(), "INTERVENTION_VERIFIED", "干预执行已核验",
                req == null || req.effectNote() == null ? "部门核验通过" : req.effectNote(),
                LoginUser.get().realName(), LoginUser.get().role(), "INTERVENTION", iv.getId());
        return saved;
    }

    private String typeName(String t) {
        return switch (t) {
            case "SEAT_ADJUSTMENT" -> "座位调整";
            case "OUTDOOR_ACTIVITY" -> "户外活动";
            case "EYE_HABIT_REMINDER" -> "用眼习惯提醒";
            case "PARENT_NOTICE" -> "家长提醒";
            case "PERIODIC_CHECK" -> "定期检查";
            default -> t;
        };
    }
}
