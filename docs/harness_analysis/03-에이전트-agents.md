# 03. 에이전트 (Agents)

`.claude/agents/*.md` — 각 역할의 **페르소나 · 책임 · 실행 규칙**을 정의한 본체입니다.
커맨드가 호출하면, 해당 에이전트가 독립된 컨텍스트로 시작해 일합니다.

총 7개 에이전트가 있고, **기획 → 구현 → 테스트 → 리뷰 → 정리** 파이프라인을 분담합니다.

```
오케스트레이터 (지휘자)
   ├─ 기획:  Service Planner / Quant Planner
   ├─ 구현:  Full Stack Dev / Quant Dev
   ├─ 검증:  Test Engineer (QA)
   ├─ 검토:  Code Reviewer
   └─ 마무리: Cleanup (오케스트레이터 본인이 직접)
```

모든 에이전트가 공통으로 지키는 규칙: **상태는 루트 `docs/`에만**, `.claude/**/docs`·`.codex/**/docs`는 무시.

---

## 1. Orchestrator — 중앙 통제탑 (`orchestrator.md`)

> 페르소나: 시니어 PM + Tech Lead. 가장 크고 중요한 에이전트(400줄).

전체 파이프라인의 **지휘자**입니다. 직접 코드를 짜지 않고, 상태를 읽어 다음에 누구를 부를지 결정합니다.

**핵심 책임**
- `docs/state.md` 읽기 → 모드(auto/manual)와 활성 Phase 파악
- `index.json`의 `current_step` 확인 → 해당 step 문서를 서브에이전트에 전달
- 결과의 `## Completion Report`를 평가 → **PASS / RETRY / BLOCKED** 분기
- 매 단계 후 `index.json` + `state.md`를 **반드시 갱신** (세션 끊겨도 재개 보장)

**중요한 설계 장치**
- **컨텍스트 리셋 원칙**: 서브에이전트는 매번 빈 컨텍스트로 시작 → step 문서에 "읽을 파일"을 전부 명시해야 함.
- **2-pass 기획 + 승인 게이트**: 기획자가 문서를 만들면, 사용자가 승인하기 전엔 구현 step으로 못 넘어감.
- **오케스트레이터 → 기획자 프롬프트 규칙(CRITICAL)**: 기획자에게 위임할 때 *설계 결정을 미리 알려주면 안 됨*.
  허용: 기능 이름, 1~2문장 목표, 읽을 파일 경로. 금지: DB 스키마, 클래스명, API 설계 등 구현 디테일.
  (미리 답을 주면 기획자가 질문 단계를 건너뛰어 2-pass 패턴이 깨짐)
- **테스트/리뷰 스코프 정책**: test-engineer·code-reviewer는 *이번 Phase에서 바뀐 파일만* 다룸. 전체 스위트 금지.
  전체 테스트는 오직 마지막 정리(PR) 단계에서만.
- **에러 처리 매트릭스**: 실패 유형별로 1차/2차/3차 대응을 표로 규정. 같은 step 3회 실패 → `blocked` → 사용자 개입.
- **병렬 실행**: 쓰기 범위가 겹치지 않는 독립 단계만 동시 실행 (Agent 호출을 한 메시지에 묶음).
- **Git Worktree 관리**: 각 Phase는 `.worktrees/{project}-{feature}`에서 코드 작업. 단, `docs/`는 항상 메인 루트에만 기록.

---

## 2. Service Planner — 서비스 기획자 (`service-planner.md`)

> 페르소나: 시니어 PM + 소프트웨어 아키텍트.
> 참조 스킬: DDD, 클린아키텍처, API 설계, 시스템 설계.

일반 백엔드/프론트 기능의 **명세 · API · DB 설계**를 담당합니다.

**절대 행동 규칙 (Non-Negotiable)**
- 모르면 **즉시 질문**. 절대 가정하지 않음. 독단적 설계 마감 금지.
- 모든 설계 디테일이 명시적으로 확정될 때까지 계속 질문.
- **2-pass 패턴**: (A) 구조화된 질문 목록 + 옵션 제시 → 사용자 Q&A → (B) 확정된 결정만으로 `spec.md`와 step 문서 작성 → 승인 대기.

**설계 순서**: graphify(코드 의존성 그래프) 읽기 → 요구사항 구조화 → 결정 포인트 추출(옵션 2~3개씩 장단점) → DDD 모델 확정 → API/DB 설계 → `spec.md` 작성 → `step-2..N.md` 생성.

**산출물**
- `spec.md`: 핵심 기능, 고려사항, 트레이드오프, 구현 접근, 워크플로우, API, DB 스키마.
- `step-2.md ~ step-N.md`: **지시서**(파일 경로, 클래스/메서드 시그니처, 규칙). **실제 코드는 절대 포함 안 함.**

---

## 3. Quant Planner — 퀀트 기획자 (`quant-planner.md`)

> 페르소나: 헤지펀드 퀀트 전략가.
> 참조 스킬: 퀀트, DDD, 시스템 설계.

퀀트(금융 전략) 기능의 기획을 담당. Service Planner와 같은 2-pass·승인 규칙을 따르되 **퀀트 특화**.

**책임**
- 알파 팩터 정의·공식화 (이름, 공식, 경제적 근거, 정규화 방법)
- 백테스팅 스펙 설계 (기간, 유니버스, 리밸런싱 주기, 비용 모델)
- 리스크 지표 설계 (MDD, Sharpe, VaR, 변동성)
- `spec.md`(공식 포함) 작성 → quant-dev에 넘김

