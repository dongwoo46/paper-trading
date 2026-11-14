# 레포 규칙 8차 수정 — PR #50 1회차 리뷰 지적 C-1·C-2·C-3 반영 증거 (2026-11-15)

대상: `rules/backend-api.md`(계층과 포트 절 머리, PTB7·PTB9), `rules/data-access.md`(PTD4), `rules/async-external.md`(PTA2), 시험 `ArchitectureRulesTest`·`ApplicationLayerRules`·`ApplicationLayerRulesTest`(trading-api), 증거 문서 1차(팀 규칙 대조 표).

출처: PR #50 1회차 독립 리뷰(2026-11-15, head 76938ce, should-fix 3건 C-1·C-2·C-3, nit 4건 C-4~C-7)와 사람 결정 2026-11-15. 이번에 승인된 범위는 C-1·C-2·C-3 세 건뿐이다. C-4 는 PTD4 에 한 구절로 자연스러워 같이 넣었고(아래), C-5·C-6·C-7 은 범위 밖이라 손대지 않았다.

## 사람 결정 기록

1. 세 건 수정(2026-11-15, «3건 고친 뒤 머지»): 리뷰 should-fix 세 건을 이 PR 에서 고친 뒤 머지한다.
2. C-1 `@Async` 허용(2026-11-15, «`@Async` 허용»): 결과를 기록하는 `AFTER_COMMIT` 리스너(application)는 `@Async` 로 별도 스레드에서 돌려 원래 트랜잭션 커넥션을 쥔 채 외부 호출·`REQUIRES_NEW` 를 하지 않게 한다. 리뷰가 낸 두 방향 중 1번(허용 목록에 `@Async` 추가)을 골랐다.

## 지적별 처분 표

| 지적 | 처분 | 고친 곳 | 근거 |
|---|---|---|---|
| C-1 동기 `AFTER_COMMIT` 결과 기록 리스너가 원래 커넥션을 쥔 채 외부 호출과 `REQUIRES_NEW` 를 함, PTB9·시험이 application 의 `@Async` 를 막아 갇힘 | 고침(사람 결정 2번, 허용 확대) | PTB9 지시문·Why, PTB7 마지막 문장, PTD4 지시문·Why, 시험 | PTB9 application Spring 허용 목록에 `org.springframework.scheduling.annotation.Async` 를 `@TransactionalEventListener(phase = AFTER_COMMIT)` 리스너 메서드 한정으로 더했다. PTD4 는 «외부 호출을 하거나 결과를 기록하는 AFTER_COMMIT 리스너는 `@Async("상한 있는 실행기 이름")`(팀 규칙 LA21)로 다른 스레드에서 돌린다»를 적고 Why 의 «PTD2 가 지켜진다»를 «리스너에 `@Transactional` 을 안 건다고 PTD2 가 지켜지지는 않는다, 동기 리스너는 커넥션을 쥔다»로 정정했다. C-4(nit)는 PTD4 에 한 구절로 같이 넣었다(`AFTER_ROLLBACK`·`AFTER_COMPLETION` 리스너가 부르는 쓰기 유스케이스도 `REQUIRES_NEW`). 아래 «시험» 절에 시험 변경과 빨강→초록 증거 |
| C-2 종목 락·단일 소비자는 순서를 지키지 않아 낡은 호가 체결을 막지 못함 | 고침 | PTA2 지시문·Why | 직렬 처리 안에서 이미 처리한 `updatedAt` 보다 새롭지 않은 메시지를 버리는 것을 필수로 적었고 «직렬 처리와 `updatedAt` 비교 둘 다 필요»를 명시했다. Why 에 리뷰의 t=5·t=6 예(락 경쟁으로 t=6 이 먼저 매칭하고 t=5 가 낡은 호가로 체결, 단일 소비자도 `onMessage` 가 여러 스레드면 큐 삽입 순서부터 뒤바뀜)를 적었다. 직렬 처리 안이라 compare-and-set 없이 단순 비교로 충분하다는 리뷰 방향대로다 |
| C-3 계층 절 머리 줄이 바깥 기술을 모두 infrastructure 몫이라 해 PTB7(Redis 리스너 같은 진입점은 presentation)과 어긋남, 클린 아키텍처 선언이 머리 줄과 PTB7 Why 끝 두 곳 | 고침 | 계층과 포트 절 머리 줄, PTB7 Why 끝 | 머리 줄을 «나가는 쪽 바깥 기술(JPA·Spring Data·외부 HTTP·Redis 발행)은 infrastructure 어댑터, 들어오는 진입점(HTTP 컨트롤러·스케줄러·Redis 구독)은 presentation»으로 PTB7 과 맞췄다. PTB7 Why 끝의 «사람 결정 2026-11-15: 클린 아키텍처(포트·어댑터).»는 «선언은 이 절 머리 줄 한 곳에 둔다»로 줄여 선언을 한 곳으로 했다 |

