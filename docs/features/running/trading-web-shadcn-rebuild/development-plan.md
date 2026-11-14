# Development Plan — trading-web 전면 재제작 (shadcn/ui), 0·1단계

> Tech Planner 산출물입니다. 승인된 feature-spec 을 받아 «어떻게 만들지 + 지금 만들 수 있나»를 정합니다.
> 기반: [feature-spec.md](./feature-spec.md) · [screen-spec-v3.md](./screen-spec-v3.md) · [open-questions.md](./open-questions.md) / 다음: state.md + steps/step-N.md
> 합의되면 고정합니다. 진행 상태는 여기 적지 않고 state.md 에 적습니다. 불가능한 부분(🔴)은 구현으로 넘기지 않고 §8-2 사람 결정으로 올립니다.
> 줄 번호는 worktree `feature/trading-web-shadcn-rebuild` 4697501 기준입니다. 경로 줄임: `TA:` = `backend/trading-api/src/main/kotlin/com/papertrading/api/`, `CA:` = `backend/collector-api/src/main/kotlin/com/papertrading/collector/`, `FE:` = 삭제 전 `frontend/trading-web/`.
> 이름표: `D`(이 문서의 기술 결정), `DEC`(사람이 정할 항목), `AS`(사소해서 가정하고 진행하는 항목), `RISK`(위험).

---

## 0. 메타데이터

| 항목 | 값 |
|---|---|
| 기능 | `trading-web-shadcn-rebuild` |
| 기반 feature-spec | `feature-spec.md`(승인 2026-11-15, 같은 날 FLAG-16~21 반영) + `screen-spec-v3.md`(승인 2026-11-15, §10·§11 갱신) |
| 작성자 | Claude (tech-planning, Tech Planner) |
| 날짜 | 2026-11-15 |
| tier | 반자동 (`.claude/stack-profile.json` `tier`) |
| 대상 repo / 브랜치 | paper-trading / `feature/trading-web-shadcn-rebuild` (worktree `.worktrees/trading-web-shadcn-rebuild`) |
| 상태 | in-review — DEC-02~09 사용자 결정 반영(2026-11-15). 남은 것: tech-plan 승인, DEC-01(삭제 실행)은 착수 직전 재확인 |

---

## 1. 구현 가능성 판단 (착수 전 점검)

| 항목 | 값 |
|---|---|
| feature-spec 준비됨? | ready (고위험 ASK 0, FLAG-08 대조 완료, FLAG-16~21 확정) |
| 막힌 ASK (open-questions) | none |
| **판정** | 🟡 일부 조건부 — 결정 전 🔴 2곳(포트폴리오 C 기간 수익률, 원장 대사 E 의 v3 명세 전체)은 사용자 결정으로 해소(DEC-02 뺌, DEC-04 불일치 목록으로 축소). 남은 조건: 삭제 직전 사람 재확인(DEC-01), 선행 작업(무손실 파싱·프록시 — §4-2) |
| 코멘트 | 0·1단계 화면의 뼈대(계좌·포지션·주문·취소·체결 SSE·세금·구독 관리)는 기존 API 로 됩니다. 계획 전체를 좌우하는 사실은 세 가지입니다. 1. trading-api 응답의 BigDecimal 은 JSON 숫자로 나옵니다(문자열 직렬화 설정 없음). 브라우저 기본 파싱은 이를 double 로 바꾸므로 경계에서 막아야 합니다(D4). 2. FLAG-04(금액 계산은 서버만) 때문에 합계·비중·기간 수익률·예상 금액처럼 서버 값이 없는 영역은 뺄 수밖에 없습니다(v3 원칙 6, 목록 v3 §11). 3. 옛 프런트의 API 경로·필드가 지금 백엔드와 여러 곳 어긋나 재사용하지 않습니다(D3). |

**부분별 가능성** (판정은 DEC 반영 뒤 상태. 🟢 가능 · 🟡 조건부 · 🔴 불가→사람 결정 · 🟠 API 없음→뺌(v3 원칙 6, RULE-13, 목록 v3 §11))

