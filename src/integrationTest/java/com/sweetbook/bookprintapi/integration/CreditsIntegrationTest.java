package com.sweetbook.bookprintapi.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIfEnvironmentVariable(named = "BOOKPRINT_API_KEY", matches = ".+")
class CreditsIntegrationTest extends IntegrationTestBase {

    @Test
    void getBalance_returnsValidShape() {
        JsonNode resp = client.credits.getBalance();
        // v1 응답 표준 검증: success + data 구조
        assertTrue(resp.path("success").asBoolean(false), "응답에 success=true");
        JsonNode data = resp.path("data");
        assertTrue(data.isObject(), "data는 객체");
        // balance 필드 존재 (음수 가능성도 있으니 값 검증은 하지 않음)
        assertNotNull(data.get("balance"), "data.balance 필드 존재");

        System.out.println("[credits.getBalance] balance=" + data.path("balance").asLong()
                + ", env-data: " + data.toString());
    }

    @Test
    void sandboxCharge_increasesBalance() {
        if (!SANDBOX) {
            // live 환경에서는 실행 금지
            return;
        }
        long before = client.credits.getBalance().path("data").path("balance").asLong();

        JsonNode chargeResp = client.credits.sandboxCharge(1000, "Java SDK 통합 테스트");
        assertTrue(chargeResp.path("success").asBoolean(false));

        // 잔액 변화 확인 (직접 +1000 검증 대신 증가 여부만)
        long after = client.credits.getBalance().path("data").path("balance").asLong();
        assertTrue(after >= before + 1000,
                "sandboxCharge 후 잔액이 1000 이상 증가해야 함. before=" + before + ", after=" + after);

        System.out.println("[credits.sandboxCharge] before=" + before + " → after=" + after);
    }
}
