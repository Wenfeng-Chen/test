package com.yinfeng.interview.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yinfeng.interview.dto.*;
import com.yinfeng.interview.entity.AggregatedMetrics;
import com.yinfeng.interview.entity.RequestResult;
import com.yinfeng.interview.entity.TestTask;
import com.yinfeng.interview.entity.WorkerNode;
import com.yinfeng.interview.enums.RunMode;
import com.yinfeng.interview.enums.TaskStatus;
import com.yinfeng.interview.mapper.AggregatedMetricsMapper;
import com.yinfeng.interview.mapper.TestTaskMapper;
import com.yinfeng.interview.sse.MetricsSseBroadcaster;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Slf4j
@Service
@Profile("master")
public class TaskService {

    private final TestTaskMapper testTaskMapper;
    private final AggregatedMetricsMapper aggregatedMetricsMapper;
    private final RequestResultService requestResultService;
    private final LoadTestExecutor loadTestExecutor;
    private final WorkerService workerService;
    private final SubTaskStore subTaskStore;
    private final MetricsSseBroadcaster sseBroadcaster;
    private final ObjectMapper objectMapper;
    private final Executor loadTestExecutorPool;

    public TaskService(TestTaskMapper testTaskMapper,
                       AggregatedMetricsMapper aggregatedMetricsMapper,
                       RequestResultService requestResultService,
                       LoadTestExecutor loadTestExecutor,
                       WorkerService workerService,
                       SubTaskStore subTaskStore,
                       MetricsSseBroadcaster sseBroadcaster,
                       ObjectMapper objectMapper,
                       @org.springframework.beans.factory.annotation.Qualifier("loadTestExecutorPool") Executor loadTestExecutorPool) {
        this.testTaskMapper = testTaskMapper;
        this.aggregatedMetricsMapper = aggregatedMetricsMapper;
        this.requestResultService = requestResultService;
        this.loadTestExecutor = loadTestExecutor;
        this.workerService = workerService;
        this.subTaskStore = subTaskStore;
        this.sseBroadcaster = sseBroadcaster;
        this.objectMapper = objectMapper;
        this.loadTestExecutorPool = loadTestExecutorPool;
    }

    public Long submitTask(TestPlanDTO plan) {
        validate(plan);

        TestTask task = new TestTask();
        task.setName(plan.getName());
        task.setMode(plan.getMode().name());
        task.setConcurrency(plan.getLoad().getConcurrency());
        task.setDurationSeconds(plan.getLoad().getDurationSeconds());
        task.setStatus(TaskStatus.PENDING.name());
        try {
            task.setRequestConfig(objectMapper.writeValueAsString(plan.getRequests()));
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid request config");
        }
        testTaskMapper.insert(task);

        CompletableFuture.runAsync(() -> executeTask(task.getId()), loadTestExecutorPool);
        return task.getId();
    }

    public void executeTask(Long taskId) {
        TestTask task = testTaskMapper.selectById(taskId);
        if (task == null) {
            return;
        }

        task.setStatus(TaskStatus.RUNNING.name());
        task.setStartedAt(LocalDateTime.now());
        testTaskMapper.updateById(task);

        List<HttpRequestDefDTO> requests = parseRequests(task.getRequestConfig());
        RunMode mode = RunMode.valueOf(task.getMode());

        if (mode == RunMode.DISTRIBUTED) {
            List<WorkerNode> workers = workerService.listActiveWorkers();
            if (!workers.isEmpty()) {
                dispatchToWorkers(task, requests, workers);
                return;
            }
        }

        runStandalone(task, requests);
    }

