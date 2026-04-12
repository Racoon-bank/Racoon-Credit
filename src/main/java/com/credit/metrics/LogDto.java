package com.credit.metrics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LogDto {
    private String serviceName;
    private String path;
    private String method;
    private int statusCode;
    private int durationMs;
    private String traceId;
    private LocalDateTime createdAt;
    private String message;
    private boolean isDuplicate;
}
