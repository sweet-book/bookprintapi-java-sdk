package com.sweetbook.bookprintapi.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.sweetbook.bookprintapi.ListResult;
import com.sweetbook.bookprintapi.OrderStatus;
import com.sweetbook.bookprintapi.ResponseParser;
import com.sweetbook.bookprintapi.http.HttpTransport;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 주문 견적/생성/조회/취소/배송지 변경. Python {@code OrdersClient} 동등. */
public class OrdersClient {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final HttpTransport http;

    public OrdersClient(HttpTransport http) {
        this.http = http;
    }

    /**
     * 가격 견적 (충전금 차감 없음).
     *
     * @param items {@code [{"bookUid":"...", "quantity":1}, ...]}
     * @return 응답 본문 JSON
     */
    public JsonNode estimate(List<Map<String, Object>> items) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("items is required");
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("items", items);
        return http.post("/orders/estimate", body);
    }

    /**
     * 주문 생성 (충전금 즉시 차감).
     *
     * @param items       {@code [{"bookUid", "quantity"}, ...]}
     * @param shipping    {@code {"recipientName", "recipientPhone", "postalCode", "address1", "address2"?, "memo"?}}
     * @param externalRef 외부 참조 ID (선택, 최대 100자)
     * @return 응답 본문 JSON
     */
    public JsonNode create(List<Map<String, Object>> items, Map<String, Object> shipping, String externalRef) {
        if (items == null || items.isEmpty()) throw new IllegalArgumentException("items is required");
        if (shipping == null) throw new IllegalArgumentException("shipping is required");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("items", items);
        body.put("shipping", shipping);
        if (externalRef != null) body.put("externalRef", externalRef);
        return http.post("/orders", body);
    }

    /**
     * 주문 목록 조회 — status는 {@link OrderStatus} 또는 그 문자열, 또는 숫자 코드.
     * @param status 상태 필터 (enum/문자열/숫자, null=전체)
     * @param limit 페이지 크기 (기본 20)
     * @param offset 페이지 오프셋 (기본 0)
     * @param fromDate 시작 날짜 (yyyy-MM-dd)
     * @param toDate 종료 날짜 (yyyy-MM-dd)
     * @return 주문 목록 + pagination
     */
    public ListResult<Map<String, Object>> list(Object status, Integer limit, Integer offset, String fromDate, String toDate) {
        Map<String, Object> q = new LinkedHashMap<>();
        q.put("limit", limit != null ? limit : 20);
        q.put("offset", offset != null ? offset : 0);
        if (status != null) {
            // OrderStatus enum이 들어오면 name(), 그 외엔 toString
            q.put("status", status instanceof OrderStatus ? ((OrderStatus) status).name() : status);
        }
        if (fromDate != null) q.put("from", fromDate);
        if (toDate != null) q.put("to", toDate);
        JsonNode body = http.get("/orders", q);
        return new ResponseParser(body).toListResult(MAP_TYPE);
    }

    public ListResult<Map<String, Object>> list() {
        return list(null, null, null, null, null);
    }

    public ListResult<Map<String, Object>> list(OrderStatus status) {
        return list(status, null, null, null, null);
    }

    public JsonNode get(String orderUid) {
        require(orderUid, "orderUid");
        return http.get("/orders/" + orderUid, null);
    }

    /**
     * 주문 취소 (PAID / PDF_READY 상태만 가능, 충전금 자동 반환).
     * @param orderUid 주문 UID
     * @param cancelReason 취소 사유
     * @return 응답 본문 JSON
     */
    public JsonNode cancel(String orderUid, String cancelReason) {
        require(orderUid, "orderUid");
        require(cancelReason, "cancelReason");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("cancelReason", cancelReason);
        return http.post("/orders/" + orderUid + "/cancel", body);
    }

    /**
     * 배송지 변경 (발송 전 상태만 가능). 변경하지 않을 필드는 null로 두면 페이로드에서 제외.
     * @param orderUid 주문 UID
     * @param patch 변경할 배송지 필드
     * @return 응답 본문 JSON
     */
    public JsonNode updateShipping(String orderUid, ShippingPatch patch) {
        require(orderUid, "orderUid");
        if (patch == null) throw new IllegalArgumentException("patch is required");
        return http.patch("/orders/" + orderUid + "/shipping", patch.toMap());
    }

    private static void require(String value, String name) {
        if (value == null || value.isEmpty()) throw new IllegalArgumentException(name + " is required");
    }

    /** 배송지 부분 업데이트 페이로드. null 필드는 페이로드에서 제외. */
    public static final class ShippingPatch {
        private String recipientName;
        private String recipientPhone;
        private String postalCode;
        private String address1;
        private String address2;
        private String shippingMemo;

        public ShippingPatch recipientName(String v) { this.recipientName = v; return this; }
        public ShippingPatch recipientPhone(String v) { this.recipientPhone = v; return this; }
        public ShippingPatch postalCode(String v) { this.postalCode = v; return this; }
        public ShippingPatch address1(String v) { this.address1 = v; return this; }
        public ShippingPatch address2(String v) { this.address2 = v; return this; }
        public ShippingPatch shippingMemo(String v) { this.shippingMemo = v; return this; }

        public Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<>();
            if (recipientName != null) m.put("recipientName", recipientName);
            if (recipientPhone != null) m.put("recipientPhone", recipientPhone);
            if (postalCode != null) m.put("postalCode", postalCode);
            if (address1 != null) m.put("address1", address1);
            if (address2 != null) m.put("address2", address2);
            if (shippingMemo != null) m.put("shippingMemo", shippingMemo);
            return m;
        }
    }
}
