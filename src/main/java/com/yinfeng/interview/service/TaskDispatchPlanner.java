package com.yinfeng.interview.service;

import java.util.ArrayList;
import java.util.List;

/**
 * 将总并发数均分给多个 Worker，余数分配给前几个 Worker。
 */
public final class TaskDispatchPlanner {

    private TaskDispatchPlanner() {
    }

    public static List<Integer> splitConcurrency(int totalConcurrency, int workerCount) {
        if (workerCount <= 0) {
            throw new IllegalArgumentException("workerCount must be positive");
        }
        if (totalConcurrency < 0) {
            throw new IllegalArgumentException("totalConcurrency must be non-negative");
        }

        int base = totalConcurrency / workerCount;
        int remainder = totalConcurrency % workerCount;
        List<Integer> quotas = new ArrayList<>(workerCount);
        for (int i = 0; i < workerCount; i++) {
            quotas.add(base + (i < remainder ? 1 : 0));
        }
        return quotas;
    }
}
