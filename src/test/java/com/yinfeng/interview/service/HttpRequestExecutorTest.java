package com.yinfeng.interview.service;

import com.yinfeng.interview.dto.HttpRequestDefDTO;
import com.yinfeng.interview.support.TestHttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.net.http.HttpClient;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class HttpRequestExecutorTest {

    private static TestHttpServer testServer;
    private HttpRequestExecutor executor;

    @BeforeAll
    static void startServer() throws Exception {
        testServer = TestHttpServer.start();
    }

    @AfterAll
    static void stopServer() {
        if (testServer != null) {
            testServer.stop();
        }
    }

    @BeforeEach
    void setUp() {
        executor = new HttpRequestExecutor(HttpClient.newHttpClient());
    }

    static Stream<Arguments> standardHttpMethods() {
        return Stream.of(
                Arguments.of("GET", 200, null),
                Arguments.of("POST", 201, "{\"name\":\"test\"}"),
                Arguments.of("PUT", 200, "{\"name\":\"test\"}"),
                Arguments.of("PATCH", 200, "{\"name\":\"test\"}"),
                Arguments.of("DELETE", 204, null),
                Arguments.of("HEAD", 200, null),
                Arguments.of("OPTIONS", 200, null)
        );
    }

    @ParameterizedTest
    @MethodSource("standardHttpMethods")
    void executeStandardMethods(String method, int expectedStatus, String body) {
        HttpRequestDefDTO def = new HttpRequestDefDTO();
        def.setMethod(method);
        def.setUrl(testServer.baseUrl() + "/test");
        def.setHeaders(Map.of("Content-Type", "application/json"));
        def.setBody(body);

        var result = executor.execute(1L, "test-worker", def);

        assertEquals(method, result.getMethod());
        assertEquals(expectedStatus, result.getStatusCode());
        assertTrue(result.getSuccess());
        assertNotNull(result.getLatencyMs());
        assertTrue(result.getLatencyMs() >= 0);
    }

    @ParameterizedTest
    @MethodSource("apacheOnlyMethods")
    void executeConnectAndTraceDoNotCrash(String method) {
        HttpRequestDefDTO def = new HttpRequestDefDTO();
        def.setMethod(method);
        def.setUrl(testServer.baseUrl() + "/test");

        var result = executor.execute(2L, "test-worker", def);

        assertEquals(method, result.getMethod());
        assertNotNull(result.getLatencyMs());
    }

    static Stream<Arguments> apacheOnlyMethods() {
        return Stream.of(
                Arguments.of("CONNECT"),
                Arguments.of("TRACE")
        );
    }
}
