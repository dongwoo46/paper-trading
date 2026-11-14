# 실행 상태 — trading-web 전면 재제작 (shadcn/ui)

## 0. 메타데이터

| 항목 | 값 |
|---|---|
| 기능 | `trading-web-shadcn-rebuild` |
| 브랜치 | `feature/trading-web-shadcn-rebuild` |
| 갱신일 | 2026-11-15 |
| 전체 상태 | 기획 승인 완료(2026-11-15) — 다음은 tech-planning |

## 1. 현재 위치

제품 재기획을 확정했고, 그 4장 단계·5장 화면 지도를 바탕으로 화면 지도 v3(`screen-spec-v3.md`)를 썼다. 첫 범위는 0·1단계 화면 7개와 공통 셸이다. feature-spec 을 v3 기준으로 갱신했다. 2026-11-15 사용자가 v3 와 FLAG-03·04·11·12·13 을 모두 추천안대로 승인했다. 고위험 ASK 열림 0건. 구현 코드는 없다.

## 2. 산출물

| 파일 | 내용 |
|---|---|
| `feature-spec.md` | 목적·흐름·상태·규칙·API 요구·완료 기준 |
| `open-questions.md` | 쟁점 10건과 고위험 ASK 추천안 |
| `inputs/screen-spec-v2.md` | 12개 화면 정보 구조 사양(ds-lab 원본 복사본) |
| `inputs/frontend-inventory.md` | 기존 프런트엔드 기능 목록(ds-lab 원본 복사본) |
| `product-replan-draft.md` | 제품 재기획(확정) |
| `screen-spec-v3.md` | 단계별 화면 지도, 0·1단계 화면 상세 |

## 3. 다음 행동

1. ~~옛 `trading-web-visual-redesign` state 에 대체 표기~~ (2026-11-15 완료)
2. 재기획 산출물 커밋(사용자 지시 2026-11-15).
3. ~~tech-planning~~ (2026-11-15 `development-plan.md` in-review, DEC-02~09 사용자 결정 반영·명세 갱신 완료) → 사람이 tech-plan 승인 → Maker 가 step-scaffold → step-1 착수 직전 옛 trading-web 삭제 사람 재확인(DEC-01).

## 3-1. 변경 이력

| 날짜 | 변경 | 이유 |
|---|---|---|
| 2026-11-15 | 사용자 설계 메모를 `inputs/user-design-notes-2026-11-15.md` 로 보존 | 대화에만 있던 입력(도메인 6개, 분석 에이전트 구상, «이때 샀으면», 단계 구상) |
| 2026-11-15 | FLAG-01 corrected, FLAG-02·06 confirmed, FLAG-05 범위 변경 | 사용자 답변 |
| 2026-11-15 | spec 상태를 blocked 로 | 화면 목록이 제품 재기획에 좌우됨 |
| 2026-11-15 | `product-replan-draft.md` 확정, 0단계(토스·KIS 계정 연동, 조회·시세만) 추가 | 사용자 답변·지시 |
| 2026-11-15 | 루트 `README.md` 를 `readme-draft.md` 본문으로 교체(worktree, 미커밋) | 사용자 승인 |
| 2026-11-15 | `screen-spec-v3.md` 작성, feature-spec 을 0·1단계 범위로 갱신, FLAG-05 corrected, FLAG-11·12·13 추가 | 인계 이어받기, 재기획 확정 반영 |
| 2026-11-15 | 기획 승인: v3 «추천안대로 승인», FLAG-03 운영자 이름, FLAG-04 금액은 서버만, FLAG-11 받지 않음, FLAG-12 차트 없음, FLAG-13 메모 넣음 | 사용자 팝업 답 |
| 2026-11-15 | 옛 visual-redesign state 에 대체 표기 | FLAG-02 반영 |
| 2026-11-15 | feature-planning 점검 반영: 딱지 `물어봄→스펙` 정리, 낡은 표기 정리, FLAG-14(초기 상태)·15(대사 확인 처리자) 추가 | 스킬 없이 고친 명세 점검(인계 §4), 사용자 승인 |
| 2026-11-15 | `development-plan.md` draft 작성: FLAG-08 대조(§4-1), 삭제 시점 D2(삭제+골격 한 커밋), 화면·영역별 가능성(🔴 2곳), 사람 결정 DEC-01~09 | tech-planning |
| 2026-11-15 | DEC-02~09 추천안 A 반영: feature-spec(RULE-03 좌표 예외·RULE-12 축소·EDGE-10 숨김·§4·§5·§7·§11), v3(§3~§10, §11 «API 대기» 신설), open-questions(FLAG-08 닫음, FLAG-16~21 추가), development-plan(§0·§1 🔴→🟡·§2 D9·D11·D12·D17·§3·§5·§6·§8-2 결정 열) | 사용자 팝업 답(코디네이터 전달) |

## 4. 막힘·이슈

| 이슈 | 영향 | 후속 |
|---|---|---|
| 전략·비교·AI 연구·사건 분석·백테스트 화면의 백엔드 API 없음 | 범위·순서 | FLAG-01 |
| 옛 `trading-web-visual-redesign` 과 범위 겹침 | 점유·중복 | FLAG-02 |
