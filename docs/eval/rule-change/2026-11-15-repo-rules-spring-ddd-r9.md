# 레포 규칙 9차 수정 — PR #50 2회차 재리뷰 지적 C2-1·C2-2·C2-3 반영 증거 (2026-11-15)

대상: `rules/data-access.md`(PTD4), 시험 `ArchitectureRulesTest`(trading-api), 주석 3곳, 증거 문서 r8·1차(팀 규칙 대조 표).

출처: PR #50 2회차 독립 재리뷰(2026-11-15, head d5665fa, 지적 C2-1 blocker·C2-2 should-fix·C2-3 nit)와 사람 결정 2026-11-15.

## 사람 결정 기록

1. C2-2 구절 빼고 후속(2026-11-15, «구절 빼고 후속»): r8 이 PTD4 에 접어 넣은 C-4(`AFTER_ROLLBACK`·`AFTER_COMPLETION` 리스너가 부르는 쓰기 유스케이스도 `REQUIRES_NEW`)를 이번 PR 에서 뺀다. 이유는 PTB9 와 시험이 `@Async` 를 `AFTER_COMMIT` 리스너에만 허용하는데 PTD4 가 롤백 뒤 리스너에도 `REQUIRES_NEW` 를 요구하면 «`REQUIRES_NEW` 를 쓰라»와 «그 해결책인 `@Async` 는 금지»가 겹치기 때문이다. `@Async` 허용을 롤백·완료 뒤 단계까지 넓힐지는 새 사람 결정이 필요해 후속으로 넘긴다.

## 지적별 처분 표

| 지적 | 처분 | 고친 곳 | 근거 |
|---|---|---|---|
| C2-1 `@Async` 한정 규칙 2건이 본 분석에 등록되지 않아 CI 가 application 전체 `@Async` 를 통과시킴(가드 회귀) | 고침 | 시험 `ArchitectureRulesTest`, 근본 원인 `docs/bug-reports/pr50-async-rules-not-registered.root-cause.md` | `asyncOnlyOnAfterCommitListenerMethodsRule()`(`.allowEmptyShould(true)`, application 에 `@Async` 메서드가 아직 없어 빈 대상 허용)와 `applicationClassesAreNotAsyncRule()` 을 동결 없이 `@ArchTest @JvmField` 로 등록했다(위반 0, 잠금 설정상 새 키 동결은 `StoreUpdateFailedException`). 아래 «시험» 절에 전 초록·후 빨강 증거 |
| C2-2 PTD4 의 C-4 구절이 PTB9·시험(`@Async` 는 `AFTER_COMMIT` 에만)과 겹쳐 롤백 감사 리스너가 갇힘 | 고침(사람 결정 1번, 구절 뺌) | PTD4 지시문·Why, r8, 1차 대조 표 PTD4 행 | PTD4 지시문에서 «`AFTER_ROLLBACK`·`AFTER_COMPLETION` 리스너가 부르는 쓰기 유스케이스도 같다» 구절을, Why 에서 «`AFTER_ROLLBACK`·`AFTER_COMPLETION` 도 Javadoc 이 … 같은 `REQUIRES_NEW` 를 요구한다» 문장을 뺐다. r8 에서 C-4 를 반영했다는 서술 6곳(출처 줄, C-1 처분 행, 약화 판정, 삭제 줄 대조 8번, 같은 대조 9번, 공식 근거 Javadoc 항목)과 1차 표 PTD4 행의 같은 구절을 «후속으로 넘김»으로 맞췄다. C-4 의 원문제(롤백 뒤 실패 감사 기록 소실)는 후속으로 남는다 |
| C2-3 새 주석 3곳이 종결어미 문장 | 고침 | `AsyncListenerFixtures.kt`·`ApplicationLayerRules.kt`·`ApplicationLayerRulesTest.kt` 주석 | 같은 정보를 명사형 한 줄로 바꿨다(동작 변화 없음). 규칙 커밋과 따로 b4c443a 로 냈다 |

## 시험

재현·회귀 확인은 임시 probe 를 `OrderQueryService`(application, src/main)에 넣어 했고 확인 뒤 지웠다(`git diff --stat -- backend/trading-api/src/main` 이 비어 있음).

1. 전(수정 전, 일반 메서드 probe `@Async fun probe() = Unit`): `./gradlew test --tests '*ArchitectureRulesTest*'` 가 BUILD SUCCESSFUL(초록). 리뷰가 말한 가드 회귀가 재현됐다.
2. 등록 후, 일반 메서드 probe 가 있는 채로: 15건 중 1건 실패(`asyncOnlyOnAfterCommitListenerMethods`, `AssertionError at ArchRule.java:94`), 빨강.
3. 등록 후, 클래스 수준 `@Async` probe(probe 를 클래스 어노테이션으로 교체): 15건 중 1건 실패(`applicationClassesAreNotAsync`), 빨강.
4. probe 제거 뒤: `./gradlew test --tests '*ArchitectureRulesTest*' --tests '*ApplicationLayerRulesTest*'` BUILD SUCCESSFUL. `@ArchTest` 는 13개에서 15개가 됐다.

r8 의 «다른 메서드·클래스 수준은 시험이 막는다»·«규칙 추가 2건, 현재 코드 위반 0»·PTB9 문면·1차 표의 «기계로 막는다»는 이 등록으로 사실과 맞게 됐다(문면은 건드리지 않았다).

## 약화 판정

