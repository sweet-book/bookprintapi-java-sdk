package com.sweetbook.bookprintapi.examples;

import com.fasterxml.jackson.databind.JsonNode;
import com.sweetbook.bookprintapi.BookPrintApiException;
import com.sweetbook.bookprintapi.ErrorCodes;
import com.sweetbook.bookprintapi.ListResult;
import com.sweetbook.bookprintapi.OrderStatus;
import com.sweetbook.bookprintapi.SweetbookClient;
import com.sweetbook.bookprintapi.client.OrdersClient;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 주문 견적/생성/조회/취소 CLI 예제.
 *
 * <p>errorCode 분기 패턴 (특히 INSUFFICIENT_CREDIT) 시연 포함.
 */
public class SimpleOrders {

    public static void main(String[] args) {
        if (args.length == 0 || "-h".equals(args[0]) || "--help".equals(args[0])) {
            printUsage();
            return;
        }

        SweetbookClient client = SweetbookClient.fromEnv();
        String cmd = args[0];

        try {
            switch (cmd) {
                case "estimate": cmdEstimate(client, args); break;
                case "create":   cmdCreate(client, args);   break;
                case "list":     cmdList(client, args);     break;
                case "get":      cmdGet(client, args);      break;
                case "cancel":   cmdCancel(client, args);   break;
                case "shipping": cmdShipping(client, args); break;
                default:
                    System.err.println("알 수 없는 명령: " + cmd);
                    printUsage();
                    System.exit(1);
            }
        } catch (BookPrintApiException e) {
            handleError(e);
            System.exit(1);
        }
    }

    private static void handleError(BookPrintApiException e) {
        if (ErrorCodes.INSUFFICIENT_CREDIT.equals(e.errorCode())) {
            Object required = e.data().getOrDefault("required", 0);
            Object balance = e.data().getOrDefault("balance", 0);
            System.err.println("충전금 부족: 필요 " + required + "원, 현재 " + balance + "원");
            return;
        }
        if (ErrorCodes.VALIDATION_FAILED.equals(e.errorCode())) {
            System.err.println("입력값 오류:");
            for (com.sweetbook.bookprintapi.FieldError fe : e.fieldErrors()) {
                System.err.println("  - " + fe.field() + ": " + fe.message());
            }
            return;
        }
        System.err.println("API 오류 [" + e.errorCode() + "]: " + e.userMessage());
    }

    private static void cmdEstimate(SweetbookClient client, String[] args) {
        if (args.length < 2) {
            System.out.println("사용법: SimpleOrders estimate <bookUid> [quantity]");
            return;
        }
        String bookUid = args[1];
        int quantity = args.length > 2 ? Integer.parseInt(args[2]) : 1;

        Map<String, Object> item = new HashMap<>();
        item.put("bookUid", bookUid);
        item.put("quantity", quantity);

        JsonNode resp = client.orders.estimate(Collections.singletonList(item));
        JsonNode data = resp.path("data");

        System.out.println("=".repeat(50));
        System.out.println("  견적 결과");
        System.out.println("=".repeat(50));
        System.out.println("  상품 금액: " + fmt(data.path("productAmount").asLong()));
        System.out.println("  배송비:    " + fmt(data.path("shippingFee").asLong()));
        System.out.println("  합계:      " + fmt(data.path("totalAmount").asLong()));
        System.out.println("  결제금액:  " + fmt(data.path("paidCreditAmount").asLong()));
        System.out.println("  현재 잔액: " + fmt(data.path("creditBalance").asLong()));
        if (!data.path("creditSufficient").asBoolean(true)) {
            System.out.println("  ⚠ 잔액 부족");
        }
    }

