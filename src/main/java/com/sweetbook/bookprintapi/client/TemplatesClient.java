package com.sweetbook.bookprintapi.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.sweetbook.bookprintapi.ListResult;
import com.sweetbook.bookprintapi.ResponseParser;
import com.sweetbook.bookprintapi.http.HttpTransport;

import java.util.LinkedHashMap;
import java.util.Map;

/** 템플릿 목록/상세/스키마 조회. */
public class TemplatesClient {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final HttpTransport http;

    public TemplatesClient(HttpTransport http) {
        this.http = http;
    }

    /**
     * 템플릿 목록 조회. 모든 파라미터 선택.
     *
     * @param query null 가능. 키 예: scope, bookSpecUid, specProfileUid, templateKind, category,
     *              templateName, theme, sort, limit, offset
     * @return 템플릿 목록 + pagination
     */
    public ListResult<Map<String, Object>> list(Map<String, Object> query) {
        Map<String, Object> q = new LinkedHashMap<>();
        if (query != null) q.putAll(query);
        if (!q.containsKey("limit")) q.put("limit", 50);
        if (!q.containsKey("offset")) q.put("offset", 0);
        JsonNode body = http.get("/templates", q);
        return new ResponseParser(body).toListResult(MAP_TYPE);
    }

    public ListResult<Map<String, Object>> list() {
        return list(null);
    }

    public JsonNode get(String templateUid) {
        if (templateUid == null || templateUid.isEmpty()) {
            throw new IllegalArgumentException("templateUid is required");
        }
        return http.get("/templates/" + templateUid, null);
    }

    /**
     * 템플릿 파라미터 스키마 조회 (JSON Schema draft-07).
     *
     * <p>AI 에이전트 / 페이로드 검증 / codegen 용도. 응답 {@code data}는 JSON Schema 문서로,
     * properties 각 항목의 {@code x-binding}으로 binding 종류 식별 (text / file / gallery /
     * collageGallery / rowGallery).
     * @param templateUid 템플릿 UID
     * @return 응답 본문 JSON (JSON Schema 문서)
     */
    public JsonNode getSchema(String templateUid) {
        if (templateUid == null || templateUid.isEmpty()) {
            throw new IllegalArgumentException("templateUid is required");
        }
        return http.get("/templates/" + templateUid + "/schema", null);
    }
}