| 화면 · 영역 | 판정 | 근거 (연 파일:줄) | 처리 |
|---|---|---|---|
| 셸 · 좌측 내비(서비스명·1단계 그룹) | 🟢 | 정적 | v3 §2-1 |
| 셸 · 내비 상태 점(브로커 연동) | 🟡 | KIS 시세 WS 만: `GET /api/kis/ws/health` (CA:presentation/kis/KisWsHealthController.kt:9-14,17,21) | KIS WS 상태로만 산출. 토큰·토스 상태 없음 |
| 셸 · 내비 상태 점(수집 현황) | 🟢 | `GET /api/subscriptions/status` (CA:presentation/subscriptions/SubscriptionStatusController.kt:13,17) | `connectionStatus` |
| 셸 · 상단 계좌 선택 | 🟢 | `GET /api/v1/accounts` (TA:presentation/controller/AccountController.kt:59-64, dto/account/AccountResponse.kt:13-27) | LOCAL·KIS_PAPER 만(AS-06). 첫 활성 계좌 자동 선택(FLAG-14) |
| 셸 · 데이터 기준 시각·상태 점 | 🟢 | 응답 수신 시각 + SSE 연결 상태 | EDGE-02, AS-05 |
| 셸 · 알림 패널(종) | 🟠 | 앱 내 알림 목록 API 없음(§4-1 18행) | 뺌. 체결 토스트는 유지 |
| 셸 · 체결 토스트 | 🟢 | `GET /api/v1/executions/stream` 이벤트 `execution` (TA:presentation/controller/ExecutionSseController.kt:14,20; infrastructure/sse/ExecutionSseRegistry.kt:35), 값은 문자열 (dto/sse/ExecutionSseEvent.kt:12-14,25-27) | 최대 5개, 자동 닫힘 |
| 셸 · 테마·768 미만 서랍·내비 접기 | 🟢 | 프런트 단독 | shadcn 프리미티브 |
| 브로커 · A 안내 줄 | 🟢 | 정적 | |
| 브로커 · B KIS 모의 카드 채널 줄(WS) | 🟡 | KisWsHealth(mode·status·lastConnectedAt·reconnectAttempts) | 시세 WS 만. 주문 체결 WS 상태 API 없음 |
| 브로커 · B 연동 상태 배지·토큰 만료·호출 성공/실패 수·REST 채널·«토큰 재발급»·«연결 확인»·미설정 카드 | 🟠 | 토큰 상태·재발급 API 없음(토큰은 TA:infrastructure/kis/KisTokenManager.kt 내부 전용) | 뺌 |
| 브로커 · B 토스 카드 | 🟠 | 토스 API 없음(다른 세션 계획 중) | 숨김(DEC-05, FLAG-20) |
| 브로커 · C 연결 계좌(KIS 모의) | 🟡 | `GET /api/kis/account/balance?accountId&mode=PAPER` (TA:presentation/controller/KisAccountBalanceController.kt:12,16-20; application/account/result/KisAccountBalanceResult.kt:8-19). 계좌번호 = `AccountResponse.externalAccountId`(AccountResponse.kt:23, 형식 `XXXXXXXX-XX` — infrastructure/kis/KisAccountBalanceAdapter.kt:37-40,116) | 로컬 KIS_PAPER 계좌마다 누를 때만 조회(RISK-08). 마스킹(AS-01) |
| 브로커 · C 토스 행, D 최근 호출·오류 | 🟠 | 해당 API 없음 | 뺌 |
| 수집 · A 갱신 줄 | 🟢 | 프런트 | |
| 수집 · B KIS 소스 카드·채널 줄 | 🟢 | subscriptions/status, ws/health, `GET /api/subscriptions/routing-status` (CA:presentation/subscriptions/SubscriptionRoutingController.kt:65), `GET /api/internal/kis/orderbook/ingest-health` (CA:presentation/kis/OrderbookIngestHealthController.kt:11,15) | ingest-health 는 내부 경로(RISK-11) |
| 수집 · B Upbit 카드, C 지연 시계열, D Redis, E PostgreSQL, F 버퍼, G 오류 로그 | 🟠 | Upbit 컨트롤러는 카탈로그·구독뿐(CA:presentation/upbit/UpbitMarketController.kt:18-122), 나머지 매핑 없음 | 뺌 |
| 구독 관리 · KIS(WS/REST)·Upbit·pykrx·yfinance·FRED 카탈로그·구독·해지 | 🟢 | collector 매핑(CA:presentation/kis/KisSymbolCatalogController.kt:23,30,73,96,141 외). 카탈로그 응답은 타입 없는 `Map<String, Any>`(같은 파일:38) | 런타임 디코더 필수(RISK-10). 해지 확인 단계 |
| 구독 관리 · 작업: 카탈로그 동기화 | 🟢 | Upbit·pykrx·FRED sync 매핑. yfinance 없음 | |
| 구독 관리 · 작업: 일봉 일괄 수집 | 🟡 | quant-worker `POST /collect/daily` (backend/quant-worker/src/interfaces/api/app.py:23-39,117). 옛 프런트의 `/api/collect/daily` 에 대응하는 백엔드 없음 | 프록시 경로 신설(D10). 요청이 끝날 때까지 응답 대기 |
| 홈 · A 시장 지수 | 🟠 | 지수 API 없음 | 뺌 |
| 홈 · B KPI 예수금, 미체결 주문 수 | 🟢 | `deposit`(AccountResponse.kt:19), `GET …/orders?status=` 개수(TA:presentation/controller/OrderController.kt:57-73) | 개수는 금액 계산 아님 |
| 홈 · B KPI 총자산·누적 손익 | 🟢 | 일별 잔고 마지막 행(dto/portfolio/PortfolioSnapshotDto.kt:9-16). 일별 잔고는 수동 POST 로만 생성(PortfolioSnapshotController.kt:45, `@Scheduled` 없음) | «YYYY-MM-DD 기준» 병기, 없으면 빈 상태(DEC-06, FLAG-19) |
| 홈 · B KPI 오늘 손익·현금 비중·오늘 체결 건수 | 🟠 | 서버 값 없음, 화면 계산은 FLAG-04 위반, 계좌 단위 체결 목록 없음 | 뺌 |
| 홈 · C 연동·수집 요약 | 🟡 | KIS WS·subscriptions/status 만 | |
| 홈 · D 최근 체결 5건 | 🟠 | 계좌 단위 체결 목록 없음(OrderController.kt:90-112 는 주문별만) | 뺌(DEC-07, FLAG-21) |
| 홈 · E 자산 추이 | 🟢 | `GET /api/accounts/{id}/daily-balances?fromDate&toDate` (PortfolioSnapshotController.kt:21,27-33). KOSPI 벤치마크 없음 → 부분 경고(spec §5 예시 그대로) | 좌표 변환 함수 한 곳(DEC-03, FLAG-17), 비면 빈 상태 |
| 홈 · F 알림 최근 4건 | 🟠 | 알림 목록 API 없음 | 뺌 |
| 계좌 · A 계좌 카드 | 🟢 | AccountResponse(모드·유형·예수금·가용·활성) | |
| 계좌 · B KPI 예수금·가용 예수금·홀드 금액 | 🟢 | `deposit`·`availableDeposit`·`lockedDeposit` (AccountResponse.kt:19-21) | «가용 = 예수금 − 홀드»는 문구만, 값은 서버 `availableDeposit` |
| 계좌 · B KPI 총자산·평가금액·미실현·누적 수익률 / 초기 자본·실현 손익 | 🟢 / 🟠 | 일별 잔고 기준일 값(DEC-06) / API 없음 | |
| 계좌 · C 포지션 표(종목·시장·수량·매도 가능·잠금 수량·평균단가·현재가·평가금액·미실현·수익률·가격소스) | 🟢 | dto/position/PositionResponse.kt:9-22. 종목명은 `GET /api/symbols/kr` (CA:presentation/kis/KrSymbolController.kt:13,17) | 정렬은 10진 문자열 비교(D5) |
| 계좌 · C 비중·당일손익·합계 행 | 🟠 | 서버 값 없음, 합산은 FLAG-04 위반 | 뺌 |
| 계좌 · D 상세 평가금액·미실현 손익·«주문» 이동 / 매입금액·종목 실현 손익·52주 | 🟢 / 🟠 | PositionResponse / 해당 값 없음 | |
| 계좌 · E 원장 대사 — 불일치 목록 표시(축소안) | 🟢 | `reconciliation{missingInLocal, missingInKis, quantityMismatch}` 종목 코드 목록(application/account/result/KisReconciliationResult.kt:3-7, 계산 application/account/KisAccountQueryService.kt:27-36) + 로컬 포지션·KIS 잔고 수량 원문 | DEC-04, FLAG-18. 요청은 GET 만 |
| 계좌 · E 차이·처리 상태·«확인함»·예수금/미체결 대사·대사 기록 | 🟠 | 대사 실행 기록·값 차이·«확인함»(RULE-12 처리자 기록) API 없음 | 뺌(DEC-04) |
| 주문 · A 종목 선택 | 🟢 | `/api/symbols/kr` | |
| 주문 · A 헤더 현재가·기준 시각 | 🟡 | `GET /api/market/features/{symbol}` `close`·`asOf` 문자열(CA:presentation/marketfeature/MarketFeatureController.kt:16,20; dto/MarketFeatureResponse.kt:3-17). 수집 중인 종목만 | 없으면 «-»(EDGE-06) |
| 주문 · A 헤더 전일대비·시고저·거래량·거래대금·52주·베타·섹터 | 🟠 | KIS 원본 통과 API(`/api/kis/rest/watchlist/price`, CA:presentation/kis/KisRestWatchlistController.kt:34,38,51 — `JsonNode` 그대로)는 필드 계약이 저장소에 없음 | 뺌 |
| 주문 · B 호가 10단계, C 최근 체결 틱 30건 | 🟠 | microstructure 는 최우선 호가·상위 N 잔량 합 요약뿐(CA:presentation/market/dto/MarketMicrostructureDto.kt:23-36), bars 는 1분봉(CA:presentation/marketbar/MarketBarController.kt:17-25) | 뺌 |
| 주문 · D 폼(방향·유형·조건·가격·수량·확인 단계·멱등키·제출 중 막기) | 🟢 | `PlaceOrderRequest`(TA:presentation/dto/order/OrderRequest.kt:13-25, `idempotencyKey`:22), 멱등 범위 `(account_id, idempotency_key)` 유일(backend/trading-api/src/main/resources/db/migration/V8__add_order_idempotency_scope.sql:2) | RULE-04, EDGE-01 |
| 주문 · D 주문가능수량·가용 예수금 | 🟢 | `orderableQuantity`, `availableDeposit` | |
| 주문 · D 예상 금액·수수료·거래세·총액·«홀드될 금액», F 리스크 경고 | 🟠 | 서버 계산 API 없음 | 뺌. 확인 단계에 «체결 금액은 체결 후 확정됩니다»(v3 §6-5). 매도 «잠길 수량»은 입력 수량 그대로라 표시(v3 §6-4) |
| 주문 · D ±1틱·호가단위 검증·빠른 비율 | 🟠 | 호가단위 서버 값 없음, 비율은 금액 계산 | 뺌. 검증은 서버 응답 문구로 |
| 주문 · D 주문 메모 | 🟠 | 서버 필드 없음 | 숨김(FLAG-13) |
| 주문 · E 미체결 표·취소(확인 단계) | 🟢 | GET orders, DELETE (OrderController.kt:57,81) | |
| 주문 · E 정정 | 🟠 | PUT/PATCH 매핑 없음 | 취소만 |
| 주문 · E 부분 체결 진행 막대·«남은 수량» | 🟢 | `quantity`·`filledQuantity` | BigInt 정수 연산(D6) |
| 주문 · 비활성 계좌 폼 막기 | 🟢 | `isActive` | EDGE-04 |
| 거래 내역 · A 필터(기간·계좌·구분·종목·상태) | 🟢 | `ticker`·`status`·`createdFrom`·`createdTo` (OrderController.kt:58-63), 구분은 화면 필터 | |
| 거래 내역 · B KPI 5개 | 🟠 | 집계 API 없음, 합산은 FLAG-04 위반 | 뺌 |
| 거래 내역 · C-1 주문 이력 표·페이지(20건) | 🟢 | OrderResponse(dto/order/OrderResponse.kt:15-29). 서버 페이징 없음 → 받은 목록을 화면에서 나눔 | |
| 거래 내역 · C-1 사유·메모·합계 행·상태 이력 | 🟠 | `rejectReason`·메모·상태 이력 없음, 합산 금지 | 뺌/숨김 |
| 거래 내역 · C-1 행 펼침 연결 체결 | 🟢 | `GET …/orders/{id}/executions` (OrderController.kt:90-104; OrderResponse.kt:65-71) | 체결은 여기서만 봄(DEC-07) |
| 거래 내역 · C-2 체결 이력 탭 | 🟠 | 계좌 단위 체결 목록 없음 | 뺌(DEC-07, FLAG-21) |
| 거래 내역 · C-3 종목별 손익 탭 | 🟠 | API 없음 | 뺌 |
| 거래 내역 · D CSV 내려받기 | 🟢 | 받은 행 문자열 그대로 | AS-08 |
| 성과 · 포트폴리오 A KPI 총자산·현금·총 손익 / 포지션 수 / 투자금액 | 🟢 / 🟢 / 🟠 | DailyBalanceResponse / positions 개수 / 원가 합계 API 없음 | 기준일 병기(DEC-06) |
| 성과 · 포트폴리오 B 자산 추이 | 🟢 | daily-balances | 좌표 변환 함수 한 곳(DEC-03) |
| 성과 · 포트폴리오 C 기간 수익률 | 🟠 | 서버 값 없음. 화면 계산은 FLAG-04 위반, 백엔드 신규는 FLAG-01 위반(결정 전 🔴) | 뺌(DEC-02, FLAG-16) |
| 성과 · 포트폴리오 D·E·F | 🟠 | v3 가 이미 뺌 | |
| 성과 · 세금 탭(연도 6년·요약·재계산 확인·READY/RUNNING/FAILED) | 🟢 | TA:presentation/controller/TaxSummaryController.kt:21,28,47; dto/portfolio/TaxSummaryDto.kt:9-18 | 요약 표 열 = 서버 필드(`totalRealizedPnl`·`taxablePnl`·`estimatedTax`·`status`·`computedAt`, v3 §9-2) |

