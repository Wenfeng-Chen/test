package com.yinfeng.interview.service;

import com.yinfeng.interview.dto.LoadConfigDTO;
import com.yinfeng.interview.dto.LoadStageDTO;
import com.yinfeng.interview.dto.SubTaskDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SubTaskStoreTest {

    private SubTaskStore store;

    @BeforeEach
    void setUp() {
        store = new SubTaskStore();
    }

    @Test
    void pollReturnsEnqueuedSubTasksInOrder() {
        store.enqueueAll(1L, List.of(subTask(1L, 10), subTask(1L, 20)));

        SubTaskDTO first = store.poll();
        SubTaskDTO second = store.poll();

        assertNotNull(first);
        assertNotNull(second);
        assertEquals(10, first.getLoad().getConcurrency());
        assertEquals(20, second.getLoad().getConcurrency());
        assertNull(store.poll());
    }

    @Test
    void markCompleteReturnsFalseUntilAllWorkersDone() {
        store.enqueueAll(1L, List.of(subTask(1L, 10), subTask(1L, 20), subTask(1L, 30)));

        assertFalse(store.markComplete(1L));
        assertFalse(store.markComplete(1L));
        assertTrue(store.markComplete(1L));
    }

    @Test
    void markCompleteForUnknownTaskReturnsTrue() {
        assertTrue(store.markComplete(999L));
    }

    @Test
    void clearRemovesCompletionTracking() {
        store.enqueueAll(2L, List.of(subTask(2L, 5), subTask(2L, 5)));
        store.markComplete(2L);
        store.clear(2L);
        assertTrue(store.markComplete(2L));
    }

    private SubTaskDTO subTask(Long taskId, int concurrency) {
        LoadConfigDTO load = new LoadConfigDTO();
        load.setConcurrency(concurrency);
        load.setDurationSeconds(10);
        SubTaskDTO sub = new SubTaskDTO();
        sub.setTaskId(taskId);
        sub.setLoad(load);
        return sub;
    }
}
