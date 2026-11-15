# 세션 인계 — KIS·토스증권 다중 브로커 1단계(토스 읽기 전용) 구현

> devkit v0.95.0 · 작성 2026-11-15 · 갱신 2026-11-16 · 상태: open
> 🧠 대화 컨텍스트가 살아있는 동안 *메인 세션이 직접* 채운다. **자동 주입되는 내용(CLAUDE.md·rules·훅 주입·메모리)과 파일로 이미 남은 내용은 넣지 않는다** — 다음 세션도 자동으로 받거나 읽을 수 있다. *대화에만 있는* 정보가 우선. 모르면 추측으로 메우지 말고 "미상" + ASK([ask-and-fallback]).
> 📎 **이 일을 구속하는 설계·제안·결정 문서는 주입되지 않는다 — 주입되지 않는 근거 문서는 0절에 가리킨다.** 파일에 있다고 읽히는 게 아니다. 가리키지 않으면 그 조건이 할 일 목록에서 조용히 빠진다.
> 📌 **상태는 3단계** — `open`(대기: 아무도 안 집음) → `in-use(시각 · 세션id)`(어느 세션이 이어받아 진행 중 — 다른 세션은 이 문서를 집지 않는다) → `done(날짜)`(남은 일 전부 끝 → `docs/archive/sessions/` 로 이관). 표시는 손으로 고치지 말고 `session-handoff.js` 의 `take`·`release`·`done` 으로 바꾼다(마커 문법이 곧 잠금이다).

