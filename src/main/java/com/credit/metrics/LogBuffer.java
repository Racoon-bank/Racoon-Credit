package com.credit.metrics;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

@Component
public class LogBuffer {

    private final ConcurrentLinkedQueue<LogDto> queue = new ConcurrentLinkedQueue<>();

    public void add(LogDto log) {
        // 4.
        queue.add(log);
    }

    public List<LogDto> flush(int maxBatchSize) {
        // 4.
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
