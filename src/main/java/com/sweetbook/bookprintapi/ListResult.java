package com.sweetbook.bookprintapi;

import java.util.List;
import java.util.Objects;

/**
 * 리스트 엔드포인트의 정규화된 반환 형태.
 *
 * <p>{@code BooksClient.list()} 등이 반환. v1 평탄화 / 구버전 응답을 흡수하여 항상
 * 동일한 shape으로 노출.
 *
 * <p>Node SDK 0.2.1 의 {@code ResponseParser.toListResult(key)} 와 동등 — 다만 Java는
 * 제네릭으로 타입 안정.
 */
public final class ListResult<T> {

    private final List<T> items;
    private final Pagination pagination;

    public ListResult(List<T> items, Pagination pagination) {
        this.items = items;
        this.pagination = pagination;
    }

    public List<T> items() { return items; }
    public Pagination pagination() { return pagination; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ListResult)) return false;
        ListResult<?> that = (ListResult<?>) o;
        return Objects.equals(items, that.items) && Objects.equals(pagination, that.pagination);
    }

    @Override
    public int hashCode() {
        return Objects.hash(items, pagination);
    }

    @Override
    public String toString() {
        return "ListResult{items=" + items + ", pagination=" + pagination + "}";
    }
}
