# 세션 인계 — KIS·토스 다중 브로커 2단계(KIS 공통 브로커 계층) 구현

> devkit v0.95.0 · 작성 2026-11-16 · 상태: open
> 🧠 대화 컨텍스트가 살아있는 동안 *메인 세션이 직접* 채운다. **자동 주입되는 내용(CLAUDE.md·rules·훅 주입·메모리)과 파일로 이미 남은 내용은 넣지 않는다** — 다음 세션도 자동으로 받거나 읽을 수 있다. *대화에만 있는* 정보가 우선. 모르면 추측으로 메우지 말고 "미상" + ASK([ask-and-fallback]).
> 📎 **이 일을 구속하는 설계·제안·결정 문서는 주입되지 않는다 — 주입되지 않는 근거 문서는 0절에 가리킨다.** 파일에 있다고 읽히는 게 아니다. 가리키지 않으면 그 조건이 할 일 목록에서 조용히 빠진다.
> 📌 **상태는 3단계** — `open`(대기: 아무도 안 집음) → `in-use(시각 · 세션id)`(어느 세션이 이어받아 진행 중 — 다른 세션은 이 문서를 집지 않는다) → `done(날짜)`(남은 일 전부 끝 → `docs/archive/sessions/` 로 이관). 표시는 손으로 고치지 말고 `session-handoff.js` 의 `take`·`release`·`done` 으로 바꾼다(마커 문법이 곧 잠금이다).

