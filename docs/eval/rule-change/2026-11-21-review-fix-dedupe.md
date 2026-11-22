# 규칙 변경 증거 — 이사 뒤 합동 리뷰 고침(중복·영어판 정리)

## 무엇이 바뀌었나

규칙 이사(docs/adr/rule-migration-2026-11-21.md) finish 뒤 합동 리뷰가 낸 막는 지적 9건을 고쳤다(사람 지시 2026-11-21 «이 PR 에서 모두 고침»). 그중 순삭제 26줄은 셋이다. 1. 같은 파일에 한국어로 고쳐 쓴 판이 남은 영어 원문 사본, 2. 글자까지 같은 사본(하나를 남김), 3. 본문 없는 빈 제목. 지운 줄이 남긴 지시는 모두 같은 파일의 남은 줄에 그대로 있다.

## 실행 비교

돌리지 않았다. 지운 줄마다 같은 뜻의 줄이 같은 규칙 파일에 남아 있고(아래 대조), 규칙 파일의 적용 범위(pointer)는 같은 커밋에서 넓히기만 했다(backend-api.md signal 추가, quant.md·market-data.md 에 quant-worker 추가). 준수가 떨어질 경로는 «남은 판이 다른 뜻»뿐이라 줄별 대조로 닫았다. 실행 비교는 모델 세션을 띄워야 해서 사람 승인이 필요한데, 그 승인은 받지 않았다.

## 공식 근거 대조

근거 문서: devkit `docs/research/ai-doc-official-guidance.md` §1~§4.

- 이유(rationale) 보존: 지운 줄에 `Why:` 줄은 없었다(모두 옛 원문 사본과 빈 제목). 남은 조항의 Why 는 건드리지 않았다. 새로 더한 «이기는 쪽» 두 줄은 짧은 근거(사람 검증 조항, 현행 계층 조항)를 같은 줄에 담는다.
- 강조 서식: 지운 줄 가운데 굵게(`**Look-ahead…**`, `**Always include…**`) 4곳이 빠져 강조가 줄었다. 새로 쓴 줄에는 강조 서식을 더하지 않았다.
- 골격(헤딩·불릿): pointer 머리말, 헤딩, 불릿 한 줄 조항 짜임을 그대로 두고 같은 절 안 사본만 뺐다. 본문 없는 빈 제목 5개는 골격이 아니라 빈 자리라 지웠다.
- 분량 판정: 규칙 파일 합계 725줄(18개 파일), 입구 CLAUDE.md 26줄, AGENTS.md 44줄로 200줄 상한 아래다(`wc -l`). 이번 고침으로 규칙 줄은 줄기만 했다.

## 삭제 줄 대조

- 삭제: - Constructor injection only. Field injection via `@Autowired` is forbidden.
  → 잔존: rules/backend-api.md:62 «Spring 빈은 생성자로 주입하고 `@Autowired` 필드 주입을 쓰지 않는다» (같은 뜻의 한국어 판)

- 삭제: - `trading-api` domain package must be pure Kotlin — no Spring, no JPA.
  → 잔존: rules/backend-api.md:64 (글자까지 같은 사본 하나를 남김)

- 삭제: - Always use BigDecimal for monetary and quantity values (double/float is forbidden)
  → 잔존: rules/backend-api.md:60 «trading-api·collector-api 에서 금액과 수량은 `BigDecimal` 로 다루고 `Double`·`Float` 을 쓰지 않는다»

- 삭제: - Constructor injection only. Field injection via `@Autowired` is forbidden.
  → 잔존: rules/backend-api.md:62 (위와 같은 한국어 판, 같은 영어 사본의 둘째 사본)

- 삭제: - Constructor injection only — no `@Autowired` field injection
  → 잔존: rules/backend-api.md:62 (같은 뜻의 한국어 판, 옛 backend/CLAUDE.md 출처)

- 삭제: - Indexes: verify with query execution plan before adding.
  → 잔존: rules/data-access.md:50 (글자까지 같은 사본 하나를 남김)

- 삭제: - **Look-ahead bias is strictly forbidden**: data at time T must use only information available before T.
  → 잔존: rules/domains/quant.md:14 «백테스트와 신호 계산은 시점 T 에 T 이전에 알 수 있던 데이터만 쓴다(룩어헤드 편향 금지)»

- 삭제: - **Always include transaction costs**: slippage + commission + tax.
  → 잔존: rules/domains/quant.md:16 «백테스트 성과에는 슬리피지, 수수료, 세금을 늘 반영한다»

