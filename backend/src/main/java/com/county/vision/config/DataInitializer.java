package com.county.vision.config;

import com.county.vision.entity.*;
import com.county.vision.repo.*;
import com.county.vision.service.TimelineService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 演示种子数据：10 类角色账号、2 所学校、完整闭环与 8 类特殊情况 */
@Component
@ConditionalOnProperty(name = "app.seed", havingValue = "true", matchIfMissing = true)
public class DataInitializer implements CommandLineRunner {

    private final AppUserRepository userRepo;
    private final OrganizationRepository orgRepo;
    private final SchoolClassRepository classRepo;
    private final StudentRepository studentRepo;
    private final ScreeningBatchRepository batchRepo;
    private final ScreeningRepository screeningRepo;
    private final ParentConfirmationRepository confirmationRepo;
    private final ReferralRepository referralRepo;
    private final RevisitDiagnosisRepository diagnosisRepo;
    private final GlassesRepository glassesRepo;
    private final SubsidyRepository subsidyRepo;
    private final InterventionRepository interventionRepo;
    private final FollowUpRepository followUpRepo;
    private final IssueRepository issueRepo;
    private final NotificationRepository notificationRepo;
    private final PasswordEncoder encoder;
    private final TimelineService timeline;

    public DataInitializer(AppUserRepository userRepo, OrganizationRepository orgRepo,
                           SchoolClassRepository classRepo, StudentRepository studentRepo,
                           ScreeningBatchRepository batchRepo, ScreeningRepository screeningRepo,
                           ParentConfirmationRepository confirmationRepo, ReferralRepository referralRepo,
                           RevisitDiagnosisRepository diagnosisRepo, GlassesRepository glassesRepo,
                           SubsidyRepository subsidyRepo, InterventionRepository interventionRepo,
                           FollowUpRepository followUpRepo, IssueRepository issueRepo,
                           NotificationRepository notificationRepo, PasswordEncoder encoder,
                           TimelineService timeline) {
        this.userRepo = userRepo;
        this.orgRepo = orgRepo;
        this.classRepo = classRepo;
        this.studentRepo = studentRepo;
        this.batchRepo = batchRepo;
        this.screeningRepo = screeningRepo;
        this.confirmationRepo = confirmationRepo;
        this.referralRepo = referralRepo;
        this.diagnosisRepo = diagnosisRepo;
        this.glassesRepo = glassesRepo;
        this.subsidyRepo = subsidyRepo;
        this.interventionRepo = interventionRepo;
        this.followUpRepo = followUpRepo;
        this.issueRepo = issueRepo;
        this.notificationRepo = notificationRepo;
        this.encoder = encoder;
        this.timeline = timeline;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepo.count() > 0) return;

        // ---------- 机构 ----------
        Organization eduBureau = org("县教育和体育局", "EDU_BUREAU", "县城区", "李科长", "0551-88001");
        Organization healthComm = org("县卫生健康委员会", "HEALTH_COMMISSION", "县城区", "王主任", "0551-88002");
        Organization agency = org("县中小学生视力筛查中心", "SCREENING_AGENCY", "县城区", "陈队长", "13800000001");
        Organization hospital = org("县人民医院", "HOSPITAL", "县城区", "周医生", "0551-88003");
        Organization clinic = org("康明合作眼科门诊", "OPTOMETRY_CLINIC", "城东片区", "吴视光师", "13800000002");
        Organization shop = org("明亮眼镜（补助定点店）", "OPTICAL_SHOP", "县城区", "孙店长", "13800000003");
        Organization school1 = org("阳光小学", "SCHOOL", "城东片区", "王校长", "0551-88004");
        Organization school2 = org("希望小学", "SCHOOL", "城西片区", "张校长", "0551-88005");

        // ---------- 10 个角色账号，密码统一 123456 ----------
        AppUser admin = user("admin", "平台管理员", "PLATFORM_ADMIN", null, "13900000000");
        AppUser edu = user("edu", "李科长", "EDU_BUREAU", eduBureau, "13900000001");
        AppUser health = user("health", "王主任", "HEALTH_COMMISSION", healthComm, "13900000002");
        AppUser healthTeacher = user("teacher.wang", "王健康老师", "SCHOOL_HEALTH_TEACHER", school1, "13900000003");
        AppUser headLi = user("head.li", "李班主任", "HEAD_TEACHER", school1, "13900000004");
        AppUser headZhao = user("head.zhao", "赵班主任", "HEAD_TEACHER", school2, "13900000005");
        AppUser screener = user("screener.chen", "陈筛查员", "SCREENER", agency, "13900000006");
        AppUser doctor = user("doctor.zhou", "周医生", "DOCTOR", hospital, "13900000007");
        AppUser optom = user("optom.wu", "吴视光师", "OPTOMETRIST", clinic, "13900000008");
        AppUser optician = user("optician.sun", "孙店长", "OPTICIAN", shop, "13900000009");

