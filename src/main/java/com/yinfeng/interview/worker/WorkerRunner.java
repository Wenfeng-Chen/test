package com.yinfeng.interview.worker;

import com.yinfeng.interview.common.response.ApiResponse;
import com.yinfeng.interview.config.LoadTestProperties;
import com.yinfeng.interview.dto.*;
import com.yinfeng.interview.entity.RequestResult;
import com.yinfeng.interview.service.HttpRequestExecutor;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
@Profile("worker")
@RequiredArgsConstructor
public class WorkerRunner {

    private final LoadTestProperties properties;
    private final HttpRequestExecutor httpRequestExecutor;
    private final RestClient.Builder restClientBuilder;

    @Value("${server.port}")
    private int port;

    private String workerId;
    private RestClient masterClient;

    @PostConstruct
    public void start() {
        masterClient = restClientBuilder.baseUrl(properties.getMasterUrl()).build();
        workerId = register();
        log.info("Worker registered: {}", workerId);

        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(
                this::heartbeat,
                properties.getWorkerHeartbeatIntervalMs(),
                properties.getWorkerHeartbeatIntervalMs(),
                TimeUnit.MILLISECONDS);

        Executors.newSingleThreadExecutor().submit(this::pollLoop);
    }

    private String register() {
        try {
            WorkerRegisterDTO dto = new WorkerRegisterDTO();
            dto.setWorkerId(UUID.randomUUID().toString());
            dto.setHost(InetAddress.getLocalHost().getHostAddress());
            dto.setPort(port);

            ApiResponse<Map<String, String>> resp = masterClient.post()
                    .uri("/api/workers/register")
                    .body(dto)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            if (resp != null && resp.getData() != null) {
                return resp.getData().get("workerId");
            }
        } catch (Exception e) {
            log.error("Register failed", e);
        }
        return UUID.randomUUID().toString();
    }

    private void heartbeat() {
        try {
            WorkerHeartbeatDTO dto = new WorkerHeartbeatDTO();
            dto.setWorkerId(workerId);
            masterClient.post()
                    .uri("/api/workers/heartbeat")
                    .body(dto)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.warn("Heartbeat failed: {}", e.getMessage());
        }
    }

    private void pollLoop() {
        while (true) {
            try {
                Thread.sleep(properties.getWorkerPollIntervalMs());
                SubTaskDTO subTask = pollTask();
                if (subTask != null) {
                    executeSubTask(subTask);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.warn("Poll loop error: {}", e.getMessage());
            }
        }
    }

    private SubTaskDTO pollTask() {
        ApiResponse<SubTaskDTO> resp = masterClient.get()
                .uri(uri -> uri.path("/api/workers/tasks/poll").queryParam("workerId", workerId).build())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
        return resp != null ? resp.getData() : null;
    }

    private void executeSubTask(SubTaskDTO subTask) {
        log.info("Executing subtask for task {}", subTask.getTaskId());
        AtomicBoolean running = new AtomicBoolean(true);
        List<RequestResult> buffer = new ArrayList<>();
        long endTime = System.currentTimeMillis() + subTask.getDurationSeconds() * 1000L;

        Thread reporter = new Thread(() -> {
            while (running.get()) {
                try {
                    Thread.sleep(500);
                    flushResults(subTask.getTaskId(), buffer);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        reporter.setDaemon(true);
        reporter.start();

        var pool = Executors.newFixedThreadPool(subTask.getConcurrency());
        for (int i = 0; i < subTask.getConcurrency(); i++) {
            pool.submit(() -> {
                int idx = 0;
                while (System.currentTimeMillis() < endTime) {
                    HttpRequestDefDTO def = subTask.getRequests().get(idx++ % subTask.getRequests().size());
                    RequestResult result = httpRequestExecutor.execute(subTask.getTaskId(), workerId, def);
                    synchronized (buffer) {
                        buffer.add(result);
                    }
                }
            });
        }

        pool.shutdown();
        try {
            pool.awaitTermination(subTask.getDurationSeconds() + 30L, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        running.set(false);
        flushResults(subTask.getTaskId(), buffer);

        TaskCompleteDTO complete = new TaskCompleteDTO();
        complete.setTaskId(subTask.getTaskId());
        complete.setWorkerId(workerId);
        masterClient.post()
                .uri("/api/workers/tasks/complete")
                .body(complete)
                .retrieve()
                .toBodilessEntity();
    }

    private void flushResults(Long taskId, List<RequestResult> buffer) {
        List<RequestResult> batch;
        synchronized (buffer) {
            if (buffer.isEmpty()) {
                return;
            }
            batch = new ArrayList<>(buffer);
            buffer.clear();
        }

        MetricsReportDTO report = new MetricsReportDTO();
        report.setTaskId(taskId);
        report.setWorkerId(workerId);
        report.setResults(batch.stream().map(r -> {
            RequestResultItemDTO item = new RequestResultItemDTO();
            item.setMethod(r.getMethod());
            item.setUrl(r.getUrl());
            item.setStatusCode(r.getStatusCode());
            item.setLatencyMs(r.getLatencyMs());
            item.setSuccess(r.getSuccess());
            item.setErrorMessage(r.getErrorMessage());
            return item;
        }).toList());

        masterClient.post()
                .uri("/api/workers/metrics/report")
                .body(report)
                .retrieve()
                .toBodilessEntity();
    }
}
