package com.sweetbook.bookprintapi.examples;

import com.fasterxml.jackson.databind.JsonNode;
import com.sweetbook.bookprintapi.BookBuildResult;
import com.sweetbook.bookprintapi.BookPrintApiException;
import com.sweetbook.bookprintapi.CreateBookFromTemplateRequest;
import com.sweetbook.bookprintapi.ErrorCodes;
import com.sweetbook.bookprintapi.HelperErrorCodes;
import com.sweetbook.bookprintapi.HelperStage;
import com.sweetbook.bookprintapi.PdfOrderBuildResult;
import com.sweetbook.bookprintapi.SweetbookClient;
import com.sweetbook.bookprintapi.SweetbookHelperError;
import com.sweetbook.bookprintapi.UploadPdfAndOrderRequest;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * SDK 헬퍼 (v0.1.0+) 사용 예제 — createBookFromTemplate / uploadPdfAndOrder.
 *
 * <p>다단계 플로우(books → cover → contents → finalize)를 한 호출로 처리.
 *
 * <p>사용법:
 * <pre>
 *   ./gradlew runHelpersExample --args="template &lt;coverTplUid&gt; &lt;contentTplUid&gt; &lt;photoPath&gt;"
 *   ./gradlew runHelpersExample --args="pdf &lt;coverPdfPath&gt; &lt;contentsPdfPath&gt;"
 * </pre>
 */
public class HelpersExample {

    public static void main(String[] args) {
        if (args.length == 0 || "-h".equals(args[0]) || "--help".equals(args[0])) {
            printUsage();
            return;
        }

        SweetbookClient client = SweetbookClient.fromEnv();
        String cmd = args[0];

        try {
            switch (cmd) {
                case "template":
                    cmdTemplate(client, args);
                    break;
                case "pdf":
                    cmdPdf(client, args);
                    break;
                default:
                    System.err.println("알 수 없는 명령: " + cmd);
                    printUsage();
                    System.exit(1);
            }
        } catch (SweetbookHelperError e) {
            handleHelperError(client, e);
            System.exit(1);
        }
    }

    private static void cmdTemplate(SweetbookClient client, String[] args) {
        if (args.length < 4) {
            System.out.println("사용법: HelpersExample template <coverTplUid> <contentTplUid> <photoPath>");
            return;
        }
        String coverTpl = args[1];
        String contentTpl = args[2];
        Path photo = Path.of(args[3]);
        if (!Files.exists(photo)) {
            System.err.println("파일 없음: " + photo);
            System.exit(1);
        }

        BookBuildResult result = client.helpers.createBookFromTemplate(
                CreateBookFromTemplateRequest.builder()
                        .bookSpecUid("PHOTOBOOK_A4_SC")
                        .coverTemplateUid(coverTpl)
                        .coverParam("title", "Helpers 데모 책")
                        .coverBindingFile("coverPhoto", photo)
                        .addContent(CreateBookFromTemplateRequest.ContentPage.builder()
                                .templateUid(contentTpl)
                                .param("text", "헬퍼 페이지 1")
                                .bindingFile("mainPhoto", photo)
                                .build())
                        .addContent(CreateBookFromTemplateRequest.ContentPage.builder()
                                .templateUid(contentTpl)
                                .param("text", "헬퍼 페이지 2")
                                .bindingFile("mainPhoto", photo)
                                .breakBefore("page")
                                .build())
                        .title("Helpers 데모 책")
                        .build());

        System.out.println("=".repeat(50));
        System.out.println("  책 생성 완료");
        System.out.println("=".repeat(50));
        System.out.println("  bookUid     : " + result.bookUid());
        System.out.println("  cover page  : " + result.coverPageNum());
        System.out.println("  내지 페이지 : " + result.contentPages().size());
        for (int i = 0; i < result.contentPages().size(); i++) {
            BookBuildResult.ContentPageInfo p = result.contentPages().get(i);
            System.out.println("    [" + i + "] pageNum=" + p.pageNum() + ", side=" + p.pageSide());
        }
        System.out.println("  finalized   : " + result.finalized());
        System.out.println("  pageCount   : " + result.pageCount());
    }

