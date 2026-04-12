package com.credit.config;

import com.credit.metrics.LogBuffer;
import com.credit.metrics.LogDto;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.regex.Pattern;

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
@RequiredArgsConstructor
public class MetricsFilter extends OncePerRequestFilter {

    private static final Pattern UUID_PATTERN = Pattern.compile("\\b[0-9a-fA-F\\-]{36}\\b");
    private final LogBuffer logBuffer;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        // 4.
        long start = System.currentTimeMillis();
        String errorMessage = null;
        int statusCode = HttpServletResponse.SC_OK;

        try {
            filterChain.doFilter(request, response);
            statusCode = response.getStatus();
        } catch (Exception ex) {
            statusCode = response.getStatus() > 0 ? response.getStatus() : HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
            errorMessage = ex.getMessage();
            throw ex;
        } finally {
            long durationMs = System.currentTimeMillis() - start;
            saveLog(request, statusCode, durationMs, errorMessage);
        }
    }

    private void saveLog(HttpServletRequest request, int statusCode, long durationMs, String message) {
        String traceId = (String) request.getAttribute(TraceFilter.TRACE_ID_ATTRIBUTE);
        boolean isDuplicate = Boolean.TRUE.equals(request.getAttribute("isDuplicate"));

        // 4.
        LogDto log = new LogDto(
                "Credit",
                normalizePath(request.getRequestURI()),
                request.getMethod(),
                statusCode,
                (int) durationMs,
                traceId,
                OffsetDateTime.now(ZoneOffset.UTC).toString(),
                message,
                isDuplicate
        );
        logBuffer.add(log);
    }

    private String normalizePath(String path) {
        // 4.
        String normalized = UUID_PATTERN.matcher(path).replaceAll("{id}");
        return normalized.replaceAll("/\\d+", "/{id}");
    }
}
