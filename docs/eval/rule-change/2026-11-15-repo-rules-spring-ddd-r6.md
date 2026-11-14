# 레포 규칙 6차 수정 — 2차 재리뷰 지적 반영 증거 (2026-11-15)

대상: `rules/async-external.md`(PTA2), `rules/backend-api.md`(PTB1·PTB4·PTB7·PTB9), `rules/data-access.md`(포인터 signal·PTD1·PTD4), `rules/failure-handling.md`(머리말), 증거 문서 r5·1차.

출처: PR #48 수정 커밋 da861a2..6c4a839 에 대한 독립 재리뷰(2026-11-15, should-fix 8건·nit 2건·기타 1건, 약화 1건)와 사람 결정 2026-11-15. 사람 결정은 두 가지다. 1. 복구 방식(R2): 놓친 구간의 고가·저가(예: 분봉)로 지정가·손절선 도달을 판정하는 재대조이고 데이터 출처는 구현 때 정한다. «최신 가격으로 대조»는 틀린 서술이라 지운다. 2. 리스너 위치(R4): 같은 트랜잭션 안에서 도는 도메인 이벤트 리스너(`@TransactionalEventListener(BEFORE_COMMIT)`, `@EventListener`)는 application 에 두고 PTB9 허용 목록에 `org.springframework.transaction.event`·`org.springframework.context.event` 의 리스너 어노테이션을 더한다. 스케줄러·Redis 리스너 같은 외부 진입점은 presentation 에서 유스케이스만 부르고 `@Transactional` 은 유스케이스에만 둔다.

나머지 값은 이 수정을 맡은 쪽이 정했고 되돌릴 수 있다(보고에 올림). R5 `common` 은 최상위 `com.papertrading.api.common` 만, R6 presentation→domain 허용은 `domain.enums`·`domain.event` 패키지, PTB1 의 domain 금지에 `org.hibernate` 추가, R1 은 옛 일반 조항 복원, R3·R7·R8·R9·R10 은 아래 표의 문장.

## 지적별 처분 표

