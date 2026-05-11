package com.sweetbook.bookprintapi.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.sweetbook.bookprintapi.BookBuildResult;
import com.sweetbook.bookprintapi.BookPrintApiException;
import com.sweetbook.bookprintapi.CreateBookFromTemplateRequest;
import com.sweetbook.bookprintapi.HelperErrorCodes;
import com.sweetbook.bookprintapi.HelperStage;
import com.sweetbook.bookprintapi.PdfOrderBuildResult;
import com.sweetbook.bookprintapi.SweetbookHelperError;
import com.sweetbook.bookprintapi.UploadPdfAndOrderRequest;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 다단계 플로우 헬퍼 — 11_sdk_helpers_design.md v0.1 구현.
 *
 * <p>{@code SweetbookClient.helpers} 로 노출.
 *
 * <p>설계 §4.1 정책:
 * <ul>
 *   <li>자동 재시도 안 함 (트랜스포트 레이어 재시도만 사용)</li>
 *   <li>자동 롤백 안 함 — 실패 시 {@link SweetbookHelperError} 의 {@code bookUid()} / {@code partial()} 노출.
 *       파트너가 {@code client.books.delete(bookUid)} 등으로 명시적 cleanup</li>
 * </ul>
 */
public class HelpersClient {

    private final BooksClient books;
    private final CoversClient covers;
    private final ContentsClient contents;
    private final OrdersClient orders;
    private final PdfsClient pdfs;

    public HelpersClient(
            BooksClient books,
            CoversClient covers,
            ContentsClient contents,
            OrdersClient orders,
            PdfsClient pdfs) {
        this.books = books;
        this.covers = covers;
        this.contents = contents;
        this.orders = orders;
        this.pdfs = pdfs;
    }

    // ====================================================================
    // createBookFromTemplate
    // ====================================================================

