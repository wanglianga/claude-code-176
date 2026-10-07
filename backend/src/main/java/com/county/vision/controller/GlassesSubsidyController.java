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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 配镜（合作店补助配镜 / 家长自行外配）、配镜后不适与调试、补助申请/审核/争议/发放 */
@RestController
@RequestMapping("/api")
public class GlassesSubsidyController {

    private final GlassesRepository glassesRepo;
    private final SubsidyRepository subsidyRepo;
    private final RevisitDiagnosisRepository diagnosisRepo;
    private final StudentRepository studentRepo;
    private final IssueRepository issueRepo;
    private final OrganizationRepository orgRepo;
    private final TimelineService timeline;

    /** 县域统一补助标准：每副不超过 300 元 */
    private static final BigDecimal SUBSIDY_CAP = new BigDecimal("300.00");

    public GlassesSubsidyController(GlassesRepository glassesRepo, SubsidyRepository subsidyRepo,
                                    RevisitDiagnosisRepository diagnosisRepo, StudentRepository studentRepo,
                                    IssueRepository issueRepo, OrganizationRepository orgRepo,
                                    TimelineService timeline) {
        this.glassesRepo = glassesRepo;
        this.subsidyRepo = subsidyRepo;
        this.diagnosisRepo = diagnosisRepo;
        this.studentRepo = studentRepo;
        this.issueRepo = issueRepo;
        this.orgRepo = orgRepo;
        this.timeline = timeline;
    }

    // ---------- 配镜 ----------

    public record GlassesReq(Long studentId, Long diagnosisId, BigDecimal price, LocalDate fittedDate,
                             String orgName) {}

    @GetMapping("/glasses")
    public List<Glasses> glasses(@RequestParam(required = false) Long studentId,
                                 @RequestParam(required = false) String source) {
        if (studentId != null) return glassesRepo.findByStudentIdOrderByFittedDateDesc(studentId);
        if (source != null) return glassesRepo.findBySource(source);
        return glassesRepo.findAll();
    }

    /** 合作眼镜店按处方配镜并登记，符合补助资格的自动发起补助申请 */
    @PostMapping("/glasses")
    @Transactional
    public Map<String, Object> fitGlasses(@RequestBody GlassesReq req) {
        if (req.studentId() == null) throw GlobalExceptionHandler.badRequest("学生必填");
        Student s = studentRepo.findById(req.studentId())
                .orElseThrow(() -> GlobalExceptionHandler.badRequest("学生不存在"));
        LoginUser me = LoginUser.get();

        RevisitDiagnosis d = req.diagnosisId() == null ? null
                : diagnosisRepo.findById(req.diagnosisId()).orElse(null);
        if (d == null) d = diagnosisRepo.findFirstByStudentIdOrderByVisitDateDesc(req.studentId()).orElse(null);

        Glasses g = new Glasses();
        g.setStudentId(s.getId());
        g.setDiagnosisId(d == null ? null : d.getId());
        g.setOrgId(me.orgId());
        g.setOrgName(req.orgName() != null ? req.orgName()
                : (me.orgId() == null ? "合作眼镜店" : orgRepo.findById(me.orgId())
                    .map(Organization::getName).orElse("合作眼镜店")));
        g.setSource("SUBSIDY_SHOP");
        g.setStatus("FITTED");
        g.setPrice(req.price());
        g.setFittedDate(req.fittedDate() == null ? LocalDate.now() : req.fittedDate());
        glassesRepo.save(g);

        s.setStatus("GLASSES_FITTED");
        s.setUpdatedAt(LocalDateTime.now());
        studentRepo.save(s);

        timeline.event(s.getId(), "GLASSES_FITTED", "合作店配镜完成",
                g.getOrgName() + "，费用 " + req.price() + " 元，按医院处方验配",
                me.realName(), me.role(), "GLASSES", g.getId());

        Long subsidyId = null;
        // 医生判定符合补助资格 → 自动生成补助申请
        if (d != null && Boolean.TRUE.equals(d.getSubsidyEligible())) {
            Subsidy sub = new Subsidy();
            sub.setStudentId(s.getId());
            sub.setGlassesId(g.getId());
            sub.setDiagnosisId(d.getId());
            sub.setAmount(req.price() == null ? SUBSIDY_CAP
                    : req.price().min(SUBSIDY_CAP));
            sub.setStatus("PENDING");
            sub.setApplyNote("眼镜店凭复诊处方发起配镜补助申请");
            subsidyRepo.save(sub);
            subsidyId = sub.getId();
            g.setSubsidyAmount(BigDecimal.ZERO);
            glassesRepo.save(g);
            timeline.event(s.getId(), "SUBSIDY_APPLIED", "配镜补助申请已发起",
                    "申请补助 " + sub.getAmount() + " 元（封顶 " + SUBSIDY_CAP + " 元），待县教体/卫健审核",
                    me.realName(), me.role(), "SUBSIDY", sub.getId());
        } else {
            timeline.event(s.getId(), "SUBSIDY_NOT_ELIGIBLE", "不符合补助条件",
                    "复诊医生未判定为补助对象，配镜费用家长自理", me.realName(), me.role(),
                    "GLASSES", g.getId());
        }
        return Map.of("glassesId", g.getId(), "subsidyId", subsidyId == null ? "" : subsidyId);
    }

