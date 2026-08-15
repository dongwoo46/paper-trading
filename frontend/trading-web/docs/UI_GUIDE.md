# UI_GUIDE — trading-web

## 공식 디자인 시스템

trading-web은 Tailwind CSS 4 기반 **shadcn/ui Base Nova**를 공식 디자인 시스템으로 사용한다.
테마의 단일 소스는 `src/app/styles/index.css`, shadcn 생성 설정은 `components.json`이다.

| 구분 | 위치 | 책임 |
| --- | --- | --- |
| shadcn 프리미티브 | `src/shared/ui/shadcn` | Button, Card, Input, Textarea, NativeSelect, Checkbox, Tabs, Dialog, Table처럼 도메인을 모르는 기초 UI |
| 공용 합성 컴포넌트 | `src/shared/ui` | SectionCard, Chip처럼 여러 화면이 공유하는 조합 |
| 기능 UI | `src/features/*/ui` | 주문·계좌처럼 도메인 의미가 있는 조합 |
| 의미 토큰 | `src/app/styles/index.css` | 색상, 간격, radius, 거래 방향 |

## 구현 순서

1. `src/shared/ui/shadcn`과 `src/shared/ui`에 같은 역할의 컴포넌트가 있는지 먼저 찾는다.
2. 기존 프리미티브를 variant와 합성으로 사용한다.
3. 없는 shadcn 컴포넌트는 frontend 디렉터리에서 `npx shadcn@latest add <component>`로 추가한다.
4. 공용 프리미티브에 주문·계좌 API 타입이나 도메인 계산을 넣지 않는다.
5. 새로운 export는 reuse-scan으로 중복을 확인한다.

shadcn 컴포넌트 소스는 이 저장소가 소유한다. 생성 후 프로젝트 토큰과 접근성 계약에 맞게 수정할 수 있지만, 같은 역할의 별도 구현을 병행해서 만들지 않는다.

## 의미 토큰

일반 UI는 `background`, `foreground`, `card`, `primary`, `secondary`, `muted`, `destructive`, `border`, `input`, `ring`을 사용한다.

거래 의미는 일반 상태 토큰과 분리한다.

| 의미 | Tailwind 토큰 |
| --- | --- |
| 매수 | `order-buy` |
| 매도 | `order-sell` |
| 양의 손익 | `market-positive` |
| 음의 손익 | `market-negative` |
| 주의 상태 | `market-warning` |

캔버스/SVG 차트는 `chart-*` 토큰을 사용한다. `lightweight-charts`처럼 문자열 색상을 요구하는 라이브러리는 컴포넌트에서 색을 선언하지 않고, `src/app/styles/index.css`의 `--chart-*` 값을 런타임 어댑터가 읽어서 전달한다.

컴포넌트에 hex, rgb, 임의 CSS 변수 fallback 또는 색상용 인라인 스타일을 추가하지 않는다.

## 컴포넌트 계약

- 폼 컨트롤은 Label과 `htmlFor`/`id`로 연결한다.
- 버튼은 Button variant를 사용하고 아이콘 전용 버튼에는 접근 가능한 이름을 제공한다.
- 조밀한 거래 데이터는 Table 프리미티브를 사용해 의미 있는 table role과 가로 스크롤을 유지한다.
- 오류는 Alert의 destructive variant, 로딩 자리표시는 Skeleton을 사용한다.
- 탭 전환은 Tabs, 도움말/확인 레이어는 Dialog, 긴 문장은 Textarea, 불리언 입력은 Checkbox를 사용한다.
- 매수·매도와 손익 표시는 색상만이 아니라 텍스트 또는 `data-tone` 의미도 함께 제공한다.
- 초기·로딩·빈·성공·에러·권한없음 상태를 각각 사용자에게 명시한다.
- `app-design-system.test.ts`가 모든 production TSX의 원시 컨트롤, 레거시 토큰, 하드코딩 팔레트를 전수 검사한다.

## 금지

- shadcn에 같은 컴포넌트가 있는데 원시 button/input/select/table을 새로 스타일링하기
- 다크 테마 클래스와 현재 라이트 토큰을 혼합하기
- glassmorphism, 네온, 장식용 그라디언트를 화면 전반에 적용하기
- 상태 이름과 시장 방향을 같은 토큰으로 간주하기
- 기능 한 곳에서만 쓰는 도메인 UI를 shared로 승격하기
