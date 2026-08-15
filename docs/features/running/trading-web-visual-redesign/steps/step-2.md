# Step 2 — 앱 셸·그룹 사이드바·유틸리티 TopBar

## 0. 메타데이터

| 항목 | 값 |
|---|---|
| step | 2 |
| 제목 | 앱 셸·그룹 사이드바·유틸리티 TopBar |
| 기능 | trading-web-visual-redesign |
| 근거 | development-plan.md §2, §5 |
| 상태 | 완료 |
| 담당 | Codex |

## 1. 목표 · 범위

- 공용 navigation SSoT로 사이드바를 4개 업무 그룹으로 렌더한다.
- TopBar에서 경쟁하는 페이지 제목을 제거하고 모바일 메뉴·알림 유틸리티만 유지한다.
- App의 기존 route와 unknown redirect를 보존하면서 공용 오류 경계를 콘텐츠 영역에 연결한다.

## 2. 접근

- Sidebar·TopBar·App 행동 테스트를 먼저 추가해 그룹·모바일 닫기·알림 `9+`·유일 h1·unknown redirect를 고정한다.
- 기존 store·route·lazy import를 유지하고 내비게이션 배열과 오류 경계만 통합한다.

## 3. 의존

- 앞 step: step-1 완료
- 선행 조건: navigation SSoT, PageHeader, RouteErrorBoundary

## 4. 완료 기준

- [x] 4개 그룹과 11개 link가 사이드바에 표시된다.
- [x] 모바일 링크 선택 시 사이드바가 닫힌다.
- [x] TopBar에 페이지 h1이 없고 알림·메뉴 동작이 유지된다.
- [x] route 렌더 오류가 셸 내부 fallback으로 전환된다.

## 5. 작업 기록

- Sidebar를 navigation SSoT 기반 4개 그룹으로 교체하고 브랜드 텍스트의 h1을 제거했다.
- TopBar에서 페이지 제목을 제거하고 모바일 메뉴·알림 유틸리티만 유지했다.
- App 콘텐츠 폭을 통일하고 route 영역을 `RouteErrorBoundary`로 감쌌다.

## 6. 검증

| 무엇 | 어떻게 확인 | 결과 |
|---|---|---|
| Sidebar·TopBar Red | 대상 Vitest | exit 1 — 그룹 heading·TopBar 제목 제거 요구 실패 확인 |
| Step 1~2 Green | 대상 Vitest 5개 | exit 0 — 5 files, 10 tests passed |

## 7. 결과

- 한 일: 그룹 내비게이션, 유틸리티 TopBar, 셸 내부 오류 경계와 콘텐츠 폭 통합
- 변경 파일 / PR: `App.tsx`, `Sidebar.tsx`, `Sidebar.test.tsx`, `TopBar.tsx`, `TopBar.test.tsx`

## 8. 발견한 문제

| 문제 | 어떻게 처리 |
|---|---|
| 없음 | - |
