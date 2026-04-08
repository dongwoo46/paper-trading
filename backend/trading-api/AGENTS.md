@../../AGENTS.md
@../AGENTS.md

---

# trading-api 서비스 규칙

핵심 도메인 서비스 — 주문·체결·계좌·포트폴리오·정산·전략 실행.
**Java 21 / Spring Boot 3.x / JPA / PostgreSQL / Redis**
검증: `./gradlew compileJava`

---

## 핵심 설계 결정: JPA Entity = 도메인 모델

**`@Entity` 클래스가 곧 도메인 모델이다. Pure Domain Object + Mapper 분리 구조 도입 금지.**

- `domain/model/`에 `@Entity` 클래스를 직접 배치.
- 비즈니스 규칙·상태 변경 메서드는 Entity 클래스 안에 작성.
- AI가 임의로 분리 구조를 제안하거나 도입하지 않는다.

---

## 패키지 구조

```
com.papertrading.api
├── domain/
│   ├── model/        # @Entity 클래스 (도메인 + DB 매핑 통합)
│   │   └── base/     # BaseTimeEntity, BaseAuditEntity
│   └── enums/        # AccountType, OrderStatus 등
├── application/
│   ├── account/      # 계좌 생성/조회 UseCase
│   ├── order/        # 주문 생성/체결 UseCase
│   ├── portfolio/    # 포트폴리오 조회 UseCase
│   ├── settlement/   # 정산 UseCase
│   └── strategy/     # 전략 등록/실행 UseCase
├── infrastructure/
│   ├── persistence/  # Spring Data JPA Repository
│   ├── redis/        # Redis 시세 조회
│   └── external/     # KIS/Upbit 연동 어댑터
└── interfaces/
    ├── rest/         # REST Controller
    └── dto/          # Request/Response DTO
```

---

## 도메인 모델

| Entity | 역할 | Aggregate Root |
|--------|------|:--------------:|
| `Account` | 계좌, 예수금, 거래 모드(LOCAL/KIS_PAPER/KIS_LIVE) | O |
| `Order` | 주문 (시장가/지정가, 매수/매도), 상태 머신 | O |
| `Execution` | 체결 결과 (주문 1건에 1~N개) | - |
| `Position` | 보유 종목 (수량, 평균단가, 평가손익) | O |
| `Settlement` | 정산 (실현손익, 거래비용) | - |

---

## JPA Entity 작성 규칙

```java
@Entity
@Table(name = "accounts")
class Account(
    @Column(name = "account_name", nullable = false)
    val accountName: String,          // 불변 필드: val

    @Column(name = "deposit", nullable = false)
    var deposit: BigDecimal,          // 변경 가능 필드: var

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
) : BaseAuditEntity() {

    fun lockDeposit(amount: BigDecimal) {  // 상태 변경은 반드시 메서드로
        require(amount <= availableDeposit) { "잠금 금액이 가용 예수금을 초과합니다." }
        availableDeposit -= amount
        lockedDeposit += amount
    }
}
```

- `data class` 사용 금지 — Hibernate proxy, equals/hashCode 오작동.
- 상태 변경은 반드시 도메인 메서드로만. 외부에서 `var` 필드 직접 수정 금지.
- `companion object { fun create(...) }` 팩토리 메서드 패턴 활용.

---

## 금융 계산 규칙

- 평균단가: `(기존수량 × 기존평균단가 + 매수수량 × 매수단가) / 총수량`
- 수익률: `(현재가 - 평균단가) / 평균단가 × 100`
- 거래비용(수수료, 세금) 계산은 별도 정책 클래스로 분리.
- 체결 처리: 단일 트랜잭션 내에서 주문 + 계좌 + 포지션 모두 반영.

---

## 외부 연동

- KIS 어댑터: `infrastructure/external/kis/` 패키지에 격리.
- 거래 모드별 어댑터를 Strategy 패턴으로 교체 가능하게 구성.
- 로컬 모의투자는 KIS 어댑터 없이 내부 체결 엔진만 사용.

---

## .agents 관리

- 기능 완료 시 `.agents/feature/{날짜}-{기능명}.md` 생성.
- API 개발 완료 시 `.agents/feature/README.md` 즉시 갱신.
- 버그/장애 발생 시 `.agents/rule/`에 재발 방지 기록 (핵심만 짧게).