    private void dispatchToWorkers(TestTask task, List<HttpRequestDefDTO> requests, List<WorkerNode> workers) {
        int total = task.getConcurrency();
        int n = workers.size();
        int base = total / n;
        int remainder = total % n;

        List<SubTaskDTO> subTasks = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            SubTaskDTO sub = new SubTaskDTO();
            sub.setTaskId(task.getId());
            sub.setConcurrency(base + (i < remainder ? 1 : 0));
            sub.setDurationSeconds(task.getDurationSeconds());
            sub.setRequests(requests);
            subTasks.add(sub);
        }
        subTaskStore.enqueueAll(task.getId(), subTasks);
    }

    private void runStandalone(TestTask task, List<HttpRequestDefDTO> requests) {
        try {
            List<RequestResult> allResults = loadTestExecutor.run(
                    task.getId(),
                    "master",
                    task.getConcurrency(),
                    task.getDurationSeconds(),
                    requests,
                    batch -> {
                    }
            );
            if (!allResults.isEmpty()) {
                requestResultService.saveBatch(allResults, 500);
            }
            finalizeTask(task.getId());
        } catch (Exception e) {
            log.error("Task {} failed", task.getId(), e);
            failTask(task.getId());
        }
    }

    public void reportMetrics(MetricsReportDTO report) {
        if (report.getResults() == null || report.getResults().isEmpty()) {
            return;
        }
        List<RequestResult> entities = report.getResults().stream().map(item -> {
            RequestResult r = new RequestResult();
            r.setTaskId(report.getTaskId());
            r.setWorkerId(report.getWorkerId());
            r.setMethod(item.getMethod());
            r.setUrl(item.getUrl());
            r.setStatusCode(item.getStatusCode());
            r.setLatencyMs(item.getLatencyMs());
            r.setSuccess(item.getSuccess());
            r.setErrorMessage(item.getErrorMessage());
            return r;
        }).toList();
        requestResultService.saveBatch(entities, 500);
    }

    public void completeWorkerTask(TaskCompleteDTO dto) {
        boolean allDone = subTaskStore.markComplete(dto.getTaskId());
        if (allDone) {
            finalizeTask(dto.getTaskId());
            subTaskStore.clear(dto.getTaskId());
        }
    }

    public void finalizeTask(Long taskId) {
        TestTask task = testTaskMapper.selectById(taskId);
        if (task == null) {
            return;
        }

        List<RequestResult> results = requestResultService.list(
                new LambdaQueryWrapper<RequestResult>().eq(RequestResult::getTaskId, taskId));

        AggregatedMetrics metrics = MetricsAggregator.aggregate(taskId, results, task.getDurationSeconds());
        metrics.setCalculatedAt(LocalDateTime.now());
        aggregatedMetricsMapper.insert(metrics);

        task.setStatus(TaskStatus.COMPLETED.name());
        task.setFinishedAt(LocalDateTime.now());
        testTaskMapper.updateById(task);

        MetricsSnapshotDTO snapshot = new MetricsSnapshotDTO();
        snapshot.setTaskId(taskId);
        snapshot.setStatus(TaskStatus.COMPLETED.name());
        snapshot.setTotalRequests(metrics.getTotalRequests());
        snapshot.setCurrentQps(metrics.getQps());
        snapshot.setAvgLatencyMs(metrics.getAvgLatencyMs());
        snapshot.setMinLatencyMs(metrics.getMinLatencyMs());
        snapshot.setMaxLatencyMs(metrics.getMaxLatencyMs());
        snapshot.setTp90(metrics.getTp90());
        snapshot.setTp95(metrics.getTp95());
        snapshot.setTp99(metrics.getTp99());
        snapshot.setErrorRate(metrics.getErrorRate());
        List<Long> latencies = results.stream().map(RequestResult::getLatencyMs).toList();
        long[] hist = MetricsAggregator.latencyHistogram(latencies);
        snapshot.setLatencyBuckets(java.util.Arrays.stream(hist).boxed().toList());
        snapshot.setBucketLabels(MetricsAggregator.histogramLabels());
        snapshot.setTimestamp(System.currentTimeMillis());
        sseBroadcaster.broadcast(snapshot);
    }

    private void failTask(Long taskId) {
        TestTask task = testTaskMapper.selectById(taskId);
        if (task != null) {
            task.setStatus(TaskStatus.FAILED.name());
            task.setFinishedAt(LocalDateTime.now());
            testTaskMapper.updateById(task);
        }
    }

    public TaskDetailVO getTaskDetail(Long id) {
        TestTask task = testTaskMapper.selectById(id);
        if (task == null) {
            return null;
        }
        TaskDetailVO vo = new TaskDetailVO();
        vo.setId(task.getId());
        vo.setName(task.getName());
        vo.setMode(task.getMode());
        vo.setConcurrency(task.getConcurrency());
        vo.setDurationSeconds(task.getDurationSeconds());
        vo.setStatus(TaskStatus.valueOf(task.getStatus()));
        AggregatedMetrics metrics = aggregatedMetricsMapper.selectOne(
                new LambdaQueryWrapper<AggregatedMetrics>().eq(AggregatedMetrics::getTaskId, id));
        vo.setMetrics(metrics);
        return vo;
    }

    public Page<RequestResult> pageResults(Long taskId, int page, int size) {
        return requestResultService.page(new Page<>(page, size),
                new LambdaQueryWrapper<RequestResult>()
                        .eq(RequestResult::getTaskId, taskId)
                        .orderByDesc(RequestResult::getId));
    }

    private void validate(TestPlanDTO plan) {
        if (plan.getName() == null || plan.getName().isBlank()) {
            throw new IllegalArgumentException("Task name is required");
        }
        if (plan.getLoad() == null) {
            throw new IllegalArgumentException("Load config is required");
        }
        if (plan.getRequests() == null || plan.getRequests().isEmpty()) {
            throw new IllegalArgumentException("At least one request is required");
        }
        for (HttpRequestDefDTO req : plan.getRequests()) {
            if (req.getUrl() == null || req.getUrl().isBlank()) {
                throw new IllegalArgumentException("Request URL is required");
            }
        }
    }

    private List<HttpRequestDefDTO> parseRequests(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse request config");
        }
    }
}
