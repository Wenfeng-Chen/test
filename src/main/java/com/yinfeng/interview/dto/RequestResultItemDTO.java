package com.yinfeng.interview.dto;

import lombok.Data;

@Data
public class RequestResultItemDTO {
    private String method;
    private String url;
    private Integer statusCode;
    private Long latencyMs;
    private Boolean success;
    private String errorMessage;
}
