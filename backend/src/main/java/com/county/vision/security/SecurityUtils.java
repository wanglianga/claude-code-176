package com.county.vision.security;

import com.county.vision.common.BusinessException;
import org.springframework.security.core.context.SecurityContextHolder;

public abstract class SecurityUtils {

    public static LoginUser current() {
        Object principal = SecurityContextHolder.getContext().getAuthentication();
        if (principal instanceof LoginUser loginUser) {
            return loginUser;
        }
        throw new BusinessException(401, "未登录");
    }

    public static String actorLabel() {
        try {
            LoginUser u = current();
            return u.getRole().getLabel() + "(" + u.getUsername() + ")";
        } catch (Exception e) {
            return "系统";
        }
    }
}
