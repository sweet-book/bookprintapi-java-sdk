package com.sweetbook.bookprintapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pagination / PageMeta / FieldError / ListResult 등 value 타입의
 * from() / equals / hashCode / toString 커버리지 보강.
 */
class ValueTypesTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static JsonNode tree(String json) throws Exception {
        return MAPPER.readTree(json);
    }

    // -- FieldError --

    @Test
    void fieldError_from_nullOrNonObject_returnsNull() throws Exception {
        assertNull(FieldError.from(null));
        assertNull(FieldError.from(tree("[1,2,3]")));
        assertNull(FieldError.from(tree("\"string\"")));
    }

    @Test
    void fieldError_from_typedValues_correctlyUnwrapped() throws Exception {
        // long
        FieldError feLong = FieldError.from(tree("{\"field\":\"x\",\"currentValue\":12345678901}"));
        assertTrue(feLong.currentValue() instanceof Long);
        assertEquals(12345678901L, feLong.currentValue());

        // double
        FieldError feDouble = FieldError.from(tree("{\"field\":\"x\",\"currentValue\":3.14}"));
        assertTrue(feDouble.currentValue() instanceof Double);

        // boolean
        FieldError feBool = FieldError.from(tree("{\"field\":\"x\",\"currentValue\":true}"));
        assertEquals(true, feBool.currentValue());

        // text
        FieldError feText = FieldError.from(tree("{\"field\":\"x\",\"currentValue\":\"hello\"}"));
        assertEquals("hello", feText.currentValue());

        // null currentValue
        FieldError feNull = FieldError.from(tree("{\"field\":\"x\",\"currentValue\":null}"));
        assertNull(feNull.currentValue());

        // 누락 필드
        FieldError feMissing = FieldError.from(tree("{\"field\":\"x\"}"));
        assertNull(feMissing.currentValue());
    }

    @Test
    void fieldError_equalsAndHashCode() {
        FieldError a = new FieldError("title", "필수", null, null, "required");
        FieldError b = new FieldError("title", "필수", null, null, "required");
        FieldError c = new FieldError("body", "필수", null, null, "required");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
        assertNotEquals(a, null);
        assertNotEquals(a, "string");
        assertSame(a, a); // self equality
    }

    @Test
    void fieldError_toString_includesFields() {
        FieldError fe = new FieldError("title", "필수", "x", "y", "required");
        String s = fe.toString();
        assertTrue(s.contains("title"));
        assertTrue(s.contains("required"));
    }

    // -- Pagination --

    @Test
    void pagination_from_nullOrNonObject_returnsEmpty() throws Exception {
        assertSame(Pagination.EMPTY, Pagination.from(null));
        assertSame(Pagination.EMPTY, Pagination.from(tree("[1,2]")));
        assertSame(Pagination.EMPTY, Pagination.from(tree("42")));
    }

    @Test
    void pagination_from_partialFields() throws Exception {
        Pagination p = Pagination.from(tree("{\"total\":5}"));
        assertEquals(5, p.total());
        assertEquals(0, p.limit());
        assertEquals(0, p.offset());
        assertFalse(p.hasMore());
    }

    @Test
    void pagination_equalsAndHashCode() {
        Pagination a = new Pagination(10, 20, 0, false);
        Pagination b = new Pagination(10, 20, 0, false);
        Pagination c = new Pagination(11, 20, 0, false);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
        assertNotEquals(a, null);
        assertNotEquals(a, "string");
        assertTrue(a.toString().contains("total=10"));
    }

    // -- PageMeta --

    @Test
    void pageMeta_from_nullOrNonObject_returnsEmpty() throws Exception {
        assertSame(PageMeta.EMPTY, PageMeta.from(null));
        assertSame(PageMeta.EMPTY, PageMeta.from(tree("[]")));
    }

    @Test
    void pageMeta_equalsAndHashCode() {
        PageMeta a = new PageMeta(8, 8, 60, 4, true);
        PageMeta b = new PageMeta(8, 8, 60, 4, true);
        PageMeta c = new PageMeta(7, 8, 60, 4, false);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
        assertNotEquals(a, null);
        assertNotEquals(a, "string");
        assertTrue(a.toString().contains("currentPageCount=8"));
    }

    // -- ListResult --

    @Test
    void listResult_equalsAndHashCode() {
        List<String> items1 = Arrays.asList("a", "b");
        List<String> items2 = Arrays.asList("a", "b");
        Pagination pg = new Pagination(2, 20, 0, false);
        ListResult<String> a = new ListResult<>(items1, pg);
        ListResult<String> b = new ListResult<>(items2, pg);
        ListResult<String> c = new ListResult<>(Collections.emptyList(), pg);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
        assertNotEquals(a, null);
        assertNotEquals(a, "string");
        assertTrue(a.toString().contains("items=[a, b]"));
        assertEquals(items1, a.items());
        assertEquals(pg, a.pagination());
    }
}
