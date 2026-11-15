# 레포 규칙 7차 수정 — 3회차 재리뷰 지적 R3-1·R3-2·R3-4 반영 증거 (2026-11-15)

대상: `rules/backend-api.md`(PTB1·PTB7·PTB9, 계층과 포트 절 머리), `rules/data-access.md`(PTD1·PTD4), `rules/async-external.md`(PTA2), 증거 문서 1차(팀 규칙 대조 표).

출처: PR #48 수정 커밋 6c4a839..bfcb0aa 에 대한 독립 재리뷰 2회차(3회차, 2026-11-15, 지적 R3-1~R3-9)와 사람 결정 2026-11-15. 이번에 승인된 범위는 R3-1·R3-2·R3-4 세 건과, R3-4 처리 중 추가된 PTB1 본문 확대 지시다. R3-3·R3-5~R3-9 는 이번 범위 밖이라 손대지 않았다.

## 사람 결정 기록

1. AFTER_COMMIT 리스너 자리(2026-11-15, «호출 대상으로 나눔»): 유스케이스를 부르거나 결과 기록 트랜잭션을 여는 리스너는 application, 어댑터만 부르는 알림·SSE 리스너는 infrastructure. 이번 수정은 이 결정을 그대로 유지하고, «결과 기록 트랜잭션을 연다»를 리스너가 `@Transactional` 을 거는 것이 아니라 `REQUIRES_NEW` 를 선언한 유스케이스를 부르는 것으로 정리했다(R3-1).
2. PTB1 예외(2026-11-15, «예외 문장을 시험에 맞춤»): R3-4 의 PTB1 Exception 과 PR #49 `ArchitectureRulesTest` 의 충돌을 규칙 문장을 시험에 맞춰 푼다.
3. 클린 아키텍처(2026-11-15, 사람 말 «paper-trading은 클린아키텍쳐로 만들건데», 조율 쪽이 전달): R3-4 는 클린 아키텍처 기준으로 쓴다. JPA 엔티티의 자리에는 예외가 없다. 단순 CRUD 컨텍스트도 JPA 엔티티는 infrastructure 영속 어댑터에만 두고 domain 은 JPA·Spring·Hibernate 없이 둔다. PTB1 Exception 에 남는 것은 «단순 CRUD 는 규칙 많은 도메인 모델(불변식·상태 전이 메서드)을 강요하지 않고 얇은 도메인 모델을 허용한다»뿐이다.
4. PTB1 본문 확대(2026-11-15, 사람 말 «규칙도 바꿔야지», 조율 쪽이 전달): 본문도 클린 아키텍처 기준으로 바꾼다. «금액을 바꾸거나 상태 필드에 전이 규칙이 있는 애그리거트는»을 «모든 도메인 모델은 순수 Kotlin»으로 넓히고, 클린 아키텍처(포트·어댑터) 선언은 PTB7 Why 끝에서 계층과 포트 절 머리로 올려 한 줄로 드러낸다. Why 에 domain 의 JPA 사용 파일 실측을 적고, 기존 위반은 ArchUnit 동결이 이미 잡고 있다고 쓴다.

## 지적별 처분 표

