package com.county.vision.controller;

import com.county.vision.entity.*;
import com.county.vision.repo.*;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

/** 县教体/卫健看板：近视发展趋势（学校/年级/片区）、真实复诊率、
 *  干预执行情况、补助发放、闭环漏斗——避免筛查后异常学生无人跟进 */
@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final StudentRepository studentRepo;
    private final ScreeningRepository screeningRepo;
    private final ParentConfirmationRepository confirmationRepo;
    private final ReferralRepository referralRepo;
    private final RevisitDiagnosisRepository diagnosisRepo;
    private final SubsidyRepository subsidyRepo;
    private final InterventionRepository interventionRepo;
    private final FollowUpRepository followUpRepo;
    private final IssueRepository issueRepo;
    private final GlassesRepository glassesRepo;
    private final OrganizationRepository orgRepo;

    public StatsController(StudentRepository studentRepo, ScreeningRepository screeningRepo,
                           ParentConfirmationRepository confirmationRepo, ReferralRepository referralRepo,
                           RevisitDiagnosisRepository diagnosisRepo, SubsidyRepository subsidyRepo,
                           InterventionRepository interventionRepo, FollowUpRepository followUpRepo,
                           IssueRepository issueRepo, GlassesRepository glassesRepo,
                           OrganizationRepository orgRepo) {
        this.studentRepo = studentRepo;
        this.screeningRepo = screeningRepo;
        this.confirmationRepo = confirmationRepo;
        this.referralRepo = referralRepo;
        this.diagnosisRepo = diagnosisRepo;
        this.subsidyRepo = subsidyRepo;
        this.interventionRepo = interventionRepo;
        this.followUpRepo = followUpRepo;
        this.issueRepo = issueRepo;
        this.glassesRepo = glassesRepo;
        this.orgRepo = orgRepo;
    }

    @GetMapping("/overview")
    public Map<String, Object> overview(@RequestParam(required = false) Long schoolId) {
        Map<String, Object> m = new LinkedHashMap<>();

        long students = schoolId == null ? studentRepo.count() : studentRepo.findBySchoolId(schoolId).size();
        long abnormalConfirm = schoolId == null
                ? screeningRepo.countByResult("ABNORMAL")
                : screeningRepo.countAbnormalBySchool(schoolId);
        long confirmedCount = confirmationRepo.findAll().stream()
                .filter(c -> "CONFIRMED".equals(c.getStatus())).count();
        long referrals = referralRepo.findAll().size();
        long referralCompleted = referralRepo.findByStatus("COMPLETED").size();
        long diagnoses = diagnosisRepo.count();
        long subsidyEligible = diagnosisRepo.countBySubsidyEligibleTrue();
        long subsidiesPaid = subsidyRepo.findByStatusOrderByCreatedAtDesc("PAID").size();
        long openIssues = issueRepo.countByStatus("OPEN");
        long interventionsTotal = interventionRepo.count();
        long interventionsVerified = interventionRepo.findAll().stream()
                .filter(i -> "VERIFIED".equals(i.getStatus())).count();
        long followUpTotal = followUpRepo.count();
        long followUpMissed = followUpRepo.findAll().stream()
                .filter(f -> "MISSED".equals(f.getStatus())).count();

        m.put("studentCount", students);
        m.put("abnormalCount", abnormalConfirm);
        m.put("parentConfirmedCount", confirmedCount);
        m.put("referralCount", referrals);
        m.put("revisitCompletedCount", referralCompleted);
        m.put("diagnosisCount", diagnoses);
        m.put("subsidyEligibleCount", subsidyEligible);
        m.put("subsidyPaidCount", subsidiesPaid);
        m.put("openIssueCount", openIssues);
        m.put("interventionTotal", interventionsTotal);
        m.put("interventionVerified", interventionsVerified);
        m.put("followUpTotal", followUpTotal);
        m.put("followUpMissed", followUpMissed);

        // 真实复诊率 = 完成复诊人数 / 筛查异常人数
        m.put("realRevisitRate", rate(referralCompleted, abnormalConfirm));
        // 干预执行率 = 已执行+已核验 / 干预总数
        long doneInterventions = interventionRepo.findAll().stream()
                .filter(i -> "DONE".equals(i.getStatus()) || "VERIFIED".equals(i.getStatus())).count();
        m.put("interventionExecRate", rate(doneInterventions, interventionsTotal));
        // 复查到场率
        m.put("followUpRate", rate(followUpTotal - followUpMissed
                - followUpRepo.findAll().stream().filter(f -> "PENDING".equals(f.getStatus())
                        || "NOTIFIED".equals(f.getStatus())).count(), followUpTotal));

        // 闭环漏斗
        List<Map<String, Object>> funnel = new ArrayList<>();
        funnel.add(step("筛查异常", abnormalConfirm));
        funnel.add(step("家长确认", confirmedCount));
        funnel.add(step("学校复核转诊", referrals));
        funnel.add(step("完成复诊", referralCompleted));
        funnel.add(step("符合补助", subsidyEligible));
        funnel.add(step("补助发放", subsidiesPaid));
        m.put("funnel", funnel);
        return m;
    }

    @GetMapping("/trend")
    public Map<String, Object> trend(@RequestParam(required = false) Long schoolId) {
        List<Map<String, Object>> monthly = new ArrayList<>();
        for (Object[] row : screeningRepo.monthlyTrend(schoolId)) {
            long total = ((Number) row[1]).longValue();
            long abnormal = ((Number) row[2]).longValue();
            monthly.add(Map.of("month", row[0], "total", total, "abnormal", abnormal,
                    "rate", rate(abnormal, total)));
        }
        return Map.of("monthly", monthly);
    }

    @GetMapping("/by-grade")
    public List<Map<String, Object>> byGrade(@RequestParam(required = false) Long schoolId) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Object[] row : screeningRepo.gradeStats(schoolId)) {
            long total = ((Number) row[1]).longValue();
            long abnormal = ((Number) row[2]).longValue();
            out.add(Map.of("grade", row[0], "total", total, "abnormal", abnormal,
                    "rate", rate(abnormal, total)));
        }
        return out;
    }

    @GetMapping("/by-area")
    public List<Map<String, Object>> byArea() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Object[] row : screeningRepo.areaStats()) {
            long total = ((Number) row[1]).longValue();
            long abnormal = ((Number) row[2]).longValue();
            out.add(Map.of("area", row[0], "total", total, "abnormal", abnormal,
                    "rate", rate(abnormal, total)));
        }
        return out;
    }

    /** 各校横向对比：异常率、真实复诊率、干预执行率、复查到场率 */
    @GetMapping("/school-ranking")
    public List<Map<String, Object>> schoolRanking() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Organization o : orgRepo.findByOrgType("SCHOOL")) {
            long abnormal = screeningRepo.countAbnormalBySchool(o.getId());
            long screened = screeningRepo.countScreenedBySchool(o.getId());
            long students = studentRepo.findBySchoolId(o.getId()).size();
            long revisited = referralRepo.findAll().stream()
                    .filter(r -> "COMPLETED".equals(r.getStatus()))
                    .filter(r -> studentRepo.findById(r.getStudentId())
                            .map(s -> o.getId().equals(s.getSchoolId())).orElse(false))
                    .count();
            List<Intervention> ivs = interventionRepo.findAll().stream()
                    .filter(i -> o.getId().equals(i.getSchoolId())).toList();
            long ivDone = ivs.stream().filter(i -> !"PLANNED".equals(i.getStatus())).count();
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("schoolId", o.getId());
            m.put("schoolName", o.getName());
            m.put("area", o.getArea());
            m.put("studentCount", students);
            m.put("screenedCount", screened);
            m.put("abnormalCount", abnormal);
            m.put("abnormalRate", rate(abnormal, screened));
            m.put("revisitRate", rate(revisited, abnormal));
            m.put("interventionExecRate", rate(ivDone, ivs.size()));
            out.add(m);
        }
        return out;
    }

    /** 按学生状态分布（持续跟进看板） */
    @GetMapping("/status-distribution")
    public List<Map<String, Object>> statusDistribution(@RequestParam(required = false) Long schoolId) {
        List<Student> list = schoolId == null ? studentRepo.findAll() : studentRepo.findBySchoolId(schoolId);
        Map<String, Long> grouped = new TreeMap<>();
        list.forEach(s -> grouped.merge(s.getStatus(), 1L, Long::sum));
        List<Map<String, Object>> out = new ArrayList<>();
        grouped.forEach((k, v) -> out.add(Map.of("status", k, "count", v)));
        return out;
    }

    private Map<String, Object> step(String name, long value) {
        return Map.of("name", name, "value", value);
    }

    private double rate(long part, long total) {
        return total == 0 ? 0 : Math.round(part * 10000.0 / total) / 100.0;
    }
}
