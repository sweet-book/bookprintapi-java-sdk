package com.sweetbook.bookprintapi.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.sweetbook.bookprintapi.http.HttpTransport;
import com.sweetbook.bookprintapi.http.MultipartBodyPublisher;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 책 내지 페이지 삽입/삭제.
 *
 * <p>Node SDK 기준 multipart의 파일 필드명은 {@code rowPhotos} (Covers의 {@code files}와 다름).
 */
public class ContentsClient {

    private final HttpTransport http;

    public ContentsClient(HttpTransport http) {
        this.http = http;
    }

    /**
     * 내지 삽입 — multipart 파일 part 이름은 템플릿 binding 이름과 일치.
     *
     * <p>예: 템플릿이 {@code mainPhoto}, {@code subPhoto} binding을 요구하면
     * {@code bindingFiles=Map.of("mainPhoto", path1, "subPhoto", path2)} 로 전달.
     *
     * @param bookUid      책 UID
     * @param templateUid  템플릿 UID
     * @param parameters   템플릿 파라미터
     * @param breakBefore  null / "page" / "spread" / "column"
     * @param bindingFiles binding 이름 → 파일 매핑. null 가능.
     * @return 응답 본문 JSON
     */
    public JsonNode insert(String bookUid, String templateUid,
                           Map<String, Object> parameters,
                           Map<String, Path> bindingFiles,
                           String breakBefore) {
        require(bookUid, "bookUid");
        require(templateUid, "templateUid");

        MultipartBodyPublisher mp = new MultipartBodyPublisher()
                .addText("templateUid", templateUid)
                .addText("parameters", CoversClient.serializeJson(parameters));

        if (bindingFiles != null) {
            for (Map.Entry<String, Path> e : bindingFiles.entrySet()) {
                if (e.getKey() == null || e.getValue() == null) continue;
                mp.addFile(e.getKey(), e.getValue(), PhotosClient.guessImageContentType(e.getValue()));
            }
        }

        Map<String, Object> q = null;
        if (breakBefore != null && !breakBefore.isEmpty()) {
            q = new LinkedHashMap<>();
            q.put("breakBefore", breakBefore);
        }

        return http.postMultipart("/books/" + bookUid + "/contents", mp, q);
    }

    /**
     * 편의 — 파일 없는 텍스트 전용 내지 삽입.
     * @param bookUid 책 UID
     * @param templateUid 템플릿 UID
     * @param parameters 템플릿 파라미터
     * @return 응답 본문 JSON
     */
    public JsonNode insert(String bookUid, String templateUid, Map<String, Object> parameters) {
        return insert(bookUid, templateUid, parameters, null, null);
    }

    /**
     * 모든 내지 페이지 삭제 (표지는 유지).
     * @param bookUid 책 UID
     * @return 응답 본문 JSON
     */
    public JsonNode clear(String bookUid) {
        require(bookUid, "bookUid");
        return http.delete("/books/" + bookUid + "/contents");
    }

    private static void require(String value, String name) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException(name + " is required");
        }
    }
}
