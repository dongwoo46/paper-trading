@../../CODEX.md

## trading-web
운영 대시보드 UI
React / TypeScript / Vite

빌드 검증: npm run build

## 아키텍처
FSD 구조: pages → features → entities → shared (역방향 의존 금지)
API 호출은 shared/api/로 일원화, feature에서 직접 fetch 금지

## 코드 규칙
- any 금지, 불명확 타입은 unknown + 타입 가드
- 함수형 컴포넌트만 사용
- 상태 최소화, 서버 상태는 전용 관리 도구 사용
- 사이드이펙트는 useEffect 내부에서만 처리
- 로딩/에러/빈 상태 분리해서 사용자에게 명시

## 디자인 시스템
- trading-web의 공식 디자인 시스템은 shadcn/ui Base Nova다.
- 구현 계약과 사용 예시는 `docs/UI_GUIDE.md`를 단일 기준으로 따른다.
- 새 UI를 만들기 전에 `src/shared/ui/shadcn` 프리미티브와 `src/shared/ui`의 기존 합성 컴포넌트를 먼저 찾는다.
- Button, Input, Textarea, Label, NativeSelect, Checkbox, Tabs, Dialog, Card, Table, Badge, Alert, Skeleton과 같은 동등 프리미티브가 있으면 원시 HTML을 새로 스타일링하지 않는다.
- 공용 프리미티브는 `shared/ui/shadcn`, 도메인 조합은 해당 `features/*/ui`에 둔다.
- 색상·간격·radius는 `src/app/styles/index.css`의 의미 토큰만 사용한다. 컴포넌트에 hex·rgb·일회성 인라인 스타일을 추가하지 않는다.
- 매수·매도와 손익 색상은 각각 `order-buy`/`order-sell`, `market-positive`/`market-negative` 토큰을 사용한다.
- Canvas/SVG 차트 색상도 `chart-*` 토큰을 사용하며 컴포넌트 파일에 색상 문자열을 작성하지 않는다.

## 안정성
- 서버 계약(API 스키마/타입) 우회 금지
- 금액·수량 포맷 단일화, 임의 반올림/절삭 금지
- 인증 토큰/시크릿 평문 저장·로그 금지
- 핵심 비즈니스 계산 UI 재구현 금지, 서버 값 우선
