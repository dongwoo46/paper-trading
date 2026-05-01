$inputJson = [Console]::In.ReadToEnd()

try {
    $payload = $inputJson | ConvertFrom-Json -ErrorAction Stop
} catch {
    $payload = $null
}

$cmd = ""
if ($payload -and $payload.tool_input -and ($payload.tool_input.PSObject.Properties.Name -contains "command")) {
    $cmd = [string]$payload.tool_input.command
}

$blocked = $false
$reason = ""
$isFullSuite = $false

if ($cmd -match "rm\s+-rf\s+[^/]*(src|backend|frontend|docs|\.claude)") {
    $blocked = $true
    $reason = "BLOCKED: 소스 디렉토리 대상 rm -rf 감지"
}

if ($cmd -match "git\s+reset\s+--hard|git\s+push\s+(--force|-f)|git\s+clean\s+-f") {
    $blocked = $true
    $reason = "BLOCKED: 되돌릴 수 없는 git 명령 감지 (--hard reset / force push / clean -f)"
}

if ($cmd -match "(?i)DROP\s+TABLE|TRUNCATE\s+TABLE|DROP\s+DATABASE") {
    $blocked = $true
    $reason = "BLOCKED: 파괴적 DB 명령 감지 (DROP/TRUNCATE)"
}

if ($cmd -match "rm.*V[0-9]+__.*\.sql") {
    $blocked = $true
    $reason = "BLOCKED: Flyway 마이그레이션 파일 삭제 시도"
}

if (($cmd -match "(?i)gradlew(\.bat)?([^;&|]*\s|[ \t])test([ \t]|$)") -and ($cmd -notmatch "--tests")) {
    $isFullSuite = $true
}

if (($cmd -match "(?i)(^|[ \t])(python[0-9.]*\s+-m\s+)?pytest(\.exe)?\s+tests/?([ \t]|$)") -and ($cmd -notmatch "::|tests/[^ \t]*test_[^ \t]+\.py")) {
    $isFullSuite = $true
}

if (($cmd -match "(?i)npm(\.cmd)?\s+(run\s+)?test([ \t]|$)") -and ($cmd -notmatch "--run\s+[^ \t]+\.(test|spec)\.(ts|tsx|js|jsx)|[^ \t]+\.(test|spec)\.(ts|tsx|js|jsx)")) {
    $isFullSuite = $true
}

if ($isFullSuite) {
    $phase = "unknown"
    $currentStep = 0
    $totalSteps = 0

    if (Test-Path "docs/state.md") {
        $state = Get-Content "docs/state.md" -Raw
        $m = [regex]::Match($state, "## 활성 Phase\s*\r?\n-\s*([^/\s]+)/([^|\s]+)")
        if ($m.Success) {
            $phase = "$($m.Groups[1].Value)/$($m.Groups[2].Value)"
            $indexPath = "docs/phase/$($m.Groups[1].Value)/$($m.Groups[2].Value)/index.json"
            if (Test-Path $indexPath) {
                try {
                    $index = Get-Content $indexPath -Raw | ConvertFrom-Json
                    $currentStep = [int]$index.current_step
                    $totalSteps = [int]$index.total_steps
                } catch {
                }
            }
        }
    }

    if ($currentStep -gt 0 -and $totalSteps -gt 0 -and $currentStep -lt $totalSteps) {
        $blocked = $true
        $reason = "BLOCKED: 중간 phase step($phase step $currentStep/$totalSteps)에서 전체 테스트 스위트 실행 시도"
    }
}

if ($blocked) {
    Write-Output $reason
    Write-Output "사용자 명시적 승인 없이 이 명령을 실행하지 마세요."
    if ($isFullSuite) {
        Write-Output "현재 step에서는 phase에서 추가/변경한 테스트만 targeted로 실행하세요."
        Write-Output "예: ./gradlew test --tests `"com.papertrading.api.<package>.<TestClass>`""
        Write-Output "전체 테스트는 마지막 phase completion gate에서만 실행합니다."
    }
    exit 1
}

exit 0
