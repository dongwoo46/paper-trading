# Step 5 — 반응형·접근성·motion 정리

## 0. 메타데이터

| 항목 | 값 |
|---|---|
| step | 5 |
| 제목 | 반응형·접근성·motion 정리 |
| 기능 | trading-web-visual-redesign |
| 근거 | feature-spec EDGE-01·05·06, development-plan §2·5 |
| 상태 | 완료 |
| 담당 | Codex |

## 1. 목표 · 범위

- 작은 화면에서 긴 메뉴와 주요 콘텐츠가 잘리거나 가로로 밀리지 않도록 한다.
- `prefers-reduced-motion`에서 비필수 진입·hover 이동을 줄이되 상태 피드백은 즉시 전달한다.
- 메뉴·알림·빠른 행동의 접근 가능한 이름과 보이는 키보드 focus를 보존한다.

## 2. 접근

- CSS와 공용 surface의 motion 계약을 정적 테스트로 먼저 고정한다.
- 기존 shadcn focus 토큰과 Sidebar 내부 스크롤을 재사용하고 필요한 범위만 보완한다.
- 390px와 desktop 렌더를 실제 브라우저에서 확인한다.

## 3. 의존

- 앞 step: step-4 완료
- 선행 조건: 공용 Sidebar·TopBar·PageHeader 통합

## 4. 완료 기준

- [x] reduced-motion에서 비필수 animation·transition·smooth scroll이 즉시 전환된다.
- [x] 공용 카드와 홈 빠른 행동에 hover 이동 변형이 없다.
- [x] 모바일 메뉴 내부 스크롤·닫기와 키보드 focus 계약이 유지된다.

## 5. 작업 기록

- 전역 reduced-motion media query로 진입·전환을 즉시 전환하되 상태 텍스트는 유지했다.
- 공용 카드와 홈 빠른 행동에서 hover translate를 제거하고 색·border·shadow 피드백만 남겼다.
- Sidebar 링크 라벨의 `truncate`를 제거하고 여러 줄 표시와 내부 세로 스크롤을 유지했다.

## 6. 검증

| 무엇 | 어떻게 확인 | 결과 |
|---|---|---|
| motion·모바일 계약 Red | 대상 Vitest | exit 1 — reduced-motion 부재, hover 이동, 라벨 truncate 3건 확인 |
| motion·모바일 계약 Green | 대상 Vitest 4개 | exit 0 — 4 files, 8 tests passed |
| 실제 desktop/mobile 캡처 | `verify-web` | 미실행 — 현재 세션에 브라우저 연결이 로드되지 않아 Step 6의 증거 누락으로 기록 |

## 7. 결과

- 한 일: reduced-motion, 비필수 hover 이동 제거, 긴 모바일 메뉴 라벨 보존
- 변경 파일 / PR: `src/app/styles/index.css`, `motion-contract.test.ts`, `shared/ui/index.tsx`, `Sidebar.tsx`, `HomePage.tsx`

## 8. 발견한 문제

| 문제 | 어떻게 처리 |
|---|---|
| 브라우저 연결 부재 | 캡처를 대체 도구로 위장하지 않고 Step 6의 미확인 시각 증거로 이관 |
