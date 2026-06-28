package com.yinfeng.interview.dto;

import lombok.Data;

@Data
public class WorkerRegisterDTO {
    private String workerId;
    private String host;
    private int port;
}
