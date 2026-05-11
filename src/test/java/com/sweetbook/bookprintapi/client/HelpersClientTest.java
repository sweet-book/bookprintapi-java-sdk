package com.sweetbook.bookprintapi.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sweetbook.bookprintapi.BookBuildResult;
import com.sweetbook.bookprintapi.BookPrintApiException;
import com.sweetbook.bookprintapi.CreateBookFromTemplateRequest;
import com.sweetbook.bookprintapi.HelperErrorCodes;
import com.sweetbook.bookprintapi.HelperStage;
import com.sweetbook.bookprintapi.PdfOrderBuildResult;
import com.sweetbook.bookprintapi.SweetbookHelperError;
import com.sweetbook.bookprintapi.UploadPdfAndOrderRequest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HelpersClientTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static JsonNode tree(String json) throws Exception {
        return MAPPER.readTree(json);
    }

    private HelpersClient buildHelpers(BooksClient books, CoversClient covers,
                                        ContentsClient contents, OrdersClient orders,
                                        PdfsClient pdfs) {
        return new HelpersClient(books, covers, contents, orders, pdfs);
    }

    // =============================================================================
    // createBookFromTemplate
    // =============================================================================

    @Test
    void createBookFromTemplate_happyPath_finalize() throws Exception {
        BooksClient books = mock(BooksClient.class);
        CoversClient covers = mock(CoversClient.class);
        ContentsClient contents = mock(ContentsClient.class);
        OrdersClient orders = mock(OrdersClient.class);
        PdfsClient pdfs = mock(PdfsClient.class);

        when(books.create(eq("PHOTOBOOK_A4_SC"), any(), eq("TEMPLATE"), any()))
                .thenReturn(tree("{\"data\":{\"bookUid\":\"bk_test\"}}"));
        when(covers.create(eq("bk_test"), any(), any(), ArgumentMatchers.<Map<String, Path>>any()))
                .thenReturn(tree("{\"data\":{\"pageNum\":1}}"));
        when(contents.insert(eq("bk_test"), eq("p1"), any(), any(), any()))
                .thenReturn(tree("{\"data\":{\"pageNum\":2,\"pageSide\":\"left\"}}"));
        when(contents.insert(eq("bk_test"), eq("p2"), any(), any(), eq("page")))
                .thenReturn(tree("{\"data\":{\"pageNum\":3,\"pageSide\":\"right\"}}"));
        when(books.finalizeBook(eq("bk_test")))
                .thenReturn(tree("{\"data\":{\"pageMeta\":{\"currentPageCount\":4}}}"));

        BookBuildResult result = buildHelpers(books, covers, contents, orders, pdfs)
                .createBookFromTemplate(CreateBookFromTemplateRequest.builder()
                        .bookSpecUid("PHOTOBOOK_A4_SC")
                        .coverTemplateUid("cv1")
                        .coverParam("title", "T")
                        .addContent(CreateBookFromTemplateRequest.ContentPage.builder()
                                .templateUid("p1").build())
                        .addContent(CreateBookFromTemplateRequest.ContentPage.builder()
                                .templateUid("p2").breakBefore("page").build())
                        .title("My Book")
                        .build());

        assertEquals("bk_test", result.bookUid());
        assertEquals(1, result.coverPageNum());
        assertEquals(2, result.contentPages().size());
        assertEquals("left", result.contentPages().get(0).pageSide());
        assertTrue(result.finalized());
        assertEquals(4, result.pageCount());
    }

    @Test
    void createBookFromTemplate_skipFinalize() throws Exception {
        BooksClient books = mock(BooksClient.class);
        CoversClient covers = mock(CoversClient.class);
        ContentsClient contents = mock(ContentsClient.class);

        when(books.create(any(), any(), any(), any()))
                .thenReturn(tree("{\"data\":{\"bookUid\":\"bk_skip\"}}"));
        when(covers.create(any(), any(), any(), ArgumentMatchers.<Map<String, Path>>any()))
                .thenReturn(tree("{\"data\":{}}"));
        when(contents.insert(any(), any(), any(), any(), any()))
                .thenReturn(tree("{\"data\":{\"pageNum\":2}}"));

        BookBuildResult r = buildHelpers(books, covers, contents,
                mock(OrdersClient.class), mock(PdfsClient.class))
                .createBookFromTemplate(CreateBookFromTemplateRequest.builder()
                        .bookSpecUid("X")
                        .coverTemplateUid("cv")
                        .addContent(CreateBookFromTemplateRequest.ContentPage.builder()
                                .templateUid("p").build())
                        .skipFinalize(true)
                        .build());

        assertEquals(false, r.finalized());
        verify(books, never()).finalizeBook(any());
    }

    @Test
    void createBookFromTemplate_validation_emptyContents() {
        SweetbookHelperError e = assertThrows(SweetbookHelperError.class,
                () -> buildHelpers(mock(BooksClient.class), mock(CoversClient.class),
                        mock(ContentsClient.class), mock(OrdersClient.class), mock(PdfsClient.class))
                        .createBookFromTemplate(CreateBookFromTemplateRequest.builder()
                                .bookSpecUid("X")
                                .coverTemplateUid("cv")
                                .build()));
        assertEquals(HelperStage.VALIDATION, e.stage());
        assertEquals(HelperErrorCodes.VALIDATION, e.code());
    }

    @Test
    void createBookFromTemplate_validation_missingBookSpec() {
        assertThrows(SweetbookHelperError.class,
                () -> buildHelpers(mock(BooksClient.class), mock(CoversClient.class),
                        mock(ContentsClient.class), mock(OrdersClient.class), mock(PdfsClient.class))
                        .createBookFromTemplate(CreateBookFromTemplateRequest.builder()
                                .coverTemplateUid("cv")
                                .addContent(CreateBookFromTemplateRequest.ContentPage.builder()
                                        .templateUid("p").build())
                                .build()));
    }

    @Test
    void createBookFromTemplate_bookCreateFailure() {
        BooksClient books = mock(BooksClient.class);
        BookPrintApiException apiErr = new BookPrintApiException(
                400, "ERR_VALIDATION_FAILED", "Bad",
                null, null, null);
        when(books.create(any(), any(), any(), any())).thenThrow(apiErr);

        SweetbookHelperError e = assertThrows(SweetbookHelperError.class,
                () -> buildHelpers(books, mock(CoversClient.class), mock(ContentsClient.class),
                        mock(OrdersClient.class), mock(PdfsClient.class))
                        .createBookFromTemplate(CreateBookFromTemplateRequest.builder()
                                .bookSpecUid("X").coverTemplateUid("cv")
                                .addContent(CreateBookFromTemplateRequest.ContentPage.builder()
                                        .templateUid("p").build())
                                .build()));
        assertEquals(HelperStage.BOOK_CREATE, e.stage());
        assertEquals(HelperErrorCodes.BOOK_CREATE_FAILED, e.code());
        assertNull(e.bookUid());
        assertEquals(false, e.partial().get("bookCreated"));
        assertNotNull(e.getCause());
    }

    @Test
    void createBookFromTemplate_coverFailure_keepsBookUid() throws Exception {
        BooksClient books = mock(BooksClient.class);
        CoversClient covers = mock(CoversClient.class);
        when(books.create(any(), any(), any(), any()))
                .thenReturn(tree("{\"data\":{\"bookUid\":\"bk_x\"}}"));
        when(covers.create(any(), any(), any(), ArgumentMatchers.<Map<String, Path>>any()))
                .thenThrow(new BookPrintApiException(400, "ERR_TEMPLATE_BINDING_MISSING",
                        "Bad", null, null, null));

        ContentsClient contents = mock(ContentsClient.class);
        SweetbookHelperError e = assertThrows(SweetbookHelperError.class,
                () -> buildHelpers(books, covers, contents,
                        mock(OrdersClient.class), mock(PdfsClient.class))
                        .createBookFromTemplate(CreateBookFromTemplateRequest.builder()
                                .bookSpecUid("X").coverTemplateUid("cv")
                                .addContent(CreateBookFromTemplateRequest.ContentPage.builder()
                                        .templateUid("p").build())
                                .build()));
        assertEquals(HelperStage.COVER_CREATE, e.stage());
        assertEquals("bk_x", e.bookUid());
        assertEquals(true, e.partial().get("bookCreated"));
        assertEquals(false, e.partial().get("coverCreated"));
        verify(contents, never()).insert(any(), any(), any(), any(), any());
    }

    @Test
    void createBookFromTemplate_contentFailure_includesIndex() throws Exception {
        BooksClient books = mock(BooksClient.class);
        CoversClient covers = mock(CoversClient.class);
        ContentsClient contents = mock(ContentsClient.class);

        when(books.create(any(), any(), any(), any()))
                .thenReturn(tree("{\"data\":{\"bookUid\":\"bk_y\"}}"));
        when(covers.create(any(), any(), any(), ArgumentMatchers.<Map<String, Path>>any()))
                .thenReturn(tree("{\"data\":{}}"));
        when(contents.insert(eq("bk_y"), eq("p1"), any(), any(), any()))
                .thenReturn(tree("{\"data\":{\"pageNum\":2,\"pageSide\":\"left\"}}"));
        when(contents.insert(eq("bk_y"), eq("p2"), any(), any(), any()))
                .thenThrow(new BookPrintApiException(400, null, "Bad", null, null, null));

        SweetbookHelperError e = assertThrows(SweetbookHelperError.class,
                () -> buildHelpers(books, covers, contents,
                        mock(OrdersClient.class), mock(PdfsClient.class))
                        .createBookFromTemplate(CreateBookFromTemplateRequest.builder()
                                .bookSpecUid("X").coverTemplateUid("cv")
                                .addContent(CreateBookFromTemplateRequest.ContentPage.builder()
                                        .templateUid("p1").build())
                                .addContent(CreateBookFromTemplateRequest.ContentPage.builder()
                                        .templateUid("p2").build())
                                .build()));
        assertEquals(HelperStage.CONTENT_INSERT, e.stage());
        assertEquals(Integer.valueOf(1), e.contentIndex());
        assertEquals("bk_y", e.bookUid());
    }

    @Test
    void createBookFromTemplate_finalizeFailure() throws Exception {
        BooksClient books = mock(BooksClient.class);
        CoversClient covers = mock(CoversClient.class);
        ContentsClient contents = mock(ContentsClient.class);

        when(books.create(any(), any(), any(), any()))
                .thenReturn(tree("{\"data\":{\"bookUid\":\"bk_z\"}}"));
        when(covers.create(any(), any(), any(), ArgumentMatchers.<Map<String, Path>>any()))
                .thenReturn(tree("{\"data\":{}}"));
        when(contents.insert(any(), any(), any(), any(), any()))
                .thenReturn(tree("{\"data\":{\"pageNum\":2}}"));
        when(books.finalizeBook(any()))
                .thenThrow(new BookPrintApiException(400, "ERR_FINALIZE_PREREQ_UNMET",
                        "Bad", null, null, null));

        SweetbookHelperError e = assertThrows(SweetbookHelperError.class,
                () -> buildHelpers(books, covers, contents,
                        mock(OrdersClient.class), mock(PdfsClient.class))
                        .createBookFromTemplate(CreateBookFromTemplateRequest.builder()
                                .bookSpecUid("X").coverTemplateUid("cv")
                                .addContent(CreateBookFromTemplateRequest.ContentPage.builder()
                                        .templateUid("p").build())
                                .build()));
        assertEquals(HelperStage.BOOK_FINALIZE, e.stage());
        assertEquals(HelperErrorCodes.FINALIZE_FAILED, e.code());
        assertEquals(false, e.partial().get("finalized"));
        assertEquals(true, e.partial().get("coverCreated"));
    }

    // =============================================================================
    // uploadPdfAndOrder
    // =============================================================================

    private Map<String, Object> shipping() {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("recipientName", "홍길동");
        s.put("recipientPhone", "010-1234-5678");
        s.put("postalCode", "06100");
        s.put("address1", "서울 강남구");
        return s;
    }

    @Test
    void uploadPdfAndOrder_happyPath() throws Exception {
        BooksClient books = mock(BooksClient.class);
        PdfsClient pdfs = mock(PdfsClient.class);
        OrdersClient orders = mock(OrdersClient.class);

        when(books.create(any(), any(), eq("PDF_UPLOAD"), any()))
                .thenReturn(tree("{\"data\":{\"bookUid\":\"bk_pdf\"}}"));
        when(pdfs.uploadCover(any(), any())).thenReturn(tree("{\"data\":{\"size\":100}}"));
        when(pdfs.uploadContents(any(), any())).thenReturn(tree("{\"data\":{\"size\":200}}"));
        when(books.finalizeBook(any())).thenReturn(tree("{\"data\":{}}"));
        when(orders.estimate(any())).thenReturn(tree("{\"data\":{\"creditSufficient\":true}}"));
        when(orders.create(any(), any(), any())).thenReturn(tree("{\"data\":{\"orderUid\":\"or_test\"}}"));

        PdfOrderBuildResult result = buildHelpers(books,
                mock(CoversClient.class), mock(ContentsClient.class), orders, pdfs)
                .uploadPdfAndOrder(UploadPdfAndOrderRequest.builder()
                        .bookSpecUid("PHOTOBOOK_A4_SC")
                        .pageCount(24)
                        .coverPdf(Path.of("c.pdf"))
                        .contentsPdf(Path.of("i.pdf"))
                        .shipping(shipping())
                        .build());

        assertEquals("bk_pdf", result.bookUid());
        assertEquals("or_test", result.orderUid());
        assertTrue(result.finalized());
        assertNotNull(result.estimate());
    }

    @Test
    void uploadPdfAndOrder_insufficientCredit_raises() throws Exception {
        BooksClient books = mock(BooksClient.class);
        PdfsClient pdfs = mock(PdfsClient.class);
        OrdersClient orders = mock(OrdersClient.class);

        when(books.create(any(), any(), any(), any()))
                .thenReturn(tree("{\"data\":{\"bookUid\":\"bk_ic\"}}"));
        when(pdfs.uploadCover(any(), any())).thenReturn(tree("{\"data\":{}}"));
        when(pdfs.uploadContents(any(), any())).thenReturn(tree("{\"data\":{}}"));
        when(books.finalizeBook(any())).thenReturn(tree("{\"data\":{}}"));
        when(orders.estimate(any())).thenReturn(tree(
                "{\"data\":{\"creditSufficient\":false,\"paidCreditAmount\":50000,\"creditBalance\":1000}}"));

        SweetbookHelperError e = assertThrows(SweetbookHelperError.class,
                () -> buildHelpers(books, mock(CoversClient.class), mock(ContentsClient.class),
                        orders, pdfs)
                        .uploadPdfAndOrder(UploadPdfAndOrderRequest.builder()
                                .bookSpecUid("X").pageCount(24)
                                .coverPdf(Path.of("c.pdf")).contentsPdf(Path.of("i.pdf"))
                                .shipping(shipping())
                                .build()));
        assertEquals(HelperStage.ORDER_ESTIMATE, e.stage());
        assertEquals(HelperErrorCodes.CREDIT_INSUFFICIENT, e.code());
        assertEquals("bk_ic", e.bookUid());
        verify(orders, never()).create(any(), any(), any());
    }

    @Test
    void uploadPdfAndOrder_skipEstimate() throws Exception {
        BooksClient books = mock(BooksClient.class);
        PdfsClient pdfs = mock(PdfsClient.class);
        OrdersClient orders = mock(OrdersClient.class);

        when(books.create(any(), any(), any(), any()))
                .thenReturn(tree("{\"data\":{\"bookUid\":\"bk_se\"}}"));
        when(pdfs.uploadCover(any(), any())).thenReturn(tree("{\"data\":{}}"));
        when(pdfs.uploadContents(any(), any())).thenReturn(tree("{\"data\":{}}"));
        when(books.finalizeBook(any())).thenReturn(tree("{\"data\":{}}"));
        when(orders.create(any(), any(), any()))
                .thenReturn(tree("{\"data\":{\"orderUid\":\"or_se\"}}"));

        PdfOrderBuildResult r = buildHelpers(books, mock(CoversClient.class),
                mock(ContentsClient.class), orders, pdfs)
                .uploadPdfAndOrder(UploadPdfAndOrderRequest.builder()
                        .bookSpecUid("X").pageCount(24)
                        .coverPdf(Path.of("c.pdf")).contentsPdf(Path.of("i.pdf"))
                        .shipping(shipping())
                        .skipEstimate(true)
                        .build());
        assertNull(r.estimate());
        verify(orders, never()).estimate(any());
        verify(orders).create(any(), any(), any());
    }

    @Test
    void uploadPdfAndOrder_pdfCoverFailure() throws Exception {
        BooksClient books = mock(BooksClient.class);
        PdfsClient pdfs = mock(PdfsClient.class);

        when(books.create(any(), any(), any(), any()))
                .thenReturn(tree("{\"data\":{\"bookUid\":\"bk_pf\"}}"));
        when(pdfs.uploadCover(any(), any()))
                .thenThrow(new BookPrintApiException(400, "ERR_PDF_FILE_MISSING",
                        "Bad", null, null, null));

        SweetbookHelperError e = assertThrows(SweetbookHelperError.class,
                () -> buildHelpers(books, mock(CoversClient.class), mock(ContentsClient.class),
                        mock(OrdersClient.class), pdfs)
                        .uploadPdfAndOrder(UploadPdfAndOrderRequest.builder()
                                .bookSpecUid("X").pageCount(24)
                                .coverPdf(Path.of("c.pdf")).contentsPdf(Path.of("i.pdf"))
                                .shipping(shipping())
                                .build()));
        assertEquals(HelperStage.PDF_UPLOAD_COVER, e.stage());
        assertEquals(HelperErrorCodes.PDF_UPLOAD_FAILED, e.code());
        assertEquals(true, e.partial().get("bookCreated"));
        assertEquals(false, e.partial().get("coverPdfUploaded"));
        verify(pdfs, never()).uploadContents(any(), any());
    }

    @Test
    void uploadPdfAndOrder_validation_missingRecipient() {
        Map<String, Object> badShipping = new LinkedHashMap<>();
        badShipping.put("address1", "서울");

        SweetbookHelperError e = assertThrows(SweetbookHelperError.class,
                () -> buildHelpers(mock(BooksClient.class), mock(CoversClient.class),
                        mock(ContentsClient.class), mock(OrdersClient.class), mock(PdfsClient.class))
                        .uploadPdfAndOrder(UploadPdfAndOrderRequest.builder()
                                .bookSpecUid("X").pageCount(24)
                                .coverPdf(Path.of("c.pdf")).contentsPdf(Path.of("i.pdf"))
                                .shipping(badShipping)
                                .build()));
        assertEquals(HelperStage.VALIDATION, e.stage());
    }

    // =============================================================================
    // SweetbookHelperError 표시 / userMessage
    // =============================================================================

    @Test
    void helperError_toString_includesStage() {
        SweetbookHelperError e = new SweetbookHelperError(
                "fail", HelperStage.CONTENT_INSERT, HelperErrorCodes.CONTENT_INSERT_FAILED,
                "bk_xyz", null, null, null, 3);
        String s = e.toString();
        assertTrue(s.contains("CONTENT_INSERT#3"));
        assertTrue(s.contains("bk_xyz"));
    }

    @Test
    void helperError_userMessage_delegatesToApiError() {
        java.util.List<String> errors = java.util.Arrays.asList("사용자 메시지");
        BookPrintApiException api = new BookPrintApiException(400, "X", "back", errors, null, null);
        SweetbookHelperError e = new SweetbookHelperError(
                "fail", HelperStage.BOOK_CREATE, HelperErrorCodes.BOOK_CREATE_FAILED,
                null, null, null, api, null);
        assertEquals("사용자 메시지", e.userMessage());
    }

    @Test
    void helperError_userMessage_fallback() {
        SweetbookHelperError e = new SweetbookHelperError(
                "fallback", HelperStage.VALIDATION, HelperErrorCodes.VALIDATION,
                null, null, null, null, null);
        assertEquals("fallback", e.userMessage());
    }
}
