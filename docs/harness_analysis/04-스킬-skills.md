# 04. 스킬 (Skills)

`.claude/skills/*.md` — 여러 에이전트가 **공유**하는 도메인 지식 모듈입니다.
에이전트 파일이 `@../skills/xxx.md` 로 import 해서 사용합니다.

지식을 한 곳에 모아두면 중복이 없고, 규칙을 한 번만 고치면 모든 에이전트에 반영됩니다.

## 어떤 에이전트가 어떤 스킬을 쓰나

| 스킬 | 사용하는 에이전트 |
|------|------------------|
| `tdd.md` | fullstack-dev, quant-dev, test-engineer |
| `ddd.md` | service-planner, quant-planner, fullstack-dev |
| `clean-architecture.md` | service-planner, fullstack-dev, code-reviewer |
| `api-design.md` | service-planner |
| `system-design.md` | service-planner, quant-planner |
| `quant.md` | quant-planner, quant-dev |
| `review.md` | code-reviewer |

---

## 1. TDD (`tdd.md`) — 가장 길고 핵심적인 스킬

> 철학: Classist(Chicago) + Khorikov 원칙. "테스트와 구현은 행동(behavior)으로만 연결된다."

**Red-Green-Refactor 사이클** (단계 건너뛰기 금지)
1. **Red**: 실패하는 테스트 먼저 → 실패를 직접 확인 (빨강 안 보고 Green 진행 금지)
2. **Green**: 통과시킬 최소 구현
3. **Refactor**: 중복 제거·가독성 개선 → 다시 돌려 여전히 통과 확인

**Mock 경계 (가장 중요한 규칙)**
- **Mock 해도 되는 것** = *제어 불가능한 외부 의존성*만: 외부 HTTP API(KIS, FRED), 시스템 시계, 난수.
- **절대 Mock 금지** = *관리되는 의존성·내부 협력자*: DB(PostgreSQL)·Redis는 **Testcontainers**로 진짜 연결, 도메인 객체·내부 서비스는 진짜 인스턴스.
  - 예: `PositionQueryService` 테스트하려고 `PositionRepository`를 Mock → **금지**.

**Stub vs Mock**: Stub은 데이터 공급(검증 대상 아님 → `verify()` 금지), Mock은 외부 부수효과 검증용.

**테스트 계층**: 통합(최우선, 애플리케이션 서비스 + 실제 DB/Redis) > 단위(도메인 엔티티·순수 함수) > 회귀(과거 장애당 1개).

**네이밍**: 메서드명이 아니라 *관찰 가능한 행동*으로. 예) `order_rejected_when_quantity_exceeds_available_balance()`.

**금지 사항**: 테스트보다 프로덕션 코드 먼저, Red 확인 없이 Green, private 메서드 직접 테스트, 도메인 객체 Mock, Stub에 `verify()`, DB/Redis를 Mock으로 대체, 서비스 검증용 E2E 추가.

---

## 2. DDD (`ddd.md`) — 도메인 주도 설계

**핵심 개념**: Entity(ID로 식별, 도메인 메서드로만 변경) / Value Object(값으로 식별, 불변) / Aggregate(일관성 경계, 모든 변경은 Root 통해) / Repository(Aggregate Root당 1개) / Domain Event(과거형, 불변) / Bounded Context / ACL.

**구현 규칙**
- 도메인 계층은 **프레임워크 의존성 0** — 순수 비즈니스 로직만.
- 모든 상태 변경은 도메인 메서드로 (`order.cancel()`, `account.lockDeposit(amount)`).
- **내부 Entity용 Repository 만들지 않기** — Aggregate Root만.
- Aggregate 간 참조는 **ID로만** (`Execution.orderId: Long`, 객체 참조 금지).
- JPA `@Entity` ≠ DDD Entity — ORM 어노테이션이 도메인 계층에 나오면 안 됨.

**프로젝트 특화**
- `trading-api` ↔ `collector-api`: 별도 Bounded Context, 시세 경계에 ACL 적용.
- `trading-api` Aggregate Root: `Order`, `Account`, `Position`, `Strategy`.

---

## 3. Clean Architecture (`clean-architecture.md`)

**계층 구조**: `presentation → application → domain ← infrastructure`

- **domain**: 순수 비즈니스 규칙. 프레임워크/DB 의존 없음. 가장 안정적.
- **application**: UseCase 오케스트레이션. **`@Transactional`은 오직 여기에만**.
- **infrastructure**: DB/Redis/외부 API 어댑터. 도메인 인터페이스(Port) 구현.
- **presentation**: HTTP 변환·입력 검증. 도메인 로직 없음.

