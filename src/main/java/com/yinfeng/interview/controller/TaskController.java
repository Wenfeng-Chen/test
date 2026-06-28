package com.yinfeng.interview.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yinfeng.interview.common.response.ApiResponse;
import com.yinfeng.interview.dto.*;
import com.yinfeng.interview.entity.RequestResult;
import com.yinfeng.interview.service.SubTaskStore;
import com.yinfeng.interview.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/tasks")
@Profile("master")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @PostMapping
    public ApiResponse<Map<String, Long>> submit(@RequestBody TestPlanDTO plan) {
        Long taskId = taskService.submitTask(plan);
        return ApiResponse.ok(Map.of("taskId", taskId));
    }

    @GetMapping("/{id}")
    public ApiResponse<TaskDetailVO> detail(@PathVariable Long id) {
        TaskDetailVO vo = taskService.getTaskDetail(id);
        if (vo == null) {
            return ApiResponse.fail("Task not found");
        }
        return ApiResponse.ok(vo);
    }

    @GetMapping("/{id}/results")
    public ApiResponse<Page<RequestResult>> results(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(taskService.pageResults(id, page, size));
    }
}