    private static void cmdPdf(SweetbookClient client, String[] args) {
        if (args.length < 3) {
            System.out.println("사용법: HelpersExample pdf <coverPdfPath> <contentsPdfPath>");
            return;
        }
        Path coverPdf = Path.of(args[1]);
        Path contentsPdf = Path.of(args[2]);
        for (Path p : new Path[] {coverPdf, contentsPdf}) {
            if (!Files.exists(p)) {
                System.err.println("파일 없음: " + p);
                System.exit(1);
            }
        }

        Map<String, Object> shipping = new LinkedHashMap<>();
        shipping.put("recipientName", "홍길동");
        shipping.put("recipientPhone", "010-1234-5678");
        shipping.put("postalCode", "06100");
        shipping.put("address1", "서울특별시 강남구 테헤란로 123");
        shipping.put("address2", "4층");

        PdfOrderBuildResult result = client.helpers.uploadPdfAndOrder(
                UploadPdfAndOrderRequest.builder()
                        .bookSpecUid("PHOTOBOOK_A4_SC")
                        .pageCount(24)
                        .coverPdf(coverPdf)
                        .contentsPdf(contentsPdf)
                        .shipping(shipping)
                        .quantity(1)
                        .orderExternalRef("HELPERS-DEMO-001")
                        .failOnInsufficientCredit(true)
                        .build());

        System.out.println("=".repeat(50));
        System.out.println("  PDF 업로드 + 주문 완료");
        System.out.println("=".repeat(50));
        System.out.println("  bookUid  : " + result.bookUid());
        System.out.println("  orderUid : " + result.orderUid());
        System.out.println("  finalized: " + result.finalized());
        if (result.estimate() != null) {
            JsonNode ed = result.estimate().path("data");
            long paid = ed.path("paidCreditAmount").asLong(0);
            System.out.println("  결제금액 : " + String.format("%,d원", paid));
        }
    }

    private static void handleHelperError(SweetbookClient client, SweetbookHelperError e) {
        System.out.println("=".repeat(50));
        System.out.println("  헬퍼 실패 — stage=" + e.stage());
        System.out.println("=".repeat(50));
        System.out.println("  code      : " + e.code());
        System.out.println("  bookUid   : " + e.bookUid());
        if (e.contentIndex() != null) {
            System.out.println("  contentIdx: " + e.contentIndex());
        }
        System.out.println("  message   : " + e.userMessage());
        System.out.println("  partial   : " + e.partial());
        Throwable cause = e.getCause();
        if (cause != null) {
            System.out.println("  cause     : " + cause.getMessage());
            if (cause instanceof BookPrintApiException) {
                System.out.println("  cause.code: " + ((BookPrintApiException) cause).errorCode());
            }
        }

        // stage 기반 분기 — 11_sdk_helpers_design.md § 4.1 (자동 롤백 안 함) 정책
        if (e.stage() == HelperStage.VALIDATION) {
            System.out.println("\n  → 클라이언트측 검증 실패. 입력 값 확인 후 재시도.");

        } else if (e.stage() == HelperStage.CONTENT_INSERT) {
            System.out.println("\n  → 페이지 #" + e.contentIndex() + " 삽입 실패. 책(" + e.bookUid() + ") 유지됨.");
            System.out.println("     사용자에게 해당 페이지 재입력 후 contents.insert 직접 호출 권장.");

        } else if (e.stage() == HelperStage.BOOK_FINALIZE) {
            String causeCode = (cause instanceof BookPrintApiException)
                    ? ((BookPrintApiException) cause).errorCode() : null;
            if (ErrorCodes.INSUFFICIENT_PAGES.equals(causeCode)
                    || ErrorCodes.FINALIZE_PREREQ_UNMET.equals(causeCode)) {
                System.out.println("\n  → finalize 전제조건 미달. 책(" + e.bookUid() + ") 유지. 페이지 추가 후 재시도.");
            } else {
                System.out.println("\n  → finalize 실패. 책(" + e.bookUid() + ") 점검 후 재시도 또는 삭제.");
            }

        } else if (e.stage() == HelperStage.ORDER_ESTIMATE) {
            if (HelperErrorCodes.CREDIT_INSUFFICIENT.equals(e.code())) {
                System.out.println("\n  → 충전금 부족. 주문 차단됨. 책(" + e.bookUid() + ")은 finalize 까지 완료.");
                System.out.println("     충전 후 직접 orders.create 호출 가능 (책 재생성 불필요).");
            } else {
                System.out.println("\n  → 견적 조회 실패. 책(" + e.bookUid() + ") 유지.");
            }

        } else if (e.stage() == HelperStage.PDF_UPLOAD_COVER
                || e.stage() == HelperStage.PDF_UPLOAD_CONTENTS) {
            System.out.println("\n  → PDF 업로드 실패. 파일 규격(456×303mm 등) 확인 후 재시도.");

        } else {
            // BOOK_CREATE / COVER_CREATE / ORDER_CREATE — 책 폐기 권장
            if (e.bookUid() != null) {
                System.out.println("\n  → 책 폐기 (books.delete(" + e.bookUid() + "))...");
                try {
                    client.books.delete(e.bookUid());
                    System.out.println("     OK — 정리 완료.");
                } catch (Exception cleanupErr) {
                    System.out.println("     cleanup 실패 (수동 처리 필요): " + cleanupErr.getMessage());
                }
            }
        }
    }

    private static void printUsage() {
        System.out.println("Commands:");
        System.out.println("  template <coverTplUid> <contentTplUid> <photoPath>");
        System.out.println("  pdf <coverPdfPath> <contentsPdfPath>");
    }
}
