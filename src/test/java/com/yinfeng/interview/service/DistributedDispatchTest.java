package com.yinfeng.interview.service;

import com.yinfeng.interview.dto.HttpRequestDefDTO;
import com.yinfeng.interview.dto.SubTaskDTO;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DistributedDispatchTest {

    @Test
    void twoWorkersSplitFiftyConcurrency() {
        List<Integer> quotas = TaskDispatchPlanner.splitConcurrency(50, 2);
        assertEquals(List.of(25, 25), quotas);

        SubTaskStore store = new SubTaskStore();
        store.enqueueAll(1L, buildSubTasks(1L, quotas, sampleRequests()));

        SubTaskDTO w1 = store.poll();
        SubTaskDTO w2 = store.poll();
        assertNull(store.poll());

        assertEquals(25, w1.getConcurrency());
        assertEquals(25, w2.getConcurrency());
        assertEquals(1, w1.getRequests().size());
    }

    @Test
    void threeWorkersCompleteAfterAllReportDone() {
        List<Integer> quotas = TaskDispatchPlanner.splitConcurrency(100, 3);
        assertEquals(List.of(34, 33, 33), quotas);

        SubTaskStore store = new SubTaskStore();
        store.enqueueAll(10L, buildSubTasks(10L, quotas, sampleRequests()));

        assertEquals(3, quotas.size());
        assertNotNull(store.poll());
        assertNotNull(store.poll());
        assertNotNull(store.poll());

        assertFalse(store.markComplete(10L));
        assertFalse(store.markComplete(10L));
        assertTrue(store.markComplete(10L));
    }

    @Test
    void standaloneUsesSingleExecutorQuota() {
        List<Integer> quotas = TaskDispatchPlanner.splitConcurrency(20, 1);
        assertEquals(List.of(20), quotas);
    }

    @Test
    void distributedQuotaSumMatchesTotalConcurrency() {
        int total = 127;
        int workers = 8;
        List<Integer> quotas = TaskDispatchPlanner.splitConcurrency(total, workers);
        assertEquals(total, quotas.stream().mapToInt(Integer::intValue).sum());
        assertEquals(workers, quotas.size());
    }

    private List<HttpRequestDefDTO> sampleRequests() {
        HttpRequestDefDTO get = new HttpRequestDefDTO();
        get.setMethod("GET");
        get.setUrl("http://localhost:8080/api/demo/ping");
        return List.of(get);
    }

    private List<SubTaskDTO> buildSubTasks(Long taskId, List<Integer> quotas, List<HttpRequestDefDTO> requests) {
        List<SubTaskDTO> subTasks = new ArrayList<>();
        for (int quota : quotas) {
            SubTaskDTO sub = new SubTaskDTO();
            sub.setTaskId(taskId);
            sub.setConcurrency(quota);
            sub.setDurationSeconds(10);
            sub.setRequests(requests);
            subTasks.add(sub);
        }
        return subTasks;
    }
}
