package com.yinfeng.interview.service;

import com.yinfeng.interview.dto.HttpRequestDefDTO;
import com.yinfeng.interview.entity.AggregatedMetrics;
import com.yinfeng.interview.entity.RequestResult;

import java.util.List;

public final class MetricsAggregator {

    private MetricsAggregator() {
    }

    public static AggregatedMetrics aggregate(Long taskId, List<RequestResult> results, int durationSeconds) {
        AggregatedMetrics metrics = new AggregatedMetrics();
        metrics.setTaskId(taskId);

        if (results.isEmpty()) {
            metrics.setTotalRequests(0L);
            metrics.setQps(0.0);
            metrics.setAvgLatencyMs(0.0);
            metrics.setMinLatencyMs(0.0);
            metrics.setMaxLatencyMs(0.0);
            metrics.setTp90(0.0);
            metrics.setTp95(0.0);
            metrics.setTp99(0.0);
            metrics.setErrorRate(0.0);
            return metrics;
        }

        long total = results.size();
        long failed = results.stream().filter(r -> !Boolean.TRUE.equals(r.getSuccess())).count();

        List<Long> latencies = results.stream()
                .map(RequestResult::getLatencyMs)
                .sorted()
                .toList();

        double sum = latencies.stream().mapToLong(Long::longValue).sum();

        metrics.setTotalRequests(total);
        metrics.setQps(durationSeconds > 0 ? (double) total / durationSeconds : total);
        metrics.setAvgLatencyMs(sum / total);
        metrics.setMinLatencyMs(latencies.get(0).doubleValue());
        metrics.setMaxLatencyMs(latencies.get(latencies.size() - 1).doubleValue());
        metrics.setTp90(percentile(latencies, 90));
        metrics.setTp95(percentile(latencies, 95));
        metrics.setTp99(percentile(latencies, 99));
        metrics.setErrorRate(total > 0 ? (double) failed / total * 100 : 0.0);
        return metrics;
    }

    public static double percentile(List<Long> sorted, int p) {
        if (sorted.isEmpty()) {
            return 0.0;
        }
        int index = (int) Math.ceil(p / 100.0 * sorted.size()) - 1;
        index = Math.max(0, Math.min(index, sorted.size() - 1));
        return sorted.get(index).doubleValue();
    }

    public static long[] latencyHistogram(List<Long> latencies) {
        long[] buckets = new long[6];
        String[] bounds = {"50", "100", "200", "500", "1000"};
        for (Long latency : latencies) {
            if (latency < 50) {
                buckets[0]++;
            } else if (latency < 100) {
                buckets[1]++;
            } else if (latency < 200) {
                buckets[2]++;
            } else if (latency < 500) {
                buckets[3]++;
            } else if (latency < 1000) {
                buckets[4]++;
            } else {
                buckets[5]++;
            }
        }
        return buckets;
    }

    public static List<String> histogramLabels() {
        return List.of("<50ms", "50-100ms", "100-200ms", "200-500ms", "500-1000ms", ">1000ms");
    }
}
