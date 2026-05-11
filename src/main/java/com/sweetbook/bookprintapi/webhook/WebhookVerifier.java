package com.sweetbook.bookprintapi.webhook;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * BookPrintAPI 웹훅 서명 검증.
 *
 * <p>서버가 보내는 헤더:
 * <pre>
 * X-Webhook-Signature: sha256=abc123...
 * X-Webhook-Timestamp: 1710000000
 * </pre>
 *
 * <p>서명 알고리즘: {@code HMAC-SHA256(secret, timestamp + "." + payload)}
 *
 * <p>Python {@code verify_signature} / Node {@code verifySignature} 와 동등.
 */
public final class WebhookVerifier {

    /** 기본 허용 오차 (5분). */
    public static final int DEFAULT_TOLERANCE_SECONDS = 300;

    private WebhookVerifier() {}

    /**
     * 서명 검증 (기본 5분 허용 오차).
     *
     * @param payload 원본 요청 본문
     * @param signatureHeader {@code X-Webhook-Signature} 헤더
     * @param timestampHeader {@code X-Webhook-Timestamp} 헤더
     * @param secret 공유 시크릿
     * @return 서명 유효 시 true
     * @throws IllegalArgumentException 서명 형식 또는 타임스탬프가 만료/형식 오류
     */
    public static boolean verify(byte[] payload, String signatureHeader,
                                  String timestampHeader, String secret) {
        return verify(payload, signatureHeader, timestampHeader, secret, DEFAULT_TOLERANCE_SECONDS);
    }

    /**
     * @param payload 원본 요청 본문
     * @param signatureHeader {@code X-Webhook-Signature} 헤더
     * @param timestampHeader {@code X-Webhook-Timestamp} 헤더
     * @param secret 공유 시크릿
     * @param toleranceSeconds 0이면 시간 검증 생략
     * @return 서명 유효 시 true
     */
    public static boolean verify(byte[] payload, String signatureHeader,
                                  String timestampHeader, String secret,
                                  int toleranceSeconds) {
        if (secret == null || secret.isEmpty()) {
            throw new IllegalArgumentException("secret is required");
        }
        if (signatureHeader == null) {
            throw new IllegalArgumentException("signatureHeader is required");
        }
        if (timestampHeader == null) {
            throw new IllegalArgumentException("timestampHeader is required");
        }

        // "sha256=..." 접두사 제거
        String sigHash = signatureHeader.startsWith("sha256=")
                ? signatureHeader.substring(7)
                : signatureHeader;

        if (sigHash.isEmpty()) {
            throw new IllegalArgumentException("Invalid signature: empty");
        }

        // 타임스탬프 검증
        if (toleranceSeconds > 0) {
            long ts;
            try {
                ts = Long.parseLong(timestampHeader.trim());
            } catch (NumberFormatException nfe) {
                throw new IllegalArgumentException("Invalid timestamp: " + timestampHeader, nfe);
            }
            long now = currentEpochSeconds();
            if (Math.abs(now - ts) > toleranceSeconds) {
                throw new IllegalArgumentException(
                        "Timestamp expired. Tolerance: " + toleranceSeconds + "s");
            }
        }

        byte[] payloadBytes = payload == null ? new byte[0] : payload;
        byte[] signed = concat((timestampHeader + ".").getBytes(StandardCharsets.UTF_8), payloadBytes);
        String expected = hmacSha256Hex(secret.getBytes(StandardCharsets.UTF_8), signed);

        return constantTimeEquals(expected, sigHash);
    }

    /** 시간 의존부를 분리해 테스트에서 override 가능. 기본 구현은 system time. */
    static long currentEpochSeconds() {
        return System.currentTimeMillis() / 1000L;
    }

    private static byte[] concat(byte[] a, byte[] b) {
        byte[] out = new byte[a.length + b.length];
        System.arraycopy(a, 0, out, 0, a.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }

    private static String hmacSha256Hex(byte[] key, byte[] data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            byte[] hash = mac.doFinal(data);
            return toHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("HMAC-SHA256 계산 실패: " + e.getMessage(), e);
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b & 0xFF));
        }
        return sb.toString();
    }

    /** 타이밍 공격 방어용 상수 시간 비교. */
    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) return false;
        return MessageDigest.isEqual(
                a.getBytes(StandardCharsets.UTF_8),
                b.getBytes(StandardCharsets.UTF_8)
        );
    }
}
