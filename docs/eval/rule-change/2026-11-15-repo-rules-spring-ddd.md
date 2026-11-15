# 레포 규칙 세 문서 추가 — 변경 증거 (2026-11-15)

대상: `rules/backend-api.md`(PTB1~PTB4), `rules/data-access.md`(PTD1~PTD5), `rules/async-external.md`(PTA1~PTA2). 새 파일 추가만이다.

## 삭제 줄 대조

`rule-change-gate` 판정: 지운 줄 0개(no-net-deletion 통과). 대조할 삭제 줄이 없어 소실 0이다.

## 전후 준수 비교

해당 없음. 지운 줄이 없어 뜻을 잃을 수 없다(rule-change-verify «Additions only = not applicable»). 새 조항이 실제로 지켜지는지는 이후 전수 감사(quality-audit)와 리팩터링 리뷰에서 잰다.

## 팀 규칙과 모순 대조

devkit 0.95.0 `rules/dev-team` 의 원문과 조항마다 대조했다. 판정 도구가 없어 사람이 읽는 대조다.

| 레포 조항 | 맞닿는 팀 조항 | 판정 |
|---|---|---|
| PTB1 도메인/영속 분리 | DD9 | 같은 방향. 초안(엔티티를 애그리거트로 씀)은 DD9 와 반대라 사람 결정으로 분리 쪽으로 고쳤다. 단순 CRUD 예외는 팀 규칙 «No over-engineering» 인용이지 새 예외가 아니다. r6: 도메인 모델 import 금지에 `org.hibernate` 를 더함(DD9 의 ORM 금지를 JPA 규약 패키지만이 아니라 Hibernate 어노테이션까지 넓힌 것이라 엄격한 쪽). r7: 적용 범위를 «금액·전이 규칙 애그리거트»에서 «모든 도메인 모델»로 넓혔고(강화, 사람 결정 «클린 아키텍처, 규칙도 바꿔야지»), 단순 CRUD 예외는 «JPA 엔티티를 그대로 쓴다»에서 «불변식·전이 메서드 없는 얇은 도메인 모델 허용»으로 좁혔다. 팀 «No over-engineering» 이 허용하는 것은 규칙 많은 도메인 모델을 강요하지 않는 것까지이고 ORM 모델을 도메인에 두는 것은 DD9 와 반대라 모순 없음 |
| PTB2 private set | DD10, DD11 | 같은 방향(Kotlin 형태) |
| PTB3 id 참조 | DD12, 제안 중인 DD15(BiocomKR/devkit#2471) | 같은 방향. DD15 머지 시 지운다고 조항에 적음 |
| PTB4 BigDecimal | 루트 CLAUDE.md Financial Safety | 같은 방향. r5: Kotlin `/`(HALF_EVEN, 피제수 scale)와 값 객체 scale 정규화를 더해 좁힘. r6: 값 객체는 `init` 재대입이 컴파일되지 않아 private 생성자와 팩토리 정규화 또는 `require` 검증으로 고치고, `equals` 를 바꾸면 `hashCode` 도 함께 바꾼다고 적음 |
| PTD1, PTD2 | DA1, DA9 | 같은 방향. r5: PTD2 대상을 DA1 문면(열린 트랜잭션 안)에 맞춰 트랜잭션 안 리스너·그 둘이 부르는 어노테이션 없는 메서드까지 넓혔고, PTD1 은 프록시로 호출되는 리스너·`@Scheduled` 를 포함한다고 적음. r6: 이 포함이 PTB7·PTB9 와 어긋나 PTD1 을 «유스케이스 메서드에만 걸고 스케줄러·Redis 리스너 같은 진입점에는 걸지 않는다»로 고침. r7: `AFTER_COMMIT` 리스너 메서드에도 걸지 않는다고 더함(PTD4 와 맞춤). DA1·DA9 는 트랜잭션 안에서 외부 호출 금지라 같은 방향 |
| PTD3 | DA10 | 같은 방향. r6: 변경 없음(바깥 경계에서 잡는다는 PTD3 문장을 PTB9 가 따라가도록 PTB9 를 고침). r5: 영속 예외를 어댑터 안 flush 로 일으켜 바꾸고 재시도는 유스케이스 바깥 경계로 적어 PTB9 와 맞춤(DA10 은 재시도 위치를 말하지 않아 충돌 없음) |
| PTD4 리스너 역할 | DA6, DA7, CS11, DA1, DA9 | 초안(커밋 뒤 리스너에서 REQUIRES_NEW 로 쓰기)이 DA6 문장과 반대로 읽혀 고쳤다. 원자적 쓰기는 트랜잭션 안 리스너(DA6·CS11), 커밋 뒤 리스너는 DA7 부수효과로 나눔. r5: 그래도 «AFTER_COMMIT 에서 DB 를 쓰면 REQUIRES_NEW» 가 쓰기 종류를 한정하지 않아 DA6·CS11 이 막는 커밋하는 동일 DB 핸들러를 열어 줬다(독립 리뷰의 약화 지적). 새 트랜잭션을 그 리스너가 한 외부 부수효과의 결과 기록(DA1·DA9 3단계)에만 허용하고 같은 DB 부수 쓰기는 트랜잭션 안(DA6·CS11)이라고 좁혀 팀 문면과 일치시킴. r6: 트랜잭션 안 리스너의 계층 자리를 application 으로 적음(PTB7·PTB9). r7: r6 까지는 «리스너가 `REQUIRES_NEW` 로 결과를 쓴다»로 읽혀 PTD1·PTB9(`@Transactional` 은 유스케이스에만)와 어긋났다. `AFTER_COMMIT` 리스너 자신에는 `@Transactional` 을 걸지 않고 `REQUIRES_NEW` 를 선언한 유스케이스를 부르게 고쳤다(기본 `REQUIRED` 유스케이스는 이미 커밋된 트랜잭션 자원에 참여만 해 쓰기가 사라지므로 `REQUIRES_NEW`). 결과 기록 새 트랜잭션은 DA1·DA9 3단계에만 허용하는 r5 좁히기는 그대로라 DA6·CS11 과 모순 없음. r8: 동기 `AFTER_COMMIT` 리스너는 원래 트랜잭션 자원이 살아 있는 채 외부 호출 동안 커넥션을 쥐고 `REQUIRES_NEW` 가 커넥션을 더 잡으므로(PR #50 리뷰 C-1), 외부 호출·결과 기록 리스너는 `@Async("상한 있는 실행기")` 로 다른 스레드에서 돌리라고 적었다(LA21 과 같은 방향, 사람 결정 «`@Async` 허용»). 비동기 예외는 호출자에게 전파되지 않아 리스너 안에서 잡아 기록. 모순 없음 |
| PTD5 | 맞닿는 팀 조항 DA23(앱 메모리로 끌어와 처리하지 않기) | 충돌 없음. 컬렉션 fetch join + 페이징은 DA23 이 막는 «DB 밖에서 자르기»의 한 형태이고, `@Modifying` flush·clear 는 팀 조항 없음. r5: `PositionRepository.kt:32` 가 이미 `flushAutomatically = true, clearAutomatically = true` 를 써서 규칙을 그 형태에 맞춤 |
| PTA1 | LA1 | 같은 방향 |
| PTA2 | 제안 중인 EV2, LA21, LA22 | 같은 방향. r5: LA22 는 유실을 허용하는 작업을 인정하므로 «시세 메시지 유실 허용»은 약화가 아니다. 그 메시지가 매칭·청산을 촉발해 복구 대상이기도 한 모순은 «메시지는 유실 허용, 촉발되는 매칭·청산은 주기 재대조로 복구»로 갈랐고(사람 결정), 실행기 상한은 LA21 과 맞춤. r6: r5 는 일반 복구 조항(돈·주문 상태를 바꾸는 처리는 원본 대조 복구)을 지워 LA22 의 «유실 허용은 시세 메시지만» 취지보다 약해졌다. 그 조항을 시세 외 메시지로 되살리고, 시세 복구를 «최신 가격 재대조»가 아니라 놓친 구간의 고가·저가 기준으로 고쳐 기술적 오류를 없앰(사람 결정). 같은 종목 순서·동시성은 직렬 처리 또는 종목별 원자 비교로 적음. r7: compare-and-set 은 낡은 값만 버리고 동시 처리를 막지 못해(앞 메시지가 매칭 중일 때 더 새 메시지도 통과) 두 역할을 갈랐다. 동시 매칭은 종목 단위 직렬 처리(종목 키 단일 소비자 또는 종목 락)로 막고 compare-and-set 은 낡은 시세 폐기에 한정. LA21·LA22 와 같은 방향. r8: 종목 락·단일 소비자는 동시 실행만 막고 순서는 지키지 않아 낡은 호가 체결을 못 막는다(PR #50 리뷰 C-2). 직렬 처리 안에서 이미 처리한 `updatedAt` 보다 새롭지 않은 메시지를 버리는 비교를 필수로 승격(직렬화와 비교 둘 다 필요, 강화) |
| PTB5 생성 시 불변식(2차 추가) | DD10, DD13 | 같은 방향(값 객체·애그리거트 생성 방식) |
| PTT1 실제 엔진 시험(2차 추가) | 팀 tests.md TT7 | TT7 은 프로세스 안(도메인·유스케이스·우리 DB 포함)에는 진짜나 가짜를 쓰고 프로세스 밖만 대역으로 바꾸라는 조항이다. PTT1 은 어댑터 자체의 쿼리·락·TTL 시험에서 가짜를 막는 더 엄격한 좁히기이고, 예외(도메인·유스케이스 단위 시험은 포트 가짜)가 TT7 과 같은 방향이라 모순 없음. r5: Testcontainers 사용 사실은 trading-api 만 맞아 모듈 범위를 적음 |
| PTF1 원장 추가 전용(2차 추가) | 팀 규칙에 없음, CLAUDE.md 감사 추적 | 충돌 없음. devkit 제안으로도 올림 |
| PTF2 대사(2차 추가) | 팀 규칙에 없음 | 충돌 없음. devkit 제안으로도 올림 |
| PTF3 불확정 응답(2차 추가) | DA9 | DA9 의 결과 반영 단계를 강화(좁히기). devkit 제안으로도 올림 |
| PTF4 계산 스냅샷(3차) | 팀 규칙에 없음 | 충돌 없음. devkit 제안 #2471 2차에 포함 |
| PTF5 반열림 구간(3차) | 팀 time.md 에 해당 조항 없음 | 충돌 없음(KST naive 원칙과 독립). devkit 제안 #2471 2차에 포함. r5: 대상을 연속 시각(`Instant`·`LocalDateTime`)으로 좁히고 이산 `LocalDate`·연도 구간은 양끝 포함이 맞다고 적음(`DailyBalanceRepositoryImpl.kt:17`, `TaxSummaryRepositoryImpl.kt:28`, 파생 `...Between` 포함). r6: 머지 때 PTF5 를 지우더라도 이산 날짜·연도 닫힌 구간 예외는 제안에 없으면 남긴다고 머리말에 적음 |
| PTF6 예수금 홀드 요청 경로(3차) | DA10, CS11 | 같은 방향. 큐는 알림에만 써서 CS11(부수효과 통로)과 맞음 |
| PTB6 컬렉션 노출, copy 얕은 복사(3차) | DD10 | 같은 방향(Kotlin 형태). r5: 읽기 전용 타입은 같은 컬렉션의 뷰라 복사본(`toList()`)을 우선으로 고침 |
| PTB7 계층 지도(4차) | backend-api 잠금 원칙(의존은 안쪽으로), DD14, DD9 | 같은 방향. DD14 가 말하는 «계층»을 이 레포 패키지 이름으로 정한 것. «No over-engineering»(계층 수를 복잡도에 맞춤)과는 돈·상태 컨텍스트가 있는 trading-api 에만 적용해 부딪치지 않음. r5: 허용·금지 방향을 빠짐없이 열거(presentation→domain 은 enum·값 타입만, infrastructure→application 은 `port` 패키지만, common 은 domain 과 같이 Spring 금지)해 판정 기준을 채움(사람 결정 «엄격»). 팀 문면보다 엄격한 쪽이라 약화 아님. r6: `common` 은 최상위 `com.papertrading.api.common` 만(`application.common` 은 application 의 일부), presentation→domain 은 `domain.enums`·`domain.event` 패키지만으로 판정 가능하게 적고, 외부 진입점은 presentation·트랜잭션 안 도메인 이벤트 리스너는 application 으로 자리를 정함(사람 결정). 실측 수치는 port 밖 import 15파일·유스케이스 직접 호출 4파일로 다시 셈. r7: 계층과 포트 절 머리에 «클린 아키텍처(포트·어댑터)» 선언을 한 줄로 드러냈다(사람 결정 2026-11-15, 내용 변경 없음). `AFTER_COMMIT` 리스너는 자신에 `@Transactional` 을 걸지 않고, 유스케이스(결과 기록은 `REQUIRES_NEW` 유스케이스)를 부르면 application, 어댑터만 부르는 알림·SSE 면 infrastructure 로 갈랐다(사람 결정 2026-11-15 «호출 대상으로 나눔» 유지). r8: 계층 절 머리 줄이 HTTP·Redis 를 통째로 infrastructure 몫으로 적어 이 조항(Redis 리스너 같은 진입점은 presentation)과 어긋났다(C-3). 머리 줄을 «나가는 쪽 바깥 기술은 infrastructure 어댑터, 들어오는 진입점은 presentation»으로 맞췄고 이 조항 Why 끝의 중복 선언은 머리 줄을 가리키게 줄였다(내용 변경 없음). 결과를 기록하는 `AFTER_COMMIT` 리스너는 `@Async` 로 돌린다는 한 문장을 더함(PTB9·PTD4) |
| PTB8 출력 포트 위치(4차) | DD9 | 같은 방향(유스케이스가 ORM·외부 SDK 를 직접 참조하지 않게 하는 구조). r5: 여러 컨텍스트가 쓰는 포트는 `application/common/port`(사람 결정) |
| PTB9 application 의 Spring 허용 범위(4차) | DD9, 잠금 원칙(프레임워크는 바깥의 교체 가능한 세부) | 같은 방향. DD9 금지 목록은 ORM·HTTP·SDK 라 트랜잭션 어노테이션 허용은 모순 아님(사람 결정). 잠금 원칙의 «프레임워크는 바깥 세부»보다는 `@Service`·`@Transactional` 을 허용하는 만큼 느슨하나, 현재 application 의 Spring 사용(Component 4, ApplicationEventPublisher 2, Page·Pageable 등)을 줄이는 방향이다. 잠금 원칙은 사람만 고치므로 이 허용 범위는 사람 결정으로 둠. r5: 영속 예외를 어댑터 안 flush 로 바꾼다고 적어 PTD3 와 맞춤. r6: application 허용 목록에 도메인 이벤트 리스너 어노테이션(`context.event`·`transaction.event`)을 더하고(사람 결정), 잡는 위치를 «실패한 트랜잭션 밖(PTD3 바깥 경계)»으로 적어 PTD3 와 어긋남을 없앰. 잠금 원칙보다 느슨한 쪽이 한 가지 더 늘었으므로 사람 결정으로 둠. r7: `@Transactional` 을 `AFTER_COMMIT` 리스너 메서드에도 두지 않는다고 더해(좁힘) 허용 목록은 늘리지 않았다. r8: application 허용 목록에 `org.springframework.scheduling.annotation.Async` 를 `@TransactionalEventListener(phase = AFTER_COMMIT)` 리스너 메서드 한정으로 더했다(허용 확대, 사람 결정 «`@Async` 허용», PR #50 리뷰 C-1). 잠금 원칙(프레임워크는 바깥 세부)보다 느슨한 쪽이 하나 더 늘었으므로 사람 결정으로 둔다. 시험 `ApplicationLayerRules` 가 메서드 단위(phase 생략 포함)와 클래스 수준 `@Async` 금지를 기계로 막는다 |

2차 추가 근거: 국내 테크 기업 공개 기술 블로그 조사(토스, 우아한형제들, 카카오페이, 강남언니 등). 요약본 기반이라 인용의 글자 대조는 하지 않았다.

## 공식 근거 대조

근거 문서: devkit `docs/research/ai-doc-official-guidance.md` §1~§4, §7.

- 2차 추가(PTB5, PTT1, PTF1~PTF3) 뒤 조항은 16개, 다섯 파일이며(3차 이후 조항 수는 r3·r4·r5 문서의 판정을 따른다) 아래 판정은 2차 추가분에도 같게 적용했다(Why 줄 16개, 강조 0, 분량 합계 약 140줄).
- 이유(rationale) 보존: 1차 조항 11개 모두 `Why:` 줄을 둔다(파일별 4·5·2개, 조항 수와 같음). §1 「explaining the why behind constraints」와 devkit 실측(근거를 지운 판본 준수 90% → 10%)을 따랐다.
- 강조 서식: 세 파일에 굵게, IMPORTANT, CRITICAL, MUST 가 0개다(grep 확인). §2 「piling on emphasis mostly makes Claude over-apply the rule」에 맞춰 강조 없이 평문 명령형으로 썼다.
- 골격(헤딩·불릿): 파일마다 관심사별 헤딩 아래 `gt:` 앵커와 불릿 한 줄 조항으로 썼다(form v2). §3 「group related instructions under markdown headers and bullets」를 따랐다.
- 분량 판정: 세 파일 23·31·38줄, 합계 92줄로 200줄 상한 아래다. §4 의 「Does Claude really need this explanation?」 판별로, 모델이 이미 아는 일반 설명(트랜잭션 정의 등)은 넣지 않고 컴파일은 통과하지만 조용히 틀리는 함정만 남겼다. 내용을 줄였다기보다 처음부터 함정만 골랐다.
- 모순: §4 「if two instructions contradict each other, Claude may pick one arbitrarily」에 따라 위 표로 팀 규칙과 대조했고, 발견한 2건(PTB1, PTD4)은 고친 뒤 커밋한다.
