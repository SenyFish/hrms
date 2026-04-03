package com.hrms.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "hrms")
public class UploadProperties {
    private String uploadDir = System.getProperty("user.home") + "/hrms-uploads";
}
