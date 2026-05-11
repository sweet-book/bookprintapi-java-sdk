package com.sweetbook.bookprintapi;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 주문 상태 enum (12종, 문자열 enum).
 *
 * <p>서버 v1 응답에서 {@code orderStatus} / {@code itemStatus} 는 본 enum의 이름과 일치하는
 * 문자열로 옵니다. 숫자 코드는 관리자 응답의 {@code orderStatusCode} / {@code itemStatusCode}
 * 별도 필드에만 노출.
 *
 * <p>Python {@code OrderStatus} 클래스 / Node {@code OrderStatus} 객체와 동등하지만,
 * Java에서는 enum 자체에 정수 코드를 묶어둔 형태로 강화.
 */
public enum OrderStatus {
    PAID_AWAITING_CONTENT(15),
    PAID(20),
    /** 주문 항목 상태에서만 등장. */
    PDF_READY(25),
    CONFIRMED(30),
    IN_PRODUCTION(40),
    /** 주문 항목 상태에서만 등장. */
    COMPLETED(45),
    /** 주문 전체 상태에서만 등장. */
    PRODUCTION_COMPLETE(50),
    SHIPPED(60),
    /** 주문 전체 상태에서만 등장. */
    DELIVERED(70),
    CANCELLED(80),
    /** 주문 전체 상태에서만 등장 (환불 동반 취소). */
    CANCELLED_REFUND(81),
    ERROR(90);

    private static final Map<Integer, OrderStatus> BY_CODE = new HashMap<>();

    static {
        for (OrderStatus s : values()) {
            BY_CODE.put(s.code, s);
        }
    }

    private final int code;

    OrderStatus(int code) {
        this.code = code;
    }

    /**
     * 정수 코드 (관리자/디버깅 용도).
     * @return 상태 코드
     */
    public int code() {
        return code;
    }

    /**
     * 정수 코드 → enum 변환. 매핑 없는 코드는 빈 Optional.
     * @param code 정수 상태 코드
     * @return 매칭된 enum 또는 빈 {@link Optional}
     */
    public static Optional<OrderStatus> fromCode(int code) {
        return Optional.ofNullable(BY_CODE.get(code));
    }

    /**
     * 문자열 → enum 변환 (대소문자 구분, null safe).
     *
     * <p>응답에서 받은 미지의 enum 값(서버가 새 상태를 추가한 경우)을 호출자가 안전하게
     * 무시할 수 있도록 Optional로 반환.
     * @param name enum 이름 문자열
     * @return 매칭된 enum 또는 빈 {@link Optional}
     */
    public static Optional<OrderStatus> tryParse(String name) {
        if (name == null || name.isEmpty()) return Optional.empty();
        try {
            return Optional.of(OrderStatus.valueOf(name));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
