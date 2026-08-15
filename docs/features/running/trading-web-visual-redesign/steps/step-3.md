# Step 3 — 작업 중심 홈

## 0. 메타데이터

| 항목 | 값 |
|---|---|
| step | 3 |
| 제목 | 작업 중심 홈 |
| 기능 | trading-web-visual-redesign |
| 근거 | feature-spec RULE-02·04, development-plan §2 |
| 상태 | 완료 |
| 담당 | Codex |

## 1. 목표 · 범위

- 홈을 가짜 KPI·운영 상태 없이 실제 10개 하위 기능으로 진입하는 작업 허브로 바꾼다.
- 공용 navigation SSoT를 재사용해 Sidebar와 Home의 경로·라벨 drift를 막는다.

## 2. 접근

- HomePage 행동 테스트로 h1·10개 목적지·가짜 상태 부재를 먼저 고정한다.
- PageHeader, shadcn Card/Button, navigationGroups를 합성해 핵심 작업과 전체 영역을 표시한다.

## 3. 의존

- 앞 step: step-2 완료
- 선행 조건: navigation SSoT, PageHeader

## 4. 완료 기준

- [x] 홈에 공통 h1과 설명이 있다.
- [x] 10개 하위 route로 이동하는 링크가 있다.
- [x] 가짜 KPI·시스템 준비 상태가 없다.

## 5. 작업 기록

- 기존 가짜 `통합 모니터링`과 `시스템 운영 준비 완료` 카드를 제거했다.
- navigationGroups에서 실제 10개 하위 route를 렌더하는 4개 업무 카드로 교체했다.
- `PageHeader`로 홈의 유일한 h1과 목적 설명을 제공했다.

## 6. 검증

| 무엇 | 어떻게 확인 | 결과 |
|---|---|---|
| HomePage Red | 대상 Vitest | exit 1 — 기존 h2·3개 링크·가짜 상태 확인 |
| HomePage Green | `npm test -- --run src/pages/home/ui/HomePage.test.tsx` | exit 0 — 1 file, 2 tests passed |

## 7. 결과

- 한 일: 작업 중심 홈과 모든 실제 목적지 진입점 구현, 가짜 운영 상태 제거
- 변경 파일 / PR: `HomePage.tsx`, `HomePage.test.tsx`

## 8. 발견한 문제

| 문제 | 어떻게 처리 |
|---|---|
| 없음 | - |
