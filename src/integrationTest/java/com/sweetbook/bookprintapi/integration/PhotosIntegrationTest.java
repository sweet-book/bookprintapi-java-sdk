package com.sweetbook.bookprintapi.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.sweetbook.bookprintapi.BookPrintApiException;
import com.sweetbook.bookprintapi.ListResult;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIfEnvironmentVariable(named = "BOOKPRINT_API_KEY", matches = ".+")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PhotosIntegrationTest extends IntegrationTestBase {

    private String bookUid;
    private Path fixture;

    @BeforeAll
    void setup() throws IOException {
        // sandbox에 책 한 권 새로 생성
        JsonNode book = client.books.create("PHOTOBOOK_A4_SC", "Java SDK photos 통합 테스트");
        bookUid = book.path("data").path("bookUid").asText();
        assertNotNull(bookUid);

        // 클래스패스의 fixture 이미지를 임시 파일로 추출
        fixture = Files.createTempFile("sample_photo_", ".jpg");
        try (InputStream in = getClass().getResourceAsStream("/fixtures/sample_photo.jpg")) {
            assertNotNull(in, "fixture 리소스가 클래스패스에 있어야 함");
            Files.copy(in, fixture, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
        System.out.println("[setup] bookUid=" + bookUid + ", fixture=" + fixture);
    }

    @AfterAll
    void cleanup() {
        if (bookUid != null) {
            try {
                client.books.delete(bookUid);
                System.out.println("[cleanup] 책 삭제 OK");
            } catch (Exception e) {
                System.out.println("[cleanup] 책 삭제 실패 — " + e.getMessage());
            }
        }
        if (fixture != null) {
            try {
                Files.deleteIfExists(fixture);
            } catch (IOException ignored) {}
        }
    }

    @Test
    @Order(1)
    void upload_photoSucceeds() {
        try {
            JsonNode resp = client.photos.upload(bookUid, fixture);
            assertTrue(resp.path("success").asBoolean(false));
            JsonNode data = resp.path("data");
            String fileName = data.path("fileName").asText();
            assertTrue(fileName.length() > 0);
            System.out.println("[photos.upload] OK fileName=" + fileName + ", size=" + data.path("size").asLong());
        } catch (BookPrintApiException e) {
            System.out.println("[photos.upload] FAIL status=" + e.statusCode()
                    + ", errorCode=" + e.errorCode() + ", message=" + e.userMessage());
            throw e;
        }
    }

    @Test
    @Order(2)
    void list_includesUploadedPhoto() {
        ListResult<Map<String, Object>> result = client.photos.list(bookUid);
        assertTrue(result.items().size() >= 1, "1장 이상 업로드된 상태");
        System.out.println("[photos.list] count=" + result.items().size());
    }
}