        // ---------- 班级 ----------
        SchoolClass c31 = clazz(school1, "三年级", "1班", headLi, "13900000004");
        SchoolClass c32 = clazz(school1, "三年级", "2班", headLi, "13900000004");
        SchoolClass c41 = clazz(school1, "四年级", "1班", null, null);
        SchoolClass c31b = clazz(school2, "三年级", "1班", headZhao, "13900000005");

        // ---------- 批次 ----------
        ScreeningBatch batch = new ScreeningBatch();
        batch.setName("2026 年阳光小学春季视力筛查");
        batch.setSchoolId(school1.getId());
        batch.setGrade("三年级");
        batch.setScreenerOrgId(agency.getId());
        batch.setScreeningDate(LocalDate.now().minusDays(45));
        batch.setNotes("年度全员筛查");
        batch.setCreatedBy(screener.getId());
        batch.setTotalCount(0);
        batch.setAbnormalCount(0);
        batchRepo.save(batch);

        ScreeningBatch batch2 = new ScreeningBatch();
        batch2.setName("2026 年希望小学春季视力筛查");
        batch2.setSchoolId(school2.getId());
        batch2.setGrade("三年级");
        batch2.setScreenerOrgId(agency.getId());
        batch2.setScreeningDate(LocalDate.now().minusDays(40));
        batch2.setCreatedBy(screener.getId());
        batch2.setTotalCount(0);
        batch2.setAbnormalCount(0);
        batchRepo.save(batch2);

        LocalDate screenDate = LocalDate.now().minusDays(45);

        // 1) 完整闭环：异常→县医院→确诊真性近视→补助配镜→审核发放→干预→复查到场
        Student s1 = student("李小明", "男", school1, c31, "20170301", "李明父", "13811110001",
                "2025 年体检双眼 5.0");
        Screening sc1 = screening(s1, batch, screener, screenDate, bd("4.7"), bd("4.6"),
                bd("-1.25"), bd("-1.50"), bd("-0.50"), bd("-0.75"), false, "ABNORMAL");
        ParentConfirmation pc1 = confirm(sc1, s1, "CONFIRMED", "COUNTY_HOSPITAL", null, "APPROVED", healthTeacher);
        Referral r1 = referral(s1, sc1, pc1, hospital, screener);
        RevisitDiagnosis d1 = diagnosis(s1, r1, hospital, doctor, screenDate.plusDays(12),
                true, true, true, "LOW", bd("-1.50"), bd("-1.75"), bd("-0.50"), bd("-0.75"), 6, true,
                "首次确诊低度数近视，建议配镜并增加户外活动");
        Glasses g1 = glasses(s1, d1, shop, "SUBSIDY_SHOP", "FITTED", bd("480.00"), screenDate.plusDays(20));
        Subsidy sub1 = subsidy(s1, g1, d1, bd("300.00"), "PAID", optician, edu);
        g1.setSubsidyAmount(bd("300.00")); glassesRepo.save(g1);
        s1.setStatus("SUBSIDY_PAID");
        intervention(s1, c31, school1, "SEAT_ADJUSTMENT", "座位调整至前排中间", "结合复诊结果调整座位",
                screenDate.plusDays(22), "VERIFIED", headLi, healthTeacher, "已执行一个月，视疲劳主诉减少");
        intervention(s1, c31, school1, "OUTDOOR_ACTIVITY", "每天 2 小时户外活动", "课间+体育课合计",
                screenDate.plusDays(23), "VERIFIED", headLi, healthTeacher, null);
        FollowUp f1 = followUp(s1, d1, screenDate.plusMonths(6).minusDays(20), "DONE",
                LocalDate.now().minusDays(5), "复查度数稳定，眼轴增长正常");
        notif(s1, "视力筛查结果告知与复诊确认", "李小明家长您好，孩子筛查异常，请确认复诊安排", screenDate);
        timelineAll(s1);

