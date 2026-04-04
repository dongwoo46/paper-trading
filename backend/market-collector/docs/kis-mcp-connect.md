# KIS Trading MCP 연결

이 프로젝트에서 한국투자증권 MCP를 쓰기 위한 최소 설정입니다.

## 1) 이미지 빌드

```powershell
./mcp/scripts/setup-kis-mcp.ps1
```

## 2) 환경변수 파일 준비

`.env.kis-mcp` 파일을 직접 만들고 아래 키를 채웁니다.

```dotenv
KIS_APP_KEY=your_live_app_key
KIS_APP_SECRET=your_live_app_secret
KIS_PAPER_APP_KEY=your_paper_app_key
KIS_PAPER_APP_SECRET=your_paper_app_secret
KIS_HTS_ID=your_hts_id
KIS_ACCT_STOCK=12345678
```

## 3) 현재 셸에 환경변수 로드 (PowerShell)

```powershell
Get-Content .env.kis-mcp | ForEach-Object {
  if ($_ -match '^\s*#' -or $_ -notmatch '=') { return }
  $pair = $_.Split('=', 2)
  [Environment]::SetEnvironmentVariable($pair[0], $pair[1], 'Process')
}
```

## 4) MCP 클라이언트 설정

다음 파일 내용을 MCP 클라이언트 설정에 반영합니다.

- `mcp/kis-trading.mcp.json`

## 5) 확인

MCP 클라이언트에서 `kis-trading` 서버가 연결되고 도구 목록이 보이면 완료입니다.

## 참고

- KIS 공식 저장소: https://github.com/koreainvestment/open-trading-api
- KIS MCP 폴더: https://github.com/koreainvestment/open-trading-api/tree/main/MCP/Kis%20Trading%20MCP
