# 결정 이력 — 외부 연동·비동기 (paper-trading)
<!-- devkit:applies-to ^backend/(collector-api|trading-api)/src/main/.+\.kt$ -->

> 옛 `docs/ADR.md`·`backend/*/docs/ADR.md` 의 결정을 관심사별로 모았다(2026-11-21 규칙 이사). 아래 글은 원문 그대로다.

아래는 옛 `backend/collector-api/docs/ADR.md` 에서 온 결정이다.

# ADR — collector-api

## ADR-001: Spring MVC + WebFlux 혼용
**결정**: REST API는 Spring MVC, WebSocket/리액티브 스트림은 WebFlux 사용
**이유**: KIS WebSocket 연동에 리액티브 파이프라인이 필요하지만, 나머지 API는 MVC가 단순.
**트레이드오프**: 두 모델 혼용으로 블로킹/논블로킹 경계 관리 필요. 리액티브 파이프라인 내부 블로킹 호출 실수 위험.

## ADR-003: FRED 429 대응 — 지수 백오프 + AtomicBoolean
**결정**: 시리즈 간 요청 간격(700ms) + Retry-After 헤더 우선 사용 + 중복 스케줄 실행 방지
**이유**: FRED API 무료 티어 rate limit(429) 실제 발생. 재시도 없으면 수집 전체 실패.
**트레이드오프**: 수집 완료 시간이 시리즈 수에 비례해 증가. 429 지속 시 수동 설정값 조정 필요.

## ADR-005: H0STCNT0만 구독 (H0STASP0 제외)
**결정**: KIS WebSocket TR ID를 H0STCNT0(체결가) 하나만 사용
**이유**: H0STCNT0 메시지에 ASKP1/BIDP1(호가)이 이미 포함되어 있어 H0STASP0 별도 구독 불필요.
**트레이드오프**: 호가 변동을 실시간으로 추적하려면 H0STASP0 추가 필요. 현재는 체결가 기준 매칭으로 충분.

아래는 옛 `backend/trading-api/docs/ADR.md` 에서 온 결정이다.

## ADR-005: KIS 토큰 Redis 공유
**결정**: KIS access token을 Redis kis:token:{mode} 에 저장, collector-api와 공유
**이유**: KIS는 하루 발급 한도가 있어 서비스별로 각자 발급하면 한도 초과.
**트레이드오프**: Redis 장애 시 KIS 연동 전체 중단. 토큰 TTL 관리 로직이 한 서비스(collector-api)에 집중.
