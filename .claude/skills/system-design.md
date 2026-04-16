Skill: System Design

## 설계 원칙

- 단순함 우선: 복잡한 솔루션보다 단순한 솔루션. 필요할 때 복잡하게.
- 점진적 확장: 오버엔지니어링 금지. 현재 규모에 맞는 설계.
- 장애 격리: 한 컴포넌트 장애가 전체로 전파되지 않도록.
- 데이터 일관성 > 가용성: 금융 도메인은 일관성 우선.

## 이 프로젝트 아키텍처

```
KIS WebSocket → collector-api → Redis Pub/Sub → trading-api
                             → Redis Hash (시세 캐시)
                             → PostgreSQL (히스토리)

collector-api → collector-worker (HTTP trigger)
             → PostgreSQL (OHLCV)

trading-api → PostgreSQL (주문/계좌/포지션)
           → Redis (시세 구독)
```

## 서비스 간 통신

- 동기: HTTP REST (명령/조회)
- 비동기: Redis Pub/Sub (시세 이벤트)
- 계약: API 스펙으로만. 공유 DB 직접 접근 금지.

## 데이터 설계

- 각 서비스는 자기 스키마만 소유 (다른 서비스 테이블 직접 조인 금지)
- 인덱스: 조회 패턴 기반으로 설계. 과도한 인덱스 금지.
- 마이그레이션: Flyway. 롤백 가능한 변경만. 컬럼 삭제는 2단계로.
- 금액/수량: NUMERIC(precision, scale) 타입. float 금지.

## 외부 의존성

- 타임아웃 필수: 모든 외부 API 호출에 명시
- 재시도: 멱등한 요청에만 (POST 주문 재시도 금지)
- Circuit Breaker: 외부 API 연속 실패 시 빠른 실패
- 폴링 vs 웹소켓: 실시간 시세는 웹소켓. 주기적 배치는 HTTP.

## 성능

- N+1 쿼리 금지 (fetch join / @EntityGraph / 별도 조회 후 조립)
- 캐시 전략: Redis는 시세처럼 자주 읽히고 빠르게 변하는 데이터에만
- 페이지네이션: 대용량 목록은 cursor 기반
- 인덱스: 쿼리 실행 계획 확인 후 추가

## 장애 처리

- 시크릿 로그 금지
- 의미 있는 에러 메시지 (내부 스택 트레이스 외부 노출 금지)
- 헬스체크 엔드포인트: /actuator/health
- 그레이스풀 셧다운: 진행 중인 요청 완료 후 종료
