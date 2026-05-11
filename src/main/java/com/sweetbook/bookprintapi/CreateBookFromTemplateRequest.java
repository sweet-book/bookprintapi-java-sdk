package com.sweetbook.bookprintapi;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * {@code helpers.createBookFromTemplate} 입력 DTO. 빌더 패턴.
 *
 * <p>11_sdk_helpers_design.md § 5.3 시그니처. 사용 예:
 * <pre>{@code
 * var req = CreateBookFromTemplateRequest.builder()
 *     .bookSpecUid("PHOTOBOOK_A4_SC")
 *     .coverTemplateUid("cv_xxx")
 *     .coverParam("title", "My Book")
 *     .coverBindingFile("coverPhoto", Path.of("cover.jpg"))
 *     .addContent(ContentPage.builder()
 *         .templateUid("p_xxx")
 *         .param("text", "본문")
 *         .build())
 *     .title("My Book")
 *     .build();
 * }</pre>
 */
public final class CreateBookFromTemplateRequest {

    private final String bookSpecUid;
    private final String coverTemplateUid;
    private final Map<String, Object> coverParams;
    private final Map<String, Path> coverBindingFiles;
    private final List<ContentPage> contents;
    private final String title;
    private final String externalRef;
    private final String specProfileUid;
    private final boolean skipFinalize;

    private CreateBookFromTemplateRequest(Builder b) {
        this.bookSpecUid = b.bookSpecUid;
        this.coverTemplateUid = b.coverTemplateUid;
        this.coverParams = b.coverParams;
        this.coverBindingFiles = b.coverBindingFiles;
        this.contents = b.contents;
        this.title = b.title;
        this.externalRef = b.externalRef;
        this.specProfileUid = b.specProfileUid;
        this.skipFinalize = b.skipFinalize;
    }

    public String bookSpecUid() { return bookSpecUid; }
    public String coverTemplateUid() { return coverTemplateUid; }
    public Map<String, Object> coverParams() { return coverParams; }
    public Map<String, Path> coverBindingFiles() { return coverBindingFiles; }
    public List<ContentPage> contents() { return contents; }
    public String title() { return title; }
    public String externalRef() { return externalRef; }
    public String specProfileUid() { return specProfileUid; }
    public boolean skipFinalize() { return skipFinalize; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String bookSpecUid;
        private String coverTemplateUid;
        private Map<String, Object> coverParams = new LinkedHashMap<>();
        private Map<String, Path> coverBindingFiles = new LinkedHashMap<>();
        private List<ContentPage> contents = new ArrayList<>();
        private String title;
        private String externalRef;
        private String specProfileUid;
        private boolean skipFinalize = false;

        public Builder bookSpecUid(String v) { this.bookSpecUid = v; return this; }
        public Builder coverTemplateUid(String v) { this.coverTemplateUid = v; return this; }
        public Builder coverParams(Map<String, Object> v) {
            if (v != null) this.coverParams.putAll(v);
            return this;
        }
        public Builder coverParam(String key, Object value) {
            this.coverParams.put(key, value);
            return this;
        }
        public Builder coverBindingFile(String bindingName, Path file) {
            this.coverBindingFiles.put(bindingName, file);
            return this;
        }
        public Builder addContent(ContentPage p) { this.contents.add(p); return this; }
        public Builder title(String v) { this.title = v; return this; }
        public Builder externalRef(String v) { this.externalRef = v; return this; }
        public Builder specProfileUid(String v) { this.specProfileUid = v; return this; }
        public Builder skipFinalize(boolean v) { this.skipFinalize = v; return this; }

        public CreateBookFromTemplateRequest build() { return new CreateBookFromTemplateRequest(this); }
    }

    /** 내지 페이지 한 항목 (빌더 패턴). */
    public static final class ContentPage {
        private final String templateUid;
        private final Map<String, Object> params;
        private final Map<String, Path> bindingFiles;
        private final String breakBefore;

        private ContentPage(ContentPage.Builder b) {
            this.templateUid = b.templateUid;
            this.params = b.params;
            this.bindingFiles = b.bindingFiles;
            this.breakBefore = b.breakBefore;
        }

        public String templateUid() { return templateUid; }
        public Map<String, Object> params() { return params; }
        public Map<String, Path> bindingFiles() { return bindingFiles; }
        public String breakBefore() { return breakBefore; }

        public static ContentPage.Builder builder() { return new ContentPage.Builder(); }

        public static final class Builder {
            private String templateUid;
            private Map<String, Object> params = new LinkedHashMap<>();
            private Map<String, Path> bindingFiles = new LinkedHashMap<>();
            private String breakBefore;

            public Builder templateUid(String v) { this.templateUid = v; return this; }
            public Builder param(String key, Object value) { this.params.put(key, value); return this; }
            public Builder params(Map<String, Object> v) {
                if (v != null) this.params.putAll(v);
                return this;
            }
            public Builder bindingFile(String bindingName, Path file) {
                this.bindingFiles.put(bindingName, file);
                return this;
            }
            public Builder breakBefore(String v) { this.breakBefore = v; return this; }
            public ContentPage build() { return new ContentPage(this); }
        }
    }
}
