package com.yinfeng.interview.service;

import com.yinfeng.interview.dto.HttpRequestDefDTO;
import com.yinfeng.interview.dto.LoadConfigDTO;
import com.yinfeng.interview.dto.MetricsSnapshotDTO;
import com.yinfeng.interview.entity.RequestResult;
import com.yinfeng.interview.enums.TaskStatus;
import com.yinfeng.interview.sse.MetricsSseBroadcaster;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@Profile("master")
@RequiredArgsConstructor
public class LoadTestExecutor {

    private final LoadDriver loadDriver;
    private final MetricsSseBroadcaster sseBroadcaster;

    public List<RequestResult> run(Long taskId, String workerId, LoadConfigDTO load,
                                   List<HttpRequestDefDTO> requests,
                                   ResultBatchHandler batchHandler) {
        List<RequestResult> allResults = loadDriver.run(taskId, workerId, load, requests,
                tick -> pushSnapshot(taskId, TaskStatus.RUNNING.name(), tick.results(), tick.windowRequestCount()));

        if (batchHandler != null && !allResults.isEmpty()) {
            batchHandler.flush(allResults);
        }
        return allResults;
    }

    private void pushSnapshot(Long taskId, String status, List<RequestResult> results, long windowQps) {
        MetricsSnapshotDTO snapshot = new MetricsSnapshotDTO();
        snapshot.setTaskId(taskId);
        snapshot.setStatus(status);
        snapshot.setTotalRequests(results.size());
        snapshot.setCurrentQps(windowQps);
        snapshot.setTimestamp(System.currentTimeMillis());

        if (!results.isEmpty()) {
            List<Long> sorted = results.stream().map(RequestResult::getLatencyMs).sorted().toList();
            double avg = sorted.stream().mapToLong(Long::longValue).average().orElse(0);
            long failed = results.stream().filter(r -> !Boolean.TRUE.equals(r.getSuccess())).count();
            snapshot.setAvgLatencyMs(avg);
            snapshot.setMinLatencyMs(sorted.get(0).doubleValue());
            snapshot.setMaxLatencyMs(sorted.get(sorted.size() - 1).doubleValue());
            snapshot.setTp90(MetricsAggregator.percentile(sorted, 90));
            snapshot.setTp95(MetricsAggregator.percentile(sorted, 95));
            snapshot.setTp99(MetricsAggregator.percentile(sorted, 99));
            snapshot.setErrorRate((double) failed / results.size() * 100);
            snapshot.setLatencyBuckets(boxLong(MetricsAggregator.latencyHistogram(sorted)));
            snapshot.setBucketLabels(MetricsAggregator.histogramLabels());
        } else {
            snapshot.setAvgLatencyMs(0);
            snapshot.setMinLatencyMs(0);
            snapshot.setMaxLatencyMs(0);
            snapshot.setTp90(0);
            snapshot.setTp95(0);
            snapshot.setTp99(0);
            snapshot.setErrorRate(0);
            snapshot.setLatencyBuckets(boxLong(new long[MetricsAggregator.histogramBucketCount()]));
            snapshot.setBucketLabels(MetricsAggregator.histogramLabels());
        }
        sseBroadcaster.broadcast(snapshot);
    }

    private List<Long> boxLong(long[] arr) {
        List<Long> list = new ArrayList<>();
        for (long v : arr) {
            list.add(v);
        }
        return list;
    }

    @FunctionalInterface
    public interface ResultBatchHandler {
        void flush(List<RequestResult> batch);
    }
}
