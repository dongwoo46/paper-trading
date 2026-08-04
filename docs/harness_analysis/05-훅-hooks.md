# 05. 훅 (Hooks)

훅은 Claude가 도구(Bash, Edit, Write 등)를 쓸 때 **전/후에 자동으로 끼어드는 스크립트**입니다.
Claude가 호출하는 게 아니라 **하네스(Claude Code 자체)가 강제로 실행**하므로,
"항상 ~한다", "~할 때마다 ~한다" 같은 규칙을 보장합니다.

`.claude/settings.json`에 "언제 어떤 훅을 돌릴지"가 등록돼 있습니다.

## 훅 등록 현황 (`settings.json`)

| 시점(이벤트) | 매처 | 실행되는 훅 | 목적 |
|--------------|------|-------------|------|
| **PreToolUse** (도구 실행 전) | `Bash` | `pre-bash-guard.sh` | 위험 명령 차단 |
| **PostToolUse** (도구 실행 후) | `Edit`/`Write` | `validate-build.sh` | 변경 서비스 dirty 기록 |
| **PostToolUse** | `Bash` | `post-test-check.sh` | 테스트 실패 시 TDD 상기 |
| **PostToolUse** | `Bash` | `track-failure.sh` | 명령 실패 분류·누적 |
| **Stop** (작업 종료 시) | — | `compile-dirty.sh` | 변경 서비스만 컴파일 |
| **Stop** | — | `on-stop.sh` | 현황 요약 + 권고 |
| **Stop** | — | `notify.sh` | 소리·알림(승인/blocked 시) |

또한 권한 설정으로 `./gradlew compileKotlin`, `compileTestKotlin`, `test *`는 **확인 없이 자동 허용**됩니다.

---

## 1. pre-bash-guard.sh — 위험 명령 차단 🛡️

**시점**: Bash 실행 *직전*. exit 1이면 Claude Code가 그 명령을 **블록**하고 이유를 Claude에게 전달.

차단 대상:
- **광범위 삭제**: `rm -rf`로 `src`/`backend`/`frontend`/`docs`/`.claude` 대상
- **되돌릴 수 없는 Git**: `git reset --hard`, `git push --force/-f`, `git clean -f`
- **파괴적 DB**: `DROP TABLE`, `TRUNCATE TABLE`, `DROP DATABASE`
- **Flyway 마이그레이션 파일 삭제**: `rm ... V123__*.sql`

차단 시 출력: `🚨 BLOCKED: ...` + "사용자 명시적 승인 없이 실행하지 마세요."

> 이것이 하네스의 **가장 중요한 안전장치**. AI가 실수로도 파괴적 명령을 못 돌리게 막습니다.

---

## 2. validate-build.sh — 변경 서비스 기록 (즉시 빌드 안 함) 📝

**시점**: Edit/Write 직후.

수정한 파일 경로를 보고 **어느 서비스가 바뀌었는지**만 `/tmp/.claude_dirty_services`에 기록합니다.
소스 파일(`.kt`/`.java`/`.py`/`.ts`/`.tsx`)이 아니면 무시.

- `backend/trading-api`, `backend/collector-api` → `kotlin|경로`
- `backend/quant-worker` (.py) → `python|파일경로`
- `frontend/trading-web` → `web|skip`

> **핵심 아이디어**: 매 수정마다 컴파일하면 느리니까, 변경분을 *모아두고* 실제 컴파일은
> 작업이 끝날 때(Stop) `compile-dirty.sh`가 한 번에 처리합니다.

---

## 3. compile-dirty.sh — 변경 서비스만 컴파일 🔨

**시점**: 작업 종료(Stop). dirty 목록이 있을 때만.

기록된 서비스만 컴파일/문법 검사:
- kotlin → `./gradlew compileKotlin -q`
- python → `python3 -m py_compile`
- web → "npm run build로 확인하라" 안내만

하나라도 실패하면 exit 1 → "진행 전에 컴파일 에러 고쳐라" 신호. 끝나면 dirty 목록 삭제.

> 변경된 서비스만 골라 빌드하므로 빠르고, 컴파일 깨진 채로 턴이 끝나는 걸 방지합니다.

---

## 4. post-test-check.sh — 테스트 실패 시 TDD 상기 🔴

**시점**: Bash 직후. 명령이 테스트(`gradlew test`/`pytest`/`npm test`/`jest`)이고 종료코드 ≠ 0이면:

```
🔴 [TDD] 테스트 실패 — Red 단계 확인
→ 의도된 Red면 정상. Green(최소 구현)으로 진행.
→ 의도치 않은 실패면 원인 파악 후 진행.
```

> 실패를 *나쁜 것*이 아니라 *TDD 사이클의 정상 단계*로 재해석하도록 돕습니다.

---

## 5. track-failure.sh — 장애 패턴 누적·학습 📊

**시점**: Bash 직후, 종료코드 ≠ 0일 때.

1. 실패한 명령+출력을 **로컬 Ollama LLM**(`gemma4:e4b`)에 보내 "2~4단어 한글 카테고리"로 분류
   (예: "빌드 컴파일 오류", "테스트 실패", "DB 연결 오류"). *Ollama 미실행 시 조용히 종료.*
2. `.claude/failure-log.json`에 카테고리별 **횟수·최초·최근** 누적.
3. 같은 카테고리가 **5회**(THRESHOLD) 도달하면 → `CLAUDE.md`의 `## 반복 장애 패턴` 섹션에 자동 기록.

> 같은 실수가 반복되면 *프로젝트 헌법(CLAUDE.md)에 박제*되어, 이후 세션에서 미리 경고됩니다.
> 일종의 자가 학습 루프입니다.

---

## 6. on-stop.sh — 작업 완료 현황 요약 ✅

**시점**: 작업 종료(Stop).

`docs/state.md`에서 모드·활성 Phase를 읽어 요약 출력:

```
━━━━━━━━━━━━━━━━━━━━━━━━
✅ 작업 완료
모드: manual | 활성 Phase: ...
→ /review 또는 /orchestrate 로 계속 진행하세요.
━━━━━━━━━━━━━━━━━━━━━━━━
```

> 턴이 끝날 때마다 "지금 어디까지 왔고 다음에 뭘 하면 되는지"를 자동으로 알려줍니다.

---

## 7. notify.sh (+ play-sound.ps1) — 소리·알림 🔔

**시점**: 작업 종료(Stop). **승인이 필요한 상황에만** 발동.

`docs/state.md`를 읽어:
- 모드가 `manual` → "✋ 승인 필요" 알림
- 상태가 `blocked` → "🚨 긴급 개입 필요" 알림

발동 시 Windows에서 소리(`play-sound.ps1`) + 시스템 트레이 풍선 알림을 띄웁니다.
(WSL/Windows 환경 전용 — `powershell.exe` 사용)

> 사람이 의사결정해야 하는 순간을 *놓치지 않게* 물리적으로 알려줍니다.

---

## 훅이 만드는 자동화 루프 (한눈에)

```
[Edit/Write] ──▶ validate-build (변경 기록)
                                       │
[Bash 실행 전] ──▶ pre-bash-guard (위험 차단)
[Bash 실행 후] ──▶ post-test-check (TDD 상기)
                └▶ track-failure (실패 누적 → 5회면 CLAUDE.md 기록)
                                       │
[작업 종료] ──▶ compile-dirty (변경분만 컴파일)
            └▶ on-stop (현황 요약)
            └▶ notify (승인/blocked면 알림)
```

사람은 **의사결정**만 하고, 빌드 검증·안전 가드·진행 보고·실패 학습은 훅이 자동으로 처리합니다.
