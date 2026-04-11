package com.credit.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class InstabilityFilter extends OncePerRequestFilter {

    private static final double ODD_MINUTE_ERROR_PROBABILITY = 0.3;
    private static final double EVEN_MINUTE_ERROR_PROBABILITY = 0.7;

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
