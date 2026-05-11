package com.sweetbook.bookprintapi.client;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.sweetbook.bookprintapi.http.HttpTransport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/** 도메인 클라이언트 테스트 공통: 임시 HttpServer 기동 + 핸들러 등록 헬퍼. */
abstract class AbstractClientTestBase {

    HttpServer server;
    int port;
    final List<RecordedRequest> recorded = new ArrayList<>();

    @BeforeEach
    void start() throws IOException {
        recorded.clear();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        port = server.getAddress().getPort();
        server.start();
    }

    @AfterEach
    void stop() {
        if (server != null) server.stop(0);
    }

    HttpTransport transport() {
        return new HttpTransport("http://127.0.0.1:" + port, "test-key", Duration.ofSeconds(5), 0);
    }

    /** path에 응답 핸들러 등록. {@link RecordedRequest}로 요청 정보 캡처. */
    void respond(String path, int status, String body) {
        respond(path, exchange -> {
            recorded.add(RecordedRequest.from(exchange));
            byte[] bytes = body == null ? new byte[0] : body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
    }

    void respond(String path, HttpHandler handler) {
        server.createContext(path, handler);
    }

    static final class RecordedRequest {
        final String method;
        final String path;
        final String query;
        final String body;

        RecordedRequest(String method, String path, String query, String body) {
            this.method = method;
            this.path = path;
            this.query = query;
            this.body = body;
        }

        static RecordedRequest from(HttpExchange exchange) throws IOException {
            byte[] reqBody = exchange.getRequestBody().readAllBytes();
            return new RecordedRequest(
                    exchange.getRequestMethod(),
                    exchange.getRequestURI().getPath(),
                    exchange.getRequestURI().getRawQuery(),
                    new String(reqBody, StandardCharsets.UTF_8)
            );
        }
    }
}