        // 2) 合作眼科门诊路径 + 家长自行外配眼镜
        Student s2 = student("王小红", "女", school1, c31, "20170512", "王母", "13811110002",
                "父母均近视");
        Screening sc2 = screening(s2, batch, screener, screenDate, bd("4.8"), bd("4.8"),
                bd("-0.75"), bd("-0.50"), bd("-0.25"), bd("0.00"), false, "ABNORMAL");
        ParentConfirmation pc2 = confirm(sc2, s2, "CONFIRMED", "PARTNER_CLINIC", null, "APPROVED", healthTeacher);
        Referral r2 = referral(s2, sc2, pc2, clinic, screener);
        RevisitDiagnosis d2 = diagnosis(s2, r2, clinic, optom, screenDate.plusDays(8),
                false, false, false, "NONE", bd("-0.50"), bd("0.00"), bd("0.00"), bd("0.00"), 3, false,
                "假性近视倾向，暂不配镜，以用眼干预为主，3 个月复查");
        s2.setStatus("REVISIT_DONE");
        Glasses g2 = glasses(s2, d2, null, "SELF_EXTERNAL", "FITTED", bd("260.00"), screenDate.plusDays(30));
        g2.setOrgName("家长自购网店成品眼镜");
        glassesRepo.save(g2);
        issue(s2, sc2, "SELF_GLASSES", "家长自行外配眼镜",
                "医生建议暂不配镜，家长仍网购眼镜，需健康老师沟通并跟进佩戴情况", "RESOLVED",
                healthTeacher, "已与家长沟通，暂停佩戴并按医嘱 3 个月后复查");
        intervention(s2, c31, school1, "EYE_HABIT_REMINDER", "纠正读写姿势与 20-20-20 法则", null,
                screenDate.plusDays(10), "DONE", headLi, null, null);
        timelineAll(s2);

        // 3) 家长选择暂缓
        Student s3 = student("张小强", "男", school1, c32, "20170920", "张父", "13811110003", null);
        Screening sc3 = screening(s3, batch, screener, screenDate, bd("4.6"), bd("4.7"),
                bd("-1.75"), bd("-1.25"), bd("-1.00"), bd("-0.50"), false, "ABNORMAL");
        ParentConfirmation pc3 = confirm(sc3, s3, "ON_HOLD", "POSTPONE", "近期家中有事，打算暑假再带孩子检查", "PENDING", null);
        s3.setStatus("ON_HOLD");
        issue(s3, sc3, "PHONE_UNREACHABLE", "家长暂缓复诊",
                "家长选择暂缓：近期家中有事，学校健康老师需持续提醒", "OPEN", healthTeacher, null);
        timelineAll(s3);

        // 4) 家长电话无法接通
        Student s4 = student("刘小雨", "女", school1, c32, "20171102", "刘父", "号码有误",
                "既往左眼弱视随访中");
        Screening sc4 = screening(s4, batch, screener, screenDate, bd("4.5"), bd("4.9"),
                bd("-2.00"), bd("-0.25"), bd("-0.50"), bd("0.00"), false, "ABNORMAL");
        ParentConfirmation pc4 = confirm(sc4, s4, "PENDING", null, null, "PENDING", null);
        s4.setStatus("AWAIT_CONFIRM");
        issue(s4, sc4, "PHONE_UNREACHABLE", "家长电话无法接通：号码有误",
                "筛查异常但联系方式无效，需班主任通过其他途径告知", "PROCESSING", headLi, null);
        timelineAll(s4);

        // 5) 筛查请假
        Student s5 = student("陈乐乐", "男", school1, c32, "20170715", "陈母", "13811110005", null);
        Screening sc5 = new Screening();
        sc5.setBatchId(batch.getId()); sc5.setStudentId(s5.getId());
        sc5.setAbsent(true); sc5.setAbsentReason("当日感冒请假");
        sc5.setResult("SUSPECTED"); sc5.setNeedRetest(true);
        sc5.setDeviceCalibNo("CAL-2026-018");
        sc5.setScreenerId(screener.getId());
        sc5.setScreeningTime(screenDate.atTime(10, 0));
        screeningRepo.save(sc5);
        s5.setLatestScreeningId(sc5.getId()); s5.setStatus("PENDING_SCREENING"); studentRepo.save(s5);
        issue(s5, sc5, "ABSENT", "筛查请假：当日感冒请假", "需安排补筛", "OPEN", screener, null);
        timeline.event(s5.getId(), "SCREENING_ABSENT", "筛查请假", "当日感冒请假，待补筛",
                "陈筛查员", "SCREENER", "SCREENING", sc5.getId());

        // 6) 复诊结果与筛查差异较大
        Student s6 = student("赵大宝", "男", school1, c41, "20160404", "赵父", "13811110006", null);
        Screening sc6 = screening(s6, batch, screener, screenDate, bd("4.4"), bd("4.9"),
                bd("-0.50"), bd("0.00"), bd("-0.25"), bd("0.00"), false, "ABNORMAL");
        ParentConfirmation pc6 = confirm(sc6, s6, "CONFIRMED", "COUNTY_HOSPITAL", null, "APPROVED", healthTeacher);
        Referral r6 = referral(s6, sc6, pc6, hospital, screener);
        RevisitDiagnosis d6 = diagnosis(s6, r6, hospital, doctor, screenDate.plusDays(15),
                true, true, true, "HIGH", bd("-3.50"), bd("-1.00"), bd("-1.50"), bd("-0.50"), 6, true,
                "散瞳后确认右眼高度近视，筛查设备当日校准异常，建议重新校准");
        s6.setStatus("REVISIT_DONE");
        issue(s6, sc6, "RESULT_DIVERGENCE", "复诊结果与筛查差异较大",
                "筛查右眼球镜 -0.50D，散瞳后 -3.50D，差异 3.00D；已排查设备校准", "OPEN",
                doctor, null);
        timelineAll(s6);

