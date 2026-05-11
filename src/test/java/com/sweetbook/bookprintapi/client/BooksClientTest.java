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
