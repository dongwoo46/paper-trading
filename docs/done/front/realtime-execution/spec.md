# Spec — front/realtime-execution

## 1. Feature Description

Operations dashboard에서 trading-api가 체결(Execution)을 완료하는 즉시 브라우저에 토스트 알림을 표시한다.
사용자는 주문 관리 페이지를 보지 않아도 "매수 체결: 삼성전자 10주 @ 75,000원" 형태의 알림으로 체결을 인지한다.

---

## 2. Transport Decision: SSE

### 후보 평가

| 항목 | SSE | WebSocket | Polling |
|------|-----|-----------|---------|
| 단방향 push | 최적 | 오버스펙 | 불가 |
| Spring Boot 지원 | SseEmitter (내장) | 별도 설정 필요 | trivial |
| 프론트 복잡도 | EventSource API | 커스텀 훅 복잡 | 높은 레이턴시 |
| 체결 후 응답 전송 | 필요 없음 | 불필요 | — |

**결정: SSE**
- 체결 이벤트는 단방향(서버 → 클라이언트)이므로 SSE가 가장 적합.
- Spring Boot의 `SseEmitter`로 추가 의존성 없이 구현 가능.
- `EventSource` 브라우저 API는 자동 재연결을 내장.
- 프론트에서 별도 WebSocket 라이브러리 불필요.

### 연결 방식
- 엔드포인트: `GET /api/v1/executions/stream`
- 인증: 현재 프로젝트에 인증 없음 → no auth header 필요
- Keep-alive: SseEmitter timeout = 5분, 프론트 EventSource 자동 재연결
- 연결 오류 시 UI에 에러 표시 없이 자동 재시도(브라우저 기본 동작)

---

## 3. Backend Dependency (신규 추가 필요)

trading-api에 SSE 엔드포인트가 현재 **존재하지 않는다**. 아래를 추가해야 한다.

### 3-1. SSE Emitter Registry (공유 레지스트리)

```
infrastructure/sse/ExecutionSseRegistry.kt
```
- `ConcurrentHashMap<String, SseEmitter>` 관리
- `register(clientId, emitter)` / `remove(clientId)` / `broadcast(event)`

### 3-2. SSE Controller

```
presentation/controller/ExecutionSseController.kt
GET /api/v1/executions/stream
```
- 클라이언트마다 `SseEmitter(5분 timeout)` 생성
- Registry에 등록, completion/timeout/error 시 제거

### 3-3. 체결 이벤트 발행 포인트

`ExecutionProcessor.fill()` 메서드 종료 직전에 `ExecutionSseRegistry.broadcast(event)` 호출.
트랜잭션 밖에서 호출해야 SSE I/O가 DB 트랜잭션을 지연시키지 않는다.
→ `@TransactionalEventListener(phase = AFTER_COMMIT)` 패턴을 사용한다.

구체적으로:
1. `ExecutionProcessor.fill()` 내부에서 `ApplicationEventPublisher.publishEvent(ExecutionFilledEvent(...))`
2. `ExecutionSseEventHandler` (`@EventListener(phase=AFTER_COMMIT)`)가 Registry.broadcast() 호출

---

## 4. Event Payload Shape

### 서버 → 클라이언트 SSE 이벤트

```
event: execution
data: <JSON>
```

```json
{
  "executionId": 42,
  "orderId": 7,
  "ticker": "005930",
  "tickerName": null,
  "side": "BUY",
  "quantity": "10",
  "price": "75000",
  "fee": "150",
  "currency": "KRW",
  "executedAt": "2026-04-30T08:30:00Z"
}
```

**참고**:
- `tickerName`은 현재 서버에서 KRX 종목명 매핑이 없으므로 `null` 허용. 프론트는 `tickerName ?? ticker` 표시.
- 모든 금액 필드는 `String` (BigDecimal → String 직렬화).
- `side`: `"BUY"` | `"SELL"` (OrderSide 열거형)

### 내부 도메인 이벤트 (Kotlin)

```kotlin
data class ExecutionFilledEvent(
  val executionId: Long,
  val orderId: Long,
  val ticker: String,
  val side: OrderSide,
  val quantity: BigDecimal,
  val price: BigDecimal,
  val fee: BigDecimal,
  val currency: String,
  val executedAt: Instant,
)
```

---

## 5. Toast Design

### 라이브러리 선택

`package.json`에 toast 라이브러리 없음. 추가 의존성 없이 프로젝트 스타일 변수를 그대로 사용하는 **자체 구현 토스트**를 채택한다.
- 이유: 기존 CSS 변수(`--status-success`, `--status-error`, `--text-primary` 등)와 통일성 유지.
- 외부 라이브러리 추가 시 팀 승인 필요(단일 개발자 프로젝트지만 ADR 원칙 유지).