## 0. 근거 문서
- docs/features/running/multi-broker-kis-toss/development-plan.md — 구속: D16·§4 줄 대기 상한 하한을 «설정된 그룹 간격 최댓값 이상»으로 바꿈(9e8a7fd, 재승인 전), D9·D13·D14·D15·D19, §5 8번 수동 확인, §9 경계·멈춤 조건 — 처분: 넘김(§4)
- docs/features/running/multi-broker-kis-toss/feature-spec.md — 구속: §10 완료 기준 7행 대기(시세 행 포함), RULE-05~09 — 처분: 넘김(§2 남음)
- docs/features/running/multi-broker-kis-toss/open-questions.md — 구속: FLAG-10·11·18, FLAG-04·05·07·08·09 가정 유지 — 처분: 넘김(§2 남음)
- docs/research/2026-11-15-toss-open-api.md — 구속: expires_in 크기 미확인(실호출 대조 몫) — 처분: 넘김(§2 남음)
- docs/features/running/multi-broker-kis-toss/steps/step-7.md — 구속: §4 완료 기준 중 CI·리뷰 후속 미완, Status in progress, §8 ASSUME 목록 — 처분: 넘김(§4)
- docs/features/running/multi-broker-kis-toss/review/step7-code-review.md — 구속: 후속 S7-01(429 멈춤 끝 몰림)·S7-02(문자열 "null" 금액)·S7-03(줄 대기 상한 하한)·S7-04(컨트롤러·클라이언트 시험 목 대체)를 이번 step에서 고침(사람 결정 2026-11-16) — 처분: 넘김(§4)
- docs/features/running/multi-broker-kis-toss/review/step6-code-review.md — 구속: S6-03·S6-06·S6-07은 findings 원장 — 처분: 미룸(S6-07은 S7-04와 함께 처리 권장)
- docs/features/running/multi-broker-kis-toss/steps/step-6.md — 구속: 없음 — 처분: 끝(2692c2d, 400d382, PR #54 머지)
- docs/features/running/multi-broker-kis-toss/steps/step-5.md — 구속: 없음 — 처분: 끝(908ac89, PR #54 머지)
- docs/features/running/multi-broker-kis-toss/review/step5-code-review.md — 구속: 없음 — 처분: 끝(2692c2d)
- docs/features/running/multi-broker-kis-toss/review/step4-rereview-1.md — 구속: R1-03~R1-08은 findings 원장 — 처분: 미룸(1단계 끝 정리 때)
- docs/features/running/multi-broker-kis-toss/review/step3-panel.md — 구속: nit 6건 deferred — 처분: 미룸(1단계 끝 정리 때)
- docs/features/running/multi-broker-kis-toss/review/step3-tests-code-review.md — 구속: S3T-01~05 원장 — 처분: 미룸(1단계 끝 정리 때)

## 1. 목표 (무엇을 만들고 있었나)
- GL1: KIS와 토스증권을 함께 쓰는 다중 브로커 연동 — 사람 «토스 연동  + kis 연동해야될거같아 왜냐하면 토스랑 kis 둘다 써야할거같아서» (2026-11-15)
- GL2: 두 브로커를 함께 쓰도록 KIS를 공통 브로커 계층으로 리팩터링 — 사람 «kis랑 토스를 같이써야해서 알맞게 다시 리팩터링해야할거같거든» (2026-11-15)
- GL3: step 7 리뷰 후속 4건을 이번에 고치고 머지 — 사람 «후속 4건도 고치자» (2026-11-16)
- GL4: 토스 실주문은 사람이 다시 지시할 때까지 범위에서 뺌 — 사람 «실주문은 제외할거야 바로 하지않는다 개발이 내가 나중에 실주문 다시 구현하라고 시킬거야» (2026-11-16)
- GL5: 기획·설계 승인과 재승인을 «승인 <코드>» 입력 한 번으로 — 사람 «그냥 사용자가 승인 asdf 이것만해도 승인하는걸로 하자», «설계서가 바뀌면 똑같이 승인 코드로 승인하는걸로» (2026-11-16)

사용자는 KIS 계좌와 토스증권 계좌를 함께 쓰려 한다. 순서는 1단계 토스 읽기 전용(REST), 2단계 KIS 공통 계층 리팩터링, 3단계 토스 웹소켓·실주문. 1단계 step 1~6은 main에 머지됐고(PR #54), step 7(시세 REST)은 리뷰 후속을 고친 뒤 별도 PR로 머지한다.

## 2. 진행 상태 (기억 아닌 파일·git 대조 후 기록)
- **완료(2026-11-16, 세 번째 이어받은 세션):**
  - step 7 문서 `e7b2eeb`, 설계 재승인 `a30bb57`(사람 커밋)
  - step 7 구현 `356b248`(maker): `/api/toss/market/prices·orderbook·candles`, `+` 인코딩 수정, S6-01·02·04·05. 메인 재실행 541건 실패 0, 린트 exit 0, CI success
  - 리뷰어 상한(브랜치당 3회, 10회 도달)으로 사람이 «일부만 머지» → «step 7 후속 PR로 분리» 선택. 새 브랜치 `feature/multi-broker-kis-toss-phase1`(400d382 기준)로 1단계 PR #54 생성, main 합침, 전체 852건 실패 0
  - PR #54 CI trading-web 실패: main에 새로 들어온 프록시 표 시험이 `/api/toss/accounts`를 못 찾음 → 사람 결정으로 `frontend/trading-web/proxy.config.ts`에 `/api/toss` 추가 `0a2f1cc`(프런트 255건 통과). CI 초록, head 일치 확인 뒤 사람 지시 «다했으면 머지»로 머지 `d533981`
  - `feature/multi-broker-kis-toss-market`(step 7)에 main 합침 `d3b8056`, 전체 1049건 실패 0, 린트 exit 0, 푸시함
  - step 7 코드 리뷰 1회차 `review/step7-code-review.md`: 머지 가능, 막는 지적 0, 후속 4. 사람 «후속 4건도 고치자» → 계획서 D16·§4 하한 변경과 state 이력 `9e8a7fd`(푸시함)
  - 벤더 문서 게이트 오판(토스증권을 토스페이먼츠로 판정): findings `5b4721f`, PR #54는 사람 결정으로 `DEVKIT_ALLOW_VENDOR_DOC=1` 1회 우회. 사람 지적 «목록만 비교하는 건 맞지 않다» → devkit 제안 PR BiocomKR/devkit #2508(외부 호출 감지, 서드파티 판별, 처음 보는 업체 자동 조사, 공식 도메인만 사람 1회 확인) 제출, 머지는 사람 몫
- **완료(2026-11-16, 네 번째 이어받은 세션):**
  - 설계 재승인 `ed07dc1`(사람이 별도 터미널에서 실행)
  - S7-01~S7-04 수정 `bea1a82`(maker): 전체 82개 클래스 1073건 실패 0, 린트 exit 0, 범위 밖 변경 0
  - 실주문 제외 결정 state.md 기록 `9cdd1d9`
  - step 7 PR #55 생성(`feature/multi-broker-kis-toss-market` → main, 벤더 게이트 오판은 사람 결정으로 1회 우회)
  - devkit 제안 BiocomKR/devkit#2514(승인 코드로 승인·재승인 통일) 사람 지시로 머지 `94f281c`, 대체된 #2499 닫음. 구현 PR은 devkit 레포 몫이라 배포 전까지 승인은 여전히 별도 터미널
- **진행 중:** PR #55 CI → 초록이면 머지(처음 지시 «step 7 PR 머지 순서로 진행해줘»)
- **남음:**
  - PR #55 머지 확인, 머지 뒤 옛 브랜치·worktree 정리
  - 수동 확인(§5 8번): 사람이 토스 키·허용 IP 준비 뒤 broker-balance 1회, prices 1회 실호출 대조(expires_in, 숫자 표기, `+` 공백 처리, "null" 글자 여부)
  - 1단계 완료 처리(feature-spec §10 표 갱신, done 여부는 사람)
  - 옛 브랜치 정리: `feature/multi-broker-kis-toss`(원래 worktree)는 step 7 커밋 3개가 `-market`에도 있음. step 7 PR 머지 뒤 정리 대상
  - `.claude/stack-profile.json`의 `"qaNumber": "off"`: 사람이 편집기로 넣어야 함(도구 편집 막힘)
  - 기존 자금 결함(LOCAL 시장가 매수 잠금 부족): 열린 PR #53이 다루는 중으로 보임(이 기능과 무관)

## 3. 핵심 결정과 이유
- 시세 시각은 토스 오프셋 원문 그대로(사람 2026-11-15): 미국 일봉이 현지 자정 고정이라 KST로 바꾸면 거래일 뜻이 흐려짐. 기각: KST naive 변환, 일봉만 LocalDate
- S6-04: 리미터 줄 대기 상한을 새 키 `rate-limit-max-queue-wait-millis`로 분리(사람 2026-11-15). 기각: 같은 키에 하한 올리기
- S7-03: 그 키의 하한을 «설정된 그룹 간격 최댓값 이상»으로(사람 2026-11-16 «후속 4건도 고치자»). 이유: 더 작으면 그 그룹 연속 호출이 늘 실패(ORDER_INFO 간격 167이면 잔고 API가 매번 실패)
- 토스 404 stock-not-found는 D15대로 400 `TOSS_REQUEST_REJECTED`(새 결정 아님)
- step 7을 1단계 PR에서 분리(사람 2026-11-16): 강제 푸시 없이 새 브랜치 둘. 기각: 기존 브랜치 되감기, 한 PR로 사람이 리뷰
- 프록시 수정은 `proxy.config.ts` 한 줄(사람 2026-11-16). 기각: 백엔드 경로를 `/api/v1/toss`로 옮기기(D9 변경)
- 벤더 게이트: 이번엔 우회, 근본 수정은 devkit 제안(사람 2026-11-16 방향 A 채택). 기각: 계약 무관으로 수용(사실과 다른 분류), 목록 유지+경고, 외부 호출 전부 차단
- 수익률 퍼센트 통일, accountNo 끝 4자리, FLAG-16·18 등 이전 결정은 step-5·6 문서와 이전 판 그대로
- 커밋은 경로 지정(`git commit -F <msg> -- <paths>`)으로 다른 세션 파일을 뺌

## 4. 다음 첫 행동
이어받기 한 줄: PR #55 머지 확인 → 실호출 대조 → 2단계 착수
- 다음 첫 행동: PR #55 상태 확인(`gh api repos/dongwoo46/paper-trading/pulls/55`). 안 됐으면 CI 초록·head 일치 뒤 머지, 됐으면 main 받고 `feature/multi-broker-kis-toss`·`-market` 브랜치와 worktree 정리(남은 미커밋 0 확인 뒤) · 섬김: GL3
- 수동 확인(§5 8번): 사람이 토스 키·허용 IP 준비 뒤 broker-balance 1회, prices 1회 실호출 대조(expires_in, 숫자 표기, `+` 공백 처리, "null" 글자) · 섬김: GL1
- 2단계(KIS 공통 브로커 계층 리팩터링) 착수: tech-planning으로 step 분해부터. 실주문은 GL4대로 손대지 않음 · 섬김: GL2

## 5. 주의·함정
- 이 인계문서는 원래 worktree `.worktrees/multi-broker-kis-toss`의 미커밋 파일이다. 작업은 `.worktrees/multi-broker-kis-toss-phase1`(지금 `-market` 브랜치 체크아웃)에서 한다. 세션을 그 worktree로 옮기려면 사람 팝업 «그 작업트리로 옮겨 이어가기» 뒤 `session-scope.js rebind "<worktree>/.git"`을 **단독 명령으로** 먼저 실행(다른 명령과 묶으면 다시 막힘)
- 이어받기 직후 편집이 «다른 라이브 세션이 같은 작업트리 사용 중»으로 막히면, 이전 세션 표시가 10분 뒤 만료될 때까지 기다림(`session-presence.js readMarkers`로 경과 확인). rebind로는 안 풀림
- Gradle 시험은 캐시로 건너뛰면 xml이 안 생겨 0건으로 보임 → `--rerun` 필수, 집계는 실행 시작 시각 뒤 xml만. `rm -rf`는 막힘
- 도구 직접 실행이 «스킬이 임자인 도구»로 막힘: step-scaffold는 tech-planning, probe-output은 code-review 스킬 안에서. 리뷰 초안은 `/private/var/folders/.../devkit-probe-out/<세션>/<에이전트>/review.md`를 Read 해서 옮겨도 됨
- 리뷰어 위임문에 작성자 시험 증거와 «전체 재실행 금지»가 없으면 훅이 경고함
- 추천 옵션 설명은 «이유: …»로 시작해야 팝업이 뜸
- 새 worktree·브랜치 만들 때 임자 확인 게이트: 열린 PR 목록(`gh api repos/dongwoo46/paper-trading/pulls?state=open`) 대조 뒤 `DEVKIT_WORK_OWNER_SKIP=1`
- heredoc 안의 python 쓰기는 쓰기 대상 판정 불가로 막힘 → 스크립트 파일을 Write 해서 실행
- 커밋 메시지에 `의도:`·`기존:`·`변경:` 줄 필수, 승인 파일은 AI가 커밋 못 함(사람 `!` 명령)
- 레포 전체 `ktlintFormat` 금지. 기존 시험 단언 변경은 `// devkit:test-change <근거>`
- 원래 worktree의 미커밋 `trading-web-visual-redesign/progress.md`와 제안 폴더 하나는 다른 세션 것, 건드리지 말 것
- 사람은 한글 존댓말 응답을 요구함. 진행 멘트까지 한글로

## 6. 관련 파일·경로
- 작업 worktree: `/Users/dongwoo/Desktop/paper-trading/.worktrees/multi-broker-kis-toss-phase1` (브랜치 `feature/multi-broker-kis-toss-market`, HEAD `9e8a7fd`, 푸시됨)
- 원래 worktree: `/Users/dongwoo/Desktop/paper-trading/.worktrees/multi-broker-kis-toss` (브랜치 `feature/multi-broker-kis-toss`, `356b248`, 이 인계문서가 미커밋으로 있음)
- step 7 코드: `backend/trading-api/src/main/kotlin/com/papertrading/api/` 아래 `application/toss/{TossMarketQueryService,port/TossMarketDataPort}.kt`, `infrastructure/toss/{TossMarketDataClient,TossRateLimiter,TossJson,TossProperties,TossTransportSupport}.kt`, `presentation/controller/TossMarketController.kt`, `common/MaskAllButLastFour.kt`
- 토스 명세 사본(이 세션 scratch, 다음 세션엔 없을 수 있음): `curl -s -o <scratch>/openapi.json https://openapi.tossinvest.com/openapi-docs/latest/openapi.json`
- devkit 제안 번들: `~/.devkit/handoffs/devkit/vendor-detect-external-calls-2026-11-16/`, PR https://github.com/BiocomKR/devkit/pull/2508

## 7. 검증 상태
- `bea1a82`(후속 4건) 메인 실행: trading-api 전체 82개 클래스 1073건 실패 0·오류 0·건너뜀 0(실행 뒤 생긴 xml만, 실행 전 xml 0), 린트 exit 0
- maker 바꾼 클래스: TossJsonTest 33, TossMarketDataClientTest 60, TossPropertiesTest 65, TossRateLimiterTest 27, TossMarketControllerIntegrationTest 19, 실패 0
- devkit#2514 CI 6개 success, head 손대조 일치 뒤 머지
- `d3b8056`(step 7 + 머지된 main) 메인 실행: trading-api 전체 82개 클래스 1049건 실패 0·오류 0·건너뜀 0, 린트 `./gradlew compileKotlin compileTestKotlin ktlintCheck detekt verifyLintBaselinesNotGrown -PlintBaselineRef=origin/main --offline` exit 0, 프록시 표 시험 9건 통과
- `review-verdict step7-code-review.md`: «머지 판정: 가능 — 막는 지적 0건 · 후속 후보 4건»
- `356b248` CI: trading-api, collector-api, trading-web success
- PR #54 head `0a2f1cc` CI 세 개 success, pr-head-sync 일치, 머지 `d533981`
- `e7a4bb7`(1단계 + main): 전체 78개 클래스 852건 실패 0
