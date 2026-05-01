param(
  [string]$CodexCommand = "codex"
)

$ErrorActionPreference = "Stop"
$promptFile = Join-Path $PSScriptRoot "orchestrate-codex.md"

if (-not (Test-Path -LiteralPath $promptFile)) {
  Write-Error "Prompt file not found: $promptFile"
}

if (-not (Get-Command $CodexCommand -ErrorAction SilentlyContinue)) {
  Write-Error "Codex CLI not found: $CodexCommand"
}

Get-Content -LiteralPath $promptFile -Raw | & $CodexCommand
