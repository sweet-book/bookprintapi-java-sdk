package com.sweetbook.bookprintapi.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfsClientTest extends AbstractClientTestBase {

    @Test
    void uploadCover_postsApplicationPdf(@TempDir Path tmp) throws IOException {
        Path pdf = tmp.resolve("cover.pdf");
        Files.write(pdf, "%PDF-1.4\n".getBytes(StandardCharsets.UTF_8));

        AtomicReference<String> method = new AtomicReference<>();
        AtomicReference<String> body = new AtomicReference<>();
        respond("/books/abc/pdf-cover", exchange -> {
            method.set(exchange.getRequestMethod());
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] resp = "{\"success\":true,\"data\":{\"pdfMode\":\"PDF_UPLOAD\"}}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, resp.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(resp);
            }
        });

        PdfsClient client = new PdfsClient(transport());
        JsonNode resp = client.uploadCover("abc", pdf);
        assertEquals("PDF_UPLOAD", resp.path("data").path("pdfMode").asText());
        assertEquals("POST", method.get());
        assertTrue(body.get().contains("Content-Type: application/pdf"));
    }

    @Test
    void replaceContents_usesPut(@TempDir Path tmp) throws IOException {
        Path pdf = tmp.resolve("contents.pdf");
        Files.write(pdf, "%PDF-1.4\n".getBytes(StandardCharsets.UTF_8));

        AtomicReference<String> method = new AtomicReference<>();
        respond("/books/abc/pdf-contents", exchange -> {
            method.set(exchange.getRequestMethod());
            byte[] resp = "{\"success\":true}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, resp.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(resp);
            }
        });

        PdfsClient client = new PdfsClient(transport());
        client.replaceContents("abc", pdf);
        assertEquals("PUT", method.get());
    }

    @Test
    void downloadCover_returnsBytes() {
        byte[] pdfBytes = "%PDF-1.4 binary content".getBytes(StandardCharsets.UTF_8);
        respond("/books/abc/pdf-cover", exchange -> {
            exchange.getResponseHeaders().add("Content-Type", "application/pdf");
            exchange.sendResponseHeaders(200, pdfBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(pdfBytes);
            }
        });

        PdfsClient client = new PdfsClient(transport());
        byte[] downloaded = client.downloadCover("abc");
        assertArrayEquals(pdfBytes, downloaded);
    }
}
