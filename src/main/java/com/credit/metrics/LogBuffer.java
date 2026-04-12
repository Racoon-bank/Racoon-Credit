package com.credit.metrics;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

@Component
public class LogBuffer {

    private final ConcurrentLinkedQueue<LogDto> queue = new ConcurrentLinkedQueue<>();

    public void add(LogDto log) {
        // 4. Временно складываем логи в память, чтобы потом отправлять их в monitoring-service пачкой.
        queue.add(log);
    }

    public List<LogDto> flush(int maxBatchSize) {
        // 4. Забираем ограниченную пачку логов из буфера для фоновой отправки.
        List<LogDto> result = new ArrayList<>();
        while (result.size() < maxBatchSize) {
            LogDto log = queue.poll();
            if (log == null) {
                break;
            }
            result.add(log);
        }
        return result;
    }
}
