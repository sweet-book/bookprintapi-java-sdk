package com.sweetbook.bookprintapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BookPrintApiExceptionTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void parsesSixFieldErrorResponse() throws Exception {
        BookPrintApiException e = BookPrintApiException.fromResponse(400, MAPPER.readTree("{"
                + "\"success\":false,"
                + "\"errorCode\":\"ERR_VALIDATION_FAILED\","
                + "\"message\":\"검증 실패\","
                + "\"errors\":[\"제목 필수\"],"
                + "\"fieldErrors\":[{\"field\":\"title\",\"message\":\"필수\",\"constraint\":\"required\"}]"
                + "}"));
        assertEquals(400, e.statusCode());
        assertEquals(ErrorCodes.VALIDATION_FAILED, e.errorCode());
        assertEquals("검증 실패", e.responseMessage());
        assertEquals(1, e.errors().size());
        assertEquals("제목 필수", e.errors().get(0));
        assertEquals(1, e.fieldErrors().size());
        FieldError fe = e.fieldError("title");
        assertNotNull(fe);
        assertEquals(ConstraintTypes.REQUIRED, fe.constraint());
    }

    @Test
    void userMessage_prefersErrorsFirst() throws Exception {
        BookPrintApiException e = BookPrintApiException.fromResponse(400, MAPPER.readTree("{"
                + "\"errorCode\":\"ERR_VALIDATION_FAILED\","
                + "\"message\":\"백엔드 메시지\","
                + "\"errors\":[\"사용자용 한글 메시지\"]"
                + "}"));
        assertEquals("사용자용 한글 메시지", e.userMessage());
    }

    @Test
    void userMessage_fallsBackToMessage() throws Exception {
        BookPrintApiException e = BookPrintApiException.fromResponse(401, MAPPER.readTree("{"
                + "\"errorCode\":\"ERR_UNAUTHORIZED\","
                + "\"message\":\"API 키가 잘못되었습니다.\""
                + "}"));
        assertEquals("API 키가 잘못되었습니다.", e.userMessage());
    }

    @Test
    void insufficientCredit_dataExposed() throws Exception {
        BookPrintApiException e = BookPrintApiException.fromResponse(402, MAPPER.readTree("{"
                + "\"errorCode\":\"ERR_INSUFFICIENT_CREDIT\","
                + "\"message\":\"충전금 부족\","
                + "\"data\":{\"required\":12000,\"balance\":5000}"
                + "}"));
        assertEquals(ErrorCodes.INSUFFICIENT_CREDIT, e.errorCode());
        assertEquals(12000, e.data().get("required"));
        assertEquals(5000, e.data().get("balance"));
    }

    @Test
    void unknownStatus_unparsableBody_fallback() {
        BookPrintApiException e = BookPrintApiException.fromResponse(500, null);
        assertEquals(500, e.statusCode());
        assertNull(e.errorCode());
        assertTrue(e.errors().isEmpty());
        assertTrue(e.fieldErrors().isEmpty());
    }
}