---

## 2. 아키텍처·통합 결정

| 결정 | 선택 | 근거 | 기각한 대안 |
|---|---|---|---|
| D1 새 앱 생성 | shadcn CLI 로 같은 경로에 Vite 프로젝트 생성: `npx shadcn@4.23.0 init -t vite -b base -p base-nova`(정확한 조합은 step 1 에서 `--help` 로 재확인) | 공식 Vite 설치 문서의 새 프로젝트 명령이 `npx shadcn@latest init -t vite`(ui.shadcn.com/docs/installation/vite, 2026-11-15 조회). 설치된 CLI 4.23.0 의 `init --help` 가 `-t vite`·`-b base|radix|aria`·`-p` 를 지원(ds-lab `node_modules`, npm 최신도 4.23.0 — 2026-11-15 `npm view`). 루트 CLAUDE.md «shadcn/ui Base Nova», 선택된 프로토타입도 `"style": "base-nova"`(.worktrees/ds-lab/design-lab/shadcn/components.json:3) | 프로토타입 폴더 복사: v2 12화면·가짜 데이터 구조라 범위가 다름. create-vite 후 수동 init: 결과는 같고 단계만 늘어남. radix base: CLAUDE.md·프로토타입과 다름 |
| D2 옛 trading-web 삭제 시점·방법 | step 1 첫 작업. 사람 재확인(DEC-01) 직후 «D15 고정 자료 생성 → `git rm -r frontend/trading-web` → D1 골격 생성 → build·lint·test 통과»를 한 커밋으로 | FLAG-06 B(제자리). 커밋마다 `npm run build` 가 돈다(.claude/stack-profile.json:59-73, compile phases `commit`) → 앱 없는 커밋을 만들 수 없음. 옛 코드가 작업 트리에 남지 않아 RULE-10 위반 경로가 물리적으로 없어짐. 대상은 추적 파일 146개뿐이고 무시·미추적 파일 0(`git status --ignored` 확인) → `git revert` 한 번으로 복구 가능 | 새 앱을 옆 폴더에서 다 만든 뒤 삭제: 사람이 고른 B 와 다르고 옛 코드가 옆에 남아 섞임(이력: open-questions.md FLAG-06 A 미선택). 화면 하나씩 바꿔 끼우기: 같은 `src`·`index.css`·`package.json` 공유로 옛 디자인 자산이 섞임(RULE-10). 삭제만 먼저 커밋: 커밋 시 빌드 검사에 걸림 |
| D3 옛 빌드 설정·API 클라이언트 재사용 | 재사용 0. 이 계획의 대조 근거로만 읽고 새로 씀 | 옛 클라이언트가 지금 백엔드와 어긋남: 포트폴리오 경로 `/api/v1/accounts/{id}/daily-balances?from&to`(FE:src/shared/api/portfolioApi.ts:25) ↔ 백엔드 `/api/accounts/{id}/daily-balances?fromDate&toDate`(PortfolioSnapshotController.kt:21,27-31). 필드 `date·evaluationAmount·deposit`(portfolioApi.ts:4-8) ↔ `businessDate·cashBalance·stockMarketValue·totalAssetValue·pnlAmount·pnlRate`(PortfolioSnapshotDto.kt:9-16). 세금 타입(FE:src/entities/tax-summary/model/types.ts:3-14) ↔ TaxSummaryDto.kt:9-18. PriceSource `REDIS|NONE`(FE:src/entities/account/model/types.ts:4) ↔ `REDIS_LIVE·REDIS_STALE·MANUAL·UNKNOWN`(TA:domain/enums/PriceSource.kt:3-8). `Number()`(portfolioApi.ts:16)·`parseFloat`(FE:src/shared/utils/format.ts:3,12)는 RULE-03 위반. 서버 오류 본문 그대로 노출(FE:src/shared/api/index.ts:8-11)은 EDGE-05 위반. 프록시가 `/api/` 전부를 8080 으로 보냄(FE:vite.config.ts:17-20) ↔ collector 는 호스트 8081(docker-compose.yml:88-89). 옛 fetch 래퍼는 13줄이라 새로 쓰는 비용이 작음 | 클라이언트·타입 그대로 가져오기: 어긋난 계약을 조용히 이어받음. 빌드 설정 파일 복사: 생성기가 같은 것을 만들고 프록시 표는 어차피 새로 짜야 함 |
| D4 금융 값 경계(파싱) | 응답 본문을 text 로 받아 `lossless-json` 4.3.3 `parse(text, undefined, { parseNumber: (s) => s })` 로 모든 JSON 숫자를 원문 문자열로 보존 → 엔드포인트별 디코더(`unknown` + 타입 가드)가 id·개수는 안전 정수 검사 후 number, 금액·수량·가격·비율은 정규화한 10진 문자열로 넘김. 지수 표기(`1E-8`)는 문자열 연산으로 평문화 | trading-api 는 BigDecimal 문자열 직렬화 설정이 없음(main 코드·application.yaml 의 Jackson 설정 검색 0건), 시험도 숫자로 단언(backend/trading-api/src/test/…/AccountControllerIntegrationTest.kt:63 `value(1000000)`, KisAccountBalanceControllerIntegrationTest.kt:81 `value(1.2)`). 토스 세션도 «기존 API 는 그대로, 새 토스 API 만 문자열»로 사용자 결정(.worktrees/multi-broker-kis-toss/docs/features/running/multi-broker-kis-toss/open-questions.md:47). 수량 최소값 `0.00000001`(OrderRequest.kt:19)은 Java `BigDecimal.toString` 규칙상 `1E-8` 로 나올 수 있음(실측 못 함). lossless-json: «Parse JSON without risk of losing numeric information», MIT, `parseNumber` 옵션(npm readme, 2026-11-15 조회). 문자열·숫자 토큰 어느 쪽이 와도 같은 결과 | `response.json()`: double 로 바뀌어 RULE-03 을 화면만으로는 못 지킴. 백엔드 직렬화 변경: FLAG-01 과 토스 세션의 «기존 API 그대로» 결정에 충돌. `JSON.parse` reviver 원문 접근: 브라우저·jsdom 지원 범위 확인 못 함 |
| D5 화면 금융 표시 | 10진 문자열 전용 유틸 한 곳(`src/shared/lib/decimal.ts`): 자리 구분·부호·tone 판정·비교(정렬)·표시용 소수 자리 맞춤·비율 단위 변환(소수점 이동). 금액 산술 함수는 두지 않음 | RULE-03·FLAG-04 A(이력: open-questions.md FLAG-04 B·C 미선택), spec §9 «비율 소수 2자리». 비율 단위가 API 마다 다름: 포지션 수익률·일별 손익률·비중은 소수 비율(TA:domain/entity/position/Position.kt:190-191, application/portfolio/DailyBalanceCommandService.kt:32-36, application/portfolio/PortfolioSnapshotCommandService.kt:38-42), KIS 잔고 수익률은 퍼센트(infrastructure/kis/KisAccountBalanceAdapter.kt:162-167) → 디코더 타입을 `RateFraction`·`RatePercent` 로 나눔 | `Intl.NumberFormat` 에 문자열 전달: 지원 범위 확인 못 함. decimal.js 류 범용 산술 라이브러리: 금액 산술을 열어 FLAG-04 를 깨기 쉬움 |
| D6 수량 정수 연산 | «남은 수량»(주문수량 − 체결수량)과 진행 막대 폭(정수 %)만 BigInt 로 스케일한 함수 2개 허용 | v3 §6-7 이 요구. RULE-03 은 부동소수점 금지·금액 계산 서버 전용이고 수량 BigInt 연산은 둘 다 아님(AS-04) | 서버 필드 대기: v3 §6-7 을 비움. Number 빼기: RULE-03 위반 |
| D7 계층 구조 | `src/app`(셸·라우터·providers) → `src/pages`(화면 7개) → `src/features`(도메인 조합) → `src/shared`(api·lib·ui). 역방향 의존 금지, API 호출은 `src/shared/api` 한 곳 | 옛 앱의 아키텍처 결정(디자인 아님)을 이어받음. 이력: 4697501 의 `frontend/trading-web/docs/ADR.md` ADR-001(FSD)·ADR-002(TanStack Query). 호출 경계가 한 곳이라 D4 디코더 우회를 시험으로 막을 수 있음 | shadcn 기본 평면 구조(`components/`·`lib/`, 프로토타입 방식): 디코더 우회 호출을 막을 경계가 없음 |
| D8 서버 상태·선택 계좌 | TanStack Query 5(키에 accountId 포함) + 선택 계좌는 작은 React context + localStorage. 폴링은 FLAG-07 가정(주문 5초·연결 상태 10초), 체결 SSE 수신 시 주문·포지션 무효화 | EDGE-03 을 키 분리로 보장. 의존성 최소 | zustand: 값 하나에 의존성 추가. 수동 `useEffect` fetch: 경쟁 상태를 직접 처리 |
| D9 자산 추이 차트 | shadcn chart(recharts 3.10.1, peer react ^19 — `npm view` 2026-11-15). Number 변환은 `src/shared/lib/chart-coordinate.ts` 한 함수만(좌표 전용). 라벨·툴팁·축 문구는 서버 원문 문자열을 D5 로 표시 | DEC-03 A(사용자 2026-11-15, FLAG-17), RULE-03 예외 문구(spec §6) | 차트 대신 표·영역 뺌: DEC-03 B·C 미선택. lightweight-charts: 캔들용이라 1단계엔 과함(FLAG-12) |
| D10 개발 서버 프록시 | 경로 접두사별 대상 분리. trading-api(`/api/v1`, `/api/accounts`, `/api/kis/account`) → `VITE_TRADING_API_TARGET`(기본 `http://127.0.0.1:8080`). collector-api(`/api/kis/ws`, `/api/kis/rest`, `/api/kis/symbols`, `/api/upbit`, `/api/fred`, `/api/pykrx`, `/api/yfinance`, `/api/subscriptions`, `/api/symbols`, `/api/market`, `/api/internal/kis`) → `VITE_COLLECTOR_API_TARGET`(기본 `http://127.0.0.1:8081`). quant-worker `/api-quant/*` → 접두사 제거 후 `VITE_QUANT_API_TARGET`(기본 `http://127.0.0.1:8082`). `preview.proxy` 도 같은 표 | 포트: trading-api 8080(backend/trading-api/src/main/resources/application.yaml:60-61, docker-compose.yml:65-66), collector 호스트 8081(docker-compose.yml:88-89, trading-api 기본값도 8081 — application.yaml:37), quant-worker 8082(backend/quant-worker/Dockerfile:8, docker-compose.yml:110-111). CORS 는 SSE 컨트롤러 하나뿐(ExecutionSseController.kt:15)이라 브라우저 직접 호출 불가 | 옛 표(`/api/` 전부 8080): collector 경로가 trading-api 로 가서 404. 백엔드 CORS 열기: 백엔드 변경(FLAG-01) |
| D11 결정적 검사 위치 | 금융·RULE-10·목업 금지 검사는 vitest «소스 검사 시험»(`src/test/guards/`)으로 둠. CI trading-web 잡에 `npm run build`·`npm run lint` 단계 추가(DEC-08 A) | 지금 CI 는 `npm test` 만 돌림(.github/workflows/full-test.yml:28-42). 소스 검사를 시험으로 두면 린터 종류와 상관없이 CI 에서 돎 | ESLint 사용자 규칙만: 린터 선택(D13)에 묶임. CI 변경 없이 로컬 기록만: DEC-08 B 미선택 |
| D12 시험 도구 버전 | vitest 4.1.x·jsdom 28.1.x 고정, CI Node 20 유지(DEC-08 A). 접근성은 axe-core 4.14.0 직접 호출 | CI `node-version: '20'`(full-test.yml:34). vitest 5.1.0 엔진 `^22.12.0‖^24‖>=26`, jsdom 31.0.1 엔진 `^22.22.2‖…` → Node 20 불가. vitest 4.1.x·jsdom 28.1.0 은 Node 20 지원(옛 lock 의 엔진 값 `^20.0.0‖…`, `^20.19.0‖…`). vite 8.3.8 엔진 `^20.19.0‖>=22.12.0`. 모두 `npm view` 2026-11-15 | vitest 5 + CI Node 22: DEC-08 C 미선택. vitest-axe 0.1.0: 2025-01 이후 갱신 없음 |
| D13 TS·린터 | 생성기 기본값을 따르고 `build`·`lint`·`test` 스크립트 이름 유지(stack-profile·CI 가 이 이름을 씀). 생성기가 TS 7 을 깔면서 ESLint 를 쓰게 되면 TS 6.0.3 으로 고정 | typescript-eslint 8.72.2 peer `typescript >=4.8.4 <6.1.0`, npm latest TS 7.0.2(`npm view` 2026-11-15). 생성기가 실제로 어떤 TS·린터를 까는지는 실행 전이라 확인 못 함 | 계획에서 린터를 미리 고정: 생성 결과를 모르는 채 정하게 됨 |
| D14 오류 문구 | 서비스별 오류 모양을 하나로 정규화: trading-api `{status, code, message}`(TA:presentation/exception/ApiErrorResponse.kt:3-7), collector `{status, error, code, message, path, timestamp}`(CA:presentation/common/GlobalExceptionHandler.kt:150-157), FastAPI `{detail}`. 사용자 문구는 status·code 로 고르고 원문은 접어서 보임 | EDGE-05 | 원문 그대로 노출: EDGE-05 위반 |
| D15 RULE-10 검사 방식 | 삭제 직전에 옛 추적 파일의 git blob 해시 목록과 옛 CSS 사용자 정의 변수 이름 목록(값 아님)을 시험 고정 자료로 떠 두고, 새 `src` 의 어떤 파일도 같은 blob 해시가 아니고 옛 변수 이름을 쓰지 않음을 시험으로 확인. 일치가 나오면 실패 — 그 파일이 `shadcn add` 생성물이면 사람이 확인한 뒤에만 예외 목록에 올림 | spec §10 «기존 src 의 스타일·토큰을 import 하지 않음»은 삭제 뒤엔 import 자체가 불가능해 검사력이 0 → 복사·이식을 잡는 검사로 바꿈 | import 검사만: 삭제 후 항상 통과. 사람 눈 검토만: 결정적이지 않음 |
| D16 tone 토큰 | 새 `index.css` 에 상승·하락 tone 과 매수·매도 tone 을 별도 토큰으로 새 이름으로 정의. 값(색)은 디자인 작업에서 정함 | RULE-05(톤 분리·부호 병기). 옛 이름(`order-buy`·`order-sell`·`market-positive`·`market-negative` — FE:CODEX.md:27)은 D15 금지 목록에 들어감 | 옛 이름 재사용: D15 검사에 걸리고 RULE-10 취지와 어긋남 |
| D17 새 앱 지시 문서 | 골격 단계에서 같은 경로에 새 앱 기준 `frontend/trading-web/CLAUDE.md`·`CODEX.md`·`docs/UI_GUIDE.md` 를 새로 씀(옛 내용 참고 없이 이 계획의 D·RULE 기준). 루트 CLAUDE.md 는 고치지 않음 | DEC-09 A(사용자 2026-11-15). 루트 «Frontend Design System» 두 줄이 이 경로를 «먼저 적용»하라고 가리키므로 같은 경로에 새로 쓰면 참조가 끊기지 않음. 루트 정리는 나중 규칙 정리에서 — CLAUDE.md 는 하네스 게이트로 미룸, 규칙 정리 때 반영(사용자 2026-11-15) | 루트 두 줄 수정: DEC-09 B 미선택. 그대로 둠: 삭제 뒤 끊긴 참조(DEC-09 C) |

