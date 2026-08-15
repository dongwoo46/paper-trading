# Research Brief — 퀀트 트레이딩 웹 디자인 시스템 후보

> **조사 결과 문서.** 공개 디자인 시스템의 현재 지원 범위와 이 프로젝트의 프론트엔드 스택을 대조해 도입 후보를 고른다.
> 원칙: 사실과 추론을 구분하며, 라이선스와 호환성은 공식 문서를 우선한다.

---

## 0. 메타데이터

| 항목 | 값 |
|---|---|
| 주제 | `trading-web`에 도입할 오픈소스 디자인 시스템과 Toss Design System 사용 가능성 |
| 용도 | 개발 / 기획 |
| 조사자 | Codex |
| 날짜 | 2026-08-12 |
| 범위·질문 | React 19 + Vite + Tailwind CSS 4 기반 퀀트 트레이딩 웹에 맞는 공개 시스템은 무엇이며, 토스 TDS를 독립 웹 프로젝트에서 쓸 수 있는가? |
| 깊이 | standard |
| 생성 하네스 | devkit v0.38.0 |

---

## 1. 핵심 발견 (결론 먼저)

- **1순위는 shadcn/ui를 기반으로 프로젝트 전용 Quant UI 규칙을 얹는 방식이다.** 현재 스택과 그대로 맞고, 컴포넌트 코드를 프로젝트가 소유하므로 주문·포지션·차트처럼 도메인 특화 UI를 바꾸기 쉽다. [S-1, S-4, S-5]
- **빠르게 완성된 컴포넌트 묶음이 필요하면 Mantine 9가 2순위다.** React 19.2 이상, Vite, 다크모드, 100개 이상의 컴포넌트를 지원하지만 Tailwind와 별도의 스타일 체계를 함께 운영하게 된다. [S-1, S-7, S-8]
- **Tremor는 전체 디자인 시스템보다 분석 위젯 보조재로 적합하다.** 대시보드·차트용 공개 컴포넌트가 강점이지만, 실시간 캔들 차트는 현재 `lightweight-charts`를 유지하는 편이 맞다. [S-1, S-6]
- **토스가 외부에 제공하는 TDS는 앱인토스용이다.** 공개 문서의 사용 범위는 앱인토스 앱으로 제한되고 다른 프로젝트 사용은 금지된다. 설치 안내도 React 18 + Emotion 기반이므로 이 독립 웹 앱에는 도입하지 않는다. [S-11, S-12, S-13]
- 현재 코드의 색상 토큰은 이미 토스풍 라이트 테마인데, 프로젝트 UI 문서는 다크 글래스 테마를 권위 규칙처럼 설명한다. 시스템 도입 전에 이 충돌부터 하나의 방향으로 확정해야 한다. [S-2, S-3]

---

## 2. 평가 기준

| 기준 | 이 프로젝트에서 중요한 이유 | 근거 |
|---|---|---|
| 현재 스택 적합성 | 이미 React 19.2.4, Vite 8, Tailwind CSS 4.2.4를 사용한다. | [S-1] |
| 도메인 확장성 | 주문 티켓, 포지션 표, 실시간 상태, 시세 차트는 일반 소비자 앱 컴포넌트만으로 표현하기 어렵다. | [S-3] |
| 고밀도 정보 표현 | 표·수치·필터·차트가 한 화면에 함께 존재한다. | [S-3] |
| 접근성·상태 일관성 | 입력, 모달, 메뉴, 포커스, 오류 상태를 개별 화면마다 다시 만들지 않아야 한다. | [S-4, S-7, S-9] |
| 테마·토큰 소유권 | 상승/하락, 매수/매도, 지연/실시간 등 거래 도메인의 의미를 시각 토큰으로 고정해야 한다. | [S-2, S-3] |
| 공개 라이선스 | 독립 프로젝트에서 수정·배포할 권리가 명확해야 한다. | [S-5, S-7, S-9, S-10, S-13] |

---

## 3. 후보 비교

점수는 이 프로젝트에 대한 **추론 평가**다. 5점이 가장 적합하다.

