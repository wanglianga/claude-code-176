package com.county.vision.controller;

import com.county.vision.common.GlobalExceptionHandler;
import com.county.vision.common.LoginUser;
import com.county.vision.entity.Issue;
import com.county.vision.repo.IssueRepository;
import com.county.vision.repo.StudentRepository;
import com.county.vision.service.TimelineService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 异常情况清单：请假、电话无法接通、导入错误、结果差异、自行外配、
 *  补助争议、配镜不适、半年复查未到场——全部挂到学生健康管理记录上持续跟进 */
@RestController
@RequestMapping("/api/issues")
public class IssueController {

    private final IssueRepository repo;
    private final StudentRepository studentRepo;
    private final TimelineService timeline;

    public IssueController(IssueRepository repo, StudentRepository studentRepo, TimelineService timeline) {
        this.repo = repo;
        this.studentRepo = studentRepo;
        this.timeline = timeline;
    }

    @GetMapping
    public List<Map<String, Object>> list(@RequestParam(required = false) String status,
                                          @RequestParam(required = false) String issueType,
                                          @RequestParam(required = false) Long studentId) {
        List<Issue> base;
        if (studentId != null) base = repo.findByStudentIdOrderByCreatedAtDesc(studentId);
        else if (issueType != null && status != null) base = repo.findByIssueTypeAndStatus(issueType, status);
        else if (status != null) base = repo.findByStatusOrderByCreatedAtDesc(status);
        else base = repo.findAll();
        List<Map<String, Object>> out = new ArrayList<>();
        for (Issue i : base) {
            if (issueType != null && !issueType.equals(i.getIssueType())) continue;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("issue", i);
            if (i.getStudentId() != null) {
                studentRepo.findById(i.getStudentId()).ifPresent(s -> {
                    m.put("studentName", s.getName());
                    m.put("grade", s.getGrade());
                    m.put("schoolId", s.getSchoolId());
                });
            }
            out.add(m);
        }
        return out;
    }

    @PostMapping
    @Transactional
    public Issue create(@RequestBody Issue body) {
        if (body.getStudentId() == null || body.getIssueType() == null || body.getTitle() == null) {
            throw GlobalExceptionHandler.badRequest("学生、问题类型、标题必填");
        }
        LoginUser me = LoginUser.get();
        body.setStatus("OPEN");
        body.setCreatedBy(me.id());
        body.setCreatedByName(me.realName());
        body.setCreatedByRole(me.role());
        Issue saved = repo.save(body);
        timeline.event(body.getStudentId(), "ISSUE_OPEN", "问题登记：" + typeName(body.getIssueType()),
                body.getTitle() + (body.getDescription() == null ? "" : "：" + body.getDescription()),
                me.realName(), me.role(), "ISSUE", saved.getId());
        return saved;
    }

    public record ResolveReq(String resolution, boolean resolve) {}

    @PostMapping("/{id}/process")
    public Issue process(@PathVariable Long id) {
        Issue i = repo.findById(id).orElseThrow(() -> GlobalExceptionHandler.notFound("问题不存在"));
        i.setStatus("PROCESSING");
        return repo.save(i);
    }

    @PostMapping("/{id}/resolve")
    @Transactional
    public Map<String, Object> resolve(@PathVariable Long id, @RequestBody ResolveReq req) {
        Issue i = repo.findById(id).orElseThrow(() -> GlobalExceptionHandler.notFound("问题不存在"));
        LoginUser me = LoginUser.get();
        i.setStatus("RESOLVED");
        i.setResolution(req.resolution());
        i.setResolvedBy(me.id());
        i.setResolvedAt(LocalDateTime.now());
        repo.save(i);
        timeline.event(i.getStudentId(), "ISSUE_RESOLVED", "问题已处理：" + typeName(i.getIssueType()),
                req.resolution(), me.realName(), me.role(), "ISSUE", i.getId());
        return Map.of("id", i.getId(), "status", "RESOLVED");
    }

    static String typeName(String t) {
        return switch (t) {
            case "ABSENT" -> "筛查请假";
            case "PHONE_UNREACHABLE" -> "联系不畅/暂缓";
            case "IMPORT_ERROR" -> "导入信息错误";
            case "RESULT_DIVERGENCE" -> "筛查复诊差异";
            case "SELF_GLASSES" -> "自行外配眼镜";
            case "SUBSIDY_DISPUTE" -> "补助资格争议";
            case "GLASSES_DISCOMFORT" -> "配镜不适";
            case "MISSED_REVIEW" -> "复查未到场";
            default -> t;
        };
    }
}
