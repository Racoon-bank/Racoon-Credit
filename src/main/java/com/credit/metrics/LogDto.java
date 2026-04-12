package com.credit.metrics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private String createdAt;
    private String message;
    private boolean isDuplicate;
}