---

## 3. 바꿀 파일·모듈

| 경로 / 모듈 | 변경 종류 | 목적 | 위험 |
|---|---|---|---|
| `frontend/trading-web/**`(옛 앱 추적 파일 146개, 문서 `docs/ADR.md`·`PRD.md`·`UI_GUIDE.md` 포함) | 삭제 | FLAG-06 B 제자리 재작성(D2) | 높음 — 착수 직전 사람 재확인(DEC-01). git 이력에는 남음 |
| `frontend/trading-web/` 골격(`package.json`·lock·`vite.config.ts`·`tsconfig*`·`components.json`·`index.html`·린터·vitest 설정) | 신설 | D1·D10·D12·D13 | 중간 |
| `src/app/`(진입점·providers·라우터·셸: 내비·상단 바·계좌 선택·데이터 기준 점·체결 토스트·테마·서랍) | 신설 | v3 §2 | 중간 |
| `src/app/styles/index.css` | 신설 | shadcn base-nova 생성 토큰 + 새 tone 토큰(D16) | 중간(RULE-10) |
| `src/shared/api/`(http·서비스별 엔드포인트·디코더·오류 정규화) | 신설 | D4·D14 | 높음 |
| `src/shared/lib/`(decimal·rate·quantity·chart-coordinate·mask·idempotency) | 신설 | D5·D6·D9·AS-01·RULE-04 | 높음 |
| `src/shared/ui/`(shadcn 프리미티브, 4상태 영역 틀, 금액·부호 표시) | 신설 | v2 §0-8, RULE-05·08 | 낮음 |
| `src/pages/{home,account,order,history,performance,broker,collector}/` | 신설 | 화면 7개 | 중간 |
| `src/features/{order-form,open-orders,positions,reconciliation,order-history,equity-chart,tax-summary,broker-status,collector-status,subscriptions}/` | 신설 | 도메인 조합(체결 이력 탭·기간 수익률·토스 카드는 없음 — DEC-02·05·07) | 높음(주문·대사), 그 외 중간 |
| `src/test/guards/`·`src/test/a11y/`·`src/test/fixtures/`(시험 전용) | 신설 | D11·D15 결정적 검사, 접근성 | 중간 |
| `frontend/trading-web/CLAUDE.md`·`CODEX.md`·`docs/UI_GUIDE.md` | 신설(새 앱 기준) | D17(DEC-09 A) | 중간 |
| 루트 `CLAUDE.md` | 변경 없음 | DEC-09 A — 나중 규칙 정리에서 다룸 | — |
| `.github/workflows/full-test.yml` trading-web 잡 | 수정 | `npm run build`·`npm run lint` 단계 추가, Node 20 유지(DEC-08 A) | 낮음 |
| `docs/features/running/trading-web-shadcn-rebuild/screen-spec-v3.md` | 수정 | §11 «API 대기» 목록을 구현 결과와 최종 대조(2026-11-15 결정분은 반영 완료) | 낮음 |
| `docs/features/running/trading-web-shadcn-rebuild/v3-checklist.md` | 신설 | spec §10 1행 «v3 항목 대조 체크리스트» | 낮음 |

