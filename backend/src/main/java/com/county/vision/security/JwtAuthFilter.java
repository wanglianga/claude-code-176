package com.county.vision.security;

import com.county.vision.domain.enums.Role;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;

    public JwtAuthFilter(JwtTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                Claims claims = tokenProvider.parse(token);
                Long uid = claims.get("uid", Number.class).longValue();
                Long schoolId = claims.get("schoolId") == null ? null
                        : claims.get("schoolId", Number.class).longValue();
                Role role = Role.valueOf(claims.get("role", String.class));
                LoginUser loginUser = new LoginUser(uid, claims.getSubject(), role, schoolId);
                SecurityContextHolder.getContext().setAuthentication(loginUser);
            } catch (Exception ignore) {
                // token 非法则按匿名处理
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(request, response);
    }
}
