package com.yinfeng.interview.service;

import com.yinfeng.interview.dto.LoadConfigDTO;
import com.yinfeng.interview.dto.LoadStageDTO;
import com.yinfeng.interview.enums.LoadMode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LoadConfigResolverTest {

    @Test
    void totalDurationForStepRamp() {
        LoadConfigDTO load = new LoadConfigDTO();
        load.setMode(LoadMode.STEP_RAMP);
        LoadStageDTO s1 = new LoadStageDTO();
        s1.setConcurrency(10);
        s1.setDurationSeconds(15);
        LoadStageDTO s2 = new LoadStageDTO();
        s2.setConcurrency(30);
        s2.setDurationSeconds(20);
        load.setStages(List.of(s1, s2));

        assertEquals(35, LoadConfigResolver.totalDurationSeconds(load));
        assertEquals(30, LoadConfigResolver.peakConcurrency(load));
    }

    @Test
    void defaultModeIsFixedConcurrency() {
        LoadConfigDTO load = new LoadConfigDTO();
        load.setConcurrency(20);
        load.setDurationSeconds(10);
        assertEquals(LoadMode.FIXED_CONCURRENCY, LoadConfigResolver.mode(load));
    }

    @Test
    void splitRpsAcrossWorkers() {
        LoadConfigDTO load = new LoadConfigDTO();
        load.setMode(LoadMode.FIXED_RPS);
        load.setTargetRps(101);
        load.setDurationSeconds(10);

        LoadConfigDTO w0 = LoadConfigResolver.splitForWorker(load, 0, 3);
        LoadConfigDTO w1 = LoadConfigResolver.splitForWorker(load, 1, 3);
        LoadConfigDTO w2 = LoadConfigResolver.splitForWorker(load, 2, 3);

        assertEquals(34, w0.getTargetRps());
        assertEquals(34, w1.getTargetRps());
        assertEquals(33, w2.getTargetRps());
    }

    @Test
    void validateRejectsEmptyStages() {
        LoadConfigDTO load = new LoadConfigDTO();
        load.setMode(LoadMode.STEP_RAMP);
        assertThrows(IllegalArgumentException.class, () -> LoadConfigResolver.validate(load));
    }
}
