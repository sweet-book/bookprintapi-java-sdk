package com.sweetbook.bookprintapi;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * {@code helpers.uploadPdfAndOrder} 반환값 — 11_sdk_helpers_design.md § 2.2.
 */
public final class PdfOrderBuildResult {

    private final String bookUid;
    private final String orderUid;
    private final boolean finalized;
    private final JsonNode coverPdf;
    private final JsonNode contentsPdf;
    private final JsonNode estimate;
    private final JsonNode order;

    public PdfOrderBuildResult(
            String bookUid,
            String orderUid,
            boolean finalized,
            JsonNode coverPdf,
            JsonNode contentsPdf,
            JsonNode estimate,
            JsonNode order) {
        this.bookUid = bookUid;
        this.orderUid = orderUid;
        this.finalized = finalized;
        this.coverPdf = coverPdf;
        this.contentsPdf = contentsPdf;
        this.estimate = estimate;
        this.order = order;
    }

    public String bookUid() { return bookUid; }
    public String orderUid() { return orderUid; }
    public boolean finalized() { return finalized; }
    public JsonNode coverPdf() { return coverPdf; }
    public JsonNode contentsPdf() { return contentsPdf; }
    /** {@code skipEstimate=true} 면 null. */
    public JsonNode estimate() { return estimate; }
    public JsonNode order() { return order; }
}
