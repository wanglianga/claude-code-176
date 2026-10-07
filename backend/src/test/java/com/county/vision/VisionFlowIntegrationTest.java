package com.county.vision;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 全链路集成测试（H2 PostgreSQL 兼容模式）：
 * 建档/导入 → 异常筛查自动告知 → 家长确认 → 学校复核转诊 → 复诊回写（差异预警/复查计划）
 * → 配镜 → 补助审核发放 → 学校干预 → 监管统计与统一时间线
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class VisionFlowIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired com.county.vision.repo.AppUserRepository userRepo;
    @Autowired com.county.vision.repo.OrganizationRepository orgRepo;
    @Autowired com.county.vision.repo.SchoolClassRepository classRepo;
    @Autowired PasswordEncoder encoder;

    Long schoolId, screenerOrgId, hospitalId, clinicId, shopId;
    Long classId;
    String screenerToken, teacherToken, doctorToken, optomToken, eduToken;

    @BeforeEach
    void setup() throws Exception {
        if (userRepo.count() == 0) {
            schoolId = createOrg("测试小学", "SCHOOL", "城东片区");
            screenerOrgId = createOrg("筛查中心", "SCREENING_AGENCY", "县城区");
            hospitalId = createOrg("县医院", "HOSPITAL", "县城区");
            clinicId = createOrg("合作门诊", "OPTOMETRY_CLINIC", "城东片区");
            shopId = createOrg("定点眼镜店", "OPTICAL_SHOP", "县城区");
            Long eduOrgId = createOrg("教体局", "EDU_BUREAU", "县城区");

            createUser("screener", "筛查员", "SCREENER", screenerOrgId);
            createUser("teacher", "健康老师", "SCHOOL_HEALTH_TEACHER", schoolId);
            createUser("doctor", "医生", "DOCTOR", hospitalId);
            createUser("optom", "店长", "OPTICIAN", shopId);
            createUser("edu", "科长", "EDU_BUREAU", eduOrgId);

            String body = """
                {"schoolId":%d,"grade":"三年级","className":"1班"}
                """.formatted(schoolId);
            MvcResult r = mvc.perform(post("/api/classes").header("Authorization", "Bearer " + login("teacher"))
                    .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isOk()).andReturn();
            classId = json.readTree(r.getResponse().getContentAsString()).get("id").asLong();
        } else {
            schoolId = orgRepo.findAll().stream()
                    .filter(o -> "测试小学".equals(o.getName())).findFirst().orElseThrow().getId();
            classId = classRepo.findBySchoolIdOrderByGradeAscClassNameAsc(schoolId).get(0).getId();
        }
        screenerToken = login("screener");
        teacherToken = login("teacher");
        doctorToken = login("doctor");
        optomToken = login("optom");
        eduToken = login("edu");
    }

    @Test
    void fullHealthManagementFlow() throws Exception {
        // 1. 建档
        Long studentId = createStudent("测试娃", "13800000001");

        // 2. 异常筛查录入
        Long batchId = createBatch("2026 筛查");
        String record = """
            {"batchId":%d,"studentId":%d,"absent":false,"nakedOd":4.6,"nakedOs":4.7,
             "sphOd":-1.50,"sphOs":-1.25,"cylOd":-0.50,"cylOs":-0.25,"wearingGlasses":false,
             "hoursOutdoor":1.0,"screenHours":3.0,"readingDistanceOk":false,"eyeExerciseDaily":true,
             "needRetest":false,"deviceCalibNo":"CAL-TEST-1"}
            """.formatted(batchId, studentId);
        MvcResult rr = mvc.perform(post("/api/screening/records").header("Authorization", "Bearer " + screenerToken)
                .contentType(MediaType.APPLICATION_JSON).content(record))
                .andExpect(status().isOk()).andReturn();
        Long screeningId = json.readTree(rr.getResponse().getContentAsString()).get("id").asLong();

        assertStudentStatus(studentId, "AWAIT_CONFIRM");

        // 3. 家长确认县医院
        JsonNode confirmations = getList("/api/screening/confirmations?status=PENDING", teacherToken);
        Long confirmationId = confirmations.get(0).get("confirmation").get("id").asLong();
        mvc.perform(post("/api/screening/confirmations/" + confirmationId + "/decision")
                .header("Authorization", "Bearer " + teacherToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"choice":"COUNTY_HOSPITAL","decidedByName":"测试家长"}"""))
                .andExpect(status().isOk());
        assertStudentStatus(studentId, "AWAIT_REVIEW");

        // 4. 学校复核通过 → 转诊
        mvc.perform(post("/api/screening/confirmations/" + confirmationId + "/review")
                .header("Authorization", "Bearer " + teacherToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"approved":true,"note":"同意"}"""))
                .andExpect(status().isOk());
        assertStudentStatus(studentId, "AWAIT_REVISIT");

        // 5. 复诊回写：确诊近视、散瞳、补助资格、差异较大（-1.50 vs -3.00）
        String diagnosis = """
            {"studentId":%d,"visitDate":"%s","trueMyopia":true,"needMydriasis":true,"mydriasisDone":true,
             "degree":"MEDIUM","rxSphOd":-3.00,"rxSphOs":-2.75,"rxCylOd":-0.75,"rxCylOs":-0.50,
             "pd":58.0,"revisitCycleMonths":6,"subsidyEligible":true,"advice":"配镜+户外"}
            """.formatted(studentId, LocalDate.now().toString());
        mvc.perform(post("/api/revisit/diagnoses").header("Authorization", "Bearer " + doctorToken)
                .contentType(MediaType.APPLICATION_JSON).content(diagnosis))
                .andExpect(status().isOk());
        assertStudentStatus(studentId, "REVISIT_DONE");

        // 差异问题自动登记
        JsonNode divergence = getList("/api/issues?issueType=RESULT_DIVERGENCE&status=OPEN", teacherToken);
        org.junit.jupiter.api.Assertions.assertTrue(divergence.size() >= 1, "差异预警应自动登记");

        // 6. 合作店配镜 → 自动发起补助
        String glasses = """
            {"studentId":%d,"price":460.00}
            """.formatted(studentId);
        MvcResult gr = mvc.perform(post("/api/glasses").header("Authorization", "Bearer " + optomToken)
                .contentType(MediaType.APPLICATION_JSON).content(glasses))
                .andExpect(status().isOk()).andReturn();
        Long subsidyId = json.readTree(gr.getResponse().getContentAsString()).get("subsidyId").asLong();
        org.junit.jupiter.api.Assertions.assertTrue(subsidyId > 0);

        // 7. 补助审核 → 发放
        mvc.perform(post("/api/subsidies/" + subsidyId + "/decision")
                .header("Authorization", "Bearer " + eduToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"action":"APPROVE","note":"符合标准"}"""))
                .andExpect(status().isOk());
        mvc.perform(post("/api/subsidies/" + subsidyId + "/pay")
                .header("Authorization", "Bearer " + eduToken))
                .andExpect(status().isOk());
        assertStudentStatus(studentId, "SUBSIDY_PAID");

        // 8. 学校干预 → 执行 → 核验
        String iv = """
            {"studentId":%d,"type":"SEAT_ADJUSTMENT","title":"调至前排","plannedDate":"%s"}
            """.formatted(studentId, LocalDate.now().toString());
        MvcResult ir = mvc.perform(post("/api/interventions").header("Authorization", "Bearer " + teacherToken)
                .contentType(MediaType.APPLICATION_JSON).content(iv))
                .andExpect(status().isOk()).andReturn();
        Long ivId = json.readTree(ir.getResponse().getContentAsString()).get("ids").get(0).asLong();
        mvc.perform(post("/api/interventions/" + ivId + "/execute")
                .header("Authorization", "Bearer " + teacherToken)
                .contentType(MediaType.APPLICATION_JSON).content("""
                    {"effectNote":"已调整"}"""))
                .andExpect(status().isOk());
        mvc.perform(post("/api/interventions/" + ivId + "/verify")
                .header("Authorization", "Bearer " + eduToken)
                .contentType(MediaType.APPLICATION_JSON).content("""
                    {"effectNote":"核验通过"}"""))
                .andExpect(status().isOk());

        // 9. 复查计划已生成且可提醒
        JsonNode fus = getList("/api/followups?studentId=" + studentId, teacherToken);
        org.junit.jupiter.api.Assertions.assertEquals(1, fus.size());
        Long fuId = fus.get(0).get("followUp").get("id").asLong();
        mvc.perform(post("/api/followups/" + fuId + "/remind").header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isOk());

        // 10. 监管统计：复诊率 100%
        MvcResult ov = mvc.perform(get("/api/stats/overview").header("Authorization", "Bearer " + eduToken))
                .andExpect(status().isOk()).andReturn();
        JsonNode overview = json.readTree(ov.getResponse().getContentAsString());
        org.junit.jupiter.api.Assertions.assertEquals(100.0, overview.get("realRevisitRate").asDouble());
        org.junit.jupiter.api.Assertions.assertEquals(1, overview.get("subsidyPaidCount").asInt());

        // 11. 统一时间线串起全部环节
        MvcResult tl = mvc.perform(get("/api/students/" + studentId).header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isOk()).andReturn();
        String timelineText = tl.getResponse().getContentAsString();
        for (String key : new String[]{"SCREENING_ABNORMAL", "PARENT_CHOICE", "SCHOOL_REVIEW_APPROVE",
                "REVISIT_RESULT", "GLASSES_FITTED", "SUBSIDY_PAID", "INTERVENTION_VERIFIED"}) {
            org.junit.jupiter.api.Assertions.assertTrue(timelineText.contains(key), "时间线缺少事件：" + key);
        }
    }

    @Test
    void csvImportAndAbsentFlow() throws Exception {
        // CSV 导入：含一行缺列错误
        MockMultipartFile file = new MockMultipartFile("file", "students.csv",
                "text/csv",
                ("姓名,性别,出生日期,年级,班级,学籍号,家长姓名,家长电话,既往视力档案\n"
                        + "导入甲,男,2017-01-01,三年级,2班,I001,甲父,13800000002,无\n"
                        + "坏行,男\n").getBytes());
        MvcResult r = mvc.perform(multipart("/api/students/import").file(file)
                .param("schoolId", String.valueOf(schoolId))
                .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isOk()).andReturn();
        JsonNode res = json.readTree(r.getResponse().getContentAsString());
        org.junit.jupiter.api.Assertions.assertEquals(1, res.get("success").asInt());
        org.junit.jupiter.api.Assertions.assertEquals(1, res.get("failed").asInt());

        // 导入错误登记问题
        JsonNode importIssues = getList("/api/issues?issueType=IMPORT_ERROR", teacherToken);
        org.junit.jupiter.api.Assertions.assertTrue(importIssues.size() >= 1);

        // 请假流程
        Long studentId = createStudent("请假娃", "13800000003");
        Long batchId = createBatch("补筛批次");
        String absent = """
            {"batchId":%d,"studentId":%d,"absent":true,"absentReason":"发烧",
             "deviceCalibNo":"CAL-TEST-2"}
            """.formatted(batchId, studentId);
        mvc.perform(post("/api/screening/records").header("Authorization", "Bearer " + screenerToken)
                .contentType(MediaType.APPLICATION_JSON).content(absent))
                .andExpect(status().isOk());
        JsonNode absentIssues = getList("/api/issues?issueType=ABSENT&status=OPEN", teacherToken);
        org.junit.jupiter.api.Assertions.assertTrue(absentIssues.size() >= 1, "请假应登记问题待补筛");
    }

    // ---------- helpers ----------

    private Long createOrg(String name, String type, String area) {
        var o = new com.county.vision.entity.Organization();
        o.setName(name); o.setOrgType(type); o.setArea(area);
        return orgRepo.save(o).getId();
    }

    private void createUser(String username, String realName, String role, Long orgId) {
        var u = new com.county.vision.entity.AppUser();
        u.setUsername(username);
        u.setPasswordHash(encoder.encode("123456"));
        u.setRealName(realName);
        u.setRole(role);
        u.setOrganizationId(orgId);
        u.setActive(true);
        userRepo.save(u);
    }

    private String login(String username) throws Exception {
        MvcResult r = mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"%s","password":"123456"}""".formatted(username)))
                .andExpect(status().isOk()).andReturn();
        return json.readTree(r.getResponse().getContentAsString()).get("token").asText();
    }

    private Long createStudent(String name, String phone) throws Exception {
        String body = """
            {"classId":%d,"schoolId":%d,"name":"%s","gender":"男","grade":"三年级",
             "parentName":"家长","parentPhone":"%s"}
            """.formatted(classId, schoolId, name, phone);
        MvcResult r = mvc.perform(post("/api/students").header("Authorization", "Bearer " + teacherToken)
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andReturn();
        return json.readTree(r.getResponse().getContentAsString()).get("id").asLong();
    }

    private Long createBatch(String name) throws Exception {
        String body = """
            {"name":"%s","schoolId":%d,"grade":"三年级","screeningDate":"%s"}
            """.formatted(name, schoolId, LocalDate.now().toString());
        MvcResult r = mvc.perform(post("/api/screening/batches").header("Authorization", "Bearer " + screenerToken)
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andReturn();
        return json.readTree(r.getResponse().getContentAsString()).get("id").asLong();
    }

    private void assertStudentStatus(Long id, String expected) throws Exception {
        MvcResult r = mvc.perform(get("/api/students/" + id).header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isOk()).andReturn();
        org.junit.jupiter.api.Assertions.assertEquals(expected,
                json.readTree(r.getResponse().getContentAsString()).get("student").get("status").asText());
    }

    private JsonNode getList(String url, String token) throws Exception {
        MvcResult r = mvc.perform(get(url).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn();
        return json.readTree(r.getResponse().getContentAsString());
    }
}
