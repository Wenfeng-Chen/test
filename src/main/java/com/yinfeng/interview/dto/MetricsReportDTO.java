package com.yinfeng.interview.dto;

import lombok.Data;

import java.util.List;

@Data
public class MetricsReportDTO {
    private Long taskId;
    private String workerId;
    private List<RequestResultItemDTO> results;
}
