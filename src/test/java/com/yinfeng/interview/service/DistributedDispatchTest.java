package com.yinfeng.interview.service;

import com.yinfeng.interview.dto.HttpRequestDefDTO;
import com.yinfeng.interview.dto.LoadConfigDTO;
import com.yinfeng.interview.dto.SubTaskDTO;
import com.yinfeng.interview.enums.LoadMode;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DistributedDispatchTest {

    @Test
    void twoWorkersSplitFiftyConcurrency() {
        LoadConfigDTO load = fixedConcurrency(50, 10);
        List<SubTaskDTO> subTasks = buildSubTasks(1L, load, 2, sampleRequests());

        SubTaskStore store = new SubTaskStore();
        store.enqueueAll(1L, subTasks);

        SubTaskDTO w1 = store.poll();
        SubTaskDTO w2 = store.poll();
        assertNull(store.poll());

        assertEquals(25, w1.getLoad().getConcurrency());
        assertEquals(25, w2.getLoad().getConcurrency());
        assertEquals(1, w1.getRequests().size());
    }

    @Test
    void twoWorkersSplitRps() {
        LoadConfigDTO load = fixedRps(100, 10);
        List<SubTaskDTO> subTasks = buildSubTasks(1L, load, 2, sampleRequests());

        assertEquals(50, subTasks.get(0).getLoad().getTargetRps());
        assertEquals(50, subTasks.get(1).getLoad().getTargetRps());
    }

    @Test
    void threeWorkersCompleteAfterAllReportDone() {
        LoadConfigDTO load = fixedConcurrency(100, 10);
        List<SubTaskDTO> subTasks = buildSubTasks(10L, load, 3, sampleRequests());

        SubTaskStore store = new SubTaskStore();
        store.enqueueAll(10L, subTasks);

        assertNotNull(store.poll());
        assertNotNull(store.poll());
        assertNotNull(store.poll());

        assertFalse(store.markComplete(10L));
        assertFalse(store.markComplete(10L));
        assertTrue(store.markComplete(10L));
    }

    @Test
    void stepRampSplitAcrossWorkers() {
        LoadConfigDTO load = stepRamp(List.of(30, 60), List.of(10, 10));
        List<SubTaskDTO> subTasks = buildSubTasks(1L, load, 3, sampleRequests());

        assertEquals(10, subTasks.get(0).getLoad().getStages().get(0).getConcurrency());
        assertEquals(10, subTasks.get(1).getLoad().getStages().get(0).getConcurrency());
        assertEquals(10, subTasks.get(2).getLoad().getStages().get(0).getConcurrency());
        assertEquals(20, subTasks.get(0).getLoad().getStages().get(1).getConcurrency());
    }

    private List<HttpRequestDefDTO> sampleRequests() {
        HttpRequestDefDTO get = new HttpRequestDefDTO();
        get.setMethod("GET");
        get.setUrl("http://localhost:8080/api/demo/ping");
        return List.of(get);
    }

    private List<SubTaskDTO> buildSubTasks(Long taskId, LoadConfigDTO load, int workerCount,
                                           List<HttpRequestDefDTO> requests) {
        List<SubTaskDTO> subTasks = new ArrayList<>();
        for (int i = 0; i < workerCount; i++) {
            SubTaskDTO sub = new SubTaskDTO();
            sub.setTaskId(taskId);
            sub.setLoad(LoadConfigResolver.splitForWorker(load, i, workerCount));
            sub.setRequests(requests);
            subTasks.add(sub);
        }
        return subTasks;
    }

    private LoadConfigDTO fixedConcurrency(int concurrency, int duration) {
        LoadConfigDTO load = new LoadConfigDTO();
        load.setMode(LoadMode.FIXED_CONCURRENCY);
        load.setConcurrency(concurrency);
        load.setDurationSeconds(duration);
        return load;
    }

    private LoadConfigDTO fixedRps(int targetRps, int duration) {
        LoadConfigDTO load = new LoadConfigDTO();
        load.setMode(LoadMode.FIXED_RPS);
        load.setTargetRps(targetRps);
        load.setDurationSeconds(duration);
        return load;
    }

    private LoadConfigDTO stepRamp(List<Integer> concurrencies, List<Integer> durations) {
        LoadConfigDTO load = new LoadConfigDTO();
        load.setMode(LoadMode.STEP_RAMP);
        List<com.yinfeng.interview.dto.LoadStageDTO> stages = new ArrayList<>();
        for (int i = 0; i < concurrencies.size(); i++) {
            com.yinfeng.interview.dto.LoadStageDTO stage = new com.yinfeng.interview.dto.LoadStageDTO();
            stage.setConcurrency(concurrencies.get(i));
            stage.setDurationSeconds(durations.get(i));
            stages.add(stage);
        }
        load.setStages(stages);
        return load;
    }
}
