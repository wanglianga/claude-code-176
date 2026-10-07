package com.county.vision.controller;

import com.county.vision.common.GlobalExceptionHandler;
import com.county.vision.common.JwtUtil;
import com.county.vision.common.LoginUser;
import com.county.vision.entity.AppUser;
import com.county.vision.entity.Organization;
import com.county.vision.repo.AppUserRepository;
import com.county.vision.repo.OrganizationRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    public record LoginReq(@NotBlank String username, @NotBlank String password) {}

    private final AppUserRepository userRepo;
    private final OrganizationRepository orgRepo;
    private final PasswordEncoder encoder;
    private final JwtUtil jwt;

    public AuthController(AppUserRepository userRepo, OrganizationRepository orgRepo,
                         PasswordEncoder encoder, JwtUtil jwt) {
        this.userRepo = userRepo;
        this.orgRepo = orgRepo;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    @PostMapping("/login")
    public Map<String, Object> login(@Valid @RequestBody LoginReq req) {
        AppUser u = userRepo.findByUsername(req.username())
                .orElseThrow(() -> GlobalExceptionHandler.badRequest("用户名或密码错误"));
        if (!Boolean.TRUE.equals(u.getActive())) {
            throw GlobalExceptionHandler.forbidden("账号已停用");
        }
        if (!encoder.matches(req.password(), u.getPasswordHash())) {
            throw GlobalExceptionHandler.badRequest("用户名或密码错误");
        }
        String orgName = null;
        String orgType = null;
        String area = null;
        if (u.getOrganizationId() != null) {
            Organization o = orgRepo.findById(u.getOrganizationId()).orElse(null);
            if (o != null) {
                orgName = o.getName();
                orgType = o.getOrgType();
                area = o.getArea();
            }
        }
        String token = jwt.generate(u.getId(), u.getUsername(), u.getRole(), u.getOrganizationId());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("token", token);
        Map<String, Object> user = new LinkedHashMap<>();
        user.put("id", u.getId());
        user.put("username", u.getUsername());
        user.put("realName", u.getRealName());
        user.put("role", u.getRole());
        user.put("phone", u.getPhone());
        user.put("organizationId", u.getOrganizationId());
        user.put("organizationName", orgName);
        user.put("organizationType", orgType);
        user.put("area", area);
        result.put("user", user);
        return result;
    }

    @GetMapping("/me")
    public LoginUser me() {
        return LoginUser.get();
    }
}