    /**
     * TEMPLATE 모드 책 한 권을 한 호출로 생성.
     *
     * <p>내부 호출 순서:
     * <ol>
     *   <li>{@code books.create(spec, title, "TEMPLATE", externalRef)}</li>
     *   <li>{@code covers.create(bookUid, coverTemplateUid, coverParams, coverBindingFiles)}</li>
     *   <li>각 contents 페이지에 대해 {@code contents.insert(bookUid, tplUid, params, bindingFiles, breakBefore)}</li>
     *   <li>{@code books.finalize(bookUid)} (skipFinalize 면 건너뜀)</li>
     * </ol>
     *
     * @param req 빌더 입력
     * @return 빌드 결과
     * @throws SweetbookHelperError 단계별 실패 — stage / bookUid / partial 노출
     */
    public BookBuildResult createBookFromTemplate(CreateBookFromTemplateRequest req) {
        if (req == null) {
            throw new SweetbookHelperError("request 는 필수입니다",
                    HelperStage.VALIDATION, HelperErrorCodes.VALIDATION,
                    null, null, null, null, null);
        }
        if (isEmpty(req.bookSpecUid())) {
            throw new SweetbookHelperError("bookSpecUid 필수",
                    HelperStage.VALIDATION, HelperErrorCodes.VALIDATION,
                    null, null, null, null, null);
        }
        if (isEmpty(req.coverTemplateUid())) {
            throw new SweetbookHelperError("coverTemplateUid 필수",
                    HelperStage.VALIDATION, HelperErrorCodes.VALIDATION,
                    null, null, null, null, null);
        }
        if (req.contents() == null || req.contents().isEmpty()) {
            throw new SweetbookHelperError("contents 는 최소 1개 이상이어야 합니다",
                    HelperStage.VALIDATION, HelperErrorCodes.VALIDATION,
                    null, null, null, null, null);
        }

        Map<String, Object> partial = new LinkedHashMap<>();
        partial.put("bookCreated", false);
        partial.put("coverCreated", false);
        partial.put("contentsInserted", new ArrayList<Map<String, Object>>());
        partial.put("finalized", false);

        // 1. books.create
        JsonNode bookResp;
        try {
            bookResp = books.create(req.bookSpecUid(), req.title(), "TEMPLATE", req.externalRef());
        } catch (BookPrintApiException e) {
            throw new SweetbookHelperError("책 생성 실패",
                    HelperStage.BOOK_CREATE, HelperErrorCodes.BOOK_CREATE_FAILED,
                    null, null, partial, e, null);
        }
        String bookUid = bookResp != null ? bookResp.path("data").path("bookUid").asText(null) : null;
        if (bookUid == null || bookUid.isEmpty()) {
            throw new SweetbookHelperError("books.create 응답에 bookUid 없음",
                    HelperStage.BOOK_CREATE, HelperErrorCodes.BOOK_CREATE_FAILED,
                    null, null, partial, null, null);
        }
        partial.put("bookCreated", true);

        // 2. covers.create
        JsonNode coverResp;
        try {
            coverResp = covers.create(
                    bookUid,
                    req.coverTemplateUid(),
                    req.coverParams(),
                    req.coverBindingFiles());
        } catch (BookPrintApiException e) {
            throw new SweetbookHelperError("표지 생성 실패",
                    HelperStage.COVER_CREATE, HelperErrorCodes.COVER_CREATE_FAILED,
                    bookUid, null, partial, e, null);
        }
        partial.put("coverCreated", true);

        Integer coverPageNum = null;
        if (coverResp != null) {
            JsonNode cd = coverResp.path("data");
            if (cd.has("pageNum") && !cd.path("pageNum").isNull()) {
                coverPageNum = cd.path("pageNum").asInt();
            }
        }

        // 3. contents.insert
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> insertedList = (List<Map<String, Object>>) partial.get("contentsInserted");
        List<BookBuildResult.ContentPageInfo> contentPagesResult = new ArrayList<>();
        List<CreateBookFromTemplateRequest.ContentPage> pages = req.contents();
        for (int idx = 0; idx < pages.size(); idx++) {
            CreateBookFromTemplateRequest.ContentPage page = pages.get(idx);
            if (isEmpty(page.templateUid())) {
                throw new SweetbookHelperError(
                        "contents[" + idx + "].templateUid 누락",
                        HelperStage.CONTENT_INSERT, HelperErrorCodes.CONTENT_INSERT_FAILED,
                        bookUid, null, partial, null, idx);
            }
            JsonNode resp;
            try {
                resp = contents.insert(
                        bookUid,
                        page.templateUid(),
                        page.params(),
                        page.bindingFiles(),
                        page.breakBefore());
            } catch (BookPrintApiException e) {
                throw new SweetbookHelperError(
                        "내지 페이지 #" + idx + " 삽입 실패",
                        HelperStage.CONTENT_INSERT, HelperErrorCodes.CONTENT_INSERT_FAILED,
                        bookUid, null, partial, e, idx);
            }
            Integer pageNum = null;
            String pageSide = null;
            if (resp != null) {
                JsonNode rd = resp.path("data");
                if (rd.has("pageNum") && !rd.path("pageNum").isNull()) pageNum = rd.path("pageNum").asInt();
                if (rd.has("pageSide") && !rd.path("pageSide").isNull()) pageSide = rd.path("pageSide").asText();
            }
            BookBuildResult.ContentPageInfo info = new BookBuildResult.ContentPageInfo(pageNum, pageSide);
            contentPagesResult.add(info);
            Map<String, Object> rec = new LinkedHashMap<>();
            rec.put("pageNum", pageNum);
            rec.put("pageSide", pageSide);
            insertedList.add(rec);
        }

        // 4. books.finalize
        JsonNode finalizeResp = null;
        if (!req.skipFinalize()) {
            try {
                finalizeResp = books.finalizeBook(bookUid);
            } catch (BookPrintApiException e) {
                throw new SweetbookHelperError("책 확정(finalize) 실패",
                        HelperStage.BOOK_FINALIZE, HelperErrorCodes.FINALIZE_FAILED,
                        bookUid, null, partial, e, null);
            }
            partial.put("finalized", true);
        }

        Integer pageCount = null;
        if (finalizeResp != null) {
            JsonNode fd = finalizeResp.path("data");
            JsonNode pm = fd.path("pageMeta");
            if (pm.has("currentPageCount") && !pm.path("currentPageCount").isNull()) {
                pageCount = pm.path("currentPageCount").asInt();
            } else if (fd.has("pageCount") && !fd.path("pageCount").isNull()) {
                pageCount = fd.path("pageCount").asInt();
            }
        }

        return new BookBuildResult(
                bookUid, coverPageNum, contentPagesResult,
                (Boolean) partial.get("finalized"), pageCount,
                bookResp, coverResp, finalizeResp);
    }

    // ====================================================================
    // uploadPdfAndOrder
    // ====================================================================

