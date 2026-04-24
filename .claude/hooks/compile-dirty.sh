#!/bin/bash
# Stop 훅 — dirty 목록의 서비스만 컴파일 (매 edit마다 아닌 turn 종료 시 1회)

DIRTY_FILE="/tmp/.claude_dirty_services"

[ -f "$DIRTY_FILE" ] && [ -s "$DIRTY_FILE" ] || exit 0

FAILED=0
WEB_CHANGED=0

while IFS='|' read -r type path; do
  case "$type" in
    kotlin)
      echo "🔨 [compile] $path"
      if (cd "$path" && ./gradlew compileKotlin -q 2>&1); then
        echo "✅ OK"
      else
        echo "❌ FAILED — fix compile errors before proceeding"
        FAILED=1
      fi
      ;;
    python)
      echo "🔨 [syntax] $path"
      if python3 -m py_compile "$path" 2>&1; then
        echo "✅ OK"
      else
        echo "❌ FAILED"
        FAILED=1
      fi
      ;;
    web)
      WEB_CHANGED=1
      ;;
  esac
done < <(sort -u "$DIRTY_FILE")

rm -f "$DIRTY_FILE"

[ $WEB_CHANGED -eq 1 ] && echo "⚡ [trading-web] changed — run 'npm run build' to verify"
[ $FAILED -ne 0 ] && exit 1

exit 0