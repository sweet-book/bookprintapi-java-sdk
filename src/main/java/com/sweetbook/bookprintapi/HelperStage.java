package com.sweetbook.bookprintapi;

/**
 * SDK 헬퍼 다단계 호출 중 어느 단계에서 실패했는지 식별.
 *
 * <p>11_sdk_helpers_design.md § 3.2.
 */
public enum HelperStage {
    /** 클라이언트측 입력 검증 (호출 전). */
    VALIDATION,

    // createBookFromTemplate 단계
    BOOK_CREATE,
    COVER_CREATE,
    CONTENT_INSERT,
    BOOK_FINALIZE,

    // uploadPdfAndOrder 단계
    PDF_UPLOAD_COVER,
    PDF_UPLOAD_CONTENTS,
    ORDER_ESTIMATE,
    ORDER_CREATE
}
