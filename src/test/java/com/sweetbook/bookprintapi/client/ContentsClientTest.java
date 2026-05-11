package com.sweetbook.bookprintapi.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContentsClientTest extends AbstractClientTestBase {

    @Test
    void insert_usesBindingNamesAsMultipartFieldNames(@TempDir Path tmp) throws IOException {
        Path a = tmp.resolve("a.jpg");
        Path b = tmp.resolve("b.jpg");
        Files.write(a, new byte[] {1});
        Files.write(b, new byte[] {2});

        AtomicReference<String> body = new AtomicReference<>();
        AtomicReference<String> query = new AtomicReference<>();
        respond("/books/abc/contents", exchange -> {
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            query.set(exchange.getRequestURI().getRawQuery());
            byte[] resp = "{\"success\":true,\"data\":{\"pageNum\":3}}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, resp.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(resp);
            }
        });

        Map<String, Object> params = new HashMap<>();
        params.put("text", "본문 내용");

        Map<String, Path> bindingFiles = new LinkedHashMap<>();
        bindingFiles.put("mainPhoto", a);
        bindingFiles.put("subPhoto", b);

        ContentsClient client = new ContentsClient(transport());
        JsonNode resp = client.insert("abc", "T-CONTENT", params, bindingFiles, "page");
        assertEquals(3, resp.path("data").path("pageNum").asInt());

        String b2 = body.get();
        // multipart 파일 part 이름 = 템플릿 binding 이름
        assertTrue(b2.contains("name=\"mainPhoto\"; filename=\"a.jpg\""));
        assertTrue(b2.contains("name=\"subPhoto\"; filename=\"b.jpg\""));
        // breakBefore는 query string으로
        assertEquals("breakBefore=page", query.get());
    }

    @Test
    void insert_omitsBreakBeforeWhenNull() throws IOException {
        AtomicReference<String> query = new AtomicReference<>();
        respond("/books/abc/contents", exchange -> {
            query.set(exchange.getRequestURI().getRawQuery());
            byte[] resp = "{\"success\":true}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, resp.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(resp);
            }
        });

        ContentsClient client = new ContentsClient(transport());
        client.insert("abc", "T1", new HashMap<>());
        assertEquals(null, query.get());
    }

    @Test
    void clear_callsDelete() {
        respond("/books/abc/contents", 200, "{\"success\":true}");
        ContentsClient client = new ContentsClient(transport());
        client.clear("abc");
        assertEquals("DELETE", recorded.get(0).method);
    }
}
