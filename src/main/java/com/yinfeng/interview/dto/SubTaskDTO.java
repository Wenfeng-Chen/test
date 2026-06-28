package com.yinfeng.interview.dto;

import lombok.Data;

import java.util.List;

@Data
public class SubTaskDTO {
    private Long taskId;
    private LoadConfigDTO load;
    private List<HttpRequestDefDTO> requests;
}
