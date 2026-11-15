# 코드 리뷰 — trading-web 골격·금융 유틸·API 경계 (c0e18a6..52331d6)

- 범위: `git diff c0e18a6 52331d6 -- frontend/trading-web` 추가·수정 파일 + `.github/workflows/full-test.yml`(8e5bd7c). 옛 앱 삭제분 제외.
- 프로필: consumer (`rule-pointer --list`). 조항 목록: backend-api · common · tests · ui-common · infra · nextjs · always:dev-team.
- 판정: blocker 0 · should-fix 5 · nit 5 → 머지 막는 항목 없음. should-fix 반영을 권합니다.
- 실측: `$DEVKIT_PROBE_ROOT/p1` 에 error.ts·decimal.ts 를 복사해 node 로 실행(F1·F7·F10 결과 아래 기록). 전체 스위트는 다시 돌리지 않았습니다.
- 확인 불가(뮤테이션): TT49 뮤테이션 렌즈는 항목당 도구 호출 상한 때문에 돌리지 않았습니다.

## 1. 규칙 준수

| ID | file:line | 무엇이 틀렸나 | 등급 | 출처 | 회귀 | 확신도 | 근거 조항 |
|---|---|---|---|---|---|---|---|
| F2 | frontend/trading-web/src/shared/api/http.ts:44-56 (48) | try 블록 안에 `JSON.stringify(options.body)` 가 있음. BigInt·순환 참조 body 의 TypeError(프로그래밍 오류)가 «서버에 연결하지 못했습니다» network ApiError 로 바뀌어 원인이 사라짐. 직렬화를 try 앞으로 빼고 fetch 거절만 잡는 쪽이 맞음 | should-fix | 이번 PR | n | 높음 | CS21, AE21 |
| F3 | frontend/trading-web/src/shared/api/decode.ts:80-84, 103 | `decodeNullable` 가 «필드 없음»도 null 로 받음. 서버가 금액 필드 이름을 바꾸면(예: `realizedPnl`→`realizedProfit`) 디코드 실패 대신 화면에 «-» 가 조용히 뜸. «값이 null» 과 «필드 없음» 을 나눠 없음은 DecodeError 로 | should-fix | 이번 PR | n | 중간 | CS14 |
| F4 | frontend/trading-web/src/main.test.tsx:5-9, 27-31 | 알려진 간헐 실패(matchMedia)의 원인 후보: main.tsx 가 만든 root 를 언마운트하지 않은 채 afterEach 가 `vi.unstubAllGlobals()` 로 matchMedia 를 걷음. waitFor 는 커밋(h1)만 기다리고 ThemeProvider 의 useEffect(applyTheme→getSystemTheme→`window.matchMedia`)는 스케줄러로 뒤에 돌아 경합. 효과 완료(`documentElement` 의 light/dark 클래스)까지 기다리거나 root 를 정리해야 함 | should-fix | 이번 PR | n | 중간 | TT16 |
| F5 | frontend/trading-web/src/test/guards/rules.ts:56-57 | RULE-03 검사가 `parseFloat·parseInt·Number(·Math.` 글자만 찾음. 가장 흔한 변환인 단항 `+price`, `price * 1` 은 못 잡음(`JSON.parse`·`.json()` 은 step-2.md:109 에 후속으로 기록됨, 단항 + 는 기록 없음). 이미 있는 `typescript` 로 AST 의 `PrefixUnaryExpression(+)` 를 세는 쪽이 정규식보다 확실 | should-fix | 이번 PR | n | 중간 | TT54 |

## 2. 부작용