    /** 家长自行外配眼镜登记：纳入统一健康管理记录，学校/医生跟进佩戴与复查 */
    @PostMapping("/glasses/external")
    @Transactional
    public Map<String, Object> selfExternal(@RequestBody GlassesReq req) {
        Student s = studentRepo.findById(req.studentId())
                .orElseThrow(() -> GlobalExceptionHandler.badRequest("学生不存在"));
        LoginUser me = LoginUser.get();
        Glasses g = new Glasses();
        g.setStudentId(s.getId());
        g.setOrgName(req.orgName() == null ? "家长自行外配（非合作机构）" : req.orgName());
        g.setSource("SELF_EXTERNAL");
        g.setStatus("FITTED");
        g.setPrice(req.price());
        g.setFittedDate(req.fittedDate() == null ? LocalDate.now() : req.fittedDate());
        g.setSubsidyAmount(BigDecimal.ZERO);
        glassesRepo.save(g);

        Issue issue = new Issue();
        issue.setStudentId(s.getId());
        issue.setIssueType("SELF_GLASSES");
        issue.setTitle("家长自行外配眼镜");
        issue.setDescription("家长在非合作机构配镜（" + g.getOrgName() + "，" + req.price()
                + " 元），不享受平台补助；需核对是否按处方验配，提醒规范佩戴与复查。");
        issue.setStatus("OPEN");
        issue.setCreatedBy(me.id());
        issue.setCreatedByName(me.realName());
        issue.setCreatedByRole(me.role());
        issueRepo.save(issue);

        timeline.event(s.getId(), "SELF_GLASSES", "家长自行外配眼镜",
                g.getOrgName() + "；不享受补助，健康老师将跟进佩戴情况",
                me.realName(), me.role(), "GLASSES", g.getId());
        return Map.of("glassesId", g.getId(), "issueId", issue.getId());
    }

    /** 配镜后不适反馈 */
    public record DiscomfortReq(String note) {}

    @PostMapping("/glasses/{id}/discomfort")
    @Transactional
    public Map<String, Object> discomfort(@PathVariable Long id, @RequestBody DiscomfortReq req) {
        Glasses g = glassesRepo.findById(id).orElseThrow(() -> GlobalExceptionHandler.notFound("配镜记录不存在"));
        LoginUser me = LoginUser.get();
        g.setStatus("DISCOMFORT_REPORTED");
        g.setDiscomfortDate(LocalDate.now());
        g.setDiscomfortNote(req.note());
        glassesRepo.save(g);

        Issue issue = new Issue();
        issue.setStudentId(g.getStudentId());
        issue.setIssueType("GLASSES_DISCOMFORT");
        issue.setTitle("配镜后不适：" + g.getOrgName());
        issue.setDescription(req.note() + "；需眼镜店调试或返回复诊机构重新评估。");
        issue.setStatus("OPEN");
        issue.setCreatedBy(me.id());
        issue.setCreatedByName(me.realName());
        issue.setCreatedByRole(me.role());
        issueRepo.save(issue);

        timeline.event(g.getStudentId(), "GLASSES_DISCOMFORT", "配镜后不适反馈",
                req.note(), me.realName(), me.role(), "GLASSES", g.getId());
        return Map.of("issueId", issue.getId());
    }

    /** 眼镜店调试处理 */
    @PostMapping("/glasses/{id}/readjust")
    @Transactional
    public Glasses readjust(@PathVariable Long id, @RequestBody DiscomfortReq req) {
        Glasses g = glassesRepo.findById(id).orElseThrow(() -> GlobalExceptionHandler.notFound("配镜记录不存在"));
        g.setStatus("READJUSTED");
        g.setReadjustNote(req.note());
        Glasses saved = glassesRepo.save(g);
        timeline.event(g.getStudentId(), "GLASSES_READJUSTED", "眼镜店完成调试",
                req.note(), LoginUser.get().realName(), LoginUser.get().role(), "GLASSES", g.getId());
        return saved;
    }

    /** 家长确认佩戴舒适 */
    @PostMapping("/glasses/{id}/confirm")
    @Transactional
    public Glasses confirmOk(@PathVariable Long id) {
        Glasses g = glassesRepo.findById(id).orElseThrow(() -> GlobalExceptionHandler.notFound("配镜记录不存在"));
        g.setStatus("CONFIRMED_OK");
        g.setConfirmedDate(LocalDate.now());
        Glasses saved = glassesRepo.save(g);
        timeline.event(g.getStudentId(), "GLASSES_CONFIRMED", "家长确认佩戴舒适",
                null, "家长", "PARENT", "GLASSES", g.getId());
        return saved;
    }

    // ---------- 补助 ----------

