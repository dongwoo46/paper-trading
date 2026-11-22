# 규칙 변경 증거 — 규칙 Why 줄 다시 쓰기 묶음 2(도메인 broker·quant)

Trial: docs/eval/trials/rule-why-lines-batch2-decimal-fee.prereg.json
Metric: decimal-fee-compliance

## 무엇이 바뀌었나

2026-11-21 규칙 이사에서 원문 그대로 옮긴 `rules/domains/broker.md`(5줄)와 `rules/domains/quant.md`(7줄)를 form v2 조항 꼴(앵커 · 명령형 한 줄 · `Why:` · enforce)로 다시 썼다(broker PTK1~PTK5, quant PTQ1~PTQ4). 지운 지시는 없다. 합친 줄은 셋이다: quant 의 금액 Decimal 두 줄(한국어·영어)은 PTQ2 로, 영어 줄 뒷부분 «verify quant math with targeted tests»는 수식 조항 PTQ1 로, 백테스트 세 줄(룩어헤드·비용·결측과 상장폐지와 거래 정지)은 PTQ3 으로 합쳤다. devkit 중복 판정: 금액 float 금지는 devkit DD13(`backend-api.md`, 지목 경로 `\.py$` 로 quant 코드에 닿음)이 «Money 를 float 로 두지 않음»만 말해 계산까지 좁히는 PTQ2 를 남겼고(`devkit:scope narrows DD13`), 429 `Retry-After` 대기는 devkit 이 우리 서버 응답(backend-api)과 프론트(ui-common)만 정해 증권사 호출 쪽 PTK2 를 남겼고, 대기 뒤 재호출 여부는 devkit LA12(`async-external.md`, 멱등 작업만·시도 상한·4xx 재시도 안 함)가 이긴다고 PTK2 에 적었다. 원문에 이유가 없던 quant 줄의 Why 는 사람이 골랐다(2026-11-22 팝업 4건: PTQ1 리뷰 대조, PTQ2 누적 오차, PTQ3 성과 부풀림, PTQ4 Flyway 미사용 사실). broker 다섯 줄의 Why 는 토스 조사 원문에 있던 이유다(PTK1~PTK4 는 aa6097e, PTK5 는 ad2c033 이 더한 줄).

## 실행 비교

과제: 격리 clone 에서 `backend/quant-worker/src/backtest/lean_template/cost_profiles.py` 에 체결 비용 함수 `fill_cost` 를 더함(과제 글에 Decimal·float 언급 없음). 팔 둘: before 는 da13051 판 `rules/`(quant.md 원문 줄), after 는 이 브랜치 작업트리 판 `rules/`(PTQ1~PTQ4). 두 팔의 차이는 `rules/domains/broker.md`·`quant.md` 두 파일뿐(`diff -rq` 확인). 같은 머신·같은 실행 신원(`claude -p`, 구독 사용량만, billing-guard 판정, 사람 승인 2026-11-22), 부하 36 아래에서 한 번에 하나씩.

| 등록 | before | after | 판정 |
|---|---|---|---|
| `docs/eval/trials/rule-why-lines-batch2-decimal-fee.prereg.json`(f809612, rebase 전 05d8b06) | 3/3 | 3/3 | 유지(통과): after ≥ before |

- 회별: 6회 모두 `fill_cost` 를 더했고, 더한 줄에 `Decimal` 이 있고 `float` 가 없다. 6회 모두 exit 0.
- 한계: 두 팔 모두 천장(3/3)이다. 대상 파일이 이미 bps 를 Decimal 문자열로 들고 있어 과제가 Decimal 쪽으로 기운다. 차이 0 을 «PTQ2 가 일을 안 한다»로 읽지 않는다. 과제가 PTQ2 만 건드리고 PTK1~PTK5·PTQ1·PTQ3·PTQ4 는 실행으로 재지 않았다. 그 조항들은 뜻을 줄이지 않은 고쳐 쓰기라 아래 대조로 닫는다.

## 삭제 줄 대조

- 삭제: > 아래 줄은 옛 문서에서 원문 그대로 옮겼다(2026-11-21 규칙 이사). 절 제목이 출처다.
  → 잔존: 지시가 아니라 출처 안내다. 줄별 출처는 각 조항 `Why:` 줄과 `docs/adr/rule-migration-2026-11-21.md`(이사 기록)에 남는다. broker·quant 두 파일 모두 같은 줄이다.

