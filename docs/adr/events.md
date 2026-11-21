# 결정 이력 — Redis 시세 캐시·Pub/Sub (paper-trading)
<!-- devkit:applies-to ^backend/(trading-api|collector-api)/src/main/.+(Redis|Quote|Listener|Publisher)\w*\.kt$ -->

> 옛 `docs/ADR.md`·`backend/*/docs/ADR.md` 의 결정을 관심사별로 모았다(2026-11-21 규칙 이사). 아래 글은 원문 그대로다.

아래는 옛 `docs/ADR.md` 에서 온 결정이다.

## ADR-003: Redis를 시세 캐시 + Pub/Sub 버스로 사용
**결정**: 실시간 시세는 Redis Hash(quote:{ticker}) 저장 + Redis Pub/Sub으로 trading-api에 전달
**이유**: 별도 메시지 브로커(Kafka 등) 없이 Redis 하나로 캐시와 이벤트 버스 역할 통합. 운영 복잡도 최소화.
**트레이드오프**: Redis 장애 시 시세 수신과 체결 엔진 동시 중단. 메시지 영속성 없음(재시작 시 유실 가능).

아래는 옛 `backend/collector-api/docs/ADR.md` 에서 온 결정이다.

## ADR-002: Redis를 시세 캐시 + Pub/Sub 버스 단일 운용
**결정**: 시세 저장(Hash)과 이벤트 발행(Pub/Sub) 모두 Redis로 처리
**이유**: Kafka 같은 별도 브로커 없이 Redis 하나로 해결. 초기 단계 인프라 최소화.
**트레이드오프**: 메시지 영속성 없음. Redis 재시작 시 미수신 시세 유실. 컨슈머 그룹 기능 없어 수평 확장 어려움.
