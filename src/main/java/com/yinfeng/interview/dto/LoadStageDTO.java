package com.yinfeng.interview.dto;

import lombok.Data;

@Data
public class LoadStageDTO {
    private int concurrency;
    private int durationSeconds;
}
