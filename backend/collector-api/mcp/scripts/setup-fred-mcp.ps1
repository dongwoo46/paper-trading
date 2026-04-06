param(
    [string]$ImageTag = "stefanoamorelli/fred-mcp-server:latest"
)

$ErrorActionPreference = "Stop"
$PSNativeCommandUseErrorActionPreference = $false

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    throw "docker is not installed."
}

docker info --format '{{.ServerVersion}}' 2>$null | Out-Null
if ($LASTEXITCODE -ne 0) {
    throw "docker daemon is not running. Start Docker Desktop first."
}

docker pull $ImageTag
if ($LASTEXITCODE -ne 0) {
    throw "docker pull failed: $ImageTag"
}

Write-Host "Pulled image: $ImageTag"
