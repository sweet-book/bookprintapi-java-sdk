package com.sweetbook.bookprintapi.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.sweetbook.bookprintapi.ListResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PhotosClientTest extends AbstractClientTestBase {

    @Test
    void upload_sendsMultipartWithFilePart(@TempDir Path tmp) throws IOException {
        Path img = tmp.resolve("test.jpg");
        Files.write(img, new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}); // JPEG SOI

        AtomicReference<String> contentType = new AtomicReference<>();
        AtomicReference<String> bodyStr = new AtomicReference<>();
        respond("/books/abc/photos", exchange -> {
            contentType.set(exchange.getRequestHeaders().getFirst("Content-Type"));
            bodyStr.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] resp = "{\"success\":true,\"data\":{\"fileName\":\"test.jpg\"}}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, resp.length);
            try (java.io.OutputStream os = exchange.getResponseBody()) {
                os.write(resp);
            }
        });

        PhotosClient client = new PhotosClient(transport());
        JsonNode resp = client.upload("abc", img);
        assertEquals("test.jpg", resp.path("data").path("fileName").asText());
        assertTrue(contentType.get().startsWith("multipart/form-data; boundary="));
        assertTrue(bodyStr.get().contains("Content-Disposition: form-data; name=\"file\"; filename=\"test.jpg\""));
        assertTrue(bodyStr.get().contains("Content-Type: image/jpeg"));
    }

    @Test
    void list_normalizesResponse() {
        respond("/books/abc/photos", 200, "{"
                + "\"success\":true,"
                + "\"data\":[{\"fileName\":\"a.jpg\"},{\"fileName\":\"b.jpg\"}],"
                + "\"pagination\":{\"total\":2,\"limit\":20,\"offset\":0}"
                + "}");
        PhotosClient client = new PhotosClient(transport());
        ListResult<Map<String, Object>> result = client.list("abc");
        assertEquals(2, result.items().size());
    }

    @Test
    void delete_callsDeleteEndpoint() {
        respond("/books/abc/photos/file.jpg", 200, "{\"success\":true}");
        PhotosClient client = new PhotosClient(transport());
        client.delete("abc", "file.jpg");
        assertEquals("DELETE", recorded.get(0).method);
    }

    @Test
    void guessContentType_handlesCommonExtensions(@TempDir Path tmp) throws IOException {
        assertEquals("image/jpeg",
                PhotosClient.guessImageContentType(tmp.resolve("a.JPG")));
        assertEquals("image/png",
                PhotosClient.guessImageContentType(tmp.resolve("b.png")));
        assertEquals("image/heic",
                PhotosClient.guessImageContentType(tmp.resolve("c.heic")));
    }
}
