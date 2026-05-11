package com.sweetbook.bookprintapi;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Collections;
import java.util.List;

/**
 * {@code helpers.createBookFromTemplate} 반환값 — 11_sdk_helpers_design.md § 2.1.
 */
public final class BookBuildResult {

    private final String bookUid;
    private final Integer coverPageNum;
    private final List<ContentPageInfo> contentPages;
    private final boolean finalized;
    private final Integer pageCount;
    private final JsonNode rawBook;
    private final JsonNode rawCover;
    private final JsonNode rawFinalize;

    public BookBuildResult(
            String bookUid,
            Integer coverPageNum,
            List<ContentPageInfo> contentPages,
            boolean finalized,
            Integer pageCount,
            JsonNode rawBook,
            JsonNode rawCover,
            JsonNode rawFinalize) {
        this.bookUid = bookUid;
        this.coverPageNum = coverPageNum;
        this.contentPages = contentPages != null
                ? Collections.unmodifiableList(contentPages)
                : Collections.emptyList();
        this.finalized = finalized;
        this.pageCount = pageCount;
        this.rawBook = rawBook;
        this.rawCover = rawCover;
        this.rawFinalize = rawFinalize;
    }

    public String bookUid() { return bookUid; }
    public Integer coverPageNum() { return coverPageNum; }
    public List<ContentPageInfo> contentPages() { return contentPages; }
    public boolean finalized() { return finalized; }
    public Integer pageCount() { return pageCount; }
    public JsonNode rawBook() { return rawBook; }
    public JsonNode rawCover() { return rawCover; }
    public JsonNode rawFinalize() { return rawFinalize; }

    /** 내지 페이지 한 항목 — pageNum / pageSide. */
    public static final class ContentPageInfo {
        private final Integer pageNum;
        private final String pageSide;

        public ContentPageInfo(Integer pageNum, String pageSide) {
            this.pageNum = pageNum;
            this.pageSide = pageSide;
        }

        public Integer pageNum() { return pageNum; }
        public String pageSide() { return pageSide; }
    }
}
