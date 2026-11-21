# UI common — trading-web 코드 규칙 (paper-trading)
<!-- devkit:pointer
path: ^frontend/trading-web/(src/.+\.(ts|tsx)|proxy\.config\.ts)$
why: trading-web 의 React·TypeScript 코드, API 호출, 금액 표시, 프록시 표
-->
<!-- devkit:scope narrows FE7, FE34 -->

> 아래 줄은 옛 문서에서 원문 그대로 옮겼다(2026-11-21 규칙 이사). 절 제목이 출처다.

## 옮겨 온 줄 — 루트 CLAUDE.md

- 프론트엔드 작업은 `frontend/trading-web/CODEX.md`와 `frontend/trading-web/docs/UI_GUIDE.md`를 먼저 적용한다.

## 옮겨 온 줄 — frontend/trading-web/CLAUDE.md

- API 호출은 shared/api/로 일원화, feature에서 직접 fetch 금지

- any 금지, 불명확 타입은 unknown + 타입 가드
- 함수형 컴포넌트만 사용
- 사용자 행동이 일으킨 효과는 이벤트 핸들러에 두고, `useEffect` 는 외부 시스템과 동기화할 때만 쓴다.

- 서버 계약(API 스키마/타입) 우회 금지
- 금액·수량 포맷 단일화, 임의 반올림/절삭 금지
- 핵심 비즈니스 계산 UI 재구현 금지, 서버 값 우선

## 옮겨 온 줄 — frontend/trading-web/CODEX.md

- `CLAUDE.md` 와 같은 규칙입니다. 먼저 `CLAUDE.md` 를 읽고, UI 작업은 `docs/UI_GUIDE.md` 를 적용합니다.

- 금액·수량·가격은 문자열이며 `parseFloat`·`parseInt`·`Number(`·`Math.` 를 쓰지 않습니다(RULE-03).
- 변경 뒤 `npm run build && npm run lint && npm test -- --run` 이 모두 통과해야 합니다.

## 옮겨 온 줄 — frontend/trading-web/README.md

- 브라우저는 백엔드를 직접 부르지 못하므로 개발 서버·`preview` 가 경로 접두사별로 나눠 보냅니다(`proxy.config.ts`). 백엔드에 컨트롤러 경로를 더하면 이 표도 함께 고칩니다 — `src/test/proxy-table.test.ts` 가 컨트롤러 소스를 읽어 빠진 접두사를 잡습니다.
