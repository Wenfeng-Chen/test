package com.yinfeng.interview.dto;

import lombok.Data;

import java.util.List;

@Data
public class MetricsSnapshotDTO {
    private Long taskId;
    private String status;
    private long totalRequests;
    private double currentQps;
    private double avgLatencyMs;
    private double minLatencyMs;
    private double maxLatencyMs;
    private double tp90;
    private double tp95;
    private double tp99;
    private double errorRate;
    private List<Long> latencyBuckets;
    private List<String> bucketLabels;
    private long timestamp;
}
