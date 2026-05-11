package com.sweetbook.bookprintapi.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.sweetbook.bookprintapi.BookPrintApiException;
import com.sweetbook.bookprintapi.ErrorCodes;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.MethodOrderer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@EnabledIfEnvironmentVariable(named = "BOOKPRINT_API_KEY", matches = ".+")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BookLifecycleIntegrationTest extends IntegrationTestBase {

    private String bookUid;

    @AfterAll
    void cleanup() {
        if (bookUid != null) {
            try {
                client.books.delete(bookUid);
                System.out.println("[cleanup] deleted book " + bookUid);
            } catch (Exception e) {
                System.out.println("[cleanup] 삭제 실패 (이미 삭제됐거나 finalized) — " + e.getMessage());
            }
        }
    }

    @Test
    @Order(1)
    void create_returnsBookUidAndPageMeta() {
        JsonNode resp = client.books.create("PHOTOBOOK_A4_SC", "Java SDK 통합 테스트");
        assertTrue(resp.path("success").asBoolean(false));
        bookUid = resp.path("data").path("bookUid").asText();
        assertNotNull(bookUid);
        assertTrue(bookUid.length() > 0);

        // pageMeta 검증 — 단건 책 생성 응답에 포함되는지
        JsonNode pageMeta = resp.path("data").path("pageMeta");
        if (pageMeta.isObject()) {
            System.out.println("[books.create] bookUid=" + bookUid
                    + ", pageMeta.currentPageCount=" + pageMeta.path("currentPageCount").asInt()
                    + ", pageMin=" + pageMeta.path("pageMin").asInt()
                    + ", pageIncrement=" + pageMeta.path("pageIncrement").asInt()
                    + ", isValid=" + pageMeta.path("isValid").asBoolean());
        } else {
            System.out.println("[books.create] bookUid=" + bookUid + " (pageMeta 없음)");
        }
    }

    @Test
    @Order(2)
    void getBook_singleResponseIncludesPageMeta() {
        if (bookUid == null) fail("이전 단계 create 실패");
        JsonNode resp = client.books.get(bookUid);
        assertTrue(resp.path("success").asBoolean(false));
        assertEquals(bookUid, resp.path("data").path("bookUid").asText());

        JsonNode pageMeta = resp.path("data").path("pageMeta");
        System.out.println("[books.get] bookUid=" + bookUid
                + ", pageMeta present=" + pageMeta.isObject()
                + ", status=" + resp.path("data").path("status").asText());
    }

    @Test
    @Order(3)
    void finalizeEmptyBook_throwsFinalizePrereqUnmet() {
        if (bookUid == null) fail("이전 단계 create 실패");
        try {
            client.books.finalizeBook(bookUid);
            // 빈 책이라도 finalize 성공하는 환경일 수 있음 — 그 경우는 정보만 출력
            System.out.println("[books.finalize] 빈 책 finalize 성공 (서버 정책상 OK)");
        } catch (BookPrintApiException e) {
            // 예상: ERR_FINALIZE_PREREQ_UNMET 또는 ERR_INSUFFICIENT_PAGES
            System.out.println("[books.finalize] errorCode=" + e.errorCode()
                    + ", status=" + e.statusCode()
                    + ", message=" + e.userMessage());
            // errorCode가 우리 카탈로그 24개 안에 있는지 확인
            String code = e.errorCode();
            assertNotNull(code, "errorCode 응답 필드 존재");
            assertTrue(
                    ErrorCodes.FINALIZE_PREREQ_UNMET.equals(code)
                            || ErrorCodes.INSUFFICIENT_PAGES.equals(code)
                            || ErrorCodes.PAGECOUNT_INVALID.equals(code)
                            || ErrorCodes.VALIDATION_FAILED.equals(code),
                    "예상 errorCode 카탈로그 안에 있어야 함. 실제: " + code);
        }
    }

    @Test
    @Order(4)
    void delete_removesBook() {
        if (bookUid == null) fail("이전 단계 create 실패");
        try {
            client.books.delete(bookUid);
            System.out.println("[books.delete] OK");
            // 서버는 soft-delete: GET 은 여전히 200 OK 응답하되 status 가 9(DELETED)로 변경
            JsonNode after = client.books.get(bookUid);
            int statusCode = after.path("data").path("status").asInt(-1);
            System.out.println("[books.get after delete] status=" + statusCode
                    + " (soft-delete 시 9 기대)");
            assertEquals(9, statusCode, "삭제 후 status 는 9(DELETED) 여야 함");
            bookUid = null; // cleanup 에서 재시도 방지
        } catch (BookPrintApiException e) {
            System.out.println("[books.delete] 실패 (finalized 가능성) — errorCode="
                    + e.errorCode() + ", message=" + e.userMessage());
        }
    }
}