| 지적 | 처분 | 고친 곳 | 근거 |
|---|---|---|---|
| R1 일반 복구 조항이 사라져 복구 요구가 좁아짐(약화) | 고침 | PTA2 지시문·Why | 옛 문장(`da861a2:rules/async-external.md:21`)의 «돈이나 주문 상태를 바꾸는 처리는 이 통로에만 기대지 않고 원본과 대조하는 복구 경로»를 «시세 외 메시지로 돈·주문·포지션 상태를 바꾸는 Pub/Sub 처리는 …»로 되살렸다. 옛 Why 의 «시세나 신호»와 «체결·잔고는 안 된다»도 되살림. r5 의 «잔존»·«빈틈 없음» 판정은 r5 문서에서 정정 |
| R2 «최신 가격 재대조»가 놓친 틱을 복구하지 못함 | 고침(사람 결정) | PTA2 지시문·Why | `LocalMatchingEngine.kt:46-56` 은 시세 한 점으로만 판정한다. 지정가 매수 100, 놓친 askp1 99, 최신 101 이면 최신 가격 재대조로는 영구히 미체결이라, 놓친 구간의 고가·저가로 도달을 판정한다고 적고 출처는 열어 둠. 그 데이터를 어디서 구할지는 구현 때 정한다 |
| R3 PTB9 가 어디서 잡는지 말하지 않아 PTD3 와 어긋남 | 고침 | PTB9 지시문·Why | «계속 진행하거나 재시도하려면 실패한 트랜잭션 밖(PTD3 바깥 경계)에서 잡는다». Why 에 참여 트랜잭션의 영속 예외는 rollback-only 가 되어 유스케이스 안에서 잡고 계속하면 커밋 때 `UnexpectedRollbackException` 이 난다고 적음(Spring·JPA 문서 지식, 실행 재현은 안 함) |
| R4 PTD1·PTB7·PTB9 가 스케줄러 트랜잭션을 허용하는 것으로 읽히고 트랜잭션 안 리스너를 둘 자리 없음 | 고침(사람 결정) | PTD1, PTD4, PTB7, PTB9 | PTD1 을 «유스케이스 메서드에만 걸고 스케줄러·Redis 리스너 같은 진입점에는 걸지 않는다»로 고침. PTB9 허용 목록에 리스너 어노테이션 추가, PTB7 에 리스너 자리(트랜잭션 안은 application, 외부 진입점은 presentation)를 적고 PTD4 가 그 자리를 가리킴. 현재 리스너 3개(`SlackNotificationEventHandler.kt:21`, `ExecutionSseEventHandler.kt:17` 은 AFTER_COMMIT, `KisExecutionWebSocketManager.kt:141` 은 `ApplicationReadyEvent`)는 모두 infrastructure 이고 트랜잭션 안 리스너는 아직 없다 |
| R5 `common` 이 어느 패키지인지 불명, Why 수치 | 고침 | PTB7 | `common` 은 최상위 `com.papertrading.api.common` 만, `application.common` 은 application 의 일부라 `application.common.result` import 는 port 밖에 든다고 적음. 수치를 다시 셌다: infrastructure→application 18파일 중 port 밖 15파일(`application.common.result` 1파일 포함), 유스케이스 서비스 직접 호출은 `QuoteEventListener`·`KisPaperPollingScheduler`·`LocalMatchingScheduler`·`KisExecutionWebSocketManager` 4파일(`SlackNotificationEventHandler` 는 이벤트 타입만 import). application→infrastructure 28, infrastructure→presentation 2 는 재측정해 그대로 |
| R6 presentation→domain «값 타입»을 판정 못 함 | 고침 | PTB7 | `domain` 은 entity·enums·event·port 네 패키지뿐(`ls`). 허용을 `domain.enums`·`domain.event` 로 적었다. 실측 presentation→domain 14파일 = `domain.entity` 5파일(금지 대상), `domain.event` 1파일(`ExecutionSseEvent.kt:3`), 나머지 `domain.enums`. presentation 이 도메인 이벤트를 받는 DTO 를 두는 것은 허용으로 한 값이다 |
| R7 값 객체 지시가 `val` 에서 컴파일되지 않고 hashCode 가 남음 | 고침 | PTB4 지시문·Why | «private 생성자와 팩토리에서 정규화하거나 `init` 에서 `require` 로 검증, `equals` 를 바꾸면 `hashCode` 도 `stripTrailingZeros()` 기준으로 함께». Why 에 `init` 재대입 불가와 BigDecimal.hashCode 가 scale 을 반영한다는 점을 적음 |
| R8 머지 때 PTF5 를 지우면 이산 날짜 예외도 사라짐 | 고침 | failure-handling 머리말 | «PTF5 의 이산 날짜·연도 닫힌 구간 예외는 제안 판본에 없으면 지우지 않고 남긴다». #2471 의 실제 내용은 네트워크 밖이라 확인하지 못했고 그래서 조건부로 적음 |
| R9 `updatedAt` 비교만으로는 동시 매칭을 못 막음 | 고침 | PTA2 지시문 | «종목 단위 직렬 처리, 또는 처리한 `updatedAt` 과의 비교·갱신을 종목별 원자 연산(compare-and-set)으로 해 낡은 값을 버리고 동시 매칭을 막는다». 이중 체결 자체는 지금 `ExecutionProcessor` 의 낙관적 락(PTD3)이 막는다 |
| R10 Q-import signal 이 안 걸림 | 고침 | data-access 포인터 signal | rule-pointer 는 signal 을 플래그 `i` 하나로 컴파일(`rule-pointer.js:487`)해 `^` 는 파일 맨 앞에만 맞는다. `import com\.papertrading\.api\.domain\.entity\.\w+\.Q[A-Z]\w+` 로 고침. 아래 «검증» 에서 node 로 실제 Q-import 파일 9개에 모두 매칭됨을 확인했고, Q-import 가 없는 main 소스에는 매칭 0(노이즈 0) |
| nit r5:54 `wc -l` 결과 없음, r5:60 rule-pointer 확인이 미래형 | 고침 | r5 문서 | `wc -l` 결과(r5 시점 184줄, r6 시점 188줄)와 rule-pointer 실측 결과를 r5 문서에 적음 |
| PTB1 domain 금지에 `org.hibernate` 추가(정한 값) | 추가 | PTB1 | `domain` 의 기존 엔티티는 `org.hibernate.annotations`·`org.hibernate.type` 을 9파일에서 import 한다. PTB1 은 새 순수 도메인 모델에 적용하므로 기존 엔티티는 이전 대상이지 위반 소급이 아니다 |
| 1차 증거 표 갱신 | 고침 | 1차 증거 문서 | PTB1·PTB4·PTD1·PTD3·PTD4·PTA2·PTF5·PTB7·PTB9 행에 r6 문장을 더함 |

