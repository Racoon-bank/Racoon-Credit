package com.credit.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "core-service")
@Data
public class CoreServiceProperties {
    private String url;
    private String apiKey;
}
