package com.sweetbook.bookprintapi;

/**
 * 헬퍼 임시 errorCode — 11_sdk_helpers_design.md § 3.4.
 *
 * <p>C03 에러코드 체계 확정 시 본 코드들을 표준 {@link ErrorCodes} 로 매핑할 예정.
 */
public final class HelperErrorCodes {

    public static final String BOOK_CREATE_FAILED = "SDK_HLPR_BOOK_CREATE_FAILED";
    public static final String COVER_CREATE_FAILED = "SDK_HLPR_COVER_CREATE_FAILED";
    public static final String CONTENT_INSERT_FAILED = "SDK_HLPR_CONTENT_INSERT_FAILED";
    public static final String PDF_UPLOAD_FAILED = "SDK_HLPR_PDF_UPLOAD_FAILED";
    public static final String FINALIZE_FAILED = "SDK_HLPR_FINALIZE_FAILED";
    public static final String CREDIT_INSUFFICIENT = "SDK_HLPR_CREDIT_INSUFFICIENT";
    public static final String ORDER_ESTIMATE_FAILED = "SDK_HLPR_ORDER_ESTIMATE_FAILED";
    public static final String ORDER_CREATE_FAILED = "SDK_HLPR_ORDER_CREATE_FAILED";
    public static final String VALIDATION = "SDK_HLPR_VALIDATION";

    private HelperErrorCodes() {}
}
