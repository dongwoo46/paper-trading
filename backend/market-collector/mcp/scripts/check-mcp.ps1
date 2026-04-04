param(
    [switch]$Fix
)

$ErrorActionPreference = "Stop"
$PSNativeCommandUseErrorActionPreference = $false

function Load-EnvFile {
    param([string]$Path)
    if (-not (Test-Path $Path)) {
        return $false
    }

    Get-Content $Path | ForEach-Object {
        if ($_ -match '^\s*#' -or $_ -notmatch '=') { return }
        $pair = $_.Split('=', 2)
        [Environment]::SetEnvironmentVariable($pair[0], $pair[1], 'Process')
    }
    return $true
}

function Ensure-McpServer {
    param(
        [string]$Name,
        [string]$AddCommand
    )

    cmd /c "codex mcp get $Name >nul 2>nul"
    if ($LASTEXITCODE -eq 0) {
        Write-Host "[OK] MCP server registered: $Name"
        return
    }

    Write-Host "[WARN] MCP server missing: $Name"
    if (-not $Fix) { return }

    Invoke-Expression $AddCommand
    if ($LASTEXITCODE -eq 0) {
        Write-Host "[FIXED] MCP server registered: $Name"
    } else {
        Write-Host "[ERROR] Failed to register MCP server: $Name"
    }
}

if (-not (Get-Command codex -ErrorAction SilentlyContinue)) {
    throw "codex CLI not found."
}

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    throw "docker not found."
}

docker info --format '{{.ServerVersion}}' 2>$null | Out-Null
if ($LASTEXITCODE -ne 0) {
    throw "docker daemon is not running."
}

$hasKisEnv = Load-EnvFile ".env.kis-mcp"
$hasFredEnv = Load-EnvFile ".env.fred-mcp"

if ($hasKisEnv) {
    Write-Host "[OK] Loaded .env.kis-mcp"
} else {
    Write-Host "[WARN] Missing .env.kis-mcp"
}

if ($hasFredEnv) {
    Write-Host "[OK] Loaded .env.fred-mcp"
} else {
    Write-Host "[WARN] Missing .env.fred-mcp"
}

docker image inspect kis-trade-mcp:latest *> $null
if ($LASTEXITCODE -eq 0) {
    Write-Host "[OK] Docker image exists: kis-trade-mcp:latest"
} else {
    Write-Host "[WARN] Docker image missing: kis-trade-mcp:latest"
    if ($Fix) {
        ./mcp/scripts/setup-kis-mcp.ps1
    }
}

docker image inspect stefanoamorelli/fred-mcp-server:latest *> $null
if ($LASTEXITCODE -eq 0) {
    Write-Host "[OK] Docker image exists: stefanoamorelli/fred-mcp-server:latest"
} else {
    Write-Host "[WARN] Docker image missing: stefanoamorelli/fred-mcp-server:latest"
    if ($Fix) {
        ./mcp/scripts/setup-fred-mcp.ps1
    }
}

$kisAcct = [Environment]::GetEnvironmentVariable("KIS_ACCT_STOCK", "Process")
if (-not $kisAcct) { $kisAcct = "" }

$kisAdd = @(
    "codex mcp add kis-trading",
    "--env `"MCP_TYPE=stdio`"",
    "--env `"KIS_FORCE_CONFIG_UPDATE=0`"",
    "--env `"KIS_APP_KEY=$env:KIS_APP_KEY`"",
    "--env `"KIS_APP_SECRET=$env:KIS_APP_SECRET`"",
    "--env `"KIS_PAPER_APP_KEY=$env:KIS_PAPER_APP_KEY`"",
    "--env `"KIS_PAPER_APP_SECRET=$env:KIS_PAPER_APP_SECRET`"",
    "--env `"KIS_HTS_ID=$env:KIS_HTS_ID`"",
    "--env `"KIS_ACCT_STOCK=$kisAcct`"",
    "-- docker run --rm -i",
    "-e MCP_TYPE -e KIS_FORCE_CONFIG_UPDATE",
    "-e KIS_APP_KEY -e KIS_APP_SECRET",
    "-e KIS_PAPER_APP_KEY -e KIS_PAPER_APP_SECRET",
    "-e KIS_HTS_ID -e KIS_ACCT_STOCK",
    "kis-trade-mcp:latest"
) -join " "

$fredAdd = @(
    "codex mcp add fred-mcp",
    "--env `"FRED_API_KEY=$env:FRED_API_KEY`"",
    "-- docker run --rm -i -e FRED_API_KEY",
    "stefanoamorelli/fred-mcp-server:latest"
) -join " "

Ensure-McpServer -Name "kis-trading" -AddCommand $kisAdd
Ensure-McpServer -Name "fred-mcp" -AddCommand $fredAdd

Write-Host ""
Write-Host "MCP summary:"
codex mcp list
