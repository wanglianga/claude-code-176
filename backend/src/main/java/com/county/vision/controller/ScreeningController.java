package com.county.vision.controller;

import com.county.vision.common.GlobalExceptionHandler;
import com.county.vision.common.LoginUser;
import com.county.vision.common.NotifyService;
import com.county.vision.entity.*;
import com.county.vision.repo.*;
import com.county.vision.service.TimelineService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/** 年度筛查：批次管理 + 现场记录（裸眼视力/屈光度/散光/问卷/戴镜/复测/设备校准编号）
 *  异常后推送家长确认与学校健康老师复核 */
@RestController
@RequestMapping("/api/screening")
public class ScreeningController {

    private final ScreeningBatchRepository batchRepo;
    private final ScreeningRepository screeningRepo;
    private final StudentRepository studentRepo;
    private final ParentConfirmationRepository confirmationRepo;
    private final ReferralRepository referralRepo;
    private final IssueRepository issueRepo;
    private final NotificationRepository notificationRepo;
    private final OrganizationRepository orgRepo;
    private final TimelineService timeline;
    private final NotifyService notify;

    public ScreeningController(ScreeningBatchRepository batchRepo, ScreeningRepository screeningRepo,
                               StudentRepository studentRepo, ParentConfirmationRepository confirmationRepo,
                               ReferralRepository referralRepo, IssueRepository issueRepo,
                               NotificationRepository notificationRepo, OrganizationRepository orgRepo,
                               TimelineService timeline, NotifyService notify) {
        this.batchRepo = batchRepo;
        this.screeningRepo = screeningRepo;
        this.studentRepo = studentRepo;
        this.confirmationRepo = confirmationRepo;
        this.referralRepo = referralRepo;
        this.issueRepo = issueRepo;
        this.notificationRepo = notificationRepo;
        this.orgRepo = orgRepo;
        this.timeline = timeline;
        this.notify = notify;
    }

    // ---------- 批次 ----------

    @GetMapping("/batches")
    public List<ScreeningBatch> batches(@RequestParam(required = false) Long schoolId) {
        if (schoolId != null) return batchRepo.findBySchoolIdOrderByScreeningDateDesc(schoolId);
        return batchRepo.findAll();
    }

    @PostMapping("/batches")
    public ScreeningBatch createBatch(@RequestBody ScreeningBatch body) {
        if (body.getSchoolId() == null || body.getScreeningDate() == null || body.getName() == null) {
            throw GlobalExceptionHandler.badRequest("批次名称、学校、筛查日期必填");
        }
        body.setCreatedBy(LoginUser.get().id());
        body.setTotalCount(0);
        body.setAbnormalCount(0);
        return batchRepo.save(body);
    }

    @GetMapping("/batches/{id}")
    public Map<String, Object> batchDetail(@PathVariable Long id) {
        ScreeningBatch b = batchRepo.findById(id).orElseThrow(() -> GlobalExceptionHandler.notFound("批次不存在"));
        List<Screening> records = screeningRepo.findByBatchId(id);
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("batch", b);
        res.put("records", records);
        res.put("absentCount", screeningRepo.countByBatchIdAndAbsentTrue(id));
        res.put("retestCount", records.stream().filter(x -> Boolean.TRUE.equals(x.getNeedRetest())).count());
        return res;
    }

    // ---------- 现场录入 ----------

