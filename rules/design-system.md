# Design system — shadcn/ui Base Nova (paper-trading)
<!-- devkit:pointer
path: ^frontend/trading-web/src/.+\.(tsx|ts|css)$
guide: guides/design-system-how.md
why: trading-web 화면 조립·토큰·금융 값 표시·표와 영역 상태
-->
<!-- devkit:scope narrows FE1, FE9, FE24, EC1 -->
<!-- devkit:form v2 -->

이 파일은 trading-web 화면의 컴포넌트 조립, 디자인 토큰, 금융 값 표시, 표와 데이터 영역 상태, 화면 문구 기준을 정한다. 디자인 시스템이 둘로 갈리거나, 색만으로 뜻을 전하거나, 가짜 값과 지운 열 때문에 화면이 사실과 달라지는 것을 막는다. 금액 문자열 처리와 API 호출 같은 코드 기준은 [ui-common.md](./ui-common.md)에 있다.

## 조항

<!-- gt:PTV1 -->
- 화면은 `src/shared/ui` 의 shadcn/ui Base Nova 프리미티브로 조립하고 있는 것을 다시 만들지 않는다. 직접 만들 컴포넌트는 먼저 shadcn 프리미티브로 되는지 본다. devkit FE1·FE24 를 shadcn 으로 좁힌다.
  Why: trading-web 의 공식 디자인 시스템은 shadcn/ui Base Nova 하나다(CLAUDE.md, trading-web-shadcn-rebuild development-plan D1, 8e5bd7c). — enforce: review
<!-- gt:PTV2 -->
- 새 shadcn 컴포넌트는 `npx shadcn@4.23.0 add <이름>` 으로 `src/shared/ui` 에 더한다. 생성물은 사람이 고치기 전에는 그대로 두고, 필요한 변경은 호출하는 쪽에서 한다.
  Why: CLI 판이 바뀌면 생성 코드 모양이 달라져 한 앱에 두 판의 컴포넌트가 섞이고, `add` 를 다시 돌리면 생성 파일이 덮여 고친 것이 사라진다. 생성물은 옛 앱 검사(development-plan D15)의 예외로 따로 관리한다(사람 결정 2026-11-23, 8e5bd7c). — enforce: review
<!-- gt:PTV3 -->
- 아이콘은 `lucide-react` 를 쓴다.
  Why: `components.json` 의 `iconLibrary` 가 `lucide` 라 shadcn 생성물이 lucide 아이콘을 쓴다(8e5bd7c). — enforce: review
<!-- gt:PTV4 -->
- 색·간격·반경은 `src/app/styles/index.css` 의 토큰(Tailwind 유틸 `bg-background`·`text-muted-foreground` 등)만 쓰고, 색 값(`#hex`·`oklch(...)`)을 컴포넌트에 직접 적지 않는다. devkit FE9 를 이 토큰 파일로 좁힌다.
  Why: devkit FE9 와 같은 이유이고, devkit hex-guard(FE11)는 인라인 hex 만 알려 `oklch(...)` 는 이 조항이 막는다(8e5bd7c). — enforce: review
<!-- gt:PTV5 -->
- 상승·하락과 매수·매도는 별도 tone 토큰 `tone-rise`·`tone-fall`·`tone-buy`·`tone-sell`(유틸 `text-tone-rise` 등)로 쓰고, 두 쌍을 섞어 쓰지 않는다. 지금 값은 임시이고 디자인 작업에서 정한다.
  Why: 매도 주문의 손익처럼 두 축이 한 칸에 겹치면 같은 색이 무슨 뜻인지 갈리지 않는다(사람 결정 2026-11-23, feature-spec RULE-05, development-plan D16). — enforce: review
<!-- gt:PTV6 -->
- 다크 모드는 `.dark` 클래스로 켜고, 토큰은 라이트·다크 양쪽에 정의한다.
  Why: 두 테마를 모두 지원하기로 해(feature-spec 테마 행), 한쪽에만 정의한 토큰은 다른 테마에서 값이 빠진다(8e5bd7c). — enforce: review