## 약화 판정

R1 은 약화였고(일반 복구 조항 삭제) 이번에 조항을 되살려 해소했다. R2 는 기술적으로 틀린 복구 서술을 놓친 구간 고저 기준으로 바꾼 것이라 팀 LA22(유실 허용은 시세 메시지만)보다 엄격한 쪽이다. R4 의 PTB9 허용 목록 확대는 잠금 원칙(프레임워크는 바깥 세부)보다 느슨한 쪽이 한 가지 늘어난 것이라 사람 결정으로 기록했다(1차 표 PTB9 행). 그 밖은 판정 기준을 채우거나 팀 문면보다 엄격하게 하는 방향이다.

## 삭제 줄 대조

지운 줄은 모두 같은 자리의 줄을 다시 쓴 것이고 조항을 없앤 것은 0개다. `git diff -U0 -- rules` 로 잡힌 삭제 줄 14개를 줄마다 판정한다.

1. PTA2 지시문 옛 줄. 소실: 시세 메시지 처리 문장의 «`updatedAt` 이 낡으면 버린다» 단독 대안과 «주기 작업이 최신 가격으로 … 다시 대조»(기술적으로 틀려 사람 결정으로 뺌, R2). 잔존: 상한 있는 실행기(LA21), 종목 직렬, 시세 유실 허용, 매칭·청산 복구 경로는 새 줄에 있다. 일반 복구 조항은 새 줄에서 되살렸다(R1).
2. PTA2 Why 옛 줄. 소실: 없음. 옛 줄의 «시세나 신호»·«체결·잔고는 안 된다»가 r5 에서 빠졌다가 새 줄에 되살아났고 스레드 무제한·순서 뒤바뀜·LA22·`QuoteEventListener` 문장은 그대로다. 더한 것은 한 점 재대조의 한계 문장뿐이다.
3. PTB1 지시문 옛 줄. 소실: 없음. `org.hibernate` 를 더했을 뿐이고 나머지 문장은 그대로다.
4. PTB7 지시문 옛 줄. 소실: «presentation → domain(enum·값 타입만)»과 모호한 `common` 표기. 잔존: 허용·금지 방향 전부는 새 줄에 있고 판정 불가한 표현만 패키지 이름으로 바꿨다(R5·R6). 외부 진입점 문장은 presentation·application 자리로 구체화했다.
5. PTB7 Why 옛 줄. 소실: «port 밖 import 14파일»·«직접 호출하는 5파일» 수치(틀려서 15·4 로 다시 셈). 잔존: 잠금 원칙·DD14 문장, 28·2 실측, 사람 결정 둘, `enforce` 모두 그대로다.
6. PTB9 지시문 옛 줄. 소실: «application 의 Spring 은 둘뿐» 의 «둘». 잔존: `@Service`·`@Transactional`, 어댑터 변환, saveAndFlush 위치는 그대로이고 리스너 어노테이션이 늘었다(R4).
7. PTB9 Why 옛 줄. 소실: 없음. 3파일 기존 위반, flush 위치 이유, DD9 문장이 모두 남고 리스너 자리와 rollback-only 문장만 더했다.
8. PTB4 지시문 옛 줄. 소실: «`init` 에서 scale 을 정규화하거나 `equals` 를 `compareTo` 기반으로 둔다»(컴파일되지 않고 hashCode 를 남김, R7). 잔존: compareTo 비교, 문자열 생성, divide 명시는 그대로다.
9. PTB4 Why 옛 줄. 소실: 없음. 전부 남고 `init` 재대입 불가와 hashCode 문장만 더했다.
10. data-access 포인터 signal 옛 줄. 소실: 영원히 안 걸리던 `^import .*\.Q[A-Z]\w+`. 잔존: 나머지 신호 전부, 그리고 이 자리를 실제로 걸리는 Q-import 신호가 대신한다(R10).
11. PTD1 지시문 옛 줄. 소실: «Spring 이 부르는 `@EventListener`·`@Scheduled` 메서드에 건다»(PTB9 와 어긋남, R4). 잔존: 프록시 경유 public 메서드, self-invocation·private 무효는 그대로다.
12. PTD1 Why 옛 줄. 소실: 없음. 프록시·self-invocation 문장이 모두 남고 진입점별 경계 분산 문장만 더했다.
13. PTD4 지시문 옛 줄. 소실: 없음. 리스너 자리(application) 괄호만 더했다.
14. failure-handling 머리말 옛 줄. 소실: 없음. PTF1~PTF5 삭제 조건에 PTF5 이산 날짜 예외 유지 문장을 더했다(R8).

