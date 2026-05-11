package com.sweetbook.bookprintapi.webhook;

import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebhookVerifierTest {

    private static final String SECRET = "whsk_test_secret";

    private static String hmacHex(String secret, String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) sb.append(String.format("%02x", b & 0xFF));
        return sb.toString();
    }

    @Test
    void validSignature_returnsTrue() throws Exception {
        String payload = "{\"event\":\"order.completed\"}";
        long ts = System.currentTimeMillis() / 1000L;
        String sig = "sha256=" + hmacHex(SECRET, ts + "." + payload);
        assertTrue(WebhookVerifier.verify(payload.getBytes(StandardCharsets.UTF_8),
                sig, String.valueOf(ts), SECRET));
    }

    @Test
    void wrongSecret_returnsFalse() throws Exception {
        String payload = "abc";
        long ts = System.currentTimeMillis() / 1000L;
        String sig = "sha256=" + hmacHex("wrong_secret", ts + "." + payload);
        assertFalse(WebhookVerifier.verify(payload.getBytes(StandardCharsets.UTF_8),
                sig, String.valueOf(ts), SECRET));
    }

    @Test
    void tamperedPayload_returnsFalse() throws Exception {
        String payload = "original";
        long ts = System.currentTimeMillis() / 1000L;
        String sig = "sha256=" + hmacHex(SECRET, ts + "." + payload);
        assertFalse(WebhookVerifier.verify("tampered".getBytes(StandardCharsets.UTF_8),
                sig, String.valueOf(ts), SECRET));
    }

    @Test
    void expiredTimestamp_throws() throws Exception {
        String payload = "x";
        long oldTs = (System.currentTimeMillis() / 1000L) - 1000; // 1000초 전
        String sig = "sha256=" + hmacHex(SECRET, oldTs + "." + payload);
        assertThrows(IllegalArgumentException.class,
                () -> WebhookVerifier.verify(payload.getBytes(StandardCharsets.UTF_8),
                        sig, String.valueOf(oldTs), SECRET));
    }

    @Test
    void zeroTolerance_skipsTimestampCheck() throws Exception {
        String payload = "x";
        long ancientTs = 1000L; // 1970년
        String sig = "sha256=" + hmacHex(SECRET, ancientTs + "." + payload);
        // tolerance=0 이면 시간 검증 생략 → 서명만 맞으면 OK
        assertTrue(WebhookVerifier.verify(payload.getBytes(StandardCharsets.UTF_8),
                sig, String.valueOf(ancientTs), SECRET, 0));
    }

    @Test
    void invalidTimestamp_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> WebhookVerifier.verify("x".getBytes(StandardCharsets.UTF_8),
                        "sha256=abc", "not-a-number", SECRET));
    }

    @Test
    void emptySignature_throws() throws Exception {
        long ts = System.currentTimeMillis() / 1000L;
        assertThrows(IllegalArgumentException.class,
                () -> WebhookVerifier.verify("x".getBytes(StandardCharsets.UTF_8),
                        "sha256=", String.valueOf(ts), SECRET));
    }

    @Test
    void prefixOptional() throws Exception {
        // sha256= 접두사 없이 raw hex만 와도 검증 동작
        String payload = "x";
        long ts = System.currentTimeMillis() / 1000L;
        String hex = hmacHex(SECRET, ts + "." + payload);
        assertTrue(WebhookVerifier.verify(payload.getBytes(StandardCharsets.UTF_8),
                hex, String.valueOf(ts), SECRET));
    }

    @Test
    void nullSecretRejected() {
        long ts = System.currentTimeMillis() / 1000L;
        assertThrows(IllegalArgumentException.class,
                () -> WebhookVerifier.verify("x".getBytes(StandardCharsets.UTF_8),
                        "sha256=abc", String.valueOf(ts), null));
    }

    @Test
    void defaultToleranceConstant() {
        assertEquals(300, WebhookVerifier.DEFAULT_TOLERANCE_SECONDS);
    }
}