    @GetMapping("/subsidies")
    public List<Map<String, Object>> subsidies(@RequestParam(required = false) String status,
                                               @RequestParam(required = false) Long studentId) {
        List<Subsidy> list;
        if (studentId != null) list = subsidyRepo.findByStudentIdOrderByCreatedAtDesc(studentId);
        else if (status != null) list = subsidyRepo.findByStatusOrderByCreatedAtDesc(status);
        else list = subsidyRepo.findAll();
        return list.stream().map(this::toView).toList();
    }

    public record SubsidyDecisionReq(String action, String note) {} // APPROVE/REJECT

    /** 县教体部门审核补助申请 */
    @PostMapping("/subsidies/{id}/decision")
    @Transactional
    public Subsidy decision(@PathVariable Long id, @RequestBody SubsidyDecisionReq req) {
        Subsidy sub = subsidyRepo.findById(id).orElseThrow(() -> GlobalExceptionHandler.notFound("补助申请不存在"));
        if (!"PENDING".equals(sub.getStatus()) && !"DISPUTED".equals(sub.getStatus())) {
            throw GlobalExceptionHandler.badRequest("该申请已审核");
        }
        LoginUser me = LoginUser.get();
        if ("APPROVE".equals(req.action())) {
            sub.setStatus("APPROVED");
        } else if ("REJECT".equals(req.action())) {
            sub.setStatus("REJECTED");
        } else {
            throw GlobalExceptionHandler.badRequest("未知审核动作");
        }
        sub.setDecisionNote(req.note());
        sub.setDecidedBy(me.id());
        sub.setDecidedByName(me.realName());
        sub.setDecidedAt(LocalDateTime.now());
        Subsidy saved = subsidyRepo.save(sub);
        timeline.event(sub.getStudentId(),
                "APPROVED".equals(saved.getStatus()) ? "SUBSIDY_APPROVED" : "SUBSIDY_REJECTED",
                "APPROVED".equals(saved.getStatus()) ? "补助审核通过" : "补助审核驳回",
                saved.getAmount() + " 元；" + req.note(), me.realName(), me.role(), "SUBSIDY", saved.getId());
        return saved;
    }

    /** 财政/教体发放补助 */
    @PostMapping("/subsidies/{id}/pay")
    @Transactional
    public Subsidy pay(@PathVariable Long id) {
        Subsidy sub = subsidyRepo.findById(id).orElseThrow(() -> GlobalExceptionHandler.notFound("补助申请不存在"));
        if (!"APPROVED".equals(sub.getStatus())) throw GlobalExceptionHandler.badRequest("仅审核通过的申请可发放");
        sub.setStatus("PAID");
        sub.setPaidAt(LocalDateTime.now());
        Subsidy saved = subsidyRepo.save(sub);
        glassesRepo.findById(sub.getGlassesId()).ifPresent(g -> {
            g.setSubsidyAmount(sub.getAmount());
            glassesRepo.save(g);
        });
        Student s = studentRepo.findById(sub.getStudentId()).orElseThrow();
        s.setStatus("SUBSIDY_PAID");
        s.setUpdatedAt(LocalDateTime.now());
        studentRepo.save(s);
        timeline.event(sub.getStudentId(), "SUBSIDY_PAID", "配镜补助已发放",
                sub.getAmount() + " 元已发放至家长/眼镜店", LoginUser.get().realName(),
                LoginUser.get().role(), "SUBSIDY", saved.getId());
        return saved;
    }

    /** 家长/学校对补助资格有争议：申请复核 */
    @PostMapping("/subsidies/{id}/dispute")
    @Transactional
    public Subsidy dispute(@PathVariable Long id, @RequestBody SubsidyDecisionReq req) {
        Subsidy sub = subsidyRepo.findById(id).orElseThrow(() -> GlobalExceptionHandler.notFound("补助申请不存在"));
        LoginUser me = LoginUser.get();
        sub.setStatus("DISPUTED");
        sub.setDecisionNote("争议复核：" + req.note());
        sub.setDecidedBy(me.id());
        sub.setDecidedByName(me.realName());
        sub.setDecidedAt(LocalDateTime.now());
        Subsidy saved = subsidyRepo.save(sub);

        Issue issue = new Issue();
        issue.setStudentId(sub.getStudentId());
        issue.setIssueType("SUBSIDY_DISPUTE");
        issue.setTitle("补助资格争议待复核");
        issue.setDescription(req.note() + "；申请金额 " + sub.getAmount() + " 元。");
        issue.setStatus("OPEN");
        issue.setCreatedBy(me.id());
        issue.setCreatedByName(me.realName());
        issue.setCreatedByRole(me.role());
        issueRepo.save(issue);

        timeline.event(sub.getStudentId(), "SUBSIDY_DISPUTED", "补助资格争议",
                req.note(), me.realName(), me.role(), "SUBSIDY", saved.getId());
        return saved;
    }

    private Map<String, Object> toView(Subsidy sub) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("subsidy", sub);
        studentRepo.findById(sub.getStudentId()).ifPresent(s -> {
            m.put("studentName", s.getName());
            m.put("schoolId", s.getSchoolId());
            orgRepo.findById(s.getSchoolId()).ifPresent(o -> m.put("schoolName", o.getName()));
        });
        return m;
    }
}