---

## 4. 데이터·API 작업

### 4-1. FLAG-08 대조 결과 (spec §7 의 🟠 항목 + 🟢 항목 중 어긋난 것)

금액 타입 열: «숫자 토큰» = 서버 BigDecimal 이 JSON 숫자로 나감(D4). «문자열» = 서버가 `toPlainString()` 으로 문자열화. Double 로 금액을 주는 응답은 trading-api·collector-api DTO 검색에서 0건(유일한 Double 은 `parseFailRatePct`, 금액 아님 — CA:presentation/kis/dto/OrderbookIngestHealthResponse.kt:6).

| 항목 | 결과 | 경로 · 응답 필드 | 금액 타입 | 근거 |
|---|---|---|---|---|
| 1. 홀드 금액 | 있음 | `GET /api/v1/accounts` `lockedDeposit` | BigDecimal, 숫자 토큰 | TA:presentation/dto/account/AccountResponse.kt:21 |
| 2. KOSPI 벤치마크 | 없음 | `/api/v1/benchmarks/kospi` 매핑 0건 | — | trading-api·collector `@*Mapping` 전수 검색 |
| 3. 시장 지수 4개 | 없음 | — | — | 같음 |
| 4. 호가 10단계 | 없음 | 대체 후보 `GET /api/market/microstructure/{symbol}?interval=` 는 `bestBid·bestAsk·spread·bidAskImbalance·depth{bidDepthTopN, askDepthTopN, depthImbalance}` 요약뿐 | 문자열 | CA:presentation/market/MarketMicrostructureController.kt:19,23; dto/MarketMicrostructureDto.kt:23-36,78-85 |
| 5. 최근 체결 틱 30건 | 없음 | `GET /api/market/bars/{symbol}` 는 1분봉(최대 100) `startedAt·open·high·low·close·volume·tradeValue·vwap·tickCount` | 문자열 | CA:presentation/marketbar/MarketBarController.kt:17,22,25; dto/MarketBarResponse.kt:9-19 |
| 6. 현재가 | 부분 | `GET /api/market/features/{symbol}` `asOf`, `features[].close` 등 / `GET /api/kis/rest/watchlist/price?mode&symbol` 는 KIS 원본 `JsonNode` 통과(필드 계약 없음) / 보유 종목은 `PositionResponse.currentPrice` | 문자열 / 원본 / 숫자 토큰 | CA:presentation/marketfeature/MarketFeatureController.kt:16,20, dto/MarketFeatureResponse.kt:3-17; CA:presentation/kis/KisRestWatchlistController.kt:34,38,51; TA:presentation/dto/position/PositionResponse.kt:16 |
| 7. 주문 예상 금액·수수료·거래세·경고 | 없음 | 주문 컨트롤러에 estimate 매핑 없음 | — | TA:presentation/controller/OrderController.kt:32,57,75,81,90,106 |
| 8. 주문 정정 | 없음 | PUT/PATCH 매핑 없음(POST·GET·DELETE 만) | — | 같음 |
| 9. 주문 메모 | 없음 | 요청·응답·엔티티에 메모 필드 없음 | — | TA:presentation/dto/order/OrderRequest.kt:13-25, OrderResponse.kt:15-29, domain/entity/order/Order.kt 필드 |
| 10. 주문 상태 이력 | 없음 | 응답은 현재 상태만(`orderStatus`) | — | OrderResponse.kt:22 |
| 11. 원장 대사 | 부분 | `GET /api/kis/account/balance?accountId&mode` 의 `reconciliation{missingInLocal, missingInKis, quantityMismatch}`(종목 코드 목록, 조회 때마다 계산·저장 안 함). 대사 기록·값·차이·처리 상태·확인함·예수금/미체결 대사 없음 | 잔고 금액은 숫자 토큰 | TA:presentation/controller/KisAccountBalanceController.kt:12,16-20; application/account/result/KisAccountBalanceResult.kt:8-19; KisReconciliationResult.kt:3-7; application/account/KisAccountQueryService.kt:27-36 |
| 12. 브로커 연동 상태 | 부분 | KIS 시세 WS 만 `GET /api/kis/ws/health` `[{mode, status, lastConnectedAt, reconnectAttempts}]`. 토큰 상태·만료·재발급 API 없음. 토스 없음(다른 세션이 `/api/v1/accounts/{id}/broker-balance`·`/api/toss/**` 계획) | 금액 없음 | CA:presentation/kis/KisWsHealthController.kt:9-14,17,21; .worktrees/multi-broker-kis-toss/…/development-plan.md:68 |
| 13. 브로커 계좌 조회 | KIS 있음 · 토스 없음 | 11행 같은 API: `asOf·cashBalance·marketValue·unrealizedPnl·returnRate(퍼센트)·positions[ticker, quantity, avgPrice, currentPrice, marketValue, unrealizedPnl, returnRate]` | 숫자 토큰 | KisAccountBalanceResult.kt:8-19; application/account/result/KisBalancePositionResult.kt:5-13; infrastructure/kis/KisAccountBalanceAdapter.kt:162-167 |
| 14. 브로커 호출 로그 | 없음 | — | — | 매핑 전수 검색 |
| 15. 수집 지연 시계열·적재 통계·버퍼·오류 로그 | 없음 | 있는 것: `/api/subscriptions/status`(`generatedAt·totalWsSlotUsed·totalWsSlotMax·modes[]`), `/api/subscriptions/routing-status`, `/api/internal/kis/orderbook/ingest-health`(`receivedCount·parseFailCount·parseFailRatePct·lastReceivedAtMs·lastReceivedAgeMs`) | 금액 없음 | CA:presentation/subscriptions/SubscriptionStatusController.kt:13,17; SubscriptionRoutingController.kt:65; presentation/kis/OrderbookIngestHealthController.kt:11,15 |
| 16. 구독 관리 응답 모양 | 있음 | 모두 collector-api. KIS `/api/kis/symbols/catalog|subscriptions`(`Map`: `items·returnedCount·totalCatalogCount·totalSubscribedCount·mode·channel`), `/api/kis/ws/subscriptions`, `/api/kis/rest/watchlist`, Upbit `/api/upbit/markets/*`, `/api/{pykrx|yfinance}/symbols/*`, FRED `/api/fred/series/*`. 일봉 일괄 수집은 quant-worker `POST /collect/daily`(`provider·symbols·success_symbols·failed_symbols·total_rows_inserted·start·end`) | 금액 없음 | CA:presentation/kis/KisSymbolCatalogController.kt:23-141; backend/quant-worker/src/interfaces/api/app.py:23-39,117 |
| 17. 포트폴리오 분석(섹터·기여·위험) | 없음 | `portfolio-snapshots?date` 는 종목별 `weight`(소수 비율)·`marketValue` 뿐 | 숫자 토큰 | TA:presentation/controller/PortfolioSnapshotController.kt:35-43; dto/portfolio/PortfolioSnapshotDto.kt:29-38 |
| 18. 앱 내 알림 목록 | 없음 | Slack 설정 API(`/api/operations/notifications/slack`)만. quant-ai `/chart-analysis/notifications` 는 LLM 분석 SSE | — | TA:presentation/controller/SlackNotificationController.kt:19; backend/quant-ai/src/chart_analysis/interfaces/chart_analysis_router.py:125 |
| 19. 체결 목록(spec §7 1행) | 계좌 단위 없음 | `/api/v1/executions` 는 SSE `/stream` 하나. 목록은 주문별 `GET …/orders/{id}/executions`(`executionId·executedQuantity·executedPrice·fee·executedAt`). SSE 이벤트에 계좌 ID 없음 | 목록 숫자 토큰 · SSE 문자열 | TA:presentation/controller/ExecutionSseController.kt:14,20; OrderController.kt:90-112; OrderResponse.kt:65-71; dto/sse/ExecutionSseEvent.kt:6-17 |
| 20. 자산 추이(spec 🟢) | 있음, 옛 프런트와 어긋남 | `GET /api/accounts/{id}/daily-balances?fromDate&toDate`(접두사 `/api/v1` 아님) `businessDate·cashBalance·stockMarketValue·totalAssetValue·pnlAmount·pnlRate`. 수동 POST 로만 생성 | 숫자 토큰 | PortfolioSnapshotController.kt:21,27-33,45; PortfolioSnapshotDto.kt:9-16 |
| 21. 세금 요약(spec 🟢) | 있음, 옛 프런트와 어긋남 | `id·accountId·taxYear·totalRealizedPnl·taxablePnl·estimatedTax·status·computedAt`. 재계산은 같은 모양을 동기로 반환 | 숫자 토큰 | TaxSummaryController.kt:21,28,47; TaxSummaryDto.kt:9-18 |

