package com.yinfeng.interview.service;

import com.yinfeng.interview.dto.SubTaskDTO;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Profile("master")
public class SubTaskStore {

    private final Queue<SubTaskDTO> pending = new ConcurrentLinkedQueue<>();
    private final Map<Long, AtomicInteger> completionCount = new ConcurrentHashMap<>();
    private final Map<Long, Integer> expectedWorkers = new ConcurrentHashMap<>();

    public void enqueueAll(Long taskId, List<SubTaskDTO> subTasks) {
        pending.addAll(subTasks);
        expectedWorkers.put(taskId, subTasks.size());
        completionCount.put(taskId, new AtomicInteger(0));
    }

    public SubTaskDTO poll() {
        return pending.poll();
    }

    public boolean markComplete(Long taskId) {
        AtomicInteger counter = completionCount.get(taskId);
        if (counter == null) {
            return true;
        }
        int count = counter.incrementAndGet();
        return count >= expectedWorkers.getOrDefault(taskId, 1);
    }

    public void clear(Long taskId) {
        completionCount.remove(taskId);
        expectedWorkers.remove(taskId);
    }
}
