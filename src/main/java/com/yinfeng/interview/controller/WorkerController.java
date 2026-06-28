package com.yinfeng.interview.controller;

import com.yinfeng.interview.common.response.ApiResponse;
import com.yinfeng.interview.dto.*;
import com.yinfeng.interview.service.SubTaskStore;
import com.yinfeng.interview.service.TaskService;
import com.yinfeng.interview.service.WorkerService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/workers")
@Profile("master")
@RequiredArgsConstructor
public class WorkerController {

    private final WorkerService workerService;
    private final SubTaskStore subTaskStore;
    private final TaskService taskService;

    @PostMapping("/register")
    public ApiResponse<Map<String, String>> register(@RequestBody WorkerRegisterDTO dto) {
        String workerId = workerService.register(dto);
        return ApiResponse.ok(Map.of("workerId", workerId));
    }

    @PostMapping("/heartbeat")
    public ApiResponse<Void> heartbeat(@RequestBody WorkerHeartbeatDTO dto) {
        workerService.heartbeat(dto.getWorkerId());
        return ApiResponse.ok(null);
    }

    @GetMapping("/tasks/poll")
    public ApiResponse<SubTaskDTO> poll(@RequestParam String workerId) {
        SubTaskDTO subTask = subTaskStore.poll();
        return ApiResponse.ok(subTask);
    }

    @PostMapping("/metrics/report")
    public ApiResponse<Void> reportMetrics(@RequestBody MetricsReportDTO report) {
        taskService.reportMetrics(report);
        return ApiResponse.ok(null);
    }

    @PostMapping("/tasks/complete")
    public ApiResponse<Void> complete(@RequestBody TaskCompleteDTO dto) {
        taskService.completeWorkerTask(dto);
        return ApiResponse.ok(null);
    }
}
