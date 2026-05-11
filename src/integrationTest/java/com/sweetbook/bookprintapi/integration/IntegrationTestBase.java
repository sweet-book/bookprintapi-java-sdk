package com.sweetbook.bookprintapi.integration;

import com.sweetbook.bookprintapi.SweetbookClient;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/**
 * Sandbox 통합 테스트 베이스.
 *
 * <p>{@code BOOKPRINT_API_KEY} / {@code BOOKPRINT_BASE_URL} 환경변수가 있을 때만 실행.
 * 각 테스트 클래스는 {@link EnabledIfEnvironmentVariable} 로 자체 가드 가능.
 *
 * <p>로컬 실행: {@code ./gradlew integrationTest} (.env 자동 로드)
 * <p>CI 실행: {@code workflow_dispatch} 트리거로만 실행, secret으로 키 주입.
 */
abstract class IntegrationTestBase {

    /** 모든 통합 테스트가 공유하는 클라이언트 인스턴스. */
    protected static final SweetbookClient client = SweetbookClient.fromEnv();

    protected static final boolean SANDBOX = isSandbox();

    private static boolean isSandbox() {
        String url = System.getenv("BOOKPRINT_BASE_URL");
        return url != null && url.contains("sandbox");
    }

    /** 짧은 sleep — sandbox 호출 직후 상태 전파 대기 시 사용. */
    protected static void waitMs(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