    /**
     * PDF_UPLOAD 모드 책 + PDF 2종 + finalize + 견적 + 주문까지 한 호출로.
     *
     * @param req 빌더 입력
     * @return 빌드 결과 (bookUid / orderUid / finalized / estimate / order)
     * @throws SweetbookHelperError 단계별 실패. {@code failOnInsufficientCredit=true} 일 때
     *     {@code estimate.creditSufficient=false} 면 ORDER_ESTIMATE 단계에서 던짐
     */
    public PdfOrderBuildResult uploadPdfAndOrder(UploadPdfAndOrderRequest req) {
        if (req == null) {
            throw new SweetbookHelperError("request 필수",
                    HelperStage.VALIDATION, HelperErrorCodes.VALIDATION,
                    null, null, null, null, null);
        }
        if (isEmpty(req.bookSpecUid())) {
            throw new SweetbookHelperError("bookSpecUid 필수",
                    HelperStage.VALIDATION, HelperErrorCodes.VALIDATION,
                    null, null, null, null, null);
        }
        if (req.pageCount() < 1) {
            throw new SweetbookHelperError("pageCount >= 1 필요",
                    HelperStage.VALIDATION, HelperErrorCodes.VALIDATION,
                    null, null, null, null, null);
        }
        if (req.coverPdf() == null || req.contentsPdf() == null) {
            throw new SweetbookHelperError("coverPdf / contentsPdf 둘 다 필수",
                    HelperStage.VALIDATION, HelperErrorCodes.VALIDATION,
                    null, null, null, null, null);
        }
        if (req.shipping() == null
                || !req.shipping().containsKey("recipientName")
                || isEmpty((String) req.shipping().get("recipientName"))) {
            throw new SweetbookHelperError("shipping.recipientName 비어있음",
                    HelperStage.VALIDATION, HelperErrorCodes.VALIDATION,
                    null, null, null, null, null);
        }

        Map<String, Object> partial = new LinkedHashMap<>();
        partial.put("bookCreated", false);
        partial.put("coverPdfUploaded", false);
        partial.put("contentsPdfUploaded", false);
        partial.put("finalized", false);
        partial.put("estimate", null);

        // 1. books.create
        JsonNode bookResp;
        try {
            bookResp = books.create(req.bookSpecUid(), req.title(), "PDF_UPLOAD", req.bookExternalRef());
        } catch (BookPrintApiException e) {
            throw new SweetbookHelperError("책 생성 실패",
                    HelperStage.BOOK_CREATE, HelperErrorCodes.BOOK_CREATE_FAILED,
                    null, null, partial, e, null);
        }
        String bookUid = bookResp != null ? bookResp.path("data").path("bookUid").asText(null) : null;
        if (bookUid == null || bookUid.isEmpty()) {
            throw new SweetbookHelperError("books.create 응답에 bookUid 없음",
                    HelperStage.BOOK_CREATE, HelperErrorCodes.BOOK_CREATE_FAILED,
                    null, null, partial, null, null);
        }
        partial.put("bookCreated", true);

        // 2. pdfs.uploadCover
        JsonNode coverPdfResp;
        try {
            coverPdfResp = pdfs.uploadCover(bookUid, req.coverPdf());
        } catch (BookPrintApiException e) {
            throw new SweetbookHelperError("표지 PDF 업로드 실패",
                    HelperStage.PDF_UPLOAD_COVER, HelperErrorCodes.PDF_UPLOAD_FAILED,
                    bookUid, null, partial, e, null);
        }
        partial.put("coverPdfUploaded", true);

        // 3. pdfs.uploadContents
        JsonNode contentsPdfResp;
        try {
            contentsPdfResp = pdfs.uploadContents(bookUid, req.contentsPdf());
        } catch (BookPrintApiException e) {
            throw new SweetbookHelperError("내지 PDF 업로드 실패",
                    HelperStage.PDF_UPLOAD_CONTENTS, HelperErrorCodes.PDF_UPLOAD_FAILED,
                    bookUid, null, partial, e, null);
        }
        partial.put("contentsPdfUploaded", true);

        // 4. books.finalize
        try {
            books.finalizeBook(bookUid);
        } catch (BookPrintApiException e) {
            throw new SweetbookHelperError("책 확정(finalize) 실패",
                    HelperStage.BOOK_FINALIZE, HelperErrorCodes.FINALIZE_FAILED,
                    bookUid, null, partial, e, null);
        }
        partial.put("finalized", true);

        // 5. orders.estimate
        JsonNode estimate = null;
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("bookUid", bookUid);
        item.put("quantity", req.quantity());
        List<Map<String, Object>> items = new ArrayList<>();
        items.add(item);

        if (!req.skipEstimate()) {
            try {
                estimate = orders.estimate(items);
            } catch (BookPrintApiException e) {
                throw new SweetbookHelperError("견적 조회 실패",
                        HelperStage.ORDER_ESTIMATE, HelperErrorCodes.ORDER_ESTIMATE_FAILED,
                        bookUid, null, partial, e, null);
            }
            partial.put("estimate", estimate);

            if (req.failOnInsufficientCredit() && estimate != null) {
                JsonNode ed = estimate.path("data");
                JsonNode sufficient = ed.path("creditSufficient");
                if (sufficient.isBoolean() && !sufficient.asBoolean()) {
                    long required = ed.path("paidCreditAmount").asLong(0);
                    long balance = ed.path("creditBalance").asLong(0);
                    throw new SweetbookHelperError(
                            "충전금 부족: 필요 " + required + ", 잔액 " + balance,
                            HelperStage.ORDER_ESTIMATE, HelperErrorCodes.CREDIT_INSUFFICIENT,
                            bookUid, null, partial, null, null);
                }
            }
        }

        // 6. orders.create
        JsonNode orderResp;
        try {
            orderResp = orders.create(items, req.shipping(), req.orderExternalRef());
        } catch (BookPrintApiException e) {
            throw new SweetbookHelperError("주문 생성 실패",
                    HelperStage.ORDER_CREATE, HelperErrorCodes.ORDER_CREATE_FAILED,
                    bookUid, null, partial, e, null);
        }

        String orderUid = orderResp != null ? orderResp.path("data").path("orderUid").asText(null) : null;

        return new PdfOrderBuildResult(
                bookUid, orderUid, true,
                coverPdfResp, contentsPdfResp, estimate, orderResp);
    }

    private static boolean isEmpty(String s) { return s == null || s.isEmpty(); }
}
