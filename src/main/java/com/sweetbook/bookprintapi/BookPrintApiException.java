package com.sweetbook.bookprintapi;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * BookPrintAPI HTTP 4xx / 5xx 응답을 나타내는 예외.
 *
 * <p>v1 표준 6필드 응답 ({@code success}, {@code errorCode}, {@code message}, {@code data},
 * {@code errors[]}, {@code fieldErrors[]}) 을 흡수하며, 구버전 4필드 응답도 호환합니다.
 *
 * <p>분기 패턴:
 * <pre>{@code
 * try {
 *     client.orders.create(...);
 * } catch (BookPrintApiException e) {
 *     if (ErrorCodes.INSUFFICIENT_CREDIT.equals(e.errorCode())) {
 *         long required = ((Number) e.data().getOrDefault("required", 0)).longValue();
 *         long balance  = ((Number) e.data().getOrDefault("balance", 0)).longValue();
 *         ...
 *     } else if (ErrorCodes.VALIDATION_FAILED.equals(e.errorCode())) {
 *         for (FieldError fe : e.fieldErrors()) { ... }
 *     } else {
 *         showToast(e.userMessage());
 *     }
 * }
 * }</pre>
 */
public class BookPrintApiException extends RuntimeException {

    private final int statusCode;
    private final String errorCode;
    private final String responseMessage;
    private final List<String> errors;
    private final List<FieldError> fieldErrors;
    private final Map<String, Object> data;

    public BookPrintApiException(int statusCode, String errorCode, String message,
                                 List<String> errors, List<FieldError> fieldErrors,
                                 Map<String, Object> data) {
        super(message != null ? message : "BookPrintAPI error " + statusCode);
        this.statusCode = statusCode;
        this.errorCode = errorCode;
        this.responseMessage = message;
        this.errors = errors != null ? Collections.unmodifiableList(errors) : Collections.emptyList();
        this.fieldErrors = fieldErrors != null ? Collections.unmodifiableList(fieldErrors) : Collections.emptyList();
        this.data = data != null ? Collections.unmodifiableMap(data) : Collections.emptyMap();
    }

    public int statusCode() { return statusCode; }
    public String errorCode() { return errorCode; }
    public String responseMessage() { return responseMessage; }
    public List<String> errors() { return errors; }
    public List<FieldError> fieldErrors() { return fieldErrors; }
    public Map<String, Object> data() { return data; }

    /**
     * 사용자 표시용 메시지 — {@code errors[0]} 우선, 없으면 {@code message}.
     * @return 사용자 표시용 메시지
     */
    public String userMessage() {
        if (!errors.isEmpty() && errors.get(0) != null) return errors.get(0);
        return responseMessage != null ? responseMessage : getMessage();
    }

    /**
     * 특정 필드의 FieldError 조회 (없으면 null).
     * @param fieldName 조회할 필드명
     * @return 매칭된 {@link FieldError} 또는 {@code null}
     */
    public FieldError fieldError(String fieldName) {
        for (FieldError fe : fieldErrors) {
            if (fieldName.equals(fe.field())) return fe;
        }
        return null;
    }

    /**
     * HTTP 상태 + JSON body로부터 예외 생성. body가 null이거나 JSON 파싱 실패면
     * statusCode + generic message 만으로 구성.
     * @param statusCode HTTP 상태 코드
     * @param body 응답 본문 JSON (null 가능)
     * @return 파싱된 예외 인스턴스
     */
    public static BookPrintApiException fromResponse(int statusCode, JsonNode body) {
        if (body == null || !body.isObject()) {
            return new BookPrintApiException(statusCode, null,
                    "HTTP " + statusCode, null, null, null);
        }

        // errorCode: camelCase 우선, snake_case fallback
        String errorCode = textOrNull(body, "errorCode");
        if (errorCode == null) errorCode = textOrNull(body, "error_code");

        String message = textOrNull(body, "message");

        // errors[]
        List<String> errors = new ArrayList<>();
        JsonNode errorsNode = body.get("errors");
        if (errorsNode != null && errorsNode.isArray()) {
            for (JsonNode e : errorsNode) {
                if (e != null && !e.isNull()) errors.add(e.asText());
            }
        }

        // fieldErrors[] (camelCase 우선, snake_case fallback)
        List<FieldError> fieldErrors = new ArrayList<>();
        JsonNode fieldErrorsNode = body.get("fieldErrors");
        if (fieldErrorsNode == null) fieldErrorsNode = body.get("field_errors");
        if (fieldErrorsNode != null && fieldErrorsNode.isArray()) {
            for (JsonNode fe : fieldErrorsNode) {
                FieldError parsed = FieldError.from(fe);
                if (parsed != null) fieldErrors.add(parsed);
            }
        }

        // data 객체 (errorCode별 진단 정보 — 예: INSUFFICIENT_CREDIT의 required/balance)
        Map<String, Object> data = new HashMap<>();
        JsonNode dataNode = body.get("data");
        if (dataNode != null && dataNode.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> it = dataNode.fields();
            while (it.hasNext()) {
                Map.Entry<String, JsonNode> entry = it.next();
                data.put(entry.getKey(), unwrap(entry.getValue()));
            }
        }

        return new BookPrintApiException(statusCode, errorCode, message, errors, fieldErrors, data);
    }

    private static String textOrNull(JsonNode node, String key) {
        JsonNode v = node.get(key);
        return v == null || v.isNull() ? null : v.asText();
    }

    private static Object unwrap(JsonNode v) {
        if (v == null || v.isNull()) return null;
        if (v.isInt()) return v.asInt();
        if (v.isLong()) return v.asLong();
        if (v.isDouble() || v.isFloat()) return v.asDouble();
        if (v.isBoolean()) return v.asBoolean();
        if (v.isTextual()) return v.asText();
        // 객체/배열은 JsonNode로 보존 — 호출자가 직접 navigate
        return v;
    }
}
