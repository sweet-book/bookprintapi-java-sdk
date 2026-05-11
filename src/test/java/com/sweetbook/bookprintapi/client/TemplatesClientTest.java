package com.sweetbook.bookprintapi.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.sweetbook.bookprintapi.ListResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TemplatesClientTest extends AbstractClientTestBase {

    @Test
    void list_normalizesAndAcceptsArbitraryFilters() {
        respond("/templates", 200, "{"
                + "\"success\":true,"
                + "\"data\":[{\"templateUid\":\"t1\"}],"
                + "\"pagination\":{\"total\":1,\"limit\":50,\"offset\":0}"
                + "}");
        TemplatesClient client = new TemplatesClient(transport());
        Map<String, Object> q = new HashMap<>();
        q.put("scope", "user");
        q.put("templateKind", "cover");
        ListResult<Map<String, Object>> result = client.list(q);
        assertEquals(1, result.items().size());

        String query = recorded.get(0).query;
        assertTrue(query.contains("scope=user"));
        assertTrue(query.contains("templateKind=cover"));
        assertTrue(query.contains("limit=50"));  // 기본값 채워짐
    }

    @Test
    void getSchema_pathIncludesSchemaSuffix() {
        respond("/templates/abc/schema", 200, "{"
                + "\"success\":true,"
                + "\"data\":{\"$schema\":\"http://json-schema.org/draft-07/schema#\",\"type\":\"object\"}"
                + "}");
        TemplatesClient client = new TemplatesClient(transport());
        JsonNode resp = client.getSchema("abc");
        assertEquals("object", resp.path("data").path("type").asText());
    }

    @Test
    void getSchema_rejectsEmpty() {
        TemplatesClient client = new TemplatesClient(transport());
        assertThrows(IllegalArgumentException.class, () -> client.getSchema(null));
        assertThrows(IllegalArgumentException.class, () -> client.getSchema(""));
    }
}
