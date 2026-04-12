package com.credit.config;

import com.credit.idempotency.Idempotent;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerMethod;

@Configuration
public class IdempotencyOpenApiConfig {

    @Bean
    public OperationCustomizer idempotencyHeaderCustomizer() {
        return (Operation operation, HandlerMethod handlerMethod) -> {
            if (!handlerMethod.hasMethodAnnotation(Idempotent.class)) {
                return operation;
            }

            if (operation.getParameters() != null && operation.getParameters().stream()
                    .anyMatch(parameter -> "Idempotency-Key".equalsIgnoreCase(parameter.getName()))) {
                return operation;
            }

            // 2.
            operation.addParametersItem(new Parameter()
                    .in("header")
                    .required(true)
                    .name("Idempotency-Key")
                    .description("Idempotency key for safe retries")
                    .schema(new StringSchema()));
            return operation;
        };
    }
}