    @PostMapping("/records")
    @Transactional
    public Screening record(@RequestBody Screening body) {
        if (body.getBatchId() == null || body.getStudentId() == null) {
            throw GlobalExceptionHandler.badRequest("批次与学生必填");
        }
        ScreeningBatch batch = batchRepo.findById(body.getBatchId())
                .orElseThrow(() -> GlobalExceptionHandler.badRequest("批次不存在"));
        Student s = studentRepo.findById(body.getStudentId())
                .orElseThrow(() -> GlobalExceptionHandler.badRequest("学生不存在"));
        LoginUser me = LoginUser.get();
        body.setScreenerId(me.id());
        body.setScreeningTime(LocalDateTime.now());

        // 请假未筛：登记问题，进入持续跟进清单
        if (Boolean.TRUE.equals(body.getAbsent())) {
            body.setResult("SUSPECTED");
            screeningRepo.save(body);
            s.setStatus("PENDING_SCREENING");
            s.setLatestScreeningId(body.getId());
            s.setUpdatedAt(LocalDateTime.now());
            studentRepo.save(s);
            openIssue(s, body, "ABSENT", "筛查请假：" + (body.getAbsentReason() == null ? "未注明" : body.getAbsentReason()),
                    "学生 " + s.getName() + " 筛查当日请假，需安排补筛", me);
            timeline.event(s.getId(), "SCREENING_ABSENT", "筛查请假",
                    body.getAbsentReason(), me.realName(), me.role(), "SCREENING", body.getId());
            return body;
        }

        // 结果判定：任一裸眼视力 < 5.0 或存在近视性屈光（球镜 <= -0.50D）判为异常
        String result = judgeResult(body);
        if (Boolean.TRUE.equals(body.getNeedRetest()) && "NORMAL".equals(result)) {
            result = "SUSPECTED";
        }
        body.setResult(result);
        screeningRepo.save(body);

        batch.setTotalCount(screeningRepo.findByBatchId(batch.getId()).size());
        if ("ABNORMAL".equals(result)) {
            batch.setAbnormalCount((int) screeningRepo.countByBatchIdAndResult(batch.getId(), "ABNORMAL"));
        }
        batchRepo.save(batch);

        if ("ABNORMAL".equals(result)) {
            handleAbnormal(batch, s, body, me);
        } else if ("SUSPECTED".equals(result)) {
            s.setStatus("SCREENED_ABNORMAL");
            timeline.event(s.getId(), "SCREENING_RETEST", "需复测",
                    "筛查结果可疑，设备校准编号：" + body.getDeviceCalibNo(), me.realName(), me.role(),
                    "SCREENING", body.getId());
        } else {
            s.setStatus("CLOSED");
            timeline.event(s.getId(), "SCREENING_NORMAL", "筛查正常",
                    "裸眼视力 右" + body.getNakedOd() + " 左" + body.getNakedOs(),
                    me.realName(), me.role(), "SCREENING", body.getId());
        }
        s.setLatestScreeningId(body.getId());
        s.setUpdatedAt(LocalDateTime.now());
        studentRepo.save(s);
        return body;
    }

    private String judgeResult(Screening b) {
        boolean lowVision = b.getNakedOd() != null && b.getNakedOd().compareTo(new BigDecimal("5.0")) < 0
                || b.getNakedOs() != null && b.getNakedOs().compareTo(new BigDecimal("5.0")) < 0;
        boolean myopicRef = b.getSphOd() != null && b.getSphOd().compareTo(new BigDecimal("-0.50")) <= 0
                || b.getSphOs() != null && b.getSphOs().compareTo(new BigDecimal("-0.50")) <= 0;
        return (lowVision || myopicRef) ? "ABNORMAL" : "NORMAL";
    }

