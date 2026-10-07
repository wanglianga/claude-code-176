package com.county.vision.security;

import com.county.vision.domain.enums.Role;
import lombok.Getter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

/** 当前登录用户上下文（从 JWT 解析） */
@Getter
public class LoginUser extends UsernamePasswordAuthenticationToken {
    private final Long userId;
    private final String username;
    private final Role role;
    private final Long schoolId;

    public LoginUser(Long userId, String username, Role role, Long schoolId) {
        super(username, null, List.of(new SimpleGrantedAuthority("ROLE_" + role.name())));
        this.userId = userId;
        this.username = username;
        this.role = role;
        this.schoolId = schoolId;
    }
}
