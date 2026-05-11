package com.sweetbook.bookprintapi;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertSame;

class SweetbookClientTest {

    @Test
    void builder_apiKeyExplicitly_buildsClient() {
        SweetbookClient client = SweetbookClient.builder()
                .apiKey("SBtestkey")
                .environment("sandbox")
                .build();
        assertNotNull(client);
        // sub-client 9개 모두 노출 확인
        assertNotNull(client.books);
        assertNotNull(client.orders);
        assertNotNull(client.credits);
        assertNotNull(client.templates);
        assertNotNull(client.bookSpecs);
        assertNotNull(client.photos);
        assertNotNull(client.covers);
        assertNotNull(client.contents);
        assertNotNull(client.pdfs);
    }

    @Test
    void builder_baseUrlOverridesEnvironment() {
        SweetbookClient client = SweetbookClient.builder()
                .apiKey("SBtest")
                .environment("sandbox")
                .baseUrl("https://example.test/api/v1")
                .build();
        assertNotNull(client);
        // transport()는 protected — 같은 패키지에서 접근 가능
        assertEquals("https://example.test/api/v1", client.transport().baseUrl());
    }

    @Test
    void builder_environmentLive_resolvesLiveDefaultUrl() {
        SweetbookClient client = SweetbookClient.builder()
                .apiKey("SBtest")
                .environment("live")
                .build();
        assertEquals("https://api.sweetbook.com/v1", client.transport().baseUrl());
    }

    @Test
    void builder_environmentSandbox_resolvesSandboxDefaultUrl() {
        SweetbookClient client = SweetbookClient.builder()
                .apiKey("SBtest")
                .environment("sandbox")
                .build();
        assertEquals("https://api-sandbox.sweetbook.com/v1", client.transport().baseUrl());
    }

    @Test
    void builder_missingApiKey_throws() {
        // 환경변수 설정 여부에 따라 다를 수 있어 skip이 더 안전한 케이스지만
        // BOOKPRINT_API_KEY 가 없는 환경에서만 검증
        if (System.getenv("BOOKPRINT_API_KEY") != null) return;
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> SweetbookClient.builder().build());
        assertEquals(true, ex.getMessage().contains("API Key"));
    }

    @Test
    void builder_chaining_returnsSameInstance() {
        SweetbookClient.Builder b = SweetbookClient.builder();
        assertSame(b, b.apiKey("SBtest"));
        assertSame(b, b.environment("sandbox"));
        assertSame(b, b.baseUrl("https://x.test"));
        assertSame(b, b.timeout(Duration.ofSeconds(10)));
        assertSame(b, b.maxRetries(2));
    }

    @Test
    void builder_customTimeoutAndRetries_accepted() {
        SweetbookClient client = SweetbookClient.builder()
                .apiKey("SBtest")
                .baseUrl("https://x.test")
                .timeout(Duration.ofSeconds(5))
                .maxRetries(0)
                .build();
        assertNotNull(client);
    }
}
