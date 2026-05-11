package com.sweetbook.bookprintapi;

/**
 * BookPrintAPI errorCode 카탈로그 (24종 = Generic 8 + Specific 16).
 *
 * <p>서버의 {@code master_대비_변경사항.md § 2} 카탈로그와 1:1 정합. 응답의
 * {@code errorCode} 필드를 분기할 때 문자열 리터럴 대신 본 클래스의 상수를 사용하세요.
 *
 * <p>Python {@code bookprintapi.ErrorCodes} / Node {@code ErrorCodes} 와 동등.
 */
public final class ErrorCodes {

    // --- Generic (HTTP 상태 자동 주입) ---

    /** 400 — 요청 검증 실패. 일반적으로 {@code fieldErrors[]} 동봉. */
    public static final String VALIDATION_FAILED = "ERR_VALIDATION_FAILED";

    /** 400 — 요청 형식 자체가 깨짐 (JSON 파싱 실패 등). */
    public static final String MALFORMED_REQUEST = "ERR_MALFORMED_REQUEST";

    /** 401 — 인증 실패 (API Key 누락/오타/만료). */
    public static final String UNAUTHORIZED = "ERR_UNAUTHORIZED";

    /** 403 — 권한 없음. */
    public static final String FORBIDDEN = "ERR_FORBIDDEN";

    /** 404 — 리소스 없음. */
    public static final String NOT_FOUND = "ERR_NOT_FOUND";

    /** 409 — 상태 충돌 (예: 이미 finalized). */
    public static final String CONFLICT = "ERR_CONFLICT";

    /** 429 — 요청 한도 초과. */
    public static final String TOO_MANY_REQUESTS = "ERR_TOO_MANY_REQUESTS";

    /** 500 — 내부 서버 에러. */
    public static final String INTERNAL_ERROR = "ERR_INTERNAL_ERROR";

    // --- Specific: 페이지 / 책 제약 ---

    /** 400 — 페이지 수 부족 ({@code pageMin} 미달). */
    public static final String INSUFFICIENT_PAGES = "ERR_INSUFFICIENT_PAGES";

    /** 400 — 페이지 수가 책 사양에 맞지 않음 (증분/최대 위반). */
    public static final String PAGECOUNT_INVALID = "ERR_PAGECOUNT_INVALID";

    /** 400 — finalize 전제조건 미달 (표지/내지 미완성 등). */
    public static final String FINALIZE_PREREQ_UNMET = "ERR_FINALIZE_PREREQ_UNMET";

    /** 400 — 해당 creationType 미지원. */
    public static final String CREATION_TYPE_UNSUPPORTED = "ERR_CREATION_TYPE_UNSUPPORTED";

    // --- Specific: 템플릿 ---

    /** 400 — 템플릿이 요구한 binding이 없음. */
    public static final String TEMPLATE_BINDING_MISSING = "ERR_TEMPLATE_BINDING_MISSING";

    /** 400 — 필수 템플릿 파라미터 누락. */
    public static final String TEMPLATE_PARAM_REQUIRED = "ERR_TEMPLATE_PARAM_REQUIRED";

    // --- Specific: 주문 ---

    /** 400 — 현재 주문 상태에서 해당 전이 불가. */
    public static final String ORDER_TRANSITION_INVALID = "ERR_ORDER_TRANSITION_INVALID";

    /** 402 — 충전금 부족. {@code data: {required, balance}} 동봉. */
    public static final String INSUFFICIENT_CREDIT = "ERR_INSUFFICIENT_CREDIT";

    // --- Specific: 환경 / Sandbox ---

    /** 403 — sandbox/live 환경 불일치 (도메인↔리소스 env 미스매치). */
    public static final String ENV_MISMATCH = "ERR_ENV_MISMATCH";

    /** 501 — sandbox에서 미지원 기능. */
    public static final String SANDBOX_UNSUPPORTED = "ERR_SANDBOX_UNSUPPORTED";

    // --- Specific: 멱등성 ---

    /** 422 — 동일 Idempotency-Key 재사용 시 페이로드 불일치. */
    public static final String IDEMPOTENCY_KEY_MISMATCH = "ERR_IDEMPOTENCY_KEY_MISMATCH";

    // --- Specific: PDF 취득/생성 (C02) ---

    /** 404 — PDF_UPLOAD/MIX 모드에서 PDF가 아직 업로드되지 않음. */
    public static final String PDF_NOT_UPLOADED = "ERR_PDF_NOT_UPLOADED";

    /** 409 — PDF가 아직 생성되지 않음 (TEMPLATE 모드). */
    public static final String PDF_NOT_GENERATED = "ERR_PDF_NOT_GENERATED";

    /** 409 — PDF 생성 진행 중. */
    public static final String PDF_PENDING = "ERR_PDF_PENDING";

    /** 422 — PDF 생성 실패. */
    public static final String PDF_GENERATION_FAILED = "ERR_PDF_GENERATION_FAILED";

    /** 500 — PDF 파일 자체가 누락 (서버 측 저장소 이슈). */
    public static final String PDF_FILE_MISSING = "ERR_PDF_FILE_MISSING";

    private ErrorCodes() {}
}
