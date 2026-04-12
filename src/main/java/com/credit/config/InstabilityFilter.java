package com.credit.config;

import com.credit.entity.IdempotencyRecord;
import com.credit.service.IdempotencyService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;

@Component
@RequiredArgsConstructor
public class InstabilityFilter extends OncePerRequestFilter {

    private static final double ODD_MINUTE_ERROR_PROBABILITY = 0.3;
    private static final double EVEN_MINUTE_ERROR_PROBABILITY = 0.7;
    private final IdempotencyService idempotencyService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String path = request.getRequestURI();
        if (path.startsWith("/swagger-ui")
                || path.equals("/swagger-ui.html")
                || path.startsWith("/api-docs")) {
            filterChain.doFilter(request, response);
            return;
        }

        String idempotencyKey = request.getHeader(IdempotencyService.IDEMPOTENCY_KEY_HEADER);
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            IdempotencyRecord existing = idempotencyService.get(idempotencyKey).orElse(null);
            if (existing != null) {
                // 2. Повтор идемпотентного запроса должен вернуть сохраненный ответ, а не снова попасть под случайную 500.
                response.setStatus(existing.getStatusCode());
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write(existing.getResponseBody());
                return;
            }
        }

        int minute = LocalDateTime.now().getMinute();
        double errorProbability = minute % 2 == 0
                ? EVEN_MINUTE_ERROR_PROBABILITY
                : ODD_MINUTE_ERROR_PROBABILITY;

        if (ThreadLocalRandom.current().nextDouble() < errorProbability) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("""
                    {
                      "status": 500,
                      "error": "Internal Server Error",
                      "message": "Simulated unstable service"
                    }
                    """);
            return;
        }

        filterChain.doFilter(request, response);
    }
}
