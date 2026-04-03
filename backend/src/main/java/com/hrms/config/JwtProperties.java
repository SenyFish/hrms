package com.hrms.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "hrms.jwt")
public class JwtProperties {
    private String secret = "dev-secret-change-me";
    private long expireHours = 168;
}
