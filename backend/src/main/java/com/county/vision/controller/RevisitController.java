package com.county.vision.controller;

import com.county.vision.common.GlobalExceptionHandler;
import com.county.vision.common.LoginUser;
import com.county.vision.entity.*;
import com.county.vision.repo.*;
import com.county.vision.service.TimelineService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/** 医院/合作门诊复诊结果回写：真性近视、散瞳、处方参数、复查周期、配镜补助资格；
 *  与筛查结果差异较大时登记问题 */
@RestController
@RequestMapping("/api/revisit")
public class RevisitController {

    private final RevisitDiagnosisRepository diagnosisRepo;
    private final ReferralRepository referralRepo;
    private final ScreeningRepository screeningRepo;
    private final StudentRepository studentRepo;
    private final IssueRepository issueRepo;
    private final FollowUpRepository followUpRepo;
    private final OrganizationRepository orgRepo;
    private final TimelineService timeline;

    public RevisitController(RevisitDiagnosisRepository diagnosisRepo, ReferralRepository referralRepo,
                             ScreeningRepository screeningRepo, StudentRepository studentRepo,
                             IssueRepository issueRepo, FollowUpRepository followUpRepo,
                             OrganizationRepository orgRepo, TimelineService timeline) {
        this.diagnosisRepo = diagnosisRepo;
        this.referralRepo = referralRepo;
        this.screeningRepo = screeningRepo;
        this.studentRepo = studentRepo;
        this.issueRepo = issueRepo;
        this.followUpRepo = followUpRepo;
        this.orgRepo = orgRepo;
        this.timeline = timeline;
    }

    @GetMapping("/diagnoses")
    public List<RevisitDiagnosis> list(@RequestParam(required = false) Long orgId,
                                       @RequestParam(required = false) Long studentId) {
        if (studentId != null) return diagnosisRepo.findByStudentIdOrderByVisitDateDesc(studentId);
        if (orgId != null) return diagnosisRepo.findByOrgIdOrderByVisitDateDesc(orgId);
        LoginUser me = LoginUser.get();
        if (me.orgId() != null) return diagnosisRepo.findByOrgIdOrderByVisitDateDesc(me.orgId());
        return diagnosisRepo.findAll();
    }

    @GetMapping("/diagnoses/{id}")
    public RevisitDiagnosis get(@PathVariable Long id) {
        return diagnosisRepo.findById(id).orElseThrow(() -> GlobalExceptionHandler.notFound("诊断记录不存在"));
    }

