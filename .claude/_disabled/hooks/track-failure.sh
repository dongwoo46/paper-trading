#!/bin/bash
# PostToolUse(Bash) 훅 — 명령 실패 시 Ollama로 장애 분류 및 CLAUDE.md 누적 기록

INPUT=$(cat)

EXIT_CODE=$(echo "$INPUT" | python3 -c "
import sys, json
d = json.load(sys.stdin)
print(d.get('tool_response', {}).get('exitCode', 0))
" 2>/dev/null)

# 성공이면 종료
[ "$EXIT_CODE" = "0" ] && exit 0

CMD=$(echo "$INPUT" | python3 -c "
import sys, json
d = json.load(sys.stdin)
print(d.get('tool_input', {}).get('command', ''))
" 2>/dev/null)

OUTPUT=$(echo "$INPUT" | python3 -c "
import sys, json
d = json.load(sys.stdin)
r = d.get('tool_response', {})
out = (r.get('stdout') or '') + (r.get('stderr') or '')
print(out[:600])
" 2>/dev/null)

# 분류할 내용 없으면 종료
[ ${#OUTPUT} -lt 15 ] && exit 0

LOG_FILE=".claude/failure-log.json"
CLAUDE_MD="CLAUDE.md"
THRESHOLD=5

[ ! -f "$LOG_FILE" ] && echo '{"failures":{}}' > "$LOG_FILE"

# Ollama 가용 여부 확인 (미실행 시 조용히 종료)
OLLAMA_HOST="${OLLAMA_HOST:-http://localhost:11434}"
if ! curl -sf --max-time 2 "$OLLAMA_HOST/api/tags" > /dev/null 2>&1; then
  exit 0
fi

OLLAMA_MODEL="${OLLAMA_MODEL:-gemma4:e4b}"

PROMPT="다음 명령 오류를 2~4단어 한국어로 분류해줘. 카테고리 이름만 출력 (예시: '빌드 컴파일 오류', '테스트 실패', 'DB 연결 오류', '의존성 누락'):

명령: $CMD
오류: $OUTPUT"

CATEGORY=$(curl -sf --max-time 10 "$OLLAMA_HOST/api/generate" \
  -H "Content-Type: application/json" \
  -d "{\"model\":\"$OLLAMA_MODEL\",\"prompt\":\"$PROMPT\",\"stream\":false}" \
  2>/dev/null \
  | python3 -c "import sys,json; print(json.load(sys.stdin).get('response','').strip())" 2>/dev/null \
  | head -1 | tr -d '"' | cut -c1-30)

[ -z "$CATEGORY" ] && exit 0

python3 - <<PYEOF
import json, datetime

log_file = "$LOG_FILE"
claude_md = "$CLAUDE_MD"
category = "$CATEGORY"
threshold = $THRESHOLD

with open(log_file) as f:
    data = json.load(f)

failures = data.setdefault("failures", {})
if category not in failures:
    failures[category] = {"count": 0, "first_seen": str(datetime.date.today()), "last_seen": ""}

failures[category]["count"] += 1
failures[category]["last_seen"] = str(datetime.date.today())

with open(log_file, "w") as f:
    json.dump(data, f, ensure_ascii=False, indent=2)

count = failures[category]["count"]
print(f"[장애추적] {category} — {count}회")

if count == threshold:
    section = "## 반복 장애 패턴"
    first = failures[category]["first_seen"]
    last = failures[category]["last_seen"]
    entry = f"- **{category}** ({first} ~ {last}, {threshold}회 반복)"

    with open(claude_md, encoding="utf-8") as f:
        content = f.read()

    if section in content:
        content = content.replace(section + "\n\n", section + "\n\n" + entry + "\n")
    else:
        content += f"\n\n{section}\n\n{entry}\n"

    with open(claude_md, "w", encoding="utf-8") as f:
        f.write(content)

    print(f"⚠️  [{category}] {threshold}회 반복 — CLAUDE.md 기록 완료")
PYEOF

exit 0