### 4-2. 작업 목록

| 항목 | 필요한 변경 | 소유 | 막는가(blocking)? |
|---|---|---|---|
| 무손실 파싱·디코더·오류 정규화(D4·D14) | 신설 | frontend | 예 — 모든 금융 화면 선행 |
| 개발 프록시 표(D10) | 신설 | frontend | 예 — collector 화면 선행 |
| CI trading-web 잡 build·lint 단계(DEC-08 A) | 수정 | frontend(CI) | 아니오 — 골격 단계에서 같이 |
| 백엔드 요구 «API 대기»(목록 v3 §11): 주문 예상 금액·수수료·거래세·경고, 주문 정정, 주문 메모, 주문 상태 이력, 거부 사유 필드, 계좌 단위 체결 목록, 거래 집계(KPI·종목별 손익), 원장 대사 실행·기록·«확인함», 브로커 토큰 상태·재발급·호출 로그, 시장 지수·KOSPI 벤치마크, 호가 10단계·체결 틱, 수집 지연·적재·버퍼·오류 로그, 앱 내 알림 목록, 일별 잔고 자동 생성, 기간 수익률·실시간 총자산 | 요구로만 기록(이 기능은 구현 안 함, FLAG-01) | backend | 아니오 — 해당 영역을 빼고 진행 |
| 토스 연동 결과 표시 | 다른 세션이 머지한 뒤 별도 갱신(그 전까지 토스 카드 숨김, DEC-05) | external(`multi-broker-kis-toss`) | 아니오 |
| 계약 변경 검사(openapi-diff) | 해당 없음 — 이 기능은 API 계약을 바꾸지 않음 | — | — |

---

## 5. 구현 순서 개요

> 굵은 순서만. 세부 단계·상태는 state.md + steps/step-N.md 에서 삽니다.

1. 교체 골격 — 사람 재확인(DEC-01) → D15 고정 자료 → 옛 폴더 삭제 + shadcn base-nova Vite 골격 + 프록시 + 소스 검사 시험 + 새 앱 지시 문서(D17) + CI build·lint 단계(DEC-08) → build·lint·test 통과, 한 커밋(D2).
2. 경계 계층 — API 클라이언트(무손실 파싱·디코더·오류 정규화), 10진 문자열·비율·수량·차트 좌표·마스킹·멱등키 유틸. 시험 먼저(golden).
3. 공통 셸 — 라우팅, 내비, 상단 바(계좌 선택·데이터 기준 점), 체결 SSE·토스트, 테마, 서랍, 4상태 영역 틀.
4. 원장 화면 — 계좌·포지션(대사 E 는 불일치 목록 축소판, DEC-04), 주문(폼·확인 단계·멱등·미체결·취소).
5. 조회 화면 — 거래 내역(주문 이력 탭만, DEC-07), 포트폴리오·성과(KPI 기준일·자산 추이·세금 탭, DEC-02·03·06), 홈.
6. 0단계 화면 — 브로커 연동(토스 카드 숨김, DEC-05), 수집 현황·구독 관리.
7. 마무리 — v3 §11 «API 대기» 목록과 구현 결과 최종 대조, 대조 체크리스트, 접근성 자동 검사, 새 앱 문서 완성, 사람 시각 확인 기록.

---

## 6. 테스트 계획

명령 기준 경로는 `frontend/trading-web`. 시험 대역 데이터는 `src/test/fixtures/` 에만 두고, 모양은 §4-1 백엔드 DTO 정의에서 만듭니다(숫자 토큰 그대로).

| spec §10 행 · 대상 | 시험 | 종류 | 명령 / 방법 | 기대 결과 |
|---|---|---|---|---|
| 1. 화면 7개·셸이 v3 영역·열·문구대로 | 화면별 렌더 시험(fetch 대역) + `v3-checklist.md` 각 행에 시험 이름 또는 «API 대기» | integration + manual 대조 | `npx vitest run src/pages src/app` | 남긴 영역의 제목·열 머리·문구가 v3 와 일치, 뺀 영역은 렌더 안 됨 |
| 2. 모든 데이터 영역 4상태 | 영역별: 로딩(응답 보류), 빈(빈 배열), 에러(500 → «다시 시도» 클릭 시 재요청), 정상. 부분 경고(벤치마크 없음) | integration | 같은 명령 | 영역 제목 유지, 본문 자리에 상태 표시 |
| 3. API 없는 영역이 가짜 데이터 없이 빠짐 | 소스 검사: 시험 파일 밖에서 `src/test/` import 0건. 뺀 영역 ↔ v3 §11 대조. 결정분 렌더 부재 시험: 토스 카드(DEC-05), 홈 최근 체결·거래 내역 체결 탭(DEC-07), 포트폴리오 기간 수익률(DEC-02) | guard + integration + manual | `npx vitest run src/test/guards src/pages` | 위반 0건, 결정분 미렌더, 목록 일치 |
| 4. 금융 값이 문자열·부동소수점 없음 | 디코더 golden: `1000000`, `1000000.0000`, `0.1`, `-1234.5`, `1E-8`, `12345678901234567890.12` → 원문 보존·평문화. 포맷 golden: 자리 구분·부호·소수 2자리·비율 단위 변환·null «-». 소스 검사: `parseFloat`·`Number(`·`parseInt(`·`Math.` 사용 0건(허용 목록: 안전 정수 디코더, `chart-coordinate.ts` 한 파일 — DEC-03) | unit + guard | `npx vitest run src/shared src/test/guards` | 모두 통과, 허용 목록 밖 0건 |
| 4-1. 차트 좌표 예외(DEC-03) | 자산 추이: 툴팁·축 라벨 텍스트가 서버 원문 문자열(golden)과 일치, Number 는 좌표 함수 반환값만 차트에 들어감 | unit + integration | `npx vitest run src/shared/lib src/features/equity-chart` | 라벨에 반올림·지수 표기 없음 |
| 4-2. KPI 기준일(DEC-06) | 일별 잔고 있음 → 값 옆 «YYYY-MM-DD 기준», 없음 → 카드 빈 상태(홈 B·계좌 B·포트폴리오 A) | integration | `npx vitest run src/pages` | 기대대로 |
| 5. 주문 멱등키·확인 단계·중복 제출 | 확인 단계 없이는 요청 0건. 제출 연타 → POST 1건. 네트워크 실패 뒤 재시도 → 같은 `idempotencyKey`(EDGE-01). 초기화·성공 뒤 → 새 키. 비활성 계좌 → 폼 비활성(EDGE-04). 현재가 없음 → 시장가 비활성(EDGE-06) | integration | `npx vitest run src/features/order-form` | 기대대로 |
| 6. 비밀값 비노출·계좌번호 가림 | 브로커 화면: 입력 요소 0개, 렌더 텍스트에 키·토큰 문자열 없음. 마스킹 golden `12345678-01` → `12****78-01`, 형식 이상 → 전체 가림 | integration + unit | `npx vitest run src/pages/broker src/shared/lib` | 통과 |
| 7. 원장 대사에 자동 수정 없음(DEC-04 축소판) | E 영역: 렌더·«지금 대사» 동안 발생 요청이 모두 GET. 고치기·«확인함» 버튼 없음. 불일치 3구분이 응답 목록과 일치, 수량은 두 응답 원문 그대로(차이 계산 없음). 안내 문구 고정. 요청 중 체결 SSE 수신 → «다시 대사 필요»(EDGE-08). LOCAL 계좌면 E 숨김 | integration | `npx vitest run src/features/reconciliation` | GET 외 요청 0건 |
| 8. 라이트·다크·768 미만 서랍·포커스·키보드 | axe-core 화면별 위반 0건(색 대비 규칙은 jsdom 에서 신뢰 못 해 제외 — 확인 못 함). 서랍 ESC 닫기·포커스 이동, 테마 선택 유지 시험. 색 대비·실제 폭은 사람 | integration + manual | `npx vitest run src/test/a11y` + 사람 | 위반 0, 사람 확인 기록 |
| 9. build·lint·test 통과 | 로컬 세 명령 + CI(DEC-08 A 로 build·lint 단계 추가) | CI + manual | `npm run build && npm run lint && npm test -- --run` | 모두 0 종료, CI trading-web 잡 초록 |
| 10. 기존 디자인 자산 미이식 | D15 소스 검사(blob 해시·옛 CSS 변수 이름) | guard | `npx vitest run src/test/guards` | 일치 0건 |
| 11. 사람 화면 확인 | `npm run dev` 로 간격·폭·다크·좁은 폭 확인 | manual | 사람 | state.md 에 기록 |
| EDGE-02 SSE 끊김 | `EventSource` 대역 오류 → 유예 뒤 «끊김», 주문 폴링 계속, 재연결 표시 | integration | `npx vitest run src/app` | 기대대로 |
| EDGE-03 계좌 전환 경쟁 | 첫 계좌 응답 보류 → 계좌 전환 → 옛 응답 도착 | integration | `npx vitest run src/app src/pages/account` | 새 계좌 화면이 덮이지 않음 |
| EDGE-09 좁은 폭 표 | 가로 스크롤 틀·첫 열 고정 클래스 존재, 열 삭제 없음 | integration + manual | 같은 명령 + 사람 | 열 개수 동일 |

