package com.sweetbook.bookprintapi.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.sweetbook.bookprintapi.ListResult;
import com.sweetbook.bookprintapi.ResponseParser;
import com.sweetbook.bookprintapi.http.HttpTransport;
import com.sweetbook.bookprintapi.http.MultipartBodyPublisher;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/** 책 사진 업로드/조회/삭제. */
public class PhotosClient {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final HttpTransport http;

    public PhotosClient(HttpTransport http) {
        this.http = http;
    }

    /**
     * 사진 1장 업로드.
     *
     * @param bookUid 책 UID
     * @param file 이미지 파일 경로
     * @return 응답 본문 JSON
     */
    public JsonNode upload(String bookUid, Path file) {
        return upload(bookUid, file, null);
    }

    /**
     * @param bookUid 책 UID
     * @param file 이미지 파일 경로
     * @param contentType MIME (예: image/jpeg). null이면 파일 확장자로 추정.
     * @return 응답 본문 JSON
     */
    public JsonNode upload(String bookUid, Path file, String contentType) {
        require(bookUid, "bookUid");
        if (file == null) throw new IllegalArgumentException("file is required");

        String ct = contentType != null ? contentType : guessImageContentType(file);
        MultipartBodyPublisher mp = new MultipartBodyPublisher().addFile("file", file, ct);
        return http.postMultipart("/books/" + bookUid + "/photos", mp, null);
    }

    /**
     * 업로드된 사진 목록 (평탄화 응답 정규화).
     * @param bookUid 책 UID
     * @return 사진 목록 + pagination
     */
    public ListResult<Map<String, Object>> list(String bookUid) {
        require(bookUid, "bookUid");
        JsonNode body = http.get("/books/" + bookUid + "/photos", null);
        return new ResponseParser(body).toListResult(MAP_TYPE);
    }

    /**
     * 사진 삭제 (draft 상태 책만).
     * @param bookUid 책 UID
     * @param fileName 업로드된 사진 파일명
     * @return 응답 본문 JSON
     */
    public JsonNode delete(String bookUid, String fileName) {
        require(bookUid, "bookUid");
        require(fileName, "fileName");
        return http.delete("/books/" + bookUid + "/photos/" + fileName);
    }

    private static void require(String value, String name) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException(name + " is required");
        }
    }

    /** 파일 확장자 기반 MIME 추정. SDK 외부 라이브러리 의존을 피하기 위한 미니 매핑. */
    static String guessImageContentType(Path file) {
        String name = file.getFileName() != null ? file.getFileName().toString().toLowerCase() : "";
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) return "image/jpeg";
        if (name.endsWith(".png")) return "image/png";
        if (name.endsWith(".gif")) return "image/gif";
        if (name.endsWith(".webp")) return "image/webp";
        if (name.endsWith(".heic")) return "image/heic";
        // probeContentType은 OS/파일시스템에 따라 null 반환 가능
        try {
            String probed = Files.probeContentType(file);
            if (probed != null) return probed;
        } catch (Exception ignored) {
        }
        return "application/octet-stream";
    }
}
