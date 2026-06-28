package com.yinfeng.interview.service;

import com.yinfeng.interview.dto.HttpRequestDefDTO;
import com.yinfeng.interview.dto.LoadConfigDTO;
import com.yinfeng.interview.dto.LoadStageDTO;
import com.yinfeng.interview.entity.RequestResult;
import com.yinfeng.interview.enums.LoadMode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

@Component
@RequiredArgsConstructor
public class LoadDriver {

    private final HttpRequestExecutor httpRequestExecutor;

    public List<RequestResult> run(Long taskId, String workerId, LoadConfigDTO load,
                                   List<HttpRequestDefDTO> requests,
                                   Consumer<TickSnapshot> onTick) {
        if (requests == null || requests.isEmpty()) {
            return List.of();
        }
        LoadMode mode = LoadConfigResolver.mode(load);
        return switch (mode) {
            case FIXED_CONCURRENCY -> runFixedConcurrency(taskId, workerId, load.getConcurrency(),
                    load.getDurationSeconds(), requests, onTick);
            case FIXED_RPS -> runFixedRps(taskId, workerId, load.getTargetRps(),
                    load.getDurationSeconds(), requests, onTick);
            case STEP_RAMP -> runStepRamp(taskId, workerId, load.getStages(), requests, onTick);
        };
    }

    private List<RequestResult> runStepRamp(Long taskId, String workerId, List<LoadStageDTO> stages,
                                            List<HttpRequestDefDTO> requests,
                                            Consumer<TickSnapshot> onTick) {
        List<RequestResult> allResults = new ArrayList<>();
        for (LoadStageDTO stage : stages) {
            allResults.addAll(runFixedConcurrency(taskId, workerId, stage.getConcurrency(),
                    stage.getDurationSeconds(), requests, onTick));
        }
        return allResults;
    }

    private List<RequestResult> runFixedConcurrency(Long taskId, String workerId, int concurrency,
                                                  int durationSeconds, List<HttpRequestDefDTO> requests,
                                                  Consumer<TickSnapshot> onTick) {
        AtomicBoolean running = new AtomicBoolean(true);
        List<RequestResult> allResults = new CopyOnWriteArrayList<>();
        AtomicLong requestIndex = new AtomicLong(0);
        AtomicLong windowCount = new AtomicLong(0);

        Thread reporter = startReporter(running, allResults, windowCount, onTick);
        ExecutorService pool = Executors.newFixedThreadPool(Math.max(1, concurrency));
        long endTime = System.currentTimeMillis() + durationSeconds * 1000L;

        for (int i = 0; i < concurrency; i++) {
            pool.submit(() -> {
                while (System.currentTimeMillis() < endTime) {
                    HttpRequestDefDTO def = requests.get(
                            (int) (requestIndex.getAndIncrement() % requests.size()));
                    RequestResult result = httpRequestExecutor.execute(taskId, workerId, def);
                    allResults.add(result);
                    windowCount.incrementAndGet();
                }
            });
        }

        shutdownPool(pool, durationSeconds);
        running.set(false);
        joinReporter(reporter);
        return new ArrayList<>(allResults);
    }

    private List<RequestResult> runFixedRps(Long taskId, String workerId, int targetRps,
                                            int durationSeconds, List<HttpRequestDefDTO> requests,
                                            Consumer<TickSnapshot> onTick) {
        AtomicBoolean running = new AtomicBoolean(true);
        List<RequestResult> allResults = new CopyOnWriteArrayList<>();
        AtomicLong requestIndex = new AtomicLong(0);
        AtomicLong windowCount = new AtomicLong(0);

        Thread reporter = startReporter(running, allResults, windowCount, onTick);
        int poolSize = Math.min(Math.max(targetRps, 1), 256);
        ExecutorService pool = Executors.newFixedThreadPool(poolSize);
        SimpleRateLimiter limiter = new SimpleRateLimiter(targetRps);
        long endTime = System.currentTimeMillis() + durationSeconds * 1000L;

        Thread dispatcher = new Thread(() -> {
            while (System.currentTimeMillis() < endTime && running.get()) {
                limiter.acquire();
                pool.submit(() -> {
                    HttpRequestDefDTO def = requests.get(
                            (int) (requestIndex.getAndIncrement() % requests.size()));
                    RequestResult result = httpRequestExecutor.execute(taskId, workerId, def);
                    allResults.add(result);
                    windowCount.incrementAndGet();
                });
            }
        });
        dispatcher.start();

        try {
            dispatcher.join(durationSeconds * 1000L + 5000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        running.set(false);
        shutdownPool(pool, durationSeconds);
        joinReporter(reporter);
        return new ArrayList<>(allResults);
    }

    private Thread startReporter(AtomicBoolean running, List<RequestResult> allResults,
                                 AtomicLong windowCount, Consumer<TickSnapshot> onTick) {
        if (onTick == null) {
            return null;
        }
        Thread reporter = new Thread(() -> {
            while (running.get()) {
                try {
                    Thread.sleep(1000);
                    long count = windowCount.getAndSet(0);
                    onTick.accept(new TickSnapshot(new ArrayList<>(allResults), count));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        reporter.setDaemon(true);
        reporter.start();
        return reporter;
    }

    private void joinReporter(Thread reporter) {
        if (reporter != null) {
            try {
                reporter.join(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void shutdownPool(ExecutorService pool, int durationSeconds) {
        pool.shutdown();
        try {
            pool.awaitTermination(durationSeconds + 30L, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public record TickSnapshot(List<RequestResult> results, long windowRequestCount) {
    }
}
