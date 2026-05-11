package com.sweetbook.bookprintapi;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Objects;

/**
 * 리스트 응답의 pagination 메타.
 *
 * <p>v1 평탄화 응답에서 최상위에 위치, 구버전은 {@code data.pagination}. {@link ResponseParser#getPagination()}
 * 가 양쪽을 흡수.
 */
public final class Pagination {

    public static final Pagination EMPTY = new Pagination(0, 0, 0, false);

    private final int total;
    private final int limit;
    private final int offset;
    private final boolean hasMore;

    public Pagination(int total, int limit, int offset, boolean hasMore) {
        this.total = total;
        this.limit = limit;
        this.offset = offset;
        this.hasMore = hasMore;
    }

    public int total() { return total; }
    public int limit() { return limit; }
    public int offset() { return offset; }
    public boolean hasMore() { return hasMore; }

    /**
     * JSON 노드 → Pagination. 누락 필드는 0 / false 폴백.
     *
     * <p>v1 envelope 평탄화 (commit 6fbf346) 이후 서버는 {@code hasNext} 필드를 사용.
     * 구 응답은 {@code hasMore} 였음. 본 메서드는 둘 다 흡수.
     *
     * @param node pagination JSON 객체
     * @return 파싱된 인스턴스 (노드가 객체가 아니면 {@link #EMPTY})
     */
    public static Pagination from(JsonNode node) {
        if (node == null || !node.isObject()) return EMPTY;
        JsonNode hasNext = node.get("hasNext");
        boolean more = hasNext != null
                ? hasNext.asBoolean(false)
                : node.path("hasMore").asBoolean(false);
        return new Pagination(
                node.path("total").asInt(0),
                node.path("limit").asInt(0),
                node.path("offset").asInt(0),
                more
        );
    }

    /**
     * total 만 알 때 Pagination 생성 (구 photos 응답의 {@code data.totalCount} 흡수용).
     * @param total 총 개수
     * @return total 만 설정된 Pagination
     */
    public static Pagination fromTotal(long total) {
        return new Pagination((int) total, 0, 0, false);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Pagination)) return false;
        Pagination p = (Pagination) o;
        return total == p.total && limit == p.limit && offset == p.offset && hasMore == p.hasMore;
    }

    @Override
    public int hashCode() {
        return Objects.hash(total, limit, offset, hasMore);
    }

    @Override
    public String toString() {
        return "Pagination{total=" + total + ", limit=" + limit
                + ", offset=" + offset + ", hasMore=" + hasMore + "}";
    }
}