| ID | file:line | 무엇이 틀렸나 | 등급 | 출처 | 회귀 | 확신도 | 근거 조항 |
|---|---|---|---|---|---|---|---|
| F6 | frontend/trading-web/proxy.config.ts:22-26 | trading-api 접두사 누락: `/api/positions`(PositionExitTriggerController.kt:21), `/api/trading-journals`(TradingJournalController.kt:28), `/api/operations`(SlackNotificationController.kt:19), `/api/tax-summaries`(TaxSummaryController.kt:21,56). 옛 표 `/api/`→8080 에서는 닿던 경로가 개발 서버 404. 기획 D10 표 그대로라 기획 쪽 누락. proxy-table.test 는 손 목록 내부 일관성만 봐서 못 잡음(백엔드 매핑과 대조 없음). 지금 화면 명세가 쓰는 경로는 아님 | nit | 기획(D10) | y | 높음 | TT54 |
| F9 | frontend/trading-web/src/app/providers/theme-provider.tsx:141-171 | 전역 «d» 단축키가 input·textarea·select·contenteditable 밖 모든 키에 반응. Base UI Select·Menu 의 글자 이동(listbox 포커스)에서 «d» 를 누르면 테마도 바뀔 수 있음. `event.defaultPrevented`·listbox/menu 역할 확인이 없음 | nit | 이번 PR(shadcn 템플릿) | n | 낮음 | FE30 |

## 3. 버그·장애 위험

| ID | file:line | 무엇이 틀렸나 | 등급 | 출처 | 회귀 | 확신도 | 근거 조항 |
|---|---|---|---|---|---|---|---|
| F1 | frontend/trading-web/src/shared/api/error.ts:62-68, 77-79 | FastAPI `detail` 이 객체면 code·원문 둘 다 버림. quant-worker 가 실제로 `detail={"code","message"}` 를 씀(backend/quant-worker/src/backtest/api.py:161-167 404 BACKTEST_RUN_NOT_FOUND, 270-276 409 BACKTEST_ARTIFACT_UNAVAILABLE). 실측: `normalizeApiError(409,{detail:{code:"BACKTEST_ARTIFACT_UNAVAILABLE",...}})` → code null · rawMessage null. 클라이언트가 code 로 가를 수 없음. 시험은 문자열·배열 detail 만 다룸 | should-fix | 이번 PR | n | 높음 | AE24 |
| F7 | frontend/trading-web/src/shared/api/error.ts:82-84 | 서비스를 본문 모양으로 판정. Spring Boot 기본 오류 본문 `{timestamp,status,error,path}`(trading-api 필터 단계 오류 등)도 collector-api 로 판정됨. 실측: `/api/v1/accounts` 401 기본 본문 → `collector-api`. 요청 URL 접두사(프록시 표)가 이미 서비스를 정하므로 `requestJson` 에서 URL 로 정하는 쪽이 근본 | nit | 이번 PR | n | 중간 | AE24 |
| F8 | frontend/trading-web/src/shared/api/decode.ts:34-37 (parse-json.ts:5) | 파서가 숫자 토큰을 그냥 문자열로 넘겨 `decodeString`·`decodeOneOf` 가 JSON 숫자와 문자열을 못 가림. 문자열 필드에 숫자가 와도 타입 불일치가 안 드러남. `parseNumber` 가 표지 객체(예: lossless-json 기본 LosslessNumber)를 남기고 `decodeDecimal`·`decodeSafeInteger` 에서만 풀면 구분됨 | nit | 이번 PR | n | 중간 | CS14 |
| F10 | frontend/trading-web/src/shared/lib/decimal.ts:120-131 | `roundDecimal` 이 `fractionDigits` 를 검증하지 않음. 실측: `formatDecimal("123.45",{fractionDigits:-1})` → `12,340`, `formatDecimal("123.456",{fractionDigits:1.5})` → `12.35`. 금액이 조용히 틀리게 표시됨. 0 이상 정수가 아니면 throw | nit | 이번 PR | n | 높음 | CS14 |

## 확인했고 문제없던 것

- parse-json: lossless-json 4.3.3 `parse(text, undefined, {parseNumber})` 는 옵션 객체 형태를 받음(node_modules/lossless-json/lib/esm/parse.js:25-30). 중복 키·잘못된 JSON 모두 SyntaxError.
- decimal 정규화·반올림(half-up)·비교·자리 이동, quantity BigInt 계산, safe-integer 범위, chart-coordinate 비유한값 거부, mask(AS-01 그대로), idempotency(UUID v4 비트·보안 컨텍스트 밖 getRandomValues) 이상 없음.
- full-test.yml: build·lint 단계 추가, node 20 은 vite 8 요건(20.19+) 충족.

