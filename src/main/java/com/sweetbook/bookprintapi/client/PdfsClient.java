package com.sweetbook.bookprintapi.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.sweetbook.bookprintapi.http.HttpTransport;
import com.sweetbook.bookprintapi.http.MultipartBodyPublisher;

import java.nio.file.Path;

/**
 * 책 표지/내지 PDF 업로드 + 다운로드.
 *
 * <p>대상: {@code creationType = "PDF_UPLOAD"} 또는 {@code "MIX_COVER_TEMPLATE"} 책.
 *
 * <ul>
 *   <li>POST /books/{uid}/pdf-cover — 신규 등록 (이미 있으면 409 → replace 사용)</li>
 *   <li>PUT  /books/{uid}/pdf-cover — 교체 (없으면 404)</li>
 *   <li>GET  /books/{uid}/pdf-cover — binary 다운로드</li>
 *   <li>(내지도 동일 패턴, path만 /pdf-contents)</li>
 * </ul>
 */
public class PdfsClient {

    private final HttpTransport http;

    public PdfsClient(HttpTransport http) {
        this.http = http;
    }

    public JsonNode uploadCover(String bookUid, Path file) {
        return upload(bookUid, file, "pdf-cover", false);
    }

    public JsonNode replaceCover(String bookUid, Path file) {
        return upload(bookUid, file, "pdf-cover", true);
    }

    public byte[] downloadCover(String bookUid) {
        require(bookUid, "bookUid");
        return http.downloadBinary("/books/" + bookUid + "/pdf-cover");
    }

    public JsonNode uploadContents(String bookUid, Path file) {
        return upload(bookUid, file, "pdf-contents", false);
    }

    public JsonNode replaceContents(String bookUid, Path file) {
        return upload(bookUid, file, "pdf-contents", true);
    }

    public byte[] downloadContents(String bookUid) {
        require(bookUid, "bookUid");
        return http.downloadBinary("/books/" + bookUid + "/pdf-contents");
    }

    private JsonNode upload(String bookUid, Path file, String kind, boolean replace) {
        require(bookUid, "bookUid");
        if (file == null) throw new IllegalArgumentException("file is required");
        String path = "/books/" + bookUid + "/" + kind;
        MultipartBodyPublisher mp = new MultipartBodyPublisher()
                .addFile("file", file, "application/pdf");
        return replace ? http.putMultipart(path, mp) : http.postMultipart(path, mp, null);
    }

    private static void require(String value, String name) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException(name + " is required");
        }
    }
}