| 순위 | 후보 | 스택 적합 | 거래 UI 확장 | 즉시 완성도 | 운영 용이 | 라이선스 | 판단 |
|---:|---|---:|---:|---:|---:|---|---|
| 1 | **shadcn/ui** | 5 | 5 | 4 | 4 | MIT | 채택 권장. Vite 설치 경로와 React 19/Tailwind 4 지원이 있고, 복사된 코드를 직접 소유·수정한다. [S-4, S-5] |
| 2 | **Mantine 9** | 4 | 4 | 5 | 4 | MIT | 풀세트가 필요할 때 가장 실용적. React 19.2+와 Vite에 맞지만 기존 Tailwind 토큰과 Mantine 테마가 이중화된다. [S-7, S-8] |
| 3 | **Blend Design System** | 3 | 5 | 4 | 2 | README상 MIT* | 핀테크 제품용이며 DataTable·통계 카드·차트를 포함한다. 다만 styled-components 기반이고 공개 생태계가 아직 작아 전면 채택보다 참고/파일럿이 안전하다. [S-9] |
| 4 | **IBM Carbon** | 2 | 4 | 5 | 2 | Apache-2.0 | 성숙한 엔터프라이즈 시스템이나 React·Sass·자체 토큰 체계를 통째로 들여와야 해 현재 Tailwind 앱에는 과하다. [S-10] |
| 보조 | **Tremor** | 5 | 3 | 4 | 4 | Apache-2.0 | 전체 기반이 아니라 KPI, 스파크라인, 분석 카드, 백테스트 요약에 선택적으로 사용한다. [S-6] |

### 3.1 shadcn/ui — 권장 기반

- **사실:** 공식 문서는 shadcn/ui를 전통적인 NPM 컴포넌트 라이브러리가 아니라 접근 가능한 컴포넌트의 코드 배포 방식으로 정의한다. 설치된 컴포넌트의 상위 코드를 직접 수정할 수 있고, Vite용 기존 프로젝트 설치 경로를 제공한다. [S-4]
- **사실:** React 19와 Tailwind CSS 4 지원, `@theme` 사용, Data Table·Chart·Sidebar·Dialog·Select 등 현재 앱에 필요한 기본 컴포넌트를 제공한다. 저장소 라이선스는 MIT다. [S-4, S-5]
- **추론:** 이미 Tailwind 4 토큰과 Lucide 아이콘을 쓰는 현재 코드에 가장 적은 충돌로 들어간다. `shared/ui`에 복사한 뒤 거래 도메인용 컴포넌트를 그 위에 구축하기 좋다. [S-1, S-3]
- **주의:** 코드를 소유하는 만큼 업스트림 업데이트를 자동으로 받는 패키지 방식은 아니다. 어떤 컴포넌트를 들였고 어떤 변경을 했는지 프로젝트가 관리해야 한다. [S-4]

### 3.2 Mantine 9 — 완제품 우선 대안

- **사실:** Mantine은 100개 이상의 코어 컴포넌트와 폼, 차트, 알림, 모달, 명령 팔레트 등의 패키지를 제공하며 MIT 라이선스다. 공식 문서는 SPA에 Vite를 권장한다. [S-7]
- **사실:** Mantine 9는 React 19.2 이상을 요구하므로 현재 React 19.2.4와 맞는다. 라이트·다크·시스템 색상 모드를 공식 지원한다. [S-1, S-8]
- **추론:** 날짜·숫자 입력, 모달, 알림 등 폼 중심 화면을 빨리 통일하는 데 유리하다. 반면 현재 Tailwind `@theme`와 MantineProvider 테마를 함께 운영하면 토큰의 권위가 둘로 갈릴 수 있다. [S-2, S-7]

### 3.3 Blend — 금융 특화 참고 후보

- **사실:** Juspay의 실제 핀테크 제품에 쓰이는 React 디자인 시스템이며 30개 이상의 컴포넌트, DataTable, Recharts 기반 Chart, StatCard, 토큰 시스템과 접근성 지원을 공개한다. 저장소 README는 MIT 라이선스라고 명시한다. [S-9]
- **사실:** 스타일 계층은 Tailwind가 아니라 styled-components와 Radix UI를 사용한다. 공개 GitHub 저장소의 규모와 사용 흔적은 shadcn/ui·Mantine·Carbon보다 작다. [S-9]
- **추론:** 금융 화면 패턴을 참고하기에는 매우 좋지만, 지금 앱의 기반으로 곧바로 채택하면 새 스타일 런타임과 작은 생태계에 의존하게 된다. 한 페이지 파일럿 전에는 전면 도입하지 않는다.

### 3.4 Carbon — 엔터프라이즈 우선 대안

- **사실:** IBM의 공개 디자인 시스템이며 React 컴포넌트, Sass 스타일, 디자인 토큰, 아이콘, 테마 패키지를 함께 제공한다. Apache-2.0 라이선스다. [S-10]
- **추론:** 복잡한 데이터 업무 화면과 접근성 규율은 강하지만, IBM 시각 언어와 Sass 중심 기반을 현재 앱에 맞게 걷어내는 비용이 크다. 대규모 다팀 제품이 아니라면 이 프로젝트에는 과하다.

