package com.sweetbook.bookprintapi.examples;

import com.sun.net.httpserver.HttpServer;
import com.sweetbook.bookprintapi.webhook.WebhookVerifier;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

/**
 * Webhook 수신 예제 — JDK 내장 HttpServer 사용 (외부 의존성 0).
 *
 * <p>실행:
 * <pre>
 *   WEBHOOK_SECRET=whsk_... java ... WebhookReceiver
 * </pre>
 *
 * <p>로컬 개발 시 ngrok 등으로 외부 노출하여 서버에 webhook URL 등록.
 */
public class WebhookReceiver {

    public static void main(String[] args) throws IOException {
        String secret = System.getenv("WEBHOOK_SECRET");
        if (secret == null || secret.isEmpty()) {
            System.err.println("WEBHOOK_SECRET 환경변수가 필요합니다.");
            System.exit(1);
        }
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/webhook", exchange -> {
            byte[] payload = exchange.getRequestBody().readAllBytes();
            String signature = exchange.getRequestHeaders().getFirst("X-Webhook-Signature");
            String timestamp = exchange.getRequestHeaders().getFirst("X-Webhook-Timestamp");

            try {
                if (!WebhookVerifier.verify(payload, signature, timestamp, secret)) {
                    System.err.println("서명 불일치");
                    exchange.sendResponseHeaders(400, 0);
                    exchange.close();
                    return;
                }
            } catch (IllegalArgumentException e) {
                System.err.println("서명 검증 실패: " + e.getMessage());
                exchange.sendResponseHeaders(400, 0);
                exchange.close();
                return;
            }

            String body = new String(payload, StandardCharsets.UTF_8);
            System.out.println("[webhook] " + body);

            byte[] resp = "{\"received\":true}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, resp.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(resp);
            }
        });
        server.start();
        System.out.println("Webhook 리스너 시작: http://localhost:" + port + "/webhook");
        System.out.println("Ctrl+C 로 종료");
    }
}
