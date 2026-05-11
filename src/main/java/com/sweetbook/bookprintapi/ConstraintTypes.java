package com.sweetbook.bookprintapi;

/**
 * {@code fieldErrors[].constraint} 열거값 (6종).
 *
 * <p>Python {@code ConstraintTypes} / Node {@code ConstraintTypes} 와 동등.
 */
public final class ConstraintTypes {

    /** 최솟값 미달. */
    public static final String MIN = "min";

    /** 최댓값 초과. */
    public static final String MAX = "max";

    /** 증분 위반 (예: 4페이지 단위인데 5). */
    public static final String INCREMENT = "increment";

    /** 허용된 enum 값 외. */
    public static final String ENUM = "enum";

    /** 정규식 패턴 위반. */
    public static final String PATTERN = "pattern";

    /** 필수 필드 누락. */
    public static final String REQUIRED = "required";

    private ConstraintTypes() {}
}