## 0. 근거 문서
- docs/features/running/multi-broker-kis-toss/development-plan.md — 구속: §5 2단계 2~7번(step 9~14), §10 D20~D29, DEC-05~08 모두 A, §9 경계·멈춤 조건(6번은 PR #53 머지 뒤만) — 처분: 넘김(§4)
- docs/features/running/multi-broker-kis-toss/feature-spec.md — 구속: §10 완료 기준 7행 대기(실호출 행 포함), RULE-03·RULE-10 — 처분: 넘김(§2 남음)
- docs/features/running/multi-broker-kis-toss/steps/step-8.md — 구속: §4 «특성화 시험 characterization/kis 는 step 9~14에서 고치지 않음», §5 main 타입 운명 대조표 — 처분: 넘김(§4)
- docs/features/running/multi-broker-kis-toss/review/step8-rereview-1.md — 구속: 후속 R2-01·R2-02는 findings 원장 — 처분: 미룸(1단계·2단계 끝 정리 때)
- docs/features/running/multi-broker-kis-toss/review/step8-code-review.md — 구속: 후속 C-02~C-10은 findings 원장(C-03 ORNO fixture 는 TODO 결함 수정 때 함께) — 처분: 미룸(TODO kis-order-field-names 착수 때)
- docs/features/running/multi-broker-kis-toss/open-questions.md — 구속: FLAG-10·11·18, FLAG-04·05·07·08·09 가정 유지 — 처분: 넘김(§2 남음)
- docs/research/2026-11-15-toss-open-api.md — 구속: expires_in 크기 미확인(실호출 대조 몫) — 처분: 넘김(§2 남음)
- docs/features/running/multi-broker-kis-toss/steps/step-7.md — 구속: §5 8번 수동 실호출 대조 미완 — 처분: 넘김(§2 남음)

## 1. 목표 (무엇을 만들고 있었나)
- GL1: KIS와 토스증권을 함께 쓰는 다중 브로커 연동 — 사람 «토스 연동  + kis 연동해야될거같아 왜냐하면 토스랑 kis 둘다 써야할거같아서» (2026-11-15)
- GL2: 두 브로커를 함께 쓰도록 KIS를 공통 브로커 계층으로 리팩터링 — 사람 «kis랑 토스를 같이써야해서 알맞게 다시 리팩터링해야할거같거든» (2026-11-15)
- GL4: 토스 실주문은 사람이 다시 지시할 때까지 범위에서 뺌 — 사람 «실주문은 제외할거야 바로 하지않는다 개발이 내가 나중에 실주문 다시 구현하라고 시킬거야» (2026-11-16)

1단계(토스 읽기 전용) 코드는 PR #54·#55로 main에 머지됐다(실호출 대조만 남음). 이 세션은 2단계 계획·승인과 step 8(특성화 시험)까지 했다. 이전 인계문서(완료 처리됨, `docs/archive/sessions/2026-11-15-multi-broker-kis-toss.md` in phase1 worktree, main에는 `docs/sessions/2026-11-15-multi-broker-kis-toss.md` 사본)의 GL3(step 7 후속 4건 머지)는 끝났고, GL5(승인 코드 통일)는 devkit 레포 몫이라 여기로 옮기지 않았다.

## 2. 진행 상태 (기억 아닌 파일·git 대조 후 기록)
- **완료(2026-11-16):**
  - PR #55 머지 확인(`263f047`), 머지된 원격 브랜치 7개 삭제(`remote-branch-clean`)
  - 2단계 계획(tech-planner): development-plan §10, §5 2단계 7묶음, DEC-05~08 A, state 로드맵 step 8~14, TODO 2건 — `f42063f`. 설계 승인은 사람이 별도 터미널에서 기록(`tech-plan-approval.md`, 미커밋·사람 몫)
  - step 8 특성화 시험(maker): `91f03b1`, 리뷰 1회차 C-01(체결 통지 시험이 step 13에 바뀔 타입에 묶임) 고침 `338a5ad`, 재리뷰 2회차 머지 가능 `6dd7f94`, 원장 `7b405b5`
  - TODO에 KIS 주문 필드명(ORNO/ODNO) 대조 항목 추가(`338a5ad`)
  - step 9 KIS 에러 정렬(잔고 경로만): `7fb0a4e`, 전체 1095건 실패 0, 리뷰 막는 지적 0(`review/step9-code-review.md`, 후속 S9-01~09 원장)
  - step 10 브로커 선택과 KIS 잔고 이전: `f9c8589`, 전체 1116건 실패 0, 메인 재실행 144건 실패 0, 리뷰 막는 지적 0(`review/step10-code-review.md`, 메인 인라인 리뷰, 후속 S10-01~03 원장)
- **진행 중:** 없음
- **남음:**
  - step 11~14(계획서 §5 2단계 4~7번). step 13은 PR #53 머지 뒤에만
  - 1단계 수동 실호출 대조(사람이 토스 키·허용 IP 준비 뒤 broker-balance 1회, prices 1회)
  - `tech-plan-approval.md` 커밋(사람)
  - 옛 worktree 정리: `.worktrees/multi-broker-kis-toss`(다른 세션 미커밋 2건 있음, 건드리지 말 것), `.worktrees/multi-broker-kis-toss-phase1`(옛 인계문서 정리 뒤)
  - 2단계 끝나면 PR(`feature/multi-broker-kis-toss-phase2` → main)

## 3. 핵심 결정과 이유
- 2단계 범위(사람 2026-11-16): trading-api만. trading-web 화면 제외(다른 세션이 shadcn 재제작 중), 토스 실주문 제외(GL4), 실호출 대조는 선행 조건에서 뺌
- DEC-05 A: RULE-10은 «돈·주문·체결 동작과 성공 응답 불변»으로 판정. 시그니처에 묶인 기존 KIS 단위 시험은 새 포트로 옮김 허용, KIS 에러 응답 502·504→503 변경 허용(AE20 목록)
- DEC-06 A: 400 묶음 매핑(FLAG-11, 31파일 162곳) 또 미룸
- DEC-07 A: KIS 토큰 암호화 이전 미룸. 이유: collector-api가 같은 `kis:token:{mode}`를 평문 공유해 trading-api만 바꾸면 깨짐(🔴). TODO 등록
- DEC-08 A: KIS 주문 기존 결함 둘(취소가 KIS 취소 안 부름, 접수 타임아웃 뒤 우리 주문만 롤백)은 동작 유지, TODO 등록
- ORNO/ODNO(사람 «공식 문서 대조 후 TODO 등록»): 2차 자료는 주문 응답 `ODNO`, 취소 `ORGN_ODNO`. 공식 저장소는 직접 못 봄. 2단계에서는 고치지 않음
- 특성화 시험은 밖 경계(HTTP 입구, KIS 요청, DB 행, 웹소켓 전송 대역)에만 묶음. 이후 step에서 고치지 않음(step-8 §4)
- 리뷰 후속 후보는 원장으로만 넘기고 고침 라운드에 넣지 않음

## 4. 다음 첫 행동
이어받기 한 줄: step 11 주문 공통 포트 → step 12 계좌 출처
- 다음 첫 행동: phase2 worktree(`feature/multi-broker-kis-toss-phase2`, HEAD `f9c8589`)에서 tech-planning 스킬 안에서 `step-scaffold.js multi-broker-kis-toss --spec <14개 제목 spec> --step 11`로 step-11.md 생성, state 로드맵 11행을 in progress로 바꾼 뒤 devkit:maker로 구현(계획서 D26, 주문 경로 D28과 KIS_ORDER_REJECTED, §10.4 395행). step 9 후속 S9-01(연결 거부가 KIS_TIMEOUT)·S9-02(rt_cd 1 한도가 인증으로 가려짐)는 주문 경로 정렬 때 같은 분류를 쓰므로 함께 볼지 사람에게 물음. 끝나면 메인 `--rerun` 확인 → code-review → 커밋 · 섬김: GL2
- step 12, 14 같은 방식. step 13은 PR #53 머지 확인 뒤 · 섬김: GL2
- 실호출 대조는 사람이 준비됐다고 할 때 · 섬김: GL1

## 5. 주의·함정
- 맥 부하가 자주 기준(36)을 넘어 에이전트 착수가 막힘. `concurrency-budget.js wait --weight 1 --reserve`를 백그라운드로 걸고 알림 뒤 다시 띄움. 사람은 «계속 기다림»을 고름
- Docker Desktop이 꺼져 있으면 Testcontainers 시험이 안 돔. `open -a Docker` 뒤 `docker info` 서버 응답 확인(사람이 켜는 것을 허락함)
- Gradle 시험은 `--rerun` 필수, 집계는 실행 뒤 생긴 xml만. 특성화 시험만 돌리기: `./gradlew test --rerun --offline --tests 'com.papertrading.api.characterization.kis.*'`(약 3분)
- 린트: `./gradlew compileKotlin compileTestKotlin ktlintCheck detekt verifyLintBaselinesNotGrown -PlintBaselineRef=origin/main --offline`. 레포 전체 ktlintFormat 금지
- zsh에서 경로들을 변수 하나에 담아 `git add $P` 하면 한 덩어리로 받아 실패함. 경로를 직접 나열
- 리뷰어(code-reviewer)는 샌드박스라 리뷰 파일을 못 씀. `review-draft:` 경로를 `cp`로 `review/step<N>-code-review.md`(재리뷰는 `step<N>-rereview-<k>.md`)에 옮기면 원장 자동 기록. 기록 뒤 생긴 `docs/findings/shards/*.jsonl`도 커밋
- `fix-round-brief.js`는 Kotlin 파일을 못 읽어 주변 자리 목록이 빔. 구조 고침은 리뷰어 지목대로 직접 넘김
- 승인 파일(`tech-plan-approval.md`)은 AI가 읽기(cat)도 Bash로는 막힘. Read 도구로 읽음
- 열린 PR #53(자금 상태)이 main과 충돌 중이고 PR 쪽으로 풀면 토스 매도가 LOCAL처럼 입금될 위험(계획서 RISK). step 13 전에 머지 여부 확인
- 사람은 한글 존댓말 응답을 요구함. 진행 멘트까지 한글로(영어 응답은 훅이 막음)
- 이 브랜치 리뷰어 실행이 상한(3회)을 넘음. 리뷰어를 띄우려면 사람에게 팝업으로 물어 이름표가 정확히 «한 회차 더»인 선택지를 고르게 함(«(추천)»을 붙이면 가드가 인정 안 함). 부하가 높으면 사람이 «메인이 직접» 리뷰를 고를 수 있음(step 10 선례)
- /clear 직후에는 이전 세션의 라이브 마커가 10분 동안 남아 편집이 «다른 세션 사용 중»으로 막힘. 끝난 세션인지 대화기록으로 확인하고 낡을 때까지 기다림(면제 마커 `~/.devkit/worktree-exempt/<세션>` 는 이 가드에 효과 없음)
- step-scaffold spec 은 `scratchpad/steps-spec.json`(이 세션 것, 다음 세션엔 없음). step 9·10 항목이 채워진 꼴을 step-9.md·step-10.md §0~4에서 볼 수 있음

## 6. 관련 파일·경로
- 작업 worktree: `/Users/dongwoo/Desktop/paper-trading/.worktrees/multi-broker-kis-toss-phase2` (브랜치 `feature/multi-broker-kis-toss-phase2`, HEAD `f9c8589`, 푸시 안 함)
- 특성화 시험: `backend/trading-api/src/test/kotlin/com/papertrading/api/characterization/kis/`(잔고 3, 주문 접수 4, 체결 통지 3, 대역 전송기, 이벤트 수집)
- step-scaffold spec 예시(이 세션 scratch, 다음 세션엔 없을 수 있음): `{"based_on":"development-plan.md","steps":[{"title":"기준선 기록"},…14개 제목(state 로드맵 그대로)…]}`에서 해당 step에만 basis·goal·scope·approach·deps·done을 채움
- 승인 명령(사람, 별도 터미널): `node /Users/dongwoo/.claude/plugins/cache/allosta-internal/devkit/0.95.0/tools/feature-approval.js set multi-broker-kis-toss tech-plan --by dongwoo406 --note "<근거>"`

## 7. 검증 상태
- `338a5ad` 메인 실행: 특성화 시험 3+3+4건 실패 0·오류 0·건너뜀 0(실행 뒤 생긴 xml), exit 0
- maker(91f03b1 착수 전 기준선): trading-api 전체 82개 클래스 1073건 실패 0. 린트 exit 0, baseline 증가 0
- `review-verdict step8-rereview-1.md --previous step8-code-review.md`: «머지 판정: 가능 — 막는 지적 0건 · 지난 지적 미해결 0건 · 후속 후보 2건»
- 전체 시험은 step 8 고침 뒤 다시 돌리지 않음(main 소스 변경 0이라 생략)
- step 9: 메인 집중 재실행 8 xml 72건 실패 0, maker 전체 85 xml 1095건 실패 0, 린트 exit 0, `review-verdict step9-code-review.md` «가능 — 막는 지적 0건 · 후속 후보 9건»
- step 10: 메인 집중 재실행 11 xml 144건 실패 0, maker 전체 85 xml 1116건 실패 0, 린트 exit 0, `review-verdict step10-code-review.md` «가능 — 막는 지적 0건 · 후속 후보 3건»
