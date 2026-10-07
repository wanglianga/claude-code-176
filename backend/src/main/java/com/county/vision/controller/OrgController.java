package com.county.vision.controller;

import com.county.vision.common.GlobalExceptionHandler;
import com.county.vision.common.LoginUser;
import com.county.vision.entity.AppUser;
import com.county.vision.entity.Organization;
import com.county.vision.entity.SchoolClass;
import com.county.vision.repo.AppUserRepository;
import com.county.vision.repo.OrganizationRepository;
import com.county.vision.repo.SchoolClassRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 机构、班级、用户管理 */
@RestController
@RequestMapping("/api")
public class OrgController {

    private final OrganizationRepository orgRepo;
    private final SchoolClassRepository classRepo;
    private final AppUserRepository userRepo;
    private final PasswordEncoder encoder;

    public OrgController(OrganizationRepository orgRepo, SchoolClassRepository classRepo,
                        AppUserRepository userRepo, PasswordEncoder encoder) {
        this.orgRepo = orgRepo;
        this.classRepo = classRepo;
        this.userRepo = userRepo;
        this.encoder = encoder;
    }

    // ---------- 机构 ----------

    @GetMapping("/organizations")
    public List<Organization> orgs(@RequestParam(required = false) String orgType,
                                   @RequestParam(required = false) String area) {
        if (orgType != null) return orgRepo.findByOrgType(orgType);
        if (area != null) return orgRepo.findByArea(area);
        return orgRepo.findAll();
    }

    @PostMapping("/organizations")
    public Organization createOrg(@RequestBody Organization body) {
        if (body.getName() == null || body.getOrgType() == null) {
            throw GlobalExceptionHandler.badRequest("机构名称与类型必填");
        }
        return orgRepo.save(body);
    }

    // ---------- 班级 ----------

    @GetMapping("/classes")
    public List<SchoolClass> classes(@RequestParam(required = false) Long schoolId) {
        if (schoolId != null) return classRepo.findBySchoolIdOrderByGradeAscClassNameAsc(schoolId);
        LoginUser me = LoginUser.get();
        if (me.orgId() != null) return classRepo.findBySchoolIdOrderByGradeAscClassNameAsc(me.orgId());
        return classRepo.findAll();
    }

    @PostMapping("/classes")
    public SchoolClass createClass(@RequestBody SchoolClass body) {
        if (body.getSchoolId() == null) {
            LoginUser me = LoginUser.get();
            body.setSchoolId(me.orgId());
        }
        if (body.getSchoolId() == null) throw GlobalExceptionHandler.badRequest("缺少学校");
        return classRepo.save(body);
    }

    // ---------- 用户 ----------

    @GetMapping("/users")
    public List<Map<String, Object>> users(@RequestParam(required = false) String role,
                                           @RequestParam(required = false) Long organizationId) {
        List<AppUser> list;
        if (role != null && organizationId != null) list = userRepo.findByRoleAndOrganizationId(role, organizationId);
        else if (role != null) list = userRepo.findByRole(role);
        else if (organizationId != null) list = userRepo.findByOrganizationId(organizationId);
        else list = userRepo.findAll();
        return list.stream().map(this::toView).toList();
    }

    public record UserReq(String username, String password, String realName, String role,
                          String phone, Long organizationId, Boolean active) {}

    @PostMapping("/users")
    public Map<String, Object> createUser(@RequestBody UserReq req) {
        if (req.username() == null || req.password() == null || req.realName() == null || req.role() == null) {
            throw GlobalExceptionHandler.badRequest("用户名、密码、姓名、角色必填");
        }
        userRepo.findByUsername(req.username()).ifPresent(x -> {
            throw GlobalExceptionHandler.conflict("用户名已存在");
        });
        AppUser u = new AppUser();
        u.setUsername(req.username());
        u.setPasswordHash(encoder.encode(req.password()));
        u.setRealName(req.realName());
        u.setRole(req.role());
        u.setPhone(req.phone());
        u.setOrganizationId(req.organizationId());
        u.setActive(req.active() == null ? Boolean.TRUE : req.active());
        return toView(userRepo.save(u));
    }

    @PutMapping("/users/{id}")
    public Map<String, Object> updateUser(@PathVariable Long id, @RequestBody UserReq req) {
        AppUser u = userRepo.findById(id).orElseThrow(() -> GlobalExceptionHandler.notFound("用户不存在"));
        if (req.realName() != null) u.setRealName(req.realName());
        if (req.role() != null) u.setRole(req.role());
        if (req.phone() != null) u.setPhone(req.phone());
        if (req.organizationId() != null) u.setOrganizationId(req.organizationId());
        if (req.active() != null) u.setActive(req.active());
        if (req.password() != null && !req.password().isBlank()) u.setPasswordHash(encoder.encode(req.password()));
        return toView(userRepo.save(u));
    }

    private Map<String, Object> toView(AppUser u) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", u.getId());
        m.put("username", u.getUsername());
        m.put("realName", u.getRealName());
        m.put("role", u.getRole());
        m.put("phone", u.getPhone());
        m.put("organizationId", u.getOrganizationId());
        m.put("active", u.getActive());
        if (u.getOrganizationId() != null) {
            orgRepo.findById(u.getOrganizationId()).ifPresent(o -> m.put("organizationName", o.getName()));
        }
        return m;
    }
}
