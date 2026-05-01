#!/bin/bash
# PreToolUse(Bash) 훅 — 위험한 명령 실행 전 차단
# exit 1 시 Claude Code가 해당 Bash 실행을 블록하고 이 메시지를 Claude에게 전달

INPUT=$(cat)
CMD=$(echo "$INPUT" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('tool_input',{}).get('command',''))" 2>/dev/null)

BLOCKED=0
REASON=""

# 광범위 삭제
if echo "$CMD" | grep -qE "rm\s+-rf\s+[^/]*(src|backend|frontend|docs|\.claude)"; then
  REASON="🚨 BLOCKED: 소스 디렉토리 대상 rm -rf 감지"
  BLOCKED=1
fi

# Git 위험 명령
if echo "$CMD" | grep -qE "git\s+reset\s+--hard|git\s+push\s+(--force|-f)|git\s+clean\s+-f"; then
  REASON="🚨 BLOCKED: 되돌릴 수 없는 git 명령 감지 (--hard reset / force push / clean -f)"
  BLOCKED=1
fi

# DB 위험 명령
if echo "$CMD" | grep -qiE "DROP\s+TABLE|TRUNCATE\s+TABLE|DROP\s+DATABASE"; then
  REASON="🚨 BLOCKED: 파괴적 DB 명령 감지 (DROP/TRUNCATE)"
  BLOCKED=1
fi

# Flyway 마이그레이션 파일 삭제
if echo "$CMD" | grep -qE "rm.*V[0-9]+__.*\.sql"; then
  REASON="🚨 BLOCKED: Flyway 마이그레이션 파일 삭제 시도"
  BLOCKED=1
fi

# 중간 phase step에서 전체 테스트 스위트 실행 방지.
# 원칙: 구현/검증 중간 단계는 현재 phase에서 추가/변경한 targeted test만 실행한다.
# 전체 테스트는 phase completion/final step gate에서만 실행한다.
IS_FULL_SUITE=0

if echo "$CMD" | grep -qiE "gradlew(\.bat)?([^;&|]*\s|[[:space:]])test([[:space:]]|$)" \
  && ! echo "$CMD" | grep -q -- "--tests"; then
  IS_FULL_SUITE=1
fi

if echo "$CMD" | grep -qiE "(^|[[:space:]])(python[0-9.]*\s+-m\s+)?pytest(\.exe)?\s+tests/?([[:space:]]|$)" \
  && ! echo "$CMD" | grep -qE "::|tests/[^[:space:]]*test_[^[:space:]]+\.py"; then
  IS_FULL_SUITE=1
fi

if echo "$CMD" | grep -qiE "npm(\.cmd)?\s+(run\s+)?test([[:space:]]|$)" \
  && ! echo "$CMD" | grep -qE "--run\s+[^[:space:]]+\.(test|spec)\.(ts|tsx|js|jsx)|[^[:space:]]+\.(test|spec)\.(ts|tsx|js|jsx)"; then
  IS_FULL_SUITE=1
fi

if [ $IS_FULL_SUITE -eq 1 ]; then
  PHASE_INFO=$(python3 - <<'PY' 2>/dev/null
import json, re
from pathlib import Path
state = Path("docs/state.md")
if not state.exists():
    print("unknown 0 0")
    raise SystemExit
text = state.read_text(encoding="utf-8")
m = re.search(r"## 활성 Phase\s*\n-\s*([^/\s]+)/([^|\s]+)", text)
if not m:
    print("unknown 0 0")
    raise SystemExit
project, feature = m.group(1), m.group(2)
idx = Path("docs/phase") / project / feature / "index.json"
if not idx.exists():
    print(f"{project}/{feature} 0 0")
    raise SystemExit
d = json.loads(idx.read_text(encoding="utf-8"))
print(f"{project}/{feature} {int(d.get('current_step') or 0)} {int(d.get('total_steps') or 0)}")
PY
)
  PHASE=$(echo "$PHASE_INFO" | awk '{print $1}')
  CURRENT_STEP=$(echo "$PHASE_INFO" | awk '{print $2}')
  TOTAL_STEPS=$(echo "$PHASE_INFO" | awk '{print $3}')

  if [ -n "$CURRENT_STEP" ] && [ -n "$TOTAL_STEPS" ] \
    && [ "$CURRENT_STEP" -gt 0 ] && [ "$TOTAL_STEPS" -gt 0 ] \
    && [ "$CURRENT_STEP" -lt "$TOTAL_STEPS" ]; then
    REASON="🚨 BLOCKED: 중간 phase step($PHASE step $CURRENT_STEP/$TOTAL_STEPS)에서 전체 테스트 스위트 실행 시도"
    BLOCKED=1
  fi
fi

if [ $BLOCKED -eq 1 ]; then
  echo "$REASON"
  echo "사용자 명시적 승인 없이 이 명령을 실행하지 마세요."
  if [ $IS_FULL_SUITE -eq 1 ]; then
    echo "현재 step에서는 phase에서 추가/변경한 테스트만 targeted로 실행하세요."
    echo "예: ./gradlew test --tests \"com.papertrading.api.<package>.<TestClass>\""
    echo "전체 테스트는 마지막 phase completion gate에서만 실행합니다."
  fi
  exit 1
fi

exit 0
