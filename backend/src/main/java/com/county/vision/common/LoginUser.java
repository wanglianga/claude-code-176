package com.county.vision.common;

import java.util.List;

/** 当前登录用户信息（由 JWT 解析，挂载在请求上下文） */
public record LoginUser(Long id, String username, String realName, String role, Long orgId) {

    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    public static void set(LoginUser u) { HOLDER.set(u); }
    public static LoginUser get() { return HOLDER.get(); }
    public static void clear() { HOLDER.remove(); }

    /** 县域部门角色 */
    public static final List<String> COUNTY_ROLES = List.of("PLATFORM_ADMIN", "EDU_BUREAU", "HEALTH_COMMISSION");
}
