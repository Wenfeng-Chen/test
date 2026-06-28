package com.yinfeng.interview.service;

import com.yinfeng.interview.entity.RequestResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.LongStream;

import static org.junit.jupiter.api.Assertions.*;

class MetricsAggregatorTest {

    @Test
    void aggregateComputesTpPercentilesAndErrorRate() {
        List<RequestResult> results = List.of(
                result(100L, true),
                result(200L, true),
                result(300L, true),
                result(400L, false)
        );

        var metrics = MetricsAggregator.aggregate(1L, results, 10);
        assertEquals(4, metrics.getTotalRequests());
        assertEquals(0.4, metrics.getQps(), 0.01);
        assertEquals(250.0, metrics.getAvgLatencyMs(), 0.01);
        assertEquals(100.0, metrics.getMinLatencyMs(), 0.01);
        assertEquals(400.0, metrics.getMaxLatencyMs(), 0.01);
        assertEquals(400.0, metrics.getTp90(), 0.01);
        assertEquals(400.0, metrics.getTp95(), 0.01);
        assertEquals(400.0, metrics.getTp99(), 0.01);
        assertEquals(25.0, metrics.getErrorRate(), 0.01);
    }

    @Test
    void aggregateEmptyResultsReturnsZeros() {
        var metrics = MetricsAggregator.aggregate(1L, List.of(), 10);
        assertEquals(0L, metrics.getTotalRequests());
        assertEquals(0.0, metrics.getQps(), 0.01);
        assertEquals(0.0, metrics.getAvgLatencyMs(), 0.01);
        assertEquals(0.0, metrics.getErrorRate(), 0.01);
    }

    @Test
    void percentileOnSortedList() {
        List<Long> sorted = List.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L);
        assertEquals(9.0, MetricsAggregator.percentile(sorted, 90));
        assertEquals(10.0, MetricsAggregator.percentile(sorted, 95));
        assertEquals(10.0, MetricsAggregator.percentile(sorted, 99));
    }

    @Test
    void latencyHistogramBuckets() {
        List<Long> latencies = List.of(1L, 4L, 8L, 15L, 40L, 80L, 150L, 250L);
        long[] buckets = MetricsAggregator.latencyHistogram(latencies);
        assertArrayEquals(new long[]{1, 1, 1, 1, 1, 1, 1, 1}, buckets);
    }

    @Test
    void latencyHistogramGroupsLowLatency() {
        List<Long> latencies = List.of(0L, 1L, 3L, 6L, 9L, 12L);
        long[] buckets = MetricsAggregator.latencyHistogram(latencies);
        assertEquals(2, buckets[0]); // <2ms: 0,1
        assertEquals(1, buckets[1]); // 2-5ms: 3
        assertEquals(2, buckets[2]); // 5-10ms: 6,9
        assertEquals(1, buckets[3]); // 10-20ms: 12
    }

    @Test
    void qpsUsesDurationSeconds() {
        List<RequestResult> results = LongStream.rangeClosed(1, 100)
                .mapToObj(i -> result(5L, true))
                .toList();
        var metrics = MetricsAggregator.aggregate(1L, results, 5);
        assertEquals(20.0, metrics.getQps(), 0.01);
    }

    private RequestResult result(long latency, boolean success) {
        RequestResult r = new RequestResult();
        r.setLatencyMs(latency);
        r.setSuccess(success);
        return r;
    }
}