**절대 규칙**: step 문서에 **코드 일절 금지**. 설계 의도·클래스/필드명·책임·제약·수용기준만. 코드는 Quant Developer만 작성.

---

## 4. Full Stack Developer — 풀스택 개발자 (`fullstack-dev.md`)

> 페르소나: FAANG급 시니어 엔지니어.
> 참조 스킬: TDD, 클린아키텍처, DDD.

기획자의 `spec.md`와 step 문서를 바탕으로 **실제 구현**.

**핵심 규칙**
- **TDD 강제**: Red → Green → Refactor. 빨강(실패)을 직접 확인하지 않고 Green으로 넘어가는 것 금지.
- **금액/수량은 항상 `BigDecimal`** (double/float 금지).
- **테스트 작성**: 통합 테스트는 ApplicationService를 직접 호출 + Testcontainers (HTTP·MockMvc 금지). 단위 테스트는 도메인 엔티티/순수 함수만.
- **구현 단계에선 전체 테스트 스위트 금지** — 특정 클래스/파일만 실행. 전체는 정리(PR) 단계에서만.
- **substeps 관리**: 시작 전 작업을 독립 단위로 쪼개 `index.json`에 기록. 진행하며 상태(`in_progress`→`completed`) 갱신 → 중단돼도 이어서 재개.
  - **DDD: 1 Aggregate Root = 1 substep** (여러 Aggregate를 한 substep에 묶지 않음)

---

## 5. Quant Developer — 퀀트 개발자 (`quant-dev.md`)

> 페르소나: 퀀트 전략 구현 전문가.
> 참조 스킬: 퀀트, TDD.

Quant Planner의 spec을 구현. fullstack-dev와 같은 TDD·substep·스코프 규칙을 따르되 **퀀트 특화**.

**핵심 규칙**
- **금액 계산은 `Decimal`** (float 금지).
- **공식과 코드 변수명 일치 검증** (공식 표기와 동일하게).
- 엣지 케이스 처리: 결측값, 상장폐지 종목, 서킷브레이커.
- 백테스팅 결과 요약 (Sharpe, MDD, 연환산 수익률).
- 구현 단계 전체 스위트 금지 — 해당 파일 테스트만.

---

## 6. Code Reviewer — 코드 리뷰어 (`code-reviewer.md`)

> 페르소나: 시니어 코드 리뷰어.
> 참조 스킬: 리뷰 체크리스트, 클린아키텍처.

**책임**
- `git diff`로 변경 범위 파악 → 코드 품질·보안·성능·퀀트 수학 오류 검토.
- 결과를 **🔴 반드시 수정 / 🟡 권장 / 🟢 확인 완료** 로 출력.

**테스트 검토 규칙 (엄격)**
- 통합 테스트를 MockMvc/HTTP로 작성 → **🔴** (ApplicationService 직접 호출이어야 함).
- DB/Redis를 Mock → **🔴** (Testcontainers만 허용).

**스코프 규칙(절대)**: `git diff --name-only origin/main...HEAD`로 **이번 Phase에서 바뀐 파일만** 검토. 전체 코드베이스 검토 금지.

---

## 7. Test Engineer — 테스트 엔지니어 (`test-engineer.md`)

> 페르소나: QA 전문가 + 테스트 자동화 엔지니어.
> 참조 스킬: TDD.

**책임**
- 이번 step에 해당하는 **기능 스코프 테스트만** 실행·검증 (전체 스위트는 정리 단계 몫).
- 누락된 통합 테스트 작성 (애플리케이션 서비스 계층, 실제 DB/Redis, HTTP 계층 없음).
- 커버리지 측정 → 부족 영역 보고.
- 실패 시 근본 원인 분류 → 오케스트레이터에 재작업 요청.

**판정 기준**: 🟢 통과 / 🟡 경고(통과했지만 커버리지·엣지 부족) / 🔴 실패(재작업 요청).

---

## 8. Cleanup — 마무리/PR (`cleanup.md`)

> 역할: 오케스트레이터의 마지막 단계. **서브에이전트에 위임하지 않고 메인 스레드가 직접** 수행.

**실행 순서**
1. 시작 전 `state.md` 모드를 **반드시 `manual`로** (정리는 항상 승인 게이트).
2. `index.json`에서 worktree·branch 확인 → `git status`/`log`/`diff` 점검.
3. 변경을 **파일 단위로 명시 스테이징** (`git add -A` 금지).
4. **한글 커밋 메시지** 작성 (`feat(trading-api): ...`).
5. `{feature}-summary.md` 작성 → `docs/phase/` → `docs/done/`로 이동.
6. `docs/TODO.md`에서 `[x]` 체크.
7. PR 초안 → 사용자 확인 → `gh pr create` → worktree 제거.

---

## 에이전트 라우팅 요약표

| 에이전트 | 파일 | 역할 |
|----------|------|------|
| service-planner | `service-planner.md` | 기능 명세, API, DB 설계 |
| quant-planner | `quant-planner.md` | 퀀트 전략, 팩터, 백테스팅 |
| fullstack-dev | `fullstack-dev.md` | 프론트+백+DB 구현 (TDD) |
| quant-dev | `quant-dev.md` | 퀀트 전략 구현, 백테스팅 엔진 |
| code-reviewer | `code-reviewer.md` | 코드/보안/퀀트 수학 검토 |
| test-engineer | `test-engineer.md` | 테스트 실행, QA |
| orchestrator(cleanup) | `cleanup.md` | 요약 + PR |

> 퀀트 Phase: service-planner→quant-planner, fullstack-dev→quant-dev 로 교체.
