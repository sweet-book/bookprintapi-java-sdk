package com.sweetbook.bookprintapi.http;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.HttpServer;
import com.sweetbook.bookprintapi.BookPrintApiException;
import com.sweetbook.bookprintapi.ErrorCodes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HttpTransportTest {

    private HttpServer server;
    private int port;

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        port = server.getAddress().getPort();
        server.start();
    }

    @AfterEach
    void stop() {
        if (server != null) server.stop(0);
    }

    private HttpTransport client(int retries) {
        return new HttpTransport("http://127.0.0.1:" + port, "test-key", Duration.ofSeconds(5), retries);
    }

    private void respond(String path, int status, String body) {
        server.createContext(path, exchange -> {
            // 헤더 검증 (Bearer)
            String auth = exchange.getRequestHeaders().getFirst("Authorization");
            if (!"Bearer test-key".equals(auth)) {
                exchange.sendResponseHeaders(401, 0);
                exchange.close();
                return;
            }
            byte[] bytes = body == null ? new byte[0] : body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
    }

    @Test
    void getRequest_attachesApiKey() {
        respond("/books", 200, "{\"success\":true,\"data\":[{\"bookUid\":\"b1\"}]}");
        JsonNode body = client(0).get("/books", null);
        assertEquals("b1", body.path("data").get(0).path("bookUid").asText());
    }

    @Test
    void postRequest_serializesJson() {
        AtomicInteger seen = new AtomicInteger();
        server.createContext("/orders", exchange -> {
            byte[] reqBody = exchange.getRequestBody().readAllBytes();
            String s = new String(reqBody, StandardCharsets.UTF_8);
            assertTrue(s.contains("\"items\""));
            seen.incrementAndGet();
            byte[] resp = "{\"success\":true,\"data\":{\"orderUid\":\"o1\"}}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, resp.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(resp);
            }
        });
        Map<String, Object> body = new HashMap<>();
        body.put("items", "anything");
        JsonNode resp = client(0).post("/orders", body);
        assertEquals("o1", resp.path("data").path("orderUid").asText());
        assertEquals(1, seen.get());
    }

    @Test
    void clientError_throwsApiException() {
        respond("/books", 400, "{"
                + "\"success\":false,"
                + "\"errorCode\":\"ERR_VALIDATION_FAILED\","
                + "\"message\":\"검증 실패\","
                + "\"errors\":[\"제목 필수\"]"
                + "}");
        BookPrintApiException ex = assertThrows(BookPrintApiException.class,
                () -> client(0).get("/books", null));
        assertEquals(400, ex.statusCode());
        assertEquals(ErrorCodes.VALIDATION_FAILED, ex.errorCode());
    }

    @Test
    void serverError_retriesThenSucceeds() {
        AtomicInteger attempts = new AtomicInteger();
        server.createContext("/credits", exchange -> {
            int n = attempts.incrementAndGet();
            byte[] resp;
            if (n < 3) {
                // 첫 두 번은 503
                resp = "{\"success\":false,\"errorCode\":\"ERR_INTERNAL_ERROR\"}".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(503, resp.length);
            } else {
                resp = "{\"success\":true,\"data\":{\"balance\":1000}}".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, resp.length);
            }
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(resp);
            }
        });
        // retries=3 → 첫 시도 + 2회 재시도 안에 성공
        JsonNode body = client(3).get("/credits", null);
        assertEquals(1000, body.path("data").path("balance").asInt());
        assertEquals(3, attempts.get());
    }

    @Test
    void serverError_exhaustsRetries() {
        respond("/credits", 500, "{\"success\":false,\"errorCode\":\"ERR_INTERNAL_ERROR\"}");
        BookPrintApiException ex = assertThrows(BookPrintApiException.class,
                () -> client(2).get("/credits", null));
        assertEquals(500, ex.statusCode());
    }

    @Test
    void deleteRequest_returnsBody() {
        respond("/books/abc", 200, "{\"success\":true}");
        JsonNode body = client(0).delete("/books/abc");
        assertTrue(body.path("success").asBoolean());
    }

    @Test
    void unauthorized_thrownWithStatus() {
        // Bearer 헤더가 없으면 미인증, 그 외 케이스 시뮬레이션
        respond("/secret", 401, "{\"success\":false,\"errorCode\":\"ERR_UNAUTHORIZED\"}");
        BookPrintApiException ex = assertThrows(BookPrintApiException.class,
                () -> client(0).get("/secret", null));
        assertEquals(401, ex.statusCode());
        assertEquals(ErrorCodes.UNAUTHORIZED, ex.errorCode());
    }

    @Test
    void queryParams_urlEncoded() {
        AtomicInteger seen = new AtomicInteger();
        server.createContext("/templates", exchange -> {
            String q = exchange.getRequestURI().getRawQuery();
            assertTrue(q.contains("limit=20"));
            assertTrue(q.contains("scope=user"));
            seen.incrementAndGet();
            byte[] resp = "{\"success\":true,\"data\":[]}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, resp.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(resp);
            }
        });
        Map<String, Object> q = new HashMap<>();
        q.put("limit", 20);
        q.put("scope", "user");
        client(0).get("/templates", q);
        assertEquals(1, seen.get());
    }
}
