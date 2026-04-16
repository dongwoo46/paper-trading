#!/bin/bash
# PostToolUse(Bash) 훅 — 테스트 실패 시 TDD 사이클 상기
# 테스트 명령 결과를 감지해서 실패면 Red 단계 확인 메시지 출력

INPUT=$(cat)
CMD=$(echo "$INPUT" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('tool_input',{}).get('command',''))" 2>/dev/null)
EXIT_CODE=$(echo "$INPUT" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('tool_response',{}).get('exit_code', 0))" 2>/dev/null)

# 테스트 명령인지 확인
IS_TEST=0
if echo "$CMD" | grep -qE "gradlew\s+test|pytest|npm\s+(run\s+)?test|jest"; then
  IS_TEST=1
fi

if [ $IS_TEST -eq 1 ] && [ "$EXIT_CODE" != "0" ]; then
  echo ""
  echo "🔴 [TDD] 테스트 실패 — Red 단계 확인"
  echo "→ 실패가 의도된 Red라면 정상입니다. Green(최소 구현)으로 진행하세요."
  echo "→ 의도치 않은 실패라면 원인을 파악한 뒤 진행하세요."
fi

exit 0
