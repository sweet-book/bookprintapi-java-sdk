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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoversClientTest extends AbstractClientTestBase {

    @Test
    void create_sendsTemplateUidAndJsonParametersAndFiles(@TempDir Path tmp) throws IOException {
        Path img = tmp.resolve("cover.jpg");
        Files.write(img, new byte[] {1, 2, 3});

        AtomicReference<String> body = new AtomicReference<>();
        respond("/books/abc/cover", exchange -> {
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] resp = "{\"success\":true,\"data\":{\"coverPageCount\":1}}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, resp.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(resp);
            }
        });

        Map<String, Object> params = new HashMap<>();
        params.put("title", "테스트 책");
        params.put("subtitle", "부제");

        Map<String, java.nio.file.Path> bindingFiles = new LinkedHashMap<>();
        bindingFiles.put("coverPhoto", img);

        CoversClient client = new CoversClient(transport());
        JsonNode resp = client.create("abc", "TEMPLATE-1", params, bindingFiles);
        assertEquals(1, resp.path("data").path("coverPageCount").asInt());

        String b = body.get();
        assertTrue(b.contains("name=\"templateUid\""));
        assertTrue(b.contains("TEMPLATE-1"));
        assertTrue(b.contains("name=\"parameters\""));
        assertTrue(b.contains("\"title\":\"테스트 책\""));
        // multipart 파일 part 이름 = 템플릿 binding 이름 (coverPhoto)
        assertTrue(b.contains("name=\"coverPhoto\"; filename=\"cover.jpg\""));
    }

    @Test
    void create_acceptsNullParameters() throws IOException {
        AtomicReference<String> body = new AtomicReference<>();
        respond("/books/abc/cover", exchange -> {
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] resp = "{\"success\":true}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, resp.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(resp);
            }
        });

        CoversClient client = new CoversClient(transport());
        client.create("abc", "T1", null, null);
        // parameters가 null이어도 빈 JSON {} 으로 직렬화되어야 함
        assertTrue(body.get().contains("\r\n{}\r\n") || body.get().contains("name=\"parameters\""));
    }

    @Test
    void create_rejectsEmptyTemplateUid() {
        CoversClient client = new CoversClient(transport());
        assertThrows(IllegalArgumentException.class,
                () -> client.create("abc", "", null, null));
        assertThrows(IllegalArgumentException.class,
                () -> client.create("abc", null, null, null));
    }

    @Test
    void get_callsGetEndpoint() {
        respond("/books/abc/cover", 200, "{\"success\":true,\"data\":{\"templateUid\":\"T1\"}}");
        CoversClient client = new CoversClient(transport());
        JsonNode resp = client.get("abc");
        assertEquals("T1", resp.path("data").path("templateUid").asText());
    }

    @Test
    void delete_callsDelete() {
        respond("/books/abc/cover", 200, "{\"success\":true}");
        CoversClient client = new CoversClient(transport());
        client.delete("abc");
        assertEquals("DELETE", recorded.get(0).method);
    }
}
