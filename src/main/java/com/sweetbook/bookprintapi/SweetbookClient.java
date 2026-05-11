package com.sweetbook.bookprintapi;

import com.sweetbook.bookprintapi.client.BookSpecsClient;
import com.sweetbook.bookprintapi.client.BooksClient;
import com.sweetbook.bookprintapi.client.ContentsClient;
import com.sweetbook.bookprintapi.client.CoversClient;
import com.sweetbook.bookprintapi.client.CreditsClient;
import com.sweetbook.bookprintapi.client.HelpersClient;
import com.sweetbook.bookprintapi.client.OrdersClient;
import com.sweetbook.bookprintapi.client.PdfsClient;
import com.sweetbook.bookprintapi.client.PhotosClient;
import com.sweetbook.bookprintapi.client.TemplatesClient;
import com.sweetbook.bookprintapi.http.HttpTransport;

import java.time.Duration;

/**
 * BookPrintAPI SDK 메인 클라이언트.
 *
 * <p>도메인별 sub-client를 public final 필드로 노출 — Python {@code Client} / Node
 * {@code SweetbookClient} 와 동일 사용성.
 *
 * <pre>{@code
 * SweetbookClient client = SweetbookClient.fromEnv();
 * // 또는
 * SweetbookClient client = SweetbookClient.builder()
 *     .apiKey("SB...")
 *     .environment("sandbox")
 *     .build();
 *
 * ListResult<Map<String,Object>> result = client.books.list(BookListParams.builder().status("finalized").build());
 * }</pre>
 */
public class SweetbookClient {

    private static final String DEFAULT_LIVE_URL = "https://api.sweetbook.com/v1";
    private static final String DEFAULT_SANDBOX_URL = "https://api-sandbox.sweetbook.com/v1";

    private final HttpTransport http;

    public final BooksClient books;
    public final OrdersClient orders;
    public final CreditsClient credits;
    public final TemplatesClient templates;
    public final BookSpecsClient bookSpecs;
    public final PhotosClient photos;
    public final CoversClient covers;
    public final ContentsClient contents;
    public final PdfsClient pdfs;
    public final HelpersClient helpers;

    private SweetbookClient(Builder b) {
        String resolvedUrl = b.baseUrl;
        if (resolvedUrl == null || resolvedUrl.isEmpty()) {
            String envUrl = System.getenv("BOOKPRINT_BASE_URL");
            if (envUrl != null && !envUrl.isEmpty()) {
                resolvedUrl = envUrl;
            } else {
                String env = b.environment != null ? b.environment
                        : System.getenv().getOrDefault("BOOKPRINT_ENV", "live");
                resolvedUrl = "sandbox".equalsIgnoreCase(env) ? DEFAULT_SANDBOX_URL : DEFAULT_LIVE_URL;
            }
        }

        String resolvedKey = b.apiKey != null ? b.apiKey : System.getenv("BOOKPRINT_API_KEY");
        if (resolvedKey == null || resolvedKey.isEmpty()) {
            throw new IllegalArgumentException(
                    "API Key가 필요합니다. builder().apiKey(...) 또는 BOOKPRINT_API_KEY 환경변수를 설정하세요.");
        }

        Duration timeout = b.timeout != null ? b.timeout : Duration.ofSeconds(60);
        int retries = b.maxRetries >= 0 ? b.maxRetries : 3;

        this.http = new HttpTransport(resolvedUrl, resolvedKey, timeout, retries);

        this.books = new BooksClient(http);
        this.orders = new OrdersClient(http);
        this.credits = new CreditsClient(http);
        this.templates = new TemplatesClient(http);
        this.bookSpecs = new BookSpecsClient(http);
        this.photos = new PhotosClient(http);
        this.covers = new CoversClient(http);
        this.contents = new ContentsClient(http);
        this.pdfs = new PdfsClient(http);
        this.helpers = new HelpersClient(books, covers, contents, orders, pdfs);
    }

    /**
     * 환경변수로 클라이언트 생성: {@code BOOKPRINT_API_KEY}, {@code BOOKPRINT_ENV} 또는 {@code BOOKPRINT_BASE_URL}.
     * @return 클라이언트 인스턴스
     */
    public static SweetbookClient fromEnv() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * 내부 사용. 테스트에서 transport를 mock으로 주입할 때 쓰기 위해 protected.
     * @return HTTP 전송 인스턴스
     */
    protected HttpTransport transport() {
        return http;
    }

    public static final class Builder {
        private String apiKey;
        private String environment;
        private String baseUrl;
        private Duration timeout;
        private int maxRetries = 3;

        private Builder() {}

        /**
         * API Key (예: "SB...") — 미지정 시 {@code BOOKPRINT_API_KEY} 환경변수.
         * @param apiKey API Key 문자열
         * @return 빌더 자기 자신
         */
        public Builder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        /**
         * "sandbox" 또는 "live" — 미지정 시 {@code BOOKPRINT_ENV} 환경변수, 그것도 없으면 "live".
         * @param environment 환경 이름
         * @return 빌더 자기 자신
         */
        public Builder environment(String environment) {
            this.environment = environment;
            return this;
        }

        /**
         * baseUrl 직접 지정 (environment보다 우선).
         * @param baseUrl API base URL
         * @return 빌더 자기 자신
         */
        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public Builder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        public Builder maxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }

        public SweetbookClient build() {
            return new SweetbookClient(this);
        }
    }
}
