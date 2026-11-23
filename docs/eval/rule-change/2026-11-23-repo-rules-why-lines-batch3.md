# 규칙 변경 증거 — 규칙 Why 줄 다시 쓰기 묶음 3(trading-web design-system·ui-common)

Trial: docs/eval/trials/rule-why-lines-batch3-ui-money-string-r2.prereg.json
Metric: ui-money-string-compliance

## 무엇이 바뀌었나

2026-11-21 규칙 이사에서 원문 그대로 옮긴 `rules/design-system.md`(18줄)와 `rules/ui-common.md`(12줄)를 form v2 조항 꼴(앵커 · 명령형 한 줄 · `Why:` · enforce)로 다시 썼다(design-system PTV1~PTV15, ui-common PTU1~PTU7). 같은 PR 에 사람 결정 둘(2026-11-23 팝업, 묶음 2 리뷰 후속 P6·N2의 처분)을 넣었다: `rules/domains/quant.md` PTQ2 에 LEAN 입출력 경계 Exception 줄, `rules/domains/broker.md` pointer 를 `(Toss|Broker|Kis)\w*\.kt` 로 넓히고 토스 전용 조항(PTK1·PTK2·PTK3·PTK5)을 목적 문단에 밝힘.

- 지운 지시: ui-common 의 메타 줄 둘(«프론트엔드 작업은 design-system.md 와 이 파일을 먼저 적용한다», «UI 작업은 design-system.md 를 적용한다»). 두 파일 pointer 가 trading-web `src` 의 ts·tsx 를 모두 지목해 편집 훅이 두 파일을 함께 부르므로 같은 일을 하는 줄이다(사람 결정 2026-11-23).
- 합친 줄: 금액 네 줄(ui-common «포맷 단일화, 임의 반올림/절삭 금지», «핵심 비즈니스 계산 UI 재구현 금지, 서버 값 우선», «parseFloat·parseInt·Number(·Math. 를 쓰지 않음», design-system «문자열 그대로 받아 표시용 유틸로만, 반올림·합산·비율 계산을 화면에서 하지 않음»)은 ui-common PTU5 로, API 일원화와 계약 우회 금지는 PTU1 로, CODEX.md 와 UI_GUIDE.md 에 겹쳐 있던 shadcn 조립·추가·토큰 줄은 PTV1·PTV2·PTV4·PTV5 로 합쳤다.
- 더한 예외: PTU5 Exception(자산 추이 그래프 좌표만 숫자 변환, `chart-coordinate.ts` 한 곳). 사람 결정 FLAG-17(2026-11-15)과 소스 검사 시험 허용 목록에 이미 있던 예외를 규칙 글에 맞췄다(사람 결정 2026-11-23).
- devkit 중복 판정: PTV1 은 devkit FE1·FE24(기존 컴포넌트 먼저, variant 로 넓힘)를 shadcn 으로, PTV4 는 FE9(토큰만)를 `index.css` 로, PTV13 은 EC1(화면 6상태)을 화면 안 데이터 영역 4상태로, PTU1 은 FE34(타입 클라이언트 하나)를 `src/shared/api` 로 좁힌다(`devkit:scope narrows FE1, FE9, FE24, EC1` · `narrows FE7, FE34`). devkit ui-common pointer(`\.(tsx|jsx)$` 등)가 trading-web tsx 에 닿는다. EC1 은 «다른 문서는 목록을 다시 적지 말라» 하지만 PTV13 의 4상태는 화면 6상태와 다른 영역 단위 목록이라 원문대로 두었다(사람 결정 2026-11-23).
- 이유가 없던 줄의 Why: 사람이 고른 안(2026-11-23 팝업 둘 — 표시 4줄 PTV9~PTV12, 화면·코드 6줄 PTV2·PTV5·PTV14·PTV15·PTU3). 나머지 Why 는 출처 문서에 있던 사실이다(CLAUDE.md, trading-web-shadcn-rebuild feature-spec·development-plan·screen-spec v2·v3, `components.json`, `.github/workflows/full-test.yml`, 이사 기록).
- 줄별 출처 커밋(`git log -S`): TDS 줄 bf6f2c8, CODEX.md·UI_GUIDE.md 줄 8e5bd7c(4상태 줄만 d41db8a), CLAUDE.md 줄 06f39ad·366640a·a1f9169·c044f83, README 프록시 줄 e8bacfe, useEffect 줄은 2026-11-21 이사에서 사람 결정으로 고쳐 쓴 줄.

## 실행 비교

과제: 격리 clone 에서 `frontend/trading-web/src/pages/skeleton/position-pnl-table.tsx` 에 평가손익 표 컴포넌트 `PositionPnlTable` 을 새로 만듦(과제 글에 Number·Decimal·부동소수점 언급 없음). 팔 둘: before 는 b363640 판 `rules/`, after 는 이 브랜치 작업트리 판 `rules/`. 두 팔의 차이는 `rules/design-system.md`·`ui-common.md`·`domains/quant.md`·`domains/broker.md` 네 파일뿐. 같은 머신·같은 실행 신원(`claude -p`, 구독 사용량만, billing-guard 판정, 사람 승인 2026-11-23), 부하 36 아래에서 한 번에 하나씩.

| 등록 | before | after | 판정 |
|---|---|---|---|
| `docs/eval/trials/rule-why-lines-batch3-ui-money-string.prereg.json`(652f0c4, rebase 전 8cfa263) | 0/3 | 0/3 | 판정 불가: 6회 모두 파일이 생기지 않음 |
| `docs/eval/trials/rule-why-lines-batch3-ui-money-string-r2.prereg.json`(38a7d45, rebase 전 06650b5) | 3/3 | 3/3 | 유지(통과): after ≥ before |

- 1차: 6회 모두 exit 0 이지만 clone 안 세션의 devkit worktree 게이트가 첫 편집을 막아, 실행 세션이 worktree 를 만들지 묻고 멈췄다. 규칙이 아니라 실행 환경 문제라 과제·지표·판정 규칙은 그대로 두고 두 팔 모두 `DEVKIT_WORKTREE_EXEMPT=1` 로 2차 등록했다(사람 승인 2026-11-23).
- 2차 회별: 6회 모두 `PositionPnlTable` 을 만들었고, 더한 줄에 `parseFloat`·`parseInt`·`Number(`·`Math.` 가 없다. 6회 모두 exit 0.
- 등록 밖 관찰(판정 근거 아님): after 3회는 모두 서버 `unrealizedPnl` 문자열을 받아 보였고, before 1회(before-2)는 화면에서 BigInt 로 평가손익을 계산했다(부동소수점은 아니라 지표는 1).
- 한계: 두 팔 모두 천장(3/3)이다. 앱에 `src/shared/lib/decimal.ts` 와 소스 검사 시험이 이미 있어 과제가 문자열 쪽으로 기운다. 차이 0 을 «PTU5 가 일을 안 한다»로 읽지 않는다. 과제가 PTU5 만 건드리고 PTV1~PTV15·PTU1~PTU4·PTU6·PTU7, quant 예외, broker pointer 는 실행으로 재지 않았다. 그 조항들은 뜻을 줄이지 않은 고쳐 쓰기라 아래 대조로 닫는다.

## 삭제 줄 대조

- 삭제: <!-- devkit:scope narrows FE1, FE9, FE24 -->
  → 잔존: `rules/design-system.md` scope 줄 `<!-- devkit:scope narrows FE1, FE9, FE24, EC1 -->`(PTV13 이 EC1 을 좁혀 EC1 을 더함)

- 삭제: > 아래 줄은 옛 문서에서 원문 그대로 옮겼다(2026-11-21 규칙 이사). 절 제목이 출처다.
  → 잔존: 지시가 아니라 출처 안내다. 줄별 출처는 각 조항 `Why:` 줄과 `docs/adr/rule-migration-2026-11-21.md`(이사 기록)에 남는다. design-system·ui-common 두 파일 모두 같은 줄이다.

- 삭제: ## 옮겨 온 줄 — docs/research/2026-08-12-frontend-design-system.md
  → 잔존: 출처 제목이다. PTV8 `Why:` 의 `docs/archive/research/2026-08-12-frontend-design-system.md`(옮겨진 자리)와 bf6f2c8 로 남는다.

- 삭제: ## 옮겨 온 줄 — frontend/trading-web/CODEX.md
  → 잔존: 출처 제목이다(design-system·ui-common 두 곳). 이사 기록 `docs/adr/rule-migration-2026-11-21.md` 가 줄별 출처를 갖고, 조항 `Why:` 에 출처 커밋 8e5bd7c 가 남는다.

- 삭제: ## 옮겨 온 줄 — frontend/trading-web/docs/UI_GUIDE.md
  → 잔존: 출처 제목이다. 이사 기록과 조항 `Why:` 의 feature-spec·development-plan 번호(RULE-·EDGE-·D)가 출처를 갖는다.

- 삭제: - 토스의 원칙과 시각적 절제는 참고할 가치가 있지만, **공개 TDS 구현체를 이 독립 퀀트 웹에 가져와 쓰면 안 된다.** 라이선스 범위, 모바일 우선 컴포넌트, React/스타일링 스택 모두 현재 프로젝트와 맞지 않는다. 토스와 비슷한 인상을 원하면 TDS 코드를 복제하는 대신 자체 토큰과 컴포넌트에서 간결한 위계·명확한 문구·일관된 상태 표현을 구현한다.
  → 잔존: `rules/design-system.md` PTV8(공개 TDS 구현체를 가져와 쓰지 않음, 복제 대신 자체 토큰·컴포넌트로 위계·문구·상태 표현). 세 이유는 Why 로 옮김. 굵게 강조는 뺌(작성 표준).

- 삭제: - 디자인 시스템은 shadcn/ui Base Nova 입니다. 새 컴포넌트는 `npx shadcn@4.23.0 add <이름>` 으로 `src/shared/ui` 에 추가하고, 직접 만든 컴포넌트는 먼저 shadcn 프리미티브로 되는지 봅니다.
  → 잔존: 앞 문장은 PTV1 Why(공식 디자인 시스템 shadcn/ui Base Nova), 추가 명령은 PTV2 첫 문장, «먼저 shadcn 프리미티브로 되는지 봄»은 PTV1 둘째 문장

- 삭제: - 색은 CSS 변수 토큰으로만 씁니다. 색 값(`#hex`·`oklch(...)`)을 컴포넌트에 직접 적지 않습니다. 상승·하락·매수·매도 색은 `tone` 토큰입니다.
  → 잔존: 앞 두 문장은 PTV4(토큰만, `#hex`·`oklch(...)` 직접 적지 않음), tone 문장은 PTV5

- 삭제: - 옛 앱의 디자인 자산은 참고하지 않습니다(RULE-10).
  → 잔존: `rules/design-system.md` PTV7(옛 앱의 디자인 자산을 참고하거나 가져오지 않음 — feature-spec RULE-10 원문 «참고하거나 가져오지 않는다»대로). 집행은 소스 검사 시험 D15

- 삭제: - 화면은 shadcn 프리미티브(`src/shared/ui`)로 조립합니다. 있는 것을 다시 만들지 않습니다.
  → 잔존: `rules/design-system.md` PTV1 첫 문장(같은 글)

- 삭제: - 추가는 `npx shadcn@4.23.0 add <이름>`. 생성물은 사람이 고치기 전에 그대로 두고, 필요한 변경은 호출하는 쪽에서 합니다.
  → 잔존: `rules/design-system.md` PTV2(같은 명령, 생성물은 그대로, 변경은 호출 쪽)

- 삭제: - 아이콘은 `lucide-react` 입니다.
  → 잔존: `rules/design-system.md` PTV3

- 삭제: - 색·간격·반경은 `src/app/styles/index.css` 의 토큰(Tailwind 유틸 `bg-background`·`text-muted-foreground` 등)만 씁니다. 컴포넌트에 색 값을 직접 적지 않습니다.
  → 잔존: `rules/design-system.md` PTV4(같은 파일·유틸 예시, 색 값 직접 적지 않음)

- 삭제: - 상승·하락과 매수·매도는 별도 토큰입니다: `tone-rise` · `tone-fall` · `tone-buy` · `tone-sell`(유틸 `text-tone-rise` 등). 지금 값은 임시이고 디자인 작업에서 정합니다. 두 쌍을 섞어 쓰지 않습니다(RULE-05).
  → 잔존: `rules/design-system.md` PTV5(같은 토큰 넷, 두 쌍을 섞지 않음, 값은 임시)

- 삭제: - 다크 모드는 `.dark` 클래스입니다. 토큰을 라이트·다크 양쪽에 정의합니다.
  → 잔존: `rules/design-system.md` PTV6

- 삭제: - 값은 문자열 그대로 받아 표시용 유틸(`src/shared/lib`, step 2 에서 도입)로만 다룹니다. 반올림·합산·비율 계산을 화면에서 하지 않습니다(RULE-03).
  → 잔존: `rules/ui-common.md` PTU5(문자열 그대로 받아 `src/shared/lib` 유틸로만, 반올림·합산·비율 계산을 화면에서 하지 않음). «step 2 에서 도입»은 이미 도입돼(`src/shared/lib/decimal.ts` 등) 낡은 사실이라 뺌

- 삭제: - 상승·하락·손익은 색 외에 부호(+/-)와 텍스트를 함께 둡니다(RULE-05).
  → 잔존: `rules/design-system.md` PTV9(같은 글)

- 삭제: - 값이 없으면 «-» 입니다(RULE-08). 가짜·예시 값으로 채우지 않습니다(RULE-13).
  → 잔존: `rules/design-system.md` PTV10(같은 글)

- 삭제: - 숫자 열은 오른쪽, 텍스트 열은 왼쪽, 배지 열은 가운데 정렬(RULE-08).
  → 잔존: `rules/design-system.md` PTV11(같은 글)

- 삭제: - 좁은 폭(768 미만)에서는 가로 스크롤과 첫 열 고정을 쓰고 열을 지우지 않습니다(EDGE-09).
  → 잔존: `rules/design-system.md` PTV12(같은 글, 대상이 표임을 밝힘 — EDGE-09 «열이 많은 표»)

- 삭제: - 데이터를 불러오는 모든 영역은 4상태(로딩·빈·에러·정상)를 가집니다. 영역 제목은 유지하고 본문 자리에 상태를 보입니다. 에러에는 «다시 시도»를 둡니다.
  → 잔존: `rules/design-system.md` PTV13(같은 4상태·제목 유지·본문 자리·다시 시도, EC1 을 영역 단위로 좁힘)

- 삭제: - 화면 문구는 한국어입니다(RULE-09). 거래 모드 라벨은 `LOCAL`·«KIS 모의».
  → 잔존: `rules/design-system.md` PTV14(같은 글, 라벨을 «고정»으로 명령형)

- 삭제: - 확인 단계가 필요한 동작: 주문 취소·구독 해지·세금 재계산(RULE-07).
  → 잔존: `rules/design-system.md` PTV15(같은 세 동작)

- 삭제: > 아래 줄은 옛 문서에서 원문 그대로 옮겼다(2026-11-21 규칙 이사). 절 제목이 출처다.
  → 잔존: 지시가 아니라 출처 안내다(ui-common.md 쪽 사본). 줄별 출처는 ui-common 조항 `Why:` 줄과 `docs/adr/rule-migration-2026-11-21.md`(이사 기록)에 남는다.

- 삭제: ## 옮겨 온 줄 — 루트 CLAUDE.md
  → 잔존: 출처 제목이다. 이사 기록이 줄별 출처를 갖는다.

- 삭제: - 프론트엔드 작업은 `rules/design-system.md`(옛 `frontend/trading-web/docs/UI_GUIDE.md`)와 이 파일을 먼저 적용한다.
  → 잔존: 지움(사람 결정 2026-11-23). 같은 일을 두 파일 머리의 `devkit:pointer`(design-system `^frontend/trading-web/src/.+\.(tsx|ts|css)$`, ui-common `^frontend/trading-web/(src/.+\.(ts|tsx)|proxy\.config\.ts)$`)가 하고, AGENTS.md 색인 «`rules/ui-common.md`·`rules/design-system.md`·`rules/architecture.md` — trading-web» 줄이 남는다.

- 삭제: ## 옮겨 온 줄 — frontend/trading-web/CLAUDE.md
  → 잔존: 출처 제목이다. 조항 `Why:` 의 출처 커밋(06f39ad·366640a·a1f9169·c044f83)과 이사 기록이 갖는다.

- 삭제: - API 호출은 shared/api/로 일원화, feature에서 직접 fetch 금지
  → 잔존: `rules/ui-common.md` PTU1 첫 구절(`src/shared/api` 로 모으고 기능 폴더에서 직접 `fetch` 하지 않음)

- 삭제: - any 금지, 불명확 타입은 unknown + 타입 가드
  → 잔존: `rules/ui-common.md` PTU2(같은 뜻). 집행은 eslint `tseslint.configs.recommended` 의 no-explicit-any

- 삭제: - 함수형 컴포넌트만 사용
  → 잔존: `rules/ui-common.md` PTU3

- 삭제: - 서버 계약(API 스키마/타입) 우회 금지
  → 잔존: `rules/ui-common.md` PTU1 끝 구절(서버 계약(API 스키마·타입)을 우회하지 않음)

- 삭제: - 금액·수량 포맷 단일화, 임의 반올림/절삭 금지
  → 잔존: `rules/ui-common.md` PTU5(유틸 하나로만 포맷, 반올림·절삭을 화면에서 하지 않음)

- 삭제: - 핵심 비즈니스 계산 UI 재구현 금지, 서버 값 우선
  → 잔존: `rules/ui-common.md` PTU5 둘째 문장(핵심 비즈니스 계산은 화면에서 다시 하지 않고 서버 값을 씀)

- 삭제: - UI 작업은 `rules/design-system.md` 를 적용한다.
  → 잔존: 지움(사람 결정 2026-11-23). 앞 «프론트엔드 작업은 … 먼저 적용한다» 줄과 같은 이유로 두 파일 pointer 와 AGENTS.md 색인이 같은 일을 한다.

- 삭제: - 금액·수량·가격은 문자열이며 `parseFloat`·`parseInt`·`Number(`·`Math.` 를 쓰지 않습니다(RULE-03).
  → 잔존: `rules/ui-common.md` PTU5 첫 문장(같은 네 낱말). 그래프 좌표 Exception 은 사람 결정 FLAG-17(2026-11-15)과 소스 검사 시험 허용 목록(`src/test/guards/source-guards.test.ts` D11 (d))에 이미 있던 것을 적음

- 삭제: - 변경 뒤 `npm run build && npm run lint && npm test -- --run` 이 모두 통과해야 합니다.
  → 잔존: `rules/ui-common.md` PTU6(같은 명령)

- 삭제: ## 옮겨 온 줄 — frontend/trading-web/README.md
  → 잔존: 출처 제목이다. PTU7 `Why:` 의 «옛 frontend/trading-web/README.md, e8bacfe»로 남는다.

- 삭제: - 브라우저는 백엔드를 직접 부르지 못하므로 개발 서버·`preview` 가 경로 접두사별로 나눠 보냅니다(`proxy.config.ts`). 백엔드에 컨트롤러 경로를 더하면 이 표도 함께 고칩니다 — `src/test/proxy-table.test.ts` 가 컨트롤러 소스를 읽어 빠진 접두사를 잡습니다.
  → 잔존: `rules/ui-common.md` PTU7(컨트롤러 경로를 더하면 표도 고침). 앞 문장은 Why, 시험은 enforce 칸 `test:frontend/trading-web/src/test/proxy-table.test.ts` 로 옮김

- 삭제: path: ^backend/(trading-api|collector-api)/src/main/.+(Toss|Broker)\w*\.kt$
  → 잔존: `rules/domains/broker.md` pointer `path: ^backend/(trading-api|collector-api)/src/main/.+(Toss|Broker|Kis)\w*\.kt$`(넓힘, 사람 결정 2026-11-23 — 묶음 2 리뷰 후속 N2)

- 삭제: why: 토스증권 등 외부 증권사 연동(토큰·호출 한도·웹소켓·주문 채널) Kotlin 코드
  → 잔존: `rules/domains/broker.md` pointer `why: 토스증권·KIS 등 외부 증권사 연동(…) Kotlin 코드`

- 삭제: 이 파일은 토스증권 Open API 를 붙이는 Kotlin 코드의 토큰, 호출 한도, 웹소켓 재연결, 주문 채널, 응답 enum 기준을 정한다. 토스 명세가 정한 제약을 어겨 연결이 끊기거나 실거래 주문이 모의 체결과 섞이는 것을 막는다. 근거 자료는 [토스증권 Open API 조사](../../docs/reference/2026-11-15-toss-open-api.md)다.
  → 잔존: `rules/domains/broker.md` 목적 문단(대상을 증권사 Open API 로 넓히고, PTK1·PTK2·PTK3·PTK5 는 토스 코드에만, PTK4 는 모든 증권사에 해당한다고 밝힘. 근거 자료 링크 그대로)

## 공식 근거 대조

근거 문서: devkit `rules/common/guides/ai-doc-writing-how.md`(작성 표준)와 그 근거 `docs/research/ai-doc-official-guidance.md`.

- 이유(rationale) 보존: 새 조항 22개 모두 `Why:` 줄을 갖는다. 출처 없는 추정 이유는 넣지 않았고, 출처에 이유가 없던 10줄은 사람이 고른 안이다(2026-11-23).
- 강조 서식: design-system 의 굵게 1곳(TDS 줄)을 뺐고 새로 더하지 않았다.
- 골격: 파일마다 pointer 머리말(그대로) · scope 줄 · `<!-- devkit:form v2 -->` · 목적 문단 · `## 조항` · 조항.
- 분량: 두 파일 합계 75줄 → 92줄(조항마다 앵커와 Why 줄이 붙음).
- 꼴 검사: `doc-form-check.js --root . --list` 에서 네 파일 위반 0, 레포 `rules/` 위반 합계 232 → 203. `rule-form-check.js --root .` exit 0 이지만 이 도구는 `rules/` 하위 폴더만 훑어 최상위 design-system·ui-common 은 재지 않는다(확인 못 함 — domains 의 quant·broker 는 잼).
