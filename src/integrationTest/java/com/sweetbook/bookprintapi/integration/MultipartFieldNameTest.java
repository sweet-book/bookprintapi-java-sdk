package com.sweetbook.bookprintapi.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.sweetbook.bookprintapi.BookPrintApiException;
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
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 위험 #2 검증: Covers는 multipart 파일 필드명이 {@code files}, Contents는 {@code rowPhotos}.
 * Node SDK 기준으로 매핑했는데 실제 sandbox에서 동작하는지 확인.
 *
 * <p>책 생성 → cover 추가 → contents 추가 → cleanup. cover/content templateUid 는
 * {@link TemplateInspectionTest} 의 {@code listTemplates_globalFirstPage} 출력에서
 * 확보된 PHOTOBOOK_A4_SC cover 템플릿과 SQUAREBOOK_HC content 템플릿 사용.
 *
 * <p>주의: 통합 테스트는 sandbox 카탈로그가 변경되면 깨질 수 있어, 첫 호출에서
 * {@code templates.list}로 실시간 확보하는 방식으로 작성.
 */
@EnabledIfEnvironmentVariable(named = "BOOKPRINT_API_KEY", matches = ".+")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MultipartFieldNameTest extends IntegrationTestBase {

    private String bookUid;
    private String coverTemplateUid;
    private Path fixture;

    @BeforeAll
    void setup() throws IOException {
        // sandbox 99에서 PHOTOBOOK_A4_SC cover 템플릿 동적으로 확보
        Map<String, Object> q = new HashMap<>();
        q.put("limit", 50);
        var templates = client.templates.list(q).items();
        for (var t : templates) {
            if ("cover".equals(t.get("templateKind"))
                    && "PHOTOBOOK_A4_SC".equals(t.get("bookSpecUid"))) {
                coverTemplateUid = (String) t.get("templateUid");
                break;
            }
        }
        assertNotNull(coverTemplateUid, "PHOTOBOOK_A4_SC cover 템플릿을 sandbox에서 찾지 못함");

        // 책 생성
        JsonNode book = client.books.create("PHOTOBOOK_A4_SC", "Java SDK multipart 필드명 검증");
        bookUid = book.path("data").path("bookUid").asText();

        // fixture 추출
        fixture = Files.createTempFile("multipart_test_", ".jpg");
        try (InputStream in = getClass().getResourceAsStream("/fixtures/sample_photo.jpg")) {
            Files.copy(in, fixture, StandardCopyOption.REPLACE_EXISTING);
        }
        System.out.println("[setup] bookUid=" + bookUid + ", coverTemplateUid=" + coverTemplateUid);
    }

    @AfterAll
    void cleanup() {
        if (bookUid != null) {
            try {
                client.books.delete(bookUid);
            } catch (Exception ignored) {}
        }
        if (fixture != null) {
            try { Files.deleteIfExists(fixture); } catch (IOException ignored) {}
        }
    }

    @Test
    @Order(1)
    void cover_legacyFilesFieldName_serverResponse() {
        // 시나리오 A: deprecated createWithFiles — 'files' 필드명으로 첨부
        try {
            @SuppressWarnings("deprecation")
            JsonNode resp = client.covers.createWithFiles(bookUid, coverTemplateUid,
                    new HashMap<>(), Collections.singletonList(fixture));
            System.out.println("[A: files 필드명] OK — success=" + resp.path("success").asBoolean());
        } catch (BookPrintApiException e) {
            System.out.println("[A: files 필드명] FAIL (예상) status=" + e.statusCode()
                    + ", message=" + e.userMessage());
        }
    }

    @Test
    @Order(2)
    void cover_bindingNameField_serverResponse() throws IOException {
        // 시나리오 B: 템플릿 binding 이름('coverPhoto')으로 첨부
        // HttpTransport / MultipartBodyPublisher 직접 사용
        var mp = new com.sweetbook.bookprintapi.http.MultipartBodyPublisher()
                .addText("templateUid", coverTemplateUid)
                .addText("parameters", "{}")
                .addFile("coverPhoto", fixture, "image/jpeg");
        try {
            // 내부 transport 노출 (protected 접근 회피 — 우회 라우트)
            // 이 테스트는 진단용이라 reflection 사용
            java.lang.reflect.Method m = client.getClass().getDeclaredMethod("transport");
            m.setAccessible(true);
            var http = (com.sweetbook.bookprintapi.http.HttpTransport) m.invoke(client);
            JsonNode resp = http.postMultipart("/books/" + bookUid + "/cover", mp, null);
            System.out.println("[B: coverPhoto binding 이름] OK — success=" + resp.path("success").asBoolean());
        } catch (BookPrintApiException e) {
            System.out.println("[B: coverPhoto binding 이름] FAIL status=" + e.statusCode()
                    + ", errorCode=" + e.errorCode() + ", message=" + e.userMessage());
        } catch (Exception e) {
            System.out.println("[B: 호출 예외] " + e.getMessage());
        }
    }

    @Test
    @Order(3)
    void cover_uploadedPhotoReference_serverResponse() {
        // 시나리오 C: 사진을 미리 업로드하고 parameters에 fileName으로 참조
        try {
            JsonNode upload = client.photos.upload(bookUid, fixture);
            String uploadedName = upload.path("data").path("fileName").asText();
            System.out.println("[C 사전준비] photos.upload OK → " + uploadedName);

            Map<String, Object> params = new HashMap<>();
            params.put("coverPhoto", uploadedName);
            JsonNode resp = client.covers.create(bookUid, coverTemplateUid, params,
                    (Map<String, java.nio.file.Path>) null);
            System.out.println("[C: parameters.coverPhoto + 파일 미첨부] OK — success="
                    + resp.path("success").asBoolean());
        } catch (BookPrintApiException e) {
            System.out.println("[C] FAIL status=" + e.statusCode()
                    + ", errorCode=" + e.errorCode() + ", message=" + e.userMessage());
        }
    }
}
