package com.credit.config;

import com.credit.idempotency.IdempotencyInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class IdempotencyConfig implements WebMvcConfigurer {

    private final IdempotencyInterceptor idempotencyInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 2. Подключаем interceptor ко всем запросам, а сам он уже решает, для каких методов включать идемпотентность.
        registry.addInterceptor(idempotencyInterceptor);
    }
}
