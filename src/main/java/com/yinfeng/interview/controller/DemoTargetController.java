package com.yinfeng.interview.controller;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@Profile("master")
public class DemoTargetController {

    @GetMapping("/api/demo/ping")
    public Map<String, String> ping() {
        return Map.of("status", "ok", "message", "pong");
    }
}
