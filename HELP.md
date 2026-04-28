# 하네스 엔지니어링 가이드

## 개념

Claude Code 에이전트들이 서로 역할을 나눠 기능을 자동으로 설계·구현·검토·완료하는 시스템.
`docs/state.md`와 `docs/phase/{project}/{feature}/index.json`이 세션 간 상태를 유지한다.
세션을 껐다 켜도 `/orchestrate`만 실행하면 중단된 지점부터 재개된다.

---

## 에이전트 구조

```
/orchestrate (Orchestrator)
├── /plan        — Service Planner  : 기능 명세, API 스펙, DB 설계, step 파일 생성
├── /plan-quant  — Quant Planner    : 퀀트 전략, 알파 팩터, 백테스팅 설계
├── /build       — Full Stack Dev   : 프론트+백엔드+DB 구현 (TDD)
├── /build-quant — Quant Developer  : 퀀트 전략 구현, 백테스팅 엔진
├── /test        — Test Engineer    : 테스트 실행, 통합 테스트 작성, QA 검증
└── /review      — Code Reviewer    : 코드·보안·퀀트 수학 오류 검토
```

Orchestrator가 `docs/state.md`와 `docs/TODO.md`를 읽어 현재 상태를 판단하고,
적절한 에이전트를 서브에이전트로 실행한 뒤 결과를 기록한다.

### 파일 분리 구조

```
.claude/
├── commands/   ← 사용자가 / 로 호출하는 얇은 래퍼 (내용: @../agents/xxx.md)
└── agents/     ← 에이전트 역할 전체 정의 (Orchestrator가 서브에이전트 호출 시 참조)
```

---

## 시작하는 법

### 새 기능 개발 시작

```
/orchestrate
```

state.md가 idle이면 Orchestrator가 project와 feature명을 물어본다.
입력하면 자동으로 아래를 생성한다:

```
docs/phase/{project}/{feature}/
├── index.json   ← step 상태 머신
└── step-1.md    ← Service Planner 초기 지시서
```

이후 Service Planner가 실행되어 `spec.md`와 `step-2.md ~ step-N.md`를 작성한다.

### 세션 재시작 후 이어서 하기

```
/orchestrate
```

state.md → index.json 순으로 읽어 중단된 step부터 자동 재개.

### 수동으로 특정 에이전트 실행

```
/plan         ← 설계만 하고 싶을 때
/build        ← 구현만 하고 싶을 때
/review       ← 리뷰만 하고 싶을 때
```

### Graphify 실행 가이드

코드 수정 후에는 서비스 단위로 graphify를 다시 실행한다.
현재 환경은 `.venv` 기준으로 실행한다.

```bash
# venv 활성화 (Git Bash)
source .venv/Scripts/activate

# 공통 형식
python -m graphify update <service-path>

# 서비스별 실행
python -m graphify update backend/collector-api
python -m graphify update backend/quant-worker
python -m graphify update backend/quant-worker
python -m graphify update backend/trading-api
```

백엔드 전체를 한 번에 갱신할 때:

```bash
python -m graphify update backend/collector-api && \
python -m graphify update backend/quant-worker && \
python -m graphify update backend/quant-worker && \
python -m graphify update backend/trading-api
```

PowerShell을 쓸 때:

```powershell
# venv 활성화 (PowerShell)
.\.venv\Scripts\Activate.ps1
python -m graphify update backend/trading-api
```

### Graphify 스크립트 실행 가이드 (`run-graphify.sh`)

루트에 있는 `run-graphify.sh`로 `ast`/`semantic`/`both` 모드를 선택 실행할 수 있다.

```bash
# 실행권한 (최초 1회)
chmod +x run-graphify.sh

# AST만 (기본)
./run-graphify.sh --mode ast --all
./run-graphify.sh -m ast -s trading-api

# 파일 경로로 서비스 자동 선택
./run-graphify.sh -m ast -f backend/trading-api/src/main/kotlin/com/papertrading/api/TradingApiApplication.kt

# semantic 또는 both
./run-graphify.sh -m semantic -s collector-api
./run-graphify.sh -m both --all

# 실제 실행 없이 명령만 확인
./run-graphify.sh --dry-run -m both --all
```

주요 옵션:

```text
-m, --mode ast|semantic|both
-s, --service <name|path[,..]>   (collector-api, quant-worker, quant-worker, trading-api)
-f, --files <file[,..]>          (파일 경로 기반 서비스 자동 매핑)
-a, --all
--python <python-path>
--semantic-cmd "<template>"      ({path} placeholder 사용)
--dry-run
```

참고:
- 현재 `graphify 0.5.x` CLI는 semantic 직접 실행 명령이 제한적이라, 스크립트의 semantic 모드는 기본적으로 AI 채팅용 `/graphify <path> --update` 실행 목록을 출력한다.
- semantic 자동 실행이 필요하면 `--semantic-cmd`를 지정해 사용한다.

---

## 모드 전환

```
"auto"    → 사용자 승인 없이 step 자동 진행
"manual"  → 각 step 전 승인 요청 (기본값)
```

