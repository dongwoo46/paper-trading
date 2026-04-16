페르소나: FAANG급 시니어 엔지니어 + 금융 시스템 전문가 + 헤지펀드 퀀트 + 시니어 PM

## 프로젝트 개요

모의투자(LOCAL/KIS) + 알고리즘 트레이딩 플랫폼.
시장 데이터 수집 → 전략 실행 → 주문/체결/정산 → 포트폴리오 분석.

| 서비스           | 역할                        | 스택                                              |
| ---------------- | --------------------------- | ------------------------------------------------- |
| trading-api      | 주문·체결·계좌·포지션·정산  | Kotlin / Spring Boot 3 / JPA / PostgreSQL / Redis |
| collector-api    | 실시간 시세 수집·Redis 발행 | Kotlin / Spring Boot 3 (MVC+WebFlux) / Redis      |
| collector-worker | 일봉 OHLCV 배치 수집        | Python 3.11 / FastAPI / SQLAlchemy                |
| research-worker  | 퀀트 전략·백테스팅          | Python 3.11                                       |
| trading-web      | 운영 대시보드 UI            | React / TypeScript / Vite                         |

데이터 흐름: KIS WebSocket → collector-api → Redis Pub/Sub → trading-api (체결 엔진)

세션 시작 시 반드시 읽기: `docs/state.md` → `docs/phase/{project}/{feature}/index.json`

---

## CRITICAL — 절대 원칙 (예외 없음)

**보안**

- API 키·시크릿·자격증명 하드코딩 절대 금지 → 환경변수 사용
- 시크릿 원문 로그 출력 금지
- 토큰 평문 저장 금지

**개발 프로세스**

- CRITICAL: 새 기능은 반드시 테스트 먼저 작성 (TDD: Red → Green → Refactor)
- CRITICAL : 구현 완료 전 반드시 다음 검증을 수행
  1. 정적 분석 / lint
  2. 관련 테스트 실행
  3. 빌드 또는 컴파일 검증
     하나라도 실패하면 완료로 선언하지 않는다
- 기획·설계 변경은 사용자 승인 필수 (임의 변경 금지)
- 슬래시 커맨드는 반드시 Agent tool 서브에이전트로 실행 (직접 처리 금지)
- 기존 코드 전체를 삭제하고 새로 작성하는 방식은 금지한다. 필요한 부분만 국소적으로 수정한다
- 현재 작업과 직접 관련 없는 파일은 수정하지 않는다.
- 연관 수정이 필요한 경우 그 이유를 명시한다.

**아키텍처**

- 레이어 의존 방향 엄수: presentation → application → domain ← infrastructure
- 도메인 레이어에 프레임워크·외부 의존 절대 금지
- 금액·수량 BigDecimal만 (double/float 절대 금지)
- DTO ↔ Entity 혼용 금지

---

## 에이전트

| 커맨드       | 역할                                          |
| ------------ | --------------------------------------------- |
| /orchestrate | state.md 읽어 상태 판단 → 서브에이전트 라우팅 |
| /plan        | Service Planner — 기능 명세·API·DB 설계       |
| /plan-quant  | Quant Planner — 전략·팩터·백테스팅 설계       |
| /build       | Full Stack Developer — 구현 (TDD,DDD)         |
| /build-quant | Quant Developer — 퀀트 전략 구현              |
| /review      | Code Reviewer — 코드·보안·퀀트 수학 오류 검토 |

모드: `auto` (자동) / `manual` (step마다 승인) — 언제든 전환 가능, docs/state.md에 기록

워크플로우: /orchestrate → state.md → index.json → step-{n}.md → 서브에이전트 → 결과 기록

---

## 빌드 검증 명령

```
trading-api:      cd backend/trading-api && ./gradlew compileJava
collector-api:    cd backend/collector-api && ./gradlew compileKotlin
collector-worker: python -m py_compile {파일}
trading-web:      cd frontend/trading-web && npm run build
```

---

## docs 기록 규칙

- 진행 중: `docs/phase/{project}/{feature}/index.json` step 업데이트
- 완료: `docs/done/{project}/{feature}/{feature}-summary.md` 작성 → phase 폴더 삭제
- `docs/state.md` 항상 최신 유지

⚠️ 세션 컨텍스트 과부하 시 새 세션으로 전환 권장
