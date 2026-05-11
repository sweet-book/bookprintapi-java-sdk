package com.sweetbook.bookprintapi;

import java.util.Collections;
import java.util.Map;

/**
 * SDK 헬퍼 다단계 호출 중 실패 — 11_sdk_helpers_design.md § 3.
 *
 * <p>{@link BookPrintApiException} 와 별개로 헬퍼 전용 예외. {@code stage} / {@code bookUid} /
 * {@code partial} 노출하여 파트너가 명시적 cleanup 결정 가능.
 *
 * <p>분기 패턴:
 * <pre>{@code
 * try {
 *     BookBuildResult r = client.helpers.createBookFromTemplate(...);
 * } catch (SweetbookHelperError e) {
 *     if (e.stage() == HelperStage.CONTENT_INSERT && e.bookUid() != null) {
 *         // 책은 남기고 사용자에게 재입력
 *     } else if (e.bookUid() != null) {
 *         client.books.delete(e.bookUid());
 *     }
 *     throw e;
 * }
 * }</pre>
 */
public class SweetbookHelperError extends RuntimeException {

    private final HelperStage stage;
    private final String code;
    private final String bookUid;
    private final String orderUid;
    private final Map<String, Object> partial;
    private final Throwable cause;
    private final Integer contentIndex;

    public SweetbookHelperError(
            String message,
            HelperStage stage,
            String code,
            String bookUid,
            String orderUid,
            Map<String, Object> partial,
            Throwable cause,
            Integer contentIndex) {
        super(message, cause);
        this.stage = stage;
        this.code = code;
        this.bookUid = bookUid;
        this.orderUid = orderUid;
        this.partial = partial != null ? Collections.unmodifiableMap(partial) : Collections.emptyMap();
        this.cause = cause;
        this.contentIndex = contentIndex;
    }

    public HelperStage stage() { return stage; }
    public String code() { return code; }
    public String bookUid() { return bookUid; }
    public String orderUid() { return orderUid; }
    public Map<String, Object> partial() { return partial; }
    @Override public Throwable getCause() { return cause; }
    public Integer contentIndex() { return contentIndex; }

    /**
     * 사용자 표시용 메시지. cause 가 {@link BookPrintApiException} 이면 그쪽으로 위임.
     * @return 사용자 메시지 문자열
     */
    public String userMessage() {
        if (cause instanceof BookPrintApiException) {
            return ((BookPrintApiException) cause).userMessage();
        }
        return getMessage();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[").append(stage);
        if (contentIndex != null) sb.append("#").append(contentIndex);
        sb.append("] ").append(getMessage());
        if (code != null) sb.append(" (").append(code).append(")");
        if (bookUid != null) sb.append(" bookUid=").append(bookUid);
        return sb.toString();
    }
}