- 삭제: > 근거 자료: [토스증권 Open API 조사](../../docs/reference/2026-11-15-toss-open-api.md).
  → 잔존: `rules/domains/broker.md` 목적 문단 끝 «근거 자료는 [토스증권 Open API 조사](...)다»(같은 링크)

- 삭제: ## 옮겨 온 줄 — docs/research/2026-11-15-toss-open-api.md
  → 잔존: 출처 제목이다. broker 조항마다 `Why:` 에 «토스증권 Open API 조사 2026-11-15 … aa6097e»(PTK5 는 ad2c033)로 남는다.

- 삭제: - 토큰 만료, 재발급, 허용 IP 이탈이면 `auth-revoked` 후 연결이 끊긴다. 재연결 뒤에는 `GET /orders`와 `GET /conditional-orders`로 상태를 다시 맞춰야 한다.
  → 잔존: `rules/domains/broker.md` PTK1(세 경우의 `auth-revoked` 뒤 재연결하고 두 조회로 주문 상태를 다시 맞춤)

- 삭제: - 시세 `MARKET_DATA` 15, 차트 `MARKET_DATA_CHART` 20, 종목 `STOCK` 5, 전체 종목 `STOCK_ALL` 1, 수급 `STOCK_TRADING_TREND` 10, 장 정보 `MARKET_INFO` 3, 랭킹 5, 시장 지표 5~10, 섹터 5, 계좌 `ACCOUNT` 1, 자산 5, 주문 `ORDER` 10, 주문 조회 5, 거래 가능 정보 6(09:00~09:10에는 3), 조건주문 5, 조건주문 조회 10, 토큰 5. 한도는 사전 공지 없이 바뀔 수 있고 `X-RateLimit-Limit` 헤더로 확인한다. 429는 `Retry-After`만큼 대기한다.
  → 잔존: `rules/domains/broker.md` PTK2(한도 숫자 17개 그대로, `X-RateLimit-Limit` 확인, 429 면 `Retry-After` 만큼 대기. 대기 뒤 재호출은 원문에 없어 더하지 않고 devkit LA12 에 맡김). «사전 공지 없이 바뀜»은 Why 로 옮김

- 삭제: - 토큰이 클라이언트당 1개라서, 같은 키를 쓰는 프로세스가 둘이면 서로 토큰을 무효화한다. 토큰 발급 지점을 `collector-api`나 `trading-api` 중 하나로 모아야 한다.
  → 잔존: `rules/domains/broker.md` PTK3(발급 지점을 둘 중 한 곳으로 모음). 무효화 사실은 Why 로 옮김

- 삭제: - 실거래 주문 채널은 trading-api LOCAL 모의 체결과의 라우팅 경계를 설계·승인한 뒤에만 붙인다.
  → 잔존: `rules/domains/broker.md` PTK4(라우팅 경계를 설계하고 사람 승인을 받은 뒤에만 붙임. 원문처럼 증권사를 가리지 않고 «토스·KIS 등»으로 적음)

- 삭제: - `marketCountry`·`currency` enum은 명세가 «모르는 값을 허용하도록 구현»하라고 적었다. 모르는 값에서 역직렬화가 터지지 않게 한다.
  → 잔존: `rules/domains/broker.md` PTK5(모르는 값에도 역직렬화가 실패하지 않게 받음). 명세 인용은 Why 로 옮김

- 삭제: > 아래 줄은 옛 문서에서 원문 그대로 옮겼다(2026-11-21 규칙 이사). 절 제목이 출처다.
  → 잔존: 지시가 아니라 출처 안내다(quant.md 쪽 사본). 줄별 출처는 quant 조항 `Why:` 줄과 `docs/adr/rule-migration-2026-11-21.md`(이사 기록)에 남는다.

- 삭제: ## 옮겨 온 줄 — 옛 하네스(.claude/_disabled·.codex)
  → 잔존: 출처 제목이다. quant 조항 `Why:` 의 «옛 퀀트 스킬»과 이사 기록 `docs/adr/rule-migration-2026-11-21.md` 가 출처를 갖는다.

- 삭제: - 퀀트 수식을 코드로 옮길 때 변수 이름은 spec 의 수식 표기와 같게 짓고, 수식과 코드가 같은지 단계별로 대조한다.
  → 잔존: `rules/domains/quant.md` PTQ1 첫 두 구절(같은 글)

- 삭제: - quant-worker 의 금액 계산은 `Decimal` 로 하고 `float` 를 쓰지 않는다.
  → 잔존: `rules/domains/quant.md` PTQ2(대상을 quant-worker 에서 pointer 가 덮는 quant 서비스 전체로 넓힘, 수수료·세금·잔고를 예로 붙임)

