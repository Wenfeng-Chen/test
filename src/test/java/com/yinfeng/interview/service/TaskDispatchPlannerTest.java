package com.yinfeng.interview.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TaskDispatchPlannerTest {

    @Test
    void splitEvenlyWhenDivisible() {
        assertEquals(List.of(50, 50), TaskDispatchPlanner.splitConcurrency(100, 2));
        assertEquals(List.of(25, 25, 25, 25), TaskDispatchPlanner.splitConcurrency(100, 4));
    }

    @Test
    void splitWithRemainderToFirstWorkers() {
        assertEquals(List.of(34, 33, 33), TaskDispatchPlanner.splitConcurrency(100, 3));
        assertEquals(List.of(13, 13, 13, 12, 12), TaskDispatchPlanner.splitConcurrency(63, 5));
    }

    @Test
    void splitSingleWorkerGetsAll() {
        assertEquals(List.of(100), TaskDispatchPlanner.splitConcurrency(100, 1));
    }

    @Test
    void splitZeroConcurrency() {
        assertEquals(List.of(0, 0, 0), TaskDispatchPlanner.splitConcurrency(0, 3));
    }

    @Test
    void quotasSumEqualsTotal() {
        List<Integer> quotas = TaskDispatchPlanner.splitConcurrency(127, 8);
        assertEquals(127, quotas.stream().mapToInt(Integer::intValue).sum());
        assertEquals(8, quotas.size());
    }

    @Test
    void rejectInvalidWorkerCount() {
        assertThrows(IllegalArgumentException.class, () -> TaskDispatchPlanner.splitConcurrency(100, 0));
    }
}
