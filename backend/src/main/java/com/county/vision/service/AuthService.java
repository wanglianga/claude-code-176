package com.county.vision.service;

import com.county.vision.common.BusinessException;
import com.county.vision.domain.entity.UserAccount;
import com.county.vision.domain.repository.UserAccountRepository;
import com.county.vision.dto.AuthDtos;
import com.county.vision.security.JwtTokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserAccountRepository userRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwt;

    public AuthService(UserAccountRepository userRepo, PasswordEncoder passwordEncoder, JwtTokenProvider jwt) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
        this.jwt = jwt;
    }

    @Transactional(readOnly = true)
    public AuthDtos.LoginResponse login(AuthDtos.LoginRequest req) {
        UserAccount user = userRepo.findByUsername(req.getUsername())
                .orElseThrow(() -> new BusinessException(401, "用户名或密码错误"));
        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new BusinessException(401, "用户名或密码错误");
        }
        Long schoolId = user.getSchool() != null ? user.getSchool().getId() : null;
        String token = jwt.create(user.getId(), user.getUsername(), user.getRole().name(), schoolId);

        AuthDtos.LoginResponse resp = new AuthDtos.LoginResponse();
        resp.setToken(token);
        resp.setUserId(user.getId());
        resp.setUsername(user.getUsername());
        resp.setRealName(user.getRealName());
        resp.setRole(user.getRole().name());
        resp.setRoleLabel(user.getRole().getLabel());
        resp.setSchoolId(schoolId);
        resp.setSchoolName(user.getSchool() != null ? user.getSchool().getName() : null);
        resp.setOrgName(user.getOrgName());
        return resp;
    }
}
