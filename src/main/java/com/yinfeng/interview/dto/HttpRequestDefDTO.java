package com.yinfeng.interview.dto;

import lombok.Data;

import java.util.Map;

@Data
public class HttpRequestDefDTO {
    private String method = "GET";
    private String url;
    private Map<String, String> headers;
    private String body;
}
