package com.sweetbook.bookprintapi.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BookSpecsClientTest extends AbstractClientTestBase {

    @Test
    void list_returnsArrayDirectly() {
        respond("/book-specs", 200, "{"
                + "\"success\":true,"
                + "\"data\":[{\"bookSpecUid\":\"SQUAREBOOK_HC\"},{\"bookSpecUid\":\"PHOTOBOOK_A4_SC\"}]"
                + "}");
        BookSpecsClient client = new BookSpecsClient(transport());
        JsonNode resp = client.list();
        assertTrue(resp.isArray());
        assertEquals(2, resp.size());
        assertEquals("SQUAREBOOK_HC", resp.get(0).path("bookSpecUid").asText());
    }

    @Test
    void get_passesAccountUidIfProvided() {
        respond("/book-specs/abc", 200, "{\"success\":true,\"data\":{\"bookSpecUid\":\"abc\"}}");
        BookSpecsClient client = new BookSpecsClient(transport());
        client.get("abc", "acc-1");
        assertTrue(recorded.get(0).query.contains("accountUid=acc-1"));
    }

    @Test
    void get_omitsQueryWhenAccountAbsent() {
        respond("/book-specs/abc", 200, "{\"success\":true,\"data\":{\"bookSpecUid\":\"abc\"}}");
        BookSpecsClient client = new BookSpecsClient(transport());
        client.get("abc");
        // query 없음 (null)
        assertEquals(null, recorded.get(0).query);
    }
}
