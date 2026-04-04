param(
    [string]$RepoUrl = "https://github.com/koreainvestment/open-trading-api.git",
    [string]$CloneDir = ".tools/open-trading-api",
    [string]$ImageTag = "kis-trade-mcp:latest"
)

$ErrorActionPreference = "Stop"
$PSNativeCommandUseErrorActionPreference = $false

if (-not (Get-Command git -ErrorAction SilentlyContinue)) {
    throw "git is not installed."
}

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    throw "docker is not installed."
}

docker info --format '{{.ServerVersion}}' 2>$null | Out-Null
if ($LASTEXITCODE -ne 0) {
    throw "docker daemon is not running. Start Docker Desktop first."
}

if (-not (Test-Path $CloneDir)) {
    git clone $RepoUrl $CloneDir
    if ($LASTEXITCODE -ne 0) {
        throw "git clone failed: $RepoUrl"
    }
} else {
    Write-Host "Skip clone: $CloneDir already exists."
}

$mcpDir = Join-Path $CloneDir "MCP/Kis Trading MCP"
if (-not (Test-Path $mcpDir)) {
    throw "MCP directory not found: $mcpDir"
}

docker build -t $ImageTag $mcpDir
if ($LASTEXITCODE -ne 0) {
    throw "docker build failed: $mcpDir"
}
Write-Host "Built image: $ImageTag"
