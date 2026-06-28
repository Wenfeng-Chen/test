package com.yinfeng.interview.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yinfeng.interview.dto.HttpRequestDefDTO;
import com.yinfeng.interview.dto.LoadConfigDTO;
import com.yinfeng.interview.dto.TestPlanDTO;
import com.yinfeng.interview.enums.RunMode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TestPlanScenarioTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void standalonePlanJsonRoundTrip() throws Exception {
        TestPlanDTO plan = standalonePlan(20, 10);
        String json = objectMapper.writeValueAsString(plan);
        TestPlanDTO parsed = objectMapper.readValue(json, TestPlanDTO.class);

        assertEquals(RunMode.STANDALONE, parsed.getMode());
        assertEquals(20, parsed.getLoad().getConcurrency());
        assertEquals(1, parsed.getRequests().size());
        assertEquals("GET", parsed.getRequests().get(0).getMethod());
    }

    @Test
    void distributedPlanJsonRoundTrip() throws Exception {
        TestPlanDTO plan = distributedPlan(50, 15);
        String json = objectMapper.writeValueAsString(plan);
        TestPlanDTO parsed = objectMapper.readValue(json, TestPlanDTO.class);

        assertEquals(RunMode.DISTRIBUTED, parsed.getMode());
        assertEquals(50, parsed.getLoad().getConcurrency());
    }

    @Test
    void multiMethodPlanContainsAllConfiguredMethods() {
        TestPlanDTO plan = allMethodsPlan("http://localhost:8080");
        List<String> methods = plan.getRequests().stream().map(HttpRequestDefDTO::getMethod).toList();

        assertEquals(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS"), methods);
    }

    @Test
    void distributedSplitForFiveWorkers() {
        TestPlanDTO plan = distributedPlan(60, 20);
        List<Integer> quotas = TaskDispatchPlanner.splitConcurrency(
                plan.getLoad().getConcurrency(), 5);
        assertEquals(List.of(12, 12, 12, 12, 12), quotas);
    }

    @ParameterizedTest
    @ValueSource(strings = {"standalone.json", "distributed.json", "all_methods.json"})
    void loadTestPlanFixtures(String fileName) throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/testplans/" + fileName)) {
            assertNotNull(in, "fixture missing: " + fileName);
            TestPlanDTO plan = objectMapper.readValue(in, TestPlanDTO.class);
            assertNotNull(plan.getName());
            assertNotNull(plan.getMode());
            assertFalse(plan.getRequests().isEmpty());
            assertTrue(plan.getLoad().getConcurrency() > 0);
        }
    }

    @Test
    void standaloneFixtureUsesStandaloneMode() throws Exception {
        assertEquals(RunMode.STANDALONE, loadFixture("standalone.json").getMode());
    }

    @Test
    void distributedFixtureUsesDistributedMode() throws Exception {
        assertEquals(RunMode.DISTRIBUTED, loadFixture("distributed.json").getMode());
    }

    @Test
    void allMethodsFixtureHasSevenRequests() throws Exception {
        assertEquals(7, loadFixture("all_methods.json").getRequests().size());
    }

    private TestPlanDTO loadFixture(String fileName) throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/testplans/" + fileName)) {
            return objectMapper.readValue(in, TestPlanDTO.class);
        }
    }

    private TestPlanDTO standalonePlan(int concurrency, int duration) {
        TestPlanDTO plan = new TestPlanDTO();
        plan.setName("standalone-test");
        plan.setMode(RunMode.STANDALONE);
        plan.setLoad(load(concurrency, duration));
        plan.setRequests(List.of(request("GET", "http://localhost:8080/api/demo/ping")));
        return plan;
    }

    private TestPlanDTO distributedPlan(int concurrency, int duration) {
        TestPlanDTO plan = new TestPlanDTO();
        plan.setName("distributed-test");
        plan.setMode(RunMode.DISTRIBUTED);
        plan.setLoad(load(concurrency, duration));
        plan.setRequests(List.of(request("GET", "http://localhost:8080/api/demo/ping")));
        return plan;
    }

    private TestPlanDTO allMethodsPlan(String baseUrl) {
        TestPlanDTO plan = new TestPlanDTO();
        plan.setName("all-methods-test");
        plan.setMode(RunMode.STANDALONE);
        plan.setLoad(load(10, 5));
        plan.setRequests(List.of(
                request("GET", baseUrl + "/api/demo/resource"),
                request("POST", baseUrl + "/api/demo/resource", "{\"k\":\"v\"}"),
                request("PUT", baseUrl + "/api/demo/resource", "{\"k\":\"v\"}"),
                request("PATCH", baseUrl + "/api/demo/resource", "{\"k\":\"v\"}"),
                request("DELETE", baseUrl + "/api/demo/resource"),
                request("HEAD", baseUrl + "/api/demo/resource"),
                request("OPTIONS", baseUrl + "/api/demo/resource")
        ));
        return plan;
    }

    private LoadConfigDTO load(int concurrency, int duration) {
        LoadConfigDTO load = new LoadConfigDTO();
        load.setConcurrency(concurrency);
        load.setDurationSeconds(duration);
        return load;
    }

    private HttpRequestDefDTO request(String method, String url) {
        return request(method, url, null);
    }

    private HttpRequestDefDTO request(String method, String url, String body) {
        HttpRequestDefDTO req = new HttpRequestDefDTO();
        req.setMethod(method);
        req.setUrl(url);
        req.setBody(body);
        return req;
    }
}