**핵심 규칙**: 의존성은 항상 안쪽으로 / DTO와 Entity 혼용 금지(경계에서 변환) / 생성자 주입만(`@Autowired` 필드 주입 금지) / 매직넘버 금지 / N+1 금지 / **금액·수량은 `BigDecimal`만**.

---

## 4. API Design (`api-design.md`) — REST 설계

- **URL**: 리소스 중심 명사·복수형·kebab-case (`POST /orders`, `/accounts/{id}/orders`). 동사 금지.
- **HTTP 메서드**: GET(읽기·멱등) / POST(생성) / PUT(전체수정) / PATCH(부분) / DELETE.
- **상태 코드**: 201(생성+Location), 204(삭제), 400(검증), 409(비즈니스 규칙 위반: 잔고부족·중복).
- **에러 포맷**: `{ code, message, detail }`.
- **규칙**: 페이지네이션은 커서 기반 / 날짜는 ISO 8601 / **금액은 문자열·정수, float 금지**.
- **내부 API**: `/api/internal/{resource}` — 외부 노출 금지, 별도 인증.

---

## 5. System Design (`system-design.md`)

**설계 원칙**: 단순함 우선 / 점진적 확장(과설계 금지) / 장애 격리 / **금융은 가용성보다 일관성**.

**프로젝트 아키텍처**
```
KIS WebSocket → collector-api → Redis Pub/Sub → trading-api
                             → Redis Hash(시세 캐시) / PostgreSQL(이력)
collector-api → quant-worker(HTTP 트리거) / PostgreSQL(OHLCV)
trading-api  → PostgreSQL(주문·계좌·포지션) / Redis(시세 구독)
```

**서비스 간 통신**: 동기=HTTP REST / 비동기=Redis Pub/Sub(시세) / 계약=API 스펙만(**서비스 간 직접 DB 접근 금지**).
**외부 의존성**: 모든 외부 호출에 타임아웃 필수 / 재시도는 멱등 요청만(주문 POST 재시도 금지) / 서킷브레이커.
**데이터**: 서비스별 자기 스키마만 / 마이그레이션은 Flyway·하위호환만 / 금액은 `NUMERIC`, float 금지.

---

## 6. Quant (`quant.md`) — 퀀트 개발 원칙

**데이터 무결성**
- **룩어헤드 편향 절대 금지**: 시점 T 데이터는 T 이전 정보만 사용.
- **생존 편향**: 현재 상장 종목만 쓰면 과대평가 → 상장폐지 종목 포함.
- **기업행위**: 배당·액면분할은 수정주가 사용.
- **결측값**: forward-fill 금지(마지막 값 전파 = 룩어헤드).

**백테스팅**: Train/Test 분리(테스트 기간은 최종 1회만) / Walk-forward 검증 / **거래비용 항상 포함**(슬리피지+수수료+세금) / 리밸런싱일 시가 체결(종가 체결 = 룩어헤드).

**구현**: 벡터화 우선(루프 금지) / 재현성(시드 고정) / **공식 기호와 코드 변수명 일치** / 팩터 계산 단위 테스트.

**리스크 지표**: Sharpe / MDD / VaR(95·99%) / Calmar.

**금지**: 미래 데이터 사용 / 거래비용 없는 백테스트 / 단일 기간 검증 / 과최적화 / **금액에 float·double (→ `Decimal` 사용)**.

---

## 7. Review (`review.md`) — 코드 리뷰 체크리스트

리뷰어가 따르는 체크리스트. 출력은 **🔴 반드시 수정 / 🟡 권장 / 🟢 확인 완료**.

- **아키텍처**: 계층 의존 방향 준수 / 도메인에 프레임워크 의존 없음 / `@Transactional`은 application만.
- **코드 품질**: SRP/OCP/DIP / 중복 없음 / 매직넘버 없음 / 주석처리된 코드 없음.
- **신뢰성**: 금액·수량 `BigDecimal` / N+1 없음 / 외부 호출 타임아웃 / 예외 처리.
- **보안**: 시크릿·토큰 평문 로깅/저장 금지 / SQL Injection·XSS 방어 / 입력 검증.
- **테스트 품질**: 통합 테스트는 ApplicationService 직접 호출(MockMvc·HTTP → 🔴) / DB·Redis Mock → 🔴 / 관찰 가능한 행동만 단언 / 서비스 검증용 E2E 추가 → 🔴.
- **퀀트 로직**: 공식과 코드 일치 / 룩어헤드 없음 / 거래비용 반영 / 재현성.