| 지적 | 처분 | 고친 곳 | 근거 |
|---|---|---|---|
| R3-1 AFTER_COMMIT 결과 기록 트랜잭션 자리가 PTB7·PTD4 와 PTD1·PTB9 사이에서 어긋남 | 고침 | PTD4 지시문·Why, PTB7 마지막 문장, PTB9 지시문, PTD1 지시문 | AFTER_COMMIT 리스너 메서드 자신에는 `@Transactional` 을 걸지 않는다(PTD1·PTB7·PTB9·PTD4 네 곳에 같은 말). 결과 기록은 `propagation = REQUIRES_NEW` 를 선언한 유스케이스 메서드를 불러서 하고, 리스너가 부르는 쓰기 유스케이스는 `REQUIRES_NEW` 다. PTD4 Why 에 함정 두 가지를 적었다. 1. 기본 `REQUIRED` 유스케이스를 부르면 이미 커밋된 트랜잭션 자원에 참여만 하고 커밋이 뒤따르지 않아 쓰기가 오류 없이 사라진다. 2. 리스너 메서드에 `@Transactional(REQUIRES_NEW)` 를 걸면 리스너 안의 외부 호출까지 트랜잭션에 들어가 PTD2 가 깨진다. 이렇게 하면 PTD1·PTB9(`@Transactional` 은 유스케이스에만)와 PTD2(트랜잭션 안 외부 호출 금지)가 모두 지켜진다. 리뷰어는 «실행 재현은 안 했다»고 했고 이번에도 실행 재현은 하지 않았다. 대신 Spring 공식 Javadoc 원문을 내려받아 대조했다(아래 «공식 근거 대조») |
| R3-2 compare-and-set 문장이 같은 종목 동시 매칭을 막는다고 주장함 | 고침 | PTA2 지시문·Why | 두 역할을 갈랐다. 동시 매칭은 종목 단위 직렬 처리(종목 키 단일 소비자 또는 종목 락)로 막고, `updatedAt` compare-and-set 은 낡은 시세 폐기에만 쓴다. Why 에 «CAS 는 받아들일 메시지를 고를 뿐 처리를 직렬화하지 않아 앞 메시지가 매칭 중일 때 더 새 메시지도 통과한다»를 적었다. 이중 체결 자체를 막는 것은 PTD3(낙관적 락)이라는 r6 의 서술은 그대로다 |
| R3-4 PTB1 Exception 이 PR #49 시험의 domain JPA 금지·PTB7 application→infrastructure 금지와 충돌 | 고침(사람 결정) | PTB1 지시문·Why·Exception | 사람 결정 2번(시험에 맞춤)과 3번(클린 아키텍처)대로 Exception 을 «JPA 엔티티를 그대로 쓴다»에서 «불변식·전이 메서드 없는 얇은 도메인 모델을 허용한다»로 바꿨다. JPA 엔티티를 infrastructure 밖에 두는 예외는 없다고 명시했다. 시험 `domainDoesNotDependOnJpaOrSpring`(`ArchitectureRulesTest.kt:31-41`)은 domain 전체에 `jakarta.persistence`·`org.hibernate`·`org.springframework` 를 막고 PTB7 은 application→infrastructure 를 막으므로, 개정 문장대로 만든 새 CRUD 컨텍스트(얇은 domain 모델 + infrastructure 엔티티)는 시험이 빨개지지 않는다 |
| 사람 결정 4번 PTB1 본문 확대 | 고침(강화) | PTB1 지시문·Why, 계층과 포트 절 머리 | 적용 범위 «금액·전이 규칙 애그리거트»를 «모든 도메인 모델»로 넓혔다. 계층과 포트 절 머리에 «trading-api 는 클린 아키텍처(포트·어댑터)다»를 한 줄로 적었다. 실측 2026-11-15(`backend/trading-api/src/main`, `grep`): domain Kotlin 60파일 중 28파일(전부 `domain.entity`)이 `jakarta.persistence` 를 import(`@Entity` 26파일), 그중 9파일이 `org.hibernate` 도 import, `org.springframework` import 0파일. 28파일은 기존 위반이라 이전 대상이다. PR #49 ArchUnit 동결 저장소(`src/test/resources/archunit_store`)에서 domain 의 JPA·Hibernate·Spring 금지 규칙의 동결 항목이 28클래스를 담고 있음을 확인했다(`jakarta.persistence`·`org.hibernate` 문자열 포함 항목 28클래스) |

## 약화 판정

세 건 모두 약화가 아니다. PTB1 본문 확대는 강화다(금액·전이 규칙이 없는 컨텍스트의 domain 도 JPA·Hibernate·Spring 을 import 하지 못하게 된다). PTB1 Exception 은 «JPA 엔티티를 그대로 씀»이라는 허용에서 «얇은 도메인 모델 허용»으로 좁아졌다. R3-1 은 `@Transactional` 을 둘 수 있는 자리를 리스너 메서드에서 유스케이스 메서드로 좁혔다. R3-2 는 CAS 가 막는다고 잘못 주장하던 보장을 직렬 처리로 옮겼고, 직렬 처리는 이전 문면에도 선택지로 있었으므로 새로 줄어든 보장은 없다. 이 변경은 PTB1·PTD4 문면을 줄이는 쪽이 아니라 늘리는 쪽이다. 허용을 넓힌 곳이 없어 잠금 원칙과 어긋나는 늘어남도 없다.