---

## 7. 도메인 제약 반영

| 차원 | 적용 | 구현·검증 방법 | review routing |
|---|---|---|---|
| 수치 무결성 | y | D4 무손실 파싱 + D5 문자열 유틸 한 곳 + D6 BigInt 수량 2함수 + D9 차트 좌표 함수 한 곳(RULE-03 예외, FLAG-17). 소스 검사로 그 밖의 float 변환 차단, 경계값 golden(`1E-8`, 20자리 정수, 음수, null) | 금액 계산 허용 범위 변경 → 사람(FLAG-04·17 재결정) |
| 단위·기준치 | y | 비율 단위 타입 분리(`RateFraction`·`RatePercent`, D5). 원화 표시는 AS-02. 호가단위·거래세율은 서버 값이 없으므로 화면에 하드코딩하지 않고 해당 영역을 뺌 | 규칙 데이터가 생기면 서버 값으로만 |
| 결측·이상치 | y | null → «-». `PriceSource` `REDIS_STALE` → 마지막 값 + `priceUpdatedAt`, `UNKNOWN` → «-»(EDGE-06). 일별 잔고 없음 → KPI·자산 추이 빈 상태(DEC-06). golden | |
| 규제·표현 | n | 0·1단계에 AI 문구 없음(FLAG-10 은 5단계) | |
| 인증·권한·감사 | y | 비밀값 입력·표시 없음(RULE-11) 시험. «확인함»은 서버 기록 API 가 없어 두지 않음(RULE-12, FLAG-18). 주문 멱등(RULE-04) 시험 | 보안·감사 변경 → 사람 |
| 개인정보(계좌번호) | y | 마스킹 함수 한 곳(AS-01) + golden. 원문은 응답에 있으므로 계좌 객체를 콘솔·오류 원문에 싣지 않음(소스 검사: `console.*` 허용 목록) | |

---

## 8. 리스크·막힘

### 8-1. 리스크

| ID | 리스크 / 막힘 | impact | 완화 / 필요한 결정 |
|---|---|---|---|
| RISK-01 | trading-api BigDecimal 이 JSON 숫자 토큰 → 브라우저 기본 파싱 시 double | 높음 | D4. 문자열·숫자 어느 쪽이 와도 같은 결과 |
| RISK-02 | 옛 프런트 계약이 백엔드와 어긋남(경로·파라미터·필드·enum) | 중간 | D3. 디코더 fixture 를 백엔드 DTO 정의에서 만들고, 화면마다 실제 응답 1건을 대조(어긋나면 §9 멈춤) |
| RISK-03 | 개발 프록시. collector 로컬 실행 시 `server.port` 미설정(backend/collector-api/src/main/resources/application.yaml)이라 Spring 기본 8080 으로 trading-api 와 겹침 | 중간 | D10 환경 변수로 대상 지정, 새 `README` 에 안내 |
| RISK-04 | 커밋 때 빌드 검사 + 삭제 구간 | 중간 | D2 한 커밋 |
| RISK-05 | CI Node 20 과 최신 시험 도구 엔진 불일치 | 중간 | D12 고정(DEC-08 A — Node 20 유지) |
| RISK-06 | 일별 잔고 자동 생성 없음, trading-api 가 재시작마다 스키마 재생성(`ddl-auto: create` — backend/trading-api/src/main/resources/application.yaml:19) | 중간 | KPI(DEC-06)·자산 추이가 대부분 빈 상태일 수 있음. 사람 화면 확인 때 계좌·주문을 다시 만들어야 함. 이 기능에서 고치지 않고 백엔드 요구로 기록 |
| RISK-07 | 체결 SSE 이벤트에 계좌 ID 없음(ExecutionSseEvent.kt:6-17), emitter 제한 300초(ExecutionSseController.kt:25) | 낮음 | 토스트는 전 계좌 체결을 보임(옛 동작과 같음). 체결이 오면 선택 계좌 주문·포지션 무효화. 300초마다 재연결 → «끊김»은 유예 뒤에만(AS-05) |
| RISK-08 | KIS 잔고 API 호출 한도 | 중간 | 브로커 C·대사 E 는 누를 때만 조회, 자동 폴링 없음. 두 영역이 같은 Query 키 공유 |
| RISK-09 | 뺀 영역이 많아 화면이 비어 보임 | 중간 | v3 원칙 6 의 결과. 목록은 v3 §11, 사람 확인 때 같이 봄 |
| RISK-10 | 카탈로그 응답이 타입 없는 `Map` | 낮음 | 디코더 필드 검사, 실패 시 영역 에러 상태 |
| RISK-11 | ingest-health 가 `/api/internal/` 경로 | 낮음 | 백엔드가 바꿀 수 있음. 실패 시 그 줄만 에러 |

### 8-2. 사람 결정 (추천 순, 첫 안이 추천)

결정은 사용자가 2026-11-15 팝업으로 답했고 코디네이터가 전달했습니다. 명세를 바꾼 결정은 open-questions FLAG-16~21 에 같은 내용으로 기록했습니다.