## 시험

`@Async` 허용을 기계로 지키려고 규칙 정의를 `ApplicationLayerRules.kt` 로 빼고(동결 래퍼 없는 본체), `ArchitectureRulesTest` 는 그 본체를 `FreezingArchRule.freeze(...)` 로 감싸게 바꿨다(설명 문구는 같음). 픽스처 클래스는 `src/test/kotlin/com/papertrading/api/application/archfixture`(application 패키지 이름, 빈 아님)에 두고 `ApplicationLayerRulesTest` 가 규칙을 직접 `check` 한다. 본 규칙 시험은 `ImportOption.DoNotIncludeTests` 라 픽스처가 본 분석에 섞이지 않는다.

- 규칙 추가 2건(동결 없음, 현재 코드 위반 0): `asyncOnlyOnAfterCommitListenerMethodsRule`(application 의 `@Async` 메서드는 `@TransactionalEventListener(phase = AFTER_COMMIT)`, phase 생략은 기본값이 `AFTER_COMMIT` 이라 포함), `applicationClassesAreNotAsyncRule`(클래스 수준 `@Async` 는 모든 메서드에 걸려 메서드 조건을 우회하므로 금지). 현재 main 의 `@Async` 는 infrastructure 4곳뿐이다(`grep`).
- 빨강→초록(`./gradlew test --tests '*ArchitectureRulesTest*' --tests '*ApplicationLayerRulesTest*'`): 먼저 허용 규칙을 현행 그대로 둔 채 `AFTER_COMMIT` + `@Async` 픽스처를 검사해 `AFTER_COMMIT 리스너의 Async 는 Spring 타입 허용 목록을 통과한다` 한 건이 `AssertionError`(위반: application→`org.springframework.scheduling.annotation.Async`)로 빨강인 것을 확인했다(19건 중 1건 실패). 허용 규칙에 `Async` 한 줄(`equivalentTo(Async)`)을 더하자 이 시험이 초록이 됐다.
- 계속 막히는 것: `@Scheduled`(Async 외 scheduling 타입), `BEFORE_COMMIT`·`AFTER_ROLLBACK` 리스너의 `@Async`, 리스너가 아닌 메서드의 `@Async`, 클래스 수준 `@Async` 를 각각 `assertThrows<AssertionError>` 로 시험한다.

### 동결 저장소 갱신

허용 규칙의 설명 문구가 바뀌어(`and not equivalent to org.springframework.scheduling.annotation.Async` 추가) 동결 저장소 키가 달라지므로 `StoreUpdateFailedException` 이 났다. 로컬에서만 `-Darchunit.freeze.store.default.allowStoreUpdate=true -Darchunit.freeze.store.default.allowStoreCreation=true` 로 한 번 돌려 갱신했다. 결과는 위반 파일 `5ffdb0db-…` 본문과 같은 내용의 새 파일이 생겼기에, 새 파일을 지우고 `stored.rules` 의 새 키가 기존 `5ffdb0db-…` 를 가리키게 맞췄다(옛 키 줄 제거). `archunit_store` diff 는 `stored.rules` 한 파일의 두 줄(타임스탬프 주석, 해당 규칙 키 문구)뿐이고 위반 목록 파일은 하나도 바뀌지 않았다. 다른 규칙의 키는 그대로다.

