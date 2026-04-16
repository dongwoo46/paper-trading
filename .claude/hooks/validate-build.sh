#!/bin/bash
# PostToolUse(Edit|Write) 훅 — 변경된 파일의 서비스 감지 후 빌드 명령 안내
# stdin으로 Claude Code가 JSON을 넘겨줌

INPUT=$(cat)
FILE_PATH=$(echo "$INPUT" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('tool_input',{}).get('file_path',''))" 2>/dev/null)

if [[ "$FILE_PATH" == *"trading-api"* ]]; then
  echo "⚡ [trading-api] 변경 감지 → cd backend/trading-api && ./gradlew compileJava"
elif [[ "$FILE_PATH" == *"collector-api"* ]]; then
  echo "⚡ [collector-api] 변경 감지 → cd backend/collector-api && ./gradlew compileKotlin"
elif [[ "$FILE_PATH" == *"collector-worker"* ]]; then
  echo "⚡ [collector-worker] 변경 감지 → python -m py_compile $FILE_PATH"
elif [[ "$FILE_PATH" == *"trading-web"* ]]; then
  echo "⚡ [trading-web] 변경 감지 → cd frontend/trading-web && npm run build"
elif [[ "$FILE_PATH" == *"research-worker"* ]]; then
  echo "⚡ [research-worker] 변경 감지 → python -m py_compile $FILE_PATH"
fi
