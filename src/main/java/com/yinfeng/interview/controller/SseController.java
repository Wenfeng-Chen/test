package com.yinfeng.interview.controller;

import com.yinfeng.interview.sse.MetricsSseBroadcaster;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/sse")
@Profile("master")
@RequiredArgsConstructor
public class SseController {

    private final MetricsSseBroadcaster broadcaster;

    @GetMapping(value = "/tasks/{id}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@PathVariable Long id) {
        return broadcaster.subscribe(id);
    }
}
