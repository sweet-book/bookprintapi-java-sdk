package com.sweetbook.bookprintapi.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.sweetbook.bookprintapi.http.HttpTransport;

import java.util.LinkedHashMap;
import java.util.Map;

/** 충전금 잔액 / 거래내역 / sandbox 충전. */
public class CreditsClient {

    private final HttpTransport http;

    public CreditsClient(HttpTransport http) {
        this.http = http;
    }

    public JsonNode getBalance() {
        return http.get("/credits", null);
    }

    public JsonNode transactions(Integer limit, Integer offset, String fromDate, String toDate) {
        Map<String, Object> q = new LinkedHashMap<>();
        q.put("limit", limit != null ? limit : 20);
        q.put("offset", offset != null ? offset : 0);
        if (fromDate != null) q.put("from", fromDate);
        if (toDate != null) q.put("to", toDate);
        return http.get("/credits/transactions", q);
    }

    /**
     * Sandbox 환경 한정: 충전금을 가짜로 부여하여 테스트.
     * @param amount 충전 금액
     * @param memo 메모 (선택)
     * @return 응답 본문 JSON
     */
    public JsonNode sandboxCharge(long amount, String memo) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("amount", amount);
        if (memo != null) body.put("memo", memo);
        return http.post("/credits/sandbox/charge", body);
    }
}
