package com.sweetbook.bookprintapi.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreditsClientTest extends AbstractClientTestBase {

    @Test
    void getBalance_returnsRawBody() {
        respond("/credits", 200, "{\"success\":true,\"data\":{\"balance\":50000}}");
        CreditsClient client = new CreditsClient(transport());
        JsonNode resp = client.getBalance();
        assertEquals(50000, resp.path("data").path("balance").asInt());
    }

    @Test
    void transactions_buildsQueryString() {
        respond("/credits/transactions", 200, "{\"success\":true,\"data\":[]}");
        CreditsClient client = new CreditsClient(transport());
        client.transactions(50, 10, "2026-04-01", "2026-04-30");
        String q = recorded.get(0).query;
        assertTrue(q.contains("limit=50"));
        assertTrue(q.contains("offset=10"));
        assertTrue(q.contains("from=2026-04-01"));
        assertTrue(q.contains("to=2026-04-30"));
    }

    @Test
    void sandboxCharge_postsAmountAndMemo() {
        respond("/credits/sandbox/charge", 200, "{\"success\":true,\"data\":{\"newBalance\":150000}}");
        CreditsClient client = new CreditsClient(transport());
        client.sandboxCharge(100000, "SDK 테스트");
        String body = recorded.get(0).body;
        assertTrue(body.contains("\"amount\":100000"));
        assertTrue(body.contains("\"memo\":\"SDK 테스트\""));
    }
}
