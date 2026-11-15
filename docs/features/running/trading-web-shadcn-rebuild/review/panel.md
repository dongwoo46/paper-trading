# 리뷰 패널 — step-1·2 (c0e18a6..52331d6, 머지 커밋 8aba884 제외)

> 2026-11-15 · 코드(code-reviewer) · 정합(spec-reviewer) · QA(tester, 엣지+시험 품질 렌즈)
> 건수: 코드 10 · 정합 5 · QA 11 = 26 → 중복 제거 후 22

## Findings

| ID | file:line | 무엇이 틀렸나 | 등급 | 출처 | 회귀 | 출처 에이전트 | 근거 조항 |
|---|---|---|---|---|---|---|---|
| P9 | frontend/trading-web/proxy.config.ts:22 | trading-api 접두사 `/api/positions`·`/api/trading-journals`·`/api/operations`·`/api/tax-summaries` 누락, 옛 표에선 8080 으로 닿던 경로가 개발 서버 404 | nit | 이 변경 | y | 코드 | TT54 |
| P1 | frontend/trading-web/src/main.test.tsx:27 | root 정리 없이 afterEach 가 matchMedia 대역을 풀어 ThemeProvider effect 와 경합, 간헐 종료 코드 1(강제 재현됨) | should-fix | 이 변경 | n | 코드+정합+QA | TT16 |
| P2 | frontend/trading-web/src/test/guards/rules.ts:56 | float 변환 소스 검사가 단항 `+x`·`x * 1`·`JSON.parse`·`res.json()`·별칭 `Number` 를 못 잡음(D4 가 막으려는 경로) | should-fix | 이 변경 | n | 코드+QA | TT54 |
| P3 | frontend/trading-web/src/shared/api/http.ts:48 | `JSON.stringify(body)` 가 try 안이라 BigInt·순환 참조 TypeError 가 네트워크 오류로 둔갑 | should-fix | 이 변경 | n | 코드+QA | AE21 |
| P4 | frontend/trading-web/package-lock.json:2986 | 생성기 의존성 `cn@0.5.2` 엔진 `>=22` 가 CI Node 20 과 충돌, §9 «멈추고 물어봐라» 상황인데 사람 결정 없이 닫힘 | should-fix | 이 변경 | n | 정합 | label:env-or-local-state-assumed |
| P5 | frontend/trading-web/src/shared/api/error.ts:73 | 서버 오류 원문(rawMessage)에 계좌번호·토큰이 섞이면 가림 없이 전달(EDGE-05 ↔ RULE-11 충돌, 사람 판단 필요) | should-fix | 이 변경 | n | QA | label:missing-input-validation |
| P6 | frontend/trading-web/src/shared/api/decode.ts:82 | `decodeNullable` 이 필드 없음도 null 로 받아 계약 깨짐을 «-» 로 숨김 | should-fix | 이 변경 | n | 코드 | CS14 |
| P7 | frontend/trading-web/src/shared/api/error.ts:62 | FastAPI `detail` 이 객체면 code·원문을 모두 버림(quant-worker 404·409 실사용, 실측) | should-fix | 이 변경 | n | 코드 | AE24 |
| P8 | frontend/trading-web/src/shared/ui/button.test.tsx:12 | 시험이 `container.querySelector` 로 button·h1·main 을 찾음, 역할 쿼리로 바꿔야 함(skeleton-page.test·App.test 포함) | should-fix | 이 변경 | n | QA | TT1 |
| P10 | frontend/trading-web/src/shared/api/error.ts:39 | 사용자 문구를 status 로만 고름, D14 는 status·code | nit | 이 변경 | n | 정합 | label:doc-claim-contradicts-code |
| P11 | frontend/trading-web/src/test/guards/source-guards.test.ts:1 | 계획 §7 의 `console.*` 소스 검사 없음(현재 사용 0건) | nit | 이 변경 | n | 정합 | label:required-state-not-designed |
| P12 | frontend/trading-web/src/shared/lib/decimal.ts:127 | `roundDecimal` 은 D6 의 «BigInt 2함수» 밖 세 번째 BigInt 산술이고 공개 함수 | nit | 이 변경 | n | 정합 | label:rule-lives-in-prose-not-tool |
| P13 | frontend/trading-web/src/shared/lib/decimal.ts:120 | `roundDecimal` 이 fractionDigits 를 검증하지 않음, -1 → `12,340`, 1.5 → `12.35`(실측) | nit | 이 변경 | n | 코드 | CS14 |
| P14 | frontend/trading-web/src/shared/api/error.ts:82 | 서비스를 본문 모양으로 판정해 Spring 기본 오류 본문을 collector-api 로 오판(실측) | nit | 이 변경 | n | 코드 | AE24 |
| P15 | frontend/trading-web/src/shared/api/decode.ts:34 | 숫자 토큰이 문자열로 넘어와 decodeString 이 숫자/문자열을 못 가림 | nit | 이 변경 | n | 코드 | CS14 |
| P16 | frontend/trading-web/src/app/providers/theme-provider.tsx:155 | 전역 «d» 단축키가 listbox·menu 글자 이동 입력에도 반응할 수 있음 | nit | 이 변경 | n | 코드 | FE30 |
| P17 | frontend/trading-web/src/app/providers/theme-provider.tsx:155 | 한글 자판(`key:"ㅇ"`)에서 «d» 단축키가 동작 안 함 | nit | 이 변경 | n | QA | FE30 |
| P18 | frontend/trading-web/src/app/providers/theme-provider.test.tsx:1 | 시스템 테마 change·입력칸·Ctrl+d·storage 동기화 분기 시험 없음 | nit | 이 변경 | n | QA | label:required-state-not-designed |
| P19 | frontend/trading-web/src/shared/api/http.ts:21 | 취소 판별이 `AbortError` 이름 하나뿐, 사유 있는 abort·TimeoutError 가 네트워크 오류로 | nit | 이 변경 | n | QA | AE21 |
| P20 | frontend/trading-web/src/shared/api/error.ts:73 | 숫자만 있는 본문·중복 키 본문이 원문 전체로 들어감 | nit | 이 변경 | n | QA | AE24 |
| P21 | frontend/trading-web/src/shared/lib/decimal.ts:58 | 지수 표기 뒤 끝 0 이 남음(`1.50e1` → `15.0`) | nit | 이 변경 | n | QA | label:output-format-assumption-wrong |
| P22 | frontend/trading-web/src/test/guards/rules.ts:70 | import 검사가 템플릿 문자열 동적 import 를 못 봄 | nit | 이 변경 | n | QA | TT54 |

## 충돌
없음.

## 사람 결정 (2026-11-15 팝업)
- P5: 오류 원문도 가려서 보인다(계좌번호 형식·토큰 모양 가림). 화면 step 에서 반영.
- P4: `cn@0.5.2` 유지, PR CI 결과로 판단. CI 가 실패하면 다시 묻는다.

## 확인 못 함
- 뮤테이션 시험(TT49): 코드 리뷰어가 도구 상한으로 실행 안 함.
- 커버리지 미측정, testing-library ESLint 플러그인 미설치.

<!-- devkit:standard-list hex:7b22636861696e223a22646f63732f66656174757265732f72756e6e696e672f74726164696e672d7765622d73686164636e2d72656275696c642f7265766965772f70616e656c2e6d64222c22726f756e6473223a7b2231223a7b226361757365223a22505220eab8b0eca48020ebb88ceb9e9cecb998eba5bc20eca095ed9598eca78020ebaabbed95a8287472756e6b20ed8c90eca09520ebb688eab08029222c2268656164223a2235323333316436353138363434386534386137653163633433343939613432663434333566336430227d7d7d -->
