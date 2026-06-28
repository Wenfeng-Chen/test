package com.yinfeng.interview.dto;

import lombok.Data;

@Data
public class LoadConfigDTO {
    private int concurrency = 10;
    private int durationSeconds = 30;
}
