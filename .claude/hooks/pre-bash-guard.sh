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

if [ $BLOCKED -eq 1 ]; then
  echo "$REASON"
  echo "사용자 명시적 승인 없이 이 명령을 실행하지 마세요."
  exit 1
fi

exit 0