## 약화 판정

C-1 은 허용 확대다. `@Async`(`org.springframework.scheduling.annotation`)를 application 허용 목록에 넣었으므로 허용 범위가 넓어졌고, 잠금 원칙(프레임워크는 바깥의 교체 가능한 세부)보다 느슨한 쪽이 하나 더 늘었다. 허용 근거는 두 가지다. 1. 사람 결정 2번(`@Async` 허용). 2. 범위 한정: `AFTER_COMMIT` 리스너 메서드에만 허용하고 다른 메서드·클래스 수준은 시험이 막는다. 같은 변경에서 PTD4 는 강화됐다(`@Async` 필수, `AFTER_ROLLBACK`·`AFTER_COMPLETION` 도 `REQUIRES_NEW`, 비동기 예외는 리스너 안에서 잡아 기록). 따라서 이 PR 은 «추가·강화만» 상시 승인 경로가 아니라 사람 머지가 필요한 변경이다(사람 결정 1번 «3건 고친 뒤 머지»가 머지 승인).
C-2 는 강화다(`updatedAt` 비교를 필수로 승격). C-3 은 머리 줄이 바깥 기술 전부를 infrastructure 에 둔다는 문면을 «나가는 쪽만»으로 좁힌 것이라 허용을 늘리지 않는다(들어오는 진입점은 원래 PTB7 이 presentation 으로 정했다).

## 삭제 줄 대조

`git diff -U0 -- rules` 로 잡힌 삭제 줄 9개(async-external 2, backend-api 5, data-access 2)를 줄마다 판정한다. 조항을 없앤 것은 0개고 모두 같은 자리 줄의 재작성이다. `devkit:rule-verified` 마커 줄은 지우지 않았고 r8 줄을 더했다.

1. PTA2 지시문 옛 줄. 소실: 한정 표현 «compare-and-set 은 낡은 시세를 버리는 데만 쓴다»(필수 규칙으로 승격). 잔존: 상한 있는 실행기, 종목 단위 직렬 처리, 시세 유실 허용, 시세 외 복구 경로, 고가·저가 복구 경로는 새 줄에 그대로 있다.
2. PTA2 Why 옛 줄. 소실: «compare-and-set 은 낡은 값 폐기에 한정한다». 잔존: CAS 가 처리를 직렬화하지 않는다는 설명, 기본 `SimpleAsyncTaskExecutor` 설명, 유실 허용·복구 근거는 모두 남고 락·단일 소비자가 순서를 안 지킨다는 예가 더해졌다.
3. 계층 절 머리 줄 옛 줄. 소실: «JPA·Spring Data·HTTP·Redis 같은 바깥 기술은 infrastructure 어댑터가 맡는다»(HTTP·Redis 를 통째로 infrastructure 몫으로 적어 PTB7 과 어긋남). 잔존: 클린 아키텍처 선언, 의존 방향, 사람 결정 인용, JPA·Spring Data·외부 HTTP·Redis 발행의 infrastructure 자리는 새 줄에 있다.
4. PTB7 지시문 옛 줄. 소실: 없음. 마지막에 `@Async` 문장 하나를 더했다.
5. PTB7 Why 옛 줄. 소실: 끝 문장 «사람 결정 2026-11-15: 클린 아키텍처(포트·어댑터).»(머리 줄과 중복 선언, C-3). 잔존: 머리 줄에 같은 선언과 사람 결정 인용이 있고 이 자리는 머리 줄을 가리킨다.
6. PTB9 지시문 옛 줄. 소실: 없음. 허용 목록에 `Async` 와 시험 설명 두 문장을 더했다.
7. PTB9 Why 옛 줄. 소실: 없음. `@Async` 허용 근거 두 문장을 더했다.
8. PTD4 지시문 옛 줄. 소실: 없음. `@Async` 지시와 `AFTER_ROLLBACK`·`AFTER_COMPLETION` 구절을 더했다.
9. PTD4 Why 옛 줄. 소실: 없음. 동기 리스너의 커넥션 점유와 `@Async` 근거, 비동기 예외, 롤백·완료 단계 문장을 더했다.

