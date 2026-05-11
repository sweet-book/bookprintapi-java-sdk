package com.sweetbook.bookprintapi;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Objects;

/**
 * 필드 단위 에러 정보. {@code fieldErrors[]} 배열의 한 항목.
 *
 * <p>Python {@code FieldError} dataclass / Node {@code FieldError} 클래스와 동등.
 *
 * <p>5필드: {@code field}, {@code message}, {@code currentValue}, {@code requiredValue},
 * {@code constraint} ({@link ConstraintTypes} 중 하나).
 */
public final class FieldError {

    private final String field;
    private final String message;
    private final Object currentValue;
    private final Object requiredValue;
    private final String constraint;

    public FieldError(String field, String message, Object currentValue, Object requiredValue, String constraint) {
        this.field = field;
        this.message = message;
        this.currentValue = currentValue;
        this.requiredValue = requiredValue;
        this.constraint = constraint;
    }

    public String field() { return field; }
    public String message() { return message; }
    public Object currentValue() { return currentValue; }
    public Object requiredValue() { return requiredValue; }
    public String constraint() { return constraint; }

    /**
     * JSON 노드(객체)에서 FieldError 추출. snake_case / camelCase 모두 흡수.
     * @param node JSON 객체 노드
     * @return 파싱된 {@link FieldError} 또는 노드가 객체가 아니면 {@code null}
     */
    public static FieldError from(JsonNode node) {
        if (node == null || !node.isObject()) return null;
        return new FieldError(
                textOrNull(node, "field"),
                textOrNull(node, "message"),
                valueOrNull(node, "currentValue", "current_value"),
                valueOrNull(node, "requiredValue", "required_value"),
                textOrNull(node, "constraint")
        );
    }

    private static String textOrNull(JsonNode node, String key) {
        JsonNode v = node.get(key);
        return v == null || v.isNull() ? null : v.asText();
    }

    private static Object valueOrNull(JsonNode node, String camel, String snake) {
        JsonNode v = node.get(camel);
        if (v == null) v = node.get(snake);
        if (v == null || v.isNull()) return null;
        if (v.isInt()) return v.asInt();
        if (v.isLong()) return v.asLong();
        if (v.isDouble() || v.isFloat()) return v.asDouble();
        if (v.isBoolean()) return v.asBoolean();
        return v.asText();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FieldError)) return false;
        FieldError that = (FieldError) o;
        return Objects.equals(field, that.field)
                && Objects.equals(message, that.message)
                && Objects.equals(currentValue, that.currentValue)
                && Objects.equals(requiredValue, that.requiredValue)
                && Objects.equals(constraint, that.constraint);
    }

    @Override
    public int hashCode() {
        return Objects.hash(field, message, currentValue, requiredValue, constraint);
    }

    @Override
    public String toString() {
        return "FieldError{field=" + field + ", message=" + message
                + ", currentValue=" + currentValue + ", requiredValue=" + requiredValue
                + ", constraint=" + constraint + "}";
    }
}
