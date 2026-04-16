#!/bin/bash
# Stop 훅 — 사용자 허가가 필요한 상황에서 Windows 알림 + 소리 출력

STATE_FILE="docs/state.md"

# 현재 모드 파악
MODE=$(grep -A1 "## 모드" "$STATE_FILE" 2>/dev/null | tail -1 | tr -d ' \r')
STATUS=$(grep -A1 "## 상태" "$STATE_FILE" 2>/dev/null | tail -1 | tr -d ' \r')
ACTIVE=$(grep -A1 "## 활성 Phase" "$STATE_FILE" 2>/dev/null | tail -1 | tr -d '\r')

# 알림이 필요한 조건:
# 1. manual 모드 (매 step마다 허가 필요)
# 2. blocked 상태 (긴급 개입 필요)
NEEDS_APPROVAL=0

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

# 알림 불필요하면 종료
if [ $NEEDS_APPROVAL -eq 0 ]; then
  exit 0
fi

# Windows 알림 + 소리 (PowerShell)
powershell.exe -NoProfile -NonInteractive -Command "
  # 소리 재생
  [System.Media.SystemSounds]::Asterisk.Play()
  Start-Sleep -Milliseconds 600
  [System.Media.SystemSounds]::Asterisk.Play()

  # Windows 토스트 알림
  try {
    \$title   = '$TITLE'
    \$message = '$MESSAGE — 활성 Phase: $ACTIVE'

    [Windows.UI.Notifications.ToastNotificationManager, Windows.UI.Notifications, ContentType = WindowsRuntime] | Out-Null
    [Windows.Data.Xml.Dom.XmlDocument, Windows.Data.Xml.Dom.XmlDocument, ContentType = WindowsRuntime] | Out-Null

    \$template = [Windows.UI.Notifications.ToastNotificationManager]::GetTemplateContent(
      [Windows.UI.Notifications.ToastTemplateType]::ToastText02
    )
    \$template.SelectSingleNode('//text[@id=\"1\"]').InnerText = \$title
    \$template.SelectSingleNode('//text[@id=\"2\"]').InnerText = \$message

    \$toast = [Windows.UI.Notifications.ToastNotification]::new(\$template)
    [Windows.UI.Notifications.ToastNotificationManager]::CreateToastNotifier('Claude Code').Show(\$toast)
  } catch {
    # 토스트 실패 시 콘솔 출력으로 fallback
    Write-Host '🔔 알림: $TITLE'
  }
" 2>/dev/null

exit 0
