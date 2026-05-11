# BookPrintAPI Java SDK

포토북 생성/주문 API의 Java SDK입니다. server v1 (2026-04-28 기준) 응답 표준 — 6필드 응답 / 평탄화 list / `pageMeta` / `OrderStatus` 문자열 enum / 24종 `errorCode` 카탈로그를 v0.1.0에서 한 번에 적용.

> **Status**: v0.1.0 준비 중 (private 단계). 외부 publish 채널은 release 시점에 결정.
> **Java**: 11+ 필수 (Java 17 호환 검증).
> **외부 의존성**: Jackson `jackson-databind` 2.16.x. HTTP는 JDK 내장 `java.net.http.HttpClient`로 의존성 0개.

## 빠른 시작

```java
import com.sweetbook.bookprintapi.SweetbookClient;

SweetbookClient client = SweetbookClient.fromEnv();
// 또는
SweetbookClient client = SweetbookClient.builder()
    .apiKey("SB...")
    .environment("sandbox")
    .build();
```

환경변수: `BOOKPRINT_API_KEY` / `BOOKPRINT_ENV` (`sandbox` / `live`) / `BOOKPRINT_BASE_URL` (직접 URL).

## 주요 사용 패턴

### errorCode 분기 (24종 카탈로그)

```java
import com.sweetbook.bookprintapi.BookPrintApiException;
import com.sweetbook.bookprintapi.ErrorCodes;
import com.sweetbook.bookprintapi.FieldError;

try {
    client.orders.create(items, shipping, null);
} catch (BookPrintApiException e) {
    if (ErrorCodes.INSUFFICIENT_CREDIT.equals(e.errorCode())) {
        long required = ((Number) e.data().getOrDefault("required", 0)).longValue();
        long balance  = ((Number) e.data().getOrDefault("balance",  0)).longValue();
        showCreditTopup(required, balance);
    } else if (ErrorCodes.VALIDATION_FAILED.equals(e.errorCode())) {
        for (FieldError fe : e.fieldErrors()) {
            highlight(fe.field(), fe.message());
        }
    } else {
        showToast(e.userMessage());  // errors[0] 우선, message 폴백
    }
}
```

전체 24종 상수: `ErrorCodes.VALIDATION_FAILED`, `INSUFFICIENT_PAGES`, `FINALIZE_PREREQ_UNMET`, `INSUFFICIENT_CREDIT`, `ENV_MISMATCH`, `ORDER_TRANSITION_INVALID`, `PDF_NOT_GENERATED`, … (자세한 목록은 `ErrorCodes.java`).

### OrderStatus (Java enum)

```java
import com.sweetbook.bookprintapi.OrderStatus;

if (OrderStatus.PAID.name().equals(order.get("orderStatus"))) { ... }

// 목록 필터 — enum / 문자열 / 숫자 모두 허용
client.orders.list(OrderStatus.PAID);
client.orders.list("PAID", null, null, null, null);

// 서버가 새 상태를 추가했을 때 안전 처리
OrderStatus.tryParse(unknownStatus).ifPresent(s -> { ... });
```

### pageMeta (페이지 검증)

```java
import com.sweetbook.bookprintapi.PageMeta;
import com.sweetbook.bookprintapi.ResponseParser;

JsonNode resp = client.contents.insert(bookUid, templateUid, params);
PageMeta meta = new ResponseParser(resp).getPageMeta();
if (!meta.isValid()) {
    int need = meta.pageMin() - meta.currentPageCount();
    System.out.println(need + "페이지 더 필요");
}
```

### 평탄화 응답 (list)

`books.list` / `orders.list` / `templates.list` / `photos.list`는 `ListResult<T>`로 정규화되어 반환:

```java
ListResult<Map<String, Object>> result = client.books.list("finalized");
result.items();        // List<Map<String, Object>>
result.pagination();   // Pagination (total/limit/offset/hasMore)
```

신/구 응답 shape (`data: [...]` + 최상위 `pagination` 또는 `data: { books:[...], pagination:{} }`) 모두 흡수.

### Webhook 검증

```java
import com.sweetbook.bookprintapi.webhook.WebhookVerifier;

byte[] payload = exchange.getRequestBody().readAllBytes();
String sig = exchange.getRequestHeaders().getFirst("X-Webhook-Signature");
String ts  = exchange.getRequestHeaders().getFirst("X-Webhook-Timestamp");
if (!WebhookVerifier.verify(payload, sig, ts, secret)) {
    return badRequest();
}
```

`{timestamp}.{payload}` 의 HMAC-SHA256, 기본 5분 허용 오차, 상수 시간 비교.

## API 개요

| Sub-client | 메서드 |
|---|---|
| `client.books` | list / create / get / finalizeBook / delete |
| `client.orders` | estimate / create / list / get / cancel / updateShipping |
| `client.credits` | getBalance / transactions / sandboxCharge |
| `client.templates` | list / get / **getSchema** (JSON Schema draft-07) |
| `client.bookSpecs` | list / get |
| `client.photos` | upload / list / delete |
| `client.covers` | create / get / delete (multipart) |
| `client.contents` | insert / clear (multipart, breakBefore query) |
| `client.pdfs` | upload/replace/download cover/contents |

## examples

`examples/` 디렉토리 — 별도 source set으로 분리되어 SDK jar에 포함되지 않습니다. 자격증명은 `.env` 파일(또는 환경변수) 로 주입.

```bash
# 인증 정보 작성 (.env.example 참고)
cp .env.example .env
# .env 의 BOOKPRINT_API_KEY 등 채움

# 실행 — args 인자는 examples 클래스의 main()에 그대로 전달
./gradlew runSimpleBooks --args="list"
./gradlew runSimpleBooks --args="list --status finalized"
./gradlew runSimpleOrders --args="list"
./gradlew runWebhookReceiver
./gradlew runServerPipeline --args="<coverTpl> <contentTpl> cover.jpg page.jpg"
```

| 파일 | 내용 | Gradle task |
|---|---|---|
| `SimpleBooks` | 책 list/create/get/finalize/delete CLI | `runSimpleBooks` |
| `SimpleOrders` | 주문 estimate/create/list/get/cancel + errorCode 분기 시연 | `runSimpleOrders` |
| `ServerPipeline` | 책 1권 전체 파이프라인 (생성 → 표지 → 내지 → finalize → 견적, 저수준 sub-client) | `runServerPipeline` |
| `HelpersExample` | **SDK 헬퍼 시연** — `createBookFromTemplate` / `uploadPdfAndOrder` + 단계별 실패 분기 (cleanup 의사결정 포함) | `runHelpersExample` |
| `WebhookReceiver` | JDK HttpServer 기반 webhook 수신 + 서명 검증 | `runWebhookReceiver` |

## 빌드

```bash
./gradlew build                  # 컴파일 + 테스트 + javadoc + jar
./gradlew test                   # 단위 테스트
./gradlew integrationTest        # sandbox 실호출 테스트 (.env 의 BOOKPRINT_API_KEY 필요)
./gradlew compileExamplesJava    # examples 컴파일
./gradlew publishToMavenLocal    # 로컬 ~/.m2 에 publish (jitpack 도 동일 명령 사용)
```

## License

MIT
