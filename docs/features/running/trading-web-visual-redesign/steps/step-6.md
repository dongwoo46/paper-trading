# Step 6 — 전체 검증·리뷰·PR 마감

## 0. 메타데이터

| 항목 | 값 |
|---|---|
| step | 6 |
| 기능 | `trading-web-visual-redesign` |
| 근거 | feature-spec §10, development-plan §6 |
| 상태 | 진행중 |
| 담당 | Codex + independent Tester/Reviewers |

## 1. 목표

- 전체 test·lint·build와 route/API diff를 결정적 증거로 남긴다.
- desktop/mobile 렌더를 실제 브라우저로 확인한다.
- 독립 review findings를 해소하고 Ready PR을 만든다.

## 2. 완료 기준

- [x] 전체 Vitest·lint·build exit 0
- [x] route/API·도메인 계산 변경 없음
- [ ] desktop/mobile 시각 증거 확보
- [x] 독립 code review 필수 finding 0건
- [x] 독립 spec review 제품·계약 finding 0건
- [x] commit·push·Ready PR #41 생성

## 3. 작업 기록

- Full Vitest 34 files / 152 tests, lint, build를 exit 0으로 확정했다.
- 390px mobile drawer와 1024px desktop 경계의 inert/focus/keyboard 회귀를 추가하고 통과시켰다.
- unknown URL, lazy route failure, route error reset을 App 통합 테스트로 검증했다.
- route/API·금융 계산·표시 로직 diff가 없음을 확인했다.
- 독립 code reviewer는 C-01~C-06 해소와 active finding 0으로 LGTM했다.
- 독립 spec reviewer는 제품·계약 gap 0을 확인했고 R-03 visual evidence만 유지했다.
- 로컬 Vite는 `127.0.0.1:5174`에서 기동했으나 브라우저 실행면이 없어 캡처하지 못한 뒤 종료했다.

## 4. 검증

| 무엇 | 결과 |
|---|---|
| Full Vitest | PASS — 34 files, 152 tests |
| 집중 responsive/route | PASS — 4 files, 17 tests, React act warning 없음 |
| ESLint | PASS |
| TypeScript + Vite build | PASS |
| route/API/domain diff | PASS — 변경 없음 |
| code review | LGTM — active finding 0 |
| spec review | 구현 PASS / visual evidence BLOCKED |
| desktop/mobile visual | BLOCKED — browser surface 없음 |

## 5. 잔여 문제

| 문제 | 처리 |
|---|---|
| browser visual evidence 없음 | 미실행으로 명시하고 Ready PR risk에 기록; 연결 가능한 세션에서 후속 캡처 |

## 6. 결과

- 코드·계약·리뷰 마감 완료.
- Ready PR: https://github.com/dongwoo46/paper-trading/pull/41
