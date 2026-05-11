package com.sweetbook.bookprintapi.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * RFC 7578 multipart/form-data 본문 빌더. {@code java.net.http.HttpClient}에 multipart가
 * 내장되어 있지 않아 직접 구현.
 *
 * <p>Python {@code requests} multipart / Node {@code FormData} 와 동일한 효과:
 * <ul>
 *   <li>{@link #addText(String, String)} — text part (파일명 없음, content-type 없음)</li>
 *   <li>{@link #addFile(String, Path, String)} — 파일 part (Content-Disposition + Content-Type)</li>
 *   <li>{@link #addBytes(String, String, byte[], String)} — 메모리 데이터 part</li>
 * </ul>
 *
 * <p>같은 이름으로 여러 part 추가 가능 ({@code files} 필드에 여러 파일 등).
 *
 * <p>Boundary는 인스턴스 생성 시 UUID 기반으로 자동 생성. {@link #contentType()}이
 * {@code multipart/form-data; boundary=...} 헤더값을 반환합니다.
 *
 * <p>현재 구현은 메모리에 전체 본문을 누적합니다. 매우 큰 파일(수백 MB)은 stream 기반
 * 구현으로 교체 필요 — 현 SDK 사용 케이스(이미지 ~수 MB, PDF ~50 MB)에서는 충분.
 */
public class MultipartBodyPublisher {

    private static final byte[] CRLF = {'\r', '\n'};

    private final String boundary;
    private final List<Part> parts = new ArrayList<>();

    public MultipartBodyPublisher() {
        this.boundary = "----bookprintapi-" + UUID.randomUUID();
    }

    public String boundary() {
        return boundary;
    }

    public String contentType() {
        return "multipart/form-data; boundary=" + boundary;
    }

    /**
     * 텍스트 필드 추가.
     * @param name 필드명
     * @param value 값 (null 가능)
     * @return 자기 자신 (체이닝)
     */
    public MultipartBodyPublisher addText(String name, String value) {
        if (name == null) throw new IllegalArgumentException("name is required");
        parts.add(new Part(name, null, null,
                value == null ? new byte[0] : value.getBytes(StandardCharsets.UTF_8)));
        return this;
    }

    /**
     * 파일 필드 추가. {@code contentType}이 null이면 application/octet-stream.
     * @param name 필드명
     * @param file 파일 경로
     * @param contentType MIME 타입 (null 가능)
     * @return 자기 자신 (체이닝)
     */
    public MultipartBodyPublisher addFile(String name, Path file, String contentType) {
        if (name == null) throw new IllegalArgumentException("name is required");
        if (file == null) throw new IllegalArgumentException("file is required");
        try {
            byte[] data = Files.readAllBytes(file);
            String filename = file.getFileName() != null ? file.getFileName().toString() : "file";
            String ct = contentType != null ? contentType : "application/octet-stream";
            parts.add(new Part(name, filename, ct, data));
        } catch (IOException e) {
            throw new RuntimeException("파일 읽기 실패: " + file + " — " + e.getMessage(), e);
        }
        return this;
    }

    /**
     * 메모리 바이트 배열을 파일 part로 추가.
     * @param name 필드명
     * @param filename 파일명 (null 가능)
     * @param data 바이트 데이터
     * @param contentType MIME 타입 (null 가능)
     * @return 자기 자신 (체이닝)
     */
    public MultipartBodyPublisher addBytes(String name, String filename, byte[] data, String contentType) {
        if (name == null) throw new IllegalArgumentException("name is required");
        String ct = contentType != null ? contentType : "application/octet-stream";
        parts.add(new Part(name, filename, ct, data == null ? new byte[0] : data));
        return this;
    }

    /**
     * 누적된 part들을 RFC 7578 형식으로 직렬화.
     * @return 직렬화된 본문 바이트
     */
    public byte[] build() {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        try {
            for (Part p : parts) {
                buf.write(("--" + boundary).getBytes(StandardCharsets.UTF_8));
                buf.write(CRLF);

                StringBuilder cd = new StringBuilder("Content-Disposition: form-data; name=\"")
                        .append(escapeQuoted(p.name)).append("\"");
                if (p.filename != null) {
                    cd.append("; filename=\"").append(escapeQuoted(p.filename)).append("\"");
                }
                buf.write(cd.toString().getBytes(StandardCharsets.UTF_8));
                buf.write(CRLF);

                if (p.contentType != null) {
                    buf.write(("Content-Type: " + p.contentType).getBytes(StandardCharsets.UTF_8));
                    buf.write(CRLF);
                }
                buf.write(CRLF);
                buf.write(p.data);
                buf.write(CRLF);
            }
            buf.write(("--" + boundary + "--").getBytes(StandardCharsets.UTF_8));
            buf.write(CRLF);
        } catch (IOException e) {
            // ByteArrayOutputStream에는 IOException이 실제로 안 던져지지만 시그니처상 catch
            throw new RuntimeException("multipart 본문 직렬화 실패", e);
        }
        return buf.toByteArray();
    }

    /**
     * {@link HttpRequest.BodyPublisher}로 변환.
     * @return body publisher
     */
    public HttpRequest.BodyPublisher publisher() {
        return HttpRequest.BodyPublishers.ofByteArray(build());
    }

    /**
     * 파트 개수 (테스트/디버깅용).
     * @return 추가된 part 개수
     */
    public int partCount() {
        return parts.size();
    }

    private static String escapeQuoted(String s) {
        // RFC 7578: filename에 큰따옴표/CR/LF 금지. 간단 이스케이프.
        return s.replace("\"", "\\\"").replace("\r", "").replace("\n", "");
    }

    private static final class Part {
        final String name;
        final String filename; // null이면 text part
        final String contentType; // null이면 헤더 미출력
        final byte[] data;

        Part(String name, String filename, String contentType, byte[] data) {
            this.name = name;
            this.filename = filename;
            this.contentType = contentType;
            this.data = data;
        }
    }
}