```json
[
  {"file":"frontend/trading-web/src/shared/api/error.ts","line":62,"summary":"FastAPI detail 이 객체({code,message})면 code·원문을 모두 버린다","failure_scenario":"quant-worker 409 {detail:{code:'BACKTEST_ARTIFACT_UNAVAILABLE',message:...}} → ApiError code null·rawMessage null(실측), 화면이 code 로 분기 불가"},
  {"file":"frontend/trading-web/src/shared/api/http.ts","line":48,"summary":"JSON.stringify(body) 가 try 안이라 직렬화 TypeError 가 네트워크 오류로 바뀐다","failure_scenario":"body 에 BigInt 또는 순환 참조 → TypeError → isAbort false → networkApiError «서버에 연결하지 못했습니다», 프로그래밍 오류가 숨음"},
  {"file":"frontend/trading-web/src/shared/api/decode.ts","line":82,"summary":"decodeNullable 이 필드 없음도 null 로 받아 계약 깨짐을 «-» 로 숨긴다","failure_scenario":"서버가 금액 필드 이름 변경 → decodeObject 가 undefined 전달 → null → 화면 «-», 디코드 오류 없음"},
  {"file":"frontend/trading-web/src/main.test.tsx","line":8,"summary":"root 를 정리하지 않은 채 matchMedia 스텁을 걷어 passive effect 와 경합한다","failure_scenario":"waitFor 가 h1 커밋 직후 통과 → afterEach unstubAllGlobals → 늦게 도는 ThemeProvider useEffect 가 window.matchMedia 호출 → 처리 안 된 TypeError(관측된 간헐 실패)"},
  {"file":"frontend/trading-web/src/test/guards/rules.ts","line":56,"summary":"부동소수점 변환 검사가 단항 + 와 산술 강제 변환을 못 잡는다","failure_scenario":"앱 소스에 const p = +order.price 추가 → findForbiddenNumberConversions 0건 → 시험 통과, 금액이 double 로 변환됨"},
  {"file":"frontend/trading-web/proxy.config.ts","line":22,"summary":"trading-api 접두사 /api/positions·/api/trading-journals·/api/operations·/api/tax-summaries 누락","failure_scenario":"개발 서버에서 GET /api/positions/1/exit-triggers → 어느 프록시에도 안 맞아 404(옛 표 /api/→8080 에서는 닿음)"},
  {"file":"frontend/trading-web/src/shared/lib/decimal.ts","line":120,"summary":"roundDecimal 이 fractionDigits 를 검증하지 않아 금액이 틀리게 나온다","failure_scenario":"formatDecimal('123.45',{fractionDigits:-1}) → '12,340', ('123.456',{fractionDigits:1.5}) → '12.35'(실측)"},
  {"file":"frontend/trading-web/src/shared/api/error.ts","line":82,"summary":"서비스를 본문 모양으로 판정해 Spring 기본 오류 본문을 collector-api 로 오판한다","failure_scenario":"trading-api 필터 단계 401 {timestamp,status,error,path} → service 'collector-api'(실측)"},
  {"file":"frontend/trading-web/src/shared/api/decode.ts","line":34,"summary":"숫자 토큰이 문자열로 넘어와 decodeString·decodeOneOf 가 숫자/문자열을 못 가린다","failure_scenario":"문자열 필드에 서버가 숫자 123 을 보냄 → decodeString 통과 '123', 타입 불일치 미검출"},
  {"file":"frontend/trading-web/src/app/providers/theme-provider.tsx","line":155,"summary":"전역 d 단축키가 listbox·menu 글자 이동 입력에도 반응할 수 있다","failure_scenario":"Base UI Select 열고 d 입력(포커스는 listbox div) → isEditableTarget false → 테마 전환"}
]
```
