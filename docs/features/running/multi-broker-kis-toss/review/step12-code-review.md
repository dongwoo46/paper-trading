# 코드 리뷰 결과 — multi-broker-kis-toss step 12

범위: `변경분` — 미커밋 변경 중 `backend/trading-api`(AccountSource·AccountResponse·BrokerAccountQueryService, 시험 3개, ktlint baseline), 루트 `docs/ADR.md` ADR-005.

## 0. 메타데이터

| 항목 | 값 |
|---|---|
| 리뷰 대상 | `domain/enums/AccountSource.kt` · `presentation/dto/account/AccountResponse.kt` · `application/account/BrokerAccountQueryService.kt` · 시험 `AccountSourceTest`(새) · `AccountResponseTest`(새) · `AccountControllerIntegrationTest` · `config/ktlint/baseline.xml` · `docs/ADR.md` |
| 기준 | 계획서 D25·D26·D29, §10.4 AE20 396행, 가정 1 |
| 프로필 | consumer |
| 리뷰어 | 메인 세션 인라인 리뷰(사람 결정 2026-11-21 «메인이 직접», 리뷰어 상한 초과) |
| 날짜 | 2026-11-21 |

## 1. 요약

| 항목 | 값 |
|---|---|
| 리뷰한 렌즈 | 정확성 · 회귀 시험 · 문서 |
| 작성자 시험 증거 | `./gradlew test --rerun --offline` exit 0, 새 xml 89개 1180건 실패·오류·건너뜀 0, 특성화 10건 통과, 린트 exit 0(baseline 항목 수 그대로, 줄 번호만 이동) |
| 발견 | 0개 |
| 한 줄 | 출처 산출이 전수 when 하나로 모였고 다섯 모드를 시험으로 고정. 응답 변경(UPBIT·KIS·TOSS 계좌 source)의 호출부는 trading-web src 0건 |

## 1-1. consumer 프로필

- 1. 규칙 준수 — 전수 `when`(else 없음), 주석 명사형 한 줄, 기본값 LOCAL 제거로 조용한 기본값 없음.
- 2. 사이드 이펙트 — `AccountResponse.source` 필수 인자가 되어 다른 생성처는 컴파일로 드러남(컴파일 통과). broker-balance 응답 source 는 Route 와 같은 값(TOSS_LIVE → TOSS, KIS 둘 → KIS)이라 동작 같음. AE20: trading-web src 에 계좌 응답 source 를 읽는 곳 0건(작성자 재검색).
- 3. 버그·장애 위험 — 돈·주문 경로 변경 없음. `account.tradingMode!!` 는 기존 그대로.

## 2. 발견 (findings)

| ID | file:line | 렌즈 | 근거 조항 | 무엇이 틀렸나 | 등급 | 영향 | 출처 | 회귀 | confidence | 구조 고침 | 재현 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| 지적 없음 | | | | | | | | | | | |

<!-- devkit:standard-list hex:7b22636861696e223a22646f63732f66656174757265732f72756e6e696e672f6d756c74692d62726f6b65722d6b69732d746f73732f7265766965772f7374657031322d636f64652d7265766965772e6d64222c22726f756e6473223a7b2231223a7b226361757365223a22505220eab8b0eca48020ebb88ceb9e9cecb998eba5bc20eca095ed9598eca78020ebaabbed95a8287472756e6b20ed8c90eca09520ebb688eab08029222c2268656164223a2263313135336465636334653830373530366130663733326361323638666333346666383733643330227d7d7d -->