<!-- gt:PTV7 -->
- 옛 trading-web 앱의 디자인 자산(토큰·스타일·컴포넌트)은 참고하거나 가져오지 않는다.
  Why: 사람의 절대 지시다(feature-spec RULE-10, 8e5bd7c). — enforce: test:frontend/trading-web/src/test/guards/source-guards.test.ts · review
<!-- gt:PTV8 -->
- 토스 디자인 시스템(TDS)의 공개 구현체를 이 웹에 가져와 쓰지 않는다. 토스와 비슷한 인상이 필요하면 TDS 코드를 복제하지 말고 자체 토큰과 컴포넌트로 간결한 위계, 명확한 문구, 일관된 상태 표현을 구현한다.
  Why: 라이선스 범위, 모바일 우선 컴포넌트, React·스타일링 스택이 모두 이 프로젝트와 맞지 않는다(docs/archive/research/2026-08-12-frontend-design-system.md, bf6f2c8). — enforce: review
<!-- gt:PTV9 -->
- 상승·하락·손익은 색 외에 부호(+/-)와 텍스트를 함께 둔다.
  Why: 색만으로 전하면 색각 이상이나 흑백 화면에서 등락을 못 읽는다(사람 결정 2026-11-23, WCAG 1.4.1, devkit FE30, feature-spec RULE-05). — enforce: review
<!-- gt:PTV10 -->
- 값이 없으면 «-» 로 보이고, 가짜·예시 값으로 채우지 않는다.
  Why: 예시 값은 실제 잔고·손익으로 읽혀 운영 판단을 틀리게 한다(사람 결정 2026-11-23, feature-spec RULE-08·RULE-13, screen-spec-v3 원칙 6). — enforce: review
<!-- gt:PTV11 -->
- 표의 숫자 열은 오른쪽, 텍스트 열은 왼쪽, 배지 열은 가운데로 정렬한다.
  Why: 숫자를 오른쪽에 맞춰야 자릿수가 세로로 맞아 금액을 견줄 수 있다(사람 결정 2026-11-23, feature-spec RULE-08). — enforce: review
<!-- gt:PTV12 -->
- 좁은 폭(768 미만)의 표는 가로 스크롤과 첫 열 고정을 쓰고 열을 지우지 않는다.
  Why: 열을 지우면 좁은 화면에서만 금액·상태가 사라져 폭마다 다른 정보를 보인다(사람 결정 2026-11-23, feature-spec EDGE-09). — enforce: review
<!-- gt:PTV13 -->
- 데이터를 불러오는 영역마다 로딩·빈·에러·정상 4상태를 다룬다. 영역 제목은 유지하고 본문 자리에 상태를 보이며, 에러에는 «다시 시도»를 둔다. devkit EC1(화면 상태)을 화면 안 영역 단위로 좁힌다.
  Why: 해피패스만 다루지 않는다는 devkit EC1 원칙을 화면 안 영역마다 지킨다(screen-spec-v2 §0 8번, d41db8a, 사람 결정 2026-11-23). — enforce: review
<!-- gt:PTV14 -->
- 화면 문구는 한국어로 쓰고, 거래 모드 라벨은 `LOCAL`·«KIS 모의» 로 고정한다.
  Why: 라벨을 고정해야 LOCAL 과 KIS 모의를 헷갈리지 않는다(사람 결정 2026-11-23, feature-spec RULE-09). — enforce: review
<!-- gt:PTV15 -->
- 주문 취소, 구독 해지, 세금 재계산은 확인 단계를 거친다.
  Why: 누르는 즉시 서버 상태가 바뀌고 주문 취소는 되돌릴 수 없다(사람 결정 2026-11-23, feature-spec RULE-07). — enforce: review
