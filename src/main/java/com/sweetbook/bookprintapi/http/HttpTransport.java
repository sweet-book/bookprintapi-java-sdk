package com.sweetbook.bookprintapi.http;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sweetbook.bookprintapi.BookPrintApiException;
import com.sweetbook.bookprintapi.SdkVersion;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * BookPrintAPI HTTP 전송 계층. {@code java.net.http.HttpClient} 래퍼.
 *
 * <p>책임:
 * <ul>
 *   <li>BaseURL + path 결합 + query string 빌딩</li>
 *   <li>{@code Authorization: Bearer <api-key>} 헤더 자동 주입</li>
 *   <li>JSON body 직렬화 (Jackson)</li>
 *   <li>응답 status + body → {@link JsonNode} 변환</li>
 *   <li>4xx / 5xx → {@link BookPrintApiException} 변환 throw</li>
 *   <li>429 / 5xx에 대해 지수 백오프 재시도 (default 3회)</li>
 * </ul>
 *
 * <p>본 클래스는 SDK 내부용. 사용자는 {@code SweetbookClient}를 통해 간접 사용.
 */
public class HttpTransport {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String baseUrl;
    private final String apiKey;
    private final Duration timeout;
    private final int maxRetries;
    private final HttpClient httpClient;

    public HttpTransport(String baseUrl, String apiKey, Duration timeout, int maxRetries) {
        if (baseUrl == null || baseUrl.isEmpty()) {
            throw new IllegalArgumentException("baseUrl is required");
        }
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalArgumentException("apiKey is required");
        }
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.apiKey = apiKey;
        this.timeout = timeout != null ? timeout : Duration.ofSeconds(60);
        this.maxRetries = Math.max(0, maxRetries);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public JsonNode get(String path, Map<String, ?> queryParams) {
        return execute(builder("GET", path, queryParams).GET().build());
    }

    public JsonNode post(String path, Object body) {
        return execute(builder("POST", path, null)
                .POST(jsonBody(body))
                .header("Content-Type", "application/json; charset=utf-8")
                .build());
    }

    public JsonNode patch(String path, Object body) {
        return execute(builder("PATCH", path, null)
                .method("PATCH", jsonBody(body))
                .header("Content-Type", "application/json; charset=utf-8")
                .build());
    }

    public JsonNode delete(String path) {
        return execute(builder("DELETE", path, null).DELETE().build());
    }

    /**
     * Multipart 등 BodyPublisher를 직접 만든 케이스용.
     * @param request 전송할 HTTP 요청
     * @return 응답 본문 JSON
     */
    public JsonNode send(HttpRequest request) {
        return execute(request);
    }

    /**
     * Multipart POST. {@link MultipartBodyPublisher}를 본문으로 전송.
     *
     * @param path 요청 경로
     * @param mp multipart body publisher
     * @param queryParams 쿼리스트링 (선택). 예: {@code breakBefore} for contents.
     * @return 응답 본문 JSON
     */
    public JsonNode postMultipart(String path, MultipartBodyPublisher mp, Map<String, ?> queryParams) {
        return execute(builder("POST", path, queryParams)
                .header("Content-Type", mp.contentType())
                .POST(mp.publisher())
                .build());
    }

    /**
     * Multipart PUT — PDF 교체 용도.
     * @param path 요청 경로
     * @param mp multipart body publisher
     * @return 응답 본문 JSON
     */
    public JsonNode putMultipart(String path, MultipartBodyPublisher mp) {
        return execute(builder("PUT", path, null)
                .header("Content-Type", mp.contentType())
                .PUT(mp.publisher())
                .build());
    }

    /**
     * Binary 다운로드 (PDF 등). 실패 시 BookPrintApiException, 성공 시 byte[] 반환.
     * @param path 요청 경로
     * @return 응답 바이너리
     */
    public byte[] downloadBinary(String path) {
        HttpRequest req = builder("GET", path, null).GET().build();
        int attempt = 0;
        while (true) {
            HttpResponse<byte[]> response;
            try {
                response = httpClient.send(req, HttpResponse.BodyHandlers.ofByteArray());
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("HTTP 요청이 중단되었습니다", ie);
            } catch (IOException ioe) {
                if (shouldRetry(attempt)) {
                    sleepBackoff(attempt);
                    attempt++;
                    continue;
                }
                throw new RuntimeException("네트워크 오류: " + ioe.getMessage(), ioe);
            }
            int status = response.statusCode();
            if (status >= 200 && status < 300) return response.body();
            if ((status == 429 || status >= 500) && shouldRetry(attempt)) {
                sleepBackoff(attempt);
                attempt++;
                continue;
            }
            // 에러 응답은 JSON일 가능성이 높지만 binary 컨텍스트라 best-effort 파싱
            JsonNode body = parseBody(response.body());
            throw com.sweetbook.bookprintapi.BookPrintApiException.fromResponse(status, body);
        }
    }

