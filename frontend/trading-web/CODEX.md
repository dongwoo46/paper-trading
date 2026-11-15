# trading-web — Codex 지침

`CLAUDE.md` 와 같은 규칙입니다. 먼저 `CLAUDE.md` 를 읽고, UI 작업은 `docs/UI_GUIDE.md` 를 적용합니다.

Codex 에서 특히 지킬 것:

- 디자인 시스템은 shadcn/ui Base Nova 입니다. 새 컴포넌트는 `npx shadcn@4.23.0 add <이름>` 으로 `src/shared/ui` 에 추가하고, 직접 만든 컴포넌트는 먼저 shadcn 프리미티브로 되는지 봅니다.
- 색은 CSS 변수 토큰으로만 씁니다. 색 값(`#hex`·`oklch(...)`)을 컴포넌트에 직접 적지 않습니다. 상승·하락·매수·매도 색은 `tone` 토큰입니다.
- 금액·수량·가격은 문자열이며 `parseFloat`·`parseInt`·`Number(`·`Math.` 를 쓰지 않습니다(RULE-03).
- 옛 앱의 디자인 자산은 참고하지 않습니다(RULE-10).
- 변경 뒤 `npm run build && npm run lint && npm test -- --run` 이 모두 통과해야 합니다.
