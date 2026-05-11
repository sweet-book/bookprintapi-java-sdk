package com.sweetbook.bookprintapi.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.sweetbook.bookprintapi.ListResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIfEnvironmentVariable(named = "BOOKPRINT_API_KEY", matches = ".+")
class ReadOnlyIntegrationTest extends IntegrationTestBase {

    @Test
    void bookSpecs_listReturnsArray() {
        JsonNode resp = client.bookSpecs.list();
        assertTrue(resp.isArray() || resp.isObject(), "bookSpecs.list() — array 또는 객체");
        // BookSpecsClient.list 는 getData()로 raw 반환 — array가 정상
        if (resp.isArray()) {
            assertTrue(resp.size() > 0, "최소 1개 spec 존재");
            JsonNode first = resp.get(0);
            assertNotNull(first.get("bookSpecUid"), "bookSpecUid 필드 존재");
            System.out.println("[bookSpecs.list] count=" + resp.size()
                    + ", first.bookSpecUid=" + first.path("bookSpecUid").asText());
        } else {
            System.out.println("[bookSpecs.list] (객체 형태) " + resp);
        }
    }

    @Test
    void templates_listFlattenedOrLegacy() {
        ListResult<Map<String, Object>> result = client.templates.list();
        // sandbox에 템플릿이 0개일 수도 있으나 응답 자체는 정상이어야 함
        assertNotNull(result.items());
        assertNotNull(result.pagination());
        System.out.println("[templates.list] items=" + result.items().size()
                + ", pagination.total=" + result.pagination().total());

        if (!result.items().isEmpty()) {
            Map<String, Object> t = result.items().get(0);
            assertNotNull(t.get("templateUid"));
        }
    }

    @Test
    void books_listInitialState() {
        ListResult<Map<String, Object>> result = client.books.list();
        assertNotNull(result.items());
        assertNotNull(result.pagination());
        System.out.println("[books.list] items=" + result.items().size()
                + ", pagination.total=" + result.pagination().total());
    }

    @Test
    void orders_listInitialState() {
        ListResult<Map<String, Object>> result = client.orders.list();
        assertNotNull(result.items());
        System.out.println("[orders.list] items=" + result.items().size()
                + ", pagination.total=" + result.pagination().total());
    }
}