실행 중 언제든 채팅창에 `auto` 또는 `manual` 입력으로 전환 가능.
현재 모드는 `docs/state.md`에 기록된다.

---

## Phase 생명주기

```
생성
docs/phase/{project}/{feature}/
├── index.json        current_step: 1, status: in_progress
├── step-1.md         Planner 지시서
├── step-2.md         Builder 지시서 (Planner가 생성)
├── step-3.md         Reviewer 지시서
└── spec.md           기능 명세서

         ↓ 모든 step 완료

완료
docs/done/{project}/{feature}/
├── index.json        완료된 전체 step 기록
├── spec.md
├── step-1.md ~ N.md
└── {feature}-summary.md   핵심 기능·트레이드오프·API·PR 정리
```

---

## step 파일 구조

각 step 파일은 에이전트가 파일만 읽고 완전히 실행할 수 있는 지시서다.

```markdown
# Step {N}: {이름}
담당 에이전트: {agent}

## 읽어야 할 파일
- CLAUDE.md
- docs/ADR.md
- docs/phase/{project}/{feature}/spec.md
- {이전 step 결과물 경로}

## 작업
{구체적 구현 지시. 파일 경로, 클래스 시그니처, 핵심 규칙}

## Acceptance Criteria
```bash
{빌드/테스트 검증 명령}
```
```

---

## step 수 기준

| 단계 수 | 상황 |
|--------|------|
| 3단계 이하 | 매우 단순한 변경 (설정, 단일 API 추가) |
| 5단계 | 일반 기능 개발 (기본값) |
| 6단계 | 리뷰 2회 필요 시 |
| 7단계 | 고복잡도 / 퀀트 전략 (최대) |

마지막 step은 항상 Orchestrator (summary 작성 + PR 생성).

---

## 리뷰 결과 처리

| 결과 | 처리 |
|------|------|
| 🟢 통과 | 다음 step 진행 |
| 🟡 권장 개선 | 빌더 재량으로 반영 후 통과 처리 |
| 🔴 필수 수정 | 직전 빌더에게 재작업 지시 (step 역방향 이동) |
| 설계 오류 | blocked 처리 → Planner 재실행 |

---

## 파일 구조

```
.claude/
├── commands/          ← 사용자 슬래시 커맨드 (얇은 래퍼, @../agents/xxx.md 참조)
│   ├── orchestrate.md
│   ├── plan.md
│   ├── plan-quant.md
│   ├── build.md
│   ├── build-quant.md
│   ├── review.md
│   ├── test.md
│   └── cleanup.md
├── agents/            ← 에이전트 역할 전체 정의 (Orchestrator가 서브에이전트 호출 시 참조)
│   ├── orchestrator.md
│   ├── service-planner.md
│   ├── quant-planner.md
│   ├── fullstack-dev.md
│   ├── quant-dev.md
│   ├── code-reviewer.md
│   ├── test-engineer.md
│   └── cleanup.md
├── skills/            ← 에이전트가 @참조하는 기술 문서
│   ├── tdd.md
│   ├── ddd.md
│   ├── clean-architecture.md
│   ├── api-design.md
│   ├── quant.md
│   ├── review.md
│   └── system-design.md
├── hooks/             ← 자동 실행 검증 스크립트
│   ├── pre-bash-guard.sh    ← 위험 명령 차단
│   ├── validate-build.sh    ← 파일 수정 시 빌드 명령 안내
│   ├── post-test-check.sh   ← 테스트 실패 시 TDD 상기
│   └── on-stop.sh           ← 작업 완료 시 현황 출력
└── settings.json      ← 훅 설정

docs/
├── state.md           ← Orchestrator 공유 상태 (모드 + 활성 phase)
├── TODO.md            ← 서비스별 개발 항목 (Orchestrator가 다음 phase 선정 시 참조)
├── phase/             ← 진행 중인 기능
│   ├── trading-api/
│   ├── collector-api/
│   ├── front/
│   └── quant-worker/
├── done/              ← 완료된 기능 아카이브
│   ├── trading-api/
│   ├── collector-api/
│   ├── front/
│   └── quant-worker/
├── PRD.md
└── ADR.md
```

---

## 자동 검증 훅

| 훅 | 트리거 | 동작 |
|----|--------|------|
| pre-bash-guard | Bash 실행 전 | `rm -rf`, `git reset --hard`, `force push`, `DROP TABLE` 차단 |
| validate-build | 파일 수정 후 | 수정된 서비스 감지 → 빌드 명령 출력 |
| post-test-check | Bash 실행 후 | 테스트 실패 시 TDD Red 단계 안내 |
| on-stop | 응답 완료 후 | state.md 현황 + /orchestrate 권고 |

---

## 주요 문서

| 문서 | 내용 |
|------|------|
| `CLAUDE.md` | 핵심 규칙·CRITICAL 원칙 |
| `docs/state.md` | 현재 모드·활성 phase 목록 |
| `docs/ADR.md` | 아키텍처 결정 기록 |
| `docs/PRD.md` | 제품 요구사항 |
