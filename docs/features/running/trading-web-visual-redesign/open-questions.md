# Open Questions — trading-web visual redesign

> devkit v0.47.0 · 짝 문서: [feature-spec.md](./feature-spec.md)

## 0. 메타데이터

| 항목 | 값 |
|---|---|
| 기능 | trading-web visual redesign |
| 프로젝트 | frontend/trading-web |
| 작성자 | Codex |
| 날짜 | 2026-08-15 |
| 짝 spec | [feature-spec.md](./feature-spec.md) |

## 1. 판정 규칙

- ASK는 비가역·경계·토대 결정이며 사람 확인 전 진행하지 않는다.
- ASSUME은 사소하고 되돌릴 수 있어 기록 후 진행한다.
- 판정 기준은 confidence가 아니라 impact다.

## 2. 점검

- [x] **Core** — 데이터 삭제·외부 발행·인증·핵심 계약 변경 없음.
- [x] **Project** — 금융 계산·주문 상태·API 계약 변경 없음.
- [x] **Feature** — 내비게이션·반응형·접근성·route 오류·가짜 데이터 위험 확인.

## 3. Flags

| id | 카테고리 | 무엇 | mode | 가정 / 질문 | 근거 | impact | 담당 | 상태 | 결정 기록 |
|---|---|---|---|---|---|---|---|---|---|
| FLAG-01 | 화면 | 메뉴 그룹명과 배치 | ASSUME | `개요 / 트레이딩 / 시장 데이터 / 분석`으로 묶고 기존 route 순서는 업무 흐름에 맞춰 재배치 | 사용자가 그룹형 워크스테이션 방향 승인; route 자체는 불변 | 낮음 | 개발 | open | 구현 후 사용자 피드백으로 조정 가능 |
| FLAG-02 | 상태 | route chunk 오류 | ASSUME | 셸을 유지한 오류 안내와 재시도 행동을 제공 | 현재 Suspense만 있고 오류 UI가 없음 | 중간 | 개발 | open | 구현·테스트에서 반영 |

## 4. ASK 추천안

고impact ASK 없음.

## 5. 막힌 것

없음.
