# UI common — trading-web 코드 규칙 (paper-trading)
<!-- devkit:pointer
path: ^frontend/trading-web/(src/.+\.(ts|tsx)|proxy\.config\.ts)$
why: trading-web 의 React·TypeScript 코드, API 호출, 금액 표시, 프록시 표
-->
<!-- devkit:scope narrows FE7, FE34 -->
<!-- devkit:form v2 -->

이 파일은 trading-web 의 React·TypeScript 코드에서 API 호출 경계, 타입, 컴포넌트와 effect, 금융 값 처리, 검증 명령, 프록시 표 기준을 정한다. 금융 값이 부동소수점으로 바뀌거나, 화면이 서버 계약을 우회하거나, 백엔드 경로가 프록시 표에서 빠지는 것을 막는다. 화면 조립과 표시 기준은 [design-system.md](./design-system.md)에 있다.

## 조항

<!-- gt:PTU1 -->
- API 호출은 `src/shared/api` 로 모으고 기능 폴더에서 직접 `fetch` 하지 않으며, 서버 계약(API 스키마·타입)을 우회하지 않는다. devkit FE34 를 이 폴더로 좁힌다.
  Why: 응답의 JSON 숫자를 원문 문자열로 보존하는 파싱과 디코더가 이 폴더에만 있어(`src/shared/api/parse-json.ts`, development-plan D4), 밖에서 부르면 금액이 부동소수점으로 바뀐다(옛 하네스 설계 06f39ad·a1f9169). — enforce: review
<!-- gt:PTU2 -->
- `any` 를 쓰지 않고, 타입이 불명확하면 `unknown` 과 타입 가드로 좁힌다.
  Why: `any` 는 타입 검사를 끄고, 응답 디코더가 `unknown` 과 타입 가드로 금융 값을 검증한다(development-plan D4, 366640a). — enforce: eslint:@typescript-eslint/no-explicit-any · review
<!-- gt:PTU3 -->
- 컴포넌트는 함수형으로만 쓴다.
  Why: React 공식 문서가 새 컴포넌트를 함수로 쓰라고 하고 훅은 함수형에서만 쓴다(사람 결정 2026-11-23, 06f39ad). — enforce: review
<!-- gt:PTU4 -->
- 사용자 행동이 일으킨 효과는 이벤트 핸들러에 두고, `useEffect` 는 외부 시스템과 동기화할 때만 쓴다.
  Why: 옛 줄 «사이드이펙트는 useEffect 내부에서만»이 React 공식 문서(You Might Not Need an Effect)와 어긋나 사람 결정으로 고쳐 쓴 조항이다(사람 결정 2026-11-21, docs/adr/rule-migration-2026-11-21.md). — enforce: review
<!-- gt:PTU5 -->
- 금액·수량·가격은 서버가 준 문자열 그대로 받아 표시용 유틸(`src/shared/lib`) 하나로만 포맷하고 `parseFloat`·`parseInt`·`Number(`·`Math.` 를 쓰지 않는다. 화면에서 반올림·합산·비율 계산과 임의 절삭을 하지 않고, 핵심 비즈니스 계산은 화면에서 다시 하지 않고 서버 값을 쓴다.
  Why: 부동소수점 변환은 금액을 조용히 틀리게 해 화면 계산을 막고 서버 값만 쓰기로 했다(CLAUDE.md 금융 안전, feature-spec RULE-03, a1f9169·c044f83·8e5bd7c). — enforce: test:frontend/trading-web/src/test/guards/source-guards.test.ts · review
  Exception: 자산 추이 그래프의 좌표 계산만 숫자 변환을 쓴다. 변환 함수는 `src/shared/lib/chart-coordinate.ts` 한 곳이고, 라벨·툴팁·축 문구는 서버 원문 문자열이다(사람 결정 FLAG-17 2026-11-15, 2026-11-23).
<!-- gt:PTU6 -->
- trading-web 을 바꾼 뒤에는 `npm run build && npm run lint && npm test -- --run` 이 모두 통과해야 한다.
  Why: CI trading-web 잡이 세 명령을 모두 돌린다(development-plan DEC-08, `.github/workflows/full-test.yml`, 8e5bd7c). — enforce: review
<!-- gt:PTU7 -->
- 백엔드에 컨트롤러 경로를 더하면 `proxy.config.ts` 의 경로 접두사 표도 함께 고친다.
  Why: 브라우저는 백엔드를 직접 부르지 못해 개발 서버와 `preview` 가 이 표로 경로 접두사별로 나눠 보낸다(옛 frontend/trading-web/README.md, e8bacfe). — enforce: test:frontend/trading-web/src/test/proxy-table.test.ts · review
