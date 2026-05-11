package com.sweetbook.bookprintapi.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.sweetbook.bookprintapi.ListResult;
import com.sweetbook.bookprintapi.OrderStatus;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrdersClientTest extends AbstractClientTestBase {

    @Test
    void list_normalizesFlattenedResponseAndSerializesEnumStatus() {
        respond("/orders", 200, "{"
                + "\"success\":true,"
                + "\"data\":[{\"orderUid\":\"o1\",\"orderStatus\":\"PAID\"}],"
                + "\"pagination\":{\"total\":1,\"limit\":20,\"offset\":0}"
                + "}");
        OrdersClient client = new OrdersClient(transport());
        ListResult<Map<String, Object>> result = client.list(OrderStatus.PAID);
        assertEquals(1, result.items().size());
        assertEquals("PAID", result.items().get(0).get("orderStatus"));
        assertTrue(recorded.get(0).query.contains("status=PAID"));
    }

    @Test
    void list_acceptsStringStatus() {
        respond("/orders", 200, "{\"success\":true,\"data\":[]}");
        OrdersClient client = new OrdersClient(transport());
        client.list("PDF_READY", null, null, null, null);
        assertTrue(recorded.get(0).query.contains("status=PDF_READY"));
    }

    @Test
    void estimate_postsItems() {
        respond("/orders/estimate", 200, "{\"success\":true,\"data\":{\"productAmount\":50000}}");
        OrdersClient client = new OrdersClient(transport());
        Map<String, Object> item = new HashMap<>();
        item.put("bookUid", "abc");
        item.put("quantity", 1);
        JsonNode resp = client.estimate(Arrays.asList(item));
        assertEquals(50000, resp.path("data").path("productAmount").asInt());
        assertTrue(recorded.get(0).body.contains("\"bookUid\":\"abc\""));
    }

    @Test
    void estimate_rejectsEmptyItems() {
        OrdersClient client = new OrdersClient(transport());
        assertThrows(IllegalArgumentException.class, () -> client.estimate(null));
        assertThrows(IllegalArgumentException.class, () -> client.estimate(java.util.Collections.emptyList()));
    }

    @Test
    void create_includesShippingAndExternalRef() {
        respond("/orders", 200, "{\"success\":true,\"data\":{\"orderUid\":\"new1\"}}");
        OrdersClient client = new OrdersClient(transport());
        Map<String, Object> shipping = new HashMap<>();
        shipping.put("recipientName", "홍길동");
        shipping.put("postalCode", "06100");
        shipping.put("address1", "서울 강남구 테헤란로 123");

        Map<String, Object> item = new HashMap<>();
        item.put("bookUid", "abc");
        item.put("quantity", 1);

        JsonNode resp = client.create(Arrays.asList(item), shipping, "EXT-001");
        assertEquals("new1", resp.path("data").path("orderUid").asText());

        String body = recorded.get(0).body;
        assertTrue(body.contains("\"externalRef\":\"EXT-001\""));
        assertTrue(body.contains("\"recipientName\""));
    }

    @Test
    void cancel_postsReason() {
        respond("/orders/o1/cancel", 200, "{\"success\":true,\"data\":{\"refundAmount\":12000}}");
        OrdersClient client = new OrdersClient(transport());
        JsonNode resp = client.cancel("o1", "고객 변심");
        assertEquals(12000, resp.path("data").path("refundAmount").asInt());
        assertTrue(recorded.get(0).body.contains("\"cancelReason\":\"고객 변심\""));
    }

    @Test
    void updateShipping_serializesOnlyProvidedFields() {
        respond("/orders/o1/shipping", 200, "{\"success\":true}");
        OrdersClient client = new OrdersClient(transport());
        client.updateShipping("o1", new OrdersClient.ShippingPatch()
                .recipientName("새이름")
                .postalCode("12345"));
        String body = recorded.get(0).body;
        assertTrue(body.contains("\"recipientName\":\"새이름\""));
        assertTrue(body.contains("\"postalCode\":\"12345\""));
        assertEquals("PATCH", recorded.get(0).method);
        // 미지정 필드는 페이로드에 없어야 함
        assertNotEquals(true, body.contains("address1"));
    }
}
