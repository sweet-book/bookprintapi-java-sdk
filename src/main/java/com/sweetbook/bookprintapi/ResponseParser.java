package com.sweetbook.bookprintapi;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * API 응답 본문 파싱 유틸리티.
 *
 * <p>{@code master_대비_변경사항.md § 1.2 / § 7.1 / § 8.1} 의 응답 shape 변경 흡수:
 * <ul>
 *   <li>6필드 실패 응답 ({@code errorCode} / {@code fieldErrors} 추가)</li>
 *   <li>리스트 응답 평탄화 ({@code data: [...]} + 최상위 {@code pagination})</li>
 *   <li>{@code data.pageMeta} 통합</li>
 * </ul>
 *
 * <p>모든 메서드가 신/구 응답 shape 양쪽을 흡수합니다.
 */
public class ResponseParser {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final JsonNode body;

    public ResponseParser(JsonNode body) {
        this.body = body;
    }

    public JsonNode raw() {
        return body;
    }

    public boolean isSuccess() {
        if (body == null) return false;
        return body.path("success").asBoolean(false);
    }

    public String getMessage() {
        return body == null ? "" : body.path("message").asText("");
    }

    /**
     * {@code data} 필드. 없으면 body 자체.
     * @return data 노드 또는 body
     */
    public JsonNode getData() {
        if (body == null) return null;
        JsonNode d = body.get("data");
        return d != null ? d : body;
    }

    /**
     * 리스트 응답을 List로 추출. 평탄화 응답 우선, 구버전 {@code data: { orders|items|books|...:[...] }}
     * fallback. 매칭 안 되면 빈 리스트.
     * @return 추출된 노드 리스트
     */
    public List<JsonNode> getList() {
        JsonNode d = getData();
        if (d == null) return Collections.emptyList();
        if (d.isArray()) {
            List<JsonNode> out = new ArrayList<>(d.size());
            d.forEach(out::add);
            return out;
        }
        if (d.isObject()) {
            String[] knownKeys = {
                "orders", "items", "books", "templates", "photos",
                "keys", "accounts", "memos", "configs", "deliveries",
                "notifications", "categories", "transactions", "targetTypes",
                "daily", "referrers", "events", "logs", "bookSpecs"
            };
            for (String key : knownKeys) {
                JsonNode v = d.get(key);
                if (v != null && v.isArray()) {
                    List<JsonNode> out = new ArrayList<>(v.size());
                    v.forEach(out::add);
                    return out;
                }
            }
            // 마지막 fallback — data 객체의 첫 번째 배열 값
            var fields = d.fields();
            while (fields.hasNext()) {
                var entry = fields.next();
                if (entry.getValue() != null && entry.getValue().isArray()) {
                    JsonNode v = entry.getValue();
                    List<JsonNode> out = new ArrayList<>(v.size());
                    v.forEach(out::add);
                    return out;
                }
            }
        }
        return Collections.emptyList();
    }

    /**
     * 리스트 응답을 타입 T로 변환. {@link #getList()} 결과 각 항목을 Jackson으로 디코딩.
     * @param <T> 요소 타입
     * @param type 변환 대상 클래스
     * @return 변환된 리스트
     */
    public <T> List<T> getList(Class<T> type) {
        List<JsonNode> raw = getList();
        List<T> out = new ArrayList<>(raw.size());
        for (JsonNode n : raw) {
            out.add(MAPPER.convertValue(n, type));
        }
        return out;
    }

    /**
     * 타입 T로 변환 (TypeReference 버전, 제네릭 컨테이너용).
     * @param <T> 요소 타입
     * @param elementType 요소 타입 참조
     * @return 변환된 리스트
     */
    public <T> List<T> getList(TypeReference<T> elementType) {
        List<JsonNode> raw = getList();
        List<T> out = new ArrayList<>(raw.size());
        for (JsonNode n : raw) {
            out.add(MAPPER.convertValue(n, elementType));
        }
        return out;
    }

    /**
     * pagination 메타. 평탄화 응답에서 최상위 우선, 구버전은 {@code data.pagination}.
     * @return pagination 메타 (없으면 {@link Pagination#EMPTY})
     */
    public Pagination getPagination() {
        if (body == null) return Pagination.EMPTY;
        JsonNode top = body.get("pagination");
        if (top != null && top.isObject()) return Pagination.from(top);
        JsonNode d = getData();
        if (d != null && d.isObject()) {
            JsonNode inner = d.get("pagination");
            if (inner != null && inner.isObject()) return Pagination.from(inner);
            // 구 photos 응답: data.totalCount → pagination.total
            JsonNode totalCount = d.get("totalCount");
            if (totalCount != null && totalCount.isNumber()) {
                return Pagination.fromTotal(totalCount.asLong());
            }
        }
        return Pagination.EMPTY;
    }

    public String getErrorCode() {
        if (body == null) return null;
        JsonNode v = body.get("errorCode");
        if (v == null) v = body.get("error_code");
        return v == null || v.isNull() ? null : v.asText();
    }

    public List<String> getErrors() {
        List<String> out = new ArrayList<>();
        if (body == null) return out;
        JsonNode arr = body.get("errors");
        if (arr != null && arr.isArray()) {
            for (JsonNode e : arr) {
                if (e != null && !e.isNull()) out.add(e.asText());
            }
        }
        return out;
    }

    public List<FieldError> getFieldErrors() {
        List<FieldError> out = new ArrayList<>();
        if (body == null) return out;
        JsonNode arr = body.get("fieldErrors");
        if (arr == null) arr = body.get("field_errors");
        if (arr != null && arr.isArray()) {
            for (JsonNode fe : arr) {
                FieldError parsed = FieldError.from(fe);
                if (parsed != null) out.add(parsed);
            }
        }
        return out;
    }

    public FieldError getFieldError(String field) {
        for (FieldError fe : getFieldErrors()) {
            if (field.equals(fe.field())) return fe;
        }
        return null;
    }

    /**
     * {@code data.pageMeta} (책 생성·내지·표지·finalize·PDF 응답).
     * @return pageMeta (없으면 {@link PageMeta#EMPTY})
     */
    public PageMeta getPageMeta() {
        JsonNode d = getData();
        if (d == null || !d.isObject()) return PageMeta.EMPTY;
        JsonNode meta = d.get("pageMeta");
        return meta != null && meta.isObject() ? PageMeta.from(meta) : PageMeta.EMPTY;
    }

    /**
     * 리스트 엔드포인트 응답을 {@link ListResult}로 정규화. v1 평탄화 / 구버전 응답 모두 흡수.
     * @param <T> 요소 타입
     * @param type 변환 대상 클래스
     * @return 페이지네이션 포함 결과
     */
    public <T> ListResult<T> toListResult(Class<T> type) {
        return new ListResult<>(getList(type), getPagination());
    }

    /**
     * {@link #toListResult(Class)}의 TypeReference 버전 (제네릭 컨테이너).
     * @param <T> 요소 타입
     * @param elementType 요소 타입 참조
     * @return 페이지네이션 포함 결과
     */
    public <T> ListResult<T> toListResult(TypeReference<T> elementType) {
        return new ListResult<>(getList(elementType), getPagination());
    }
}
