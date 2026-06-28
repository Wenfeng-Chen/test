package com.yinfeng.interview.worker;

import com.yinfeng.interview.common.response.ApiResponse;
import com.yinfeng.interview.config.LoadTestProperties;
import com.yinfeng.interview.dto.*;
import com.yinfeng.interview.entity.RequestResult;
import com.yinfeng.interview.service.HttpRequestExecutor;
import com.yinfeng.interview.service.LoadDriver;
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
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
@Profile("worker")
@RequiredArgsConstructor
public class WorkerRunner {

    private final LoadTestProperties properties;
    private final LoadDriver loadDriver;
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
        log.info("Executing subtask for task {}, mode={}", subTask.getTaskId(),
                subTask.getLoad() != null ? subTask.getLoad().getMode() : "FIXED_CONCURRENCY");
        AtomicInteger reportedSize = new AtomicInteger(0);

        loadDriver.run(subTask.getTaskId(), workerId, subTask.getLoad(), subTask.getRequests(), tick -> {
            List<RequestResult> all = tick.results();
            int prev = reportedSize.get();
            if (all.size() > prev) {
                reportBatch(subTask.getTaskId(), new ArrayList<>(all.subList(prev, all.size())));
                reportedSize.set(all.size());
            }
        });

        TaskCompleteDTO complete = new TaskCompleteDTO();
        complete.setTaskId(subTask.getTaskId());
        complete.setWorkerId(workerId);
        masterClient.post()
                .uri("/api/workers/tasks/complete")
                .body(complete)
                .retrieve()
                .toBodilessEntity();
    }

    private void reportBatch(Long taskId, List<RequestResult> batch) {
        if (batch.isEmpty()) {
            return;
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
