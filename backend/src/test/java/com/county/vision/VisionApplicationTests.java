package com.county.vision;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class VisionApplicationTests {

    @Test
    void contextLoads() {
        // 验证 Spring 上下文（JPA/Security/缓存/所有服务与控制器）可正常装配
    }
}