    /** 异常处理：生成家长确认单 + 学校复核单 + 推送通知（Redis 队列模拟短信），电话问题登记 */
    private void handleAbnormal(ScreeningBatch batch, Student s, Screening sc, LoginUser me) {
        s.setStatus("AWAIT_CONFIRM");

        ParentConfirmation pc = confirmationRepo.findByScreeningId(sc.getId()).orElseGet(ParentConfirmation::new);
        pc.setScreeningId(sc.getId());
        pc.setStudentId(s.getId());
        pc.setStatus("PENDING");
        pc.setSchoolReviewStatus("PENDING");
        pc.setNotifiedAt(LocalDateTime.now());
        confirmationRepo.save(pc);

        String content = "【" + orgRepo.findById(s.getSchoolId()).map(Organization::getName).orElse("学校") + "】"
                + s.getName() + "家长您好，孩子年度视力筛查异常（裸眼视力右" + sc.getNakedOd()
                + " 左" + sc.getNakedOs() + "），请登录平台确认复诊安排（县医院/合作眼科门诊/暂缓）。";

        Notification n = new Notification();
        n.setTitle("视力筛查结果告知与复诊确认");
        n.setContent(content);
        n.setStudentId(s.getId());
        n.setBizType("SCREENING_ABNORMAL");
        n.setChannel("SMS");
        n.setPhone(s.getParentPhone());
        n.setStatus("SENT");
        n.setSentAt(LocalDateTime.now());
        notificationRepo.save(n);
        notify.pushToGateway(s.getParentPhone(), content, s.getId(), "SCREENING_ABNORMAL");
        notify.incrementUnread(0L); // 家长端未登录场景的公共计数（演示）

        // 家长电话无法接通（号码缺失/明显非法或班主任主动标记）：登记问题交学校跟进
        if (s.getParentPhone() == null || s.getParentPhone().isBlank()
                || s.getParentPhone().replaceAll("\\d", "").length() > 3) {
            openIssue(s, sc, "PHONE_UNREACHABLE", "家长电话无法接通：" + s.getParentPhone(),
                    "筛查异常但联系方式无效，需班主任通过其他途径告知", me);
        }

        timeline.event(s.getId(), "SCREENING_ABNORMAL", "筛查异常，已告知家长",
                "裸眼视力右" + sc.getNakedOd() + " 左" + sc.getNakedOs()
                        + "；球镜右" + sc.getSphOd() + " 左" + sc.getSphOs()
                        + "；散光右" + sc.getCylOd() + " 左" + sc.getCylOs()
                        + "；" + (Boolean.TRUE.equals(sc.getWearingGlasses()) ? "已戴镜；" : "未戴镜；")
                        + "设备校准编号：" + sc.getDeviceCalibNo()
                        + "；短信已推送至 " + maskPhone(s.getParentPhone()),
                me.realName(), me.role(), "SCREENING", sc.getId());
    }

    // ---------- 家长决策 ----------

    public record ParentDecisionReq(String choice, String reason, String decidedByName) {}

    @PostMapping("/confirmations/{id}/decision")
    @Transactional
    public ParentConfirmation parentDecision(@PathVariable Long id, @RequestBody ParentDecisionReq req) {
        ParentConfirmation pc = confirmationRepo.findById(id)
                .orElseThrow(() -> GlobalExceptionHandler.notFound("确认单不存在"));
        if (!"PENDING".equals(pc.getStatus()) && !"ON_HOLD".equals(pc.getStatus())) {
            throw GlobalExceptionHandler.badRequest("该确认单已完成决策");
        }
        if (req.choice() == null) throw GlobalExceptionHandler.badRequest("请选择复诊方式或暂缓");
        Student s = studentRepo.findById(pc.getStudentId()).orElseThrow();

        if ("POSTPONE".equals(req.choice())) {
            pc.setStatus("ON_HOLD");
            pc.setChoice("POSTPONE");
            pc.setReason(req.reason());
            pc.setConfirmedAt(LocalDateTime.now());
            pc.setDecidedByName(req.decidedByName() == null ? "家长" : req.decidedByName());
            s.setStatus("ON_HOLD");
            timeline.event(s.getId(), "PARENT_POSTPONE", "家长选择暂缓处理",
                    "原因：" + req.reason(), pc.getDecidedByName(), "PARENT", "CONFIRMATION", pc.getId());
            openIssue(s, null, "PHONE_UNREACHABLE", "家长暂缓复诊",
                    "家长选择暂缓：" + req.reason() + "，学校健康老师需复核并持续提醒",
                    LoginUser.get());
        } else {
            pc.setStatus("CONFIRMED");
            pc.setChoice(req.choice());
            pc.setReason(req.reason());
            pc.setConfirmedAt(LocalDateTime.now());
            pc.setDecidedByName(req.decidedByName() == null ? "家长" : req.decidedByName());
            // 复核通过前保持 AWAIT_REVIEW
            s.setStatus("AWAIT_REVIEW");
            timeline.event(s.getId(), "PARENT_CHOICE", "家长确认复诊",
                    "COUNTY_HOSPITAL".equals(req.choice()) ? "选择县医院复诊" : "选择合作眼科门诊复诊",
                    pc.getDecidedByName(), "PARENT", "CONFIRMATION", pc.getId());
        }
        confirmationRepo.save(pc);
        s.setUpdatedAt(LocalDateTime.now());
        studentRepo.save(s);
        return pc;
    }

