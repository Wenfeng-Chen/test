package com.yinfeng.interview.service;

import com.yinfeng.interview.dto.HttpRequestDefDTO;
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
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
@Profile("master")
@RequiredArgsConstructor
public class LoadTestExecutor {

    private final HttpRequestExecutor httpRequestExecutor;
    private final MetricsSseBroadcaster sseBroadcaster;

    public List<RequestResult> run(Long taskId, String workerId, int concurrency,
                                   int durationSeconds, List<HttpRequestDefDTO> requests,
                                   ResultBatchHandler batchHandler) {
        if (requests == null || requests.isEmpty()) {
            return List.of();
        }

        AtomicBoolean running = new AtomicBoolean(true);
        List<RequestResult> allResults = new CopyOnWriteArrayList<>();
        AtomicLong lastSecondCount = new AtomicLong(0);
        AtomicLong windowCount = new AtomicLong(0);

        ExecutorService pool = Executors.newFixedThreadPool(concurrency);
        long endTime = System.currentTimeMillis() + durationSeconds * 1000L;

        Thread reporter = new Thread(() -> {
            while (running.get()) {
                try {
                    Thread.sleep(1000);
                    long count = windowCount.getAndSet(0);
                    pushSnapshot(taskId, TaskStatus.RUNNING.name(), allResults, count);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        reporter.setDaemon(true);
        reporter.start();

        for (int i = 0; i < concurrency; i++) {
            pool.submit(() -> {
                while (System.currentTimeMillis() < endTime) {
                    HttpRequestDefDTO def = requests.get((int) (lastSecondCount.incrementAndGet() % requests.size()));
                    RequestResult result = httpRequestExecutor.execute(taskId, workerId, def);
                    allResults.add(result);
                    windowCount.incrementAndGet();
                }
            });
        }

        pool.shutdown();
        try {
            pool.awaitTermination(durationSeconds + 30L, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        running.set(false);

        if (batchHandler != null && !allResults.isEmpty()) {
            batchHandler.flush(allResults);
        }
        return new ArrayList<>(allResults);
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
            snapshot.setLatencyBuckets(List.of(0L, 0L, 0L, 0L, 0L, 0L));
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