    /**
     * 외부에서 HttpRequest.Builder를 받기 위한 팩토리 (Phase 5 multipart에서 사용).
     * @param method HTTP 메서드
     * @param path 요청 경로
     * @param queryParams 쿼리스트링 (선택)
     * @return 헤더가 채워진 빌더
     */
    public HttpRequest.Builder builder(String method, String path, Map<String, ?> queryParams) {
        URI uri = buildUri(path, queryParams);
        HttpRequest.Builder b = HttpRequest.newBuilder(uri)
                .timeout(timeout)
                .header("Authorization", "Bearer " + apiKey)
                .header("Accept", "application/json")
                .header("User-Agent", "bookprintapi-java-sdk/" + SdkVersion.VERSION);
        return b;
    }

    private URI buildUri(String path, Map<String, ?> queryParams) {
        StringBuilder url = new StringBuilder(baseUrl);
        if (!path.startsWith("/")) url.append("/");
        url.append(path);
        if (queryParams != null && !queryParams.isEmpty()) {
            StringBuilder qs = new StringBuilder();
            for (Map.Entry<String, ?> e : queryParams.entrySet()) {
                if (e.getValue() == null) continue;
                if (qs.length() > 0) qs.append("&");
                qs.append(URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8));
                qs.append("=");
                qs.append(URLEncoder.encode(String.valueOf(e.getValue()), StandardCharsets.UTF_8));
            }
            if (qs.length() > 0) {
                url.append("?").append(qs);
            }
        }
        return URI.create(url.toString());
    }

    private static HttpRequest.BodyPublisher jsonBody(Object body) {
        try {
            byte[] bytes = body == null ? new byte[] {'{', '}'} : MAPPER.writeValueAsBytes(body);
            return HttpRequest.BodyPublishers.ofByteArray(bytes);
        } catch (Exception e) {
            throw new IllegalArgumentException("JSON 직렬화 실패: " + e.getMessage(), e);
        }
    }

    JsonNode execute(HttpRequest request) {
        int attempt = 0;
        while (true) {
            HttpResponse<byte[]> response;
            try {
                response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("HTTP 요청이 중단되었습니다", ie);
            } catch (IOException ioe) {
                if (shouldRetry(attempt)) {
                    sleepBackoff(attempt);
                    attempt++;
                    continue;
                }
                throw new RuntimeException("네트워크 오류: " + ioe.getMessage(), ioe);
            }

            int status = response.statusCode();
            JsonNode body = parseBody(response.body());

            if (status >= 200 && status < 300) {
                return body;
            }

            if ((status == 429 || status >= 500) && shouldRetry(attempt)) {
                sleepBackoff(attempt);
                attempt++;
                continue;
            }

            throw BookPrintApiException.fromResponse(status, body);
        }
    }

    private boolean shouldRetry(int attempt) {
        return attempt < maxRetries;
    }

    private void sleepBackoff(int attempt) {
        long base = 500L * (1L << attempt); // 500ms, 1s, 2s, 4s ...
        long jitter = ThreadLocalRandom.current().nextLong(0, 200);
        try {
            Thread.sleep(base + jitter);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static JsonNode parseBody(byte[] bytes) {
        if (bytes == null || bytes.length == 0) return null;
        try {
            return MAPPER.readTree(bytes);
        } catch (Exception e) {
            // 본문이 JSON이 아닌 경우 (예: HTML 에러 페이지)
            return null;
        }
    }

    /**
     * 디버깅 / 로깅용.
     * @return 정규화된 base URL
     */
    public String baseUrl() {
        return baseUrl;
    }

    /**
     * 신규 path 빌더 (DTO 타입 변환 등 공통 흐름에서 활용).
     * @param kvs key, value 가변 인자 쌍
     * @return 순서가 유지된 쿼리 맵
     */
    public static Map<String, Object> queryOf(Object... kvs) {
        if (kvs.length % 2 != 0) {
            throw new IllegalArgumentException("queryOf는 key,value 쌍을 요구합니다");
        }
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < kvs.length; i += 2) {
            Object k = kvs[i];
            Object v = kvs[i + 1];
            if (k == null) continue;
            map.put(k.toString(), v);
        }
        return map;
    }
}