### 3.5 Tremor — 분석 UI 보조재

- **사실:** React, Tailwind CSS, Radix UI 기반의 35개 이상 공개 대시보드·차트 컴포넌트를 제공하며 Recharts를 사용한다. 저장소 라이선스는 Apache-2.0이다. [S-6]
- **추론:** 백테스트 결과, KPI, 드로다운, 분포, 스파크라인에는 잘 맞는다. 현재의 실시간/금융 차트 엔진 `lightweight-charts`를 대체할 이유는 없다. [S-1]

---

## 4. Toss Design System 확인 결과

### 확인된 사실

- 토스는 TDS를 실제 운영하며 일관성, 제작 속도, 접근성, 다크모드 같은 반복 문제를 컴포넌트에서 해결한다고 설명한다. [S-14]
- 외부 개발자에게 공개된 설치 경로는 **앱인토스 WebView용** `@toss/tds-mobile`, `@toss/tds-mobile-ait`이며 공식 설치 예시는 React 18, React DOM 18, Emotion 11을 함께 요구한다. [S-12]
- 앱인토스 TDS 자료의 권리는 토스에 있고, 사용 허가는 앱인토스 서비스를 위한 범위로 제한된다. UI Kit의 다른 프로젝트·제품·서비스 사용은 명시적으로 금지된다. [S-11, S-13]
- 공개 앱인토스 디자인 자료는 폭 375px 모바일 화면을 기준으로 하고, 현재 다크모드는 추후 지원 사항으로 안내한다. [S-15]

### 결론

토스의 원칙과 시각적 절제는 참고할 가치가 있지만, **공개 TDS 구현체를 이 독립 퀀트 웹에 가져와 쓰면 안 된다.** 라이선스 범위, 모바일 우선 컴포넌트, React/스타일링 스택 모두 현재 프로젝트와 맞지 않는다. 토스와 비슷한 인상을 원하면 TDS 코드를 복제하는 대신 자체 토큰과 컴포넌트에서 간결한 위계·명확한 문구·일관된 상태 표현을 구현한다.

---

## 5. 권장 목표 구조

다음은 조사 결과에 따른 **설계 제안**이며 아직 구현 결정은 아니다.

```text
Tailwind CSS 4 semantic tokens
        ↓
shadcn/ui primitives in shared/ui
        ↓
Quant domain components
  ├─ MetricCard / PnLValue / MarketStatus
  ├─ DataTable / PositionTable / OrderTable
  ├─ OrderTicket / PriceInput / QuantityInput
  ├─ ChartPanel (existing lightweight-charts)
  └─ Empty / Loading / Error / Permission states
        ↓
pages & features
```

### 프로젝트 전용 토큰에서 분리할 의미

- 표면: `canvas`, `surface`, `raised`, `overlay`
- 텍스트: `primary`, `secondary`, `muted`, `inverse`
- 시장 값: `price-up`, `price-down`, `price-flat`
- 주문 행동: `buy`, `sell`, `cancel` — 손익의 성공/실패 색과 별도
- 데이터 상태: `live`, `delayed`, `stale`, `disconnected`
- 숫자: tabular 숫자, 금액·수량·비율별 정렬/정밀도 규칙
- 밀도: `comfortable`, `compact` 행 높이와 간격

---

## 6. 열린 질문 / 미확인

- 최종 제품의 기본 테마를 현재 코드의 **토스풍 라이트**로 할지, 문서의 **프로 트레이더 다크**로 할지 사용자 결정이 필요하다. [S-2, S-3]
- 모바일을 동등한 거래 화면으로 지원할지, 조회 중심의 축약 화면으로 지원할지 아직 명세가 없다.
- Blend의 현재 패키지가 이 프로젝트의 React 19.2.4에서 문제없이 동작하는지는 실제 최소 설치 파일럿으로 검증하지 않았다.
- Blend 저장소에서 별도 `LICENSE` 파일은 이번 조사로 확인하지 못했다. README의 MIT 표기만으로 채택하지 말고 도입 전에 라이선스 파일을 다시 확인해야 한다.
- 후보 라이브러리의 번들 크기와 기존 화면 마이그레이션 공수는 구현 전 스파이크에서 측정해야 한다.

---

## 7. 권장 다음 액션