## 팀 규칙 대조

팀 규칙 대조 표(`docs/eval/rule-change/2026-11-15-repo-rules-spring-ddd.md`)의 PTD4·PTB7·PTB9·PTA2 행에 r8 내용을 이어 적었다. 새 모순은 없다. PTD4 의 «상한 있는 실행기»는 팀 규칙 LA21 과 같은 방향이고, PTA2 의 `updatedAt` 필수화는 LA21·LA22 와 같은 방향이다.

## 공식 근거 대조

공식 근거 문서(2026-11-15 내려받아 원문 대조, 모두 HTTP 200):

- Spring Framework `Async` Javadoc, https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/scheduling/annotation/Async.html — «Annotation that marks a method as a candidate for asynchronous execution. Can also be used at the type level, in which case all the type's methods are considered as asynchronous.» 시험이 클래스 수준 `@Async` 를 따로 막는 근거(타입에 붙이면 모든 메서드가 비동기)다.
- Spring Framework 참조 문서 «Asynchronous Listeners», https://docs.spring.io/spring-framework/reference/core/beans/context-introduction.html — «If you want a particular listener to process events asynchronously, you can reuse the regular @Async support.» 그리고 한계: «If an asynchronous event listener throws an Exception, it is not propagated to the caller.» PTD4 Why 의 «`@Async` 로 다른 스레드에서 돌린다»와 «비동기 예외는 리스너 안에서 잡아 기록한다»의 근거다.
- Spring Framework `TransactionalEventListener` Javadoc, https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/transaction/event/TransactionalEventListener.html — «The default phase is TransactionPhase.AFTER_COMMIT.»(시험이 phase 생략 리스너를 `AFTER_COMMIT` 으로 보는 근거), 그리고 경고문 «if the TransactionPhase is set to AFTER_COMMIT (the default), AFTER_ROLLBACK, or AFTER_COMPLETION, the transaction will have been committed or rolled back already, but the transactional resources might still be active and accessible. As a consequence, any data access code triggered at this point will still "participate" in the original transaction, but changes will not be committed to the transactional resource.» PTD4 의 `AFTER_ROLLBACK`·`AFTER_COMPLETION` 구절(C-4)의 근거다.
- 참조 문서 «Transaction-bound Events», https://docs.spring.io/spring-framework/reference/data-access/transaction/event.html — 유효한 phase 가 `BEFORE_COMMIT`, `AFTER_COMMIT`(기본), `AFTER_ROLLBACK`, `AFTER_COMPLETION` 이라는 설명.

확인 못 한 범위: «동기 `AFTER_COMMIT` 리스너가 원래 트랜잭션의 EntityManager·JDBC 커넥션을 쥔 채 돈다»는 리뷰의 Spring 구현 지식 판정(`AbstractPlatformTransactionManager` 의 커밋 → afterCompletion → cleanup 순서, Hibernate 기본 커넥션 보유 모드)이고, 위 Javadoc 은 «자원이 아직 활성»까지만 말한다. 실행으로 재현하지 않았고 이 변경의 시험은 규칙 판정(ArchUnit)만 확인한다. 실행기 상한과 비동기 예외 처리의 실제 코드는 이후 리팩터링 때 잰다. 리뷰의 신뢰도 판정(중간~높음)을 그대로 따른다.

- 이유 보존: 바꾼 조항 전부 `Why:` 줄을 남겼고 새 결정마다 실패 메커니즘을 Why 에 더했다(커넥션 점유와 풀 고갈, 순서 역전의 낡은 호가 체결).
- 강조 서식: 규칙 파일에 굵게·IMPORTANT·CRITICAL·MUST 를 쓰지 않았다.
- 골격(헤딩·불릿): `gt:` 앵커와 한 줄 불릿 조항과 `Why:` 줄 구조(form v2)를 그대로 유지했고 새 줄을 더하지 않고 같은 조항 줄 안에서 고쳤다.
- 분량: 규칙 파일 합계 196줄(`wc -l`)로 200줄 상한 아래다(마커 줄 3개 증가).