        // 7) 补助资格争议
        Student s7 = student("孙小花", "女", school1, c32, "20171225", "孙母", "13811110007", null);
        Screening sc7 = screening(s7, batch, screener, screenDate, bd("4.7"), bd("4.7"),
                bd("-1.00"), bd("-1.00"), bd("0.00"), bd("0.00"), false, "ABNORMAL");
        ParentConfirmation pc7 = confirm(sc7, s7, "CONFIRMED", "COUNTY_HOSPITAL", null, "APPROVED", healthTeacher);
        Referral r7 = referral(s7, sc7, pc7, hospital, screener);
        RevisitDiagnosis d7 = diagnosis(s7, r7, hospital, doctor, screenDate.plusDays(10),
                true, false, false, "LOW", bd("-1.00"), bd("-1.00"), bd("0"), bd("0"), 6, false,
                "度数较低，按现行标准暂不纳入补助");
        Glasses g7 = glasses(s7, d7, shop, "SUBSIDY_SHOP", "FITTED", bd("420.00"), screenDate.plusDays(18));
        Subsidy sub7 = subsidy(s7, g7, d7, bd("300.00"), "DISPUTED", optician, healthTeacher);
        sub7.setDecisionNote("争议复核：家长认为孩子已确诊近视应享补助，健康老师支持上报");
        subsidyRepo.save(sub7);
        issue(s7, sc7, "SUBSIDY_DISPUTE", "补助资格争议待复核",
                "医生判定不符合现行补助标准，家长与学校申请复核", "OPEN", healthTeacher, null);
        timelineAll(s7);

        // 8) 配镜后不适
        Student s8 = student("周小杰", "男", school1, c31, "20170218", "周父", "13811110008", null);
        Screening sc8 = screening(s8, batch, screener, screenDate, bd("4.6"), bd("4.6"),
                bd("-1.50"), bd("-1.50"), bd("-0.75"), bd("-0.75"), true, "ABNORMAL");
        ParentConfirmation pc8 = confirm(sc8, s8, "CONFIRMED", "COUNTY_HOSPITAL", null, "APPROVED", healthTeacher);
        Referral r8 = referral(s8, sc8, pc8, hospital, screener);
        RevisitDiagnosis d8 = diagnosis(s8, r8, hospital, doctor, screenDate.plusDays(9),
                true, true, true, "MEDIUM", bd("-2.00"), bd("-2.00"), bd("-0.75"), bd("-0.75"), 6, true,
                null);
        Glasses g8 = glasses(s8, d8, shop, "SUBSIDY_SHOP", "DISCOMFORT_REPORTED", bd("500.00"),
                screenDate.plusDays(17));
        g8.setDiscomfortDate(LocalDate.now().minusDays(10));
        g8.setDiscomfortNote("戴镜头晕、看地面变形");
        glassesRepo.save(g8);
        Subsidy sub8 = subsidy(s8, g8, d8, bd("300.00"), "APPROVED", optician, edu);
        issue(s8, sc8, "GLASSES_DISCOMFORT", "配镜后不适：戴镜头晕、看地面变形",
                "疑似瞳距/镜架问题，眼镜店已重新调试", "PROCESSING", healthTeacher, null);
        timelineAll(s8);

        // 9) 半年复查未到场（逾期）
        Student s9 = student("吴天天", "男", school2, c31b, "20161230", "吴父", "13811110009",
                "去年已配镜");
        ScreeningBatch b2 = batch2;
        Screening sc9 = screening(s9, b2, screener, LocalDate.now().minusDays(40), bd("4.5"), bd("4.5"),
                bd("-2.25"), bd("-2.25"), bd("-0.50"), bd("-0.50"), true, "ABNORMAL");
        ParentConfirmation pc9 = confirm(sc9, s9, "CONFIRMED", "PARTNER_CLINIC", null, "APPROVED", healthTeacher);
        Referral r9 = referral(s9, sc9, pc9, clinic, screener);
        RevisitDiagnosis d9 = diagnosis(s9, r9, clinic, optom, LocalDate.now().minusDays(35),
                true, false, false, "MEDIUM", bd("-2.50"), bd("-2.50"), bd("-0.50"), bd("-0.50"), 6, true,
                null);
        FollowUp f9 = followUp(s9, d9, LocalDate.now().minusDays(7), "NOTIFIED", null,
                "已短信提醒，家长未预约");
        s9.setStatus("AWAIT_REVISIT");
        intervention(s9, c31b, school2, "PARENT_NOTICE", "复查到期电话+短信提醒",
                "半年复查逾期", LocalDate.now().minusDays(7), "PLANNED", headZhao, null, null);
        timelineAll(s9);