    private static void cmdCreate(SweetbookClient client, String[] args) {
        if (args.length < 6) {
            System.out.println("사용법: SimpleOrders create <bookUid> <수령인> <전화번호> <우편번호> <주소>");
            return;
        }
        Map<String, Object> item = new HashMap<>();
        item.put("bookUid", args[1]);
        item.put("quantity", 1);

        Map<String, Object> shipping = new HashMap<>();
        shipping.put("recipientName", args[2]);
        shipping.put("recipientPhone", args[3]);
        shipping.put("postalCode", args[4]);
        shipping.put("address1", args[5]);

        JsonNode resp = client.orders.create(Collections.singletonList(item), shipping, null);
        JsonNode data = resp.path("data");
        System.out.println("주문 생성 완료!");
        System.out.println("  주문번호: " + data.path("orderUid").asText());
        System.out.println("  결제금액: " + fmt(data.path("paidCreditAmount").asLong()));
    }

    private static void cmdList(SweetbookClient client, String[] args) {
        Object status = null;
        for (int i = 1; i < args.length - 1; i++) {
            if ("--status".equals(args[i])) {
                String raw = args[i + 1];
                // OrderStatus 문자열 우선, 매칭 안 되면 그대로 전달 (숫자 코드 호환)
                status = OrderStatus.tryParse(raw).map(Object.class::cast).orElse(raw);
            }
        }
        ListResult<Map<String, Object>> result = client.orders.list(status, null, null, null, null);
        if (result.items().isEmpty()) {
            System.out.println("주문이 없습니다.");
            return;
        }
        System.out.printf("%-18s %-20s %-12s %s%n", "주문번호", "상태", "결제금액", "수령인");
        System.out.println("-".repeat(80));
        for (Map<String, Object> o : result.items()) {
            System.out.printf("%-18s %-20s %-12s %s%n",
                    o.getOrDefault("orderUid", ""),
                    o.getOrDefault("orderStatus", ""),
                    fmt(((Number) o.getOrDefault("paidCreditAmount", 0)).longValue()),
                    o.getOrDefault("recipientName", ""));
        }
        System.out.println("\n총 " + result.pagination().total() + "건");
    }

    private static void cmdGet(SweetbookClient client, String[] args) {
        if (args.length < 2) {
            System.out.println("사용법: SimpleOrders get <orderUid>");
            return;
        }
        JsonNode resp = client.orders.get(args[1]);
        System.out.println(resp.path("data").toPrettyString());
    }

    private static void cmdCancel(SweetbookClient client, String[] args) {
        if (args.length < 3) {
            System.out.println("사용법: SimpleOrders cancel <orderUid> <취소사유>");
            return;
        }
        String reason = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        JsonNode resp = client.orders.cancel(args[1], reason);
        long refund = resp.path("data").path("refundAmount").asLong(0);
        System.out.println("주문 취소 완료: " + args[1]);
        if (refund > 0) System.out.println("환불 금액: " + fmt(refund));
    }

    private static void cmdShipping(SweetbookClient client, String[] args) {
        if (args.length < 2) {
            System.out.println("사용법: SimpleOrders shipping <orderUid> [--name 홍길동] [--phone ...] [--postal ...] [--addr1 ...] [--addr2 ...]");
            return;
        }
        OrdersClient.ShippingPatch patch = new OrdersClient.ShippingPatch();
        for (int i = 2; i < args.length - 1; i++) {
            switch (args[i]) {
                case "--name":   patch.recipientName(args[i + 1]); break;
                case "--phone":  patch.recipientPhone(args[i + 1]); break;
                case "--postal": patch.postalCode(args[i + 1]); break;
                case "--addr1":  patch.address1(args[i + 1]); break;
                case "--addr2":  patch.address2(args[i + 1]); break;
                case "--memo":   patch.shippingMemo(args[i + 1]); break;
            }
        }
        client.orders.updateShipping(args[1], patch);
        System.out.println("배송지 변경 완료: " + args[1]);
    }

    private static String fmt(long amount) {
        return String.format("%,d원", amount);
    }

    private static void printUsage() {
        System.out.println("Commands:");
        System.out.println("  estimate <bookUid> [quantity]");
        System.out.println("  create <bookUid> <수령인> <전화번호> <우편번호> <주소>");
        System.out.println("  list [--status PAID]");
        System.out.println("  get <orderUid>");
        System.out.println("  cancel <orderUid> <사유>");
        System.out.println("  shipping <orderUid> [--name ...] [--phone ...] [--postal ...] [--addr1 ...]");
    }
}