- 삭제: - 백테스트와 신호 계산은 시점 T 에 T 이전에 알 수 있던 데이터만 쓴다(룩어헤드 편향 금지).
  → 잔존: `rules/domains/quant.md` PTQ3 첫 구절(같은 글)

- 삭제: - 백테스트 성과에는 슬리피지, 수수료, 세금을 늘 반영한다.
  → 잔존: `rules/domains/quant.md` PTQ3 둘째 구절(같은 글)

- 삭제: - 백테스트는 결측값, 상장폐지 종목, 서킷브레이커(거래 정지) 구간을 명시적으로 처리한다.
  → 잔존: `rules/domains/quant.md` PTQ3 셋째 구절(같은 글)

- 삭제: - Use `Decimal` for monetary calculations and verify quant math with targeted tests.
  → 잔존: 앞부분은 `rules/domains/quant.md` PTQ2(금액 계산 Decimal), 뒷부분은 PTQ1 셋째 구절(수식 계산은 손으로 구한 값과 맞추는 대상 시험으로 확인 — 옛 퀀트 스킬 «Unit test factor calculations: verify against manually computed results» 뜻)

- 삭제: ## 옮겨 온 줄 — docs/TODO.md
  → 잔존: 출처 제목이다. 이사 기록 `docs/adr/rule-migration-2026-11-21.md` 가 줄별 출처를 갖는다.

- 삭제: - Python 서비스가 사용하는 테이블은 Python 서비스가 직접 생성 (Kotlin Flyway 의존 금지)
  → 잔존: `rules/domains/quant.md` PTQ4(그 Python 서비스가 직접 만들고 Kotlin 쪽 Flyway 마이그레이션에 기대지 않음)

## 공식 근거 대조

근거 문서: devkit `rules/common/guides/ai-doc-writing-how.md`(작성 표준)와 그 근거 `docs/research/ai-doc-official-guidance.md`.

- 이유(rationale) 보존: 새 조항 9개 모두 `Why:` 줄을 갖는다. 출처는 토스 조사 원문(aa6097e, PTK1~PTK5), 옛 퀀트 스킬(PTQ1·PTQ3), 백테스트 spec(PTQ2), `docs/TODO.md` 스키마 관리 점검 항목과 service-role-refactor 결정(PTQ4), 사람 결정 2026-11-22(PTQ1~PTQ4)다. 출처 없는 추정 이유는 넣지 않았다.
- 강조 서식: 두 파일에 강조가 없었고 새로 더하지 않았다.
- 골격: 파일마다 pointer 머리말(그대로) · scope 줄(broker `repo-only`, quant `narrows DD13`) · `<!-- devkit:form v2 -->` · 목적 문단 · `## 조항` · 조항.
- 분량: 두 파일 합계 43줄 → 51줄(조항마다 앵커와 Why 줄이 붙음).
- 꼴 검사: `rule-form-check.js --root .` exit 0, `doc-form-check.js --root .` 레포 `rules/` 위반 합계 243 → 232.

## 리뷰 뒤 고침(2026-11-23 합동 리뷰 1회차)

합동 리뷰(코드·정합성) 막는 지적 2건과 사실 오류 3건을 고쳤다. 실행 비교의 after 팔은 고치기 전 판이지만 고친 곳은 broker.md 뿐이고 과제(quant `fill_cost`)가 닿는 quant.md 는 그대로라 결과를 다시 재지 않았다.

- PTK2: 원문에 없던 «그룹별 초당 한도 안에서»와 «기다린 뒤 다시 부른다»를 빼고 원문대로 «기다린다»로 되돌렸다. 대기 뒤 재호출은 devkit LA12 가 정한다고 적었다(리뷰 P1: 상한·멱등 한정 없는 재호출로 읽혀 LA12 와 부딪힘).
- PTK4: «토스 실거래 주문 채널»을 원문처럼 증권사 전체로 되돌려 «증권사(토스·KIS 등)»로 적었다(리뷰 P4). Why 의 «샌드박스가 없어» 단정은 출처대로 «개요 문서에 샌드박스가 없어 실거래로 취급»으로 고쳤다(리뷰 P7).
- PTK5 Why 출처를 aa6097e 에서 실제로 그 줄을 더한 ad2c033 으로 고쳤다(리뷰 P2).
- 사전 등록 커밋 표기를 rebase 뒤 브랜치 커밋 f809612 로 고쳤다(리뷰 P3).
- 중복 판정 문단에 LA12 를 더했다(리뷰 P12).