        // 10) 筛查正常
        Student s10 = student("郑好", "女", school1, c31, "20170808", "郑母", "13811110010", null);
        screening(s10, batch, screener, screenDate, bd("5.1"), bd("5.1"),
                bd("0.00"), bd("0.00"), bd("0.00"), bd("0.00"), false, "NORMAL");
        s10.setStatus("CLOSED");
        timeline.event(s10.getId(), "SCREENING_NORMAL", "筛查正常", "双眼 5.1，继续保持户外活动",
                "陈筛查员", "SCREENER", "SCREENING", s10.getLatestScreeningId());

        // 11) 需要复测（可疑）
        Student s11 = student("钱多多", "男", school1, c32, "20171010", "钱父", "13811110011", null);
        Screening sc11 = screening(s11, batch, screener, screenDate, bd("4.9"), bd("5.0"),
                bd("-0.25"), bd("0.00"), bd("0.00"), bd("0.00"), false, "SUSPECTED");
        sc11.setNeedRetest(true);
        screeningRepo.save(sc11);
        s11.setStatus("SCREENED_ABNORMAL");
        timeline.event(s11.getId(), "SCREENING_RETEST", "需复测",
                "视力临界，建议 2 周后复测，设备校准编号 CAL-2026-018",
                "陈筛查员", "SCREENER", "SCREENING", sc11.getId());

        // 12) 刚异常待家长确认
        Student s12 = student("冯小美", "女", school1, c41, "20160606", "冯父", "13811110012", null);
        Screening sc12 = screening(s12, batch, screener, screenDate, bd("4.7"), bd("4.8"),
                bd("-1.25"), bd("-1.00"), bd("-0.25"), bd("0.00"), false, "ABNORMAL");
        ParentConfirmation pc12 = confirm(sc12, s12, "PENDING", null, null, "PENDING", null);
        s12.setStatus("AWAIT_CONFIRM");
        notif(s12, "视力筛查结果告知与复诊确认", "冯小美家长您好，孩子筛查异常，请确认复诊安排", screenDate);

        // 13) 家长已确认待学校复核
        Student s13 = student("褚国强", "男", school1, c41, "20160909", "褚父", "13811110013", null);
        Screening sc13 = screening(s13, batch, screener, screenDate, bd("4.6"), bd("4.7"),
                bd("-1.50"), bd("-1.25"), bd("-0.50"), bd("-0.25"), false, "ABNORMAL");
        ParentConfirmation pc13 = confirm(sc13, s13, "CONFIRMED", "COUNTY_HOSPITAL", null, "PENDING", null);
        s13.setStatus("AWAIT_REVIEW");

        // 14) 已转诊待复诊
        Student s14 = student("卫子轩", "男", school1, c41, "20161111", "卫父", "13811110014", null);
        Screening sc14 = screening(s14, batch, screener, screenDate, bd("4.5"), bd("4.6"),
                bd("-1.75"), bd("-1.50"), bd("-0.75"), bd("-0.50"), false, "ABNORMAL");
        ParentConfirmation pc14 = confirm(sc14, s14, "CONFIRMED", "PARTNER_CLINIC", null, "APPROVED", healthTeacher);
        referral(s14, sc14, pc14, clinic, screener);
        s14.setStatus("AWAIT_REVISIT");

        studentRepo.saveAll(java.util.List.of(s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, s11, s12, s13, s14));

