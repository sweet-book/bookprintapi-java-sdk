package com.sweetbook.bookprintapi;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Objects;

/**
 * 책 페이지 검증 메타 ({@code data.pageMeta}).
 *
 * <p>{@code POST /books}, {@code POST /books/{uid}/cover}, {@code POST /books/{uid}/contents},
 * {@code POST /books/{uid}/finalization}, PDF 응답, {@code GET /books/{uid}} 응답에 포함.
 */
public final class PageMeta {

    public static final PageMeta EMPTY = new PageMeta(0, 0, 0, 0, false);

    private final int currentPageCount;
    private final int pageMin;
    private final int pageMax;
    private final int pageIncrement;
    private final boolean isValid;

    public PageMeta(int currentPageCount, int pageMin, int pageMax, int pageIncrement, boolean isValid) {
        this.currentPageCount = currentPageCount;
        this.pageMin = pageMin;
        this.pageMax = pageMax;
        this.pageIncrement = pageIncrement;
        this.isValid = isValid;
    }

    public int currentPageCount() { return currentPageCount; }
    public int pageMin() { return pageMin; }
    public int pageMax() { return pageMax; }
    public int pageIncrement() { return pageIncrement; }
    public boolean isValid() { return isValid; }

    public static PageMeta from(JsonNode node) {
        if (node == null || !node.isObject()) return EMPTY;
        return new PageMeta(
                node.path("currentPageCount").asInt(0),
                node.path("pageMin").asInt(0),
                node.path("pageMax").asInt(0),
                node.path("pageIncrement").asInt(0),
                node.path("isValid").asBoolean(false)
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PageMeta)) return false;
        PageMeta p = (PageMeta) o;
        return currentPageCount == p.currentPageCount && pageMin == p.pageMin
                && pageMax == p.pageMax && pageIncrement == p.pageIncrement && isValid == p.isValid;
    }

    @Override
    public int hashCode() {
        return Objects.hash(currentPageCount, pageMin, pageMax, pageIncrement, isValid);
    }

    @Override
    public String toString() {
        return "PageMeta{currentPageCount=" + currentPageCount
                + ", pageMin=" + pageMin + ", pageMax=" + pageMax
                + ", pageIncrement=" + pageIncrement + ", isValid=" + isValid + "}";
    }
}
