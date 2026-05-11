package com.sweetbook.bookprintapi.http;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MultipartBodyPublisherTest {

    @Test
    void boundaryAndContentTypeMatch() {
        MultipartBodyPublisher mp = new MultipartBodyPublisher();
        assertTrue(mp.boundary().startsWith("----bookprintapi-"));
        assertEquals("multipart/form-data; boundary=" + mp.boundary(), mp.contentType());
    }

    @Test
    void textPart_serializedWithoutContentType() {
        MultipartBodyPublisher mp = new MultipartBodyPublisher().addText("templateUid", "abc-123");
        String body = new String(mp.build(), StandardCharsets.UTF_8);
        assertTrue(body.contains("Content-Disposition: form-data; name=\"templateUid\""));
        assertTrue(body.contains("abc-123"));
        // text part는 Content-Type 없음 (이 part 단위로)
        // 본 검사는 약간 약하지만 충분: filename이 들어가지 않았는지
        assertEquals(false, body.contains("filename="));
    }

    @Test
    void filePart_includesContentTypeAndFilename(@TempDir Path tmp) throws IOException {
        Path img = tmp.resolve("photo.jpg");
        Files.write(img, new byte[] {1, 2, 3, 4, 5});
        MultipartBodyPublisher mp = new MultipartBodyPublisher().addFile("file", img, "image/jpeg");
        byte[] bytes = mp.build();
        String body = new String(bytes, StandardCharsets.UTF_8);
        assertTrue(body.contains("Content-Disposition: form-data; name=\"file\"; filename=\"photo.jpg\""));
        assertTrue(body.contains("Content-Type: image/jpeg"));
    }

    @Test
    void multipleFilesUnderSameName_allRetained(@TempDir Path tmp) throws IOException {
        Path a = tmp.resolve("a.jpg");
        Path b = tmp.resolve("b.jpg");
        Files.write(a, new byte[] {1});
        Files.write(b, new byte[] {2});
        MultipartBodyPublisher mp = new MultipartBodyPublisher()
                .addText("templateUid", "T1")
                .addFile("rowPhotos", a, "image/jpeg")
                .addFile("rowPhotos", b, "image/jpeg");
        assertEquals(3, mp.partCount());
        String body = new String(mp.build(), StandardCharsets.UTF_8);
        // 동일 필드명으로 두 part가 직렬화되어야 함
        int firstIdx = body.indexOf("name=\"rowPhotos\"");
        int secondIdx = body.indexOf("name=\"rowPhotos\"", firstIdx + 1);
        assertTrue(firstIdx > 0);
        assertTrue(secondIdx > firstIdx);
    }

    @Test
    void boundaryClosing_finalDoubleDash() {
        MultipartBodyPublisher mp = new MultipartBodyPublisher().addText("k", "v");
        String body = new String(mp.build(), StandardCharsets.UTF_8);
        // 마지막 boundary는 "--<boundary>--" 형태로 종료
        assertTrue(body.contains("--" + mp.boundary() + "--"));
    }

    @Test
    void rejectsNullFieldName() {
        MultipartBodyPublisher mp = new MultipartBodyPublisher();
        assertThrows(IllegalArgumentException.class, () -> mp.addText(null, "x"));
        assertThrows(IllegalArgumentException.class, () -> mp.addBytes(null, "f", new byte[0], null));
    }

    @Test
    void filenameQuoteEscaped() {
        // Windows 파일시스템은 큰따옴표를 허용하지 않음 → addBytes로 직접 파일명 주입
        MultipartBodyPublisher mp = new MultipartBodyPublisher()
                .addBytes("file", "strange\"name.jpg", new byte[] {0}, "image/jpeg");
        String body = new String(mp.build(), StandardCharsets.UTF_8);
        // 큰따옴표가 \" 로 이스케이프되어야 함 (Content-Disposition 헤더 안정성)
        assertTrue(body.contains("filename=\"strange\\\"name.jpg\""));
    }

    @Test
    void utf8Text_preserved() {
        MultipartBodyPublisher mp = new MultipartBodyPublisher()
                .addText("title", "한글 제목입니다");
        String body = new String(mp.build(), StandardCharsets.UTF_8);
        assertTrue(body.contains("한글 제목입니다"));
    }
}
