package com.sweetbook.bookprintapi.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.sweetbook.bookprintapi.ListResult;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BooksClientTest extends AbstractClientTestBase {

    @Test
    void list_normalizesFlattenedResponse() {
        respond("/books", 200, "{"
                + "\"success\":true,"
                + "\"data\":[{\"bookUid\":\"a1\",\"title\":\"제목1\"},{\"bookUid\":\"b2\"}],"
                + "\"pagination\":{\"total\":2,\"limit\":20,\"offset\":0,\"hasMore\":false}"
                + "}");
        BooksClient client = new BooksClient(transport());
        ListResult<Map<String, Object>> result = client.list("finalized");
        assertEquals(2, result.items().size());
        assertEquals("a1", result.items().get(0).get("bookUid"));
        assertEquals("제목1", result.items().get(0).get("title"));
        assertEquals(2, result.pagination().total());

        // 쿼리스트링 검증
        assertEquals(1, recorded.size());
        String q = recorded.get(0).query;
        assertTrue(q.contains("status=finalized"));
        assertTrue(q.contains("limit=20"));
    }

    @Test
    void list_normalizesLegacyResponse() {
        respond("/books", 200, "{"
                + "\"success\":true,"
                + "\"data\":{\"books\":[{\"bookUid\":\"x\"}],\"pagination\":{\"total\":1,\"limit\":10,\"offset\":0}}"
                + "}");
        BooksClient client = new BooksClient(transport());
        ListResult<Map<String, Object>> result = client.list();
        assertEquals(1, result.items().size());
        assertEquals(1, result.pagination().total());
    }

    @Test
    void create_postsRequiredFields() {
        respond("/books", 200, "{\"success\":true,\"data\":{\"bookUid\":\"new1\"}}");
        BooksClient client = new BooksClient(transport());
        JsonNode resp = client.create("SQUAREBOOK_HC", "테스트");
        assertEquals("new1", resp.path("data").path("bookUid").asText());

        String reqBody = recorded.get(0).body;
        assertTrue(reqBody.contains("\"bookSpecUid\":\"SQUAREBOOK_HC\""));
        assertTrue(reqBody.contains("\"creationType\":\"TEMPLATE\""));
        assertTrue(reqBody.contains("\"title\""));
    }

    @Test
    void create_rejectsMissingBookSpec() {
        BooksClient client = new BooksClient(transport());
        assertThrows(IllegalArgumentException.class, () -> client.create(null, "x"));
        assertThrows(IllegalArgumentException.class, () -> client.create("", "x"));
    }

    @Test
    void create_pdfUploadRequiresPageCount() {
        BooksClient client = new BooksClient(transport());
        // pageCount null → 거부
        IllegalArgumentException e1 = assertThrows(IllegalArgumentException.class,
            () -> client.create("SQUAREBOOK_HC", "t", "PDF_UPLOAD", null, null));
        assertTrue(e1.getMessage().contains("pageCount"));
        // pageCount 0 → 거부
        assertThrows(IllegalArgumentException.class,
            () -> client.create("SQUAREBOOK_HC", "t", "PDF_UPLOAD", null, 0));
        // pageCount -1 → 거부
        assertThrows(IllegalArgumentException.class,
            () -> client.create("SQUAREBOOK_HC", "t", "PDF_UPLOAD", null, -1));
    }

    @Test
    void create_pdfUploadPassesPageCount() {
        respond("/books", 200, "{\"success\":true,\"data\":{\"bookUid\":\"pdf1\"}}");
        BooksClient client = new BooksClient(transport());
        JsonNode resp = client.create("SQUAREBOOK_HC", "p", "PDF_UPLOAD", null, 24);
        assertEquals("pdf1", resp.path("data").path("bookUid").asText());

        String reqBody = recorded.get(0).body;
        assertTrue(reqBody.contains("\"creationType\":\"PDF_UPLOAD\""));
        assertTrue(reqBody.contains("\"pageCount\":24"));
    }

    @Test
    void create_mixCoverTemplateRequiresPageCount() {
        BooksClient client = new BooksClient(transport());
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> client.create("SQUAREBOOK_HC", "t", "MIX_COVER_TEMPLATE", null, null));
        assertTrue(e.getMessage().contains("pageCount"));
    }

    @Test
    void create_mixCoverTemplatePassesPageCount() {
        respond("/books", 200, "{\"success\":true,\"data\":{\"bookUid\":\"mix1\"}}");
        BooksClient client = new BooksClient(transport());
        JsonNode resp = client.create("SQUAREBOOK_HC", "m", "MIX_COVER_TEMPLATE", "ext-1", 12);
        assertEquals("mix1", resp.path("data").path("bookUid").asText());

        String reqBody = recorded.get(0).body;
        assertTrue(reqBody.contains("\"creationType\":\"MIX_COVER_TEMPLATE\""));
        assertTrue(reqBody.contains("\"pageCount\":12"));
        assertTrue(reqBody.contains("\"externalRef\":\"ext-1\""));
    }

    @Test
    void create_templateModeIgnoresPageCountRequirement() {
        // TEMPLATE 모드에서는 pageCount 없어도 통과
        respond("/books", 200, "{\"success\":true,\"data\":{\"bookUid\":\"tpl1\"}}");
        BooksClient client = new BooksClient(transport());
        client.create("SQUAREBOOK_HC", "t", "TEMPLATE", null, null);
        String reqBody = recorded.get(0).body;
        assertTrue(reqBody.contains("\"creationType\":\"TEMPLATE\""));
        assertTrue(!reqBody.contains("\"pageCount\""));
    }

    @Test
    void create_v020CompatOverloadStillWorks() {
        // v0.2.0 시그니처(4-arg) 호환 확인
        respond("/books", 200, "{\"success\":true,\"data\":{\"bookUid\":\"compat1\"}}");
        BooksClient client = new BooksClient(transport());
        client.create("SQUAREBOOK_HC", "t", "TEMPLATE", "ext-2");
        String reqBody = recorded.get(0).body;
        assertTrue(reqBody.contains("\"externalRef\":\"ext-2\""));
        assertTrue(!reqBody.contains("\"pageCount\""));
    }

    @Test
    void get_singleResource() {
        respond("/books/abc", 200, "{"
                + "\"success\":true,"
                + "\"data\":{\"bookUid\":\"abc\",\"pageMeta\":{\"currentPageCount\":12,\"isValid\":true}}"
                + "}");
        BooksClient client = new BooksClient(transport());
        JsonNode resp = client.get("abc");
        assertEquals(12, resp.path("data").path("pageMeta").path("currentPageCount").asInt());
    }

    @Test
    void finalize_postsToFinalizationPath() {
        respond("/books/abc/finalization", 200, "{\"success\":true,\"data\":{\"status\":\"finalized\"}}");
        BooksClient client = new BooksClient(transport());
        JsonNode resp = client.finalizeBook("abc");
        assertEquals("finalized", resp.path("data").path("status").asText());
        assertEquals("POST", recorded.get(0).method);
    }

    @Test
    void delete_callsDelete() {
        respond("/books/abc", 200, "{\"success\":true}");
        BooksClient client = new BooksClient(transport());
        client.delete("abc");
        assertEquals("DELETE", recorded.get(0).method);
    }
}
