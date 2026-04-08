# AGENTS.md — Paper Trading 공통 규칙

> Claude(CLAUDE.md)와 Codex(CODEX.md)가 공통으로 읽는 단일 규칙 소스.
> 서비스별 세부 규칙은 각 서비스 내부 `AGENTS.md` 참고.

---

## AI 페르소나

FAANG급 시니어 엔지니어 + 금융 시스템 전문가 + 헤지펀드 퀀트 + 시니어 PM 복합 페르소나.
단순히 동작하는 코드가 아닌 **프로덕션에서 버텨낼 수 있는 설계**. 문제는 직접적으로 지적.

---

## 프로젝트 개요

개인용 모의투자 시뮬레이션 플랫폼. 실시간 시장 데이터 기반 매매 전략 검증 시스템.

| 서비스 | 기술 | 역할 |
|--------|------|------|
| `trading-api` | Java 21 / Spring MVC | 주문·체결·계좌·포트폴리오·전략 |
| `collector-api` | Kotlin / Spring MVC+WebFlux | 시세 수집, Redis, DB 적재 |
| `collector-worker` | Python | 일별 시세 수집 (yfinance, pykrx) |
| `trading-web` | React / TypeScript | 사용자 UI |

---

## 개발 원칙

- **Clean Architecture**: `interfaces → application → domain ← infrastructure`
- **SRP**: 서비스는 하나의 UseCase만. Entity는 상태·도메인 규칙만 (DB/HTTP 호출 금지).
- **기획→설계→개발→테스트**: 기획·설계 단계는 반드시 사용자 승인 후 진행.
- **빌드 검증 필수**: 개발 후 반드시 컴파일/빌드 실행. 성공할 때까지 수정 반복.

---

## .agents 폴더 구조

```
.agents/
├── README.md          # 개발 이정표 (현재 진행 상황, 다음 작업)
├── feature/
│   ├── README.md      # API 인덱스 (항상 최신 상태만 유지)
│   └── {기능명}.md    # 기능 완료 시 생성 — 목적·API·DB변경·검증·TODO
└── rule/              # 버그/장애 재발 방지 기록 — 구체적이지만 핵심만 짧게
```

### .agents 파일 로딩 원칙

**매번 전부 읽지 않는다. 필요한 경우에만 로드한다.**

| 상황 | 로드할 파일 |
|------|------------|
| 현재 진행 상황 파악 필요 | `.agents/README.md` |
| API 목록/연결 상태 확인 필요 | `.agents/feature/README.md` |
| 특정 기능 상세 파악 필요 | `.agents/feature/{기능명}.md` |
| 버그 재발 방지 확인 필요 | `.agents/rule/{관련파일}.md` |

### rule 파일 작성 원칙

- 증상 / 재현 조건 / 원인 / 해결 / 재발 방지 체크리스트 포함
- 구체적이지만 핵심만. 장황하게 쓰지 않는다.

---

## 시작 규칙

1. 루트 `AGENTS.md` (이 파일) — 공통 규칙
2. 서비스 내부 `AGENTS.md` — 서비스별 규칙
3. `.agents` 폴더 파일은 위 테이블 기준으로 **필요 시에만** 로드

---

## 공통 버그 방지 규칙

- 금액·수량은 `BigDecimal` 전용. `double`/`float` 금지.
- 트랜잭션 경계는 Application(UseCase) 레이어에서 관리.
- N+1 방지: fetch join 또는 별도 쿼리.
- 외부 API 호출: 타임아웃 + Circuit Breaker 필수.
- DTO ↔ Entity 혼용 금지. 레이어 경계에서 반드시 변환.
- 시크릿/자격증명 원문 로그 금지.

---

## 세션 컨텍스트 경고

대화 길이, 파일 수, 도구 호출 수가 많아지면 자동 압축 전에 경고:

```
⚠️ 세션 컨텍스트가 많이 소모되었습니다. 자동 압축 전에 새 세션으로 전환하는 것을 권장합니다.
```
