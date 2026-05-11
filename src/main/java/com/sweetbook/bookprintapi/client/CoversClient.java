package com.sweetbook.bookprintapi.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sweetbook.bookprintapi.http.HttpTransport;
import com.sweetbook.bookprintapi.http.MultipartBodyPublisher;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 책 표지 생성/조회/삭제. multipart로 templateUid + parameters(JSON) + files[] 전송. */
public class CoversClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final HttpTransport http;

    public CoversClient(HttpTransport http) {
        this.http = http;
    }

    /**
     * 표지 생성/수정 — 권장 시그니처. multipart 파일 part 이름은 템플릿 binding 이름과 일치.
     *
     * <p>예: 템플릿이 {@code coverPhoto} binding을 요구하면
     * {@code bindingFiles=Map.of("coverPhoto", Path.of("photo.jpg"))} 로 전달.
     * 또는 사진을 미리 {@link PhotosClient#upload}로 업로드한 뒤
     * {@code parameters.coverPhoto = "uploaded fileName"} 으로 참조.
     *
     * @param bookUid       책 UID
     * @param templateUid   표지 템플릿 UID
     * @param parameters    템플릿 파라미터 (텍스트 / 사진 fileName 참조 등). null 가능.
     * @param bindingFiles  binding 이름 → 파일 매핑. null 가능.
     * @return 응답 본문 JSON
     */
    public JsonNode create(String bookUid, String templateUid,
                           Map<String, Object> parameters,
                           Map<String, Path> bindingFiles) {
        require(bookUid, "bookUid");
        require(templateUid, "templateUid");

        MultipartBodyPublisher mp = new MultipartBodyPublisher()
                .addText("templateUid", templateUid)
                .addText("parameters", serializeJson(parameters));

        if (bindingFiles != null) {
            for (Map.Entry<String, Path> e : bindingFiles.entrySet()) {
                if (e.getKey() == null || e.getValue() == null) continue;
                mp.addFile(e.getKey(), e.getValue(), PhotosClient.guessImageContentType(e.getValue()));
            }
        }

        return http.postMultipart("/books/" + bookUid + "/cover", mp, null);
    }

    /**
     * @param bookUid     책 UID
     * @param templateUid 표지 템플릿 UID
     * @param parameters  템플릿 파라미터
     * @param files       업로드 파일 리스트
     * @return 응답 본문 JSON
     * @deprecated multipart 파일 part 이름은 템플릿 binding 이름과 일치해야 합니다 — 본 메서드는
     *     모든 파일을 {@code files} 라는 단일 필드명으로 보내 서버가 거부합니다. 대신
     *     {@link #create(String, String, Map, Map)} (binding 이름 → Path 매핑) 를 사용하세요.
     */
    @Deprecated
    public JsonNode createWithFiles(String bookUid, String templateUid,
                                    Map<String, Object> parameters, List<Path> files) {
        Map<String, Path> bindingFiles = new LinkedHashMap<>();
        if (files != null) {
            int i = 0;
            for (Path f : files) {
                if (f == null) continue;
                // legacy: 파일 N개를 'files', 'files1', 'files2' ... 로 매핑 — 동작 보장 안 됨
                bindingFiles.put(i == 0 ? "files" : ("files" + i), f);
                i++;
            }
        }
        return create(bookUid, templateUid, parameters, bindingFiles);
    }

    /**
     * 표지 정보 조회.
     * @param bookUid 책 UID
     * @return 응답 본문 JSON
     */
    public JsonNode get(String bookUid) {
        require(bookUid, "bookUid");
        return http.get("/books/" + bookUid + "/cover", null);
    }

    /**
     * 표지 삭제.
     * @param bookUid 책 UID
     * @return 응답 본문 JSON
     */
    public JsonNode delete(String bookUid) {
        require(bookUid, "bookUid");
        return http.delete("/books/" + bookUid + "/cover");
    }

    static String serializeJson(Map<String, Object> m) {
        try {
            return MAPPER.writeValueAsString(m == null ? new java.util.HashMap<String, Object>() : m);
        } catch (Exception e) {
            throw new IllegalArgumentException("parameters JSON 직렬화 실패: " + e.getMessage(), e);
        }
    }

    private static void require(String value, String name) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException(name + " is required");
        }
    }
}
