#!/bin/bash
# Stop hook — manual mode / blocked 상태에서 소리 + Windows 알림

STATE_FILE="docs/state.md"

MODE=$(grep -A1 "## 모드" "$STATE_FILE" 2>/dev/null | tail -1 | tr -d ' \r')
STATUS=$(grep -A1 "## 상태" "$STATE_FILE" 2>/dev/null | tail -1 | tr -d ' \r')
ACTIVE=$(grep -A1 "## 활성 Phase" "$STATE_FILE" 2>/dev/null | tail -1 | tr -d '\r' | sed "s/['\"]//g")

NEEDS_APPROVAL=0
TITLE=""
MESSAGE=""

if [ "$MODE" = "manual" ]; then
  NEEDS_APPROVAL=1
  TITLE="✋ 승인 필요 — Claude Code"
  MESSAGE="에이전트가 대기 중입니다. 확인 후 진행해 주세요."
fi

if [ "$STATUS" = "blocked" ]; then
  NEEDS_APPROVAL=1
  TITLE="🚨 긴급 개입 필요 — Claude Code"
  MESSAGE="Phase가 blocked 상태입니다. 수동 확인이 필요합니다."
fi

if [ $NEEDS_APPROVAL -eq 0 ]; then
  exit 0
fi

# ── 소리 재생 ─────────────────────────────────────────────────────────────
WIN_PS1=$(cygpath -w "$(cd "$(dirname "$0")" && pwd)/play-sound.ps1")
powershell.exe -NoProfile -NonInteractive -File "$WIN_PS1" >/dev/null 2>&1

# ── Windows 알림 (NotifyIcon 시스템 트레이) ───────────────────────────────
TITLE_ENV="$TITLE" MESSAGE_ENV="$MESSAGE — 활성 Phase: $ACTIVE" \
powershell.exe -NoProfile -NonInteractive -Command "
  Add-Type -AssemblyName System.Windows.Forms
  Add-Type -AssemblyName System.Drawing
  \$n = New-Object System.Windows.Forms.NotifyIcon
  \$n.Icon = [System.Drawing.SystemIcons]::Information
  \$n.BalloonTipIcon = [System.Windows.Forms.ToolTipIcon]::Warning
  \$n.BalloonTipTitle = \$env:TITLE_ENV
  \$n.BalloonTipText  = \$env:MESSAGE_ENV
  \$n.Visible = \$true
  \$n.ShowBalloonTip(6000)
  Start-Sleep -Seconds 6
  \$n.Visible = \$false
  \$n.Dispose()
" >/dev/null 2>&1

exit 0