- 삭제: - Translate formulas to code (variable names must match formula notation; verify step-by-step).
  → 잔존: rules/domains/quant.md:11 «퀀트 수식을 코드로 옮길 때 변수 이름은 spec 의 수식 표기와 같게 짓고, 수식과 코드가 같은지 단계별로 대조한다»

- 삭제: - **Look-ahead bias is strictly forbidden**: data at time T must use only information available before T.
  → 잔존: rules/domains/quant.md:14 (위와 같은 한국어 판, 영어 사본의 둘째 사본)

- 삭제: - **Always include transaction costs**: slippage + commission + tax.
  → 잔존: rules/domains/quant.md:16 (위와 같은 한국어 판, 영어 사본의 둘째 사본)

- 삭제: ## 1. 컴포넌트
  → 잔존: rules/design-system.md (본문 없는 자리 표시 제목, 규칙 줄은 이사 때 design-system.md 로 옮겨짐)

- 삭제: ## 2. 토큰
  → 잔존: rules/design-system.md (본문 없는 자리 표시 제목)

- 삭제: ## 3. 금융 값 표시
  → 잔존: rules/design-system.md (본문 없는 자리 표시 제목)

- 삭제: ## 4. 표·영역
  → 잔존: rules/design-system.md (본문 없는 자리 표시 제목)

- 삭제: ## 5. 문구·접근성
  → 잔존: rules/design-system.md (본문 없는 자리 표시 제목)

- 삭제: - Never expose externally. Use separate authentication.
  → 잔존: rules/security-privacy-config.md:12 «`/api/internal/**` 서비스 간 API 는 외부에 노출하지 않고 외부 API 와 다른 인증을 건다»

- 삭제: - Never expose externally. Use separate authentication.
  → 잔존: rules/security-privacy-config.md:12 (위와 같은 한국어 판, 영어 사본의 둘째 사본)

- 삭제: - Enforce test policy: service/application business logic must be verified by integration tests, not E2E additions.
  → 잔존: rules/tests.md:26 «서비스·애플리케이션 비즈니스 로직은 통합 시험으로 검증하고, 그 검증을 위해 E2E 시험을 새로 더하지 않는다»

- 삭제: - Enforce test policy: service/application business logic must be verified by integration tests, not E2E additions.
  → 잔존: rules/tests.md:26 (위와 같은 한국어 판, 영어 사본의 둘째 사본)

- 삭제: - Changing production code for test convenience = forbidden.
  → 잔존: rules/tests.md:30 (글자까지 같은 사본 하나를 남김)

- 삭제: - Prefer an HTTP-level fake (e.g. WireMock) over an interface/trait mock for HTTP APIs.
  → 잔존: rules/tests.md:32 (글자까지 같은 사본 하나를 남김)

- 삭제: - Use `@SpringBootTest` + Testcontainers PostgreSQL instead.
  → 잔존: rules/tests.md:34 (글자까지 같은 사본 하나를 남김)

- 삭제: - 3. Never fix flakiness with `sleep()`, retry loops, or higher timeouts → fix the root cause.
  → 잔존: rules/tests.md:36 (글자까지 같은 사본 하나를 남김)

- 삭제: - New E2E test introduced for service verification that should be covered by integration tests.
  → 잔존: rules/tests.md:38 (글자까지 같은 사본 하나를 남김)

- 삭제: - `CLAUDE.md` 와 같은 규칙입니다. 먼저 `CLAUDE.md` 를 읽고, UI 작업은 `docs/UI_GUIDE.md` 를 적용합니다.
  → 잔존: rules/ui-common.md:28 «UI 작업은 `rules/design-system.md` 를 적용한다» (지운 docs/UI_GUIDE.md 를 새 자리로 고쳐 씀, «CLAUDE.md 와 같은 규칙» 은 지운 CODEX.md 의 자기 설명이라 대상이 없음)

## 재리뷰 뒤 고침 (2026-11-22)

- 삭제: > 옛 PRD 시점의 그림이다. 실제 패키지 이름과 계층 자리(Redis 리스너·컨트롤러 같은 진입점은 presentation)는 `rules/backend-api.md` 의 «계층과 포트» 절이 이긴다.
  → 잔존: rules/domains/market-data.md:20 «옛 PRD 시점의 그림이다. 지금 collector-api 패키지는 … 컨트롤러는 `presentation/`, 스케줄러는 `application/*/runtime/` 에 있다. 새 코드는 그림이 아니라 이 자리를 따른다» (가리키던 절이 trading-api 전용이라 collector-api 실제 자리로 고쳐 씀, 재리뷰 P3)
