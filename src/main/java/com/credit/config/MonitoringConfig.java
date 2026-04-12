package com.credit.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@RequiredArgsConstructor
public class MonitoringConfig {

    private final MonitoringProperties monitoringProperties;

    @Bean
    public RestClient monitoringRestClient() {
        // 4. Отдельный HTTP-клиент нужен для отправки логов и метрик в сервис мониторинга.
        return RestClient.builder()
                .baseUrl(monitoringProperties.getUrl())
                .build();
    }
}
