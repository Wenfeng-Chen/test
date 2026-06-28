package com.yinfeng.interview.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("aggregated_metrics")
public class AggregatedMetrics {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long taskId;

    private Long totalRequests;

    private Double qps;

    private Double avgLatencyMs;

    private Double minLatencyMs;

    private Double maxLatencyMs;

    private Double tp90;

    private Double tp95;

    private Double tp99;

    private Double errorRate;

    private LocalDateTime calculatedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
