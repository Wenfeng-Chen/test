package com.yinfeng.interview.service;

import com.yinfeng.interview.dto.LoadConfigDTO;
import com.yinfeng.interview.dto.LoadStageDTO;
import com.yinfeng.interview.enums.LoadMode;

import java.util.ArrayList;
import java.util.List;

public final class LoadConfigResolver {

    private LoadConfigResolver() {
    }

    public static LoadMode mode(LoadConfigDTO load) {
        return load.getMode() != null ? load.getMode() : LoadMode.FIXED_CONCURRENCY;
    }

    public static int totalDurationSeconds(LoadConfigDTO load) {
        return switch (mode(load)) {
            case FIXED_CONCURRENCY, FIXED_RPS -> load.getDurationSeconds();
            case STEP_RAMP -> load.getStages().stream().mapToInt(LoadStageDTO::getDurationSeconds).sum();
        };
    }

    public static int peakConcurrency(LoadConfigDTO load) {
        return switch (mode(load)) {
            case FIXED_CONCURRENCY -> load.getConcurrency();
            case FIXED_RPS -> Math.min(load.getTargetRps(), 256);
            case STEP_RAMP -> load.getStages().stream().mapToInt(LoadStageDTO::getConcurrency).max().orElse(0);
        };
    }

    public static void validate(LoadConfigDTO load) {
        if (load == null) {
            throw new IllegalArgumentException("Load config is required");
        }
        switch (mode(load)) {
            case FIXED_CONCURRENCY -> {
                if (load.getConcurrency() <= 0) {
                    throw new IllegalArgumentException("concurrency must be positive");
                }
                if (load.getDurationSeconds() <= 0) {
                    throw new IllegalArgumentException("durationSeconds must be positive");
                }
            }
            case FIXED_RPS -> {
                if (load.getTargetRps() <= 0) {
                    throw new IllegalArgumentException("targetRps must be positive");
                }
                if (load.getDurationSeconds() <= 0) {
                    throw new IllegalArgumentException("durationSeconds must be positive");
                }
            }
            case STEP_RAMP -> {
                if (load.getStages() == null || load.getStages().isEmpty()) {
                    throw new IllegalArgumentException("stages must not be empty for STEP_RAMP");
                }
                for (LoadStageDTO stage : load.getStages()) {
                    if (stage.getConcurrency() <= 0) {
                        throw new IllegalArgumentException("stage concurrency must be positive");
                    }
                    if (stage.getDurationSeconds() <= 0) {
                        throw new IllegalArgumentException("stage durationSeconds must be positive");
                    }
                }
            }
        }
    }

    public static LoadConfigDTO splitForWorker(LoadConfigDTO source, int workerIndex, int workerCount) {
        LoadConfigDTO copy = copyOf(source);
        switch (mode(source)) {
            case FIXED_CONCURRENCY -> copy.setConcurrency(
                    TaskDispatchPlanner.splitConcurrency(source.getConcurrency(), workerCount).get(workerIndex));
            case FIXED_RPS -> copy.setTargetRps(
                    TaskDispatchPlanner.splitConcurrency(source.getTargetRps(), workerCount).get(workerIndex));
            case STEP_RAMP -> {
                List<LoadStageDTO> stages = new ArrayList<>();
                for (LoadStageDTO stage : source.getStages()) {
                    LoadStageDTO workerStage = new LoadStageDTO();
                    workerStage.setDurationSeconds(stage.getDurationSeconds());
                    workerStage.setConcurrency(
                            TaskDispatchPlanner.splitConcurrency(stage.getConcurrency(), workerCount).get(workerIndex));
                    stages.add(workerStage);
                }
                copy.setStages(stages);
            }
        }
        return copy;
    }

    private static LoadConfigDTO copyOf(LoadConfigDTO source) {
        LoadConfigDTO copy = new LoadConfigDTO();
        copy.setMode(mode(source));
        copy.setConcurrency(source.getConcurrency());
        copy.setDurationSeconds(source.getDurationSeconds());
        copy.setTargetRps(source.getTargetRps());
        if (source.getStages() != null) {
            List<LoadStageDTO> stages = new ArrayList<>();
            for (LoadStageDTO stage : source.getStages()) {
                LoadStageDTO s = new LoadStageDTO();
                s.setConcurrency(stage.getConcurrency());
                s.setDurationSeconds(stage.getDurationSeconds());
                stages.add(s);
            }
            copy.setStages(stages);
        }
        return copy;
    }
}