## 삭제 줄 대조

`git diff -U0 -- rules` 로 잡힌 삭제 줄 10개를 줄마다 판정한다. 조항을 없앤 것은 0개고 모두 같은 자리 줄의 재작성이다.

1. PTA2 지시문 옛 줄. 소실: «이미 처리한 `updatedAt` 과의 비교와 갱신을 종목별 원자 연산으로 해 낡은 값을 버리고 두 메시지가 같은 종목을 동시에 매칭하지 못하게 한다»의 주체 오류(CAS 가 동시 매칭을 막는다는 주장, R3-2). 잔존: 상한 있는 실행기(LA21), 종목 단위 직렬 처리, CAS 로 낡은 값 폐기, 시세 유실 허용, 시세 외 메시지 복구 경로, 매칭·청산 고가·저가 복구 경로는 모두 새 줄에 있다.
2. PTA2 Why 옛 줄. 소실: 없음. 옛 줄 전체가 새 줄에 남고 CAS 한계 문장 하나만 더했다.
3. PTB1 지시문 옛 줄. 소실: 적용 범위의 한정어 «금액을 바꾸거나 상태 필드에 전이 규칙이 있는 애그리거트는»(사람 결정 4번으로 넓힘). 잔존: import 금지 세 패키지, JPA 엔티티와 변환은 영속 어댑터, 리포지토리 포트는 도메인 모델을 주고받는다는 문장은 새 줄에 그대로 있고 «영속 어댑터»를 «infrastructure 영속 어댑터에만»으로 구체화했다.
4. PTB1 Why 옛 줄. 소실: 없음. DD9 형태 설명과 지연 로딩·더티 체킹·프록시 문장, 사람 결정 «DD9 를 따라 분리»는 새 줄에 남았고(분리 결정은 사람 결정 3번으로 포섭) 예외 없음의 근거·실측·동결 문장을 더했다.
5. PTB1 Exception 옛 줄. 소실: «JPA 엔티티를 그대로 쓴다»(사람 결정 2·3번으로 뺌, R3-4). 잔존: 단순 CRUD·설정 성격 컨텍스트가 규칙 많은 도메인 모델을 강요받지 않는다는 취지와 팀 «No over-engineering» 인용은 새 Exception 에 남았다.
6. PTB7 지시문 옛 줄. 소실: 마지막 문장 «커밋 뒤(`AFTER_COMMIT`) 리스너는 유스케이스를 부르거나 결과 기록용 새 트랜잭션을 열면 application 에, 어댑터만 부르는 부수효과면 infrastructure 에 둔다»의 «새 트랜잭션을 연다» 표현(리스너가 직접 연다고 읽힘, R3-1). 잔존: 허용·금지 방향 전부, common 정의, presentation 의 엔티티 사용 금지, 진입점 자리, BEFORE_COMMIT·`@EventListener` 의 application 자리는 그대로이고 AFTER_COMMIT 리스너 자리(호출 대상으로 나눔)도 새 문장에 있다.
7. PTB9 지시문 옛 줄. 소실: 없음. `@Transactional` 을 진입점에 두지 않는다는 문장에 AFTER_COMMIT 리스너 메서드를 더했을 뿐 나머지(허용 목록, 어댑터 변환, flush 위치, 바깥 경계)는 그대로다.
8. PTD1 지시문 옛 줄. 소실: 없음. 외부 진입점 문장에 AFTER_COMMIT 리스너 메서드와 PTD4 참조를 더했을 뿐이다.
9. PTD4 지시문 옛 줄. 소실: «`@Transactional(propagation = REQUIRES_NEW)` 새 짧은 트랜잭션으로 그 결과만 쓰고»(리스너가 직접 건다고 읽힘, R3-1). 잔존: 원자적 쓰기는 트랜잭션 안 리스너, AFTER_COMMIT 은 부수효과만, 결과 기록만 새 트랜잭션, 원본 id 중복 걸러내기, 같은 DB 부수 쓰기 금지는 모두 새 줄에 있다.
10. PTD4 Why 옛 줄. 소실: 없음. 옛 줄의 AFTER_COMMIT 쓰기 소실 설명, DA6 반전 설명, 쓰기 종류 제한 문장이 모두 남고 `REQUIRED` 참여 함정과 리스너 `REQUIRES_NEW` 의 PTD2 위반 문장을 더했다.

