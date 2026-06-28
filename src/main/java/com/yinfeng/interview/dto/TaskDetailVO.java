package com.yinfeng.interview.dto;

import com.yinfeng.interview.entity.AggregatedMetrics;
import com.yinfeng.interview.enums.TaskStatus;
import lombok.Data;

@Data
public class TaskDetailVO {
    private Long id;
    private String name;
    private String mode;
    private Integer concurrency;
    private Integer durationSeconds;
    private TaskStatus status;
    private AggregatedMetrics metrics;
}
