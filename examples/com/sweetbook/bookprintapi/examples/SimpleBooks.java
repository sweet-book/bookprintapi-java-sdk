package com.sweetbook.bookprintapi.examples;

import com.fasterxml.jackson.databind.JsonNode;
import com.sweetbook.bookprintapi.BookPrintApiException;
import com.sweetbook.bookprintapi.ListResult;
import com.sweetbook.bookprintapi.SweetbookClient;

import java.util.Map;

/**
 * 책 목록/생성/상세/확정/삭제 CLI 예제.
 *
 * <p>실행:
 * <pre>
 *   ./gradlew :compileExamplesJava
 *   java -cp build/classes/java/main:build/classes/java/examples \
 *        com.sweetbook.bookprintapi.examples.SimpleBooks list
 * </pre>
 *
 * <p>환경변수:
 * <ul>
 *   <li>{@code BOOKPRINT_API_KEY} — 필수</li>
 *   <li>{@code BOOKPRINT_ENV} — "sandbox" / "live" (기본 "live")</li>
 *   <li>{@code BOOKPRINT_BASE_URL} — 직접 URL 지정 (선택)</li>
 * </ul>
 *
 * <p>⚠️ 백엔드/CLI 실행 전제. SDK를 브라우저/프론트엔드에 번들하지 마세요.
 */
public class SimpleBooks {

    public static void main(String[] args) {
        if (args.length == 0 || "-h".equals(args[0]) || "--help".equals(args[0])) {
            printUsage();
            return;
        }

        SweetbookClient client = SweetbookClient.fromEnv();
        String cmd = args[0];

        try {
            switch (cmd) {
                case "list":     cmdList(client, args);     break;
                case "create":   cmdCreate(client, args);   break;
                case "get":      cmdGet(client, args);      break;
                case "finalize": cmdFinalize(client, args); break;
                case "delete":   cmdDelete(client, args);   break;
                default:
                    System.err.println("알 수 없는 명령: " + cmd);
                    printUsage();
                    System.exit(1);
            }
        } catch (BookPrintApiException e) {
            System.err.println("API 오류: " + e.userMessage());
            if (e.errorCode() != null) System.err.println("  errorCode: " + e.errorCode());
            for (com.sweetbook.bookprintapi.FieldError fe : e.fieldErrors()) {
                System.err.println("  - " + fe.field() + ": " + fe.message());
            }
            System.exit(1);
        }
    }

    private static void cmdList(SweetbookClient client, String[] args) {
        String status = null;
        for (int i = 1; i < args.length - 1; i++) {
            if ("--status".equals(args[i])) status = args[i + 1];
        }
        ListResult<Map<String, Object>> result = client.books.list(status);
        if (result.items().isEmpty()) {
            System.out.println("책이 없습니다.");
            return;
        }
        System.out.printf("%-20s %-12s %-6s %s%n", "UID", "상태", "페이지", "제목");
        System.out.println("-".repeat(70));
        for (Map<String, Object> b : result.items()) {
            System.out.printf("%-20s %-12s %-6s %s%n",
                    b.getOrDefault("bookUid", ""),
                    b.getOrDefault("status", ""),
                    b.getOrDefault("pageCount", 0),
                    b.getOrDefault("title", "(제목 없음)"));
        }
        System.out.println();
        System.out.println("총 " + result.pagination().total() + "권");
    }

    private static void cmdCreate(SweetbookClient client, String[] args) {
        if (args.length < 2) {
            System.out.println("사용법: SimpleBooks create <제목> [--spec SQUAREBOOK_HC]");
            return;
        }
        String title = args[1];
        String spec = "SQUAREBOOK_HC";
        for (int i = 2; i < args.length - 1; i++) {
            if ("--spec".equals(args[i])) spec = args[i + 1];
        }
        JsonNode resp = client.books.create(spec, title);
        String bookUid = resp.path("data").path("bookUid").asText();
        System.out.println("책 생성 완료: " + bookUid);
        System.out.println(resp.path("data").toPrettyString());
    }

    private static void cmdGet(SweetbookClient client, String[] args) {
        if (args.length < 2) {
            System.out.println("사용법: SimpleBooks get <bookUid>");
            return;
        }
        JsonNode resp = client.books.get(args[1]);
        System.out.println(resp.path("data").toPrettyString());
    }

    private static void cmdFinalize(SweetbookClient client, String[] args) {
        if (args.length < 2) {
            System.out.println("사용법: SimpleBooks finalize <bookUid>");
            return;
        }
        JsonNode resp = client.books.finalizeBook(args[1]);
        System.out.println(resp.path("data").toPrettyString());
        System.out.println("책 확정 완료!");
    }

    private static void cmdDelete(SweetbookClient client, String[] args) {
        if (args.length < 2) {
            System.out.println("사용법: SimpleBooks delete <bookUid>");
            return;
        }
        client.books.delete(args[1]);
        System.out.println("삭제 완료: " + args[1]);
    }

    private static void printUsage() {
        System.out.println("Commands:");
        System.out.println("  list [--status finalized]");
        System.out.println("  create <제목> [--spec SQUAREBOOK_HC]");
        System.out.println("  get <bookUid>");
        System.out.println("  finalize <bookUid>");
        System.out.println("  delete <bookUid>");
    }
}