| ID | 질문 | 선택지 (장점 / 단점) | 결정 (결정자·날짜·반영 위치) |
|---|---|---|---|
| DEC-01 | 옛 `frontend/trading-web` 삭제 실행 — 착수 직전 재확인 | 대상: 추적 파일 146개(코드·설정·`docs/ADR.md`·`PRD.md`·`UI_GUIDE.md`·`public/` 포함), 무시·미추적 0. 방법: D2 한 커밋. 복구: `git revert`. 메인 체크아웃에 `*.local`(예: `.env.local`) 이 있으면 머지 뒤에도 남음 — 사람이 확인 | 승인 — 사용자·2026-11-15(step-1 착수 직전 재확인: «삭제·골격 진행»). 반영: step-1 삭제 실행, 같은 커밋에 shadcn 골격 |
| DEC-02 | 포트폴리오 C 기간 수익률(결정 전 🔴) | A. 빼고 «API 대기» / 규칙 그대로, 작업 없음 / v3 §9-1 의 1차 범위가 줄어듦. B. FLAG-04 를 다시 정해 10진 화면 계산 허용 / 표가 생김 / 규칙 복제·불일치 위험(FLAG-04 C 와 같은 단점). C. 백엔드에 기간 수익률 추가 / 정확 / FLAG-01 재결정 필요 | A — 사용자·2026-11-15. FLAG-16. 반영: v3 §9-1·§11, spec §4·§7, 이 문서 §1·§5 |
| DEC-03 | 자산 추이 차트 좌표의 Number 변환 | A. 좌표 전용 변환 함수 한 곳만 허용, 라벨·툴팁·축 문구는 원문 문자열 / 차트 가능, 금액에 되먹지 않음 / RULE-03 문구의 예외를 명시해야 함. B. 차트 대신 표(날짜·총자산·손익) / 예외 없음 / v2 §2-7·§4-2 의 곡선이 아님. C. 영역 뺌 / 가장 단순 / 홈·포트폴리오가 더 빔 | A — 사용자·2026-11-15. FLAG-17. 반영: spec §6 RULE-03·§9·§10, v3 §0·§4·§9-1, 이 문서 D9·§6 4-1행 |
| DEC-04 | 원장 대사 E(결정 전 🔴 — 명세 전체) | A. 축소안: KIS 잔고 응답의 불일치 3목록(로컬에만·브로커에만·수량 다름) + 조회 시각, 로컬 수량·브로커 수량을 두 응답 원문으로 나란히. 차이·처리 상태·«확인함»·예수금·미체결 대사는 «API 대기» / 기존 API 로 실제 불일치를 봄, 화면 계산 없음 / v3 §5-5 의 절반 이하. B. 영역 전체 뺌 / 가장 안전 / 1단계 목표(원장 확정)의 확인 수단이 없음. C. 백엔드 대사 API 먼저 / 명세대로 / FLAG-01 재결정 | A — 사용자·2026-11-15. FLAG-18. 반영: spec §4·§5·§6 RULE-12·§7·§9, v3 §5-5·§5-6·§11, 이 문서 §1·§6 7행 |
| DEC-05 | 토스 연동 카드 | A. 토스 API 가 생길 때까지 카드 숨김 / v3 원칙 6 과 일관 / v3 §3-2 «브로커당 1장»과 다름. B. 카드는 두고 «연동 API 준비 전» 문구 / 자리가 보임 / 원칙 1 의 «준비 중 금지» 취지와 어긋남. C. EDGE-10 그대로 «미설정» / spec 대로 / «미설정» 정의는 «서버 환경 변수에 인증 정보가 없습니다»(v3 §3-3)인데 화면은 그걸 알 수 없어 사실과 다를 수 있음(RULE-13) | A — 사용자·2026-11-15. FLAG-20, EDGE-10 변경. 반영: spec §5·§7·§8, v3 §3-2·§11, 이 문서 §1 |
| DEC-06 | 총자산·손익 KPI 의 출처(홈 B·계좌 B·포트폴리오 A) | A. 일별 잔고 마지막 행을 «YYYY-MM-DD 기준»과 함께, 없으면 빈 상태 / 서버 값만, 출처가 보임 / «현재» 값이 아니고 수동 생성이라 자주 빔(RISK-06). B. 해당 카드 뺌 / 단순 / 핵심 수치가 없음 | A — 사용자·2026-11-15. FLAG-19. 반영: spec §4·§5·§7, v3 §4·§5-2·§9-1, 이 문서 §1·§6 4-2행 |
| DEC-07 | 계좌 단위 체결 목록 없음(거래 내역 C-2 탭, 홈 D) | A. 둘 다 빼고 체결은 주문 행 펼침에서 / 기존 API 그대로 / 체결 중심 복기가 약함. B. 주문별 체결 API 를 주문 수만큼 불러 화면에서 합침 / 탭이 생김 / 요청 N+1, 오래된 GTC 주문의 새 체결을 놓칠 수 있음. C. 백엔드 대기 / 정확 / 지금 없음 | A — 사용자·2026-11-15. FLAG-21. 반영: spec §4·§7, v3 §4·§7-5·§11, 이 문서 §1·§5 |
| DEC-08 | CI 가 build·lint 를 안 돌림(spec §10 9행의 증명 경로) | A. trading-web 잡에 `npm run build`·`npm run lint` 두 단계 추가 / CI 로 증명 / 공유 CI 파일 변경. B. 로컬 실행 기록만 / 변경 없음 / 재현 증거가 약함. C. A + Node 22 로 올리고 vitest 5 사용 / 최신 도구 / 변경 폭이 큼 | A — 사용자·2026-11-15: build·lint 단계 추가, Node 20 유지, vitest 4.1.x. 반영: spec §10, 이 문서 D11·D12·§3·§5 1단계 |
| DEC-09 | 루트 CLAUDE.md «Frontend Design System»이 옛 `CODEX.md`·`docs/UI_GUIDE.md`(삭제 대상, RULE-10상 참고 금지)를 «먼저 적용»하라고 함 | A. 골격 단계에서 같은 경로에 새 앱 기준 `CLAUDE.md`·`CODEX.md`·`docs/UI_GUIDE.md` 를 새로 쓰고 루트 두 줄은 그대로 둠 / 루트 수정 없음, 참조가 끊기지 않음 / 새 지시 파일 내용을 사람이 승인해야 함. B. 루트 두 줄을 이 기능 문서로 바꿈 / 명확 / 프로젝트 지시 파일 수정. C. 그대로 둠 / 작업 없음 / 삭제 뒤 끊긴 참조, 에이전트가 옛 가이드를 찾음 | A — 사용자·2026-11-15: 같은 경로(`frontend/trading-web/docs/UI_GUIDE.md`)에 새 앱 기준 문서를 새로 씀, 루트 CLAUDE.md 는 안 고침(나중 규칙 정리에서). `frontend/trading-web/CLAUDE.md` 는 하네스 게이트로 미룸, 규칙 정리 때 반영(사용자 2026-11-15) — step-1 커밋에서 뺌(옛 파일이 추적 중이라 작업 트리에 수정 상태로 남음). 반영: 이 문서 D17·§3·§5 1단계 |

### 8-3. 가정하고 진행 (사소·되돌리기 쉬움, 기록)

| ID | 가정 | 근거 |
|---|---|---|
| AS-01 | 계좌번호 마스킹: `XXXXXXXX-XX` 의 앞 8자리 중 가운데 4자리를 `*` 로(`12345678-01` → `12****78-01`). 형식이 다르면 전체를 가림 | RULE-11 «가운데 자리»의 자릿수 미정 |
| AS-02 | 원화 표시: 소수부가 모두 0 이면 정수로, 0 이 아니면 소수부를 그대로(반올림 안 함) | spec §9 «원 단위 정수» + 금액 임의 반올림 회피 |
| AS-03 | 비율 표시 소수 2자리 half-up, 단위 변환은 소수점 이동 | spec §9, D5 |
| AS-04 | 수량 BigInt 연산 2함수 허용 | D6 |
| AS-05 | SSE «끊김» 표시는 연결 오류 후 5초 안에 재연결되지 않을 때만 | RISK-07 |
| AS-06 | FLAG-07 폴링 주기 채택. FLAG-09: `KIS_LIVE`·`UPBIT_LIVE`(토스 머지 뒤 `TOSS_LIVE` 포함) 계좌는 상단 선택에서 빼고 계좌 카드에 «지원 안 함» 배지 | open-questions FLAG-07·09 ASSUME, TA:domain/enums/TradingMode.kt:3 |
| AS-07 | shadcn preset `base-nova`, base `base` | 루트 CLAUDE.md·선택 프로토타입 일치(D1) |
| AS-08 | CSV 내려받기는 주문 이력 탭만, 받은 문자열 그대로. 텍스트 칸이 `=`·`+`·`@` 로 시작하면 앞에 `'`(숫자 형식 칸은 그대로) | v3 §7-7 |

---

## 9. 구현자에게 넘기기 (Handoff)

> 구현자는 아래 경계 안에서만 작업합니다. 착수 전 Maker 가 §5 를 7단계 로드맵으로 나눠 `step-scaffold` 로 state.md §2 와 step-1 을 만듭니다. 설계 승인 흔적은 사람이 Claude 밖 터미널에서 `feature-approval.js set trading-web-shadcn-rebuild tech-plan --by <이름>` 으로 남깁니다.

**이것만 만들어라 (승인된 범위):**
- v3 0·1단계 화면 7개 + 공통 셸 중 §1 표에서 🟢·🟡 인 영역(DEC-02~07 반영 뒤).
- D1~D17 결정대로의 골격·경계 계층·검사 시험·새 앱 지시 문서·CI build·lint 단계.
- v3 §11 «API 대기» 최종 대조, `v3-checklist.md`.

**이건 하지 마라 (범위 밖):**
- 백엔드 코드 수정(FLAG-01), 토스 세션 코드 가져오기, 2단계 이후 화면·영역.
- 옛 trading-web 디자인 자산(토큰·스타일·컴포넌트·글꼴·아이콘) 참고·복사(RULE-10), ds-lab 프로토타입 화면 코드 옮기기.
- 가짜·예시 데이터(RULE-13), 금액 산술(FLAG-04), 차트 좌표 함수 밖의 숫자 변환(FLAG-17), 비밀값 입력·표시(RULE-11).
- v3 §11 에 있는 영역 붙이기(토스 카드, 체결 이력 탭, 기간 수익률, 대사 «확인함» 등).
- 주문 정정·메모 입력 칸 노출, 일별 잔고 생성 POST 처럼 spec 에 없는 금융 상태 변경 호출(허용: 주문 생성·취소, 세금 재계산, 구독 변경·동기화·일괄 수집).
- 루트 CLAUDE.md 수정(DEC-09), CI 에서 Node 버전 변경(DEC-08).

**이때는 멈추고 물어봐라:**
- 옛 trading-web 삭제 직전(DEC-01) — 사람 재확인 없이는 삭제 금지.
- 실제 응답이 §4-1 대조와 다를 때(경로·필드 이름·타입).
- 생성기가 만든 의존성이 D12·D13 과 충돌할 때(엔진·peer).
- 화면을 채우려 서버 값 없는 계산이 필요해 보일 때.
- D15 검사가 일치 항목을 낼 때.
- 토스 세션 API 가 머지돼 화면에 넣고 싶을 때(범위 변경).
