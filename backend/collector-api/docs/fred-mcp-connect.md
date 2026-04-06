# FRED MCP 연결

이 프로젝트에서 FRED MCP를 쓰기 위한 최소 설정입니다.

## 1) 이미지 준비

```powershell
./mcp/scripts/setup-fred-mcp.ps1
```

## 2) 환경변수 파일 준비

`.env.fred-mcp` 파일을 직접 만들고 아래 키를 채웁니다.

```dotenv
FRED_API_KEY=your_fred_api_key
```

## 3) 현재 셸에 환경변수 로드 (PowerShell)

```powershell
Get-Content .env.fred-mcp | ForEach-Object {
  if ($_ -match '^\s*#' -or $_ -notmatch '=') { return }
  $pair = $_.Split('=', 2)
  [Environment]::SetEnvironmentVariable($pair[0], $pair[1], 'Process')
}
```

## 4) MCP 클라이언트 설정

다음 파일 내용을 MCP 클라이언트 설정에 반영합니다.

- `mcp/fred-mcp.json`

## 5) 확인

MCP 클라이언트에서 `fred-mcp` 서버가 연결되고 도구 목록이 보이면 완료입니다.

## 참고

- GitHub: https://github.com/stefanoamorelli/fred-mcp-server
- NPM: https://www.npmjs.com/package/fred-mcp-server