### 스펙

| 항목 | 값 |
|------|----|
| 위치 | 화면 우상단 (fixed, top: 16px, right: 16px) |
| 애니메이션 | slide-in from right, fade-out |
| 표시 시간 | 4,500ms 자동 소멸 |
| 최대 동시 표시 | 5개 (초과 시 가장 오래된 것 제거) |
| 매수 아이콘 | ↑ (lucide `TrendingUp`) |
| 매도 아이콘 | ↓ (lucide `TrendingDown`) |

### 텍스트 형식

```
[매수 체결] 005930 10주 @ 75,000원  (currency=KRW)
[매도 체결] AAPL 5주 @ $152.30      (currency=USD)
```

---

## 6. Component Architecture (FSD)

```
shared/
  lib/
    sse/
      useExecutionSse.ts          # SSE 연결 커스텀 훅 (EventSource)
  ui/
    Toast/
      ToastContainer.tsx          # 토스트 목록 렌더링 (fixed position)
      ToastItem.tsx               # 단일 토스트 카드
      toast.css                   # 애니메이션 스타일
      index.ts                    # re-export

entities/
  execution/                      # 새 entity slice
    model/
      types.ts                    # ExecutionEvent 타입 정의

features/
  execution-toast/                # 새 feature slice
    model/
      useToastStore.ts            # zustand store (toasts 배열)
    ui/
      ExecutionToastProvider.tsx  # SSE 훅 구독 + 스토어 연결

App.tsx                           # <ExecutionToastProvider /> + <ToastContainer /> 마운트
```

### 의존성 방향 (FSD 준수)

```
App.tsx
  └─ features/execution-toast (ExecutionToastProvider)
       └─ shared/lib/sse/useExecutionSse
       └─ entities/execution/model/types
       └─ features/execution-toast/model/useToastStore
  └─ shared/ui/Toast (ToastContainer)
       └─ features/execution-toast/model/useToastStore  [읽기 전용]
```

---

## 7. Files to Create / Modify

### Backend (trading-api)

| 경로 | 변경 |
|------|------|
| `backend/trading-api/src/main/kotlin/com/papertrading/api/domain/event/ExecutionFilledEvent.kt` | 신규 |
| `backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/sse/ExecutionSseRegistry.kt` | 신규 |
| `backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/sse/ExecutionSseEventHandler.kt` | 신규 |
| `backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/controller/ExecutionSseController.kt` | 신규 |
| `backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/dto/sse/ExecutionSseEvent.kt` | 신규 |
| `backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/ExecutionProcessor.kt` | 수정 (publishEvent 추가) |

### Frontend (trading-web)

| 경로 | 변경 |
|------|------|
| `frontend/trading-web/src/entities/execution/model/types.ts` | 신규 |
| `frontend/trading-web/src/shared/lib/sse/useExecutionSse.ts` | 신규 |
| `frontend/trading-web/src/shared/ui/Toast/ToastContainer.tsx` | 신규 |
| `frontend/trading-web/src/shared/ui/Toast/ToastItem.tsx` | 신규 |
| `frontend/trading-web/src/shared/ui/Toast/toast.css` | 신규 |
| `frontend/trading-web/src/shared/ui/Toast/index.ts` | 신규 |
| `frontend/trading-web/src/features/execution-toast/model/useToastStore.ts` | 신규 |
| `frontend/trading-web/src/features/execution-toast/ui/ExecutionToastProvider.tsx` | 신규 |
| `frontend/trading-web/src/App.tsx` | 수정 (Provider + ToastContainer 마운트) |

---

## 8. CORS 고려사항

`ExecutionSseController`는 EventStream 응답을 반환하므로 Spring의 CORS 설정에
`/api/v1/executions/stream` 경로를 포함하거나 전역 CORS 설정이 이미 적용돼야 한다.
기존 컨트롤러에 CORS 설정이 없으면 별도 `WebMvcConfigurer` 빈 필요.
(현재 CORS 설정 파일 확인 후 step-2에서 처리)

---

## 9. 제약사항 및 결정 불포함 항목

- **인증**: 현재 시스템에 인증 없음 → SSE endpoint도 인증 없음. 향후 인증 추가 시 별도 phase.
- **종목명(tickerName)**: KRX 종목 마스터 없으므로 ticker 코드만 표시.
- **다중 계좌**: SSE는 계좌 필터 없이 모든 체결을 broadcast. 향후 계좌별 필터 필요 시 path param 추가.
