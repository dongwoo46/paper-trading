# Step 4: 코드 리뷰 (회귀/접근성/정합성)
Assigned agent: code-reviewer

## Goal
포트폴리오 차트 구현의 위험요소를 식별하고 병합 가능 여부를 판단한다.

## Files to Read
- CODEX.md
- docs/phase/front/portfolio-chart/spec.md
- docs/phase/front/portfolio-chart/step-2.md
- docs/phase/front/portfolio-chart/step-3.md
- step 2~3에서 변경된 파일 전체

## Tasks
- 아래 체크리스트를 중심으로 리뷰한다.
- 회귀:
  - 기존 `/account` 및 공통 라우팅 동작 영향 여부
  - 기존 API 모듈 계약 파손 여부
- 접근성:
  - 차트 컨테이너의 제목/설명 텍스트 제공 여부
  - 키보드 포커스 이동 및 기간 선택 컨트롤 라벨링 여부
  - 색상만으로 의미 전달하지 않는지 여부
- 데이터 정합성:
  - 날짜 정렬/중복 제거 규칙
  - 기준일 수익률 0% 고정 여부
  - 결측/0 division 처리 규칙
- 보안/품질:
  - 하드코딩된 민감정보 없음
  - 타입 안정성(any 남용, nullable 처리 누락) 점검

## Acceptance Criteria
```bash
cd frontend/trading-web
npm run build
```
- MUST FIX/SHOULD FIX/WARNING으로 구분된 리뷰 결과 제출.
- MUST FIX가 0개일 때만 PASS.

## Agent Return Protocol
When you finish, output a completion report in EXACTLY this format so the Orchestrator can evaluate:

---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <one or two sentences of what was done>
- Files modified: <list of paths relative to main repo root>
- Test result: <passed N/N | failed N — list failing cases> (if applicable)
- Blockers: <none | description>
---
