package com.yinfeng.interview.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "loadtest")
public class LoadTestProperties {

    private String masterUrl = "http://localhost:8080";
    private long workerHeartbeatIntervalMs = 10000;
    private long workerPollIntervalMs = 2000;
    private int workerTimeoutSeconds = 30;
}
