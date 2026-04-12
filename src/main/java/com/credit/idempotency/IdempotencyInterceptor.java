package com.credit.idempotency;

import com.credit.entity.IdempotencyRecord;
import com.credit.service.IdempotencyService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class IdempotencyInterceptor implements HandlerInterceptor {

    private final IdempotencyService idempotencyService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod handlerMethod) || !handlerMethod.hasMethodAnnotation(Idempotent.class)) {
            return true;
        }

        // 2.
        String key = request.getHeader(IdempotencyService.IDEMPOTENCY_KEY_HEADER);
        if (key == null || key.isBlank()) {
            return true;
        }

        IdempotencyRecord existing = idempotencyService.get(key).orElse(null);
        if (existing == null) {
            return true;
        }

        // 2.
        request.setAttribute("isDuplicate", true);
        response.setStatus(existing.getStatusCode());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(existing.getResponseBody());
        return false;
    }
}
