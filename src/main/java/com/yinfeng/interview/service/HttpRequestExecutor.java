package com.yinfeng.interview.service;

import com.yinfeng.interview.dto.HttpRequestDefDTO;
import com.yinfeng.interview.entity.RequestResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class HttpRequestExecutor {

    private final HttpClient httpClient;

    public RequestResult execute(Long taskId, String workerId, HttpRequestDefDTO def) {
        RequestResult result = new RequestResult();
        result.setTaskId(taskId);
        result.setWorkerId(workerId);
        result.setMethod(def.getMethod().toUpperCase());
        result.setUrl(def.getUrl());

        long startNanos = System.nanoTime();
        try {
            String method = def.getMethod().toUpperCase();
            if ("CONNECT".equals(method) || "TRACE".equals(method)) {
                executeApache(method, def, result);
            } else {
                executeJdk(method, def, result);
            }
            result.setSuccess(result.getStatusCode() != null && result.getStatusCode() < 400);
        } catch (Exception e) {
            result.setSuccess(false);
            result.setErrorMessage(truncate(e.getMessage()));
            result.setStatusCode(null);
        }
        result.setLatencyMs((System.nanoTime() - startNanos) / 1_000_000L);
        return result;
    }

    private void executeJdk(String method, HttpRequestDefDTO def, RequestResult result) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(def.getUrl()))
                .timeout(Duration.ofSeconds(30));

        if (def.getHeaders() != null) {
            def.getHeaders().forEach(builder::header);
        }

        HttpRequest.BodyPublisher bodyPublisher = HttpRequest.BodyPublishers.noBody();
        if (def.getBody() != null && !def.getBody().isBlank()) {
            bodyPublisher = HttpRequest.BodyPublishers.ofString(def.getBody());
        }

        builder.method(method, hasBody(method) ? bodyPublisher : HttpRequest.BodyPublishers.noBody());

        HttpResponse<Void> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.discarding());
        result.setStatusCode(response.statusCode());
    }

    private void executeApache(String method, HttpRequestDefDTO def, RequestResult result) throws Exception {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            ClassicHttpRequest request = new HttpUriRequestBase(method, URI.create(def.getUrl()));
            if (def.getHeaders() != null) {
                def.getHeaders().forEach(request::addHeader);
            }
            if (def.getBody() != null && !def.getBody().isBlank()) {
                request.setEntity(new StringEntity(def.getBody(), StandardCharsets.UTF_8));
            }
            client.execute(request, response -> {
                result.setStatusCode(response.getCode());
                return null;
            });
        }
    }

    private boolean hasBody(String method) {
        return switch (method) {
            case "POST", "PUT", "PATCH" -> true;
            default -> false;
        };
    }

    private String truncate(String msg) {
        if (msg == null) {
            return null;
        }
        return msg.length() > 500 ? msg.substring(0, 500) : msg;
    }
}
