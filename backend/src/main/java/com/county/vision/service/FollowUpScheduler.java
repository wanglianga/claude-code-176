package com.county.vision.service;

import com.county.vision.entity.FollowUp;
import com.county.vision.entity.Issue;
import com.county.vision.entity.Student;
import com.county.vision.repo.FollowUpRepository;
import com.county.vision.repo.IssueRepository;
import com.county.vision.repo.StudentRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/** 每日扫描：到期未完成复查 → 标记 MISSED_REVIEW 并登记问题，防止筛查后无人跟进 */
@Component
public class FollowUpScheduler {

    private final FollowUpRepository followUpRepo;
    private final IssueRepository issueRepo;
    private final StudentRepository studentRepo;
    private final TimelineService timeline;

    public FollowUpScheduler(FollowUpRepository followUpRepo, IssueRepository issueRepo,
                             StudentRepository studentRepo, TimelineService timeline) {
        this.followUpRepo = followUpRepo;
        this.issueRepo = issueRepo;
        this.studentRepo = studentRepo;
        this.timeline = timeline;
    }

    /** 每天 07:30 执行（也可通过 /api/followups/scan-overdue 手动触发） */
    @Scheduled(cron = "0 30 7 * * ?")
    @Transactional
    public void scanOverdue() {
        List<FollowUp> overdue = followUpRepo.findByStatusAndDueDateBefore("NOTIFIED", LocalDate.now());
        overdue.addAll(followUpRepo.findByStatusAndDueDateBefore("PENDING", LocalDate.now()));
        for (FollowUp f : overdue) {
            f.setStatus("MISSED");
            followUpRepo.save(f);

            Issue issue = new Issue();
            issue.setStudentId(f.getStudentId());
            issue.setIssueType("MISSED_REVIEW");
            issue.setTitle("周期复查逾期未到场");
            issue.setDescription("应于 " + f.getDueDate() + " 完成复查，至今未登记到场，需学校与家长核实并重新安排。");
            issue.setStatus("OPEN");
            issue.setCreatedByName("系统定时任务");
            issue.setCreatedByRole("SYSTEM");
            issueRepo.save(issue);

            studentRepo.findById(f.getStudentId()).ifPresent(s -> {
                s.setStatus("MISSED_REVIEW");
                studentRepo.save(s);
            });
            timeline.event(f.getStudentId(), "FOLLOWUP_MISSED", "复查逾期未到场",
                    "应复查日期 " + f.getDueDate() + "，系统自动登记问题并通知学校跟进",
                    "系统定时任务", "SYSTEM", "FOLLOWUP", f.getId());
        }
    }
}
