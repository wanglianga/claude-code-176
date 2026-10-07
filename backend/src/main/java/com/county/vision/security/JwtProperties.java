package com.county.vision.security;

import com.county.vision.domain.enums.Role;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {
    /** Base64 secret */
    private String secret;
    private int expireHours = 12;
}