    // ---------- 学校健康老师复核 ----------

    public record ReviewReq(boolean approved, String note) {}

    @PostMapping("/confirmations/{id}/review")
    @Transactional
    public Map<String, Object> schoolReview(@PathVariable Long id, @RequestBody ReviewReq req) {
        ParentConfirmation pc = confirmationRepo.findById(id)
                .orElseThrow(() -> GlobalExceptionHandler.notFound("确认单不存在"));
        if (!"CONFIRMED".equals(pc.getStatus())) {
            throw GlobalExceptionHandler.badRequest("家长尚未确认复诊方式");
        }
        LoginUser me = LoginUser.get();
        Student s = studentRepo.findById(pc.getStudentId()).orElseThrow();

        if (!req.approved()) {
            pc.setSchoolReviewStatus("REJECTED");
            pc.setSchoolReviewNote(req.note());
            pc.setSchoolReviewBy(me.id());
            pc.setSchoolReviewAt(LocalDateTime.now());
            confirmationRepo.save(pc);
            timeline.event(s.getId(), "SCHOOL_REVIEW_REJECT", "学校复核退回",
                    req.note(), me.realName(), me.role(), "CONFIRMATION", pc.getId());
            return Map.of("reviewStatus", "REJECTED");
        }

        pc.setSchoolReviewStatus("APPROVED");
        pc.setSchoolReviewNote(req.note());
        pc.setSchoolReviewBy(me.id());
        pc.setSchoolReviewAt(LocalDateTime.now());
        confirmationRepo.save(pc);

        // 复核通过 → 开转诊单
        String targetType = pc.getChoice();
        List<Organization> targets = orgRepo.findByOrgType(
                "COUNTY_HOSPITAL".equals(targetType) ? "HOSPITAL" : "OPTOMETRY_CLINIC");
        Organization target = targets.stream().findFirst().orElse(null);

        Referral r = new Referral();
        r.setStudentId(s.getId());
        r.setScreeningId(pc.getScreeningId());
        r.setConfirmationId(pc.getId());
        r.setTargetType(targetType);
        r.setTargetOrgId(target == null ? null : target.getId());
        r.setStatus("PENDING");
        r.setIssuedBy(me.id());
        referralRepo.save(r);

        s.setStatus("AWAIT_REVISIT");
        s.setUpdatedAt(LocalDateTime.now());
        studentRepo.save(s);

        timeline.event(s.getId(), "SCHOOL_REVIEW_APPROVE", "学校复核通过并转诊",
                (target == null ? "等待分配机构" : "转诊至：" + target.getName())
                        + (req.note() == null ? "" : "；备注：" + req.note()),
                me.realName(), me.role(), "REFERRAL", r.getId());
        return Map.of("reviewStatus", "APPROVED", "referralId", r.getId());
    }

