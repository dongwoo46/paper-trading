@../../AGENTS.md

---

# trading-api 서비스 규칙

> 공통 규칙은 루트 `AGENTS.md`를 참조한다. 이 파일은 trading-api 전용 규칙이다.

## 서비스 개요

- **역할**: 모의투자/실거래 핵심 도메인 — 주문·체결·계좌·포트폴리오·정산·전략 실행
- **언어/기술**: Java 21 + Spring Boot 3.x (Spring MVC) + JPA + PostgreSQL + Redis
- **지원 모드**: 로컬 모의투자 / KIS 연동 모의투자 / 실시간 거래
- **검증 명령**: `./gradlew compileJava`

## 패키지 구조

```
com.papertrading.api
├── domain/
│   ├── account/      # 계좌, 예수금, 잔고
│   ├── order/        # 주문, 체결
│   ├── portfolio/    # 포트폴리오, 보유 종목
│   ├── settlement/   # 정산, 실현손익
│   └── strategy/     # 자동매매 전략
├── application/
│   ├── account/      # 계좌 생성/조회 UseCase
│   ├── order/        # 주문 생성/체결 UseCase
│   ├── portfolio/    # 포트폴리오 조회 UseCase
│   ├── settlement/   # 정산 UseCase
│   └── strategy/     # 전략 등록/실행 UseCase
├── infrastructure/
│   ├── persistence/  # JPA Repository 구현체
│   ├── redis/        # Redis 시세 조회
│   └── external/     # KIS 연동 어댑터
└── interfaces/
    ├── rest/         # REST Controller
    └── dto/          # Request/Response DTO
```

## 도메인 설계 규칙

- `Account`: 계좌 정보, 예수금, 거래 모드(LOCAL/KIS_PAPER/KIS_LIVE)
- `Order`: 주문 (시장가/지정가, 매수/매도), 상태 머신으로 관리
- `Execution`: 체결 결과 (주문 1건에 1~N개의 체결)
- `Position`: 보유 종목 (수량, 평균단가, 평가손익)
- `Settlement`: 정산 (실현손익, 거래비용)

## 금융 계산 규칙

- 금액·수량·수익률 계산에는 **`BigDecimal`만 사용** (`double`, `float` 절대 금지)
- 평균단가 계산: `(기존수량 × 기존평균단가 + 매수수량 × 매수단가) / 총수량`
- 수익률 계산: `(현재가 - 평균단가) / 평균단가 × 100`
- 거래비용(수수료, 세금) 계산은 별도 정책 클래스로 분리한다

## 트랜잭션 / 동시성 규칙

- 트랜잭션 경계는 Application(UseCase) 레이어에서 관리한다
- 주문 상태 변경은 낙관적 락(Optimistic Lock, `@Version`) 적용
- 예수금 차감/반환은 비관적 락(Pessimistic Lock) 적용
- 체결 처리는 반드시 단일 트랜잭션 내에서 주문+계좌+포지션 모두 반영

## 외부 연동 규칙

- KIS 연동 어댑터는 `infrastructure/external/kis/` 패키지에 격리
- 로컬 모의투자는 KIS 어댑터 없이 내부 체결 엔진만 사용
- 거래 모드(LOCAL/KIS_PAPER/KIS_LIVE)별 어댑터를 Strategy 패턴으로 교체 가능하게 구성

## .agents 폴더 관리

- 기능 완료 시 `.agents/feature/{날짜}-{기능명}.md` 생성
- API 개발 완료 시 `.agents/feature/api.md`에 API 명세서 작성
- `.agents/feature/README.md`는 최신 API 목록만 유지
- 버그/장애 발생 시 `.agents/rule/` 에 재발 방지 기록