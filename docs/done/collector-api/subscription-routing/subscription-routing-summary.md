# subscription-routing Summary

## 완료 일자
- 2026-05-04

## 구현 요약
- 전략 종목(STRATEGY) 최우선 라우팅 정책 추가: WS 우선 배정, 포화 시 MANUAL eviction 우선 시도 후 REST fallback.
- 수동 종목(MANUAL) 라우팅 정책 추가: WS/REST 포화 시 drop 허용.
- WS 여유 발생 시 REST 전략 종목의 WS 자동 승격 규칙 반영.
- 수동 등록/해제, 전략 등록/해제, 즐겨찾기 토글/조회, 라우팅 상태 조회 내부 API 배선.
- 라우팅 요청/슬롯 상태 영속화 테이블 및 JPA 저장소 추가.
- 멱등 키 재요청 처리(동일 payload 재요청 허용, 상이 payload 충돌 처리) 보강.

## 검증 요약
- `./gradlew compileKotlin --no-daemon` 통과.
- `./gradlew test --tests "*InternalSubscriptionController*" --no-daemon` 통과.
- `./gradlew test --tests "*SubscriptionRoutingService*" --no-daemon` 통과.
- `./gradlew test --tests "*SubscriptionRouting*" --no-daemon` 통과.

## 메모
- PR 번호는 아직 미기재 상태로 TODO에 `#TBD`로 반영.