        // 批次统计回算
        for (ScreeningBatch b : java.util.List.of(batch, batch2)) {
            b.setTotalCount((int) screeningRepo.findByBatchId(b.getId()).stream()
                    .filter(x -> !Boolean.TRUE.equals(x.getAbsent())).count());
            b.setAbnormalCount((int) screeningRepo.countByBatchIdAndResult(b.getId(), "ABNORMAL"));
            batchRepo.save(b);
        }
    }

    // ---------------- helpers ----------------

    private Organization org(String name, String type, String area, String contact, String phone) {
        Organization o = new Organization();
        o.setName(name); o.setOrgType(type); o.setArea(area);
        o.setContactName(contact); o.setContactPhone(phone);
        return orgRepo.save(o);
    }

    private AppUser user(String username, String realName, String role, Organization org, String phone) {
        AppUser u = new AppUser();
        u.setUsername(username);
        u.setPasswordHash(encoder.encode("123456"));
        u.setRealName(realName);
        u.setRole(role);
        u.setPhone(phone);
        if (org != null) u.setOrganizationId(org.getId());
        u.setActive(true);
        return userRepo.save(u);
    }

    private SchoolClass clazz(Organization school, String grade, String name, AppUser head, String phone) {
        SchoolClass c = new SchoolClass();
        c.setSchoolId(school.getId());
        c.setGrade(grade); c.setClassName(name);
        if (head != null) c.setHeadTeacherId(head.getId());
        c.setHeadTeacherName(head == null ? null : head.getRealName());
        c.setHeadTeacherPhone(phone);
        return classRepo.save(c);
    }

    private Student student(String name, String gender, Organization school, SchoolClass c,
                            String birth, String parent, String phone, String past) {
        Student s = new Student();
        s.setName(name); s.setGender(gender);
        s.setBirthDate(LocalDate.of(Integer.parseInt(birth.substring(0, 4)),
                Integer.parseInt(birth.substring(4, 6)), Integer.parseInt(birth.substring(6, 8))));
        s.setSchoolId(school.getId()); s.setClassId(c.getId()); s.setGrade(c.getGrade());
        s.setStudentNo("S" + birth + String.format("%03d", studentRepo.count() + 1));
        s.setParentName(parent); s.setParentPhone(phone); s.setPastVisionNote(past);
        s.setStatus("PENDING_SCREENING");
        s.setCreatedAt(LocalDateTime.now()); s.setUpdatedAt(LocalDateTime.now());
        return studentRepo.save(s);
    }

    private Screening screening(Student s, ScreeningBatch batch, AppUser screener, LocalDate date,
                                BigDecimal nod, BigDecimal nos, BigDecimal sod, BigDecimal sos,
                                BigDecimal cod, BigDecimal cos, boolean wearing, String result) {
        Screening sc = new Screening();
        sc.setBatchId(batch.getId()); sc.setStudentId(s.getId());
        sc.setAbsent(false);
        sc.setNakedOd(nod); sc.setNakedOs(nos);
        sc.setSphOd(sod); sc.setSphOs(sos);
        sc.setCylOd(cod); sc.setCylOs(cos);
        sc.setAxisOd(180); sc.setAxisOs(175);
        sc.setWearingGlasses(wearing);
        sc.setHoursOutdoor(bd("1.0")); sc.setScreenHours(bd("2.5"));
        sc.setReadingDistanceOk(false); sc.setEyeExerciseDaily(true);
        sc.setParentalMyopia(false);
        sc.setQuestionnaireNote("问卷：课间偶尔远眺，睡前使用手机");
        sc.setNeedRetest(false);
        sc.setDeviceCalibNo("CAL-2026-018");
        sc.setResult(result);
        sc.setScreenerId(screener.getId());
        sc.setScreeningTime(date.atTime(9, 30));
        screeningRepo.save(sc);
        s.setLatestScreeningId(sc.getId());
        studentRepo.save(s);
        return sc;
    }

    private ParentConfirmation confirm(Screening sc, Student s, String status, String choice,
                                       String reason, String review, AppUser reviewer) {
        ParentConfirmation pc = new ParentConfirmation();
        pc.setScreeningId(sc.getId()); pc.setStudentId(s.getId());
        pc.setStatus(status); pc.setChoice(choice); pc.setReason(reason);
        pc.setNotifiedAt(sc.getScreeningTime());
        if (!"PENDING".equals(status)) {
            pc.setConfirmedAt(sc.getScreeningTime().plusDays(1));
            pc.setDecidedByName(s.getParentName());
        }
        pc.setSchoolReviewStatus(review);
        if (reviewer != null && "APPROVED".equals(review)) {
            pc.setSchoolReviewBy(reviewer.getId());
            pc.setSchoolReviewAt(sc.getScreeningTime().plusDays(2));
            pc.setSchoolReviewNote("健康老师复核确认");
        }
        return confirmationRepo.save(pc);
    }

    private Referral referral(Student s, Screening sc, ParentConfirmation pc, Organization target, AppUser by) {
        Referral r = new Referral();
        r.setStudentId(s.getId()); r.setScreeningId(sc.getId()); r.setConfirmationId(pc.getId());
        r.setTargetOrgId(target.getId());
        r.setTargetType("HOSPITAL".equals(target.getOrgType()) ? "COUNTY_HOSPITAL" : "PARTNER_CLINIC");
        r.setStatus("PENDING");
        r.setIssuedBy(by.getId());
        r.setIssuedAt(sc.getScreeningTime().plusDays(2));
        return referralRepo.save(r);
    }

    private RevisitDiagnosis diagnosis(Student s, Referral r, Organization org, AppUser doc, LocalDate date,
                                       boolean trueMyopia, boolean needDil, boolean dilDone, String degree,
                                       BigDecimal sod, BigDecimal sos, BigDecimal cod, BigDecimal cos,
                                       int cycle, boolean subsidy, String advice) {
        RevisitDiagnosis d = new RevisitDiagnosis();
        d.setStudentId(s.getId());
        if (r != null) { d.setReferralId(r.getId()); r.setStatus("COMPLETED"); r.setCompletedAt(date.atTime(11, 0)); referralRepo.save(r); }
        d.setOrgId(org.getId()); d.setDoctorId(doc.getId()); d.setDoctorName(doc.getRealName());
        d.setVisitDate(date);
        d.setTrueMyopia(trueMyopia); d.setNeedMydriasis(needDil); d.setMydriasisDone(dilDone);
        d.setDegree(degree);
        d.setRxSphOd(sod); d.setRxSphOs(sos); d.setRxCylOd(cod); d.setRxCylOs(cos);
        d.setRxAxisOd(180); d.setRxAxisOs(175); d.setPd(bd("58.5"));
        d.setRevisitCycleMonths(cycle); d.setSubsidyEligible(subsidy);
        d.setAdvice(advice);
        diagnosisRepo.save(d);
        s.setLatestDiagnosisId(d.getId());
        s.setStatus("REVISIT_DONE");
        studentRepo.save(s);
        return d;
    }

    private Glasses glasses(Student s, RevisitDiagnosis d, Organization org, String source,
                            String status, BigDecimal price, LocalDate fitted) {
        Glasses g = new Glasses();
        g.setStudentId(s.getId()); g.setDiagnosisId(d.getId());
        if (org != null) { g.setOrgId(org.getId()); g.setOrgName(org.getName()); }
        g.setSource(source); g.setStatus(status); g.setPrice(price);
        g.setSubsidyAmount(BigDecimal.ZERO);
        g.setFittedDate(fitted);
        glassesRepo.save(g);
        s.setStatus("GLASSES_FITTED");
        studentRepo.save(s);
        return g;
    }

    private Subsidy subsidy(Student s, Glasses g, RevisitDiagnosis d, BigDecimal amount, String status,
                            AppUser applicant, AppUser decider) {
        Subsidy sub = new Subsidy();
        sub.setStudentId(s.getId()); sub.setGlassesId(g.getId()); sub.setDiagnosisId(d.getId());
        sub.setAmount(amount); sub.setStatus(status);
        sub.setApplyNote("眼镜店凭复诊处方发起补助申请");
        sub.setCreatedAt(LocalDateTime.now());
        if ("APPROVED".equals(status) || "PAID".equals(status)) {
            sub.setDecidedBy(decider.getId()); sub.setDecidedByName(decider.getRealName());
            sub.setDecisionNote("符合县域配镜补助标准");
            sub.setDecidedAt(LocalDateTime.now().minusDays(20));
        }
        if ("PAID".equals(status)) sub.setPaidAt(LocalDateTime.now().minusDays(18));
        return subsidyRepo.save(sub);
    }

    private void intervention(Student s, SchoolClass c, Organization school, String type, String title,
                              String detail, LocalDate planned, String status, AppUser operator,
                              AppUser verifier, String effect) {
        Intervention iv = new Intervention();
        iv.setStudentId(s.getId()); iv.setClassId(c.getId()); iv.setSchoolId(school.getId());
        iv.setType(type); iv.setTitle(title); iv.setDetail(detail);
        iv.setPlannedDate(planned); iv.setStatus(status);
        iv.setOperatorId(operator == null ? null : operator.getId());
        if (!"PLANNED".equals(status)) iv.setExecutedAt(planned.atTime(16, 0));
        iv.setEffectNote(effect);
        interventionRepo.save(iv);
        timeline.event(s.getId(), "INTERVENTION_" + status, title, detail,
                operator == null ? "学校" : operator.getRealName(), "HEAD_TEACHER", "INTERVENTION", iv.getId());
    }

    private FollowUp followUp(Student s, RevisitDiagnosis d, LocalDate due, String status,
                              LocalDate done, String note) {
        FollowUp f = new FollowUp();
        f.setStudentId(s.getId()); f.setDiagnosisId(d.getId());
        f.setDueDate(due); f.setStatus(status);
        if ("NOTIFIED".equals(status)) f.setRemindedAt(due.minusDays(3).atTime(9, 0));
        f.setDoneDate(done); f.setResultNote(note);
        return followUpRepo.save(f);
    }

    private void issue(Student s, Screening sc, String type, String title, String desc,
                       String status, AppUser creator, String resolution) {
        Issue i = new Issue();
        i.setStudentId(s.getId()); i.setScreeningId(sc == null ? null : sc.getId());
        i.setIssueType(type); i.setTitle(title); i.setDescription(desc);
        i.setStatus(status);
        i.setCreatedBy(creator == null ? null : creator.getId());
        i.setCreatedByName(creator == null ? "系统" : creator.getRealName());
        i.setCreatedByRole(creator == null ? "SYSTEM" : creator.getRole());
        if ("RESOLVED".equals(status)) {
            i.setResolution(resolution);
            i.setResolvedBy(creator == null ? null : creator.getId());
            i.setResolvedAt(LocalDateTime.now());
        }
        issueRepo.save(i);
        timeline.event(s.getId(), "ISSUE_" + status, "问题：" + title, desc,
                creator == null ? "系统" : creator.getRealName(),
                creator == null ? "SYSTEM" : creator.getRole(), "ISSUE", i.getId());
    }

    private void notif(Student s, String title, String content, LocalDate date) {
        Notification n = new Notification();
        n.setTitle(title); n.setContent(content); n.setStudentId(s.getId());
        n.setBizType("SCREENING_ABNORMAL"); n.setChannel("SMS");
        n.setPhone(s.getParentPhone()); n.setStatus("SENT"); n.setSentAt(date.atTime(12, 0));
        notificationRepo.save(n);
    }

    /** 为有完整链路的学生补齐各阶段时间线（与各 service 落库格式保持一致） */
    private void timelineAll(Student s) {
        screeningRepo.findFirstByStudentIdOrderByScreeningTimeDesc(s.getId()).ifPresent(sc -> {
            if (Boolean.TRUE.equals(sc.getAbsent())) return;
            if ("ABNORMAL".equals(sc.getResult())) {
                timeline.event(s.getId(), "SCREENING_ABNORMAL", "筛查异常，已告知家长",
                        "裸眼视力右" + sc.getNakedOd() + " 左" + sc.getNakedOs()
                                + "；球镜右" + sc.getSphOd() + " 左" + sc.getSphOs()
                                + "；散光右" + sc.getCylOd() + " 左" + sc.getCylOs()
                                + "；设备校准编号：" + sc.getDeviceCalibNo()
                                + "；短信已推送", "陈筛查员", "SCREENER", "SCREENING", sc.getId());
            }
        });
        confirmationRepo.findByScreeningId(s.getLatestScreeningId()).ifPresent(pc -> {
            if ("CONFIRMED".equals(pc.getStatus())) {
                timeline.event(s.getId(), "PARENT_CHOICE", "家长确认复诊",
                        "COUNTY_HOSPITAL".equals(pc.getChoice()) ? "选择县医院复诊" : "选择合作眼科门诊复诊",
                        s.getParentName(), "PARENT", "CONFIRMATION", pc.getId());
            }
            if ("APPROVED".equals(pc.getSchoolReviewStatus())) {
                timeline.event(s.getId(), "SCHOOL_REVIEW_APPROVE", "学校复核通过并转诊",
                        "健康老师复核确认", "王健康老师", "SCHOOL_HEALTH_TEACHER", "CONFIRMATION", pc.getId());
            }
        });
        diagnosisRepo.findFirstByStudentIdOrderByVisitDateDesc(s.getId()).ifPresent(d -> {
            timeline.event(s.getId(), "REVISIT_RESULT", "复诊结果回写",
                    "真性近视：" + (Boolean.TRUE.equals(d.getTrueMyopia()) ? "是" : "否")
                            + "；处方右" + d.getRxSphOd() + "/" + d.getRxCylOd()
                            + " 左" + d.getRxSphOs() + "/" + d.getRxCylOs()
                            + "；复查周期 " + d.getRevisitCycleMonths() + " 个月；配镜补助："
                            + (Boolean.TRUE.equals(d.getSubsidyEligible()) ? "符合" : "不符合"),
                    d.getDoctorName(), "DOCTOR", "DIAGNOSIS", d.getId());
        });
        glassesRepo.findByStudentIdOrderByFittedDateDesc(s.getId()).stream().findFirst().ifPresent(g -> {
            timeline.event(s.getId(), "SELF_EXTERNAL".equals(g.getSource()) ? "SELF_GLASSES" : "GLASSES_FITTED",
                    "SELF_EXTERNAL".equals(g.getSource()) ? "家长自行外配眼镜" : "配镜完成",
                    g.getOrgName() + "，费用 " + g.getPrice() + " 元",
                    "孙店长", "OPTICIAN", "GLASSES", g.getId());
        });
        subsidyRepo.findFirstByStudentIdOrderByCreatedAtDesc(s.getId()).ifPresent(sub -> {
            String t = switch (sub.getStatus()) {
                case "PAID" -> "配镜补助已发放";
                case "APPROVED" -> "补助审核通过";
                case "DISPUTED" -> "补助资格争议";
                default -> "配镜补助申请已发起";
            };
            timeline.event(s.getId(), "SUBSIDY_" + sub.getStatus(), t,
                    sub.getAmount() + " 元", sub.getDecidedByName() == null ? "孙店长" : sub.getDecidedByName(),
                    "EDU_BUREAU", "SUBSIDY", sub.getId());
        });
    }

    private BigDecimal bd(String v) { return new BigDecimal(v); }
}