    @PostMapping("/diagnoses")
    @Transactional
    public Map<String, Object> create(@RequestBody RevisitDiagnosis body) {
        if (body.getStudentId() == null || body.getVisitDate() == null) {
            throw GlobalExceptionHandler.badRequest("学生与复诊日期必填");
        }
        Student s = studentRepo.findById(body.getStudentId())
                .orElseThrow(() -> GlobalExceptionHandler.badRequest("学生不存在"));
        LoginUser me = LoginUser.get();
        body.setDoctorId(me.id());
        if (body.getDoctorName() == null) body.setDoctorName(me.realName());
        if (body.getOrgId() == null) body.setOrgId(me.orgId());
        if (body.getRevisitCycleMonths() == null) body.setRevisitCycleMonths(6);
        if (body.getSubsidyEligible() == null) body.setSubsidyEligible(false);
        RevisitDiagnosis d = diagnosisRepo.save(body);

        // 回写学生档案
        s.setLatestDiagnosisId(d.getId());
        s.setStatus("REVISIT_DONE");
        s.setUpdatedAt(LocalDateTime.now());
        studentRepo.save(s);

        // 关闭转诊单
        if (d.getReferralId() != null) {
            referralRepo.findById(d.getReferralId()).ifPresent(r -> {
                r.setStatus("COMPLETED");
                r.setCompletedAt(LocalDateTime.now());
                referralRepo.save(r);
            });
        } else {
            var pending = referralRepo.findFirstByStudentIdAndStatusOrderByIssuedAtDesc(s.getId(), "PENDING");
            if (pending.isPresent()) {
                Referral r = pending.get();
                r.setStatus("COMPLETED");
                r.setCompletedAt(LocalDateTime.now());
                referralRepo.save(r);
                d.setReferralId(r.getId());
                diagnosisRepo.save(d);
            }
        }

        String orgName = d.getOrgId() == null ? "医疗机构" :
                orgRepo.findById(d.getOrgId()).map(Organization::getName).orElse("医疗机构");
        StringBuilder detail = new StringBuilder();
        detail.append("机构：").append(orgName)
              .append("；真性近视：").append(Boolean.TRUE.equals(d.getTrueMyopia()) ? "是" : "否")
              .append("；散瞳：").append(Boolean.TRUE.equals(d.getNeedMydriasis()) ? "需要" : "不需要")
              .append(Boolean.TRUE.equals(d.getMydriasisDone()) ? "(已做)" : "")
              .append("；处方 右").append(d.getRxSphOd()).append("/").append(d.getRxCylOd())
              .append(" 左").append(d.getRxSphOs()).append("/").append(d.getRxCylOs())
              .append("；复查周期：").append(d.getRevisitCycleMonths()).append("个月")
              .append("；配镜补助：").append(Boolean.TRUE.equals(d.getSubsidyEligible()) ? "符合" : "不符合");
        if (d.getAdvice() != null) detail.append("；医嘱：").append(d.getAdvice());
        timeline.event(s.getId(), "REVISIT_RESULT", "复诊结果回写", detail.toString(),
                me.realName(), me.role(), "DIAGNOSIS", d.getId());

        // 与筛查结果差异较大：自动判定并登记问题
        Screening sc = s.getLatestScreeningId() == null ? null
                : screeningRepo.findById(s.getLatestScreeningId()).orElse(null);
        if (sc != null && checkDivergence(sc, d)) {
            String desc = "筛查：裸眼右" + sc.getNakedOd() + " 左" + sc.getNakedOs()
                    + "，球镜右" + sc.getSphOd() + " 左" + sc.getSphOs()
                    + "；复诊处方右" + d.getRxSphOd() + " 左" + d.getRxSphOs()
                    + (d.getDivergenceNote() == null ? "" : "；医生说明：" + d.getDivergenceNote());
            openIssue(s, sc, "RESULT_DIVERGENCE", "复诊结果与筛查差异较大", desc, me);
            timeline.event(s.getId(), "RESULT_DIVERGENCE", "筛查-复诊差异预警",
                    desc, "系统", "SYSTEM", "DIAGNOSIS", d.getId());
        }

        // 按复查周期生成半年（或医嘱周期）复查计划
        if (Boolean.TRUE.equals(d.getTrueMyopia())) {
            FollowUp fu = new FollowUp();
            fu.setStudentId(s.getId());
            fu.setDiagnosisId(d.getId());
            fu.setDueDate(body.getVisitDate().plusMonths(body.getRevisitCycleMonths()));
            fu.setStatus("PENDING");
            followUpRepo.save(fu);
            timeline.event(s.getId(), "FOLLOWUP_PLANNED", "复查计划已生成",
                    "应于 " + fu.getDueDate() + " 前完成" + body.getRevisitCycleMonths() + "个月复查",
                    me.realName(), me.role(), "FOLLOWUP", fu.getId());
        }

        return Map.of("id", d.getId(), "status", s.getStatus());
    }

    /** 差异判定：等效球镜差 ≥1.00D，或筛查判正常而复诊确诊近视 */
    private boolean checkDivergence(Screening sc, RevisitDiagnosis d) {
        if (Boolean.TRUE.equals(d.getTrueMyopia()) && "NORMAL".equals(sc.getResult())) return true;
        BigDecimal screenSph = firstNonNull(sc.getSphOd(), sc.getSphOs());
        BigDecimal rxSph = firstNonNull(d.getRxSphOd(), d.getRxSphOs());
        if (screenSph != null && rxSph != null && screenSph.subtract(rxSph).abs().compareTo(BigDecimal.ONE) >= 0) {
            return true;
        }
        return false;
    }

    private BigDecimal firstNonNull(BigDecimal a, BigDecimal b) {
        return a != null ? a : b;
    }

    private void openIssue(Student s, Screening sc, String type, String title, String desc, LoginUser me) {
        Issue issue = new Issue();
        issue.setStudentId(s.getId());
        issue.setScreeningId(sc == null ? null : sc.getId());
        issue.setIssueType(type);
        issue.setTitle(title);
        issue.setDescription(desc);
        issue.setStatus("OPEN");
        issue.setCreatedBy(me.id());
        issue.setCreatedByName(me.realName());
        issue.setCreatedByRole(me.role());
        issueRepo.save(issue);
    }
}
