# Step 6: 문서 정리 및 Phase 완료 처리
Assigned agent: cleanup

## Working Directory
.worktrees/trading-api-trading-journal

## Files to Read
- CODEX.md
- docs/TODO.md
- docs/phase/trading-api/trading-journal/index.json
- docs/phase/trading-api/trading-journal/spec.md
- docs/phase/trading-api/trading-journal/step-2.md
- docs/phase/trading-api/trading-journal/step-3.md
- docs/phase/trading-api/trading-journal/step-4.md
- docs/phase/trading-api/trading-journal/step-5.md

## Tasks
1. `docs/phase/trading-api/trading-journal/index.json` 갱신:
   - 현재 step 상태/결과/재시도 횟수 반영
   - blocker 해소 여부 업데이트
2. 완료 요약 문서 작성:
   - `docs/done/trading-api/trading-journal/trading-journal-summary.md`
   - 구현 범위, 테스트 결과, 잔여 리스크 기록
3. phase 종료 처리:
   - 필요 시 phase 폴더를 `docs/done/...`로 이동
   - `docs/TODO.md`의 trading-journal 체크박스 및 done 날짜 갱신
4. 최종 검증 로그 첨부:
   - compile/test 핵심 결과를 summary에 포함

## Acceptance Criteria
```bash
cd backend/trading-api
./gradlew compileKotlin
./gradlew test --tests "*TradingJournal*"
```

## Agent Return Protocol
---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <one or two sentences of what was done>
- Files modified: <list of paths relative to main repo root>
- Blockers: <none | description>
---
