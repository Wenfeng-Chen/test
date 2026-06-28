package com.yinfeng.interview.support;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public final class TestHttpServer {

    private final HttpServer server;
    private final String baseUrl;

    private TestHttpServer(HttpServer server, String baseUrl) {
        this.server = server;
        this.baseUrl = baseUrl;
    }

    public static TestHttpServer start() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/test", new MethodEchoHandler());
        server.start();
        String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
        return new TestHttpServer(server, baseUrl);
    }

    public String baseUrl() {
        return baseUrl;
    }

    public void stop() {
        server.stop(0);
    }

    private static class MethodEchoHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            int status = switch (method) {
                case "GET" -> 200;
                case "POST" -> 201;
                case "PUT", "PATCH" -> 200;
                case "DELETE" -> 204;
                case "HEAD" -> 200;
                case "OPTIONS" -> 200;
                default -> 405;
            };

            if ("OPTIONS".equals(method)) {
                exchange.getResponseHeaders().add("Allow", "GET,POST,PUT,PATCH,DELETE,HEAD,OPTIONS");
            }

            byte[] body = ("{\"method\":\"" + method + "\"}").getBytes(StandardCharsets.UTF_8);
            if ("HEAD".equals(method) || "DELETE".equals(method)) {
                exchange.sendResponseHeaders(status, -1);
            } else {
                exchange.sendResponseHeaders(status, body.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(body);
                }
            }
            exchange.close();
        }
    }
}