## 공식 근거 대조

공식 근거 문서(2026-11-15 내려받아 원문 대조, 모두 HTTP 200):

- Spring Framework `TransactionalEventListener` Javadoc, https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/transaction/event/TransactionalEventListener.html — «if the TransactionPhase is set to AFTER_COMMIT (the default), AFTER_ROLLBACK, or AFTER_COMPLETION, the transaction will have been committed or rolled back already, but the transactional resources might still be active and accessible. As a consequence, any data access code triggered at this point will still "participate" in the original transaction, but changes will not be committed to the transactional resource.» PTD4 Why 의 «기본 전파 유스케이스를 부르면 참여만 하고 커밋이 뒤따르지 않아 쓰기가 사라진다»가 이 문장과 같다.
- Spring Framework `TransactionSynchronization` Javadoc, https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/transaction/support/TransactionSynchronization.html — afterCommit 설명 «Use PROPAGATION_REQUIRES_NEW for any transactional operation that is called from here.» 리스너가 부르는 쓰기 유스케이스를 `REQUIRES_NEW` 로 하라는 PTD4 문장의 근거다.
- Spring Framework `Propagation` Javadoc, https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/transaction/annotation/Propagation.html — REQUIRED «Support a current transaction, create a new one if none exists», REQUIRES_NEW «Create a new transaction, and suspend the current transaction if one exists». 기본 전파가 현재 트랜잭션을 따라가고 `REQUIRES_NEW` 만 새로 만든다는 두 정의가 PTD4 의 `REQUIRED` 대 `REQUIRES_NEW` 구분이다.

devkit 문서 근거: `docs/research/ai-doc-official-guidance.md` §1~§4(devkit 레포 문서, 이번에는 직접 다시 열지 않고 r6 에서 대조한 판정을 이어서 썼다).

- 이유 보존: 바꾼 조항 전부 `Why:` 줄을 남겼고 새 결정마다 실패 메커니즘을 Why 에 더했다(참여 트랜잭션 쓰기 소실, 리스너 `REQUIRES_NEW` 의 PTD2 위반, CAS 비직렬화, domain 28파일 실측).
- 강조 서식: 규칙 파일에 굵게·IMPORTANT·CRITICAL·MUST 를 쓰지 않았다.
- 골격: `gt:` 앵커와 한 줄 불릿과 `Why:` 줄 구조(form v2)를 유지했다. 계층과 포트 절 머리의 선언 한 줄은 인용 블록 한 줄로 앵커 밖에 둔다.
- 분량: 규칙 파일 합계 193줄(`wc -l`)로 200줄 상한 아래다.
- 모순: 바뀐 조항의 팀 규칙 대조 표는 1차 증거 문서에 r7 행으로 갱신했다(PTB1·PTD1·PTD4·PTA2·PTB7·PTB9).

## 검증

- 시험은 이 변경에서 돌릴 대상이 없다(문서만 바뀜). PR #49 `ArchitectureRulesTest` 는 읽어서 문장과 모순 없는지만 대조했고 실행하지 않았다(`chore/trading-api-arch-guard` 브랜치 코드).
- 실측 수치는 이 worktree 의 `backend/trading-api/src/main` 에서 `grep` 으로 셌다(위 표). 동결 저장소 수치는 `chore/trading-api-arch-guard` worktree 의 `archunit_store` 에서 셌다.
- 확인 못 한 범위: R3-1 의 «쓰기 소실»은 실행으로 재현하지 않았고 공식 Javadoc 원문 대조로만 확인했다. 개정 문장을 따른 코드가 실제로 시험·런타임에서 통과하는지는 이후 리팩터링 때 잰다.
