package com.sweetbook.bookprintapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResponseParserTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static ResponseParser parse(String json) throws Exception {
        return new ResponseParser(MAPPER.readTree(json));
    }

    @Test
    void flattenedListResponse_v1() throws Exception {
        // v1 평탄화: data: [...] + 최상위 pagination
        ResponseParser p = parse("{"
                + "\"success\":true,"
                + "\"message\":\"OK\","
                + "\"data\":[{\"bookUid\":\"a1\"},{\"bookUid\":\"b2\"}],"
                + "\"pagination\":{\"total\":2,\"limit\":20,\"offset\":0,\"hasMore\":false}"
                + "}");
        assertTrue(p.isSuccess());
        List<JsonNode> raw = p.getList();
        assertEquals(2, raw.size());
        assertEquals("a1", raw.get(0).path("bookUid").asText());
        Pagination pg = p.getPagination();
        assertEquals(2, pg.total());
        assertEquals(20, pg.limit());
        assertFalse(pg.hasMore());
    }

    @Test
    void legacyNestedListResponse() throws Exception {
        // 구버전: data: { books: [...], pagination: {...} }
        ResponseParser p = parse("{"
                + "\"success\":true,"
                + "\"data\":{"
                + "  \"books\":[{\"bookUid\":\"x9\"}],"
                + "  \"pagination\":{\"total\":1,\"limit\":10,\"offset\":0,\"hasMore\":false}"
                + "}}");
        assertEquals(1, p.getList().size());
        Pagination pg = p.getPagination();
        assertEquals(1, pg.total());
        assertEquals(10, pg.limit());
    }

    @Test
    void legacyOrdersList_extractsOrdersKey() throws Exception {
        ResponseParser p = parse("{"
                + "\"success\":true,"
                + "\"data\":{\"orders\":[{\"orderUid\":\"o1\"},{\"orderUid\":\"o2\"},{\"orderUid\":\"o3\"}]}"
                + "}");
        assertEquals(3, p.getList().size());
    }

    @Test
    void sixFieldErrorResponse_camelCase() throws Exception {
        // v1 6필드 실패 응답
        ResponseParser p = parse("{"
                + "\"success\":false,"
                + "\"errorCode\":\"ERR_VALIDATION_FAILED\","
                + "\"message\":\"입력값 검증 실패\","
                + "\"errors\":[\"제목은 필수입니다.\"],"
                + "\"fieldErrors\":["
                + "  {\"field\":\"title\",\"message\":\"필수입니다\",\"constraint\":\"required\"},"
                + "  {\"field\":\"pageCount\",\"message\":\"최소 8\",\"currentValue\":4,\"requiredValue\":8,\"constraint\":\"min\"}"
                + "]}");
        assertFalse(p.isSuccess());
        assertEquals(ErrorCodes.VALIDATION_FAILED, p.getErrorCode());
        assertEquals(1, p.getErrors().size());
        assertEquals(2, p.getFieldErrors().size());

        FieldError titleErr = p.getFieldError("title");
        assertNotNull(titleErr);
        assertEquals(ConstraintTypes.REQUIRED, titleErr.constraint());

        FieldError pagesErr = p.getFieldError("pageCount");
        assertNotNull(pagesErr);
        assertEquals(4, pagesErr.currentValue());
        assertEquals(8, pagesErr.requiredValue());
        assertEquals(ConstraintTypes.MIN, pagesErr.constraint());
    }

    @Test
    void sixFieldErrorResponse_snakeCaseFallback() throws Exception {
        // 일부 환경에서 snake_case로 옴
        ResponseParser p = parse("{"
                + "\"success\":false,"
                + "\"error_code\":\"ERR_INSUFFICIENT_CREDIT\","
                + "\"field_errors\":[{\"field\":\"items\",\"message\":\"부족\",\"current_value\":1000,\"required_value\":5000}]"
                + "}");
        assertEquals(ErrorCodes.INSUFFICIENT_CREDIT, p.getErrorCode());
        FieldError fe = p.getFieldError("items");
        assertNotNull(fe);
        assertEquals(1000, fe.currentValue());
        assertEquals(5000, fe.requiredValue());
    }

    @Test
    void pageMeta_extracted() throws Exception {
        ResponseParser p = parse("{"
                + "\"success\":true,"
                + "\"data\":{\"bookUid\":\"abc\",\"pageMeta\":{"
                + "  \"currentPageCount\":12,\"pageMin\":8,\"pageMax\":60,\"pageIncrement\":4,\"isValid\":true"
                + "}}}");
        PageMeta meta = p.getPageMeta();
        assertEquals(12, meta.currentPageCount());
        assertEquals(8, meta.pageMin());
        assertEquals(60, meta.pageMax());
        assertEquals(4, meta.pageIncrement());
        assertTrue(meta.isValid());
    }

    @Test
    void pageMeta_emptyWhenAbsent() throws Exception {
        ResponseParser p = parse("{\"success\":true,\"data\":{\"bookUid\":\"abc\"}}");
        assertEquals(PageMeta.EMPTY, p.getPageMeta());
    }

    @Test
    void emptyOrNullBody_isSafe() {
        ResponseParser p = new ResponseParser(null);
        assertFalse(p.isSuccess());
        assertEquals(0, p.getList().size());
        assertEquals(Pagination.EMPTY, p.getPagination());
        assertNull(p.getErrorCode());
        assertEquals(0, p.getErrors().size());
        assertEquals(0, p.getFieldErrors().size());
        assertEquals(PageMeta.EMPTY, p.getPageMeta());
    }

    static class BookSummary {
        public String bookUid;
        public String title;
    }

    @Test
    void newEnvelope_hasNextField_v1_6fbf346() throws Exception {
        // commit 6fbf346 (2026-05-11) 이후 서버는 hasNext 사용 (hasMore 아님)
        ResponseParser p = parse("{"
                + "\"success\":true,"
                + "\"data\":[{\"bookUid\":\"a1\"}],"
                + "\"pagination\":{\"total\":1,\"limit\":20,\"offset\":0,\"hasNext\":true}"
                + "}");
        Pagination pg = p.getPagination();
        assertEquals(1, pg.total());
        assertTrue(pg.hasMore(), "hasNext: true 를 hasMore() 로 흡수해야 함");
    }

    @Test
    void legacyPhotos_totalCountAbsorbedAsTotal() throws Exception {
        // 구 photos 응답: data.totalCount → pagination.total
        ResponseParser p = parse("{"
                + "\"success\":true,"
                + "\"data\":{"
                + "  \"photos\":[{\"fileName\":\"a.jpg\"},{\"fileName\":\"b.jpg\"}],"
                + "  \"totalCount\":2"
                + "}}");
        assertEquals(2, p.getList().size());
        assertEquals(2, p.getPagination().total());
    }

    @Test
    void extendedKeysRecognized_accounts() throws Exception {
        ResponseParser p = parse("{\"success\":true,\"data\":{\"accounts\":[{\"id\":1},{\"id\":2}]}}");
        assertEquals(2, p.getList().size());
    }

    @Test
    void extendedKeysRecognized_keys() throws Exception {
        ResponseParser p = parse("{\"success\":true,\"data\":{\"keys\":[{\"keyId\":\"K1\"}]}}");
        assertEquals(1, p.getList().size());
    }

    @Test
    void extendedKeysRecognized_transactions() throws Exception {
        ResponseParser p = parse("{\"success\":true,\"data\":{\"transactions\":[{\"amount\":1000}]}}");
        assertEquals(1, p.getList().size());
    }

    @Test
    void typedListResult_normalizesShape() throws Exception {
        ResponseParser p = parse("{"
                + "\"success\":true,"
                + "\"data\":[{\"bookUid\":\"a1\",\"title\":\"책1\"},{\"bookUid\":\"b2\",\"title\":\"책2\"}],"
                + "\"pagination\":{\"total\":2,\"limit\":20,\"offset\":0,\"hasMore\":false}"
                + "}");
        ListResult<BookSummary> result = p.toListResult(BookSummary.class);
        assertEquals(2, result.items().size());
        assertEquals("a1", result.items().get(0).bookUid);
        assertEquals("책1", result.items().get(0).title);
        assertEquals(2, result.pagination().total());
    }
}
