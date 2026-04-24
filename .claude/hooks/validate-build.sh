#!/bin/bash
# PostToolUse(Edit|Write) — 변경된 서비스를 dirty 목록에 기록 (컴파일은 Stop 훅에서)

INPUT=$(cat)
FILE_PATH=$(echo "$INPUT" | python3 -c "
import sys, json
d = json.load(sys.stdin)
print(d.get('tool_input', {}).get('file_path', ''))
" 2>/dev/null)

# 경로 정규화 (Windows 백슬래시 → 슬래시)
FILE_PATH=$(echo "$FILE_PATH" | tr '\\' '/')

# 소스 파일이 아니면 skip
if [[ "$FILE_PATH" != *.kt && "$FILE_PATH" != *.java && "$FILE_PATH" != *.py \
   && "$FILE_PATH" != *.ts && "$FILE_PATH" != *.tsx ]]; then
  exit 0
fi

DIRTY_FILE="/tmp/.claude_dirty_services"

record() {
  grep -qF "$1" "$DIRTY_FILE" 2>/dev/null || echo "$1" >> "$DIRTY_FILE"
}

if [[ "$FILE_PATH" == *"backend/trading-api"* ]]; then
  BASE="${FILE_PATH%%backend/trading-api*}backend/trading-api"
  record "kotlin|${BASE}"

elif [[ "$FILE_PATH" == *"backend/collector-api"* ]]; then
  BASE="${FILE_PATH%%backend/collector-api*}backend/collector-api"
  record "kotlin|${BASE}"

elif [[ "$FILE_PATH" == *"backend/collector-worker"* && "$FILE_PATH" == *.py ]]; then
  record "python|${FILE_PATH}"

elif [[ "$FILE_PATH" == *"research-worker"* && "$FILE_PATH" == *.py ]]; then
  record "python|${FILE_PATH}"

elif [[ "$FILE_PATH" == *"frontend/trading-web"* ]]; then
  record "web|skip"
fi
