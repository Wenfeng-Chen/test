package com.yinfeng.interview.sse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yinfeng.interview.dto.MetricsSnapshotDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Component
@Profile("master")
@RequiredArgsConstructor
public class MetricsSseBroadcaster {

    private final ObjectMapper objectMapper;
    private final Map<Long, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long taskId) {
        SseEmitter emitter = new SseEmitter(0L);
        emitters.computeIfAbsent(taskId, k -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> remove(taskId, emitter));
        emitter.onTimeout(() -> remove(taskId, emitter));
        emitter.onError(e -> remove(taskId, emitter));
        return emitter;
    }

    public void broadcast(MetricsSnapshotDTO snapshot) {
        List<SseEmitter> list = emitters.get(snapshot.getTaskId());
        if (list == null || list.isEmpty()) {
            return;
        }
        for (SseEmitter emitter : list) {
            try {
                emitter.send(SseEmitter.event()
                        .name("metrics")
                        .data(objectMapper.writeValueAsString(snapshot)));
            } catch (IOException e) {
                remove(snapshot.getTaskId(), emitter);
            }
        }
    }

    private void remove(Long taskId, SseEmitter emitter) {
        List<SseEmitter> list = emitters.get(taskId);
        if (list != null) {
            list.remove(emitter);
        }
    }
}
