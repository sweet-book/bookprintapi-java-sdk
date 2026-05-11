package com.sweetbook.bookprintapi.examples;

import com.fasterxml.jackson.databind.JsonNode;
import com.sweetbook.bookprintapi.PageMeta;
import com.sweetbook.bookprintapi.ResponseParser;
import com.sweetbook.bookprintapi.SweetbookClient;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 책 1권 전체 파이프라인 예제.
 *
 * <p>책 생성 → 표지 → 내지 N장 → finalize → 견적 → 주문.
 *
 * <p>사용법:
 * <pre>
 *   java ... ServerPipeline &lt;cover-template-uid&gt; &lt;content-template-uid&gt; \
 *        &lt;cover-photo.jpg&gt; &lt;content-photo.jpg&gt;
 * </pre>
 */
public class ServerPipeline {

    public static void main(String[] args) {
        if (args.length < 4) {
            System.out.println("사용법: ServerPipeline <coverTemplateUid> <contentTemplateUid> <coverPhoto> <contentPhoto>");
            System.exit(1);
        }
        String coverTemplate = args[0];
        String contentTemplate = args[1];
        Path coverPhoto = Path.of(args[2]);
        Path contentPhoto = Path.of(args[3]);

        SweetbookClient client = SweetbookClient.fromEnv();

        // 1. 책 생성
        System.out.println("[1/6] 책 생성 중...");
        JsonNode book = client.books.create("SQUAREBOOK_HC", "테스트 포토북");
        String bookUid = book.path("data").path("bookUid").asText();
        System.out.println("    bookUid: " + bookUid);

        // 2. 표지 — multipart 파일 part 이름 = 템플릿 binding 이름
        System.out.println("[2/6] 표지 생성 중...");
        Map<String, Object> coverParams = new HashMap<>();
        coverParams.put("title", "테스트 포토북");
        Map<String, Path> coverFiles = new LinkedHashMap<>();
        coverFiles.put("coverPhoto", coverPhoto);  // 'coverPhoto'는 템플릿이 정의한 binding 이름
        client.covers.create(bookUid, coverTemplate, coverParams, coverFiles);

        // 3. 내지 3페이지
        for (int i = 1; i <= 3; i++) {
            System.out.println("[3/6] 내지 " + i + " 추가 중...");
            Map<String, Object> p = new HashMap<>();
            p.put("text", "페이지 " + i + " 내용");
            Map<String, Path> contentFiles = new LinkedHashMap<>();
            contentFiles.put("photo", contentPhoto);  // 'photo'는 템플릿이 정의한 binding 이름
            JsonNode insertResp = client.contents.insert(bookUid, contentTemplate, p,
                    contentFiles, "page");
            PageMeta meta = new ResponseParser(insertResp).getPageMeta();
            System.out.println("    누적 페이지: " + meta.currentPageCount());
        }

        // 4. finalize
        System.out.println("[4/6] 책 확정 중...");
        JsonNode finalizeResp = client.books.finalizeBook(bookUid);
        PageMeta finalMeta = new ResponseParser(finalizeResp).getPageMeta();
        System.out.println("    총 페이지: " + finalMeta.currentPageCount() + " (valid=" + finalMeta.isValid() + ")");

        // 5. 견적
        System.out.println("[5/6] 견적 조회...");
        Map<String, Object> item = new HashMap<>();
        item.put("bookUid", bookUid);
        item.put("quantity", 1);
        JsonNode est = client.orders.estimate(Collections.singletonList(item));
        long total = est.path("data").path("totalAmount").asLong();
        System.out.println("    합계: " + String.format("%,d원", total));

        // 6. 주문 (실 주문은 주석 처리 — 실수로 충전금 차감 방지)
        System.out.println("[6/6] 주문은 주석 처리되어 있습니다. 코드 참고만.");
        System.out.println();
        System.out.println("파이프라인 완료. bookUid: " + bookUid);
    }
}
