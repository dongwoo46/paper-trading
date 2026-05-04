# Step 5: QA and Routing Policy Verification
Assigned agent: test-engineer

## Files to Read
- CODEX.md
- docs/ADR.md
- docs/phase/collector-api/subscription-routing/spec.md
- docs/phase/collector-api/subscription-routing/step-2.md
- docs/phase/collector-api/subscription-routing/step-3.md
- docs/phase/collector-api/subscription-routing/step-4.md
- backend/collector-api/src/test/kotlin/com/papertrading/collector/presentation
- backend/collector-api/src/test/kotlin/com/papertrading/collector/application

## Tasks
- 생성: `backend/collector-api/src/test/kotlin/com/papertrading/collector/application/subscriptions/SubscriptionRoutingServiceTest.kt`
  - 검증 시나리오:
    - STRATEGY가 MANUAL보다 WS 우선 배치.
    - WS 포화 시 STRATEGY 유입이 MANUAL eviction 또는 REST fallback으로 처리.
    - MANUAL은 WS/REST 포화 시 drop 처리.
    - WS 여유 발생 시 REST STRATEGY가 WS로 승격.
    - 멱등 키 재요청 처리(동일 payload=성공, 상이 payload=409).
- 생성: `backend/collector-api/src/test/kotlin/com/papertrading/collector/presentation/internal/InternalSubscriptionControllerRoutingTest.kt`
  - API 상태코드/응답 스키마 검증: 200/400/404/409.
- 수정: 필요한 기존 테스트 픽스처 파일(경로 명시 필수).
- 규칙:
  - 테스트는 정책 회귀를 잡는 데 집중하고 구현 상세에 과도 결합하지 않는다.
  - flaky 방지를 위해 시간/동시성 의존성은 deterministic fixture로 고정.

## Acceptance Criteria
```bash
cd backend/collector-api && ./gradlew test --tests "*SubscriptionRouting*" --no-daemon
cd backend/collector-api && ./gradlew compileKotlin --no-daemon
```

## Agent Return Protocol
When you finish, output a completion report in EXACTLY this format so the Orchestrator can evaluate:

---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <one or two sentences of what was done>
- Files modified: <list of paths relative to main repo root>
- Blockers: <none | description>
---
