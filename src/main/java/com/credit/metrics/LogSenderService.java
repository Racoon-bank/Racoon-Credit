package com.credit.metrics;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LogSenderService {

    private final LogBuffer logBuffer;
    private final RestClient monitoringRestClient;

    @Scheduled(fixedDelay = 2000)
    public void sendLogs() {
        // 4. Периодически отправляем накопленные логи в monitoring-service отдельным батчем.
        List<LogDto> logs = logBuffer.flush(50);
        if (logs.isEmpty()) {
            return;
        }

        try {
            monitoringRestClient.post()
                    .uri("/api/logs/batch")
                    .body(logs)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception ignored) {
        }
    }
}
