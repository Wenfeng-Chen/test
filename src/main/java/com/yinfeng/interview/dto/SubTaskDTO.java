package com.yinfeng.interview.dto;

import lombok.Data;

import java.util.List;

@Data
public class SubTaskDTO {
    private Long taskId;
    private int concurrency;
    private int durationSeconds;
    private List<HttpRequestDefDTO> requests;
}
