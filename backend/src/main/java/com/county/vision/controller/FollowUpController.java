package com.county.vision.controller;

import com.county.vision.common.GlobalExceptionHandler;
import com.county.vision.common.LoginUser;
import com.county.vision.common.NotifyService;
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

/** 半年/周期复查：提醒、到场登记、逾期处理 */
@RestController
@RequestMapping("/api/followups")
public class FollowUpController {

    private final FollowUpRepository repo;
    private final StudentRepository studentRepo;
    private final NotificationRepository notificationRepo;
    private final IssueRepository issueRepo;
    private final NotifyService notify;
    private final TimelineService timeline;
    private final com.county.vision.service.FollowUpScheduler scheduler;

    public FollowUpController(FollowUpRepository repo, StudentRepository studentRepo,
                              NotificationRepository notificationRepo, IssueRepository issueRepo,
                              NotifyService notify, TimelineService timeline,
                              com.county.vision.service.FollowUpScheduler scheduler) {
        this.repo = repo;
        this.studentRepo = studentRepo;
        this.notificationRepo = notificationRepo;
        this.issueRepo = issueRepo;
        this.notify = notify;
        this.timeline = timeline;
        this.scheduler = scheduler;
    }

    /** 手动触发逾期复查扫描（与每日 07:30 定时任务逻辑一致，便于演示验证） */
    @PostMapping("/scan-overdue")
    public Map<String, Object> scan() {
        int before = (int) repo.findAll().stream().filter(f -> "MISSED".equals(f.getStatus())).count();
        scheduler.scanOverdue();
        int after = (int) repo.findAll().stream().filter(f -> "MISSED".equals(f.getStatus())).count();
        return Map.of("newlyMissed", after - before, "totalMissed", after);
    }

    @GetMapping
    public List<Map<String, Object>> list(@RequestParam(required = false) String status,
                                          @RequestParam(required = false) Long studentId,
                                          @RequestParam(required = false) Boolean overdue) {
        List<FollowUp> base;
        if (studentId != null) base = repo.findByStudentIdOrderByDueDateDesc(studentId);
        else if (status != null) {
            base = Boolean.TRUE.equals(overdue)
                    ? repo.findByStatusAndDueDateBefore(status, LocalDate.now())
                    : repo.findAll().stream().filter(f -> status.equals(f.getStatus())).toList();
        } else base = repo.findAll();
        List<Map<String, Object>> out = new ArrayList<>();
        for (FollowUp f : base) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("followUp", f);
            m.put("overdue", f.getDueDate().isBefore(LocalDate.now())
                    && !"DONE".equals(f.getStatus()));
            studentRepo.findById(f.getStudentId()).ifPresent(s -> {
                m.put("studentName", s.getName());
                m.put("parentPhone", s.getParentPhone());
                m.put("schoolId", s.getSchoolId());
            });
            out.add(m);
        }
        return out;
    }

    /** 到期前提醒家长复查（短信走 Redis 队列） */
    @PostMapping("/{id}/remind")
    @Transactional
    public Map<String, Object> remind(@PathVariable Long id) {
        FollowUp f = repo.findById(id).orElseThrow(() -> GlobalExceptionHandler.notFound("复查计划不存在"));
        Student s = studentRepo.findById(f.getStudentId()).orElseThrow();
        f.setStatus("NOTIFIED");
        f.setRemindedAt(LocalDateTime.now());
        repo.save(f);

        String content = "【半年复查提醒】" + s.getName() + "家长您好，孩子应于 " + f.getDueDate()
                + " 前完成视力复查，请预约县医院或合作眼科门诊。";
        Notification n = new Notification();
        n.setTitle("周期复查提醒");
        n.setContent(content);
        n.setStudentId(s.getId());
        n.setBizType("FOLLOWUP_REMIND");
        n.setPhone(s.getParentPhone());
        n.setStatus("SENT");
        n.setSentAt(LocalDateTime.now());
        notificationRepo.save(n);
        notify.pushToGateway(s.getParentPhone(), content, s.getId(), "FOLLOWUP_REMIND");

        timeline.event(s.getId(), "FOLLOWUP_REMINDED", "复查提醒已发送",
                "应复查日期：" + f.getDueDate(), LoginUser.get().realName(), LoginUser.get().role(),
                "FOLLOWUP", f.getId());
        return Map.of("sent", true);
    }

    public record DoneReq(LocalDate doneDate, String resultNote) {}

    /** 学生到场完成复查 */
    @PostMapping("/{id}/done")
    @Transactional
    public FollowUp done(@PathVariable Long id, @RequestBody DoneReq req) {
        FollowUp f = repo.findById(id).orElseThrow(() -> GlobalExceptionHandler.notFound("复查计划不存在"));
        f.setStatus("DONE");
        f.setDoneDate(req.doneDate() == null ? LocalDate.now() : req.doneDate());
        f.setResultNote(req.resultNote());
        FollowUp saved = repo.save(f);
        timeline.event(f.getStudentId(), "FOLLOWUP_DONE", "按期完成复查",
                req.resultNote(), LoginUser.get().realName(), LoginUser.get().role(), "FOLLOWUP", f.getId());
        return saved;
    }
}
