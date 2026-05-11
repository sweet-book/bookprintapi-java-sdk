package com.sweetbook.bookprintapi;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * {@code helpers.uploadPdfAndOrder} 입력 DTO. 빌더 패턴.
 *
 * <p>11_sdk_helpers_design.md § 5.3 시그니처.
 */
public final class UploadPdfAndOrderRequest {

    private final String bookSpecUid;
    private final int pageCount;
    private final Path coverPdf;
    private final Path contentsPdf;
    private final Map<String, Object> shipping;
    private final int quantity;
    private final String orderExternalRef;
    private final String title;
    private final String bookExternalRef;
    private final String specProfileUid;
    private final boolean failOnInsufficientCredit;
    private final boolean skipEstimate;

    private UploadPdfAndOrderRequest(Builder b) {
        this.bookSpecUid = b.bookSpecUid;
        this.pageCount = b.pageCount;
        this.coverPdf = b.coverPdf;
        this.contentsPdf = b.contentsPdf;
        this.shipping = b.shipping;
        this.quantity = b.quantity;
        this.orderExternalRef = b.orderExternalRef;
        this.title = b.title;
        this.bookExternalRef = b.bookExternalRef;
        this.specProfileUid = b.specProfileUid;
        this.failOnInsufficientCredit = b.failOnInsufficientCredit;
        this.skipEstimate = b.skipEstimate;
    }

    public String bookSpecUid() { return bookSpecUid; }
    public int pageCount() { return pageCount; }
    public Path coverPdf() { return coverPdf; }
    public Path contentsPdf() { return contentsPdf; }
    public Map<String, Object> shipping() { return shipping; }
    public int quantity() { return quantity; }
    public String orderExternalRef() { return orderExternalRef; }
    public String title() { return title; }
    public String bookExternalRef() { return bookExternalRef; }
    public String specProfileUid() { return specProfileUid; }
    public boolean failOnInsufficientCredit() { return failOnInsufficientCredit; }
    public boolean skipEstimate() { return skipEstimate; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String bookSpecUid;
        private int pageCount;
        private Path coverPdf;
        private Path contentsPdf;
        private Map<String, Object> shipping = new LinkedHashMap<>();
        private int quantity = 1;
        private String orderExternalRef;
        private String title;
        private String bookExternalRef;
        private String specProfileUid;
        private boolean failOnInsufficientCredit = true;
        private boolean skipEstimate = false;

        public Builder bookSpecUid(String v) { this.bookSpecUid = v; return this; }
        public Builder pageCount(int v) { this.pageCount = v; return this; }
        public Builder coverPdf(Path v) { this.coverPdf = v; return this; }
        public Builder contentsPdf(Path v) { this.contentsPdf = v; return this; }
        public Builder shipping(Map<String, Object> v) {
            if (v != null) this.shipping.putAll(v);
            return this;
        }
        public Builder shippingField(String key, Object value) {
            this.shipping.put(key, value);
            return this;
        }
        public Builder quantity(int v) { this.quantity = v; return this; }
        public Builder orderExternalRef(String v) { this.orderExternalRef = v; return this; }
        public Builder title(String v) { this.title = v; return this; }
        public Builder bookExternalRef(String v) { this.bookExternalRef = v; return this; }
        public Builder specProfileUid(String v) { this.specProfileUid = v; return this; }
        public Builder failOnInsufficientCredit(boolean v) { this.failOnInsufficientCredit = v; return this; }
        public Builder skipEstimate(boolean v) { this.skipEstimate = v; return this; }

        public UploadPdfAndOrderRequest build() { return new UploadPdfAndOrderRequest(this); }
    }
}
