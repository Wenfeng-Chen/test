package com.yinfeng.interview.service;

import com.yinfeng.interview.entity.RequestResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MetricsAggregatorTest {

    @Test
    void aggregateComputesTpPercentiles() {
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
        assertEquals(25.0, metrics.getErrorRate(), 0.01);
        assertEquals(400.0, metrics.getTp99(), 0.01);
    }

    private RequestResult result(long latency, boolean success) {
        RequestResult r = new RequestResult();
        r.setLatencyMs(latency);
        r.setSuccess(success);
        return r;
    }
}
