# Design system — shadcn/ui Base Nova (paper-trading)
<!-- devkit:pointer
path: ^frontend/trading-web/src/.+\.(tsx|ts|css)$
guide: guides/design-system-how.md
why: trading-web 화면 조립·토큰·금융 값 표시·표와 영역 상태
-->
<!-- devkit:scope narrows FE1, FE9, FE24 -->

> 아래 줄은 옛 문서에서 원문 그대로 옮겼다(2026-11-21 규칙 이사). 절 제목이 출처다.

## 옮겨 온 줄 — docs/research/2026-08-12-frontend-design-system.md

- 토스의 원칙과 시각적 절제는 참고할 가치가 있지만, **공개 TDS 구현체를 이 독립 퀀트 웹에 가져와 쓰면 안 된다.** 라이선스 범위, 모바일 우선 컴포넌트, React/스타일링 스택 모두 현재 프로젝트와 맞지 않는다. 토스와 비슷한 인상을 원하면 TDS 코드를 복제하는 대신 자체 토큰과 컴포넌트에서 간결한 위계·명확한 문구·일관된 상태 표현을 구현한다.

## 옮겨 온 줄 — frontend/trading-web/CODEX.md

- 디자인 시스템은 shadcn/ui Base Nova 입니다. 새 컴포넌트는 `npx shadcn@4.23.0 add <이름>` 으로 `src/shared/ui` 에 추가하고, 직접 만든 컴포넌트는 먼저 shadcn 프리미티브로 되는지 봅니다.
- 색은 CSS 변수 토큰으로만 씁니다. 색 값(`#hex`·`oklch(...)`)을 컴포넌트에 직접 적지 않습니다. 상승·하락·매수·매도 색은 `tone` 토큰입니다.
- 옛 앱의 디자인 자산은 참고하지 않습니다(RULE-10).

## 옮겨 온 줄 — frontend/trading-web/docs/UI_GUIDE.md

- 화면은 shadcn 프리미티브(`src/shared/ui`)로 조립합니다. 있는 것을 다시 만들지 않습니다.
- 추가는 `npx shadcn@4.23.0 add <이름>`. 생성물은 사람이 고치기 전에 그대로 두고, 필요한 변경은 호출하는 쪽에서 합니다.
- 아이콘은 `lucide-react` 입니다.

- 색·간격·반경은 `src/app/styles/index.css` 의 토큰(Tailwind 유틸 `bg-background`·`text-muted-foreground` 등)만 씁니다. 컴포넌트에 색 값을 직접 적지 않습니다.
- 상승·하락과 매수·매도는 별도 토큰입니다: `tone-rise` · `tone-fall` · `tone-buy` · `tone-sell`(유틸 `text-tone-rise` 등). 지금 값은 임시이고 디자인 작업에서 정합니다. 두 쌍을 섞어 쓰지 않습니다(RULE-05).
- 다크 모드는 `.dark` 클래스입니다. 토큰을 라이트·다크 양쪽에 정의합니다.

- 값은 문자열 그대로 받아 표시용 유틸(`src/shared/lib`, step 2 에서 도입)로만 다룹니다. 반올림·합산·비율 계산을 화면에서 하지 않습니다(RULE-03).
- 상승·하락·손익은 색 외에 부호(+/-)와 텍스트를 함께 둡니다(RULE-05).
- 값이 없으면 «-» 입니다(RULE-08). 가짜·예시 값으로 채우지 않습니다(RULE-13).

- 숫자 열은 오른쪽, 텍스트 열은 왼쪽, 배지 열은 가운데 정렬(RULE-08).
- 좁은 폭(768 미만)에서는 가로 스크롤과 첫 열 고정을 쓰고 열을 지우지 않습니다(EDGE-09).
- 데이터를 불러오는 모든 영역은 4상태(로딩·빈·에러·정상)를 가집니다. 영역 제목은 유지하고 본문 자리에 상태를 보입니다. 에러에는 «다시 시도»를 둡니다.

- 화면 문구는 한국어입니다(RULE-09). 거래 모드 라벨은 `LOCAL`·«KIS 모의».
- 확인 단계가 필요한 동작: 주문 취소·구독 해지·세금 재계산(RULE-07).
