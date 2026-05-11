package com.sweetbook.bookprintapi;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderStatusTest {

    @Test
    void enumValueCount() {
        assertEquals(12, OrderStatus.values().length);
    }

    @Test
    void codeMapping_paid() {
        assertEquals(20, OrderStatus.PAID.code());
        assertEquals(OrderStatus.PAID, OrderStatus.fromCode(20).orElseThrow());
    }

    @Test
    void codeMapping_orderTransitions() {
        // 주요 전이 코드들이 enum에 정확히 매핑되는지
        assertEquals(15, OrderStatus.PAID_AWAITING_CONTENT.code());
        assertEquals(25, OrderStatus.PDF_READY.code());
        assertEquals(30, OrderStatus.CONFIRMED.code());
        assertEquals(40, OrderStatus.IN_PRODUCTION.code());
        assertEquals(45, OrderStatus.COMPLETED.code());
        assertEquals(50, OrderStatus.PRODUCTION_COMPLETE.code());
        assertEquals(60, OrderStatus.SHIPPED.code());
        assertEquals(70, OrderStatus.DELIVERED.code());
        assertEquals(80, OrderStatus.CANCELLED.code());
        assertEquals(81, OrderStatus.CANCELLED_REFUND.code());
        assertEquals(90, OrderStatus.ERROR.code());
    }

    @Test
    void fromCode_unknown() {
        assertTrue(OrderStatus.fromCode(999).isEmpty());
    }

    @Test
    void tryParse_validString() {
        Optional<OrderStatus> s = OrderStatus.tryParse("PAID");
        assertTrue(s.isPresent());
        assertEquals(OrderStatus.PAID, s.get());
    }

    @Test
    void tryParse_invalidString_returnsEmpty() {
        // 서버가 v1.x에서 새 상태를 추가했을 때 호출자가 안전하게 무시하도록
        assertFalse(OrderStatus.tryParse("FUTURE_STATUS").isPresent());
        assertFalse(OrderStatus.tryParse(null).isPresent());
        assertFalse(OrderStatus.tryParse("").isPresent());
    }
}
