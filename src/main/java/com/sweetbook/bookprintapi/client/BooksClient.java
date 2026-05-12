package com.sweetbook.bookprintapi.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.sweetbook.bookprintapi.ListResult;
import com.sweetbook.bookprintapi.PageMeta;
import com.sweetbook.bookprintapi.ResponseParser;
import com.sweetbook.bookprintapi.http.HttpTransport;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 책 생성/조회/확정/삭제. Python {@code BooksClient} / Node {@code BooksClient} 와 동등.
 */
public class BooksClient {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final HttpTransport http;

    public BooksClient(HttpTransport http) {
        this.http = http;
    }

    /**
     * 책 목록 조회.
     *
     * @param status "draft" / "finalized" / null (전체)
     * @param limit  1~100 (기본 20)
     * @param offset 페이지네이션 오프셋
     * @return 책 목록 + pagination
     */
    public ListResult<Map<String, Object>> list(String status, int limit, int offset) {
        Map<String, Object> q = new LinkedHashMap<>();
        if (status != null) q.put("status", status);
        q.put("limit", limit);
        q.put("offset", offset);
        JsonNode body = http.get("/books", q);
        return new ResponseParser(body).toListResult(MAP_TYPE);
    }

    /**
     * 편의 오버로드: 전체 목록 (limit=20, offset=0).
     * @return 책 목록 + pagination
     */
    public ListResult<Map<String, Object>> list() {
        return list(null, 20, 0);
    }

    /**
     * 편의 오버로드: 상태 필터만.
     * @param status "draft" / "finalized" / null
     * @return 책 목록 + pagination
     */
    public ListResult<Map<String, Object>> list(String status) {
        return list(status, 20, 0);
    }

    /**
     * 새 책 생성 (draft 상태).
     *
     * @param bookSpecUid    필수 — 상품 규격 UID (예: "SQUAREBOOK_HC")
     * @param title          제목 (선택)
     * @param creationType   "TEMPLATE" / "PDF_UPLOAD" / "MIX_COVER_TEMPLATE" — 미지정 시 "TEMPLATE"
     * @param externalRef    외부 참조 ID (선택, 최대 100자)
     * @param pageCount      내지 페이지수. {@code creationType} 이
     *                       {@code PDF_UPLOAD} / {@code MIX_COVER_TEMPLATE} 일 때 <b>필수</b>(&gt;0).
     *                       {@code TEMPLATE} 모드에서는 서버가 무시. {@code null} 허용.
     * @return 응답 본문 JSON
     */
    public JsonNode create(String bookSpecUid, String title, String creationType,
                           String externalRef, Integer pageCount) {
        if (bookSpecUid == null || bookSpecUid.isEmpty()) {
            throw new IllegalArgumentException("bookSpecUid is required");
        }
        String effectiveCreationType = creationType != null ? creationType : "TEMPLATE";
        if (("PDF_UPLOAD".equals(effectiveCreationType) || "MIX_COVER_TEMPLATE".equals(effectiveCreationType))
                && (pageCount == null || pageCount <= 0)) {
            throw new IllegalArgumentException(
                "creationType=" + effectiveCreationType + " 는 pageCount(내지 페이지수, >0)가 필수입니다.");
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("bookSpecUid", bookSpecUid);
        body.put("creationType", effectiveCreationType);
        if (title != null) body.put("title", title);
        if (externalRef != null) body.put("externalRef", externalRef);
        if (pageCount != null) body.put("pageCount", pageCount);
        return http.post("/books", body);
    }

    /**
     * 호환 오버로드 (v0.2.0 시그니처) — TEMPLATE 모드 또는 pageCount 불필요 시 사용.
     * @param bookSpecUid 상품 규격 UID
     * @param title 책 제목
     * @param creationType 생성 방식
     * @param externalRef 외부 참조 ID
     * @return 응답 본문 JSON
     */
    public JsonNode create(String bookSpecUid, String title, String creationType, String externalRef) {
        return create(bookSpecUid, title, creationType, externalRef, null);
    }

    /**
     * 편의 — TEMPLATE 모드, externalRef/pageCount 없음.
     * @param bookSpecUid 상품 규격 UID
     * @param title 책 제목
     * @return 응답 본문 JSON
     */
    public JsonNode create(String bookSpecUid, String title) {
        return create(bookSpecUid, title, "TEMPLATE", null, null);
    }

    /**
     * 책 상세 조회 (RESTful 단건). 응답 {@code data}에 {@link PageMeta} 포함.
     * @param bookUid 책 UID
     * @return 응답 본문 JSON
     */
    public JsonNode get(String bookUid) {
        require(bookUid, "bookUid");
        return http.get("/books/" + bookUid, null);
    }

    /**
     * 책 확정 (draft → finalized). 확정 후엔 내용 수정 불가.
     * @param bookUid 책 UID
     * @return 응답 본문 JSON
     */
    public JsonNode finalizeBook(String bookUid) {
        require(bookUid, "bookUid");
        return http.post("/books/" + bookUid + "/finalization", new HashMap<>());
    }

    /**
     * 책 삭제 (draft 상태만).
     * @param bookUid 책 UID
     * @return 응답 본문 JSON
     */
    public JsonNode delete(String bookUid) {
        require(bookUid, "bookUid");
        return http.delete("/books/" + bookUid);
    }

    private static void require(String value, String name) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException(name + " is required");
        }
    }
}
