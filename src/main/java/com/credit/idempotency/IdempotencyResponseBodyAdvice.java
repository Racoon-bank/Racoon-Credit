package com.credit.idempotency;

import com.credit.service.IdempotencyService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@Component
@ControllerAdvice
@RequiredArgsConstructor
public class IdempotencyResponseBodyAdvice implements ResponseBodyAdvice<Object> {

    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return returnType.hasMethodAnnotation(Idempotent.class);
    }

    @Override
    public Object beforeBodyWrite(
            Object body,
            MethodParameter returnType,
            MediaType selectedContentType,
            Class<? extends HttpMessageConverter<?>> selectedConverterType,
            ServerHttpRequest request,
            ServerHttpResponse response
    ) {
        if (!(request instanceof ServletServerHttpRequest servletRequest)
                || !(response instanceof ServletServerHttpResponse servletResponse)) {
            return body;
        }

        String key = servletRequest.getServletRequest().getHeader(IdempotencyService.IDEMPOTENCY_KEY_HEADER);
        if (key == null || key.isBlank()) {
            return body;
        }

        int statusCode = servletResponse.getServletResponse().getStatus();
        if (statusCode >= 500) {
            return body;
        }

        // 2. Сохраняем только успешный ответ, чтобы повторный запрос с тем же ключом получил тот же результат.
        String responseBody = serializeBody(body);
        idempotencyService.save(key, responseBody, statusCode);
        return body;
    }

    private String serializeBody(Object body) {
        try {
            return body == null ? "" : objectMapper.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize idempotent response", e);
        }
    }
}
