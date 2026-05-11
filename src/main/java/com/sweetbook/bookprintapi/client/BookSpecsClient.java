package com.sweetbook.bookprintapi.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.sweetbook.bookprintapi.ResponseParser;
import com.sweetbook.bookprintapi.http.HttpTransport;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 상품 스펙 목록/상세 (읽기 전용).
 *
 * <p>주의: 서버 응답이 배열 형태로 오므로 list()는 {@link com.sweetbook.bookprintapi.ListResult}가 아닌
 * {@code data} 배열을 그대로 노출. 평탄화 변경 영향 받지 않음.
 */
public class BookSpecsClient {

    private final HttpTransport http;

    public BookSpecsClient(HttpTransport http) {
        this.http = http;
    }

    /**
     * 상품 스펙 목록. accountUid 미지정 시 공용 스펙.
     * @param accountUid 계정 UID (선택)
     * @return 응답 data 노드
     */
    public JsonNode list(String accountUid) {
        Map<String, Object> q = new LinkedHashMap<>();
        if (accountUid != null) q.put("accountUid", accountUid);
        JsonNode body = http.get("/book-specs", q.isEmpty() ? null : q);
        return new ResponseParser(body).getData();
    }

    public JsonNode list() {
        return list(null);
    }

    public JsonNode get(String bookSpecUid, String accountUid) {
        if (bookSpecUid == null || bookSpecUid.isEmpty()) {
            throw new IllegalArgumentException("bookSpecUid is required");
        }
        Map<String, Object> q = new LinkedHashMap<>();
        if (accountUid != null) q.put("accountUid", accountUid);
        return http.get("/book-specs/" + bookSpecUid, q.isEmpty() ? null : q);
    }

    public JsonNode get(String bookSpecUid) {
        return get(bookSpecUid, null);
    }
}
