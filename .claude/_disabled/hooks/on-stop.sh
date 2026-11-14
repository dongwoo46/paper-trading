#!/bin/bash
# Stop 훅 — 작업 완료 시 state.md 현황 요약 + 권고

STATE_FILE="docs/state.md"

if [ -f "$STATE_FILE" ]; then
  MODE=$(grep -A1 "## 모드" "$STATE_FILE" 2>/dev/null | tail -1 | tr -d ' \r' || echo "unknown")
  ACTIVE=$(grep -A1 "## 활성 Phase" "$STATE_FILE" 2>/dev/null | tail -1 | tr -d '\r')
  echo ""
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
  echo "✅ 작업 완료"
  echo "모드: $MODE | 활성 Phase: $ACTIVE"
  echo "→ /review 또는 /orchestrate 로 계속 진행하세요."
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
else
  echo ""
  echo "✅ 작업 완료 — /orchestrate 실행을 권장합니다."
fi
