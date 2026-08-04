# 02. 커맨드 (Commands)

`.claude/commands/*.md` — 사용자가 `/명령어`로 호출하는 **진입점**입니다.

각 커맨드 파일은 3줄짜리 얇은 파일로, "담당 에이전트 한 줄 + 해당 에이전트 `@import`" 형태입니다.
즉 커맨드는 **라우터** 역할만 하고, 실제 행동 규칙은 에이전트 파일에 있습니다.

```markdown
# 예시: .claude/commands/build.md
담당 에이전트: Full Stack Developer — TDD 기반 구현

@../agents/fullstack-dev.md
```

---

## 커맨드 목록 (8개)

| 커맨드 | 담당 에이전트 | 한 줄 역할 |
|--------|--------------|-----------|
| `/orchestrate` | Orchestrator | **중앙 통제탑** — 전체 Phase·단계 실행을 조율 |
| `/plan` | Service Planner | 기능 명세 · API 스펙 · DB 설계 |
| `/plan-quant` | Quant Planner | 전략 · 알파 팩터 · 백테스팅 설계 |
| `/build` | Full Stack Developer | 애플리케이션 구현 (TDD 기반) |
| `/build-quant` | Quant Developer | 퀀트 전략 구현 · 백테스팅 엔진 |
| `/review` | Code Reviewer | 코드 · 보안 · 퀀트 수학 오류 검토 |
| `/test` | Test Engineer | QA 검증 · 스코프 테스트 · 커버리지 |
| `/cleanup` | Orchestrator (Cleanup) | 마무리 정리 + PR 생성 |

---

## 두 갈래: 일반 서비스 vs 퀀트

이 프로젝트는 **일반 백엔드/프론트 기능**과 **퀀트(금융 전략) 기능**을 구분합니다.
각 단계마다 두 종류의 커맨드가 짝을 이룹니다.

| 단계 | 일반 서비스 | 퀀트 |
|------|------------|------|
| 기획 | `/plan` (Service Planner) | `/plan-quant` (Quant Planner) |
| 구현 | `/build` (Full Stack Dev) | `/build-quant` (Quant Dev) |
| 리뷰/테스트/정리 | 공통: `/review`, `/test`, `/cleanup` | 공통 사용 |

퀀트 Phase에서는 오케스트레이터가 자동으로
`service-planner → quant-planner`, `fullstack-dev → quant-dev` 로 교체합니다.

---

## 사용 흐름

보통은 커맨드를 직접 하나씩 부르지 않고, **`/orchestrate` 하나만** 부릅니다.
그러면 오케스트레이터가 현재 상태(`docs/state.md`, `index.json`)를 읽고
적절한 다음 단계의 에이전트를 **알아서** 호출합니다.

```
사용자: /orchestrate
   → 오케스트레이터가 state.md 확인
   → "현재 step 2 (구현) 차례군" 판단
   → fullstack-dev 서브에이전트 호출 (= /build 와 동일 효과)
   → 결과 받아 docs 갱신 → 다음 step으로
```

개별 커맨드(`/plan`, `/build` 등)는 **특정 단계만 따로 돌리고 싶을 때** 수동으로 사용합니다.

> 📌 프로젝트 규칙(CLAUDE.md)상, 슬래시 커맨드 워크플로우는 반드시
> **Agent 도구(서브에이전트)로 위임**해서 실행합니다. (각 에이전트가 독립 컨텍스트로 작동)
