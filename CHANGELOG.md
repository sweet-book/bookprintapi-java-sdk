# Changelog

## 0.2.0 (2026-05-11) — 첫 public 릴리즈

bookprintapi-java-sdk 첫 외부 공개판. v0.1.0 으로 계획되어 있던 카탈로그/sub-client/헬퍼 + v0.2.0 으로 계획되어 있던 list envelope 호환 강화를 한 번에 묶어 첫 public 발행.

### Added — list 응답 envelope 통일 호환 강화

photobook-api commit [`6fbf346`](https://github.com/sweet-book/photobook-api-platform/commit/6fbf346) (2026-05-11) 의 list 응답 envelope 평탄화에 대응.

- **`ResponseParser.getList`** — 인식 키 19개로 확장: `orders` / `items` / `books` / `templates` / `photos` / `keys` / `accounts` / `memos` / `configs` / `deliveries` / `notifications` / `categories` / `transactions` / `targetTypes` / `daily` / `referrers` / `events` / `logs` / `bookSpecs`. 마지막 fallback 으로 `data` 객체의 첫 배열 자동 채택
- **`ResponseParser.getPagination`** — 구 photos 응답의 `data.totalCount` 를 `pagination.total` 로 자동 흡수
- **`Pagination.from`** — `hasNext` (v1 envelope 통일 후) / `hasMore` (구) 양쪽 흡수
- **`Pagination.fromTotal`** — `total` 만 알 때의 헬퍼 (`totalCount` 변환용)

### 변경된 envelope

**Before:**
```json
{ "success": true, "data": { "books": [...], "pagination": {...} } }
```

**After (commit 6fbf346 이후):**
```json
{
  "success": true,
  "data": [...],
  "pagination": { "total": 120, "limit": 20, "offset": 0, "hasNext": true }
}
```

`toListResult(Class<T>)` 가 두 envelope 모두에서 동일한 `ListResult<T>` 반환.

### Added — SDK 헬퍼 (다단계 플로우 한 호출)

### Added — SDK 헬퍼 (다단계 플로우 한 호출)

11_sdk_helpers_design.md v0.1 구현. R011-S01 (16p 책에 35+ API 호출) / C08 (다단계 실패 컨텍스트) 대응.

- **`client.helpers.createBookFromTemplate(req)`** — TEMPLATE 모드 책 + 표지 + 내지 N + finalize 한 호출. 반환 `BookBuildResult`
- **`client.helpers.uploadPdfAndOrder(req)`** — PDF_UPLOAD 모드 책 + PDF 2종 + finalize + 견적 + 주문 한 호출. 반환 `PdfOrderBuildResult`
- 빌더 패턴 — `CreateBookFromTemplateRequest.builder()` / `UploadPdfAndOrderRequest.builder()` / `ContentPage.builder()`
- 새 예외 `SweetbookHelperError` (RuntimeException) — `stage()` / `code()` / `bookUid()` / `orderUid()` / `partial()` / `getCause()` / `contentIndex()` / `userMessage()` 노출
- `HelperStage` enum 9종, `HelperErrorCodes` 상수 9종 (C03 확정 시 표준 errorCode로 매핑 예정)

#### 정책 (설계 §4)
- 자동 재시도 / 자동 롤백 안 함 — 파트너가 `partial` / `bookUid` 보고 명시적 결정
- 호출 전 클라이언트측 검증 (`bookSpecUid` / `coverTemplateUid` / `contents ≥ 1` / `shipping.recipientName` 등)

### Tests
- `HelpersClientTest` 16건 (Mockito 기반, `BooksClient` 등 sub-client mock)
- `ResponseParserTest` 14건 (신·구 envelope 5건 신규 포함)
- 누적 121 tests / 0 fail. `BUILD SUCCESSFUL`

### Highlights

- **Java 11 baseline**, JDK 내장 `java.net.http.HttpClient` 사용 — Jackson 외 의존성 0개
- Python/Node SDK v0.4.0 과 기능 동등 (envelope 통일 호환 레이어 포함)
- 6필드 응답 + 평탄화 list + `pageMeta` + `OrderStatus` enum + 24종 `errorCode` 카탈로그를 v0.2.0에서 한 번에 적용 (이행 단계 없음)

### Public API

- `SweetbookClient` (Builder + `fromEnv()`)
- 9개 sub-client: books / orders / credits / templates / bookSpecs / photos / covers / contents / pdfs
- 카탈로그: `ErrorCodes` (24종), `ConstraintTypes` (6종), `OrderStatus` (Java enum 12종)
- 응답 모델: `ResponseParser`, `ListResult<T>`, `Pagination`, `PageMeta`, `FieldError`
- 예외: `BookPrintApiException` (statusCode / errorCode / errors / fieldErrors / data, `userMessage()` / `fieldError(name)`)
- 웹훅: `WebhookVerifier.verify(...)` (HMAC-SHA256, 5분 tolerance 기본, 상수 시간 비교)
- HTTP 헬퍼: `HttpTransport`, `MultipartBodyPublisher` (RFC 7578 직접 구현)

### 테스트

- JUnit 5 단위 테스트 85건, 모두 통과
- mock HTTP server는 JDK 내장 `com.sun.net.httpserver.HttpServer` 사용 (외부 의존성 0)

### 알려진 제약

- DTO는 `Map<String, Object>` / `JsonNode` 노출 — 강타입 DTO는 v0.3.0에 점진 도입 예정
- multipart 본문은 메모리 누적 방식 — 매우 큰 파일(수백 MB) 업로드는 stream 방식으로 추후 교체 검토
- javadoc warning 다수 (missing `@return` 등) — 빌드는 통과. 점진 정리

### JitPack 좌표

```kotlin
repositories {
    maven("https://jitpack.io")
}
dependencies {
    implementation("com.github.sweet-book:bookprintapi-java-sdk:v0.2.0")
}
```
