# 실행 상태 — trading-web 전면 재제작 (shadcn/ui)

## 0. 메타데이터

| 항목 | 값 |
|---|---|
| 기능 | `trading-web-shadcn-rebuild` |
| 브랜치 | `feature/trading-web-shadcn-rebuild` |
| 갱신일 | 2026-11-15 |
| 전체 상태 | step-1(교체 골격) 완료 — 다음은 step-2(경계 계층) |

## 1. 현재 위치

제품 재기획과 화면 지도 v3를 확정했고, tech-planning 으로 `development-plan.md` 를 썼다. 실제 API 대조(FLAG-08) 결과 없는 영역은 사용자 결정(DEC-02~09)으로 빼거나 줄였고 명세에 반영해 기획을 재승인했다. 로드맵 7단계를 아래에 두었다. step-1 로 옛 앱을 지우고 shadcn base-nova 골격으로 교체했다(화면·API 클라이언트는 아직 없음).

## 2. 로드맵

| step | 제목 | 상태 | 파일 | 비고 |
|---|---|---|---|---|
| 1 | 교체 골격 | 완료 | steps/step-1.md | 옛 앱 삭제 + shadcn base-nova 골격 한 커밋. build·lint·test 통과 |
| 2 | 경계 계층 | 대기 |  | API 클라이언트·금융 값 유틸, 시험 먼저 |
| 3 | 공통 셸 | 대기 |  |  |
| 4 | 원장 화면 | 대기 |  | 계좌·포지션, 주문 |
| 5 | 조회 화면 | 대기 |  | 거래 내역, 포트폴리오, 홈 |
| 6 | 0단계 화면 | 대기 |  | 브로커 연동, 수집 현황·구독 관리 |
| 7 | 마무리 대조 | 대기 |  | v3·API 대기 목록 대조, 사람 시각 확인 |

## 2-1. 산출물

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
3. ~~tech-planning~~ (2026-11-15 `development-plan.md`, DEC-02~09 반영·명세 갱신·기획 재승인 완료) → ~~로드맵·step-1 골조~~(2026-11-15) → 사람이 tech-plan 승인 → step-1 착수 직전 옛 trading-web 삭제 사람 재확인(DEC-01).

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
| 2026-11-15 | DEC-01 사용자 승인(삭제·골격 진행). step-1 중 D15(a) 일치 1건(`.gitignore`)으로 멈춤 → 사용자 결정: (a) 검사 범위를 `src/` 로 한정(예외 목록 없음, `.gitignore` 유지), (b) CSS 변수 `generatorStandardNames` 42개 분리·금지 65개 승인. step-1 완료 | 생성기 산출물 `.gitignore` 가 옛 파일과 같은 blob, 계획 D15 문구는 «새 src» |

## 4. 막힘·이슈

| 이슈 | 영향 | 후속 |
|---|---|---|
| 전략·비교·AI 연구·사건 분석·백테스트 화면의 백엔드 API 없음 | 범위·순서 | FLAG-01 |
| 옛 `trading-web-visual-redesign` 과 범위 겹침 | 점유·중복 | FLAG-02 |