C2-1 은 가드 복구라 강화다. C2-2 는 r8 이 더한 PTD4 강화 한 구절을 거둔 것이다. 이 구절은 r8(d5665fa)에서 처음 들어왔고 PR 기준선(c791289)의 PTD4 에는 없었으므로 기준선 대비 줄어든 보장은 없다. 허용을 넓힌 곳은 없다. 이 PR 이 사람 머지가 필요한 변경이라는 r8 결론은 그대로다(`@Async` 허용 확대, 사람 결정 «`@Async` 허용»). C2-3 은 주석 표기다.

## 삭제 줄 대조

`git diff -U0 -- rules` 로 잡힌 삭제 줄 2개(data-access 2)를 판정한다. 둘 다 같은 자리 줄의 재작성이고 조항을 없앤 것은 0개다.

1. PTD4 지시문 옛 줄(r8 판). 소실: «`AFTER_ROLLBACK`·`AFTER_COMPLETION` 리스너가 부르는 쓰기 유스케이스도 같다»(C-4 구절, 사람 결정 1번으로 뺌, 후속). 잔존: 트랜잭션 안 리스너에 원자적 쓰기, `AFTER_COMMIT` 은 부수효과만, 리스너 메서드에 `@Transactional` 금지, `@Async("상한 있는 실행기 이름")`(LA21), 결과 기록은 `REQUIRES_NEW` 유스케이스, 원본 id 로 중복 거름, 리스너가 부르는 쓰기 유스케이스는 `REQUIRED` 가 아니라 `REQUIRES_NEW`, 같은 DB 부수 쓰기를 `AFTER_COMMIT` 에 두지 않는다는 문장은 새 줄에 그대로 있다. 소실한 구절은 PR 기준선에 없던 r8 추가분이다.
2. PTD4 Why 옛 줄(r8 판). 소실: «`AFTER_ROLLBACK`·`AFTER_COMPLETION` 도 Javadoc 이 AFTER_COMMIT 과 같은 «원래 트랜잭션에 참여하지만 커밋되지 않는다» 경고를 달아 같은 `REQUIRES_NEW` 를 요구한다»(같은 C-4 근거 문장). 잔존: `AFTER_COMMIT` 참여 함정과 `REQUIRES_NEW` 근거, 리스너 메서드에 `@Transactional(REQUIRES_NEW)` 를 걸면 PTD2 가 깨진다는 문장, 동기 리스너의 커넥션 점유와 `@Async` 근거, 비동기 예외 문장, DA6 반전 설명, 쓰기 종류 제한 문장은 새 줄에 그대로 있다.

`devkit:rule-verified` 마커 줄은 지우지 않았고 r9 줄을 더했다.

## 팀 규칙 대조

팀 규칙 대조 표(`docs/eval/rule-change/2026-11-15-repo-rules-spring-ddd.md`)의 PTD4 행에서 C-4 구절을 뺐다. 새 모순은 없다. 나머지 행(PTB9 등)의 «기계로 막는다» 서술은 C2-1 로 사실이 됐다.

## 공식 근거 대조

공식 근거 문서(2026-11-15 다시 내려받아 원문 대조, HTTP 200):

- Spring Framework `TransactionalEventListener` Javadoc, https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/transaction/event/TransactionalEventListener.html — «WARNING: if the TransactionPhase is set to AFTER_COMMIT (the default), AFTER_ROLLBACK, or AFTER_COMPLETION, the transaction will have been committed or rolled back already, but the transactional resources might still be active and accessible. As a consequence, any data access code triggered at this point will still "participate" in the original transaction, but changes will not be committed to the transactional resource.» 이 경고는 `AFTER_ROLLBACK`·`AFTER_COMPLETION` 에도 같아 C-4 의 원문제는 실재한다. 다만 이 PR 의 `@Async` 허용 범위(사람 결정, `AFTER_COMMIT` 한정)로는 그 해법을 규칙에 쓸 수 없어 구절을 후속으로 넘긴다. PTD4 Why 의 `AFTER_COMMIT` 설명은 같은 경고문으로 계속 뒷받침된다.
- 이번 변경의 시험 쪽(ArchUnit 규칙 등록)은 새 외부 주장이 없다. `allowEmptyShould` 는 ArchUnit 1.5.1 에서 컴파일·실행으로 확인했다(위 «시험»).

- 이유 보존: 남은 PTD4 조항은 `Why:` 줄을 그대로 둔다.
- 강조 서식: 규칙 파일에 굵게·IMPORTANT·CRITICAL·MUST 를 쓰지 않았다.
- 골격: `gt:` 앵커와 한 줄 불릿 조항과 `Why:` 줄 구조(form v2)를 유지했다.
- 분량: 규칙 파일 합계는 마커 줄 1개 증가로 200줄 상한 아래다(`wc -l` 로 확인, 아래 «검증»).

## 검증

- `./gradlew test --tests '*ArchitectureRulesTest*' --tests '*ApplicationLayerRulesTest*'`, `./gradlew ktlintCheck --rerun-tasks detekt verifyLintBaselinesNotGrown` 결과는 PR 본문·작업 보고에 적는다.
- 동결 저장소(`archunit_store`)는 바뀌지 않았다(`git diff --stat -- backend/trading-api/src/test/resources` 가 비어 있음).
- 확인 못 한 범위: 롤백 뒤 리스너의 커넥션 점유(C2-2 의 메커니즘)는 구현 지식 판정이고 실행으로 재현하지 않았다. 구절을 빼므로 이 PR 은 그 주장에 기대지 않는다.