    /** 学校健康老师对暂缓决定重新提醒家长（家长再决策） */
    @PostMapping("/confirmations/{id}/remind")
    @Transactional
    public Map<String, Object> remindParent(@PathVariable Long id) {
        ParentConfirmation pc = confirmationRepo.findById(id)
                .orElseThrow(() -> GlobalExceptionHandler.notFound("确认单不存在"));
        Student s = studentRepo.findById(pc.getStudentId()).orElseThrow();
        String content = "【复诊提醒】" + s.getName() + "家长您好，孩子视力筛查异常后尚未安排复诊，请尽快登录平台确认。";
        Notification n = new Notification();
        n.setTitle("复诊提醒");
        n.setContent(content);
        n.setStudentId(s.getId());
        n.setBizType("REVISIT_REMIND");
        n.setPhone(s.getParentPhone());
        n.setStatus("SENT");
        n.setSentAt(LocalDateTime.now());
        notificationRepo.save(n);
        notify.pushToGateway(s.getParentPhone(), content, s.getId(), "REVISIT_REMIND");
        timeline.event(s.getId(), "PARENT_REMINDED", "学校再次提醒家长",
                "健康老师发起复诊提醒", LoginUser.get().realName(), LoginUser.get().role(),
                "CONFIRMATION", pc.getId());
        return Map.of("sent", true);
    }

    @GetMapping("/confirmations")
    public List<Map<String, Object>> confirmations(@RequestParam(required = false) String status,
                                                   @RequestParam(required = false) String reviewStatus) {
        List<ParentConfirmation> all = confirmationRepo.findAll();
        List<Map<String, Object>> out = new ArrayList<>();
        for (ParentConfirmation pc : all) {
            if (status != null && !status.equals(pc.getStatus())) continue;
            if (reviewStatus != null && !reviewStatus.equals(pc.getSchoolReviewStatus())) continue;
            studentRepo.findById(pc.getStudentId()).ifPresent(s -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("confirmation", pc);
                m.put("studentName", s.getName());
                m.put("schoolId", s.getSchoolId());
                m.put("grade", s.getGrade());
                m.put("parentPhone", s.getParentPhone());
                orgRepo.findById(s.getSchoolId()).ifPresent(o -> m.put("schoolName", o.getName()));
                out.add(m);
            });
        }
        return out;
    }

    @GetMapping("/referrals")
    public List<Map<String, Object>> referrals(@RequestParam(required = false) String status,
                                               @RequestParam(required = false) Long targetOrgId) {
        List<Referral> list;
        if (targetOrgId != null && status != null) list = referralRepo.findByTargetOrgIdAndStatus(targetOrgId, status);
        else if (status != null) list = referralRepo.findByStatus(status);
        else if (targetOrgId != null) list = referralRepo.findByTargetOrgIdAndStatus(targetOrgId, null);
        else list = referralRepo.findAll();
        List<Map<String, Object>> out = new ArrayList<>();
        for (Referral r : list) {
            Map<String, Object> m = new LinkedHashMap<>(toMap(r));
            studentRepo.findById(r.getStudentId()).ifPresent(s -> {
                m.put("studentName", s.getName());
                m.put("grade", s.getGrade());
            });
            if (r.getTargetOrgId() != null) {
                orgRepo.findById(r.getTargetOrgId()).ifPresent(o -> m.put("targetOrgName", o.getName()));
            }
            out.add(m);
        }
        return out;
    }

    private void openIssue(Student s, Screening sc, String type, String title, String desc, LoginUser me) {
        Issue issue = new Issue();
        issue.setStudentId(s.getId());
        issue.setScreeningId(sc == null ? null : sc.getId());
        issue.setIssueType(type);
        issue.setTitle(title);
        issue.setDescription(desc);
        issue.setStatus("OPEN");
        issue.setCreatedBy(me == null ? null : me.id());
        issue.setCreatedByName(me == null ? "系统" : me.realName());
        issue.setCreatedByRole(me == null ? "SYSTEM" : me.role());
        issueRepo.save(issue);
    }

    static String maskPhone(String p) {
        if (p == null || p.length() < 7) return p;
        return p.substring(0, 3) + "****" + p.substring(p.length() - 4);
    }

    private Map<String, Object> toMap(Referral r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.getId());
        m.put("studentId", r.getStudentId());
        m.put("targetOrgId", r.getTargetOrgId());
        m.put("targetType", r.getTargetType());
        m.put("status", r.getStatus());
        m.put("issuedAt", r.getIssuedAt());
        return m;
    }
}
