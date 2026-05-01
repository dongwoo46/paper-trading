$inputJson = [Console]::In.ReadToEnd()

try {
    $payload = $inputJson | ConvertFrom-Json -ErrorAction Stop
} catch {
    $payload = $null
}

$hookEvent = ""
$hookMessage = ""
$toolFilePath = ""
if ($payload) {
    if ($payload.PSObject.Properties.Name -contains "hook_event_name") {
        $hookEvent = [string]$payload.hook_event_name
    } elseif ($payload.PSObject.Properties.Name -contains "event") {
        $hookEvent = [string]$payload.event
    }

    if ($payload.PSObject.Properties.Name -contains "tool_input" -and $payload.tool_input) {
        if ($payload.tool_input.PSObject.Properties.Name -contains "file_path") {
            $toolFilePath = [string]$payload.tool_input.file_path
        }
    }

    foreach ($name in @("message", "notification", "reason")) {
        if ($payload.PSObject.Properties.Name -contains $name) {
            $hookMessage = [string]$payload.$name
            break
        }
    }
}

$stateFile = "docs/state.md"
$mode = ""
$status = ""
$active = ""
$nextAction = ""
if (Test-Path $stateFile) {
    $lines = Get-Content $stateFile
    for ($i = 0; $i -lt $lines.Count; $i++) {
        if ($lines[$i] -eq "## 모드" -and $i + 1 -lt $lines.Count) {
            $mode = $lines[$i + 1].Trim()
        }
        if ($lines[$i] -eq "## 상태" -and $i + 1 -lt $lines.Count) {
            $status = $lines[$i + 1].Trim()
        }
        if ($lines[$i] -eq "## 활성 Phase" -and $i + 1 -lt $lines.Count) {
            $active = $lines[$i + 1].Trim()
        }
        if ($lines[$i] -eq "## 다음 액션" -and $i + 1 -lt $lines.Count) {
            $nextAction = $lines[$i + 1].Trim()
        }
    }
}

$needsApproval = $false
$title = ""
$message = ""

if ($hookEvent -eq "Notification") {
    $needsApproval = $true
    $title = "승인/선택 필요 - Codex"
    if ($hookMessage) {
        $message = $hookMessage
    } else {
        $message = "Codex가 사용자 승인 또는 선택을 기다리고 있습니다."
    }
} elseif ($mode -eq "manual") {
    $needsApproval = $true
    $title = "승인 필요 - Codex"
    $message = "에이전트가 대기 중입니다. 확인 후 진행해 주세요."
}

if ($status -eq "blocked") {
    $needsApproval = $true
    $title = "개입 필요 - Codex"
    $message = "Phase가 blocked 상태입니다. 수동 확인이 필요합니다."
}

if ($status -eq "needs_input" -or $status -eq "waiting_for_user") {
    $needsApproval = $true
    $title = "선택 필요 - Codex"
    if ($nextAction) {
        $message = $nextAction
    } else {
        $message = "Codex가 사용자 선택을 기다리고 있습니다."
    }
}

if ($nextAction -match "(?i)(Choose next action|사용자 선택|승인|선택|proceed|continue|go ahead|select|pick|decide|choose|accept|reject|allow|deny|yes/no|option|옵션|동의|거절|진행)") {
    $needsApproval = $true
    if (-not $title) {
        $title = "선택 필요 - Codex"
    }
    if (-not $message) {
        $message = $nextAction
    }
}

if (-not $needsApproval) {
    if ($toolFilePath -and ($toolFilePath -notmatch '(^|[\\/])docs[\\/](state\.md|phase[\\/].+[\\/]index\.json)$')) {
        exit 0
    }
    if (-not $toolFilePath) {
        exit 0
    }
    exit 0
}

$suffix = if ($active) { " - 활성 Phase: $active" } else { "" }
$balloonText = "$message$suffix"
if ($balloonText.Length -gt 250) {
    $balloonText = $balloonText.Substring(0, 250)
}

$wav = @(
    "$env:SystemRoot\Media\Windows Notify System Generic.wav",
    "$env:SystemRoot\Media\Windows Notify.wav",
    "$env:SystemRoot\Media\chimes.wav",
    "$env:SystemRoot\Media\ding.wav"
) | Where-Object { Test-Path $_ } | Select-Object -First 1

if ($wav) {
    try {
        (New-Object System.Media.SoundPlayer $wav).Play()
    } catch {
    }
}

try {
    Add-Type -AssemblyName System.Windows.Forms
    Add-Type -AssemblyName System.Drawing
    $n = New-Object System.Windows.Forms.NotifyIcon
    $n.Icon = [System.Drawing.SystemIcons]::Information
    $n.BalloonTipIcon = [System.Windows.Forms.ToolTipIcon]::Warning
    $n.BalloonTipTitle = $title
    $n.BalloonTipText = $balloonText
    $n.Visible = $true
    $n.ShowBalloonTip(6000)
    Start-Sleep -Seconds 6
    $n.Visible = $false
    $n.Dispose()
} catch {
}

exit 0