## 공식 근거 대조

근거 문서: devkit `docs/research/ai-doc-official-guidance.md` §1~§4.

- 이유(rationale) 보존: 고친 조항 모두 `Why:` 줄을 남기고, 새로 정한 내용마다 실패 메커니즘을 Why 에 더했다(한 점 재대조로 놓치는 사례, 참여 트랜잭션 rollback-only, `init` 재대입 불가와 hash 불일치, 진입점별 트랜잭션 범위 분산). §1 「explaining the why behind constraints」를 따랐다.
- 강조 서식: 다섯 규칙 파일에 굵게·IMPORTANT·CRITICAL·MUST 가 0개다(grep 확인). §2 「piling on emphasis mostly makes Claude over-apply the rule」에 맞춰 평문 명령형으로 썼다.
- 골격(헤딩·불릿): 헤딩 아래 `gt:` 앵커와 불릿 한 줄 조항과 `Why:` 줄 구조(form v2)를 그대로 유지했다. §3 「group related instructions under markdown headers and bullets」.
- 분량 판정: 규칙 파일 합계 188줄로 200줄 상한 아래다(`wc -l`). 새 글은 레포 실측값과 컴파일을 통과하고도 조용히 틀리는 함정에 한했다. §4 「Does Claude really need this explanation?」.
- 모순: §4 「if two instructions contradict each other, Claude may pick one arbitrarily」에 따라 PTB9↔PTD3(R3), PTD1↔PTB7↔PTB9(R4), PTA2 안의 복구 서술(R1·R2)을 해소했다. 바뀐 조항의 팀 규칙 대조는 1차 증거 문서 표에 r6 행으로 갱신했다.

## 검증

- 레포 사실은 이 worktree 의 `backend/trading-api` 소스를 `grep` 으로 다시 확인했다(import 집계, 리스너 위치, `LocalMatchingEngine`·`QuoteEventListener` 줄). 기능 코드는 바꾸지 않았다.
- 바꾼 signal 실측: node 로 `data-access.md` 의 Q-import 대안을 `new RegExp(대안, "i")` 로 컴파일해(rule-pointer 와 같은 방식) backend main 의 Q-import 파일 9개에 돌렸다. 결과 9개 모두 매칭, Q-import 가 없는 파일 매칭 0.
- `rule-pointer --list` 를 `QuoteEventListener.kt`·`TradingJournalRepositoryImpl.kt` 에 돌려 async-external·backend-api·failure-handling·data-access 네 규칙이 걸리는 것을 확인했다.
- 규칙 파일 합계 188줄(`wc -l`).
- 강제 도구(ArchUnit 동결 목록, ktlint, detekt)는 `chore/trading-api-arch-guard` 브랜치의 별도 PR 이 맡는다.