1. **기반 선택:** shadcn/ui를 기본안으로 승인한다.
2. **시각 방향 선택:** 라이트 우선 / 다크 우선 / 둘 다 지원 중 하나를 확정한다.
3. **한 페이지 파일럿:** 주문 화면 하나에서 Button, Input, Select, Dialog, Toast, Table, 상태 토큰만 교체한다.
4. **검증:** 기존 테스트, 키보드 조작, 포커스, 1024px 이하 레이아웃, 상승/하락 색 의미, 빌드 크기를 비교한다.
5. **확대:** 파일럿 통과 뒤 `shared/ui`와 Quant 도메인 컴포넌트를 확정하고 나머지 화면을 순차 마이그레이션한다.

---

## 8. 출처

| id | 출처 | 종류 | 날짜/버전 | 비고 |
|---|---|---|---|---|
| S-1 | [`frontend/trading-web/package.json`](../../frontend/trading-web/package.json) | 내부 코드 | 2026-08-12 확인 | React 19.2.4, Vite 8.0.1, Tailwind 4.2.4, lightweight-charts 5.2.0 |
| S-2 | [`frontend/trading-web/src/app/styles/index.css`](../../frontend/trading-web/src/app/styles/index.css) | 내부 코드 | 2026-08-12 확인 | 토스풍 라이트 토큰과 상승/하락 색 정의 |
| S-3 | [`frontend/trading-web/README.md`](../../frontend/trading-web/README.md), [`UI_GUIDE.md`](../../frontend/trading-web/docs/UI_GUIDE.md), [`shared/ui/index.tsx`](../../frontend/trading-web/src/shared/ui/index.tsx) | 내부 문서·코드 | 2026-08-12 확인 | 다크 글래스 지침과 현재 공통 UI 구조 |
| S-4 | [shadcn/ui Introduction](https://ui.shadcn.com/docs), [Vite 설치](https://ui.shadcn.com/docs/installation/vite), [Tailwind v4](https://v3.shadcn.com/docs/tailwind-v4) | 공식 문서 | 2026-08-12 확인 | Open Code, Vite, React 19/Tailwind 4 |
| S-5 | [shadcn/ui LICENSE](https://github.com/shadcn-ui/ui/blob/main/LICENSE.md) | 공식 저장소 | MIT | 라이선스 |
| S-6 | [Tremor](https://www.tremor.so/), [Tremor GitHub](https://github.com/tremorlabs/tremor-npm) | 공식 문서·저장소 | 2026-08-12 확인 | React/Tailwind/Radix, 대시보드·차트, Apache-2.0 |
| S-7 | [Mantine Getting started](https://mantine.dev/getting-started/), [Mantine GitHub](https://github.com/mantinedev/mantine), [Color schemes](https://mantine.dev/theming/color-schemes/) | 공식 문서·저장소 | Mantine 9.x, 2026-08-12 확인 | Vite 권장, MIT, 컴포넌트 묶음, 테마 |
| S-8 | [Mantine 8.x → 9.x migration](https://mantine.dev/guides/8x-to-9x/) | 공식 문서 | Mantine 9.x | React 19.2+ 요구 |
| S-9 | [Juspay Blend Design System](https://github.com/juspay/blend-design-system) | 공식 저장소 | 2026-08-12 확인 | 핀테크 React 시스템, styled-components/Radix, 컴포넌트 범위 |
| S-10 | [IBM Carbon GitHub](https://github.com/carbon-design-system/carbon) | 공식 저장소 | Carbon 11.x, 2026-08-12 확인 | React/Sass/토큰, Apache-2.0 |
| S-11 | [앱인토스 TDS 컴포넌트](https://developers-apps-in-toss.toss.im/design/components.html) | 토스 공식 문서 | 2026-08-12 확인 | 앱인토스 사용 범위와 지식재산권 |
| S-12 | [기존 웹 프로젝트에 앱인토스 SDK 연동](https://developers-apps-in-toss.toss.im/tutorials/webview.html) | 토스 공식 문서 | 2026-08-12 확인 | `@toss/tds-mobile`, React 18, Emotion 설치 안내 |
| S-13 | [피그마/TDS Mobile UI Kit 라이선스](https://developers-apps-in-toss.toss.im/design/prepare/figma-ui-license.html) | 토스 공식 라이선스 | 2026-08-12 확인 | 다른 프로젝트 사용 금지 |
| S-14 | [토스 디자이너가 제품에만 집중할 수 있는 방법](https://toss.tech/article/toss-design-system), [디자인 시스템 다시 생각해보기](https://toss.tech/article/44097) | 토스 공식 기술 블로그 | 2024-03-05 / 2026-01-08 | TDS 운영 원칙과 확장성 |
| S-15 | [앱인토스 디자인 도구](https://developers-apps-in-toss.toss.im/design/prepare/design.html) | 토스 공식 문서 | 2026-08-12 확인 | 375px 모바일 기준, 다크모드 상태 |
