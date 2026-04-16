# ADR — collector-api

## ADR-001: Spring MVC + WebFlux 혼용
**결정**: REST API는 Spring MVC, WebSocket/리액티브 스트림은 WebFlux 사용
**이유**: KIS WebSocket 연동에 리액티브 파이프라인이 필요하지만, 나머지 API는 MVC가 단순.
**트레이드오프**: 두 모델 혼용으로 블로킹/논블로킹 경계 관리 필요. 리액티브 파이프라인 내부 블로킹 호출 실수 위험.

## ADR-002: Redis를 시세 캐시 + Pub/Sub 버스 단일 운용
**결정**: 시세 저장(Hash)과 이벤트 발행(Pub/Sub) 모두 Redis로 처리
**이유**: Kafka 같은 별도 브로커 없이 Redis 하나로 해결. 초기 단계 인프라 최소화.
**트레이드오프**: 메시지 영속성 없음. Redis 재시작 시 미수신 시세 유실. 컨슈머 그룹 기능 없어 수평 확장 어려움.

## ADR-003: FRED 429 대응 — 지수 백오프 + AtomicBoolean
**결정**: 시리즈 간 요청 간격(700ms) + Retry-After 헤더 우선 사용 + 중복 스케줄 실행 방지
**이유**: FRED API 무료 티어 rate limit(429) 실제 발생. 재시도 없으면 수집 전체 실패.
**트레이드오프**: 수집 완료 시간이 시리즈 수에 비례해 증가. 429 지속 시 수동 설정값 조정 필요.

## ADR-004: 수요 기반 자동 구독 (Demand-driven)
**결정**: trading-api가 주문/포지션 생성 시 collector-api에 구독 요청, 종료 시 해제
**이유**: 불필요한 종목까지 상시 구독하면 KIS WebSocket 41슬롯 낭비.
**트레이드오프**: 내부 API 결합 발생. trading-api 장애 시 구독 해제 누락 가능성. 슬롯 관리 로직 복잡.

## ADR-005: H0STCNT0만 구독 (H0STASP0 제외)
**결정**: KIS WebSocket TR ID를 H0STCNT0(체결가) 하나만 사용
**이유**: H0STCNT0 메시지에 ASKP1/BIDP1(호가)이 이미 포함되어 있어 H0STASP0 별도 구독 불필요.
**트레이드오프**: 호가 변동을 실시간으로 추적하려면 H0STASP0 추가 필요. 현재는 체결가 기준 매칭으로 충분.
