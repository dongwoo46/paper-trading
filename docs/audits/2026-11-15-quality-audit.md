# 코드 품질 전수조사 — run 2026-11-15-0500

> 항목표(렌즈)마다 감사자 하나가 전 파일을 항목별로 채점한 읽기 전용 결과. 합산·중복 제거·커버리지·점수는 `quality-audit.js merge` 가 결정적으로 냈다 — 표의 판정을 손으로 고치지 않는다. 리팩터링은 이 문서가 아니라 같은 폴더의 files.md 를 연다.

## 1. 요약

| 항목 | 값 |
|---|---|
| 감사 인스턴스 | 기대 42 · 도착 42 · 빠짐 0 |
| 발견 | 중복 제거 전 646 → 후 646 |
| 커버리지(범위 전체) | 파일 자리(인스턴스×파일) 545/545 (100%) · 한 번이라도 감사된 파일 307/307 (100%) — «전수»는 이 줄로 판단한다 |
| 커버리지(도착분 안에서) | 파일 545/545 (100%) — 도착한 인스턴스가 자기 몫을 다 봤는지만 잰다. 빠진 인스턴스는 분모에 없다 |
| 안 돌린 도구 | 32건 |
| 도구 목록(실행당 한 번) | 25개 · 코디네이터가 안 돌린 것 0개 · 감사자가 다시 돌린 것 0건(감사자 0개) |
| 기준 충돌(항목표 ↔ 팀 규칙) | 14건 |
| 다른 렌즈가 반증한 발견 | 111건 (반증 기록 241건) — 지우지 않고 표식만, 사람이 고른다 |

## 2. 발견 요약 (우선순위 = 심각도·확신 내림차순 — 상세는 §2-1)

| 순위 | 위치 | 항목 | 무엇이 틀렸나 | 심각도 | 확신 |
|---|---|---|---|---|---|
| 1 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/account/AccountCommandService.kt:10 | L1 | 유스케이스가 infrastructure 의 Spring Data 리포지토리를 직접 주입받음 | high | high |
| 2 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/account/AccountExitTriggerDefaultCommandService.kt:6 | L1 | 유스케이스가 infrastructure 리포지토리를 직접 주입받음 | high | high |
| 3 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/account/AccountQueryService.kt:8 | L1 | 조회 유스케이스가 infrastructure 리포지토리를 직접 주입받음 | high | high |
| 4 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/account/KisAccountQueryService.kt:9 | L1 | KIS 잔고 조회 유스케이스가 PositionRepository 를 직접 주입받음(KIS 쪽만 포트 사용) | high | high |
| 5 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/account/RiskPolicyService.kt:7 | L1 | 유스케이스가 Spring Data 리포지토리를 직접 import·주입함 | high | high |
| 6 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/ExecutionProcessor.kt:16 | L1 | 유스케이스가 infrastructure 리포지토리 8개를 직접 import·주입 | high | high |
| 7 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/ExecutionProcessor.kt:88 | S1 | processFill 한 함수가 약 115줄 — 체결·수수료·포지션·원장·정산·구독 해제·이벤트·알림을 모두 함 | high | high |
| 8 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/ExecutionProcessor.kt:114 | S6 | 평균 체결가를 서비스가 계산해 넘겨 Order.totalAmount 가 누적 평균 × 수량으로 틀어짐 | high | high |
| 9 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/ExecutionProcessor.kt:205 | X3 | 체결 트랜잭션(주문·계좌·포지션 락 보유) 안에서 collector 구독 해제 HTTP 호출 | high | high |
| 10 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/KisExecutionNoticeService.kt:3 | L1 | KIS 통지 유스케이스가 infrastructure 타입(KisExecutionNotice·리포지토리 2개)을 직접 import | high | high |
| 11 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/KisLiveOrderExecutor.kt:5 | L1 | application 실행기가 infrastructure HTTP 클라이언트·리포지토리를 직접 주입받음 | high | high |
| 12 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/KisLiveOrderExecutor.kt:25 | X3 | @Transactional 메서드가 KIS 실거래 REST 호출을 직접 수행 | high | high |
| 13 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/KisPaperOrderExecutor.kt:5 | L1 | application 실행기가 infrastructure HTTP 클라이언트를 직접 주입받음 | high | high |
| 14 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/KisPaperOrderExecutor.kt:22 | X3 | @Transactional 메서드가 KIS 모의투자 REST 호출을 직접 수행 | high | high |
| 15 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/LocalMatchingEngine.kt:7 | L1 | 매칭 엔진이 Spring Data 리포지토리를 직접 주입받음 | high | high |
| 16 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/OrderCommandService.kt:19 | L1 | 주문 명령 서비스가 리포지토리 4개를 직접 주입받음 | high | high |
| 17 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/OrderCommandService.kt:49 | I3 | KIS 주문 호출이 커밋 전 트랜잭션 안에서 실행됨 — 의도 기록·불확정 상태 없음 | high | high |
| 18 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/OrderCommandService.kt:66 | X3 | 주문 접수 트랜잭션 안에서 collector-api 구독 HTTP(타임아웃 없음)를 호출 | high | high |
| 19 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/OrderCommandService.kt:70 | X3 | 주문 접수 트랜잭션(계좌 비관 락 보유) 안에서 KIS 주문 REST 를 호출 | high | high |
| 20 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/OrderCommandService.kt:238 | I3 | KIS 주문 취소가 로컬 상태만 바꾸고 KIS 취소를 호출하지 않음 | high | high |
| 21 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/OrderQueryService.kt:8 | L1 | 주문 조회 서비스가 리포지토리를 직접 주입받음 | high | high |
| 22 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/OrderQueryService.kt:25 | Q5 | 주문 목록 유스케이스가 상한 인자 없이 전체를 돌려줌 | high | high |
| 23 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/portfolio/DailyBalanceCommandService.kt:5 | L1 | 유스케이스가 infrastructure.persistence 의 Spring Data 리포지토리 3개를 직접 import·주입 | high | high |
| 24 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/portfolio/DailyBalanceQueryService.kt:6 | L1 | 조회 유스케이스가 infrastructure 리포지토리 2개를 직접 import·주입 | high | high |
| 25 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/portfolio/PortfolioSnapshotCommandService.kt:7 | L1 | 유스케이스가 infrastructure.persistence 리포지토리 3개를 직접 import·주입 | high | high |
| 26 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/portfolio/PortfolioSnapshotQueryService.kt:5 | L1 | 조회 유스케이스가 infrastructure 리포지토리 2개를 직접 import·주입 | high | high |
| 27 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/portfolio/SnapshotJobRunTxService.kt:5 | L1 | 유스케이스가 infrastructure 의 Spring Data 리포지토리를 직접 주입받음 | high | high |
| 28 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/portfolio/SnapshotJobService.kt:8 | L1 | 유스케이스가 AccountRepository·SnapshotJobRunRepository 를 직접 주입받음 | high | high |
| 29 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/portfolio/TaxSummaryBatchService.kt:6 | L1 | 유스케이스가 AccountRepository 를 직접 주입받음 | high | high |
| 30 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/portfolio/TaxSummaryCommandService.kt:18 | L1 | 유스케이스가 리포지토리 3개를 직접 주입받음 | high | high |
| 31 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/portfolio/TaxSummaryQueryService.kt:10 | L1 | 조회 유스케이스가 리포지토리를 직접 주입받음 | high | high |
| 32 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/position/PositionCommandService.kt:7 | L1 | 유스케이스가 infrastructure 의 Spring Data 리포지토리를 직접 주입받는다 | high | high |
| 33 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/position/PositionExitTriggerCommandService.kt:15 | L1 | 유스케이스가 PositionExitTriggerRepository·PositionRepository 를 직접 주입받는다 | high | high |
| 34 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/position/PositionExitTriggerOrchestrator.kt:10 | L1 | 유스케이스가 OrderRepository·PositionExitTriggerRepository·PositionRepository 를 직접 주입받는다 | high | high |
| 35 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/position/PositionExitTriggerQueryService.kt:6 | L1 | 조회 유스케이스가 두 리포지토리를 직접 주입받는다 | high | high |
| 36 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/position/PositionQueryService.kt:10 | L1 | 조회 유스케이스가 PositionRepository 를 직접 주입받는다 | high | high |
| 37 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/account/Account.kt:10 | D1 | 계좌 애그리거트 루트가 JPA 엔티티 그대로 domain 에 있음 | high | high |
| 38 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/account/AccountExitTriggerDefault.kt:13 | D1 | 청산 트리거 기본값이 JPA 엔티티로 domain 에 있음 | high | high |
| 39 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/account/AccountExitTriggerDefault.kt:17 | D2 | 청산 트리거 기본값의 모든 상태 필드가 공개 setter 이고 밖에서 대입됨 | high | high |
| 40 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/account/AccountLedger.kt:24 | D1 | 계좌 원장이 JPA 엔티티로 domain 에 있음 | high | high |
| 41 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/account/RiskEvent.kt:20 | D1 | 리스크 이벤트 이력이 JPA 엔티티로 domain 에 있음 | high | high |
| 42 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/account/RiskPolicy.kt:24 | D1 | 리스크 정책이 JPA 엔티티로 domain 에 있음 | high | high |
| 43 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/base/BaseAuditEntity.kt:3 | D1 | 도메인 베이스 클래스가 JPA·Hibernate 에 묶여 있음 (BaseAuditEntity) | high | high |
| 44 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/base/BaseTimeEntity.kt:3 | D1 | 도메인 베이스 클래스가 JPA·Hibernate 에 묶여 있음 (BaseTimeEntity) | high | high |
| 45 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/notification/Notification.kt:7 | D1 | 도메인 파일이 JPA 엔티티를 겸한다 | high | high |
| 46 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/order/Execution.kt:27 | D1 | 도메인 Execution 이 JPA 엔티티 | high | high |
| 47 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/order/Order.kt:32 | D1 | 도메인 Order(애그리거트 루트)가 JPA 엔티티 (@Version 포함) | high | high |
| 48 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/order/OrderAmendment.kt:19 | D1 | 도메인 OrderAmendment 가 JPA 엔티티 | high | high |
| 49 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/order/OrderSignal.kt:29 | D1 | 도메인 OrderSignal 이 JPA 엔티티이고 Hibernate 타입(JdbcTypeCode)까지 의존 | high | high |
| 50 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/portfolio/DailyBalance.kt:5 | D1 | domain 파일이 jakarta.persistence·org.hibernate 를 import 하고 @Entity 를 직접 단다 | high | high |
| 51 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/portfolio/PortfolioSnapshot.kt:5 | D1 | domain 파일이 jakarta.persistence·org.hibernate 를 import 하고 @Entity 를 직접 단다 | high | high |
| 52 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/portfolio/SnapshotJobRun.kt:6 | D1 | domain 파일이 jakarta.persistence 를 import 하고 @Entity 를 직접 단다 | high | high |
| 53 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/portfolio/TaxSummary.kt:6 | D1 | domain 파일이 jakarta.persistence 를 import 하고 @Entity 를 직접 단다 | high | high |
| 54 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/portfolio/TaxSummaryRun.kt:7 | D1 | domain 파일이 jakarta.persistence 를 import 하고 @Entity 를 직접 단다 | high | high |
| 55 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/position/Position.kt:6 | D1 | Position 도메인 모델이 JPA·Hibernate 엔티티 그 자체다 | high | high |
| 56 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/position/PositionExitTrigger.kt:11 | D1 | PositionExitTrigger 도메인 모델이 JPA 엔티티 그 자체다 | high | high |
| 57 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/settlement/Settlement.kt:28 | D1 | 도메인 Settlement 가 JPA 엔티티 | high | high |
| 58 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/settlement/SettlementExecution.kt:20 | D1 | 도메인 SettlementExecution(조인 엔티티)이 JPA 엔티티 | high | high |
| 59 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/strategy/Strategy.kt:7 | D1 | 도메인 Strategy 가 JPA 엔티티(jakarta.persistence·Hibernate 상속) | high | high |
| 60 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/strategy/StrategyDerivation.kt:4 | D1 | 도메인 StrategyDerivation 이 JPA 엔티티 | high | high |
| 61 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/strategy/StrategyLog.kt:5 | D1 | 도메인 StrategyLog 가 JPA·Hibernate 엔티티 | high | high |
| 62 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/strategy/StrategyPerformanceSnapshot.kt:4 | D1 | 도메인 StrategyPerformanceSnapshot 이 JPA 엔티티 | high | high |
| 63 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/strategy/StrategyVersion.kt:4 | D1 | 도메인 StrategyVersion 이 JPA·Hibernate 엔티티 | high | high |
| 64 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/port/MarketQuotePort.kt:1 | L3 | 포트가 domain/port 에 있고 시그니처가 application 타입을 반환해 의존 방향이 뒤집힘 | high | high |
| 65 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/client/CollectorSubscriptionAdapter.kt:19 | X3 | 주문 트랜잭션(계좌 락 보유) 안에서 타임아웃 없는 RestTemplate 로 collector-api 호출 | high | high |
| 66 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/client/CollectorSubscriptionAdapter.kt:19 | E5 | collector-api 구독 호출이 타임아웃 없는 RestTemplate 을 직접 만들어 씀 | high | high |
| 67 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/kis/KisExecutionWebSocketManager.kt:4 | L4 | infrastructure 어댑터가 application 서비스(비포트)를 직접 호출 | high | high |
| 68 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/kis/KisExecutionWebSocketManager.kt:56 | A3 | 재연결 작업의 Future 를 버려 예외가 조용히 사라지고 재연결 사슬이 끊김 | high | high |
| 69 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/kis/KisExecutionWebSocketManager.kt:143 | I6 | KIS 체결 반영이 WebSocket 통지 한 통로뿐 — 대조·복구 경로가 죽어 있음 | high | high |
| 70 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/AccountLedgerRepositoryCustom.kt:3 | L4 | infrastructure 가 port 밖 application 패키지(query·result)를 import | high | high |
| 71 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/AccountLedgerRepositoryImpl.kt:3 | L4 | Querydsl 구현체가 port 밖 application 타입을 import 하고 필터를 직접 읽음 | high | high |
| 72 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/OrderRepositoryImpl.kt:19 | Q5 | 주문 목록 조회에 건수 상한이 없고 기간 조건도 선택값 | high | high |
| 73 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/PositionRepositoryImpl.kt:4 | D1 | 도메인 패키지의 JPA 엔티티(Position)에서 Querydsl Q 클래스가 생성되고 영속 어댑터가 그것을 직접 사용 | high | high |
| 74 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/redis/QuoteEventListener.kt:4 | L4 | infrastructure 리스너가 application 유스케이스 3개를 직접 주입받아 호출함 | high | high |
| 75 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/redis/RedisListenerConfig.kt:17 | A1 | RedisMessageListenerContainer 에 taskExecutor 가 없고 리스너가 onMessage 에서 매칭·DB 쓰기를 직접 함 | high | high |
| 76 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/scheduler/LocalMatchingScheduler.kt:3 | L4 | infrastructure 가 application 서비스·명령을 직접 import (ArchUnit 동결 2건) | high | high |
| 77 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/sse/ExecutionSseEventHandler.kt:4 | L4 | infrastructure 가 presentation DTO 를 import (ArchUnit 동결 1건) — presentation↔infrastructure 순환 | high | high |
| 78 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/sse/ExecutionSseRegistry.kt:3 | L4 | infrastructure 의 SSE 레지스트리가 presentation DTO 를 import (의존 방향 역전) | high | high |
| 79 | backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/controller/ExecutionSseController.kt:3 | L5 | 컨트롤러가 infrastructure 어댑터를 직접 import·주입 (presentation → infrastructure) | high | high |
| 80 | backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/dto/account/AccountResponse.kt:8 | L5 | 응답 DTO 가 JPA 엔티티(Account·AccountLedger)를 직접 받아 변환한다 | high | high |
| 81 | backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/dto/account/RiskPolicyDto.kt:3 | L5 | 응답 DTO 가 JPA 엔티티(RiskPolicy)를 직접 받아 변환한다 | high | high |
| 82 | backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/dto/order/OrderResponse.kt:10 | L5 | 응답 DTO 가 domain.entity 를 import 하고 엔티티를 받는 매퍼를 가짐 | high | high |
| 83 | backend/trading-api/src/test/kotlin/com/papertrading/api/application/portfolio/SnapshotJobServiceTest.kt:30 | T1 | 같은 레포 서비스 3개를 mockk 로 갈음해 시험함 | high | high |
| 84 | (repo) | C11 | 놓친 시세 구간의 고가·저가로 미체결 지정가·손절선을 다시 판정하는 복구 경로가 없다 | high | med |
| 85 | (repo) | S10 | trading-api 에 인증이 전혀 없고, 인증 없음의 근거(네트워크 격리 등) 문서도 없다 | high | med |
| 86 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/account/AccountCommandService.kt:72 | X8 | 계좌 이름·활성 변경이 락·버전 없이 전체 열 UPDATE 를 내 동시 입출금을 덮어쓸 수 있음 | high | med |
| 87 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/KisLiveOrderExecutor.kt:30 | I2 | KIS_LIVE 주문 호출 실패를 불확정으로 다루지 않고 예외로 롤백 | high | med (반증 있음) |
| 88 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/portfolio/SnapshotJobService.kt:69 | L2 | 유스케이스가 DataIntegrityViolationException 을 직접 잡음 | high | med (반증 있음) |
| 89 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/portfolio/TaxSummaryCommandService.kt:66 | L2 | 유스케이스가 DataIntegrityViolationException 을 직접 잡음 | high | med (반증 있음) |
| 90 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/position/PositionExitTriggerOrchestrator.kt:36 | X3 | 시세 틱 전체 트랜잭션이 포지션 행 락을 쥔 채 안쪽 주문 경로(KIS 제출·RestTemplate)를 부르고, 안쪽이 같은 행을 다시 잠가 교착 위험 | high | med |
| 91 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/position/PositionExitTriggerOrchestrator.kt:36 | E7 | 한 트랜잭션 안에서 잠금 실패를 잡고 다음 그룹을 계속 처리 | high | med |
| 92 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/feepolicy/FeePolicy.kt:23 | D1 | 도메인 FeePolicy 가 JPA 엔티티 (@Entity·@Table·@Column) | high | med (반증 있음) |
| 93 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/order/Execution.kt:60 | M11 | 체결 수수료만 저장하고 적용한 요율·정책 버전을 함께 저장하지 않음 | high | med |
| 94 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/order/Order.kt:32 | M4 | orders 의 핫 경로 필터 열(ticker·order_status·external_order_id·created_at)에 인덱스가 없음 | high | med |
| 95 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/order/Order.kt:72 | M8 | KIS 주문 접수 결과를 «불확정»으로 담을 상태(UNKNOWN)가 없음 | high | med |
| 96 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/portfolio/TradingJournal.kt:5 | D1 | domain 파일이 jakarta.persistence 를 import 하고 @Entity 를 직접 단다 | high | med |
| 97 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/settlement/Settlement.kt:56 | M11 | 정산의 수수료·세금에 적용한 요율(tax_rate)·정책 버전이 저장되지 않음 | high | med |
| 98 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/kis/KisExecutionNoticeParser.kt:19 | E1 | 체결 통지 파싱 실패와 필드 누락이 로그 없이 null 로 사라짐 | high | med (반증 있음) |
| 99 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/kis/KisExecutionWebSocketManager.kt:227 | I6 | 체결 처리 예외 경계가 없어 한 건 실패가 통지 유실로 이어짐 | high | med |
| 100 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/kis/KisTokenManager.kt:85 | S3 | KIS 접근 토큰·WebSocket 승인키를 Redis 에 평문으로 저장, Redis 는 인증·TLS 없음 | high | med |
| 101 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/redis/RedisListenerConfig.kt:20 | A2 | 같은 종목의 시세 메시지가 직렬화되지 않아 같은 종목 매칭이 동시에 돌 수 있음 | high | med |
| 102 | backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/controller/PositionExitTriggerController.kt:26 | S7 | positionId 만으로 청산 트리거를 만들고 고치고 취소 — 소유 범위 없음 | high | med |
| 103 | backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/exception/GlobalExceptionHandler.kt:25 | E4 | IllegalArgument·IllegalState 등 일반 예외의 원문 메시지를 400 응답 본문에 그대로 실음 | high | med |
| 104 | backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/exception/GlobalExceptionHandler.kt:59 | O1 | 전역 핸들러가 500 을 만들면서 예외를 로그에 남기지 않음 | high | med (반증 있음) |
| 105 | backend/trading-api/src/main/resources/application.yaml:19 | S2 | 기본 프로필에 ddl-auto: create 가 커밋돼 있음 | high | med (반증 있음) |
| 106 | backend/trading-api/src/main/resources/db/migration/V8__add_order_idempotency_scope.sql:1 | M12 | trading-api 에 마이그레이션 적용 주체가 없고 V1 기준선이 없다 — 마이그레이션이 Hibernate 가 만든 테이블을 가정 | high | med |
| 107 | backend/trading-api/src/test/kotlin/com/papertrading/api/application/position/PositionExitTriggerOrchestratorTest.kt:78 | T5 | PESSIMISTIC_WRITE 락·@Modifying 쿼리에 실제 엔진 시험이 없음 — 전부 목으로만 시험 | high | med |
| 108 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/ExecutionProcessor.kt:233 | D3 | 잠금 해제 금액 규칙이 서비스 두 곳에 제각각 구현 — 부분 체결 후 최종 체결에서 과다 해제 | high | low (반증 있음) |
| 109 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/position/PositionExitTriggerOrchestrator.kt:42 | X6 | 같은 @Transactional 안에서 락 예외를 잡고 계속 진행 — 트랜잭션이 이미 rollback-only 라 커밋에서 터지거나 모든 SKIPPED 기록이 사라짐 | high | low (반증 있음) |
| 110 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/position/PositionExitTriggerOrchestrator.kt:42 | E2 | 시세 틱마다 ARMED 그룹 수에 비례해 평가 전에 잠금 쿼리를 두 번씩 실행 | high | low (반증 있음) |
| 111 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/account/Account.kt:26 | M2 | 잔고를 가진 Account 에 @Version 이 없고 락 없는 쓰기 경로가 있음 | high | low (반증 있음) |
| 112 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/notification/SlackWebhookNotifier.kt:27 | O7 | 실패 로그에 ex.message 를 찍어 Slack 웹훅 비밀 URL 이 로그로 샐 수 있음 | high | low (반증 있음) |
| 113 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/redis/QuoteEventListener.kt:32 | A1 | 리스너가 onMessage 안에서 매칭·DB 쓰기를 직접 하고, 컨테이너에 실행기·큐 상한이 없음 | high | low (반증 있음) |
| 114 | backend/trading-api/src/main/resources/application.yaml:17 | M12 | 운영 기본 설정이 ddl-auto: create — 기동마다 엔티티 테이블을 DROP 후 재생성 | high | low (반증 있음) |
| 115 | (repo) | N1 | 취소 철자가 CANCELLED 와 CANCELED 로 갈림 — 저장되는 enum 값까지 | med | high |
| 116 | (repo) | N1 | 체결 수량이 qty 와 quantity 로 갈려 한 흐름에서 세 번 번역됨 | med | high |
| 117 | (repo) | O10 | 요청 상관 id 필터·추적이 레포 어디에도 없고 진입점에서도 새로 만들지 않음 | med | high |
| 118 | (repo) | S11 | 의존성 취약점 검사·의존성 잠금·Gradle wrapper 체크섬·Docker 이미지 고정이 모두 없다 | med | high |
| 119 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/account/AccountQueryService.kt:10 | L2 | application 이 허용 목록 밖 org.springframework.data.domain.Page 를 사용 | med | high |
| 120 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/account/query/LedgerFilter.kt:4 | L2 | 조회 조건이 Spring Data Pageable 을 필드로 가짐 | med | high |
| 121 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/common/result/QuoteSnapshot.kt:1 | L3 | 출력 포트의 결과 타입이 port 패키지가 아닌 application.common.result 에 있음 | med | high |
| 122 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/notification/SlackNotificationEventPublisher.kt:4 | L2 | application 이 허용 목록 밖 Spring 타입을 쓴다 | med | high |
| 123 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/notification/SlackNotificationService.kt:40 | F4 | 서비스가 시각과 난수를 직접 읽는다 | med | high |
| 124 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/ExecutionProcessor.kt:25 | L2 | 허용 목록 밖 Spring 타입 ApplicationEventPublisher 를 application 이 사용 | med | high |
| 125 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/ExecutionProcessor.kt:65 | F4 | fillLocal 이 체결번호(UUID)와 체결 시각을 함수 안에서 직접 만듦 | med | high |
| 126 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/ExecutionProcessor.kt:108 | F4 | 수수료 정책을 체결 시각(executedAt)이 아니라 처리 시각 Instant.now() 로 조회 | med | high |
| 127 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/ExecutionProcessor.kt:116 | F1 | BigDecimal 을 == 로 비교 (prevQty == BigDecimal.ZERO) | med | high |
| 128 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/ExecutionProcessor.kt:246 | E4 | 체결마다 계정의 활성 주문 전체를 로딩한 뒤 앱에서 ticker 로 거름 | med | high |
| 129 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/KisExecutionNoticeService.kt:23 | O7 | 체결 통지 로그에 계좌 번호 원문이 포함됨 | med | high |
| 130 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/KisLiveOrderExecutor.kt:39 | E2 | 주문 실패 로그에 예외 객체가 없고, 재던진 예외도 어디서도 로그되지 않음 | med | high |
| 131 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/KisPaperOrderExecutor.kt:27 | I2 | KIS_PAPER 주문 호출 실패를 불확정으로 다루지 않고 예외로 롤백 | med | high |
| 132 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/KisPaperOrderExecutor.kt:36 | E2 | 주문 실패 로그에 예외 객체가 없음 | med | high |
| 133 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/portfolio/PortfolioSnapshotCommandService.kt:24 | S1 | recalculate 본문이 빈 줄 제외 46줄 — 시세 조회·평가액·비중 계산·조회/갱신/생성 분기·저장이 한 함수에 섞임 | med | high |
| 134 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/portfolio/PortfolioSnapshotCommandService.kt:45 | E2 | 보유 종목마다 스냅샷을 조회·저장하는 루프 — 종목 수만큼 왕복 | med | high |
| 135 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/portfolio/TaxSummaryCommandService.kt:43 | S1 | recalculate 본문이 61줄 — 검증·잠금·실행기록·집계·저장·실패기록이 한 함수에 섞여 있음 | med | high |
| 136 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/portfolio/TaxSummaryCommandService.kt:60 | D3 | «LOCAL 계좌만 지원» 규칙을 같은 모양으로 한 번 더 구현 | med | high |
| 137 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/portfolio/TaxSummaryCommandService.kt:101 | X6 | 실패 이력(FAILED)을 같은 트랜잭션에서 기록하고 예외를 다시 던져 롤백과 함께 사라진다 | med | high |
| 138 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/portfolio/TaxSummaryCommandService.kt:101 | O8 | 실패한 세금 요약 실행의 FAILED 기록이 같은 트랜잭션 안에서 저장돼 예외와 함께 롤백됨 | med | high |
| 139 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/portfolio/TaxSummaryCommandService.kt:118 | S3 | 세금 계산 규칙이 서비스 안 private compute 와 TaxSummaryCalculator 두 곳에 있고, 분리해 둔 계산기는 운영 코드에서 쓰이지 않음 | med | high |
| 140 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/portfolio/TaxSummaryQueryService.kt:46 | D3 | «LOCAL 계좌만 지원» 규칙을 서비스가 직접 구현하고 Account 에는 그 메서드가 없음 | med | high |
| 141 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/portfolio/TradingJournalQueryService.kt:10 | L2 | 유스케이스가 Spring Data 의 Page·Pageable 을 시그니처에 씀 | med | high |
| 142 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/position/AutoExitOrderPlacementService.kt:4 | L8 | position 컨텍스트가 order 컨텍스트의 서비스 클래스를 직접 호출한다 | med | high |
| 143 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/position/PositionCommandService.kt:31 | F4 | updateCurrentPriceByTicker 가 Instant.now() 를 직접 읽는다 | med | high |
| 144 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/position/PositionExitQuoteEventListener.kt:3 | L2 | 허용 목록 밖 org.springframework.stereotype.Component 를 쓴다 | med | high |
| 145 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/position/PositionExitTriggerEvaluator.kt:9 | L2 | 허용 목록 밖 org.springframework.stereotype.Component 를 쓴다 | med | high |
| 146 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/position/PositionExitTriggerOrchestrator.kt:13 | X10 | 어댑터가 영속 예외를 바꾸지 않아 Spring DAO 예외가 application 에서 업무 분기로 쓰임 | med | high |
| 147 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/position/PositionExitTriggerOrchestrator.kt:130 | S1 | tryCreateGroupedAutoExitOrderWithRetry 본문이 48줄로 40줄을 넘는다 | med | high |
| 148 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/position/PositionExitTriggerOrchestrator.kt:170 | E1 | 자동 청산 주문 재시도 중간 실패를 로그·기록 없이 삼킴 | med | high |
| 149 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/position/PositionExitTriggerOrchestrator.kt:170 | E3 | 진입점이 아닌 재시도 루프 안의 넓은 catch(Exception) | med | high |
| 150 | backend/trading-api/src/main/kotlin/com/papertrading/api/common/exception/ApiDomainException.kt:3 | L7 | 최상위 common 예외가 Spring HttpStatus 를 품음 (ApiDomainException) | med | high |
| 151 | backend/trading-api/src/main/kotlin/com/papertrading/api/common/exception/CommonApiExceptions.kt:3 | L7 | common 예외 묶음이 HttpStatus 를 직접 고정함 (CommonApiExceptions) | med | high |
| 152 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/account/Account.kt:3 | L7 | domain 이 HTTP 상태를 품은 common 예외를 import·throw 함 | med | high |
| 153 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/account/AccountExitTriggerDefault.kt:36 | D5 | 청산 트리거 기본값의 불변식 검증이 모델이 아니라 Account 에 있음 | med | high |
| 154 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/notification/Notification.kt:35 | D4 | 알림이 계좌 애그리거트를 직접 참조한다 | med | high |
| 155 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/notification/Notification.kt:97 | F4 | 도메인 메서드가 시각을 직접 읽는다 | med | high |
| 156 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/order/Execution.kt:38 | D4 | Execution 이 Order·Account 두 루트를 객체로 참조 | med | high |
| 157 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/order/Order.kt:43 | D4 | Order 가 다른 애그리거트 Account 를 객체로 참조 (@ManyToOne) | med | high |
| 158 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/order/OrderSignal.kt:40 | D4 | OrderSignal 이 Account 루트를 객체로 참조 | med | high |
| 159 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/portfolio/DailyBalance.kt:31 | D4 | 다른 애그리거트(Account)를 @ManyToOne 객체 참조로 가짐 | med | high |
| 160 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/portfolio/PortfolioSnapshot.kt:36 | D4 | 다른 애그리거트(Account)를 @ManyToOne 객체 참조로 가짐 | med | high |
| 161 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/portfolio/SnapshotJobRun.kt:27 | D4 | 다른 애그리거트(Account)를 @ManyToOne 객체 참조로 가짐 | med | high |
| 162 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/portfolio/SnapshotJobRun.kt:53 | F4 | complete·fail·start 가 Instant.now() 를 직접 읽음 | med | high |
| 163 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/portfolio/TaxSummary.kt:38 | D4 | 다른 애그리거트(Account)를 @ManyToOne 객체 참조로 가짐 | med | high |
| 164 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/portfolio/TaxSummary.kt:60 | F4 | computedAt 이 Instant.now() 직접 읽기(초기화식·recalculate·create 3곳) | med | high |
| 165 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/portfolio/TaxSummary.kt:78 | D5 | recalculate 는 예상세금 ≥ 0 을 강제하는데 create 에는 같은 검증이 없음 | med | high |
| 166 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/portfolio/TaxSummaryRun.kt:48 | F4 | startedAt·finishedAt 이 Instant.now() 직접 읽기(4곳) | med | high |
| 167 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/portfolio/TradingJournal.kt:28 | D4 | 다른 애그리거트(Account)를 @ManyToOne 객체 참조로 가짐 — orderId 는 id 로 참조하는데 account 만 객체 | med | high |
| 168 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/position/Position.kt:46 | D4 | Position 이 다른 애그리거트 루트 Account 를 @ManyToOne 으로 직접 든다 | med | high |
| 169 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/position/Position.kt:195 | F4 | Position.updatePrice 가 Instant.now() 를 직접 읽는다 | med | high |
| 170 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/position/PositionExitTrigger.kt:3 | L7 | 도메인이 HTTP 상태를 품은 common 예외를 직접 던진다 | med | high |
| 171 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/settlement/Settlement.kt:38 | D4 | Settlement 가 Order·Account 두 루트를 객체로 참조 | med | high |
| 172 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/strategy/Strategy.kt:36 | D4 | Strategy 가 Account 애그리거트를 객체로 직접 참조 | med | high |
| 173 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/strategy/Strategy.kt:150 | F4 | 도메인 함수 본문이 Instant.now() 를 직접 읽음 (cancel·markExecuted) | med | high |
| 174 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/strategy/StrategyDerivation.kt:26 | D4 | StrategyDerivation 이 Strategy 루트 둘을 객체로 참조 | med | high |
| 175 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/strategy/StrategyLog.kt:30 | D4 | StrategyLog 가 Strategy 와 Account 루트를 객체로 참조 | med | high |
| 176 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/strategy/StrategyLog.kt:54 | F4 | StrategyLog 가 속성 초기값·기본 인자·검증 본문에서 Instant.now() 를 직접 읽음 | med | high |
| 177 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/port/CollectorSubscriptionPort.kt:1 | L3 | 출력 포트가 application/<ctx>/port 가 아니라 domain/port 에 있음 | med | high |
| 178 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/port/LivePositionCachePort.kt:1 | L3 | 출력 포트가 domain.port 에 있다(application.position.port 가 아님) | med | high |
| 179 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/port/NotificationSender.kt:1 | L3 | 출력 포트가 domain/port 에 있다 | med | high |
| 180 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/kis/KisAccountBalanceAdapter.kt:121 | E1 | KIS 응답의 금액 필드 파싱 실패가 조용히 0 이 됨 | med | high |
| 181 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/kis/KisExecutionWebSocketManager.kt:202 | C4 | 재연결에 지터가 없고 5회 소진 뒤 영구 포기 | med | high |
| 182 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/kis/KisOrderProperties.kt:13 | S4 | 필수 KIS 자격 증명이 빈 문자열 기본값이고 시작 시 검증이 없음 | med | high |
| 183 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/kis/KisOrderRestClient.kt:133 | F1 | BigDecimal 을 == 로 비교(equals, scale 민감) | med | high |
| 184 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/notification/SlackNotificationEventHandler.kt:4 | L4 | infrastructure 가 port 밖 application 클래스를 import 한다 | med | high |
| 185 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/notification/SlackNotificationScheduler.kt:17 | L6 | 스케줄러 진입점이 infrastructure 에 있고 유스케이스 없이 재전송을 몬다 | med | high |
| 186 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/AccountLedgerRepositoryImpl.kt:37 | Q6 | 계속 자라는 원장 목록이 OFFSET 페이징이고 페이지 상한이 없음 | med | high |
| 187 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/AccountLedgerRepositoryImpl.kt:54 | Q2 | 원장 기간 조회의 상한이 배타 값으로 넘어오는데 저장소는 <= 로 비교 | med | high |
| 188 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/ExecutionRepositoryCustom.kt:3 | L4 | infrastructure 가 port 밖 application 패키지를 import (ArchUnit 동결 1건) | med | high |
| 189 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/ExecutionRepositoryImpl.kt:3 | L4 | infrastructure 가 port 밖 application 패키지를 import (ArchUnit 동결 1건) | med | high |
| 190 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/ExecutionRepositoryImpl.kt:20 | Q2 | 체결 목록 조회의 끝 경계가 포함(<=)이라 반열림 [start, end) 규칙을 어김 | med | high |
| 191 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/OrderRepositoryCustom.kt:3 | L4 | infrastructure 가 port 밖 application 패키지를 import (ArchUnit 동결 1건) | med | high |
| 192 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/OrderRepositoryImpl.kt:3 | L4 | infrastructure 가 port 밖 application 패키지를 import (ArchUnit 동결 1건) | med | high |
| 193 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/OrderRepositoryImpl.kt:27 | Q2 | 주문 목록 조회의 끝 경계가 포함(<=)이라 반열림 [start, end) 규칙을 어김 | med | high |
| 194 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/SettlementTaxReadRepositoryImpl.kt:3 | L3 | 세금 집계 출력 포트와 결과 타입이 port 패키지 밖(application.portfolio.tax)에 있음 | med | high |
| 195 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/TradingJournalRepositoryCustom.kt:3 | L4 | 리포지토리 조각 인터페이스가 application.portfolio.query 의 필터를 import (application ↔ infrastructure 순환) | med | high |
| 196 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/redis/QuoteEventListener.kt:22 | L6 | Redis MessageListener 진입점이 infrastructure 에 있고 유스케이스를 직접 부름 | med | high |
| 197 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/redis/RedisLivePositionCacheAdapter.kt:6 | L3 | 캐시 출력 포트 LivePositionCachePort 가 domain.port 에 있어 application.<ctx>.port 규칙을 벗어남 | med | high |
| 198 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/redis/RedisLivePositionCacheAdapter.kt:57 | E1 | 스냅샷 파싱 실패를 runCatching + getOrNull 로 로그 없이 버림 | med | high |
| 199 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/redis/RedisMarketQuoteAdapter.kt:4 | L4 | infrastructure 어댑터가 port 밖 application.common.result 를 import 함 | med | high |
| 200 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/redis/RedisMarketQuoteAdapter.kt:40 | E1 | 시세 해시 파싱 실패를 runCatching.getOrNull() 로 삼킴 | med | high |
| 201 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/scheduler/KisPaperPollingScheduler.kt:3 | L4 | infrastructure 가 application 유스케이스를 직접 import (ArchUnit 동결 2건) — 죽은 클래스 | med | high |
| 202 | backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/controller/AccountController.kt:90 | S8 | 입출금 요청 금액·문자열에 자릿수·길이 상한 검증이 없음 | med | high |
| 203 | backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/controller/ExecutionSseController.kt:15 | S9 | SSE 컨트롤러가 CORS 출처를 와일드카드(*)로 열어 둠 (TODO(prod) 로 남은 임시 설정) | med | high |
| 204 | backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/controller/TradingJournalController.kt:58 | S7 | 매매일지 수정이 요청의 accountId 를 받고도 소유 검사를 하지 않음 | med | high |
| 205 | backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/dto/portfolio/PortfolioSnapshotDto.kt:3 | L5 | presentation DTO 가 도메인 엔티티를 직접 import 해 변환 인자로 받음 | med | high |
| 206 | backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/dto/portfolio/TaxSummaryDto.kt:3 | L5 | TaxSummaryResponse 가 TaxSummary 엔티티를 받아 다른 애그리거트(Account)까지 타고 넘어감 | med | high |
| 207 | backend/trading-api/src/main/resources/application.yaml:10 | S1 | DB 비밀번호 기본값이 커밋돼 있음 | med | high |
| 208 | backend/trading-api/src/test/kotlin/com/papertrading/api/application/account/AccountCommandServiceTest.kt:28 | T2 | 유스케이스 시험이 Spring Data 리포지토리 목으로 락·멱등 조회를 흉내 냄 | med | high |
| 209 | backend/trading-api/src/test/kotlin/com/papertrading/api/application/account/AccountCommandServiceTest.kt:47 | T1 | 도메인 엔티티 AccountLedger 를 목으로 갈음함 | med | high |
| 210 | backend/trading-api/src/test/kotlin/com/papertrading/api/application/account/AccountQueryServiceTest.kt:26 | T2 | 원장 필터·페이징 시험이 Querydsl 어댑터를 목 반환으로 대신함 | med | high |
| 211 | backend/trading-api/src/test/kotlin/com/papertrading/api/application/account/RiskPolicyServiceTest.kt:23 | T2 | 리스크 정책 시험이 리포지토리 목으로 락·활성 정책 조회를 흉내 냄 | med | high |
| 212 | backend/trading-api/src/test/kotlin/com/papertrading/api/application/order/ExecutionProcessorTest.kt:43 | T2 | 체결 처리 시험이 리포지토리 9개 목으로 락·원자 갱신을 흉내 냄 | med | high |
| 213 | backend/trading-api/src/test/kotlin/com/papertrading/api/application/order/KisExecutionNoticeServiceTest.kt:29 | T2 | «계좌 범위 중복 판정» 시험이 리포지토리 목으로 쿼리 범위를 흉내 냄 | med | high |
| 214 | backend/trading-api/src/test/kotlin/com/papertrading/api/application/order/KisExecutionNoticeServiceTest.kt:31 | T1 | 체결 처리 유스케이스와 도메인 엔티티를 목으로 갈음 | med | high |
| 215 | backend/trading-api/src/test/kotlin/com/papertrading/api/application/order/KisExecutionNoticeServiceTest.kt:41 | T3 | 세 시험 모두 verify 만 있고 결과 상태 단언이 없음 | med | high |
| 216 | backend/trading-api/src/test/kotlin/com/papertrading/api/application/order/OrderCommandServiceTest.kt:38 | T2 | 유스케이스 시험이 Spring Data 리포지토리 4개를 목으로 쓰고 락·멱등 조회를 흉내 냄 | med | high |
| 217 | backend/trading-api/src/test/kotlin/com/papertrading/api/application/order/OrderCommandServiceTest.kt:44 | T1 | 같은 레포 서비스 3개를 목으로 갈음하고 내부 순수 규칙의 반환을 지어냄 | med | high |
| 218 | backend/trading-api/src/test/kotlin/com/papertrading/api/application/order/OrderCommandServiceTest.kt:105 | T6 | 시험 이름은 stale 시세 예외인데 본문은 시세 없음 경로 | med | high |
| 219 | backend/trading-api/src/test/kotlin/com/papertrading/api/application/order/OrderCommandServiceTest.kt:201 | T3 | 실행기 호출 여부만 검증하고 결과 상태 단언이 없는 시험 2개 | med | high |
| 220 | backend/trading-api/src/test/kotlin/com/papertrading/api/application/order/OrderQueryServiceTest.kt:29 | T2 | 조회 시험이 리포지토리 목으로 검색 동작을 흉내 냄 | med | high |
| 221 | backend/trading-api/src/test/kotlin/com/papertrading/api/application/portfolio/DailyBalanceCommandServiceTest.kt:45 | T2 | Spring Data 리포지토리 목으로 락·필터 쿼리 결과를 흉내 냄 | med | high |
| 222 | backend/trading-api/src/test/kotlin/com/papertrading/api/application/portfolio/DailyBalanceCommandServiceTest.kt:52 | T8 | BigDecimal 을 assertEquals 로 scale 까지 비교 | med | high |
| 223 | backend/trading-api/src/test/kotlin/com/papertrading/api/application/portfolio/DailyBalanceQueryServiceTest.kt:36 | T2 | 리포지토리 목이 기간 조회 결과를 정함 | med | high |
| 224 | backend/trading-api/src/test/kotlin/com/papertrading/api/application/portfolio/PortfolioSnapshotCommandServiceTest.kt:68 | T2 | 리포지토리 목이 upsert 대상 조회·저장을 흉내 냄 | med | high |
| 225 | backend/trading-api/src/test/kotlin/com/papertrading/api/application/portfolio/PortfolioSnapshotCommandServiceTest.kt:85 | T8 | BigDecimal 을 assertEquals 로 scale 까지 비교 | med | high |
| 226 | backend/trading-api/src/test/kotlin/com/papertrading/api/application/portfolio/PortfolioSnapshotQueryServiceTest.kt:33 | T2 | 조회 정렬을 목이 정해 정렬 단언이 항상 통과 | med | high |
| 227 | backend/trading-api/src/test/kotlin/com/papertrading/api/application/portfolio/SnapshotJobServiceTest.kt:45 | T2 | 리포지토리 목과 DataIntegrityViolationException 목으로 락·유일 제약 동작을 흉내 냄 | med | high |
| 228 | backend/trading-api/src/test/kotlin/com/papertrading/api/application/portfolio/tax/TaxSummaryQueryServiceTest.kt:28 | T2 | 리포지토리 목이 연도 범위 조회 결과를 정함 | med | high |
| 229 | backend/trading-api/src/test/kotlin/com/papertrading/api/application/portfolio/TradingJournalCommandQueryServiceTest.kt:100 | T2 | ticker 조건 시험이 any() 목이라 조건을 확인하지 않음 | med | high |
| 230 | backend/trading-api/src/test/kotlin/com/papertrading/api/application/position/PositionCommandServiceTest.kt:126 | T2 | 유스케이스 단위 시험이 Spring Data 리포지토리를 Mockito 목으로 쓰고 never() 검증 하나로 «DB 조회 없음»을 증명 | med | high |
| 231 | backend/trading-api/src/test/kotlin/com/papertrading/api/application/position/PositionExitTriggerCommandServiceTest.kt:30 | T2 | Spring Data 리포지토리 목으로 락 조회 결과를 지어내고 triggerRepository 는 relaxed | med | high |
| 232 | backend/trading-api/src/test/kotlin/com/papertrading/api/application/position/PositionExitTriggerOrchestratorTest.kt:34 | T2 | Spring Data 리포지토리 3개를 목으로 쓰고 락·집계 쿼리 결과를 지어냄 | med | high |
| 233 | backend/trading-api/src/test/kotlin/com/papertrading/api/domain/entity/position/PositionTest.kt:37 | T8 | BigDecimal 단언 8곳이 assertEquals 로 scale 까지 비교한다 | med | high |
| 234 | backend/trading-api/src/test/kotlin/com/papertrading/api/infrastructure/kis/KisAccountBalanceAdapterTest.kt:84 | T7 | LIVE 엔드포인트 시험에 명시적 단언이 없음 | med | high |
| 235 | backend/trading-api/src/test/kotlin/com/papertrading/api/infrastructure/notification/SlackNotificationSchedulerTest.kt:113 | T5 | 원자 선점 쿼리(claimForDelivery)에 Testcontainers 시험이 없고 목으로만 흉내 냄 | med | high |
| 236 | backend/trading-api/src/test/kotlin/com/papertrading/api/infrastructure/sse/ExecutionSseRegistryTest.kt:57 | T7 | 'emitter 제거됨' 시험의 제거 단언이 항상 참 (가짜 초록) | med | high |
| 237 | backend/trading-api/src/test/kotlin/com/papertrading/api/presentation/controller/OrderControllerIntegrationTest.kt:135 | T6 | 시험 이름의 기대(400)와 실제 단언(409·404) 불일치 | med | high |
| 238 | backend/trading-api/src/test/kotlin/com/papertrading/api/presentation/controller/OrderControllerIntegrationTest.kt:196 | T7 | '잠금 해제' 시험이 잠금이 걸렸는지 단언하지 않음 (가짜 초록) | med | high |
| 239 | backend/trading-api/src/test/kotlin/com/papertrading/api/presentation/controller/PositionControllerIntegrationTest.kt:180 | T7 | «공백과 소문자 ticker 정상 처리» 시험이 실제로는 평범한 "005930" 을 요청 — 가짜 초록 | med | high |
| 240 | backend/trading-api/src/test/kotlin/com/papertrading/api/presentation/controller/PositionControllerIntegrationTest.kt:238 | T7 | «orderableQuantity 가 올바르게 반환된다» 시험이 값 없이 isNotEmpty 만 단언 | med | high |
| 241 | backend/trading-api/src/test/kotlin/com/papertrading/api/presentation/controller/TradingJournalControllerIntegrationTest.kt:155 | T6 | 시험 이름은 404 인데 단언은 409(isConflict) — 이름과 기대가 어긋남 | med | high |
| 242 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/account/AccountCommandService.kt:46 | I1 | 같은 멱등 키에 다른 요청이 오면 4xx 없이 이전 원장을 그대로 돌려줌 | med | med (반증 있음) |
| 243 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/ExecutionProcessor.kt:7 | L8 | order 컨텍스트가 notification 컨텍스트의 구체 클래스를 import | med | med |
| 244 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/ExecutionProcessor.kt:33 | S3 | ExecutionProcessor 가 체결·정산·시세 구독 관리를 함께 가짐 (주인공 시험 불합격) | med | med |
| 245 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/ExecutionProcessor.kt:108 | D3 | 수수료 규칙을 서비스가 다시 구현 — 도메인 calculateFee(올림)와 반올림 방식이 다름 | med | med |
| 246 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/KisLiveOrderExecutor.kt:26 | F3 | submit(order) 가 인자 order 를 바꾸는데 이름이 변경을 말하지 않음 | med | med |
| 247 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/KisLiveOrderExecutor.kt:51 | E1 | KIS_LIVE 취소 실패를 WARN 로그로만 남기고 성공처럼 반환 | med | med |
| 248 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/KisPaperOrderExecutor.kt:23 | F3 | submit(order) 가 인자 order 를 바꾸는데 이름이 변경을 말하지 않음 | med | med |
| 249 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/OrderCommandService.kt:51 | X10 | 멱등 키 충돌이 Spring 예외 타입으로 그대로 올라감(락 전 조회 → 재확인 없음) | med | med |
| 250 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/OrderCommandService.kt:51 | I1 | 같은 멱등 키에 다른 요청이 와도 기존 주문을 그대로 돌려줌 | med | med |
| 251 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/OrderCommandService.kt:103 | F4 | application 함수가 Instant.now() 를 직접 읽음(시세 신선도 판정) | med | med (반증 있음) |
| 252 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/OrderCommandService.kt:243 | S6 | 호출자가 주문 상태를 읽고 결정한 뒤 order.cancel() 호출(묻지 말고 시켜라 위반) | med | med |
| 253 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/OrderCommandService.kt:251 | D3 | 취소 시 잠금 해제 금액 규칙을 서비스가 직접 계산하고 도메인 메서드와 식이 다름 | med | med |
| 254 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/portfolio/SnapshotJobService.kt:55 | X3 | 오케스트레이터 전체가 @Transactional + 계좌 락 — REQUIRES_NEW 와 Redis 읽기가 그 안에서 돈다 | med | med |
| 255 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/portfolio/SnapshotJobService.kt:69 | I5 | 비정상 종료로 남은 RUNNING 행을 되살리는 복구 작업이 없음 | med | med (반증 있음) |
| 256 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/portfolio/SnapshotJobService.kt:71 | E2 | 무결성 위반 예외를 cause 없이 «이미 실행 중»으로 바꿈 | med | med (반증 있음) |
| 257 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/portfolio/SnapshotJobService.kt:86 | X4 | SUCCESS 기록이 본 쓰기 커밋보다 먼저 커밋된다 | med | med |
| 258 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/portfolio/SnapshotJobService.kt:86 | I5 | SUCCESS 표시가 스냅샷 데이터 커밋보다 먼저 확정됨 | med | med |
| 259 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/portfolio/tax/SettlementTaxReadRepository.kt:9 | L3 | 출력 포트가 application/portfolio/port 밖(tax 패키지)에 있음 | med | med (반증 있음) |
| 260 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/portfolio/TaxSummaryBatchService.kt:31 | E7 | 연말 일괄 루프가 실패 항목·성공 개수를 남기지 않고 첫 실패에서 전체 중단 | med | med (반증 있음) |
| 261 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/portfolio/TaxSummaryCommandService.kt:68 | E2 | 무결성 위반 예외를 cause 없이 «이미 실행 중»으로 바꿈(detekt SwallowedException) | med | med (반증 있음) |
| 262 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/portfolio/TradingJournalCommandService.kt:10 | L1 | 유스케이스가 AccountRepository·TradingJournalRepository 를 직접 주입받음 | med | med |
| 263 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/portfolio/TradingJournalCommandService.kt:37 | D3 | update 가 command.accountId 를 쓰지 않아 소유권 검사가 빠짐(조회 쪽은 belongsTo 로 검사) | med | med |
| 264 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/portfolio/TradingJournalQueryService.kt:8 | L1 | 조회 유스케이스가 AccountRepository·TradingJournalRepository 를 직접 주입받음 | med | med |
| 265 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/position/PositionExitTriggerOrchestrator.kt:13 | L2 | 유스케이스가 org.springframework.dao 예외 3종을 import 해 잡는다 | med | med |
| 266 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/position/PositionExitTriggerOrchestrator.kt:36 | O6 | 청산 판정 틱의 소요·처리 건수·시세 지연이 계측되지 않음 | med | med |
| 267 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/position/PositionExitTriggerOrchestrator.kt:39 | C2 | 가격 틱마다 ARMED 트리거 전부를 잠금 조회하고 미발동 틱도 UPDATE | med | med |
| 268 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/position/PositionExitTriggerOrchestrator.kt:45 | O1 | 잠금 충돌 skip 경로에 로그 0줄 — 파일 전체에 로거 없음 | med | med (반증 있음) |
| 269 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/position/PositionExitTriggerOrchestrator.kt:53 | S1 | processGroup 본문이 빈 줄 제외 약 57줄로 40줄을 넘고 추상화 수준이 셋이다 | med | med (반증 있음) |
| 270 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/account/Account.kt:238 | F3 | updateRiskPolicy 가 인자(existing)를 바꾸면서 새 객체를 돌려줌 | med | med |
| 271 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/account/Account.kt:270 | S3 | Account 가 청산 트리거 기본값의 규칙·쓰기까지 가짐(주인공이 둘) | med | med |
| 272 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/account/Account.kt:285 | F3 | updateExitTriggerDefault 가 인자(existing)를 바꾸면서 같은 객체를 돌려줌 | med | med |
| 273 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/account/Account.kt:291 | S6 | Account 가 다른 객체(existing)의 속성을 읽고 판단한 뒤 그 객체의 필드를 직접 씀 | med | med |
| 274 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/account/AccountLedger.kt:25 | M4 | 원장 목록의 정렬·기간 열(created_at)에 계좌별 복합 인덱스가 없음 | med | med |
| 275 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/account/AccountLedger.kt:40 | D4 | 원장이 Account 애그리거트 루트 객체를 직접 참조 | med | med |
| 276 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/account/RiskEvent.kt:28 | D4 | 리스크 이벤트가 Account·RiskPolicy 루트 객체를 직접 참조 | med | med |
| 277 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/notification/Notification.kt:35 | M4 | 알림 재시도 스캔 열(status·attempt_count)과 FK(account_id)에 인덱스가 없음 | med | med |
| 278 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/notification/Notification.kt:95 | D2 | 상태 전이 메서드가 이전 상태를 검사하지 않는다 | med | med |
| 279 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/order/Execution.kt:27 | M4 | executions 의 FK 열(order_id·account_id)과 정렬 열(executed_at)에 인덱스가 없음 | med | med |
| 280 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/order/Order.kt:181 | D5 | GTD 주문의 expireAt 필수 불변식이 팩토리에 없고 서비스에만 있음 | med | med |
| 281 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/portfolio/SnapshotJobRun.kt:53 | D2 | complete·fail 이 이전 상태를 검사하지 않아 SUCCESS 가 FAILED 로 뒤집힐 수 있음 | med | med |
| 282 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/portfolio/TaxSummaryRun.kt:28 | D4 | 다른 애그리거트(Account)를 @ManyToOne 객체 참조로 가짐 | med | med (반증 있음) |
| 283 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/portfolio/TaxSummaryRun.kt:59 | D2 | completeSuccess·fail 이 이전 상태를 검사하지 않음 | med | med |
| 284 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/portfolio/TaxSummaryRun.kt:65 | D5 | fail(message) 가 오류 메시지를 500자로 자르지 않아 컬럼 길이(500)를 넘을 수 있음 | med | med |
| 285 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/position/PositionExitTrigger.kt:161 | D3 | 가격 기준 정책 검증이 도메인과 CommandService 에 두 벌이고 유효가격 계산은 Evaluator 에 있다 | med | med |
| 286 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/strategy/StrategyLog.kt:24 | M4 | 계속 쌓이는 전략 로그의 strategy_id·정렬 열에 인덱스가 없음 | med | med |
| 287 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/strategy/StrategyPerformanceSnapshot.kt:22 | M3 | 전략·기간별 성과 스냅샷에 유일 제약이 없어 같은 기간 행이 중복될 수 있음 | med | med |
| 288 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/strategy/StrategyPerformanceSnapshot.kt:28 | D4 | StrategyPerformanceSnapshot 이 Strategy 루트를 객체로 참조 | med | med |
| 289 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/strategy/StrategyVersion.kt:33 | D4 | StrategyVersion 이 Strategy 루트를 객체로 참조 | med | med |
| 290 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/kis/KisAccountBalanceAdapter.kt:34 | S1 | fetchBalance 본문이 빈 줄 제외 48줄이고 추상화 수준이 여럿 | med | med (반증 있음) |
| 291 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/kis/KisAccountBalanceAdapter.kt:58 | O5 | KIS REST 호출의 소요 시간·결과가 기록되지 않음 | med | med (반증 있음) |
| 292 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/kis/KisExecutionWebSocketManager.kt:141 | L6 | 기동 이벤트·웹소켓 수신 진입점이 infrastructure 에 있고 유스케이스로 직결 | med | med |
| 293 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/kis/KisExecutionWebSocketManager.kt:202 | A4 | 재연결 횟수 갱신이 원자적이지 않고 한 장애에 재연결이 중복 예약됨 | med | med |
| 294 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/kis/KisOrderRestClient.kt:30 | O5 | KIS REST 호출의 소요 시간·결과가 기록되지 않음 | med | med |
| 295 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/kis/KisOrderRestClient.kt:126 | E1 | 체결 조회 실패를 빈 목록으로 돌려줌 — '체결 없음' 과 구분 안 됨 | med | med |
| 296 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/kis/KisTokenManager.kt:32 | F2 | get* 이름 함수가 KIS 발급 POST 와 Redis 쓰기를 숨김 | med | med |
| 297 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/kis/KisTokenManager.kt:35 | A6 | 토큰 발급 중복 방지가 프로세스 안 락뿐 — Redis 공유 키는 보호 안 됨 | med | med |
| 298 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/notification/NotificationPersistenceTxService.kt:14 | X5 | AFTER_COMMIT 리스너가 부르는 REQUIRES_NEW 결과 기록이 원본 id 로 중복을 거르지 않음 | med | med |
| 299 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/notification/SlackNotificationEventHandler.kt:29 | X11 | 핸들러가 PENDING 행을 만든 뒤 점유(PENDING→DELIVERING) 없이 전송해 스케줄러와 중복 전송 가능 | med | med |
| 300 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/notification/SlackNotificationScheduler.kt:17 | C1 | Slack 재시도 스케줄러가 매칭 스케줄러와 단일 스레드 스케줄러를 공유하며 건당 블로킹 호출 | med | med |
| 301 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/notification/SlackNotificationScheduler.kt:21 | I5 | DELIVERING 으로 점유된 행을 되돌리는 경로가 없어 프로세스 중단 시 알림이 영구 미발송 | med | med (반증 있음) |
| 302 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/notification/SlackWebhookNotifier.kt:26 | E3 | Slack 웹훅 호출 실패를 Exception 전체로 잡아 Boolean 하나로 뭉갬 | med | med (반증 있음) |
| 303 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/ExecutionRepositoryCustom.kt:8 | L3 | 사실상 출력 포트인 인터페이스가 port 밖에 있고 시그니처가 port 밖 application 타입을 씀 | med | med |
| 304 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/OrderRepository.kt:38 | D3 | 「미체결」 상태 집합이 기본 인자로 또 복사됨 (LLM-only) | med | med |
| 305 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/OrderRepositoryCustom.kt:10 | L3 | 사실상 출력 포트인 인터페이스가 port 밖에 있고 시그니처가 port 밖 application 타입을 씀 | med | med |
| 306 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/OrderRepositoryImpl.kt:51 | D3 | 「미체결(PENDING·PARTIAL)」 규칙이 쿼리마다 복사됨 (LLM-only) | med | med |
| 307 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/redis/QuoteEventListener.kt:34 | E2 | 매칭·포지션 갱신 실패 로그가 예외 객체 없이 메시지만 남김 | med | med (반증 있음) |
| 308 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/redis/RedisListenerConfig.kt:12 | L6 | Redis 구독 진입점이 infrastructure 에서 등록됨 | med | med |
| 309 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/redis/RedisLivePositionCacheAdapter.kt:60 | S10 | Redis 해시 파싱을 runCatching{...}.getOrNull() 로 감싸 필드 누락·형식 오류를 구분 없이 null 로 삼킴 | med | med |
| 310 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/redis/RedisMarketQuoteAdapter.kt:40 | S10 | runCatching getOrNull 로 필드 누락·형식 오류를 시세 없음으로 바꿈 | med | med |
| 311 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/scheduler/LocalMatchingScheduler.kt:16 | L6 | @Scheduled 진입점이 infrastructure 에 있고 유스케이스를 직접 부름 (LLM-only) | med | med |
| 312 | backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/controller/AccountController.kt:46 | L5 | 컨트롤러가 서비스가 돌려준 도메인 엔티티를 그대로 받아 필드를 읽고 DTO 로 매핑함 | med | med |
| 313 | backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/controller/AccountController.kt:90 | S7 | 인증·소유 범위 없이 계좌 id 로 입출금·원장 조회 | med | med |
| 314 | backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/controller/KisAccountBalanceController.kt:16 | S7 | 인증 없이 클라이언트가 준 accountId 로 KIS 실계좌 잔고를 조회 | med | med |
| 315 | backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/controller/OrderController.kt:87 | S7 | 주문 취소가 주문 id 만으로 조회하고 계좌 범위를 같은 쿼리에 넣지 않음 | med | med |
| 316 | backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/controller/PositionController.kt:15 | S7 | 경로의 accountId 를 호출자 범위 검증 없이 그대로 조회 | med | med |
| 317 | backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/controller/PositionExitTriggerController.kt:26 | I1 | 청산 트리거 생성 POST 에 멱등 키도 자연 키 유일 제약도 없음 | med | med |
| 318 | backend/trading-api/src/main/kotlin/com/papertrading/api/TradingApiApplication.kt:11 | C1 | @Scheduled 작업 둘이 기본 단일 스레드 스케줄러를 공유함 | med | med |
| 319 | backend/trading-api/src/main/resources/application.yaml:30 | E5 | Redis 연결·명령 타임아웃이 명시되지 않음 | med | med |
| 320 | backend/trading-api/src/main/resources/db/migration/V6__create_account_exit_trigger_defaults.sql:8 | M6 | 마이그레이션은 시각 열을 TIMESTAMP(시간대 없음)로 선언하고 엔티티는 Instant 를 씀 | med | med |
| 321 | backend/trading-api/src/main/resources/db/migration/V7__create_position_exit_triggers.sql:1 | M9 | V7 이 근거 주석 없이 테이블을 DROP 하고, 적용된 마이그레이션을 제자리에서 고친 흔적 | med | med |
| 322 | backend/trading-api/src/test/kotlin/com/papertrading/api/application/account/KisAccountQueryServiceTest.kt:25 | T2 | 정합성 시험이 PositionRepository 목으로 수량 조건 쿼리를 흉내 냄 | med | med |
| 323 | backend/trading-api/src/test/kotlin/com/papertrading/api/application/account/RiskPolicyServiceTest.kt:63 | T3 | 새 정책 생성 단언이 save(any()) 호출 횟수뿐임 | med | med |
| 324 | backend/trading-api/src/test/kotlin/com/papertrading/api/application/order/OrderCommandServiceTest.kt:100 | T8 | BigDecimal 단언이 assertEquals 로 scale 까지 비교 | med | med (반증 있음) |
| 325 | backend/trading-api/src/test/kotlin/com/papertrading/api/application/portfolio/SnapshotJobServiceTest.kt:71 | T3 | 내부 협력자 호출 횟수 verify 와 @Transactional 리플렉션 단언이 결과 단언을 대신함 | med | med |
| 326 | backend/trading-api/src/test/kotlin/com/papertrading/api/application/portfolio/tax/TaxSummaryCommandServiceTest.kt:96 | T2 | 리포지토리 목이 실행 중 조회·유일 제약 위반을 흉내 냄 | med | med (반증 있음) |
| 327 | backend/trading-api/src/test/kotlin/com/papertrading/api/application/portfolio/tax/TaxSummaryCommandServiceTest.kt:121 | T3 | 실패·배치 경로 시험의 유일한 단언이 리포지토리 save 호출 횟수·인자 verify | med | med |
| 328 | backend/trading-api/src/test/kotlin/com/papertrading/api/application/portfolio/TradingJournalCommandQueryServiceTest.kt:66 | T6 | 시험 이름이 약속한 정책(동일 계좌만 수정)을 시험하지 않고 다음 시험이 반대 동작을 고정함 | med | med |
| 329 | backend/trading-api/src/test/kotlin/com/papertrading/api/application/position/PositionExitTriggerCommandServiceTest.kt:56 | T8 | BigDecimal 결과를 assertEquals 로 scale 까지 비교 | med | med |
| 330 | backend/trading-api/src/test/kotlin/com/papertrading/api/application/position/PositionExitTriggerOrchestratorTest.kt:37 | T1 | 내부 서비스 AutoExitOrderPlacementService·Slack 발행기와 도메인 엔티티 Order 를 mockk 로 갈음 | med | med |
| 331 | backend/trading-api/src/test/kotlin/com/papertrading/api/domain/entity/account/AccountTest.kt:24 | T8 | BigDecimal 단언 54곳이 assertEquals 로 scale 까지 비교 | med | med |
| 332 | backend/trading-api/src/test/kotlin/com/papertrading/api/domain/entity/strategy/StrategyDomainEntitiesTest.kt:18 | T5 | 전략 리포지토리 5개에 실제 엔진(Testcontainers) 시험이 없음 | med | med |
| 333 | backend/trading-api/src/test/kotlin/com/papertrading/api/domain/entity/strategy/StrategyDomainEntitiesTest.kt:206 | T8 | BigDecimal 단언이 assertEquals(scale 까지 비교) | med | med |
| 334 | backend/trading-api/src/test/kotlin/com/papertrading/api/infrastructure/kis/KisAccountBalanceAdapterTest.kt:49 | T11 | KIS 응답 JSON·오류 코드를 출처 없이 손으로 적은 가짜 | med | med |
| 335 | backend/trading-api/src/test/kotlin/com/papertrading/api/infrastructure/kis/KisExecutionWebSocketManagerTest.kt:35 | T1 | 내부 파서·application 서비스를 relaxed 목으로 갈음 — 수신 경로 미시험 | med | med |
| 336 | backend/trading-api/src/test/kotlin/com/papertrading/api/infrastructure/notification/SlackNotificationEventHandlerTest.kt:21 | T1 | 같은 레포의 영속 헬퍼(NotificationPersistenceTxService)를 목으로 갈음해 저장 결과를 지어냄 | med | med |
| 337 | backend/trading-api/src/test/kotlin/com/papertrading/api/infrastructure/notification/SlackNotificationEventHandlerTest.kt:94 | T3 | 결과 단언 없이 내부 협력자 호출 횟수·순서(verify·verifyOrder)만 단언 | med | med |
| 338 | backend/trading-api/src/test/kotlin/com/papertrading/api/infrastructure/notification/SlackNotificationEventHandlerTest.kt:100 | T10 | AFTER_COMMIT 리스너를 직접 호출하고 영속은 목이라 «PENDING 유지»가 인메모리 값에 대한 단언이 됨 | med | med |
| 339 | backend/trading-api/src/test/kotlin/com/papertrading/api/infrastructure/notification/SlackNotificationSchedulerTest.kt:15 | T1 | 영속 헬퍼 목이 선점 성공·실패 결과를 지어냄 | med | med |
| 340 | backend/trading-api/src/test/kotlin/com/papertrading/api/infrastructure/notification/SlackNotificationSchedulerTest.kt:84 | T3 | 내부 협력자 호출 횟수와 인자(오류 메시지 문자열)를 결과 단언 대신 못 박음 | med | med |
| 341 | backend/trading-api/src/test/kotlin/com/papertrading/api/presentation/controller/ExecutionSseControllerTest.kt:28 | T1 | 의존 없는 내부 레지스트리를 목으로 갈음하고 등록 호출은 단언하지 않음 | med | med |
| 342 | backend/trading-api/src/test/kotlin/com/papertrading/api/presentation/controller/KisAccountBalanceControllerIntegrationTest.kt:38 | T1 | 같은 레포 application 서비스(KisAccountQueryService)를 @MockitoBean 으로 갈음하고 던질 예외도 시험이 지어낸다 | med | med |
| 343 | backend/trading-api/src/test/kotlin/com/papertrading/api/presentation/controller/OrderControllerIntegrationTest.kt:57 | T9 | 실제 DB 시험이 행을 정리하지 않고 종목 전체 매칭을 호출 — 순서 의존 위험 | med | med |
| 344 | backend/trading-api/src/test/kotlin/com/papertrading/api/presentation/controller/PositionControllerIntegrationTest.kt:217 | T7 | «avgBuyPrice=0 이면 returnRate null» 시험이 시세 없음만으로도 통과하는 가짜 초록 | med | med |
| 345 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/ExecutionProcessor.kt:97 | X7 | 한 체결 트랜잭션이 Order·Account·Position·Settlement 불변식을 함께 바꿈 | med | low (반증 있음) |
| 346 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/LocalMatchingEngine.kt:25 | A2 | 같은 종목의 시세 처리가 직렬화되지 않음 | med | low (반증 있음) |
| 347 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/LocalMatchingEngine.kt:25 | O6 | 시세 처리 지연·매칭 틱 소요가 전혀 계측되지 않음 | med | low (반증 있음) |
| 348 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/portfolio/DailyBalanceCommandService.kt:27 | D3 | 총자산·손익·손익률 산식이 서비스에 있고 DailyBalance 는 계산 결과만 받아 저장 | med | low (반증 있음) |
| 349 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/portfolio/PortfolioSnapshotCommandService.kt:29 | X3 | 계좌 행 락을 쥔 트랜잭션 안에서 종목마다 Redis 시세를 읽는다 | med | low (반증 있음) |
| 350 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/portfolio/PortfolioSnapshotCommandService.kt:33 | D3 | 평가액·미실현손익 산식이 서비스에 있고 Position·PortfolioSnapshot 에는 규칙 메서드가 없음 | med | low (반증 있음) |
| 351 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/portfolio/TaxSummaryBatchService.kt:31 | X12 | 계좌마다 따로 커밋하는 일괄이 첫 실패에서 전체를 중단하고 부분 상태로 남는다 | med | low (반증 있음) |
| 352 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/portfolio/TaxSummaryCommandService.kt:105 | E2 | IllegalArgumentException 을 cause 없이 «지원하지 않는 통화»로 오분류 | med | low (반증 있음) |
| 353 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/position/PositionExitTriggerOrchestrator.kt:39 | A2 | 정렬 없는 그룹 순서로 한 트랜잭션에서 포지션 행 잠금을 누적 — 동시 onQuote 교착 가능 | med | low |
| 354 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/position/PositionExitTriggerOrchestrator.kt:107 | X12 | 그룹마다 REQUIRES_NEW 로 주문을 먼저 커밋하고 트리거 상태는 바깥 트랜잭션에서 나중에 반영 — 바깥 롤백 시 주문만 남고 트리거는 ARMED | med | low (반증 있음) |
| 355 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/position/PositionExitTriggerOrchestrator.kt:139 | E6 | 대기·백오프·지터 없는 즉시 3회 재시도를 잠금 보유 중에 실행 | med | low (반증 있음) |
| 356 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/position/PositionExitTriggerOrchestrator.kt:155 | E2 | 중복 키 실패 알림이 예외 객체 없이 ex.message 문자열만 사용 | med | low (반증 있음) |
| 357 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/account/Account.kt:65 | M3 | KIS 계좌 번호(external_account_id)에 UNIQUE 가 없고 중복 확인도 없음 | med | low |
| 358 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/account/AccountLedger.kt:50 | M11 | 수수료(FEE) 원장 행에 계산에 쓴 요율·정책 버전이 저장되지 않음 | med | low (반증 있음) |
| 359 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/account/RiskPolicy.kt:32 | D4 | 리스크 정책이 Account 루트 객체를 직접 참조(자기 리포지토리 있음) | med | low |
| 360 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/feepolicy/FeePolicy.kt:23 | M3 | 수수료 정책의 자연 키(모드·시장·시작 시각) 유일 제약과 기간 겹침 방지가 없음 | med | low (반증 있음) |
| 361 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/notification/Notification.kt:45 | M3 | 알림의 원본 참조(source_ref)에 UNIQUE 제약이 없음 | med | low (반증 있음) |
| 362 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/portfolio/DailyBalance.kt:79 | D5 | create·refresh 에 불변식 검증이 없어 totalAssetValue ≠ cash + stock 인 행도 만들 수 있음 | med | low |
| 363 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/portfolio/PortfolioSnapshot.kt:94 | D5 | create·refresh 에 수량·가격·비중 범위 검증이 없음 | med | low |
| 364 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/portfolio/TaxSummary.kt:68 | M8 | 세금 요약 상태에 전이 정의가 없고 재계산이 FINAL 을 DRAFT 로 되돌려 덮어쓴다 | med | low (반증 있음) |
| 365 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/position/Position.kt:41 | D3 | Position 규칙(폐쇄 여부·평단 기준 임계가)이 Position 이 아니라 서비스 여러 곳에 있다 | med | low (반증 있음) |
| 366 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/settlement/SettlementExecution.kt:35 | D4 | SettlementExecution 이 Execution 루트를 객체로 참조 | med | low (반증 있음) |
| 367 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/kis/KisAccountBalanceAdapter.kt:65 | O1 | KIS 호출 실패가 다시 던져진 뒤 어디서도 로그되지 않음 | med | low (반증 있음) |
| 368 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/notification/SlackNotificationConfigStore.kt:14 | A5 | 운영자가 PUT 으로 바꾸는 알림 정책을 프로세스 메모리에만 보관 | med | low (반증 있음) |
| 369 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/notification/SlackNotificationEventHandler.kt:21 | L6 | 알림 전송 흐름 전체가 infrastructure 리스너에 있다 | med | low (반증 있음) |
| 370 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/notification/SlackNotificationEventHandler.kt:22 | A3 | 알림 실행기의 CallerRunsPolicy 로 큐가 차면 주문·체결 스레드가 Slack 호출에 묶임 | med | low (반증 있음) |
| 371 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/AccountLedgerRepositoryCustom.kt:7 | L3 | 유스케이스가 쓰는 조회 포트가 infrastructure 에 있고 시그니처가 port 밖 타입을 씀 | med | low (반증 있음) |
| 372 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/NotificationRepository.kt:10 | Q5 | 재시도 대상 PENDING 알림을 상한 없이 전부 읽어 30초마다 직렬 처리 | med | low (반증 있음) |
| 373 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/NotificationRepositoryImpl.kt:16 | D2 | PENDING→DELIVERING 전이의 이전 상태 검사가 어댑터 쿼리에만 있고 애그리거트 메서드에는 없음 | med | low (반증 있음) |
| 374 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/PositionRepository.kt:32 | D2 | 매도 체결 시 Position 상태를 JPQL UPDATE 가 직접 대입해 애그리거트 메서드(applySell)를 우회 | med | low (반증 있음) |
| 375 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/PositionRepositoryImpl.kt:20 | D4 | 포지션 조회가 Account 애그리거트를 fetch join 하고 Account 속성으로 포지션을 거름 | med | low (반증 있음) |
| 376 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/redis/QuoteEventListener.kt:32 | O6 | 시세 처리 지연·적체·누락이 계측되지 않음 | med | low (반증 있음) |
| 377 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/redis/QuoteEventListener.kt:34 | A2 | 같은 종목 시세 처리가 직렬화되지 않고, 낡은 시세를 버리는 비교도 없음 | med | low (반증 있음) |
| 378 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/redis/QuoteEventListener.kt:53 | E1 | 시세 메시지 필드 누락·알 수 없는 mode 가 로그 없이 버려짐 | med | low (반증 있음) |
| 379 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/redis/RedisMarketQuoteAdapter.kt:31 | E5 | Redis 시세 조회에 명시 타임아웃 설정이 없음 | med | low |
| 380 | backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/controller/ExecutionSseController.kt:25 | C4 | SSE 연결 수 상한이 없음 (제거 콜백은 있음) | med | low (반증 있음) |
| 381 | backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/controller/ExecutionSseController.kt:28 | S7 | 체결 SSE 스트림이 호출자·계좌 범위 없이 모든 계좌의 체결을 모든 연결에 전달함 | med | low (반증 있음) |
| 382 | backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/controller/OrderController.kt:49 | I1 | 주문 멱등 키 선조회가 계좌 락보다 앞에 있어, 같은 키 동시 재시도가 재생 응답이 아니라 유일 제약 위반 500 으로 끝날 수 있음 | med | low |
| 383 | backend/trading-api/src/main/kotlin/com/papertrading/api/TradingApiApplication.kt:12 | A3 | @EnableAsync 에 기본 실행기 지정이 없어 한정자 없는 @Async 가 어느 풀을 쓰는지 불분명 | med | low |
| 384 | backend/trading-api/src/main/resources/application.yaml:39 | S4 | KIS 자격 증명 기본값이 빈 문자열이라 누락이 첫 호출에야 드러날 수 있음 | med | low |
| 385 | backend/trading-api/src/test/kotlin/com/papertrading/api/application/portfolio/SnapshotJobServiceAtomicRollbackIntegrationTest.kt:52 | T1 | 통합 시험에서 내부 서비스를 @MockitoBean 으로 갈음 | med | low (반증 있음) |
| 386 | backend/trading-api/src/test/kotlin/com/papertrading/api/application/position/PositionExitQuoteEventListenerTest.kt:12 | T1 | 내부 협력자 PositionExitTriggerOrchestrator 를 relaxed mockk 로 갈음 | med | low (반증 있음) |
| 387 | backend/trading-api/src/test/kotlin/com/papertrading/api/application/position/PositionExitTriggerOrchestratorTest.kt:171 | T10 | 영속 예외를 목이 직접 던지게 해 @Transactional 경계에서의 예외 번역·커밋 경로를 우회 | med | low |
| 388 | backend/trading-api/src/test/kotlin/com/papertrading/api/application/position/PositionQueryServiceTest.kt:91 | T11 | collector-api 가 쓰는 Redis 시세 해시 키·필드를 시험이 손으로 베낌 | med | low |
| 389 | backend/trading-api/src/test/kotlin/com/papertrading/api/infrastructure/kis/KisExecutionNoticeParserTest.kt:18 | T11 | 손으로 쓴 KIS 체결통보 원문의 출처가 시험에 없음 | med | low |
| 390 | backend/trading-api/src/test/kotlin/com/papertrading/api/infrastructure/kis/KisOrderRestClientTest.kt:43 | T11 | 손으로 지은 KIS 오류 응답(rt_cd·msg1)의 출처가 시험에 없음 | med | low |
| 391 | backend/trading-api/build.gradle:74 | S11 | detekt 가 기본 임계(LongMethod 60·NestedBlockDepth 4)만 쓰고 팀 수치(40·3)로 조이는 설정이 없음 | low | high |
| 392 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/account/command/UpsertAccountExitTriggerDefaultCommand.kt:7 | N3 | Boolean 필드 enabled 에 is 접두가 없음 | low | high |
| 393 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/account/result/AccountExitTriggerDefaultResult.kt:8 | N3 | Boolean 필드 enabled 에 is 접두가 없음 | low | high |
| 394 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/notification/SlackNotificationService.kt:8 | L2 | application 이 http 패키지 import 를 달고 있다 | low | high |
| 395 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/ExecutionProcessor.kt:39 | S4 | 생성자 주입 의존 11개 | low | high |
| 396 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/ExecutionProcessor.kt:59 | N6 | 공개 함수 인자 이름의 약어 qty (fillQty ×3) | low | high |
| 397 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/ExecutionProcessor.kt:88 | X1 | 여러 save 를 부르는 public 메서드 processFill 에 @Transactional 이 없음 | low | high |
| 398 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/KisExecutionNoticeService.kt:22 | E5 | 같은 체결번호 중복 조회를 handle 과 processFill 이 연달아 수행 | low | high |
| 399 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/OrderCommandService.kt:36 | S4 | 생성자 주입 의존 9개 | low | high |
| 400 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/OrderCommandService.kt:180 | S5 | 인자 6개 함수(createGroupedAutoExitSellOrder) | low | high |
| 401 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/portfolio/SnapshotJobService.kt:60 | E5 | 한 작업 안에서 계좌 락 조회 3번·포지션 조회 2번을 같은 조건으로 반복 | low | high |
| 402 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/portfolio/tax/TaxSummaryCalculator.kt:5 | L2 | 순수 계산기에 @Component 를 달아 허용 목록 밖 Spring 을 import | low | high |
| 403 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/portfolio/TradingJournalQueryService.kt:29 | D6 | 같은 타입 id 인자 둘(journalId, accountId)이 나란히 옴 | low | high |
| 404 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/position/AutoExitOrderPlacementService.kt:17 | S5 | createGroupedAutoExitSellOrder 인자가 6개다 | low | high |
| 405 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/position/PositionExitTriggerCommandService.kt:89 | D6 | 같은 두 id 를 받는 함수 둘의 인자 순서가 서로 반대다 | low | high |
| 406 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/position/PositionQueryService.kt:45 | E5 | 단건 조회에서 같은 캐시 조회를 두 번 수행 | low | high |
| 407 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/position/result/EffectivePositionExitTriggerResult.kt:1 | N7 | 파일 이름과 안의 타입 이름이 다르고 그 별칭은 쓰이지 않는다 | low | high |
| 408 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/account/Account.kt:165 | D6 | 같은 타입 id 인자(refOrderId, refExecutionId)가 나란히 옴 | low | high |
| 409 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/account/Account.kt:335 | S5 | Account.create 인자가 6개 | low | high |
| 410 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/account/AccountExitTriggerDefault.kt:24 | N3 | Boolean 속성 enabled 에 is 접두가 없음 | low | high |
| 411 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/account/RiskEvent.kt:61 | F4 | 도메인 팩토리가 Instant.now() 를 직접 읽음 | low | high |
| 412 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/feepolicy/FeePolicy.kt:94 | S5 | FeePolicy.create 인자 7개 | low | high |
| 413 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/order/Execution.kt:96 | S5 | Execution.create 인자 12개 | low | high |
| 414 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/order/Order.kt:134 | N6 | 공개 함수 인자 이름의 약어 qty (executedQty) | low | high |
| 415 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/order/OrderAmendment.kt:57 | N11 | 정정 유형 MODIFY·CANCEL 을 문자열 리터럴로 반복 | low | high |
| 416 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/order/OrderAmendment.kt:83 | S5 | OrderAmendment.create 인자 7개, modify 인자 6개 | low | high |
| 417 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/order/OrderSignal.kt:103 | S5 | OrderSignal.create 인자 12개 | low | high |
| 418 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/position/Position.kt:119 | S5 | createWithHolding 인자가 8개다 | low | high |
| 419 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/position/Position.kt:150 | N6 | 함수 밖으로 나가는 인자 이름에 qty 약어가 있다(executedQty·qty) | low | high |
| 420 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/settlement/Settlement.kt:103 | N11 | 통화 코드 "KRW" 리터럴을 비교·기본값에 반복 | low | high |
| 421 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/strategy/StrategyLog.kt:64 | S5 | StrategyLog.create 인자가 6개 | low | high |
| 422 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/strategy/StrategyPerformanceSnapshot.kt:77 | S5 | StrategyPerformanceSnapshot.create 인자가 8개, validateMetrics 가 6개 | low | high |
| 423 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/strategy/StrategyVersion.kt:68 | S5 | StrategyVersion.create 인자가 6개 | low | high |
| 424 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/kis/KisExecutionNotice.kt:15 | N6 | 공개 속성 이름 executedQty 가 허용 목록 밖 약어 | low | high |
| 425 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/kis/KisExecutionNoticeParser.kt:86 | N11 | 체결통보 채널 ID·구분 코드를 파서에 리터럴로 중복 | low | high |
| 426 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/kis/KisExecutionWebSocketManager.kt:153 | N3 | Boolean 인자 resetAttempts 가 is·should 접두 없는 동사구이고 항상 false | low | high |
| 427 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/kis/KisOrderProperties.kt:26 | N11 | mode == "paper" 비교가 속성 접근 함수 6개에 반복 | low | high |
| 428 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/kis/KisOrderRestClient.kt:33 | N11 | 모드("paper"/"live")·성공 코드("0")를 문자열로 반복 비교 | low | high |
| 429 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/ExecutionRepositoryCustom.kt:9 | D6 | 같은 타입 Long id 세 개가 나란히 옴 | low | high |
| 430 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/OrderRepositoryCustom.kt:11 | D6 | 같은 타입 Long id 둘이 나란히 옴 | low | high |
| 431 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/OrderRepositoryImpl.kt:51 | Q7 | «활성 주문(PENDING·PARTIAL)» 필터를 여러 곳이 따로 작성 | low | high |
| 432 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/PositionExitTriggerRepository.kt:22 | N9 | 락 타임아웃 "3000" 이 단위 없는 문자열 리터럴로 두 번 복제됨 | low | high |
| 433 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/PositionRepository.kt:48 | N6 | executedQty — 허용 목록 밖 약어 qty 를 함수 인자·JPQL 파라미터 이름으로 사용 | low | high |
| 434 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/SettlementTaxReadRepositoryImpl.kt:3 | L4 | infrastructure 어댑터가 port 밖 application 패키지를 import (L3 의 결과) | low | high |
| 435 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/TradingJournalRepositoryImpl.kt:3 | L4 | Querydsl 구현이 port 밖 application 타입(TradingJournalFilter)을 import (Custom 인터페이스와 같은 원인) | low | high |
| 436 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/redis/RedisMarketQuoteAdapter.kt:40 | E3 | runCatching 이 Throwable 전체를 잡음(좁은 예외가 알려진 자리) | low | high |
| 437 | backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/controller/AccountController.kt:136 | Q6 | 원장 목록 page 인자에 최댓값 검증이 없음 | low | high |
| 438 | backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/dto/notification/SlackNotificationDtos.kt:36 | S8 | 요청 DTO 에 재시도 횟수·간격·메시지 길이의 상한 검증이 없음 | low | high |
| 439 | backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/dto/order/OrderRequest.kt:13 | N7 | 파일 이름(OrderRequest.kt)과 유일한 최상위 선언(PlaceOrderRequest) 이름 불일치 | low | high |
| 440 | backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/dto/portfolio/TradingJournalCreateRequest.kt:15 | N11 | sentiment 허용값이 enum 없이 정규식 문자열로 두 DTO 에 중복됨 | low | high |
| 441 | backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/dto/portfolio/TradingJournalUpdateRequest.kt:12 | N11 | sentiment 허용값 정규식이 Create 요청 DTO 와 중복됨 | low | high |
| 442 | backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/exception/GlobalExceptionHandler.kt:22 | N11 | 에러 코드·상태 값이 상수 없이 리터럴로 흩어지고 메시지 부분 문자열로 판정함 | low | high |
| 443 | backend/trading-api/src/test/kotlin/com/papertrading/api/application/position/PositionExitTriggerCommandServiceTest.kt:50 | T12 | 트리거 생성 시험 하나에 단언 10개 | low | high |
| 444 | backend/trading-api/src/test/kotlin/com/papertrading/api/domain/entity/feepolicy/FeePolicyTest.kt:25 | T8 | BigDecimal 단언이 assertEquals 로 scale 까지 비교 | low | high |
| 445 | backend/trading-api/src/test/kotlin/com/papertrading/api/domain/entity/order/OrderTest.kt:115 | T8 | BigDecimal 단언이 assertEquals 로 scale 까지 비교 (4곳) | low | high |
| 446 | backend/trading-api/src/test/kotlin/com/papertrading/api/domain/entity/strategy/StrategyDomainEntitiesTest.kt:102 | T12 | 한 시험이 대상 호출 여러 번에 각자 단언을 붙임 | low | high |
| 447 | backend/trading-api/src/test/kotlin/com/papertrading/api/presentation/controller/TradingJournalControllerIntegrationTest.kt:43 | T12 | 시험 하나가 대상 호출 5번에 각자 단언을 달음 | low | high |
| 448 | backend/trading-api/src/test/kotlin/com/papertrading/api/presentation/controller/TradingJournalControllerIntegrationTest.kt:44 | T6 | 시험 이름이 «흐름이 동작한다» 뿐이라 상황·행동·기대가 없음 | low | high |
| 449 | (repo) | N1 | 한 번의 체결을 execution 과 fill 두 용어로 부름 | low | med |
| 450 | (repo) | N1 | 청산 트리거를 PositionExitTrigger·PositionTrigger·Trigger 로 섞고, 임계가를 triggerPrice·thresholdPrice·entryPrice 로 부름 | low | med |
| 451 | (repo):20 | O11 | 운영 로그 형식(JSON 구조 로그)·로그 수준·느린 쿼리 로그 설정이 없음 | low | med |
| 452 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/account/port/KisAccountBalancePort.kt:7 | N6 | 포트 인자 이름 trId 가 허용 목록 밖 약어 | low | med |
| 453 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/common/result/QuoteSnapshot.kt:11 | N6 | 호가 속성 이름이 허용 목록 밖 약어 (askp1·bidp1) | low | med |
| 454 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/notification/command/UpdateSlackNotificationConfigCommand.kt:6 | N3 | Boolean 속성에 is 접두가 없다 | low | med |
| 455 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/notification/port/SlackNotificationRuntimeConfig.kt:7 | N3 | Boolean 속성 두 개에 is 접두가 없다 | low | med |
| 456 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/notification/result/SlackNotificationConfigResult.kt:8 | N3 | Boolean 속성 두 개에 is 접두가 없다 | low | med |
| 457 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/notification/SlackNotificationEventPublisher.kt:14 | F4 | 기본 인자가 시각을 직접 읽는다 | low | med |
| 458 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/command/PlaceOrderCommand.kt:21 | D6 | 같은 타입 id strategyId·signalId 가 나란히 옴 (PlaceOrderCommand) | low | med |
| 459 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/KisPaperOrderExecutor.kt:47 | E1 | KIS_PAPER 취소 실패를 WARN 로그로만 남기고 성공처럼 반환 | low | med |
| 460 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/KisPaperOrderExecutor.kt:61 | N2 | pollFills() 가 폴링을 약속하지만 아무 일도 하지 않음 | low | med |
| 461 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/LocalMatchingEngine.kt:36 | D3 | 잔여 수량 식이 Order 에 없어 여러 곳에 복사됨 | low | med |
| 462 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/OrderCommandService.kt:30 | N12 | 자동 청산 트리거 개념의 입력 타입이 주문 서비스 파일 안에 있고 이름은 position 쪽 용어 | low | med |
| 463 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/OrderCommandService.kt:49 | X7 | 주문 접수 한 트랜잭션이 Order·Account(홀드)·Position(수량 락)을 함께 바꿈 | low | med |
| 464 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/OrderCommandService.kt:189 | S3 | 주문 서비스가 자동 청산 트리거 멱등 키 규칙과 계좌·포지션 잠금 해제까지 가짐 | low | med |
| 465 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/OrderCommandService.kt:239 | D6 | 같은 타입 id 가 나란한 위치 인자(accountId, orderId) | low | med |
| 466 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/OrderQueryService.kt:31 | D6 | 같은 타입 id 3개가 나란한 위치 인자 | low | med |
| 467 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/portfolio/result/TaxComputationResult.kt:6 | F1 | private 생성자 data class 의 copy() 가 열려 있어 scale 정규화 팩토리를 우회할 수 있음 | low | med |
| 468 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/portfolio/SnapshotJobService.kt:71 | X10 | application 이 Spring 영속 예외 DataIntegrityViolationException 을 직접 잡는다 | low | med (반증 있음) |
| 469 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/portfolio/tax/TaxSettlementAggregate.kt:12 | L3 | 포트 결과 타입이 port 패키지 밖에 있음 | low | med |
| 470 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/portfolio/tax/TaxSummaryCalculator.kt:12 | N11 | 통화 코드 "KRW" 를 문자열 리터럴로 비교 | low | med |
| 471 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/portfolio/TaxSummaryCommandService.kt:68 | X10 | application 이 Spring 영속 예외 DataIntegrityViolationException 을 직접 잡는다 | low | med (반증 있음) |
| 472 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/portfolio/TaxSummaryCommandService.kt:119 | N11 | 통화 코드 "KRW" 를 문자열 리터럴로 비교(계산기와 중복) | low | med |
| 473 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/position/command/UpsertPositionExitTriggerCommand.kt:7 | N7 | 파일 이름이 Upsert 인데 Create·Update·Cancel 명령 셋이 들어 있다 | low | med |
| 474 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/position/PositionExitTriggerCommandService.kt:140 | F1 | percent.divide(HUNDRED) 가 scale·RoundingMode 없이 나눈다 | low | med |
| 475 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/position/PositionExitTriggerEvaluator.kt:70 | F1 | percentRatio 가 scale·RoundingMode 없는 divide(x) 를 쓴다 | low | med |
| 476 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/position/PositionExitTriggerOrchestrator.kt:3 | L8 | notification·order 컨텍스트의 구현 클래스를 직접 import 한다 | low | med |
| 477 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/position/PositionExitTriggerOrchestrator.kt:33 | A4 | 락 맵이 줄지 않고, 모니터를 쥔 채 무제한 DB 락 대기·재시도 | low | med |
| 478 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/position/PositionExitTriggerOrchestrator.kt:187 | S5 | publishOrderCreateFailure 인자가 6개다 | low | med (반증 있음) |
| 479 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/position/PositionQueryService.kt:33 | F2 | get* 조회 경로(getPositionWithCurrentPrice)가 Redis 캐시에 쓴다 | low | med |
| 480 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/position/PositionQueryService.kt:44 | N2 | 부작용이 있는 snapshotFor·snapshotFromDbWithLatestQuote 가 명사구 이름이다 | low | med |
| 481 | backend/trading-api/src/main/kotlin/com/papertrading/api/common/exception/CommonApiExceptions.kt:93 | N12 | 한 컨텍스트 전용 예외 42개가 공유 common 이름·자리에 있음 | low | med (반증 있음) |
| 482 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/account/Account.kt:29 | S4 | Account 의 public 함수가 23개로 표면이 넓음 | low | med (반증 있음) |
| 483 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/account/Account.kt:271 | N3 | Boolean 인자 enabled 에 is 접두가 없음 | low | med |
| 484 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/account/AccountLedger.kt:75 | S5 | AccountLedger.create 인자가 8개 | low | med (반증 있음) |
| 485 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/account/RiskEvent.kt:28 | M4 | risk_events 의 FK(account_id·risk_policy_id)에 인덱스가 없음 | low | med |
| 486 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/account/RiskPolicy.kt:32 | M4 | risk_policies 의 FK(account_id)에 인덱스가 없음 | low | med |
| 487 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/account/RiskPolicy.kt:49 | M3 | «계좌당 활성 정책 1개» 불변식에 DB 부분 유일 제약이 없음 | low | med |
| 488 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/base/BaseAuditEntity.kt:10 | M7 | updated_at 도 DB 는 NOT NULL 인데 Kotlin 타입은 nullable(Instant?) | low | med |
| 489 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/base/BaseTimeEntity.kt:10 | M7 | created_at 은 DB 가 NOT NULL 인데 Kotlin 타입은 nullable(Instant?) | low | med |
| 490 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/feepolicy/FeePolicy.kt:72 | F4 | 도메인 함수 isCurrent() 가 Instant.now() 를 직접 읽음 (호출처 없음) | low | med |
| 491 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/notification/Notification.kt:109 | S5 | 팩토리 create 의 인자가 7개다 | low | med |
| 492 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/order/Execution.kt:65 | N11 | 통화 코드 "KRW" 리터럴이 상수 없이 반복 | low | med (반증 있음) |
| 493 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/order/Order.kt:165 | S5 | Order.create 인자 14개 | low | med (반증 있음) |
| 494 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/order/Order.kt:178 | D6 | 같은 타입 id 인자 strategyId·signalId 가 나란히 옴 (Order.create) | low | med |
| 495 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/order/OrderAmendment.kt:19 | M4 | order_amendments 의 order_id FK 에 인덱스가 없음 | low | med |
| 496 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/order/OrderSignal.kt:29 | M4 | order_signals 의 account_id FK 와 is_processed 필터 열에 인덱스가 없음 | low | med |
| 497 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/portfolio/DailyBalance.kt:80 | S5 | create 인자 7개 — 같은 BigDecimal 다섯 개가 위치로 나란히 옴 | low | med |
| 498 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/portfolio/PortfolioSnapshot.kt:78 | S5 | refresh 인자 6개·create 인자 9개 — BigDecimal 이 위치로 연속 | low | med |
| 499 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/portfolio/TradingJournal.kt:53 | N11 | sentiment·journalType 이 닫힌 값인데 String 으로 흩어져 있고 허용값 정의가 DTO 정규식 두 곳과 KDoc 에만 있음 | low | med |
| 500 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/portfolio/TradingJournal.kt:76 | S5 | create 인자 7개 — 문자열 4개가 위치로 연속 | low | med |
| 501 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/position/Position.kt:100 | M7 | Kotlin 비널 필드 priceSource 가 DB 에서는 nullable | low | med |
| 502 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/position/PositionExitTrigger.kt:229 | S5 | PositionExitTrigger.create 인자가 8개다 | low | med (반증 있음) |
| 503 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/settlement/Settlement.kt:28 | M4 | settlements 의 account_id FK 에 인덱스가 없음 | low | med |
| 504 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/settlement/Settlement.kt:85 | S5 | Settlement.createFromExecution 인자 6개 | low | med (반증 있음) |
| 505 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/strategy/StrategyDerivation.kt:20 | M4 | 파생 계보의 parent·child 전략 id 조회에 인덱스가 없음 | low | med |
| 506 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/strategy/StrategyPerformanceSnapshot.kt:22 | M4 | 성과 스냅샷의 strategy_id·period_start 조회에 인덱스가 없음 | low | med |
| 507 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/strategy/StrategyVersion.kt:38 | N6 | versionNo — 허용 목록 밖 약어 «No» | low | med |
| 508 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/enums/PriceBasisPolicy.kt:3 | N12 | 포지션 청산 트리거만 쓰는 enum 이 접두 없이 공유 domain.enums 에 있음 | low | med |
| 509 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/event/ExecutionFilledEvent.kt:8 | D6 | 같은 타입 id executionId·orderId 가 나란히 옴 (ExecutionFilledEvent) | low | med |
| 510 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/port/CollectorSubscriptionPort.kt:4 | D6 | 값 집합이 정해진 mode 를 String 으로 받고 같은 String 인자 둘이 나란히 옴 | low | med |
| 511 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/client/CollectorSubscriptionAdapter.kt:23 | E2 | 구독 실패 로그에 예외 객체가 빠져 스택·원인 상실 | low | med (반증 있음) |
| 512 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/kis/KisAccountBalanceAdapter.kt:41 | N11 | KIS 응답 코드와 모드를 상수·enum 없이 문자열 리터럴로 비교함 | low | med |
| 513 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/kis/KisExecutionWebSocketManager.kt:129 | N5 | 클래스 이름이 Manager 로 끝나 역할을 말하지 않음 | low | med |
| 514 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/kis/KisExecutionWebSocketManager.kt:144 | N11 | 모드 문자열("paper"/"live")과 성공 코드("0") 리터럴 | low | med |
| 515 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/kis/KisExecutionWebSocketManager.kt:155 | A5 | CONNECTING 상태 표시가 예외 때 풀리지 않음 | low | med |
| 516 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/kis/KisExecutionWebSocketManager.kt:190 | E2 | WebSocket 전송 오류 로그에 예외 클래스 이름만 남음 | low | med |
| 517 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/kis/KisExecutionWebSocketManager.kt:197 | O5 | WebSocket 연결 성공이 로그에 남지 않음 | low | med |
| 518 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/kis/KisOrderRestClient.kt:153 | O7 | 외부 계좌번호 형식 오류 예외 메시지에 원문이 들어가고 실행기가 그대로 로그함 | low | med |
| 519 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/kis/KisOrderRestClient.kt:188 | N6 | 공개 DTO 속성 이름 executedQty 가 허용 목록 밖 약어 | low | med |
| 520 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/kis/KisTokenManager.kt:33 | E5 | 주문 요청 경로의 Redis 호출에 타임아웃 설정이 없음 | low | med |
| 521 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/notification/NotificationPersistenceTxService.kt:11 | N10 | 리포지토리 위임 어댑터에 Service 접미가 붙었다 | low | med |
| 522 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/notification/NotificationPersistenceTxService.kt:23 | F3 | 인자를 바꾸면서 그 인자를 돌려준다 | low | med |
| 523 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/notification/SlackNotificationEventHandler.kt:29 | A6 | 리스너 경로는 claim 없이 발송해 스케줄러와 같은 행을 이중 발송할 수 있음 | low | med |
| 524 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/notification/SlackNotificationProperties.kt:6 | S4 | enabled=true 인데 웹훅 URL 이 비어도 기동되고, 실패는 로그 없이 false 로만 처리 | low | med (반증 있음) |
| 525 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/notification/SlackNotificationScheduler.kt:17 | E6 | 재시도가 30초 고정 간격이고 retryBackoffMillis 는 읽히지 않으며, 재시도 횟수 상한도 API 로 사실상 해제됨 | low | med |
| 526 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/notification/SlackWebhookNotifier.kt:19 | O5 | Slack 웹훅 호출의 소요 시간·결과가 기록되지 않음 | low | med |
| 527 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/notification/SlackWebhookNotifier.kt:20 | E5 | 설정·API 로 노출되는 Slack timeoutMillis 가 실제 호출에 연결되지 않음 | low | med |
| 528 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/notification/SlackWebhookNotifier.kt:27 | E2 | 실패 로그에 예외 객체와 알림 식별자가 없고, 스케줄러는 고정 문자열만 저장 | low | med (반증 있음) |
| 529 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/AccountLedgerRepository.kt:7 | Q5 | 상한 없는 원장 목록 메서드와 계좌 범위 없는 멱등키 조회 메서드(현재 호출 없음) | low | med |
| 530 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/ExecutionRepository.kt:10 | Q5 | 계정 전체 체결을 상한 없이 반환하는 파생 메서드(현재 호출처 없음) | low | med |
| 531 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/OrderRepository.kt:23 | Q5 | 계정 전체 주문을 상한 없이 반환하는 파생 메서드(현재 호출처 없음) | low | med |
| 532 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/OrderRepositoryImpl.kt:17 | N6 | 클래스 전체에서 쓰는 한 글자 속성 이름 o | low | med |
| 533 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/PortfolioSnapshotRepository.kt:15 | Q7 | 일자별 스냅샷 조회가 파생 메서드와 Querydsl 두 곳에 있고 파생 쪽 둘은 호출처가 없다 | low | med |
| 534 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/RiskEventRepository.kt:7 | Q5 | 상한 없는 리스크 이벤트 이력 목록 메서드(현재 호출 없음) | low | med |
| 535 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/SettlementRepository.kt:9 | Q5 | 계정 전체 정산을 상한 없이 반환하는 파생 메서드(현재 호출처 없음) | low | med |
| 536 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/TaxSummaryRepository.kt:8 | Q7 | 계좌·연도 단건 조회가 파생 메서드와 Querydsl 두 곳에서 따로 쓰인다 | low | med |
| 537 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/TradingJournalRepository.kt:9 | Q5 | 상한 없는 List 조회 메서드(호출처 없음)가 남아 있음 | low | med |
| 538 | backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/controller/AccountController.kt:43 | I1 | 계좌 생성 POST 에 멱등 키·자연 키 유일 제약이 없음 | low | med |
| 539 | backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/controller/AccountExitTriggerDefaultController.kt:18 | S7 | 인증·소유 범위 없이 계좌 id 로 자동 청산 기본값 변경 | low | med |
| 540 | backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/controller/RiskPolicyController.kt:25 | S7 | 인증·소유 범위 없이 계좌 id 로 리스크 정책 조회·교체 | low | med |
| 541 | backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/controller/TradingJournalController.kt:74 | Q6 | OFFSET 페이징에 page 상한이 없음 | low | med |
| 542 | backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/dto/account/AccountExitTriggerDefaultDtos.kt:12 | N3 | Boolean 속성 enabled 에 is 접두가 없다 | low | med |
| 543 | backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/dto/order/OrderRequest.kt:19 | S8 | 수량·지정가의 자릿수·부호 제한이 요청 경계에 없음 (limitPrice 는 검증 0개) | low | med |
| 544 | backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/dto/order/OrderRequest.kt:22 | S8 | idempotencyKey 에 길이 상한 검증이 없어 DB 열 길이(100) 초과 시 500 이 됨 | low | med |
| 545 | backend/trading-api/src/main/resources/application.yaml:60 | C6 | server.shutdown·tomcat 스레드 상한·종료 대기 시간이 설정되지 않음 | low | med |
| 546 | backend/trading-api/src/main/resources/db/migration/V10__add_transaction_tax_rate_to_fee_policies.sql:1 | M9 | 구조 변경(ADD COLUMN)과 데이터 백필(UPDATE)이 한 파일에 섞임 | low | med |
| 547 | backend/trading-api/src/main/resources/db/migration/V9__drop_receivable_settlements.sql:1 | M9 | 정산 테이블을 근거 주석·보관 단계 없이 CASCADE 로 삭제 | low | med |
| 548 | backend/trading-api/src/test/kotlin/com/papertrading/api/application/account/AccountQueryServiceTest.kt:55 | T6 | 시험 이름 4개가 행동만 말하고 상황·기대가 없음 | low | med |
| 549 | backend/trading-api/src/test/kotlin/com/papertrading/api/application/account/KisAccountQueryServiceTest.kt:75 | T6 | 시험 이름이 상황·기대 없이 행동만 말함 | low | med |
| 550 | backend/trading-api/src/test/kotlin/com/papertrading/api/application/order/ExecutionProcessorTest.kt:55 | T1 | 내부 알림 발행 서비스(SlackNotificationEventPublisher)를 목으로 갈음 | low | med |
| 551 | backend/trading-api/src/test/kotlin/com/papertrading/api/application/order/ExecutionProcessorTest.kt:147 | T6 | 시험 이름이 «정산 생성 없음» 인데 본문은 정산 저장 1회를 단언 | low | med |
| 552 | backend/trading-api/src/test/kotlin/com/papertrading/api/application/order/ExecutionProcessorTest.kt:165 | T8 | 예수금 불변 단언이 assertEquals 로 scale 까지 비교 | low | med |
| 553 | backend/trading-api/src/test/kotlin/com/papertrading/api/application/order/OrderQueryServiceTest.kt:43 | T6 | 시험 이름이 구현(리포지토리 사용)을 말하고 정책을 말하지 않음 | low | med |
| 554 | backend/trading-api/src/test/kotlin/com/papertrading/api/application/position/PositionCommandServiceTest.kt:104 | T12 | 시험 하나에 단언 10개 — DB 불변과 캐시 갱신 두 행동이 한 시험에 | low | med |
| 555 | backend/trading-api/src/test/kotlin/com/papertrading/api/application/position/PositionExitQuoteEventListenerTest.kt:18 | T3 | 시험의 유일한 단언이 verify 호출 검증 — 결과 단언 없음 | low | med |
| 556 | backend/trading-api/src/test/kotlin/com/papertrading/api/application/position/PositionExitTriggerEvaluatorTest.kt:27 | T12 | 한 시험이 evaluate 를 두 번 호출하고 각자 단언 — 경계값과 경계 밖이 한 시험에 | low | med |
| 557 | backend/trading-api/src/test/kotlin/com/papertrading/api/application/position/PositionExitTriggerEvaluatorTest.kt:82 | T8 | exitRatioPercent 를 assertEquals 로 scale 까지 비교하는데 같은 파일 다른 단언은 compareTo | low | med |
| 558 | backend/trading-api/src/test/kotlin/com/papertrading/api/application/position/PositionExitTriggerOrchestratorTest.kt:67 | T3 | 주문 생성 결과를 createGroupedAutoExitSellOrder 호출 횟수 verify 로 단언 | low | med |
| 559 | backend/trading-api/src/test/kotlin/com/papertrading/api/domain/entity/account/AccountTest.kt:72 | T12 | 시험 하나가 대상 호출 2번 이상에 각각 단언을 달고 있음 | low | med |
| 560 | backend/trading-api/src/test/kotlin/com/papertrading/api/domain/entity/account/AccountTest.kt:113 | T6 | 시험 이름 약 9개가 상황 없이 «대상 메서드_기대»만 말함 | low | med |
| 561 | backend/trading-api/src/test/kotlin/com/papertrading/api/domain/entity/feepolicy/FeePolicyTest.kt:52 | T12 | 시험 하나가 같은 대상을 2번 호출하고 각각 단언 (2곳) | low | med |
| 562 | backend/trading-api/src/test/kotlin/com/papertrading/api/domain/entity/notification/NotificationTest.kt:24 | T12 | 시험 하나가 단언 9개를 단다 | low | med |
| 563 | backend/trading-api/src/test/kotlin/com/papertrading/api/domain/entity/order/OrderTest.kt:220 | T12 | isCancellable 시험 하나가 3번 호출하고 각각 단언 | low | med |
| 564 | backend/trading-api/src/test/kotlin/com/papertrading/api/domain/entity/portfolio/TaxSummaryTest.kt:27 | T8 | BigDecimal 단언이 assertEquals 로 scale 까지 비교하며, 정규화 시험은 이름에 드러나지 않음 | low | med |
| 565 | backend/trading-api/src/test/kotlin/com/papertrading/api/domain/entity/position/PositionTest.kt:34 | T6 | 시험 이름이 «대상 메서드 + 정상»이라 어떤 정책을 지키는지 말하지 않는다 | low | med |
| 566 | backend/trading-api/src/test/kotlin/com/papertrading/api/domain/entity/position/PositionTest.kt:47 | T12 | 시험 하나가 대상 메서드를 두 번 호출하고 각각 단언한다 | low | med |
| 567 | backend/trading-api/src/test/kotlin/com/papertrading/api/domain/entity/strategy/StrategyDomainEntitiesTest.kt:29 | T6 | 시험 이름이 상황·행동·기대를 말하지 않음 (flow_works·validates_*) | low | med |
| 568 | backend/trading-api/src/test/kotlin/com/papertrading/api/infrastructure/kis/KisExecutionNoticeParserTest.kt:15 | T12 | 시험 하나에 단언 12개, 다른 시험은 호출 3번에 각자 단언 | low | med |
| 569 | backend/trading-api/src/test/kotlin/com/papertrading/api/infrastructure/notification/SlackNotificationSchedulerTest.kt:158 | T6 | 시험 이름에 상황이 없고 앞선 시험과 단언이 겹침 | low | med |
| 570 | backend/trading-api/src/test/kotlin/com/papertrading/api/infrastructure/notification/SlackWebhookNotifierTest.kt:41 | T11 | Slack 응답값을 출처 표기 없이 손으로 적고 요청 본문 계약은 검증하지 않음 | low | med |
| 571 | backend/trading-api/src/test/kotlin/com/papertrading/api/interfaces/rest/position/PositionExitTriggerControllerTest.kt:76 | T8 | 금액·비율 JSON 기댓값을 Double 리터럴로 작성 | low | med |
| 572 | backend/trading-api/src/test/kotlin/com/papertrading/api/presentation/controller/KisAccountBalanceControllerIntegrationTest.kt:87 | T6 | 시험 이름이 상황·행동·기대 세 토막이 아니라 상태 코드 나열이다 | low | med |
| 573 | backend/trading-api/src/test/kotlin/com/papertrading/api/presentation/controller/KisAccountBalanceControllerIntegrationTest.kt:94 | T12 | 시험 하나가 대상 호출 5번에 각자 단언을 단다 | low | med |
| 574 | backend/trading-api/src/test/kotlin/com/papertrading/api/presentation/controller/OrderControllerIntegrationTest.kt:90 | T12 | 시험 하나에 세 행동(주문 생성·매칭 체결·포지션 생성)과 단언 9개 | low | med |
| 575 | backend/trading-api/src/test/kotlin/com/papertrading/api/presentation/controller/PortfolioSnapshotControllerTest.kt:57 | T8 | BigDecimal 필드를 Double 기댓값으로 단언해 scale 정규화 회귀를 못 잡음 | low | med |
| 576 | backend/trading-api/src/test/kotlin/com/papertrading/api/presentation/dto/sse/ExecutionSseEventTest.kt:26 | T6 | 시험 이름의 기대가 'correctly' 뿐 | low | med |
| 577 | (repo) | N1 | LOCAL/KIS 구분이 TradingMode·AccountType·AccountSource·KisAccountMode·executionMode 로 흩어졌을 가능성 | low | low |
| 578 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/account/KisAccountQueryService.kt:22 | N11 | KIS tr_id 코드가 유스케이스에 문자열 리터럴로 박혀 있고 어댑터에도 따로 있음 | low | low (반증 있음) |
| 579 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/account/result/KisBalancePositionResult.kt:11 | N6 | 허용 목록 밖 약어 Pnl 이 속성 이름에 쓰임 | low | low |
| 580 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/account/result/LedgerResult.kt:10 | D6 | 같은 타입 인자가 나란히 있어 위치 인자로 바꿔 넣어도 컴파일됨 | low | low |
| 581 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/notification/port/SlackNotificationConfig.kt:3 | N10 | 포트 이름이 값 객체처럼 읽힌다 | low | low |
| 582 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/notification/SlackNotificationEventPublisher.kt:44 | D6 | 같은 Long 타입 id 인자가 나란히 온다 | low | low |
| 583 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/ExecutionProcessor.kt:39 | N5 | 클래스 이름이 Processor 로 끝나 «무엇을 처리하는지» 말하지 않음 | low | low |
| 584 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/ExecutionProcessor.kt:98 | D3 | «미체결(PENDING·PARTIAL)» 상태 집합을 서비스 곳곳에 인라인 | low | low (반증 있음) |
| 585 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/KisLiveOrderExecutor.kt:3 | L8 | order 컨텍스트가 notification 컨텍스트의 구체 서비스 클래스를 직접 import | low | low |
| 586 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/KisPaperOrderExecutor.kt:3 | L8 | order 컨텍스트가 notification 컨텍스트의 구체 서비스 클래스를 직접 import | low | low |
| 587 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/LocalMatchingEngine.kt:26 | E4 | 틱마다 종목의 활성 주문 전부를 로딩한 뒤 가격 조건을 앱에서 거름 | low | low (반증 있음) |
| 588 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/LocalMatchingEngine.kt:26 | C2 | 시세 틱마다 종목의 미체결 주문 전체를 DB 에서 조회·평가 | low | low (반증 있음) |
| 589 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/LocalMatchingEngine.kt:28 | E2 | 매칭 실패 로그에 예외 객체가 없음 | low | low (반증 있음) |
| 590 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/OrderCommandService.kt:159 | E5 | IOC/FOK 즉시 취소 경로가 방금 저장한 주문과 이미 락 건 계좌를 다시 조회 | low | low (반증 있음) |
| 591 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/OrderCommandService.kt:272 | E1 | 매도 취소 시 포지션이 없으면 잠금 해제를 조용히 건너뜀 | low | low |
| 592 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/portfolio/SnapshotJobRunTxService.kt:12 | N6 | 클래스 이름의 Tx 가 허용 약어 목록 밖 | low | low |
| 593 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/portfolio/tax/TaxSettlementAggregate.kt:12 | N10 | 집계 결과 값에 DDD 용어 Aggregate 접미를 씀 | low | low |
| 594 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/portfolio/TaxSummaryBatchService.kt:20 | D3 | «종료된 과세연도» 규칙이 Batch·Command 서비스에 각각 있음 | low | low (반증 있음) |
| 595 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/portfolio/TaxSummaryBatchService.kt:28 | Q8 | 활성 계좌 id 만 필요한데 Account 엔티티 전체를 읽는다 | low | low (반증 있음) |
| 596 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/portfolio/TaxSummaryCommandService.kt:46 | N3 | Boolean 인자 force 가 is·should 접두 없는 플래그 이름 | low | low |
| 597 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/position/PositionCommandService.kt:41 | E2 | 시세 틱마다 보유 계좌 수만큼 Redis 저장 왕복을 반복 | low | low (반증 있음) |
| 598 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/position/PositionExitTriggerCommandService.kt:108 | N11 | 같은 에러 코드 문자열이 한 파일 안과 도메인에 흩어져 반복된다 | low | low |
| 599 | backend/trading-api/src/main/kotlin/com/papertrading/api/common/exception/CommonApiExceptions.kt:57 | D6 | 같은 타입 id 두 개가 나란히 오는 생성자 | low | low |
| 600 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/order/Order.kt:37 | S8 | Order → BaseAuditEntity → BaseTimeEntity 프로젝트 상속 3단 | low | low |
| 601 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/order/Order.kt:114 | M3 | KIS 주문번호(external_order_id)에 제약·인덱스가 없는데 조회는 단건(fetchOne) | low | low |
| 602 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/strategy/Strategy.kt:30 | M4 | 전략의 account_id 조회에 인덱스가 없음 | low | low (반증 있음) |
| 603 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/strategy/Strategy.kt:31 | S4 | Strategy 클래스의 public 함수가 11개로 기준(10개) 초과 | low | low (반증 있음) |
| 604 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/strategy/Strategy.kt:79 | M2 | 전략 실행 횟수·승인 상태를 읽고 고쳐 쓰는데 @Version·락이 없음 | low | low (반증 있음) |
| 605 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/strategy/StrategyDerivation.kt:20 | M3 | 전략 파생 관계에 (parent, child) 유일 제약이 없음 | low | low |
| 606 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/strategy/StrategyDerivation.kt:36 | N11 | derivationType 이 enum 이 아니라 문자열 코드 값 | low | low |
| 607 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/strategy/StrategyLog.kt:40 | N11 | logLevel 이 enum 이 아니라 문자열 코드 값 (저장소 API 까지 노출) | low | low |
| 608 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/kis/KisExecutionNoticeParser.kt:19 | E3 | 파싱 전체를 runCatching 으로 감싸 Error 까지 삼킴 | low | low (반증 있음) |
| 609 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/kis/KisExecutionWebSocketManager.kt:63 | E5 | WebSocket 연결에 명시적 타임아웃이 없음 | low | low |
| 610 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/kis/KisOrderProperties.kt:17 | S9 | KIS WebSocket 기본 URL 이 평문 ws:// 이고 자격 증명이 그 위로 전송됨 | low | low (반증 있음) |
| 611 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/notification/NotificationPersistenceTxService.kt:11 | N6 | 클래스 이름에 허용 목록 밖 약어 Tx 가 있다 | low | low |
| 612 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/notification/SlackNotificationEventHandler.kt:22 | O6 | 비동기 알림 실행기의 큐 깊이·거부·대기 시간 계측이 없음 | low | low (반증 있음) |
| 613 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/notification/SlackNotificationScheduler.kt:21 | C2 | 틱마다 PENDING 전량 조회하고 건수 상한 없이 건당 Slack 호출 | low | low (반증 있음) |
| 614 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/notification/SlackNotificationScheduler.kt:21 | O6 | 재시도 틱이 대기 건수·성공/실패 건수·소요 시간을 남기지 않음 | low | low (반증 있음) |
| 615 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/DailyBalanceRepository.kt:10 | Q7 | 같은 일자 구간 조회가 파생 메서드와 Querydsl 두 곳에 있고 파생 쪽은 호출처가 없다 | low | low (반증 있음) |
| 616 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/PositionRepository.kt:30 | Q7 | 같은 필터의 보유 포지션 조회가 호출처 없이 중복 선언됨 | low | low (반증 있음) |
| 617 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/SettlementTaxReadRepositoryImpl.kt:17 | S1 | summarizeForTax 본문 45줄 — 쿼리 둘과 업무 규칙(통화 혼재 거부)과 결과 매핑이 한 함수 | low | low (반증 있음) |
| 618 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/SettlementTaxReadRepositoryImpl.kt:18 | E5 | 같은 계좌·기간 조건으로 정산 테이블을 두 번 집계 | low | low (반증 있음) |
| 619 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/StrategyLogRepository.kt:7 | Q5 | 계속 자라는 전략 로그를 상한 없는 List 로 반환 | low | low |
| 620 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/TradingJournalRepositoryImpl.kt:52 | Q3 | 키워드 검색이 컬럼 함수와 앞 와일드카드를 써서 인덱스를 못 탐 | low | low (반증 있음) |
| 621 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/scheduler/LocalMatchingScheduler.kt:23 | C1 | 스케줄러가 기본 단일 스레드를 다른 작업과 공유 | low | low (반증 있음) |
| 622 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/scheduler/LocalMatchingScheduler.kt:23 | C3 | 다중 인스턴스 단일 실행 임대가 없고 인스턴스 수 가정도 문서에 없음 | low | low (반증 있음) |
| 623 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/scheduler/LocalMatchingScheduler.kt:34 | E2 | GTD 취소 실패 로그에 예외 객체가 없음 | low | low (반증 있음) |
| 624 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/sse/ExecutionSseEventHandler.kt:17 | A3 | @Async 가 실행기를 명시하지 않아 기본 실행기에 의존 | low | low (반증 있음) |
| 625 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/sse/ExecutionSseRegistry.kt:16 | C4 | SSE 연결 수 상한이 없음 | low | low (반증 있음) |
| 626 | backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/controller/ExecutionSseController.kt:25 | N9 | SSE 타임아웃이 단위·의미 없는 숫자 리터럴 | low | low (반증 있음) |
| 627 | backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/controller/PositionController.kt:26 | D3 | ticker 정규화 규칙(trim+uppercase)이 컨트롤러·리포지토리 구현·캐시 어댑터·엔티티에 흩어져 복제됨 | low | low |
| 628 | backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/controller/TradingJournalController.kt:91 | D6 | 같은 Long id 두 개가 나란히 위치 인자로 서비스에 전달됨 | low | low |
| 629 | backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/dto/notification/SlackNotificationDtos.kt:12 | N3 | Boolean 속성(enabled·webhookConfigured·accepted)에 is/has 접두가 없음 | low | low (반증 있음) |
| 630 | backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/dto/portfolio/TaxSummaryDto.kt:33 | N3 | Boolean 속성 force 에 is·should 접두가 없음 | low | low |
| 631 | backend/trading-api/src/main/kotlin/com/papertrading/api/TradingApiApplication.kt:11 | C3 | @Scheduled 작업에 분산 단일 실행 임대가 없고 단일 인스턴스 가정이 코드에 없음 | low | low |
| 632 | backend/trading-api/src/main/resources/application.yaml:22 | Q13 | 느린 쿼리 로그·Hibernate 통계·pg_stat_statements 가 레포 어디에도 없음 (batch fetch 크기만 설정됨) | low | low (반증 있음) |
| 633 | backend/trading-api/src/main/resources/application.yaml:44 | S9 | 실전 KIS WebSocket 기본 URL 이 평문 ws:// 이고 근거 주석이 없음 | low | low (반증 있음) |
| 634 | backend/trading-api/src/main/resources/application.yaml:60 | O9 | Actuator 설정이 없어 liveness·readiness 구분과 헬스 소비자가 없음 | low | low |
| 635 | backend/trading-api/src/main/resources/db/migration/V7__create_position_exit_triggers.sql:15 | M6 | Instant 필드에 대응하는 열이 시간대 없는 TIMESTAMP — Hibernate 6 기본 매핑(timestamptz)과 다를 수 있음 | low | low |
| 636 | backend/trading-api/src/test/kotlin/com/papertrading/api/application/portfolio/tax/TaxSummaryCalculatorTest.kt:14 | T6 | 시험 이름 `compute scale 4 and expected tax` 에 상황과 기대가 없음 | low | low |
| 637 | backend/trading-api/src/test/kotlin/com/papertrading/api/application/portfolio/tax/TaxSummaryCommandServiceTest.kt:76 | T8 | BigDecimal 을 assertEquals 로 scale 까지 비교 | low | low |
| 638 | backend/trading-api/src/test/kotlin/com/papertrading/api/domain/entity/portfolio/TaxSummaryTest.kt:33 | T6 | 시험 이름 일부가 상황·기대를 말하지 않음 — 값을_재계산한다, 0이면_정상_처리된다 | low | low |
| 639 | backend/trading-api/src/test/kotlin/com/papertrading/api/domain/entity/portfolio/TradingJournalTest.kt:15 | T6 | create_필드가_정상_설정된다 — «정상» 이 기대를 말하지 않고 상황이 없음 | low | low |
| 640 | backend/trading-api/src/test/kotlin/com/papertrading/api/interfaces/rest/position/PositionExitTriggerControllerTest.kt:38 | T1 | @WebMvcTest 슬라이스에서 내부 서비스 두 개를 @MockBean 으로 갈음 (렌즈 기본값 high 를 낮춤) | low | low |
| 641 | backend/trading-api/src/test/kotlin/com/papertrading/api/presentation/controller/AccountControllerIntegrationTest.kt:69 | T6 | 시험 이름의 기대가 «정상 동작한다» 로 모호하다 | low | low |
| 642 | backend/trading-api/src/test/kotlin/com/papertrading/api/presentation/controller/AccountControllerIntegrationTest.kt:91 | T12 | 시험 하나가 입금·출금·조회·원장 4개 호출에 각자 단언을 달고 단언이 10개를 넘는다 | low | low (반증 있음) |
| 643 | backend/trading-api/src/test/kotlin/com/papertrading/api/presentation/controller/PortfolioSnapshotControllerTest.kt:38 | T1 | 컨트롤러 시험이 같은 레포 서비스 3개를 @MockBean 으로 갈음하고 @MockBean 은 폐기 경고 | low | low |
| 644 | backend/trading-api/src/test/kotlin/com/papertrading/api/presentation/controller/TaxSummaryControllerTest.kt:40 | T1 | 컨트롤러 시험이 같은 레포 서비스 3개를 @MockBean 으로 갈음하고 @MockBean 은 폐기 경고 | low | low |
| 645 | backend/trading-api/src/test/kotlin/com/papertrading/api/support/TestEntityUtils.kt:3 | F3 | 수신 객체를 바꾸면서 돌려주는 withId 이름이 복사본을 약속함 | low | low (반증 있음) |
| 646 | backend/trading-api/src/test/kotlin/com/papertrading/api/TradingApiApplicationTests.kt:28 | T7 | 본문이 빈 contextLoads 시험에 단언이 하나도 없음 | low | low (반증 있음) |

## 2-1. 발견 상세 (한 건 = 한 블록 — 코드 먼저, 그다음 원인·영향·해결)

### 1위 [high · 확신 high] 유스케이스가 infrastructure 의 Spring Data 리포지토리를 직접 주입받음

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/account/AccountCommandService.kt:10` · 항목표 backend/layering · 항목 L1

```
import com.papertrading.api.infrastructure.persistence.AccountLedgerRepository // ← application → infrastructure
import com.papertrading.api.infrastructure.persistence.AccountRepository
class AccountCommandService(
    private val accountRepository: AccountRepository, // ← 포트가 아닌 리포지토리 직접 주입
    private val accountLedgerRepository: AccountLedgerRepository
```

- 원인: AccountRepository·AccountLedgerRepository 를 infrastructure.persistence 에서 import 해 생성자로 받는다. PTB7·PTB8 은 application 이 port 만 보게 한다. ArchUnit 동결 4건.
- 생길 수 있는 일: 계좌 입출금 단위 시험이 JPA 리포지토리 목에 묶이고(T2), 포트로 바꿀 자리가 없어 영속 세부(findByIdWithLock)가 유스케이스에 박힌다.
- 해결: application/account/port 에 AccountPort·AccountLedgerPort(도메인 모델을 주고받음)를 두고 infrastructure 어댑터가 구현한다.

### 2위 [high · 확신 high] 유스케이스가 infrastructure 리포지토리를 직접 주입받음

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/account/AccountExitTriggerDefaultCommandService.kt:6` · 항목표 backend/layering · 항목 L1

```
import com.papertrading.api.infrastructure.persistence.AccountRepository // ← application → infrastructure
import com.papertrading.api.infrastructure.persistence.AccountExitTriggerDefaultRepository
class AccountExitTriggerDefaultCommandService(
    private val accountRepository: AccountRepository,
    private val accountExitTriggerDefaultRepository: AccountExitTriggerDefaultRepository,
```

- 원인: AccountRepository·AccountExitTriggerDefaultRepository 를 infrastructure.persistence 에서 import 해 생성자로 받는다. ArchUnit 동결 4건.
- 생길 수 있는 일: 포트가 없어 유스케이스 단위 시험이 JPA 리포지토리를 목으로 쓰게 된다.
- 해결: application/account/port 로 출력 포트를 두고 어댑터가 구현한다.

### 3위 [high · 확신 high] 조회 유스케이스가 infrastructure 리포지토리를 직접 주입받음

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/account/AccountQueryService.kt:8` · 항목표 backend/layering · 항목 L1

```
import com.papertrading.api.infrastructure.persistence.AccountLedgerRepository // ← application → infrastructure
import com.papertrading.api.infrastructure.persistence.AccountRepository
class AccountQueryService(
    private val accountRepository: AccountRepository,
    private val accountLedgerRepository: AccountLedgerRepository
```

- 원인: AccountLedgerRepository·AccountRepository 를 infrastructure.persistence 에서 import 한다. ArchUnit 동결 4건.
- 생길 수 있는 일: 조회 유스케이스가 영속 세부(파생 쿼리 이름)에 묶이고 Page 같은 Spring 타입이 위로 샌다(L2).
- 해결: 조회 포트(AccountReadPort 등)를 application/account/port 에 둔다.

### 4위 [high · 확신 high] KIS 잔고 조회 유스케이스가 PositionRepository 를 직접 주입받음(KIS 쪽만 포트 사용)

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/account/KisAccountQueryService.kt:9` · 항목표 backend/layering · 항목 L1

```
import com.papertrading.api.infrastructure.persistence.PositionRepository // ← application → infrastructure
class KisAccountQueryService(
    private val kisAccountBalancePort: KisAccountBalancePort,
    private val positionRepository: PositionRepository, // ← 포트가 아닌 리포지토리
```

- 원인: KisAccountBalancePort 는 포트로 잘 분리했는데 로컬 포지션은 infrastructure.persistence.PositionRepository 를 직접 쓴다. 다른 컨텍스트(포지션)의 리포지토리를 넘어 쓰기도 한다. ArchUnit 동결 2건.
- 생길 수 있는 일: 대사(reconciliation) 로직이 JPA 리포지토리 목 없이는 시험되지 않는다.
- 해결: application/position/port(또는 account/port)에 로컬 보유 수량 조회 포트를 두고 어댑터가 구현한다.

### 5위 [high · 확신 high] 유스케이스가 Spring Data 리포지토리를 직접 import·주입함

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/account/RiskPolicyService.kt:7` · 항목표 backend/layering · 항목 L1

```
import com.papertrading.api.infrastructure.persistence.AccountRepository      // ← application 이 infrastructure 를 import
import com.papertrading.api.infrastructure.persistence.RiskPolicyRepository
...
class RiskPolicyService(
    private val accountRepository: AccountRepository,      // ← Spring Data 리포지토리를 유스케이스에 직접 주입
    private val riskPolicyRepository: RiskPolicyRepository
```

- 원인: RiskPolicyService(application)가 infrastructure.persistence 의 AccountRepository·RiskPolicyRepository 를 import 하고 생성자로 직접 주입받는다. PTB7·PTB8 은 유스케이스가 application/<ctx>/port 의 출력 포트만 주입받도록 한다. ArchUnit 동결 색인에 이 클래스의 application→infrastructure 위반 4건과 생성자 인자 위반(동결 저장소 320·321줄)이 그대로 남아 있다. 동결은 해결이 아니다.
- 생길 수 있는 일: 단위 시험이 DB 계층 타입을 목으로 흉내 내야 하고(RiskPolicyServiceTest 의 mockk<AccountRepository>), 락·활성 정책 조회 같은 쿼리 동작은 목의 반환값을 믿을 뿐 시험되지 않는다. 영속 계층을 바꾸면 유스케이스도 함께 깨진다.
- 해결: application/account/port 에 출력 포트(계좌 락 조회·활성 정책 조회·정책 저장)를 정의하고 infrastructure 어댑터가 Spring Data 리포지토리에 위임하게 옮긴다. 서비스는 포트만 주입받는다.

고친 모양(예시):
```
// application/account/port/RiskPolicyPort.kt
interface RiskPolicyPort {
    fun lockAccount(accountId: Long): Account
    fun findActive(accountId: Long): RiskPolicy?
    fun save(policy: RiskPolicy): RiskPolicy
}

@Service
class RiskPolicyService(private val riskPolicyPort: RiskPolicyPort)
```

### 6위 [high · 확신 high] 유스케이스가 infrastructure 리포지토리 8개를 직접 import·주입

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/ExecutionProcessor.kt:16` · 항목표 backend/layering · 항목 L1

```
import com.papertrading.api.infrastructure.persistence.AccountLedgerRepository   // ← application 이 infrastructure import
import com.papertrading.api.infrastructure.persistence.AccountRepository
...
import com.papertrading.api.infrastructure.persistence.SettlementRepository         // ← L23
...
    private val orderRepository: OrderRepository,                                       // ← L40 구체 리포지토리 주입
```

- 원인: application 파일이 infrastructure.persistence 의 *Repository 8개를 import 하고 생성자로 주입받는다(PTB7·PTB8). ArchUnit 동결 16건(tools/T96587ad0.txt:96).
- 생길 수 있는 일: 유스케이스 단위 시험이 DB·Spring Data 를 요구하고, 포트 계층이 우회돼 영속 기술 교체가 유스케이스 수정으로 번진다.
- 해결: application.order.port 에 출력 포트(OrderPersistencePort 등)를 두고 infrastructure 어댑터가 구현하게 한다.

고친 모양(예시):
```
// application/order/port/OrderPersistencePort.kt
interface OrderPersistencePort {
    fun findOpenWithLock(orderId: Long): Order?
    fun save(order: Order): Order
}
// ExecutionProcessor(private val orders: OrderPersistencePort, …)
```

### 7위 [high · 확신 high] processFill 한 함수가 약 115줄 — 체결·수수료·포지션·원장·정산·구독 해제·이벤트·알림을 모두 함

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/ExecutionProcessor.kt:88` · 항목표 backend/code-shape · 항목 S1

```
    fun processFill(                              // ← L88 시작 ~ L231 끝: 빈 줄·주석 제외 약 115줄(한계 40)
        orderId: Long,
        fillPrice: BigDecimal,
        fillQty: BigDecimal,
        externalExecutionId: String,
        executedAt: Instant,
    ) {
        if (executionRepository.findByExternalExecutionId(externalExecutionId).isPresent) return
        // … L96~L223 생략 (수수료·평균가·체결 저장·매수/매도 분기·정산·잠금 해제)
        slackNotificationEventPublisher.publishExecutionFilled(   // ← L224: 같은 함수 끝에서 알림 발행까지
```

- 원인: 함수 본문(L94~L231)이 빈 줄·주석을 빼고 약 115줄로 한계 40줄의 약 3배다. 멱등 검사 → 수수료 계산 → 평균가 계산 → 주문·체결 저장 → 매수/매도 분기 → 정산 → 잠금 해제 → 이벤트·알림까지 추상화 수준이 여럿이다. detekt 기준선에 LongMethod 로 이미 흡수돼 있다(tools/Tb661de0c.txt:73).
- 생길 수 있는 일: 돈이 움직이는 핵심 쓰기 경로가 한 화면에 안 읽혀, 한 단계를 고칠 때 다른 단계(잠금 해제·정산)를 깨기 쉽고 단계별로 시험하기 어렵다. 이 파일의 S6·F1·F4·D3 발견이 모두 이 함수 안에 있다.
- 해결: 단계를 비공개 함수·도메인 메서드로 쪼갠다: 중복 체결 가드 · 수수료 산정 · order.applyExecution · 매수/매도 정산 · 후처리(잠금 해제·구독 해제·이벤트).

고친 모양(예시):
```
fun processFill(orderId: Long, fillPrice: BigDecimal, fillQty: BigDecimal, externalExecutionId: String, executedAt: Instant) {
    if (alreadyProcessed(externalExecutionId)) return
    val order = loadOpenOrder(orderId) ?: return
    val account = lockAccount(order)
    val fee = feeFor(account, order, fillPrice.multiply(fillQty), executedAt)
    order.applyExecution(fillQty, fillPrice, fee)
    val execution = recordExecution(order, account, fillPrice, fillQty, fee, externalExecutionId, executedAt)
    if (order.orderSide == OrderSide.BUY) settleBuy(order, account, execution) else settleSell(order, account, execution)
    afterFill(order, account, execution)
}
```

### 8위 [high · 확신 high] 평균 체결가를 서비스가 계산해 넘겨 Order.totalAmount 가 누적 평균 × 수량으로 틀어짐

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/ExecutionProcessor.kt:114` · 항목표 backend/code-shape · 항목 S6

```
        val prevQty = order.filledQuantity                              // ← 주문 속성을 서비스가 읽어
        val prevAvg = order.avgFilledPrice ?: BigDecimal.ZERO
        val newAvgPrice = if (prevQty == BigDecimal.ZERO) fillPrice
        else prevAvg.multiply(prevQty).add(fillPrice.multiply(fillQty))
            .divide(prevQty.add(fillQty), 4, RoundingMode.HALF_UP)         // ← 평균가를 서비스가 정하고

        order.applyExecution(fillQty, newAvgPrice, fee)                 // ← 그 결과로 같은 주문 메서드를 부름
// Order.kt:140
        totalAmount = (totalAmount ?: BigDecimal.ZERO).add(executedQty.multiply(newAvgPrice))   // ← 누적 평균가를 이번 체결가처럼 곱함
```

- 원인: 서비스가 order.filledQuantity·order.avgFilledPrice 두 속성을 읽어 가중평균 newAvgPrice 를 정하고, 그 결과로 order.applyExecution 을 부른다(묻지 말고 시켜라 위반). 더 나쁘게 Order.applyExecution 은 이 값을 «이번 체결가»처럼 totalAmount 에 곱한다. 두 번째 체결부터 서비스는 누적 평균가를 넘기므로 totalAmount += 체결수량 × 누적평균가 가 되어, 가격이 다른 두 체결이면 합계가 틀린다(4주@69000 + 6주@70000: 정답 696000, 실제 276000 + 6×69600 = 693600). OrderTest.kt:147-153 은 두 번째 인자에 체결가(70000)를 넘겨 696000 을 기대하므로, 시험이 쓰는 계약과 운영이 쓰는 계약이 다르다 — 시험은 초록, 운영은 틀림.
- 생길 수 있는 일: 가격이 다른 부분 체결이 있는 주문마다 Order.totalAmount(누적 체결금액)가 조용히 틀린다. remainingLockedAmount 등 이 값을 쓰는 모든 잠금 계산·표시가 오염된다. 컴파일·시험 모두 통과해 발견되지 않는다.
- 해결: Order.applyExecution 이 체결수량·체결가·수수료만 받아 누적 금액과 평균가를 스스로 계산하게 한다. 서비스의 평균가 계산을 지우고, OrderTest 의 인자 의미를 같은 계약으로 맞춘다.

고친 모양(예시):
```
fun applyExecution(fillQty: BigDecimal, fillPrice: BigDecimal, fillFee: BigDecimal) {
    check(isOpen()) { "체결 적용 불가 상태입니다: $orderStatus" }
    val newTotal = (totalAmount ?: BigDecimal.ZERO).add(fillQty.multiply(fillPrice))
    val newFilled = filledQuantity.add(fillQty)
    totalAmount = newTotal
    filledQuantity = newFilled
    avgFilledPrice = newTotal.divide(newFilled, 4, RoundingMode.HALF_UP)
    fee = fee.add(fillFee)
    orderStatus = if (newFilled >= quantity) OrderStatus.FILLED else OrderStatus.PARTIAL
}
```

### 9위 [high · 확신 high] 체결 트랜잭션(주문·계좌·포지션 락 보유) 안에서 collector 구독 해제 HTTP 호출

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/ExecutionProcessor.kt:205` · 항목표 data/transaction-boundary · 항목 X3

```
tryUnsubscribe(accountId, ticker, tradingMode.name)   // ← fillLocal/fillExternal @Transactional 안, 계좌·포지션 락 보유 중
// :250  collectorSubscriptionPort.unsubscribe(mode, ticker)   // ← 원격 HTTP (타임아웃 없는 RestTemplate)
```

- 원인: processFill 은 fillLocal·fillExternal 의 @Transactional 안에서 돌고, FILLED 이면 tryUnsubscribe 가 collectorSubscriptionPort.unsubscribe 를 부릅니다. 구현체는 타임아웃 없는 RestTemplate()(CollectorSubscriptionAdapter.kt:19)로 delete(:30)를 호출합니다. 실패는 삼키지만 지연은 막지 못합니다(LLM-only 판정).
- 생길 수 있는 일: collector 가 느리면 체결 트랜잭션이 길어져 계좌·포지션 락이 풀리지 않고, 그 계좌의 후속 체결·주문이 대기합니다.
- 해결: 구독 해제는 AFTER_COMMIT 리스너로 옮기고, 리스너가 부르는 클라이언트에 타임아웃을 둡니다.

고친 모양(예시):
```
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
fun onOrderFilled(e: ExecutionFilledEvent) { if (e.orderFilled) unsubscribeIfIdle(e) }
```

### 10위 [high · 확신 high] KIS 통지 유스케이스가 infrastructure 타입(KisExecutionNotice·리포지토리 2개)을 직접 import

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/KisExecutionNoticeService.kt:3` · 항목표 backend/layering · 항목 L1

```
import com.papertrading.api.infrastructure.kis.KisExecutionNotice               // ← L3 application 이 infrastructure DTO 를 import
import com.papertrading.api.infrastructure.persistence.ExecutionRepository       // ← L4
import com.papertrading.api.infrastructure.persistence.OrderRepository           // ← L5
```

- 원인: application 이 infrastructure.kis.KisExecutionNotice 와 infrastructure.persistence 의 ExecutionRepository·OrderRepository 를 import·주입한다. ArchUnit 동결 7건(tools/T96587ad0.txt:98).
- 생길 수 있는 일: KIS 응답 모양이 유스케이스 시그니처가 되어 KIS 계약 변경이 application 을 바꾸고, 시험이 DB 를 요구한다.
- 해결: 통지를 application.order.command 의 ExecutionNoticeCommand 로 받고, 리포지토리는 포트로 바꾼다.

### 11위 [high · 확신 high] application 실행기가 infrastructure HTTP 클라이언트·리포지토리를 직접 주입받음

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/KisLiveOrderExecutor.kt:5` · 항목표 backend/layering · 항목 L1

```
import com.papertrading.api.infrastructure.kis.KisOrderRestClient // ← application 이 infrastructure 클라이언트를 import
import com.papertrading.api.infrastructure.persistence.OrderRepository // ← infrastructure 리포지토리를 import
...
class KisLiveOrderExecutor(
    private val orderRepository: OrderRepository, // ← 본문에서 안 쓰는 의존(detekt UnusedPrivateProperty)
    private val kisOrderRestClient: KisOrderRestClient, // ← 출력 포트 없이 구현 클래스를 직접 주입
```

- 원인: 유스케이스 계층 클래스가 infrastructure.kis.KisOrderRestClient 와 infrastructure.persistence.OrderRepository 를 import 하고 생성자로 주입받는다(PTB7·PTB8). ArchUnit 동결 색인에 이 파일 4건이 있으나 동결은 해결이 아니다. orderRepository 는 본문에서 쓰이지도 않는다(detekt UnusedPrivateProperty).
- 생길 수 있는 일: 단위 시험이 HTTP 클라이언트·리포지토리 목을 요구하고, KIS 연동을 바꾸거나 가짜로 대체할 때 유스케이스가 따라 바뀐다. 안 쓰는 의존이 계속 주입되어 변경 영향 범위가 부풀려진다.
- 해결: application/order/port 에 KIS 주문 접수·취소 출력 포트를 두고 KisOrderRestClient 를 그 어댑터로 바꾼다. 안 쓰는 orderRepository 는 먼저 지운다.

고친 모양(예시):
```
// application/order/port/KisOrderGateway.kt
interface KisOrderGateway {
    fun placeOrder(order: Order, mode: TradingMode): String
    fun cancelOrder(order: Order, mode: TradingMode)
}
```

### 12위 [high · 확신 high] @Transactional 메서드가 KIS 실거래 REST 호출을 직접 수행

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/KisLiveOrderExecutor.kt:25` · 항목표 data/transaction-boundary · 항목 X3

```
@Transactional
fun submit(order: Order) {
    ...
        val orno = kisOrderRestClient.placeOrder(order, "live")   // ← 열린 트랜잭션 안 KIS 실주문 HTTP
        order.assignExternalOrderId(orno)
```

- 원인: submit 이 @Transactional 이고 그 안에서 kisOrderRestClient.placeOrder(:31, "live")를 부릅니다. KisPaperOrderExecutor 와 같은 결함이며 실주문이라 피해가 큽니다. cancel(:46)도 같은 모양입니다(호출처 없음).
- 생길 수 있는 일: 실주문 접수 중 KIS 지연이 계좌 락을 붙잡고, KIS 성공 뒤 커밋 실패 시 로컬에 없는 실주문이 남습니다.
- 해결: KisPaperOrderExecutor 와 같이 KIS 호출을 트랜잭션 밖으로 빼고 결과 반영만 새 트랜잭션으로 합니다.

### 13위 [high · 확신 high] application 실행기가 infrastructure HTTP 클라이언트를 직접 주입받음

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/KisPaperOrderExecutor.kt:5` · 항목표 backend/layering · 항목 L1

```
import com.papertrading.api.infrastructure.kis.KisOrderRestClient // ← application 이 infrastructure 클라이언트를 import
...
class KisPaperOrderExecutor(
    private val kisOrderRestClient: KisOrderRestClient, // ← 출력 포트 없이 구현 클래스를 직접 주입
```

- 원인: KisOrderRestClient(infrastructure.kis)를 import·생성자 주입한다(PTB7·PTB8). ArchUnit 동결 색인에 이 파일 2건이 있다. KisLiveOrderExecutor 와 같은 결함이다.
- 생길 수 있는 일: KIS 연동 교체·가짜 대체 때 유스케이스가 따라 바뀌고, 단위 시험이 HTTP 클라이언트를 요구한다.
- 해결: KisLiveOrderExecutor 와 같은 KIS 주문 출력 포트(application/order/port)를 주입받게 한다.

고친 모양(예시):
```
class KisPaperOrderExecutor(
    private val kisOrderGateway: KisOrderGateway,
    private val slackNotificationEventPublisher: SlackNotificationEventPublisher,
)
```

### 14위 [high · 확신 high] @Transactional 메서드가 KIS 모의투자 REST 호출을 직접 수행

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/KisPaperOrderExecutor.kt:22` · 항목표 data/transaction-boundary · 항목 X3

```
@Transactional
fun submit(order: Order) {
    ...
        val orno = kisOrderRestClient.placeOrder(order, "paper")   // ← 열린 트랜잭션 안 KIS HTTP
        order.assignExternalOrderId(orno)
```

- 원인: submit 이 @Transactional 이고 그 안에서 kisOrderRestClient.placeOrder(:28)를 부릅니다. 바깥 placeOrder 트랜잭션에 합류(REQUIRED)해 계좌 락 아래에서 도는 같은 결함(OrderCommandService.kt:70)의 실제 호출 자리입니다. cancel(:42)도 같은 모양이며 cancelOrder(:48)가 @Transactional 안에서 HTTP 를 부릅니다(호출처 없음).
- 생길 수 있는 일: KIS 응답 지연 동안 계좌 락과 커넥션이 묶이고, KIS 성공 뒤 롤백되면 외부 주문이 고아가 됩니다.
- 해결: executor 에서 @Transactional 을 제거하고 KIS 호출은 트랜잭션 밖, 결과 반영만 짧은 새 트랜잭션으로 합니다.

### 15위 [high · 확신 high] 매칭 엔진이 Spring Data 리포지토리를 직접 주입받음

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/LocalMatchingEngine.kt:7` · 항목표 backend/layering · 항목 L1

```
import com.papertrading.api.infrastructure.persistence.OrderRepository // ← application 이 infrastructure 리포지토리를 import
...
class LocalMatchingEngine(
    private val orderRepository: OrderRepository, // ← 포트가 아닌 Spring Data 리포지토리 주입
```

- 원인: infrastructure.persistence.OrderRepository 를 import 하고 생성자로 주입받는다(PTB8). ArchUnit 동결 색인 2건. 진행 중 주문 조회(findActiveLocalOrdersByTicker)가 포트 없이 구현에 묶여 있다.
- 생길 수 있는 일: 매칭 단위 시험이 리포지토리 목에 묶이고 조회 쿼리의 실제 동작은 검증되지 않는다.
- 해결: 활성 LOCAL 주문 조회를 application/order/port 의 출력 포트로 올리고 어댑터가 리포지토리를 감싸게 한다.

고친 모양(예시):
```
interface ActiveOrderReader {
    fun findActiveLocalOrdersByTicker(ticker: String): List<Order>
}
```

### 16위 [high · 확신 high] 주문 명령 서비스가 리포지토리 4개를 직접 주입받음

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/OrderCommandService.kt:19` · 항목표 backend/layering · 항목 L1

```
import com.papertrading.api.infrastructure.persistence.AccountLedgerRepository // ← application 이 infrastructure 를 import
import com.papertrading.api.infrastructure.persistence.AccountRepository
import com.papertrading.api.infrastructure.persistence.OrderRepository
import com.papertrading.api.infrastructure.persistence.PositionRepository
...
class OrderCommandService(
    private val accountRepository: AccountRepository, // ← 포트가 아닌 Spring Data 리포지토리
```

- 원인: AccountLedgerRepository·AccountRepository·OrderRepository·PositionRepository(infrastructure.persistence)를 import·주입한다(PTB8). ArchUnit 동결 색인 8건. 시세·수집 구독만 포트(domain.port)를 쓴다.
- 생길 수 있는 일: 유스케이스 시험이 리포지토리 목에 묶이고 비관적 락·멱등 조회가 실제 엔진에서 검증되지 않는다. 영속 교체 때 서비스가 같이 바뀐다.
- 해결: 리포지토리 4개를 application/order/port 의 출력 포트(저장·락 조회 의미를 담은 메서드)로 바꾸고 infrastructure 어댑터가 구현한다.

고친 모양(예시):
```
interface AccountPort {
    fun getForUpdate(accountId: Long): Account
}
interface OrderPort {
    fun findByIdempotencyKey(accountId: Long, key: String): Order?
    fun save(order: Order): Order
}
```

### 17위 [high · 확신 high] KIS 주문 호출이 커밋 전 트랜잭션 안에서 실행됨 — 의도 기록·불확정 상태 없음

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/OrderCommandService.kt:49` · 항목표 reliability/failure-handling · 항목 I3

```
@Transactional
fun placeOrder(accountId: Long, command: PlaceOrderCommand): Order {   // ← 주문 저장·홀드·KIS 호출·결과 반영이 한 트랜잭션
...
    val order = saveOrder(account, command, lockedAmount)
...
    submitExternalOrderIfNeeded(tradingMode, order)   // ← 아직 커밋되지 않은 트랜잭션 안에서 KIS REST 호출
...
TradingMode.KIS_LIVE -> kisLiveOrderExecutor.submit(order)   // ← L174, 실계좌 주문도 동일
```

- 원인: placeOrder 는 한 @Transactional 안에서 주문 저장·예수금 홀드 원장·collector 구독을 한 뒤 KIS REST 주문(submitExternalOrderIfNeeded)을 호출하고, 결과(externalOrderId)도 같은 트랜잭션에서 반영한다. 의도 행이 먼저 커밋되지 않는다. 호출이 불확정(읽기 타임아웃 10초·프로세스 종료·호출 성공 뒤 assignExternalOrderId·알림 예외)으로 끝나면 주문·홀드 전체가 롤백되고, UNKNOWN 같은 확정 대기 상태도 없다. 호출 동안 계좌 행 락(findByIdWithLock)과 DB 커넥션을 계속 쥔다.
- 생길 수 있는 일: KIS 에는 주문이 살아 있는데 우리 DB 에는 흔적이 없는 상태가 생긴다. 같은 멱등 키로 재시도하면 주문 행이 없어 KIS 에 두 번째 주문이 나가고(이중 주문), 이후 체결 통지는 매칭할 주문이 없어 버려진다.
- 해결: 3단계로 나눈다. 1) 트랜잭션 1: 주문(SUBMITTING)·예수금 홀드를 커밋한다. 2) 트랜잭션 밖에서 KIS 호출하고, 타임아웃·5xx·파싱 실패는 UNKNOWN 으로 기록한다. 3) 트랜잭션 2: 성공이면 externalOrderId 저장, UNKNOWN 이면 조회·취소로 확정하는 작업에 넘긴다.

고친 모양(예시):
```
// 1) 의도 커밋
val intent = orderIntentService.recordSubmitting(accountId, command) // status=SUBMITTING, 홀드 포함, 커밋
// 2) 트랜잭션 밖 호출
val outcome = runCatching { kisOrderRestClient.placeOrder(intent, mode) }
// 3) 결과 반영 — 불확정이면 UNKNOWN
orderIntentService.applyOutcome(intent.id, outcome)
```

### 18위 [high · 확신 high] 주문 접수 트랜잭션 안에서 collector-api 구독 HTTP(타임아웃 없음)를 호출

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/OrderCommandService.kt:66` · 항목표 data/transaction-boundary · 항목 X3

```
collectorSubscriptionPort.subscribe(tradingMode.name, command.ticker)   // ← 계좌 PESSIMISTIC 락 보유 중 원격 HTTP
// CollectorSubscriptionAdapter.kt:19  private val restTemplate = RestTemplate()   // ← 타임아웃 없음
// CollectorSubscriptionAdapter.kt:23  runCatching { restTemplate.postForObject(url, null, String::class.java) }
```

- 원인: placeOrder(@Transactional, 계좌 락 보유)가 collectorSubscriptionPort.subscribe 를 부릅니다. 구현체 CollectorSubscriptionAdapter 는 `RestTemplate()` 기본 인스턴스(CollectorSubscriptionAdapter.kt:19)로 postForObject(:23)를 호출하고 읽기·연결 타임아웃을 두지 않습니다. 실패는 삼키지만 지연은 막지 못합니다. 같은 트랜잭션에서 marketQuotePort.getQuote(Redis 읽기, :101·:157)도 락 안에서 돕니다.
- 생길 수 있는 일: collector-api 가 느리거나 멈추면 HTTP 호출이 무기한 대기해 계좌 락과 커넥션을 붙잡고, 같은 계좌의 모든 주문·체결이 멈춥니다.
- 해결: 구독 요청은 커밋 뒤(AFTER_COMMIT 리스너)로 옮기고, RestTemplate 에 연결·읽기 타임아웃을 둡니다.

고친 모양(예시):
```
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
fun onOrderPlaced(e: OrderPlacedEvent) = collectorSubscriptionPort.subscribe(e.mode, e.ticker)
```

### 19위 [high · 확신 high] 주문 접수 트랜잭션(계좌 비관 락 보유) 안에서 KIS 주문 REST 를 호출

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/OrderCommandService.kt:70` · 항목표 data/transaction-boundary · 항목 X3

```
submitExternalOrderIfNeeded(tradingMode, order)   // ← @Transactional placeOrder(:49) 안, 계좌 PESSIMISTIC 락(:55) 보유 중 KIS REST 주문
// :173  TradingMode.KIS_PAPER -> kisPaperOrderExecutor.submit(order)
// :174  TradingMode.KIS_LIVE -> kisLiveOrderExecutor.submit(order)
// KisOrderRestClient.kt:56  val response = restTemplate.exchange(   // ← 실제 HTTP
```

- 원인: placeOrder 는 @Transactional 이고 계좌를 PESSIMISTIC 락(findByIdWithLock)한 뒤 submitExternalOrderIfNeeded 로 KisPaper/LiveOrderExecutor.submit 을 부릅니다. 그 안에서 KisOrderRestClient.placeOrder→restTemplate.exchange(KisOrderRestClient.kt:56)가 돌고 토큰 발급 HTTP(KisTokenManager.kt:74)도 같은 트랜잭션 안일 수 있습니다. PTD2·DA1 위반입니다. 주문 저장·홀드는 DB 에 있고 KIS 호출은 트랜잭션 밖이어야 합니다(LLM-only 판정).
- 생길 수 있는 일: KIS 지연이 계좌 행 락과 DB 커넥션을 붙잡아 같은 계좌의 다른 요청을 막습니다. KIS 접수 성공 뒤 커밋이 실패하면 로컬엔 없는 KIS 주문이 남는 고아 부수효과(이중 주문)가 됩니다.
- 해결: 주문 저장+홀드를 커밋(SUBMITTING)한 뒤 트랜잭션 밖에서 KIS 를 부르고 결과는 새 트랜잭션으로 반영합니다. 실패·타임아웃은 UNKNOWN 으로 남깁니다.

고친 모양(예시):
```
fun placeOrder(...): Order {
    val order = tx.execute { savePendingOrderAndHold(accountId, command) }   // 커밋
    if (order.mode.isKis()) {
        val result = runCatching { kis.placeOrder(order) }    // 트랜잭션 밖
        tx.execute { applyKisResult(order.id!!, result) }     // 새 트랜잭션
    }
    return order
}
```

### 20위 [high · 확신 high] KIS 주문 취소가 로컬 상태만 바꾸고 KIS 취소를 호출하지 않음

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/OrderCommandService.kt:238` · 항목표 reliability/failure-handling · 항목 I3

```
@Transactional
fun cancelOrder(accountId: Long, orderId: Long, command: CancelOrderCommand): Order {
...
    order.cancel()   // ← 로컬 상태만 CANCELLED, KIS 취소 호출 없음
...
// KisLiveOrderExecutor.kt:47  fun cancel(order: Order) { ... }   // ← 호출처 0건
```

- 원인: cancelOrder 는 order.cancel() 로 로컬 상태를 CANCELLED 로 바꾸고 예수금·수량 잠금을 풀 뿐 KIS 취소를 호출하지 않는다. 실행기에 cancel() 이 있으나 호출처가 없다(grep: executor 호출은 OrderCommandService.kt:173-174 의 submit 뿐). LocalMatchingScheduler 의 GTD 만료도 findExpiredOrders 가 모드를 거르지 않아 KIS 주문까지 같은 경로로 로컬에서만 취소한다.
- 생길 수 있는 일: 사용자가 취소하거나 GTD 가 만료된 KIS 주문이 KIS 에는 계속 살아 있다. 이후 체결되면 findActiveKisOrderByExternalOrderId 가 PENDING·PARTIAL 만 찾아 통지가 미매칭으로 버려진다. 실계좌는 체결됐는데 우리 잔고·포지션은 모르는 불일치가 된다.
- 해결: KIS 모드 주문은 CANCEL_REQUESTED 로 바꾸고 트랜잭션 밖에서 executor.cancel 을 호출한 뒤, 취소 확정 통지(또는 조회)가 오면 CANCELLED 와 잠금 해제를 반영한다. 취소 호출이 불확정이면 UNKNOWN 으로 두고 조회로 확정한다.

고친 모양(예시):
```
if (order.isExternal()) {
    order.requestCancel()          // CANCEL_REQUESTED, 홀드는 유지
    afterCommit { kisExecutor.cancel(order) }   // 트랜잭션 밖
} else {
    order.cancel(); releaseLocks(order)
}
```

### 21위 [high · 확신 high] 주문 조회 서비스가 리포지토리를 직접 주입받음

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/OrderQueryService.kt:8` · 항목표 backend/layering · 항목 L1

```
import com.papertrading.api.infrastructure.persistence.ExecutionRepository // ← application 이 infrastructure 리포지토리를 import
import com.papertrading.api.infrastructure.persistence.OrderRepository
...
class OrderQueryService(
    private val orderRepository: OrderRepository, // ← 조회 포트 없이 구현 주입
```

- 원인: ExecutionRepository·OrderRepository(infrastructure.persistence)를 import·주입한다(PTB8). ArchUnit 동결 색인 4건. searchOrders·searchExecutions 가 application 의 쿼리 객체를 받는 리포지토리 확장 메서드라 의존 방향도 거꾸로 묶인다.
- 생길 수 있는 일: 조회 시험이 리포지토리 목에 묶여 필터·정렬 동작이 검증되지 않는다.
- 해결: 조회 전용 출력 포트(OrderReader·ExecutionReader)를 application/order/port 에 두고 어댑터가 리포지토리를 감싸게 한다.

고친 모양(예시):
```
interface OrderReader {
    fun findByIdAndAccountId(orderId: Long, accountId: Long): Order?
    fun search(query: OrderListQuery): List<Order>
}
```

### 22위 [high · 확신 high] 주문 목록 유스케이스가 상한 인자 없이 전체를 돌려줌

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/OrderQueryService.kt:25` · 항목표 data/query · 항목 Q5

```
fun listOrders(query: OrderListQuery): List<OrderResult> {
    validateOrderRange(query)
    return orderRepository.searchOrders(query)   // ← 상한(size/limit) 인자 없음
        .map { OrderResult.from(it) }
```

- 원인: listOrders 는 검증으로 기간 순서만 보고 건수 상한·페이지 인자가 없습니다. 같은 결함의 유스케이스 쪽 자리이며 고치는 곳은 OrderRepositoryImpl.kt:19 와 OrderListQuery 입니다.
- 생길 수 있는 일: 목록 엔드포인트 한 번의 호출이 계정의 주문 이력 전체를 응답으로 만듭니다.
- 해결: 유스케이스 시그니처에 size·cursor 를 받아 최댓값(@Max 상당)을 검증하고 리포지토리에 넘깁니다.

### 23위 [high · 확신 high] 유스케이스가 infrastructure.persistence 의 Spring Data 리포지토리 3개를 직접 import·주입

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/portfolio/DailyBalanceCommandService.kt:5` · 항목표 backend/layering · 항목 L1

```
import com.papertrading.api.infrastructure.persistence.AccountRepository   // ← L5 infrastructure import
import com.papertrading.api.infrastructure.persistence.DailyBalanceRepository
import com.papertrading.api.infrastructure.persistence.PositionRepository
    private val accountRepository: AccountRepository,   // ← L16 생성자 주입
```

- 원인: AccountRepository·DailyBalanceRepository·PositionRepository 를 infrastructure 에서 import 하고 생성자로 주입받는다. PTB7·PTB8 은 유스케이스가 application.<ctx>.port 포트만 받으라고 한다. ArchUnit 동결 6건.
- 생길 수 있는 일: 단위 시험이 리포지토리 목을 요구하고(이 서비스의 시험도 every { repository… } 로 쿼리를 흉내 냄), 영속 방식을 바꾸면 유스케이스가 함께 바뀐다.
- 해결: application.portfolio.port 에 AccountPort·DailyBalancePort·PositionReadPort 를 두고 infrastructure 어댑터가 구현한다. 서비스는 포트만 주입받는다.

### 24위 [high · 확신 high] 조회 유스케이스가 infrastructure 리포지토리 2개를 직접 import·주입

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/portfolio/DailyBalanceQueryService.kt:6` · 항목표 backend/layering · 항목 L1

```
import com.papertrading.api.infrastructure.persistence.AccountRepository   // ← L6
import com.papertrading.api.infrastructure.persistence.DailyBalanceRepository   // ← L7
    private val dailyBalanceRepository: DailyBalanceRepository,   // ← L15 생성자 주입
```

- 원인: AccountRepository·DailyBalanceRepository 를 infrastructure.persistence 에서 import 해 생성자로 주입받는다. ArchUnit 동결 4건.
- 생길 수 있는 일: 조회 유스케이스 시험이 리포지토리에 묶이고 영속 구현 교체가 유스케이스를 건드린다.
- 해결: 조회 전용 포트(DailyBalanceReadPort)를 application.portfolio.port 에 두고 어댑터가 구현한다.

### 25위 [high · 확신 high] 유스케이스가 infrastructure.persistence 리포지토리 3개를 직접 import·주입

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/portfolio/PortfolioSnapshotCommandService.kt:7` · 항목표 backend/layering · 항목 L1

```
import com.papertrading.api.infrastructure.persistence.AccountRepository   // ← L7
import com.papertrading.api.infrastructure.persistence.PortfolioSnapshotRepository
import com.papertrading.api.infrastructure.persistence.PositionRepository
    private val marketQuotePort: MarketQuotePort,   // ← 포트는 쓰면서 영속은 리포지토리 직접 주입(일관성 없음)
```

- 원인: AccountRepository·PortfolioSnapshotRepository·PositionRepository 를 import·주입한다. 시세만 MarketQuotePort 로 받는다. ArchUnit 동결 6건.
- 생길 수 있는 일: 시세는 포트로 가짜를 꽂을 수 있지만 영속 쪽은 리포지토리 목에 의존해 시험이 쿼리를 흉내 낸다.
- 해결: 영속 포트 3개를 application.portfolio.port 에 두고 주입받는다.

### 26위 [high · 확신 high] 조회 유스케이스가 infrastructure 리포지토리 2개를 직접 import·주입

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/portfolio/PortfolioSnapshotQueryService.kt:5` · 항목표 backend/layering · 항목 L1

```
import com.papertrading.api.infrastructure.persistence.AccountRepository   // ← L5
import com.papertrading.api.infrastructure.persistence.PortfolioSnapshotRepository   // ← L6
    private val portfolioSnapshotRepository: PortfolioSnapshotRepository,   // ← L14
```

- 원인: AccountRepository·PortfolioSnapshotRepository 를 import·주입한다. ArchUnit 동결 4건.
- 생길 수 있는 일: 조회 유스케이스 시험이 리포지토리에 묶인다.
- 해결: 조회 전용 포트를 application.portfolio.port 에 두고 어댑터가 구현한다.

### 27위 [high · 확신 high] 유스케이스가 infrastructure 의 Spring Data 리포지토리를 직접 주입받음

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/portfolio/SnapshotJobRunTxService.kt:5` · 항목표 backend/layering · 항목 L1

```
import com.papertrading.api.infrastructure.persistence.SnapshotJobRunRepository  // ← application → infrastructure
class SnapshotJobRunTxService(
    private val snapshotJobRunRepository: SnapshotJobRunRepository,  // ← Spring Data 리포지토리 직접 주입
```

- 원인: application 파일이 infrastructure.persistence 를 import 하고 JpaRepository 확장 인터페이스를 생성자로 받는다(PTB7·PTB8). ArchUnit 동결 목록에 이 파일 2건이 있으나 동결은 해결이 아니다.
- 생길 수 있는 일: 단위 시험이 리포지토리 목에 묶이고 영속 구현이 바뀌면 유스케이스가 함께 깨진다.
- 해결: application/portfolio/port 에 SnapshotJobRunPort 를 두고 infrastructure 어댑터가 구현하게 한다.

고친 모양(예시):
```
// application/portfolio/port/SnapshotJobRunPort.kt
interface SnapshotJobRunPort {
    fun startAndFlush(account: Account, businessDate: LocalDate): SnapshotJobRun
    fun findById(runId: Long): SnapshotJobRun?
}
```

### 28위 [high · 확신 high] 유스케이스가 AccountRepository·SnapshotJobRunRepository 를 직접 주입받음

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/portfolio/SnapshotJobService.kt:8` · 항목표 backend/layering · 항목 L1

```
import com.papertrading.api.infrastructure.persistence.AccountRepository  // ← application → infrastructure
import com.papertrading.api.infrastructure.persistence.SnapshotJobRunRepository
class SnapshotJobService(
    private val accountRepository: AccountRepository,  // ← 직접 주입
    private val snapshotJobRunRepository: SnapshotJobRunRepository,
```

- 원인: infrastructure.persistence 의 Spring Data 리포지토리 2개를 생성자로 주입받는다(PTB7·PTB8). ArchUnit 동결 4건.
- 생길 수 있는 일: 계좌 잠금·최근 실행 조회 같은 쿼리 계약이 유스케이스에 새고 단위 시험이 리포지토리 목에 묶인다.
- 해결: 포트(`AccountLockPort`·`SnapshotJobRunPort`)로 바꿔 주입한다.

고친 모양(예시):
```
class SnapshotJobService(
    private val accountPort: AccountPort,
    private val snapshotJobRunPort: SnapshotJobRunPort,
    ...
)
```

### 29위 [high · 확신 high] 유스케이스가 AccountRepository 를 직접 주입받음

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/portfolio/TaxSummaryBatchService.kt:6` · 항목표 backend/layering · 항목 L1

```
import com.papertrading.api.infrastructure.persistence.AccountRepository  // ← application → infrastructure
class TaxSummaryBatchService(
    private val accountRepository: AccountRepository,  // ← 직접 주입
```

- 원인: infrastructure.persistence.AccountRepository 를 생성자로 받는다(PTB7·PTB8). ArchUnit 동결 3건.
- 생길 수 있는 일: 활성 계좌 목록 조회가 Spring Data 메서드 이름(`findByIsActiveTrue`)에 묶여 영속 변경이 유스케이스를 깬다.
- 해결: `ActiveAccountIdsPort` 같은 포트로 바꾼다.

고친 모양(예시):
```
interface ActiveAccountIdsPort { fun findActiveAccountIds(): List<Long> }
```

### 30위 [high · 확신 high] 유스케이스가 리포지토리 3개를 직접 주입받음

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/portfolio/TaxSummaryCommandService.kt:18` · 항목표 backend/layering · 항목 L1

```
import com.papertrading.api.infrastructure.persistence.AccountRepository  // ← application → infrastructure
import com.papertrading.api.infrastructure.persistence.TaxSummaryRepository
import com.papertrading.api.infrastructure.persistence.TaxSummaryRunRepository
    private val taxSummaryRepository: TaxSummaryRepository,  // ← 직접 주입
```

- 원인: AccountRepository·TaxSummaryRepository·TaxSummaryRunRepository 가 모두 JpaRepository 확장이다(PTB7·PTB8). ArchUnit 동결 9건으로 이 묶음에서 가장 많다.
- 생길 수 있는 일: 락 쿼리·`existsRunning` 쿼리 계약이 유스케이스에 묶이고 포트 가짜로 시험할 수 없다.
- 해결: TaxSummaryPort·TaxSummaryRunPort 등 포트로 옮긴다.

고친 모양(예시):
```
interface TaxSummaryRunPort {
    fun existsRunning(accountId: Long, taxYear: Int): Boolean
    fun start(run: TaxSummaryRun): TaxSummaryRun
}
```

### 31위 [high · 확신 high] 조회 유스케이스가 리포지토리를 직접 주입받음

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/portfolio/TaxSummaryQueryService.kt:10` · 항목표 backend/layering · 항목 L1

```
import com.papertrading.api.infrastructure.persistence.AccountRepository  // ← application → infrastructure
import com.papertrading.api.infrastructure.persistence.TaxSummaryRepository
    private val taxSummaryRepository: TaxSummaryRepository,  // ← 직접 주입
```

- 원인: AccountRepository·TaxSummaryRepository 를 직접 받는다(PTB7·PTB8). ArchUnit 동결 4건.
- 생길 수 있는 일: 조회 쿼리 계약이 유스케이스에 새어 시험이 리포지토리 목에 묶인다.
- 해결: 조회 포트(`TaxSummaryReadPort`)로 바꾼다.

고친 모양(예시):
```
interface TaxSummaryReadPort {
    fun findByAccountAndYear(accountId: Long, taxYear: Int): TaxSummary?
}
```

### 32위 [high · 확신 high] 유스케이스가 infrastructure 의 Spring Data 리포지토리를 직접 주입받는다

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/position/PositionCommandService.kt:7` · 항목표 backend/layering · 항목 L1

```
import com.papertrading.api.infrastructure.persistence.PositionRepository  // ←
...
class PositionCommandService(
    private val positionRepository: PositionRepository,
```

- 원인: application 파일이 infrastructure.persistence.PositionRepository 를 import 해 생성자 주입한다. PTB8 은 포트만 주입하라고 한다. ArchUnit 동결 목록에 2줄로 올라 있다.
- 생길 수 있는 일: 단위 시험이 DB 계층 타입에 묶여 포트 가짜로 대체할 수 없고 영속 구현 변경이 유스케이스에 번진다.
- 해결: application.position.port 에 출력 포트(PositionReader 등)를 정의하고 영속 어댑터가 구현한다.

고친 모양(예시):
```
// application.position.port
interface OpenPositionReader { fun findOpenByTickerAndMode(ticker: String, mode: TradingMode): List<Position> }
```

### 33위 [high · 확신 high] 유스케이스가 PositionExitTriggerRepository·PositionRepository 를 직접 주입받는다

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/position/PositionExitTriggerCommandService.kt:15` · 항목표 backend/layering · 항목 L1

```
import com.papertrading.api.infrastructure.persistence.PositionExitTriggerRepository  // ←
import com.papertrading.api.infrastructure.persistence.PositionRepository
...
    private val positionRepository: PositionRepository,
    private val positionExitTriggerRepository: PositionExitTriggerRepository,
```

- 원인: infrastructure.persistence 의 두 리포지토리를 import 해 생성자로 받는다. ArchUnit 동결 목록 4줄.
- 생길 수 있는 일: 락 조회(findByIdWithLock·findByIdForUpdate) 같은 영속 세부가 유스케이스에 새어 시험이 실제 리포지토리에 묶인다.
- 해결: 락 조회를 포트 메서드로 두고(findOpenForUpdate) application.position.port 로 옮긴다.

### 34위 [high · 확신 high] 유스케이스가 OrderRepository·PositionExitTriggerRepository·PositionRepository 를 직접 주입받는다

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/position/PositionExitTriggerOrchestrator.kt:10` · 항목표 backend/layering · 항목 L1

```
import com.papertrading.api.infrastructure.persistence.OrderRepository  // ←
import com.papertrading.api.infrastructure.persistence.PositionExitTriggerRepository
import com.papertrading.api.infrastructure.persistence.PositionRepository
```

- 원인: infrastructure.persistence 리포지토리 세 개를 생성자로 받는다. OrderRepository 는 다른 컨텍스트(order)의 영속 타입이다. ArchUnit 동결 목록 6줄.
- 생길 수 있는 일: position 유스케이스가 order 의 쿼리 모양(existsOpenOrderByAccountTickerSideWithoutGroup 등)에 직접 묶여 order 쪽 쿼리 변경이 이 유스케이스를 깬다.
- 해결: 주문 쪽 질의는 order 컨텍스트 포트(OpenSellOrderQuery)로, 트리거·포지션 질의는 position 포트로 나눈다.

### 35위 [high · 확신 high] 조회 유스케이스가 두 리포지토리를 직접 주입받는다

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/position/PositionExitTriggerQueryService.kt:6` · 항목표 backend/layering · 항목 L1

```
import com.papertrading.api.infrastructure.persistence.PositionExitTriggerRepository  // ←
import com.papertrading.api.infrastructure.persistence.PositionRepository
```

- 원인: PositionExitTriggerRepository·PositionRepository 를 infrastructure 에서 직접 import 한다. ArchUnit 동결 목록 4줄.
- 생길 수 있는 일: 조회 서비스도 포트 가짜로 시험할 수 없다.
- 해결: 조회용 출력 포트(PositionExitTriggerReader)를 application.position.port 에 둔다.

### 36위 [high · 확신 high] 조회 유스케이스가 PositionRepository 를 직접 주입받는다

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/position/PositionQueryService.kt:10` · 항목표 backend/layering · 항목 L1

```
import com.papertrading.api.infrastructure.persistence.PositionRepository  // ←
...
    private val positionRepository: PositionRepository,
    private val livePositionCachePort: LivePositionCachePort,
```

- 원인: infrastructure.persistence.PositionRepository 를 import 해 생성자로 받는다. ArchUnit 동결 목록 2줄. 같은 서비스가 쓰는 LivePositionCachePort·MarketQuotePort 는 포트 형태라 대조된다.
- 생길 수 있는 일: DB 조회만 포트 밖에 남아 서비스 한 개가 포트와 리포지토리를 섞어 쓴다.
- 해결: PositionRepository 조회를 포트(PositionReader)로 바꾼다.

### 37위 [high · 확신 high] 계좌 애그리거트 루트가 JPA 엔티티 그대로 domain 에 있음

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/account/Account.kt:10` · 항목표 backend/layering · 항목 D1

```
import jakarta.persistence.Column // ← 도메인 파일이 jakarta.persistence 를 import
import org.hibernate.annotations.Check // ← org.hibernate 도 import
@Entity // ← 도메인 모델이 곧 ORM 행
@Table(name = "accounts")
class Account protected constructor() : BaseAuditEntity() {
```

- 원인: Account 는 금액·전이 규칙을 가진 애그리거트 루트인데 jakarta.persistence·org.hibernate 를 import 하고 @Entity·@Table·@Column·@Check 를 직접 단다. PTB1(PR #50 판본)은 domain 이 순수 Kotlin 이어야 하고 JPA 엔티티는 infrastructure 영속 어댑터에만 둔다고 정한다. 이 레포 ArchUnit 동결 목록에 Account.kt 19건이 있지만 동결은 해결이 아니다.
- 생길 수 있는 일: 지연 로딩·더티 체킹·프록시가 deposit/availableDeposit/lockedDeposit 불변식 메서드를 우회할 수 있고, 도메인 단위 시험이 ORM 어노테이션에 묶인다.
- 해결: Account 를 순수 Kotlin 도메인 모델로 옮기고 infrastructure 영속 어댑터에 AccountJpaEntity 와 변환 매퍼를 둔다. 리포지토리 포트는 도메인 모델을 주고받게 한다.

고친 모양(예시):
```
// domain
class Account private constructor(val id: Long?, deposit: BigDecimal, /* … */) { fun withdraw(amount: BigDecimal) { /* 불변식 */ } }
// infrastructure.persistence
@Entity @Table(name = "accounts") class AccountJpaEntity(/* 컬럼 */) { fun toDomain(): Account = /* … */ }
```

### 38위 [high · 확신 high] 청산 트리거 기본값이 JPA 엔티티로 domain 에 있음

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/account/AccountExitTriggerDefault.kt:13` · 항목표 backend/layering · 항목 D1

```
@Entity // ← domain 파일에 @Entity
@Table(name = "account_exit_trigger_defaults")
class AccountExitTriggerDefault protected constructor() : BaseAuditEntity() {
    @Version // ← 영속 세부(낙관적 락)가 도메인에 있음
```

- 원인: jakarta.persistence import 와 @Entity·@Table·@Version 이 domain 파일에 있다. 금액·전이 규칙이 없는 설정 성격이라 main 판본 PTB1 Exception 이 허용하지만 PR #50 판본 기준으로는 발견이다. ArchUnit 동결 11건.
- 생길 수 있는 일: 도메인 모델과 ORM 행이 같아 검증 규칙을 모델에 넣기 어렵고(D2·D5 참고), 낙관적 락 필드가 도메인에 새어 나온다.
- 해결: 순수 Kotlin 도메인 모델 + infrastructure 영속 어댑터의 JPA 엔티티·매퍼로 분리한다. 단순 CRUD 예외를 사람이 채택하면 ruleConflicts 로 결정한다.

### 39위 [high · 확신 high] 청산 트리거 기본값의 모든 상태 필드가 공개 setter 이고 밖에서 대입됨

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/account/AccountExitTriggerDefault.kt:17` · 항목표 backend/layering · 항목 D2

```
@Column(name = "account_id", nullable = false, unique = true)
var accountId: Long = 0 // ← 공개 setter
@Column(name = "enabled", nullable = false)
var enabled: Boolean = false // ← 공개 setter
@Column(name = "stop_loss_percent", precision = 8, scale = 4)
var stopLossPercent: BigDecimal? = null // ← 공개 setter
// Account.kt:299  existing.enabled = enabled // ← 다른 클래스가 대입
```

- 원인: id·accountId·enabled·stopLossPercent·takeProfitPercent 가 private set 없는 var 이다. 실제로 Account.updateExitTriggerDefault 가 existing.enabled = …, existing.stopLossPercent = … 로 바깥에서 대입한다(Account.kt:299-301). 이름 붙은 변경 메서드와 검증이 이 클래스에 없다. Account 는 private set 으로 같은 일을 올바르게 한다.
- 생길 수 있는 일: 서비스·시험·다른 코드가 percent 범위·scale 검증(Account 안에 있음)을 건너뛰고 필드를 바로 바꿀 수 있어 잘못된 손절·익절 기준이 저장된다.
- 해결: 필드를 private set 으로 바꾸고 AccountExitTriggerDefault 에 change(enabled, stopLoss, takeProfit) 메서드를 두어 검증을 이 클래스로 옮긴다.

고친 모양(예시):
```
var enabled: Boolean = false
    private set

fun change(enabled: Boolean, stopLossPercent: BigDecimal?, takeProfitPercent: BigDecimal?) {
    validate(enabled, stopLossPercent, takeProfitPercent)
    this.enabled = enabled
    this.stopLossPercent = stopLossPercent
    this.takeProfitPercent = takeProfitPercent
}
```

### 40위 [high · 확신 high] 계좌 원장이 JPA 엔티티로 domain 에 있음

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/account/AccountLedger.kt:24` · 항목표 backend/layering · 항목 D1

```
@Entity // ← 도메인 파일에 @Entity
@Table(
    name = "account_ledger",
    uniqueConstraints = [UniqueConstraint(name = "uk_account_ledger_account_id_idempotency", columnNames = ["account_id", "idempotency_key"])] // ← 영속 제약이 도메인에
)
class AccountLedger protected constructor() : BaseTimeEntity() {
```

- 원인: 금액(amount·balanceAfter)을 담은 이력 엔티티가 jakarta.persistence 를 import 하고 @Entity·@Table·@UniqueConstraint 를 단다. PTB1 PR #50 위반이며 단순 CRUD 예외 대상이 아니다(금액이 있음). ArchUnit 동결 20건.
- 생길 수 있는 일: 원장은 감사 추적의 근거인데 ORM 프록시·더티 체킹이 도메인 객체와 섞여 불변 이력 보장이 어댑터가 아니라 어노테이션에 달려 있다.
- 해결: 원장 도메인 모델과 JPA 엔티티를 분리하고 멱등키 유일 제약은 영속 어댑터가 가진다.

### 41위 [high · 확신 high] 리스크 이벤트 이력이 JPA 엔티티로 domain 에 있음

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/account/RiskEvent.kt:20` · 항목표 backend/layering · 항목 D1

```
@Entity // ← 도메인 파일에 @Entity
@Table(name = "risk_events")
class RiskEvent protected constructor() : BaseTimeEntity() {
```

- 원인: jakarta.persistence import 와 @Entity·@Table·@ManyToOne 이 domain 에 있다. append-only 이력이라 main 판본 단순 CRUD 예외가 허용하지만 PR #50 기준 발견. ArchUnit 동결 18건.
- 생길 수 있는 일: 도메인 모델과 ORM 행이 같아 리스크 이력 생성 규칙(시각·필드 검증)을 모델에 둘 자리가 없다.
- 해결: 도메인 모델과 JPA 엔티티를 분리한다.

### 42위 [high · 확신 high] 리스크 정책이 JPA 엔티티로 domain 에 있음

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/account/RiskPolicy.kt:24` · 항목표 backend/layering · 항목 D1

```
@Entity // ← 도메인 파일에 @Entity
@Table(name = "risk_policies")
class RiskPolicy protected constructor() : BaseAuditEntity() {
```

- 원인: 한도 금액(maxDailyLoss·maxOrderAmount)을 가진 모델이 @Entity·@Table·@ManyToOne 을 단다. 금액이 있어 단순 CRUD 예외 대상이 아니다. ArchUnit 동결 14건.
- 생길 수 있는 일: 주문 접수 시 한도 검사의 근거 모델이 ORM 행이라 도메인 단위 시험·변환 경계가 흐려진다.
- 해결: 도메인 모델과 JPA 엔티티를 분리한다.

### 43위 [high · 확신 high] 도메인 베이스 클래스가 JPA·Hibernate 에 묶여 있음 (BaseAuditEntity)

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/base/BaseAuditEntity.kt:3` · 항목표 backend/layering · 항목 D1

```
import jakarta.persistence.Column                       // ← 도메인 파일이 JPA 를 import
import jakarta.persistence.MappedSuperclass
import org.hibernate.annotations.UpdateTimestamp         // ← 도메인 파일이 Hibernate 를 import
@MappedSuperclass                                        // ← 도메인 타입이 곧 ORM 매핑
abstract class BaseAuditEntity : BaseTimeEntity() {
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant? = null
```

- 원인: domain/** 파일이 jakarta.persistence.Column·MappedSuperclass 와 org.hibernate.annotations.UpdateTimestamp 를 import 하고 @MappedSuperclass·@Column·@UpdateTimestamp 를 단다. 레포 규칙 PTB1(PR #50 판본)은 도메인 모델을 순수 Kotlin 으로 두고 영속 매핑은 infrastructure 영속 어댑터에 두도록 정한다. ArchUnit 동결 목록(tools/T96587ad0.txt 161·620·746-748줄)에 이미 올라 있지만 동결은 해결이 아니다. detekt 출력(144·211·225줄)상 Account·PositionExitTrigger·Strategy 가 이 클래스를 상속한다.
- 생길 수 있는 일: 이 베이스를 상속하는 모든 도메인 엔티티가 영속 프레임워크 타입을 끌고 가, 도메인 모델을 순수 Kotlin 으로 옮기는 작업이 이 베이스에서 막힌다. 갱신 시각이 Hibernate 어노테이션으로만 채워져 도메인 코드가 시각을 직접 다룰 수 없다.
- 해결: 도메인 모델에서 감사 필드와 어노테이션을 걷어내고, createdAt·updatedAt 매핑은 infrastructure 영속 엔티티의 @MappedSuperclass 베이스로 옮긴다. 도메인은 필요하면 순수 값(Instant)만 가진다.

고친 모양(예시):
```
// infrastructure/persistence/BaseAuditJpaEntity.kt
@MappedSuperclass
abstract class BaseAuditJpaEntity {
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant? = null
        protected set
    @UpdateTimestamp @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant? = null
        protected set
}
```

### 44위 [high · 확신 high] 도메인 베이스 클래스가 JPA·Hibernate 에 묶여 있음 (BaseTimeEntity)

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/base/BaseTimeEntity.kt:3` · 항목표 backend/layering · 항목 D1

```
import jakarta.persistence.Column                 // ← 도메인 파일이 JPA 를 import
import jakarta.persistence.MappedSuperclass
import org.hibernate.annotations.CreationTimestamp // ← 도메인 파일이 Hibernate 를 import
@MappedSuperclass                                  // ← 도메인 타입이 곧 ORM 매핑
abstract class BaseTimeEntity {
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
```

- 원인: BaseAuditEntity 와 같은 뿌리다. domain/** 파일이 jakarta.persistence 와 org.hibernate.annotations.CreationTimestamp 를 import 하고 @MappedSuperclass 를 단다. ArchUnit 동결 목록(tools/T96587ad0.txt 163·621·749-751줄)에 있다. 파일당 하나로 올리며 BaseAuditEntity 와 함께 고쳐야 한다.
- 생길 수 있는 일: BaseAuditEntity 를 통해 모든 엔티티의 최상위 부모가 되므로, 이 클래스가 남아 있는 한 domain 패키지에서 JPA·Hibernate 의존이 사라지지 않는다.
- 해결: BaseAuditEntity 와 함께 infrastructure 영속 베이스로 옮기고 도메인 쪽 상속 사슬을 끊는다.

### 45위 [high · 확신 high] 도메인 파일이 JPA 엔티티를 겸한다

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/notification/Notification.kt:7` · 항목표 backend/layering · 항목 D1

```
import jakarta.persistence.Column   // ← 도메인 파일이 jakarta.persistence 를 import
import jakarta.persistence.Entity
// … L8–L17 jakarta.persistence.* import 계속
@Entity                              // ← 도메인 클래스가 JPA 엔티티
@Table(name = "notifications")
class Notification protected constructor() : BaseTimeEntity() {
```

- 원인: domain 패키지의 Notification 이 jakarta.persistence.* 11개를 import 하고 @Entity·@Table·@Column·@ManyToOne 을 단다. PTB1(PR #50 판본)은 도메인 모델을 순수 Kotlin 으로 두고 JPA 엔티티는 infrastructure 영속 어댑터에 두라고 한다. ArchUnit 동결 목록(25건)에 이미 있지만 동결은 해결이 아니라서 발견으로 남긴다.
- 생길 수 있는 일: 도메인 모델이 곧 DB 행이라 지연 로딩·더티 체킹·프록시가 markFailed 같은 불변식 메서드를 우회하고, 도메인 시험이 JPA 어노테이션 클래스패스에 묶인다.
- 해결: 순수 Kotlin 도메인 Notification 과 infrastructure/persistence 의 NotificationJpaEntity 를 나누고, 변환은 영속 어댑터가 맡는다. 리포지토리 포트는 도메인 모델을 주고받게 한다.

고친 모양(예시):
```
// domain
class Notification private constructor(val id: Long?, val eventType: NotificationEventType, status: DeliveryStatus /* … */) {
    var status: DeliveryStatus = status
        private set
}
// infrastructure/persistence
@Entity @Table(name = "notifications")
class NotificationJpaEntity(/* 컬럼 필드 */) { fun toDomain(): Notification = TODO() }
```

### 46위 [high · 확신 high] 도메인 Execution 이 JPA 엔티티

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/order/Execution.kt:27` · 항목표 backend/layering · 항목 D1

```
@Entity                                            // ← 도메인이 ORM 행
@Table(
    name = "executions",
    uniqueConstraints = [UniqueConstraint(name = "uk_executions_external_id", columnNames = ["external_execution_id"])]
)
class Execution protected constructor() : BaseTimeEntity() {
```

- 원인: @Entity·@Table(uniqueConstraints)·@ManyToOne·@Column 이 도메인 클래스에 붙어 있다. ArchUnit 동결 28건(tools/T96587ad0.txt:169-170).
- 생길 수 있는 일: 체결은 불변 기록이어야 하는데 Hibernate 프록시·더티 체킹이 걸려, 불변 보장이 JPA 구현에 의존한다.
- 해결: D1(FeePolicy)와 같은 방식으로 순수 Execution 도메인 + infrastructure JPA 엔티티·매퍼로 분리한다.

### 47위 [high · 확신 high] 도메인 Order(애그리거트 루트)가 JPA 엔티티 (@Version 포함)

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/order/Order.kt:32` · 항목표 backend/layering · 항목 D1

```
@Entity                                            // ← 애그리거트 루트가 ORM 행
@Table(
    name = "orders",
    uniqueConstraints = [UniqueConstraint(name = "uk_orders_account_idempotency", columnNames = ["account_id", "idempotency_key"])]
)
class Order protected constructor() : BaseAuditEntity() {
```

- 원인: 애그리거트 루트가 @Entity·@Table·@ManyToOne·@Version 을 직접 단다. ArchUnit 동결 42건(tools/T96587ad0.txt:171-172)으로 이 묶음에서 가장 많다.
- 생길 수 있는 일: applyExecution·cancel 의 상태 전이 검사가 JPA 프록시·더티 체킹 뒤에서 우회될 수 있고, 낙관적 락(@Version) 같은 영속 관심사가 도메인에 새어 있다.
- 해결: Order 를 순수 도메인 클래스로 두고 OrderJpaEntity + 영속 어댑터를 infrastructure 에 둔다. @Version 은 JPA 엔티티의 몫이다.

고친 모양(예시):
```
// domain
class Order private constructor(val id: Long?, val accountId: Long, /* … */) {
    fun applyExecution(fillQty: BigDecimal, fillPrice: BigDecimal, fillFee: BigDecimal) { /* 전이 검사 */ }
}
// infrastructure.persistence
@Entity @Table(name = "orders") class OrderJpaEntity(/* 컬럼 */) { @Version var version: Int = 0 }
class OrderPersistenceAdapter(private val jpa: OrderJpaRepository) : OrderRepositoryPort { /* 변환 */ }
```

### 48위 [high · 확신 high] 도메인 OrderAmendment 가 JPA 엔티티

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/order/OrderAmendment.kt:19` · 항목표 backend/layering · 항목 D1

```
@Entity                                            // ← 도메인이 ORM 행
@Table(name = "order_amendments")
class OrderAmendment protected constructor() : BaseTimeEntity() {
```

- 원인: @Entity·@Table·@ManyToOne 을 도메인이 직접 단다. ArchUnit 동결 16건(tools/T96587ad0.txt:173-174). 단순 기록 엔티티라 main 판본 PTB1 예외에 해당할 수 있어 ruleConflicts 에도 올린다.
- 생길 수 있는 일: 감사 기록이 JPA 모델에 묶여 영속 구조 변경이 도메인 변경이 된다.
- 해결: D1(FeePolicy)와 같은 분리. 필드만 있는 얇은 기록이면 PR #50 예외 여부를 사람이 정한다.

고친 모양(예시):
```
class OrderAmendment private constructor(val orderId: Long, val type: AmendmentType, /* … */)
```

### 49위 [high · 확신 high] 도메인 OrderSignal 이 JPA 엔티티이고 Hibernate 타입(JdbcTypeCode)까지 의존

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/order/OrderSignal.kt:29` · 항목표 backend/layering · 항목 D1

```
import org.hibernate.annotations.JdbcTypeCode      // ← L20 도메인이 Hibernate 의존
import org.hibernate.type.SqlTypes                 // ← L21
...
@Entity                                            // ← L29 도메인이 ORM 행
@Table(
```

- 원인: @Entity·@Table 뿐 아니라 org.hibernate.annotations.JdbcTypeCode·SqlTypes 를 import 해 JSONB 매핑을 도메인에 둔다. ArchUnit 동결 30건(tools/T96587ad0.txt:175-176).
- 생길 수 있는 일: 도메인이 Hibernate 버전에 직접 묶이고 conditionSnapshot 의 JSON 매핑이 도메인 변경 사유가 된다.
- 해결: JSONB 매핑은 infrastructure JPA 엔티티로 옮기고 도메인에는 순수 String/값 객체만 둔다.

### 50위 [high · 확신 high] domain 파일이 jakarta.persistence·org.hibernate 를 import 하고 @Entity 를 직접 단다

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/portfolio/DailyBalance.kt:5` · 항목표 backend/layering · 항목 D1

```
import jakarta.persistence.Column   // ← L5 domain 의 jakarta.persistence import
import org.hibernate.annotations.UpdateTimestamp   // ← L15 org.hibernate import
@Entity   // ← L20 도메인 클래스가 JPA 엔티티
class DailyBalance protected constructor() : BaseTimeEntity() {
```

- 원인: 도메인 모델이 곧 JPA 엔티티다. PR #50 판본 PTB1 은 도메인 모델을 순수 Kotlin 으로 두고 JPA 엔티티·변환은 infrastructure 영속 어댑터에 두라고 한다. ArchUnit 동결 목록에 20건으로 올라 있으나 동결은 해결이 아니다.
- 생길 수 있는 일: 지연 로딩·더티 체킹·프록시가 refresh 같은 불변식 메서드를 우회할 수 있고, 도메인 단위 시험이 JPA 어노테이션과 BaseTimeEntity 에 묶인다.
- 해결: 순수 Kotlin DailyBalance 도메인 모델을 두고, JPA 엔티티(DailyBalanceJpaEntity)와 매퍼를 infrastructure 영속 어댑터에 둔다. 리포지토리는 application.portfolio.port 에 도메인 모델을 주고받는 포트로 정의한다.

### 51위 [high · 확신 high] domain 파일이 jakarta.persistence·org.hibernate 를 import 하고 @Entity 를 직접 단다

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/portfolio/PortfolioSnapshot.kt:5` · 항목표 backend/layering · 항목 D1

```
import jakarta.persistence.Column   // ← L5
import org.hibernate.annotations.UpdateTimestamp   // ← L15
@Entity   // ← L20
class PortfolioSnapshot protected constructor() : BaseTimeEntity() {
```

- 원인: ArchUnit 동결 목록에 22건. DailyBalance D1 과 같은 구조다.
- 생길 수 있는 일: 도메인 모델이 곧 ORM 행이라 지연 로딩·더티 체킹이 불변식 메서드를 우회하고 도메인 시험이 JPA 에 묶인다.
- 해결: 순수 도메인 모델 + infrastructure 의 JPA 엔티티·매퍼·포트로 분리한다.

### 52위 [high · 확신 high] domain 파일이 jakarta.persistence 를 import 하고 @Entity 를 직접 단다

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/portfolio/SnapshotJobRun.kt:6` · 항목표 backend/layering · 항목 D1

```
import jakarta.persistence.Column   // ← L6
@Entity   // ← L19
@Table(name = "snapshot_job_runs")
class SnapshotJobRun protected constructor() : BaseAuditEntity() {
```

- 원인: ArchUnit 동결 목록에 17건. 같은 구조다.
- 생길 수 있는 일: 도메인 모델이 ORM 행이라 상태 전이 메서드를 JPA 가 우회할 수 있고 도메인 시험이 JPA 에 묶인다.
- 해결: 순수 도메인 모델 + infrastructure 의 JPA 엔티티·매퍼·포트로 분리한다.

### 53위 [high · 확신 high] domain 파일이 jakarta.persistence 를 import 하고 @Entity 를 직접 단다

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/portfolio/TaxSummary.kt:6` · 항목표 backend/layering · 항목 D1

```
import jakarta.persistence.Column   // ← L6
@Entity   // ← L27
class TaxSummary protected constructor() : BaseAuditEntity() {
```

- 원인: ArchUnit 동결 목록에 19건. 같은 구조다.
- 생길 수 있는 일: 세금 요약의 금액 필드와 상태가 ORM 행으로 노출돼 정규화·검증 메서드를 우회하는 경로가 열린다.
- 해결: 순수 도메인 모델 + infrastructure 의 JPA 엔티티·매퍼·포트로 분리한다.

### 54위 [high · 확신 high] domain 파일이 jakarta.persistence 를 import 하고 @Entity 를 직접 단다

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/portfolio/TaxSummaryRun.kt:7` · 항목표 backend/layering · 항목 D1

```
import jakarta.persistence.Column   // ← L7
@Entity   // ← L20
class TaxSummaryRun protected constructor() : BaseTimeEntity() {
```

- 원인: ArchUnit 동결 목록에 20건. 같은 구조다.
- 생길 수 있는 일: 실행 기록의 상태 전이가 ORM 행 위에서 일어나 도메인 시험이 JPA 에 묶인다.
- 해결: 순수 도메인 모델 + infrastructure 의 JPA 엔티티·매퍼·포트로 분리한다.

### 55위 [high · 확신 high] Position 도메인 모델이 JPA·Hibernate 엔티티 그 자체다

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/position/Position.kt:6` · 항목표 backend/layering · 항목 D1

```
import jakarta.persistence.Column  // ←
import jakarta.persistence.Entity
import org.hibernate.annotations.Check
...
@Entity
@Table(
    name = "positions",
```

- 원인: domain 파일이 jakarta.persistence 와 org.hibernate.annotations.Check 를 import 하고 @Entity·@Table·@Column·@ManyToOne 을 단다. ArchUnit 동결 목록에 30줄로 올라 있지만 동결은 해결이 아니라 발견으로 남긴다. Position 은 applyBuy·lockQuantity 같은 불변식 메서드를 가진 단순 CRUD 가 아닌 모델이라 PTB1 PR #50 예외에 해당하지 않는다.
- 생길 수 있는 일: 지연 로딩 프록시와 더티 체킹이 lockQuantity·applySell 같은 불변식 메서드를 우회할 수 있고, 도메인 단위 시험이 JPA 어노테이션에 묶인다.
- 해결: Position 을 순수 Kotlin 도메인 모델로 두고, JPA 엔티티와 매퍼를 infrastructure 영속 어댑터로 옮긴다.

고친 모양(예시):
```
// domain
class Position private constructor(val accountId: Long, val ticker: String, quantity: BigDecimal) { ... }
// infrastructure.persistence
@Entity @Table(name = "positions") class PositionJpaEntity(...)
fun PositionJpaEntity.toDomain(): Position
```

### 56위 [high · 확신 high] PositionExitTrigger 도메인 모델이 JPA 엔티티 그 자체다

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/position/PositionExitTrigger.kt:11` · 항목표 backend/layering · 항목 D1

```
import jakarta.persistence.Column  // ←
import jakarta.persistence.Entity
...
import jakarta.persistence.Version
...
@Entity
@Table(name = "position_exit_triggers")
```

- 원인: jakarta.persistence.* 와 @Entity·@Table·@Column·@Version·@Enumerated 를 domain 파일이 직접 쓴다. ArchUnit 동결 목록에 28줄로 올라 있다. 상태 전이 메서드(cancel·markTriggered 등)와 낙관적 락 @Version 이 있는 모델이라 단순 CRUD 예외가 아니다.
- 생길 수 있는 일: @Version 충돌과 프록시가 도메인 메서드의 상태 전이 검사와 섞여 도메인 규칙만 따로 시험할 수 없다.
- 해결: 순수 도메인 모델과 infrastructure JPA 엔티티를 나누고 영속 어댑터가 변환한다. @Version 은 JPA 엔티티 쪽에 둔다.

### 57위 [high · 확신 high] 도메인 Settlement 가 JPA 엔티티

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/settlement/Settlement.kt:28` · 항목표 backend/layering · 항목 D1

```
@Entity                                            // ← 도메인이 ORM 행
@Table(
    name = "settlements",
    uniqueConstraints = [UniqueConstraint(name = "uk_settlements_order", columnNames = ["order_id"])]
)
class Settlement protected constructor() : BaseTimeEntity() {
```

- 원인: @Entity·@Table·@OneToOne·@ManyToOne·@Column 이 도메인에 있다. ArchUnit 동결 25건(tools/T96587ad0.txt:193-194).
- 생길 수 있는 일: 정산 금액 계산 불변식(createFromExecution)이 JPA 프록시·더티 체킹 뒤에서 우회될 수 있다.
- 해결: D1(FeePolicy)와 같은 분리.

### 58위 [high · 확신 high] 도메인 SettlementExecution(조인 엔티티)이 JPA 엔티티

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/settlement/SettlementExecution.kt:20` · 항목표 backend/layering · 항목 D1

```
@Entity                                            // ← 도메인이 ORM 행
@Table(
    name = "settlement_executions",
    uniqueConstraints = [UniqueConstraint(name = "uk_settlement_executions", columnNames = ["settlement_id", "execution_id"])]
)
class SettlementExecution protected constructor() : BaseTimeEntity() {
```

- 원인: @Entity·@Table(uniqueConstraints)·@ManyToOne 이 도메인에 있다. ArchUnit 동결 16건(tools/T96587ad0.txt:195-196). 필드만 있는 조인 엔티티라 main 판본 PTB1 예외에 해당할 수 있어 ruleConflicts 에도 올린다.
- 생길 수 있는 일: 조인 테이블 구조가 도메인 모델에 박혀 정산-체결 연결 방식을 바꾸면 도메인이 바뀐다.
- 해결: D1(FeePolicy)와 같은 분리. 조인 정보는 Settlement 도메인에 executionIds 로 두는 방안도 있다.

### 59위 [high · 확신 high] 도메인 Strategy 가 JPA 엔티티(jakarta.persistence·Hibernate 상속)

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/strategy/Strategy.kt:7` · 항목표 backend/layering · 항목 D1

```
import jakarta.persistence.Entity   // ← 도메인 파일이 jakarta.persistence 를 import
...
@Entity   // ← 도메인 모델이 곧 ORM 행
@Table(name = "strategies")
class Strategy protected constructor() : BaseAuditEntity() {
```

- 원인: domain 파일이 jakarta.persistence.* 를 import 하고 @Entity·@Table·@Column·@ManyToOne 을 달며, 상속하는 BaseAuditEntity 도 @MappedSuperclass·@UpdateTimestamp(Hibernate)다. PTB1(PR #50 판본) 위반이다. ArchUnit 동결 위반 28줄(tools/T96587ad0.txt:665-666·1079-1104)에 이미 있으나 동결은 해결이 아니다.
- 생길 수 있는 일: 승인 상태 전이(requestReview·approve·activate)가 JPA 지연 로딩·더티 체킹과 같은 객체에 묶여, 매퍼·유스케이스가 영속 프록시 상태를 우회해 불변식을 깨뜨릴 수 있다.
- 해결: 순수 Kotlin 도메인 모델 Strategy 를 domain 에 두고, JPA 엔티티·변환은 infrastructure/persistence 어댑터로 옮긴다. 리포지토리 포트는 application/strategy/port 에 둔다.

고친 모양(예시):
```
// domain
class Strategy private constructor(val id: StrategyId?, val accountId: Long, name: String, ...) { fun approve() { ... } }
// infrastructure/persistence
@Entity @Table(name = "strategies") class StrategyJpaEntity(...)
```

### 60위 [high · 확신 high] 도메인 StrategyDerivation 이 JPA 엔티티

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/strategy/StrategyDerivation.kt:4` · 항목표 backend/layering · 항목 D1

```
import jakarta.persistence.Entity   // ← 도메인 파일이 jakarta.persistence 를 import
...
@Entity   // ←
@Table(name = "strategy_derivations")
class StrategyDerivation protected constructor() : BaseTimeEntity() {
```

- 원인: domain 파일이 jakarta.persistence.* 를 import 하고 @Entity·@Table 을 단다. 필드만 있는 얇은 모델에 가깝지만 JPA 엔티티를 domain 에 두는 예외는 PR #50 판본에 없다. ArchUnit 동결 17줄(tools/T96587ad0.txt:667-668).
- 생길 수 있는 일: 도메인 모델이 ORM 행이라 영속 계층 교체·시험 때 DB 없이 단위로 다룰 수 없다.
- 해결: 순수 도메인 모델과 infrastructure JPA 엔티티로 나눈다(위 Strategy D1 과 같은 이전).

고친 모양(예시):
```
class StrategyDerivation private constructor(val parentStrategyId: Long, val childStrategyId: Long, val type: DerivationType, val note: String?)
```

### 61위 [high · 확신 high] 도메인 StrategyLog 가 JPA·Hibernate 엔티티

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/strategy/StrategyLog.kt:5` · 항목표 backend/layering · 항목 D1

```
import jakarta.persistence.Entity   // ← JPA import
import org.hibernate.annotations.JdbcTypeCode   // ← Hibernate import (L14)
...
@Entity   // ←
@Table(name = "strategy_logs")
class StrategyLog protected constructor() : BaseTimeEntity() {
```

- 원인: domain 파일이 jakarta.persistence.* 와 org.hibernate.annotations.JdbcTypeCode·org.hibernate.type.SqlTypes 를 import 하고 @Entity·@Table 을 단다. ArchUnit 동결 20줄(tools/T96587ad0.txt:669-670, 1126).
- 생길 수 있는 일: JSONB 매핑(Hibernate 타입)까지 도메인 모델에 박혀 있어 Hibernate 버전 변경이 도메인 파일을 건드린다.
- 해결: 순수 도메인 모델과 infrastructure JPA 엔티티로 나눈다.

고친 모양(예시):
```
class StrategyLog private constructor(val strategyId: Long, val accountId: Long, val level: LogLevel, val message: String, val context: String?, val loggedAt: Instant)
```

### 62위 [high · 확신 high] 도메인 StrategyPerformanceSnapshot 이 JPA 엔티티

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/strategy/StrategyPerformanceSnapshot.kt:4` · 항목표 backend/layering · 항목 D1

```
import jakarta.persistence.Entity   // ← JPA import
...
@Entity   // ←
@Table(name = "strategy_performance_snapshots")
class StrategyPerformanceSnapshot protected constructor() : BaseTimeEntity() {
```

- 원인: domain 파일이 jakarta.persistence.* 를 import 하고 @Entity·@Table 을 단다. ArchUnit 동결 17줄(tools/T96587ad0.txt:671-672).
- 생길 수 있는 일: 성과 검증 규칙(validateMetrics)이 JPA 객체에 묶여 DB 없이 쓰기 어렵다.
- 해결: 순수 도메인 모델과 infrastructure JPA 엔티티로 나눈다.

고친 모양(예시):
```
class StrategyPerformanceSnapshot private constructor(val strategyId: Long, val period: ClosedRange<LocalDate>, val metrics: PerformanceMetrics)
```

### 63위 [high · 확신 high] 도메인 StrategyVersion 이 JPA·Hibernate 엔티티

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/strategy/StrategyVersion.kt:4` · 항목표 backend/layering · 항목 D1

```
import jakarta.persistence.Entity   // ← JPA import
import org.hibernate.annotations.JdbcTypeCode   // ← Hibernate import (L14)
...
@Entity   // ←
@Table(
    name = "strategy_versions",
    uniqueConstraints = [UniqueConstraint(name = "uk_strategy_versions_strategy_version", columnNames = ["strategy_id", "version_no"])]
)
```

- 원인: domain 파일이 jakarta.persistence.* 와 org.hibernate.* 를 import 하고 @Entity·@Table(uniqueConstraints 포함)을 단다. ArchUnit 동결 17줄(tools/T96587ad0.txt:673-675, 1160).
- 생길 수 있는 일: 유일 제약·JSONB 매핑 같은 영속 세부가 도메인 모델에 박혀 있다.
- 해결: 순수 도메인 모델과 infrastructure JPA 엔티티로 나눈다. 유일 제약은 어댑터 쪽에 둔다.

고친 모양(예시):
```
class StrategyVersion private constructor(val strategyId: Long, val versionNumber: Int, val rules: String, ...)
```

### 64위 [high · 확신 high] 포트가 domain/port 에 있고 시그니처가 application 타입을 반환해 의존 방향이 뒤집힘

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/port/MarketQuotePort.kt:1` · 항목표 backend/layering · 항목 L3

```
package com.papertrading.api.domain.port // ← 포트가 domain/port 에 있음

import com.papertrading.api.application.common.result.QuoteSnapshot // ← domain 이 application 타입을 import — 의존 방향 역전

interface MarketQuotePort {
    fun getQuote(tradingMode: TradingMode, ticker: String): QuoteSnapshot? // ← 시그니처가 domain·port 밖 타입을 반환
```

- 원인: MarketQuotePort 는 domain/port 에 있으면서 application.common.result.QuoteSnapshot 을 import 하고 반환한다. domain 이 application 타입에 의존해 PTB7(application → domain, 안쪽으로만)이 거꾸로 된다. 포트 요청·결과 타입은 같은 port 패키지에 두라는 PTB8 과도 어긋난다. ArchUnit 동결 목록 211·272·604행에 같은 위반이 올라 있다.
- 생길 수 있는 일: domain 패키지가 application 구현 세부(result 패키지)에 묶여 순수성 검사가 사실상 무력화되고, QuoteSnapshot 을 고치면 domain 컴파일이 깨진다. 주문·포트폴리오·포지션 유스케이스 4곳이 이 포트를 주입받아 영향이 넓다.
- 해결: 포트와 QuoteSnapshot 을 함께 application/common/port 로 옮긴다(결과 타입도 같은 port 패키지). 그러면 domain → application import 가 사라지고 RedisMarketQuoteAdapter·QuoteEventListener 의 L4 도 같이 풀린다.

고친 모양(예시):
```
package com.papertrading.api.application.common.port

data class QuoteSnapshot(/* 기존 필드 */)

interface MarketQuotePort {
    fun getQuote(tradingMode: TradingMode, ticker: String): QuoteSnapshot?
}
```

### 65위 [high · 확신 high] 주문 트랜잭션(계좌 락 보유) 안에서 타임아웃 없는 RestTemplate 로 collector-api 호출

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/client/CollectorSubscriptionAdapter.kt:19` · 항목표 data/transaction-boundary · 항목 X3

```
// CollectorSubscriptionAdapter.kt:19-23
private val restTemplate = RestTemplate()   // ← 타임아웃 없는 자체 RestTemplate (properties 의 timeout 값 미사용)
override fun subscribe(mode: String, ticker: String) {
    val url = "${properties.baseUrl}/api/internal/subscriptions/$ticker?mode=$mode"
    runCatching { restTemplate.postForObject(url, null, String::class.java) }   // ← 원격 HTTP 호출
// OrderCommandService.kt:49-66
@Transactional
fun placeOrder(accountId: Long, command: PlaceOrderCommand): Order {
    val account = accountRepository.findByIdWithLock(accountId)   // ← 계좌 행 PESSIMISTIC_WRITE 락 보유
    collectorSubscriptionPort.subscribe(tradingMode.name, command.ticker)   // ← 열린 트랜잭션 안에서 collector-api 호출
```

- 원인: OrderCommandService.placeOrder 는 @Transactional 이고 findByIdWithLock 으로 계좌 행에 PESSIMISTIC_WRITE 락을 잡은 채 collectorSubscriptionPort.subscribe 를 부른다(OrderCommandService.kt:49-66). 어댑터는 주입된 RestTemplate 빈이 아니라 자체 RestTemplate() 을 만들어 연결·읽기 타임아웃이 없다. CollectorApiProperties 에 connectTimeoutMs=2000·readTimeoutMs=3000 이 있지만 어댑터는 쓰지 않는다. runCatching 은 예외만 삼킬 뿐 멈춤은 막지 못한다. 레포 규칙 PTD2·DA1 위반. ExecutionProcessor.kt:250 의 tryUnsubscribe 도 같은 포트로 unsubscribe 를 부르며, 그 경로의 트랜잭션 여부는 이 청크에서 끝까지 확인하지 못했다.
- 생길 수 있는 일: collector-api 가 응답하지 않으면 주문 요청이 무한정 걸려 계좌 락과 DB 커넥션(Hikari 최대 10)을 붙잡는다. 같은 계좌의 입출금·다른 주문이 락 대기에 쌓여 주문 경로 전체가 멈춘다.
- 해결: 구독 요청을 트랜잭션 밖(커밋 뒤 이벤트 또는 유스케이스 바깥 호출)으로 옮기고, 어댑터는 CollectorApiProperties 의 타임아웃을 건 RestTemplate 을 쓴다.

고친 모양(예시):
```
private val restTemplate = RestTemplateBuilder()
    .connectTimeout(Duration.ofMillis(properties.connectTimeoutMs))
    .readTimeout(Duration.ofMillis(properties.readTimeoutMs))
    .build()
```

### 66위 [high · 확신 high] collector-api 구독 호출이 타임아웃 없는 RestTemplate 을 직접 만들어 씀

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/client/CollectorSubscriptionAdapter.kt:19` · 항목표 reliability/failure-handling · 항목 E5

```
private val restTemplate = RestTemplate()  // ← 타임아웃 없는 기본 생성
...
runCatching { restTemplate.postForObject(url, null, String::class.java) }
// CollectorApiProperties.kt:10-11
var connectTimeoutMs: Long = 2000  // ← 선언만 되고 사용처 없음
var readTimeoutMs: Long = 3000
// OrderCommandService.kt:49-66
@Transactional
fun placeOrder(...) { ... accountRepository.findByIdWithLock(accountId) ... collectorSubscriptionPort.subscribe(tradingMode.name, command.ticker)  // ← 락·트랜잭션 안에서 호출
```

- 원인: 어댑터가 주입받은 타임아웃 설정 빈(RestTemplateConfig 의 5초·10초)을 쓰지 않고 RestTemplate() 을 직접 만든다. 기본 생성은 SimpleClientHttpRequestFactory 기본값이라 연결·읽기 타임아웃이 없다. CollectorApiProperties 에 connectTimeoutMs=2000·readTimeoutMs=3000 이 선언돼 있지만 src/main 어디서도 읽히지 않는다(죽은 설정). 이 호출은 OrderCommandService.placeOrder 의 @Transactional 안(계좌 PESSIMISTIC_WRITE 락을 잡은 뒤)에서 불리고 ExecutionProcessor 에서도 불린다.
- 생길 수 있는 일: collector-api 가 응답하지 않으면 주문 요청 스레드가 무한히 붙고, 그동안 계좌 행 락과 DB 커넥션(풀 10개)을 쥔다. 같은 계좌의 다른 요청과 체결이 줄줄이 막히고 풀이 마른다. 'best-effort 라 주문을 막지 않는다'는 KDoc 의도와 반대로 동작한다.
- 해결: 어댑터에서 RestTemplateBuilder 로 properties 의 연결·읽기 타임아웃을 적용한 전용 RestTemplate 을 만든다. 더 근본적으로는 구독 호출을 트랜잭션 커밋 뒤로 옮긴다(transaction-boundary 렌즈 소관).

고친 모양(예시):
```
private val restTemplate = RestTemplateBuilder()
    .connectTimeout(Duration.ofMillis(properties.connectTimeoutMs))
    .readTimeout(Duration.ofMillis(properties.readTimeoutMs))
    .build()
```

### 67위 [high · 확신 high] infrastructure 어댑터가 application 서비스(비포트)를 직접 호출

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/kis/KisExecutionWebSocketManager.kt:4` · 항목표 backend/layering · 항목 L4

```
import com.papertrading.api.application.order.KisExecutionNoticeService // ← infrastructure 가 application 서비스 클래스를 import
...
    private val noticeService: KisExecutionNoticeService,
...
        noticeService.handle(notice) // ← 어댑터가 유스케이스를 직접 호출
```

- 원인: KIS 웹소켓 관리자가 application.order.KisExecutionNoticeService 를 import 하고 주입받아 수신한 체결 통지를 handle() 로 바로 넘긴다(PTB7). ArchUnit 동결 색인 2건(infrastructure → application 비포트).
- 생길 수 있는 일: 의존 방향이 뒤집혀 어댑터가 유스케이스를 몰고, application ↔ infrastructure 순환이 자랄 길이 열린다.
- 해결: 체결 통지 수신을 application/order/port 의 입력 계약(콜백 또는 이벤트)으로 분리하고, 연결 관리자는 그 계약에만 의존하게 한다. 유스케이스를 부르는 진입점은 presentation 으로 옮긴다.

고친 모양(예시):
```
fun interface ExecutionNoticeSink {
    fun accept(notice: KisExecutionNotice)
}
// 진입점(presentation)이 ExecutionNoticeSink 구현으로 KisExecutionNoticeService.handle 을 호출
```

### 68위 [high · 확신 high] 재연결 작업의 Future 를 버려 예외가 조용히 사라지고 재연결 사슬이 끊김

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/kis/KisExecutionWebSocketManager.kt:56` · 항목표 reliability/load-concurrency · 항목 A3

```
executor.schedule(action, delayMillis, TimeUnit.MILLISECONDS)   // ← 반환 Future 를 버림, action 예외를 아무도 읽지 않음
...
reconnectScheduler.schedule(mode, nextAttempt, delayMillis) {
    connectMode(mode, resetAttempts = false)   // ← 안에서 getWebSocketApprovalKey(REST) 가 던질 수 있음
}
```

- 원인: ThreadingKisExecutionWebSocketReconnectScheduler.schedule 은 executor.schedule 의 ScheduledFuture 를 읽지 않는다. 예약된 action(connectMode)은 KIS 승인키 발급 REST 호출(getWebSocketApprovalKey)을 포함해 네트워크 오류로 던질 수 있는데, 그 예외는 Future 안에 갇혀 로그가 남지 않고 다음 재연결도 예약되지 않는다. connectMode 는 이미 상태를 CONNECTING 으로 올려 두므로 상태도 거기서 멈춘다.
- 생길 수 있는 일: KIS 장애 직후 승인키 발급이 한 번 실패하면 로그 한 줄 없이 체결 통지 연결이 영구히 죽는다. 상태 조회는 CONNECTING 으로 보여 정상 시도 중인 것처럼 속인다.
- 해결: action 을 try/catch 로 감싸 실패를 ERROR 로그하고 다음 재연결을 다시 예약한다(attempt 증가). 상태는 실패 시 DISCONNECTED 로 되돌린다.

고친 모양(예시):
```
executor.schedule({
    runCatching(action).onFailure { log.error(it) { "재연결 실패 mode=$mode" }; onFailure(it) }
}, delayMillis, TimeUnit.MILLISECONDS)
```

### 69위 [high · 확신 high] KIS 체결 반영이 WebSocket 통지 한 통로뿐 — 대조·복구 경로가 죽어 있음

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/kis/KisExecutionWebSocketManager.kt:143` · 항목표 reliability/failure-handling · 항목 I6

```
if (!properties.executionWsEnabled) return   // ← 기본 false(application.yaml:45) — 체결 수신 자체가 꺼져 있음
// KisPaperOrderExecutor.kt:61
fun pollFills() = Unit   // ← 폴링 대체 경로가 비어 있음
// KisPaperPollingScheduler.kt:6-9
class KisPaperPollingScheduler(...) { fun pollFills() = kisPaperOrderExecutor.pollFills() }   // ← @Component·@Scheduled 없는 죽은 클래스
// KisOrderRestClient.kt:96
fun inquireFills(order: Order, mode: String): List<FillResult> {   // ← 호출처 0건
```

- 원인: KIS 체결은 WebSocket 통지로만 반영되고, 기본값은 연결 자체가 꺼져 있다(execution-ws-enabled 기본 false). 폴링 대체 경로는 비어 있다. KisPaperOrderExecutor.pollFills 는 Unit, KisPaperPollingScheduler 는 @Component·@Scheduled 가 없는 죽은 클래스이고, KisOrderRestClient.inquireFills 는 호출처가 없다. KisLiveOrderExecutor 주석은 이 스케줄러가 체결 확인을 처리한다고 쓰여 있어 사실과 다르다. 또 통지가 주문 트랜잭션 커밋 전에 도착하면 externalOrderId 가 보이지 않아 KisExecutionNoticeService.kt:29-44 가 log.error 후 버린다(KIS 호출이 커밋 전에 일어나는 I3 의 결과).
- 생길 수 있는 일: 연결이 끊긴 사이·재시작 중·커밋 직전에 온 체결은 영구히 사라진다. 주문은 PENDING 으로 남고 예수금과 포지션이 실계좌와 어긋나며, 이를 바로잡을 대조 작업이 없다.
- 해결: 주기 작업으로 PENDING·PARTIAL 인 KIS 주문을 inquireFills 로 조회해 체결 통지와 같은 처리기(fillExternal, externalExecutionId 로 중복 제거)에 태운다. WebSocket 재연결 직후에도 같은 대조를 한 번 돌린다.

고친 모양(예시):
```
@Scheduled(fixedDelay = 60_000)
fun reconcileKisFills() {
    orderRepository.findPendingKisOrders().forEach { o ->
        kisOrderRestClient.inquireFills(o, mode(o)).forEach { f -> executionProcessor.fillExternal(o.id!!, f.executedPrice, f.executedQty, externalId(o, f), now) }
    }
}
```

### 70위 [high · 확신 high] infrastructure 가 port 밖 application 패키지(query·result)를 import

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/AccountLedgerRepositoryCustom.kt:3` · 항목표 backend/layering · 항목 L4

```
import com.papertrading.api.application.account.query.LedgerFilter      // ← port 밖 application 패키지(query) import
import com.papertrading.api.application.account.result.LedgerResult      // ← port 밖 application 패키지(result) import
```

- 원인: AccountLedgerRepositoryCustom 이 application.account.query.LedgerFilter 와 application.account.result.LedgerResult 를 import 한다. PTB7 은 infrastructure 가 application 중 *.port 패키지만 보도록 한다. ArchUnit 동결 색인에 이 파일 2건이 남아 있다.
- 생길 수 있는 일: 어댑터가 유스케이스 쪽 타입에 묶여 의존 역전이 뒤집힌다. result·query 타입이 바뀌면 영속 계층이 같이 깨지고 application↔infrastructure 순환이 자란다.
- 해결: 조회 요청·결과 타입을 application/account/port 로 옮기거나(포트 요청·결과는 port 패키지 소유) 포트가 도메인·원시 타입만 주고받게 한다.

고친 모양(예시):
```
// application/account/port/LedgerQueryPort.kt
data class LedgerQuery(val transactionType: TransactionType?, val from: Instant?, val to: Instant?, val offset: Long, val limit: Int)
interface LedgerQueryPort { fun findLedgers(accountId: Long, query: LedgerQuery): LedgerPage }
```

### 71위 [high · 확신 high] Querydsl 구현체가 port 밖 application 타입을 import 하고 필터를 직접 읽음

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/AccountLedgerRepositoryImpl.kt:3` · 항목표 backend/layering · 항목 L4

```
import com.papertrading.api.application.account.query.LedgerFilter      // ← port 밖 application 패키지 import
import com.papertrading.api.application.account.result.LedgerResult
...
            .offset(filter.pageable.offset)      // ← application 타입의 필드를 어댑터가 직접 읽음
```

- 원인: AccountLedgerRepositoryImpl 이 application.account.query.LedgerFilter·application.account.result.LedgerResult 를 import 하고 filter.pageable 의 offset·pageSize 를 직접 읽는다. 동결 색인에 이 파일 4건이 남아 있다.
- 생길 수 있는 일: Custom 인터페이스와 같은 원인으로 영속 구현이 유스케이스 내부 타입에 묶인다. 필터 타입이 바뀌면 쿼리 어댑터가 함께 깨진다.
- 해결: Custom 인터페이스를 port 로 옮기면서 요청·결과 타입도 port 패키지에 두고, 어댑터는 그 타입만 사용한다. Spring Page 대신 포트 소유의 페이지 결과를 쓴다.

### 72위 [high · 확신 high] 주문 목록 조회에 건수 상한이 없고 기간 조건도 선택값

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/OrderRepositoryImpl.kt:19` · 항목표 data/query · 항목 Q5

```
override fun searchOrders(query: OrderListQuery): List<Order> =
    queryFactory.select(o).from(o).where(o.account.id.eq(query.accountId), /* 나머지는 선택 필터 */)
        .orderBy(o.createdAt.desc(), o.id.desc())
        .fetch()   // ← limit·Pageable 없음 — 계정 전체 주문을 한 번에 로딩
```

- 원인: searchOrders 는 accountId 만 필수이고 ticker·status·기간은 모두 선택값입니다. `.fetch()` 앞에 limit·Pageable 이 없고 OrderListQuery 에도 size 필드가 없습니다. 주문은 삭제되지 않고 계속 쌓입니다. OrderQueryService.listOrders(:25)가 이를 그대로 노출합니다.
- 생길 수 있는 일: 필터 없이 호출하면 한 계정의 전체 주문 이력을 엔티티로 한 번에 메모리에 올립니다. 자동매매 계정은 주문이 빠르게 늘어 응답 지연과 OOM 위험이 있습니다.
- 해결: OrderListQuery 에 size(최대값 검증)와 키셋 커서(createdAt, id)를 받고 쿼리에 `.limit(size + 1)` 을 겁니다.

고친 모양(예시):
```
queryFactory.select(o).from(o)
    .where(o.account.id.eq(accountId), cursor?.let { o.createdAt.lt(it.createdAt).or(o.createdAt.eq(it.createdAt).and(o.id.lt(it.id))) })
    .orderBy(o.createdAt.desc(), o.id.desc())
    .limit(size.coerceAtMost(100) + 1L)
    .fetch()
```

### 73위 [high · 확신 high] 도메인 패키지의 JPA 엔티티(Position)에서 Querydsl Q 클래스가 생성되고 영속 어댑터가 그것을 직접 사용

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/PositionRepositoryImpl.kt:4` · 항목표 backend/layering · 항목 D1

```
import com.papertrading.api.domain.entity.position.QPosition.position  // ← 도메인 패키지의 JPA 엔티티에서 Q 클래스 생성
// Position.kt:46-48 (열람)
@ManyToOne(fetch = FetchType.LAZY, optional = false)
@JoinColumn(name = "account_id", nullable = false)
lateinit var account: Account  // ← domain.entity 클래스가 jakarta.persistence 를 직접 사용
```

- 원인: QPosition 이 domain.entity.position 패키지에 생성된다는 것은 Position 이 도메인 모델이 아니라 jakarta.persistence 어노테이션을 단 JPA 엔티티로 domain 안에 있다는 뜻이다. Position.kt 는 @ManyToOne·@JoinColumn·@Column·@Enumerated·@Check 를 직접 단다(PTB1 PR #50 위반, ArchUnit 동결 목록에 있어도 발견). PositionExitTrigger 와 JpaRepository<Position, Long>·JpaRepository<PositionExitTrigger, Long> 도 같은 뿌리라 파일당 하나로 여기에 올린다.
- 생길 수 있는 일: 도메인 모델이 곧 ORM 행이라 지연 로딩·더티 체킹·프록시가 Position.applySell·lockQuantity 같은 불변식 메서드를 우회한다. 컬럼·연관 변경이 도메인 모델 변경이 된다.
- 해결: Position·PositionExitTrigger 를 순수 Kotlin 도메인 모델로 두고 JPA 엔티티·Q 클래스·매퍼는 infrastructure.persistence 로 옮긴다. 리포지토리 포트는 도메인 모델을 주고받게 한다. 한 번에 어렵다면 포지션 컨텍스트부터 단계 이전한다.

고친 모양(예시):
```
// infrastructure/persistence/position/PositionJpaEntity.kt
@Entity @Table(name = "positions")
class PositionJpaEntity(...) { fun toDomain(): Position = ... }
```

### 74위 [high · 확신 high] infrastructure 리스너가 application 유스케이스 3개를 직접 주입받아 호출함

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/redis/QuoteEventListener.kt:4` · 항목표 backend/layering · 항목 L4

```
import com.papertrading.api.application.order.LocalMatchingEngine // ← infrastructure 가 application 유스케이스를 import
import com.papertrading.api.application.position.PositionExitQuoteEventListener // ← 같음
import com.papertrading.api.application.position.PositionCommandService // ← 같음
...
import com.papertrading.api.application.common.result.QuoteSnapshot // ← port 밖 application 패키지
...
class QuoteEventListener(
    private val localMatchingEngine: LocalMatchingEngine, // ← 유스케이스 직접 주입
    private val positionCommandService: PositionCommandService,
    private val positionExitQuoteEventListener: PositionExitQuoteEventListener,
```

- 원인: QuoteEventListener 는 infrastructure 인데 application.order.LocalMatchingEngine, application.position.PositionCommandService, application.position.PositionExitQuoteEventListener 와 application.common.result.QuoteSnapshot 을 import 하고 생성자로 주입받아 onMessage 에서 직접 부른다. PTB7 은 infrastructure → application.<ctx>.port·application.common.port 만 허용한다. ArchUnit 동결 목록 1172~1240행에 생성자·필드·호출이 줄줄이 올라 있다.
- 생길 수 있는 일: 어댑터가 유스케이스를 모는 형태라 의존 역전이 뒤집히고 infrastructure ↔ application 순환이 자란다. 이 리스너를 시험하려면 매칭 엔진·포지션 서비스 전체가 필요하고, 새 시세 소비자가 생길 때마다 infrastructure 클래스를 고치게 된다.
- 해결: Redis 메시지 파싱만 하는 진입점을 presentation 으로 옮기고 application 에 시세 수신 유스케이스 하나(QuoteReceivedService.onQuote)를 둬 그 안에서 매칭·포지션 갱신·청산 트리거로 나눠 부른다. 리스너는 유스케이스 하나만 부른다.

고친 모양(예시):
```
// presentation/redis/QuoteMessageListener.kt
@Component
class QuoteMessageListener(
    private val quoteReceivedService: QuoteReceivedService,
    private val objectMapper: ObjectMapper,
) : MessageListener {
    override fun onMessage(message: Message, pattern: ByteArray?) {
        val quote = parseMessage(message.body) ?: return
        quoteReceivedService.onQuote(quote)
    }
}
```

### 75위 [high · 확신 high] RedisMessageListenerContainer 에 taskExecutor 가 없고 리스너가 onMessage 에서 매칭·DB 쓰기를 직접 함

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/redis/RedisListenerConfig.kt:17` · 항목표 reliability/load-concurrency · 항목 A1

```
        val container = RedisMessageListenerContainer()   // ← setTaskExecutor 없음 → 메시지마다 새 스레드
        container.setConnectionFactory(connectionFactory)
...
// QuoteEventListener.kt:34
        runCatching { localMatchingEngine.tryMatchPendingOrders(quote.ticker, quote) }   // ← onMessage 에서 매칭·DB 쓰기 직접 수행
```

- 원인: 컨테이너에 setTaskExecutor 호출이 없어 기본 SimpleAsyncTaskExecutor(메시지마다 새 스레드, 상한 없음)가 쓰인다. 등록된 QuoteEventListener.onMessage 는 같은 스레드에서 매칭과 포지션 갱신·청산 판정 같은 DB 쓰기를 직접 호출한다. 큐 상한·거부 정책도 없다.
- 생길 수 있는 일: 시세가 몰리면 스레드가 무제한으로 생기고 각 스레드가 DB 연결(hikari 풀 10개)을 다투어 OOM 이나 연결 풀 고갈로 이어진다.
- 해결: 상한 있는 ThreadPoolTaskExecutor(큐 상한·거부 정책)를 container.setTaskExecutor 로 지정하거나, onMessage 는 역직렬화 후 종목별 큐에 넘기기만 한다.

고친 모양(예시):
```
val executor = ThreadPoolTaskExecutor().apply {
    corePoolSize = 4; maxPoolSize = 4; setQueueCapacity(1000)
    setRejectedExecutionHandler(ThreadPoolExecutor.DiscardOldestPolicy()); initialize()
}
container.setTaskExecutor(executor)
```

### 76위 [high · 확신 high] infrastructure 가 application 서비스·명령을 직접 import (ArchUnit 동결 2건)

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/scheduler/LocalMatchingScheduler.kt:3` · 항목표 backend/layering · 항목 L4

```
import com.papertrading.api.application.order.OrderCommandService   // ← infrastructure → application 서비스
import com.papertrading.api.application.order.command.CancelOrderCommand   // ← port 밖 application 패키지
```

- 원인: OrderCommandService 와 command.CancelOrderCommand 를 import 해 cancelOrder 를 직접 호출한다(PTB7: infrastructure 는 application 의 port 만 import 허용). L6 과 같은 뿌리. T96587ad0.txt 244-245행.
- 생길 수 있는 일: 어댑터가 유스케이스를 몰아 의존 역전이 뒤집히고, 서비스 변경이 infrastructure 컴파일을 깨뜨리며 순환이 자란다.
- 해결: 스케줄러를 presentation 으로 옮기고 application 의 유스케이스 하나만 부르게 한다(L6 참고).

### 77위 [high · 확신 high] infrastructure 가 presentation DTO 를 import (ArchUnit 동결 1건) — presentation↔infrastructure 순환

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/sse/ExecutionSseEventHandler.kt:4` · 항목표 backend/layering · 항목 L4

```
import com.papertrading.api.presentation.dto.sse.ExecutionSseEvent   // ← infrastructure → presentation import
// …
val sseEvent = ExecutionSseEvent.from(event)   // ← 어댑터가 응답 DTO 로 변환
```

- 원인: ExecutionSseEvent(presentation.dto.sse)를 import 해 ExecutionSseEvent.from(event) 로 변환한다. T96587ad0.txt 246-247·250-251행에 따르면 ExecutionSseController(presentation)가 infrastructure.sse 를 다시 import 해 두 패키지가 서로를 가리킨다(PTB7).
- 생길 수 있는 일: presentation↔infrastructure 순환이 굳어 SSE 응답 모양을 바꿀 때 어댑터까지 같이 고쳐야 하고, 순환 때문에 계층 분리·모듈화가 막힌다.
- 해결: 변환 책임을 presentation 쪽 리스너로 옮기거나, 레지스트리가 도메인 이벤트(ExecutionFilledEvent)를 받고 DTO 변환은 presentation 에서 하게 한다.

고친 모양(예시):
```
// presentation 쪽
@Component
class ExecutionSseEventListener(private val registry: ExecutionSseRegistryPort) {
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun on(event: ExecutionFilledEvent) = registry.broadcast(ExecutionSseEvent.from(event))
}
```

### 78위 [high · 확신 high] infrastructure 의 SSE 레지스트리가 presentation DTO 를 import (의존 방향 역전)

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/sse/ExecutionSseRegistry.kt:3` · 항목표 backend/layering · 항목 L4

```
import com.papertrading.api.presentation.dto.sse.ExecutionSseEvent  // ← infrastructure 가 presentation DTO 를 import
    fun broadcast(event: ExecutionSseEvent) {  // ← 어댑터 공개 시그니처가 presentation 타입
```

- 원인: ExecutionSseRegistry 가 presentation.dto.sse.ExecutionSseEvent 를 import 해 공개 함수 broadcast 의 인자 타입으로 쓴다. PTB7(PR #50)은 infrastructure 가 presentation 을 import 하지 못하게 한다. ArchUnit 동결 목록(tools/T96587ad0.txt, 'infrastructure → presentation' 규칙, 동결 3줄)에 올라 있지만 동결은 해결이 아니다. 같은 뿌리로 ExecutionSseController(presentation)가 이 레지스트리를 import 한다(L5 발견). 클래스당 한 건으로, 같은 파일 ExecutionSseEventHandler 의 위반은 이 묶음 밖이다.
- 생길 수 있는 일: 의존이 양방향이라 SSE 페이로드 필드를 바꾸면 어댑터·컨트롤러가 함께 깨지고, 동결 목록이 줄지 않으며 순환이 자란다.
- 해결: SseEmitter 는 HTTP 전송 상태이므로 ExecutionSseRegistry 와 ExecutionSseEventHandler 를 presentation(예: presentation/sse)으로 옮긴다 — presentation → domain.event 는 PTB7 허용 방향이다. 그러면 이 L4 와 컨트롤러의 L5 가 함께 풀린다.

고친 모양(예시):
```
// presentation/sse/ExecutionSseRegistry.kt
package com.papertrading.api.presentation.sse

@Component
class ExecutionSseRegistry {
    fun broadcast(event: ExecutionSseEvent) { /* 그대로 */ }
}
// ExecutionSseEventHandler(@EventListener ExecutionFilledEvent)도 같은 패키지로 이동
```

### 79위 [high · 확신 high] 컨트롤러가 infrastructure 어댑터를 직접 import·주입 (presentation → infrastructure)

`backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/controller/ExecutionSseController.kt:3` · 항목표 backend/layering · 항목 L5

```
import com.papertrading.api.infrastructure.sse.ExecutionSseRegistry  // ← presentation 이 infrastructure 를 import
    private val registry: ExecutionSseRegistry,  // ← 어댑터 구현을 컨트롤러가 직접 주입
```

- 원인: ExecutionSseController 가 infrastructure.sse.ExecutionSseRegistry 를 import 하고 생성자로 주입받는다. PTB7(PR #50)은 presentation 이 application·domain.enums·domain.event·common 만 쓰게 한다. ArchUnit 동결 목록에 2줄(생성자·필드) + 호출 1줄로 올라 있다. 레지스트리를 presentation 으로 옮기면 L4 와 함께 해소된다.
- 생길 수 있는 일: HTTP 진입점이 어댑터 구현에 묶여 레지스트리 교체·단위 시험이 어렵고, 계층 규칙 동결 목록이 늘어난 채 남는다.
- 해결: ExecutionSseRegistry 를 presentation 패키지로 이동(L4 fix 와 동일)하거나, application 포트(구독 등록)를 두고 컨트롤러는 포트만 부른다.

고친 모양(예시):
```
import com.papertrading.api.presentation.sse.ExecutionSseRegistry  // presentation 내부 의존으로 바뀜
```

### 80위 [high · 확신 high] 응답 DTO 가 JPA 엔티티(Account·AccountLedger)를 직접 받아 변환한다

`backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/dto/account/AccountResponse.kt:8` · 항목표 backend/layering · 항목 L5

```
import com.papertrading.api.domain.entity.account.Account  // ← presentation 이 domain.entity 를 import
import com.papertrading.api.domain.entity.account.AccountLedger  // ← 같음
...
        fun from(account: Account) = AccountResponse(  // ← JPA 엔티티를 DTO 팩토리 인자로 받음
            id = account.id!!,
```

- 원인: presentation 파일이 domain.entity.account.Account·AccountLedger 를 import 하고 from(account)·from(ledger) 가 엔티티 필드를 그대로 읽는다. PTB7(PR #50 판본)은 presentation 이 엔티티를 요청·응답에 쓰지 못하게 하고 application result 나 presentation DTO 만 허용한다. 같은 파일에 LedgerResult 를 받는 from(result) 오버로드가 이미 있어 올바른 형태가 한 파일 안에 함께 있다. ArchUnit 동결 저장소에도 이 파일 위반 2건이 올라 있다(동결은 해결이 아니다).
- 생길 수 있는 일: 엔티티 필드명·널 가능성·지연 로딩 필드가 API 응답 계약에 묶인다. 컬럼 이름이나 엔티티 구조를 바꾸면 클라이언트가 깨지고, 컨트롤러·서비스가 엔티티를 계속 위로 올려 보내게 된다.
- 해결: application 에 AccountResult(필드 복사만 하는 result)를 두고 AccountResponse.from(result: AccountResult) 로 바꾼다. LedgerResponse.from(ledger: AccountLedger) 는 쓰는 곳이 없는지 확인해 지우고 from(LedgerResult) 만 남긴다.

고친 모양(예시):
```
// application/account/result/AccountResult.kt
data class AccountResult(val id: Long, val accountName: String, val deposit: BigDecimal /* … */)

// presentation/dto/account/AccountResponse.kt
fun from(result: AccountResult) = AccountResponse(id = result.id, accountName = result.accountName, deposit = result.deposit /* … */)
```

### 81위 [high · 확신 high] 응답 DTO 가 JPA 엔티티(RiskPolicy)를 직접 받아 변환한다

`backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/dto/account/RiskPolicyDto.kt:3` · 항목표 backend/layering · 항목 L5

```
import com.papertrading.api.domain.entity.account.RiskPolicy  // ← presentation 이 domain.entity 를 import
...
        fun from(policy: RiskPolicy) = RiskPolicyResponse(  // ← 엔티티를 DTO 팩토리 인자로 받음
            id = policy.id!!,
```

- 원인: presentation 파일이 domain.entity.account.RiskPolicy 를 import 하고 from(policy) 가 엔티티를 받는다. RiskPolicyController.kt:22·30-38 은 서비스가 돌려준 RiskPolicy 엔티티(RiskPolicyService.kt:18·37 의 반환 타입)를 그대로 이 from 에 넘긴다. 컨트롤러 시그니처는 DTO 만 써서 컨트롤러에는 별도 발견을 달지 않고 뿌리인 이 자리에서 한 번만 올린다. ArchUnit 동결 저장소에 이 파일 위반 1건이 있다.
- 생길 수 있는 일: 엔티티 필드가 응답 계약이 되고, 서비스가 엔티티를 돌려주는 한 컨트롤러가 영속 객체를 만지게 된다. 엔티티 변경이 API 를 조용히 바꾼다.
- 해결: application 에 RiskPolicyResult 를 두고 RiskPolicyService 가 그것을 돌려주게 한 뒤 RiskPolicyResponse.from(result: RiskPolicyResult) 로 바꾼다. 서비스 쪽 변경은 다른 묶음 파일이다.

고친 모양(예시):
```
data class RiskPolicyResult(val id: Long, val maxPositionRatio: BigDecimal?, val maxDailyLoss: BigDecimal?, val maxOrderAmount: BigDecimal?, val isActive: Boolean)

fun from(result: RiskPolicyResult) = RiskPolicyResponse(id = result.id, maxPositionRatio = result.maxPositionRatio, maxDailyLoss = result.maxDailyLoss, maxOrderAmount = result.maxOrderAmount, isActive = result.isActive)
```

### 82위 [high · 확신 high] 응답 DTO 가 domain.entity 를 import 하고 엔티티를 받는 매퍼를 가짐

`backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/dto/order/OrderResponse.kt:10` · 항목표 backend/layering · 항목 L5

```
import com.papertrading.api.domain.entity.order.Execution  // ← presentation DTO 가 domain.entity 를 import
import com.papertrading.api.domain.entity.order.Order
        fun from(order: Order) = OrderResponse(  // ← 엔티티를 받아 requireNotNull 로 영속 null 을 방어하는 매퍼
```

- 원인: OrderResponse.kt 가 domain.entity.order.Execution·Order 를 import 하고 from(order: Order)·from(e: Execution) 에서 엔티티 getter 를 읽는다. PTB7(PR #50) 상 presentation 은 domain 중 enums·event 만 쓸 수 있다. ArchUnit 동결 목록에 이 파일 2건이 있다. 같은 파일에 application result 용 from(OrderResult)·from(ExecutionResult) 오버로드(L47·L81)가 이미 있어 같은 매핑이 두 벌이다. 컨트롤러의 서비스 반환 타입과 엔티티 오버로드 호출처는 범위 밖이라 열어 보지 못했다.
- 생길 수 있는 일: 엔티티 필드 변경이 API 응답 계약을 깨고, 매핑이 두 벌이라 한쪽만 고쳐져 응답이 갈릴 수 있다.
- 해결: 엔티티 오버로드 from(Order)·from(Execution) 과 두 import 를 지우고 OrderResult·ExecutionResult 만 받는다. 호출 쪽이 엔티티를 넘기고 있으면 서비스가 result 로 매핑해 돌려주게 바꾼다.

고친 모양(예시):
```
data class OrderResponse(/* 필드 동일 */) {
    companion object {
        fun from(order: OrderResult) = OrderResponse(orderId = order.orderId, /* … */)
    }
}
```

### 83위 [high · 확신 high] 같은 레포 서비스 3개를 mockk 로 갈음해 시험함

`backend/trading-api/src/test/kotlin/com/papertrading/api/application/portfolio/SnapshotJobServiceTest.kt:30` · 항목표 backend/tdd-purity · 항목 T1

```
private val snapshotJobRunTxService = mockk<SnapshotJobRunTxService>()  // ← 내부 서비스를 목으로
private val dailyBalanceCommandService = mockk<DailyBalanceCommandService>()
private val portfolioSnapshotCommandService = mockk<PortfolioSnapshotCommandService>()
```

- 원인: SnapshotJobRunTxService·DailyBalanceCommandService·PortfolioSnapshotCommandService 는 내부 협력자인데 mockk 로 대체했다. 반환·예외를 시험이 지어내므로 REQUIRES_NEW 경계와 실제 재계산 호출은 이 시험으로 확인되지 않는다.
- 생길 수 있는 일: 서비스 시그니처·호출 방식을 바꾸면 시험이 깨지고, 반대로 실제 결합이 틀려도 초록이다.
- 해결: 내부 협력자는 실제 객체나 가짜 포트 구현으로 조립하고, 영속 경계는 포트 가짜·통합 시험으로 맡긴다.

고친 모양(예시):
```
val service = SnapshotJobService(fakeAccountPort, fakeJobRunPort, SnapshotJobRunTxService(fakeJobRunPort), DailyBalanceCommandService(...), PortfolioSnapshotCommandService(...))
```

### 84위 [high · 확신 med] 놓친 시세 구간의 고가·저가로 미체결 지정가·손절선을 다시 판정하는 복구 경로가 없다

`(repo)` · 항목표 reliability/load-concurrency · 항목 C11

```
// infrastructure/redis/QuoteEventListener.kt
32:    override fun onMessage(message: Message, pattern: ByteArray?) {
34:        runCatching { localMatchingEngine.tryMatchPendingOrders(quote.ticker, quote) }  // ← 틱 한 건이 유일한 매칭 진입점, 실패는 runCatching 으로 흡수, 재시도·구간 재판정 없음
// infrastructure/scheduler/LocalMatchingScheduler.kt
23:    @Scheduled(fixedDelay = 30_000)
24:    fun cancelExpiredGtdOrders() {  // ← 이름과 달리 GTD 만료 취소만 한다. 미체결 지정가의 가격 구간 재대조가 아니다
// infrastructure/notification/SlackNotificationScheduler.kt
17:    @Scheduled(fixedDelay = 30_000)
18:    fun retryPending() {  // ← 위치(notification)상 알림 재전송. 주문 재판정이 아니다
// application/position/PositionExitTriggerEvaluator.kt
34:            TriggerType.STOP_LOSS -> quotePrice <= threshold  // ← 단일 틱 가격만 비교. 놓친 구간의 저가로 재판정하지 않는다
```

- 원인: main 소스 전체에서 @Scheduled 는 두 곳뿐이다. LocalMatchingScheduler.cancelExpiredGtdOrders 는 GTD 만료 주문 취소(findExpiredOrders)만 하고, SlackNotificationScheduler.retryPending 은 알림 재전송이다. 둘 다 가격 구간 재대조가 아니다. LocalMatchingEngine.tryMatchPendingOrders 의 호출처는 QuoteEventListener.onMessage(Redis Pub/Sub 시세 한 건)뿐이다. 손절 판정도 PositionExitTriggerEvaluator 가 틱 하나의 quotePrice 와 임계값을 비교하는 형태다(STOP_LOSS -> quotePrice <= threshold). 구간 저가·고가를 쓰는 재판정은 발견되지 않았다. 시작 시점 훅인 ApplicationReadyEvent 는 KisExecutionWebSocketManager 하나뿐이라 재시작 뒤 LOCAL 미체결 주문을 재대조하는 경로도 없다. RedisMessageListenerContainer 는 구독을 되살리더라도 끊긴 동안의 메시지는 되찾지 못한다. 매칭 실패도 runCatching 으로 흡수되어 같은 틱을 다시 시도하지 않는다. LLM-only 판정이며, 아래 notes 의 미확인 항목 때문에 confidence 를 med 로 둔다.
- 생길 수 있는 일: Pub/Sub 은 at-most-once 다. 서버 재시작·Redis 단절·리스너 예외로 놓친 틱 사이에 가격이 지정가나 손절선을 건드렸다가 돌아오면, 이후 틱은 이미 임계값 밖이라 그 체결·청산이 영구히 일어나지 않는다. 모의투자 계좌의 체결 누락과 손절 미발동으로 이어진다.
- 해결: @Scheduled 복구 작업을 추가한다. 마지막으로 처리한 시세 시각(Redis 또는 DB 에 저장)부터 지금까지의 고가·저가를 종목별로 구해, 미체결 지정가와 손절·익절 조건을 다시 대조한다. 같은 작업을 애플리케이션 시작 직후에도 한 번 돌린다. 재판정은 종목 단위로 직렬화해 실시간 매칭과 같은 주문을 동시에 체결하지 않게 하고, 체결 쪽에는 멱등 키를 둔다. 다중 인스턴스라면 분산 단일 실행 임대(C3)도 필요하다.

고친 모양(예시):
```
// 예시 — 포트·메서드 이름은 가칭
@Scheduled(fixedDelay = 60_000)
fun recoverMissedQuoteRange() {
    val since = lastProcessedQuoteAtStore.get()  // 마지막 처리 시각 저장소
    val now = clock.instant()
    pendingOrderRepository.findAllPendingLimitOrders().groupBy { it.ticker }.forEach { (ticker, orders) ->
        val range = marketQuotePort.highLowBetween(ticker, since, now)  // 구간 고가·저가
        orders.forEach { order ->
            if (order.isTouchedBy(range.high, range.low)) localMatchingEngine.tryMatchOne(order, range.asQuote())
        }
    }
    lastProcessedQuoteAtStore.set(now)
}
```

### 85위 [high · 확신 med] trading-api 에 인증이 전혀 없고, 인증 없음의 근거(네트워크 격리 등) 문서도 없다

`(repo)` · 항목표 reliability/security-config-hygiene · 항목 S10

```
backend/trading-api/build.gradle:33  implementation 'org.springframework.boot:spring-boot-starter-actuator'
backend/trading-api/build.gradle:37  implementation 'org.springframework.boot:spring-boot-starter-web'   // ← main 의존성에 spring-boot-starter-security 없음 (SecurityFilterChain 등 인증 코드 grep 0건)
backend/trading-api/build.gradle:53  testImplementation 'org.springframework.security:spring-security-test'   // ← 시험 전용만 남아 있고 운영 인증은 없음
OrderController.kt:27  @RequestMapping("/api/v1/accounts/{accountId}/orders")
OrderController.kt:32      @PostMapping   // ← 주문 생성: 인증 없이 열림
OrderController.kt:81      @DeleteMapping("/{orderId}")   // ← 주문 취소: 인증 없이 열림
AccountController.kt:90      @PostMapping("/{id}/deposit")   // ← 입금: 인증 없이 열림
AccountController.kt:110      @PostMapping("/{id}/withdraw")   // ← 출금: 인증 없이 열림
application.yaml:41-47  live-app-key / live-app-secret / live-ws-url / live-execution-channel   // ← KIS 실전 설정 존재 (값은 ***)
docs/done/front/realtime-execution/spec.md:226  - **인증**: 현재 시스템에 인증 없음 → SSE endpoint도 인증 없음. 향후 인증 추가 시 별도 phase.   // ← 인증 없음의 근거가 아니라 현황 서술
```

- 원인: main 의존성에 spring-boot-starter-security 가 없다. 인증 필터(SecurityFilterChain·EnableWebSecurity·HttpSecurity·authorizeHttpRequests·OncePerRequestFilter·HandlerInterceptor·X-API-KEY)는 main 소스에서 0건이다. 그런데 주문 생성·취소(OrderController), 계좌 생성·수정·삭제·입금·출금(AccountController), 손익절 트리거, Slack 알림 설정(PUT) 같은 변경 라우트가 전부 열려 있다. build.gradle:53 에 spring-security-test 가 시험용으로만 남아 있어 인증을 넣으려다 만 흔적이거나 계획만 있는 상태로 보인다. 인증 없음이 의도라는 문서는 있다(docs/done/front/realtime-execution/spec.md:29·226 «현재 시스템에 인증 없음 … 향후 인증 추가 시 별도 phase»). 하지만 이 문서는 사실 진술일 뿐, 네트워크 격리·내부망 한정 같은 보호 근거는 아니다. application.yaml 에는 KIS 실전 설정 키(live-app-key·live-app-secret·live-ws-url·live-execution-channel)가 있어 실계좌 연결 가능성이 있다. 다만 주문 경로가 실전 계좌에 실제로 닿는지는 확인하지 못했다.
- 생길 수 있는 일: 네트워크에 닿는 누구나 계좌를 만들고 입금·출금하고 주문을 넣고 취소할 수 있다. Slack 알림 설정 라우트도 열려 있어 알림 대상이 바뀔 수 있다. 실전 모드가 켜진 환경이면 실계좌 주문 권한이 노출된다.
- 해결: 1) 인증 없음이 의도라면 «내부망·사설 네트워크에서만 접근» 같은 근거와 포트 노출 조건(docker-compose 바인딩·방화벽)을 docs 에 남기고 운영 문서에 못 박는다. 2) 아니면 spring-boot-starter-security 를 main 에 추가하고 /actuator/health 외 전 라우트에 인증을 건다. 3) KIS 실전 모드는 인증이 생기기 전까지 기동 설정으로 막는다.

고친 모양(예시):
```
// build.gradle
implementation 'org.springframework.boot:spring-boot-starter-security'

// config/SecurityConfig.kt
@Configuration
class SecurityConfig {
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain =
        http.csrf { it.disable() }
            .authorizeHttpRequests {
                it.requestMatchers("/actuator/health").permitAll()
                    .anyRequest().authenticated()
            }
            .httpBasic(Customizer.withDefaults())
            .build()
}
```

### 86위 [high · 확신 med] 계좌 이름·활성 변경이 락·버전 없이 전체 열 UPDATE 를 내 동시 입출금을 덮어쓸 수 있음

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/account/AccountCommandService.kt:72` · 항목표 data/transaction-boundary · 항목 X8

```
// AccountCommandService.kt:72-82
// 돈과 관련된 것이 아니기 떄문에 락X
@Transactional
fun updateAccount(accountId: Long, command: UpdateAccountCommand): Account {
    val account = accountRepository.findById(accountId)   // ← 락·버전 없이 읽음 (잔고 열 스냅샷 포함)
        .orElseThrow { AccountNotFoundException(accountId) }
    command.accountName?.let { account.rename(it) }   // ← 더티 → 잔고 열까지 포함한 전체 UPDATE
```

- 원인: updateAccount·deactivateAccount 는 findById(락 없음)로 Account 를 읽고 필드만 바꾼다. Account 에는 @Version 도 @DynamicUpdate 도 없다(src 전체 grep 으로 확인). Hibernate 는 더티일 때 변경 없는 열까지 전부 UPDATE 하므로 스냅샷의 deposit·available_deposit·locked_deposit 이 그대로 쓰인다. 주석 «돈과 관련된 것이 아니기 떄문에 락X» 가 그 전제를 보여 주지만, 같은 행에 잔고 열이 있어 성립하지 않는다. deposit·withdraw 는 findByIdWithLock 을 써서 안전하다.
- 생길 수 있는 일: 이름 변경·비활성화 요청과 입금·출금이 겹치면 READ COMMITTED 에서 커밋된 입출금이 오래된 잔고로 덮여 오류 없이 사라진다. 원장(balanceAfter)과 계좌 잔고가 어긋난다.
- 해결: Account 에 @Version 을 달고(또는 이 두 메서드도 findByIdWithLock 사용) 엔티티에 @DynamicUpdate 를 붙여 잔고 열이 이름 변경 UPDATE 에 실리지 않게 한다.

고친 모양(예시):
```
val account = accountRepository.findByIdWithLock(accountId)
    .orElseThrow { AccountNotFoundException(accountId) }
```

### 87위 [high · 확신 med] KIS_LIVE 주문 호출 실패를 불확정으로 다루지 않고 예외로 롤백

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/KisLiveOrderExecutor.kt:30` · 항목표 reliability/failure-handling · 항목 I2

```
runCatching {
    val orno = kisOrderRestClient.placeOrder(order, "live")   // ← 응답이 불확정이어도 결과를 알 수 없음
    order.assignExternalOrderId(orno)
...
}.onFailure {
    log.error { "KIS_LIVE 주문 접수 실패: orderId=$orderId, reason=${it.message}" }   // ← 타임아웃·5xx 도 '실패' 로 단정
    throw it   // ← 트랜잭션 전체 롤백, UNKNOWN 기록·확정 경로 없음
}
```

- 원인: placeOrder 가 ResourceAccessException(타임아웃)·5xx·파싱 실패로 끝나도 onFailure 가 '접수 실패' 를 로그하고 throw it 으로 호출자 트랜잭션을 통째로 롤백한다. 상대가 이미 처리했을 수 있는 응답을 실패로 단정하며, UNKNOWN 상태 기록과 조회·취소로 확정하는 경로가 없다. 같은 runCatching 블록 안의 assignExternalOrderId·슬랙 발행 예외도 KIS 호출 성공 뒤인데 '접수 실패' 로 처리된다.
- 생길 수 있는 일: 실계좌에서 KIS 는 주문을 받았는데 우리는 롤백한다. 사용자·전략이 재시도하면 이중 주문이 나가고, 사후에는 KIS 주문번호가 어디에도 남지 않아 추적할 수 없다.
- 해결: 예외를 확정 실패(4xx·rt_cd 거부)와 불확정(타임아웃·5xx·연결 끊김·파싱 실패)으로 나누고, 불확정은 주문을 UNKNOWN 으로 커밋한 뒤 inquireFills·조회로 확정하는 작업을 둔다. 확정 전에는 같은 주문을 재전송하지 않는다.
- 다른 렌즈의 반증: backend/code-shape+layering+naming-terminology+tdd-purity — 로그 후 다시 던지는 catch-log-rethrow 이고 흐름 분기에 쓰이지 않는다(KisPaperOrderExecutor:27 동일).

고친 모양(예시):
```
val result = try {
    Outcome.Accepted(kisOrderRestClient.placeOrder(order, "live"))
} catch (e: KisApiException) { Outcome.Rejected(e.code) }
  catch (e: RestClientException) { Outcome.Unknown(e) }   // 타임아웃·5xx
// Unknown → order.markUnknown() 후 조회로 확정
```

### 88위 [high · 확신 med] 유스케이스가 DataIntegrityViolationException 을 직접 잡음

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/portfolio/SnapshotJobService.kt:69` · 항목표 backend/layering · 항목 L2

```
import org.springframework.dao.DataIntegrityViolationException  // ← 허용 목록 밖 Spring 타입
val run = try {
    snapshotJobRunTxService.start(account, businessDate)
} catch (_: DataIntegrityViolationException) {  // ← 영속 예외를 유스케이스가 직접 잡음
    throw SnapshotAlreadyRunningException(
```

- 원인: application 이 org.springframework.dao.DataIntegrityViolationException 을 import 하고 start() 호출을 try/catch 로 감싸 업무 예외로 바꾼다(PTB9). 영속 예외 번역은 어댑터 몫이다. 참고: 원인 예외를 넘기지 않아 원인이 사라진다.
- 생길 수 있는 일: JPA 세부가 업무 분기로 새고, 참여 중인 트랜잭션에서 영속 예외를 잡으면 커밋 때 UnexpectedRollbackException 이 될 수 있다.
- 해결: 어댑터가 saveAndFlush 후 SnapshotAlreadyRunningException 으로 번역하고 유스케이스는 업무 예외만 잡는다.
- 다른 렌즈의 반증: backend/code-shape+layering+naming-terminology+tdd-purity — 유일 제약 위반을 업무 예외로 번역하는 코드라 흐름 제어가 아니다. L2 로 따로 적었다.

고친 모양(예시):
```
// 어댑터
try { return repository.saveAndFlush(run) } catch (ex: DataIntegrityViolationException) { throw SnapshotAlreadyRunningException(msg, ex) }
```

### 89위 [high · 확신 med] 유스케이스가 DataIntegrityViolationException 을 직접 잡음

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/portfolio/TaxSummaryCommandService.kt:66` · 항목표 backend/layering · 항목 L2

```
import org.springframework.dao.DataIntegrityViolationException  // ← 허용 목록 밖
val run = try {
    taxSummaryRunRepository.save(TaxSummaryRun.Companion.start(account, taxYear.value, runType))
} catch (ex: DataIntegrityViolationException) {  // ← 영속 예외를 유스케이스에서 직접 잡음
```

- 원인: application 이 org.springframework.dao.DataIntegrityViolationException 을 import 하고 save 호출을 try/catch 로 감싼다(PTB9). ArchUnit 동결 목록 302행. 원인 예외가 변환 예외에 실리지 않는다.
- 생길 수 있는 일: JPA 구현 세부가 업무 분기가 되고 포트 이전 때 이 catch 가 안 맞게 된다.
- 해결: 어댑터에서 번역하고 유스케이스는 TaxSummaryAlreadyRunningException 만 다룬다.
- 다른 렌즈의 반증: data/query+schema-data-model+transaction-boundary — 변환한 예외를 바로 던져 대체 경로로 진행하지 않는다. Spring 타입 노출은 X10 으로 보고했다.

고친 모양(예시):
```
// 어댑터: saveAndFlush 뒤 DataIntegrityViolationException → TaxSummaryAlreadyRunningException(cause = ex)
```

### 90위 [high · 확신 med] 시세 틱 전체 트랜잭션이 포지션 행 락을 쥔 채 안쪽 주문 경로(KIS 제출·RestTemplate)를 부르고, 안쪽이 같은 행을 다시 잠가 교착 위험

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/position/PositionExitTriggerOrchestrator.kt:36` · 항목표 data/transaction-boundary · 항목 X3

```
@Transactional                                                      // ← L36 시세 틱 전체를 하나의 바깥 트랜잭션으로 감쌈
fun onQuote(ticker: String, price: BigDecimal, quoteAt: Instant) {
...
val position = positionRepository.findByIdWithLock(groupKey.positionId).orElse(null)   // ← L55 포지션 행 PESSIMISTIC_WRITE, 바깥 커밋까지 유지
...
autoExitOrderPlacementService.createGroupedAutoExitSellOrder(       // ← L141 REQUIRES_NEW 새 커넥션 → placeOrder 가 같은 행을 다시 FOR UPDATE (OrderCommandService.kt:87), KIS 제출(L70) 포함
```

- 원인: onQuote 는 @Transactional 바깥 트랜잭션이고, processGroup 이 findByIdWithLock(PESSIMISTIC_WRITE)로 포지션 행 락을 잡은 채 커밋까지 쥔다. 그 상태에서 REQUIRES_NEW 서비스(AutoExitOrderPlacementService)를 부르면 새 커넥션의 OrderCommandService.placeOrder 로 들어가, SELL 이라 lockSellQuantityIfNeeded 가 같은 포지션 행을 findByAccountIdAndTickerWithLock(OrderCommandService.kt:87)로 다시 FOR UPDATE 한다. 이 경로 안에서 collectorSubscriptionPort.subscribe(L66, RestTemplate)와 KIS 주문 제출(L70)도 실행된다. 안쪽은 바깥의 락을 기다리고 바깥은 자바 스레드로 안쪽 반환을 기다리므로 PostgreSQL 교착 탐지기에 보이지 않는 무한 대기가 된다. 두 락 쿼리에는 lock.timeout 힌트도 없다(3초 힌트는 트리거 쿼리에만 있음). 또 Position.kt 주석의 락 순서(Account 후 Position)와 반대로 Position 을 먼저 잡아, 동시 주문과의 순환 대기도 같은 방식으로 숨는다. DB 를 실행하지 않고 코드 경로만으로 판정했다.
- 생길 수 있는 일: 자동 청산이 발동하는 시세 틱에서 Redis 리스너 스레드가 멈추고 커넥션 2개(풀 최대 10)가 풀리지 않는다. 같은 종목 시세 처리와 청산이 멎고, 풀이 고갈되면 무관한 요청까지 막힌다. KIS 호출이 느려도 포지션 행이 그만큼 잠겨 사용자 주문·체결이 대기한다.
- 해결: 바깥 @Transactional 을 없애고 3단계로 나눈다. 1) 짧은 트랜잭션에서 그룹을 점유(ARMED→PROCESSING 커밋), 2) 트랜잭션 밖에서 주문 생성 호출, 3) 새 트랜잭션에서 결과(TRIGGERED·FAILED·SKIPPED) 반영. 포지션 락은 주문 유스케이스(Account→Position 순서) 한 곳에서만 잡는다.

고친 모양(예시):
```
fun onQuote(ticker: String, price: BigDecimal, quoteAt: Instant) {
    val claimed = triggerClaimService.claimFiredGroups(ticker, price, quoteAt) // 짧은 @Transactional, 상태 PROCESSING 커밋
    claimed.forEach { group ->
        val created = autoExitOrderPlacementService.place(group)          // 트랜잭션 밖
        triggerResultService.record(group, created)                        // 새 @Transactional
    }
}
```

### 91위 [high · 확신 med] 한 트랜잭션 안에서 잠금 실패를 잡고 다음 그룹을 계속 처리

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/position/PositionExitTriggerOrchestrator.kt:36` · 항목표 reliability/failure-handling · 항목 E7

```
@Transactional
fun onQuote(ticker: String, price: BigDecimal, quoteAt: Instant) {   // ← 모든 그룹을 한 트랜잭션에서 처리
    ...
    groups.forEach { (groupKey, candidates) ->
        try {
            processGroup(normalizedTicker, groupKey, price, quoteAt)
        } catch (ex: CannotAcquireLockException) {
            markCandidatesSkipped(candidates, quoteAt, price, TriggerSkipReason.LOCK_CONFLICT)   // ← 오류가 난 트랜잭션 안에서 계속 쓰기, 로그·건수 없음
```

- 원인: `onQuote` 가 @Transactional 하나로 모든 그룹을 돈다. 한 그룹에서 잠금 실패가 나면 catch 에서 같은 트랜잭션으로 skip 기록을 쓰고 다음 그룹으로 간다. PostgreSQL 은 잠금 타임아웃·교착 오류가 난 트랜잭션을 중단 상태로 만들고 Spring 은 rollback-only 로 표시하는 것이 일반적이라(코드 실측 아님) catch 안의 쓰기와 이후 그룹이 실패하거나 커밋 때 전부 롤백될 수 있다. 반면 주문은 REQUIRES_NEW 로 이미 커밋됐다. 실패 그룹 수·id 를 남기는 로그도 없다.
- 생길 수 있는 일: 앞 그룹에서 이미 나간 자동 청산 주문의 트리거 상태 갱신이 롤백돼 트리거가 ARMED 로 남고, 다음 틱에 같은 트리거가 다시 발동해 청산 주문이 중복·과다 발생할 수 있다.
- 해결: 그룹 한 개 처리를 별도 빈의 @Transactional(REQUIRES_NEW) 로 분리해 한 그룹의 실패가 다른 그룹·skip 기록을 오염시키지 않게 하고, 실패 그룹 id·건수를 로그한다.

고친 모양(예시):
```
groups.forEach { (key, candidates) ->
    try { groupProcessor.process(ticker, key, price, quoteAt) }   // 별도 빈 @Transactional(REQUIRES_NEW)
    catch (ex: PessimisticLockingFailureException) {
        log.warn(ex) { "auto-exit group skipped: positionId=${key.positionId}, type=${key.triggerType}" }
        skipRecorder.recordLockConflict(candidates, quoteAt, price)   // 별도 트랜잭션
    }
}
```

### 92위 [high · 확신 med] 도메인 FeePolicy 가 JPA 엔티티 (@Entity·@Table·@Column)

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/feepolicy/FeePolicy.kt:23` · 항목표 backend/layering · 항목 D1

```
import jakarta.persistence.Entity                 // ← L7 도메인이 JPA import
...
@Entity                                            // ← 도메인 모델이 곧 ORM 행
@Table(name = "fee_policies")
class FeePolicy protected constructor() : BaseTimeEntity() {
```

- 원인: domain 파일이 jakarta.persistence 를 import 하고 @Entity·@Table·@Column 을 단다(PTB1 PR #50). ArchUnit 동결 목록에 16건(tools/T96587ad0.txt:165-166)이 있으나 동결은 해결이 아니다.
- 생길 수 있는 일: 도메인 모델이 곧 ORM 행이라 지연 로딩·더티 체킹이 expire·calculateFee 같은 불변식 메서드를 우회하고, 영속 스키마 변경이 도메인 변경이 된다.
- 해결: FeePolicy 를 순수 Kotlin 도메인 클래스로 두고 JPA 엔티티와 변환을 infrastructure.persistence 어댑터로 옮기며, 리포지토리 포트(application.<ctx>.port)는 도메인 모델을 주고받게 한다.
- 다른 렌즈의 반증: data/query+schema-data-model+transaction-boundary — 시장·모드 조합 수만큼의 설정성 소량 테이블이라 인덱스 없이도 비용이 작습니다. 유일성 문제는 M3 로 따로 보고했습니다.

### 93위 [high · 확신 med] 체결 수수료만 저장하고 적용한 요율·정책 버전을 함께 저장하지 않음

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/order/Execution.kt:60` · 항목표 data/schema-data-model · 항목 M11

```
@Column(name = "fee", nullable = false, precision = 20, scale = 4)
var fee: BigDecimal = BigDecimal.ZERO   // ← 계산된 금액만 저장 — fee_rate·fee_policy_id 열 없음
// ExecutionProcessor.kt:108  feePolicyRepository.findActivePolicy(tradingMode, marketType, Instant.now())   // ← executedAt 아님
```

- 원인: executions.fee 는 계산 결과만 담고 fee_rate 나 정책 id 열이 없습니다. ExecutionProcessor.kt:108 은 체결 시각(executedAt)이 아니라 처리 시각 Instant.now() 기준 현재 정책으로 요율을 읽습니다. 요율을 되찾으려면 현재 FeePolicy 를 다시 읽어야 합니다. 도메인의 FeePolicy.calculateFee 는 호출처가 없고 ExecutionProcessor 가 별도 식(HALF_UP)으로 계산합니다(LLM-only 판정).
- 생길 수 있는 일: 요율이 바뀌면 과거 체결의 수수료 근거를 복원할 수 없고, 대사·재계산이 거짓 불일치를 냅니다. 금융 상태 추적 가능성 원칙(CLAUDE.md)에도 어긋납니다.
- 해결: executions 에 fee_rate(precision 10, scale 6)와 fee_policy_id 를 추가해 체결 당시 값을 저장하고, 정책 조회 기준 시각은 executedAt 으로 맞춥니다.

고친 모양(예시):
```
@Column(name = "fee_rate", nullable = false, precision = 10, scale = 6)
lateinit var feeRate: BigDecimal
    private set

@Column(name = "fee_policy_id")
var feePolicyId: Long? = null
    private set
```

### 94위 [high · 확신 med] orders 의 핫 경로 필터 열(ticker·order_status·external_order_id·created_at)에 인덱스가 없음

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/order/Order.kt:32` · 항목표 data/schema-data-model · 항목 M4

```
@Table(
    name = "orders",
    uniqueConstraints = [UniqueConstraint(name = "uk_orders_account_idempotency", columnNames = ["account_id", "idempotency_key"])]
)   // ← indexes 없음: (order_status, ticker)·external_order_id·(account_id, created_at) 인덱스 없음
```

- 원인: orders 의 유일 제약은 (account_id, idempotency_key)라 account_id 선두만 덮고, V12 가 order_group_id 인덱스를 추가했을 뿐입니다. 그런데 findActiveLocalOrdersByTicker 는 시세 틱마다, findExpiredOrders 는 30초마다, findActiveKisOrderByExternalOrderId 는 체결 통지마다 돌고, searchOrders 는 created_at 으로 정렬합니다. 모두 ticker·order_status·external_order_id 필터입니다. 틱마다 도는 경로라 high 로 올렸고 DB 를 보지 못해 confidence 는 med 입니다.
- 생길 수 있는 일: 주문은 삭제되지 않아 시간이 갈수록 틱마다 orders 전체 스캔이 되고, 매칭 지연과 DB CPU 상승으로 이어집니다.
- 해결: (order_status, ticker) 부분 인덱스(활성 상태), external_order_id, (account_id, created_at) 인덱스를 추가합니다.

고친 모양(예시):
```
@Table(name = "orders", indexes = [
    Index(name = "idx_orders_status_ticker", columnList = "order_status, ticker"),
    Index(name = "idx_orders_external_order_id", columnList = "external_order_id"),
    Index(name = "idx_orders_account_created", columnList = "account_id, created_at")
], uniqueConstraints = [...])
```

### 95위 [high · 확신 med] KIS 주문 접수 결과를 «불확정»으로 담을 상태(UNKNOWN)가 없음

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/order/Order.kt:72` · 항목표 data/schema-data-model · 항목 M8

```
@Enumerated(EnumType.STRING)
@Column(name = "order_status", nullable = false, length = 20)
var orderStatus: OrderStatus = OrderStatus.PENDING   // ← enum 에 «접수 불확정» 상태 없음 → 외부 호출 예외는 롤백으로만 처리됨
```

- 원인: OrderStatus 는 PENDING·PARTIAL·FILLED·CANCELLED·REJECTED 뿐입니다(domain/enums/OrderStatus.kt:3-9). KIS 주문은 placeOrder 트랜잭션 안에서 REST 로 나가고(OrderCommandService.kt:70) 타임아웃·예외면 전체가 롤백되어 로컬엔 주문 흔적이 남지 않습니다. KIS 쪽이 접수했는지 알 수 없는 상태를 표현할 자리가 없습니다(LLM-only 판정).
- 생길 수 있는 일: 응답만 유실되고 KIS 가 접수한 주문을 로컬은 모른 채, 같은 idempotencyKey 로 재시도하면 이중 주문이 나갈 수 있습니다. 실주문(KIS_LIVE)에서는 금전 손실입니다.
- 해결: SUBMITTING(접수 시도 기록)과 UNKNOWN(응답 불확정) 상태를 추가하고, 트랜잭션 안에서 SUBMITTING 으로 먼저 커밋한 뒤 KIS 호출 결과를 새 트랜잭션에서 반영합니다. UNKNOWN 은 조회·대사로 확정합니다.

고친 모양(예시):
```
enum class OrderStatus { PENDING_SUBMIT, UNKNOWN, PENDING, PARTIAL, FILLED, CANCELLED, REJECTED }
// 1) tx: 주문 저장 + 홀드, 상태 PENDING_SUBMIT 커밋
// 2) tx 밖: kisOrderRestClient.placeOrder()
// 3) 새 tx: 성공 -> PENDING + externalOrderId / 타임아웃 -> UNKNOWN
```

### 96위 [high · 확신 med] domain 파일이 jakarta.persistence 를 import 하고 @Entity 를 직접 단다

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/portfolio/TradingJournal.kt:5` · 항목표 backend/layering · 항목 D1

```
import jakarta.persistence.Column   // ← L5
@Entity   // ← L20
class TradingJournal protected constructor() : BaseAuditEntity() {
```

- 원인: ArchUnit 동결 목록에 16건. 매매 일지는 필드가 적은 단순 CRUD 에 가깝지만 create·update 에 검증 로직이 있어 «필드만 있는 얇은 모델» 예외에는 맞지 않는다고 본다. main 판본 PTB1 Exception 과는 충돌 가능(ruleConflicts).
- 생길 수 있는 일: 검증·정규화 로직(trim·uppercase)이 ORM 행 위에 있어 JPA 가 우회할 수 있고 도메인 시험이 JPA 에 묶인다.
- 해결: 순수 도메인 모델 + infrastructure 의 JPA 엔티티·매퍼·포트로 분리한다. 단순 CRUD 예외로 둘지는 사람이 정한다.

### 97위 [high · 확신 med] 정산의 수수료·세금에 적용한 요율(tax_rate)·정책 버전이 저장되지 않음

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/settlement/Settlement.kt:56` · 항목표 data/schema-data-model · 항목 M11

```
@Column(name = "tax", nullable = false, precision = 20, scale = 4)
var tax: BigDecimal = BigDecimal.ZERO   // ← 계산된 세금만 저장 — transaction_tax_rate·fee_policy_id 열 없음
```

- 원인: settlements 는 fee·tax 금액만 저장합니다. 세금은 ExecutionProcessor.kt:184 가 현재 FeePolicy 로 계산해 넘기고 요율과 정책 id 는 어디에도 남지 않습니다(LLM-only 판정).
- 생길 수 있는 일: 증권거래세율이 바뀌면 과거 정산의 세금 근거를 복원할 수 없고 세금 요약(TaxSummary) 대사가 어긋납니다.
- 해결: settlements 에 tax_rate 와 fee_policy_id 를 추가해 정산 시점 값을 저장합니다.

### 98위 [high · 확신 med] 체결 통지 파싱 실패와 필드 누락이 로그 없이 null 로 사라짐

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/kis/KisExecutionNoticeParser.kt:19` · 항목표 reliability/failure-handling · 항목 E1

```
fun parse(rawMessage: String, mode: String): KisExecutionNotice? = runCatching {
    val fields = parseFields(rawMessage) ?: return null   // ← 파싱 불가를 로그 없이 null
...
    val qty = fields.firstValue("CCLD_QTY", "ccld_qty")?.toBigDecimalOrNull() ?: return null   // ← 필드 누락도 침묵
...
}.getOrNull()   // ← 모든 예외를 삼킴
```

- 원인: parse 전체가 runCatching 안에 있고 .getOrNull() 로 끝난다. JSON·시각 파싱 예외는 흔적 없이 null 이 되며, 필수 필드 누락도 전부 '?: return null' 이다. 호출자 handleRawMessage 는 null 이면 return 하므로 체결 통지가 어디에도 기록되지 않고 버려진다. 이 파일엔 logger 도 없다.
- 생길 수 있는 일: KIS 가 통지 포맷을 바꾸거나 필드가 하나 비면 체결이 전부 조용히 유실된다. 로컬 주문은 PENDING 으로 남고 잔고와 포지션이 어긋나지만 로그에는 아무것도 없다.
- 해결: 파싱 예외는 WARN 이상으로 원문 일부(시크릿 제외)와 함께 로그하고, 필수 필드 누락은 사유를 담은 결과(sealed)로 돌려 호출자가 기록하게 한다. 체결로 보이는 메시지를 못 읽었다면 ERROR 로 올린다.
- 다른 렌즈의 반증: backend/code-shape+layering+naming-terminology+tdd-purity — JSON 파싱·LocalDateTime 파싱 실패 같은 외부 입력 오류를 감싼 것이라 firstOrNull·?: 한 식으로 대체할 수 없다. 오류를 삼키는 문제는 failure-handling 렌즈 몫이다.

고친 모양(예시):
```
sealed interface ParseResult { data class Ok(val notice: KisExecutionNotice): ParseResult; data class Skipped(val reason: String): ParseResult; data class Failed(val cause: Throwable): ParseResult }
// Failed → log.error(cause) { "체결 통지 파싱 실패 mode=$mode" }
```

### 99위 [high · 확신 med] 체결 처리 예외 경계가 없어 한 건 실패가 통지 유실로 이어짐

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/kis/KisExecutionWebSocketManager.kt:227` · 항목표 reliability/failure-handling · 항목 I6

```
private fun handleRawMessage(mode: String, rawMessage: String) {
    val notice = parser.parse(rawMessage, mode) ?: return
    noticeService.handle(notice)   // ← 예외 경계·실패 기록·재처리 큐 없음
}
```

- 원인: handleRawMessage 는 noticeService.handle(notice) 를 try 없이 호출한다. 낙관적 락 충돌·PositionNotFound·유일 제약 위반 등이 나면 예외가 WebSocket 핸들러 밖으로 전파된다. Spring 클라이언트 어댑터가 핸들러 예외에 세션을 닫는 것이 일반 동작이라 연결이 끊기고 재연결 소진 위험이 커진다(설치 패키지 미검증). 실패한 통지는 어디에도 기록·재전송되지 않는다.
- 생길 수 있는 일: 일시적 DB 충돌 하나가 체결 통지를 영구 유실시키고 연결까지 흔들어 뒤따르는 통지도 놓칠 수 있다. KIS 는 놓친 통지를 재전송하지 않는다.
- 해결: 통지 1건 단위로 예외를 잡아 externalExecutionId 와 함께 ERROR 로 남기고, 실패 통지는 재처리 테이블이나 큐에 보관한다(또는 I6 의 조회 대조가 메운다). 연결은 유지한다.

고친 모양(예시):
```
private fun handleRawMessage(mode: String, raw: String) {
    val notice = parser.parse(raw, mode) ?: return
    runCatching { noticeService.handle(notice) }
        .onFailure { log.error(it) { "체결 통지 처리 실패 execId=${notice.externalExecutionId}" }; retryStore.save(notice) }
}
```

### 100위 [high · 확신 med] KIS 접근 토큰·WebSocket 승인키를 Redis 에 평문으로 저장, Redis 는 인증·TLS 없음

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/kis/KisTokenManager.kt:85` · 항목표 reliability/security-config-hygiene · 항목 S3

```
redisTemplate.opsForValue().set(redisKey(mode), token, ttl)   // ← 토큰 원문을 Redis 에 저장 (key: kis:token:{mode})
...
redisTemplate.opsForValue().set(key, approvalKey, Duration.ofHours(20))   // ← 승인키 원문 저장
// application.yaml:30-33  data.redis 에 host·port 만 있고 password·ssl 없음
```

- 원인: 토큰과 승인키가 값 그대로 Redis 에 들어간다. TTL 은 있으나(토큰 expires_in, 승인키 20시간) 암호화·접근 제어가 코드에 없고, application.yaml 의 spring.data.redis 에는 host·port 만 있어 password·ssl 이 설정되지 않는다(내 묶음 밖 파일, 열어 확인). 이 값은 collector-api 와 공유하는 실계좌 주문 권한이다. 루트 CLAUDE.md 는 토큰의 평문 저장을 예외 없이 금지한다.
- 생길 수 있는 일: Redis 에 접근 가능한 누구나 토큰을 읽어 KIS 실계좌 주문을 낼 수 있다.
- 해결: Redis 에 인증과 TLS 를 켜고, 토큰은 공유 키로 암호화해 저장한다(collector-api 와 키 공유). 접근은 서비스 계정에 한정한다.

고친 모양(예시):
```
spring:
  data:
    redis:
      password: ${REDIS_PASSWORD}
      ssl:
        enabled: true
// 저장 시 AES-GCM 으로 암호화한 값을 set
```

### 101위 [high · 확신 med] 같은 종목의 시세 메시지가 직렬화되지 않아 같은 종목 매칭이 동시에 돌 수 있음

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/redis/RedisListenerConfig.kt:20` · 항목표 reliability/load-concurrency · 항목 A2

```
        container.addMessageListener(quoteEventListener, PatternTopic("quote:kis:*:*"))   // ← 종목 단위 직렬화 없이 메시지마다 병렬 처리
...
// LocalMatchingEngine.kt:25-26
    fun tryMatchPendingOrders(ticker: String, quote: QuoteSnapshot) {
        val orders = orderRepository.findActiveLocalOrdersByTicker(ticker)   // ← 종목 락 없이 목록 조회 후 주문별 체결
```

- 원인: 리스너가 종목 키로 단일 소비자에 묶이지 않고(A1 의 기본 실행기), tryMatchPendingOrders(ticker, …) 에 종목 락도 없다. order 디렉터리의 락은 계좌 행 findByIdWithLock 뿐이다. 두 메시지가 같은 종목의 PENDING 주문 목록을 동시에 읽고 remainQty 를 읽은 시점 값으로 fillLocal 을 부른다. fillLocal 이 락 안에서 주문 상태를 다시 검증하는지는 ExecutionProcessor 를 열지 않아 확인하지 못했다(LLM-only).
- 생길 수 있는 일: 한 주문이 두 번 체결되거나 낡은 가격으로 체결될 수 있다.
- 해결: 종목 키 해시로 단일 스레드 실행기에 보내 종목별 직렬화하거나 종목 락을 둔다. fillLocal 은 락 안에서 주문 상태·잔여 수량을 다시 읽어 검증한다.

### 102위 [high · 확신 med] positionId 만으로 청산 트리거를 만들고 고치고 취소 — 소유 범위 없음

`backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/controller/PositionExitTriggerController.kt:26` · 항목표 reliability/security-config-hygiene · 항목 S7

```
@PostMapping("/{positionId}/exit-triggers")
fun create(
    @PathVariable positionId: Long,   // ← 호출자 소유 범위 없이 positionId 만으로 대상 결정
    @Valid @RequestBody request: CreatePositionExitTriggerRequest,
) = commandService.createPositionTrigger(request.toCommand(positionId)).toResponse()
```

- 원인: `/api/positions/{positionId}/exit-triggers` 의 생성·수정·취소가 positionId 만 받고 계좌·호출자 범위가 없다(서비스 `findOpenPositionForUpdate` 도 positionId 로만 조회). 이 트리거는 이후 자동 매도 주문을 만든다. 인증 존재 여부는 repo 단위 S10 몫.
- 생길 수 있는 일: 남의 포지션에 손절·익절 트리거를 심거나 취소해 의도하지 않은 매도를 일으킬 수 있다 — 읽기만 되는 IDOR 보다 피해가 크다.
- 해결: 경로에 accountId 를 넣거나 인증 주체에서 계좌를 얻어 `findByIdAndAccountId` 로 포지션을 잠금 조회한다.

고친 모양(예시):
```
@PostMapping("/api/v1/accounts/{accountId}/positions/{positionId}/exit-triggers")
// 서비스: positionRepository.findByIdAndAccountIdWithLock(positionId, accountId) ?: throw PositionNotFoundException(positionId)
```

### 103위 [high · 확신 med] IllegalArgument·IllegalState 등 일반 예외의 원문 메시지를 400 응답 본문에 그대로 실음

`backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/exception/GlobalExceptionHandler.kt:25` · 항목표 reliability/failure-handling · 항목 E4

```
    @ExceptionHandler(IllegalStateException::class)
    fun handleIllegalState(ex: IllegalStateException): ResponseEntity<ApiErrorResponse> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            ApiErrorResponse(400, "BAD_REQUEST", ex.message ?: "요청을 처리할 수 없는 상태입니다.")   // ← 원문 노출 + 서버 불변식 위반을 400 으로
```

- 원인: handleIllegalArgument·handleIllegalState·handleTypeMismatch·handleNoSuchElement 는 ex.message 를 응답 message 로 쓴다. 이 타입은 JDK·Spring·Hibernate·Jackson 도 던지므로 내부 클래스명·엔티티 정보가 섞일 수 있고, 서버 불변식 위반(IllegalStateException)이 클라이언트 오류 400 으로 둔갑한다. ApiDomainException 의 메시지는 코드에서 만든 문장이라 대상이 아니다.
- 생길 수 있는 일: 프레임워크·도메인 내부 문구가 클라이언트로 새고, 서버 결함이 4xx 로 분류돼 알람·재시도 판단이 틀어진다.
- 해결: 일반 예외는 고정 문구만 응답하고 원문은 로그에만 남긴다. 사용자 입력 오류는 전용 ApiDomainException 으로 던지게 바꾸고 IllegalStateException 은 500 으로 처리한다.

고친 모양(예시):
```
@ExceptionHandler(IllegalStateException::class)
fun handleIllegalState(ex: IllegalStateException): ResponseEntity<ApiErrorResponse> {
    log.error(ex) { "불변식 위반" }
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ApiErrorResponse(500, "INTERNAL_SERVER_ERROR", "서버 오류가 발생했습니다."))
}
```

### 104위 [high · 확신 med] 전역 핸들러가 500 을 만들면서 예외를 로그에 남기지 않음

`backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/exception/GlobalExceptionHandler.kt:59` · 항목표 reliability/observability · 항목 O1

```
    @ExceptionHandler(Exception::class)
    fun handleException(ex: Exception): ResponseEntity<ApiErrorResponse> =   // ← ex 를 로그에 남기지 않음
        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            ApiErrorResponse(500, "INTERNAL_SERVER_ERROR", "서버 오류가 발생했습니다.")
        )
```

- 원인: handleException 은 ex 를 쓰지 않고 고정 500 본문만 돌려준다(detekt UnusedParameter 가 같은 사실을 보고). handleApiDomain 도 SnapshotComputeFailedException·TaxSummaryComputeFailedException 같은 500 계열을 로그 없이 응답하고, IllegalStateException 은 400 으로 바꾼다(예: SnapshotJobRunTxService 의 snapshot job run not found 불변식 위반). 이 묶음의 애플리케이션 코드에 로거가 하나도 없어 SnapshotJobService·TaxSummaryCommandService 가 cause 를 붙여 다시 던진 예외도 어디에서도 기록되지 않는다.
- 생길 수 있는 일: 스냅샷·세금 계산이 5xx 로 실패해도 스택트레이스가 서버 로그에 없다. 클라이언트가 받은 고정 문장 말고는 원인을 되짚을 방법이 없다.
- 해결: 핸들러에 로거를 두고 Exception 핸들러는 ERROR(예외 포함), 500 계열 ApiDomainException 은 ERROR(cause 포함), IllegalStateException 은 WARN 이상으로 남긴다. 4xx 는 로그 면제.
- 다른 렌즈의 반증: reliability/failure-handling+load-concurrency+observability+security-config-hygiene — 최종 그물이고 500 실패 응답을 돌려주므로 E1·E3 은 통과. 로그 부재는 O1 로 따로 발견

고친 모양(예시):
```
private val log = KotlinLogging.logger {}

@ExceptionHandler(Exception::class)
fun handleException(ex: Exception): ResponseEntity<ApiErrorResponse> {
    log.error(ex) { "처리되지 않은 예외" }
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
        ApiErrorResponse(500, "INTERNAL_SERVER_ERROR", "서버 오류가 발생했습니다.")
    )
}
```

### 105위 [high · 확신 med] 기본 프로필에 ddl-auto: create 가 커밋돼 있음

`backend/trading-api/src/main/resources/application.yaml:19` · 항목표 reliability/security-config-hygiene · 항목 S2

```
      ddl-auto: create   # ← 기본 프로필에서 기동마다 스키마 삭제·재생성 (프로필 분리 없음)
```

- 원인: resources 에는 application.yaml 과 db/ 뿐이라(application-prod 없음) 운영도 이 값으로 뜬다. docker-compose.yml 의 trading-api 서비스는 SPRING_PROFILES_ACTIVE 도 ddl-auto 환경 변수도 주지 않고, build.gradle 에는 flyway 의존이 없어 db/migration SQL 을 적용하는 도구도 없다(V11 주석에만 Flyway 언급). 비밀 파일인 .env 는 열지 않았다.
- 생길 수 있는 일: Hibernate create 는 기동할 때마다 기존 테이블을 지우고 다시 만든다. restart: unless-stopped 재시작이나 재배포마다 계좌·주문·체결·원장이 전부 사라진다.
- 해결: 기본 application.yaml 은 ddl-auto: validate(또는 none)로 두고 create 는 로컬 전용 application-local.yaml 로 옮긴다. 스키마는 Flyway 등 마이그레이션 도구가 소유하게 한다.
- 다른 렌즈의 반증: reliability/observability — show-sql: false 이고 application.yaml 에 org.hibernate.orm.jdbc.bind 설정이 0건이라 켜져 있지 않음. O11 의 세 번째 조건은 통과

고친 모양(예시):
```
spring:
  jpa:
    hibernate:
      ddl-auto: validate
```

### 106위 [high · 확신 med] trading-api 에 마이그레이션 적용 주체가 없고 V1 기준선이 없다 — 마이그레이션이 Hibernate 가 만든 테이블을 가정

`backend/trading-api/src/main/resources/db/migration/V8__add_order_idempotency_scope.sql:1` · 항목표 data/schema-data-model · 항목 M12

```sql
// V8__add_order_idempotency_scope.sql:1-2
ALTER TABLE orders DROP CONSTRAINT IF EXISTS uk_orders_account_idempotency;   // ← orders 의 CREATE TABLE 은 어느 마이그레이션에도 없음 (V1 부재)
ALTER TABLE orders ADD CONSTRAINT uk_orders_account_scope_idempotency UNIQUE (account_id, idempotency_key);
// V12__add_order_group_id_to_orders.sql:1-2
ALTER TABLE orders
ADD COLUMN IF NOT EXISTS order_group_id VARCHAR(100);   // ← 이 모듈에는 이를 적용할 Flyway·Liquibase 가 없음
```

- 원인: trading-api 의 db/migration 에는 V2~V12 가 있으나 V1 이 없다. trading-api 의 build.gradle·settings.gradle 에는 flyway·liquibase 의존성이 없고 application.yaml 에도 flyway 설정이 없다. flyway 의존성·설정은 collector-api 쪽에만 있다. V3·V8·V10·V12 는 tax_summaries·orders·fee_policies 를 ALTER 하지만 이 디렉터리 어디에도 이 테이블들의 CREATE TABLE 이 없다. 따라서 기준선은 ddl-auto 가 만든다고 가정한 구조다. CREATE TABLE IF NOT EXISTS·ADD COLUMN IF NOT EXISTS 를 쓰는 것도 두 소유자가 공존하는 흔적이다. 스키마 소유자가 둘(Hibernate create · 미적용 마이그레이션)이라 엔티티 매핑과 DDL 의 드리프트를 막을 장치가 없다. collector-api 의 db/migration 에 V1 이 있는지와 Flyway locations 는 열지 않았다(notSeen). 그래도 trading-api 스스로는 위 파일들을 적용하지 않는다.
- 생길 수 있는 일: V2~V12 의 유니크 제약(uk_orders_account_scope_idempotency)·인덱스·컬럼 추가가 운영 DB 에 적용된다는 보장이 없다. 엔티티와 DDL 이 어긋나도 배포 시 걸러지지 않아 멱등 키 제약 누락으로 중복 주문이 생길 수 있다. 새 환경은 마이그레이션만으로 스키마를 재현할 수 없다.
- 해결: trading-api 에 flyway-core·flyway-database-postgresql 을 추가하고, 현재 운영 스키마를 덤프해 V1__baseline.sql 로 만든다(기존 DB 는 baseline-on-migrate 또는 baseVersion 설정). ddl-auto 는 validate 로 바꾸고 엔티티와 DDL 대조는 validate 와 스키마 시험으로 맡긴다.

고친 모양(예시):
```sql
// build.gradle
implementation 'org.flywaydb:flyway-core'
implementation 'org.flywaydb:flyway-database-postgresql'

# application.yaml
spring:
  flyway:
    enabled: true
    baseline-on-migrate: true
    baseline-version: 1
```

### 107위 [high · 확신 med] PESSIMISTIC_WRITE 락·@Modifying 쿼리에 실제 엔진 시험이 없음 — 전부 목으로만 시험

`backend/trading-api/src/test/kotlin/com/papertrading/api/application/position/PositionExitTriggerOrchestratorTest.kt:78` · 항목표 backend/tdd-purity · 항목 T5

```
every { positionRepository.findByIdWithLock(10L) } returns Optional.of(position)  // ← PESSIMISTIC_WRITE 쿼리를 목으로 대체 (실제 락 동작 미검증)
every { triggerRepository.findArmedGroupForUpdate("005930", 10L, TriggerType.STOP_LOSS) } returns listOf(stopLoss)  // ← 락·타임아웃 힌트·JPQL 이 한 번도 실행되지 않음
```

- 원인: findByIdWithLock(Position)·findByIdForUpdate·findArmedGroupForUpdate(타임아웃 힌트 3000, JPQL enum FQCN 리터럴)·applySellExecutionAtomically 는 src/test 전체에서 목이 쓰이는 시험(ExecutionProcessorTest·OrchestratorTest·CommandServiceTest 등)에만 나오고 Testcontainers 를 쓰는 시험(Position Command·Query·ControllerIntegration·ExitTriggerPersistenceConstraintTest)에는 없다(grep 목록 대조). ExitTriggerPersistenceConstraintTest 는 orders 유일 제약만 본다. 락 타임아웃 시 던져지는 예외 타입, 락 순서, @Modifying 0행 갱신 동작이 검증되지 않는다.
- 생길 수 있는 일: 자동 청산 경로의 동시성 보호(PTD3)가 한 번도 실제 PostgreSQL 에서 검증되지 않아 락 쿼리를 고쳐도 시험은 초록이고, 운영에서만 데드락·타임아웃·중복 주문이 드러난다.
- 해결: PositionExitTriggerRepository·PositionRepository 락·@Modifying 쿼리를 Testcontainers PostgreSQL 위의 @DataJpaTest 로 시험한다(두 스레드 락 경합 시 타임아웃 예외 타입, 0행 갱신 시 반환값 0).

고친 모양(예시):
```
@Test fun `applySellExecutionAtomically 는 잠금 수량 부족 시 0행을 갱신한다`() {
    val updated = positionRepository.applySellExecutionAtomically(id, BigDecimal("5"))
    assertEquals(0, updated)
}
```

### 108위 [high · 확신 low] 잠금 해제 금액 규칙이 서비스 두 곳에 제각각 구현 — 부분 체결 후 최종 체결에서 과다 해제

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/ExecutionProcessor.kt:233` · 항목표 backend/layering · 항목 D3

```
    private fun releaseLockedExcess(orderId: Long, lockedAmount: BigDecimal, fillPrice: BigDecimal, fillQty: BigDecimal, account: com.papertrading.api.domain.entity.account.Account) {
        val actualCost = fillPrice.multiply(fillQty)          // ← 이번 체결 1건의 금액만 (누적 체결금액 아님)
        val excess = lockedAmount.subtract(actualCost)        // ← 주문 전체 잠금액에서 마지막 체결분만 뺌
        if (excess <= BigDecimal.ZERO) return
// Order.kt:157-159
    fun remainingLockedAmount(): BigDecimal =
        lockedAmount.subtract(totalAmount ?: BigDecimal.ZERO).coerceAtLeast(BigDecimal.ZERO)   // ← 올바른 규칙이 이미 있으나 호출처 없음
```

- 원인: Order.remainingLockedAmount()(초기 잠금 − 누적 체결금액)가 있으나 호출처가 시험뿐이다. ExecutionProcessor.releaseLockedExcess 는 주문 전체 잠금액에서 «마지막 체결 1건»의 금액만 빼고, OrderCommandService.cancelOrder(L256-259)는 lockedAmount ÷ quantity × 잔량이라는 세 번째 식을 쓴다. 10주@70000 지정가 매수(lockedAmount 700000)가 4주@70000, 이어 6주@70000 로 체결되면 마지막 호출의 excess = 700000 − 420000 = 280000 이다. 실제 누적 체결금액이 700000 이라 해제액은 0 이어야 한다. 앞선 두 체결에서 이미 recordBuyExecution(confirmBuy)이 잠금 700000 을 모두 소진했다.
- 생길 수 있는 일: unlockDeposit 의 require(lockedDeposit >= amount)에 걸려 예외로 마지막 체결 트랜잭션 전체가 롤백돼 FILLED 반영이 실패하거나, 같은 계좌의 다른 주문 잠금 예수금이 풀려 가용 예수금이 부풀려진다. 부분 체결 경로가 운영에서 실제로 쓰이는지는 실행으로 확인하지 못했다(KIS 통지는 부분 체결을 낸다).
- 해결: 해제액을 order.remainingLockedAmount() 한 곳으로 모으고 취소 경로도 같은 메서드를 쓴다. 단 이 메서드가 의존하는 totalAmount 가 S6 때문에 틀리므로 S6 를 먼저 고친다.
- 다른 렌즈의 반증: backend/code-shape+layering+naming-terminology+tdd-purity — release 는 잠금 해제를 뜻하는 명시적 변경 동사이고 KDoc 이 해제를 설명 — 숨은 변경이 아님

고친 모양(예시):
```
if (order.orderStatus == OrderStatus.FILLED) {
    val excess = order.remainingLockedAmount()
    if (excess > BigDecimal.ZERO) {
        accountLedgerRepository.save(account.recordBuyUnlock(excess, orderId, "unlock-excess-$orderId"))
    }
}
```

### 109위 [high · 확신 low] 같은 @Transactional 안에서 락 예외를 잡고 계속 진행 — 트랜잭션이 이미 rollback-only 라 커밋에서 터지거나 모든 SKIPPED 기록이 사라짐

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/position/PositionExitTriggerOrchestrator.kt:42` · 항목표 data/transaction-boundary · 항목 X6

```
groups.forEach { (groupKey, candidates) ->
    try {
        processGroup(normalizedTicker, groupKey, price, quoteAt)
    } catch (ex: CannotAcquireLockException) {                       // ← L45 같은 바깥 트랜잭션 안에서 잡고
        markCandidatesSkipped(candidates, quoteAt, price, TriggerSkipReason.LOCK_CONFLICT)   // ← 이미 rollback-only 인 트랜잭션에 이어서 쓰기
```

- 원인: onQuote(@Transactional) 안에서 processGroup 이 부르는 findByIdWithLock·findArmedGroupForUpdate 가 CannotAcquireLockException·PessimisticLockingFailureException 을 던지면 catch 에서 markCandidatesSkipped(→ saveAll)로 같은 바깥 트랜잭션에 이어 쓴다. 리포지토리 프록시를 거친 예외는 참여 트랜잭션을 rollback-only 로 표시하고, PostgreSQL 은 오류 난 트랜잭션의 이후 문장을 모두 거부한다. 따라서 catch 안 saveAll 이 다시 터지거나, 터지지 않아도 커밋 때 UnexpectedRollbackException 이 난다. 예외가 onQuote 밖으로 나가면 QuoteEventListener.kt:49 의 runCatching 이 삼켜 흔적도 남지 않는다. 같은 파일 L155 의 DataIntegrityViolationException 은 REQUIRES_NEW 안쪽 트랜잭션에서 난 예외라 해당하지 않아 disproved 에 적었다.
- 생길 수 있는 일: 락 경합이 한 번 나면 그 틱의 모든 그룹 처리와 앞선 그룹의 SKIPPED·TRIGGERED 기록이 롤백된다. 이미 REQUIRES_NEW 로 커밋된 주문이 있으면 트리거는 ARMED 로 남아 다음 틱에 재발동한다. 잡았다고 믿은 실패가 조용히 사라진다.
- 해결: 그룹 단위 처리를 별도 빈의 REQUIRES_NEW 유스케이스로 빼서 그 실패가 바깥에 영향을 주지 않게 하거나, 위 X3 수정처럼 바깥 트랜잭션을 없앤 뒤 호출자(트랜잭션 밖)에서 잡고 새 트랜잭션으로 SKIPPED 를 기록한다.
- 다른 렌즈의 반증: backend/code-shape+layering+naming-terminology+tdd-purity — 한 식으로 검사 가능한 조건을 예외로 대신하는 모양이 아니라 DB 잠금 실패에 대한 처리다. 트랜잭션 오염 문제는 L2 로 따로 올렸다.

고친 모양(예시):
```
// 트랜잭션 밖 오케스트레이터
try { groupProcessor.process(groupKey, price, quoteAt) }   // @Transactional(REQUIRES_NEW) 별도 빈
catch (ex: LockConflictException) { skipRecorder.record(candidates, quoteAt, price, LOCK_CONFLICT) }  // 새 트랜잭션
```

### 110위 [high · 확신 low] 시세 틱마다 ARMED 그룹 수에 비례해 평가 전에 잠금 쿼리를 두 번씩 실행

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/position/PositionExitTriggerOrchestrator.kt:42` · 항목표 data/query · 항목 E2

```
groups.forEach { (groupKey, candidates) ->                       // ← L42 틱마다 그룹 수만큼 반복
...
val position = positionRepository.findByIdWithLock(groupKey.positionId).orElse(null)          // ← L55 평가 전에 그룹마다 락 쿼리
val triggers = triggerRepository.findArmedGroupForUpdate(ticker, groupKey.positionId, groupKey.triggerType)  // ← L56
```

- 원인: findByTickerAndState 로 후보를 한 번에 읽은 뒤, 그룹마다 processGroup 이 평가 전에 findByIdWithLock 과 findArmedGroupForUpdate 를 실행한다(그룹당 잠금 쿼리 2회). 조건이 안 맞는 대부분의 틱에도 모든 그룹의 포지션 행에 PESSIMISTIC_WRITE 를 잡고 바깥 트랜잭션이 끝날 때까지 쥔다. 평가에 필요한 값(포지션 평단·트리거 조건)은 이미 읽은 후보로 판정할 수 있다. 반복 횟수는 종목별 ARMED 그룹 수가 정하며 상수가 아니다.
- 생길 수 있는 일: 종목 시세가 들어올 때마다 그룹 수의 두 배 쿼리와 그만큼의 포지션 행 락이 생긴다. 같은 행을 잠그는 사용자 주문·체결 경로가 틱마다 대기한다.
- 해결: 락 없이 후보와 포지션을 일괄 조회해 조건을 먼저 평가하고, 발동한 그룹만 잠그고 다시 검증한다. 포지션은 IN 절 한 번으로 읽는다.
- 다른 렌즈의 반증: backend/code-shape+layering+naming-terminology+tdd-purity — 한 식으로 검사 가능한 조건을 예외로 대신하는 모양이 아니라 DB 잠금 실패에 대한 처리다. 트랜잭션 오염 문제는 L2 로 따로 올렸다.

고친 모양(예시):
```
val positions = positionRepository.findAllById(groups.keys.map { it.positionId }).associateBy { it.id }
val fired = groups.filter { (k, c) -> c.any { evaluator.evaluate(positions.getValue(k.positionId), it, price, quoteAt) != null } }
fired.keys.forEach { lockAndRecheck(it) }  // 발동 그룹만 락
```

### 111위 [high · 확신 low] 잔고를 가진 Account 에 @Version 이 없고 락 없는 쓰기 경로가 있음

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/account/Account.kt:26` · 항목표 data/schema-data-model · 항목 M2

```
@Entity
@Table(name = "accounts")
@Check(constraints = "deposit >= 0 AND available_deposit >= 0 AND locked_deposit >= 0")
class Account protected constructor() : BaseAuditEntity() {   // ← @Version·@DynamicUpdate 없음, 잔고 열 보유
    @Column(name = "deposit", nullable = false, precision = 20, scale = 4)
    lateinit var deposit: BigDecimal
```

- 원인: Account 는 deposit·available_deposit·locked_deposit 을 가진 동시 갱신 엔티티인데 @Version 이 없다. 락을 잡는 경로(입출금·주문·리스크 정책)는 PESSIMISTIC_WRITE 로 보호되지만 AccountCommandService.updateAccount·deactivateAccount 는 락 없이 같은 행을 전체 열 UPDATE 한다. 갱신 쪽 상세는 transaction-boundary X8 발견과 같다.
- 생길 수 있는 일: 락 없는 경로 하나만 있어도 잔고 갱신이 덮여 사라진다. 이후 새 코드가 락 규칙을 모르고 Account 를 고치면 조용히 같은 결함이 생긴다.
- 해결: Account 에 @Version var version: Long 을 추가하고 마이그레이션을 같은 변경에 포함한다. 락 없는 갱신 경로는 X8 발견대로 락을 쓰게 바꾼다.
- 다른 렌즈의 반증: backend/code-shape+layering+naming-terminology+tdd-purity — 프로젝트 조상이 2단(깊이 2)이라 > 2 가 아니고, 두 부모는 감사 컬럼용 @MappedSuperclass 로 JPA 매핑 상속 관례다

고친 모양(예시):
```
@Version
@Column(name = "version", nullable = false)
var version: Long = 0
```

### 112위 [high · 확신 low] 실패 로그에 ex.message 를 찍어 Slack 웹훅 비밀 URL 이 로그로 샐 수 있음

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/notification/SlackWebhookNotifier.kt:27` · 항목표 reliability/observability · 항목 O7

```
} catch (ex: Exception) {
    log.warn { "Slack webhook call failed: ${ex.message}" }   // ← ex.message 에 요청 URL(= Slack 웹훅 비밀 경로)이 포함될 수 있음
    false
```

- 원인: Spring RestTemplate 의 ResourceAccessException(연결 실패·타임아웃) 메시지는 요청 URL 을 포함한다("I/O error on POST request for \"<url>\": ..."). 이 URL 이 곧 비밀 토큰이 든 Slack 웹훅(properties.webhookUrl)이다. 타임아웃은 가장 흔한 실패라 WARN 로그에 자주 찍힌다. Spring 소스는 이 실행에서 열지 않아 확인이 필요하다(LLM-only).
- 생길 수 있는 일: 웹훅 URL 이 로그 저장소에 평문으로 남아, 접근자가 우리 Slack 채널로 임의 메시지를 보낼 수 있다. CLAUDE.md 의 시크릿 평문 로그 금지 위반이다.
- 해결: 로그에는 예외 클래스와 상태 코드만 남기고 URL 은 싣지 않는다. 원문이 필요하면 URL 을 마스킹한다.
- 다른 렌즈의 반증: reliability/failure-handling+load-concurrency+observability+security-config-hygiene — WARN 로그가 있어 E1 은 아님. 같은 지점의 문제는 E3·E2·O7 로 보고

고친 모양(예시):
```
} catch (ex: RestClientException) {
    log.warn { "Slack webhook call failed: type=${ex::class.simpleName}" }
    false
}
```

### 113위 [high · 확신 low] 리스너가 onMessage 안에서 매칭·DB 쓰기를 직접 하고, 컨테이너에 실행기·큐 상한이 없음

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/redis/QuoteEventListener.kt:32` · 항목표 reliability/load-concurrency · 항목 A1

```
override fun onMessage(message: Message, pattern: ByteArray?) {
    val quote = parseMessage(message.body) ?: return
    runCatching { localMatchingEngine.tryMatchPendingOrders(quote.ticker, quote) }  // ← 리스너 스레드에서 DB 매칭 직접 수행
// RedisListenerConfig.kt:17-18
val container = RedisMessageListenerContainer()  // ← setTaskExecutor·큐 상한 없음
container.setConnectionFactory(connectionFactory)
```

- 원인: onMessage 가 tryMatchPendingOrders(DB 조회·체결 트랜잭션)·updateCurrentPriceByTicker·exit trigger 처리를 모두 직접 실행한다. RedisListenerConfig 의 RedisMessageListenerContainer 는 setTaskExecutor 없이 만들어져 기본 실행기(메시지마다 새 스레드, 상한 없음)로 돈다(라이브러리 소스는 열지 않았고 Spring 문서 기준, LLM-only).
- 생길 수 있는 일: 시세 폭주(종목 수×틱) 때 메시지마다 새 스레드가 생겨 스레드 수가 무제한으로 늘고, 각 스레드가 DB 커넥션(풀 10)을 다투어 풀 고갈·OOM 으로 이어진다.
- 해결: 컨테이너에 크기·큐 상한·거부 정책이 있는 실행기를 지정하고, 리스너는 역직렬화 후 종목별 큐에 넘기기만 한다.
- 다른 렌즈의 반증: backend/code-shape+layering+naming-terminology+tdd-purity — public 메서드는 onMessage 하나이고 시세 이벤트를 소비자에게 나눠주는 팬아웃 진입점이라 존재 이유가 하나다. 컨텍스트 경계 문제는 L4·L6 으로 따로 올렸다

고친 모양(예시):
```
container.setTaskExecutor(ThreadPoolTaskExecutor().apply {
    corePoolSize = 4; maxPoolSize = 8; queueCapacity = 1000
    setRejectedExecutionHandler(ThreadPoolExecutor.CallerRunsPolicy()); initialize()
})
```

### 114위 [high · 확신 low] 운영 기본 설정이 ddl-auto: create — 기동마다 엔티티 테이블을 DROP 후 재생성

`backend/trading-api/src/main/resources/application.yaml:17` · 항목표 data/schema-data-model · 항목 M12

```
  jpa:
    hibernate:
      ddl-auto: create   # ← 프로파일 분기 없는 기본 설정에서 기동마다 스키마 DROP·재생성
```

- 원인: trading-api 의 운영 설정 파일은 application.yaml 하나뿐이고(src/main/resources 에 프로파일별 파일 없음) 여기서 spring.jpa.hibernate.ddl-auto 가 create 다. Hibernate 는 기동할 때마다 엔티티에 매핑된 테이블을 DROP 한 뒤 다시 CREATE 한다. 같은 파일의 datasource 기본값(jdbc:postgresql://localhost:5432/paper)은 collector-api 의 Flyway 기본 URL 과 같은 DB 다. 테스트용 create-drop 은 테스트 설정에 격리돼 있어 문제가 아니다. 운영은 환경변수 오버라이드(SPRING_JPA_HIBERNATE_DDL_AUTO 등)나 배포 설정으로 덮을 수 있으나 이 레포에서는 열람되지 않았다.
- 생길 수 있는 일: 기본 설정으로 배포·재시작하면 주문·체결·계좌·포지션 테이블이 기동마다 비워진다. 금융 원장 데이터가 소실되고 감사 추적이 끊긴다. 마이그레이션은 스키마 소유자가 아니라서 아무 효과가 없다.
- 해결: 운영 기본값을 validate(또는 none)로 바꾸고, 로컬 개발이 필요하면 application-local.yaml 프로파일에만 update·create 를 둔다. 스키마 변경은 마이그레이션 도구가 소유한다.
- 다른 렌즈의 반증: reliability/observability — show-sql: false 이고 application.yaml 에 org.hibernate.orm.jdbc.bind 설정이 0건이라 켜져 있지 않음. O11 의 세 번째 조건은 통과

고친 모양(예시):
```
spring:
  jpa:
    hibernate:
      ddl-auto: validate   # 스키마는 Flyway 마이그레이션이 소유, Hibernate 는 대조만
```

### 115위 [med · 확신 high] 취소 철자가 CANCELLED 와 CANCELED 로 갈림 — 저장되는 enum 값까지

`(repo)` · 항목표 backend/naming-terminology · 항목 N1

```
domain/enums/OrderStatus.kt:7:    CANCELLED,   // ← 주문 상태는 double-l
domain/enums/TriggerState.kt:4:    ARMED, TRIGGERED, CANCELED, FAILED, SKIPPED   // ← 트리거 상태는 single-l
domain/enums/NotificationEventType.kt:5:    ORDER_CANCELED,   // ← 알림 유형도 single-l
application/notification/SlackNotificationEventPublisher.kt:29:    fun publishOrderCanceled(accountId: Long, orderId: Long, message: String, ...)   // ← 같은 주문 취소를 single-l 로 발행
application/order/OrderCommandService.kt:278:        log.info { "order cancelled: orderId=$orderId, ..." }   // ← 같은 서비스 안의 로그는 double-l
domain/entity/strategy/Strategy.kt:67:    @Column(name = "cancelled_at")   // ← 컬럼명도 double-l
```

- 원인: 한 개념(취소)이 영국식 double-l(cancelled·cancellable)과 미국식 single-l(canceled)로 섞여 있다. 주문 상태 OrderStatus.CANCELLED, 전략 컬럼 is_cancelled·cancelled_at, Order.isCancellable 은 double-l 이고, 포지션 트리거 상태 TriggerState.CANCELED, 알림 유형 NotificationEventType.ORDER_CANCELED, 발행 함수 publishOrderCanceled 와 로그 문구는 single-l 이다. 같은 컨텍스트 경계가 아니라 철자만 다르다. 상태 enum 값은 저장·응답되므로 문자열 비교와 대사에서 어긋난다. 소스 파일에서 확인한 것은 enum 상수 이름까지다.
- 생길 수 있는 일: 상태 문자열로 조회·필터하거나 운영 화면·슬랙 유형을 맞출 때 CANCELLED 와 CANCELED 를 혼동하면 취소 건이 조용히 빠진다. 새 코드는 어느 쪽을 따라 쓸지 정할 기준이 없다.
- 해결: 철자를 한 가지로 정한다(저장 enum·컬럼이 이미 double-l 이 다수이므로 CANCELLED 를 추천). 저장되는 상수(TriggerState.CANCELED·알림 유형)를 바꾸면 기존 행이 고아가 되므로(N8) 데이터 이전 마이그레이션을 함께 낸다. 코드 안 이름(publishOrderCanceled·로그 문구)만 먼저 맞추는 것은 무위험이다. 선택은 사람이 한다.

고친 모양(예시):
```
enum class TriggerState { ARMED, TRIGGERED, CANCELLED, FAILED, SKIPPED }
fun publishOrderCancelled(accountId: Long, orderId: Long, message: String, ...)
```

### 116위 [med · 확신 high] 체결 수량이 qty 와 quantity 로 갈려 한 흐름에서 세 번 번역됨

`(repo)` · 항목표 backend/naming-terminology · 항목 N1

```
application/order/KisExecutionNoticeService.kt:41:    "executedPrice=${notice.executedPrice}, executedQty=${notice.executedQty}, ..."   // ← 통보 쪽 철자 executedQty
application/order/KisExecutionNoticeService.kt:49:            fillQty = notice.executedQty,   // ← executedQty 가 fillQty 로 번역됨
application/order/ExecutionProcessor.kt:114:        val prevQty = order.filledQuantity   // ← 한 줄 안에서 qty 와 Quantity 가 섞임
application/order/ExecutionProcessor.kt:127:                executedQuantity = fillQty,   // ← 엔티티는 executedQuantity 로 다시 번역
application/order/OrderCommandService.kt:274:            val remainQty = order.quantity.subtract(order.filledQuantity)   // ← qty 지역명
application/order/OrderCommandService.kt:275:            position?.unlockQuantity(remainQty)   // ← 바로 다음 줄은 Quantity
```

- 원인: KIS 체결 통보의 executedQty 가 ExecutionProcessor 의 fillQty 로 이름이 바뀌고, 다시 Execution 엔티티의 executedQuantity 로 한 번 더 바뀐다. 같은 메서드 안에서 prevQty·fillQty 와 order.filledQuantity·executedQuantity 가 함께 쓰인다. OrderCommandService 의 releasedQty·remainQty 는 같은 서비스의 unlockQuantity·lockQuantity 와 섞인다. DB 컬럼(quantity·executed_quantity·filled_quantity·locked_quantity)과 파생 메서드(findByTickerAndQuantityGreaterThan)는 quantity 로 통일돼 있어 인덱스 이름(idx_positions_ticker_qty)과 코드 지역 이름만 qty 다. KIS 원본 필드(ccld_qty·hldg_qty)는 외부 계약이라 제외했다.
- 생길 수 있는 일: 수량은 주문·체결·정산 대사(PTF2)의 핵심 값이다. 용어 사이 번역층이 늘수록 값이 다른 필드로 잘못 넘어가거나 검색이 누락될 때 원인을 찾기 어렵다.
- 해결: qty 철자를 quantity 로 통일한다(fillQty→fillQuantity 또는 executedQuantity, prevQty→previousQuantity, remainQty→remainingQuantity, releasedQty→releasedQuantity, 통보 필드 executedQty→executedQuantity). 인덱스 이름 idx_positions_*_qty 는 별도 마이그레이션이 필요하므로 코드 이름을 먼저 맞춘다.

고친 모양(예시):
```
fun fillLocal(orderId: Long, executedPrice: BigDecimal, executedQuantity: BigDecimal)
val remainingQuantity = order.quantity.subtract(order.filledQuantity)
position?.unlockQuantity(remainingQuantity)
```

### 117위 [med · 확신 high] 요청 상관 id 필터·추적이 레포 어디에도 없고 진입점에서도 새로 만들지 않음

`(repo)` · 항목표 reliability/observability · 항목 O10

```
// build.gradle:33
implementation 'org.springframework.boot:spring-boot-starter-actuator'   // ← Actuator 만 있고 micrometer-tracing·MDC 필터는 레포 어디에도 없음 (grep 0건)
// SlackNotificationService.kt:50
val requestId = UUID.randomUUID().toString()   // ← Slack 테스트 응답용 id — MDC 에 넣지도 외부 호출에 싣지도 않음
// QuoteEventListener.kt:28
) : MessageListener {   // ← Redis 리스너 진입 — 상관 id 생성·MDC.put 0건
// CollectorSubscriptionAdapter.kt:19
private val restTemplate = RestTemplate()   // ← 인터셉터 없는 맨 RestTemplate — 상관 id 전달 지점 없음
```

- 원인: src/main 과 build.gradle 전체에서 MDC·OncePerRequestFilter·X-Request-Id·traceparent·micrometer-tracing·ObservationRegistry·HandlerInterceptor 가 0건이다. 유일한 requestId 는 Slack 테스트 메시지 응답용 UUID 로, MDC 에 넣지도 외부 호출에 싣지도 않는다. Redis 리스너(QuoteEventListener)·스케줄러(LocalMatchingScheduler·SlackNotificationScheduler)·KIS WebSocket 처리 진입에서도 상관 id 를 만들지 않는다. 외부 호출용 RestTemplate 은 인터셉터가 없다(ClientHttpRequestInterceptor 0건). CollectorSubscriptionAdapter 는 설정 빈을 쓰지 않고 맨 RestTemplate() 을 직접 만든다.
- 생길 수 있는 일: 한 주문의 접수 → KIS 호출 → 체결 통보 → 원장 반영을 로그 줄로 이을 수 없다. 체결 유실이나 불확정 응답을 추적할 때 시각·주문 번호를 눈으로 맞춰야 해서 장애 원인 파악이 오래 걸린다.
- 해결: 1. 요청 진입에 OncePerRequestFilter 를 두어 X-Request-Id 를 읽거나 새로 만들어 MDC 에 넣고 응답 헤더에도 싣는다. 2. Redis 리스너·@Scheduled·WebSocket 메시지 처리 진입에서 MDC 에 id 를 새로 넣고 finally 에서 제거한다. 3. RestTemplate 빈에 ClientHttpRequestInterceptor 를 달아 외부 호출(KIS·Slack·collector)에 id 를 싣는다. 4. CollectorSubscriptionAdapter 의 RestTemplate() 직접 생성은 설정 빈 주입으로 바꾼다. 또는 micrometer-tracing 을 도입한다.

고친 모양(예시):
```
@Component
class RequestIdFilter : OncePerRequestFilter() {
    override fun doFilterInternal(req: HttpServletRequest, res: HttpServletResponse, chain: FilterChain) {
        val id = req.getHeader("X-Request-Id") ?: UUID.randomUUID().toString()
        MDC.put("requestId", id)
        res.setHeader("X-Request-Id", id)
        try { chain.doFilter(req, res) } finally { MDC.remove("requestId") }
    }
}
```

### 118위 [med · 확신 high] 의존성 취약점 검사·의존성 잠금·Gradle wrapper 체크섬·Docker 이미지 고정이 모두 없다

`(repo)` · 항목표 reliability/security-config-hygiene · 항목 S11

```
backend/trading-api/gradle/wrapper/gradle-wrapper.properties  distributionUrl=https\://services.gradle.org/distributions/gradle-8.14.4-bin.zip   // ← distributionSha256Sum 없음
backend/trading-api/gradle/wrapper/gradle-wrapper.properties  validateDistributionUrl=true   // ← URL 형식 검사일 뿐 체크섬 검증 아님
backend/trading-api/Dockerfile:1  FROM eclipse-temurin:21-jdk-alpine AS build   // ← 변경 가능한 태그, 다이제스트 고정 없음
backend/trading-api/Dockerfile:12  FROM eclipse-temurin:21-jre-alpine   // ← 위와 같음
backend/quant-worker/Dockerfile:1  FROM python:3.11-slim   // ← quant-* 4곳 모두 같은 모양
.github/workflows/full-test.yml:14  - uses: gradle/actions/setup-gradle@v4   // ← 빌드·시험만 있고 의존성 취약점 검사 단계 없음 (dependency-check·trivy·snyk·osv grep 0건)
(repo)  gradle.lockfile·dependabot.yml·renovate.json 없음 (find 결과 0건)   // ← 의존성 잠금·자동 갱신 도구 없음
```

- 원인: 1) 의존성 취약점 검사: dependabot.yml·renovate 설정이 없고, dependencyCheck·cyclonedx·owasp 등이 build.gradle 에 없고, .github/workflows 는 full-test.yml 하나인데 checkout·setup-java·setup-gradle·setup-node 만 쓰고 dependency-check·trivy·snyk·osv 같은 단계가 없다. 2) 의존성 잠금: gradle.lockfile 이 없고 dependencyLocking·lockAllConfigurations 도 없다. 3) wrapper: gradle-wrapper.properties 에 distributionSha256Sum 이 없다. validateDistributionUrl=true 는 URL 형식만 보며 체크섬 검증이 아니다. 4) Docker 기반 이미지: trading-api·collector-api 는 eclipse-temurin:21-jdk-alpine·21-jre-alpine, quant-* 는 python:3.11-slim 으로 변경 가능한 태그다. 다이제스트(@sha256) 고정이 없다.
- 생길 수 있는 일: 알려진 취약 라이브러리나 바뀐 기반 이미지가 아무 알림 없이 함께 배포되고 빌드가 재현되지 않는다. 변조된 Gradle 배포본을 내려받아도 걸러낼 장치가 없다.
- 해결: 1) .github/dependabot.yml 로 gradle·docker 생태계를 주 1회 점검한다. 2) CI 에 의존성 취약점 검사 단계(OWASP dependency-check 또는 trivy)를 넣는다. 3) gradle-wrapper.properties 에 distributionSha256Sum 을 추가한다. 4) Docker FROM 을 패치 태그나 @sha256 다이제스트로 고정한다. 5) 필요하면 dependencyLocking 을 켠다.

고친 모양(예시):
```
# .github/dependabot.yml
version: 2
updates:
  - package-ecosystem: gradle
    directory: /backend/trading-api
    schedule:
      interval: weekly
  - package-ecosystem: docker
    directory: /backend/trading-api
    schedule:
      interval: weekly

# gradle-wrapper.properties
distributionSha256Sum=<gradle-8.14.4-bin.zip 공식 sha256>
```

### 119위 [med · 확신 high] application 이 허용 목록 밖 org.springframework.data.domain.Page 를 사용

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/account/AccountQueryService.kt:10` · 항목표 backend/layering · 항목 L2

```
import org.springframework.data.domain.Page // ← 허용 목록 밖 Spring 타입
fun getLedgers(accountId: Long, filter: LedgerFilter): Page<LedgerResult> = // ← 반환 타입이 Spring Page
    accountLedgerRepository.findLedgers(accountId, filter)
```

- 원인: getLedgers 가 Page<LedgerResult> 를 반환하며 org.springframework.data.domain.Page 를 import 한다. PTB9 허용 목록은 Service·transaction.annotation·이벤트 리스너뿐이다. ArchUnit 의 동결 색인에는 이 파일의 L2 항목이 없고 별도 위반 줄로만 남아 있다.
- 생길 수 있는 일: 페이징 구현 세부가 유스케이스 계약이 되어 프레임워크를 바꾸면 presentation 까지 고쳐야 한다.
- 해결: application 고유 페이지 결과 타입(PageResult<T>)을 정의하고 어댑터에서 Page 로 변환한다.

고친 모양(예시):
```
data class PageResult<T>(val items: List<T>, val page: Int, val size: Int, val totalElements: Long)
```

### 120위 [med · 확신 high] 조회 조건이 Spring Data Pageable 을 필드로 가짐

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/account/query/LedgerFilter.kt:4` · 항목표 backend/layering · 항목 L2

```
import org.springframework.data.domain.PageRequest // ← 허용 목록 밖
import org.springframework.data.domain.Pageable
    val pageable: Pageable = PageRequest.of(0, 20) // ← Spring 타입 + 매직 넘버
```

- 원인: LedgerFilter 가 PageRequest·Pageable 을 import 하고 기본값을 PageRequest.of(0, 20) 으로 만든다. ArchUnit 동결 1건(생성자·필드·copy·component4 모두).
- 생길 수 있는 일: 유스케이스 입력이 Spring Data 에 묶여 시험·재사용 시 Spring 타입이 필요하고 기본 크기 20 이 매직 넘버로 박힌다(detekt MagicNumber 기준선).
- 해결: page: Int, size: Int 같은 application 고유 값으로 받고 컨트롤러·어댑터가 Pageable 로 바꾼다.

고친 모양(예시):
```
data class LedgerFilter(
    val transactionType: TransactionType? = null,
    val from: Instant? = null,
    val to: Instant? = null,
    val page: Int = 0,
    val size: Int = DEFAULT_PAGE_SIZE,
)
```

### 121위 [med · 확신 high] 출력 포트의 결과 타입이 port 패키지가 아닌 application.common.result 에 있음

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/common/result/QuoteSnapshot.kt:1` · 항목표 backend/layering · 항목 L3

```
package com.papertrading.api.application.common.result   // ← 공유 출력 포트(MarketQuotePort)의 결과 타입이 port 패키지가 아닌 result 패키지에 있음

data class QuoteSnapshot(
    val ticker: String,
```

- 원인: QuoteSnapshot 은 domain/port/MarketQuotePort.kt:3 이 import 해 getQuote 의 반환 타입으로 쓰고, infrastructure 의 RedisMarketQuoteAdapter.kt:4 와 QuoteEventListener.kt:9 도 import 한다. 레포 규칙 PTB8 은 포트 요청·결과 타입도 같은 port 패키지(공유는 application/common/port)에 두도록 정하고, MarketQuotePort 는 order·portfolio·position 세 컨텍스트가 쓰는 공유 포트다. 그래서 infrastructure 가 port 밖 application 패키지(application.common.result)를 import 하게 되어 PTB7 이 깨진다. ArchUnit 동결 목록(tools/T96587ad0.txt 604·1233-1240줄)에 있다.
- 생길 수 있는 일: 어댑터가 port 밖 패키지에 의존해 포트 이전이 끝나도 infrastructure→application 규칙이 계속 동결 목록에 남고, 시세 모델을 바꿀 때 포트·어댑터·리스너가 함께 흔들린다.
- 해결: QuoteSnapshot 을 application.common.port 로 옮기고, MarketQuotePort 도 같은 패키지로 옮긴다(L3 포트 자리 정정과 한 번에).

고친 모양(예시):
```
package com.papertrading.api.application.common.port

data class QuoteSnapshot(...)

interface MarketQuotePort {
    fun getQuote(tradingMode: TradingMode, ticker: String): QuoteSnapshot?
}
```

### 122위 [med · 확신 high] application 이 허용 목록 밖 Spring 타입을 쓴다

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/notification/SlackNotificationEventPublisher.kt:4` · 항목표 backend/layering · 항목 L2

```
import org.springframework.context.ApplicationEventPublisher   // ← 허용 목록(PTB9) 밖 org.springframework.context.*
import org.springframework.stereotype.Component                 // ← application 은 @Service 만 허용
// …
@Component
class SlackNotificationEventPublisher(
    private val eventPublisher: ApplicationEventPublisher,
```

- 원인: application 인 이 클래스가 @Component 와 ApplicationEventPublisher(org.springframework.context)를 쓴다. PTB9 허용 목록은 @Service·transaction.annotation·@EventListener·@TransactionalEventListener·TransactionPhase 뿐이다. ArchUnit 동결 목록에도 있다. order·position 컨텍스트 4개 파일이 이 클래스를 직접 주입받는다.
- 생길 수 있는 일: 프레임워크 이벤트 버스에 application 이 묶여 단위 시험이 Spring 이벤트 퍼블리셔 목을 요구하고, 이벤트 발행 방식을 바꾸면 호출 컨텍스트까지 다시 컴파일된다.
- 해결: application/notification/port 에 NotificationEventPublisher 포트를 두고, ApplicationEventPublisher 를 쓰는 구현은 infrastructure 로 옮긴다. 호출 컨텍스트는 포트만 주입받는다.

고친 모양(예시):
```
// application/notification/port
interface NotificationEventPublisher {
    fun publishOrderCreated(accountId: Long, orderId: Long, message: String, occurredAt: Instant)
}
// infrastructure/notification
@Component
class SpringNotificationEventPublisher(private val publisher: ApplicationEventPublisher) : NotificationEventPublisher
```

### 123위 [med · 확신 high] 서비스가 시각과 난수를 직접 읽는다

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/notification/SlackNotificationService.kt:40` · 항목표 backend/code-shape · 항목 F4

```
        val updated = policyStore.update(
            current.copy(
                // …
                updatedAt = Instant.now(),   // ← 시각을 직접 읽음
// …
        val requestId = UUID.randomUUID().toString()   // ← 난수를 직접 읽음 (L50)
```

- 원인: updateConfig 가 Instant.now() 로 updatedAt 을, sendTestMessage 가 UUID.randomUUID() 로 requestId 를 만든다. Clock·IdGenerator 주입이 없다. requestId 는 전송 호출과 연결되지도 않는다.
- 생길 수 있는 일: updatedAt·requestId 를 시험이 고정할 수 없고, 호출자가 받는 requestId 로 전송 결과를 추적할 수도 없다.
- 해결: 생성자에 Clock 과 requestId 생성기를 주입하고, requestId 를 로그나 전송 호출에 실제로 연결하거나 반환값에서 뺀다.

고친 모양(예시):
```
class SlackNotificationService(private val policyStore: SlackNotificationConfig, private val sender: NotificationSender, private val clock: Clock) {
    // updatedAt = Instant.now(clock)
}
```

### 124위 [med · 확신 high] 허용 목록 밖 Spring 타입 ApplicationEventPublisher 를 application 이 사용

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/ExecutionProcessor.kt:25` · 항목표 backend/layering · 항목 L2

```
import org.springframework.context.ApplicationEventPublisher   // ← 허용 목록 밖 Spring import
...
    private val eventPublisher: ApplicationEventPublisher,          // ← L49
```

- 원인: 허용 목록은 stereotype.Service·transaction.annotation.*·context.event.EventListener·transaction.event.* 뿐인데 org.springframework.context.ApplicationEventPublisher 를 주입하고 publishEvent 를 부른다. ArchUnit 동결 1건(tools/T96587ad0.txt:95).
- 생길 수 있는 일: 프레임워크 세부가 유스케이스에 새고, 이벤트 발행 시험이 Spring 컨텍스트나 목을 요구한다.
- 해결: application 포트 DomainEventPublisher 를 두고 infrastructure 가 ApplicationEventPublisher 로 구현한다.

고친 모양(예시):
```
interface DomainEventPublisher { fun publish(event: Any) }   // application/common/port
```

### 125위 [med · 확신 high] fillLocal 이 체결번호(UUID)와 체결 시각을 함수 안에서 직접 만듦

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/ExecutionProcessor.kt:65` · 항목표 backend/code-shape · 항목 F4

```
            externalExecutionId = "LOCAL-${UUID.randomUUID()}",   // ← 난수를 함수 안에서 직접 생성 — 시험이 못 고정, 재전달해도 번호가 매번 달라 멱등 가드 무력
            executedAt = Instant.now(),                            // ← 시각 직접 읽기
```

- 원인: LOCAL 체결의 externalExecutionId 를 UUID.randomUUID() 로, executedAt 을 Instant.now() 로 함수 안에서 직접 읽는다. Clock·IdGenerator 주입이 없어 시험이 값을 고정할 수 없다. 더 중요하게, 호출마다 새 체결번호가 만들어지므로 processFill 의 멱등 가드(findByExternalExecutionId, L95)가 LOCAL 체결에는 아무것도 막지 못한다(주문 상태 검사만 남는다).
- 생길 수 있는 일: LOCAL 모드에서 같은 체결 요청이 두 번 들어오면 둘 다 새 체결번호로 통과해 부분 체결이 중복 반영될 수 있다. 시험도 체결번호·시각을 고정하지 못한다.
- 해결: 호출자(LocalMatchingEngine)가 매칭 이벤트에서 체결번호·시각을 만들어 넘기고, 시각은 Clock 을 주입받는다.

고친 모양(예시):
```
fun fillLocal(orderId: Long, fillPrice: BigDecimal, fillQty: BigDecimal, matchId: String) {
    processFill(orderId, fillPrice, fillQty, "LOCAL-$matchId", Instant.now(clock))
}
```

### 126위 [med · 확신 high] 수수료 정책을 체결 시각(executedAt)이 아니라 처리 시각 Instant.now() 로 조회

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/ExecutionProcessor.kt:108` · 항목표 backend/code-shape · 항목 F4

```
        val feePolicy = feePolicyRepository.findActivePolicy(tradingMode, marketType, Instant.now())   // ← 체결 시각이 아닌 처리 시각
...
                        settledAt = Instant.now(),                                                      // ← L185 시각 직접 읽기
```

- 원인: processFill 이 executedAt 을 받는데도 수수료 정책 조회(L108)와 정산 시각(L185)에 Instant.now() 를 직접 읽는다. 외부(KIS) 체결 통지가 늦게 도착하면 체결 시점에 유효했던 정책이 아니라 지금 유효한 정책으로 수수료·세금이 계산된다. Clock 주입도 없다.
- 생길 수 있는 일: 수수료율 개정 경계(effective_from/until) 근처에 늦게 도착한 체결이 다른 정책으로 계산되고, 시험이 시각을 고정할 수 없다.
- 해결: 정책 조회에는 executedAt 을, 정산 시각에는 주입한 Clock 을 쓴다.

고친 모양(예시):
```
val feePolicy = feePolicyRepository.findActivePolicy(tradingMode, marketType, executedAt)
...
settledAt = Instant.now(clock),
```

### 127위 [med · 확신 high] BigDecimal 을 == 로 비교 (prevQty == BigDecimal.ZERO)

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/ExecutionProcessor.kt:116` · 항목표 backend/code-shape · 항목 F1

```
        val newAvgPrice = if (prevQty == BigDecimal.ZERO) fillPrice   // ← BigDecimal 을 == 로 비교 (scale 다르면 false)
```

- 원인: Kotlin 의 == 는 BigDecimal.equals 라 scale 까지 비교한다. DB 에서 읽은 filledQuantity 는 scale 8 의 0(0E-8)이라 BigDecimal.ZERO(scale 0)와 같지 않다. 그러면 첫 체결인데도 else 가중평균 가지로 가고, 결과는 fillPrice 를 소수 4자리로 반올림한 값이다. 값은 대개 같지만 fillPrice 가 4자리를 넘으면 첫 가지(원값)와 둘째 가지(반올림값)가 달라진다. 렌즈 기본 심각도는 high 이나 다른 가지가 거의 같은 값을 내므로 med 로 낮춘다.
- 생길 수 있는 일: 첫 체결의 평균가가 저장된 주문(scale 8 의 0)과 새 주문(scale 0 의 0)에서 다른 가지로 계산돼, 소수 5자리 이상 체결가에서 평균가가 다르게 저장될 수 있다.
- 해결: signum() 또는 compareTo 로 비교한다(S6 수정으로 이 분기는 사라진다).

고친 모양(예시):
```
val isFirstFill = prevQty.signum() == 0
```

### 128위 [med · 확신 high] 체결마다 계정의 활성 주문 전체를 로딩한 뒤 앱에서 ticker 로 거름

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/ExecutionProcessor.kt:246` · 항목표 data/query · 항목 E4

```
val activeOrders = orderRepository.findByAccountIdAndOrderStatusIn(
    accountId, listOf(OrderStatus.PENDING, OrderStatus.PARTIAL)
)   // ← 계정의 활성 주문 전부를 엔티티로 로딩
if (activeOrders.none { it.ticker == ticker }) {   // ← ticker 조건은 컬럼 — exists 쿼리로 DB 에서
```

- 원인: tryUnsubscribe 는 계정의 PENDING/PARTIAL 주문을 엔티티로 전부 읽고 `none { it.ticker == ticker }` 로 걸러 같은 종목 활성 주문이 있는지만 봅니다. ticker 는 컬럼이라 DB 의 exists 쿼리로 표현됩니다. 이 호출은 FILLED 되는 모든 체결 트랜잭션 안에서 돕니다.
- 생길 수 있는 일: 활성 주문이 많은 자동매매 계정은 체결 1건마다 주문 수만큼 행·더티체킹 스냅샷을 만들어 체결 트랜잭션이 길어지고 락 보유 시간이 늘어납니다.
- 해결: `existsByAccountIdAndTickerAndOrderStatusIn` 파생 쿼리로 바꿉니다.

고친 모양(예시):
```
fun existsByAccountIdAndTickerAndOrderStatusIn(accountId: Long, ticker: String, statuses: Collection<OrderStatus>): Boolean
// 호출: if (!orderRepository.existsByAccountIdAndTickerAndOrderStatusIn(accountId, ticker, ACTIVE)) { ... }
```

### 129위 [med · 확신 high] 체결 통지 로그에 계좌 번호 원문이 포함됨

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/KisExecutionNoticeService.kt:23` · 항목표 reliability/observability · 항목 O7

```
"duplicate KIS execution notice ignored: mode=${notice.mode}, externalOrderId=${notice.externalOrderId}, externalExecutionId=${notice.externalExecutionId}"   // ← 실행 id 에 계좌 번호가 들어 있음
...
"externalExecutionId=${notice.externalExecutionId}, accountScope=${notice.accountScope}, " +   // ← 계좌 번호 원문
```

- 원인: 미매칭 통지의 ERROR 로그가 accountScope(CANO-상품코드, 실제 계좌 번호)를 그대로 출력한다. 중복 통지 INFO 로그의 externalExecutionId 도 모드:계좌:주문번호:체결번호 형식이라 계좌 번호를 품고 있다. 마스킹이 없다.
- 생길 수 있는 일: 로그 저장소에 계좌 번호가 남는다. 토큰은 아니지만 개인정보·금융 식별자 노출이다.
- 해결: accountScope 와 execution id 는 앞 4자리·뒤 2자리만 남기는 마스킹 함수를 거쳐 출력한다.

고친 모양(예시):
```
fun mask(scope: String?) = scope?.let { it.take(4) + "****" + it.takeLast(3) }
log.error { "... accountScope=${mask(notice.accountScope)}" }
```

### 130위 [med · 확신 high] 주문 실패 로그에 예외 객체가 없고, 재던진 예외도 어디서도 로그되지 않음

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/KisLiveOrderExecutor.kt:39` · 항목표 reliability/failure-handling · 항목 E2

```
}.onFailure {
    log.error { "KIS_LIVE 주문 접수 실패: orderId=$orderId, reason=${it.message}" }   // ← 예외 객체·스택 미포함
    throw it
}
// GlobalExceptionHandler.kt:59-63  fun handleException(ex: Exception) = ResponseEntity.status(500)...   // ← 로그 한 줄 없음
```

- 원인: onFailure 가 reason=${it.message} 문자열만 남기고 예외 객체를 넘기지 않아 스택과 cause 가 사라진다. 재던진 예외는 GlobalExceptionHandler.handleException(Exception) 이 받는데, 그 핸들러도 예외를 로그하지 않는다(GlobalExceptionHandler.kt:59-63, 내 묶음 밖이지만 열어 확인함). 스택이 어느 층에서도 기록되지 않는다.
- 생길 수 있는 일: KIS 주문 실패 원인이 RestClient 예외인지 NPE 인지 로그로 구분할 수 없다. 실계좌 장애 때 원인 추적이 불가능하다.
- 해결: log.error(it) { ... } 로 예외 객체를 넘긴다. 전역 핸들러의 5xx 경로에도 예외 로그를 둔다.

고친 모양(예시):
```
}.onFailure {
    log.error(it) { "KIS_LIVE 주문 접수 실패: orderId=$orderId" }
    throw it
}
```

### 131위 [med · 확신 high] KIS_PAPER 주문 호출 실패를 불확정으로 다루지 않고 예외로 롤백

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/KisPaperOrderExecutor.kt:27` · 항목표 reliability/failure-handling · 항목 I2

```
runCatching {
    val orno = kisOrderRestClient.placeOrder(order, "paper")   // ← 불확정 응답 구분 없음
    order.assignExternalOrderId(orno)
...
}.onFailure {
    log.error { "KIS_PAPER 주문 접수 실패: orderId=$orderId, reason=${it.message}" }   // ← 실패 단정
    throw it   // ← 롤백, UNKNOWN 없음
}
```

- 원인: KisLiveOrderExecutor 와 같은 구조다. 타임아웃·5xx 가 모두 로그 후 재던지기로 끝나 주문 트랜잭션이 롤백되고, UNKNOWN 상태와 조회 확정 경로가 없다. 모의투자 계좌라 금전 피해는 없지만 같은 코드가 실전 경로의 기준이 된다.
- 생길 수 있는 일: 모의 계좌에서도 KIS 에는 주문이 있고 우리 DB 에는 없는 상태가 생기며, 재시도 때 이중 주문이 된다. 체결 통지는 미매칭으로 버려진다.
- 해결: Live 와 동일하게 확정 실패와 불확정을 나눠 UNKNOWN 상태와 조회 확정 경로를 둔다. 모의 계좌라 심각도는 med 로 낮췄다.

고친 모양(예시):
```
catch (e: RestClientException) { order.markUnknown(); /* 조회 큐로 */ }
```

### 132위 [med · 확신 high] 주문 실패 로그에 예외 객체가 없음

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/KisPaperOrderExecutor.kt:36` · 항목표 reliability/failure-handling · 항목 E2

```
}.onFailure {
    log.error { "KIS_PAPER 주문 접수 실패: orderId=$orderId, reason=${it.message}" }   // ← 예외 객체·스택 미포함
    throw it
}
```

- 원인: KisLiveOrderExecutor 와 같은 패턴이다. 메시지 문자열만 남기고 재던지며, 전역 핸들러도 로그하지 않아 스택이 사라진다.
- 생길 수 있는 일: 모의 주문 실패 원인을 로그에서 되짚을 수 없다.
- 해결: log.error(it) { ... } 로 예외 객체를 함께 넘긴다.

고친 모양(예시):
```
log.error(it) { "KIS_PAPER 주문 접수 실패: orderId=$orderId" }
```

### 133위 [med · 확신 high] recalculate 본문이 빈 줄 제외 46줄 — 시세 조회·평가액·비중 계산·조회/갱신/생성 분기·저장이 한 함수에 섞임

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/portfolio/PortfolioSnapshotCommandService.kt:24` · 항목표 backend/code-shape · 항목 S1

```
    fun recalculate(accountId: Long, businessDate: LocalDate): List<PortfolioSnapshot> {   // ← 본문 L25~L73, 빈 줄 제외 46줄(기준 40)
        val priced = positions.map { position ->   // ← 1단계: 시세 조회·평가액
        return priced.map { item ->   // ← 2~4단계: 비중 계산 + 조회/갱신/생성 + 저장을 한 람다에 담음
            val entity = portfolioSnapshotRepository.findByAccountIdAndBusinessDateAndTicker(
            portfolioSnapshotRepository.save(entity)
```

- 원인: 본문 L25~L73 이 49줄이고 빈 줄 3개를 빼면 46줄로 기준 40줄을 넘는다. 1) 포지션별 시세 조회와 평가액 계산 2) 비중·미실현손익 계산 3) 기존 스냅샷 조회 후 갱신 또는 생성 4) 저장이 한 함수·한 람다 안에 있어 추상화 수준이 넷이다.
- 생길 수 있는 일: 종목 수만큼 도는 람다 안에서 조회·계산·저장이 한꺼번에 일어나 비중 계산만 바꿔도 저장 경로까지 다시 읽어야 한다. 계산 부분을 따로 시험할 수 없어 분기(합계 0·기존 행 있음/없음)가 서비스 단위 시험에 묶인다.
- 해결: 시세 반영(priced 만들기), 비중·미실현손익 계산, 갱신-또는-생성(upsert)을 각각 private 함수 또는 도메인 메서드로 뺀다. 계산 규칙은 Position·PortfolioSnapshot 쪽 메서드로 옮기면 D3 도 함께 풀린다.

고친 모양(예시):
```
@Transactional
fun recalculate(accountId: Long, businessDate: LocalDate): List<PortfolioSnapshot> {
    val account = loadLockedAccount(accountId)
    val priced = pricePositions(accountId)
    val total = priced.sumOf { it.marketValue }
    return priced.map { upsert(account, businessDate, it, weightOf(it, total)) }
}
```

### 134위 [med · 확신 high] 보유 종목마다 스냅샷을 조회·저장하는 루프 — 종목 수만큼 왕복

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/portfolio/PortfolioSnapshotCommandService.kt:45` · 항목표 data/query · 항목 E2

```
        return priced.map { item ->                       // ← 종목마다 한 바퀴
            ...
            val entity = portfolioSnapshotRepository.findByAccountIdAndBusinessDateAndTicker(   // ← 종목마다 SELECT 1회
                accountId = accountId,
                businessDate = businessDate,
                ticker = item.position.ticker!!,
            ).map {
            ...
            portfolioSnapshotRepository.save(entity)      // ← 종목마다 save (신규는 IDENTITY 즉시 INSERT)
```

- 원인: priced.map 안에서 종목마다 findByAccountIdAndBusinessDateAndTicker 로 기존 스냅샷을 SELECT 하고 save 를 종목마다 부른다. 같은 (accountId, businessDate) 의 기존 행은 PortfolioSnapshotRepository.findByAccountIdAndBusinessDateOrderByTickerAsc 한 번이면 모두 읽힌다. 이 메서드는 PortfolioSnapshotRepository.kt:15 에 선언만 돼 있고 어디서도 호출되지 않는다. 같은 루프 안에서 종목마다 Redis 시세도 따로 읽는다(L29-33). 루프 횟수는 보유 종목 수가 정하는 데이터 크기 반복이다(상수 ≤3 아님).
- 생길 수 있는 일: 보유 종목이 N개면 계좌 행 락(FOR UPDATE)을 쥔 채 SELECT N회와 신규 INSERT N회(IDENTITY 는 save 즉시 실행)가 나간다. 같은 계좌의 주문 처리가 그만큼 오래 막힌다.
- 해결: 기존 스냅샷을 한 번에 읽어 ticker 맵으로 대조하고, 저장은 saveAll 로 모은다. Redis 시세도 트랜잭션 밖에서 미리 모은다(X3 발견 참조).

고친 모양(예시):
```
val existing = portfolioSnapshotRepository
    .findByAccountIdAndBusinessDateOrderByTickerAsc(accountId, businessDate)
    .associateBy { it.ticker }

val snapshots = priced.map { item ->
    val ticker = requireNotNull(item.position.ticker)
    existing[ticker]?.also { it.refresh(/* 계산값 */) }
        ?: PortfolioSnapshot.create(/* 계산값 */)
}
return portfolioSnapshotRepository.saveAll(snapshots)
```

### 135위 [med · 확신 high] recalculate 본문이 61줄 — 검증·잠금·실행기록·집계·저장·실패기록이 한 함수에 섞여 있음

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/portfolio/TaxSummaryCommandService.kt:43` · 항목표 backend/code-shape · 항목 S1

```
@Transactional
fun recalculate(  // ← 43행: 본문 49~115행(빈 줄 6개 제외 61줄)
    ...
    return try {  // ← 74행: 집계·계산·upsert·성공 기록이 한 try 안
    ...
} catch (ex: ApiDomainException) {
    run.fail(ex.message)
    taxSummaryRunRepository.save(run)  // ← 실패 기록이
    throw ex
} catch (ex: IllegalArgumentException) {
    ...
    taxSummaryRunRepository.save(run)  // ← catch 마다
} catch (ex: Exception) {
    ...
    taxSummaryRunRepository.save(run)  // ← 세 번 반복
```

- 원인: 빈 줄·주석을 뺀 본문(49~115행)이 61줄로 팀 수치 40줄을 넘고 detekt LongMethod 기준선에도 올라 있다(tools/Tb661de0c.txt 105행). 연도 검증, 중복 실행 검사, 계좌 잠금·모드 검사, 실행 이력 시작, 집계·계산·upsert, 성공·실패 기록의 6단계(추상화 수준 둘 이상)가 한 함수에 있고, catch 3갈래마다 실패 기록 2~3줄이 복사돼 있다.
- 생길 수 있는 일: 한 단계를 고칠 때 함수 전체를 읽어야 하고, 실패 기록이 catch 마다 복사돼 한 갈래만 고치면 FAILED 이력이 서로 어긋난다.
- 해결: 단계별 private 함수(검증·실행 시작·upsert·실패 기록)로 추출하고 실패 기록은 한 곳(`failRun`)으로 모은다.

고친 모양(예시):
```
@Transactional
fun recalculate(accountId: Long, taxYear: TaxYear, force: Boolean, runType: TaxSummaryRunType = TaxSummaryRunType.MANUAL): TaxSummary {
    assertRecalculable(accountId, taxYear, force)
    val account = loadLocalAccount(accountId)
    val run = startRun(account, taxYear, runType)
    return try { completeRun(run, upsertSummary(account, taxYear)) } catch (ex: Exception) { throw failRun(run, ex) }
}
```

### 136위 [med · 확신 high] «LOCAL 계좌만 지원» 규칙을 같은 모양으로 한 번 더 구현

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/portfolio/TaxSummaryCommandService.kt:60` · 항목표 backend/layering · 항목 D3

```
if (account.tradingMode != TradingMode.LOCAL) {  // ← Query 서비스와 같은 규칙을 다시 구현
    throw InvalidAccountModeForTaxSummaryException(
```

- 원인: TaxSummaryQueryService.ensureLocalMode 와 같은 규칙·같은 예외·같은 메시지를 이 서비스도 직접 갖는다(위 발견의 두 번째 자리).
- 생길 수 있는 일: 한쪽만 바뀌면 조회와 재계산이 서로 다른 계좌 범위를 허용한다.
- 해결: Account.supportsTaxSummary() 하나로 모은다.

고친 모양(예시):
```
if (!account.supportsTaxSummary()) throw InvalidAccountModeForTaxSummaryException(...)
```

### 137위 [med · 확신 high] 실패 이력(FAILED)을 같은 트랜잭션에서 기록하고 예외를 다시 던져 롤백과 함께 사라진다

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/portfolio/TaxSummaryCommandService.kt:101` · 항목표 data/transaction-boundary · 항목 X6

```
            val saved = taxSummaryRepository.save(summary)
...
        } catch (ex: ApiDomainException) {
            run.fail(ex.message)                       // ← 같은 트랜잭션에서 FAILED 기록
            taxSummaryRunRepository.save(run)
            throw ex                                   // ← 다시 던지면 전체 롤백 — FAILED 행도 함께 사라진다
        } catch (ex: Exception) {
            val wrapped = TaxSummaryComputeFailedException("세금 요약 계산에 실패했습니다.", ex)
            run.fail(wrapped.message)
            taxSummaryRunRepository.save(run)          // ← 깨진 세션에서 save — 2차 예외가 원래 예외를 가릴 수 있음
```

- 원인: recalculate 는 @Transactional 하나 안에서 TaxSummaryRun(RUNNING)을 만들고, 실패하면 catch 안에서 run.fail + save 를 한 뒤 예외를 다시 던진다. 던지는 순간 전체가 롤백되어 FAILED 행도 함께 사라진다. 같은 이유로 RUNNING 행은 커밋 전까지 다른 트랜잭션에 보이지 않아 L54 의 existsRunning 가드는 사실상 작동하지 않는다. 직렬화는 L58 의 계좌 락이 대신 맡는다. 또 DB 오류로 세션이 깨진 뒤 L103 의 save(run)이 2차 예외를 내면 원래 예외를 가린다. SnapshotJobService 는 같은 문제를 SnapshotJobRunTxService(REQUIRES_NEW)로 풀어 놓았다. L66-72 의 DataIntegrityViolationException catch 는 변환 후 다시 던져 계속 진행하지 않으므로 이 발견의 대상이 아니다.
- 생길 수 있는 일: 세금 요약 실행 실패의 원인·시각이 tax_summary_runs 에 남지 않는다. 운영자가 실패를 감사·추적할 수 없고 RUNNING 중복 방지 가드도 무력하다.
- 해결: SnapshotJobRunTxService 처럼 start/complete/fail 을 REQUIRES_NEW 전용 빈으로 분리하고, 계산·저장 트랜잭션 바깥에서 catch 해 fail 을 기록한다.

고친 모양(예시):
```
// @Transactional 없는 오케스트레이터
val runId = taxSummaryRunTxService.start(accountId, year, runType)   // REQUIRES_NEW 로 RUNNING 커밋
try { val s = computeTxService.recalculate(accountId, year); taxSummaryRunTxService.complete(runId); return s }
catch (e: Exception) { taxSummaryRunTxService.fail(runId, e.message); throw e }
```

### 138위 [med · 확신 high] 실패한 세금 요약 실행의 FAILED 기록이 같은 트랜잭션 안에서 저장돼 예외와 함께 롤백됨

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/portfolio/TaxSummaryCommandService.kt:101` · 항목표 reliability/observability · 항목 O8

```
        } catch (ex: ApiDomainException) {
            run.fail(ex.message)               // ← 같은 @Transactional 안에서 FAILED 표시
            taxSummaryRunRepository.save(run)
            throw ex                           // ← 재던짐 → 롤백, FAILED·RUNNING 행 모두 사라짐
```

- 원인: recalculate 는 메서드 전체가 @Transactional 이고 ApiDomainException 은 RuntimeException 이라, run.fail(...)·save(run) 뒤에 throw 하면 TaxSummaryRun 행(RUNNING 포함) 전체가 롤백된다. 같은 패턴의 SnapshotJobService 는 REQUIRES_NEW 로 이 문제를 피하고 KDoc 에도 적어 두었는데 세금 쪽은 그렇지 않다. 성공 시 기존 TaxSummary 를 덮어쓰는데 이전 값 기록도 없다.
- 생길 수 있는 일: 세금 요약 실패 이력이 DB 어디에도 남지 않아 어떤 계좌·연도가 몇 번 실패했는지 사후에 알 수 없다.
- 해결: SnapshotJobRunTxService 처럼 실행 이력의 start·complete·fail 을 REQUIRES_NEW 별도 빈으로 분리한다. 실패는 ERROR 로그로도 남긴다.

### 139위 [med · 확신 high] 세금 계산 규칙이 서비스 안 private compute 와 TaxSummaryCalculator 두 곳에 있고, 분리해 둔 계산기는 운영 코드에서 쓰이지 않음

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/portfolio/TaxSummaryCommandService.kt:118` · 항목표 backend/code-shape · 항목 S3

```
118: private fun compute(input: TaxSettlementAggregate): TaxComputationResult {  // ← 서비스 안의 사본(운영에서 실제로 도는 쪽)
123:     val totalRealized = input.totalRealizedPnl.setScale(4, RoundingMode.HALF_UP)
124:     val taxable = totalRealized.subtract(input.totalFee).subtract(input.totalTax).setScale(4, RoundingMode.HALF_UP)
---- tax/TaxSummaryCalculator.kt
10: class TaxSummaryCalculator {  // ← 같은 본문(16~19행)인데 운영 코드 어디서도 주입하지 않음
```

- 원인: 서비스가 실행 이력·저장 흐름과 세금 계산 규칙(통화 검사·scale 4·음수 절사)을 함께 갖고, 같은 본문을 가진 @Component TaxSummaryCalculator 가 따로 있다. grep 으로 보면 TaxSummaryCalculator 를 참조하는 곳은 TaxSummaryCalculatorTest 뿐이라 계산기는 운영에서 호출되지 않는다. 바꿀 이유가 둘(흐름·계산)이고 계산 규칙의 사본이 둘이다. S3 에 정확히 맞는 항목이 없어 «서비스가 두 이유로 바뀜»로 이 번호에 적는다.
- 생길 수 있는 일: scale·반올림·음수 처리 같은 금액 규칙을 한쪽만 고치면 시험(계산기 쪽)은 초록인데 운영(서비스 쪽)은 옛 규칙으로 계산해 세금 요약 금액이 조용히 틀린다.
- 해결: 서비스 생성자에 TaxSummaryCalculator 를 주입해 쓰고 서비스의 private compute 를 지운다(또는 계산기를 지운다).

고친 모양(예시):
```
@Service
class TaxSummaryCommandService(
    ...,
    private val taxSummaryCalculator: TaxSummaryCalculator,
) {
    // val result = taxSummaryCalculator.compute(aggregate)
}
```

### 140위 [med · 확신 high] «LOCAL 계좌만 지원» 규칙을 서비스가 직접 구현하고 Account 에는 그 메서드가 없음

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/portfolio/TaxSummaryQueryService.kt:46` · 항목표 backend/layering · 항목 D3

```
private fun ensureLocalMode(accountId: Long, tradingMode: TradingMode) {
    if (tradingMode != TradingMode.LOCAL) {  // ← 도메인 값을 읽어 서비스가 판단
        throw InvalidAccountModeForTaxSummaryException(
```

- 원인: 서비스가 account.tradingMode 를 읽어 LOCAL 이 아니면 예외를 던진다. 같은 규칙과 같은 메시지를 TaxSummaryCommandService 도 다시 구현한다. Account 에는 이 규칙을 가진 메서드가 없다(Account.kt 363~366행은 계좌 종류·모드 조합 규칙을 도메인에 두고 있어 같은 방식이 가능하다).
- 생길 수 있는 일: 규칙이 바뀌면(예: KIS 모의 계좌 허용) 서비스 둘을 모두 고쳐야 하고 하나를 빠뜨리면 조회·재계산 허용 범위가 어긋난다.
- 해결: Account 에 `supportsTaxSummary()` 같은 메서드를 두고 두 서비스가 그것을 쓴다.

고친 모양(예시):
```
// Account
fun supportsTaxSummary(): Boolean = tradingMode == TradingMode.LOCAL
// 서비스
if (!account.supportsTaxSummary()) throw InvalidAccountModeForTaxSummaryException(...)
```

### 141위 [med · 확신 high] 유스케이스가 Spring Data 의 Page·Pageable 을 시그니처에 씀

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/portfolio/TradingJournalQueryService.kt:10` · 항목표 backend/layering · 항목 L2

```
import org.springframework.data.domain.Page  // ← 허용 목록 밖
import org.springframework.data.domain.Pageable
fun list(filter: TradingJournalFilter, pageable: Pageable): Page<TradingJournalResult> {  // ← 시그니처가 Spring Data 타입
```

- 원인: org.springframework.data.domain.Page·Pageable 을 application 이 import 해 list 의 인자와 반환 타입으로 쓴다(PTB9 허용 밖). ArchUnit 동결 303~305행.
- 생길 수 있는 일: 페이징 타입이 프레임워크에 묶여 도메인 쪽 결과 타입으로 갈아끼울 수 없다.
- 해결: application 자체 PageQuery·PageResult 타입으로 바꾸고 변환은 presentation 이 한다.

고친 모양(예시):
```
data class PageResult<T>(val items: List<T>, val totalElements: Long, val page: Int, val size: Int)
```

### 142위 [med · 확신 high] position 컨텍스트가 order 컨텍스트의 서비스 클래스를 직접 호출한다

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/position/AutoExitOrderPlacementService.kt:4` · 항목표 backend/layering · 항목 L8

```
import com.papertrading.api.application.order.AutoExitTriggerAuditInput  // ←
import com.papertrading.api.application.order.OrderCommandService  // ←
...
class AutoExitOrderPlacementService(
    private val orderCommandService: OrderCommandService,
```

- 원인: application.order.OrderCommandService 를 생성자로 받아 호출한다. port 도 result 도 아닌 B 컨텍스트 내부 서비스다. 같은 서비스가 application.order.AutoExitTriggerAuditInput 도 import 한다. order → position 방향 import 는 없어(application/order 아래에 application.position 참조 0건) 순환은 아니다.
- 생길 수 있는 일: order 서비스의 시그니처나 트랜잭션 의미가 바뀌면 position 쪽 자동 청산이 같이 깨지고, 컨텍스트 경계가 없어진다.
- 해결: order 컨텍스트가 application.order.port 에 자동 청산 매도 주문 포트(AutoExitSellOrderPlacer)를 제공하고 position 은 그것만 부른다.

### 143위 [med · 확신 high] updateCurrentPriceByTicker 가 Instant.now() 를 직접 읽는다

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/position/PositionCommandService.kt:31` · 항목표 backend/code-shape · 항목 F4

```
val normalizedTicker = ticker.trim().uppercase()
val now = Instant.now()  // ← Clock 주입이나 시각 인자가 아님
```

- 원인: application 함수가 Clock 주입 없이 현재 시각을 읽어 캐시 스냅샷의 priceUpdatedAt 으로 쓴다. 시세 자체의 시각(quoteAt)이 아니라 처리 시각이 들어간다. PositionCommandServiceTest 가 이 시각을 고정할 수 없다.
- 생길 수 있는 일: 스냅샷의 priceUpdatedAt 이 시세 도착 시각이 아니라 처리 시각이 되어, 지연된 시세가 최신처럼 보이고 시험이 시각 경계를 못 잡는다.
- 해결: 시세 시각을 인자로 받거나 Clock 을 생성자로 주입해 Instant.now(clock) 을 쓴다.

고친 모양(예시):
```
class PositionCommandService(..., private val clock: Clock) {
    val now = Instant.now(clock)
}
```

### 144위 [med · 확신 high] 허용 목록 밖 org.springframework.stereotype.Component 를 쓴다

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/position/PositionExitQuoteEventListener.kt:3` · 항목표 backend/layering · 항목 L2

```
import org.springframework.stereotype.Component  // ←
...
@Component
class PositionExitQuoteEventListener(
```

- 원인: application 에서 허용되는 stereotype 은 Service 뿐인데 @Component 를 쓴다. ArchUnit 동결 목록 1줄. 이 클래스는 로직 없이 Orchestrator.onQuote 로 위임만 한다.
- 생길 수 있는 일: 허용 import 목록이 조용히 넓어져 ArchUnit 이 지키려는 application 의 스프링 의존 경계가 흐려진다.
- 해결: @Service 로 바꾸거나, 위임만 하는 이 클래스를 없애고 진입점(infrastructure.redis.QuoteEventListener)이 유스케이스를 직접 부르게 한다.

### 145위 [med · 확신 high] 허용 목록 밖 org.springframework.stereotype.Component 를 쓴다

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/position/PositionExitTriggerEvaluator.kt:9` · 항목표 backend/layering · 항목 L2

```
import org.springframework.stereotype.Component  // ←
...
@Component
class PositionExitTriggerEvaluator {
```

- 원인: Evaluator 는 스프링 상태 없이 BigDecimal 비교만 하는 순수 도메인 서비스인데 @Component 로 application 에 있다. ArchUnit 동결 목록 1줄.
- 생길 수 있는 일: 순수 규칙이 스프링 컨테이너에 묶이고 허용 import 경계가 넓어진다.
- 해결: 스프링 어노테이션 없는 순수 Kotlin 클래스(도메인 서비스)로 두고 생성은 구성 클래스에서 한다.

### 146위 [med · 확신 high] 어댑터가 영속 예외를 바꾸지 않아 Spring DAO 예외가 application 에서 업무 분기로 쓰임

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/position/PositionExitTriggerOrchestrator.kt:13` · 항목표 data/transaction-boundary · 항목 X10

```
import org.springframework.dao.CannotAcquireLockException            // ← L13 application 이 Spring DAO 예외에 의존
import org.springframework.dao.DataIntegrityViolationException
...
} catch (ex: DataIntegrityViolationException) {                    // ← L155 중복 키 판정을 Spring 타입으로
```

- 원인: application 계층의 오케스트레이터가 org.springframework.dao.* 타입을 import 해 catch 하고(L45·L47·L155) DataIntegrityViolationException.getMessage() 를 호출한다(L167). ArchUnit 매니페스트(tools/T96587ad0.txt L306-309)가 동결 목록으로 같은 4건을 이미 가리킨다. 중복 키 판정과 락 충돌 판정이 JPA/Spring 세부에 묶여 있다.
- 생길 수 있는 일: 영속 기술(예외 계층)이 바뀌면 업무 분기가 조용히 깨진다. 동결 목록에 올라 있어 새 위반으로 잡히지 않고 계속 남는다.
- 해결: 어댑터(infrastructure)가 saveAndFlush 로 예외를 어댑터 안에서 일으켜 DuplicateOrderException·LockConflictException 같은 애플리케이션 예외로 바꾸고, 오케스트레이터는 그 타입만 잡는다.

고친 모양(예시):
```
class LockConflictException(cause: Throwable) : RuntimeException(cause)
// 어댑터: catch (e: PessimisticLockingFailureException) { throw LockConflictException(e) }
```

### 147위 [med · 확신 high] tryCreateGroupedAutoExitOrderWithRetry 본문이 48줄로 40줄을 넘는다

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/position/PositionExitTriggerOrchestrator.kt:130` · 항목표 backend/code-shape · 항목 S1

```
private fun tryCreateGroupedAutoExitOrderWithRetry(  // ← 본문 L136~L184, 48줄
    ...
    repeat(maxOrderCreateRetries) { attempt ->
        try {
            ...
        } catch (ex: DataIntegrityViolationException) {
            val existing = orderRepository.findByAccountIdAndOrderGroupId(accountId, orderGroupId)
            ...
        } catch (ex: Exception) {
            if (attempt == maxOrderCreateRetries - 1) {
```

- 원인: 본문 L136~L184 49줄(주석 1줄 제외 48줄)에 재시도 루프, 중복키 예외 뒤 기존 주문 존재 확인(멱등), 실패 알림 발행이 한 함수로 엮여 있다. 중첩은 repeat → try → if 로 3단이라 S2 는 한계선이다.
- 생길 수 있는 일: 재시도 정책과 멱등 확인과 알림이 한 함수에 있어, 재시도 횟수나 알림 조건을 바꾸다가 return true/false 경로를 하나 빠뜨리면 트리거 상태(TRIGGERED/FAILED)가 틀어진다.
- 해결: 한 번의 주문 시도(+중복키면 기존 주문 확인)를 attemptCreateOrder 로, 재시도 루프와 최종 실패 알림을 호출부로 나눈다.

고친 모양(예시):
```
private fun attemptCreateOrder(...): AttemptResult { /* 생성 시도 + 중복키 멱등 확인 */ }
repeat(maxOrderCreateRetries) { if (attemptCreateOrder(...) is AttemptResult.Done) return true }
publishOrderCreateFailure(...)
return false
```

### 148위 [med · 확신 high] 자동 청산 주문 재시도 중간 실패를 로그·기록 없이 삼킴

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/position/PositionExitTriggerOrchestrator.kt:170` · 항목표 reliability/failure-handling · 항목 E1

```
} catch (ex: Exception) {
    if (attempt == maxOrderCreateRetries - 1) {   // ← 마지막 시도가 아니면 본문이 없다: 로그·기록 없이 다음 반복
        publishOrderCreateFailure(
            ...
            reason = ex.message ?: ex::class.simpleName.orEmpty(),   // ← 마지막에도 예외 객체·스택은 버리고 문자열만 남김
        )
        return false
    }
}
```

- 원인: `catch (ex: Exception)` 가 마지막 시도가 아니면 본문이 비어 다음 반복으로 넘어간다. 이 파일에는 로거가 아예 없어(로그 grep 0건) 1·2회차 실패 원인(락 충돌·교착·검증 오류·버그)이 어디에도 남지 않는다. 3회차에 성공하면 앞선 실패는 흔적이 없다. 마지막 실패만 Slack 알림 문자열과 트리거 FAILED 상태로 남는다. detekt TooGenericExceptionCaught 기준선(manifest Tb661de0c)과 같은 지점.
- 생길 수 있는 일: 손절·익절 자동 청산 주문 생성이 간헐적으로 실패·재시도해도 운영자가 알 수 없고, 반복되는 락 경합·교착의 원인을 사후에 분석할 수 없다.
- 해결: 각 시도 실패를 계좌·포지션·orderGroupId·시도 번호와 함께 log.warn(ex) 로 남기고, 잡는 타입을 일시 오류로 좁힌다.

고친 모양(예시):
```
private val log = KotlinLogging.logger {}
...
} catch (ex: PessimisticLockingFailureException) {
    log.warn(ex) { "auto-exit order create retry: accountId=$accountId, orderGroupId=$orderGroupId, attempt=${attempt + 1}/$maxOrderCreateRetries" }
    if (attempt == maxOrderCreateRetries - 1) { publishOrderCreateFailure(...); return false }
}
```

### 149위 [med · 확신 high] 진입점이 아닌 재시도 루프 안의 넓은 catch(Exception)

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/position/PositionExitTriggerOrchestrator.kt:170` · 항목표 reliability/failure-handling · 항목 E3

```
} catch (ex: Exception) {   // ← 호출 한 개의 try 에 최상위 그물을 씀 (detekt TooGenericExceptionCaught)
    if (attempt == maxOrderCreateRetries - 1) {
```

- 원인: try 본문은 `createGroupedAutoExitSellOrder` 호출 하나이고 중복 키는 위 catch 에서 이미 분리했다. 남은 `Exception` 은 일시 오류가 아닌 검증 오류·NPE 같은 버그까지 모두 재시도 대상으로 묶는다. 최상위 진입점(전역 핸들러·스케줄러 틱) 밖이라 그물로 인정되지 않는다.
- 생길 수 있는 일: 프로그램 오류가 3번 재시도된 뒤 'Slack 알림 1건'으로만 보이고, 서로 다른 원인이 한 처리기에 가려진다.
- 해결: 재시도 대상을 일시 오류(PessimisticLockingFailureException·TransientDataAccessException 등)로 좁히고, 그 밖의 예외는 바로 실패 처리·로그한다.

고친 모양(예시):
```
} catch (ex: TransientDataAccessException) { ...재시도... }
// 그 밖의 예외는 잡지 않고 바깥에서 한 번에 처리
```

### 150위 [med · 확신 high] 최상위 common 예외가 Spring HttpStatus 를 품음 (ApiDomainException)

`backend/trading-api/src/main/kotlin/com/papertrading/api/common/exception/ApiDomainException.kt:3` · 항목표 backend/layering · 항목 L7

```
import org.springframework.http.HttpStatus      // ← common 이 Spring 을 import

open class ApiDomainException(
    val errorCode: String,
    val status: HttpStatus,                     // ← HTTP 상태를 도메인 예외가 보관
```

- 원인: common/** 가 org.springframework.http.HttpStatus 를 import 하고 status 속성으로 보관한다. PTB7 은 common 이 org.springframework 를 import 하지 않도록 정한다. domain 의 Account.kt:3-5 와 PositionExitTrigger.kt:3-5 가 이 예외 계열(BadRequestException·ConflictException·StaleTriggerVersionException·EntityMappingException)을 import 해 «domain 에 Spring 없음»이 우회로 깨진다. ArchUnit 동결 목록(tools/T96587ad0.txt 584-600줄)에 있다.
- 생길 수 있는 일: HTTP 의미가 common 을 거쳐 domain 에 새서 도메인 모델이 웹 계층 개념을 던지게 되고, 도메인을 Spring 없이 옮기거나 단위 시험하려면 HttpStatus 가 따라온다.
- 해결: 예외는 errorCode 와 의미만 가진 순수 타입(Spring 없음)으로 두고, 에러 코드→HttpStatus 매핑은 presentation 의 GlobalExceptionHandler 로 옮긴다.

고친 모양(예시):
```
open class ApiDomainException(val errorCode: String, message: String, cause: Throwable? = null) : RuntimeException(message, cause)
// presentation: errorCode/예외 종류 → HttpStatus 매핑 테이블
```

### 151위 [med · 확신 high] common 예외 묶음이 HttpStatus 를 직접 고정함 (CommonApiExceptions)

`backend/trading-api/src/main/kotlin/com/papertrading/api/common/exception/CommonApiExceptions.kt:3` · 항목표 backend/layering · 항목 L7

```
import org.springframework.http.HttpStatus   // ← common 이 Spring 을 import
...
    ApiDomainException("STALE_TRIGGER_VERSION", HttpStatus.CONFLICT, message)   // ← 구체 예외가 HTTP 상태를 직접 고정(124줄)
```

- 원인: org.springframework.http.HttpStatus 를 import 해 NotFound·Conflict·BadRequest·Unauthorized·Forbidden·BadGateway·GatewayTimeout 기본 예외와 StaleTriggerVersion·PositionNotEligible·SnapshotComputeFailed 등 구체 예외가 상태 코드를 직접 넣는다. ApiDomainException 과 같은 뿌리이며 domain 2파일이 이 파일의 예외를 쓴다(PTB7 본문 기록과 일치). ArchUnit 동결 목록(584-600줄)에 있다.
- 생길 수 있는 일: HTTP 상태가 예외 정의에 박혀 있어 도메인에서 던지는 순간 웹 의미가 domain 으로 새고, 상태 코드를 바꾸려면 도메인 쪽 예외를 건드려야 한다.
- 해결: ApiDomainException 정리와 함께 상태 매핑을 presentation 으로 옮긴다.

### 152위 [med · 확신 high] domain 이 HTTP 상태를 품은 common 예외를 import·throw 함

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/account/Account.kt:3` · 항목표 backend/layering · 항목 L7

```
import com.papertrading.api.common.exception.BadRequestException // ← HTTP 400 을 품은 예외
import com.papertrading.api.common.exception.ConflictException // ← HTTP 409
import com.papertrading.api.common.exception.EntityMappingException
            throw ConflictException( // ← domain 이 HTTP 의미 예외를 던짐
            throw BadRequestException(
```

- 원인: Account 가 BadRequestException·ConflictException·EntityMappingException 을 import 하고 던진다. 이 예외들은 ApiDomainException(HttpStatus.BAD_REQUEST·CONFLICT)을 상속한다(CommonApiExceptions.kt:5-9, 17-21, 108-109 를 grep 으로 읽음). PTB7 은 이 경로를 «domain 은 Spring 금지»를 우회하는 누수로 명시한다.
- 생길 수 있는 일: HTTP 의미가 common 을 거쳐 domain 에 새어, 도메인 규칙이 웹 계층 상태 코드에 묶인다.
- 해결: domain 은 HTTP 를 모르는 도메인 예외(예 InvalidExitTriggerException)를 던지고 전역 핸들러가 HTTP 로 변환한다.

고친 모양(예시):
```
class InvalidExitTriggerException(val code: String, message: String) : RuntimeException(message)
// presentation 의 전역 핸들러가 400 으로 변환
```

### 153위 [med · 확신 high] 청산 트리거 기본값의 불변식 검증이 모델이 아니라 Account 에 있음

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/account/AccountExitTriggerDefault.kt:36` · 항목표 backend/layering · 항목 D5

```
companion object {
    fun create( // ← 공개 팩토리에 require·검증이 없음
        accountId: Long,
        enabled: Boolean,
        stopLossPercent: BigDecimal?,
        takeProfitPercent: BigDecimal?,
    ): AccountExitTriggerDefault =
        AccountExitTriggerDefault().apply { this.accountId = accountId; /* 대입만 */ }
```

- 원인: percent 범위(0~100 미포함)·scale ≤ 4·enabled 면 한쪽은 필수라는 불변식이 있는데 AccountExitTriggerDefault.create 는 검증 없이 대입만 한다. 검증은 Account.validateExitTriggerDefault(Account.kt:305-332)에만 있다. create 는 공개라 Account 를 거치지 않는 호출이 가능하다(AccountTest 도 직접 호출).
- 생길 수 있는 일: Account 를 거치지 않는 호출자·매퍼·픽스처가 범위 밖 percent 나 enabled=true+둘 다 null 인 인스턴스를 만든다.
- 해결: 검증(validate*)을 AccountExitTriggerDefault 의 팩토리·변경 메서드로 옮기고 Account 는 소유자 id 확인만 맡는다.

고친 모양(예시):
```
fun create(accountId: Long, enabled: Boolean, stopLossPercent: BigDecimal?, takeProfitPercent: BigDecimal?): AccountExitTriggerDefault {
    validate(enabled, stopLossPercent, takeProfitPercent)
    return AccountExitTriggerDefault().apply { /* 대입 */ }
}
```

### 154위 [med · 확신 high] 알림이 계좌 애그리거트를 직접 참조한다

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/notification/Notification.kt:35` · 항목표 backend/layering · 항목 D4

```
@ManyToOne(fetch = FetchType.LAZY)   // ← 다른 애그리거트(Account) 루트를 직접 참조
@JoinColumn(name = "account_id")
var account: Account? = null
    private set
```

- 원인: Notification 이 @ManyToOne 으로 Account 루트를 들고 있다. PTB3·DD12 는 다른 애그리거트를 id 로만 참조하라고 한다. 게다가 유일한 생성처인 SlackNotificationEventHandler(L29–36)는 eventType·sourceRef·title·message 만 넘기고 이벤트의 accountId 를 버려서, account 는 항상 null 이고 연관만 남아 있다.
- 생길 수 있는 일: 알림을 읽을 때 Account 가 프록시로 딸려 오고, 연관이 쓰이지도 않으면서 accountId·refOrderId·refExecutionId 가 저장되지 않아 알림을 계좌·주문에 거슬러 추적할 수 없다.
- 해결: account 연관을 accountId: Long? 로 바꾸고, 핸들러가 이벤트의 accountId·orderId·executionId 를 생성 시 넘기게 한다.

고친 모양(예시):
```
var accountId: Long? = null
    private set
// create(…, accountId = event.accountId, refOrderId = event.orderId, refExecutionId = event.executionId)
```

### 155위 [med · 확신 high] 도메인 메서드가 시각을 직접 읽는다

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/notification/Notification.kt:97` · 항목표 backend/code-shape · 항목 F4

```
fun markDelivered(): Notification {
    status = DeliveryStatus.DELIVERED
    deliveredAt = Instant.now()   // ← 도메인 함수가 시각을 직접 읽음 (Clock·시각 인자 아님)
    return this
```

- 원인: markDelivered() 가 Instant.now() 를 본문에서 부른다. Clock 이나 시각 인자 주입이 없어 시험이 시각을 고정할 수 없고, 실제로 시험은 assertNotNull(deliveredAt) 로만 확인한다.
- 생길 수 있는 일: deliveredAt 의 값을 검증하는 시험을 쓸 수 없어 시각 관련 회귀(시간대·경계)가 숨는다.
- 해결: markDelivered 가 deliveredAt: Instant 를 인자로 받고, 호출하는 쪽(application)이 Clock 으로 시각을 만든다.

고친 모양(예시):
```
fun markDelivered(deliveredAt: Instant) {
    status = DeliveryStatus.DELIVERED
    this.deliveredAt = deliveredAt
}
```

### 156위 [med · 확신 high] Execution 이 Order·Account 두 루트를 객체로 참조

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/order/Execution.kt:38` · 항목표 backend/layering · 항목 D4

```
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    lateinit var order: Order                      // ← 다른 루트(Order) 객체 참조
...
    lateinit var account: Account                  // ← L45 다른 루트(Account) 객체 참조
```

- 원인: Execution 은 ExecutionRepository(findByExternalExecutionId)로 따로 다뤄지므로 Order 의 자식이 아니라 루트다. Order 와 Account 를 각각 @ManyToOne 으로 들고 있다.
- 생길 수 있는 일: 체결 한 건을 읽으면 주문·계좌가 지연 로딩으로 딸려 오고, 저장 시 세 애그리거트가 한 트랜잭션에서 얽힌다.
- 해결: orderId: Long·accountId: Long 으로 참조한다.

### 157위 [med · 확신 high] Order 가 다른 애그리거트 Account 를 객체로 참조 (@ManyToOne)

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/order/Order.kt:43` · 항목표 backend/layering · 항목 D4

```
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    lateinit var account: Account                  // ← 다른 애그리거트 루트를 객체로 참조
        private set
```

- 원인: Account 는 자기 리포지토리(AccountRepository)와 락 경로를 가진 루트다. 주문 애그리거트 안에서 Account 객체를 들고 있어 한 트랜잭션이 Order·Account 를 함께 바꾼다(ExecutionProcessor 가 실제로 그렇게 한다).
- 생길 수 있는 일: 주문을 읽으면 계좌가 딸려 오고, 한 트랜잭션이 불변식 둘(주문 전이·예수금 잠금)을 바꿔 락 순서·데드락 관리(ExecutionProcessor KDoc 의 «Account 락 먼저»)가 코드 밖 약속이 된다.
- 해결: accountId: Long 만 두고, 계좌 변경은 별도 유스케이스 단계에서 AccountRepository 로 한다.

고친 모양(예시):
```
class Order private constructor(/* … */ val accountId: Long /* , … */)
```

### 158위 [med · 확신 high] OrderSignal 이 Account 루트를 객체로 참조

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/order/OrderSignal.kt:40` · 항목표 backend/layering · 항목 D4

```
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    lateinit var account: Account                  // ← 다른 루트 객체 참조
        private set
```

- 원인: @ManyToOne 으로 Account 객체를 들고 있다(PTB3: 다른 애그리거트는 id 로만).
- 생길 수 있는 일: 신호 조회가 계좌를 끌고 오고 신호 처리와 계좌 변경이 한 트랜잭션으로 얽힐 수 있다.
- 해결: accountId: Long 으로 참조한다.

### 159위 [med · 확신 high] 다른 애그리거트(Account)를 @ManyToOne 객체 참조로 가짐

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/portfolio/DailyBalance.kt:31` · 항목표 backend/layering · 항목 D4

```
    @ManyToOne(fetch = FetchType.LAZY, optional = false)   // ← L31 다른 애그리거트를 객체로 참조
    @JoinColumn(name = "account_id", nullable = false)
    lateinit var account: Account   // ← L33
```

- 원인: PTB3 은 다른 애그리거트를 id 로만 참조하라고 한다. Account 는 자기 리포지토리·락(findByIdWithLock)으로 따로 다뤄지는 루트다.
- 생길 수 있는 일: DailyBalance 를 읽을 때 Account 가 지연 로딩으로 딸려 오고, 한 트랜잭션이 두 애그리거트를 건드리게 된다.
- 해결: account 대신 accountId: Long 을 가지고, 컬럼은 account_id 를 그대로 쓴다. 호출부(서비스)는 계좌를 따로 읽어 검증한다.

고친 모양(예시):
```
@Column(name = "account_id", nullable = false)
var accountId: Long = 0
    private set
```

### 160위 [med · 확신 high] 다른 애그리거트(Account)를 @ManyToOne 객체 참조로 가짐

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/portfolio/PortfolioSnapshot.kt:36` · 항목표 backend/layering · 항목 D4

```
    @ManyToOne(fetch = FetchType.LAZY, optional = false)   // ← L36
    @JoinColumn(name = "account_id", nullable = false)
    lateinit var account: Account   // ← L38
```

- 원인: DailyBalance D4 와 같다.
- 생길 수 있는 일: 스냅샷 조회 때 Account 가 딸려 오고 한 트랜잭션이 두 애그리거트를 건드린다.
- 해결: accountId: Long 으로 바꾼다.

### 161위 [med · 확신 high] 다른 애그리거트(Account)를 @ManyToOne 객체 참조로 가짐

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/portfolio/SnapshotJobRun.kt:27` · 항목표 backend/layering · 항목 D4

```
    @ManyToOne(fetch = FetchType.LAZY, optional = false)   // ← L27
    @JoinColumn(name = "account_id", nullable = false)
    lateinit var account: Account   // ← L29
```

- 원인: DailyBalance D4 와 같다.
- 생길 수 있는 일: 실행 기록 조회 때 Account 가 딸려 온다.
- 해결: accountId: Long 으로 바꾼다.

### 162위 [med · 확신 high] complete·fail·start 가 Instant.now() 를 직접 읽음

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/portfolio/SnapshotJobRun.kt:53` · 항목표 backend/code-shape · 항목 F4

```
    fun complete() {
        status = SnapshotJobRunStatus.SUCCESS
        finishedAt = Instant.now()   // ← L55 시각 직접 읽기
        finishedAt = Instant.now()   // ← L60 fail() 안
            this.startedAt = Instant.now()   // ← L69 start() 안
```

- 원인: domain 본문 3곳(L55·L60·L69)이 시각을 직접 읽고 Clock 이나 시각 인자 주입이 없다. startedAt·finishedAt 은 감사 필드(@CreatedDate)가 아니라 업무 시각이다.
- 생길 수 있는 일: 시험이 시작·종료 시각을 고정할 수 없어 소요 시간·자정 경계 검증이 불가능하고, 실행 시간 기반 로직이 생기면 시험이 깜빡인다.
- 해결: complete(at: Instant)·fail(message, at: Instant)·start(account, businessDate, at: Instant) 로 시각을 인자로 받고, 호출하는 유스케이스가 Clock 을 주입받아 넘긴다.

고친 모양(예시):
```
fun complete(finishedAt: Instant) {
    this.status = SnapshotJobRunStatus.SUCCESS
    this.finishedAt = finishedAt
}
```

### 163위 [med · 확신 high] 다른 애그리거트(Account)를 @ManyToOne 객체 참조로 가짐

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/portfolio/TaxSummary.kt:38` · 항목표 backend/layering · 항목 D4

```
    @ManyToOne(fetch = FetchType.LAZY, optional = false)   // ← L38
    @JoinColumn(name = "account_id", nullable = false)
    lateinit var account: Account   // ← L40
```

- 원인: DailyBalance D4 와 같다.
- 생길 수 있는 일: 세금 요약 조회 때 Account 가 딸려 온다.
- 해결: accountId: Long 으로 바꾼다.

### 164위 [med · 확신 high] computedAt 이 Instant.now() 직접 읽기(초기화식·recalculate·create 3곳)

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/portfolio/TaxSummary.kt:60` · 항목표 backend/code-shape · 항목 F4

```
    var computedAt: Instant = Instant.now()   // ← L60 초기화식에서 직접 읽음
        this.computedAt = Instant.now()   // ← L73 recalculate 안
            this.computedAt = Instant.now()   // ← L90 create 안
```

- 원인: computedAt 의 속성 초기화식(L60)과 recalculate(L73)·create(L90) 가 모두 Instant.now() 를 직접 읽는다. computedAt 은 업무 시각(계산 시각)이다.
- 생길 수 있는 일: 세금 계산 시각을 시험이 고정할 수 없어 연말 배치·재계산 경계(연도 전환) 시험이 불가능하다. 지금 TaxSummaryTest 가 computedAt 을 전혀 검증하지 않는 이유이기도 하다.
- 해결: create(…, computedAt: Instant)·recalculate(…, computedAt: Instant) 로 받고 유스케이스가 Clock 으로 만든 시각을 넘긴다. 초기화식은 lateinit 으로 바꿔 생성 시 반드시 채운다.

### 165위 [med · 확신 high] recalculate 는 예상세금 ≥ 0 을 강제하는데 create 에는 같은 검증이 없음

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/portfolio/TaxSummary.kt:78` · 항목표 backend/layering · 항목 D5

```
    fun recalculate(totalRealizedPnl: BigDecimal, taxablePnl: BigDecimal, estimatedTax: BigDecimal) {
        require(estimatedTax >= BigDecimal.ZERO) { "예상 세금은 0 이상이어야 합니다." }   // ← 재계산에는 있음
        fun create(
        ): TaxSummary = TaxSummary().apply {   // ← create 에는 같은 require 가 없음
```

- 원인: 불변식 «estimatedTax ≥ 0» 이 recalculate 에만 있고 create 에는 없다. TaxSummaryTest 에도 create 의 음수 케이스 시험이 없다. taxYear 범위 검증도 없다.
- 생길 수 있는 일: 음수 예상세금을 가진 TaxSummary 가 create 로 만들어져 저장될 수 있고, 이후 recalculate 에서야 예외가 난다. 서비스를 안 거치는 호출자·픽스처가 잘못된 인스턴스를 만든다.
- 해결: create 와 recalculate 가 같은 private 검증을 거치게 한다. TaxSummaryTest 에 create 음수 세금 예외 시험을 추가한다.

고친 모양(예시):
```
companion object {
    fun create(account: Account, taxYear: Int, totalRealizedPnl: BigDecimal, taxablePnl: BigDecimal, estimatedTax: BigDecimal): TaxSummary {
        require(estimatedTax >= BigDecimal.ZERO) { "예상 세금은 0 이상이어야 합니다." }
        return TaxSummary().apply { /* … */ }
    }
}
```

### 166위 [med · 확신 high] startedAt·finishedAt 이 Instant.now() 직접 읽기(4곳)

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/portfolio/TaxSummaryRun.kt:48` · 항목표 backend/code-shape · 항목 F4

```
    var startedAt: Instant = Instant.now()   // ← L48 초기화식
        finishedAt = Instant.now()   // ← L61 completeSuccess
        finishedAt = Instant.now()   // ← L67 fail
                this.startedAt = Instant.now()   // ← L78 start (초기화식과 중복)
```

- 원인: startedAt 초기화식(L48), completeSuccess(L61), fail(L67), start(L78)가 시각을 직접 읽는다. start() 는 초기화식과 같은 값을 한 번 더 대입한다.
- 생길 수 있는 일: 실행 시각을 시험이 고정하지 못해 연도 경계·소요 시간 시험이 불가능하고, 초기화식과 start() 두 곳이 같은 값을 다른 시각으로 읽는다.
- 해결: 시각을 인자로 받고(Clock 은 유스케이스가 보유) 초기화식은 제거한다. SnapshotJobRun F4 와 같은 수정.

### 167위 [med · 확신 high] 다른 애그리거트(Account)를 @ManyToOne 객체 참조로 가짐 — orderId 는 id 로 참조하는데 account 만 객체

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/portfolio/TradingJournal.kt:28` · 항목표 backend/layering · 항목 D4

```
    @ManyToOne(fetch = FetchType.LAZY, optional = false)   // ← L28 Account 는 객체 참조
    lateinit var account: Account
    var orderId: Long? = null   // ← L34 주문은 id 참조(기준이 섞임)
    fun belongsTo(accountId: Long): Boolean = account.id == accountId
```

- 원인: 같은 클래스에서 주문은 orderId: Long 으로 참조(L33~L35)하면서 계좌는 객체로 참조해 기준이 섞였다. belongsTo 도 account.id 로 지연 로딩 프록시를 건드린다.
- 생길 수 있는 일: 일지 한 건을 읽을 때 Account 가 딸려 오고, belongsTo 가 프록시 초기화를 일으킬 수 있다.
- 해결: accountId: Long 으로 바꾸면 belongsTo 도 단순 비교가 된다.

### 168위 [med · 확신 high] Position 이 다른 애그리거트 루트 Account 를 @ManyToOne 으로 직접 든다

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/position/Position.kt:46` · 항목표 backend/layering · 항목 D4

```
@ManyToOne(fetch = FetchType.LAZY, optional = false)  // ← 다른 애그리거트 루트를 객체로 참조
@JoinColumn(name = "account_id", nullable = false)
lateinit var account: Account
    private set
```

- 원인: Position 과 Account 는 각각 리포지토리를 가진 별개 루트인데 Position 이 account: Account 를 지연 로딩 연관으로 갖는다. 같은 컨텍스트의 PositionExitTrigger 는 positionId·accountId 로만 참조해 대조된다. LivePositionSnapshot.fromPosition 이 position.account.tradingMode 를 읽느라 이 연관이 쓰인다.
- 생길 수 있는 일: Position 을 읽으면 Account 프록시가 딸려 오고, 락 순서 주석(Account 락 후 Position 락)처럼 한 트랜잭션이 두 애그리거트의 불변식을 함께 다루게 된다.
- 해결: accountId: Long 으로만 참조하고, tradingMode 는 필요한 쪽이 Account 조회로 얻거나 포지션 생성 시 값으로 가진다.

### 169위 [med · 확신 high] Position.updatePrice 가 Instant.now() 를 직접 읽는다

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/position/Position.kt:195` · 항목표 backend/code-shape · 항목 F4

```
priceUpdatedAt = Instant.now()  // ← 시각 직접 읽기(Clock·인자 아님)
```

- 원인: domain 함수 본문이 시각을 직접 읽어 시험이 고정할 수 없다. 같은 일을 하는 LivePositionSnapshot.evaluate 는 updatedAt 을 인자로 받아 일관되지 않다. PositionTest 는 priceUpdatedAt 을 assertNotNull 로만 확인한다(L127).
- 생길 수 있는 일: 시세 갱신 시각을 검증하는 시험이 불가능하고, 시세 시각과 처리 시각이 섞여도 눈에 띄지 않는다.
- 해결: updatePrice(price, source, updatedAt: Instant) 로 시각을 인자로 받고, 호출부(createWithHolding 포함)가 시각을 넘긴다.

고친 모양(예시):
```
fun updatePrice(price: BigDecimal, source: PriceSource, updatedAt: Instant) {
    ...
    priceUpdatedAt = updatedAt
}
```

### 170위 [med · 확신 high] 도메인이 HTTP 상태를 품은 common 예외를 직접 던진다

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/position/PositionExitTrigger.kt:3` · 항목표 backend/layering · 항목 L7

```
import com.papertrading.api.common.exception.BadRequestException  // ←
import com.papertrading.api.common.exception.ConflictException
...
    throw BadRequestException("EXIT_TRIGGER_CONDITION_REQUIRED", "triggerPercent or triggerPrice is required")
// CommonApiExceptions L5-9
open class BadRequestException(...) : ApiDomainException(errorCode, HttpStatus.BAD_REQUEST, message, cause)
```

- 원인: domain 파일이 BadRequestException·ConflictException·StaleTriggerVersionException 을 import 해 던진다. 이 예외들은 ApiDomainException 을 상속하며 org.springframework.http.HttpStatus 를 필드로 가진다(ApiDomainException L3·L7, CommonApiExceptions L5~L9). 그래서 «domain 에 Spring 없음»이 common 을 거쳐 우회로 깨진다.
- 생길 수 있는 일: 도메인 규칙 위반이 곧 HTTP 400·409 의미를 갖고, 도메인 단위 시험이 HTTP 모델에 묶이며 domain 순수화 때 되돌려야 할 의존이 하나 더 남는다.
- 해결: 도메인은 IllegalArgumentException 이나 도메인 전용 예외(InvalidExitTriggerPolicyException)를 던지고, HTTP 상태 변환은 전역 예외 핸들러(presentation)가 한다.

### 171위 [med · 확신 high] Settlement 가 Order·Account 두 루트를 객체로 참조

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/settlement/Settlement.kt:38` · 항목표 backend/layering · 항목 D4

```
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    lateinit var order: Order                      // ← 다른 루트 객체 참조
...
    lateinit var account: Account                  // ← L45
```

- 원인: @OneToOne Order, @ManyToOne Account 로 다른 루트를 객체로 들고 있다. 정산은 자기 리포지토리(SettlementRepository)가 있는 루트다.
- 생길 수 있는 일: 정산 저장이 주문·계좌 애그리거트와 같은 트랜잭션·같은 영속성 컨텍스트에 얽힌다.
- 해결: orderId: Long·accountId: Long 으로 참조한다.

### 172위 [med · 확신 high] Strategy 가 Account 애그리거트를 객체로 직접 참조

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/strategy/Strategy.kt:36` · 항목표 backend/layering · 항목 D4

```
@ManyToOne(fetch = FetchType.LAZY, optional = false)
@JoinColumn(name = "account_id", nullable = false)
lateinit var account: Account   // ← 다른 애그리거트 루트를 객체로 참조
```

- 원인: @ManyToOne 으로 Account(다른 애그리거트 루트)를 속성 타입으로 들고 있다. PTB3·DD12 는 다른 애그리거트를 id 로만 참조하라고 한다. 이웃 OrderSignal 은 strategyId: Long? 로 id 만 쓰는데(tools/Tb661de0c.txt:175) 여기만 객체 참조다.
- 생길 수 있는 일: Strategy 를 읽거나 저장할 때 Account 가 딸려 오고 한 트랜잭션이 두 애그리거트를 바꾼다.
- 해결: accountId: Long 만 들고, 필요할 때 유스케이스가 Account 를 조회한다.

고친 모양(예시):
```
val accountId: Long
```

### 173위 [med · 확신 high] 도메인 함수 본문이 Instant.now() 를 직접 읽음 (cancel·markExecuted)

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/strategy/Strategy.kt:150` · 항목표 backend/code-shape · 항목 F4

```
fun cancel(reason: String, cancelledAt: Instant = Instant.now()) {   // ← 기본 인자가 시각을 직접 읽음
    ...
    require(!cancelledAt.isAfter(Instant.now().plusSeconds(5))) {   // ← 본문이 Clock 없이 현재 시각을 직접 읽음 (L167 markExecuted 도 동일)
        "cancelledAt은 미래 시각일 수 없습니다."
    }
```

- 원인: cancel 과 markExecuted 가 기본 인자와 «미래 시각 금지» 검증 본문에서 Instant.now() 를 직접 읽는다. 시각 인자는 받지만 검증 기준 시각(now + 5초)은 시험이 고정할 수 없다.
- 생길 수 있는 일: 5초 허용 경계(경계값 ±1초)를 시험으로 고정할 수 없어 실제로 이 분기들이 시험되지 않는다(시험에는 미래 시각 케이스가 없음). 시각 경계 버그가 숨는다.
- 해결: 검증 기준 시각을 호출자(유스케이스)가 Clock 으로 만들어 인자로 넘긴다. 엔티티는 Instant.now() 를 부르지 않는다.

고친 모양(예시):
```
fun cancel(reason: String, cancelledAt: Instant, now: Instant) {
    require(!cancelledAt.isAfter(now.plusSeconds(CLOCK_SKEW_SECONDS))) { "cancelledAt은 미래 시각일 수 없습니다." }
    ...
}
```

### 174위 [med · 확신 high] StrategyDerivation 이 Strategy 루트 둘을 객체로 참조

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/strategy/StrategyDerivation.kt:26` · 항목표 backend/layering · 항목 D4

```
@ManyToOne(fetch = FetchType.LAZY, optional = false)
@JoinColumn(name = "parent_strategy_id", nullable = false)
lateinit var parentStrategy: Strategy   // ← 다른 루트 객체 참조
...
lateinit var childStrategy: Strategy   // ← 같은 문제 (L33)
```

- 원인: parentStrategy 와 childStrategy 가 모두 Strategy(자기 리포지토리 StrategyRepository 를 가진 루트) 타입 @ManyToOne 이다.
- 생길 수 있는 일: 계보 한 건을 읽으면 두 전략이 딸려 오고, 두 애그리거트 상태가 한 객체 그래프에 묶인다.
- 해결: parentStrategyId·childStrategyId(Long)로 바꾼다. 같은 전략 금지 검증은 id 비교로 한다.

고친 모양(예시):
```
val parentStrategyId: Long
val childStrategyId: Long
```

### 175위 [med · 확신 high] StrategyLog 가 Strategy 와 Account 루트를 객체로 참조

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/strategy/StrategyLog.kt:30` · 항목표 backend/layering · 항목 D4

```
@ManyToOne(fetch = FetchType.LAZY, optional = false)
@JoinColumn(name = "strategy_id", nullable = false)
lateinit var strategy: Strategy   // ← 다른 루트 객체 참조
...
lateinit var account: Account   // ← 같은 문제 (L37)
```

- 원인: strategy: Strategy 와 account: Account 두 @ManyToOne 이 있다. StrategyLog 는 자기 리포지토리(StrategyLogRepository)로 다뤄지므로 루트이고 두 참조 모두 애그리거트 경계를 넘는다.
- 생길 수 있는 일: 로그 조회 때 전략·계좌가 딸려 오고(지연 로딩 N+1 위험), 로그 기록 한 번이 두 애그리거트를 잠글 수 있다.
- 해결: strategyId·accountId(Long)로 바꾼다.

고친 모양(예시):
```
val strategyId: Long
val accountId: Long
```

### 176위 [med · 확신 high] StrategyLog 가 속성 초기값·기본 인자·검증 본문에서 Instant.now() 를 직접 읽음

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/strategy/StrategyLog.kt:54` · 항목표 backend/code-shape · 항목 F4

```
var loggedAt: Instant = Instant.now()   // ← 속성 초기값이 시각을 직접 읽음
...
loggedAt: Instant = Instant.now()   // ← 기본 인자
...
require(!loggedAt.isAfter(Instant.now().plusSeconds(5))) {   // ← 검증 본문
```

- 원인: loggedAt 속성 초기값(L54), create 의 기본 인자(L70), 미래 시각 검증 본문(L77) 세 곳이 Clock 없이 Instant.now() 를 읽는다.
- 생길 수 있는 일: 로그 시각 경계(5초 허용)를 시험이 고정할 수 없고 시험은 Instant.now().plusSeconds(60) 같은 넉넉한 값으로만 우회한다.
- 해결: loggedAt 과 기준 시각을 호출자가 Clock 으로 만들어 필수 인자로 넘긴다.

고친 모양(예시):
```
fun create(strategy: Strategy, account: Account, level: LogLevel, message: String, loggedAt: Instant, now: Instant): StrategyLog
```

### 177위 [med · 확신 high] 출력 포트가 application/<ctx>/port 가 아니라 domain/port 에 있음

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/port/CollectorSubscriptionPort.kt:1` · 항목표 backend/layering · 항목 L3

```
package com.papertrading.api.domain.port // ← 출력 포트가 application/<ctx>/port 가 아닌 domain/port 에 있음

interface CollectorSubscriptionPort {
    fun subscribe(mode: String, ticker: String)
```

- 원인: PTB8 은 출력 포트를 application/<ctx>/port(공유는 application/common/port)에 두라고 한다. CollectorSubscriptionPort 는 collector-api HTTP 호출을 가리는 출력 포트인데 domain/port 에 있다. ArchUnit 동결 목록(tools/T96587ad0.txt 270행)에도 '패키지 com.papertrading.api.application.*.port 에 있지 않음' 으로 올라 있다. 동결은 해결이 아니다.
- 생길 수 있는 일: 포트 자리가 domain/port 와 application/*/port 두 곳으로 갈려 새 포트가 어디에 생길지 사람마다 달라진다. 주문·체결 유스케이스(OrderCommandService·ExecutionProcessor)가 domain 패키지의 포트에 계속 의존하고 동결 목록은 줄지 않는다.
- 해결: application/order/port(여러 컨텍스트가 쓰면 application/common/port)로 옮기고 import 를 갱신한다. 이름과 시그니처는 그대로 둔다.

고친 모양(예시):
```
package com.papertrading.api.application.common.port

interface CollectorSubscriptionPort {
    fun subscribe(mode: TradingMode, ticker: String)
    fun unsubscribe(mode: TradingMode, ticker: String)
}
```

### 178위 [med · 확신 high] 출력 포트가 domain.port 에 있다(application.position.port 가 아님)

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/port/LivePositionCachePort.kt:1` · 항목표 backend/layering · 항목 L3

```
package com.papertrading.api.domain.port  // ← application.position.port 가 아님

import com.papertrading.api.domain.enums.TradingMode

interface LivePositionCachePort {
```

- 원인: infrastructure.redis.RedisLivePositionCacheAdapter 가 구현하는 Redis 캐시 포트가 domain/port 아래 있다. PTB8 은 application/<ctx>/port 를 요구한다. 포트의 요청·결과 타입 LivePositionSnapshot 도 같은 domain.port 에 있어 같이 옮겨야 한다(ArchUnit 동결 목록 1줄).
- 생길 수 있는 일: 포트 자리가 domain.port 와 application.*.port 둘로 갈라지고 infrastructure 가 domain.port 를 import 하는 방향이 굳는다.
- 해결: LivePositionCachePort 와 LivePositionSnapshot 을 application.position.port 로 옮긴다.

### 179위 [med · 확신 high] 출력 포트가 domain/port 에 있다

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/port/NotificationSender.kt:1` · 항목표 backend/layering · 항목 L3

```
 package com.papertrading.api.domain.port   // ← 출력 포트가 domain/port 에 있음 (application/<ctx>/port 여야 함)

fun interface NotificationSender {
    fun send(message: String): Boolean
}
```

- 원인: NotificationSender 는 Slack 웹훅을 감싸는 출력 포트인데 domain.port 에 있다. PTB8 은 출력 포트를 application/<ctx>/port 에 두라고 한다. 사용처가 SlackNotificationService·핸들러·스케줄러·SlackWebhookNotifier 로 전부 알림 컨텍스트뿐이다. ArchUnit 동결 목록에도 있다. 파일 1행 맨 앞에 공백이 하나 있다(ktlint indent).
- 생길 수 있는 일: 포트 자리가 domain.port 와 application.*.port 둘로 흩어져 infrastructure 가 포트를 찾는 규칙(PTB7)이 한 곳을 놓친다.
- 해결: application/notification/port/NotificationSender.kt 로 옮기고 import 4곳을 고친다. 맨 앞 공백도 지운다.

고친 모양(예시):
```
package com.papertrading.api.application.notification.port

fun interface NotificationSender {
    fun send(message: String): Boolean
}
```

### 180위 [med · 확신 high] KIS 응답의 금액 필드 파싱 실패가 조용히 0 이 됨

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/kis/KisAccountBalanceAdapter.kt:121` · 항목표 reliability/failure-handling · 항목 E1

```
private fun outputMap(response: Map<*, *>?, key: String): Map<*, *> =
    (response?.get(key) as? List<*>)?.firstOrNull() as? Map<*, *> ?: emptyMap<String, Any>()  // ← 응답 구조 이상을 빈 맵으로 덮음

private fun decimal(map: Map<*, *>, key: String): BigDecimal =
    map[key]?.toString()?.replace(",", "")?.toBigDecimalOrNull() ?: BigDecimal.ZERO  // ← 파싱 실패를 0 으로
```

- 원인: decimal() 이 값이 없거나 숫자가 아니면 BigDecimal.ZERO 를 돌려주고, outputMap 은 output2 가 없으면 emptyMap 을 돌려준다. rt_cd=0 인데 응답 스키마가 바뀌거나 필드가 비면 예수금·평가금액이 오류 없이 0 으로 나오고 보유 종목도 비어 정상 응답처럼 반환된다.
- 생길 수 있는 일: 잔고 대사(missingInKis 등)가 전 종목 불일치로 보이거나, 반대로 0 원 잔고가 정상값으로 노출된다. 금액에 대한 침묵 기본값이라 잘못된 판단으로 이어진다.
- 해결: 필수 금액 필드(dnca_tot_amt·scts_evlu_amt 등)가 없거나 숫자가 아니면 KisResponseParseException 을 던진다. 선택 필드만 기본값을 허용한다.

고친 모양(예시):
```
private fun requiredDecimal(map: Map<*, *>, key: String): BigDecimal =
    map[key]?.toString()?.replace(",", "")?.toBigDecimalOrNull()
        ?: throw KisResponseParseException("missing numeric field: $key")
```

### 181위 [med · 확신 high] 재연결에 지터가 없고 5회 소진 뒤 영구 포기

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/kis/KisExecutionWebSocketManager.kt:202` · 항목표 reliability/load-concurrency · 항목 C4

```
if (nextAttempt > properties.wsReconnectMaxAttempts) {
    statuses[mode] = KisWebSocketConnectionStatus.DISCONNECTED
    log.error { "KIS WebSocket reconnect attempts exhausted: mode=$mode, attempts=${properties.wsReconnectMaxAttempts}" }   // ← 로그만 남기고 영구 포기
    return
}
...
delay = (delay * 2).coerceAtMost(properties.wsReconnectMaxBackoffMillis)   // ← 지터 없음
```

- 원인: 백오프는 1초부터 두 배씩(최대 30초)이며 무작위 지터가 없다(LA24). 시도가 5회(약 31초분)를 넘으면 DISCONNECTED 로 두고 ERROR 로그만 남긴 채 다시 시도하는 경로가 없다. handleConnected 가 시도 횟수를 0 으로 되돌려도, 포기한 뒤에는 재시작 전까지 호출되지 않는다.
- 생길 수 있는 일: KIS·네트워크 장애가 1분만 넘어가도 장중 체결 통지 수신이 재시작 전까지 끊긴다. 여러 인스턴스가 같은 시각에 재접속하면 KIS 제한을 칠 수 있다.
- 해결: 소진 뒤에도 최대 백오프 간격으로 계속 재시도하고(지터 포함), 소진 이벤트는 경보로 연결한다. I6 의 조회 대조가 단절 구간을 메우게 한다.

고친 모양(예시):
```
val jitter = ThreadLocalRandom.current().nextLong(0, delay / 2 + 1)
return (delay + jitter).coerceAtMost(maxBackoff)   // 소진 시에도 maxBackoff 간격으로 재시도
```

### 182위 [med · 확신 high] 필수 KIS 자격 증명이 빈 문자열 기본값이고 시작 시 검증이 없음

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/kis/KisOrderProperties.kt:13` · 항목표 reliability/security-config-hygiene · 항목 S4

```
var paperAppKey: String = ""   // ← 필수 시크릿이 빈 기본값, 시작 시 검증 없음
var paperAppSecret: String = ""
var liveAppKey: String = ""
var liveAppSecret: String = ""
```

- 원인: paperAppKey·paperAppSecret·liveAppKey·liveAppSecret 이 "" 기본값의 var 이며 @Validated·@NotBlank 가 없다. application.yaml 도 ${KIS_PAPER_APP_KEY:} 처럼 빈 기본값이다. 값이 빠져도 기동은 성공하고 첫 KIS 호출에서야 빈 appkey 로 호출이 실패한다.
- 생길 수 있는 일: 키가 빠진 배포가 정상 기동으로 보이다가 장중 첫 주문에서 터진다. 실전 모드면 그 시점이 이미 주문 요청 시점이다.
- 해결: @Validated 와 lateinit·@NotBlank 로 모드별 필수 값을 시작 시 검증한다. 모드를 쓰지 않는 환경은 조건부 검증(예: 활성 모드만)으로 처리한다.

고친 모양(예시):
```
@Validated
@ConfigurationProperties(prefix = "trading.kis")
class KisOrderProperties { @field:NotBlank lateinit var paperAppKey: String /* ... */ }
```

### 183위 [med · 확신 high] BigDecimal 을 == 로 비교(equals, scale 민감)

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/kis/KisOrderRestClient.kt:133` · 항목표 backend/code-shape · 항목 F1

```
isFullyFilled = (item["PSBL_QTY"]?.toString()?.toBigDecimalOrNull() ?: BigDecimal.ONE) == BigDecimal.ZERO, // ← == 는 equals — scale 이 다르면 false
```

- 원인: Kotlin 의 == 는 equals 를 부르므로 BigDecimal("0.00") == BigDecimal.ZERO 는 false 다. KIS 응답 수량 문자열의 scale 에 따라 완전 체결 판정이 조용히 틀린다(PTB4). inquireFills 는 현재 호출처가 없어(main·test grep 0건) 잠복 결함이다.
- 생길 수 있는 일: 이 함수를 쓰기 시작하면 PSBL_QTY 가 "0.00" 꼴로 올 때 완전 체결이 부분 체결로 판정되어 체결 확인·재조회가 어긋난다.
- 해결: compareTo 로 비교한다.

고친 모양(예시):
```
isFullyFilled = (item["PSBL_QTY"]?.toString()?.toBigDecimalOrNull() ?: BigDecimal.ONE).compareTo(BigDecimal.ZERO) == 0,
```

### 184위 [med · 확신 high] infrastructure 가 port 밖 application 클래스를 import 한다

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/notification/SlackNotificationEventHandler.kt:4` · 항목표 backend/layering · 항목 L4

```
import com.papertrading.api.application.notification.SlackNotificationRequestedEvent   // ← port 밖 application 패키지를 infrastructure 가 import
```

- 원인: 핸들러가 application.notification.SlackNotificationRequestedEvent 를 import 한다. PTB7 은 infrastructure 가 application.<ctx>.port 만 보게 한다. ArchUnit 동결 목록(2건)에도 있다. 서비스 호출이 아니라 이벤트 타입이라 심각도는 낮춘다.
- 생길 수 있는 일: 어댑터가 application 패키지 내부 타입에 묶여 이벤트 모양을 바꾸면 infrastructure 도 같이 고쳐야 하고, 의존 방향 규칙의 예외가 늘어난다.
- 해결: 이벤트 타입을 domain.event 나 application/notification/port 로 옮겨 PTB7 허용 방향 안에서 쓰게 한다. 어느 쪽으로 둘지는 사람이 정한다.

고친 모양(예시):
```
package com.papertrading.api.application.notification.port

data class SlackNotificationRequestedEvent(/* … */)
```

### 185위 [med · 확신 high] 스케줄러 진입점이 infrastructure 에 있고 유스케이스 없이 재전송을 몬다

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/notification/SlackNotificationScheduler.kt:17` · 항목표 backend/layering · 항목 L6

```
@Scheduled(fixedDelay = 30_000)   // ← 진입점이 infrastructure 에 있음
fun retryPending() {
    val policy = policyStore.get()
    if (!policy.enabled) return
    val pendingNotifications = txService.findPendingForRetry(policy.maxRetries)
    // …
            val success = sender.send(claimedNotification.message)   // ← 유스케이스 없이 흐름 전체를 스케줄러가 쥠
```

- 원인: @Scheduled 메서드가 infrastructure 에 있다. PTB7 은 스케줄러를 presentation 에 두고 유스케이스만 부르게 한다. 이 스케줄러는 부를 유스케이스 없이 정책 확인·대기 건 조회·선점·전송·결과 기록 전부를 직접 쥔다.
- 생길 수 있는 일: 재전송 규칙(선점 실패 스킵, 실패 시 markFailed)이 시험하기 어려운 어댑터에 갇혀, 규칙을 바꿀 때 스케줄 설정과 업무 로직이 같이 움직인다.
- 해결: 재전송 흐름을 application 유스케이스(retryPending)로 옮기고, 스케줄러는 presentation 에서 그것만 부른다.

고친 모양(예시):
```
// presentation/scheduler
@Component
class SlackNotificationRetryScheduler(private val retryService: SlackNotificationRetryService) {
    @Scheduled(fixedDelay = 30_000)
    fun run() = retryService.retryPending()
}
```

### 186위 [med · 확신 high] 계속 자라는 원장 목록이 OFFSET 페이징이고 페이지 상한이 없음

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/AccountLedgerRepositoryImpl.kt:37` · 항목표 data/query · 항목 Q6

```
// AccountLedgerRepositoryImpl.kt:37-39
.orderBy(ledger.createdAt.desc())
.offset(filter.pageable.offset)   // ← 깊어지는 원장 목록에 OFFSET
.limit(filter.pageable.pageSize.toLong())
```

- 원인: findLedgers 는 createdAt desc 정렬에 offset/limit 을 쓰고, 컨트롤러는 size 만 1..100 으로 제한하며 page 에는 상한이 없다(AccountController.kt:136,143). 원장은 입출금·잠금·체결마다 늘어난다. 뒤 페이지일수록 버려지는 행까지 읽는다.
- 생길 수 있는 일: 원장이 커지면 깊은 페이지 요청이 느려지고, 큰 page 값 하나로 DB 를 오래 붙잡을 수 있다.
- 해결: 키셋 페이징(WHERE (createdAt, id) < (:cursor, :id))으로 바꾸거나, 최소한 page 최댓값을 검증한다.

고친 모양(예시):
```
.where(where, ledger.createdAt.lt(cursorAt).or(ledger.createdAt.eq(cursorAt).and(ledger.id.lt(cursorId))))
.orderBy(ledger.createdAt.desc(), ledger.id.desc())
.limit(size.toLong())
```

### 187위 [med · 확신 high] 원장 기간 조회의 상한이 배타 값으로 넘어오는데 저장소는 <= 로 비교

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/AccountLedgerRepositoryImpl.kt:54` · 항목표 data/query · 항목 Q2

```
// AccountLedgerRepositoryImpl.kt:54-55
.and(filter.from?.let { ledger.createdAt.goe(it) })
.and(filter.to?.let { ledger.createdAt.loe(it) })   // ← 상한이 포함(<=)
// AccountController.kt:142
to = to?.plusDays(1)?.atStartOfDay(ZoneOffset.UTC)?.toInstant(),   // ← 컨트롤러는 배타 상한(다음 날 00:00)을 넘김
```

- 원인: 컨트롤러는 to 를 «다음 날 00:00 UTC»(배타 상한)로 바꿔 넘긴다. 저장소 buildWhere 는 그 값을 loe(<=)로 비교해 정확히 그 순간의 행을 포함한다. 레포 규칙 PTF5 는 연속 시각 구간을 반열림 [start, end) 로 요구한다. 기본 심각도는 high 이나, 마이크로초 단위 Instant 가 정각에 겹칠 확률이 낮아 med 로 낮췄다.
- 생길 수 있는 일: 자정 정각에 기록된 원장 행(일괄·정산성 기록)이 두 날짜 조회에 모두 잡혀 일별 합계가 중복된다.
- 해결: 상한 비교를 lt 로 바꿔 [from, to) 반열림으로 맞춘다.

고친 모양(예시):
```
.and(filter.to?.let { ledger.createdAt.lt(it) })
```

### 188위 [med · 확신 high] infrastructure 가 port 밖 application 패키지를 import (ArchUnit 동결 1건)

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/ExecutionRepositoryCustom.kt:3` · 항목표 backend/layering · 항목 L4

```
import com.papertrading.api.application.order.query.ExecutionQuery   // ← infrastructure → application(port 밖) import
```

- 원인: ExecutionQuery 가 application.order.query 에 있어 어댑터 쪽 인터페이스가 안쪽 application 의 조회 모델에 묶인다(PTB7). 유스케이스를 부르지는 않고 값 객체만 import 해 기본 high 에서 med 로 낮춤. ArchUnit 출력 T96587ad0.txt 224-225행.
- 생길 수 있는 일: 의존 방향이 안쪽으로만 가지 않아 조회 모델 필드 변경이 영속 어댑터를 깨뜨리고, 포트 이전 때 순환이 생길 수 있다.
- 해결: ExecutionQuery 를 application.order.port 로 옮기거나 어댑터가 원시 인자(accountId·orderId·구간)를 받게 한다.

### 189위 [med · 확신 high] infrastructure 가 port 밖 application 패키지를 import (ArchUnit 동결 1건)

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/ExecutionRepositoryImpl.kt:3` · 항목표 backend/layering · 항목 L4

```
import com.papertrading.api.application.order.query.ExecutionQuery   // ← infrastructure → application(port 밖) import
```

- 원인: 어댑터 구현이 application.order.query.ExecutionQuery 를 import 해 searchExecutions 를 구현한다(PTB7). 값 객체만이라 med. T96587ad0.txt 226-227행.
- 생길 수 있는 일: 어댑터가 application 조회 모델에 묶여 안쪽 변경이 바깥을 깨뜨린다.
- 해결: ExecutionQuery 를 port 패키지로 옮겨 이 어댑터가 포트 타입만 import 하게 한다.

### 190위 [med · 확신 high] 체결 목록 조회의 끝 경계가 포함(<=)이라 반열림 [start, end) 규칙을 어김

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/ExecutionRepositoryImpl.kt:20` · 항목표 data/query · 항목 Q2

```
query.executedFrom?.let { execution.executedAt.goe(it) },
query.executedTo?.let { execution.executedAt.loe(it) },   // ← 끝 경계를 <= 로 포함 (PTF5 위반)
```

- 원인: executedTo 를 `loe`(<=)로 거릅니다. PTF5 는 연속 시각(Instant) 구간을 `>= start AND < end` 로 정합니다. OrderQueryService.validateExecutionRange(:50-54)도 «같거나 이전»을 허용해 닫힌 구간 의미를 굳힙니다.
- 생길 수 있는 일: executedTo 와 정확히 같은 시각의 체결이 [a,b] 와 [b,c] 두 조회에 모두 잡혀 이어 붙이는 호출자가 중복 집계합니다. 화면 목록 조회라 영향 폭은 제한적이어서 high 대신 med 로 낮춰 보고합니다.
- 해결: executedTo 를 배타 상한(`lt`)으로 바꾸고, 검증은 from < to 로 맞추며, API 문서에 [from, to) 를 명시합니다.

고친 모양(예시):
```
query.executedTo?.let { execution.executedAt.lt(it) },
```

### 191위 [med · 확신 high] infrastructure 가 port 밖 application 패키지를 import (ArchUnit 동결 1건)

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/OrderRepositoryCustom.kt:3` · 항목표 backend/layering · 항목 L4

```
import com.papertrading.api.application.order.query.OrderListQuery   // ← infrastructure → application(port 밖) import
```

- 원인: OrderListQuery 가 application.order.query 에 있다(PTB7). 값 객체만 import 해 med. T96587ad0.txt 228-229행.
- 생길 수 있는 일: 조회 모델 변경이 영속 어댑터 컴파일을 깨뜨리고 포트 이전 때 순환이 남는다.
- 해결: OrderListQuery 를 application.order.port 로 옮기거나 원시 인자로 받는다.

### 192위 [med · 확신 high] infrastructure 가 port 밖 application 패키지를 import (ArchUnit 동결 1건)

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/OrderRepositoryImpl.kt:3` · 항목표 backend/layering · 항목 L4

```
import com.papertrading.api.application.order.query.OrderListQuery   // ← infrastructure → application(port 밖) import
```

- 원인: 어댑터 구현이 application.order.query.OrderListQuery 를 받아 searchOrders 를 구현한다(PTB7). 값 객체만이라 med. T96587ad0.txt 230-231행.
- 생길 수 있는 일: 어댑터가 application 조회 모델에 묶여 안쪽 변경이 바깥을 깨뜨린다.
- 해결: OrderListQuery 를 port 패키지로 옮겨 어댑터가 포트 타입만 import 하게 한다.

### 193위 [med · 확신 high] 주문 목록 조회의 끝 경계가 포함(<=)이라 반열림 [start, end) 규칙을 어김

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/OrderRepositoryImpl.kt:27` · 항목표 data/query · 항목 Q2

```
query.createdFrom?.let { o.createdAt.goe(it) },
query.createdTo?.let { o.createdAt.loe(it) },   // ← 끝 경계를 <= 로 포함 (PTF5 위반)
```

- 원인: createdTo 를 `loe`(<=)로 거릅니다. OrderQueryService.validateOrderRange(:57-62)도 «같거나 이전»을 허용합니다. 같은 모양이 ExecutionRepositoryImpl.kt:21 에도 있습니다.
- 생길 수 있는 일: createdTo 와 정확히 같은 시각의 주문이 인접한 두 구간 조회에 모두 잡힙니다. 화면 목록 조회라 med 로 보고합니다.
- 해결: createdTo 를 배타 상한(`lt`)으로 바꿉니다.

고친 모양(예시):
```
query.createdTo?.let { o.createdAt.lt(it) },
```

### 194위 [med · 확신 high] 세금 집계 출력 포트와 결과 타입이 port 패키지 밖(application.portfolio.tax)에 있음

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/SettlementTaxReadRepositoryImpl.kt:3` · 항목표 backend/layering · 항목 L3

```
import com.papertrading.api.application.portfolio.tax.SettlementTaxReadRepository  // ← 출력 포트가 application.portfolio.port 밖(tax 패키지)에 있음
import com.papertrading.api.application.portfolio.tax.TaxSettlementAggregate  // ← 포트 결과 타입도 port 밖에 있음
```

- 원인: SettlementTaxReadRepositoryImpl 이 구현하는 출력 포트 SettlementTaxReadRepository 와 그 결과 타입 TaxSettlementAggregate 가 application.portfolio.port 가 아니라 application.portfolio.tax 에 있다. PTB8 은 출력 포트와 포트 요청·결과 타입을 application/<ctx>/port 에 두라고 한다. ArchUnit 동결 목록(tools/T96587ad0.txt 269행)에도 같은 위반이 있다.
- 생길 수 있는 일: 포트 자리가 port 와 tax 두 곳으로 흩어지고, infrastructure 가 port 밖 application 패키지를 import 하게 되어 PTB7 방향 규칙이 이 어댑터에서 깨진다. 동결 목록에 들어 있어도 해결된 것이 아니다.
- 해결: SettlementTaxReadRepository 와 TaxSettlementAggregate 를 application/portfolio/port 로 옮기고 이 어댑터의 import 를 바꾼다. 그러면 ArchUnit 동결 항목도 함께 해소된다.

고친 모양(예시):
```
// application/portfolio/port/SettlementTaxReadRepository.kt
package com.papertrading.api.application.portfolio.port

interface SettlementTaxReadRepository {
    fun summarizeForTax(accountId: Long, yearStartInclusive: Instant, yearEndExclusive: Instant): TaxSettlementAggregate
}
// TaxSettlementAggregate 도 같은 port 패키지로 이동
```

### 195위 [med · 확신 high] 리포지토리 조각 인터페이스가 application.portfolio.query 의 필터를 import (application ↔ infrastructure 순환)

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/TradingJournalRepositoryCustom.kt:3` · 항목표 backend/layering · 항목 L4

```
import com.papertrading.api.application.portfolio.query.TradingJournalFilter  // ← infrastructure 가 port 밖 application 타입을 import
    fun search(filter: TradingJournalFilter, pageable: Pageable): Page<TradingJournal>  // ← 시그니처가 application 쿼리 타입과 Spring Data Page 에 묶임
```

- 원인: TradingJournalRepositoryCustom.search 의 시그니처가 application.portfolio.query.TradingJournalFilter 를 받는다. 이 타입은 port 패키지가 아니다. 한편 TradingJournalQueryService 는 이 Spring Data 리포지토리를 직접 주입받는다(ArchUnit 133행의 application→infrastructure 동결 위반). 그래서 application → infrastructure → application 순환이 생겼다. ArchUnit 동결 목록에도 이 파일이 1건으로 있다(T96587ad0.txt 234-235행).
- 생길 수 있는 일: 쿼리 필터 타입을 바꾸면 infrastructure 어댑터 컴파일이 깨지고, 서비스와 어댑터가 서로 의존하는 순환이 굳는다. 포트를 도입하려 할 때 필터 위치가 걸림돌이 된다.
- 해결: TradingJournalFilter 를 application/portfolio/port 로 옮기거나 포트가 쓰는 조회 조건 타입으로 새로 정의하고, 유스케이스는 포트(application/portfolio/port)를 통해 search 를 부르게 한다.

고친 모양(예시):
```
// application/portfolio/port/TradingJournalSearchPort.kt
package com.papertrading.api.application.portfolio.port

interface TradingJournalSearchPort {
    fun search(filter: TradingJournalFilter, page: PageRequestSpec): PageResult<TradingJournal>
}
// TradingJournalFilter 도 같은 port 패키지에 둔다
```

### 196위 [med · 확신 high] Redis MessageListener 진입점이 infrastructure 에 있고 유스케이스를 직접 부름

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/redis/QuoteEventListener.kt:22` · 항목표 backend/layering · 항목 L6

```
@Component("redisQuoteEventListener")
class QuoteEventListener(
    ...
) : MessageListener { // ← 외부 진입점(Redis MessageListener)이 infrastructure 에 있음

    override fun onMessage(message: Message, pattern: ByteArray?) {
        val quote = parseMessage(message.body) ?: return
        runCatching { localMatchingEngine.tryMatchPendingOrders(quote.ticker, quote) } // ← 진입점이 유스케이스를 직접 호출
```

- 원인: PTB7 은 스케줄러·Redis 리스너 같은 외부 진입점을 presentation 에 두고 유스케이스만 부르게 한다. QuoteEventListener.onMessage 는 Redis Pub/Sub 진입점인데 infrastructure.redis 에 있고 매칭 엔진·포지션 갱신·청산 트리거 3개를 직접 부른다. L4 가 import 간선을 짚는다면 이 발견은 진입점의 자리를 짚는다.
- 생길 수 있는 일: 시세 한 건이 주문 체결과 포지션 평가손익을 모두 움직이는 핵심 진입점이라, 계층 밖에 있으면 트랜잭션·예외 격리·재시도 정책을 한곳에서 점검할 자리가 없다. 진입점이 application 서비스를 import 하는 구조가 굳는다.
- 해결: 리스너 클래스를 presentation 으로 옮기고 파싱 결과를 application 유스케이스 하나에 넘긴다(L4 의 fix 와 한 번에 처리). 빈 이름 redisQuoteEventListener 도 이때 정리한다.

고친 모양(예시):
```
// presentation/redis/QuoteMessageListener.kt
override fun onMessage(message: Message, pattern: ByteArray?) {
    parseMessage(message.body)?.let(quoteReceivedService::onQuote)
}
```

### 197위 [med · 확신 high] 캐시 출력 포트 LivePositionCachePort 가 domain.port 에 있어 application.<ctx>.port 규칙을 벗어남

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/redis/RedisLivePositionCacheAdapter.kt:6` · 항목표 backend/layering · 항목 L3

```
import com.papertrading.api.domain.port.LivePositionCachePort  // ← 출력 포트가 application.position.port 가 아닌 domain.port 에 있음
import com.papertrading.api.domain.port.LivePositionSnapshot
```

- 원인: 어댑터가 구현하는 LivePositionCachePort 와 LivePositionSnapshot 이 domain/port 에 있다. ArchUnit 동결 목록(T96587ad0.txt:271)이 «does not reside in a package application.*.port» 로 이미 잡고 있지만 동결은 해결이 아니다. PositionResult.kt:6 도 같은 domain.port.LivePositionSnapshot 에 기댄다. 포트 선언 파일은 내 묶음 밖이라 import 줄을 근거로 올린다.
- 생길 수 있는 일: 포트 자리가 domain.port 와 application.<ctx>.port 두 곳으로 갈려 infrastructure 의 의존 방향(PTB7)이 이 포트 때문에만 예외가 된다.
- 해결: LivePositionCachePort·LivePositionSnapshot 을 application/position/port 로 옮기고 어댑터·결과 매퍼의 import 를 갱신한다. 이어서 동결 저장소 항목을 줄인다.

고친 모양(예시):
```
package com.papertrading.api.application.position.port
interface LivePositionCachePort { fun find(accountId: Long, ticker: String): LivePositionSnapshot? }
```

### 198위 [med · 확신 high] 스냅샷 파싱 실패를 runCatching + getOrNull 로 로그 없이 버림

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/redis/RedisLivePositionCacheAdapter.kt:57` · 항목표 reliability/failure-handling · 항목 E1

```
private fun read(key: String): LivePositionSnapshot? {
    val hash = redisTemplate.opsForHash<String, String>().entries(key)
    if (hash.isEmpty()) return null
    return runCatching {
        LivePositionSnapshot(
            id = hash.getValue("id").toLong(),
            ...
    }.getOrNull()   // ← 파싱 실패를 로그 없이 null 로 버림 → 호출자 mapNotNull 이 조용히 제외
```

- 원인: Redis 해시 → LivePositionSnapshot 변환(`getValue`·`BigDecimal(...)`·`valueOf`)이 실패하면(필드 누락·숫자·enum 오류, 구버전 스키마) 로그 없이 null 이 되어 `mapNotNull` 에서 조용히 빠진다. PositionCommandService.kt:32 의 DB 대체 조회는 목록이 통째로 비었을 때만 돌아, 일부만 깨지면 그 포지션은 갱신·복구 없이 사라진 채 남는다. (runCatching 이 Throwable 까지 잡는 넓은 catch 인 점과 실패 항목을 세지 않고 지나가는 루프 모양도 같은 지점이다.)
- 생길 수 있는 일: 포지션 일부가 실시간 평가 목록에서 빠져도 알림·로그가 없어 화면의 보유 종목 누락을 원인 없이 마주친다.
- 해결: 파싱 예외를 좁은 타입으로 잡아 key 와 함께 WARN 로그를 남기고, 깨진 키는 삭제하거나 DB 에서 재생성하게 한다.

고친 모양(예시):
```
return try { LivePositionSnapshot(...) } catch (ex: RuntimeException) {
    log.warn(ex) { "live position snapshot parse failed: key=$key" }
    null
}
```

### 199위 [med · 확신 high] infrastructure 어댑터가 port 밖 application.common.result 를 import 함

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/redis/RedisMarketQuoteAdapter.kt:4` · 항목표 backend/layering · 항목 L4

```
import com.papertrading.api.application.common.result.QuoteSnapshot // ← infrastructure 가 port 밖 application 패키지를 import (PTB7)
```

- 원인: RedisMarketQuoteAdapter 가 application.common.result.QuoteSnapshot 을 import 한다. PTB7 은 infrastructure 가 application 중 *.port 만 import 하도록 허용한다. 뿌리는 MarketQuotePort 의 L3 와 같다(결과 타입이 port 패키지에 없음).
- 생길 수 있는 일: QuoteSnapshot 의 패키지·필드가 바뀌면 Redis 어댑터가 함께 깨지고, 허용 목록 밖 의존이 ArchUnit 동결 목록에 계속 남는다.
- 해결: QuoteSnapshot 을 application/common/port 로 옮겨(MarketQuotePort L3 와 함께) import 를 *.port 안으로 바꾼다.

고친 모양(예시):
```
import com.papertrading.api.application.common.port.MarketQuotePort
import com.papertrading.api.application.common.port.QuoteSnapshot
```

### 200위 [med · 확신 high] 시세 해시 파싱 실패를 runCatching.getOrNull() 로 삼킴

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/redis/RedisMarketQuoteAdapter.kt:40` · 항목표 reliability/failure-handling · 항목 E1

```
return runCatching {
    QuoteSnapshot(
        price = BigDecimal(hash.getValue("price")),  // ← 필드 누락·숫자 오류가 예외로
        ...
    )
}.getOrNull()  // ← 로그 없이 null 로 변환
```

- 원인: 해시에 price·askp1·bidp1 가 없거나 숫자가 아니면 NoSuchElementException·NumberFormatException 이 나지만 getOrNull() 로 버리고 null(=시세 없음)을 돌려준다. 로그가 없다. 시세 없음(만료·미구독)과 시세 손상이 호출자에게 같은 null 로 보인다.
- 생길 수 있는 일: 손상된 시세는 '시세 없음' 으로 위장돼 시장가 주문이 이유 없이 거부되거나 지정가 검증이 건너뛰어진다. 원인 추적용 로그가 없다.
- 해결: getOrElse 로 WARN(키·실패 필드)을 남기고 null 을 돌려주거나, 손상은 별도 결과로 구분한다. 잡는 예외도 NumberFormatException·NoSuchElementException 으로 좁힌다.

고친 모양(예시):
```
}.getOrElse { log.warn(it) { "시세 해시 파싱 실패: key=${quoteKey(tradingMode, ticker)}" }; null }
```

### 201위 [med · 확신 high] infrastructure 가 application 유스케이스를 직접 import (ArchUnit 동결 2건) — 죽은 클래스

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/scheduler/KisPaperPollingScheduler.kt:3` · 항목표 backend/layering · 항목 L4

```
import com.papertrading.api.application.order.KisPaperOrderExecutor   // ← infrastructure → application 유스케이스 import

// 스케쥴러 삭제 필요   // ← 스스로 삭제 대상이라고 적은 죽은 코드
class KisPaperPollingScheduler(
```

- 원인: KisPaperOrderExecutor(application)를 주입받아 pollFills 를 위임한다. 이 클래스는 @Component·@Scheduled 가 없고 코드 참조도 없다(KisLiveOrderExecutor.kt:14 KDoc 한 줄뿐). 파일 안 주석 「스케쥴러 삭제 필요」도 같은 뜻이라, 실행되지 않아 기본 high 에서 med 로 낮춤. T96587ad0.txt 242-243행.
- 생길 수 있는 일: 동결 위반 한 건을 계속 안고 가고, 이름이 스케줄러라 읽는 사람이 폴링이 도는 줄 오해한다.
- 해결: 파일을 삭제한다(그러면 동결 위반 2건도 사라진다). 폴링 진입점이 필요하면 presentation 에서 유스케이스만 부른다.

### 202위 [med · 확신 high] 입출금 요청 금액·문자열에 자릿수·길이 상한 검증이 없음

`backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/controller/AccountController.kt:90` · 항목표 reliability/security-config-hygiene · 항목 S8

```
@PostMapping("/{id}/deposit")
fun deposit(@PathVariable id: Long, @Valid @RequestBody request: DepositRequest)
// AccountRequest.kt:26-28
@field:NotNull @field:DecimalMin("0.0001") val amount: BigDecimal,  // ← @Digits(integer=16, fraction=4) 없음
@field:NotBlank val idempotencyKey: String,  // ← @Size(max=100) 없음 (DB length 100)
val description: String? = null  // ← @Size(max=500) 없음
```

- 원인: 컨트롤러는 @Valid 를 쓰지만 DTO(AccountRequest.kt)의 amount 는 @DecimalMin 만 있고 @Digits 가 없다(DB 는 numeric(20,4)). idempotencyKey·description·accountName·externalAccountId 에는 @Size 가 없는데 DB 길이는 100·500·100·100 이다. 결함 위치는 DTO 파일이며 이 렌즈 범위에는 컨트롤러만 있어 컨트롤러 줄에 기록한다.
- 생길 수 있는 일: 소수점 5자리 이상 금액이 들어오면 메모리 계산과 DB 저장값(반올림)이 어긋나고, 너무 큰 값·긴 문자열은 DB 오류로 500 이 된다.
- 해결: 금액에 @Digits(integer = 16, fraction = 4), 문자열에 DB 길이에 맞춘 @Size 를 단다.

고친 모양(예시):
```
@field:NotNull @field:DecimalMin("0.0001") @field:Digits(integer = 16, fraction = 4) val amount: BigDecimal,
@field:NotBlank @field:Size(max = 100) val idempotencyKey: String,
```

### 203위 [med · 확신 high] SSE 컨트롤러가 CORS 출처를 와일드카드(*)로 열어 둠 (TODO(prod) 로 남은 임시 설정)

`backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/controller/ExecutionSseController.kt:15` · 항목표 reliability/security-config-hygiene · 항목 S9

```
@CrossOrigin(origins = ["*"]) // TODO(prod): restrict to allowed origins before production deployment   // ← 모든 출처 허용, 운영 전 제한하겠다는 임시값
```

- 원인: @CrossOrigin(origins = ["*"]) 로 모든 출처가 이 스트림을 읽을 수 있습니다. allowCredentials 는 설정되지 않았습니다(기본 false). 그래서 렌즈가 말하는 와일드카드+자격 증명 조합은 아니고, 위험도가 그만큼 높지 않습니다. 다만 주석이 이 값을 운영 전에 줄이겠다는 임시값이라고 스스로 밝히고 있고, 레포 어디에도 전역 CORS 설정이 없어(CorsConfiguration·addCorsMappings grep 0건) 이 한 줄이 유일한 CORS 정책입니다.
- 생길 수 있는 일: 인증 없는 전 계좌 체결 스트림을 어떤 웹페이지의 스크립트든 EventSource 로 구독할 수 있습니다. TODO 가 그대로 배포되면 임시 설정이 운영 정책이 됩니다.
- 해결: 허용 출처를 설정값(@ConfigurationProperties)으로 받아 명시 목록으로 제한하고, 컨트롤러 어노테이션은 제거하거나 그 목록을 참조하게 합니다.

고친 모양(예시):
```
@Bean
fun corsConfigurer(props: CorsProps) = object : WebMvcConfigurer {
    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/api/v1/executions/stream").allowedOrigins(*props.allowedOrigins.toTypedArray())
    }
}
```

### 204위 [med · 확신 high] 매매일지 수정이 요청의 accountId 를 받고도 소유 검사를 하지 않음

`backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/controller/TradingJournalController.kt:58` · 항목표 reliability/security-config-hygiene · 항목 S7

```
            UpdateTradingJournalCommand(
                accountId = request.accountId,   // ← 소유 계좌를 받아 넘기지만
...
// TradingJournalCommandService.kt:37-41
    val journal = tradingJournalRepository.findById(journalId)
        .orElseThrow { TradingJournalNotFoundException(journalId) }
    journal.update(command.title, command.content, command.sentiment)   // ← accountId 를 쓰지 않음 — 소유 검사 없음
```

- 원인: 컨트롤러는 request.accountId 를 UpdateTradingJournalCommand 로 넘기지만 TradingJournalCommandService.update 는 journalId 로만 조회하고 command.accountId 를 보지 않는다. 같은 일지를 읽는 TradingJournalQueryService.get 은 belongsTo(accountId) 로 막는다. CommandService 에 TradingJournalOwnershipMismatchException import 가 남아 있어(미사용) 검사가 빠진 흔적으로 보인다.
- 생길 수 있는 일: 일지 id 만 알면 다른 계좌의 일지 제목·내용을 수정할 수 있다. 인증이 생겨도 이 모양이면 IDOR 가 된다.
- 해결: update 에서 조회 직후 journal.belongsTo(command.accountId) 를 검사하고 아니면 TradingJournalOwnershipMismatchException 을 던진다(조회와 같은 규칙).

고친 모양(예시):
```
val journal = tradingJournalRepository.findById(journalId)
    .orElseThrow { TradingJournalNotFoundException(journalId) }
if (!journal.belongsTo(command.accountId)) {
    throw TradingJournalOwnershipMismatchException(journalId, command.accountId)
}
journal.update(command.title, command.content, command.sentiment)
```

### 205위 [med · 확신 high] presentation DTO 가 도메인 엔티티를 직접 import 해 변환 인자로 받음

`backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/dto/portfolio/PortfolioSnapshotDto.kt:3` · 항목표 backend/layering · 항목 L5

```
import com.papertrading.api.domain.entity.portfolio.DailyBalance      // ← presentation 이 domain.entity 를 직접 import
import com.papertrading.api.domain.entity.portfolio.PortfolioSnapshot  // ← 같은 위반
...
        fun from(entity: DailyBalance): DailyBalanceResponse = DailyBalanceResponse(   // ← 변환 인자가 도메인 엔티티
...
        fun from(entity: PortfolioSnapshot): PortfolioSnapshotResponse = PortfolioSnapshotResponse(   // ← 같은 위반
```

- 원인: DailyBalanceResponse.from(entity: DailyBalance)·PortfolioSnapshotResponse.from(entity: PortfolioSnapshot) 가 domain.entity 를 import 합니다. 호출 쪽 응용 서비스가 List<DailyBalance>·List<PortfolioSnapshot> 엔티티를 그대로 돌려주기 때문입니다(DailyBalanceQueryService.kt:19, PortfolioSnapshotQueryService.kt:18). ArchUnit 동결 목록(presentation→domain.entity 규칙, 이 파일 2건)에 있지만 동결은 해결이 아닙니다. DTO 가 필드를 하나씩 매핑해 엔티티 자체가 JSON 으로 새지는 않으므로 심각도는 high 에서 med 로 낮춥니다. 같은 결함의 컨트롤러 쪽 끝(PortfolioSnapshotController.kt:32,42 의 .map { …from(it) })은 따로 올리지 않았습니다.
- 생길 수 있는 일: 도메인 엔티티·JPA 매핑이 바뀌면 presentation 컴파일이 깨지고, 엔티티가 트랜잭션 밖 DTO 변환까지 살아 있어 지연 로딩·프록시 접근이 응답 직렬화 시점으로 새어 나갑니다.
- 해결: 응용 서비스가 DailyBalanceResult·PortfolioSnapshotResult 같은 application result(TradingJournalResult 처럼)를 돌려주게 하고, DTO 의 from 은 result 를 받는다. 엔티티→result 변환은 application 안에 둔다.

고친 모양(예시):
```
// application/portfolio/result/DailyBalanceResult.kt
data class DailyBalanceResult(val businessDate: LocalDate, val cashBalance: BigDecimal, val pnlRate: BigDecimal /* … */)

// presentation
fun from(result: DailyBalanceResult): DailyBalanceResponse = DailyBalanceResponse(
    businessDate = result.businessDate,
    pnlRate = result.pnlRate.setScale(6, RoundingMode.HALF_UP),
    /* … */
)
```

### 206위 [med · 확신 high] TaxSummaryResponse 가 TaxSummary 엔티티를 받아 다른 애그리거트(Account)까지 타고 넘어감

`backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/dto/portfolio/TaxSummaryDto.kt:3` · 항목표 backend/layering · 항목 L5

```
import com.papertrading.api.domain.entity.portfolio.TaxSummary   // ← presentation → domain.entity
...
        fun from(summary: TaxSummary): TaxSummaryResponse = TaxSummaryResponse(   // ← 변환 인자가 도메인 엔티티
            id = summary.id ?: 0L,   // ← id 없음을 0 으로 조용히 메움
            accountId = summary.account.id ?: 0L,   // ← 다른 애그리거트(Account)로 엔티티 그래프를 타고 넘어감
```

- 원인: TaxSummaryResponse.from(summary: TaxSummary) 가 domain.entity.portfolio.TaxSummary 를 import 하고, summary.account.id 로 다른 애그리거트 루트(Account)를 엔티티 그래프로 건너가 id 를 읽습니다. id 가 null 이면 0L 로 조용히 메웁니다(id 는 summary.id ?: 0L 도 같음). ArchUnit 동결 1건과 같은 대상입니다. 엔티티가 JSON 으로 새지는 않아 high 에서 med 로 낮춥니다.
- 생길 수 있는 일: TaxSummary 의 account 연관이 지연 로딩이면 DTO 변환이 영속성 컨텍스트 밖에서 일어나 프록시 접근 오류 위험이 있고, 저장 전 엔티티가 응답으로 나가면 id 0 이 가짜 값으로 노출됩니다.
- 해결: TaxSummaryQueryService·TaxSummaryCommandService 가 accountId 를 담은 TaxSummaryResult 를 돌려주게 하고 DTO 는 result 만 받는다. id 는 널 불가로 두어 0 대체를 없앤다.

고친 모양(예시):
```
data class TaxSummaryResult(val id: Long, val accountId: Long, val taxYear: Int, val totalRealizedPnl: BigDecimal /* … */)

fun from(result: TaxSummaryResult): TaxSummaryResponse = TaxSummaryResponse(id = result.id, accountId = result.accountId, /* … */)
```

### 207위 [med · 확신 high] DB 비밀번호 기본값이 커밋돼 있음

`backend/trading-api/src/main/resources/application.yaml:10` · 항목표 reliability/security-config-hygiene · 항목 S1

```
    password: ${DB_PASSWORD:***}   # ← 기본값이 실제 비밀번호(값은 가림)
```

- 원인: ${DB_PASSWORD:기본값} 의 기본값이 실제 쓰이는 비밀번호이고 docker-compose.yml 도 같은 값을 직접 적고 있다. DB_USERNAME 기본값도 같은 계정이다. 값 원문은 가렸다.
- 생길 수 있는 일: 환경 변수를 빠뜨린 채 뜨면 누구나 아는 비밀번호로 DB 에 붙는다. 커밋된 값은 회수할 수 없다.
- 해결: 비밀번호 기본값을 비우고(${DB_PASSWORD}) 로컬 값은 .env 로만 주입한다. 값이 없으면 기동 때 실패하게 한다.

고친 모양(예시):
```
    password: ${DB_PASSWORD}
```

### 208위 [med · 확신 high] 유스케이스 시험이 Spring Data 리포지토리 목으로 락·멱등 조회를 흉내 냄

`backend/trading-api/src/test/kotlin/com/papertrading/api/application/account/AccountCommandServiceTest.kt:28` · 항목표 backend/tdd-purity · 항목 T2

```
    private val accountRepository = mockk<AccountRepository>()      // ← Spring Data 리포지토리 목(포트 가짜 아님)
    private val accountLedgerRepository = mockk<AccountLedgerRepository>()
...
        every { accountRepository.findByIdWithLock(1L) } returns Optional.of(account)      // ← 락 조회를 목 반환으로 흉내
...
        } returns null      // ← 멱등키 조회·중복 동작을 목으로 흉내(63~67줄)
```

- 원인: AccountRepository·AccountLedgerRepository 목의 findByIdWithLock 과 findByAccountIdAndIdempotencyKey 반환값으로 비관적 락과 멱등키 중복 판단을 만들어 낸다. 포트 가짜가 아니라 PTB8 기존 위반 위에 선 목이다.
- 생길 수 있는 일: 락 대기·유일 제약·멱등키 조회의 실제 동작은 이 시험에서 한 번도 돌지 않고, 목이 믿는 결과가 운영 쿼리와 갈려도 초록이다.
- 해결: 리포지토리가 포트로 올라가면 인메모리 포트 가짜로 바꾸고, 락·멱등 유일 제약은 Testcontainers 시험으로 따로 덮는다.

### 209위 [med · 확신 high] 도메인 엔티티 AccountLedger 를 목으로 갈음함

`backend/trading-api/src/test/kotlin/com/papertrading/api/application/account/AccountCommandServiceTest.kt:47` · 항목표 backend/tdd-purity · 항목 T1

```
        every { accountLedgerRepository.save(any()) } returns mockk()      // ← 도메인 엔티티를 목으로 대체
...
        val existingLedger = mockk<AccountLedger>()      // ← 도메인 엔티티 목(120줄에도 같은 패턴)
```

- 원인: 멱등 재호출 시험 두 곳은 mockk<AccountLedger>() 를, 생성 시험은 save 의 반환으로 returns mockk() 를 쓴다. 목은 불변식·생성 팩토리를 타지 않는다. 단언은 isEqualTo(existingLedger) 같은 동일성 확인뿐이라 파급은 제한적이어서 high 에서 med 로 낮췄다.
- 생길 수 있는 일: AccountLedger 의 생성 규칙이 바뀌어도 이 시험은 계속 초록이고, 서비스가 원장 필드를 읽는 순간 MockKException 으로 깨져 구현에 묶인다.
- 해결: 실제 AccountLedger 를 만들어 쓴다(Account.deposit 결과·시험 픽스처).

고친 모양(예시):
```
val existingLedger = account.deposit(BigDecimal("100000"), "key-001")      // 실제 원장 생성(팩토리 시그니처는 도메인 따름)
assertThat(result).isSameAs(existingLedger)
```

### 210위 [med · 확신 high] 원장 필터·페이징 시험이 Querydsl 어댑터를 목 반환으로 대신함

`backend/trading-api/src/test/kotlin/com/papertrading/api/application/account/AccountQueryServiceTest.kt:26` · 항목표 backend/tdd-purity · 항목 T2

```
    private val accountRepository = mockk<AccountRepository>()      // ← Spring Data 리포지토리 목
    private val accountLedgerRepository = mockk<AccountLedgerRepository>()
...
        every { accountLedgerRepository.findLedgers(1L, filter) } returns PageImpl(ledgerResults)      // ← 필터·페이징·정렬을 PageImpl 반환으로 대체(100줄)
```

- 원인: 시험 이름은 «원장을 필터 조건으로 페이징 조회한다»지만 findLedgers 목이 PageImpl 을 그대로 돌려줘 where·offset·limit·정렬이 한 줄도 실행되지 않는다. 활성·거래모드 필터 시험도 파생 쿼리 목으로 결과를 정한다.
- 생길 수 있는 일: 필터·페이징·정렬 조건이 틀려도 이 시험은 초록이라 시험 이름이 지키지 않는 정책을 지키는 것처럼 보인다.
- 해결: 서비스 단위 시험은 포트 가짜로 위임만 보고, findLedgers 의 필터·페이징·정렬은 Testcontainers(PostgreSQL) 어댑터 시험으로 덮는다.

### 211위 [med · 확신 high] 리스크 정책 시험이 리포지토리 목으로 락·활성 정책 조회를 흉내 냄

`backend/trading-api/src/test/kotlin/com/papertrading/api/application/account/RiskPolicyServiceTest.kt:23` · 항목표 backend/tdd-purity · 항목 T2

```
    private val accountRepository = mockk<AccountRepository>()      // ← Spring Data 리포지토리 목
    private val riskPolicyRepository = mockk<RiskPolicyRepository>()
...
        every { accountRepository.findByIdWithLock(1L) } returns Optional.of(account)      // ← 락 조회 흉내(41줄)
        every { riskPolicyRepository.findByAccountIdAndIsActiveTrue(1L) } returns Optional.empty()      // ← 활성 정책 조회 흉내(42줄)
```

- 원인: AccountRepository·RiskPolicyRepository 목의 findByIdWithLock 과 findByAccountIdAndIsActiveTrue 반환값으로 락과 «활성 정책은 하나» 의미를 만든다.
- 생길 수 있는 일: 활성 정책 유일성이나 락 동작이 실제 엔진에서 어긋나도 이 시험은 모른다.
- 해결: RiskPolicyPort 를 두고 인메모리 가짜로 바꾸며, 활성 정책 유일 조건은 Testcontainers 시험으로 덮는다.

### 212위 [med · 확신 high] 체결 처리 시험이 리포지토리 9개 목으로 락·원자 갱신을 흉내 냄

`backend/trading-api/src/test/kotlin/com/papertrading/api/application/order/ExecutionProcessorTest.kt:43` · 항목표 backend/tdd-purity · 항목 T2

```
private val orderRepository = mockk<OrderRepository>() // ← Spring Data 리포지토리 목(9종)
...
every { positionRepository.findByAccountIdAndTickerWithLock(1L, "005930") } returns Optional.of(position) // ← 락 조회를 흉내
every { positionRepository.applySellExecutionAtomically(any(), any()) } returns 1 // ← 원자 갱신 SQL 을 흉내
```

- 원인: 리포지토리 9종을 목으로 두고 findByIdWithOptimisticLock·findByIdWithLock·findByAccountIdAndTickerWithLock 으로 락 조회를, applySellExecutionAtomically 반환 1 로 원자 갱신 SQL 을 흉내 낸다(PTB8·PTT1).
- 생길 수 있는 일: 낙관·비관 락 충돌과 원자 갱신의 실제 동작은 검증되지 않는다. 금융 상태를 바꾸는 체결 경로의 동시성 결함이 시험 밖에 남는다.
- 해결: 포트 도입 후 인메모리 가짜 포트로 유스케이스를 검증하고, 락·원자 갱신은 Testcontainers 통합 시험으로 따로 검증한다.

### 213위 [med · 확신 high] «계좌 범위 중복 판정» 시험이 리포지토리 목으로 쿼리 범위를 흉내 냄

`backend/trading-api/src/test/kotlin/com/papertrading/api/application/order/KisExecutionNoticeServiceTest.kt:29` · 항목표 backend/tdd-purity · 항목 T2

```
private val orderRepository = mockk<OrderRepository>() // ← 리포지토리 목
private val executionRepository = mockk<ExecutionRepository>()
...
every {
    orderRepository.findActiveKisOrderByExternalOrderId("10000001", TradingMode.KIS_PAPER, "12345678-01")
} returns Optional.of(order) // ← 쿼리 범위 동작을 목이 지어냄
```

- 원인: 시험 이름은 계좌 범위에 따른 중복 판정을 말하지만, 범위 조건(findActiveKisOrderByExternalOrderId 의 mode·accountScope)은 목이 인자 일치로 돌려주는 값일 뿐이다(PTB8·PTT1). 쿼리가 실제로 범위를 걸러내는지는 검증되지 않는다.
- 생길 수 있는 일: 계좌 범위 필터가 빠지거나 틀려도 이 시험은 초록이라, 다른 계좌의 체결 통지가 섞여 들어가는 결함을 못 잡는다.
- 해결: 포트 가짜를 쓰는 유스케이스 시험과, 범위 쿼리를 Testcontainers PostgreSQL 로 검증하는 어댑터 시험으로 나눈다.

### 214위 [med · 확신 high] 체결 처리 유스케이스와 도메인 엔티티를 목으로 갈음

`backend/trading-api/src/test/kotlin/com/papertrading/api/application/order/KisExecutionNoticeServiceTest.kt:31` · 항목표 backend/tdd-purity · 항목 T1

```
private val executionProcessor = mockk<ExecutionProcessor>() // ← 내부 유스케이스를 목으로 갈음
...
every { executionRepository.findByExternalExecutionId(notice.externalExecutionId) } returns Optional.of(mockk<Execution>(relaxed = true)) // ← 도메인 엔티티를 relaxed 목으로 지어냄
```

- 원인: ExecutionProcessor(체결·포지션·정산을 바꾸는 핵심 유스케이스)를 목으로 두고 인자 일치만 확인한다. 중복 판정에는 Execution 엔티티를 relaxed 목으로 지어냈다(TT6·TT7). 그래서 통지 처리의 실제 상태 변화는 한 번도 검증되지 않는다.
- 생길 수 있는 일: 체결 통지가 금융 상태를 올바르게 바꾸는지 이 시험으로는 알 수 없고, 처리기 시그니처 변경에도 초록이다.
- 해결: 실제 Execution 을 만들어 쓰고(Execution.create), 처리기는 실물이나 포트 가짜로 두어 결과 상태(체결 수·잔고)를 단언한다.

고친 모양(예시):
```
val existing = Execution.create(order = sampleOrder(), account = sampleAccount(), ticker = "005930", executedQuantity = BigDecimal("3"), executedPrice = BigDecimal("70000"), krwExecutedPrice = BigDecimal("70000"), externalExecutionId = notice.externalExecutionId, executedAt = notice.executedAt)
every { executionRepository.findByExternalExecutionId(notice.externalExecutionId) } returns Optional.of(existing)
```

### 215위 [med · 확신 high] 세 시험 모두 verify 만 있고 결과 상태 단언이 없음

`backend/trading-api/src/test/kotlin/com/papertrading/api/application/order/KisExecutionNoticeServiceTest.kt:41` · 항목표 backend/tdd-purity · 항목 T3

```
verify(exactly = 0) { orderRepository.findActiveKisOrderByExternalOrderId(any(), any(), any()) } // ← 상호작용 단언만
verify(exactly = 0) { executionProcessor.fillExternal(any(), any(), any(), any(), any()) }
```

- 원인: 중복 통지 시험은 verify(exactly = 0) 두 개만, 나머지 둘은 verify(exactly = 1) 만 단언한다. 내부 협력자(리포지토리·ExecutionProcessor) 호출 횟수가 곧 합격 기준이라 행동이 아니라 구현을 시험한다(TT44).
- 생길 수 있는 일: 처리기를 다른 방식으로 호출하게 리팩터링하면 시험이 깨지고, 반대로 호출만 하면 결과가 틀려도 통과한다.
- 해결: 가짜 포트로 저장된 체결·잔고를 단언한다(중복 통지 뒤 체결 1건·잔고 불변).

### 216위 [med · 확신 high] 유스케이스 시험이 Spring Data 리포지토리 4개를 목으로 쓰고 락·멱등 조회를 흉내 냄

`backend/trading-api/src/test/kotlin/com/papertrading/api/application/order/OrderCommandServiceTest.kt:38` · 항목표 backend/tdd-purity · 항목 T2

```
private val accountRepository = mockk<AccountRepository>() // ← 포트가 아닌 Spring Data 리포지토리 목
private val orderRepository = mockk<OrderRepository>()
...
every { accountRepository.findByIdWithLock(1L) } returns Optional.of(account) // ← 락 동작을 목이 흉내
every { orderRepository.findByAccountIdAndIdempotencyKey(1L, "key-1") } returns null // ← 멱등 조회를 흉내
```

- 원인: AccountRepository·OrderRepository·PositionRepository·AccountLedgerRepository 목이 findByIdWithLock·findByAccountIdAndIdempotencyKey 반환으로 락·멱등 조회 동작을 지어낸다(PTB8·PTT1). 포트 가짜가 아니라 리포지토리 목이다.
- 생길 수 있는 일: 비관적 락·멱등 키 조회의 실제 동작(동시 접수)은 한 번도 시험되지 않는다.
- 해결: 포트(AccountPort·OrderPort) 도입 후 인메모리 가짜 포트로 바꾸고, 락·멱등은 Testcontainers PostgreSQL 통합 시험으로 따로 지킨다.

### 217위 [med · 확신 high] 같은 레포 서비스 3개를 목으로 갈음하고 내부 순수 규칙의 반환을 지어냄

`backend/trading-api/src/test/kotlin/com/papertrading/api/application/order/OrderCommandServiceTest.kt:44` · 항목표 backend/tdd-purity · 항목 T1

```
private val localMatchingEngine = mockk<LocalMatchingEngine>() // ← 내부 서비스를 목으로 갈음
private val kisPaperOrderExecutor = mockk<KisPaperOrderExecutor>() // ← 내부 서비스(허용 목록 밖)
private val kisLiveOrderExecutor = mockk<KisLiveOrderExecutor>()
...
every { localMatchingEngine.determineFillPrice(any(), any()) } returns null // ← 순수 규칙의 동작을 시험이 지어냄
```

- 원인: LocalMatchingEngine·KisPaperOrderExecutor·KisLiveOrderExecutor(application.order)는 외부 경계 허용 목록 밖의 내부 협력자다. determineFillPrice 는 쿼리 사용 없는 순수 규칙인데 항상 null 을 돌려주게 만들어 IOC/FOK 체결 규칙을 시험이 통제한다(TT6·TT7).
- 생길 수 있는 일: 체결가 결정 규칙이나 실행기 시그니처가 바뀌어도 이 시험은 초록이다. 접수 경로의 실제 협력(IOC 즉시 취소 조건)이 검증되지 않는다.
- 해결: determineFillPrice 는 실제 LocalMatchingEngine(또는 순수 함수로 분리한 규칙)을 쓰고, 실행기는 포트 가짜로 바꾼다.

고친 모양(예시):
```
private val fakeKisOrderGateway = FakeKisOrderGateway() // KIS 주문 출력 포트 가짜
private val matchingRule = FillPriceRule() // 순수 규칙은 실물
```

### 218위 [med · 확신 high] 시험 이름은 stale 시세 예외인데 본문은 시세 없음 경로

`backend/trading-api/src/test/kotlin/com/papertrading/api/application/order/OrderCommandServiceTest.kt:105` · 항목표 backend/tdd-purity · 항목 T6

```
fun `시장가 매수 — stale 시세 시 예외`() { // ← 이름은 stale
    ...
    every { marketQuotePort.getQuote(TradingMode.LOCAL, "005930") } returns null // ← 본문은 시세 없음 경로
    ...
    assertThrows<QuoteUnavailableException> {
```

- 원인: 이름은 «시장가 매수 — stale 시세 시 예외» 지만 본문은 getQuote 가 null 을 돌려주고 QuoteUnavailableException 을 기대한다. 실제 stale 경로(60초 초과 → IllegalStateException)를 거는 시험은 이 파일에 없다. 83행 «지정가 매수 주문 정상 접수» 도 기대 결과가 이름에 없다(TT21).
- 생길 수 있는 일: stale 분기가 시험된 것처럼 보여 커버리지 구멍이 가려진다. 이 분기는 매수 잠금액 계산의 안전장치다.
- 해결: 이름을 «시세가 없으면 QuoteUnavailableException» 으로 고치고, stale 경로는 고정 Clock 으로 별도 시험을 추가한다.

고친 모양(예시):
```
fun `시세가 없으면 시장가 매수는 QuoteUnavailableException 으로 거절된다`() { ... }
fun `시세가 60초보다 오래되면 시장가 매수는 거절된다`() { /* Clock.fixed 사용 */ }
```

### 219위 [med · 확신 high] 실행기 호출 여부만 검증하고 결과 상태 단언이 없는 시험 2개

`backend/trading-api/src/test/kotlin/com/papertrading/api/application/order/OrderCommandServiceTest.kt:201` · 항목표 backend/tdd-purity · 항목 T3

```
verify(exactly = 1) { kisPaperOrderExecutor.submit(any()) } // ← 상호작용만 단언, 결과 단언 없음
verify(exactly = 0) { kisLiveOrderExecutor.submit(any()) }
```

- 원인: KIS_PAPER·KIS_LIVE 시험이 verify(exactly=…) 로 내부 협력자(KisPaperOrderExecutor·KisLiveOrderExecutor) 호출 횟수만 확인하고 주문 상태·잠금액 같은 결과를 단언하지 않는다(TT44).
- 생길 수 있는 일: 호출만 하고 order 상태나 잠금액이 틀려도 초록이다. 실행기 내부 구현이 바뀌면 시험이 깨진다.
- 해결: 포트 가짜가 받은 요청을 수집해 모드·주문 id 를 단언하고, 반환 order 의 상태·잠금액도 단언한다.

### 220위 [med · 확신 high] 조회 시험이 리포지토리 목으로 검색 동작을 흉내 냄

`backend/trading-api/src/test/kotlin/com/papertrading/api/application/order/OrderQueryServiceTest.kt:29` · 항목표 backend/tdd-purity · 항목 T2

```
private val orderRepository = mockk<OrderRepository>() // ← 리포지토리 목
...
every { orderRepository.searchOrders(any()) } returns listOf(sampleOrder()) // ← 검색 동작을 흉내, 조건과 무관하게 항상 1건
```

- 원인: searchOrders(any()) 가 항상 목록 하나를 돌려주고 단언은 size == 1 뿐이다. 필터(ticker·status·기간)·정렬이 실제로 적용되는지는 검증되지 않는다(PTB8·PTT1).
- 생길 수 있는 일: 검색 조건이 쿼리에서 빠져도 초록이다.
- 해결: 검색 쿼리는 Testcontainers PostgreSQL 로 조건별 시험을 두고, 서비스 시험은 포트 가짜로 매핑·소유권 검증만 본다.

### 221위 [med · 확신 high] Spring Data 리포지토리 목으로 락·필터 쿼리 결과를 흉내 냄

`backend/trading-api/src/test/kotlin/com/papertrading/api/application/portfolio/DailyBalanceCommandServiceTest.kt:45` · 항목표 backend/tdd-purity · 항목 T2

```
every { accountRepository.findByIdWithLock(1L) } returns Optional.of(account)  // ← 락 쿼리를 목으로
every { positionRepository.findByAccountIdAndQuantityGreaterThan(1L, BigDecimal.ZERO) } returns listOf(position)  // ← 필터 결과를 목이 정함
```

- 원인: AccountRepository·PositionRepository·DailyBalanceRepository 목이 락 조회와 수량 필터 쿼리 결과를 정해 준다. 이 쿼리의 실제 동작은 시험되지 않는다.
- 생길 수 있는 일: 쿼리·락이 틀려도 초록이다.
- 해결: 포트 가짜로 바꾸고 쿼리 동작은 Testcontainers 어댑터 시험으로 확인한다.

고친 모양(예시):
```
class InMemoryPositionPort : PositionPort { ... }
```

### 222위 [med · 확신 high] BigDecimal 을 assertEquals 로 scale 까지 비교

`backend/trading-api/src/test/kotlin/com/papertrading/api/application/portfolio/DailyBalanceCommandServiceTest.kt:52` · 항목표 backend/tdd-purity · 항목 T8

```
assertEquals(BigDecimal("1000000"), result.cashBalance)  // ← scale 까지 비교
assertEquals(BigDecimal("0.700000"), result.pnlRate)  // ← scale 6 까지 고정
assertEquals(BigDecimal.ZERO, result.stockMarketValue)
```

- 원인: cashBalance 1000000(scale 0), pnlRate 0.700000(scale 6), stockMarketValue BigDecimal.ZERO 등을 assertEquals 로 비교한다(PTB4). 값이 같아도 scale 이 다르면 빨강이다.
- 생길 수 있는 일: scale 내부 표현만 바꿔도 시험이 깨지고 금액 시험이 값이 아니라 표현을 지킨다.
- 해결: `assertThat(x).isEqualByComparingTo(...)` 또는 compareTo 로 비교하고 scale 규칙은 별도 시험으로 둔다.

고친 모양(예시):
```
assertThat(result.cashBalance).isEqualByComparingTo("1000000")
```

### 223위 [med · 확신 high] 리포지토리 목이 기간 조회 결과를 정함

`backend/trading-api/src/test/kotlin/com/papertrading/api/application/portfolio/DailyBalanceQueryServiceTest.kt:36` · 항목표 backend/tdd-purity · 항목 T2

```
every { accountRepository.findById(1L) } returns Optional.of(account)  // ← 리포지토리 목
every { dailyBalanceRepository.searchDailyBalances(1L, from, to) } returns balances  // ← 기간 필터 결과를 목이 정함
```

- 원인: searchDailyBalances 목이 범위 안 목록을 그대로 돌려준다. 기간 필터와 정렬은 시험 대상이 아니다.
- 생길 수 있는 일: 기간 경계·정렬 쿼리 오류가 시험에 잡히지 않는다.
- 해결: 어댑터 쿼리는 Testcontainers 로 시험하고 유스케이스는 포트 가짜로 시험한다.

고친 모양(예시):
```
@Testcontainers class DailyBalanceRepositoryImplTest { /* from·to 경계 포함 여부 확인 */ }
```

### 224위 [med · 확신 high] 리포지토리 목이 upsert 대상 조회·저장을 흉내 냄

`backend/trading-api/src/test/kotlin/com/papertrading/api/application/portfolio/PortfolioSnapshotCommandServiceTest.kt:68` · 항목표 backend/tdd-purity · 항목 T2

```
every { accountRepository.findByIdWithLock(1L) } returns Optional.of(account)  // ← 락 쿼리 목
every {
    portfolioSnapshotRepository.findByAccountIdAndBusinessDateAndTicker(1L, businessDate, "005930")
} returns Optional.of(existingA)  // ← 유일 키 조회 결과를 목이 정함
```

- 원인: findByIdWithLock·필터 조회·키별 조회·save 를 목이 대신해 «account-date-ticker 로 upsert» 가 실제 유일 키와 어긋나도 시험은 통과한다.
- 생길 수 있는 일: 실제 유일 제약·락과 시험의 가정이 갈려도 모른다.
- 해결: 포트 가짜 + 어댑터 Testcontainers 시험.

고친 모양(예시):
```
class FakePortfolioSnapshotPort : PortfolioSnapshotPort { /* 메모리 맵 */ }
```

### 225위 [med · 확신 high] BigDecimal 을 assertEquals 로 scale 까지 비교

`backend/trading-api/src/test/kotlin/com/papertrading/api/application/portfolio/PortfolioSnapshotCommandServiceTest.kt:85` · 항목표 backend/tdd-purity · 항목 T8

```
assertEquals(BigDecimal("700000"), rA.marketValue)  // ← scale 까지 비교
assertEquals(BigDecimal("0.400000"), rA.weight)  // ← scale 6 까지 고정
```

- 원인: marketValue·weight(0.400000)·unrealizedPnl 을 assertEquals 로 비교한다(PTB4).
- 생길 수 있는 일: weight 의 scale 6 같은 표현이 바뀌면 값이 맞아도 시험이 깨진다.
- 해결: isEqualByComparingTo 로 바꾼다.

고친 모양(예시):
```
assertThat(rA.weight).isEqualByComparingTo("0.4")
```

### 226위 [med · 확신 high] 조회 정렬을 목이 정해 정렬 단언이 항상 통과

`backend/trading-api/src/test/kotlin/com/papertrading/api/application/portfolio/PortfolioSnapshotQueryServiceTest.kt:33` · 항목표 backend/tdd-purity · 항목 T2

```
every { portfolioSnapshotRepository.searchByAccountIdAndBusinessDate(1L, date) } returns snapshots  // ← 정렬된 목록을 목이 돌려줌
assertEquals("000660", result[0].ticker)  // ← 목의 순서를 확인
```

- 원인: 목이 이미 정렬된 목록(000660, 005930)을 돌려주고 시험이 첫 원소가 000660 인지 단언한다. 정렬은 쿼리의 일인데 목 자신의 순서를 확인할 뿐이다.
- 생길 수 있는 일: 정렬 쿼리를 깨도 시험이 초록이다.
- 해결: 정렬은 어댑터 Testcontainers 시험에서 확인한다.

고친 모양(예시):
```
@Testcontainers class PortfolioSnapshotRepositoryImplTest { /* order by ticker */ }
```

### 227위 [med · 확신 high] 리포지토리 목과 DataIntegrityViolationException 목으로 락·유일 제약 동작을 흉내 냄

`backend/trading-api/src/test/kotlin/com/papertrading/api/application/portfolio/SnapshotJobServiceTest.kt:45` · 항목표 backend/tdd-purity · 항목 T2

```
every { accountRepository.findByIdWithLock(1L) } returns Optional.of(account)  // ← 락 쿼리 목
every { snapshotJobRunRepository.findTopByAccountIdAndBusinessDateOrderByStartedAtDesc(...) } returns null
every { snapshotJobRunTxService.start(any(), any()) } throws DataIntegrityViolationException("uq")  // ← 유일 제약 위반을 목으로 지어냄
```

- 원인: 락 조회·최근 실행 조회를 목이 정하고, 중복 실행(유일 제약 위반)을 DataIntegrityViolationException("uq") 던지는 목으로 대신한다. 부분 유일 인덱스 동작은 시험되지 않는다.
- 생길 수 있는 일: 실제 인덱스가 바뀌어도 중복 실행 차단이 깨진 걸 모른다.
- 해결: 유일 인덱스 동작은 Testcontainers 시험에서 중복 시작으로 확인한다.

고친 모양(예시):
```
assertThrows<SnapshotAlreadyRunningException> { service.generateDailySnapshots(id, date) }  // 같은 날짜 RUNNING 행이 실제로 있을 때
```

### 228위 [med · 확신 high] 리포지토리 목이 연도 범위 조회 결과를 정함

`backend/trading-api/src/test/kotlin/com/papertrading/api/application/portfolio/tax/TaxSummaryQueryServiceTest.kt:28` · 항목표 backend/tdd-purity · 항목 T2

```
every { taxSummaryRepository.findOneByAccountIdAndTaxYear(1L, 2024) } returns Optional.of(sampleSummary(1L, 2024))  // ← 쿼리 결과 목
assertEquals(2024, summaries.first().taxYear)  // ← 목이 준 순서
```

- 원인: findOneByAccountIdAndTaxYear·searchByAccountIdAndTaxYearRange 가 목이다. 연도 범위 경계와 정렬(2024 가 먼저)은 목의 순서 그대로다.
- 생길 수 있는 일: 범위 경계·정렬 쿼리 오류가 시험에 안 잡힌다.
- 해결: 어댑터 Testcontainers 시험으로 옮긴다.

고친 모양(예시):
```
@Testcontainers class TaxSummaryRepositoryImplTest { /* 범위 포함 경계·내림차순 */ }
```

### 229위 [med · 확신 high] ticker 조건 시험이 any() 목이라 조건을 확인하지 않음

`backend/trading-api/src/test/kotlin/com/papertrading/api/application/portfolio/TradingJournalCommandQueryServiceTest.kt:100` · 항목표 backend/tdd-purity · 항목 T2

```
every {
    tradingJournalRepository.search(any(), any())  // ← 필터를 아무거나 받는 목
} returns PageImpl(listOf(journal), PageRequest.of(0, 20), 1)
assertThat(result.totalElements).isEqualTo(1)  // ← ticker 조건과 무관한 단언
```

- 원인: `list는 ticker가 있으면 ticker 조건으로 조회한다` 인데 tradingJournalRepository.search(any(), any()) 목이 필터와 상관없이 일지를 돌려주고 단언은 totalElements 1 뿐이라 ticker 조건은 어디서도 검증되지 않는다.
- 생길 수 있는 일: ticker 필터가 깨져도 시험이 초록이다.
- 해결: 어댑터 Testcontainers 시험에서 ticker 가 다른 일지를 섞어 필터를 확인한다.

고친 모양(예시):
```
@Testcontainers class TradingJournalRepositoryImplTest { /* ticker 다른 행 제외 */ }
```

### 230위 [med · 확신 high] 유스케이스 단위 시험이 Spring Data 리포지토리를 Mockito 목으로 쓰고 never() 검증 하나로 «DB 조회 없음»을 증명

`backend/trading-api/src/test/kotlin/com/papertrading/api/application/position/PositionCommandServiceTest.kt:126` · 항목표 backend/tdd-purity · 항목 T2

```
val repository = mock(PositionRepository::class.java)  // ← Spring Data 리포지토리 목
val service = PositionCommandService(repository, cache)
...
verify(repository, never()).findOpenByTickerAndMode("005930", TradingMode.KIS_PAPER, BigDecimal.ZERO)  // ← 메서드 하나만 지정, 다른 조회는 통과
```

- 원인: mock(PositionRepository::class.java) 로 서비스를 만든 뒤 verify(never()).findOpenByTickerAndMode(...) 한 메서드로 «without_db_select» 를 주장한다. 서비스가 findByTickerAndQuantityGreaterThan 같은 다른 조회 메서드를 불러도 목은 빈 값을 돌려주므로 시험은 통과한다. 포트 가짜가 아니라 Spring Data 목이라 T2 에 해당한다(PTB8 기존 위반의 영향).
- 생길 수 있는 일: «DB 조회 없이 캐시만 갱신» 이라는 성능 계약이 조회 메서드를 바꾸는 리팩터링에 조용히 빠져나간다.
- 해결: PositionRepository 대신 포트 가짜로 바꾸거나 verifyNoMoreInteractions(repository) 로 어떤 조회도 없음을 확인한다. 이 시험의 실질 관심(캐시만 갱신)은 포트 가짜로 옮기는 것이 근본 해결이다.

고친 모양(예시):
```
verifyNoMoreInteractions(repository)  // 어떤 조회 메서드도 호출되지 않았음
```

### 231위 [med · 확신 high] Spring Data 리포지토리 목으로 락 조회 결과를 지어내고 triggerRepository 는 relaxed

`backend/trading-api/src/test/kotlin/com/papertrading/api/application/position/PositionExitTriggerCommandServiceTest.kt:30` · 항목표 backend/tdd-purity · 항목 T2

```
private val positionRepository = mockk<PositionRepository>()  // ← Spring Data 리포지토리 목
private val triggerRepository = mockk<PositionExitTriggerRepository>(relaxed = true)  // ← relaxed 라 스텁 안 한 호출은 가짜 값으로 통과
```

- 원인: PositionRepository 와 PositionExitTriggerRepository 를 mockk 로 만들고 findByIdWithLock·findByIdForUpdate 가 엔티티를 돌려준다고 정한다. PESSIMISTIC_WRITE 쿼리의 실제 동작은 한 번도 시험되지 않는다. triggerRepository 가 relaxed=true 라 스텁하지 않은 메서드 호출은 가짜 값을 돌려줘 조용히 통과할 수 있다.
- 생길 수 있는 일: 락 쿼리 변경·삭제가 시험에 안 잡히고 relaxed 목이 호출 누락도 숨긴다.
- 해결: relaxed 를 빼 필요한 호출만 스텁하고, 포트 이전 전까지는 락 쿼리를 Testcontainers 시험(T5 항목)으로 보강한다.

고친 모양(예시):
```
private val triggerRepository = mockk<PositionExitTriggerRepository>()
```

### 232위 [med · 확신 high] Spring Data 리포지토리 3개를 목으로 쓰고 락·집계 쿼리 결과를 지어냄

`backend/trading-api/src/test/kotlin/com/papertrading/api/application/position/PositionExitTriggerOrchestratorTest.kt:34` · 항목표 backend/tdd-purity · 항목 T2

```
private val positionRepository = mockk<PositionRepository>()  // ← Spring Data 목
private val triggerRepository = mockk<PositionExitTriggerRepository>()
private val orderRepository = mockk<OrderRepository>()  // ← 집계 쿼리 결과를 시험이 지어냄
```

- 원인: PositionRepository·PositionExitTriggerRepository·OrderRepository 를 mockk 로 만들어 findArmedGroupForUpdate·existsOpenOrderByAccountTickerSideWithoutGroup·sumOpenSellQuantity 의 반환값을 시나리오마다 정한다. 오케스트레이터의 핵심 분기(SELL_ALREADY_LOCKED·MANUAL_SELL_CONFLICT·수량 상한)가 전부 목이 «있다고 믿는» 쿼리 결과에 기댄다.
- 생길 수 있는 일: 집계 쿼리의 실제 의미(미체결 매도 합계·그룹 없는 주문 존재)가 틀려도 시험은 초록이라 자동 청산이 중복 주문이나 누락으로 이어질 수 있다.
- 해결: 포트 이전 전까지는 sumOpenSellQuantity·existsOpenOrder... 를 Testcontainers 시험(T5)으로 따로 고정하고, 이전 후에는 포트 가짜로 바꾼다.

고친 모양(예시):
```
class FakeOrderPort : OrderPort { var openSellQuantity = BigDecimal.ZERO; override fun sumOpenSellQuantity(...) = openSellQuantity }
```

### 233위 [med · 확신 high] BigDecimal 단언 8곳이 assertEquals 로 scale 까지 비교한다

`backend/trading-api/src/test/kotlin/com/papertrading/api/domain/entity/position/PositionTest.kt:37` · 항목표 backend/tdd-purity · 항목 T8

```
assertEquals(BigDecimal("3"), p.lockedQuantity)  // ← scale 까지 비교(equals)
assertEquals(BigDecimal("7"), p.orderableQuantity)
...
assertEquals(BigDecimal("15"), p.quantity)
// 같은 파일 L74
assertEquals(0, p.avgBuyPrice.compareTo(BigDecimal("73333.3333")))
```

- 원인: L37·38·58·59·72·81·82·83 이 assertEquals(BigDecimal("3"), …) 모양이다. 현재는 두 쪽이 모두 scale 0 이라 통과하지만 같은 파일의 L74·L99~L125 는 compareTo 를 쓴다. 한 파일 안에서 비교 방식이 갈린다.
- 생길 수 있는 일: Position 이 scale 이 다른 값을 만들게 바뀌면(예 BigDecimal("3.0")) 값이 같아도 시험이 빨개지고, 반대 방향으론 금액 시험이 scale 을 고정하는 의도 없는 계약이 된다.
- 해결: assertEquals(0, actual.compareTo(expected)) 나 보조 함수 assertBigDecimalEquals 로 통일한다.

고친 모양(예시):
```
private fun assertSameValue(expected: String, actual: BigDecimal) =
    assertEquals(0, BigDecimal(expected).compareTo(actual))
```

### 234위 [med · 확신 high] LIVE 엔드포인트 시험에 명시적 단언이 없음

`backend/trading-api/src/test/kotlin/com/papertrading/api/infrastructure/kis/KisAccountBalanceAdapterTest.kt:84` · 항목표 backend/tdd-purity · 항목 T7

```
    fun `LIVE trId면 live endpoint를 호출한다`() {
...
                match<String> { it.startsWith("https://live.example/") },      // ← 암묵 검증은 이 매처뿐
...
        adapter.fetchBalance(1L, "TTTC8434R")      // ← 반환값·토큰 사용 단언 없음
```

- 원인: «LIVE trId면 live endpoint를 호출한다» 시험은 adapter.fetchBalance(...) 만 호출하고 단언이 없다. 목의 match 인자가 틀리면 MockKException 으로 실패하는 암묵 검증은 있어 high 에서 med 로 낮췄다. 그래도 반환 스냅샷과 토큰 사용은 확인하지 않는다.
- 생길 수 있는 일: live 경로의 매핑·예외 변환이 깨져도 호출만 되면 초록이고, 암묵 검증이 mockk 설정에 기대 읽는 사람이 알아채기 어렵다.
- 해결: 반환 스냅샷의 positions 가 비어 있음과 getToken("live") 사용을 단언한다.

고친 모양(예시):
```
val snapshot = adapter.fetchBalance(1L, "TTTC8434R")

assertThat(snapshot.positions).isEmpty()
assertThat(snapshot.cashBalance).isEqualByComparingTo("0")
```

### 235위 [med · 확신 high] 원자 선점 쿼리(claimForDelivery)에 Testcontainers 시험이 없고 목으로만 흉내 냄

`backend/trading-api/src/test/kotlin/com/papertrading/api/infrastructure/notification/SlackNotificationSchedulerTest.kt:113` · 항목표 backend/tdd-purity · 항목 T5

```
    fun `claimForDelivery가 0을 반환하면 (다른 인스턴스가 선점) 해당 레코드를 건너뛴다`() {   // ← 이름은 DB 선점을 말하지만
...
        every { txService.markDelivering(notification) } returns null                      // ← 목이 null 로 흉내만 냄. 실제 UPDATE 는 한 번도 실행되지 않음
```

- 원인: NotificationRepositoryImpl.claimForDelivery 는 UPDATE…WHERE status='PENDING' 로 인스턴스 간 중복 발송을 막는 PostgreSQL 어댑터입니다. 그런데 이 시험은 목이 null 을 돌려주는 것으로 선점 실패를 흉내 냅니다. src/test 전체에서 claimForDelivery·NotificationRepository·NotificationPersistenceTxService·QNotification 을 함께 찾았을 때 이 시험 이름의 문자열 한 줄뿐이고, 같은 레포의 다른 어댑터에는 Testcontainers 시험이 있어 방법은 이미 있습니다. 알림은 금전 상태가 아니라서 심각도를 high 에서 med 로 낮췄습니다.
- 생길 수 있는 일: 선점 조건·트랜잭션 격리가 실제 PostgreSQL 에서 의도대로 도는지 아무 시험도 확인하지 않아, 다중 인스턴스에서 같은 알림이 중복 발송돼도 CI 는 초록입니다.
- 해결: NotificationRepositoryImpl 용 Testcontainers 시험을 추가해 같은 행을 두 번 선점하면 처음만 1을 돌려주는지 검증합니다.

고친 모양(예시):
```
@Test
fun `같은 알림을 두 번 선점하면 첫 번째만 성공한다`() {
    val saved = repository.save(Notification.create(NotificationEventType.EXECUTION_FILLED, "ref:1", "t", "m"))
    assertEquals(1, repository.claimForDelivery(saved.id!!))
    assertEquals(0, repository.claimForDelivery(saved.id!!))
}
```

### 236위 [med · 확신 high] 'emitter 제거됨' 시험의 제거 단언이 항상 참 (가짜 초록)

`backend/trading-api/src/test/kotlin/com/papertrading/api/infrastructure/sse/ExecutionSseRegistryTest.kt:57` · 항목표 backend/tdd-purity · 항목 T7

```
        assertDoesNotThrow { registry.broadcast(sampleSseEvent()) }
        val otherEmitter = mockk<SseEmitter>(relaxed = true)
        registry.broadcast(sampleSseEvent())
        verify(exactly = 0) { otherEmitter.send(any<SseEmitter.SseEventBuilder>()) }  // ← 등록한 적 없는 emitter 검증, 항상 참 — failingEmitter 제거는 확인하지 않음
```

- 원인: 시험 이름은 emitter removed from registry 인데 제거 여부를 보는 단언은 등록한 적 없는 otherEmitter 에 send 가 0번인지뿐이라 레지스트리가 무엇을 하든 통과한다. catch 안의 remove(clientId) 를 지워도 assertDoesNotThrow 는 catch 가 예외를 삼켜 통과한다. 같은 동작은 L106~L128 시험이 failingEmitter.send 1회로 실제로 지키므로 이 시험은 중복이면서 거짓 안심을 준다.
- 생길 수 있는 일: 제거 회귀를 이 시험이 지키는 것으로 오해하게 하고, L106 시험이 바뀌거나 지워지면 보호가 조용히 사라진다.
- 해결: 두 번째 broadcast 뒤 failingEmitter.send 가 총 1회뿐임을 검증하거나, L106 시험과 중복이므로 삭제한다.

고친 모양(예시):
```
registry.broadcast(sampleSseEvent())
registry.broadcast(sampleSseEvent())
verify(exactly = 1) { failingEmitter.send(any<SseEmitter.SseEventBuilder>()) }
```

### 237위 [med · 확신 high] 시험 이름의 기대(400)와 실제 단언(409·404) 불일치

`backend/trading-api/src/test/kotlin/com/papertrading/api/presentation/controller/OrderControllerIntegrationTest.kt:135` · 항목표 backend/tdd-purity · 항목 T6

```
    fun `LOCAL 시장가 매수 주문 — stale 시세면 400`() {
        }.andExpect { status { isConflict() } }  // ← 이름은 400, 단언은 409
    fun `LOCAL 미보유 종목 매도 시 400`() {
        }.andExpect { status { isNotFound() } }  // ← 이름은 400, 단언은 404
```

- 원인: 'stale 시세면 400' 시험은 isConflict()(409), '미보유 종목 매도 시 400' 시험은 isNotFound()(404)를 단언한다. 이름이 말하는 정책과 지키는 정책이 달라, 빨간 시험이 어떤 계약이 깨졌는지 거짓으로 알려 준다.
- 생길 수 있는 일: 실패 시 400 계약이 깨졌다고 오해해 진단이 길어지고, 이름을 믿고 상태 코드를 400 으로 바꾸는 변경을 시험이 막지 못한다는 사실이 가려진다.
- 해결: 이름을 실제 계약(409·404)에 맞추거나, 계약이 400 이라면 제품 코드와 단언을 함께 고친다.

### 238위 [med · 확신 high] '잠금 해제' 시험이 잠금이 걸렸는지 단언하지 않음 (가짜 초록)

`backend/trading-api/src/test/kotlin/com/papertrading/api/presentation/controller/OrderControllerIntegrationTest.kt:196` · 항목표 backend/tdd-purity · 항목 T7

```
        // 계좌 잔고 확인 — 잠금 반영
        val beforeCancel = mockMvc.get("/api/v1/accounts/$accountId").andReturn()
        val beforeAvailable = objectMapper.readTree(beforeCancel.response.contentAsString)["availableDeposit"].asLong()  // ← 읽기만 하고 단언 없음
```

- 원인: 취소 전 잔고를 읽는 beforeAvailable 은 읽기만 하고 쓰이지 않는다(detekt UnusedPrivateProperty 도 같은 줄). 잠금이 한 번도 걸리지 않아도 취소 뒤 availableDeposit 이 10,000,000 이라는 마지막 단언이 초록이라, 이름 '잠금 해제' 의 전제가 지켜지지 않는다. 예수금 잠금은 CLAUDE.md 금융 안전의 핵심 경로다.
- 생길 수 있는 일: 주문 시 예수금 잠금이 빠지는 회귀가 있어도 이 시험이 초록이라 과매수 가능한 상태로 배포될 수 있다.
- 해결: 취소 전에 beforeAvailable 이 초기 예수금보다 작음을 단언한 뒤 취소 후 복구를 확인한다.

고친 모양(예시):
```
assertTrue(beforeAvailable < 10_000_000) { "주문 후 예수금 잠금이 반영돼야 한다" }
```

### 239위 [med · 확신 high] «공백과 소문자 ticker 정상 처리» 시험이 실제로는 평범한 "005930" 을 요청 — 가짜 초록

`backend/trading-api/src/test/kotlin/com/papertrading/api/presentation/controller/PositionControllerIntegrationTest.kt:180` · 항목표 backend/tdd-purity · 항목 T7

```
fun `공백과_소문자가_포함된_ticker_입력시_정상_처리된다`() {  // ← 이름은 공백·소문자 입력을 약속
    savePosition("005930", BigDecimal("7"), BigDecimal("65000"))
    mockMvc.get("/api/v1/accounts/${account.id}/positions/005930")  // ← 실제 요청은 공백도 소문자도 없는 평범한 티커
```

- 원인: 시험 이름이 trim+uppercase 처리를 약속하지만 본문은 savePosition("005930") 후 "/positions/005930" 을 요청한다. 150행의 단건 조회 시험과 같은 경로라 이 시험이 지키는 정책(공백·소문자 정규화)은 없다. 소문자 요청은 165행 시험이 따로 다루지만 trim 은 어디서도 시험되지 않는다.
- 생길 수 있는 일: 컨트롤러의 trim() 을 지워도 이 시험은 초록이라 ticker 정규화 회귀를 못 막는다.
- 해결: URL 인코딩된 공백(%20005930%20)과 소문자 조합을 실제로 요청하거나, 입력이 같다면 시험을 삭제해 150행 시험과의 중복을 없앤다.

고친 모양(예시):
```
mockMvc.get("/api/v1/accounts/${account.id}/positions/%20aapl%20").andExpect { status { isOk() }; jsonPath("$.ticker") { value("AAPL") } }
```

### 240위 [med · 확신 high] «orderableQuantity 가 올바르게 반환된다» 시험이 값 없이 isNotEmpty 만 단언

`backend/trading-api/src/test/kotlin/com/papertrading/api/presentation/controller/PositionControllerIntegrationTest.kt:238` · 항목표 backend/tdd-purity · 항목 T7

```
fun `잠금수량이_있는_포지션은_orderableQuantity가_올바르게_반환된다`() {  // ← 이름은 «올바른 값»을 약속
    ...
    jsonPath("$.orderableQuantity") { isNotEmpty() }  // ← 값 7 이 아니라 존재만 확인
```

- 원인: 잠금 수량 3·보유 10 이면 orderableQuantity 는 7 이어야 하는데 jsonPath 단언이 isNotEmpty() 뿐이다. orderableQuantity 에 lockedQuantity 가 매핑돼도 통과한다.
- 생길 수 있는 일: 응답 매핑 회귀(필드 뒤바뀜)가 시험에 안 잡힌다.
- 해결: 값을 단언한다(10 - 3 = 7).

고친 모양(예시):
```
jsonPath("$.orderableQuantity") { value(7) }
jsonPath("$.lockedQuantity") { value(3) }
```

### 241위 [med · 확신 high] 시험 이름은 404 인데 단언은 409(isConflict) — 이름과 기대가 어긋남

`backend/trading-api/src/test/kotlin/com/papertrading/api/presentation/controller/TradingJournalControllerIntegrationTest.kt:155` · 항목표 backend/tdd-purity · 항목 T6

```
    fun `journal account 불일치면 detail이 404를 반환한다`() {   // ← 이름은 404
...
        }.andExpect {
            status { isConflict() }   // ← 단언은 409 — 이름과 기대가 어긋남
```

- 원인: 이름은 «detail이 404를 반환한다» 인데 본문은 status { isConflict() } 를 단언합니다. 어느 쪽이 의도한 계약인지 시험만으로 알 수 없습니다. 이름이 틀렸다면 문서 역할을 못하고, 단언이 틀렸다면 계약이 잘못 굳은 것입니다.
- 생길 수 있는 일: 시험이 빨개지면 사람이 이름의 404 를 믿고 코드를 고쳐 계약이 바뀌거나, 반대로 409 가 맞는데도 404 로 되돌려 클라이언트를 깰 수 있습니다.
- 해결: 실제 계약(계좌 불일치 = 409)에 맞춰 이름을 `다른 계좌로 일지 상세를 조회하면 409를 돌려준다`로 바꾼다. 계약이 404 라면 단언과 서비스를 고친다(사람이 결정).

고친 모양(예시):
```
fun `다른 계좌로 일지 상세를 조회하면 409를 돌려준다`() {
```

### 242위 [med · 확신 med] 같은 멱등 키에 다른 요청이 오면 4xx 없이 이전 원장을 그대로 돌려줌

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/account/AccountCommandService.kt:46` · 항목표 reliability/failure-handling · 항목 I1

```
accountLedgerRepository.findByAccountIdAndIdempotencyKey(
    accountId = accountId,
    idempotencyKey = command.idempotencyKey,
)?.let { return it }  // ← 금액·거래 유형 일치 검증 없이 이전 결과 재생
// withdraw 도 동일 (62-65행)
```

- 원인: deposit·withdraw 는 (accountId, idempotencyKey) 유일 제약과 계좌 비관적 락 뒤 조회로 이중 기록은 막는다. 하지만 같은 키로 금액·종류가 다른 요청이 오면 비교 없이 기존 행을 반환한다. 입금에 쓴 키로 출금을 호출하면 출금이 성공한 것처럼 입금 원장이 반환된다(키 공간이 입출금 공통).
- 생길 수 있는 일: 클라이언트 키 재사용 실수로 의도한 출금·입금이 실행되지 않았는데 성공 응답을 받는다. 금액이 다른 재시도도 4xx 없이 무시된다.
- 해결: 기존 행의 transactionType·amount 가 요청과 다르면 ConflictException(409)으로 거절하고 같을 때만 재생한다.
- 다른 렌즈의 반증: reliability/failure-handling+load-concurrency+observability+security-config-hygiene — findByIdWithLock(PESSIMISTIC_WRITE, 락 타임아웃 3000ms, AccountRepository.kt:15-18)가 먼저 직렬화하고 (account_id, idempotency_key) 유일 제약(AccountLedger.kt:27-31)이 안전망이다

고친 모양(예시):
```
accountLedgerRepository.findByAccountIdAndIdempotencyKey(accountId, command.idempotencyKey)?.let {
    if (it.transactionType != TransactionType.DEPOSIT || it.amount.compareTo(command.amount) != 0)
        throw ConflictException("IDEMPOTENCY_KEY_REUSED", "같은 키로 다른 요청")
    return it
}
```

### 243위 [med · 확신 med] order 컨텍스트가 notification 컨텍스트의 구체 클래스를 import

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/ExecutionProcessor.kt:7` · 항목표 backend/layering · 항목 L8

```
import com.papertrading.api.application.notification.SlackNotificationEventPublisher   // ← 다른 컨텍스트의 구체 클래스
...
    private val slackNotificationEventPublisher: SlackNotificationEventPublisher,            // ← L50
```

- 원인: application.notification.SlackNotificationEventPublisher 는 port·result 가 아닌 @Component 구체 클래스이고 자신도 Spring ApplicationEventPublisher 에 의존한다(동결 위반 tools/T96587ad0.txt:93). 컨텍스트 경계가 구체 클래스로 넘어간다.
- 생길 수 있는 일: 알림 구현 변경이 체결 유스케이스를 건드리고, 체결 시험이 알림 발행기를 대체해야 한다.
- 해결: 체결 쪽은 ExecutionFilledEvent 만 발행하고 알림 컨텍스트가 그 이벤트를 구독해 Slack 요청을 만든다.

### 244위 [med · 확신 med] ExecutionProcessor 가 체결·정산·시세 구독 관리를 함께 가짐 (주인공 시험 불합격)

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/ExecutionProcessor.kt:33` · 항목표 backend/code-shape · 항목 S3

```
 * 체결 처리 단일 트랜잭션
 * Order → Execution → Position → Account → AccountLedger 순서로 처리.   // ← 존재 이유가 애그리거트 5개
@Service
class ExecutionProcessor(
...
                settlementRepository.save(                               // ← L179 정산 생성도 이 클래스
...
            collectorSubscriptionPort.unsubscribe(mode, ticker)          // ← L250 시세 구독 관리까지
```

- 원인: 클래스 KDoc 이 스스로 «Order → Execution → Position → Account → AccountLedger»를 한 클래스의 일이라고 적고, 정산(Settlement·SettlementExecution) 생성과 collector 구독 해제(tryUnsubscribe)까지 한다. public 메서드(fillLocal·fillExternal·processFill)는 모두 «체결 반영»이 존재 이유지만, 클래스가 바꾸는 이유는 체결 규칙·정산 규칙·시세 구독 정책 셋 이상이다.
- 생길 수 있는 일: 정산 규칙이나 구독 정책을 바꾸는 변경이 체결 경로를 건드려, 한 번의 수정이 돈이 오가는 트랜잭션 전체의 회귀 범위가 된다.
- 해결: 체결 반영(주문·체결 저장)과 후속 처리(정산 생성·구독 해제)를 분리해 후속 처리를 도메인 이벤트(ExecutionFilledEvent)의 리스너로 옮긴다.

### 245위 [med · 확신 med] 수수료 규칙을 서비스가 다시 구현 — 도메인 calculateFee(올림)와 반올림 방식이 다름

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/ExecutionProcessor.kt:108` · 항목표 backend/layering · 항목 D3

```
        val feeRate = feePolicy.map { it.feeRate }.orElse(BigDecimal.ZERO)       // ← 속성 2개를 읽어 서비스가 규칙을 다시 구현
        val minFee = feePolicy.map { it.minFee }.orElse(BigDecimal.ZERO)
        val rawFee = fillPrice.multiply(fillQty).multiply(feeRate).setScale(4, RoundingMode.HALF_UP)   // ← 반올림 HALF_UP
        val fee = rawFee.max(minFee)
// FeePolicy.kt:80
        val calculated = tradeAmount.multiply(feeRate).setScale(4, RoundingMode.UP)   // ← 도메인 규칙은 올림 UP, 서비스는 쓰지 않음
```

- 원인: 서비스가 FeePolicy 의 feeRate·minFee 두 속성을 읽어 수수료를 직접 계산한다. FeePolicy.calculateFee 가 같은 규칙(max(금액 × 요율, 최소수수료), scale 4)을 이미 갖고 있으나 서비스는 쓰지 않는다(grep: 시험 외 호출처 0건). 두 구현의 반올림이 다르다 — 도메인은 RoundingMode.UP, 서비스는 HALF_UP. 읽는 곳이 한 서비스뿐이라 D3 의 «2곳 이상»에는 못 미치지만 DD11(로직 누수)에 해당한다.
- 생길 수 있는 일: 운영 수수료가 도메인에 문서화된 올림 규칙과 최대 0.0001 단위로 어긋나고, FeePolicy 의 규칙을 바꿔도 운영 계산에 반영되지 않는다. 시험은 calculateFee 만 검증해 이 차이를 못 잡는다.
- 해결: 서비스는 feePolicy.calculateFee(fillPrice × fillQty)를 부르고, 정책이 없을 때의 0 처리만 서비스에 둔다.

고친 모양(예시):
```
val fee = feePolicy.map { it.calculateFee(fillPrice.multiply(fillQty)) }.orElse(BigDecimal.ZERO)
```

### 246위 [med · 확신 med] submit(order) 가 인자 order 를 바꾸는데 이름이 변경을 말하지 않음

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/KisLiveOrderExecutor.kt:26` · 항목표 backend/code-shape · 항목 F3

```
fun submit(order: Order) {
    ...
        val orno = kisOrderRestClient.placeOrder(order, "live")
        order.assignExternalOrderId(orno) // ← 인자 order 를 변경 — 이름은 submit
```

- 원인: submit 은 KIS 에 주문을 보내는 이름이지만 본문에서 인자 order 의 externalOrderId 를 대입한다(assignExternalOrderId). 호출자 OrderCommandService.placeOrder 는 이 부수효과에 기대 그 order 를 그대로 반환한다.
- 생길 수 있는 일: 호출자가 부수효과를 모르고 값을 쓰면 externalOrderId 없이 반환하거나 중복 대입으로 check 실패가 난다. 읽는 사람은 반환값이 없는 submit 이 입력을 바꾼다고 예상하지 않는다.
- 해결: submit 이 외부 주문번호를 반환하게 하고 order 변경은 호출자(OrderCommandService)가 하거나, 이름을 submitAndAssignExternalOrderId 로 바꾼다.

고친 모양(예시):
```
fun submit(order: Order): String { /* KIS 호출 후 orno 반환 */ }
// OrderCommandService
order.assignExternalOrderId(kisLiveOrderExecutor.submit(order))
```

### 247위 [med · 확신 med] KIS_LIVE 취소 실패를 WARN 로그로만 남기고 성공처럼 반환

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/KisLiveOrderExecutor.kt:51` · 항목표 reliability/failure-handling · 항목 E1

```
runCatching {
    kisOrderRestClient.cancelOrder(order, "live")
...
}.onFailure {
    log.warn { "KIS_LIVE 주문 취소 실패: orderId=$orderId, reason=${it.message}" }   // ← 실패를 삼키고 Unit 반환, 호출자는 모름
}
```

- 원인: cancel 은 runCatching 의 실패 쪽에서 log.warn 만 하고 Unit 으로 정상 반환한다. 호출자는 취소 성공과 실패를 구분할 수 없다(CE21: 잡은 예외는 다시 던지거나 실패로 읽히는 값으로). 지금은 호출처가 없어 잠복 상태지만, I3 취소 연동을 붙이면 곧바로 드러난다.
- 생길 수 있는 일: 실계좌 취소가 실패해도 호출자는 취소됐다고 믿고 로컬 상태·예수금 잠금을 풀 수 있다. 주문이 그대로 체결될 수 있다.
- 해결: 결과를 Boolean 이나 sealed 결과로 돌려주거나 예외를 다시 던진다. 불확정 응답이면 UNKNOWN 으로 남긴다.

고친 모양(예시):
```
fun cancel(order: Order): CancelOutcome =
    runCatching { kisOrderRestClient.cancelOrder(order, "live"); CancelOutcome.Ok }
        .getOrElse { log.error(it) { "cancel failed orderId=${order.id}" }; CancelOutcome.Failed }
```

### 248위 [med · 확신 med] submit(order) 가 인자 order 를 바꾸는데 이름이 변경을 말하지 않음

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/KisPaperOrderExecutor.kt:23` · 항목표 backend/code-shape · 항목 F3

```
fun submit(order: Order) {
    ...
        val orno = kisOrderRestClient.placeOrder(order, "paper")
        order.assignExternalOrderId(orno) // ← 인자 order 를 변경 — 이름은 submit
```

- 원인: KisLiveOrderExecutor 와 같은 결함이다. submit 본문이 인자 order 에 assignExternalOrderId 를 호출한다.
- 생길 수 있는 일: 호출자가 부수효과를 모르고 쓰면 externalOrderId 가 비거나 중복 대입으로 실패한다.
- 해결: submit 이 외부 주문번호를 반환하게 하고 변경은 호출자가 하거나, 이름에 변경을 드러낸다.

고친 모양(예시):
```
order.assignExternalOrderId(kisPaperOrderExecutor.submit(order))
```

### 249위 [med · 확신 med] 멱등 키 충돌이 Spring 예외 타입으로 그대로 올라감(락 전 조회 → 재확인 없음)

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/OrderCommandService.kt:51` · 항목표 data/transaction-boundary · 항목 X10

```
orderRepository.findByAccountIdAndIdempotencyKey(accountId, command.idempotencyKey)
    ?.let { return it }   // ← 락 전 조회, 락(:55) 뒤 재확인 없음
...
val order = saveOrder(account, command, lockedAmount)   // ← 동시 같은 키: DataIntegrityViolationException 이 Spring 타입 그대로 상향
```

- 원인: placeOrder 가 멱등 조회를 계좌 락(:55) 앞에서 하고 락 뒤에는 재확인하지 않습니다. 같은 키 동시 요청이 둘 다 조회를 통과하면 둘째는 save(:116, IDENTITY 즉시 INSERT)에서 uk_orders_account_idempotency 위반을 DataIntegrityViolationException 으로 받습니다. OrderRepository 는 어댑터 없이 Spring Data 라 변환이 없습니다. 묶음 밖 PositionExitTriggerOrchestrator.kt:140 이 그 Spring 타입을 직접 잡는 것이 ArchUnit 출력(T96587ad0.txt:308-309)에 있습니다.
- 생길 수 있는 일: 동시 중복 요청의 둘째는 첫 주문을 돌려받지 못하고 Spring 예외를 받습니다. 호출자마다 Spring 타입에 의존하는 분기가 늘어납니다.
- 해결: 계좌 락을 먼저 잡고 그 뒤에 멱등 조회를 하며, 어댑터(saveAndFlush)에서 제약 위반을 DuplicateOrderException 으로 바꿉니다.

고친 모양(예시):
```
val account = accountRepository.findByIdWithLock(accountId).orElseThrow { AccountNotFoundException(accountId) }
orderRepository.findByAccountIdAndIdempotencyKey(accountId, command.idempotencyKey)?.let { return it }   // 락 뒤 재확인
```

### 250위 [med · 확신 med] 같은 멱등 키에 다른 요청이 와도 기존 주문을 그대로 돌려줌

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/OrderCommandService.kt:51` · 항목표 reliability/failure-handling · 항목 I1

```
orderRepository.findByAccountIdAndIdempotencyKey(accountId, command.idempotencyKey)
    ?.let { return it }   // ← 기존 주문의 ticker·quantity·side 와 이번 command 를 비교하지 않음
```

- 원인: 키가 이미 있으면 본문을 비교하지 않고 기존 주문을 돌려준다(FH2: 같은 키 다른 본문은 4xx). 유일 제약(uk_orders_account_scope_idempotency)은 있지만 조회 뒤 삽입 사이의 경합에서는 DataIntegrityViolation 이 전역 핸들러의 500 으로 나간다. 저장 응답을 재생하는 경로가 아니다.
- 생길 수 있는 일: 수량·종목이 다른 요청이 같은 키로 오면 사용자는 성공으로 받지만 실제로는 이전 주문이 반환된다. 동시 이중 클릭은 500 이 된다.
- 해결: 기존 주문과 command 의 핵심 필드(ticker·side·type·quantity·limitPrice)가 다르면 409 로 거절한다. 유일 제약 위반은 잡아서 기존 주문 조회로 바꾼다.

고친 모양(예시):
```
orderRepository.findByAccountIdAndIdempotencyKey(accountId, key)?.let {
    check(it.matches(command)) { "같은 멱등 키에 다른 요청입니다." }  // → 409
    return it
}
```

### 251위 [med · 확신 med] application 함수가 Instant.now() 를 직접 읽음(시세 신선도 판정)

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/OrderCommandService.kt:103` · 항목표 backend/code-shape · 항목 F4

```
check(Duration.between(quote.updatedAt, Instant.now()).seconds <= 60) { // ← 시각을 직접 읽음 + 60 매직 넘버
    "시세가 오래되었습니다. (stale) ticker=${command.ticker}"
}
```

- 원인: 시장가 매수 잠금액 계산에서 시세 시각과 Instant.now() 를 직접 비교해 60초 초과를 stale 로 본다(TT16). Clock 주입이 아니라 시험이 시각을 고정할 수 없다. 실제로 stale 경로를 거는 시험이 없다(OrderCommandServiceTest 의 «stale» 시험은 시세 null 경로).
- 생길 수 있는 일: 신선도 경계(59·60·61초) 회귀를 시험으로 지킬 수 없고 환율 급변 시 오래된 시세로 잠금액이 계산돼도 알아채지 못한다.
- 해결: Clock 을 생성자로 주입하고 Instant.now(clock) 을 쓰며 60초는 이름 붙은 상수로 뺀다.
- 다른 렌즈의 반증: data/query+schema-data-model+transaction-boundary — 시세 신선도(초) 검증이지 쿼리 시각 구간이 아닙니다.

고친 모양(예시):
```
class OrderCommandService(..., private val clock: Clock) {
    private companion object { const val QUOTE_STALE_SECONDS = 60L }
    ...
    check(Duration.between(quote.updatedAt, Instant.now(clock)).seconds <= QUOTE_STALE_SECONDS) { ... }
}
```

### 252위 [med · 확신 med] 호출자가 주문 상태를 읽고 결정한 뒤 order.cancel() 호출(묻지 말고 시켜라 위반)

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/OrderCommandService.kt:243` · 항목표 backend/code-shape · 항목 S6

```
check(order.account?.id == accountId) { "해당 계좌의 주문이 아닙니다." }
check(order.orderStatus in setOf(OrderStatus.PENDING, OrderStatus.PARTIAL)) { // ← 상태 규칙을 호출자가 다시 구현 (Order.isCancellable() 존재)
    "취소 불가 상태입니다. status=${order.orderStatus}"
}
order.cancel() // ← cancel() 도 같은 검사를 함
```

- 원인: 서비스가 order.account?.id 와 order.orderStatus 를 읽어 check 로 판정한 뒤 order.cancel() 을 부르는데, Order.cancel() 이 같은 상태 검사를 이미 하고 Order.isCancellable() 도 있다(main 에서는 미사용). 같은 모양이 lockSellQuantityIfNeeded(89행: orderableQuantity 를 읽어 check 후 position.lockQuantity)에도 있고, lockQuantity 는 require 로 같은 검사를 한다.
- 생길 수 있는 일: 규칙이 두 곳에 있고 예외 타입이 갈린다(서비스 check → IllegalStateException, 도메인 require → IllegalArgumentException). 한쪽만 고치면 오류 응답이 달라진다.
- 해결: 상태 검사는 Order.cancel() 에 맡기고 서비스의 중복 check 를 지운다. 소유권 검사만 남기려면 order.isOwnedBy(accountId) 같은 메서드로 옮긴다.

고친 모양(예시):
```
check(order.isOwnedBy(accountId)) { "해당 계좌의 주문이 아닙니다." }
order.cancel() // 상태 검사는 도메인이 한다
```

### 253위 [med · 확신 med] 취소 시 잠금 해제 금액 규칙을 서비스가 직접 계산하고 도메인 메서드와 식이 다름

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/OrderCommandService.kt:251` · 항목표 backend/layering · 항목 D3

```
if (order.orderSide == OrderSide.BUY && order.lockedAmount > BigDecimal.ZERO) {
    ...
    val releasedQty = order.quantity.subtract(order.filledQuantity)
    val releaseAmount = order.lockedAmount
        .divide(order.quantity, 4, java.math.RoundingMode.HALF_UP)
        .multiply(releasedQty) // ← 해제 금액 규칙을 서비스가 직접 계산 (Order.remainingLockedAmount() 와 다른 식)
```

- 원인: Order 에는 취소 때 되돌릴 금액을 계산하는 remainingLockedAmount()(잠금 − 실체결 금액, Order.kt:158)와 isCancellable() 이 이미 있는데, main 코드는 둘 다 쓰지 않는다(참조는 OrderTest 뿐). 서비스가 lockedAmount ÷ quantity × 잔여수량 이라는 다른 식을 따로 구현한다. 가격 개선 체결(체결가 < 지정가)에서 두 식의 결과가 갈린다. ExecutionProcessor 가 체결 시 잠금을 어떻게 줄이는지는 다른 묶음이라 어느 쪽이 맞는지는 확정하지 못했다.
- 생길 수 있는 일: 같은 «취소 시 해제 금액»이 두 식으로 존재해 한쪽만 고치면 회귀가 나고, 가격 개선 체결 뒤 취소하면 계좌 잠금액이 어긋날 수 있다.
- 해결: 해제 금액 규칙을 Order 한 곳(기존 remainingLockedAmount 또는 정리한 새 메서드)으로 모으고 서비스는 그 값만 쓴다. 두 식 중 맞는 쪽을 먼저 정한다.

고친 모양(예시):
```
val releaseAmount = order.remainingLockedAmount()
if (releaseAmount > BigDecimal.ZERO) {
    accountLedgerRepository.save(account.recordBuyUnlock(releaseAmount, orderId, "cancel-unlock-$orderId"))
}
```

### 254위 [med · 확신 med] 오케스트레이터 전체가 @Transactional + 계좌 락 — REQUIRES_NEW 와 Redis 읽기가 그 안에서 돈다

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/portfolio/SnapshotJobService.kt:55` · 항목표 data/transaction-boundary · 항목 X3

```
    @Transactional                                   // ← 오케스트레이터 전체가 트랜잭션 (커넥션 1개 점유)
    fun generateDailySnapshots(
...
        val account = accountRepository.findByIdWithLock(accountId)   // ← 작업 끝까지 계좌 락
...
            snapshotJobRunTxService.start(account, businessDate)      // ← REQUIRES_NEW: 바깥을 쥔 채 두 번째 커넥션 요청
// application.yaml:13
      maximum-pool-size: 10                           // ← 풀 10개
```

- 원인: generateDailySnapshots 가 @Transactional 이라 L60 의 계좌 락을 작업 끝까지 쥔다. 그 안에서 snapshotJobRunTxService.start/complete/fail 이 REQUIRES_NEW 로 두 번째 커넥션을 쓰고, PortfolioSnapshotCommandService 는 Redis 를 읽는다. 바깥 커넥션을 쥔 채 안쪽 커넥션을 요청하는 구조라, 동시에 풀 크기(10)만큼 이 작업이 몰리면 모두 두 번째 커넥션을 기다리는 풀 고갈 교착이 가능하다.
- 생길 수 있는 일: 계좌 락이 작업 전체 길이만큼 잡히고, 동시 실행이 많아지면 커넥션 풀(10) 고갈로 30초 타임아웃 뒤 일괄 실패한다.
- 해결: 오케스트레이터에서 @Transactional 을 빼고, 실행 행 점유(start)·계산(락 + 쓰기 트랜잭션)·결과 기록(complete/fail)을 서로 독립된 트랜잭션으로 순서대로 호출한다. 계산 메서드 하나만 계좌 락을 잡는다.

고친 모양(예시):
```
// @Transactional 없음
fun generateDailySnapshots(accountId: Long, businessDate: LocalDate): Int {
    val runId = snapshotJobRunTxService.start(accountId, businessDate)
    return try {
        val n = snapshotComputeTxService.computeAndSave(accountId, businessDate) // 자체 @Transactional
        snapshotJobRunTxService.complete(runId); n
    } catch (ex: Exception) { snapshotJobRunTxService.fail(runId, ex.message); throw ex }
}
```

### 255위 [med · 확신 med] 비정상 종료로 남은 RUNNING 행을 되살리는 복구 작업이 없음

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/portfolio/SnapshotJobService.kt:69` · 항목표 reliability/failure-handling · 항목 I5

```
        val run = try {
            snapshotJobRunTxService.start(account, businessDate)   // ← RUNNING 을 먼저 커밋 — 죽으면 영구 RUNNING
        } catch (_: DataIntegrityViolationException) {
```

- 원인: start() 는 REQUIRES_NEW 로 RUNNING 행을 먼저 커밋한다. 프로세스가 complete·fail 전에 죽으면 RUNNING 이 영구히 남고 부분 유일 인덱스(uq_snapshot_job_runs_running_account_date)가 같은 계좌·영업일의 재실행을 계속 막는다. KDoc 이 timeout 기반 복구가 필요하다고 적었지만 코드 전체의 @Scheduled 는 LocalMatchingScheduler·SlackNotificationScheduler 둘뿐이고 스냅샷 복구는 없다.
- 생길 수 있는 일: 재시작·배포 중 죽은 실행의 계좌·영업일은 이후 호출이 모두 409 SNAPSHOT_ALREADY_RUNNING 으로 막혀 수동 SQL 없이는 풀리지 않는다.
- 해결: startedAt 이 임계 시간을 넘은 RUNNING 행을 FAILED 로 바꾸는 주기 작업을 두거나, start 에서 오래된 RUNNING 을 회수한다.
- 다른 렌즈의 반증: backend/code-shape+layering+naming-terminology+tdd-purity — 유일 제약 위반을 업무 예외로 번역하는 코드라 흐름 제어가 아니다. L2 로 따로 적었다.

### 256위 [med · 확신 med] 무결성 위반 예외를 cause 없이 «이미 실행 중»으로 바꿈

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/portfolio/SnapshotJobService.kt:71` · 항목표 reliability/failure-handling · 항목 E2

```
        } catch (_: DataIntegrityViolationException) {   // ← 원인 예외를 버림, 모든 무결성 위반을 «이미 실행 중»으로 단정
            throw SnapshotAlreadyRunningException(
                "이미 실행 중인 스냅샷 작업이 있습니다. accountId=${account.id} businessDate=$businessDate"
            )
```

- 원인: start() 가 던지는 DataIntegrityViolationException 은 RUNNING 유일 인덱스 위반만이 아니라 FK·NOT NULL 위반일 수도 있는데 전부 SnapshotAlreadyRunningException 으로 바뀌고 원인이 버려진다. SnapshotAlreadyRunningException(CommonApiExceptions.kt:135)은 cause 인자 자체가 없다.
- 생길 수 있는 일: 계좌 FK 위반 같은 다른 DB 오류가 «이미 실행 중 409» 로 보이고 로그에도 원인이 없어 진단이 어렵다.
- 해결: SnapshotAlreadyRunningException 에 cause 를 추가해 넘기고, 제약 이름(uq_snapshot_job_runs_running_account_date)이 맞을 때만 변환하고 나머지는 다시 던진다.
- 다른 렌즈의 반증: backend/code-shape+layering+naming-terminology+tdd-purity — 유일 제약 위반을 업무 예외로 번역하는 코드라 흐름 제어가 아니다. L2 로 따로 적었다.

고친 모양(예시):
```
} catch (ex: DataIntegrityViolationException) {
    if (!ex.isRunningUniqueViolation()) throw ex
    throw SnapshotAlreadyRunningException("이미 실행 중 ...", ex)
}
```

### 257위 [med · 확신 med] SUCCESS 기록이 본 쓰기 커밋보다 먼저 커밋된다

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/portfolio/SnapshotJobService.kt:86` · 항목표 data/transaction-boundary · 항목 X4

```
        return try {
            dailyBalanceCommandService.recalculate(accountId, businessDate)
...
            snapshotJobRunTxService.complete(runId)   // ← REQUIRES_NEW 로 SUCCESS 를 먼저 커밋 (바깥 트랜잭션은 아직 커밋 전)
            snapshots.size
// L65
        if (latestRun?.status == SnapshotJobRunStatus.SUCCESS) {   // ← 다음 호출은 SUCCESS 만 보고 재계산을 생략
            return 0
```

- 원인: complete(runId)는 REQUIRES_NEW 라 호출 즉시 SUCCESS 가 독립 커밋된다. 그런데 DailyBalance·PortfolioSnapshot 쓰기를 담은 바깥 트랜잭션의 커밋은 메서드가 반환된 뒤(프록시)에 일어난다. 기존 행을 refresh 하는 경로는 UPDATE 가 커밋 시점 flush 로 나가므로 그때 실패(수치 범위 초과·연결 끊김 등)하면 데이터는 롤백되고 SUCCESS 만 남는다. L65 는 마지막 실행이 SUCCESS 면 재계산을 생략한다.
- 생길 수 있는 일: 커밋 시점 실패 한 번이면 해당 계좌·일자는 SUCCESS 로 기록된 채 스냅샷이 비거나 낡아, 이후 재실행이 계속 0 을 반환하고 영구히 건너뛴다.
- 해결: X3 의 구조로 바꿔 계산·저장 트랜잭션이 커밋된 뒤에 complete 를 호출한다. 그러면 SUCCESS 는 커밋 성공 이후에만 기록된다.

고친 모양(예시):
```
val n = snapshotComputeTxService.computeAndSave(accountId, businessDate) // 이 호출이 반환되면 이미 커밋됨
snapshotJobRunTxService.complete(runId)
```

### 258위 [med · 확신 med] SUCCESS 표시가 스냅샷 데이터 커밋보다 먼저 확정됨

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/portfolio/SnapshotJobService.kt:86` · 항목표 reliability/failure-handling · 항목 I5

```
            snapshotJobRunTxService.complete(runId)   // ← REQUIRES_NEW 로 SUCCESS 가 먼저 커밋
            snapshots.size                            // ← 바깥 트랜잭션은 반환 뒤에야 커밋
```

- 원인: complete(runId) 는 REQUIRES_NEW 라 즉시 커밋되지만 일별 잔고·스냅샷 쓰기는 바깥 @Transactional 이 메서드 반환 뒤에야 커밋한다. 그 사이 바깥 커밋이 실패하거나 프로세스가 죽으면 run 은 SUCCESS 인데 데이터는 없다. 재호출은 latestRun SUCCESS 를 보고 0 을 반환해 그대로 건너뛴다.
- 생길 수 있는 일: 해당 계좌·영업일의 스냅샷이 조용히 영구 누락된다. 재실행 정책이 SUCCESS 를 믿고 건너뛰기 때문이다.
- 해결: complete 를 바깥 트랜잭션 커밋 이후(TransactionSynchronization afterCommit 또는 오케스트레이션 메서드의 @Transactional 밖)로 옮겨 처리 표시와 효과를 같은 성공 순간에 맞춘다.

### 259위 [med · 확신 med] 출력 포트가 application/portfolio/port 밖(tax 패키지)에 있음

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/portfolio/tax/SettlementTaxReadRepository.kt:9` · 항목표 backend/layering · 항목 L3

```
package com.papertrading.api.application.portfolio.tax  // ← port 패키지가 아님
interface SettlementTaxReadRepository {  // ← infrastructure 가 구현하는 출력 포트
    fun summarizeForTax(accountId: Long, yearStartInclusive: Instant, yearEndExclusive: Instant): TaxSettlementAggregate  // ← 결과 타입도 port 밖
```

- 원인: infrastructure 가 구현하는 인터페이스가 application.portfolio.tax 에 있어 PTB8 의 port 패키지 규칙을 벗어난다(ArchUnit 동결 269행). 시그니처 반환 타입도 port 밖 타입이다.
- 생길 수 있는 일: infrastructure 가 port 밖 application 패키지를 import 하게 되어 PTB7 이 깨진다.
- 해결: application.portfolio.port 로 옮기고 결과 타입도 같은 패키지로 옮긴다.
- 다른 렌즈의 반증: backend/code-shape+layering+naming-terminology+tdd-purity — DB 를 감싸는 포트(구현 SettlementTaxReadRepositoryImpl 은 infrastructure/persistence)라 S9 에서 제외. 포트 위치 문제는 L3 로 적었다.

고친 모양(예시):
```
package com.papertrading.api.application.portfolio.port
interface SettlementTaxReadPort { fun summarizeForTax(...): TaxSettlementResult }
```

### 260위 [med · 확신 med] 연말 일괄 루프가 실패 항목·성공 개수를 남기지 않고 첫 실패에서 전체 중단

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/portfolio/TaxSummaryBatchService.kt:31` · 항목표 reliability/failure-handling · 항목 E7

```
        targetAccountIds.forEach { accountId ->   // ← try/catch 도 실패 기록도 없음 — 한 계좌 실패가 나머지를 중단
            taxSummaryCommandService.recalculate(
```

- 원인: 계좌별 recalculate 는 각자 트랜잭션이라 앞 계좌는 커밋되지만 중간 계좌가 예외를 던지면 forEach 가 끝나고 호출이 500 으로 끝난다. 어느 계좌까지 성공했는지는 응답에도 로그에도 없고 TaxSummaryRun FAILED 행도 롤백된다(O8 발견). 활성 계좌 전체를 요청 스레드에서 순차 처리하므로 HTTP 타임아웃이 나면 진행 상태를 알 수 없다. 재실행은 upsert 라 안전하다.
- 생길 수 있는 일: 계좌 하나가 실패하면 뒤 계좌가 계산되지 않고, 운영자는 어디까지 됐는지 모른 채 전체를 다시 돌려야 한다.
- 해결: 계좌별 try/catch 로 실패 id·사유를 모아 로그(WARN 이상)와 응답에 싣고 나머지는 계속한다. 계좌 수가 많으면 요청 경로 밖(비동기 작업)으로 옮긴다.
- 다른 렌즈의 반증: data/query+schema-data-model+transaction-boundary — 계좌마다 독립 트랜잭션·계좌 락이 필요한 단위 작업이라 일괄 읽기로 바꿀 수 없다. 루프가 부르는 것은 리포지토리가 아니라 유스케이스다. 대신 X12 로 보고했다.

고친 모양(예시):
```
val failed = mutableListOf<Long>()
targetAccountIds.forEach { id ->
    try { taxSummaryCommandService.recalculate(id, taxYear, true, YEAR_END_BATCH) }
    catch (ex: ApiDomainException) { log.warn(ex) { "세금 요약 실패 accountId=$id" }; failed += id }
}
```

### 261위 [med · 확신 med] 무결성 위반 예외를 cause 없이 «이미 실행 중»으로 바꿈(detekt SwallowedException)

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/portfolio/TaxSummaryCommandService.kt:68` · 항목표 reliability/failure-handling · 항목 E2

```
        } catch (ex: DataIntegrityViolationException) {   // ← ex 를 쓰지 않고 버림
            throw TaxSummaryAlreadyRunningException(
                "이미 실행 중인 세금 요약 작업이 있습니다. accountId=$accountId taxYear=${taxYear.value}"
            )
```

- 원인: taxSummaryRunRepository.save 의 DataIntegrityViolationException 을 전부 TaxSummaryAlreadyRunningException 으로 바꾸고 원인을 버린다. detekt 기준선도 SwallowedException(ex: DataIntegrityViolationException)으로 이 자리를 보고한다. TaxSummaryAlreadyRunningException 도 cause 인자가 없다.
- 생길 수 있는 일: FK·NOT NULL 같은 다른 DB 오류가 «이미 실행 중 409» 로 오인되고 원인이 사라진다.
- 해결: 예외 클래스에 cause 인자를 추가해 넘기고, 유일 제약 위반만 변환하고 나머지는 다시 던진다.
- 다른 렌즈의 반증: data/query+schema-data-model+transaction-boundary — 변환한 예외를 바로 던져 대체 경로로 진행하지 않는다. Spring 타입 노출은 X10 으로 보고했다.

### 262위 [med · 확신 med] 유스케이스가 AccountRepository·TradingJournalRepository 를 직접 주입받음

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/portfolio/TradingJournalCommandService.kt:10` · 항목표 backend/layering · 항목 L1

```
import com.papertrading.api.infrastructure.persistence.AccountRepository  // ← application → infrastructure
import com.papertrading.api.infrastructure.persistence.TradingJournalRepository
    private val tradingJournalRepository: TradingJournalRepository  // ← 직접 주입
```

- 원인: JpaRepository 확장 둘을 직접 주입받는다(PTB7·PTB8). 단순 CRUD 컨텍스트라 PTB1 예외와 겹쳐 ruleConflicts 에도 적는다. ArchUnit 동결 4건.
- 생길 수 있는 일: 단순 CRUD 라도 PTB8 에 CRUD 예외가 없어 이 파일이 규칙 위반으로 남는다.
- 해결: 단순 CRUD 로 둘지(규칙 예외를 사람이 명시) 포트로 옮길지 정한다.

고친 모양(예시):
```
interface TradingJournalPort {
    fun save(journal: TradingJournal): TradingJournal
    fun findById(journalId: Long): TradingJournal?
}
```

### 263위 [med · 확신 med] update 가 command.accountId 를 쓰지 않아 소유권 검사가 빠짐(조회 쪽은 belongsTo 로 검사)

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/portfolio/TradingJournalCommandService.kt:37` · 항목표 backend/layering · 항목 D3

```
8: import com.papertrading.api.common.exception.TradingJournalOwnershipMismatchException  // ← 쓰이지 않는 import
37: fun update(journalId: Long, command: UpdateTradingJournalCommand): TradingJournalResult {
38:     val journal = tradingJournalRepository.findById(journalId)
41:     journal.update(command.title, command.content, command.sentiment)  // ← command.accountId 는 쓰지 않음 — 소유 검사 없음
```

- 원인: TradingJournal 에는 belongsTo(accountId) 가 있고 TradingJournalQueryService.get 이 그것으로 소유권을 검사하지만, update 는 command 에 accountId 가 있어도(컨트롤러가 넘김) 검사 없이 수정한다. 필요 없어진 TradingJournalOwnershipMismatchException import(8행)가 남아 있다. 시험이 이 동작을 의도로 못 박아 두었으나(`update는 accountId와 무관하게 …`) 계좌 소유 검사 누락일 가능성이 크다.
- 생길 수 있는 일: journalId 만 알면 다른 계좌의 일지를 수정할 수 있다. 인증이 없는 구조라면 검사의 실효는 낮지만 규칙이 서비스마다 갈려 한 곳이 빠졌다.
- 해결: journal.update 가 accountId 를 받아 소유를 검사하게 하거나(묻지 말고 시켜라) 서비스에서 belongsTo 로 검사한다.

고친 모양(예시):
```
fun update(journalId: Long, command: UpdateTradingJournalCommand): TradingJournalResult {
    val journal = tradingJournalRepository.findById(journalId).orElseThrow { TradingJournalNotFoundException(journalId) }
    if (!journal.belongsTo(command.accountId)) throw TradingJournalOwnershipMismatchException(journalId, command.accountId)
    journal.update(command.title, command.content, command.sentiment)
    return TradingJournalResult.from(journal)
}
```

### 264위 [med · 확신 med] 조회 유스케이스가 AccountRepository·TradingJournalRepository 를 직접 주입받음

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/portfolio/TradingJournalQueryService.kt:8` · 항목표 backend/layering · 항목 L1

```
import com.papertrading.api.infrastructure.persistence.AccountRepository  // ← application → infrastructure
import com.papertrading.api.infrastructure.persistence.TradingJournalRepository
    private val tradingJournalRepository: TradingJournalRepository  // ← 직접 주입
```

- 원인: JpaRepository 확장 둘을 직접 주입받는다(PTB7·PTB8). 단순 CRUD 컨텍스트라 ruleConflicts 에도 적는다. ArchUnit 동결 4건.
- 생길 수 있는 일: CRUD 예외 여부가 불분명해 이 파일의 위반이 해소되지 않는다.
- 해결: 단순 CRUD 예외를 사람이 정한 뒤 포트로 옮기거나 예외를 문서화한다.

고친 모양(예시):
```
interface TradingJournalReadPort {
    fun search(filter: TradingJournalFilter, page: PageQuery): PageResult<TradingJournal>
}
```

### 265위 [med · 확신 med] 유스케이스가 org.springframework.dao 예외 3종을 import 해 잡는다

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/position/PositionExitTriggerOrchestrator.kt:13` · 항목표 backend/layering · 항목 L2

```
import org.springframework.dao.CannotAcquireLockException  // ←
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.dao.PessimisticLockingFailureException
...
        } catch (ex: CannotAcquireLockException) {  // L45
            markCandidatesSkipped(candidates, quoteAt, price, TriggerSkipReason.LOCK_CONFLICT)
```

- 원인: CannotAcquireLockException·DataIntegrityViolationException·PessimisticLockingFailureException 은 허용 목록 밖이다. L45 의 두 catch 는 같은 @Transactional onQuote 안에서 findByIdWithLock 이 던진 예외를 잡고 이어서 saveAll 을 부른다. 참여 중인 트랜잭션의 리포지토리 예외는 트랜잭션을 rollback-only 로 표시하고 PostgreSQL 은 오류 뒤 같은 트랜잭션의 다음 명령을 거부한다. L155 의 DataIntegrityViolationException catch 는 REQUIRES_NEW 래퍼 호출을 감싸 바깥 트랜잭션을 오염시키지 않아 반증했다(disproved 참조). 잠금 타임아웃 설정은 열어보지 않아 확신을 낮췄고 transaction-boundary 렌즈와 겹친다.
- 생길 수 있는 일: 잠금 충돌 때 skip 으로 기록하려던 시도가 커밋 시 UnexpectedRollbackException 이 되거나 그 틱의 모든 그룹 처리가 롤백된다.
- 해결: 영속 예외는 어댑터가 애플리케이션 예외(PositionLockConflictException 등)로 바꿔 던지게 하고, 그룹 단위 처리는 REQUIRES_NEW 유스케이스로 나눠 예외가 바깥 트랜잭션을 건드리지 않게 한다.

### 266위 [med · 확신 med] 청산 판정 틱의 소요·처리 건수·시세 지연이 계측되지 않음

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/position/PositionExitTriggerOrchestrator.kt:36` · 항목표 reliability/observability · 항목 O6

```
@Transactional
fun onQuote(ticker: String, price: BigDecimal, quoteAt: Instant) {   // ← 소요·건수·지연 계측 0개
    val normalizedTicker = ticker.uppercase()
    val groups = triggerRepository.findByTickerAndState(normalizedTicker, TriggerState.ARMED)
```

- 원인: `onQuote` 는 틱마다 ARMED 트리거 수·그룹 수·발동 건수·소요 시간·`now − quoteAt` 지연 어느 것도 로그·Timer·Counter 로 남기지 않는다(파일에 로그·Micrometer 사용 0건). 놓친 시세 구간(PTA2)과 느린 틱을 알 방법이 없다.
- 생길 수 있는 일: 손절 판정 지연·적체가 장애가 될 때까지 보이지 않는다.
- 해결: Micrometer Timer 로 틱 소요를, Counter 로 평가·발동·skip 건수를, `Duration.between(quoteAt, now)` 로 시세 지연을 기록한다.

고친 모양(예시):
```
val sample = Timer.start(meterRegistry)
try { ... } finally { sample.stop(meterRegistry.timer("exit_trigger.tick", "ticker", normalizedTicker)) }
meterRegistry.summary("exit_trigger.quote_lag_ms").record(Duration.between(quoteAt, Instant.now()).toMillis().toDouble())
```

### 267위 [med · 확신 med] 가격 틱마다 ARMED 트리거 전부를 잠금 조회하고 미발동 틱도 UPDATE

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/position/PositionExitTriggerOrchestrator.kt:39` · 항목표 reliability/load-concurrency · 항목 C2

```
val groups = triggerRepository.findByTickerAndState(normalizedTicker, TriggerState.ARMED)   // ← 종목의 ARMED 전부, 상한·사전 필터 없음
...
val decision = evaluator.evaluate(position, trigger, price, quoteAt)
if (decision == null) trigger.recordEvaluation(quoteAt, price)   // ← 발동하지 않는 틱마다 상태 변경
...
if (decisions.isEmpty()) {
    triggerRepository.saveAll(triggers)   // ← 매 틱 UPDATE
    return
}
```

- 원인: 틱마다 해당 종목의 ARMED 트리거를 상한 없이 모두 읽고, 그룹마다 포지션·트리거 FOR UPDATE 두 번을 건 뒤에야 임계값을 평가한다. 발동하지 않으면 recordEvaluation 후 `saveAll(triggers)` 로 매 틱 쓴다(recordEvaluation 본문은 미열람 — 값이 바뀐다면 @Version 도 매 틱 증가해 expectedVersion 낙관 검사가 계속 어긋난다). 비용이 변경이 아니라 틱 수×트리거 수에 비례한다.
- 생길 수 있는 일: 활발한 종목에 트리거가 늘수록 틱당 쓰기·행 잠금이 비례해 늘어나고, 사용자의 트리거 수정·취소 요청과 포지션 잠금을 두고 경합한다.
- 해결: 잠금 없이 시세와 임계값만 비교해 발동 후보만 추린 뒤 후보 그룹에만 잠금을 걸고, 미발동 평가 기록은 쓰지 않거나 변화가 있을 때만 쓴다.

고친 모양(예시):
```
val fired = armed.filter { evaluator.isCrossed(it, price) }   // 잠금 없는 사전 판정
fired.groupBy { TriggerGroupKey(it.positionId, it.triggerType) }.forEach { (key, _) ->
    groupProcessor.process(ticker, key, price, quoteAt)   // 후보 그룹만 FOR UPDATE
}
```

### 268위 [med · 확신 med] 잠금 충돌 skip 경로에 로그 0줄 — 파일 전체에 로거 없음

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/position/PositionExitTriggerOrchestrator.kt:45` · 항목표 reliability/observability · 항목 O1

```
} catch (ex: CannotAcquireLockException) {
    markCandidatesSkipped(candidates, quoteAt, price, TriggerSkipReason.LOCK_CONFLICT)   // ← 로그 없음, 예외 객체 버림 (detekt SwallowedException)
} catch (ex: PessimisticLockingFailureException) {
    markCandidatesSkipped(candidates, quoteAt, price, TriggerSkipReason.LOCK_CONFLICT)
}
```

- 원인: 잠금 예외 두 종류를 잡아 트리거에 LOCK_CONFLICT 로 skip 만 표시하고 예외 객체를 버린다(detekt SwallowedException 2건, manifest Tb661de0c). 로그 grep 결과 이 파일에는 로그 호출이 0줄이며, 3회 재시도 중간 실패도 같다. 트리거 행 상태·Slack 알림 말고는 검색할 줄이 없다.
- 생길 수 있는 일: 락 경합·교착으로 청산 판정이 건너뛰어지는 빈도를 로그로 셀 수 없어 지연된 손절의 원인을 사후에 되짚을 수 없다.
- 해결: 각 catch 에서 positionId·triggerType·종목을 담아 log.warn(ex) 를 남긴다.
- 다른 렌즈의 반증: backend/code-shape+layering+naming-terminology+tdd-purity — 한 식으로 검사 가능한 조건을 예외로 대신하는 모양이 아니라 DB 잠금 실패에 대한 처리다. 트랜잭션 오염 문제는 L2 로 따로 올렸다.

고친 모양(예시):
```
} catch (ex: PessimisticLockingFailureException) {
    log.warn(ex) { "exit trigger group skipped (lock conflict): ticker=$normalizedTicker, positionId=${groupKey.positionId}, type=${groupKey.triggerType}" }
    markCandidatesSkipped(candidates, quoteAt, price, TriggerSkipReason.LOCK_CONFLICT)
}
```

### 269위 [med · 확신 med] processGroup 본문이 빈 줄 제외 약 57줄로 40줄을 넘고 추상화 수준이 셋이다

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/position/PositionExitTriggerOrchestrator.kt:53` · 항목표 backend/code-shape · 항목 S1

```
private fun processGroup(ticker: String, groupKey: TriggerGroupKey, price: BigDecimal, quoteAt: Instant) {  // ← 본문 L54~L119, 빈 줄 제외 약 57줄
    synchronized(positionLocks.computeIfAbsent(groupKey.positionId) { Any() }) {
        ...
        val orderQuantity = calculateOrderQuantity(availableQuantity, decisions)
        ...
        val created = tryCreateGroupedAutoExitOrderWithRetry(
        ...
            firedTriggers.forEach { it.markTriggered(quoteAt, price) }
```

- 원인: 본문 L54~L119 중 빈 줄 9개를 빼도 약 57줄이다. 한 함수 안에 락 잡기, 폐쇄 포지션 건너뛰기, 트리거 평가, 수동 매도 충돌 검사, 잔여 수량 산정, 주문 생성 위임, 상태 기록이 이어진다. detekt 의 LongMethod·CyclomaticComplexMethod 기준선에도 이 함수가 올라 있다(tools/Tb661de0c.txt). 중첩은 3단이라 S2 는 통과한다.
- 생길 수 있는 일: 건너뛰기 사유가 늘 때마다 같은 함수에 분기가 붙고, 분기마다 triggerRepository.saveAll 을 따로 불러야 해서 저장 누락(상태가 ARMED 로 남아 다음 틱에 같은 주문이 다시 나감)이 생기기 쉽다. 단위 시험도 경로 곱만큼 필요하다.
- 해결: 건너뛰기 판정(폐쇄 포지션·수동 매도 충돌·잔여 수량)을 skipReasonOf(...) 로 빼 사유 또는 null 을 돌려주게 하고, 평가 단계와 주문 단계를 각각 함수로 추출한다. saveAll 은 한 곳에서만 부른다.
- 다른 렌즈의 반증: data/query+schema-data-model+transaction-boundary — findArmedGroupForUpdate·findByIdWithLock 으로 PESSIMISTIC_WRITE 점유를 먼저 잡는다. 롤백 시 중복 발동 위험은 X12 로 보고함

고친 모양(예시):
```
private fun processGroup(...) = synchronized(lockFor(groupKey.positionId)) {
    val ctx = loadGroupContext(groupKey) ?: return
    val skip = skipReasonOf(ctx, price)
    if (skip != null) return markTriggersSkipped(ctx.firedTriggers, quoteAt, price, skip)
    placeOrderAndMark(ctx, price, quoteAt)
}
```

### 270위 [med · 확신 med] updateRiskPolicy 가 인자(existing)를 바꾸면서 새 객체를 돌려줌

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/account/Account.kt:238` · 항목표 backend/code-shape · 항목 F3

```
fun updateRiskPolicy(
    existing: RiskPolicy,
    ...
): RiskPolicy {
    existing.deactivate() // ← 인자를 바꾸고
    return createRiskPolicy(maxPositionRatio, maxDailyLoss, maxOrderAmount) // ← 새 객체를 반환
```

- 원인: existing.deactivate() 로 인자를 비활성화하고 새 RiskPolicy 를 반환한다. 호출자가 반환값만 저장하면 기존 정책의 변경(비활성화)은 영속 컨텍스트의 더티 체킹에 암묵적으로 의존한다.
- 생길 수 있는 일: 반환값만 보고 existing 을 저장·분리하는 호출자는 비활성화를 놓쳐 활성 정책이 둘이 될 수 있다.
- 해결: Account 가 활성 정책 하나를 직접 보관·교체하거나, 비활성화된 기존·새 정책을 모두 돌려주는 결과 타입(RiskPolicyChange(old, new))을 쓴다.

### 271위 [med · 확신 med] Account 가 청산 트리거 기본값의 규칙·쓰기까지 가짐(주인공이 둘)

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/account/Account.kt:270` · 항목표 backend/code-shape · 항목 S3

```
fun createExitTriggerDefault( // ← 다른 애그리거트의 생성
    enabled: Boolean, stopLossPercent: BigDecimal?, takeProfitPercent: BigDecimal?,
): AccountExitTriggerDefault {
    validateExitTriggerDefault(enabled, stopLossPercent, takeProfitPercent)
private fun validatePercentRange(value: BigDecimal?) { // ← 다른 애그리거트의 값 규칙
```

- 원인: Account 의 존재 이유는 예수금 흐름인데 createExitTriggerDefault·updateExitTriggerDefault·validateExitTriggerDefault·validatePercentScale·validatePercentRange 가 AccountExitTriggerDefault 의 불변식과 필드 쓰기를 맡는다. AccountExitTriggerDefault 는 별도 테이블·리포지토리라 다른 애그리거트다.
- 생길 수 있는 일: 청산 트리거 규칙을 바꾸려면 계좌 엔티티를 고쳐야 해 예수금 로직과 변경 이유가 섞인다.
- 해결: 검증·변경을 AccountExitTriggerDefault 로 옮기고 Account 는 소유자 id 확인만 둔다(D2·D5 와 함께 해결).

### 272위 [med · 확신 med] updateExitTriggerDefault 가 인자(existing)를 바꾸면서 같은 객체를 돌려줌

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/account/Account.kt:285` · 항목표 backend/code-shape · 항목 F3

```
fun updateExitTriggerDefault(
    existing: AccountExitTriggerDefault,
    ...
): AccountExitTriggerDefault {
    existing.enabled = enabled // ← 인자를 변경
    return existing // ← 변경한 인자를 반환
```

- 원인: existing 의 필드를 직접 바꾸고 existing 을 반환한다. 서비스는 반환값을 save 하므로 인자 변경과 반환이 한 시그니처에 섞여 호출자가 사본으로 오해할 수 있다.
- 생길 수 있는 일: 호출자 객체가 등 뒤에서 바뀐다는 사실이 시그니처에 드러나지 않는다.
- 해결: AccountExitTriggerDefault 의 change(...) 메서드(반환 없음)로 바꾸고 서비스가 그 객체를 저장한다.

### 273위 [med · 확신 med] Account 가 다른 객체(existing)의 속성을 읽고 판단한 뒤 그 객체의 필드를 직접 씀

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/account/Account.kt:291` · 항목표 backend/code-shape · 항목 S6

```
val accountId = id ?: throw EntityMappingException("account id가 없습니다.")
if (existing.accountId != accountId) { // ← 남의 속성을 읽어 판단
    throw ConflictException(...)
}
validateExitTriggerDefault(enabled, stopLossPercent, takeProfitPercent)
existing.enabled = enabled // ← 남의 필드에 직접 대입
existing.stopLossPercent = stopLossPercent
existing.takeProfitPercent = takeProfitPercent
```

- 원인: updateExitTriggerDefault 가 existing.accountId 를 읽어 소유자를 비교하고, 검증 후 existing.enabled·stopLossPercent·takeProfitPercent 를 직접 대입한다. 결정은 AccountExitTriggerDefault 의 것이어야 한다(PTB2).
- 생길 수 있는 일: 변경 규칙이 호출 쪽에 있어 다른 호출자가 같은 규칙을 다시 구현하거나 빠뜨린다.
- 해결: existing.changeBy(ownerAccountId, enabled, stopLossPercent, takeProfitPercent) 처럼 모델이 스스로 소유자 확인·검증·변경을 한다.

### 274위 [med · 확신 med] 원장 목록의 정렬·기간 열(created_at)에 계좌별 복합 인덱스가 없음

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/account/AccountLedger.kt:25` · 항목표 data/schema-data-model · 항목 M4

```
@Table(
    name = "account_ledger",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_account_ledger_account_id_idempotency",   // ← 유일 제약뿐, (account_id, created_at) 인덱스 없음
            columnNames = ["account_id", "idempotency_key"]
        )
    ]
)
// AccountLedgerRepositoryImpl.kt:37
.orderBy(ledger.createdAt.desc())
```

- 원인: account_id 는 유일 제약 (account_id, idempotency_key) 의 선두 열이라 필터는 덮인다. 그러나 목록 조회는 account_id 필터에 created_at 구간·내림차순 정렬을 쓰는데 (account_id, created_at) 인덱스가 없다(@Table 에 indexes 없음, 마이그레이션에 account_ledger DDL 없음). 계좌의 원장 전부를 읽어 정렬한다.
- 생길 수 있는 일: 계좌 원장이 쌓일수록 목록·건수 조회가 선형으로 느려진다.
- 해결: (account_id, created_at DESC) 인덱스를 추가한다.

고친 모양(예시):
```
@Table(name = "account_ledger", indexes = [Index(name = "idx_account_ledger_account_created", columnList = "account_id, created_at DESC")], uniqueConstraints = [ /* 기존 */ ])
```

### 275위 [med · 확신 med] 원장이 Account 애그리거트 루트 객체를 직접 참조

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/account/AccountLedger.kt:40` · 항목표 backend/layering · 항목 D4

```
@ManyToOne(fetch = FetchType.LAZY, optional = false) // ← 다른 애그리거트 루트로의 연관
@JoinColumn(name = "account_id", nullable = false)
lateinit var account: Account
    private set
```

- 원인: AccountLedger 는 자기 리포지토리(AccountLedgerRepository)와 조회 라우트(AccountQueryService.getLedgers)로 따로 다뤄지므로 부모의 자식이 아니라 루트다. 그런데 @ManyToOne 으로 Account 객체를 들고 있다. 같은 패키지의 AccountExitTriggerDefault 는 accountId 로만 참조해 대비된다.
- 생길 수 있는 일: 원장 한 건을 읽으면 Account 가 딸려 오고(지연 로딩 프록시), 한 트랜잭션에서 두 애그리거트가 함께 바뀔 수 있다.
- 해결: AccountLedger 는 accountId: Long 만 보관하고 Account 와는 id 로만 연결한다.

고친 모양(예시):
```
@Column(name = "account_id", nullable = false)
val accountId: Long
```

### 276위 [med · 확신 med] 리스크 이벤트가 Account·RiskPolicy 루트 객체를 직접 참조

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/account/RiskEvent.kt:28` · 항목표 backend/layering · 항목 D4

```
@ManyToOne(fetch = FetchType.LAZY, optional = false)
@JoinColumn(name = "account_id", nullable = false)
lateinit var account: Account // ← 루트 객체 참조
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "risk_policy_id")
var riskPolicy: RiskPolicy? = null // ← 루트 객체 참조
```

- 원인: RiskEvent 가 @ManyToOne 으로 Account 와 RiskPolicy 두 루트를 객체로 들고 있다. 이벤트 이력은 id 만 알면 충분하다.
- 생길 수 있는 일: 이력 한 건 조회가 계좌·정책을 끌어오고, 이력 생성이 두 애그리거트에 걸친 변경으로 번질 수 있다.
- 해결: accountId: Long, riskPolicyId: Long? 로 바꾼다.

### 277위 [med · 확신 med] 알림 재시도 스캔 열(status·attempt_count)과 FK(account_id)에 인덱스가 없음

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/notification/Notification.kt:35` · 항목표 data/schema-data-model · 항목 M4

```
@Entity
@Table(name = "notifications")   // ← 인덱스 없음
...
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "account_id")   // ← FK 인덱스 없음
var account: Account? = null
...
@Column(name = "status", nullable = false, length = 20)   // ← 스케줄러 필터 열, 인덱스 없음
var status: DeliveryStatus = DeliveryStatus.PENDING
```

- 원인: 스케줄러가 30초마다 status=PENDING AND attempt_count<max 로 조회하는데 notifications 에는 인덱스가 하나도 없다(@Table 에 indexes 없음). 알림은 이벤트마다 한 행씩 쌓이고 지워지지 않아 테이블이 계속 자란다. account_id FK 도 어떤 인덱스의 선두 열이 아니다.
- 생길 수 있는 일: 테이블이 커질수록 30초마다 전체 스캔이 반복되어 DB 부하가 상시 늘어난다.
- 해결: (status, attempt_count) 또는 PENDING 행만 덮는 부분 인덱스와 account_id 인덱스를 추가한다.

고친 모양(예시):
```
@Table(name = "notifications", indexes = [Index(name = "idx_notifications_status_attempt", columnList = "status, attempt_count"), Index(name = "idx_notifications_account", columnList = "account_id")])
```

### 278위 [med · 확신 med] 상태 전이 메서드가 이전 상태를 검사하지 않는다

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/notification/Notification.kt:95` · 항목표 backend/layering · 항목 D2

```
fun markDelivered(): Notification {
    status = DeliveryStatus.DELIVERED   // ← 이전 상태 검사 없음 (FAILED·PENDING 에서도 DELIVERED)
    deliveredAt = Instant.now()
    return this
}

fun markFailed(error: String, maxRetries: Int): Notification {
    attemptCount++
    lastError = error
    status = if (attemptCount < maxRetries) DeliveryStatus.PENDING else DeliveryStatus.FAILED   // ← DELIVERED 인 알림도 PENDING 으로 되돌림
```

- 원인: markDelivering·markDelivered·markFailed 가 현재 status 를 보지 않고 대입한다. PTB2 는 이전 상태를 검사하는 이름 붙은 메서드를 요구한다. FAILED 에서 DELIVERED 로, DELIVERED 에서 다시 PENDING 으로 가는 전이도 막지 않는다. 시험도 허용 전이만 확인하고 거부 전이는 없다.
- 생길 수 있는 일: 경합이나 호출 실수로 늦게 도착한 markFailed 가 이미 DELIVERED 인 알림을 PENDING 으로 되돌리면 스케줄러가 다시 집어 Slack 에 중복 전송할 수 있다.
- 해결: 각 전이 메서드가 허용된 이전 상태(DELIVERING 만 DELIVERED·실패로 갈 수 있음 등)를 check 로 검사하게 하고, 거부 전이 시험을 추가한다.

고친 모양(예시):
```
fun markDelivered(deliveredAt: Instant) {
    check(status == DeliveryStatus.DELIVERING) { "DELIVERING 에서만 DELIVERED 로 갈 수 있습니다: $status" }
    status = DeliveryStatus.DELIVERED
    this.deliveredAt = deliveredAt
}
```

### 279위 [med · 확신 med] executions 의 FK 열(order_id·account_id)과 정렬 열(executed_at)에 인덱스가 없음

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/order/Execution.kt:27` · 항목표 data/schema-data-model · 항목 M4

```
@Table(
    name = "executions",
    uniqueConstraints = [UniqueConstraint(name = "uk_executions_external_id", columnNames = ["external_execution_id"])]
)   // ← indexes 없음 — order_id·account_id FK 와 executed_at 인덱스 없음
```

- 원인: @Table 은 uniqueConstraints 만 있고 indexes 가 없으며 V2~V12 마이그레이션에도 executions 인덱스가 없습니다. findByOrderId·searchExecutions(account_id, order_id, executed_at 정렬)가 이 열을 씁니다. application.yaml:19 가 ddl-auto: create 라 마이그레이션 적용 여부도 불확실합니다(M12 인스턴스 판정). DB 를 보지 못해 confidence 는 med 입니다.
- 생길 수 있는 일: 체결이 쌓이면 주문별 체결 조회와 부모 갱신이 executions 전체 스캔이 됩니다.
- 해결: (order_id, executed_at) 와 (account_id, executed_at) 인덱스를 추가하고 마이그레이션 파일로도 만듭니다.

고친 모양(예시):
```
@Table(name = "executions", indexes = [
    Index(name = "idx_executions_order_executed", columnList = "order_id, executed_at"),
    Index(name = "idx_executions_account_executed", columnList = "account_id, executed_at")
], uniqueConstraints = [...])
```

### 280위 [med · 확신 med] GTD 주문의 expireAt 필수 불변식이 팩토리에 없고 서비스에만 있음

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/order/Order.kt:181` · 항목표 backend/layering · 항목 D5

```
            require(orderType != OrderType.LIMIT || limitPrice != null) { "지정가 주문은 가격이 필요합니다." }
            require(limitPrice == null || limitPrice > BigDecimal.ZERO) { "주문 가격은 0보다 커야 합니다." }
            require(lockedAmount >= BigDecimal.ZERO) { "잠금 금액은 0 이상이어야 합니다." }
            require(orderGroupId == null || orderGroupId.isNotBlank()) { "orderGroupId는 비어 있을 수 없습니다." }   // ← orderCondition == GTD 이면 expireAt 필수 검증이 없음
```

- 원인: PlaceOrderCommand 주석은 «GTD 주문 시 필수»라 적고 OrderCommandService.kt:79-80 이 requireNotNull 로 검사하지만, Order.create 는 LIMIT 가격 검증만 하고 GTD 의 expireAt 은 검증하지 않는다. 서비스를 거치지 않는 호출자(자동 청산 주문 L204·L231 등)는 GTD + expireAt=null 인스턴스를 만들 수 있다.
- 생길 수 있는 일: 만료 시각 없는 GTD 주문이 생성돼 만료 처리 대상에서 영구히 빠질 수 있다.
- 해결: 팩토리에 GTD 불변식을 추가하고 서비스의 중복 검사는 제거한다.

고친 모양(예시):
```
require(orderCondition != OrderCondition.GTD || expireAt != null) { "GTD 주문은 expireAt이 필요합니다." }
```

### 281위 [med · 확신 med] complete·fail 이 이전 상태를 검사하지 않아 SUCCESS 가 FAILED 로 뒤집힐 수 있음

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/portfolio/SnapshotJobRun.kt:53` · 항목표 backend/layering · 항목 D2

```
    fun complete() {
        status = SnapshotJobRunStatus.SUCCESS   // ← 이전 상태(RUNNING 여부) 검사 없음
        finishedAt = Instant.now()
    fun fail(message: String?) {
        status = SnapshotJobRunStatus.FAILED   // ← SUCCESS 에서도 FAILED 로 전이 가능
```

- 원인: PTB2 는 이름 붙은 메서드가 허용된 이전 상태를 검사하라고 한다. complete() 와 fail() 은 현재 status 를 보지 않고 대입하고 finishedAt 도 덮어쓴다.
- 생길 수 있는 일: 재시도·중복 호출로 fail() 이 complete() 뒤에 불리면 성공한 실행 기록이 FAILED 로 바뀌고 finishedAt 이 새로 찍혀 감사 추적이 어긋난다.
- 해결: complete()/fail() 시작에서 check(status == RUNNING) 로 허용 전이만 통과시킨다.

고친 모양(예시):
```
fun complete(finishedAt: Instant) {
    check(status == SnapshotJobRunStatus.RUNNING) { "RUNNING 상태에서만 완료할 수 있습니다: $status" }
    this.status = SnapshotJobRunStatus.SUCCESS
    this.finishedAt = finishedAt
}
```

### 282위 [med · 확신 med] 다른 애그리거트(Account)를 @ManyToOne 객체 참조로 가짐

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/portfolio/TaxSummaryRun.kt:28` · 항목표 backend/layering · 항목 D4

```
    @ManyToOne(fetch = FetchType.LAZY, optional = false)   // ← L28
    @JoinColumn(name = "account_id", nullable = false)
    lateinit var account: Account   // ← L30
```

- 원인: DailyBalance D4 와 같다.
- 생길 수 있는 일: 실행 기록 조회 때 Account 가 딸려 온다.
- 해결: accountId: Long 으로 바꾼다.
- 다른 렌즈의 반증: backend/code-shape+layering+naming-terminology+tdd-purity — id 는 IDENTITY 로 채워지는 식별자이고 D2 의 대상인 «상태·금액 필드» 가 아니다. S7 은 domain.entity 의 엔티티를 D2 몫으로 넘긴다. 다만 다른 5개 엔티티는 val id 라 일관성만 어긋난다(발견으로 올리지 않음).

### 283위 [med · 확신 med] completeSuccess·fail 이 이전 상태를 검사하지 않음

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/portfolio/TaxSummaryRun.kt:59` · 항목표 backend/layering · 항목 D2

```
    fun completeSuccess() {
        status = TaxSummaryRunStatus.SUCCESS   // ← 이전 상태 검사 없음
        errorMessage = null   // ← FAILED 였던 실행의 오류 메시지도 지움
    fun fail(message: String?) {
        status = TaxSummaryRunStatus.FAILED
```

- 원인: SnapshotJobRun 과 같은 모양이다. 현재 status 를 보지 않고 SUCCESS·FAILED 로 대입한다. completeSuccess 는 errorMessage 까지 null 로 지운다.
- 생길 수 있는 일: FAILED 로 기록된 실행이 completeSuccess 로 성공이 되면서 오류 메시지가 지워져 실패 이력이 사라진다.
- 해결: 둘 다 check(status == RUNNING) 로 시작한다.

### 284위 [med · 확신 med] fail(message) 가 오류 메시지를 500자로 자르지 않아 컬럼 길이(500)를 넘을 수 있음

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/portfolio/TaxSummaryRun.kt:65` · 항목표 backend/layering · 항목 D5

```
    @Column(name = "error_message", length = 500)   // ← 길이 제한 500
    fun fail(message: String?) {
        errorMessage = message   // ← 자르기 없음(SnapshotJobRun 은 message?.take(500))
```

- 원인: error_message 컬럼은 length = 500 인데 fail 은 message 를 그대로 대입한다. 같은 모양의 SnapshotJobRun.fail 은 message?.take(500) 으로 자른다. 불변식(필드 정합)이 한쪽 클래스에만 있다.
- 생길 수 있는 일: 긴 예외 메시지(스택·SQL 상세)가 들어오면 실패 기록을 저장하는 순간 value too long 오류가 나 원래 오류를 가리고 FAILED 기록 자체가 남지 않을 수 있다.
- 해결: errorMessage = message?.take(500) 로 SnapshotJobRun 과 같게 하고, 길이 상수를 한 곳에 둔다. 500 초과 시험을 추가한다.

### 285위 [med · 확신 med] 가격 기준 정책 검증이 도메인과 CommandService 에 두 벌이고 유효가격 계산은 Evaluator 에 있다

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/position/PositionExitTrigger.kt:161` · 항목표 backend/layering · 항목 D3

```
// PositionExitTrigger L175-181
when (priceBasisPolicy) {
    PriceBasisPolicy.FIXED_PRICE -> {
        if (triggerPrice == null) {
            throw BadRequestException("TRIGGER_PRICE_REQUIRED", "FIXED_PRICE requires triggerPrice")  // ←
// CommandService L106-108
PriceBasisPolicy.FIXED_PRICE -> {
    if (triggerPrice == null) {
        throw BadRequestException("TRIGGER_PRICE_REQUIRED", "FIXED_PRICE requires triggerPrice")
```

- 원인: FIXED_PRICE 는 triggerPrice 필수, FOLLOW_AVG_PRICE 는 triggerPercent 필수라는 같은 규칙이 PositionExitTrigger.validatePolicy 와 PositionExitTriggerCommandService.resolveTriggerPolicy 양쪽에 있고 에러 코드 문자열(TRIGGER_PRICE_REQUIRED·TRIGGER_PERCENT_REQUIRED)까지 중복이다. 한쪽(CommandService)만 «FIXED_PRICE 가 아니면 triggerPrice 금지» 규칙을 갖는다. 유효 트리거가(effectiveTriggerPrice)는 priceBasisPolicy·triggerPercent·triggerPrice 를 읽는 Evaluator 에 있다. LLM-only 판정.
- 생길 수 있는 일: 정책 규칙이 어긋난 채 진화한다. 도메인은 FOLLOW_AVG_PRICE 에 triggerPrice 를 허용하지만 서비스 경로는 거부해서, 서비스를 거치지 않는 호출자(시험·다른 유스케이스)가 서비스가 막는 조합의 트리거를 만들 수 있다.
- 해결: 정책 검증과 유효가격 계산을 PositionExitTrigger 한 곳으로 모으고 서비스는 위임만 한다.

고친 모양(예시):
```
class PositionExitTrigger {
    fun effectiveTriggerPrice(avgBuyPrice: BigDecimal): BigDecimal? = ...
}
```

### 286위 [med · 확신 med] 계속 쌓이는 전략 로그의 strategy_id·정렬 열에 인덱스가 없음

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/strategy/StrategyLog.kt:24` · 항목표 data/schema-data-model · 항목 M4

```
@Table(name = "strategy_logs")   // ← L24 indexes 없음
// StrategyLogRepository: findByStrategyIdOrderByLoggedAtDesc(strategyId)   // ← strategy_id 필터 + logged_at 정렬
```

- 원인: StrategyLog 에 @Table indexes 가 없고 이 묶음의 마이그레이션도 strategy_logs 를 만들지 않는다. StrategyLogRepository 는 strategy_id 로 거르고 logged_at 내림차순으로 정렬한다. Hibernate 는 FK 열에 인덱스를 만들지 않는다.
- 생길 수 있는 일: 로그가 쌓일수록 전략별 로그 조회가 테이블 전체 스캔과 정렬이 된다.
- 해결: (strategy_id, logged_at) 복합 인덱스를 추가한다.

고친 모양(예시):
```
@Table(name = "strategy_logs",
    indexes = [Index(name = "idx_strategy_logs_strategy_logged", columnList = "strategy_id, logged_at")])
```

### 287위 [med · 확신 med] 전략·기간별 성과 스냅샷에 유일 제약이 없어 같은 기간 행이 중복될 수 있음

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/strategy/StrategyPerformanceSnapshot.kt:22` · 항목표 data/schema-data-model · 항목 M3

```
@Entity
@Table(name = "strategy_performance_snapshots")   // ← L22 uniqueConstraints 없음, (strategy_id, period_start, period_end) 중복 허용
```

- 원인: (strategy_id, period_start, period_end)가 스냅샷의 자연 키인데 @Table 에 uniqueConstraints 가 없다. 주기적으로 저장하고 reviseMetrics 로 고치는 구조라 같은 기간을 두 번 저장하는 경로가 생기기 쉽다. main 코드에 쓰는 곳은 아직 없다.
- 생길 수 있는 일: 같은 기간 성과가 중복 행으로 쌓여 조회·집계가 중복 집계하고, 어느 행이 맞는지 알 수 없다.
- 해결: uniqueConstraints 로 (strategy_id, period_start, period_end)를 막고, 저장은 있으면 reviseMetrics 하는 방식으로 쓴다.

고친 모양(예시):
```
@Table(name = "strategy_performance_snapshots",
    uniqueConstraints = [UniqueConstraint(name = "uk_strategy_perf_period", columnNames = ["strategy_id", "period_start", "period_end"])])
```

### 288위 [med · 확신 med] StrategyPerformanceSnapshot 이 Strategy 루트를 객체로 참조

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/strategy/StrategyPerformanceSnapshot.kt:28` · 항목표 backend/layering · 항목 D4

```
@ManyToOne(fetch = FetchType.LAZY, optional = false)
@JoinColumn(name = "strategy_id", nullable = false)
lateinit var strategy: Strategy   // ← 다른 루트 객체 참조
```

- 원인: strategy: Strategy @ManyToOne 이다. 스냅샷은 자기 리포지토리(StrategyPerformanceSnapshotRepository)로 따로 다뤄지므로 자식이 아니라 루트로 본다.
- 생길 수 있는 일: 스냅샷 기간 조회에서 Strategy 가 딸려 오고, 성과 기록 저장이 전략 애그리거트와 같은 영속 문맥에 묶인다.
- 해결: strategyId: Long 으로 바꾼다.

고친 모양(예시):
```
val strategyId: Long
```

### 289위 [med · 확신 med] StrategyVersion 이 Strategy 루트를 객체로 참조

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/strategy/StrategyVersion.kt:33` · 항목표 backend/layering · 항목 D4

```
@ManyToOne(fetch = FetchType.LAZY, optional = false)
@JoinColumn(name = "strategy_id", nullable = false)
lateinit var strategy: Strategy   // ← 다른 루트 객체 참조
```

- 원인: strategy: Strategy @ManyToOne 이다. 버전은 전략의 이력이라 «자기 자식»으로 볼 여지가 있으나 StrategyVersionRepository 로 따로 조회(findTopBy…)되므로 루트로 판정했다.
- 생길 수 있는 일: 버전 조회가 Strategy 와 그 Account 까지 지연 로딩으로 끌어 온다.
- 해결: strategyId: Long 으로 바꾼다. 버전을 Strategy 의 자식으로 두려면 StrategyVersionRepository 를 없애고 Strategy 가 버전 목록을 소유하게 한다.

고친 모양(예시):
```
val strategyId: Long
```

### 290위 [med · 확신 med] fetchBalance 본문이 빈 줄 제외 48줄이고 추상화 수준이 여럿

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/kis/KisAccountBalanceAdapter.kt:34` · 항목표 backend/code-shape · 항목 S1

```
    override fun fetchBalance(accountId: Long, trId: String): KisBalanceSnapshot {      // ← 빈 줄 제외 본문 48줄(>40)
        val account = accountRepository.findById(accountId)      // ← 1) 계좌 조회·외부계좌번호 파싱
...
        val url = UriComponentsBuilder      // ← 2) URL 조립
...
        val response = try {      // ← 3) HTTP 호출·예외 번역
...
        val positions = outputList(response, "output1").mapNotNull { positionFrom(it) }      // ← 4) 응답 파싱·조립
...
        return KisBalanceSnapshot(
```

- 원인: 34~88줄 fetchBalance 한 함수가 계좌 조회와 외부계좌번호 파싱, URL 조립(queryParam 11개), HTTP 호출과 예외 번역, 응답 파싱, 수익률 계산, 스냅샷 조립을 모두 한다. 팀 기준(CS25) 40줄을 넘는다. detekt LongMethod 는 60줄 기준이라 안 잡힌다.
- 생길 수 있는 일: KIS 응답 형식이나 오류 처리를 고칠 때 한 함수 전체를 읽어야 하고, URL·파싱·번역 어느 한 단계만 따로 시험할 수 없다.
- 해결: 요청 조립(buildInquireBalanceUrl)과 호출(callInquireBalance)과 응답 변환(toSnapshot)으로 나눠 fetchBalance 는 호출 순서만 남긴다.
- 다른 렌즈의 반증: backend/code-shape+layering+naming-terminology+tdd-purity — KIS 헤더 tr_id 를 그대로 가리키는 외부 계약 이름이고 kis 계열 약어와 같은 성격이라 통과로 본다(포트 선언은 청크 밖).

고친 모양(예시):
```
override fun fetchBalance(accountId: Long, trId: String): KisBalanceSnapshot {
    val (cano, acntPrdtCd) = parseExternalAccountId(loadExternalAccountId(accountId))
    val mode = modeOf(trId)
    val response = callInquireBalance(mode, trId, cano, acntPrdtCd)
    ensureSuccess(response)
    return toSnapshot(response)
}
```

### 291위 [med · 확신 med] KIS REST 호출의 소요 시간·결과가 기록되지 않음

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/kis/KisAccountBalanceAdapter.kt:58` · 항목표 reliability/observability · 항목 O5

```
val response = try {
    restTemplate.exchange(url, HttpMethod.GET, HttpEntity<Void>(kisHeaders(mode, trId)), Map::class.java).body  // ← 소요 시간·결과 계측 없음
} catch (ex: ResourceAccessException) {
```

- 원인: 이 파일에는 로거가 없다. 시작·끝 로그, 소요 ms, 응답 코드(rt_cd), Micrometer Timer·Observation 이 모두 없다.
- 생길 수 있는 일: 장애 때 KIS 가 느린 건지 우리가 느린 건지 가를 수 없고 KIS 호출 제한·실패율 추세도 보이지 않는다.
- 해결: Timer 로 호출 시간과 결과(rt_cd) 태그를 기록하거나 시작·끝 로그에 소요 ms 를 남긴다.
- 다른 렌즈의 반증: data/query+schema-data-model+transaction-boundary — 어댑터에 @Transactional 이 없고 호출자 KisAccountQueryService 에도 없다(open-in-view: false). 주입된 RestTemplate 은 RestTemplateConfig 에서 연결 5초·읽기 10초 타임아웃이 걸려 있다.

고친 모양(예시):
```
val sample = Timer.start(meterRegistry)
try { ... } finally { sample.stop(meterRegistry.timer("kis.inquire_balance", "mode", mode)) }
```

### 292위 [med · 확신 med] 기동 이벤트·웹소켓 수신 진입점이 infrastructure 에 있고 유스케이스로 직결

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/kis/KisExecutionWebSocketManager.kt:141` · 항목표 backend/layering · 항목 L6

```
@EventListener(ApplicationReadyEvent::class)
fun start() { // ← 기동 시 외부 연결을 여는 진입점이 infrastructure 에 있음
...
private fun handleRawMessage(mode: String, rawMessage: String) {
    val notice = parser.parse(rawMessage, mode) ?: return
    noticeService.handle(notice) // ← 수신 통지가 곧바로 유스케이스를 호출
```

- 원인: @EventListener(ApplicationReadyEvent) 로 외부 연결을 열고, 수신 프레임을 파서 → KisExecutionNoticeService.handle 로 이어 붙인다. 외부에서 들어오는 흐름의 진입점이 presentation 이 아니라 infrastructure 에 있다(PTB7 PR #50). 나가는 연결을 여는 클라이언트라 완전한 서버 진입점은 아니어서 신뢰도는 중간이다.
- 생길 수 있는 일: 진입점이 application 서비스를 import 해 PTB7 이 깨지고, 유스케이스가 이 어댑터를 통해서만 닿는다.
- 해결: 수신 어댑터(연결·재연결·파싱)는 infrastructure 에 두고, 통지를 유스케이스로 넘기는 호출 지점은 presentation 의 진입점 클래스로 올린다.

### 293위 [med · 확신 med] 재연결 횟수 갱신이 원자적이지 않고 한 장애에 재연결이 중복 예약됨

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/kis/KisExecutionWebSocketManager.kt:202` · 항목표 reliability/load-concurrency · 항목 A4

```
fun handleSocketClosed(mode: String) { ...; reconnect(mode) }
fun handleTransportFailure(mode: String, error: Throwable) { ...; reconnect(mode) }   // ← 같은 끊김에 두 번 호출
...
val nextAttempt = (reconnectAttempts[mode] ?: 0) + 1   // ← 읽고
...
reconnectAttempts[mode] = nextAttempt   // ← 쓰는 사이가 원자적이지 않음
```

- 원인: 한 번의 끊김이 handleTransportFailure 와 handleSocketClosed 양쪽을 부르고, 둘 다 reconnect(mode) 를 무조건 호출한다. reconnect 는 reconnectAttempts 를 get 후 put 으로 갱신해 compute 같은 원자 연산이 아니고, 상태가 CONNECTING·CONNECTED 인지도 확인하지 않는다. 제어 메시지 오류 경로(handleControlMessage)는 기존 세션을 닫지 않고 새 연결을 연다.
- 생길 수 있는 일: 한 끊김이 연결 둘을 만들고 시도 횟수를 두 배로 소모해 5회 한도에 빨리 닿는다. 열린 채 남은 기존 세션이 통지를 중복 전달하고(유일 제약이 막긴 함) 자원이 샌다.
- 해결: mode 별로 상태를 compareAndSet 하듯 DISCONNECTED → CONNECTING 전이에 성공한 한 호출만 재연결을 예약하게 하고, 열려 있는 세션은 먼저 닫는다.

고친 모양(예시):
```
if (statuses.replace(mode, KisWebSocketConnectionStatus.CONNECTED, KisWebSocketConnectionStatus.DISCONNECTED) ||
    statuses.replace(mode, KisWebSocketConnectionStatus.CONNECTING, KisWebSocketConnectionStatus.DISCONNECTED)) scheduleReconnect(mode)
```

### 294위 [med · 확신 med] KIS REST 호출의 소요 시간·결과가 기록되지 않음

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/kis/KisOrderRestClient.kt:30` · 항목표 reliability/observability · 항목 O5

```
private val log = KotlinLogging.logger {}   // ← 선언만 있고 사용처 0
...
val response = restTemplate.exchange(   // ← 호출 시간·상태 기록 없음
    "${properties.restBaseUrl(mode)}/uapi/domestic-stock/v1/trading/order-cash",
```

- 원인: logger 가 선언돼 있지만 이 파일에서는 한 번도 쓰이지 않는다. 주문·취소·체결 조회 호출에 시작·끝 로그, Timer, 인터셉터가 없다. RestTemplateBuilder 로 만든 빈이라 Boot 가 관측 훅을 붙이지만, build.gradle 에 meter 레지스트리가 없고 application.yaml 에 management 설정도 없어 지표가 외부로 보이지 않는다.
- 생길 수 있는 일: 장애 때 KIS 가 느린 것인지 우리가 느린 것인지 가를 근거가 없다.
- 해결: 호출 래퍼에서 소요 ms·상태(rt_cd)를 한 줄 로그하고, actuator metrics 를 노출하거나 Micrometer Timer 를 기록한다.

고친 모양(예시):
```
private inline fun <T> timed(op: String, mode: String, block: () -> T): T {
    val t0 = System.nanoTime()
    return try { block() } finally { log.info { "kis $op mode=$mode took=${(System.nanoTime()-t0)/1_000_000}ms" } }
}
```

### 295위 [med · 확신 med] 체결 조회 실패를 빈 목록으로 돌려줌 — '체결 없음' 과 구분 안 됨

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/kis/KisOrderRestClient.kt:126` · 항목표 reliability/failure-handling · 항목 E1

```
if (rtCd(response) != "0") return emptyList()   // ← API 오류가 '체결 없음' 으로 둔갑
...
executedQty = item["TOT_CCLD_QTY"]?.toString()?.toBigDecimalOrNull() ?: BigDecimal.ZERO,   // ← 누락도 기본값
```

- 원인: inquireFills 가 rt_cd 가 0 이 아니면 로그 없이 emptyList() 를 돌려준다. 금액·수량 필드 누락도 BigDecimal.ZERO·ONE 기본값으로 채운다. 이 메서드는 WebSocket 통지 유실을 복구할 유일한 조회 수단인데 실패와 정상 빈 결과가 같다.
- 생길 수 있는 일: 복구 작업을 붙여도 KIS 오류 때 '체결 없음' 으로 읽혀 유실된 체결이 영영 확정되지 않는다.
- 해결: 실패는 KisApiException 으로 던지고 필수 필드 누락은 해당 행을 파싱 실패로 처리한다.

고친 모양(예시):
```
if (rtCd(response) != "0") throw KisApiException(mapKisErrorCode(rtCd(response), message(response)), rtCd(response), message(response))
```

### 296위 [med · 확신 med] get* 이름 함수가 KIS 발급 POST 와 Redis 쓰기를 숨김

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/kis/KisTokenManager.kt:32` · 항목표 backend/code-shape · 항목 F2

```
fun getToken(mode: String): String {   // ← get* 이름인데 키가 없으면 KIS 발급 POST + Redis 저장까지 함
    redisTemplate.opsForValue().get(redisKey(mode))?.let { return it }
    return synchronized(lock) {
        redisTemplate.opsForValue().get(redisKey(mode))
            ?: issueAndSave(mode)   // ← 외부 POST + Redis set
// …
fun getWebSocketApprovalKey(mode: String): String {   // ← 같은 모양(42행)
    val response = restTemplate.postForObject(   // ← 조회 이름 안의 외부 POST
// …
    redisTemplate.opsForValue().set(key, approvalKey, Duration.ofHours(20))   // ← 조회 이름 안의 Redis 쓰기
```

- 원인: getToken 은 Redis 에 키가 없으면 issueAndSave 로 KIS 토큰 발급 POST 를 보내고 Redis 에 저장한다. getWebSocketApprovalKey 도 같은 모양이다(미스면 /oauth2/Approval POST + Redis set). 조회 이름이 외부 쓰기를 약속하지 않는다. 클래스 KDoc 스스로 「KIS 토큰은 하루 1회만 발급 가능」이라고 적는 엔드포인트다. 캐시 미스 때 발급하는 get-or-create 관용이라 기본 high 에서 med 로 낮춤.
- 생길 수 있는 일: 호출자가 getToken 을 값싼 조회로 알고 재시도 루프·헬스체크·시험 준비 코드에서 마구 부르면, Redis 가 비었을 때 하루 1회 제한 발급이 호출 수만큼 시도돼 토큰·승인키 발급이 막힌다.
- 해결: 이름에 발급 가능성을 드러낸다: getToken → getOrIssueToken, getWebSocketApprovalKey → getOrIssueWebSocketApprovalKey(또는 issue*IfAbsent). 호출처 이름도 함께 바꾼다.

고친 모양(예시):
```
fun getOrIssueToken(mode: String): String { /* 캐시 조회 후 없으면 발급 */ }
fun getOrIssueWebSocketApprovalKey(mode: String): String { /* 캐시 조회 후 없으면 발급 */ }
```

### 297위 [med · 확신 med] 토큰 발급 중복 방지가 프로세스 안 락뿐 — Redis 공유 키는 보호 안 됨

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/kis/KisTokenManager.kt:35` · 항목표 reliability/load-concurrency · 항목 A6

```
return synchronized(lock) {   // ← 프로세스 안 락, collector-api 나 다른 인스턴스는 보지 못함
    redisTemplate.opsForValue().get(redisKey(mode))
        ?: issueAndSave(mode)   // ← HTTP 발급(최대 15초)을 락 안에서 수행
}
```

- 원인: getToken 은 Redis 조회 → synchronized(lock)(프로세스 안 락) → 재조회 → 발급이다. 키 kis:token:{mode} 는 collector-api 와 공유되고 인스턴스가 늘 수 있는데, 다른 프로세스는 이 락을 못 본다. Redis SET NX 같은 분산 락이 없다. getWebSocketApprovalKey 는 락도 없어 같은 JVM 안에서도 동시 발급이 가능하다. 락 안에서 HTTP(최대 15초)를 호출해 모드와 무관하게 호출자들이 줄 선다.
- 생길 수 있는 일: 토큰 만료 순간 두 프로세스가 동시에 발급하면 KIS 의 발급 횟수 제한에 걸려 한쪽이 실패하고, 그 시점의 주문이 실패한다.
- 해결: Redis SET NX PX 로 발급 락을 잡고, 락을 못 얻으면 짧게 대기 후 Redis 를 다시 읽는다. 승인키 발급에도 같은 락을 쓴다.

고친 모양(예시):
```
if (redis.setIfAbsent("kis:token:lock:$mode", id, Duration.ofSeconds(20)) == true) { try { issueAndSave(mode) } finally { redis.delete(lockKey) } } else { waitAndReread(mode) }
```

### 298위 [med · 확신 med] AFTER_COMMIT 리스너가 부르는 REQUIRES_NEW 결과 기록이 원본 id 로 중복을 거르지 않음

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/notification/NotificationPersistenceTxService.kt:14` · 항목표 data/transaction-boundary · 항목 X5

```
@Transactional(propagation = Propagation.REQUIRES_NEW)
fun save(notification: Notification): Notification =
    repository.save(notification)   // ← sourceRef 중복 확인·충돌 처리 없이 삽입
```

- 원인: SlackNotificationEventHandler(AFTER_COMMIT·@Async)는 txService.save 로 알림 행을 만든다. save 는 REQUIRES_NEW 라 커밋은 보장되지만 같은 source_ref 가 이미 있는지 확인하지 않고 DB 유일 제약도 없다(M3 발견). 규칙 PTD4 는 REQUIRES_NEW 결과 기록이 원본 id 로 중복을 거르게 요구한다.
- 생길 수 있는 일: 재시도·재발행으로 같은 이벤트가 다시 들어오면 알림이 중복 저장되고 Slack 이 중복 발송된다.
- 해결: sourceRef 유일 제약을 두고 save 에서 충돌을 «이미 처리됨»으로 받아 기존 행을 돌려준다.

고친 모양(예시):
```
@Transactional(propagation = Propagation.REQUIRES_NEW)
fun save(n: Notification): Notification =
    repository.findBySourceRef(n.sourceRef) ?: repository.save(n)
```

### 299위 [med · 확신 med] 핸들러가 PENDING 행을 만든 뒤 점유(PENDING→DELIVERING) 없이 전송해 스케줄러와 중복 전송 가능

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/notification/SlackNotificationEventHandler.kt:29` · 항목표 data/transaction-boundary · 항목 X11

```
val notification = txService.save(   // ← PENDING 으로 커밋, 스케줄러가 바로 집어갈 수 있음
    Notification.create(...),
)
val success = sender.send(event.message)   // ← 점유(PENDING→DELIVERING) 없이 부수효과
if (success) {
    txService.markDelivered(notification)
```

- 원인: handle 은 알림을 PENDING 으로 저장한 직후 DELIVERING 으로 선점하지 않고 sender.send 를 부른다. 스케줄러는 PENDING 행을 조건부 UPDATE(claimForDelivery)로 선점해 보내므로, 핸들러의 전송이 느리거나 30초 틱과 겹치면 같은 행을 둘이 보낸다. 스케줄러 쪽 점유 자체는 올바르다(DA15).
- 생길 수 있는 일: Slack 메시지가 두 번 나가고 markDelivered 가 두 번 실행된다. 알림이라 금전 영향은 없으나 인스턴스가 늘수록 잦아진다.
- 해결: 저장과 함께 DELIVERING 으로 시작하게 하거나, 전송 전에 txService.markDelivering 으로 선점하고 null 이면 건너뛴다.

고친 모양(예시):
```
val claimed = txService.markDelivering(notification) ?: return
val success = sender.send(event.message)
```

### 300위 [med · 확신 med] Slack 재시도 스케줄러가 매칭 스케줄러와 단일 스레드 스케줄러를 공유하며 건당 블로킹 호출

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/notification/SlackNotificationScheduler.kt:17` · 항목표 reliability/load-concurrency · 항목 C1

```
// SlackNotificationScheduler.kt:17
@Scheduled(fixedDelay = 30_000)   // ← LocalMatchingScheduler.kt:23 와 같은 단일 스레드 스케줄러 공유 (spring.task.scheduling.pool.size 미설정)
...
val success = sender.send(claimedNotification.message)   // ← PENDING 건마다 직렬로 최대 약 15초(connect 5s + read 10s) 블로킹
```

- 원인: LocalMatchingScheduler.kt:23 도 @Scheduled(fixedDelay = 30_000) 이고 application.yaml 에는 spring.task.scheduling.pool.size 설정이 없어 기본 단일 스레드 스케줄러를 쓴다(별도 TaskScheduler 빈 정의는 grep 하지 않아 확인 못함). retryPending 은 PENDING 건마다 직렬로 최대 15초 블로킹 호출을 한다.
- 생길 수 있는 일: Slack 장애 때 PENDING N건 × 약 15초 동안 로컬 매칭 틱이 뒤로 밀려 지정가 체결이 지연된다.
- 해결: spring.task.scheduling.pool.size 를 2 이상으로 올리거나 Slack 재시도 전용 TaskScheduler 를 쓰고, 틱당 처리 건수 상한을 둔다(C2).

고친 모양(예시):
```
spring:
  task:
    scheduling:
      pool:
        size: 4
```

### 301위 [med · 확신 med] DELIVERING 으로 점유된 행을 되돌리는 경로가 없어 프로세스 중단 시 알림이 영구 미발송

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/notification/SlackNotificationScheduler.kt:21` · 항목표 reliability/failure-handling · 항목 I5

```
val pendingNotifications = txService.findPendingForRetry(policy.maxRetries)   // ← PENDING 만 조회 (NotificationPersistenceTxService.kt:42-43)
...
val claimedNotification = txService.markDelivering(notification)   // ← 여기서 DELIVERING 커밋
...
val success = sender.send(claimedNotification.message)             // ← 이 사이에 죽으면 행이 DELIVERING 으로 영구 잔류, 되돌리는 코드 없음
```

- 원인: markDelivering 은 REQUIRES_NEW 로 PENDING→DELIVERING 을 커밋한다. 그 뒤 sender.send 중에 프로세스가 죽거나 재배포되면 행이 DELIVERING 으로 남는다. 조회는 PENDING 만 하고(findPendingForRetry), src/main 에서 DELIVERING 을 소비하는 곳은 claimForDelivery 설정·엔티티 전이·enum 뿐이다(grep 확인). lease 만료나 진행 지점 복구가 없다.
- 생길 수 있는 일: 배포·OOM 직후 처리 중이던 알림은 로그도 에러도 없이 영구히 발송되지 않고, 이후 어떤 재시도 대상도 되지 않는다.
- 해결: claim 시각 열(claimed_at)을 두고, 임대 시간이 지난 DELIVERING 행을 PENDING 으로 되돌리는 쿼리를 스케줄러 틱 앞단에서 실행한다.
- 다른 렌즈의 반증: data/query+schema-data-model+transaction-boundary — 각 알림이 독립 작업이고 «전체 성공/실패» 응답을 받는 호출자가 없다. 건별로 점유(claimForDelivery)→전송→결과 기록이 목적이다. 건별 예외는 catch 로 격리되고 바깥 트랜잭션이 없어 UnexpectedRollbackException 도 생기지 않는다.

고친 모양(예시):
```
// claimForDelivery: .set(notification.claimedAt, Instant.now())
fun releaseStaleDelivering(before: Instant): Int =
    queryFactory.update(notification)
        .set(notification.status, DeliveryStatus.PENDING)
        .where(notification.status.eq(DeliveryStatus.DELIVERING), notification.claimedAt.lt(before))
        .execute().toInt()
```

### 302위 [med · 확신 med] Slack 웹훅 호출 실패를 Exception 전체로 잡아 Boolean 하나로 뭉갬

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/notification/SlackWebhookNotifier.kt:26` · 항목표 reliability/failure-handling · 항목 E3

```
} catch (ex: Exception) {   // ← RestClientException 계열만 던지는 단일 호출에 Exception 전체를 잡음 (detekt TooGenericExceptionCaught)
    log.warn { "Slack webhook call failed: ${ex.message}" }
    false                   // ← 일시·영구 오류를 구분 없이 false 하나로 뭉갬
}
```

- 원인: try 본문은 RestTemplate.postForEntity 한 호출뿐인데 Exception 전체를 잡는다(detekt TooGenericExceptionCaught: SlackWebhookNotifier$ex: Exception). 웹훅 폐기(404·403), 타임아웃, 5xx, 코드 버그가 모두 false 하나로 합쳐진다. 스케줄러 틱이나 리스너 같은 최상위 진입점이 아니라 어댑터 안쪽의 catch 다.
- 생길 수 있는 일: 영구 오류(웹훅 폐기)도 일시 오류와 똑같이 재시도되고, DB last_error 에는 항상 고정 문자열만 남아 원인(타임아웃인지 폐기인지)을 알 수 없다. 관련 없는 버그도 같은 false 로 가려진다.
- 해결: HttpClientErrorException(영구)과 RestClientException(일시)을 따로 잡고, 반환을 Sent·Retryable·Permanent 로 구분해 스케줄러가 영구 오류는 바로 FAILED 로 확정하게 한다.
- 다른 렌즈의 반증: reliability/failure-handling+load-concurrency+observability+security-config-hygiene — WARN 로그가 있어 E1 은 아님. 같은 지점의 문제는 E3·E2·O7 로 보고

고친 모양(예시):
```
sealed interface SendResult {
    data object Sent : SendResult
    data class Retryable(val reason: String) : SendResult
    data class Permanent(val reason: String) : SendResult
}

override fun send(message: String): SendResult = try {
    restTemplate.postForEntity(properties.webhookUrl, HttpEntity(mapOf("text" to message)), String::class.java)
    SendResult.Sent
} catch (ex: HttpClientErrorException) {
    SendResult.Permanent("status=${ex.statusCode.value()}")
} catch (ex: RestClientException) {
    SendResult.Retryable(ex::class.simpleName ?: "RestClientException")
}
```

### 303위 [med · 확신 med] 사실상 출력 포트인 인터페이스가 port 밖에 있고 시그니처가 port 밖 application 타입을 씀

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/ExecutionRepositoryCustom.kt:8` · 항목표 backend/layering · 항목 L3

```
interface ExecutionRepositoryCustom {   // ← 조회 계약이 infrastructure 에 있음
    fun searchExecutions(query: ExecutionQuery): List<Execution>   // ← port 밖 application 타입을 시그니처에 씀
```

- 원인: application 이 ExecutionRepository(이 인터페이스를 상속)로 직접 쓰는 조회 계약이 infrastructure/persistence 에 있고, searchExecutions 가 application.order.query.ExecutionQuery 를 받는다. 포트는 application/<ctx>/port 에 있어야 하고 시그니처는 자기 port 패키지·domain 타입만 쓴다(PTB8). L4 와 같은 뿌리라 한 번에 고친다.
- 생길 수 있는 일: 포트 자리가 둘로 갈라지고 infrastructure 가 port 밖 application 패키지를 import 하게 돼 PTB7 이 계속 깨진다.
- 해결: 조회 포트를 application/order/port 에 두고 ExecutionQuery 도 같은 port 패키지로 옮긴다. ExecutionRepositoryImpl 은 그 포트를 구현하는 어댑터가 된다.

고친 모양(예시):
```
// application/order/port/ExecutionSearchPort.kt
interface ExecutionSearchPort {
    fun search(query: ExecutionSearchQuery): List<Execution>
}
```

### 304위 [med · 확신 med] 「미체결」 상태 집합이 기본 인자로 또 복사됨 (LLM-only)

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/OrderRepository.kt:38` · 항목표 backend/layering · 항목 D3

```
@Param("statuses") statuses: Collection<OrderStatus> = listOf(OrderStatus.PENDING, OrderStatus.PARTIAL),   // ← 38행: 미체결 규칙 복사
// …
@Param("statuses") statuses: Collection<OrderStatus> = listOf(OrderStatus.PENDING, OrderStatus.PARTIAL),   // ← 56행: 또 복사
```

- 원인: sumOpenSellQuantity(38행)와 existsOpenOrderByAccountTickerSideWithoutGroup(56행)의 기본 인자가 listOf(PENDING, PARTIAL) 로 같은 규칙을 다시 적는다. OrderRepositoryImpl 4곳·application 2곳과 같은 뿌리 — 규칙의 자리가 OrderStatus 에 없다.
- 생길 수 있는 일: 상태 추가 때 쿼리 기본값이 낡은 채 남으면 매도 가능 수량 합계가 어긋나 과매도 검사가 틀릴 수 있다.
- 해결: OrderStatus.OPEN 같은 단일 정의를 기본 인자에서 참조한다.

고친 모양(예시):
```
@Param("statuses") statuses: Collection<OrderStatus> = OrderStatus.OPEN,
```

### 305위 [med · 확신 med] 사실상 출력 포트인 인터페이스가 port 밖에 있고 시그니처가 port 밖 application 타입을 씀

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/OrderRepositoryCustom.kt:10` · 항목표 backend/layering · 항목 L3

```
interface OrderRepositoryCustom {   // ← 조회 계약이 infrastructure 에 있음
    fun searchOrders(query: OrderListQuery): List<Order>   // ← port 밖 application 타입을 시그니처에 씀
```

- 원인: searchOrders(query: OrderListQuery) 가 application.order.query 타입을 받고, 인터페이스 자체가 infrastructure/persistence 에 있다. application 이 OrderRepository 로 이 계약을 직접 쓴다. L4 와 같은 뿌리라 함께 고친다.
- 생길 수 있는 일: 포트 자리가 둘로 갈라져 어댑터가 port 밖 application 패키지에 묶이고, 조회 모델 변경이 영속 어댑터 컴파일을 깨뜨린다.
- 해결: application/order/port 에 포트를 두고 OrderListQuery 를 같은 port 패키지로 옮긴다.

고친 모양(예시):
```
// application/order/port/OrderSearchPort.kt
interface OrderSearchPort {
    fun search(query: OrderListQuery): List<Order>
}
```

### 306위 [med · 확신 med] 「미체결(PENDING·PARTIAL)」 규칙이 쿼리마다 복사됨 (LLM-only)

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/OrderRepositoryImpl.kt:51` · 항목표 backend/layering · 항목 D3

```
o.orderStatus.`in`(OrderStatus.PENDING, OrderStatus.PARTIAL),   // ← 51행: 미체결 규칙 복사 1
// …
o.orderStatus.`in`(OrderStatus.PENDING, OrderStatus.PARTIAL),   // ← 61행: 복사 2
// …
o.orderStatus.`in`(OrderStatus.PENDING, OrderStatus.PARTIAL),   // ← 74행: 복사 3
// …
o.orderStatus.`in`(OrderStatus.PENDING, OrderStatus.PARTIAL),   // ← 93행: 복사 4
```

- 원인: 미체결 주문 판정이 OrderStatus·Order 의 상수나 메서드로 없고, 이 파일에서 51·61·74·93행 4번, OrderRepository 에서 2번(38·56행 기본 인자), application 에서도 order.orderStatus in setOf(PENDING, PARTIAL) 로 2곳(OrderCommandService.kt:244, ExecutionProcessor.kt:98 — grep 으로 확인) 복사됐다. 규칙이 데이터와 떨어져 서비스·쿼리마다 다시 구현된다(DD10·DD11).
- 생길 수 있는 일: 주문 상태가 하나 늘면(예: PTF3 의 UNKNOWN 불확정 상태) 한 곳만 빠져도 그 쿼리가 주문을 놓쳐 매칭·만료·폴링에서 조용히 빠진다. 주문 상태 전이 누락은 금융 정합성 문제다.
- 해결: OrderStatus 에 미체결 집합을 한 곳으로 정의하고(또는 Order.isOpen() 메서드) 쿼리·서비스가 그것을 쓴다.

고친 모양(예시):
```
enum class OrderStatus {
    PENDING, PARTIAL, FILLED, CANCELLED, REJECTED;
    companion object { val OPEN: Set<OrderStatus> = EnumSet.of(PENDING, PARTIAL) }
}
// 쿼리: o.orderStatus.`in`(OrderStatus.OPEN)
```

### 307위 [med · 확신 med] 매칭·포지션 갱신 실패 로그가 예외 객체 없이 메시지만 남김

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/redis/QuoteEventListener.kt:34` · 항목표 reliability/failure-handling · 항목 E2

```
runCatching { localMatchingEngine.tryMatchPendingOrders(quote.ticker, quote) }
    .onFailure { log.warn { "매칭 처리 오류: ticker=${quote.ticker}, reason=${it.message}" } }  // ← 예외 객체·스택 없음
...
.onFailure { log.warn { "exit trigger 처리 오류: ticker=${quote.ticker}, reason=${it.message}" } }
```

- 원인: 세 runCatching 의 onFailure 가 모두 reason=${it.message} 문자열만 쓰고 예외를 넘기지 않는다. 매칭 실패(돈이 움직이는 경로) 스택이 로그에 없다. 종목 식별자는 있다.
- 생길 수 있는 일: 체결 엔진 내부 버그(NPE·락 충돌·제약 위반)가 WARN 한 줄(message 가 null 이면 reason=null)로만 남아 원인을 재현할 수 없다.
- 해결: log.warn(it) { ... } 로 예외를 함께 넘기고, 돈이 움직이는 매칭 실패는 ERROR 수준을 검토한다.
- 다른 렌즈의 반증: backend/code-shape+layering+naming-terminology+tdd-purity — public 메서드는 onMessage 하나이고 시세 이벤트를 소비자에게 나눠주는 팬아웃 진입점이라 존재 이유가 하나다. 컨텍스트 경계 문제는 L4·L6 으로 따로 올렸다

고친 모양(예시):
```
.onFailure { log.error(it) { "매칭 처리 오류: ticker=${quote.ticker}" } }
```

### 308위 [med · 확신 med] Redis 구독 진입점이 infrastructure 에서 등록됨

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/redis/RedisListenerConfig.kt:12` · 항목표 backend/layering · 항목 L6

```
@Bean
fun redisMessageListenerContainer(
    connectionFactory: RedisConnectionFactory,
    quoteEventListener: QuoteEventListener,        // ← 유스케이스를 부르는 Redis 진입점(MessageListener)을 infrastructure 에서 등록
): RedisMessageListenerContainer {
    container.addMessageListener(quoteEventListener, PatternTopic("quote:kis:*:*"))
```

- 원인: RedisListenerConfig 가 QuoteEventListener 를 PatternTopic 에 등록한다. QuoteEventListener.kt:21-23(이 묶음 밖에서 앞 35줄만 열어 확인)는 infrastructure.redis 에서 MessageListener 를 구현하고 LocalMatchingEngine·PositionCommandService 같은 유스케이스를 직접 부른다. PTB7(PR #50 판본)은 Redis 리스너 같은 진입점을 presentation 에서 유스케이스만 부르게 둔다. 진입점 클래스 자체의 판정은 그 파일이 속한 묶음 몫이고, 여기서는 구독 등록이 같은 위치에 묶여 있음을 기록한다.
- 생길 수 있는 일: 진입점과 그 등록 설정이 infrastructure 에 남아 있어 application 서비스를 import 하는 방향이 굳고, 진입점을 presentation 으로 옮길 때 이 구독 설정도 함께 옮겨야 하는데 놓치기 쉽다.
- 해결: QuoteEventListener 와 이 구독 설정을 presentation 쪽 진입점으로 옮기고, 리스너는 유스케이스 하나만 호출하게 한다(포트 이전과 함께).

고친 모양(예시):
```
// presentation/redis/QuoteSubscriptionConfig.kt
@Configuration
class QuoteSubscriptionConfig { /* 같은 컨테이너 설정, 리스너는 유스케이스 하나만 호출 */ }
```

### 309위 [med · 확신 med] Redis 해시 파싱을 runCatching{...}.getOrNull() 로 감싸 필드 누락·형식 오류를 구분 없이 null 로 삼킴

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/redis/RedisLivePositionCacheAdapter.kt:60` · 항목표 backend/code-shape · 항목 S10

```
return runCatching {
    LivePositionSnapshot(
        id = hash.getValue("id").toLong(),  // ← 필수 필드 누락을 NoSuchElementException 으로 던져 분기
        ...
        priceSource = PriceSource.valueOf(hash.getValue("priceSource")),  // ← 미지의 enum 이름도 예외로 처리
    )
}.getOrNull()  // ← 모든 예외를 구분 없이 null 로 삼킴
```

- 원인: read() 가 필수 필드 존재(getValue 의 NoSuchElementException)와 값 형식(toLong·BigDecimal·enum valueOf)을 예외로 판별하고 getOrNull() 로 한꺼번에 null 처리한다. 키 존재 여부는 한 식으로 검사할 수 있는 조건인데 예외로 분기하고, 같은 블록 안의 진짜 오류(손상된 숫자 문자열·미지의 enum 이름)까지 같은 null 로 삼킨다.
- 생길 수 있는 일: 캐시 항목이 손상되거나 스키마가 바뀌면(필드 추가·enum 이름 변경) 그 포지션이 아무 로그 없이 목록에서 빠진다. findByAccountId·findByTicker·findByTickerAndMode 가 mapNotNull 로 이어져 호출자는 원인을 알 수 없다.
- 해결: 필수 키는 hash["id"]?.toLongOrNull() ?: return null 처럼 명시 검사하고, 손상 항목은 키와 함께 warn 로그를 남긴 뒤 null 을 돌려준다. 예외를 잡아야 하면 NumberFormatException·IllegalArgumentException 만 좁게 잡는다.

고친 모양(예시):
```
val id = hash["id"]?.toLongOrNull() ?: return null.also { log.warn("live position hash 손상: key={}", key) }
val quantity = hash["quantity"]?.toBigDecimalOrNull() ?: return null
```

### 310위 [med · 확신 med] runCatching getOrNull 로 필드 누락·형식 오류를 시세 없음으로 바꿈

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/redis/RedisMarketQuoteAdapter.kt:40` · 항목표 backend/code-shape · 항목 S10

```
        return runCatching {
            QuoteSnapshot(
                ticker = ticker.uppercase(),
                tradingMode = tradingMode,
                price = BigDecimal(hash.getValue("price")),
                askp1 = BigDecimal(hash.getValue("askp1")),
                bidp1 = BigDecimal(hash.getValue("bidp1")),
                updatedAt = updatedAt,
            )
        }.getOrNull() // ← 필드 누락·형식 오류 예외를 null 로 바꿔 삼킴, 로그도 없음
```

- 원인: hash.getValue(...) 는 키가 없으면 NoSuchElementException, BigDecimal(String) 은 형식이 틀리면 NumberFormatException 을 던진다. 그 둘을 runCatching { ... }.getOrNull() 로 한꺼번에 삼켜 null(시세 없음)로 바꾼다. hash["price"]?.toBigDecimalOrNull() ?: return null 같은 한 식으로 검사할 수 있는 조건이다. 삼킴 자체(로그 없음)는 failure-handling 소관이고 여기서는 예외를 분기로 쓰는 모양만 센다.
- 생길 수 있는 일: collector 가 필드를 빠뜨리거나 형식이 깨진 시세를 발행해도 호출자(주문 유스케이스·포트폴리오 평가)는 평범한 시세 없음과 구분하지 못한다. 원인 파악이 어렵고 같은 타입의 진짜 오류도 같이 묻힌다.
- 해결: 필드를 직접 검사하고 없거나 틀리면 이유를 warn 로그로 남기고 null 을 돌려준다. 예외를 제어 흐름으로 쓰지 않는다.

고친 모양(예시):
```
val price = hash["price"]?.toBigDecimalOrNull() ?: return null
val askp1 = hash["askp1"]?.toBigDecimalOrNull() ?: return null
val bidp1 = hash["bidp1"]?.toBigDecimalOrNull() ?: return null
return QuoteSnapshot(ticker.uppercase(), tradingMode, price, askp1, bidp1, updatedAt)
```

### 311위 [med · 확신 med] @Scheduled 진입점이 infrastructure 에 있고 유스케이스를 직접 부름 (LLM-only)

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/scheduler/LocalMatchingScheduler.kt:16` · 항목표 backend/layering · 항목 L6

```
@Component
class LocalMatchingScheduler(
    private val orderRepository: OrderRepository,
    private val orderCommandService: OrderCommandService,   // ← 진입점이 유스케이스를 직접 주입
// …
    @Scheduled(fixedDelay = 30_000)   // ← infrastructure 의 @Scheduled 진입점
    fun cancelExpiredGtdOrders() {
// …
            orderCommandService.cancelOrder(accountId, orderId, CancelOrderCommand("GTD 만료"))   // ← 유스케이스 직접 호출
```

- 원인: cancelExpiredGtdOrders 가 infrastructure 에서 만료 주문 조회(orderRepository.findExpiredOrders), 반복, 건별 실패 처리와 cancelOrder 호출까지 한다. 진입점은 presentation 에서 유스케이스 하나만 불러야 하고(PTB7), 이 조회·반복은 유스케이스 몫이다. L4 와 같은 뿌리.
- 생길 수 있는 일: 만료 취소 규칙이 어댑터에 흩어져 시험이 스케줄러·리포지토리를 함께 띄워야 하고, 진입점이 application 서비스를 import 해 PTB7 이 계속 깨진다.
- 해결: 스케줄러를 presentation 에 두고, 「만료 주문 조회 + 건별 취소」는 application 의 단일 유스케이스(예 CancelExpiredOrdersUseCase)로 옮긴다. 스케줄러는 그 유스케이스만 호출한다.

고친 모양(예시):
```
// presentation/scheduler/GtdOrderExpiryScheduler.kt
@Component
class GtdOrderExpiryScheduler(private val cancelExpiredOrders: CancelExpiredOrdersUseCase) {
    @Scheduled(fixedDelay = 30_000)
    fun run() = cancelExpiredOrders.execute()
}
```

### 312위 [med · 확신 med] 컨트롤러가 서비스가 돌려준 도메인 엔티티를 그대로 받아 필드를 읽고 DTO 로 매핑함

`backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/controller/AccountController.kt:46` · 항목표 backend/layering · 항목 L5

```
        val account = accountCommandService.createAccount(      // ← 서비스가 도메인 엔티티 Account 를 돌려줌
...
        return AccountResponse.from(account)      // ← 엔티티를 DTO 매퍼에 넘김(동결 위반)
...
        val ledger = accountCommandService.deposit(      // ← 서비스가 AccountLedger 엔티티를 돌려줌
...
            ledgerId = ledger.id!!,      // ← 컨트롤러가 엔티티 필드를 직접 읽고 !! 사용
...
            transactionType = ledger.transactionType!!
// AccountCommandService.kt:42  fun deposit(accountId: Long, command: DepositCommand): AccountLedger {      // ← 서비스 반환 타입이 엔티티
```

- 원인: createAccount·updateAccount 는 Account 엔티티를, deposit·withdraw 는 AccountLedger 엔티티를 서비스에서 받는다(AccountCommandService.kt:24·42·58 의 반환 타입). 핸들러 반환 타입은 DTO 지만 컨트롤러가 ledger.id!!·ledger.transactionType!! 으로 엔티티 필드를 직접 읽는다. AccountResponse.from(account: Account) 도 presentation DTO 가 엔티티를 받는 모양이고 ArchUnit 동결 색인(1284·1293줄)에 올라 있다. 이 컨트롤러 파일에는 domain.entity import 는 없다. 엔티티 노출의 뿌리는 서비스 반환 타입과 DTO 매퍼(청크 밖)에 있다.
- 생길 수 있는 일: JPA 엔티티의 nullable id·지연 로딩 필드가 presentation 까지 올라와 컬럼·엔티티 변경이 API 계층을 직접 깬다. !! 는 id 미할당 시 NPE 가 되고, backend/CLAUDE.md 의 «!! 금지»에도 어긋난다.
- 해결: application 에 결과 타입(AccountResult·LedgerCreatedResult)을 두고 서비스가 그것을 돌려주게 한다. 컨트롤러는 결과 타입만 DTO 로 변환하고 !! 를 쓰지 않는다.

고친 모양(예시):
```
data class LedgerCreatedResult(val ledgerId: Long, val balanceAfter: BigDecimal, val transactionType: TransactionType)

fun deposit(accountId: Long, command: DepositCommand): LedgerCreatedResult

// controller
return LedgerCreatedResponse.from(accountCommandService.deposit(id, DepositCommand(...)))
```

### 313위 [med · 확신 med] 인증·소유 범위 없이 계좌 id 로 입출금·원장 조회

`backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/controller/AccountController.kt:90` · 항목표 reliability/security-config-hygiene · 항목 S7

```
@PostMapping("/{id}/withdraw")
fun withdraw(
    @PathVariable id: Long,  // ← 호출자 소유 검사 없음
    @Valid @RequestBody request: WithdrawRequest
): LedgerCreatedResponse {
```

- 원인: deposit·withdraw·getLedgers·updateAccount·deactivateAccount 가 경로의 id 만으로 계좌를 조작하며 호출자 범위 검사가 없다(인증 부재는 build.gradle 의존성 grep 기준).
- 생길 수 있는 일: 네트워크에 닿는 누구나 임의 계좌에서 출금하거나 비활성화할 수 있다. 인증이 생겨도 소유 범위가 쿼리에 없으면 IDOR 다.
- 해결: 인증 주체의 계좌 범위를 서비스 조회에 포함한다. 인증 없음이 의도라면 근거 문서를 남긴다.

### 314위 [med · 확신 med] 인증 없이 클라이언트가 준 accountId 로 KIS 실계좌 잔고를 조회

`backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/controller/KisAccountBalanceController.kt:16` · 항목표 reliability/security-config-hygiene · 항목 S7

```
@GetMapping("/balance")
fun getBalance(
    @RequestParam accountId: Long,  // ← 호출자 범위 검사 없이 id 만으로 조회
    @RequestParam mode: KisAccountMode,  // ← LIVE 선택 가능
): KisAccountBalanceResult = kisAccountQueryService.getBalance(accountId, mode)
```

- 원인: accountId·mode 쿼리 파라미터만으로 KIS 잔고·보유 종목을 반환하고 호출자 범위 검사가 없다. build.gradle 에는 시험용 spring-security-test 만 있고(grep 결과) main 에 spring-boot-starter-security 가 보이지 않아 인증 자체가 없다. LIVE 모드면 실계좌 정보다.
- 생길 수 있는 일: 네트워크에 닿는 누구나 임의 계좌 id 로 실계좌 잔고·포지션을 읽는다. 인증이 생겨도 같은 모양이면 IDOR 다. 내부용이라는 근거 문서는 이 파일 범위에서 보지 못했다.
- 해결: 인증을 도입하고 조회 쿼리에 호출자 소유 범위를 함께 넣는다. 인증 없음이 의도라면 네트워크 격리 근거를 문서로 남긴다(S10 은 repo 인스턴스 판정).

### 315위 [med · 확신 med] 주문 취소가 주문 id 만으로 조회하고 계좌 범위를 같은 쿼리에 넣지 않음

`backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/controller/OrderController.kt:87` · 항목표 reliability/security-config-hygiene · 항목 S7

```
// OrderController.kt:87
orderCommandService.cancelOrder(accountId, orderId, CancelOrderCommand())   // ← 경로 accountId 를 넘기지만
// OrderCommandService.kt:239-241 (grep 으로 읽은 줄)
fun cancelOrder(accountId: Long, orderId: Long, command: CancelOrderCommand): Order {
    val order = orderRepository.findByIdWithOptimisticLock(orderId)   // ← 주문 id 만으로 조회, 계좌 범위 없음
// OrderCommandService.kt:253
val account = accountRepository.findByIdWithLock(accountId2)   // ← 매개변수 accountId 가 아닌 accountId2 를 사용
// 대조: OrderQueryService.kt:20
val order = orderRepository.findByIdAndAccountId(orderId, accountId)   // ← 조회 쪽은 범위가 쿼리에 들어 있음
```

- 원인: 조회 계열(getOrder 와 getExecution) 은 findByIdAndAccountId·findByIdAndOrderIdAndAccountId 로 호출자 계좌 범위를 쿼리에 넣습니다. 반면 cancelOrder 는 경로의 accountId 를 서비스에 넘기지만, 서비스는 findByIdWithOptimisticLock(orderId) 로 주문 id 만으로 조회합니다. 이후 계좌 락에는 accountId2 라는 별도 변수를 씁니다(주문에서 파생된 값으로 보이며, 매개변수 accountId 가 쓰이지 않거나 사후에만 쓰인다는 신호입니다). 서비스 242~252줄은 열지 못해서, 사후 소유 검사가 있는지는 확인하지 못했습니다. 사후 검사가 있어도 렌즈 S7 은 404 와 403 이 갈리는 모양을 발견으로 봅니다.
- 생길 수 있는 일: /accounts/1/orders/{타계좌 주문 id} 로 DELETE 하면 다른 계좌의 주문이 취소되고 예수금 홀드가 풀릴 수 있습니다. 지금은 인증이 없어서(build.gradle 에 spring-boot-starter-security 없음) 네트워크에 닿는 누구나 가능합니다.
- 해결: 취소 조회도 findByIdAndAccountIdWithLock(orderId, accountId) 처럼 계좌 범위를 쿼리에 넣고, 없으면 OrderNotFoundException(404) 한 가지로 통일합니다.

고친 모양(예시):
```
val order = orderRepository.findByIdAndAccountIdWithOptimisticLock(orderId, accountId)
    .orElseThrow { OrderNotFoundException(orderId) }
```

### 316위 [med · 확신 med] 경로의 accountId 를 호출자 범위 검증 없이 그대로 조회

`backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/controller/PositionController.kt:15` · 항목표 reliability/security-config-hygiene · 항목 S7

```
@GetMapping
fun listPositions(@PathVariable accountId: Long): List<PositionResponse> =   // ← 호출자 소유 검증 없이 경로 id 로 조회
    positionQueryService.listPositionsWithCurrentPrice(accountId)
        .map { PositionResponse.from(it) }
```

- 원인: `accountId` 를 경로 변수로 받아 서비스에 넘기며, 요청한 사람이 그 계좌의 주인인지 확인하는 코드가 이 컨트롤러에 없다. 인증 존재 여부는 repo 단위 S10 이 판정하므로 이 발견은 '현재 호출자 범위가 쿼리에 없다'에 한정된다.
- 생길 수 있는 일: 다른 계좌의 보유 종목·평가손익을 계좌 번호만 바꿔 열람할 수 있다.
- 해결: 인증 주체의 계좌 범위를 쿼리에 포함하거나(`findByIdAndOwner`) 경로의 accountId 와 인증 주체 계좌를 서비스 진입에서 대조한다.

고친 모양(예시):
```
fun listPositions(@AuthenticationPrincipal user: AuthUser, @PathVariable accountId: Long) =
    positionQueryService.listPositionsWithCurrentPrice(user.requireOwnedAccount(accountId))
```

### 317위 [med · 확신 med] 청산 트리거 생성 POST 에 멱등 키도 자연 키 유일 제약도 없음

`backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/controller/PositionExitTriggerController.kt:26` · 항목표 reliability/failure-handling · 항목 I1

```
@PostMapping("/{positionId}/exit-triggers")
fun create(
    @PathVariable positionId: Long,
    @Valid @RequestBody request: CreatePositionExitTriggerRequest,
) = commandService.createPositionTrigger(request.toCommand(positionId)).toResponse()   // ← 멱등 키·유일 제약 없이 매번 새 트리거 생성
```

- 원인: POST 한 번마다 트리거 행이 새로 생기며 `Idempotency-Key` 처리가 없다. 엔티티 `@Table(name = "position_exit_triggers")`(PositionExitTrigger.kt:25)에도 유일 제약이 보이지 않는다. 같은 포지션·유형의 트리거는 오케스트레이터가 청산 비율을 합산(Orchestrator.kt:123-125 `acc.add(decision.exitRatioPercent)`, 상한 100)하므로 이중 클릭·클라이언트 재시도로 만든 중복 트리거가 매도 비율을 두 배로 만든다. 렌즈 문구는 주문·원장·입출금 POST 를 열거하나 이 POST 는 자동 매도 주문을 직접 유발한다(CLAUDE.md 멱등).
- 생길 수 있는 일: 50% 청산 트리거를 한 번 의도했는데 요청이 두 번 들어가면 100% 매도가 나가는 식의 의도하지 않은 대량 매도.
- 해결: `Idempotency-Key` 헤더를 받아 (positionId, key) 유일 제약으로 원자 예약하고 중복이면 저장된 응답을 재생하거나, 자연 키(포지션·유형·기준)에 유일 제약을 둔다.

고친 모양(예시):
```
@PostMapping("/{positionId}/exit-triggers")
fun create(
    @PathVariable positionId: Long,
    @RequestHeader("Idempotency-Key") idempotencyKey: String,
    @Valid @RequestBody request: CreatePositionExitTriggerRequest,
) = commandService.createPositionTrigger(request.toCommand(positionId, idempotencyKey)).toResponse()
// 테이블: UNIQUE (position_id, idempotency_key)
```

### 318위 [med · 확신 med] @Scheduled 작업 둘이 기본 단일 스레드 스케줄러를 공유함

`backend/trading-api/src/main/kotlin/com/papertrading/api/TradingApiApplication.kt:11` · 항목표 reliability/load-concurrency · 항목 C1

```
@EnableScheduling   // ← 스케줄러 풀 크기 설정 없음 → 단일 스레드
...
// SlackNotificationScheduler.kt:31
                val success = sender.send(claimedNotification.message)   // ← 느린 외부 호출이 같은 스레드를 점유
```

- 원인: application.yaml 에 spring.task.scheduling.pool.size 설정이 없고 TaskScheduler 빈도 없다. LocalMatchingScheduler.cancelExpiredGtdOrders 와 SlackNotificationScheduler.retryPending 이 같은 단일 스레드에서 돌고, 후자는 알림마다 sender.send(타임아웃 3초)를 순차 호출한다.
- 생길 수 있는 일: Slack 전송이 느리면 GTD 만료 취소와 놓친 주문 복구 틱이 밀려 체결·취소가 늦어진다.
- 해결: spring.task.scheduling.pool.size 를 2 이상으로 두거나 Slack 전송을 별도 실행기로 분리한다.

고친 모양(예시):
```
spring:
  task:
    scheduling:
      pool:
        size: 4
```

### 319위 [med · 확신 med] Redis 연결·명령 타임아웃이 명시되지 않음

`backend/trading-api/src/main/resources/application.yaml:30` · 항목표 reliability/failure-handling · 항목 E5

```
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}   # ← timeout·connect-timeout 명시 없음
```

- 원인: spring.data.redis 에 host·port 만 있고 timeout·connect-timeout 이 없다. RestTemplate(5초·10초)과 hikari(30초)는 명시돼 있어 대비된다. 기본값은 라이브러리에 의존하며 그 값은 확인하지 못했다. 이 묶음의 Redis 사용처는 RedisListenerConfig 의 리스너 컨테이너다.
- 생길 수 있는 일: Redis 가 느리거나 반쯤 끊기면 호출이 라이브러리 기본 시간 동안 붙잡혀 스레드가 묶인다.
- 해결: spring.data.redis.timeout 과 connect-timeout 을 요청 경로 예산에 맞게 명시한다.

고친 모양(예시):
```
  data:
    redis:
      timeout: 2s
      connect-timeout: 2s
```

### 320위 [med · 확신 med] 마이그레이션은 시각 열을 TIMESTAMP(시간대 없음)로 선언하고 엔티티는 Instant 를 씀

`backend/trading-api/src/main/resources/db/migration/V6__create_account_exit_trigger_defaults.sql:8` · 항목표 data/schema-data-model · 항목 M6

```sql
created_at TIMESTAMP NOT NULL DEFAULT now(),   // ← 시간대 없는 열 + 서버 시간대 기본값
updated_at TIMESTAMP NOT NULL DEFAULT now()
// BaseTimeEntity: var createdAt: Instant? (jdbc.time_zone: UTC)
```

- 원인: V6(와 V4·V5·V7)은 created_at·updated_at 을 TIMESTAMP 로 선언하고 DEFAULT now() 를 쓴다. 엔티티의 시각은 Instant 이고 jdbc.time_zone 은 UTC 다. ddl-auto: create 로 만들어지는 테이블은 Hibernate 6 기본 매핑상 timestamptz 가 될 가능성이 높다(실 DB 없이 판정해 확신은 중간). 한 레포에 두 형이 섞인다.
- 생길 수 있는 일: 조인·비교에서 캐스트가 필요해지고, DB 기본값(now())은 서버 시간대 기준 값이라 앱이 쓰는 UTC 값과 9시간까지 어긋날 수 있다.
- 해결: 레포의 시각 열 정책(timestamptz+UTC 또는 naive 로컬)을 정하고 새 마이그레이션부터 통일한다. 기존 열 변경은 별도 마이그레이션으로 한다.

### 321위 [med · 확신 med] V7 이 근거 주석 없이 테이블을 DROP 하고, 적용된 마이그레이션을 제자리에서 고친 흔적

`backend/trading-api/src/main/resources/db/migration/V7__create_position_exit_triggers.sql:1` · 항목표 data/schema-data-model · 항목 M9

```sql
DROP TABLE IF EXISTS position_exit_triggers;     -- ← L1 근거 주석 없는 파괴적 DDL

CREATE TABLE position_exit_triggers (
...
  skip_reason VARCHAR(32),                         -- ← V7 을 고쳐 쓰며 추가됨, V11 은 -- no-op 주석만 있는 빈 마이그레이션
```

- 원인: V7 첫 줄이 DROP TABLE IF EXISTS position_exit_triggers 로 시작한다. 파일 안에 근거 주석이 없고, V11 의 주석이 V7 을 개발 DB 용으로 다시 만든 것이라고 적는다. 이미 적용된 V7 을 고쳐 쓰고 V11 을 빈 파일(no-op)로 둔 것은 앞으로 고치는 방식이 아니다. 이전 V7 체크섬으로 적용된 DB 는 Flyway 검증이 실패하고, 복구해도 새 V7 이 다시 돌지 않아 skip_reason 컬럼이 없는 채 남는다(엔티티는 skip_reason 매핑). 운영 DB 배포 여부는 확인하지 못했다.
- 생길 수 있는 일: V7 이 데이터가 있는 DB 에서 돌면 트리거 행이 사라진다. 이전 V7 을 적용한 DB 는 skip_reason 컬럼 없이 남아 SKIPPED 기록 시 런타임 오류가 난다.
- 해결: V7 은 최초 판본 그대로 되돌리고, V11 에서 ALTER TABLE position_exit_triggers ADD COLUMN IF NOT EXISTS skip_reason VARCHAR(32) 로 앞으로 고친다. DROP 이 필요하면 근거 주석을 같은 파일에 적는다.

고친 모양(예시):
```sql
-- V11: skip_reason 추가 (V7 은 원 판본 유지)
ALTER TABLE position_exit_triggers ADD COLUMN IF NOT EXISTS skip_reason VARCHAR(32);
```

### 322위 [med · 확신 med] 정합성 시험이 PositionRepository 목으로 수량 조건 쿼리를 흉내 냄

`backend/trading-api/src/test/kotlin/com/papertrading/api/application/account/KisAccountQueryServiceTest.kt:25` · 항목표 backend/tdd-purity · 항목 T2

```
    private val positionRepository = mockk<PositionRepository>()      // ← Spring Data 리포지토리 목
...
        every { positionRepository.findByAccountIdAndQuantityGreaterThan(any(), BigDecimal.ZERO) } returns listOf(      // ← 수량 조건 쿼리를 목 반환으로 흉내(89줄)
```

- 원인: mockk<PositionRepository> 의 findByAccountIdAndQuantityGreaterThan 이 반환한 목록으로 «수량 > 0» 쿼리 의미를 대신한다. KIS 쪽은 포트 가짜(KisAccountBalancePort)라 허용되지만 로컬 포지션 쪽은 Spring Data 목이다.
- 생길 수 있는 일: 실제 쿼리의 수량 조건이나 정렬이 어긋나도 정합성 계산 시험은 계속 초록이다.
- 해결: 로컬 포지션 조회를 포트(PositionReadPort)로 올려 가짜로 바꾸고, 수량 조건 쿼리는 Testcontainers 시험으로 덮는다.

### 323위 [med · 확신 med] 새 정책 생성 단언이 save(any()) 호출 횟수뿐임

`backend/trading-api/src/test/kotlin/com/papertrading/api/application/account/RiskPolicyServiceTest.kt:63` · 항목표 backend/tdd-purity · 항목 T3

```
        every { riskPolicyRepository.save(any()) } answers { firstArg() }

        service.replaceRiskPolicy(1L, command)      // ← 반환값을 버림

        assertThat(existingPolicy.isActive).isFalse()
        verify(exactly = 1) { riskPolicyRepository.save(any()) }      // ← 새 정책은 내용 단언 없이 호출 횟수만
```

- 원인: «기존 활성 정책이 있으면 비활성화하고 새 정책을 생성한다» 시험은 service.replaceRiskPolicy 의 반환값을 버리고 기존 정책 비활성만 단언한다. 새 정책 쪽은 save(any()) 의 exactly = 1 호출 검증이 전부다.
- 생길 수 있는 일: 새 정책의 비율이 틀리거나 비활성 상태로 저장돼도 save 가 한 번만 불리면 초록이다.
- 해결: 반환된 새 정책의 비율·활성 여부를 결과로 단언하고 호출 횟수 검증은 빼거나 보조로 둔다.

고친 모양(예시):
```
val result = service.replaceRiskPolicy(1L, command)
assertThat(existingPolicy.isActive).isFalse()
assertThat(result.isActive).isTrue()
assertThat(result.maxPositionRatio).isEqualByComparingTo("0.3")
```

### 324위 [med · 확신 med] BigDecimal 단언이 assertEquals 로 scale 까지 비교

`backend/trading-api/src/test/kotlin/com/papertrading/api/application/order/OrderCommandServiceTest.kt:100` · 항목표 backend/tdd-purity · 항목 T8

```
assertEquals(BigDecimal("650000"), account.availableDeposit) // ← equals — scale 까지 비교
...
assertEquals(BigDecimal("856000"), account.availableDeposit)
```

- 원인: assertEquals(BigDecimal("650000"), account.availableDeposit)·assertEquals(BigDecimal("856000"), …) 은 equals 라 scale 이 다르면 값이 같아도 빨강이다. 지금은 입력이 정수 문자열이라 통과하지만 잠금 계산에 scale 을 주는 변경에 취약하다(PTB4).
- 생길 수 있는 일: 금액 계산의 scale 정책을 바꾸면 값은 맞는데 시험이 깨져 진짜 회귀와 구분되지 않는다.
- 해결: compareTo 기반 단언으로 바꾼다.
- 다른 렌즈의 반증: backend/code-shape+layering+naming-terminology+tdd-purity — 수집기 구독 출력 포트(외부 경계) 호출 검증이고 바로 앞에서 주문 상태·예수금 결과를 단언한다.

고친 모양(예시):
```
assertEquals(0, BigDecimal("650000").compareTo(account.availableDeposit))
```

### 325위 [med · 확신 med] 내부 협력자 호출 횟수 verify 와 @Transactional 리플렉션 단언이 결과 단언을 대신함

`backend/trading-api/src/test/kotlin/com/papertrading/api/application/portfolio/SnapshotJobServiceTest.kt:71` · 항목표 backend/tdd-purity · 항목 T3

```
verify(exactly = 1) { snapshotJobRunTxService.fail(10L, any()) }  // ← 호출 횟수가 곧 유일한 «실패 기록» 단언
verify(exactly = 0) { dailyBalanceCommandService.recalculate(any(), any()) }
val transactional = SnapshotJobService::class.java
    .getDeclaredMethod("generateDailySnapshots", Long::class.java, LocalDate::class.java)
    .getAnnotation(Transactional::class.java)  // ← 리플렉션으로 어노테이션 존재만 확인
```

- 원인: 실패 이력 기록은 verify(exactly = 1) 로만 확인하고(71행), 재계산 생략도 verify(exactly = 0) 둘로 확인한다(88~89행). 93~98행 시험은 리플렉션으로 @Transactional 존재만 단언해 구현 형태에 묶인다. 실제 롤백 동작은 통합 시험이 따로 맡는다.
- 생길 수 있는 일: 행동이 틀려도 호출만 맞으면 초록이고, 어노테이션만 두고 프록시·자기 호출로 동작하지 않아도 이 시험은 통과한다.
- 해결: 결과(실행 이력 상태 FAILED·반환값)로 단언하고 어노테이션 시험은 지운다(통합 시험이 맡음).

고친 모양(예시):
```
assertEquals(SnapshotJobRunStatus.FAILED, fakeJobRunPort.lastRun().status)
```

### 326위 [med · 확신 med] 리포지토리 목이 실행 중 조회·유일 제약 위반을 흉내 냄

`backend/trading-api/src/test/kotlin/com/papertrading/api/application/portfolio/tax/TaxSummaryCommandServiceTest.kt:96` · 항목표 backend/tdd-purity · 항목 T2

```
every { taxSummaryRunRepository.existsRunning(1L, 2024) } returns false  // ← 쿼리 결과를 목이 정함
every { taxSummaryRunRepository.save(any<TaxSummaryRun>()) } throws
    DataIntegrityViolationException("uq_tax_summary_runs_running_account_year")  // ← 제약 위반을 목이 지어냄
```

- 원인: existsRunning 결과와 save 의 DataIntegrityViolationException 을 목이 정한다. TaxSummaryRun 이 IDENTITY 키라 save 에서 insert 가 일어나는 점은 맞지만 실제 유일 인덱스 이름·동작은 확인되지 않는다.
- 생길 수 있는 일: 인덱스 정의가 바뀌어도 중복 방지 시험이 계속 초록이다.
- 해결: 포트 가짜 + 실제 DB 로 유일 제약 확인.
- 다른 렌즈의 반증: backend/code-shape+layering+naming-terminology+tdd-purity — TaxSummaryRun 이 IDENTITY 키라 save 시점에 insert 가 일어나 catch 위치는 맞다. 목이라 인덱스 동작을 못 본다는 점은 T2 로 적었다.

고친 모양(예시):
```
@Testcontainers class TaxSummaryRunPersistenceConstraintTest { /* 실행 중 행 두 번 저장 시 예외 */ }
```

### 327위 [med · 확신 med] 실패·배치 경로 시험의 유일한 단언이 리포지토리 save 호출 횟수·인자 verify

`backend/trading-api/src/test/kotlin/com/papertrading/api/application/portfolio/tax/TaxSummaryCommandServiceTest.kt:121` · 항목표 backend/tdd-purity · 항목 T3

```
verify(exactly = 2) { taxSummaryRunRepository.save(any<TaxSummaryRun>()) }  // ← 호출 횟수가 단언의 전부
verify(exactly = 0) { taxSummaryRepository.save(any<TaxSummary>()) }
verify { taxSummaryRunRepository.save(match<TaxSummaryRun> { it.runType == TaxSummaryRunType.YEAR_END_BATCH }) }  // ← 배치 시험의 유일한 단언
```

- 원인: 실패 시 «실패 실행 이력 저장, 요약 미저장»은 verify(exactly = 2/0) 로만 확인하고, 배치 시험(143~158행)은 단언이 verify 하나뿐이다.
- 생길 수 있는 일: run.status 가 FAILED 로 안 바뀌어도 save 만 두 번 불리면 통과한다.
- 해결: 저장된 run 의 status·runType 을 캡처해 값으로 단언한다.

고친 모양(예시):
```
val saved = slot<TaxSummaryRun>(); every { save(capture(saved)) } answers { saved.captured }
assertEquals(TaxSummaryRunStatus.FAILED, saved.captured.status)
```

### 328위 [med · 확신 med] 시험 이름이 약속한 정책(동일 계좌만 수정)을 시험하지 않고 다음 시험이 반대 동작을 고정함

`backend/trading-api/src/test/kotlin/com/papertrading/api/application/portfolio/TradingJournalCommandQueryServiceTest.kt:66` · 항목표 backend/tdd-purity · 항목 T6

```
66: fun `update는 동일 계좌 journal만 수정한다`() {  // ← 불일치 계좌 케이스가 없음
88: fun `update는 accountId와 무관하게 journal을 수정한다`() {  // ← 소유 검사 없는 동작을 의도로 고정
95:     val result = commandService.update(9L, UpdateTradingJournalCommand(2L, "new", "new", null))
```

- 원인: `update는 동일 계좌 journal만 수정한다` 는 불일치 계좌 케이스가 없고 단순 수정만 확인한다. 바로 아래 시험(88행)은 `update는 accountId와 무관하게 journal을 수정한다` 로 소유 검사 없는 동작을 못 박는다. 이름이 정책을 거짓으로 말해 소유 검사 누락(D3 발견)을 가린다.
- 생길 수 있는 일: 일지 소유권 규칙이 시험 이름에는 있고 코드에는 없는 채로 남는다.
- 해결: 소유 불일치 시 예외를 기대하는 시험으로 바꾸고 이름은 상황·행동·기대로 맞춘다.

고친 모양(예시):
```
@Test fun `다른 계좌의 journal 을 수정하면 소유 불일치 예외가 난다`() { assertThrows<TradingJournalOwnershipMismatchException> { commandService.update(9L, UpdateTradingJournalCommand(2L, "new", "new", null)) } }
```

### 329위 [med · 확신 med] BigDecimal 결과를 assertEquals 로 scale 까지 비교

`backend/trading-api/src/test/kotlin/com/papertrading/api/application/position/PositionExitTriggerCommandServiceTest.kt:56` · 항목표 backend/tdd-purity · 항목 T8

```
assertEquals(BigDecimal("107.0000"), result.triggerPrice)  // ← 계산된 금액을 scale 까지 비교 (81행)
assertEquals(BigDecimal("100.0000"), result.exitRatioPercent)  // ← (58행)
```

- 원인: result.triggerPrice·exitRatioPercent·triggerPercent 를 assertEquals(BigDecimal("95.0000"), ...) 로 비교한다(56·58·80·81·83·103·106행). 특히 81행은 평균단가에 7%를 곱해 계산한 값이라 계산 경로의 scale 이 바뀌면 값이 같아도 빨강이 된다. 같은 묶음의 Evaluator 시험은 compareTo 를 쓴다.
- 생길 수 있는 일: scale 정규화 방식이 바뀌면(4자리 → 8자리) 금액이 맞아도 깨지고, 반대로 scale 만 맞고 값이 틀린 경우도 구분 근거가 약하다.
- 해결: assertThat(...).isEqualByComparingTo(...) 또는 assertEquals(0, expected.compareTo(actual)) 로 바꾸고, scale 4 저장이 계약이면 따로 한 시험으로 못박는다.

고친 모양(예시):
```
assertThat(result.triggerPrice).isEqualByComparingTo("107.0000")
```

### 330위 [med · 확신 med] 내부 서비스 AutoExitOrderPlacementService·Slack 발행기와 도메인 엔티티 Order 를 mockk 로 갈음

`backend/trading-api/src/test/kotlin/com/papertrading/api/application/position/PositionExitTriggerOrchestratorTest.kt:37` · 항목표 backend/tdd-purity · 항목 T1

```
private val autoExitOrderPlacementService = mockk<AutoExitOrderPlacementService>()  // ← 내부 서비스 목
private val notificationEventPublisher = mockk<SlackNotificationEventPublisher>(relaxed = true)  // ← application 클래스 목
...
} returns mockk<Order>()  // ← 도메인 엔티티를 목으로 지어냄 (83·115·173·242행)
```

- 원인: 같은 레포 application 서비스 두 개(AutoExitOrderPlacementService:37, application.notification.SlackNotificationEventPublisher:39)를 mockk 로 만들고, 83·115·173·242행에서 mockk<Order>() 로 도메인 엔티티를 지어낸다. Slack 발행기는 허용 목록(infrastructure.notification.SlackWebhookNotifier)이 아니라 application 클래스다. 트리거 상태 단언은 실제 엔티티로 하므로 기본 심각도 high 에서 낮췄다.
- 생길 수 있는 일: 주문 생성 서비스 시그니처가 바뀌면 any() 6개짜리 스텁이 전부 깨지고, 가짜 Order 는 불변식을 타지 않는다.
- 해결: 주문 배치를 AutoExitOrderPlacementPort 로 올려 기록용 가짜를 쓰고, 알림은 발행 포트의 가짜로 바꾸며 Order 는 Order.create 로 만든다.

고친 모양(예시):
```
class RecordingAutoExitOrderPort : AutoExitOrderPort { val created = mutableListOf<GroupedOrder>() ... }
```

### 331위 [med · 확신 med] BigDecimal 단언 54곳이 assertEquals 로 scale 까지 비교

`backend/trading-api/src/test/kotlin/com/papertrading/api/domain/entity/account/AccountTest.kt:24` · 항목표 backend/tdd-purity · 항목 T8

```
assertEquals(BigDecimal("100000.0000"), account.deposit) // ← scale 까지 비교
assertEquals(BigDecimal.ZERO, p0.maxPositionRatio) // ← L226
assertEquals(BigDecimal("2.5"), trigger.stopLossPercent) // ← L260
private fun assertZero(value: BigDecimal) =
    assertEquals(0, value.compareTo(BigDecimal.ZERO), "Expected zero but was $value") // ← 올바른 방식이 같은 파일에 있음
```

- 원인: assertEquals(BigDecimal("100000.0000"), account.deposit) 처럼 기댓값 리터럴의 scale 이 결과 scale 과 같아야 통과한다. PTB4 는 compareTo 비교를 정한다. 같은 파일의 assertZero·assertInvariant 는 compareTo 를 쓴다(L626-635). 지금은 입력 scale 이 그대로 흘러 통과하지만 연산 순서나 scale 정규화가 바뀌면 값이 같아도 빨개진다.
- 생길 수 있는 일: scale 만 다른 같은 금액으로 시험이 깨지거나, 반대로 scale 에 기대는 시험이 되어 금액 정책 변경을 막는다.
- 해결: assertAmountEquals(expected, actual) 같은 compareTo 헬퍼를 두고 BigDecimal 단언을 모두 그것으로 바꾼다.

고친 모양(예시):
```
private fun assertAmountEquals(expected: BigDecimal, actual: BigDecimal) =
    assertEquals(0, expected.compareTo(actual), "expected=$expected actual=$actual")
```

### 332위 [med · 확신 med] 전략 리포지토리 5개에 실제 엔진(Testcontainers) 시험이 없음

`backend/trading-api/src/test/kotlin/com/papertrading/api/domain/entity/strategy/StrategyDomainEntitiesTest.kt:18` · 항목표 backend/tdd-purity · 항목 T5

```
class StrategyDomainEntitiesTest {   // ← 순수 도메인 단위 시험뿐, Testcontainers·@DataJpaTest 없음
// StrategyVersionRepository.kt:9  fun findTopByStrategyIdOrderByVersionNoDesc(strategyId: Long): Optional<StrategyVersion>   // ← 시험 없음
// StrategyVersion.kt:26  uniqueConstraints = [UniqueConstraint(name = "uk_strategy_versions_strategy_version", ...)]   // ← 시험 없음
```

- 원인: 전략 컨텍스트의 영속 동작 — StrategyVersion 의 유일 제약 uk_strategy_versions_strategy_version, findTopByStrategyIdOrderByVersionNoDesc(Top1·정렬), findByAccountIdAndIsActiveTrueAndApprovalStatus(isActive 속성명 파생), JSONB(@JdbcTypeCode(SqlTypes.JSON)) 매핑 — 를 검증하는 시험이 없다. test 소스에서 전략 리포지토리·엔티티를 참조하는 파일은 이 도메인 단위 시험 하나뿐이다(grep). 이 레포 테스트 소스 전체가 아닌 이름 grep 근거라 confidence 는 med.
- 생길 수 있는 일: 파생 질의 이름 오타·컬럼 길이·유일 제약 위반·JSONB 매핑 오류가 운영 DB 에 처음 닿을 때 터진다. 전략 컨텍스트에 소비자가 아직 없어 지금은 드러나지 않을 뿐이다.
- 해결: PostgreSQLContainer 기반 리포지토리 시험을 추가해 유일 제약 위반·Top1 정렬·isActive 파생 질의·JSONB 라운드트립을 검증한다(PTT1).

고친 모양(예시):
```
@Test fun `같은 전략의 같은 버전 번호를 저장하면 유일 제약 위반이 난다`() { /* Testcontainers PostgreSQL + saveAndFlush 두 번 */ }
```

### 333위 [med · 확신 med] BigDecimal 단언이 assertEquals(scale 까지 비교)

`backend/trading-api/src/test/kotlin/com/papertrading/api/domain/entity/strategy/StrategyDomainEntitiesTest.kt:206` · 항목표 backend/tdd-purity · 항목 T8

```
assertEquals(BigDecimal("-0.2500"), strategy.maxDrawdown)   // ← scale 까지 비교 (L286·L328 도 동일)
```

- 원인: L206·286·328 이 assertEquals(BigDecimal(...), 값) 으로 scale 까지 비교한다. 지금은 입력과 같은 객체를 되돌리는 에코라 통과하지만 엔티티가 scale 을 정규화하지 않으므로(예: L327 의 BigDecimal("1.2") 는 컬럼 scale 4 와 다름) 정규화를 넣는 순간 의미 없이 깨지고, 반대로 scale 불일치는 이 시험으로 드러나지 않는다.
- 생길 수 있는 일: 금액·비율 시험이 값 동일성이 아니라 표기 동일성을 지켜 scale 변경이 가짜 빨강을 만들고 진짜 계산 오차는 놓친다.
- 해결: compareTo 기반 단언으로 바꾼다.

고친 모양(예시):
```
assertEquals(0, BigDecimal("-0.2500").compareTo(strategy.maxDrawdown))
```

### 334위 [med · 확신 med] KIS 응답 JSON·오류 코드를 출처 없이 손으로 적은 가짜

`backend/trading-api/src/test/kotlin/com/papertrading/api/infrastructure/kis/KisAccountBalanceAdapterTest.kt:49` · 항목표 backend/tdd-purity · 항목 T11

```
            mapOf(
                "rt_cd" to "0",      // ← 출처 없는 KIS 응답 필드
                "output1" to listOf(
                    mapOf(
                        "pdno" to "005930",
...
            ResponseEntity.ok(mapOf("rt_cd" to "7", "msg1" to "권한 없음"))      // ← 출처 없는 오류 코드(122줄)
```

- 원인: rt_cd·output1·pdno·hldg_qty·evlu_erng_rt 필드와 "rt_cd" to "1"(인증)·"7"(권한)·"9"(기타) 코드를 시험이 직접 적었고 KIS 문서나 녹취 픽스처 출처가 없다. 어댑터(92~98줄)가 같은 코드 의미를 믿어 시험과 코드가 같은 믿음을 복사한다.
- 생길 수 있는 일: KIS 실제 응답 형식이나 rt_cd 의미가 다르면 둘 다 초록인 채 운영에서 어긋난다. 인증·권한 구분이 틀리면 재시도·알림 분기가 어긋난다.
- 해결: KIS 문서 링크를 주석으로 달거나 실제 응답 녹취를 src/test/resources 픽스처 JSON 으로 두고 시험이 그 파일을 읽게 한다.

### 335위 [med · 확신 med] 내부 파서·application 서비스를 relaxed 목으로 갈음 — 수신 경로 미시험

`backend/trading-api/src/test/kotlin/com/papertrading/api/infrastructure/kis/KisExecutionWebSocketManagerTest.kt:35` · 항목표 backend/tdd-purity · 항목 T1

```
    private val parser = mockk<KisExecutionNoticeParser>(relaxed = true)  // ← 같은 레포 순수 파서를 relaxed 목으로 갈음, 이 파일에서 사용하지 않음
    private val service = mockk<KisExecutionNoticeService>(relaxed = true)  // ← application 서비스를 relaxed 목으로 갈음, stub·verify 없음
```

- 원인: tokenManager·transport·StandardWebSocketClient 는 KIS 경계라 통과다. 그러나 KisExecutionNoticeParser 는 HTTP·WS 경계가 아닌 순수 파싱 로직이고 KisExecutionNoticeService 는 application 서비스(내부)인데 둘 다 mockk(relaxed = true)이고 이 파일 어디에서도 stub·verify 하지 않는다 — 생성자를 채우는 더미다. 그래서 매니저가 수신 원문을 파서→서비스로 넘기는 경로(ArchUnit 출력상 KisExecutionWebSocketManager.kt:229 가 서비스 handle 호출)가 시험되지 않는다.
- 생길 수 있는 일: 원문 수신→파싱→체결 서비스 호출이라는 핵심 경로가 가짜 초록의 자리가 되고, relaxed 목이라 호출 누락·인자 변경이 시험에 안 잡힌다.
- 해결: 파서는 실제 인스턴스를 쓰고 서비스는 호출을 기록하는 포트 가짜로 바꿔 handleRawMessage 시험(원문 → 서비스 호출 인자)을 추가한다.

고친 모양(예시):
```
private val parser = KisExecutionNoticeParser()
private val recorded = mutableListOf<KisExecutionNotice>()
// handleRawMessage("paper", rawMessage) 뒤 recorded.single().externalExecutionId 단언
```

### 336위 [med · 확신 med] 같은 레포의 영속 헬퍼(NotificationPersistenceTxService)를 목으로 갈음해 저장 결과를 지어냄

`backend/trading-api/src/test/kotlin/com/papertrading/api/infrastructure/notification/SlackNotificationEventHandlerTest.kt:21` · 항목표 backend/tdd-purity · 항목 T1

```
    private val sender = mockk<NotificationSender>()
    private val txService = mockk<NotificationPersistenceTxService>()   // ← 내부 영속 헬퍼 목
...
        every { txService.save(any()) } returns savedNotification        // ← 저장 결과를 목이 지어냄
```

- 원인: 허용 목록의 외부 경계는 NotificationSender(Slack)·RestTemplate 입니다. NotificationPersistenceTxService 는 REQUIRES_NEW 트랜잭션과 리포지토리를 감싼 같은 레포 클래스라 내부 협력자인데 mockk 로 대체하고, save 의 반환 결과도 every 로 지어냅니다. 시험이 Notification 도메인 객체는 실제로 만들어 쓰기 때문에 심각도를 high 에서 med 로 낮췄습니다.
- 생길 수 있는 일: 핸들러가 저장·상태 전이를 올바르게 하는지가 목의 약속에 묶여, 영속 헬퍼 시그니처를 바꿀 때마다 시험이 깨지고, 실제 커밋·REQUIRES_NEW 동작은 어느 시험도 보지 않아 거짓 초록이 됩니다.
- 해결: 영속 경계를 application 포트로 올려 인메모리 가짜 구현으로 시험하고, 실제 커밋 경로는 Testcontainers 시험으로 따로 확인합니다.

고친 모양(예시):
```
class InMemoryNotificationStore : NotificationStorePort {
    val rows = mutableListOf<Notification>()
    override fun save(n: Notification) = n.also { rows += it }
}
// 시험: handler.handle(event) 뒤 assertEquals(DeliveryStatus.DELIVERED, store.rows.single().status)
```

### 337위 [med · 확신 med] 결과 단언 없이 내부 협력자 호출 횟수·순서(verify·verifyOrder)만 단언

`backend/trading-api/src/test/kotlin/com/papertrading/api/infrastructure/notification/SlackNotificationEventHandlerTest.kt:94` · 항목표 backend/tdd-purity · 항목 T3

```
        verify(exactly = 1) { txService.save(any()) }          // ← 내부 협력자 호출 횟수만 단언
        verify(exactly = 1) { txService.markDelivered(any()) }
        verify(exactly = 1) { sender.send(any()) }
...
        verifyOrder {                                           // ← L133 내부 호출 순서를 못 박음
            txService.save(any())
            sender.send(any())
        }
```

- 원인: 시험 3 은 verify 세 줄만 단언합니다. 시험 5 는 verifyOrder 로 txService.save → sender.send 호출 순서를 못 박습니다. txService 는 내부 협력자이므로 구현 방식(어떤 메서드를 몇 번·어떤 순서로 부르는가)에 시험이 묶입니다.
- 생길 수 있는 일: 내부 호출 구조를 리팩터링하면 행동이 그대로여도 시험이 빨개지고, 반대로 최종 상태가 틀려도 호출만 맞으면 초록입니다.
- 해결: 인메모리 가짜 저장소를 쓰고 처리 뒤 알림 상태(PENDING·DELIVERED)와 발송된 메시지로 단언합니다. 호출 순서 단언은 지웁니다.

고친 모양(예시):
```
handler.handle(makeEvent())
assertEquals(DeliveryStatus.DELIVERED, store.rows.single().status)
assertEquals(listOf("체결 알림"), sender.sent)
```

### 338위 [med · 확신 med] AFTER_COMMIT 리스너를 직접 호출하고 영속은 목이라 «PENDING 유지»가 인메모리 값에 대한 단언이 됨

`backend/trading-api/src/test/kotlin/com/papertrading/api/infrastructure/notification/SlackNotificationEventHandlerTest.kt:100` · 항목표 backend/tdd-purity · 항목 T10

```
    fun `즉시 전송 실패 시 txService save 1회만 호출 (PENDING 저장만), markDelivered 없음, status는 PENDING 유지`() {   // ← 영속 결과를 주장
...
        handler.handle(makeEvent())                                      // ← 리스너 직접 호출, 커밋 경로 우회
...
        assertEquals(DeliveryStatus.PENDING, savedSlot.captured.status)   // ← 목에 넘긴 인메모리 객체의 기본값 — 커밋과 무관하게 항상 참
```

- 원인: 핸들러는 @TransactionalEventListener(AFTER_COMMIT)·@Async 입니다(SlackNotificationEventHandler.kt L21-22). 시험은 handle() 을 직접 호출하고 txService 목을 쓰므로 어노테이션 배선과 REQUIRES_NEW 커밋 경로가 실행되지 않습니다. 그런데 이름은 «PENDING 저장만 … status는 PENDING 유지»라고 영속 결과를 주장합니다. 실제 단언은 목에 넘긴 객체의 status 이며, 이는 Notification.create 가 기본값 PENDING 으로 만든 값이라 구현이 어떻든 항상 참입니다.
- 생길 수 있는 일: 핸들러가 커밋 전에 불리거나 실패가 PENDING 저장을 롤백해도 이 시험은 초록이라, Slack 장애 때 알림이 유실되는 퇴행을 못 잡습니다.
- 해결: 실제 PostgreSQL 에서 트랜잭션 안에 이벤트를 발행하고 커밋 뒤 저장 행의 status 를 읽어 검증하는 통합 시험을 추가합니다. 현재 시험의 자명한 assertEquals 는 지웁니다.

고친 모양(예시):
```
transactionTemplate.executeWithoutResult { publisher.publishEvent(event) }
await.untilAsserted {
    assertEquals(DeliveryStatus.PENDING, notificationRepository.findAll().single().status)   // Slack 실패를 가짜 sender 로 구성
}
```

### 339위 [med · 확신 med] 영속 헬퍼 목이 선점 성공·실패 결과를 지어냄

`backend/trading-api/src/test/kotlin/com/papertrading/api/infrastructure/notification/SlackNotificationSchedulerTest.kt:15` · 항목표 backend/tdd-purity · 항목 T1

```
    private val txService = mockk<NotificationPersistenceTxService>()   // ← 내부 영속 헬퍼 목
...
        every { txService.markDelivering(notification) } returns null        // ← 선점 실패 결과를 목이 지어냄
```

- 원인: txService 목이 markDelivering 의 결과를 answers 로 대신 흉내 내고(L75-78), 선점 실패는 returns null 로 지어냅니다(L120). 실제 claimForDelivery 의 원자 선점 결과는 목이 약속한 값일 뿐입니다. 도메인 엔티티는 실제 객체를 쓰므로 심각도를 med 로 낮췄습니다.
- 생길 수 있는 일: 선점 규칙이 바뀌어도 이 시험은 계속 초록이라, 중복 발송 방지가 깨져도 시험으로는 알 수 없습니다.
- 해결: 선점·저장을 포트로 올려 인메모리 가짜로 스케줄러 분기를 시험하고, 선점 UPDATE 는 실제 PostgreSQL 시험으로 따로 검증합니다.

고친 모양(예시):
```
class FakeNotificationStore : NotificationStorePort {
    override fun claim(n: Notification): Notification? =
        if (n.status == DeliveryStatus.PENDING) n.markDelivering() else null
}
```

### 340위 [med · 확신 med] 내부 협력자 호출 횟수와 인자(오류 메시지 문자열)를 결과 단언 대신 못 박음

`backend/trading-api/src/test/kotlin/com/papertrading/api/infrastructure/notification/SlackNotificationSchedulerTest.kt:84` · 항목표 backend/tdd-purity · 항목 T3

```
        verify(exactly = 1) { txService.markDelivering(notification) }   // ← 내부 호출 횟수
        verify(exactly = 1) { txService.markDelivered(any()) }
...
        verify(exactly = 1) { txService.markFailed(notification, "Slack webhook returned failure", maxRetries) }   // ← L108 내부 호출 인자·메시지 문구에 묶임
```

- 원인: txService 호출 횟수를 verify 로 단언하고(L84-86), markFailed 의 오류 메시지 리터럴과 maxRetries 인자까지 그대로 고정합니다(L108). 결과 상태 단언은 없습니다.
- 생길 수 있는 일: 오류 메시지 문구만 바꿔도 시험이 깨지고, 알림이 실제로 어떤 상태로 끝나는지는 시험이 보증하지 못합니다.
- 해결: 인메모리 가짜 저장소로 처리 후 알림의 status·attemptCount·lastError 를 단언합니다.

고친 모양(예시):
```
scheduler.retryPending()
assertEquals(DeliveryStatus.PENDING, store.rows.single().status)
assertEquals(1, store.rows.single().attemptCount)
```

### 341위 [med · 확신 med] 의존 없는 내부 레지스트리를 목으로 갈음하고 등록 호출은 단언하지 않음

`backend/trading-api/src/test/kotlin/com/papertrading/api/presentation/controller/ExecutionSseControllerTest.kt:28` · 항목표 backend/tdd-purity · 항목 T1

```
            val registry = mockk<ExecutionSseRegistry>()  // ← 의존 없는 같은 레포 인메모리 클래스를 목으로 갈음(외부 경계 아님)
            justRun { registry.register(any(), any<SseEmitter>()) }  // ← stub 만, 호출 검증 없음
```

- 원인: ExecutionSseRegistry 는 허용 목록(KIS·Slack·client·RestTemplate·SseEmitter·StringRedisTemplate) 밖의 프로젝트 클래스이고 생성자 의존이 없어 실제 객체를 쓸 수 있다. 목은 stub 만 하고 register 호출 여부를 단언하지 않아, 컨트롤러가 emitter 를 레지스트리에 등록하는 행동은 어디에서도 시험되지 않는다(상태 코드·콘텐츠 타입만 확인).
- 생길 수 있는 일: 컨트롤러가 register 호출을 빠뜨려도 시험이 초록이라 SSE 구독이 조용히 끊긴다.
- 해결: 실제 ExecutionSseRegistry 를 빈으로 두고 요청 뒤 registry.broadcast 로 연결이 수신되는지 확인하거나, 최소한 register 호출을 단언한다.

고친 모양(예시):
```
@Bean fun executionSseRegistry() = ExecutionSseRegistry()
// 요청 후 registry.broadcast(sampleEvent) 가 예외 없이 emitter 에 전달되는지 확인
```

### 342위 [med · 확신 med] 같은 레포 application 서비스(KisAccountQueryService)를 @MockitoBean 으로 갈음하고 던질 예외도 시험이 지어낸다

`backend/trading-api/src/test/kotlin/com/papertrading/api/presentation/controller/KisAccountBalanceControllerIntegrationTest.kt:38` · 항목표 backend/tdd-purity · 항목 T1

```
    @MockitoBean lateinit var kisAccountQueryService: KisAccountQueryService  // ← 내부 application 서비스를 목으로 갈음
...
        given(kisAccountQueryService.getBalance(1L, KisAccountMode.PAPER)).willThrow(KisAuthorizationException("unauthorized"))  // ← 서비스가 던질 예외를 시험이 지어냄
```

- 원인: 외부 경계 허용 목록은 infrastructure/kis/**·notification·client/** 의 HTTP·WebSocket 과 RestTemplate 등이다. KisAccountQueryService 는 application.account 의 내부 서비스라 허용 목록 밖이다. 이 시험은 서비스를 통째로 갈음해 401·403·502·504 예외를 시험이 직접 던진다. 그래서 KIS 응답이 실제로 이 예외로 번역되는지는 보지 않고, 컨트롤러와 예외 핸들러의 매핑만 확인한다. 서비스 본문은 이 묶음 밖이라 열지 않았다. 렌즈 기본은 high 지만 시험 목적이 응답 매핑 확인이라 med 로 낮춘다.
- 생길 수 있는 일: 서비스가 KIS 오류를 다른 예외로 번역하도록 바뀌어도 이 시험은 초록이다. 서비스에서 컨트롤러까지 이어지는 오류 매핑이 한 번도 시험되지 않는다.
- 해결: @MockitoBean 대상을 외부 경계인 KIS 클라이언트(infrastructure/kis/**)로 바꿔 실제 KisAccountQueryService 와 예외 핸들러가 함께 돌게 한다. 서비스 목을 유지하려면 @WebMvcTest 슬라이스로 내려 컨테이너 2개 기동을 없앤다.

고친 모양(예시):
```
@MockitoBean lateinit var kisClient: KisBalanceClient  // 외부 경계만 갈음

given(kisClient.fetchBalance(any())).willThrow(KisHttpUnauthorizedException("unauthorized"))
```

### 343위 [med · 확신 med] 실제 DB 시험이 행을 정리하지 않고 종목 전체 매칭을 호출 — 순서 의존 위험

`backend/trading-api/src/test/kotlin/com/papertrading/api/presentation/controller/OrderControllerIntegrationTest.kt:57` · 항목표 backend/tdd-purity · 항목 T9

```
        redisTemplate.keys("quote:*").forEach { redisTemplate.delete(it) }  // ← Redis 시세 키만 정리, PostgreSQL 행은 시험 사이에 남음
        localMatchingEngine.tryMatchPendingOrders("005930", quote)  // ← 종목 전체 PENDING 주문 대상 — 잔존 주문과 섞임
```

- 원인: @SpringBootTest + 정적 PostgreSQL 컨테이너인데 @Transactional 롤백·@Sql·truncate 가 없고 정리는 Redis quote 키뿐이다. 각 시험이 새 계좌를 만들어 지금은 겹치지 않지만, tryMatchPendingOrders("005930", …)는 계좌를 가리지 않고 같은 종목의 PENDING 주문을 매칭하므로 앞 시험이 남긴 PENDING 주문(예: 멱등성 시험의 limitPrice 60000 주문)이 뒤 시험에 섞일 수 있다. 현재 호가·지정가 조합에서는 겹치지 않아 지금은 초록일 수 있다(잠재 위험).
- 생길 수 있는 일: 시험 추가·순서 변경·호가 조정 때 혼자는 통과하고 CI 에서만 깨지는 깜빡임이 생긴다.
- 해결: 시험마다 DB 정리(@Sql 또는 @AfterEach 로 주문·체결·포지션·계좌 삭제)를 하거나, 시험별 고유 종목 코드를 쓴다.

고친 모양(예시):
```
@AfterEach
fun cleanDatabase() {
    jdbcTemplate.execute("TRUNCATE orders, executions, positions, accounts RESTART IDENTITY CASCADE")
}
```

### 344위 [med · 확신 med] «avgBuyPrice=0 이면 returnRate null» 시험이 시세 없음만으로도 통과하는 가짜 초록

`backend/trading-api/src/test/kotlin/com/papertrading/api/presentation/controller/PositionControllerIntegrationTest.kt:217` · 항목표 backend/tdd-purity · 항목 T7

```
val pos = Position.createWithHolding(..., avgBuyPrice = BigDecimal.ZERO)  // ← 시세 없이 저장, currentPrice 도 null
...
jsonPath("$.returnRate") { doesNotExist() }  // ← 시세가 없으면 평균단가와 무관하게 returnRate 는 null
```

- 원인: 시험은 avgBuyPrice=0 포지션을 저장하고 returnRate 가 없음을 단언하지만 시세(quote)를 저장하지 않는다. 시세가 없으면 currentPrice 가 null 이고(PositionQueryServiceTest 134-142행: 캐시·시세 모두 없으면 currentPrice 가 null), 평균단가가 70000 이어도 returnRate 는 null 이므로 0 나누기 방지 가드는 이 시험에서 한 번도 실행되지 않는다.
- 생길 수 있는 일: Position.updatePrice 의 avgBuyPrice > 0 가드를 지워도 시험은 초록이라 0 나누기 회귀를 못 막는다.
- 해결: 해당 티커의 시세를 Redis 에 저장해 가격이 있는 상태에서 returnRate 가 null 임을 확인한다.

고친 모양(예시):
```
saveQuote("TEST", "75000")  // 시세 저장 후
jsonPath("$.currentPrice") { value(75000) }
jsonPath("$.returnRate") { doesNotExist() }
```

### 345위 [med · 확신 low] 한 체결 트랜잭션이 Order·Account·Position·Settlement 불변식을 함께 바꿈

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/ExecutionProcessor.kt:97` · 항목표 data/transaction-boundary · 항목 X7

```
val order = orderRepository.findByIdWithOptimisticLock(orderId).orElse(null) ?: return   // ← Order
val account = accountRepository.findByIdWithLock(accountId)   // ← Account
position.applyBuy(fillQty, fillPrice)   // ← Position (:142)
settlementRepository.save(Settlement.createFromExecution(...))   // ← Settlement (:179)
```

- 원인: processFill 은 주문 상태(applyExecution), 계좌 잔고(recordBuyExecution·receiveSellProceeds), 포지션 수량(applyBuy·applySellExecutionAtomically), 정산(Settlement)을 한 커밋에서 바꿉니다. 원장·감사 행 추가는 제외해도 애그리거트 루트 셋 이상입니다. 클래스 주석(:33-36)이 «체결 처리 단일 트랜잭션»을 의도로 밝히고 락 순서(Account→Position)로 데드락을 막는 설계라, 사람이 분리 여부를 고를 사안으로 ruleConflicts 에 남깁니다.
- 생길 수 있는 일: 세 루트가 한 락 범위에 묶여 포지션 락 대기가 주문·계좌 락 보유 시간을 늘리고, 한쪽 실패가 전체 체결을 롤백합니다. 반대로 분리하면 부분 반영 위험이 생깁니다(LLM-only 판정).
- 해결: 단일 트랜잭션을 유지한다면 결정을 문서화하고 락 순서·타임아웃을 못 박습니다. 분리한다면 Order 체결→도메인 이벤트(BEFORE_COMMIT)로 Account·Position 반영을 같은 트랜잭션 안 리스너에 둡니다.
- 다른 렌즈의 반증: backend/code-shape+layering+naming-terminology+tdd-purity — Optional 을 null 로 풀어 elvis 로 조기 반환할 뿐 예외를 던지고 잡지 않음

### 346위 [med · 확신 low] 같은 종목의 시세 처리가 직렬화되지 않음

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/LocalMatchingEngine.kt:25` · 항목표 reliability/load-concurrency · 항목 A2

```
fun tryMatchPendingOrders(ticker: String, quote: QuoteSnapshot) {
    val orders = orderRepository.findActiveLocalOrdersByTicker(ticker)   // ← 종목 직렬화·낡은 시세 폐기(updatedAt 비교) 없음
    for (order in orders) {
        runCatching { tryMatchOne(order, quote) }
// RedisListenerConfig.kt:17-18  val container = RedisMessageListenerContainer()  // ← setTaskExecutor 없음
```

- 원인: tryMatchPendingOrders(ticker, quote) 는 종목 락·종목별 큐 없이 호출 스레드에서 바로 돈다. 호출하는 QuoteEventListener 는 RedisListenerConfig.kt:17-22(내 묶음 밖, 열어 확인)에서 setTaskExecutor 없이 만든 컨테이너 위에 있어 기본 SimpleAsyncTaskExecutor 로 메시지마다 새 스레드에서 실행된다. 엔진에는 updatedAt 비교(낡은 시세 폐기)도 없어 순서가 뒤바뀐 낡은 시세로 MARKET 주문이 체결될 수 있다. 같은 주문의 이중 체결은 Order.@Version 과 findByIdWithOptimisticLock 이 막아 제외했다(disproved 항목 참조). 대신 경합한 쪽은 OptimisticLockException 으로 실패해 WARN 로그만 남긴다.
- 생길 수 있는 일: 시세 폭주 때 스레드가 늘어나고 DB 커넥션 풀(최대 10)이 마르며, 낡은 틱이 최신 틱 뒤에 처리돼 시장가가 낡은 가격으로 체결될 수 있다.
- 해결: 리스너는 받아서 종목 키별 단일 소비자 큐(상한 있는 실행기)로 넘기고, 엔진은 종목 단위로 직렬 실행한다. 종목별 마지막 updatedAt 보다 오래된 틱은 버린다.
- 다른 렌즈의 반증: backend/code-shape+layering+naming-terminology+tdd-purity — 주문별 실패를 격리해 나머지 주문 매칭을 계속하는 로깅 경로이고 같은 호출 사슬에서 던져 잡아 분기하지 않는다.

고친 모양(예시):
```
private val locks = ConcurrentHashMap<String, ReentrantLock>()
fun tryMatchPendingOrders(ticker: String, quote: QuoteSnapshot) {
    locks.computeIfAbsent(ticker) { ReentrantLock() }.withLock {
        if (!lastTick.advanceIfNewer(ticker, quote.updatedAt)) return
        // 매칭
    }
}
```

### 347위 [med · 확신 low] 시세 처리 지연·매칭 틱 소요가 전혀 계측되지 않음

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/LocalMatchingEngine.kt:25` · 항목표 reliability/observability · 항목 O6

```
fun tryMatchPendingOrders(ticker: String, quote: QuoteSnapshot) {   // ← 소요 시간·지연·건수 계측 없음
    val orders = orderRepository.findActiveLocalOrdersByTicker(ticker)
```

- 원인: tryMatchPendingOrders 에는 now - updatedAt(시세 처리 지연), 한 틱의 소요 시간, 처리한 주문 수에 대한 로그·타이머·게이지가 없다. 이 경로가 Pub/Sub 로 들어오므로 적체와 놓친 시세 구간(PTA2)을 알 방법이 로그에 없다.
- 생길 수 있는 일: 시세 적체나 매칭 지연이 장애가 되기 전까지 보이지 않는다.
- 해결: Micrometer Timer 와 시세 지연 게이지(Instant.now() - quote.updatedAt)를 기록한다.
- 다른 렌즈의 반증: backend/code-shape+layering+naming-terminology+tdd-purity — 주문별 실패를 격리해 나머지 주문 매칭을 계속하는 로깅 경로이고 같은 호출 사슬에서 던져 잡아 분기하지 않는다.

고친 모양(예시):
```
timer.record { match(ticker, quote) }
meterRegistry.gauge("quote.lag.ms", Duration.between(quote.updatedAt, Instant.now()).toMillis())
```

### 348위 [med · 확신 low] 총자산·손익·손익률 산식이 서비스에 있고 DailyBalance 는 계산 결과만 받아 저장

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/portfolio/DailyBalanceCommandService.kt:27` · 항목표 backend/layering · 항목 D3

```
        val stockMarketValue = positions.fold(BigDecimal.ZERO) { acc, position ->
            acc.add((position.currentPrice ?: BigDecimal.ZERO).multiply(position.quantity))   // ← 평가액 산식(PortfolioSnapshotCommandService 와 중복)
        val totalAssetValue = cashBalance.add(stockMarketValue)   // ← 엔티티가 아닌 서비스가 합계 규칙 보유
        val pnlRate = if (account.deposit.compareTo(BigDecimal.ZERO) == 0) {
```

- 원인: 서비스가 Position·Account 필드를 읽어 stockMarketValue, totalAssetValue(= cash + stock), pnlAmount(= total - deposit), pnlRate(= pnl / deposit, deposit 0 이면 0)를 계산하고 DailyBalance 에는 결과만 넘긴다. 규칙이 엔티티 밖에 있어 DailyBalance 는 어떤 값 조합도 받는다. LLM-only 판정.
- 생길 수 있는 일: totalAssetValue 가 cash + stock 과 어긋난 행도 저장될 수 있고, 같은 산식이 다른 서비스에 복제되면 한쪽만 틀린다.
- 해결: DailyBalance.of(account, date, stockMarketValue) 처럼 엔티티 팩토리가 cash·total·pnl·pnlRate 를 스스로 도출하게 한다. 평가액은 Position 메서드로 모은다.
- 다른 렌즈의 반증: data/query+schema-data-model+transaction-boundary — 한 계좌의 보유 종목 수로 행이 묶이는 소규모 표이고 조회가 findAll 이 아닌 계좌 한정 파생 쿼리다.

### 349위 [med · 확신 low] 계좌 행 락을 쥔 트랜잭션 안에서 종목마다 Redis 시세를 읽는다

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/portfolio/PortfolioSnapshotCommandService.kt:29` · 항목표 data/transaction-boundary · 항목 X3

```
    @Transactional
    fun recalculate(accountId: Long, businessDate: LocalDate): List<PortfolioSnapshot> {
        val account = accountRepository.findByIdWithLock(accountId)     // ← 계좌 행 락 획득
...
            val closePrice = marketQuotePort.getQuote(position.account.tradingMode, position.ticker)?.price   // ← 락·트랜잭션 안에서 종목마다 Redis 왕복
// RedisMarketQuoteAdapter.kt:31
        val hash = redisTemplate.opsForHash<String, String>().entries(quoteKey(tradingMode, ticker))   // ← 원격 Redis HGETALL
```

- 원인: recalculate 는 @Transactional 이고 L25 에서 findByIdWithLock 으로 계좌 행을 PESSIMISTIC_WRITE 잠근 뒤, 종목마다 marketQuotePort.getQuote 를 부른다. 구현 RedisMarketQuoteAdapter.kt:31 은 redisTemplate.opsForHash().entries(...) 로 원격 Redis 에 왕복한다. 렌즈의 X3 신호 목록에는 Redis 발행만 있고 읽기는 없지만, 원격 지연이 커넥션과 행 락을 붙잡는 원리는 같다. 이 판정은 LLM 판단이다.
- 생길 수 있는 일: Redis 가 느려지면(명령 타임아웃 전까지) 종목 수 × 지연만큼 DB 커넥션과 계좌 락이 잡혀 같은 계좌의 주문 처리와 풀 전체가 막힌다.
- 해결: 시세는 트랜잭션 밖에서 먼저 모은 뒤, 짧은 쓰기 트랜잭션에서는 계산값만 반영한다.
- 다른 렌즈의 반증: data/query+schema-data-model+transaction-boundary — 같은 계좌 엔티티가 L25 findByIdWithLock 으로 이미 영속성 컨텍스트에 있어 추가 쿼리가 없고, default_batch_fetch_size:100(application.yaml:26)도 걸려 있다.

고친 모양(예시):
```
// 오케스트레이터(트랜잭션 없음)
val quotes = tickers.associateWith { marketQuotePort.getQuote(mode, it)?.price }
// 이후 짧은 @Transactional 메서드에 quotes 를 인자로 넘겨 저장만 한다.
```

### 350위 [med · 확신 low] 평가액·미실현손익 산식이 서비스에 있고 Position·PortfolioSnapshot 에는 규칙 메서드가 없음

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/portfolio/PortfolioSnapshotCommandService.kt:33` · 항목표 backend/layering · 항목 D3

```
            PricedPosition(position, closePrice, closePrice.multiply(position.quantity))   // ← 평가액 산식이 서비스에 있음
            val unrealizedPnl = item.closePrice.subtract(item.position.avgBuyPrice).multiply(item.position.quantity)   // ← 미실현손익 산식도 서비스에 있음
```

- 원인: closePrice × quantity 와 (closePrice - avgBuyPrice) × quantity 를 서비스가 Position 필드를 읽어 계산한다. DailyBalanceCommandService 도 같은 평가액(currentPrice × quantity)을 따로 계산한다. Position 이 unrealizedPnl 속성을 이미 가진 것으로 보인다(PositionResult.kt:40 의 p.unrealizedPnl, Position.kt 는 열지 않음). 서비스 두 곳이 같은 산식을 서로 다른 대체값 규칙(시세 → currentPrice → 0 / currentPrice → 0)으로 구현한다. LLM-only 판정.
- 생길 수 있는 일: 산식이 서비스마다 갈라져 한쪽만 고치면 일별 잔고(DailyBalance)와 종목별 스냅샷(PortfolioSnapshot)의 평가액이 서로 어긋난다.
- 해결: Position.marketValueAt(price) 와 Position.unrealizedPnlAt(price) 메서드를 두고 두 서비스가 같은 메서드를 부르게 한다. 대체 가격 선택 규칙도 한 곳에 둔다.
- 다른 렌즈의 반증: data/query+schema-data-model+transaction-boundary — 같은 계좌 엔티티가 L25 findByIdWithLock 으로 이미 영속성 컨텍스트에 있어 추가 쿼리가 없고, default_batch_fetch_size:100(application.yaml:26)도 걸려 있다.

### 351위 [med · 확신 low] 계좌마다 따로 커밋하는 일괄이 첫 실패에서 전체를 중단하고 부분 상태로 남는다

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/portfolio/TaxSummaryBatchService.kt:31` · 항목표 data/transaction-boundary · 항목 X12

```
        targetAccountIds.forEach { accountId ->
            taxSummaryCommandService.recalculate(   // ← 계좌마다 독립 커밋, 예외를 잡지 않음
                accountId = accountId,
                taxYear = taxYear,
                force = true,
                runType = TaxSummaryRunType.YEAR_END_BATCH,
            )
        }

        return targetAccountIds.size   // ← 중간 실패면 여기 도달 못 하고 부분 상태만 남음
```

- 원인: runYearEnd 는 트랜잭션 없이 계좌마다 taxSummaryCommandService.recalculate(별도 빈, 계좌당 독립 커밋)를 부른다. 예외는 잡지 않으므로 k번째 계좌가 실패하면 1..k-1 은 커밋된 채 나머지는 처리되지 않고 호출자는 예외 응답만 받는다. 어느 계좌까지 됐는지 응답에 없다. 이 파일은 transaction-boundary 렌즈의 대상 파일 목록에 없었다(신호 정규식이 부재형 결함을 못 잡음). X12 판정을 위해 열어 기록한다.
- 생길 수 있는 일: 계좌 하나의 오류(통화 혼재 등)가 연말 일괄 전체를 멈추고, 처리된 계좌 수를 알 수 없어 운영자가 어디서부터 다시 돌릴지 모른다. force=true 재실행은 멱등이라 데이터 손상은 아니다.
- 해결: 계좌별로 결과(성공·실패와 사유)를 모아 끝까지 돌고, 실패 목록을 함께 돌려준다.
- 다른 렌즈의 반증: data/query+schema-data-model+transaction-boundary — 계좌마다 독립 트랜잭션·계좌 락이 필요한 단위 작업이라 일괄 읽기로 바꿀 수 없다. 루프가 부르는 것은 리포지토리가 아니라 유스케이스다. 대신 X12 로 보고했다.

고친 모양(예시):
```
val failures = targetAccountIds.mapNotNull { id ->
    runCatching { taxSummaryCommandService.recalculate(id, taxYear, true, TaxSummaryRunType.YEAR_END_BATCH) }
        .exceptionOrNull()?.let { id to it.message }
}
return BatchResult(succeeded = targetAccountIds.size - failures.size, failed = failures)
```

### 352위 [med · 확신 low] IllegalArgumentException 을 cause 없이 «지원하지 않는 통화»로 오분류

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/portfolio/TaxSummaryCommandService.kt:105` · 항목표 reliability/failure-handling · 항목 E2

```
        } catch (ex: IllegalArgumentException) {   // ← 모든 IAE 를 통화 오류로 단정, cause 버림
            val wrapped = UnsupportedCurrencyException(ex.message ?: "지원하지 않는 통화입니다.")
```

- 원인: try 본문(집계 조회·계산·저장) 어디서든 나는 IllegalArgumentException(require 실패 등)이 전부 UnsupportedCurrencyException(422)으로 바뀌고 원래 예외는 버려진다. 통화 검사는 이미 compute() 에서 UnsupportedCurrencyException 을 직접 던진다.
- 생길 수 있는 일: 통화와 무관한 인자 오류가 «지원하지 않는 통화» 로 응답·기록(run.fail 메시지)돼 원인을 잘못 짚게 된다.
- 해결: 이 catch 를 지우고 IAE 는 아래 Exception 분기에서 cause 를 붙여 TaxSummaryComputeFailedException 으로 감싼다.
- 다른 렌즈의 반증: backend/code-shape+layering+naming-terminology+tdd-purity — 한 식으로 검사할 조건을 예외로 대신한 것이 아니라 실패 이력을 남기고 다시 던지는 경로라 S10 대상 아님. 모든 IAE 를 «지원하지 않는 통화»로 부르는 오분류는 failure-handling 몫(notes).

### 353위 [med · 확신 low] 정렬 없는 그룹 순서로 한 트랜잭션에서 포지션 행 잠금을 누적 — 동시 onQuote 교착 가능

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/position/PositionExitTriggerOrchestrator.kt:39` · 항목표 reliability/load-concurrency · 항목 A2

```
val groups = triggerRepository.findByTickerAndState(normalizedTicker, TriggerState.ARMED)   // ← ORDER BY 없음
    .groupBy { TriggerGroupKey(it.positionId, it.triggerType) }
...
synchronized(positionLocks.computeIfAbsent(groupKey.positionId) { Any() }) {
    val position = positionRepository.findByIdWithLock(groupKey.positionId).orElse(null)   // ← 커밋(onQuote 끝)까지 유지되는 행 잠금이 그룹마다 누적
```

- 원인: `findByTickerAndState` 는 ORDER BY 가 없고 `groupBy` 는 조회 순서를 유지한다. 행 잠금은 @Transactional 인 onQuote 가 끝날 때까지 유지돼 그룹마다 쌓이는데, 가격 틱마다 recordEvaluation 으로 행이 갱신되면 PostgreSQL 의 행 물리 순서가 바뀌어 같은 종목의 두 틱이 서로 반대 순서로 잠글 수 있다. onQuote 호출 스레드 모델(QuoteEventListener)은 이 인스턴스 범위 밖이라 동시 실행 여부는 미확인.
- 생길 수 있는 일: 교착이 나면 한쪽 틱 전체가 실패해 그 틱의 청산 판정이 유실된다(Pub/Sub 이라 재시도 없음).
- 해결: 그룹을 positionId·triggerType 으로 정렬한 뒤 처리하고, 그룹마다 짧은 트랜잭션으로 잠금 보유 시간을 줄인다.

고친 모양(예시):
```
val groups = triggerRepository.findByTickerAndState(normalizedTicker, TriggerState.ARMED)
    .sortedWith(compareBy({ it.positionId }, { it.triggerType }))
    .groupBy { TriggerGroupKey(it.positionId, it.triggerType) }
```

### 354위 [med · 확신 low] 그룹마다 REQUIRES_NEW 로 주문을 먼저 커밋하고 트리거 상태는 바깥 트랜잭션에서 나중에 반영 — 바깥 롤백 시 주문만 남고 트리거는 ARMED

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/position/PositionExitTriggerOrchestrator.kt:107` · 항목표 data/transaction-boundary · 항목 X12

```
val created = tryCreateGroupedAutoExitOrderWithRetry(...)     // ← L107 REQUIRES_NEW 로 주문 커밋 완료
if (created) {
    firedTriggers.forEach { it.markTriggered(quoteAt, price) }    // ← L114 트리거 상태는 아직 바깥 트랜잭션 안 (커밋 전)
}
triggerRepository.saveAll(triggers)                              // ← L118 바깥이 롤백되면 주문만 남음
```

- 원인: onQuote 는 한 틱을 하나의 작업으로 보면서, 그룹마다 REQUIRES_NEW 로 주문을 커밋(L107-112)하고 트리거 상태(markTriggered·markFailed, saveAll)는 바깥 트랜잭션에 쌓는다(L113-118). 뒤 그룹에서 catch 되지 않는 예외(X6 의 rollback-only, markSkipped 의 ConflictException 등)가 나면 바깥이 롤백돼 앞 그룹의 주문은 커밋된 채 트리거는 ARMED 로 되돌아간다. 락은 쥐고 있어 동시 인스턴스는 막히지만, 롤백 뒤 부수효과 중복 방지 장치가 없다. orderGroupId 에 quoteAt 밀리초가 들어가 다음 틱은 다른 멱등키가 된다.
- 생길 수 있는 일: 같은 트리거가 다음 시세 틱에 다시 발동해 부분 청산(예 50%)이 중복 매도된다. 전부 청산은 POSITION_CLOSED 로 걸러지지만 부분 청산은 막지 못한다.
- 해결: X3 의 수정과 같다. 점유 상태를 먼저 커밋하고, 주문 생성 뒤 결과를 새 트랜잭션에서 반영하며, 주문 쪽 멱등키를 틱 시각이 아니라 트리거 id·버전으로만 만든다.
- 다른 렌즈의 반증: reliability/failure-handling+load-concurrency+observability+security-config-hygiene — orderGroupId 를 루프 밖(106행)에서 한 번만 만들어 모든 시도가 같은 번호를 쓴다.

고친 모양(예시):
```
val orderGroupId = "auto-exit:${positionId}:${triggerType}:${triggerIds}@${versions}" // quoteAt 제외 — 재발동해도 같은 키
```

### 355위 [med · 확신 low] 대기·백오프·지터 없는 즉시 3회 재시도를 잠금 보유 중에 실행

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/position/PositionExitTriggerOrchestrator.kt:139` · 항목표 reliability/failure-handling · 항목 E6

```
private val maxOrderCreateRetries = 3
...
repeat(maxOrderCreateRetries) { attempt ->   // ← 대기·백오프·지터 없이 즉시 반복, 잠금(synchronized + FOR UPDATE) 보유 중
    try {
        autoExitOrderPlacementService.createGroupedAutoExitSellOrder(
```

- 원인: `repeat(maxOrderCreateRetries)` 가 대기 없이 곧장 반복하고, 모든 Exception 을 재시도하며, 재시도 동안 JVM 모니터(synchronized)와 포지션·트리거 행 잠금을 계속 쥔다. orderGroupId 는 루프 밖(106행)에서 한 번 만들어 고정이라 이중 주문 위험은 낮다(중복 키 시 기존 주문 확인).
- 생길 수 있는 일: DB 장애·락 경합 때 같은 틱에서 즉시 재호출이 반복돼 부하를 키우고, 잠금을 쥔 채 재시도하는 동안 같은 종목 포지션을 쓰는 다른 요청이 대기한다.
- 해결: 재시도를 잠금 밖으로 빼거나, 일시 오류에만 지수 백오프+지터로 2회 이내 재시도한다.
- 다른 렌즈의 반증: data/query+schema-data-model+transaction-boundary — REQUIRES_NEW 메서드는 별도 빈 AutoExitOrderPlacementService 에 있고 프록시를 거쳐 호출된다

고친 모양(예시):
```
// 잠금 해제 뒤 바깥 경계에서
retryTemplate.execute { autoExitOrderPlacementService.createGroupedAutoExitSellOrder(...) }  // 일시 오류만, 백오프+지터
```

### 356위 [med · 확신 low] 중복 키 실패 알림이 예외 객체 없이 ex.message 문자열만 사용

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/position/PositionExitTriggerOrchestrator.kt:155` · 항목표 reliability/failure-handling · 항목 E2

```
} catch (ex: DataIntegrityViolationException) {
    val existing = orderRepository.findByAccountIdAndOrderGroupId(accountId, orderGroupId)
    ...
    publishOrderCreateFailure(
        ...
        reason = "duplicate key without existing order: ${ex.message}",   // ← 예외 객체·cause 를 버리고 message 만 알림에 실음
    )
    return false
```

- 원인: DataIntegrityViolationException 을 잡아 기존 주문이 없으면 `ex.message` 를 문자열로 이어 Slack 알림에만 싣고 예외 객체는 버린다. 스택·cause 가 사라지고, DataIntegrityViolationException 메시지는 SQL·제약 이름을 품을 수 있어 알림 채널로 샌다.
- 생길 수 있는 일: 중복 키 충돌의 근본 원인(어느 제약인지·어느 SQL 인지)을 되짚을 수 없고, 내부 SQL 조각이 Slack 으로 나간다.
- 해결: 예외를 log.error(ex) 로 남기고 Slack 에는 요약(제약 이름 제외)만 보낸다.
- 다른 렌즈의 반증: backend/code-shape+layering+naming-terminology+tdd-purity — catch 가 감싸는 호출은 AutoExitOrderPlacementService.createGroupedAutoExitSellOrder(@Transactional REQUIRES_NEW, L16)라 예외가 별도 트랜잭션에서 롤백되고 바깥 onQuote 트랜잭션은 rollback-only 가 되지 않는다. import 자체가 허용 목록 밖이라는 L2 발견(L13)은 유지한다.

고친 모양(예시):
```
} catch (ex: DataIntegrityViolationException) {
    log.error(ex) { "auto-exit duplicate key without order: accountId=$accountId, orderGroupId=$orderGroupId" }
    publishOrderCreateFailure(..., reason = "duplicate key without existing order")
    return false
}
```

### 357위 [med · 확신 low] KIS 계좌 번호(external_account_id)에 UNIQUE 가 없고 중복 확인도 없음

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/account/Account.kt:65` · 항목표 data/schema-data-model · 항목 M3

```
@Column(name = "external_account_id", length = 100)   // ← 자연 키인데 UNIQUE 없음(null 허용 부분 유일 인덱스 필요)
var externalAccountId: String? = null
    private set
```

- 원인: external_account_id 는 KIS 계좌 번호(XXXXXXXX-XX)로 자연 키 성격이다. 열에 유일 제약이 없고 createAccount·updateAccount 에도 중복 확인이 없다. 한 KIS 계좌를 여러 로컬 계좌가 공유해도 되는지는 코드에서 알 수 없어 확신은 낮다.
- 생길 수 있는 일: 두 로컬 계좌가 같은 KIS 계좌에 연결되면 각각 로컬 예수금 검사만 통과해 같은 실계좌로 중복 주문이 나가고, 잔고 대사(reconciliation)도 어긋난다.
- 해결: 도메인이 1:1 이면 external_account_id IS NOT NULL 조건의 부분 유일 인덱스를 추가하고 생성·수정 때 중복을 검사한다. 공유가 허용이면 근거를 주석으로 남긴다.

### 358위 [med · 확신 low] 수수료(FEE) 원장 행에 계산에 쓴 요율·정책 버전이 저장되지 않음

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/account/AccountLedger.kt:50` · 항목표 data/schema-data-model · 항목 M11

```
@Column(name = "amount", nullable = false, precision = 20, scale = 4)   // ← 금액만 저장
lateinit var amount: BigDecimal
...
// Account.kt:196-206 recordFee(fee, ...) → AccountLedger.create(transactionType = FEE, amount = fee, ...)   // ← 요율·정책 id 없이 금액만
```

- 원인: Account.recordFee 는 계산된 수수료 금액만 amount 로 원장에 남긴다. 요율을 가진 FeePolicy 이외의 엔티티에는 fee_rate·policy_version 류 열이 없다(domain/entity grep). ExecutionProcessor 는 체결 시점의 현재 정책에서 요율을 읽는다. «재계산이 현재 정책을 다시 읽는다»는 경로는 이 청크에서 확인하지 못해 확신은 낮다.
- 생길 수 있는 일: 요율이 바뀐 뒤 과거 수수료를 검증·재계산하면 이력이 조용히 달라져 대사가 거짓 불일치를 낸다.
- 해결: 수수료·세금 행에 fee_rate(또는 fee_policy_id)를 함께 저장한다. 원장 행이 아니라 체결(execution) 행에 둔다면 그 근거를 남긴다.
- 다른 렌즈의 반증: data/query+schema-data-model+transaction-boundary — 모든 필드가 private set 이고 수정 메서드·@Modifying UPDATE·delete 가 코드 어디에도 없다(append-only). 정정은 BUY_UNLOCK 같은 반대 행으로 쓴다. 다만 열에 updatable = false 는 없어 아주 약한 방어만 있다.

### 359위 [med · 확신 low] 리스크 정책이 Account 루트 객체를 직접 참조(자기 리포지토리 있음)

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/account/RiskPolicy.kt:32` · 항목표 backend/layering · 항목 D4

```
@ManyToOne(fetch = FetchType.LAZY, optional = false) // ← 별도 리포지토리로 다뤄지는 루트가 Account 객체를 직접 참조
@JoinColumn(name = "account_id", nullable = false)
lateinit var account: Account
```

- 원인: KDoc 은 «Account Aggregate 내부 구성 요소»라고 하지만 RiskPolicyRepository·RiskPolicyService(application/account) 로 따로 다뤄진다. 렌즈 기준으로 자기 리포지토리로 다뤄지는 자식은 루트다. KDoc 의 의도와 코드 취급이 어긋난다.
- 생길 수 있는 일: 의도(Account 내부)와 취급(독립 저장·조회)이 달라 어느 쪽 규칙을 따를지 모호하다.
- 해결: 의도가 Account 내부 자식이면 리포지토리를 Account 하나로 모으고, 독립 루트면 accountId: Long 으로 참조한다.

### 360위 [med · 확신 low] 수수료 정책의 자연 키(모드·시장·시작 시각) 유일 제약과 기간 겹침 방지가 없음

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/feepolicy/FeePolicy.kt:23` · 항목표 data/schema-data-model · 항목 M3

```
@Entity
@Table(name = "fee_policies")   // ← (trading_mode, market_type, effective_from) 유일 제약·기간 겹침 방지(EXCLUDE) 없음
// FeePolicyRepository.kt:21  fun findActivePolicy(...): Optional<FeePolicy>   // ← 2행이면 단건 조회가 예외
```

- 원인: @Table 에 uniqueConstraints·indexes 가 없습니다. 같은 (trading_mode, market_type)에 활성 기간이 겹치는 행을 넣을 수 있고, findActivePolicy 는 단건 Optional 로 받습니다(FeePolicyRepository.kt:21). 코드에서 만료 처리 규율에만 의존합니다(LLM-only 판정).
- 생길 수 있는 일: 정책 행이 겹치면 findActivePolicy 가 IncorrectResultSizeDataAccessException 을 던져 해당 모드·시장의 모든 체결 트랜잭션이 실패합니다. 발생 가능성은 낮지만 영향이 큽니다.
- 해결: UNIQUE (trading_mode, market_type, effective_from) 를 추가하고, 겹침 방지는 EXCLUDE 제약(tstzrange) 또는 만료-삽입을 한 트랜잭션에서 하는 서비스로 보장합니다.
- 다른 렌즈의 반증: data/query+schema-data-model+transaction-boundary — 시장·모드 조합 수만큼의 설정성 소량 테이블이라 인덱스 없이도 비용이 작습니다. 유일성 문제는 M3 로 따로 보고했습니다.

고친 모양(예시):
```
@Table(
    name = "fee_policies",
    uniqueConstraints = [UniqueConstraint(name = "uk_fee_policies_mode_market_from", columnNames = ["trading_mode", "market_type", "effective_from"])]
)
```

### 361위 [med · 확신 low] 알림의 원본 참조(source_ref)에 UNIQUE 제약이 없음

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/notification/Notification.kt:45` · 항목표 data/schema-data-model · 항목 M3

```
@Entity
@Table(name = "notifications")   // ← uniqueConstraints·indexes 없음
class Notification protected constructor() : BaseTimeEntity() {
    @Column(name = "source_ref", nullable = false, length = 255)   // ← 멱등 키 성격인데 UNIQUE 없음
    lateinit var sourceRef: String
```

- 원인: source_ref 는 eventType:원본id 로 만든 사실상의 멱등 키지만 @Table 에 uniqueConstraints 가 없고, 저장 전에 같은 source_ref 를 조회하지도 않는다(NotificationPersistenceTxService.save). 핸들러는 id 가 없으면 0 으로 대체해 서로 다른 이벤트가 같은 값을 가질 수 있어, 제약을 추가하려면 이 대체 규칙부터 정해야 한다.
- 생길 수 있는 일: 같은 이벤트가 두 번 발행되면 알림 행과 Slack 메시지가 중복된다. 금액 영향은 없다.
- 해결: (event_type, source_ref) UNIQUE 를 추가하고 저장 시 충돌을 중복으로 처리한다. id 없는 이벤트의 대체값 규칙도 함께 정한다.
- 다른 렌즈의 반증: backend/code-shape+layering+naming-terminology+tdd-purity — ref(reference)는 렌즈 예시(qty·amt·cnt·acct·ord·exec)처럼 모호하게 펼쳐지는 약어가 아니라 관용 접미라서 통과. SlackNotificationConfigStore 의 private policyRef 도 같은 이유.

고친 모양(예시):
```
@Table(name = "notifications", uniqueConstraints = [UniqueConstraint(name = "uk_notifications_source_ref", columnNames = ["source_ref"])])
```

### 362위 [med · 확신 low] create·refresh 에 불변식 검증이 없어 totalAssetValue ≠ cash + stock 인 행도 만들 수 있음

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/portfolio/DailyBalance.kt:79` · 항목표 backend/layering · 항목 D5

```
        ): DailyBalance = DailyBalance().apply {   // ← init/require 도 팩토리 검증도 없음
            this.cashBalance = cashBalance
            this.totalAssetValue = totalAssetValue   // ← cash + stock 과의 정합을 아무도 검사하지 않음
```

- 원인: companion create 와 refresh 가 값을 검증 없이 대입한다. 총자산은 현금+평가액, 손익은 총자산-예치금이라는 정합 규칙이 서비스에만 있고(그것도 코드로 강제되지 않음) 엔티티는 어떤 값 조합도 받는다. LLM-only 판정.
- 생길 수 있는 일: 서비스를 거치지 않는 호출자·테스트 픽스처가 서로 안 맞는 잔고 행을 만들고 그대로 저장된다.
- 해결: create 가 cash·stock 만 받아 total 을 스스로 계산하거나, 최소한 require(totalAssetValue.compareTo(cash + stock) == 0) 를 둔다.

### 363위 [med · 확신 low] create·refresh 에 수량·가격·비중 범위 검증이 없음

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/portfolio/PortfolioSnapshot.kt:94` · 항목표 backend/layering · 항목 D5

```
        ): PortfolioSnapshot = PortfolioSnapshot().apply {   // ← 검증 없음
            this.quantity = quantity
            this.weight = weight   // ← 0..1 범위 검증 없음
```

- 원인: quantity ≥ 0, closePrice ≥ 0, 0 ≤ weight ≤ 1, ticker 비어 있지 않음 같은 규칙이 엔티티에 없고 값 정합(marketValue = closePrice × quantity)도 검사하지 않는다. LLM-only 판정.
- 생길 수 있는 일: 음수 수량이나 1 을 넘는 비중이 그대로 저장돼 이후 포트폴리오 화면·분석이 조용히 틀린다.
- 해결: create·refresh 에 require(quantity >= ZERO), require(weight in 0..1) 를 두거나 값 객체로 감싼다.

### 364위 [med · 확신 low] 세금 요약 상태에 전이 정의가 없고 재계산이 FINAL 을 DRAFT 로 되돌려 덮어쓴다

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/portfolio/TaxSummary.kt:68` · 항목표 data/schema-data-model · 항목 M8

```
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: TaxSummaryStatus = TaxSummaryStatus.DRAFT   // ← FINAL 로 가는 전이가 엔티티에 없음 (enum: DRAFT, FINAL)
...
    fun recalculate(totalRealizedPnl: BigDecimal, taxablePnl: BigDecimal, estimatedTax: BigDecimal) {
        require(estimatedTax >= BigDecimal.ZERO) { "예상 세금은 0 이상이어야 합니다." }
        this.totalRealizedPnl = totalRealizedPnl.setScale(4, RoundingMode.HALF_UP)   // ← FINAL 이어도 금액을 덮어씀
        this.status = TaxSummaryStatus.DRAFT   // ← 현재 상태와 무관하게 DRAFT 로 되돌림
```

- 원인: TaxSummaryStatus 는 DRAFT·FINAL 두 값인데 엔티티의 어느 메서드도 FINAL 로 보내지 않는다. 반면 recalculate 는 현재 상태를 보지 않고 금액을 덮어쓰며 status 를 DRAFT 로 되돌린다. 허용 전이 표나 DB CHECK 도 없다. 확정(FINAL) 처리가 다른 경로에서 생기면 확정된 신고 값이 오류 없이 되돌려진다.
- 생길 수 있는 일: 확정된 연간 세금 요약이 연말 일괄이나 수동 재계산(force)으로 조용히 DRAFT 가 되어 신고 자료가 바뀐다.
- 해결: 전이 메서드(finalize)를 두고 recalculate 는 DRAFT 일 때만 허용한다. FINAL 재계산이 필요하면 명시적 재개(reopen) 전이를 둔다.
- 다른 렌즈의 반증: backend/code-shape+layering+naming-terminology+tdd-purity — Kotlin 의 >= 는 compareTo 로 컴파일되므로 scale 과 무관하게 값 비교가 된다. PTB4 통과.

고친 모양(예시):
```
fun recalculate(...) {
    check(status == TaxSummaryStatus.DRAFT) { "확정된 세금 요약은 재계산할 수 없습니다." }
    ...
}
fun finalize() { check(status == TaxSummaryStatus.DRAFT); status = TaxSummaryStatus.FINAL }
```

### 365위 [med · 확신 low] Position 규칙(폐쇄 여부·평단 기준 임계가)이 Position 이 아니라 서비스 여러 곳에 있다

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/position/Position.kt:41` · 항목표 backend/layering · 항목 D3

```
// CommandService L83
if (position.quantity <= BigDecimal.ZERO) {  // ←
// Orchestrator L59
if (position == null || position.quantity <= BigDecimal.ZERO) {
// CommandService L145
return avgBuyPrice.multiply(multiplier).setScale(4, RoundingMode.HALF_UP)
// Evaluator L65
entryPrice.multiply(BigDecimal.ONE.subtract(percentRatio(percent)))
```

- 원인: quantity <= 0 이면 닫힌 포지션이라는 판정이 PositionExitTriggerCommandService L83 과 PositionExitTriggerOrchestrator L59 두 곳에 따로 있고 Position 에는 isClosed 같은 메서드가 없다. avgBuyPrice 에 (1 ± percent/100) 을 곱하는 임계가 산술도 CommandService L136~L145 와 Evaluator L64~L71 에 두 벌 있다. 서비스가 두 곳 이상 필드를 읽어 분기하는데 규칙 메서드가 클래스에 없는 DD10·DD11 모양이다. LLM-only 판정.
- 생길 수 있는 일: 규칙을 한 곳에서 바꾸면 다른 곳이 낡아 진다. 예를 들어 임계가 반올림을 CommandService 만 setScale(4) 로 하고 Evaluator 는 안 해서(L145 대 L64~L68) 등록 시점 임계가와 평가 시점 임계가가 이미 어긋나 있다.
- 해결: Position 에 isClosed() 와 thresholdPrice(triggerType, percent) 를 두고, 서비스는 그 메서드만 부른다.
- 다른 렌즈의 반증: backend/code-shape+layering+naming-terminology+tdd-purity — BaseAuditEntity.kt L9 abstract class BaseAuditEntity : BaseTimeEntity() 이고 BaseTimeEntity 는 루트라 깊이는 2(Position 제외 조상 2개)이다. 감사 열만 담은 JPA MappedSuperclass 라 동작 재사용 상속이 아니다. 세 클래스를 모두 센다고 읽으면 3이라 사람이 판단할 여지는 있다.

고친 모양(예시):
```
class Position {
    fun isClosed(): Boolean = quantity <= BigDecimal.ZERO
    fun exitThreshold(type: TriggerType, percent: BigDecimal): BigDecimal = ...
}
```

### 366위 [med · 확신 low] SettlementExecution 이 Execution 루트를 객체로 참조

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/settlement/SettlementExecution.kt:35` · 항목표 backend/layering · 항목 D4

```
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "execution_id", nullable = false)
    lateinit var execution: Execution              // ← 다른 루트 객체 참조
```

- 원인: settlement(부모)는 허용이지만 execution 은 별도 루트(ExecutionRepository)라 객체 참조가 애그리거트를 넘는다.
- 생길 수 있는 일: 조인 행을 읽으면 체결(+주문+계좌)이 딸려 온다.
- 해결: executionId: Long 으로 참조한다.
- 다른 렌즈의 반증: data/query+schema-data-model+transaction-boundary — settlement_id 로만 조회(SettlementExecutionRepository.findBySettlementId)하고 체결 삭제·execution_id 역조회 경로가 없습니다. 영향이 거의 없어 발견에서 제외합니다.

### 367위 [med · 확신 low] KIS 호출 실패가 다시 던져진 뒤 어디서도 로그되지 않음

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/kis/KisAccountBalanceAdapter.kt:65` · 항목표 reliability/observability · 항목 O1

```
} catch (ex: ResourceAccessException) {
    throw KisTimeoutException("KIS request timed out", ex)  // ← 로그 없이 다시 던짐
} catch (ex: RestClientException) {
    throw KisRemoteCallException("KIS request failed", ex)
}
// GlobalExceptionHandler.kt:59-63 (범위 밖, 전문 열람)
@ExceptionHandler(Exception::class)
fun handleException(ex: Exception) = ResponseEntity.status(500).body(...)  // ← 로거·로그 호출 없음
```

- 원인: 어댑터는 ResourceAccessException·RestClientException·rt_cd 오류를 KisTimeoutException·KisRemoteCallException 등으로 바꿔 던지고 자체 로그가 없다. 상위 GlobalExceptionHandler(범위 밖, 전문 열람)에도 로거가 없어 ApiDomainException·일반 Exception(500) 모두 로그 없이 응답으로 바뀐다. 따라서 '상위가 로그한다'는 통과 근거가 성립하지 않는다. KisTimeoutException 등의 HTTP 상태는 열어 확인하지 못했다(5xx 가 아니면 면제).
- 생길 수 있는 일: KIS 가 느리거나 실패해도 서버 로그에 검색할 줄이 없어 장애 원인이 KIS 인지 우리인지 가를 수 없다.
- 해결: 전역 핸들러에서 5xx 와 KIS 오류는 예외를 포함해 로그하거나, 어댑터가 호출 실패를 WARN 으로 남기고 다시 던진다.
- 다른 렌즈의 반증: reliability/failure-handling+load-concurrency+observability+security-config-hygiene — ResourceAccessException·RestClientException 으로 좁게 잡고 원래 예외를 cause 로 넘긴다

고친 모양(예시):
```
@ExceptionHandler(Exception::class)
fun handleException(ex: Exception): ResponseEntity<ApiErrorResponse> {
    log.error(ex) { "unhandled exception" }
    return ResponseEntity.status(500).body(ApiErrorResponse(500, "INTERNAL_SERVER_ERROR", "서버 오류가 발생했습니다."))
}
```

### 368위 [med · 확신 low] 운영자가 PUT 으로 바꾸는 알림 정책을 프로세스 메모리에만 보관

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/notification/SlackNotificationConfigStore.kt:14` · 항목표 reliability/load-concurrency · 항목 A5

```
private val policyRef = AtomicReference(   // ← 운영자가 PUT 으로 바꾸는 정책을 프로세스 메모리에만 보관
    SlackNotificationRuntimeConfig(
        enabled = properties.enabled,
...
override fun update(newConfig: SlackNotificationRuntimeConfig): SlackNotificationRuntimeConfig {
    policyRef.set(newConfig)   // ← 다른 인스턴스·재시작 뒤에는 반영되지 않음
```

- 원인: enabled·maxRetries·retryBackoffMillis·enabledTypes 는 PUT 으로 바뀌는데 AtomicReference 에만 저장된다. 다른 인스턴스에는 반영되지 않고 재시작하면 환경 변수 값으로 되돌아간다. 코드가 다중 인스턴스를 전제한다(NotificationPersistenceTxService.kt:20 "다른 인스턴스가 이미 선점했으면"). 스케줄러는 policy.enabled 가 false 면 그 인스턴스에서 재시도를 건너뛴다.
- 생길 수 있는 일: 부하 분산기 뒤에서 PUT 한 번이 한 인스턴스에만 적용되어 인스턴스마다 알림 정책이 달라지고, 재배포하면 변경이 조용히 사라진다.
- 해결: 정책을 DB 행(또는 Redis)에 저장하고 읽을 때 짧은 캐시를 둔다. 단일 인스턴스 고정이 문서로 보장되면 low 로 낮춘다.
- 다른 렌즈의 반증: reliability/failure-handling+load-concurrency+observability+security-config-hygiene — 불변 data class 를 AtomicReference 로 통째 교체하므로 경쟁 조건 없음(Service 의 get→copy→update 는 서버 소유 필드를 안 바꿔 무해). A5 의 프로세스 메모리 문제는 별도로 보고

고친 모양(예시):
```
// notification_slack_policy 테이블 1행 (@Version) 을 읽고 쓰는 SlackNotificationConfig 구현으로 교체
```

### 369위 [med · 확신 low] 알림 전송 흐름 전체가 infrastructure 리스너에 있다

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/notification/SlackNotificationEventHandler.kt:21` · 항목표 backend/layering · 항목 L6

```
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
@Async("slackNotificationExecutor")
fun handle(event: SlackNotificationRequestedEvent) {   // ← 정책→기록→전송→결과 기록 흐름 전체가 어댑터 안에 있음
    val policy = policyStore.get()
    // …
    val notification = txService.save(   // ← REQUIRES_NEW 기록이 application 유스케이스가 아닌 infra TxService
```

- 원인: AFTER_COMMIT 리스너가 정책 게이트(enabled·enabledTypes), Notification 생성, REQUIRES_NEW 저장, 전송, 결과 기록을 모두 직접 한다. 부르는 application 유스케이스가 없다. PTB7 은 결과 기록(REQUIRES_NEW)을 유스케이스로 보고 그것을 부르는 리스너는 application 에 두라고 한다. 반면 어댑터만 부르면 infrastructure 라는 문구도 있어 해석이 갈리므로 신뢰도를 낮춘다.
- 생길 수 있는 일: 알림 업무 규칙이 infrastructure 에만 있어 application 단위 시험으로 닿지 않고, 스케줄러와 같은 규칙이 두 곳에 복제될 위험이 있다.
- 해결: application 에 알림 전송 유스케이스(SlackNotificationDeliveryService)를 만들고 리스너는 이벤트를 받아 그 유스케이스를 부르기만 한다. 리스너의 위치(application)는 PTB7 에 맞춘다.
- 다른 렌즈의 반증: reliability/failure-handling+load-concurrency+observability+security-config-hygiene — slackNotificationExecutor 는 큐 100·최대 4스레드·거부 정책이 있음(AsyncConfig.kt:14-18). CallerRuns 문제는 A3 로 보고

고친 모양(예시):
```
@Service
class SlackNotificationDeliveryService(/* 포트들 */) {
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun deliver(event: SlackNotificationRequestedEvent) { /* 정책 게이트→기록→전송 */ }
}
```

### 370위 [med · 확신 low] 알림 실행기의 CallerRunsPolicy 로 큐가 차면 주문·체결 스레드가 Slack 호출에 묶임

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/notification/SlackNotificationEventHandler.kt:22` · 항목표 reliability/load-concurrency · 항목 A3

```
// SlackNotificationEventHandler.kt:21-22
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
@Async("slackNotificationExecutor")
// AsyncConfig.kt:16-18
executor.setQueueCapacity(100)
executor.setRejectedExecutionHandler(ThreadPoolExecutor.CallerRunsPolicy())   // ← 큐 가득 시 발행 스레드(주문·체결 트랜잭션을 방금 커밋한 스레드)가 Slack 호출을 직접 실행
```

- 원인: 실행기는 코어 2·최대 4·큐 100으로 상한이 있지만(A1 통과), 거부 정책이 CallerRunsPolicy 라 큐가 가득 차면 이벤트를 발행한 스레드가 handle 을 직접 실행한다. AFTER_COMMIT 이벤트라 그 스레드는 주문 접수·체결 반영 트랜잭션을 방금 끝낸 요청·매칭 스레드이며, handle 은 DB 저장과 최대 5초+10초 타임아웃의 Slack 호출을 한다.
- 생길 수 있는 일: Slack 장애·지연 중 워커 2개가 타임아웃에 묶여 큐 100이 차면, 이후 주문 처리마다 해당 스레드가 최대 약 15초 막힌다. 알림 장애가 주문 지연으로 번진다.
- 해결: 알림은 유실을 허용하므로 거부 시 호출 스레드를 막지 않고 버리거나(로그 남김), 이벤트 수신 시 PENDING 행 저장만 하고 발송은 스케줄러가 이어받게 한다.
- 다른 렌즈의 반증: reliability/failure-handling+load-concurrency+observability+security-config-hygiene — slackNotificationExecutor 는 큐 100·최대 4스레드·거부 정책이 있음(AsyncConfig.kt:14-18). CallerRuns 문제는 A3 로 보고

고친 모양(예시):
```
executor.setRejectedExecutionHandler { _, _ ->
    log.warn { "slack-notif 큐 가득: 알림 이벤트 폐기" }   // 호출 스레드를 막지 않음
}
```

### 371위 [med · 확신 low] 유스케이스가 쓰는 조회 포트가 infrastructure 에 있고 시그니처가 port 밖 타입을 씀

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/AccountLedgerRepositoryCustom.kt:7` · 항목표 backend/layering · 항목 L3

```
interface AccountLedgerRepositoryCustom {
    fun findLedgers(accountId: Long, filter: LedgerFilter): Page<LedgerResult>      // ← infrastructure 에 있는 조회 포트, 시그니처가 port 밖 application 타입·Spring Page 를 씀
```

- 원인: AccountQueryService 가 AccountLedgerRepository 를 통해 호출하는 findLedgers 가 infrastructure.persistence 의 인터페이스에 있다. 시그니처는 application.account.query·result 타입과 Spring Data Page 를 쓴다. PTB8 은 출력 포트를 application/<ctx>/port 에 두고 요청·결과 타입도 그 패키지에 두게 한다. L4 의 import 간선은 이 자리 문제의 증상이다.
- 생길 수 있는 일: 포트 자리가 둘로 갈라져 유스케이스가 영속 인터페이스를 직접 알게 되고(L1), 시험은 Page 를 직접 만들어 목으로 흉내 낸다.
- 해결: findLedgers 를 application/account/port 의 포트로 올리고 AccountLedgerRepositoryImpl 은 그 포트를 구현하는 어댑터로 둔다.
- 다른 렌즈의 반증: backend/code-shape+layering+naming-terminology+tdd-purity — Spring Data 사용자 정의 조각(fragment) 관례 이름이고 Impl 접미도 프레임워크가 요구한다.

### 372위 [med · 확신 low] 재시도 대상 PENDING 알림을 상한 없이 전부 읽어 30초마다 직렬 처리

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/NotificationRepository.kt:10` · 항목표 data/query · 항목 Q5

```
// NotificationRepository.kt:10-13
fun findByStatusAndAttemptCountLessThan(
    status: DeliveryStatus,
    maxAttempts: Int,
): List<Notification>   // ← 상한 없는 목록
// SlackNotificationScheduler.kt:21-23
val pendingNotifications = txService.findPendingForRetry(policy.maxRetries)
for (notification in pendingNotifications) {   // ← 전체를 건마다 직렬 처리
```

- 원인: findByStatusAndAttemptCountLessThan 은 List 를 상한 없이 돌려주고 스케줄러가 30초마다 전체를 받아 건마다 HTTP 전송과 REQUIRES_NEW 커밋을 직렬로 한다. Slack 장애 중에는 이벤트마다 PENDING 이 쌓인다. findByAccountIdAndIsReadFalseOrderByCreatedAtDesc 도 상한이 없으나 main·test 에 호출처가 없다.
- 생길 수 있는 일: 장애가 길어지면 한 번의 스케줄 틱이 길어지고 메모리·DB 왕복이 백로그에 비례해 커진다. 아래 M4 발견(status 인덱스 없음)과 겹쳐 전체 스캔이 된다.
- 해결: 한 틱에 가져올 개수를 제한(Pageable·findTop50…OrderByCreatedAtAsc)하고 오래된 것부터 처리한다.
- 다른 렌즈의 반증: backend/code-shape+layering+naming-terminology+tdd-purity — Spring Data 파생 질의 이름이며 isRead 접두도 있음 — 통과

고친 모양(예시):
```
fun findTop50ByStatusAndAttemptCountLessThanOrderByCreatedAtAsc(status: DeliveryStatus, maxAttempts: Int): List<Notification>
```

### 373위 [med · 확신 low] PENDING→DELIVERING 전이의 이전 상태 검사가 어댑터 쿼리에만 있고 애그리거트 메서드에는 없음

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/NotificationRepositoryImpl.kt:16` · 항목표 backend/layering · 항목 D2

```
            .set(notification.status, DeliveryStatus.DELIVERING)   // ← 상태 전이를 어댑터가 벌크 UPDATE 로 직접 기록
            .where(
                notification.id.eq(id),
                notification.status.eq(DeliveryStatus.PENDING),   // ← 이전 상태 검사가 어댑터 쿼리에만 있음
            )
// Notification.kt L90-93
    fun markDelivering(): Notification {
        status = DeliveryStatus.DELIVERING   // ← 이전 상태(PENDING) 검사 없이 대입
        return this
    }
```

- 원인: claimForDelivery 가 벌크 UPDATE 로 status 를 직접 DELIVERING 으로 쓰고, 허용 이전 상태(PENDING) 검사도 이 쿼리의 where 절에만 있습니다. 같은 전이를 맡는 Notification.markDelivering()(Notification.kt L90-93)은 이전 상태를 검사하지 않고 곧바로 대입합니다. 그래서 전이 규칙이 어댑터 한 곳에만 있고, 애그리거트 메서드는 어느 상태에서든 DELIVERING 으로 바꿉니다. 다중 인스턴스에서 원자적으로 선점하는 UPDATE…WHERE status=PENDING 자체는 정당한 방식이라 심각도를 high 에서 med 로 낮췄습니다. 문제는 규칙이 애그리거트에 없다는 점입니다.
- 생길 수 있는 일: NotificationPersistenceTxService.markDelivering 이 선점 성공 뒤에 부르는 notification.markDelivering() 은 DELIVERED·FAILED 상태에서도 성공하므로, 다른 호출 경로(예: 새 유스케이스)가 쿼리를 거치지 않고 부르면 완료·실패 건이 다시 DELIVERING 으로 돌아가 Slack 중복 발송이나 상태 불일치가 생깁니다.
- 해결: 애그리거트의 markDelivering() 이 PENDING 일 때만 전이하도록 검사를 넣고, 어댑터는 원자적 선점 UPDATE 를 유지하되 허용 이전 상태를 도메인 규칙과 같은 출처(예: DeliveryStatus 의 전이 허용 집합)에서 가져옵니다. Notification.kt 는 다른 묶음 파일이므로 markDelivered·markFailed 의 같은 문제는 그쪽 인스턴스가 판정합니다.
- 다른 렌즈의 반증: data/query+schema-data-model+transaction-boundary — @Modifying @Query 가 아니고, 호출하는 markDelivering 이 REQUIRES_NEW 새 트랜잭션에서 update 를 먼저 실행해 영속성 컨텍스트에 로딩된 엔티티가 없다. 이후 갱신은 별도 트랜잭션의 merge 라 낡은 읽기가 없다.

고친 모양(예시):
```
fun markDelivering(): Notification {
    check(status == DeliveryStatus.PENDING) { "PENDING 에서만 DELIVERING 으로 전이할 수 있습니다: $status" }
    status = DeliveryStatus.DELIVERING
    return this
}
```

### 374위 [med · 확신 low] 매도 체결 시 Position 상태를 JPQL UPDATE 가 직접 대입해 애그리거트 메서드(applySell)를 우회

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/PositionRepository.kt:32` · 항목표 backend/layering · 항목 D2

```
@Modifying(clearAutomatically = true, flushAutomatically = true)
@Query("UPDATE Position p SET p.quantity = p.quantity - :executedQty, ...
   p.totalBuyAmount = p.avgBuyPrice * (p.quantity - :executedQty)  // ← 애그리거트 상태를 쿼리가 직접 대입
   WHERE ... AND p.quantity >= :executedQty AND p.lockedQuantity >= :executedQty")  // ← applySell 의 불변식을 JPQL 로 복제
fun applySellExecutionAtomically(...): Int
```

- 원인: applySellExecutionAtomically 가 quantity·lockedQuantity·totalBuyAmount 를 JPQL 로 직접 갱신하고, Position.applySell 의 require 3개(수량>0, 보유>=체결, 잠금>=체결)를 WHERE 절로 복제한다. 운영 경로(ExecutionProcessor.kt:153)는 이 쿼리만 쓰고 Position.applySell 은 main 에서 호출이 없다(application·infrastructure·presentation·domain grep 0건, 호출은 PositionTest 뿐). 원자성을 위한 의도된 설계라 심각도를 한 단계 낮춘다.
- 생길 수 있는 일: 매도 규칙(실현손익·수수료 반영 등)을 바꿀 때 applySell 만 고치면 운영 경로에는 반영되지 않고, 도메인 단위 시험은 초록인 채 운영과 갈라진다. 이 JPQL 은 실제 DB 시험도 없다(T5).
- 해결: 규칙의 단일 원천을 정한다. 낙관적 락(@Version)·PESSIMISTIC_WRITE 로 applySell 을 쓰거나, 쿼리를 쓰되 Position.applySell 을 삭제하고 불변식을 어댑터 한 곳에 두고 Testcontainers 시험으로 고정한다.
- 다른 렌즈의 반증: data/query+schema-data-model+transaction-boundary — applySellExecutionAtomically 에 flushAutomatically = true, clearAutomatically = true 둘 다 있다

고친 모양(예시):
```
val position = positionPort.findForUpdate(accountId, ticker)  // PESSIMISTIC_WRITE
position.applySell(fillQty)  // 도메인 메서드가 단일 규칙 원천
positionPort.save(position)
```

### 375위 [med · 확신 low] 포지션 조회가 Account 애그리거트를 fetch join 하고 Account 속성으로 포지션을 거름

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/PositionRepositoryImpl.kt:20` · 항목표 backend/layering · 항목 D4

```
.join(position.account).fetchJoin()  // ← Position 조회에 Account 애그리거트가 딸려 옴
.where(
    position.ticker.eq(ticker.trim().uppercase()),
    position.account.tradingMode.eq(tradingMode),  // ← 다른 애그리거트 속성으로 필터링
    position.quantity.gt(minQuantity),
```

- 원인: Position 이 @ManyToOne 으로 Account 를 직접 가지고(Position.kt:46), 이 쿼리가 join(position.account).fetchJoin() 으로 Account 를 함께 적재하며 position.account.tradingMode 로 거른다. 다른 애그리거트 속성으로 이 애그리거트를 조회한다(PTB3). PositionResult.from 도 p.account?.id 로 연관을 따라간다.
- 생길 수 있는 일: 포지션을 읽을 때마다 계좌 행이 딸려 오고, 한 트랜잭션이 두 애그리거트 상태를 건드릴 여지가 생긴다. 시세 틱마다 도는 조회라 비용이 커진다.
- 해결: Position 에 accountId 만 두고 tradingMode 는 계좌 id 목록 조회 뒤 position.accountId.in(ids) 로 거르거나, 조회 전용 모델에 tradingMode 사본을 둔다.
- 다른 렌즈의 반증: data/query+schema-data-model+transaction-boundary — join(position.account).fetchJoin() 은 @ManyToOne 단건 fetch join 이고 offset·limit·Pageable 이 없다

고친 모양(예시):
```
val accountIds = accountPort.findIdsByTradingMode(tradingMode)
queryFactory.selectFrom(position).where(position.accountId.`in`(accountIds), position.ticker.eq(normalized))
```

### 376위 [med · 확신 low] 시세 처리 지연·적체·누락이 계측되지 않음

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/redis/QuoteEventListener.kt:32` · 항목표 reliability/observability · 항목 O6

```
override fun onMessage(message: Message, pattern: ByteArray?) {
    val quote = parseMessage(message.body) ?: return  // ← 수신 지연·처리 시간·폐기 수 계측 없음
    runCatching { localMatchingEngine.tryMatchPendingOrders(quote.ticker, quote) }
```

- 원인: onMessage 에 now-updatedAt 지연, 처리 소요, 버린 메시지 수, 처리 중 동시 건수를 남기는 게이지·타이머·로그가 없다. 실패 WARN 만 있다. Micrometer 사용도 이 파일에 없다.
- 생길 수 있는 일: Pub/Sub 은 at-most-once 라 놓친 시세 구간을 알 수 없고, 적체나 지연이 장애로 터질 때까지 보이지 않는다.
- 해결: Timer 로 처리 시간, 게이지로 now-updatedAt, 카운터로 파싱 실패·폐기 건수를 기록한다.
- 다른 렌즈의 반증: backend/code-shape+layering+naming-terminology+tdd-purity — public 메서드는 onMessage 하나이고 시세 이벤트를 소비자에게 나눠주는 팬아웃 진입점이라 존재 이유가 하나다. 컨텍스트 경계 문제는 L4·L6 으로 따로 올렸다

고친 모양(예시):
```
quoteLagTimer.record(Duration.between(quote.updatedAt, Instant.now()))
```

### 377위 [med · 확신 low] 같은 종목 시세 처리가 직렬화되지 않고, 낡은 시세를 버리는 비교도 없음

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/redis/QuoteEventListener.kt:34` · 항목표 reliability/load-concurrency · 항목 A2

```
runCatching { localMatchingEngine.tryMatchPendingOrders(quote.ticker, quote) }  // ← 종목 락·큐 없이 여러 스레드가 동시 진입
runCatching {
    positionCommandService.updateCurrentPriceByTicker(quote.ticker, quote.price, PriceSource.REDIS_LIVE, quote.tradingMode)  // ← quote.updatedAt 비교 없이 덮어씀
}
// ExecutionProcessor.kt:97-98 (안전망)
val order = orderRepository.findByIdWithOptimisticLock(orderId).orElse(null) ?: return
if (order.orderStatus !in setOf(OrderStatus.PENDING, OrderStatus.PARTIAL)) return
```

- 원인: 종목 단일 소비자·종목 락이 없어 같은 종목의 두 메시지가 tryMatchPendingOrders 를 동시에 탈 수 있다. 이중 체결은 ExecutionProcessor.processFill 의 상태 재확인(PENDING·PARTIAL)과 Order @Version(+findByIdWithOptimisticLock)·계좌 비관적 락이 막아주지만, 그 결과 한쪽은 낙관적 락 예외로 실패해 WARN 으로만 남는다. updatedAt 비교가 없어 늦게 도착한 옛 틱이 최신 가격 캐시를 덮고(quote.updatedAt 은 position 갱신에 넘기지 않음) 낡은 호가로 매칭할 수 있다.
- 생길 수 있는 일: 이중 체결은 @Version 이 막지만 동시 틱마다 낙관적 락 실패·재시도 잡음이 생기고, 순서가 뒤집힌 틱으로 평가손익 캐시가 옛 가격으로 되돌아간다.
- 해결: 종목 키 단일 소비자(종목별 큐)나 종목 락으로 직렬화하고, updatedAt 비교(종목별 compare-and-set)는 낡은 시세를 버리는 데만 쓴다.
- 다른 렌즈의 반증: backend/code-shape+layering+naming-terminology+tdd-purity — public 메서드는 onMessage 하나이고 시세 이벤트를 소비자에게 나눠주는 팬아웃 진입점이라 존재 이유가 하나다. 컨텍스트 경계 문제는 L4·L6 으로 따로 올렸다

고친 모양(예시):
```
private val lastQuoteAt = ConcurrentHashMap<String, Instant>()
if (lastQuoteAt.merge(quote.ticker, quote.updatedAt) { a, b -> maxOf(a, b) } != quote.updatedAt) return  // 낡은 틱 폐기
```

### 378위 [med · 확신 low] 시세 메시지 필드 누락·알 수 없는 mode 가 로그 없이 버려짐

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/redis/QuoteEventListener.kt:53` · 항목표 reliability/failure-handling · 항목 E1

```
val quote = parseMessage(message.body) ?: return  // ← 버려진 이유가 안 남음
...
ticker = map["ticker"] as? String ?: return@runCatching null,  // ← 필드 누락, 무로그
tradingMode = parseTradingMode(map) ?: return@runCatching null,
...
else -> null  // ← 알 수 없는 mode, 무로그
```

- 원인: parseMessage 는 예외가 나면 WARN 을 남기지만, 필드 누락(ticker·price·askp1·bidp1·updatedAt)이나 알 수 없는 tradingMode 는 return@runCatching null / else -> null 로 아무 로그 없이 null 을 돌려주고, onMessage 는 ?: return 으로 조용히 끝낸다.
- 생길 수 있는 일: collector-api 가 필드명을 바꾸거나 새 mode 를 보내면 모든 시세가 소리 없이 버려져 매칭·포지션 평가·청산 판정이 통째로 멈추는데 로그 줄이 하나도 없어 알아챌 수 없다.
- 해결: null 을 돌려주는 분기마다 WARN(원인 필드·원문 일부 제외한 채널명)을 남기거나 결과 타입에 실패 사유를 담아 호출부가 한 번에 기록한다. 버린 개수는 카운터로 센다.
- 다른 렌즈의 반증: backend/code-shape+layering+naming-terminology+tdd-purity — JSON 역직렬화 실패는 외부 입력의 진짜 오류이고 getOrElse 에서 warn 로그를 남긴다. 한 식으로 검사할 조건이 아니라 통과

고친 모양(예시):
```
val ticker = map["ticker"] as? String ?: run { log.warn { "quote 메시지 ticker 누락" }; return@runCatching null }
```

### 379위 [med · 확신 low] Redis 시세 조회에 명시 타임아웃 설정이 없음

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/redis/RedisMarketQuoteAdapter.kt:31` · 항목표 reliability/failure-handling · 항목 E5

```
val hash = redisTemplate.opsForHash<String, String>().entries(quoteKey(tradingMode, ticker))  // ← 타임아웃은 설정 없이 기본값 의존
// application.yaml:30-33
data:
  redis:
    host: ${REDIS_HOST:localhost}
    port: ${REDIS_PORT:6379}  // ← timeout·connect-timeout 없음
```

- 원인: 주문 접수 경로(OrderCommandService 의 marketQuotePort)가 쓰는 Redis 조회인데 application.yaml 에 spring.data.redis.timeout·connect-timeout 이 없다(host·port 만). 기본값은 Lettuce 기본(명령 60초 안팎)에 기댄다. 라이브러리 소스는 열어 확인하지 못했다(LLM-only).
- 생길 수 있는 일: Redis 가 느려지거나 끊기면 주문 요청이 길게 붙들린다. 기본값 의존이라 운영 의도가 코드에 안 보인다.
- 해결: spring.data.redis.timeout 과 connect-timeout 을 짧게(예: 1~2초) 명시한다.

고친 모양(예시):
```
spring:
  data:
    redis:
      timeout: 2s
      connect-timeout: 2s
```

### 380위 [med · 확신 low] SSE 연결 수 상한이 없음 (제거 콜백은 있음)

`backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/controller/ExecutionSseController.kt:25` · 항목표 reliability/load-concurrency · 항목 C4

```
// ExecutionSseController.kt:25-28
val emitter = SseEmitter(300_000L)
val clientId = UUID.randomUUID().toString()
registry.register(clientId, emitter)   // ← 연결 수 상한 확인 없이 등록
// ExecutionSseRegistry.kt:16-17
fun register(clientId: String, emitter: SseEmitter) {
    emitters[clientId] = emitter   // ← 상한 없음 (18~20줄의 제거 콜백은 있음)
```

- 원인: 레지스트리는 onCompletion·onTimeout·onError 에서 emitter 를 제거하고, 컨트롤러는 300초 타임아웃을 줍니다. 이 부분은 통과입니다. 다만 register 는 무조건 맵에 넣고, 컨트롤러는 현재 연결 수를 확인하지 않습니다. broadcast 도 전체 emitter 에 동기 순차 send 라서(스레드는 미확인) 연결이 늘수록 한 번의 전달이 오래 걸립니다.
- 생길 수 있는 일: 인증이 없고 CORS 가 *(S9) 인 상태에서, 연결을 계속 여는 클라이언트 하나가 emitter·소켓·힙을 무제한으로 쌓고 모든 체결 전달을 느리게 만듭니다.
- 해결: 등록 전에 emitters.size 가 상한(설정값)을 넘으면 429·503 으로 거절합니다. 필요하면 IP·계좌별 상한도 둡니다. broadcast 는 느린 연결이 나머지를 막지 않게 하는 방안(전용 실행기)을 별도로 검토합니다.
- 다른 렌즈의 반증: backend/code-shape+layering+naming-terminology+tdd-purity — F4 는 application·domain 함수 본문만 대상이고 이 값(clientId)은 어느 시험도 단언에 쓰지 않는다

고친 모양(예시):
```
fun register(clientId: String, emitter: SseEmitter) {
    check(emitters.size < MAX_CLIENTS) { "SSE 연결 수 상한 초과" } // → 핸들러에서 503
    emitters[clientId] = emitter
    ...
}
```

### 381위 [med · 확신 low] 체결 SSE 스트림이 호출자·계좌 범위 없이 모든 계좌의 체결을 모든 연결에 전달함

`backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/controller/ExecutionSseController.kt:28` · 항목표 reliability/security-config-hygiene · 항목 S7

```
// ExecutionSseController.kt:25-28
val emitter = SseEmitter(300_000L)
val clientId = UUID.randomUUID().toString()
registry.register(clientId, emitter)   // ← 계좌·호출자 범위 없이 전역 등록
// ExecutionSseRegistry.kt:29-37
fun broadcast(event: ExecutionSseEvent) {
    val snapshot = emitters.toMap()
    snapshot.forEach { (clientId, emitter) ->
        try {
            emitter.send(   // ← 모든 연결에 동일 이벤트 전송
```

- 원인: /stream 은 계좌 id 도 호출자 식별도 받지 않고 clientId 로 UUID 를 새로 만들어 등록합니다. 레지스트리의 broadcast 는 등록된 모든 emitter 에 같은 체결 이벤트를 보냅니다. 이벤트에 계좌 id 가 들어 있는지는 ExecutionSseEvent 를 열지 못해 확인하지 못했습니다.
- 생길 수 있는 일: 스트림에 연결한 누구나 전 계좌의 체결 정보(종목·수량·가격)를 실시간으로 받습니다. 아래 CORS 와일드카드와 인증 없음이 겹쳐서, 운영자 브라우저에서 열린 임의의 웹페이지가 이를 읽을 수 있습니다.
- 해결: 연결 시 accountId 를 받아 레지스트리를 계좌별로 나누고 해당 계좌의 이벤트만 보냅니다. 인증이 생기면 호출자 소유 계좌인지 검증합니다. 내부용 단일 사용자 전제라면 그 근거 문서를 두어 사람이 선택하게 합니다.
- 다른 렌즈의 반증: backend/code-shape+layering+naming-terminology+tdd-purity — F4 는 application·domain 함수 본문만 대상이고 이 값(clientId)은 어느 시험도 단언에 쓰지 않는다

고친 모양(예시):
```
@GetMapping("/stream")
fun stream(@RequestParam accountId: Long): SseEmitter {
    val emitter = SseEmitter(SSE_TIMEOUT_MS)
    registry.register(accountId, UUID.randomUUID().toString(), emitter)
    return emitter
}
// registry.broadcast(accountId, event) 는 해당 accountId 의 emitter 에만 send
```

### 382위 [med · 확신 low] 주문 멱등 키 선조회가 계좌 락보다 앞에 있어, 같은 키 동시 재시도가 재생 응답이 아니라 유일 제약 위반 500 으로 끝날 수 있음

`backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/controller/OrderController.kt:49` · 항목표 reliability/failure-handling · 항목 I1

```
// OrderController.kt:36-49
@Valid @RequestBody request: PlaceOrderRequest,
...
        idempotencyKey = request.idempotencyKey,   // ← 키를 서비스에 위임 (예약·재생은 서비스 몫)
// OrderCommandService.kt:51 (grep 으로 읽은 줄)
orderRepository.findByAccountIdAndIdempotencyKey(accountId, command.idempotencyKey)   // ← 선조회: 락 이전, 경합 구간
// OrderCommandService.kt:55
val account = accountRepository.findByIdWithLock(accountId)   // ← 계좌 락은 선조회보다 뒤
// Order.kt:35
uniqueConstraints = [UniqueConstraint(name = "uk_orders_account_idempotency", columnNames = ["account_id", "idempotency_key"])]   // ← 경합 패자는 여기서 터짐
// GlobalExceptionHandler.kt:59
@ExceptionHandler(Exception::class)   // ← DataIntegrityViolationException 전용 매핑 없음, 최종 그물로 떨어짐
```

- 원인: 컨트롤러는 idempotencyKey 를 본문 필드로 받아 서비스에 넘깁니다. 서비스는 키 선조회(51줄)를 계좌 비관 락(55줄)보다 먼저 합니다. 같은 키 요청 둘이 동시에 오면 둘 다 선조회에서 null 을 보고 락에서 직렬화됩니다. 뒤 요청은 INSERT 에서 uk_orders_account_scope_idempotency(V8) 를 위반합니다. 전역 핸들러의 @ExceptionHandler 목록에는 DataIntegrityViolationException 매핑이 없고, 마지막 Exception::class(59줄) 로 떨어집니다. 이중 주문은 유일 제약과 트랜잭션 롤백이 막으므로 안전성은 유지되지만, 멱등 재생(FH1·FH2)이 아니라 오류 응답이 나갑니다. 같은 키에 다른 본문이 왔을 때 4xx 로 거절하는지는 서비스 52~54줄을 열지 못해 확인하지 못했습니다. LLM-only 판정입니다.
- 생길 수 있는 일: 모바일·프론트의 타임아웃 재시도나 이중 클릭에서 첫 요청은 성공했는데 재시도는 500 을 받습니다. 클라이언트가 실패로 오인해 새 키로 다시 주문할 수 있고, 그러면 같은 주문이 실제로 두 번 들어갑니다.
- 해결: 서비스에서 계좌 락을 잡은 뒤에 키를 조회합니다. 기존 주문이 있으면 본문 지문(수량·종목·가격 등)을 비교해서, 같으면 기존 결과를 재생하고 다르면 409 로 거절합니다. 전역 핸들러에 DataIntegrityViolationException(uk_orders_account_scope_idempotency) 을 409 또는 재조회 재생으로 매핑합니다.

고친 모양(예시):
```
@Transactional
fun placeOrder(accountId: Long, command: PlaceOrderCommand): Order {
    val account = accountRepository.findByIdWithLock(accountId)
        .orElseThrow { AccountNotFoundException(accountId) }
    orderRepository.findByAccountIdAndIdempotencyKey(accountId, command.idempotencyKey)?.let { existing ->
        if (!existing.sameRequestAs(command)) throw IdempotencyKeyConflictException(command.idempotencyKey) // 409
        return existing
    }
    // ... 홀드·주문 생성
}
```

### 383위 [med · 확신 low] @EnableAsync 에 기본 실행기 지정이 없어 한정자 없는 @Async 가 어느 풀을 쓰는지 불분명

`backend/trading-api/src/main/kotlin/com/papertrading/api/TradingApiApplication.kt:12` · 항목표 reliability/load-concurrency · 항목 A3

```
@EnableAsync   // ← 기본 @Async 실행기를 명시하지 않음 (ExecutionSseEventHandler.kt:18 의 @Async 는 한정자 없음)
```

- 원인: AsyncConfig 에는 Slack 전용 실행기 빈 하나만 있고 AsyncConfigurer 도 없다. ExecutionSseEventHandler 의 @Async 는 한정자가 없다. Executor 빈이 하나뿐이면 Boot 기본 실행기 자동 구성이 물러나 SSE 브로드캐스트가 Slack 풀(core 2·max 4·queue 100·CallerRunsPolicy)을 공유할 수 있다. 이 동작은 Spring Boot 문서 기준 추정이며 패키지 소스는 열어 확인하지 못했다.
- 생길 수 있는 일: Slack 전송이 밀리면 SSE 브로드캐스트가 같은 큐에서 기다리고, 큐가 차면 CallerRunsPolicy 로 커밋한 스레드가 직접 브로드캐스트를 떠안는다.
- 해결: SSE 용 실행기를 따로 정의해 @Async("sseExecutor") 로 지정하거나, AsyncConfigurer 로 기본 실행기를 명시한다.

### 384위 [med · 확신 low] KIS 자격 증명 기본값이 빈 문자열이라 누락이 첫 호출에야 드러날 수 있음

`backend/trading-api/src/main/resources/application.yaml:39` · 항목표 reliability/security-config-hygiene · 항목 S4

```
    paper-app-key: ${KIS_PAPER_APP_KEY:}        # ← 빈 기본값 — 누락돼도 기동 성공
    paper-app-secret: ${KIS_PAPER_APP_SECRET:}
```

- 원인: 4개 KIS 키·시크릿이 ${ENV:} 빈 기본값이다. 이 키들을 읽는 속성 클래스(@ConfigurationProperties·@Validated·@NotBlank 여부)는 열지 않아 시작 시 검증이 있는지는 확인하지 못했다(LLM-only).
- 생길 수 있는 일: 키가 빠져도 기동은 성공하고 첫 KIS 호출에서 인증 실패로 드러난다.
- 해결: KIS 속성 클래스에 @Validated + @NotBlank 를 달고, KIS 연동을 켠 모드에서는 값이 없으면 기동 실패하게 한다.

### 385위 [med · 확신 low] 통합 시험에서 내부 서비스를 @MockitoBean 으로 갈음

`backend/trading-api/src/test/kotlin/com/papertrading/api/application/portfolio/SnapshotJobServiceAtomicRollbackIntegrationTest.kt:52` · 항목표 backend/tdd-purity · 항목 T1

```
@MockitoBean
lateinit var portfolioSnapshotCommandService: PortfolioSnapshotCommandService  // ← 내부 서비스 대체
```

- 원인: PortfolioSnapshotCommandService 를 목으로 바꿔 실패를 주입한다. 실패 주입 목적이라 허용 가능성이 있으나, 외부 경계가 아닌 내부 서비스 대체라 T1 에 해당한다. 롤백 시험 자체는 실제 Postgres 로 돌아 심각도를 낮춘다.
- 생길 수 있는 일: 포트가 없어 실패 주입 지점이 내부 서비스뿐이라 서비스 시그니처 변경에 시험이 깨진다.
- 해결: 실패를 주입할 포트(저장 포트 등)를 두고 그 가짜로 실패를 일으킨다.
- 다른 렌즈의 반증: backend/code-shape+layering+naming-terminology+tdd-purity — 컨테이너가 클래스 단위 static 이라 클래스가 끝나면 사라지고 시험 시작에서 deleteAll 로 비운다. 시험 사이 오염이 없다. 통과.

고친 모양(예시):
```
@TestConfiguration class FailingSnapshotPortConfig { @Bean @Primary fun port(): PortfolioSnapshotPort = FailingPort() }
```

### 386위 [med · 확신 low] 내부 협력자 PositionExitTriggerOrchestrator 를 relaxed mockk 로 갈음

`backend/trading-api/src/test/kotlin/com/papertrading/api/application/position/PositionExitQuoteEventListenerTest.kt:12` · 항목표 backend/tdd-purity · 항목 T1

```
val orchestrator = mockk<PositionExitTriggerOrchestrator>(relaxed = true)  // ← 같은 레포 application 서비스를 목으로 대체
```

- 원인: 리스너 시험이 같은 레포 application 서비스인 오케스트레이터를 mockk(relaxed = true) 로 대체하고 그 호출만 본다. 리스너가 사실상 한 줄 위임이라 상호작용이 곧 행동이라는 점을 감안해 심각도를 낮춘다.
- 생길 수 있는 일: 오케스트레이터 시그니처 변경마다 깨지고, 위임 대상이 바뀌어도 실제 트리거 평가는 이 시험에서 한 번도 돌지 않는다.
- 해결: 오케스트레이터를 실제 객체(가짜 저장소·가짜 주문 포트 조합)로 쓰는 통합 성격 시험으로 합치거나, 위임만 있는 이 클래스는 별도 시험 없이 오케스트레이터 시험이 커버하게 한다.
- 다른 렌즈의 반증: backend/code-shape+layering+naming-terminology+tdd-purity — 같은 인스턴스 now 를 입력으로 넘기고 verify 인자로 그대로 비교한다. 시계 값에 기대지 않아 결정적이다.

고친 모양(예시):
```
val listener = PositionExitQuoteEventListener(realOrchestratorWithFakes)
listener.onQuote("005930", BigDecimal("100"), now)
assertEquals(TriggerState.TRIGGERED, trigger.state)
```

### 387위 [med · 확신 low] 영속 예외를 목이 직접 던지게 해 @Transactional 경계에서의 예외 번역·커밋 경로를 우회

`backend/trading-api/src/test/kotlin/com/papertrading/api/application/position/PositionExitTriggerOrchestratorTest.kt:171` · 항목표 backend/tdd-purity · 항목 T10

```
every { autoExitOrderPlacementService.createGroupedAutoExitSellOrder(any(), any(), any(), any(), any(), any()) } throws
    DataIntegrityViolationException("uq")  // ← 예외가 목에서 직접 발생, 실제 flush·커밋 경로 아님
every { positionRepository.findByIdWithLock(10L) } throws PessimisticLockingFailureException("lock")  // ← 212행 동일
```

- 원인: 오케스트레이터는 @Transactional onQuote 안에서 createGroupedAutoExitSellOrder 의 DataIntegrityViolationException·CannotAcquireLockException 을 잡아 분기한다(Orchestrator.kt:36-49·155). 시험은 목이 예외를 직접 던지게 해(171행) 같은 트랜잭션에 참여한 서비스가 던진 영속 예외가 롤백 전용으로 표시된 뒤 커밋 시점에 UnexpectedRollbackException 이 되는 실제 경로를 타지 않는다. AutoExitOrderPlacementService 의 전파 설정은 열지 않아 confidence 를 낮췄다.
- 생길 수 있는 일: 중복 키를 «이미 성공»으로 처리한다는 분기가 시험은 초록이지만 운영 커밋에서 예외가 터질 수 있다.
- 해결: REQUIRES_NEW 유스케이스로 주문을 만들고 영속 예외 번역은 어댑터가 하게 한 뒤, 중복 키 시나리오를 실제 PostgreSQL 위 @SpringBootTest 로 커밋까지 돌린다.

고친 모양(예시):
```
@Test fun `같은 order_group_id 로 동시에 두 번 발동하면 한 주문만 남는다`() { /* 실제 DB, 트랜잭션 커밋 포함 */ }
```

### 388위 [med · 확신 low] collector-api 가 쓰는 Redis 시세 해시 키·필드를 시험이 손으로 베낌

`backend/trading-api/src/test/kotlin/com/papertrading/api/application/position/PositionQueryServiceTest.kt:91` · 항목표 backend/tdd-purity · 항목 T11

```
val key = if (mode == "local") {
    "quote:local:${ticker.uppercase()}"  // ← collector 계약을 손으로 베낌, 출처 표기 없음
} else { "quote:kis:$mode:${ticker.uppercase()}" }  // ← 시험이 local 만 써서 이 가지는 미실행
redisTemplate.opsForHash<String, String>().putAll(key, mapOf("price" to price, "askp1" to price, ...
```

- 원인: saveQuote 가 quote:local:<TICKER>·quote:kis:<mode>:<TICKER> 키와 price·askp1·bidp1·updatedAt 필드를 직접 적는다. 이 형식의 출처(collector-api 발행 코드·계약 문서·픽스처)가 시험에 없다. else 가지(quote:kis:$mode:...)는 시험이 "local" 만 넘겨 한 번도 실행되지 않는다.
- 생길 수 있는 일: collector-api 가 키 이름이나 필드를 바꿔도 이 시험과 읽는 쪽 코드가 같은 믿음을 공유해 둘 다 초록이다.
- 해결: 키 패턴·필드 이름을 collector-api 와 공유하는 상수나 계약 문서 경로를 시험 주석으로 남기고, 같은 키를 쓰는 collector 쪽 발행 시험과 대조하는 계약 시험을 둔다.

고친 모양(예시):
```
// 출처: collector-api QuoteRedisKeys.kt — local 키 패턴
val key = QuoteRedisKeys.local(ticker)
```

### 389위 [med · 확신 low] 손으로 쓴 KIS 체결통보 원문의 출처가 시험에 없음

`backend/trading-api/src/test/kotlin/com/papertrading/api/infrastructure/kis/KisExecutionNoticeParserTest.kt:18` · 항목표 backend/tdd-purity · 항목 T11

```
            rawMessage = "0|H0STCNI9|001|ODNO=10000001^CCLD_NO=00003^PDNO=005930^SLL_BUY_DVSN_CD=02^CCLD_QTY=3.5^CCLD_UNPR=70000.25^CCLD_DTTM=20260501093015000^CANO=12345678^ACNT_PRDT_CD=01",  // ← 손으로 쓴 KIS 원문, 출처 표시 없음
```

- 원인: 파서 시험의 입력인 KIS 실시간 체결통보 원문(paper 의 '0|H0STCNI9|001|KEY=값^…' 키=값 형식, live 의 JSON 형식)을 직접 적었고 형식 출처(KIS 문서·녹취 픽스처 파일)가 시험 어디에도 없다. 파서와 시험이 같은 믿음을 복사했다면 둘 다 초록인 채 실제 KIS 형식과 어긋날 수 있다. 실제 KIS 형식과의 일치 여부는 이 감사에서 확인하지 못했다(KIS 문서 미열람).
- 생길 수 있는 일: 실제 KIS 체결통보 형식이 다르면 파서가 운영에서 조용히 null 을 돌려주고(체결 누락) 시험은 계속 초록이다.
- 해결: 녹취한 실제 메시지를 test resources 픽스처 파일로 두고 파일 머리에 KIS 문서 위치·수집일을 적어 시험이 그 파일을 읽게 한다.

### 390위 [med · 확신 low] 손으로 지은 KIS 오류 응답(rt_cd·msg1)의 출처가 시험에 없음

`backend/trading-api/src/test/kotlin/com/papertrading/api/infrastructure/kis/KisOrderRestClientTest.kt:43` · 항목표 backend/tdd-purity · 항목 T11

```
        } returns ResponseEntity.ok(mapOf("rt_cd" to "1", "msg1" to "invalid account"))  // ← 손으로 지은 KIS 오류 응답, 코드·문구 출처 없음
```

- 원인: rt_cd="1", msg1="invalid account" 를 직접 적어 KisErrorCode.KIS_INVALID_ACCOUNT 매핑을 단언한다. 응답 코드·문구의 출처(KIS 문서·녹취 픽스처)가 없어, 매핑 코드가 기대는 필드(rt_cd·msg_cd·msg1)와 실제 KIS 응답이 일치하는지 이 시험으로는 보장되지 않는다. 실제 KIS 응답 형식은 이 감사에서 확인하지 못했다.
- 생길 수 있는 일: 실제 응답과 다른 문구로 통과한 시험 때문에 운영의 계좌 오류 응답이 다른 코드로 매핑돼도 회귀가 안 잡힌다.
- 해결: 실제 KIS 오류 응답을 픽스처 파일(출처·수집일 표기)로 두고 그 파일을 읽어 응답으로 쓴다.

### 391위 [low · 확신 high] detekt 가 기본 임계(LongMethod 60·NestedBlockDepth 4)만 쓰고 팀 수치(40·3)로 조이는 설정이 없음

`backend/trading-api/build.gradle:74` · 항목표 backend/code-shape · 항목 S11

```
detekt {
	buildUponDefaultConfig = true   // ← 기본 임계(LongMethod 60·NestedBlockDepth 4)만 사용, config 파일로 팀 수치(40·3)를 조이는 설정 없음
	baseline = file('config/detekt/baseline.xml')
}
```

- 원인: detekt 블록이 buildUponDefaultConfig = true 와 baseline 지정만 하고 config 파일을 얹지 않는다. backend/trading-api/config/detekt/ 에는 baseline.xml 하나뿐이고 detekt.yml 은 없다(ls 와 grep 으로 확인). 그래서 팀 규칙 CS25 의 함수 40줄·중첩 3단보다 느슨한 기본값(LongMethod 60·NestedBlockDepth 4)이 그대로 적용된다. detekt 는 «새 발견 0건»으로 통과하지만 41~60줄 함수와 중첩 4단은 한 번도 걸리지 않으므로, 이 구간은 사람의 리뷰만이 지킨다. 반대쪽 절반인 기준선 증가 차단은 CI 에서 돈다. build.gradle 의 verifyLintBaselinesNotGrown 이 full-test.yml 의 run 줄에 들어 있고, 이 쪽은 통과다.
- 생길 수 있는 일: 41~60줄 함수와 중첩 4단 코드가 도구에 안 걸리고 머지된다. 주문·체결·정산 같은 금융 경로에서 추상화 수준이 섞인 긴 함수가 리뷰 누락 한 번으로 들어올 수 있다. 기준선 243건이 «안다»일 뿐 «괜찮다»가 아니라는 점도 도구 출력에서 드러나지 않는다.
- 해결: config/detekt/detekt.yml 을 만들어 complexity.LongMethod.threshold 를 40 으로, complexity.NestedBlockDepth.threshold 를 3 으로 조이고, detekt 블록에 config.setFrom(files('config/detekt/detekt.yml')) 을 추가한다. 조이면 기준선 밖 새 위반이 한꺼번에 나온다. verifyLintBaselinesNotGrown 이 기준선 증가를 막으므로 baseline 재생성으로 흡수할 수 없다. 기존 위반을 고칠지 일괄 면제할지는 사람이 정한다.

고친 모양(예시):
```
# config/detekt/detekt.yml
complexity:
  LongMethod:
    threshold: 40
  NestedBlockDepth:
    threshold: 3

// build.gradle
detekt {
	buildUponDefaultConfig = true
	config.setFrom(files('config/detekt/detekt.yml'))
	baseline = file('config/detekt/baseline.xml')
}
```

### 392위 [low · 확신 high] Boolean 필드 enabled 에 is 접두가 없음

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/account/command/UpsertAccountExitTriggerDefaultCommand.kt:7` · 항목표 backend/naming-terminology · 항목 N3

```
val enabled: Boolean, // ← is 접두 없음
```

- 원인: AccountExitTriggerDefault.enabled 와 같은 이름이 그대로 전파된다.
- 생길 수 있는 일: 일관성 없는 불리언 이름이 계층마다 번역을 만든다.
- 해결: 엔티티와 함께 isEnabled 로 통일한다.

### 393위 [low · 확신 high] Boolean 필드 enabled 에 is 접두가 없음

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/account/result/AccountExitTriggerDefaultResult.kt:8` · 항목표 backend/naming-terminology · 항목 N3

```
val enabled: Boolean, // ← is 접두 없음
```

- 원인: 결과 DTO 의 enabled 가 같은 이름을 따른다.
- 생길 수 있는 일: API 응답 필드와 도메인 필드 이름의 일관성이 흔들린다.
- 해결: 엔티티와 함께 isEnabled 로 통일한다(응답 JSON 필드명은 별도 호환 검토).

### 394위 [low · 확신 high] application 이 http 패키지 import 를 달고 있다

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/notification/SlackNotificationService.kt:8` · 항목표 backend/layering · 항목 L2

```
import com.papertrading.api.common.exception.ApiDomainException   // ← 쓰이지 않는 import
// …
import org.springframework.http.HttpStatus   // ← 허용 목록 밖 org.springframework.http.*, 본문에서 쓰이지 않음
```

- 원인: 서비스가 허용 목록 밖 org.springframework.http.HttpStatus 를 import 한다. 본문에서는 쓰지 않는 죽은 import 라 동작에는 영향이 없다. ApiDomainException(L8)도 쓰이지 않는다. 바이트코드에 남지 않아 ArchUnit 이 못 본다.
- 생길 수 있는 일: 죽은 의존이 남아 이후 누가 HttpStatus 를 쓰기 시작해도 막는 장치가 없고, application 이 HTTP 의미를 알게 되는 입구가 된다.
- 해결: HttpStatus·ApiDomainException import 를 지운다. ktlint no-unused-imports 를 켜서 재발을 막는다.

고친 모양(예시):
```
// 두 import 줄 삭제
```

### 395위 [low · 확신 high] 생성자 주입 의존 11개

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/ExecutionProcessor.kt:39` · 항목표 backend/code-shape · 항목 S4

```
class ExecutionProcessor(
    private val orderRepository: OrderRepository,
    private val accountRepository: AccountRepository,
    private val positionRepository: PositionRepository,
    private val executionRepository: ExecutionRepository,
    private val accountLedgerRepository: AccountLedgerRepository,
    private val feePolicyRepository: FeePolicyRepository,
    private val collectorSubscriptionPort: CollectorSubscriptionPort,
    private val settlementRepository: SettlementRepository,
    private val settlementExecutionRepository: SettlementExecutionRepository,
    private val eventPublisher: ApplicationEventPublisher,        // ← 11개째
    private val slackNotificationEventPublisher: SlackNotificationEventPublisher,
) {
```

- 원인: 리포지토리 8개·포트 1개·이벤트 발행기 2개를 주입받는다(한계 7). detekt 기준선에 LongParameterList 로 흡수돼 있다(tools/Tb661de0c.txt:74). S1·S3 와 같은 뿌리다.
- 생길 수 있는 일: 넓은 의존 표면이 책임 여럿을 숨기고, 시험이 의존 11개를 모두 대체해야 한다.
- 해결: S3 로 책임을 나누면 의존이 자연히 갈린다. 정산·구독 해제를 후속 리스너로 빼면 이 클래스는 약 6개가 된다.

### 396위 [low · 확신 high] 공개 함수 인자 이름의 약어 qty (fillQty ×3)

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/ExecutionProcessor.kt:59` · 항목표 backend/naming-terminology · 항목 N6

```
        fillQty: BigDecimal,                 // ← L59 약어 qty (L74, L91 도 동일)
```

- 원인: fillLocal(L59)·fillExternal(L74)·processFill(L91)의 공개 인자가 fillQty 다. 도메인은 quantity 로 풀어 쓴다.
- 생길 수 있는 일: 같은 개념이 quantity/qty 로 갈려 도메인과 서비스 사이 번역층이 생긴다.
- 해결: fillQuantity 로 바꾼다.

### 397위 [low · 확신 high] 여러 save 를 부르는 public 메서드 processFill 에 @Transactional 이 없음

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/ExecutionProcessor.kt:88` · 항목표 data/transaction-boundary · 항목 X1

```
// 메인 체결 함수
fun processFill(   // ← public 이고 @Transactional 없음 — 외부 호출 시 save 마다 개별 커밋
```

- 원인: processFill 은 public 이고 Execution·Position·Ledger·Settlement 를 여러 번 save 하지만 어노테이션이 없습니다. 지금은 같은 클래스의 fillLocal·fillExternal(@Transactional)만 부르지만(:61·:78), 다른 빈이 직접 부르면 리포지토리 호출마다 따로 커밋되고 AFTER_COMMIT 리스너도 트랜잭션이 없어 발행되지 않습니다.
- 생길 수 있는 일: 현재 외부 호출처가 없어 실피해는 없지만, 누군가 직접 호출하면 중간 실패에 앞 쓰기만 커밋되는 부분 체결이 생깁니다.
- 해결: processFill 을 private 으로 바꿔 경계가 fillLocal·fillExternal 에만 있게 합니다.

고친 모양(예시):
```
private fun processFill(orderId: Long, fillPrice: BigDecimal, fillQty: BigDecimal, externalExecutionId: String, executedAt: Instant) { ... }
```

### 398위 [low · 확신 high] 같은 체결번호 중복 조회를 handle 과 processFill 이 연달아 수행

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/KisExecutionNoticeService.kt:22` · 항목표 data/query · 항목 E5

```
if (executionRepository.findByExternalExecutionId(notice.externalExecutionId).isPresent) {   // ← 1차 조회
...
executionProcessor.fillExternal(   // ← processFill 이 같은 조회를 다시 함 (ExecutionProcessor.kt:95)
```

- 원인: handle 이 externalExecutionId 중복 여부를 조회한 뒤 fillExternal→processFill 첫 줄(ExecutionProcessor.kt:95)이 같은 조회를 다시 합니다. 두 번째 조회는 순수 비용입니다.
- 생길 수 있는 일: 체결 통지마다 불필요한 조회가 1회 더 나갑니다. 동시 중복 통지를 막는 효과는 유니크 제약이 하므로 이 조회는 방어가 되지 못합니다.
- 해결: 중복 확인을 processFill 한 곳에만 두거나, 로그가 필요하면 processFill 이 결과를 돌려주게 합니다.

### 399위 [low · 확신 high] 생성자 주입 의존 9개

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/OrderCommandService.kt:36` · 항목표 backend/code-shape · 항목 S4

```
class OrderCommandService(
    private val accountRepository: AccountRepository,
    private val orderRepository: OrderRepository,
    private val positionRepository: PositionRepository,
    private val accountLedgerRepository: AccountLedgerRepository,
    private val marketQuotePort: MarketQuotePort,
    private val collectorSubscriptionPort: CollectorSubscriptionPort,
    private val localMatchingEngine: LocalMatchingEngine,
    private val kisPaperOrderExecutor: KisPaperOrderExecutor,
    private val kisLiveOrderExecutor: KisLiveOrderExecutor, // ← 의존 9개
```

- 원인: 리포지토리 4 + 포트 2 + 매칭 엔진 + 실행기 2 = 9개로 기준(7)을 넘는다. detekt LongParameterList 기준선에도 올라 있다.
- 생길 수 있는 일: 책임이 넓어 시험 준비가 무거워지고(목 9개) 변경 이유가 늘어난다.
- 해결: 외부 주문 전달(paper·live 실행기)을 하나의 포트로 묶고, 잠금 처리 책임을 분리해 의존을 줄인다.

### 400위 [low · 확신 high] 인자 6개 함수(createGroupedAutoExitSellOrder)

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/OrderCommandService.kt:180` · 항목표 backend/code-shape · 항목 S5

```
fun createGroupedAutoExitSellOrder(
    accountId: Long,
    ticker: String,
    marketType: com.papertrading.api.domain.enums.MarketType,
    quantity: BigDecimal,
    orderGroupId: String,
    triggerAuditInputs: List<AutoExitTriggerAuditInput>, // ← 인자 6개
): Order {
```

- 원인: accountId·ticker·marketType·quantity·orderGroupId·triggerAuditInputs 6개를 위치로 받는다. detekt LongParameterList 기준선에 있다. 호출처 AutoExitOrderPlacementService 도 같은 6개 시그니처를 되풀이한다.
- 생길 수 있는 일: 같은 타입(ticker·orderGroupId: String) 인자가 뒤바뀌어도 컴파일이 통과한다.
- 해결: GroupedAutoExitSellCommand data class 하나로 묶는다.

고친 모양(예시):
```
data class GroupedAutoExitSellCommand(
    val accountId: Long, val ticker: String, val marketType: MarketType,
    val quantity: BigDecimal, val orderGroupId: String,
    val triggerAuditInputs: List<AutoExitTriggerAuditInput>,
)
```

### 401위 [low · 확신 high] 한 작업 안에서 계좌 락 조회 3번·포지션 조회 2번을 같은 조건으로 반복

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/portfolio/SnapshotJobService.kt:60` · 항목표 data/query · 항목 E5

```
// SnapshotJobService.kt:60
        val account = accountRepository.findByIdWithLock(accountId)    // ← 1번째 락 조회
// DailyBalanceCommandService.kt:22,24
        val account = accountRepository.findByIdWithLock(accountId)    // ← 2번째 락 조회
        val positions = positionRepository.findByAccountIdAndQuantityGreaterThan(accountId, BigDecimal.ZERO)    // ← 포지션 1번째
// PortfolioSnapshotCommandService.kt:25,27
        val account = accountRepository.findByIdWithLock(accountId)    // ← 3번째 락 조회
        val positions = positionRepository.findByAccountIdAndQuantityGreaterThan(accountId, BigDecimal.ZERO)    // ← 같은 포지션 조회 2번째
```

- 원인: generateDailySnapshots 한 번이 계좌를 findByIdWithLock 으로 3번(SnapshotJobService L60, DailyBalanceCommandService L22, PortfolioSnapshotCommandService L25), 같은 계좌의 보유 포지션을 findByAccountIdAndQuantityGreaterThan 으로 2번 읽는다. findByIdWithLock 은 JPQL 쿼리라 영속성 컨텍스트에 이미 있어도 SELECT … FOR UPDATE 를 다시 보낸다. 두 번째 읽기는 순수 비용이다.
- 생길 수 있는 일: 스냅샷 작업마다 계좌당 불필요한 왕복이 4번 늘고, 계좌 락을 쥔 시간이 그만큼 길어진다.
- 해결: 오케스트레이터가 account 와 positions 를 한 번만 읽고, 두 계산 메서드에 인자로 넘기도록 시그니처를 바꾼다.

고친 모양(예시):
```
fun recalculate(account: Account, positions: List<Position>, businessDate: LocalDate): DailyBalance { /* 읽기 없음 */ }
```

### 402위 [low · 확신 high] 순수 계산기에 @Component 를 달아 허용 목록 밖 Spring 을 import

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/portfolio/tax/TaxSummaryCalculator.kt:5` · 항목표 backend/layering · 항목 L2

```
import org.springframework.stereotype.Component  // ← 허용 목록 밖
@Component
class TaxSummaryCalculator {
```

- 원인: PTB9 가 application 에 허용하는 stereotype 은 @Service 뿐인데 순수 계산기가 @Component 를 쓴다(ArchUnit 동결 278행). 해가 거의 없어 low 로 낮춘다.
- 생길 수 있는 일: 규칙 예외가 쌓일 뿐 동작 영향은 작다.
- 해결: Spring 의존 없는 일반 클래스로 두고 조립 자리(설정)에서 만들거나 @Service 로 바꾼다.

고친 모양(예시):
```
class TaxSummaryCalculator { /* 어노테이션 없음 */ }
```

### 403위 [low · 확신 high] 같은 타입 id 인자 둘(journalId, accountId)이 나란히 옴

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/portfolio/TradingJournalQueryService.kt:29` · 항목표 backend/layering · 항목 D6

```
fun get(journalId: Long, accountId: Long): TradingJournalResult {  // ← 같은 타입 id 둘이 나란히
```

- 원인: get(journalId: Long, accountId: Long) 의 두 Long id 가 호출부에서 뒤바뀌어도 컴파일된다. 시험도 queryService.get(9L, 2L) 처럼 숫자만 적는다.
- 생길 수 있는 일: 인자 순서를 바꿔 호출하면 엉뚱한 일지를 읽거나 항상 소유권 불일치 예외가 난다.
- 해결: id 값 객체(`JournalId`·`AccountId`)나 query 객체로 받는다.

고친 모양(예시):
```
data class TradingJournalKey(val journalId: Long, val accountId: Long)
fun get(key: TradingJournalKey): TradingJournalResult
```

### 404위 [low · 확신 high] createGroupedAutoExitSellOrder 인자가 6개다

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/position/AutoExitOrderPlacementService.kt:17` · 항목표 backend/code-shape · 항목 S5

```
fun createGroupedAutoExitSellOrder(  // ← 인자 6개
    accountId: Long,
    ticker: String,
    marketType: MarketType,
    quantity: BigDecimal,
    orderGroupId: String,
    triggerAuditInputs: List<AutoExitTriggerAuditInput>,
): Order =
```

- 원인: accountId·ticker·marketType·quantity·orderGroupId·triggerAuditInputs 6개. detekt LongParameterList 기준선에도 있다. 호출부(Orchestrator)가 이미 이름 인자를 쓰지만 서비스 시그니처는 명령 객체가 아니다.
- 생길 수 있는 일: 인자가 하나 더 붙을 때마다 REQUIRES_NEW 래퍼와 OrderCommandService 두 곳의 시그니처를 같이 고쳐야 해서 한쪽이 어긋나기 쉽다.
- 해결: CreateAutoExitSellOrderCommand data class 하나로 받는다.

### 405위 [low · 확신 high] 같은 두 id 를 받는 함수 둘의 인자 순서가 서로 반대다

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/position/PositionExitTriggerCommandService.kt:89` · 항목표 backend/layering · 항목 D6

```
private fun findTriggerForUpdate(triggerId: Long, positionId: Long): PositionExitTrigger {  // ← triggerId 먼저
...
private fun exitTriggerNotFound(positionId: Long, triggerId: Long): NotFoundException =  // ← positionId 먼저
```

- 원인: findTriggerForUpdate(triggerId, positionId) 는 triggerId 가 먼저이고 exitTriggerNotFound(positionId, triggerId) 는 positionId 가 먼저다. 둘 다 Long 이고 호출부가 위치 인자로 넘긴다(L54·L75·L91·L93).
- 생길 수 있는 일: 한 호출부에서 순서를 헷갈려도 컴파일이 통과해 다른 포지션의 트리거를 조회하거나 NotFound 메시지의 id 가 뒤바뀐다.
- 해결: 두 함수의 인자 순서를 맞추거나 id 를 값 클래스(PositionId·TriggerId)로 감싼다.

### 406위 [low · 확신 high] 단건 조회에서 같은 캐시 조회를 두 번 수행

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/position/PositionQueryService.kt:45` · 항목표 data/query · 항목 E5

```
livePositionCachePort.find(accountId, normalizedTicker)?.let {      // ← L35 첫 조회
...
livePositionCachePort.find(position.account.id!!, position.ticker)   // ← L45 같은 키로 재조회
    ?: snapshotFromDbWithLatestQuote(position)
```

- 원인: getPositionWithCurrentPrice 가 L35 에서 livePositionCachePort.find(accountId, ticker)를 하고, 없으면 DB 조회 뒤 snapshotFor 가 L45 에서 같은 키로 다시 find 한다. 첫 조회가 비었던 직후라 두 번째도 거의 빈다.
- 생길 수 있는 일: 캐시 미스 때마다 Redis 왕복 1회가 낭비된다. 비용이 작아 low.
- 해결: L41 에서 snapshotFromDbWithLatestQuote(position)를 바로 호출한다. snapshotFor 는 목록 경로에서 쓰지 않으니 제거한다.

고친 모양(예시):
```
return PositionResult.from(snapshotFromDbWithLatestQuote(position))
```

### 407위 [low · 확신 high] 파일 이름과 안의 타입 이름이 다르고 그 별칭은 쓰이지 않는다

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/position/result/EffectivePositionExitTriggerResult.kt:1` · 항목표 backend/naming-terminology · 항목 N7

```
typealias PositionExitTriggersResult = PositionExitTriggerListResult  // ← 파일명 EffectivePositionExitTriggerResult.kt 와 불일치, 참조 0건
```

- 원인: 파일은 EffectivePositionExitTriggerResult.kt 인데 안에는 typealias PositionExitTriggersResult 하나뿐이다. ktlint standard:filename 이 L1:1 에서 잡는다(tools/T33dcbad6.txt). main 전체에서 PositionExitTriggersResult 를 참조하는 곳이 이 정의 줄뿐이다(grep).
- 생길 수 있는 일: Effective 개념(계정 기본값 병합)이 QueryService 주석에서 이미 빠졌는데 이름이 남아 사라진 설계를 가리킨다. 안 쓰는 별칭이 같은 결과 타입의 두 번째 이름으로 굳는다.
- 해결: 쓰이지 않는 별칭이면 파일을 지우고, 쓸 거면 파일명을 별칭 이름에 맞춘다.

### 408위 [low · 확신 high] 같은 타입 id 인자(refOrderId, refExecutionId)가 나란히 옴

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/account/Account.kt:165` · 항목표 backend/layering · 항목 D6

```
fun recordBuyExecution(cost: BigDecimal, refOrderId: Long, refExecutionId: Long, idempotencyKey: String): AccountLedger { // ← Long id 두 개가 나란히
fun recordSellExecution(netProceeds: BigDecimal, refOrderId: Long, refExecutionId: Long, idempotencyKey: String): AccountLedger {
fun recordFee(fee: BigDecimal, refOrderId: Long, refExecutionId: Long, idempotencyKey: String): AccountLedger {
```

- 원인: recordBuyExecution·recordSellExecution·recordFee 가 refOrderId: Long, refExecutionId: Long 을 연달아 받는다. 호출부는 위치 인자로 부른다(AccountTest 의 recordBuyExecution(BigDecimal("30000.0000"), 1L, 10L, "buy-exec-10")).
- 생길 수 있는 일: 주문 id 와 체결 id 를 뒤바꿔 넘겨도 컴파일이 통과하고 원장의 참조 필드가 조용히 틀어진다.
- 해결: 값 객체(OrderId·ExecutionId)나 참조 묶음 data class(LedgerReference(orderId, executionId))로 받는다.

### 409위 [low · 확신 high] Account.create 인자가 6개

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/account/Account.kt:335` · 항목표 backend/code-shape · 항목 S5

```
fun create( // ← 인자 6개
    accountName: String,
    accountType: AccountType,
    tradingMode: TradingMode,
    initialDeposit: BigDecimal,
    baseCurrency: String = "KRW",
    externalAccountId: String? = null
): Account {
```

- 원인: accountName·accountType·tradingMode·initialDeposit·baseCurrency·externalAccountId. detekt LongParameterList 기준선에도 있다. CreateAccountCommand 가 이미 같은 6개 묶음이다.
- 생길 수 있는 일: 위치 결합으로 인자 순서 실수와 시그니처 변경 파급이 생긴다.
- 해결: 도메인 안에서 쓰는 생성 명세 값 객체(AccountSpec)로 받거나, 호출부 모두 이름 있는 인자를 쓰게 한다.

### 410위 [low · 확신 high] Boolean 속성 enabled 에 is 접두가 없음

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/account/AccountExitTriggerDefault.kt:24` · 항목표 backend/naming-terminology · 항목 N3

```
@Column(name = "enabled", nullable = false)
var enabled: Boolean = false // ← is 접두 없음(Account.isActive 와 불일치)
```

- 원인: 같은 클래스 계열의 Account.isActive·RiskPolicy.isActive 는 is 접두를 쓰는데 enabled 만 다르다(CS3).
- 생길 수 있는 일: if (enabled) 가 명사처럼 읽히고 같은 코드베이스의 isActive 규칙과 어긋난다.
- 해결: isEnabled 로 바꾸고 같은 이름을 쓰는 Account·Command·Result·컬럼명까지 함께 맞춘다.

### 411위 [low · 확신 high] 도메인 팩토리가 Instant.now() 를 직접 읽음

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/account/RiskEvent.kt:61` · 항목표 backend/code-shape · 항목 F4

```
internal fun create(account: Account, eventType: String, riskPolicy: RiskPolicy? = null, description: String? = null): RiskEvent = RiskEvent().apply {
    this.triggeredAt = Instant.now() // ← Clock 주입 아님
```

- 원인: RiskEvent.create 가 triggeredAt 을 Instant.now() 로 채운다. Clock·시각 인자 주입이 아니다. 현재 main 에서 RiskEvent.create 호출처는 grep 으로 찾지 못했다.
- 생길 수 있는 일: 이 팩토리를 쓰는 시험이 시각을 고정할 수 없고, 호출이 생기면 시각 의존 비결정이 시험에 들어온다.
- 해결: triggeredAt: Instant 를 인자로 받거나 Clock 을 주입받는다.

고친 모양(예시):
```
internal fun create(account: Account, eventType: String, triggeredAt: Instant, /* … */): RiskEvent = RiskEvent().apply {
    this.triggeredAt = triggeredAt
}
```

### 412위 [low · 확신 high] FeePolicy.create 인자 7개

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/feepolicy/FeePolicy.kt:94` · 항목표 backend/code-shape · 항목 S5

```
        fun create(                              // ← 인자 7개
            tradingMode: TradingMode,
            marketType: MarketType,
            feeRate: BigDecimal,
            minFee: BigDecimal,
            transactionTaxRate: BigDecimal,
            effectiveFrom: Instant,
            effectiveUntil: Instant? = null
        ): FeePolicy {
```

- 원인: 팩토리 create 가 인자 7개(한계 5)다. 호출부(시험)는 모두 이름 붙은 인자를 써서 위치 결합 위험은 낮다. detekt 기준선 LongParameterList(tools/Tb661de0c.txt:155).
- 생길 수 있는 일: 인자가 더 늘면 호출부가 길어지고 같은 타입 인자(feeRate·minFee·transactionTaxRate: 모두 BigDecimal)의 순서 착오가 컴파일을 통과한다.
- 해결: 요율(feeRate·minFee·transactionTaxRate)을 값 객체 FeeSchedule 로 묶거나 호출부에서 이름 붙은 인자를 강제한다.

### 413위 [low · 확신 high] Execution.create 인자 12개

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/order/Execution.kt:96` · 항목표 backend/code-shape · 항목 S5

```
        fun create(                                  // ← 인자 12개
            order: Order,
            account: Account,
            ticker: String,
            executedQuantity: BigDecimal,
            executedPrice: BigDecimal,
            krwExecutedPrice: BigDecimal,
            externalExecutionId: String,
            executedAt: Instant,
            executionMode: TradingMode = account.tradingMode,
            fee: BigDecimal = BigDecimal.ZERO,
            currency: String = "KRW",
            fxRate: BigDecimal? = null
        ): Execution {
```

- 원인: 팩토리 인자가 12개(한계 5). BigDecimal 인자 5개(executedQuantity·executedPrice·krwExecutedPrice·fee·fxRate)가 같은 타입으로 이어진다. detekt 기준선 LongParameterList(tools/Tb661de0c.txt:163).
- 생길 수 있는 일: 체결 금액 인자 순서 착오(체결가·원화 체결가 뒤바뀜)가 컴파일을 통과해 금액이 조용히 틀어진다.
- 해결: 체결 값(수량·체결가·원화 체결가·수수료·통화·환율)을 FillDetails 데이터 클래스로 묶어 넘긴다.

### 414위 [low · 확신 high] 공개 함수 인자 이름의 약어 qty (executedQty)

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/order/Order.kt:134` · 항목표 backend/naming-terminology · 항목 N6

```
    fun applyExecution(executedQty: BigDecimal, newAvgPrice: BigDecimal, executedFee: BigDecimal) {   // ← executedQty: 약어 qty
```

- 원인: 같은 클래스의 속성은 quantity·filledQuantity 로 풀어 쓰는데 applyExecution 인자는 executedQty 다. qty 는 허용 약어 목록에 없다.
- 생길 수 있는 일: 같은 개념이 quantity/qty 두 철자로 갈려 검색·번역층 비용이 생긴다(N1 용어 조사와 이어짐).
- 해결: executedQuantity 로 바꾼다(S6 수정 때 함께).

### 415위 [low · 확신 high] 정정 유형 MODIFY·CANCEL 을 문자열 리터럴로 반복

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/order/OrderAmendment.kt:57` · 항목표 backend/naming-terminology · 항목 N11

```
        private val VALID_TYPES = setOf("MODIFY", "CANCEL")   // ← 유형 값을 리터럴로
...
            amendmentType = "MODIFY",                            // ← L68
...
            amendmentType = "CANCEL",                            // ← L78
```

- 원인: 유형 값이 enum 이 아닌 String 컬럼이고 VALID_TYPES 집합(L57)·modify(L68)·cancel(L78)·오류 메시지(L93)에 같은 리터럴이 흩어져 있다(CS24).
- 생길 수 있는 일: 오타 하나가 조용히 require 실패나 잘못된 유형 저장이 된다.
- 해결: enum class AmendmentType { MODIFY, CANCEL } 과 @Enumerated(EnumType.STRING) 로 바꾼다.

고친 모양(예시):
```
enum class AmendmentType { MODIFY, CANCEL }
```

### 416위 [low · 확신 high] OrderAmendment.create 인자 7개, modify 인자 6개

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/order/OrderAmendment.kt:83` · 항목표 backend/code-shape · 항목 S5

```
        fun create(                                  // ← 인자 7개
            order: Order,
            amendmentType: String,
            beforeQuantity: BigDecimal? = null,
            afterQuantity: BigDecimal? = null,
            beforeLimitPrice: BigDecimal? = null,
            afterLimitPrice: BigDecimal? = null,
            reason: String? = null
        ): OrderAmendment {
```

- 원인: create(7)·modify(6) 모두 한계 5 초과. detekt 기준선 LongParameterList 2건(tools/Tb661de0c.txt:170-171). before/after 쌍이 BigDecimal? 로 이어진다.
- 생길 수 있는 일: before·after 값을 뒤바꿔 넘겨도 컴파일을 통과해 정정 이력(감사 기록)이 틀린 채 저장된다.
- 해결: 변경 전후를 QuantityChange(before, after)·PriceChange(before, after) 값으로 묶는다.

### 417위 [low · 확신 high] OrderSignal.create 인자 12개

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/order/OrderSignal.kt:103` · 항목표 backend/code-shape · 항목 S5

```
        fun create(                                  // ← 인자 12개
            account: Account,
            ticker: String,
            signalSide: OrderSide,
            quantity: BigDecimal,
            idempotencyKey: String,
            strategyId: Long? = null,
            signalCategory: SignalCategory? = null,
            signalIndicator: SignalIndicator? = null,
            signalThreshold: BigDecimal? = null,
            signalActualValue: BigDecimal? = null,
            conditionSnapshot: String? = null,
            limitPrice: BigDecimal? = null
        ): OrderSignal {
```

- 원인: 팩토리 인자 12개(한계 5), 선택 값 대부분이 nullable 기본값이다. detekt 기준선 LongParameterList(tools/Tb661de0c.txt:175).
- 생길 수 있는 일: signalThreshold·signalActualValue(둘 다 BigDecimal?)처럼 같은 타입이 인접해 순서 착오가 조용히 신호 근거를 뒤바꾼다.
- 해결: 신호 근거(category·indicator·threshold·actualValue·snapshot)를 SignalBasis 값 객체로 묶는다.

### 418위 [low · 확신 high] createWithHolding 인자가 8개다

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/position/Position.kt:119` · 항목표 backend/code-shape · 항목 S5

```
fun createWithHolding(
    account: Account,
    ticker: String,
    marketType: MarketType,
    quantity: BigDecimal,
    avgBuyPrice: BigDecimal,
    lockedQuantity: BigDecimal = BigDecimal.ZERO,
    currentPrice: BigDecimal? = null,
    priceSource: PriceSource = PriceSource.UNKNOWN,
): Position {  // ← 인자 8개
```

- 원인: account·ticker·marketType·quantity·avgBuyPrice·lockedQuantity·currentPrice·priceSource 가 위치 인자로 나열된다. detekt LongParameterList 기준선에도 올라 있다(Position.Companion). 보유 수량·평단 묶음과 시세 묶음이 값 객체로 빠지지 않았다.
- 생길 수 있는 일: BigDecimal 인자 quantity·avgBuyPrice·lockedQuantity 가 같은 타입으로 나란해 호출부에서 순서가 바뀌어도 컴파일이 통과하고 금액이 조용히 틀린다.
- 해결: 보유(quantity·avgBuyPrice·lockedQuantity)와 시세(currentPrice·priceSource)를 값 객체 인자로 묶는다.

### 419위 [low · 확신 high] 함수 밖으로 나가는 인자 이름에 qty 약어가 있다(executedQty·qty)

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/position/Position.kt:150` · 항목표 backend/naming-terminology · 항목 N6

```
fun applyBuy(executedQty: BigDecimal, executedPrice: BigDecimal) {  // ← qty 약어
...
fun lockQuantity(qty: BigDecimal) {  // ← qty 약어
```

- 원인: applyBuy(executedQty), applySell(executedQty), lockQuantity(qty), unlockQuantity(qty) 가 공개 시그니처에서 qty 를 쓴다. 같은 클래스의 필드는 quantity·lockedQuantity·orderableQuantity 로 풀어 쓴다.
- 생길 수 있는 일: 같은 개념이 한 클래스 안에서 두 철자로 갈려 호출부 이름 인자와 필드 이름이 어긋난다.
- 해결: executedQuantity·quantity 로 푼다.

### 420위 [low · 확신 high] 통화 코드 "KRW" 리터럴을 비교·기본값에 반복

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/settlement/Settlement.kt:103` · 항목표 backend/naming-terminology · 항목 N11

```
        val resolvedFxRate = if (normalizedCurrency == "KRW") null else fxRateAtSettlement   // ← 리터럴 비교
        val krwNetPnl = if (normalizedCurrency == "KRW") {                                    // ← L104 같은 리터럴
```

- 원인: L69 기본값, L103·L104 비교에 "KRW" 가 상수 없이 있다.
- 생길 수 있는 일: 기준통화 정책 변경·오타가 조용히 환율 필수 분기(외화 정산)를 잘못 타게 한다.
- 해결: 상수 BASE_CURRENCY 를 Execution 과 공유한다.

### 421위 [low · 확신 high] StrategyLog.create 인자가 6개

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/strategy/StrategyLog.kt:64` · 항목표 backend/code-shape · 항목 S5

```
fun create(
    strategy: Strategy,
    account: Account,
    logLevel: String,   // ← String 인자 3개가 나란함
    message: String,
    context: String? = null,
    loggedAt: Instant = Instant.now()
): StrategyLog {   // ← 인자 6개
```

- 원인: create(strategy, account, logLevel, message, context, loggedAt) 가 위치 결합 인자 6개를 받는다. detekt 기준선(LongParameterList)에도 등록돼 있다.
- 생길 수 있는 일: 같은 타입(String 3개 — logLevel·message·context)이 나란해 호출부에서 순서가 뒤바뀌어도 컴파일이 통과한다.
- 해결: logLevel 을 enum 으로 올리고(naming N11 참고), 나머지를 명령 객체(data class)로 묶는다.

고친 모양(예시):
```
data class NewStrategyLog(val strategyId: Long, val accountId: Long, val level: LogLevel, val message: String, val context: String? = null)
```

### 422위 [low · 확신 high] StrategyPerformanceSnapshot.create 인자가 8개, validateMetrics 가 6개

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/strategy/StrategyPerformanceSnapshot.kt:77` · 항목표 backend/code-shape · 항목 S5

```
fun create(
    strategy: Strategy,
    periodStart: LocalDate,
    periodEnd: LocalDate,
    totalReturn: BigDecimal,
    sharpeRatio: BigDecimal?,   // ← BigDecimal 4개가 나란함
    maxDrawdown: BigDecimal?,
    winRate: BigDecimal?,
    totalTrades: Int
): StrategyPerformanceSnapshot {   // ← 인자 8개 (L100 validateMetrics 는 6개)
```

- 원인: create 가 인자 8개(BigDecimal 4개가 나란함), validateMetrics 가 6개다. 두 곳 모두 detekt 기준선(LongParameterList)에 등록돼 있다.
- 생길 수 있는 일: sharpeRatio·maxDrawdown·winRate 가 모두 BigDecimal? 이라 인자 순서가 바뀌어도 컴파일이 통과하고 금액·비율이 조용히 잘못 저장된다.
- 해결: 성과 지표를 값 객체로 묶어 create·reviseMetrics·validateMetrics 가 같은 객체를 받게 한다(Strategy.updatePerformance 의 4종과 같은 묶음).

고친 모양(예시):
```
data class PerformanceMetrics(val totalReturn: BigDecimal, val sharpeRatio: BigDecimal?, val maxDrawdown: BigDecimal?, val winRate: BigDecimal?, val totalTrades: Int)
```

### 423위 [low · 확신 high] StrategyVersion.create 인자가 6개

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/strategy/StrategyVersion.kt:68` · 항목표 backend/code-shape · 항목 S5

```
fun create(
    strategy: Strategy,
    versionNo: Int,
    rules: String,   // ← String 계열 3개
    createdBy: String? = null,
    changeNote: String? = null,
    backtestRunId: UUID? = null
): StrategyVersion {   // ← 인자 6개
```

- 원인: create(strategy, versionNo, rules, createdBy, changeNote, backtestRunId) 가 6개이고 rules·createdBy·changeNote 가 모두 String 계열이다. detekt 기준선(LongParameterList)에 있다.
- 생길 수 있는 일: 문자열 인자 3개의 순서 착오가 컴파일에서 안 잡혀 rules 에 변경 메모가 들어가는 식의 오염이 생긴다.
- 해결: 생성 인자를 명령 객체로 묶는다.

고친 모양(예시):
```
data class NewStrategyVersion(val strategyId: Long, val versionNumber: Int, val rules: String, val createdBy: String? = null, val changeNote: String? = null, val backtestRunId: UUID? = null)
```

### 424위 [low · 확신 high] 공개 속성 이름 executedQty 가 허용 목록 밖 약어

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/kis/KisExecutionNotice.kt:15` · 항목표 backend/naming-terminology · 항목 N6

```
val executedQty: BigDecimal, // ← qty 약어 (ExecutionResult·Execution 은 executedQuantity)
```

- 원인: 함수 밖으로 나가는 속성에 Qty 약어를 쓴다. 같은 개념을 ExecutionResult 와 Execution 엔티티는 executedQuantity 로 쓴다. 같은 이름이 KisOrderRestClient.FillResult 에도 있다(188행). KIS 필드 이름(CCLD_QTY)을 그대로 받는 것이 아니라 프로젝트가 고른 이름이다.
- 생길 수 있는 일: 같은 개념이 두 철자로 갈려 매퍼·대사 코드에 번역층이 생기고 검색이 한쪽만 걸린다.
- 해결: executedQuantity 로 통일한다(파서·서비스·시험 호출처 포함).

고친 모양(예시):
```
val executedQuantity: BigDecimal,
```

### 425위 [low · 확신 high] 체결통보 채널 ID·구분 코드를 파서에 리터럴로 중복

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/kis/KisExecutionNoticeParser.kt:86` · 항목표 backend/naming-terminology · 항목 N11

```
private fun isExpectedChannel(channelId: String, mode: TradingMode): Boolean =
    (mode == TradingMode.KIS_PAPER && channelId == "H0STCNI9") || // ← KisOrderProperties 기본값과 중복
        (mode == TradingMode.KIS_LIVE && channelId == "H0STCNI0")
```

- 원인: H0STCNI9·H0STCNI0 은 KisOrderProperties 의 기본값(paperExecutionChannel·liveExecutionChannel)과 같은 값을 파서가 다시 하드코딩한다. 매수·매도 코드("02"/"01")와 모드 문자열("paper"/"live")도 상수 없이 흩어져 있다(CS24).
- 생길 수 있는 일: 설정으로 채널을 바꾸면 Manager 는 새 채널을 구독하지만 파서는 옛 채널만 통과시켜 통지를 조용히 버린다(isExpectedChannel 이 null 반환).
- 해결: 채널 ID 는 KisOrderProperties.executionChannel(mode) 를 주입받아 비교하고 매수·매도 코드는 상수로 둔다.

고친 모양(예시):
```
private fun isExpectedChannel(channelId: String, mode: TradingMode) =
    channelId == properties.executionChannel(mode)
```

### 426위 [low · 확신 high] Boolean 인자 resetAttempts 가 is·should 접두 없는 동사구이고 항상 false

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/kis/KisExecutionWebSocketManager.kt:153` · 항목표 backend/naming-terminology · 항목 N3

```
private fun connectMode(mode: String, resetAttempts: Boolean) { // ← 동사구 Boolean, 호출처는 항상 false
    if (resetAttempts) reconnectAttempts[mode] = 0 // ← 도달하지 않는 분기
```

- 원인: private connectMode(mode, resetAttempts: Boolean) 의 두 호출처(150·215행)가 모두 resetAttempts = false 를 넘겨 if (resetAttempts) 분기는 죽은 코드다. 이름도 명령형이라 if 문이 동사구처럼 읽힌다(CS3).
- 생길 수 있는 일: 죽은 분기가 재연결 횟수 초기화 정책을 가린다. 어디서 횟수가 0 으로 돌아가는지(public connectMode 의 직접 대입·handleConnected) 읽기 어렵다.
- 해결: 인자를 지우고 초기화 시점을 한 곳에 둔다. 남기려면 shouldResetAttempts 로 바꾼다.

### 427위 [low · 확신 high] mode == "paper" 비교가 속성 접근 함수 6개에 반복

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/kis/KisOrderProperties.kt:26` · 항목표 backend/naming-terminology · 항목 N11

```
fun restBaseUrl(mode: String) = if (mode == "paper") paperRestBaseUrl else liveRestBaseUrl // ← 같은 비교가 6번
fun tokenUrl(mode: String) = if (mode == "paper") paperTokenUrl else liveTokenUrl
```

- 원인: restBaseUrl·tokenUrl·appKey·appSecret·wsUrl·executionChannel 6개 함수가 같은 문자열 비교를 되풀이하고 그 밖의 값은 전부 live 로 흘린다(CS24). 같은 모드 문자열이 KisOrderRestClient·Parser·Manager 에도 있다.
- 생길 수 있는 일: 모드를 늘리거나 오타가 나면 6곳이 조용히 live 설정으로 폴백한다.
- 해결: 모드를 enum 으로 받고 모드별 설정을 한 객체(KisEndpoint)로 묶어 when 한 곳에서 고른다.

고친 모양(예시):
```
data class KisEndpoint(val restBaseUrl: String, val tokenUrl: String, val appKey: String, val appSecret: String, val wsUrl: String, val executionChannel: String)
fun endpoint(mode: TradingMode): KisEndpoint = when (mode) { ... }
```

### 428위 [low · 확신 high] 모드("paper"/"live")·성공 코드("0")를 문자열로 반복 비교

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/kis/KisOrderRestClient.kt:33` · 항목표 backend/naming-terminology · 항목 N11

```
private fun buyTrId(mode: String) = if (mode == "paper") "VTTC0802U" else "TTTC0802U" // ← 문자열 모드, 그 밖의 값은 전부 live
private fun sellTrId(mode: String) = if (mode == "paper") "VTTC0801U" else "TTTC0801U"
...
if (rtCd(response) != "0") return emptyList() // ← 성공 코드 리터럴
```

- 원인: TradingMode enum 이 있는데 mode: String 으로 "paper" 를 비교하는 곳이 4군데(33~36행), 호출처도 문자열 리터럴(KisLive·KisPaperOrderExecutor 의 "live"/"paper")을 넘긴다. KIS 성공 코드 "0" 도 126·163행과 KisExecutionWebSocketManager:116 에 상수 없이 흩어져 있다(CS24).
- 생길 수 있는 일: "papper" 같은 오타가 조용히 live 쪽으로 분기해 모의 주문이 실계좌 TR_ID 로 나갈 수 있다(else 가 live).
- 해결: mode 를 TradingMode(또는 전용 enum)로 받고 KIS 성공 코드는 상수로 둔다. else 분기에 알 수 없는 모드 예외를 둔다.

고친 모양(예시):
```
private fun buyTrId(mode: TradingMode) = when (mode) {
    TradingMode.KIS_PAPER -> "VTTC0802U"
    TradingMode.KIS_LIVE -> "TTTC0802U"
    else -> error("KIS 주문 모드가 아닙니다: $mode")
}
private const val KIS_SUCCESS_CODE = "0"
```

### 429위 [low · 확신 high] 같은 타입 Long id 세 개가 나란히 옴

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/ExecutionRepositoryCustom.kt:9` · 항목표 backend/layering · 항목 D6

```
fun findByIdAndOrderIdAndAccountId(executionId: Long, orderId: Long, accountId: Long): Optional<Execution>   // ← Long id 3개 나란히
```

- 원인: findByIdAndOrderIdAndAccountId(executionId, orderId, accountId) 의 세 인자가 모두 Long 이라 호출부에서 순서가 바뀌어도 컴파일된다. accountId 는 소유권 필터 역할이라 더 위험하다. ExecutionRepositoryImpl 의 override 는 이 선언을 따를 뿐이라 한 번만 센다.
- 생길 수 있는 일: 인자를 뒤바꿔 부르면 조용히 빈 결과가 나오거나 엉뚱한 소유자 조건으로 조회돼, 체결 조회 오류·접근 제어 오판으로 이어질 수 있다.
- 해결: id 별 값 타입(ExecutionId·OrderId·AccountId)을 두거나, 호출부 이름 인자를 강제한다.

### 430위 [low · 확신 high] 같은 타입 Long id 둘이 나란히 옴

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/OrderRepositoryCustom.kt:11` · 항목표 backend/layering · 항목 D6

```
fun findByIdAndAccountId(orderId: Long, accountId: Long): Optional<Order>   // ← Long id 2개 나란히
```

- 원인: findByIdAndAccountId(orderId: Long, accountId: Long) 가 Long 두 개를 나란히 받는다. 소유권 필터 인자라 바뀌면 조용히 빈 결과다. OrderRepositoryImpl 의 override 는 이 선언을 따르므로 한 번만 센다.
- 생길 수 있는 일: 인자를 뒤바꿔 불러도 컴파일되어 주문 조회가 비거나 다른 계좌 주문과 맞물릴 수 있다.
- 해결: id 별 값 타입을 두거나 호출부에서 이름 인자를 쓴다.

### 431위 [low · 확신 high] «활성 주문(PENDING·PARTIAL)» 필터를 여러 곳이 따로 작성

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/OrderRepositoryImpl.kt:51` · 항목표 data/query · 항목 Q7

```
o.orderStatus.`in`(OrderStatus.PENDING, OrderStatus.PARTIAL),   // ← 같은 «활성» 정의가 파일 4곳 이상에 중복
```

- 원인: 같은 상태 집합이 OrderRepositoryImpl.kt:51·61·74·93, OrderRepository.kt:19·38·56, ExecutionProcessor.kt:98·247, OrderCommandService.kt:244, Order.kt:135·146·162 에 흩어져 있습니다. 활성의 정의가 바뀌면(예: REJECTED·UNKNOWN 추가) 한 군데가 빠집니다.
- 생길 수 있는 일: 상태 모델이 변할 때 일부 쿼리만 낡은 정의로 남아 주문 매칭·구독 해제·취소 판정이 어긋납니다.
- 해결: OrderStatus.ACTIVE 상수(예: companion 의 setOf)를 한 곳에 두고 모두 그것을 씁니다.

### 432위 [low · 확신 high] 락 타임아웃 "3000" 이 단위 없는 문자열 리터럴로 두 번 복제됨

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/PositionExitTriggerRepository.kt:22` · 항목표 backend/naming-terminology · 항목 N9

```
QueryHint(
    name = "jakarta.persistence.lock.timeout",
    value = "3000", // ← 단위는 주석에만, 두 번째 사용처(41행)에는 주석도 없음
```

- 원인: jakarta.persistence.lock.timeout 값이 22-25행과 38-41행에 "3000" 으로 중복되고, 단위(밀리초)는 첫 번째에만 주석(// 3초)으로 있고 두 번째에는 없다. 이름 있는 상수가 아니다.
- 생길 수 있는 일: 한쪽만 고치면 두 락 쿼리의 타임아웃이 갈라지고, 단위 착오(초로 오해)는 조용한 1000배 버그가 된다.
- 해결: 파일 최상위에 const val LOCK_TIMEOUT_MILLIS = "3000" 을 두고 두 힌트가 함께 쓰게 한다.

고친 모양(예시):
```
private const val LOCK_TIMEOUT_MILLIS = "3000"
@QueryHints(QueryHint(name = "jakarta.persistence.lock.timeout", value = LOCK_TIMEOUT_MILLIS))
```

### 433위 [low · 확신 high] executedQty — 허용 목록 밖 약어 qty 를 함수 인자·JPQL 파라미터 이름으로 사용

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/PositionRepository.kt:48` · 항목표 backend/naming-terminology · 항목 N6

```
@Param("executedQty") executedQty: BigDecimal,  // ← qty 약어 (같은 파일 minQuantity 는 전체 철자)
```

- 원인: applySellExecutionAtomically 의 인자와 @Param, JPQL 이 executedQty 를 쓴다. 같은 파일의 minQuantity 와 엔티티 필드 quantity 는 전체 철자라 한 개념이 두 철자가 된다. 호출부 ExecutionProcessor.kt:153 도 fillQty 로 또 다르게 부른다.
- 생길 수 있는 일: 약어가 읽는 사람마다 다르게 펼쳐지고 quantity 검색으로 이 쿼리가 안 잡힌다.
- 해결: executedQuantity 로 바꾼다(@Param·JPQL 포함).

고친 모양(예시):
```
fun applySellExecutionAtomically(@Param("positionId") positionId: Long, @Param("executedQuantity") executedQuantity: BigDecimal): Int
```

### 434위 [low · 확신 high] infrastructure 어댑터가 port 밖 application 패키지를 import (L3 의 결과)

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/SettlementTaxReadRepositoryImpl.kt:3` · 항목표 backend/layering · 항목 L4

```
import com.papertrading.api.application.portfolio.tax.SettlementTaxReadRepository  // ← infrastructure 가 port 밖 application 패키지를 import
import com.papertrading.api.application.portfolio.tax.TaxSettlementAggregate  // ← 같은 사유
```

- 원인: 같은 파일이 application.portfolio.tax.* 두 줄을 import 한다. ArchUnit 은 이 파일에서 2건을 동결 위반으로 센다(T96587ad0.txt 232-233행). 가져오는 것은 유스케이스가 아니라 포트 인터페이스와 결과 레코드라 방향 역전이 뒤집힌 것은 아니다. 원인은 L3 이며, L3 를 고치면 함께 해소된다.
- 생길 수 있는 일: L3 와 같은 원인이라 별도 비용은 없다. L3 가 남아 있는 동안 infrastructure → application 비포트 의존이 새 위반으로 늘어나는 것을 규칙이 동결 목록에 가린다.
- 해결: L3 의 이동으로 해소한다. 별도 작업은 없다.

### 435위 [low · 확신 high] Querydsl 구현이 port 밖 application 타입(TradingJournalFilter)을 import (Custom 인터페이스와 같은 원인)

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/TradingJournalRepositoryImpl.kt:3` · 항목표 backend/layering · 항목 L4

```
import com.papertrading.api.application.portfolio.query.TradingJournalFilter  // ← infrastructure 가 port 밖 application 타입을 import
    private fun buildWhere(filter: TradingJournalFilter): BooleanBuilder {  // ← 필터 타입에 직접 결합
```

- 원인: TradingJournalRepositoryCustom 과 같은 필터 타입을 구현체도 import 하고 buildWhere 에서 필드를 읽는다. ArchUnit 은 이 파일에서 2건을 센다(T96587ad0.txt 236-237행). 원인과 고침은 TradingJournalRepositoryCustom 의 L4 와 같다.
- 생길 수 있는 일: Custom 쪽을 고치면 함께 해소된다. 필터 필드 변경이 infrastructure 컴파일을 깨는 결합이 남는다.
- 해결: TradingJournalRepositoryCustom 의 L4 와 같이 필터를 port 패키지로 옮기고 import 를 바꾼다.

### 436위 [low · 확신 high] runCatching 이 Throwable 전체를 잡음(좁은 예외가 알려진 자리)

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/redis/RedisMarketQuoteAdapter.kt:40` · 항목표 reliability/failure-handling · 항목 E3

```
return runCatching {  // ← Throwable 전체를 잡음
    QuoteSnapshot(... BigDecimal(hash.getValue("price")) ...)
}.getOrNull()
```

- 원인: 본문이 던지는 것은 NumberFormatException·NoSuchElementException 뿐인데 runCatching 으로 Throwable 을 모두 잡는다. 최상위 진입점이 아니다. E1 과 같은 줄의 다른 면이다.
- 생길 수 있는 일: 관련 없는 버그나 Error 계열이 같은 null 에 가려진다.
- 해결: try/catch 로 NumberFormatException·NoSuchElementException 만 잡는다.

### 437위 [low · 확신 high] 원장 목록 page 인자에 최댓값 검증이 없음

`backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/controller/AccountController.kt:136` · 항목표 data/query · 항목 Q6

```
@RequestParam(defaultValue = "0") page: Int,   // ← page 최댓값 검증 없음
...
pageable = PageRequest.of(page, size.coerceIn(1, 100))   // ← size 에만 상한
```

- 원인: size 는 coerceIn(1, 100) 으로 제한하지만 page 는 기본 0 외에 상한이 없다. 위 AccountLedgerRepositoryImpl.kt:37 의 OFFSET 발견과 같은 뿌리(DA22: OFFSET 이면 페이지 상한).
- 생길 수 있는 일: 임의로 큰 page 로 전체 원장을 건너뛰며 읽게 만들 수 있다.
- 해결: page 에 @Max 를 두거나 키셋 커서로 바꾼다.

고친 모양(예시):
```
@RequestParam(defaultValue = "0") @Min(0) @Max(1000) page: Int
```

### 438위 [low · 확신 high] 요청 DTO 에 재시도 횟수·간격·메시지 길이의 상한 검증이 없음

`backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/dto/notification/SlackNotificationDtos.kt:36` · 항목표 reliability/security-config-hygiene · 항목 S8

```
@field:Min(1)
val maxRetries: Int,              // ← 상한(@Max) 없음
@field:Min(0)
val retryBackoffMillis: Long,     // ← 상한 없음
...
@field:NotBlank
val message: String,              // ← 길이 상한(@Size) 없음 (같은 파일 52-55행)
```

- 원인: maxRetries 는 @Min(1), retryBackoffMillis 는 @Min(0) 뿐이고 상한이 없다. 테스트 메시지는 @NotBlank 뿐이라 길이 상한 없이 그대로 Slack 으로 중계된다. @Valid 는 있다.
- 생길 수 있는 일: maxRetries=2147483647 로 바꾸면 폐기된 웹훅에도 30초마다 영구 재시도하고, 거대한 메시지가 그대로 Slack 으로 나간다.
- 해결: @Max·@Size 상한을 추가하고 Service 의 이중 검증도 같은 상수를 쓰게 한다.

고친 모양(예시):
```
@field:Min(1) @field:Max(10) val maxRetries: Int,
@field:Min(0) @field:Max(3_600_000) val retryBackoffMillis: Long,
@field:NotBlank @field:Size(max = 3000) val message: String
```

### 439위 [low · 확신 high] 파일 이름(OrderRequest.kt)과 유일한 최상위 선언(PlaceOrderRequest) 이름 불일치

`backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/dto/order/OrderRequest.kt:13` · 항목표 backend/naming-terminology · 항목 N7

```
data class PlaceOrderRequest(  // ← 파일 이름은 OrderRequest.kt (ktlint standard:filename · detekt MatchingDeclarationName)
```

- 원인: 파일에 최상위 선언이 PlaceOrderRequest 하나뿐인데 파일 이름이 OrderRequest.kt 다. ktlint standard:filename(L1:1)과 detekt MatchingDeclarationName 이 같은 곳을 지적한다(기준선 항목). 클래스 이름 검색이 파일 이름과 어긋난다.
- 생길 수 있는 일: 클래스 이름으로 파일을 찾을 수 없고 기준선 항목이 남아 린트 기준선이 줄지 않는다.
- 해결: 파일 이름을 PlaceOrderRequest.kt 로 바꾼다(이동은 사람 몫).

### 440위 [low · 확신 high] sentiment 허용값이 enum 없이 정규식 문자열로 두 DTO 에 중복됨

`backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/dto/portfolio/TradingJournalCreateRequest.kt:15` · 항목표 backend/naming-terminology · 항목 N11

```
    @field:Pattern(regexp = "^(BULLISH|BEARISH|NEUTRAL)$")   // ← 허용값 리터럴 정규식 (Update 에도 같은 줄)
    @field:Size(max = 20) val sentiment: String? = null   // ← 값 집합이 타입이 아니라 문자열
```

- 원인: BULLISH·BEARISH·NEUTRAL 세 값이 TradingJournalCreateRequest 와 TradingJournalUpdateRequest(L12) 의 @Pattern 정규식에 똑같이 적혀 있습니다. 코드에는 sentiment enum 이 없고(grep 결과 BULLISH 는 두 DTO 와 TradingJournal.kt:18 KDoc 뿐), sentiment 와 journalType 이 String 으로 command·result 까지 흐릅니다. 허용값 규칙이 HTTP 경계에만 있어 다른 진입점은 우회합니다.
- 생길 수 있는 일: 값을 하나 추가·변경할 때 두 곳을 맞춰 고치지 않으면 생성과 수정이 서로 다른 값을 받아들이고, 오타는 컴파일이 못 잡습니다. 배치·다른 컨트롤러 같은 진입점에서는 아무 문자열이나 저장됩니다.
- 해결: domain.enums 에 TradingJournalSentiment enum 을 만들고 DTO·command·result 가 그 타입을 쓴다(Jackson 이 400 으로 거른다). 저장은 EnumType.STRING.

고친 모양(예시):
```
enum class TradingJournalSentiment { BULLISH, BEARISH, NEUTRAL }

data class TradingJournalCreateRequest(
    /* … */
    val sentiment: TradingJournalSentiment? = null
)
```

### 441위 [low · 확신 high] sentiment 허용값 정규식이 Create 요청 DTO 와 중복됨

`backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/dto/portfolio/TradingJournalUpdateRequest.kt:12` · 항목표 backend/naming-terminology · 항목 N11

```
    @field:Pattern(regexp = "^(BULLISH|BEARISH|NEUTRAL)$")   // ← Create 요청과 같은 리터럴 중복
    @field:Size(max = 20) val sentiment: String? = null
```

- 원인: Create 요청 DTO L15 의 같은 정규식을 복사한 줄입니다. 한 결함이며 위 항목과 함께 고칩니다.
- 생길 수 있는 일: 허용값이 갈리면 생성은 되고 수정은 400 이 되는 불일치가 생깁니다.
- 해결: TradingJournalSentiment enum 을 공유한다(Create 요청 항목 참조).

### 442위 [low · 확신 high] 에러 코드·상태 값이 상수 없이 리터럴로 흩어지고 메시지 부분 문자열로 판정함

`backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/exception/GlobalExceptionHandler.kt:22` · 항목표 backend/naming-terminology · 항목 N11

```
ApiErrorResponse(400, "INVALID_BUSINESS_DATE", ex.message ?: "날짜 형식이 올바르지 않습니다.")   // ← 22줄: 모든 타입 불일치를 날짜 코드로 고정, 400 은 같은 줄 HttpStatus 와 중복
...
(ex.message ?: "").contains("계좌") -> "ACCOUNT_NOT_FOUND"   // ← 49줄: 코드를 한글 메시지 부분 문자열로 판정
...
"ACCOUNT_NOT_FOUND" -> "계좌를 찾을 수 없습니다."   // ← 66줄: CommonApiExceptions.kt:24 의 같은 코드가 상수 없이 중복
```

- 원인: 같은 에러 코드(ACCOUNT_NOT_FOUND·ORDER_NOT_FOUND 등)가 CommonApiExceptions.kt:24·27 의 예외 정의와 GlobalExceptionHandler 의 when 두 곳(49-52줄·66-71줄)에 리터럴로 중복된다. handleNoSuchElement 는 한글 메시지에 contains("계좌") 가 들어 있는지로 코드를 정한다. 상태 400·404·500 도 HttpStatus 와 같은 줄에서 숫자로 반복된다(detekt MagicNumber 3건과 일치). handleTypeMismatch 는 모든 타입 불일치를 INVALID_BUSINESS_DATE, handleValidation 은 모든 검증 실패를 INVALID_PERCENT_VALUE 로 고정한다.
- 생길 수 있는 일: 오타나 메시지 문구 변경이 조용히 NOT_FOUND 로 떨어져 클라이언트가 코드로 분기하는 계약이 깨지고, 날짜가 아닌 타입 불일치나 비율이 아닌 검증 실패도 잘못된 코드로 응답된다.
- 해결: 에러 코드를 enum(ErrorCode)이나 const 로 한 곳에 두고 예외와 핸들러가 같은 상수를 쓰게 한다. 상태는 ex.status.value() 나 HttpStatus 하나만 쓴다. NoSuchElementException 을 메시지로 판정하지 말고 전용 예외(AccountNotFoundException 등)로 던진다.

고친 모양(예시):
```
enum class ErrorCode(val code: String, val defaultMessage: String) {
    ACCOUNT_NOT_FOUND("ACCOUNT_NOT_FOUND", "계좌를 찾을 수 없습니다."),
}
// 예외와 핸들러가 ErrorCode 를 공유
```

### 443위 [low · 확신 high] 트리거 생성 시험 하나에 단언 10개

`backend/trading-api/src/test/kotlin/com/papertrading/api/application/position/PositionExitTriggerCommandServiceTest.kt:50` · 항목표 backend/tdd-purity · 항목 T12

```
assertEquals(100L, result.id)  // ← 단언 10개가 연달아 (50~59행)
...
assertEquals(TriggerState.ARMED, result.state)
```

- 원인: fixed price stop loss trigger… 시험이 result 의 id·positionId·accountId·ticker·triggerType·triggerPercent·triggerPrice·priceBasisPolicy·exitRatioPercent·state 10개를 한 번에 단언한다.
- 생길 수 있는 일: 첫 실패가 나머지를 가리고 기본 청산 비율 100% 같은 핵심 정책이 이름과 단언 더미에 묻힌다.
- 해결: 핵심 행동(기본 청산 비율 100%·초기 상태 ARMED)만 단언하고 나머지 필드 매핑은 결과 객체 비교 한 줄로 묶는다.

고친 모양(예시):
```
assertEquals(expectedResult, result)  // data class 동등성 한 줄
```

### 444위 [low · 확신 high] BigDecimal 단언이 assertEquals 로 scale 까지 비교

`backend/trading-api/src/test/kotlin/com/papertrading/api/domain/entity/feepolicy/FeePolicyTest.kt:25` · 항목표 backend/tdd-purity · 항목 T8

```
        assertEquals(BigDecimal("0.001500"), policy.feeRate)   // ← equals: scale 까지 비교
        assertEquals(BigDecimal("0.0000"), policy.minFee)       // ← L26
```

- 원인: feeRate·minFee 를 assertEquals(BigDecimal(..), x) 로 비교한다(scale 6·4 까지 같아야 통과). 같은 파일의 다른 단언은 compareTo 를 쓴다.
- 생길 수 있는 일: scale 정규화가 바뀌면 값이 같아도 빨강이 되고, 반대로 scale 만 맞으면 값 비교의 의미가 흐려진다. 현재는 통과하므로 med 기본값을 low 로 낮춤.
- 해결: assertEquals(0, expected.compareTo(actual)) 또는 AssertJ isEqualByComparingTo 로 통일한다.

### 445위 [low · 확신 high] BigDecimal 단언이 assertEquals 로 scale 까지 비교 (4곳)

`backend/trading-api/src/test/kotlin/com/papertrading/api/domain/entity/order/OrderTest.kt:115` · 항목표 backend/tdd-purity · 항목 T8

```
        assertEquals(BigDecimal("10"), order.filledQuantity)      // ← scale 까지 비교
        assertEquals(BigDecimal("70000"), order.avgFilledPrice)   // ← L116
```

- 원인: filledQuantity·avgFilledPrice 를 assertEquals(BigDecimal(..), x) 로 비교한다(L115·116·131·142). 운영에서는 DB 에서 읽은 값이 scale 8 이라 이 비교 방식이 맞지 않는다.
- 생길 수 있는 일: scale 만 달라도 빨강이 되어 리팩터링 시 가짜 실패를 낸다.
- 해결: compareTo 기반 단언 헬퍼(assertZero 와 같은 방식)로 통일한다.

### 446위 [low · 확신 high] 한 시험이 대상 호출 여러 번에 각자 단언을 붙임

`backend/trading-api/src/test/kotlin/com/papertrading/api/domain/entity/strategy/StrategyDomainEntitiesTest.kt:102` · 항목표 backend/tdd-purity · 항목 T12

```
fun strategy_cancelled_cannot_be_reviewed_approved_or_activated() {
    ...
    assertThrows(IllegalStateException::class.java) { strategy.requestReview() }   // ← 호출 1
    assertThrows(IllegalStateException::class.java) { strategy.approve() }   // ← 호출 2
    assertThrows(IllegalStateException::class.java) { strategy.activate() }   // ← 호출 3
```

- 원인: strategy_cancelled_cannot_be_reviewed_approved_or_activated 는 서로 다른 세 전이를 각각 assertThrows 로 단언한다. strategy_request_review_allows_draft_and_rejected_only(L149)는 DRAFT·APPROVED·REJECTED 세 상태 시나리오를, strategy_update_performance_validates_ranges(L195)는 성공 1 + 실패 3 호출을 한 시험에 담았다.
- 생길 수 있는 일: 첫 실패가 나머지를 가려 어떤 전이가 깨졌는지 한 번에 안 보이고 이름이 행동 하나를 못 담는다.
- 해결: 전이마다 시험을 나누거나 @ParameterizedTest 로 상태·전이 조합을 데이터로 돌린다.

고친 모양(예시):
```
@ParameterizedTest @MethodSource("cancelledTransitions")
fun `취소된 전략에 전이를 요청하면 IllegalStateException 을 던진다`(transition: (Strategy) -> Unit) { ... }
```

### 447위 [low · 확신 high] 시험 하나가 대상 호출 5번에 각자 단언을 달음

`backend/trading-api/src/test/kotlin/com/papertrading/api/presentation/controller/TradingJournalControllerIntegrationTest.kt:43` · 항목표 backend/tdd-purity · 항목 T12

```
    @Test
    fun `create list detail update 흐름이 동작한다`() {
...
        }.andExpect {
            status { isCreated() }   // ← 호출 1 단언 (일지 생성)
...
        }.andExpect {
            status { isOk() }   // ← 호출 2 단언 (목록)
...
            status { isOk() }   // ← 호출 3 단언 (상세)
...
            status { isOk() }   // ← 호출 4 단언 (수정)
```

- 원인: 한 @Test 안에서 계좌 생성·일지 생성·목록·상세·수정을 차례로 호출하며 호출마다 status·jsonPath 단언을 답니다(L45-106). 첫 실패가 나머지를 가립니다. detekt 도 이 메서드를 LongMethod 로 기준선에 올렸습니다(tools/Tb661de0c.txt:404).
- 생길 수 있는 일: 생성이 깨지면 목록·상세·수정 시험이 전부 실행조차 안 되어 어디까지가 정상인지 알 수 없고, 이름 하나로 네 행동을 설명할 수 없습니다.
- 해결: 생성·목록·상세·수정을 각각 별도 시험으로 나누고 공통 준비는 createAccount·createJournal 헬퍼로 한다(이미 있음).

### 448위 [low · 확신 high] 시험 이름이 «흐름이 동작한다» 뿐이라 상황·행동·기대가 없음

`backend/trading-api/src/test/kotlin/com/papertrading/api/presentation/controller/TradingJournalControllerIntegrationTest.kt:44` · 항목표 backend/tdd-purity · 항목 T6

```
    fun `create list detail update 흐름이 동작한다`() {   // ← 기대가 «동작한다» — 어떤 정책이 깨졌는지 말하지 못함
```

- 원인: `create list detail update 흐름이 동작한다` 는 대상 동사 4개를 나열하고 기대는 «동작한다» 뿐이라 정상 동작·works 와 같은 모양입니다. 빨간 시험이 어떤 정책이 깨졌는지 말하지 못합니다. T12 의 한 시험 다중 행동과 같은 줄입니다.
- 생길 수 있는 일: 실패했을 때 생성·목록·상세·수정 중 무엇이 깨졌는지 이름으로 알 수 없어 진단 비용이 늘어납니다.
- 해결: 행동별로 시험을 나누고 `일지를 만들면 201과 요청 계좌 id를 돌려준다`처럼 정책 문장으로 이름 붙인다.

### 449위 [low · 확신 med] 한 번의 체결을 execution 과 fill 두 용어로 부름

`(repo)` · 항목표 backend/naming-terminology · 항목 N1

```
application/order/ExecutionProcessor.kt:58:        fillPrice: BigDecimal,   // ← 처리 인자는 fill
application/order/ExecutionProcessor.kt:128:                executedPrice = fillPrice,   // ← 엔티티 필드는 executed 로 번역
infrastructure/kis/KisOrderRestClient.kt:96:    fun inquireFills(order: Order, mode: String): List<FillResult> {   // ← 외부 조회도 fill
application/order/LocalMatchingEngine.kt:46:    fun determineFillPrice(order: Order, quote: QuoteSnapshot): BigDecimal? {   // ← 체결 단가 결정도 fill
```

- 원인: 체결 한 건의 단가·수량이 엔티티·DB·KIS 통보에서는 executedPrice·executedQuantity·Execution 이고, 처리 로직 인자에서는 fillPrice·fillQty·determineFillPrice·FillResult·inquireFills 다. ExecutionProcessor 에서는 fillPrice 를 executedPrice 필드에 대입하는 번역이 일어난다. 주문 단위 누계(filledQuantity·avgFilledPrice·OrderStatus.FILLED)에서 filled 를 쓰는 것은 의미가 다르므로 허용할 수 있으나, 체결 한 건을 가리키는 쪽이 둘로 갈린다. executedAt 은 레포 전체에서 하나(Execution.executed_at·parseExecutedAt)로 통일돼 있어 시각 이름은 문제없다.
- 생길 수 있는 일: 새로 들어온 사람이 Execution 레코드와 fill 결과가 같은 것인지 다른 것인지 매번 코드를 열어 확인해야 한다. 두 종류의 체결 값이 있다고 오해하면 정산 대사 코드에 잘못된 필드를 꽂을 수 있다.
- 해결: 체결 한 건은 executed·Execution 으로, 주문 누계·상태만 filled·FILLED 로 나눠 용어집에 한 줄로 못 박는다. 처리 인자 fillPrice·fillQty 를 executedPrice·executedQuantity 로, FillResult 는 ExecutionLookupResult 같이 맞춘다.

고친 모양(예시):
```
fun fillLocal(orderId: Long, executedPrice: BigDecimal, executedQuantity: BigDecimal)
```

### 450위 [low · 확신 med] 청산 트리거를 PositionExitTrigger·PositionTrigger·Trigger 로 섞고, 임계가를 triggerPrice·thresholdPrice·entryPrice 로 부름

`(repo)` · 항목표 backend/naming-terminology · 항목 N1

```
application/position/PositionExitTriggerCommandService.kt:29:    fun createPositionTrigger(command: CreatePositionExitTriggerCommand): PositionExitTriggerResult {   // ← 메서드는 PositionTrigger, 타입은 PositionExitTrigger
application/position/PositionExitTriggerCommandService.kt:136:    private fun thresholdPrice(avgBuyPrice: BigDecimal, percent: BigDecimal, triggerType: TriggerType): BigDecimal {   // ← 같은 값을 threshold 로 부름
application/position/PositionExitTriggerEvaluator.kt:58:                    TriggerType.STOP_LOSS -> stopLossThreshold(position.avgBuyPrice, percent)   // ← 인자는 avgBuyPrice
application/position/PositionExitTriggerEvaluator.kt:64:    private fun stopLossThreshold(entryPrice: BigDecimal, percent: BigDecimal): BigDecimal =   // ← 같은 값을 entryPrice 로 받음
```

- 원인: 클래스·명령·DTO·테이블은 PositionExitTrigger(position_exit_triggers)로 통일돼 있다. 그러나 같은 서비스의 공개 메서드는 createPositionTrigger·updatePositionTrigger·cancelPositionTrigger 이고 PositionExitTriggerCommandService 의 비공개 메서드는 findTriggerForUpdate·exitTriggerNotFound 다. 가격 쪽도 트리거 발동 가격이 triggerPrice·thresholdPrice(서비스)·stopLossThreshold(평가자)로 갈리고, 평가자 안에서 평균매수가를 매개변수 entryPrice 로 받는다(호출 쪽 이름은 position.avgBuyPrice). 소스 열람은 grep 줄 단위다.
- 생길 수 있는 일: 트리거는 자동 주문을 만드는 개념이라 로그·알림·API 에서 이름이 갈리면 어느 객체를 가리키는지 헷갈리고 검색(grep) 누락이 생긴다.
- 해결: 공개 메서드를 createExitTrigger·updateExitTrigger·cancelExitTrigger 로 맞추고, 발동 가격은 triggerPrice 한 철자로, 기준 평균매수가는 avgBuyPrice 로 통일한다.

고친 모양(예시):
```
fun createExitTrigger(command: CreatePositionExitTriggerCommand): PositionExitTriggerResult
private fun stopLossTriggerPrice(avgBuyPrice: BigDecimal, percent: BigDecimal): BigDecimal =
```

### 451위 [low · 확신 med] 운영 로그 형식(JSON 구조 로그)·로그 수준·느린 쿼리 로그 설정이 없음

`(repo):20` · 항목표 reliability/observability · 항목 O11

```
# application.yaml
    show-sql: false              # ← 바인딩 로그는 꺼져 있어 이 항목은 통과
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true         # ← logging.structured.format.console · logging.level · 느린 쿼리 설정 · logback xml 이 레포 어디에도 없음
```

- 원인: src/main/resources 에는 application.yaml 과 db/ 만 있고 logback*.xml 이 없다. application.yaml 에는 logging.* 블록(structured.format.console·level)도, 느린 쿼리 설정(hibernate LOG_QUERIES_SLOWER_THAN_MS)도, management.* 블록도 없다(grep 적중은 hibernate·show-sql·format_sql 뿐). docker-compose.yml 에서도 LOGGING·SPRING_PROFILES·SHOW_SQL·MANAGEMENT·JAVA_OPTS 환경변수 0건이다. SQL 바인딩 값 로그는 켜져 있지 않아 그 항목은 통과다.
- 생길 수 있는 일: 수집기가 로그를 필드(수준·주문 id·종목)로 거르지 못하고 평문 줄로만 본다. 운영 로그 수준이 기본값(INFO)에 맡겨지고 느린 쿼리를 알려 주는 흔적도 없어, 매칭·정산 지연이 생겨도 DB 때문인지 알 수 없다.
- 해결: application.yaml 에 JSON 구조 로그(Spring Boot 3.4 이상은 logging.structured.format.console, 그 미만은 logstash-logback-encoder), 운영 로그 수준(logging.level.root·패키지별), 느린 쿼리 임계(hibernate.session.events.log.LOG_QUERIES_SLOWER_THAN_MS)를 명시한다. org.hibernate.orm.jdbc.bind 는 TRACE 로 켜지 않는다(바인딩 값에 계좌 번호·금액이 남음).

고친 모양(예시):
```
logging:
  structured:
    format:
      console: ecs
  level:
    root: INFO
spring:
  jpa:
    properties:
      hibernate:
        session.events.log.LOG_QUERIES_SLOWER_THAN_MS: 500
```

### 452위 [low · 확신 med] 포트 인자 이름 trId 가 허용 목록 밖 약어

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/account/port/KisAccountBalancePort.kt:7` · 항목표 backend/naming-terminology · 항목 N6

```
fun fetchBalance(accountId: Long, trId: String): KisBalanceSnapshot // ← trId 약어
```

- 원인: fetchBalance(accountId, trId: String) 의 trId 는 KIS 의 tr_id(거래 ID)를 줄인 말로 허용 약어 목록에 없다. Kotlin 인자 이름이라 외부 계약 면제 대상이 아니다.
- 생길 수 있는 일: KIS 문서를 모르는 독자는 trId 가 무엇인지 알 수 없다. 더구나 이 값은 포트 호출자(유스케이스)가 KIS 프로토콜 코드를 알아야 만들어 낸다.
- 해결: 포트는 KisAccountMode 를 받고 tr_id 는 어댑터 안에서 정하게 한다(N11 과 함께 해결).

고친 모양(예시):
```
fun fetchBalance(accountId: Long, mode: KisAccountMode): KisBalanceSnapshot
```

### 453위 [low · 확신 med] 호가 속성 이름이 허용 목록 밖 약어 (askp1·bidp1)

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/common/result/QuoteSnapshot.kt:11` · 항목표 backend/naming-terminology · 항목 N6

```
    val askp1: BigDecimal,   // ← 약어. bestAskPrice 처럼 풀어 쓰지 않음
    val bidp1: BigDecimal,   // ← 약어
```

- 원인: QuoteSnapshot 은 Kotlin 속성이라 KIS 응답의 JSON 필드 이름(외부 계약) 면제가 적용되지 않는다. askp1·bidp1 은 «매도 1호가·매수 1호가»를 뜻하는 약어로, 허용 목록(id·kis·min·max 등)에 없고 함수 밖으로 나가는 속성이다.
- 생길 수 있는 일: 읽는 사람마다 askp1 을 다르게 펼쳐 호가 방향(매도·매수)을 바꿔 읽을 수 있고, 매칭 엔진에서 매수·매도 호가를 뒤바꿔 쓰는 버그를 이름이 막아 주지 못한다.
- 해결: Kotlin 속성은 bestAskPrice·bestBidPrice 처럼 풀어 쓰고, JSON 키(askp1·bidp1)와의 대응은 파싱 지점에서만 둔다.

### 454위 [low · 확신 med] Boolean 속성에 is 접두가 없다

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/notification/command/UpdateSlackNotificationConfigCommand.kt:6` · 항목표 backend/naming-terminology · 항목 N3

```
data class UpdateSlackNotificationConfigCommand(
    val enabled: Boolean,   // ← is·has 접두 없는 Boolean
```

- 원인: enabled 가 is·has·can 접두 없이 명사처럼 읽힌다. 이 명령 타입은 application 내부 타입이라 외부 설정 키 제약이 없다.
- 생길 수 있는 일: if (request.enabled) 가 값처럼 읽혀 의미가 묻히고, 같은 개념이 타입마다 enabled·webhookConfigured 로 갈린다.
- 해결: isEnabled 로 바꾸고 presentation DTO 와의 JSON 매핑은 DTO 쪽에서 맞춘다.

고친 모양(예시):
```
val isEnabled: Boolean,
```

### 455위 [low · 확신 med] Boolean 속성 두 개에 is 접두가 없다

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/notification/port/SlackNotificationRuntimeConfig.kt:7` · 항목표 backend/naming-terminology · 항목 N3

```
data class SlackNotificationRuntimeConfig(
    val enabled: Boolean,             // ← 접두 없음
    val webhookConfigured: Boolean,   // ← 접두 없음
```

- 원인: enabled 와 webhookConfigured 가 접두 없이 쓰인다. 같은 이름이 Command·Result 에도 복제되어 있다.
- 생길 수 있는 일: 플래그 이름이 상태(과거분사)와 명사를 섞어 읽는 사람이 Boolean 인지 값인지 매번 타입을 봐야 한다.
- 해결: isEnabled·isWebhookConfigured 로 바꾸고 Command·Result 도 같이 바꾼다.

고친 모양(예시):
```
val isEnabled: Boolean,
val isWebhookConfigured: Boolean,
```

### 456위 [low · 확신 med] Boolean 속성 두 개에 is 접두가 없다

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/notification/result/SlackNotificationConfigResult.kt:8` · 항목표 backend/naming-terminology · 항목 N3

```
data class SlackNotificationConfigResult(
    val enabled: Boolean,             // ← 접두 없음
    val webhookConfigured: Boolean,   // ← 접두 없음
```

- 원인: Result 도 enabled·webhookConfigured 를 접두 없이 쓴다. 필드 7개가 RuntimeConfig 와 동일하다.
- 생길 수 있는 일: Command·RuntimeConfig 와 같은 이유로 의미가 흐려지고 한 곳만 고치면 세 타입의 이름이 어긋난다.
- 해결: 세 타입을 같이 isEnabled·isWebhookConfigured 로 바꾼다.

고친 모양(예시):
```
val isEnabled: Boolean,
val isWebhookConfigured: Boolean,
```

### 457위 [low · 확신 med] 기본 인자가 시각을 직접 읽는다

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/notification/SlackNotificationEventPublisher.kt:14` · 항목표 backend/code-shape · 항목 F4

```
fun publishOrderCreated(accountId: Long, orderId: Long, message: String, occurredAt: Instant = Instant.now()) {   // ← 기본 인자가 시각을 직접 읽음
```

- 원인: publishOrderCreated·publishOrderCanceled·publishExecutionFailed·publishOrderError·publishRiskAlert 5개가 occurredAt 의 기본값을 Instant.now() 로 둔다. 기본값이라 호출처가 값을 넘기면 시험이 고정할 수 있어 위험은 낮다. publishExecutionFilled 만 기본값이 없어 규칙도 갈린다.
- 생길 수 있는 일: 기본값을 쓰는 호출처의 occurredAt 은 시험으로 고정되지 않고, 같은 클래스 안에서 발행 시각 규칙이 두 가지가 된다.
- 해결: 기본값을 없애고 호출처가 Clock 으로 만든 시각을 넘기게 한다. publishExecutionFilled 와 규칙을 맞춘다.

고친 모양(예시):
```
fun publishOrderCreated(accountId: Long, orderId: Long, message: String, occurredAt: Instant) {
```

### 458위 [low · 확신 med] 같은 타입 id strategyId·signalId 가 나란히 옴 (PlaceOrderCommand)

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/command/PlaceOrderCommand.kt:21` · 항목표 backend/layering · 항목 D6

```
    val strategyId: Long? = null,          // ← 같은 타입 id 가 나란히
    val signalId: Long? = null,            // ←
```

- 원인: 둘 다 Long? 기본값 null 이라 위치 인자로 넘기면 뒤바뀐다.
- 생길 수 있는 일: Order.create 로 그대로 넘어가 전략·신호 추적이 틀어진다.
- 해결: StrategyId·SignalId 값 클래스를 쓴다.

### 459위 [low · 확신 med] KIS_PAPER 취소 실패를 WARN 로그로만 남기고 성공처럼 반환

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/KisPaperOrderExecutor.kt:47` · 항목표 reliability/failure-handling · 항목 E1

```
runCatching {
    kisOrderRestClient.cancelOrder(order, "paper")
...
}.onFailure {
    log.warn { "KIS_PAPER 주문 취소 실패: orderId=$orderId, reason=${it.message}" }   // ← 실패를 삼키고 Unit 반환
}
```

- 원인: KisLiveOrderExecutor.cancel 과 같은 모양이다. 실패가 Unit 정상 반환으로 읽혀 호출자가 알 수 없다. 호출처는 현재 없다.
- 생길 수 있는 일: 모의 계좌 기준으로 취소 실패가 성공처럼 처리돼 로컬과 KIS 상태가 어긋날 수 있다.
- 해결: 취소 결과를 호출자가 읽을 수 있는 값으로 돌려준다.

고친 모양(예시):
```
fun cancel(order: Order): Boolean = runCatching { kisOrderRestClient.cancelOrder(order, "paper") }.isSuccess
```

### 460위 [low · 확신 med] pollFills() 가 폴링을 약속하지만 아무 일도 하지 않음

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/KisPaperOrderExecutor.kt:61` · 항목표 backend/naming-terminology · 항목 N2

```
// 웹소켓 체결 통지로 전환되어 폴링은 사용하지 않는다.
fun pollFills() = Unit // ← 이름은 폴링인데 no-op
```

- 원인: 이름은 체결 폴링을 뜻하지만 본문이 Unit 이다. 주석은 웹소켓 통지로 바뀌어 폴링을 쓰지 않는다고 하는데, infrastructure.scheduler.KisPaperPollingScheduler 가 이 메서드를 계속 위임 호출한다.
- 생길 수 있는 일: 읽는 사람이 체결 폴링이 돌고 있다고 오해해 체결 누락 원인을 엉뚱한 곳에서 찾는다.
- 해결: 스케줄러와 no-op 메서드를 함께 지우거나, 남길 이유가 있으면 이름에 비활성을 드러낸다.

### 461위 [low · 확신 med] 잔여 수량 식이 Order 에 없어 여러 곳에 복사됨

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/LocalMatchingEngine.kt:36` · 항목표 backend/layering · 항목 D3

```
val remainQty = order.quantity.subtract(order.filledQuantity) // ← 같은 식이 OrderCommandService:256·274, KisOrderRestClient:79 에도 있음
executionProcessor.fillLocal(order.id!!, fillPrice, remainQty)
```

- 원인: quantity − filledQuantity 로 잔여 수량을 구하는 같은 식이 LocalMatchingEngine:36, OrderCommandService:256·274, KisOrderRestClient:79 에 각각 있고 Order 에는 그 메서드가 없다(DD10·DD11). 체결가 결정 규칙(determineFillPrice)도 Order 의 필드 3개를 서비스가 읽어 정한다.
- 생길 수 있는 일: 잔여 수량 정의(예: 정정 주문 추가)가 바뀌면 호출처 4곳을 모두 고쳐야 하고 하나가 빠지면 체결·취소 수량이 어긋난다.
- 해결: Order 에 remainingQuantity() 를 두고 네 호출처가 그것을 쓰게 한다.

고친 모양(예시):
```
// Order.kt
fun remainingQuantity(): BigDecimal = quantity.subtract(filledQuantity)
// LocalMatchingEngine
executionProcessor.fillLocal(orderId, fillPrice, order.remainingQuantity())
```

### 462위 [low · 확신 med] 자동 청산 트리거 개념의 입력 타입이 주문 서비스 파일 안에 있고 이름은 position 쪽 용어

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/OrderCommandService.kt:30` · 항목표 backend/naming-terminology · 항목 N12

```
data class AutoExitTriggerAuditInput( // ← 트리거 개념인데 주문 서비스 파일 최상위에 선언
    val triggerId: Long,
    val triggerVersion: Long,
)
```

- 원인: AutoExitTriggerAuditInput 은 자동 청산 트리거(position 컨텍스트) 개념인데 OrderCommandService.kt 최상위에 선언되어 있고, 다른 명령 타입(PlaceOrderCommand·CancelOrderCommand)이 있는 order/command 패키지에 없다. 사용처는 application.position 의 PositionExitTriggerOrchestrator·AutoExitOrderPlacementService 다.
- 생길 수 있는 일: position 컨텍스트가 order 서비스 파일의 내부 타입에 기대어, 서비스 파일을 옮기거나 쪼개면 의존이 깨진다.
- 해결: order/command 패키지(또는 공유 계약 위치)로 옮겨 별도 파일로 둔다.

### 463위 [low · 확신 med] 주문 접수 한 트랜잭션이 Order·Account(홀드)·Position(수량 락)을 함께 바꿈

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/OrderCommandService.kt:49` · 항목표 data/transaction-boundary · 항목 X7

```
@Transactional
fun placeOrder(accountId: Long, command: PlaceOrderCommand): Order {
    ...
    lockSellQuantityIfNeeded(accountId, command)   // ← Position (:60)
    val order = saveOrder(account, command, lockedAmount)   // ← Order (:62)
    recordBuyLockIfNeeded(command, account, orderId, lockedAmount)   // ← Account 홀드 (:65)
```

- 원인: placeOrder 는 주문 저장, 매수 예수금 홀드(recordBuyLock), 매도 포지션 수량 잠금(lockQuantity)을 한 커밋에 합니다. PTF6 가 예수금 홀드를 요청 경로 트랜잭션 안에서 즉시 하라고 정하므로 이 모양은 레포 규칙이 요구합니다. 렌즈 기본(med)보다 낮은 low 로 두고 ruleConflicts 에 남깁니다.
- 생길 수 있는 일: 요청 하나가 계좌·포지션 락을 함께 잡아 락 범위가 넓지만, 홀드 누락으로 초과 주문이 나는 위험이 더 큽니다.
- 해결: 변경 없이 두되, PTF6 에 근거한 의도임을 주석으로 남깁니다.

### 464위 [low · 확신 med] 주문 서비스가 자동 청산 트리거 멱등 키 규칙과 계좌·포지션 잠금 해제까지 가짐

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/OrderCommandService.kt:189` · 항목표 backend/code-shape · 항목 S3

```
val triggerAuditKey = triggerAuditInputs
    .sortedWith(compareBy<AutoExitTriggerAuditInput> { it.triggerId }.thenBy { it.triggerVersion })
    .joinToString(",") { "${it.triggerId}@${it.triggerVersion}" } // ← 트리거 감사 키 규칙이 주문 서비스 안에 있음
val idempotencyKey = "$orderGroupId:$triggerAuditKey"
```

- 원인: public 메서드의 존재 이유를 적으면 주문 접수·취소(Order) 외에 자동 청산 트리거의 감사 키 규칙(triggerId@version 정렬·결합)과 취소 시 계좌 원장·포지션 수량 해제 계산이 섞여 있다(CS18). 트리거 규칙은 호출 쪽(position 컨텍스트)의 지식이다. 계좌·포지션 조율은 응용 서비스의 흔한 역할이라 신뢰도는 중간이다.
- 생길 수 있는 일: 자동 청산 트리거 감사 규칙이 바뀌면 주문 서비스를 고쳐야 하고, 취소 경로 변경이 계좌·포지션 규칙까지 건드린다.
- 해결: 트리거 감사 키 조립은 position 컨텍스트(AutoExitOrderPlacementService)로 옮기고 주문 서비스는 완성된 멱등 키만 받는다. 계좌·포지션 잠금 해제는 각 애그리거트의 메서드가 책임지게 한다.

### 465위 [low · 확신 med] 같은 타입 id 가 나란한 위치 인자(accountId, orderId)

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/OrderCommandService.kt:239` · 항목표 backend/layering · 항목 D6

```
fun cancelOrder(accountId: Long, orderId: Long, command: CancelOrderCommand): Order { // ← Long id 둘이 나란히
...
cancelOrder(accountId, orderId, CancelOrderCommand("시세 없음 — IOC/FOK 즉시 취소")) // ← 위치 인자 호출
```

- 원인: cancelOrder(accountId: Long, orderId: Long, ...)·cancelIfUnfillableIocFok(accountId: Long, orderId: Long, ...) 가 Long id 둘을 나란히 받고 호출부(159행 등)는 위치 인자로 부른다(DD13).
- 생길 수 있는 일: 호출부에서 두 id 가 뒤바뀌어도 컴파일이 통과하고, 다른 계좌의 주문을 조회·취소하려는 조용한 오류가 된다.
- 해결: 명령 객체에 accountId·orderId 를 담거나 값 객체(AccountId·OrderId)로 감싼다.

고친 모양(예시):
```
data class CancelOrderCommand(val accountId: Long, val orderId: Long, val reason: String)
```

### 466위 [low · 확신 med] 같은 타입 id 3개가 나란한 위치 인자

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/OrderQueryService.kt:31` · 항목표 backend/layering · 항목 D6

```
fun getExecution(accountId: Long, orderId: Long, executionId: Long): ExecutionResult { // ← Long id 셋이 나란히
```

- 원인: getExecution(accountId: Long, orderId: Long, executionId: Long) 이 Long id 를 셋 나란히 받는다. getOrder·ensureOrderOwnership 도 accountId·orderId 를 나란히 받는다(DD13).
- 생길 수 있는 일: 인자 순서를 바꿔도 컴파일되고 소유권 검사가 엉뚱한 id 로 돌아 다른 계좌 데이터가 조회되거나 항상 not found 가 된다.
- 해결: listExecutions 처럼 쿼리 객체(ExecutionQuery 에 executionId 추가)로 받거나 값 객체로 감싼다.

고친 모양(예시):
```
data class ExecutionKey(val accountId: Long, val orderId: Long, val executionId: Long)
```

### 467위 [low · 확신 med] private 생성자 data class 의 copy() 가 열려 있어 scale 정규화 팩토리를 우회할 수 있음

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/portfolio/result/TaxComputationResult.kt:6` · 항목표 backend/code-shape · 항목 F1

```
data class TaxComputationResult private constructor(   // ← private 생성자인데 data class 라 copy() 가 public
            return TaxComputationResult(
                totalRealizedPnl.setScale(4, RoundingMode.HALF_UP),   // ← 정규화는 of() 안에서만 일어남
```

- 원인: of() 팩토리가 BigDecimal 3개를 setScale(4, HALF_UP) 로 정규화하지만, data class 는 private 생성자여도 copy() 가 public 이라 정규화 없이 새 인스턴스를 만들 수 있다. 컴파일러가 이 경고를 이미 냈다(매니페스트 ArchUnit 출력 35행). data class 의 equals 는 BigDecimal.equals 라 scale 이 다르면 같은 값도 다르다고 본다.
- 생길 수 있는 일: 현재 copy() 호출처는 없어(grep: 이 타입의 copy 사용 0건) 지금은 정상이다. 다만 누군가 copy(estimatedTax = x) 를 쓰면 scale 이 맞지 않는 값이 TaxSummary.recalculate 로 넘어가고, 값 객체 동등 비교(==)가 scale 차이로 어긋난다.
- 해결: data class 를 일반 class 로 바꾸고 equals·hashCode 는 compareTo 기준(또는 정규화된 값)으로 직접 구현하거나, 모든 값이 정규화된 뒤에만 만들어지게 init 에서 setScale 한다.

고친 모양(예시):
```
class TaxComputationResult private constructor(
    val totalRealizedPnl: BigDecimal,
    val taxablePnl: BigDecimal,
    val estimatedTax: BigDecimal,
) {
    companion object {
        fun of(a: BigDecimal, b: BigDecimal, c: BigDecimal) = TaxComputationResult(
            a.setScale(4, RoundingMode.HALF_UP), b.setScale(4, RoundingMode.HALF_UP), c.setScale(4, RoundingMode.HALF_UP),
        )
    }
}
```

### 468위 [low · 확신 med] application 이 Spring 영속 예외 DataIntegrityViolationException 을 직접 잡는다

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/portfolio/SnapshotJobService.kt:71` · 항목표 data/transaction-boundary · 항목 X10

```
        val run = try {
            snapshotJobRunTxService.start(account, businessDate)
        } catch (_: DataIntegrityViolationException) {   // ← Spring 영속 예외가 application 에 올라옴 (ArchUnit 동결 위반 L69)
```

- 원인: 어댑터가 영속 예외를 애플리케이션 예외로 바꾸지 않아 Spring 타입이 application 으로 올라왔다. ArchUnit 동결 저장소에 같은 위반이 기록돼 있다(SnapshotJobService.kt:69). 이 catch 자체는 REQUIRES_NEW 안쪽에서 나온 예외를 바깥 롤백 없이 변환하는 올바른 모양이다.
- 생길 수 있는 일: JPA 세부(유일 제약 위반)가 업무 분기에 새어, 영속 기술을 바꾸면 application 코드도 고쳐야 한다. 동결 목록에 있어 새 위반은 아니다.
- 해결: SnapshotJobRunTxService.start 가 유일 제약 위반을 SnapshotAlreadyRunningException 으로 변환해 던지게 하고, 서비스에서는 이 catch 를 지운다.
- 다른 렌즈의 반증: backend/code-shape+layering+naming-terminology+tdd-purity — 유일 제약 위반을 업무 예외로 번역하는 코드라 흐름 제어가 아니다. L2 로 따로 적었다.

### 469위 [low · 확신 med] 포트 결과 타입이 port 패키지 밖에 있음

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/portfolio/tax/TaxSettlementAggregate.kt:12` · 항목표 backend/layering · 항목 L3

```
package com.papertrading.api.application.portfolio.tax  // ← port 패키지 밖
data class TaxSettlementAggregate(  // ← 포트 반환 타입
```

- 원인: SettlementTaxReadRepository 가 돌려주는 결과 타입이 application.portfolio.tax 에 있어 PTB8 의 «요청·결과 타입도 같은 port 패키지»에 어긋난다. 포트 이전 때 함께 옮겨야 하므로 같은 결함의 두 번째 자리다.
- 생길 수 있는 일: 인터페이스만 옮기면 infrastructure 가 port 밖 타입을 계속 import 한다.
- 해결: 포트와 함께 application.portfolio.port 로 옮긴다.

고친 모양(예시):
```
package com.papertrading.api.application.portfolio.port
data class TaxSettlementResult(...)
```

### 470위 [low · 확신 med] 통화 코드 "KRW" 를 문자열 리터럴로 비교

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/portfolio/tax/TaxSummaryCalculator.kt:12` · 항목표 backend/naming-terminology · 항목 N11

```
if (input.currency != "KRW") {  // ← 통화 코드 문자열 리터럴 비교(TaxSummaryCommandService 119행에도 동일)
```

- 원인: input.currency != "KRW" 가 상수·enum 없이 비교된다. 같은 비교가 TaxSummaryCommandService.kt 119행에도 있다.
- 생길 수 있는 일: 오타나 새 통화 추가 때 비교가 한쪽에서만 바뀌어 지원 통화 판정이 어긋난다.
- 해결: `SUPPORTED_CURRENCY` 상수나 Currency enum 으로 모은다.

고친 모양(예시):
```
const val SUPPORTED_CURRENCY = "KRW"
```

### 471위 [low · 확신 med] application 이 Spring 영속 예외 DataIntegrityViolationException 을 직접 잡는다

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/portfolio/TaxSummaryCommandService.kt:68` · 항목표 data/transaction-boundary · 항목 X10

```
        val run = try {
            taxSummaryRunRepository.save(TaxSummaryRun.Companion.start(account, taxYear.value, runType))
        } catch (ex: DataIntegrityViolationException) {   // ← Spring 영속 예외가 application 에 올라옴 (ArchUnit 동결 위반 L66)
```

- 원인: TaxSummaryRun 저장 시 유일 제약 위반을 서비스가 Spring 타입으로 잡아 TaxSummaryAlreadyRunningException 으로 바꾼다. ArchUnit 동결 저장소에 같은 위반이 기록돼 있다(TaxSummaryCommandService.kt:66).
- 생길 수 있는 일: 영속 세부가 application 으로 새고, 어댑터를 바꾸면 이 분기가 조용히 안 걸린다. 동결 목록에 있어 새 위반은 아니다.
- 해결: 실행 행 저장을 담는 어댑터가 예외를 애플리케이션 예외로 변환해 던지게 한다. X6 의 REQUIRES_NEW 분리와 함께 고치면 자연스럽다.
- 다른 렌즈의 반증: data/query+schema-data-model+transaction-boundary — 변환한 예외를 바로 던져 대체 경로로 진행하지 않는다. Spring 타입 노출은 X10 으로 보고했다.

### 472위 [low · 확신 med] 통화 코드 "KRW" 를 문자열 리터럴로 비교(계산기와 중복)

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/portfolio/TaxSummaryCommandService.kt:119` · 항목표 backend/naming-terminology · 항목 N11

```
if (input.currency != "KRW") {  // ← 문자열 리터럴 비교, 계산기에도 같은 줄
```

- 원인: private compute 가 TaxSummaryCalculator 와 같은 "KRW" 문자열 비교를 한다.
- 생길 수 있는 일: 통화 규칙이 두 곳에 흩어진다(S3 발견과 같은 뿌리).
- 해결: 상수 하나로 모으고 계산기 하나만 남긴다.

고친 모양(예시):
```
const val SUPPORTED_CURRENCY = "KRW"
```

### 473위 [low · 확신 med] 파일 이름이 Upsert 인데 Create·Update·Cancel 명령 셋이 들어 있다

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/position/command/UpsertPositionExitTriggerCommand.kt:7` · 항목표 backend/naming-terminology · 항목 N7

```
data class CreatePositionExitTriggerCommand(  // ← 파일명은 Upsert...
...
data class UpdatePositionExitTriggerCommand(
...
data class CancelPositionExitTriggerCommand(
```

- 원인: 파일명 UpsertPositionExitTriggerCommand.kt 와 달리 안에는 CreatePositionExitTriggerCommand·UpdatePositionExitTriggerCommand·CancelPositionExitTriggerCommand 만 있고 Upsert 명령은 없다. 여러 클래스를 담은 파일은 내용을 설명하는 이름이어야 한다(Kotlin 컨벤션). ktlint filename 규칙은 최상위 클래스가 여럿이면 검사하지 않아 도구에 안 잡힌다.
- 생길 수 있는 일: 파일명으로 클래스를 찾는 사람이 Upsert 명령이 있다고 믿고, 실제 의미(생성·수정·취소 분리)를 놓친다.
- 해결: 파일명을 PositionExitTriggerCommands.kt 로 바꾼다.

### 474위 [low · 확신 med] percent.divide(HUNDRED) 가 scale·RoundingMode 없이 나눈다

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/position/PositionExitTriggerCommandService.kt:140` · 항목표 backend/code-shape · 항목 F1

```
val ratio = percent.divide(HUNDRED)  // ← scale·RoundingMode 없는 divide(x)
```

- 원인: PTB4 는 divide(x, scale, RoundingMode) 또는 MathContext 를 요구한다. 다만 제수가 상수 100 이라 항상 유한 소수로 떨어져 ArithmeticException 도 조용한 반올림도 실제로는 나지 않는다. 그래서 심각도를 낮췄다. HUNDRED 상수가 바뀌거나 다른 제수로 복사되면 그때 터진다.
- 생길 수 있는 일: 지금은 결과가 정확하지만, 같은 모양을 100 이 아닌 제수에 복사하면 비종결 소수에서 ArithmeticException 으로 시세 틱 전체 처리가 중단된다.
- 해결: percent.divide(HUNDRED, 8, RoundingMode.HALF_UP) 처럼 scale 과 RoundingMode 를 명시한다. Evaluator 와 한 곳에서 계산하도록 합치면 더 좋다.

고친 모양(예시):
```
val ratio = percent.divide(HUNDRED, 8, RoundingMode.HALF_UP)
```

### 475위 [low · 확신 med] percentRatio 가 scale·RoundingMode 없는 divide(x) 를 쓴다

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/position/PositionExitTriggerEvaluator.kt:70` · 항목표 backend/code-shape · 항목 F1

```
private fun percentRatio(percent: BigDecimal): BigDecimal =
    percent.divide(HUNDRED)  // ← scale·RoundingMode 없는 divide(x)
```

- 원인: CommandService L140 과 같은 모양이다. 제수가 상수 100 이라 항상 종결 소수이므로 실제 오동작은 없고, PTB4 의 명시 규칙 위반으로만 남는다. 같은 식이 CommandService 에도 따로 있어 두 곳이 어긋날 수 있다.
- 생길 수 있는 일: 제수가 100 이 아니게 바뀌면 ArithmeticException 으로 onQuote 처리가 중단된다. 두 곳의 반올림 방식이 달라지면 등록 시점 임계가와 평가 시점 임계가가 어긋난다.
- 해결: divide(HUNDRED, scale, RoundingMode.HALF_UP) 로 명시하고, 임계가 계산을 한 곳(도메인 메서드)으로 모은다(D3 참고).

### 476위 [low · 확신 med] notification·order 컨텍스트의 구현 클래스를 직접 import 한다

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/position/PositionExitTriggerOrchestrator.kt:3` · 항목표 backend/layering · 항목 L8

```
import com.papertrading.api.application.notification.SlackNotificationEventPublisher  // ←
import com.papertrading.api.application.order.AutoExitTriggerAuditInput
...
    private val notificationEventPublisher: SlackNotificationEventPublisher,
```

- 원인: application.notification.SlackNotificationEventPublisher(스프링 @Component 클래스)와 application.order.AutoExitTriggerAuditInput 을 import 한다. notification 은 여러 컨텍스트가 쓰는 공용 성격이라 심각도를 낮췄다.
- 생길 수 있는 일: 알림 발행 구현 변경이 position 유스케이스로 번지고 단위 시험이 구체 클래스에 묶인다.
- 해결: application.common.port 의 알림 발행 포트를 주입받는다.

### 477위 [low · 확신 med] 락 맵이 줄지 않고, 모니터를 쥔 채 무제한 DB 락 대기·재시도

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/position/PositionExitTriggerOrchestrator.kt:33` · 항목표 reliability/load-concurrency · 항목 A4

```
private val positionLocks = ConcurrentHashMap<Long, Any>()   // ← 제거 없이 포지션 수만큼 누적
...
synchronized(positionLocks.computeIfAbsent(groupKey.positionId) { Any() }) {   // ← 모니터 보유 중 DB 락 대기·재시도
    val position = positionRepository.findByIdWithLock(groupKey.positionId).orElse(null)   // ← lock.timeout 힌트 없음
```

- 원인: `positionLocks` 는 처리한 포지션마다 항목이 생기고 제거되지 않는다. synchronized 모니터를 쥔 상태에서 `findByIdWithLock`(PositionRepository.kt:16-25 — lock.timeout 힌트 없음, 트리거 쿼리에는 3000ms 힌트가 있음)과 재시도 루프가 돌아 같은 포지션을 노리는 다른 스레드가 모니터에서 대기한다. 인스턴스 사이 보호는 DB 행 잠금이 맡고 JVM 모니터는 중복이라 이점이 적다. (PostgreSQL 이 lock.timeout 힌트를 실제로 적용하는지는 패키지 미열람.)
- 생길 수 있는 일: 포지션 수만큼 맵이 자라고, 한 포지션의 락 대기가 길어지면 시세를 처리하는 스레드가 줄줄이 묶인다.
- 해결: JVM 모니터를 제거하고 DB 행 잠금(타임아웃 힌트 포함)만 쓰거나, 필요하면 항목을 정리하는 Striped lock 을 쓴다.

고친 모양(예시):
```
// synchronized 제거, PositionRepository.findByIdWithLock 에 힌트 추가
@QueryHints(QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))
@Lock(LockModeType.PESSIMISTIC_WRITE)
fun findByIdWithLock(...)
```

### 478위 [low · 확신 med] publishOrderCreateFailure 인자가 6개다

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/position/PositionExitTriggerOrchestrator.kt:187` · 항목표 backend/code-shape · 항목 S5

```
private fun publishOrderCreateFailure(
    accountId: Long,
    positionId: Long,
    triggerType: TriggerType,
    orderGroupId: String,
    quoteAt: Instant,
    reason: String,  // ← 인자 6개
) {
```

- 원인: accountId·positionId·triggerType·orderGroupId·quoteAt·reason 6개. 호출부 두 곳이 모두 같은 6개를 풀어서 넘긴다.
- 생길 수 있는 일: 실패 알림 항목을 늘릴 때 호출부 두 곳이 같이 바뀌어야 하고, 한 곳만 빠뜨려도 알림 메시지가 달라진다.
- 해결: AutoExitFailure 값 객체(또는 호출 문맥 객체)로 묶는다.
- 다른 렌즈의 반증: backend/code-shape+layering+naming-terminology+tdd-purity — 호출부 두 곳(L161~L168, L172~L179)이 모두 이름 인자로 부른다.

### 479위 [low · 확신 med] get* 조회 경로(getPositionWithCurrentPrice)가 Redis 캐시에 쓴다

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/position/PositionQueryService.kt:33` · 항목표 backend/code-shape · 항목 F2

```
fun getPositionWithCurrentPrice(accountId: Long, ticker: String): PositionResult {  // ← get* 인데
    ...
    return PositionResult.from(snapshotFor(position))
}
...
    return livePositionCachePort.save(evaluated)  // ← 캐시에 쓴다
```

- 원인: getPositionWithCurrentPrice → snapshotFor → snapshotFromDbWithLatestQuote 끝에서 livePositionCachePort.save 를 부른다. 목록 조회(listPositionsWithCurrentPrice)도 같다. 캐시가 파생 데이터이고 같은 값을 다시 쓰는 캐시 채우기라 해가 작아 심각도를 낮췄다.
- 생길 수 있는 일: readOnly 트랜잭션의 조회가 공유 Redis 상태를 바꾸므로, 조회가 재시도되거나 동시에 불리면 PositionCommandService 가 쓴 더 최신 스냅샷을 DB 포지션 기준 값으로 덮을 수 있다.
- 해결: 조회는 캐시를 읽기만 하고, 캐시 채우기는 PositionCommandService 쪽(시세 수신)이나 명시적 refresh 함수로 분리한다.

### 480위 [low · 확신 med] 부작용이 있는 snapshotFor·snapshotFromDbWithLatestQuote 가 명사구 이름이다

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/position/PositionQueryService.kt:44` · 항목표 backend/naming-terminology · 항목 N2

```
private fun snapshotFor(position: Position): LivePositionSnapshot =  // ← 명사구인데 아래에서 캐시에 쓴다
    livePositionCachePort.find(position.account.id!!, position.ticker)
        ?: snapshotFromDbWithLatestQuote(position)
...
    return livePositionCachePort.save(evaluated)
```

- 원인: 두 함수가 이름은 «스냅샷»이라는 명사구인데 snapshotFromDbWithLatestQuote 끝에서 livePositionCachePort.save 로 Redis 에 쓴다(L56). 호출자는 순수 계산으로 읽는다.
- 생길 수 있는 일: 읽는 사람이 공짜 계산이라 믿고 루프에서 부르면(listPositionsWithCurrentPrice 가 map 안에서 부른다) 포지션 수만큼 Redis 쓰기가 일어난다.
- 해결: loadAndCacheSnapshot·refreshSnapshot 처럼 동사형으로 바꾸거나 캐시 쓰기를 분리한다.

### 481위 [low · 확신 med] 한 컨텍스트 전용 예외 42개가 공유 common 이름·자리에 있음

`backend/trading-api/src/main/kotlin/com/papertrading/api/common/exception/CommonApiExceptions.kt:93` · 항목표 backend/naming-terminology · 항목 N12

```
class KisAuthorizationException(message: String, cause: Throwable? = null) :   // ← KIS 어댑터 전용인데 최상위 common 에 있음
    UnauthorizedException("KIS_UNAUTHORIZED", message, cause)
...
class TaxSummaryNotFoundException(message: String) :   // ← 세금 컨텍스트 전용(147줄)
    NotFoundException("TAX_SUMMARY_NOT_FOUND", message)
```

- 원인: 파일 이름은 «공통»을 말하지만 TaxSummaryNotFoundException·TaxYearNotClosedException(세금), SnapshotAlreadyRunningException·SnapshotComputeFailedException(스냅샷), KisAuthorizationException·KisRemoteCallException·KisTimeoutException(KIS 어댑터), SlackWebhookFailedException(알림)처럼 특정 컨텍스트가 던지는 예외가 한 파일에 모여 있다. 공유로 읽히지만 실제로는 한 곳만 쓰는 심벌이 common 에 있다.
- 생길 수 있는 일: 새 예외가 모두 이 파일로 모여 비대해지고, 컨텍스트를 바꾸거나 떼어낼 때 어느 예외가 누구 것인지 이름만으로 알 수 없어 다음 재사용자가 공유 계약으로 오해한다.
- 해결: 공통 기반 예외(BadRequest·NotFound·Conflict 등)만 common 에 남기고, 컨텍스트 전용 예외는 각 컨텍스트 패키지(예 tax·snapshot·infrastructure/kis)로 옮긴다.
- 다른 렌즈의 반증: reliability/security-config-hygiene — KIS 외부 API 호출 실패(KIS_AUTH_FAILED)용 예외다(KisAccountBalanceAdapter.kt:96). trading-api 자체 요청 인증과 무관하다.

### 482위 [low · 확신 med] Account 의 public 함수가 23개로 표면이 넓음

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/account/Account.kt:29` · 항목표 backend/code-shape · 항목 S4

```
class Account protected constructor() : BaseAuditEntity() { // ← public 함수 23개(addDeposit … updateExitTriggerDefault)
```

- 원인: addDeposit·recordDeposit·recordInitialDeposit·withdraw·recordWithdrawal·lockDeposit·recordBuyLock·unlockDeposit·recordBuyUnlock·confirmBuy·recordBuyExecution·receiveSellProceeds·recordSellExecution·recordFee·recordDepositWithLedger·rename·updateExternalAccountId·deactivate·activate·updateRiskPolicy·createRiskPolicy·createExitTriggerDefault·updateExitTriggerDefault. detekt TooManyFunctions(기준선)도 같은 지적이다.
- 생길 수 있는 일: 넓은 표면이 S3(주인공 둘)를 가리고 변경 파급이 커진다.
- 해결: S3 처럼 다른 애그리거트 규칙을 걷어내고, record* 쌍(변경+원장)을 한 연산으로 묶어 표면을 줄인다.
- 다른 렌즈의 반증: backend/code-shape+layering+naming-terminology+tdd-purity — 프로젝트 조상이 2단(깊이 2)이라 > 2 가 아니고, 두 부모는 감사 컬럼용 @MappedSuperclass 로 JPA 매핑 상속 관례다

### 483위 [low · 확신 med] Boolean 인자 enabled 에 is 접두가 없음

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/account/Account.kt:271` · 항목표 backend/naming-terminology · 항목 N3

```
fun createExitTriggerDefault(
    enabled: Boolean, // ← is 접두 없음
```

- 원인: createExitTriggerDefault·updateExitTriggerDefault·validateExitTriggerDefault 의 인자 이름이 enabled 이다.
- 생길 수 있는 일: 함수 시그니처가 불리언임을 이름으로 드러내지 못한다.
- 해결: 엔티티와 함께 isEnabled 로 통일한다.

### 484위 [low · 확신 med] AccountLedger.create 인자가 8개

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/account/AccountLedger.kt:75` · 항목표 backend/code-shape · 항목 S5

```
internal fun create( // ← 인자 8개
    account: Account,
    transactionType: TransactionType,
    amount: BigDecimal,
    balanceAfter: BigDecimal,
    idempotencyKey: String,
    refOrderId: Long? = null,
    refExecutionId: Long? = null,
    description: String? = null
): AccountLedger
```

- 원인: account·transactionType·amount·balanceAfter·idempotencyKey·refOrderId·refExecutionId·description. detekt LongParameterList 기준선에 있다. 호출부가 모두 이름 있는 인자와 기본값을 쓰긴 한다.
- 생길 수 있는 일: 원장 필드가 늘 때마다 시그니처가 커지고 refOrderId·refExecutionId 처럼 같은 타입이 섞인다.
- 해결: 참조(refOrderId·refExecutionId)를 LedgerReference 값 객체로 묶고, 거래 종류별 팩토리(buyLock·deposit 등)를 둔다.
- 다른 렌즈의 반증: backend/code-shape+layering+naming-terminology+tdd-purity — protected 생성자 + companion 팩토리 구조가 있고 internal 팩토리는 Account.record* 가 먼저 금액을 검증한 뒤 부른다(렌즈 기준 «private 생성자 + 팩토리» 충족)

### 485위 [low · 확신 med] risk_events 의 FK(account_id·risk_policy_id)에 인덱스가 없음

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/account/RiskEvent.kt:28` · 항목표 data/schema-data-model · 항목 M4

```
@ManyToOne(fetch = FetchType.LAZY, optional = false)
@JoinColumn(name = "account_id", nullable = false)   // ← 인덱스 없음
lateinit var account: Account
```

- 원인: @Table 에 indexes 가 없고 마이그레이션에도 DDL 이 없어 두 FK 열이 어떤 인덱스의 선두도 아니다. append-only 이력이라 계속 자란다. 조회 메서드(findByAccountIdOrderByTriggeredAtDesc)는 현재 호출처가 없어 영향은 잠복 상태다.
- 생길 수 있는 일: 이력 조회가 생기는 순간 계좌별 조회가 전체 스캔이 된다.
- 해결: (account_id, triggered_at DESC) 인덱스를 추가한다.

### 486위 [low · 확신 med] risk_policies 의 FK(account_id)에 인덱스가 없음

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/account/RiskPolicy.kt:32` · 항목표 data/schema-data-model · 항목 M4

```
@ManyToOne(fetch = FetchType.LAZY, optional = false)
@JoinColumn(name = "account_id", nullable = false)   // ← 인덱스 없음
lateinit var account: Account
```

- 원인: 활성 정책 조회(findByAccountIdAndIsActiveTrue)가 account_id 로 걸리는데 인덱스가 없다. 정책 수정마다 비활성 이력 행이 남는다. 테이블이 작아 영향은 낮다.
- 생길 수 있는 일: 이력이 쌓이면 주문 접수 경로의 정책 조회가 전체 스캔이 된다.
- 해결: M3 의 부분 유일 인덱스 (account_id) WHERE is_active 가 이 조회를 함께 덮는다.

### 487위 [low · 확신 med] «계좌당 활성 정책 1개» 불변식에 DB 부분 유일 제약이 없음

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/account/RiskPolicy.kt:49` · 항목표 data/schema-data-model · 항목 M3

```
@Column(name = "is_active", nullable = false)   // ← 계좌당 활성 1행을 DB 가 보장하지 않음
var isActive: Boolean = true
    private set
```

- 원인: replaceRiskPolicy 는 계좌 락 안에서 기존 활성 정책을 끄고 새 정책을 만들어 앱에서 유일성을 지킨다. DB 에는 (account_id) WHERE is_active 유일 인덱스가 없고, 조회는 Optional 로 하나만 기대하므로 둘이 생기면 조회가 예외를 낸다.
- 생길 수 있는 일: 락을 우회하는 경로나 수동 데이터 수정으로 활성 정책이 둘이 되면 주문 접수 때 리스크 정책 조회가 실패한다.
- 해결: 마이그레이션으로 CREATE UNIQUE INDEX … ON risk_policies (account_id) WHERE is_active 를 추가한다.

### 488위 [low · 확신 med] updated_at 도 DB 는 NOT NULL 인데 Kotlin 타입은 nullable(Instant?)

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/base/BaseAuditEntity.kt:10` · 항목표 data/schema-data-model · 항목 M7

```
@UpdateTimestamp
@Column(name = "updated_at", nullable = false)  // ← DB 는 NOT NULL
var updatedAt: Instant? = null  // ← Kotlin 타입은 nullable: 두 쪽이 어긋남
```

- 원인: BaseTimeEntity 의 created_at 과 같은 모양입니다. @UpdateTimestamp 는 insert·update 때 Hibernate 가 값을 채우므로 영속 뒤에는 항상 값이 있는데, 타입은 Instant? 입니다. 소비 코드는 열어 보지 못했습니다(LLM-only).
- 생길 수 있는 일: BaseAuditEntity 를 상속하는 엔티티 전부의 updatedAt 소비자가 null 분기를 하거나 !!·기본값 대체를 쓰게 됩니다. 마지막 변경 시각이 감사 근거라면 조용한 대체가 거짓 시각을 남깁니다.
- 해결: created_at 과 같은 방식으로 정합니다(비널 lateinit 또는 소비자 쪽 null 처리 한 곳 집중). 두 베이스 클래스를 같은 방식으로 맞춥니다.

고친 모양(예시):
```
@UpdateTimestamp
@Column(name = "updated_at", nullable = false)
lateinit var updatedAt: Instant
    protected set
```

### 489위 [low · 확신 med] created_at 은 DB 가 NOT NULL 인데 Kotlin 타입은 nullable(Instant?)

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/base/BaseTimeEntity.kt:10` · 항목표 data/schema-data-model · 항목 M7

```
@CreationTimestamp
@Column(name = "created_at", nullable = false, updatable = false)  // ← DB 는 NOT NULL
var createdAt: Instant? = null  // ← Kotlin 타입은 nullable: 두 쪽이 어긋남
```

- 원인: createdAt 은 Hibernate 가 저장할 때 채우므로 영속 뒤에는 항상 값이 있습니다. 그런데 타입이 Instant? 라서 BaseTimeEntity 를 상속하는 모든 엔티티의 소비자가 null 을 다뤄야 하고, null 이 셋째 상태가 됩니다. 저장 전 null 을 허용하려는 관용 표현이라 위험은 낮습니다. 소비 코드는 이 묶음 밖이라 열어 보지 못했습니다(LLM-only).
- 생길 수 있는 일: 응답·정산·감사 매핑에서 !!(레포 backend 규칙이 금지)나 ?: Instant.now() 같은 조용한 대체가 생기면 감사 시각이 거짓이 됩니다. 전 엔티티에 퍼지는 공통 베이스라 한 번 정하면 영향이 넓습니다.
- 해결: 영속 뒤에는 항상 값이 있으니 비널 lateinit 로 선언하고 저장 전 접근은 예외로 드러내거나, 소비자 쪽 null 처리를 한 곳으로 모읍니다. 어느 쪽이든 !!·조용한 기본값 대체는 두지 않습니다.

고친 모양(예시):
```
@CreationTimestamp
@Column(name = "created_at", nullable = false, updatable = false)
lateinit var createdAt: Instant
    protected set
```

### 490위 [low · 확신 med] 도메인 함수 isCurrent() 가 Instant.now() 를 직접 읽음 (호출처 없음)

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/feepolicy/FeePolicy.kt:72` · 항목표 backend/code-shape · 항목 F4

```
    fun isCurrent(): Boolean = isActiveAt(Instant.now())   // ← 도메인이 시각을 직접 읽음
```

- 원인: domain 함수가 Instant.now() 를 직접 읽어 시험이 고정할 수 없다. 호출처를 grep 했으나 main·test 어디에도 없다(serena 언어 서버 장애로 grep 대체, 0건이 부재 증명은 아님).
- 생길 수 있는 일: 지금은 호출이 없어 영향이 없으나, 쓰이는 순간 시각 비결정이 도메인에 들어오고 시험이 불가능해진다. 죽은 코드이기도 하다.
- 해결: 삭제하거나 시각을 인자로 받는 isActiveAt(at)만 남긴다.

### 491위 [low · 확신 med] 팩토리 create 의 인자가 7개다

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/notification/Notification.kt:109` · 항목표 backend/code-shape · 항목 S5

```
fun create(
    eventType: NotificationEventType,
    sourceRef: String,
    title: String,
    message: String,
    account: Account? = null,
    refOrderId: Long? = null,
    refExecutionId: Long? = null,   // ← 7번째 인자 (5개 초과)
): Notification {
```

- 원인: companion 의 create 가 인자 7개를 받는다(detekt LongParameterList 도 같은 줄을 잡았다). 인자 객체가 빠져 있다. 기본값과 이름 붙은 인자 덕에 현재 유일한 호출처(핸들러)는 안전하지만 위치 결합 위험은 남는다.
- 생길 수 있는 일: 같은 Long? 타입의 refOrderId·refExecutionId 가 나란히 있어 새 호출처에서 위치 인자로 부르면 뒤바뀌어도 컴파일이 통과한다.
- 해결: 계좌·주문·체결 참조를 NotificationReference 같은 값 객체 하나로 묶어 인자를 5개 이하로 줄인다.

고친 모양(예시):
```
data class NotificationReference(val accountId: Long?, val orderId: Long?, val executionId: Long?)
fun create(eventType: NotificationEventType, sourceRef: String, title: String, message: String, reference: NotificationReference = NotificationReference(null, null, null)): Notification
```

### 492위 [low · 확신 med] 통화 코드 "KRW" 리터럴이 상수 없이 반복

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/order/Execution.kt:65` · 항목표 backend/naming-terminology · 항목 N11

```
    var currency: String = "KRW"            // ← L65 리터럴
...
            currency: String = "KRW",           // ← L107 같은 리터럴 반복
```

- 원인: 통화 기본값이 Execution L65·L107 에 리터럴로 있고 Settlement(L69·L103·L104)에도 같은 "KRW" 가 있다. ExecutionProcessor 는 account.baseCurrency 를 쓰므로 기본값 "KRW" 와 어긋날 수 있다.
- 생길 수 있는 일: 기준통화 정책을 바꾸면 리터럴을 모두 찾아야 하고, 오타("KWR")가 조용한 분기 누락이 된다.
- 해결: const val BASE_CURRENCY = "KRW" 또는 Currency 값 타입으로 모은다.
- 다른 렌즈의 반증: backend/code-shape+layering+naming-terminology+tdd-purity — FX·PnL·KRW 는 금융 표준 용어이고 Settlement KDoc 의 «실현손익(realizedPnl)»처럼 기획 용어와 코드가 일치(CS3 보편 언어)

### 493위 [low · 확신 med] Order.create 인자 14개

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/order/Order.kt:165` · 항목표 backend/code-shape · 항목 S5

```
        fun create(                                  // ← 인자 14개
            account: Account,
            ticker: String,
            marketType: MarketType,
            orderType: OrderType,
            orderSide: OrderSide,
            orderCondition: OrderCondition,
            quantity: BigDecimal,
            limitPrice: BigDecimal? = null,
            lockedAmount: BigDecimal = BigDecimal.ZERO,
            idempotencyKey: String,
            orderGroupId: String? = null,
            expireAt: Instant? = null,
            strategyId: Long? = null,
            signalId: Long? = null
        ): Order {
```

- 원인: 팩토리 인자가 14개(한계 5). 이 파일에서 가장 심한 위치 결합이다. detekt 기준선 LongParameterList(tools/Tb661de0c.txt:166).
- 생길 수 있는 일: strategyId·signalId(둘 다 Long?)처럼 같은 타입이 인접해 순서 착오가 컴파일을 통과한다.
- 해결: 서비스가 이미 가진 PlaceOrderCommand 와 비슷하게 주문 명세를 한 객체(OrderSpec)로 묶어 팩토리에 넘긴다.
- 다른 렌즈의 반증: backend/code-shape+layering+naming-terminology+tdd-purity — 빈 줄을 빼면 본문 약 30줄로 40 이하(인자 수는 S5 로 별도 판정)

### 494위 [low · 확신 med] 같은 타입 id 인자 strategyId·signalId 가 나란히 옴 (Order.create)

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/order/Order.kt:178` · 항목표 backend/layering · 항목 D6

```
            strategyId: Long? = null,              // ← 같은 타입 id 가 나란히
            signalId: Long? = null                 // ←
```

- 원인: 둘 다 Long? 기본값 null 이라 순서를 바꿔 넘겨도 컴파일을 통과한다. 현재 호출부는 이름 붙은 인자를 써서 위험은 낮다.
- 생길 수 있는 일: 전략 id 와 신호 id 가 바뀌어 저장되면 신호 추적(감사)이 조용히 틀어진다.
- 해결: @JvmInline value class StrategyId(val value: Long)·SignalId 로 타입을 나눈다.

### 495위 [low · 확신 med] order_amendments 의 order_id FK 에 인덱스가 없음

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/order/OrderAmendment.kt:19` · 항목표 data/schema-data-model · 항목 M4

```
@Entity
@Table(name = "order_amendments")   // ← indexes 없음 — order_id FK 인덱스 없음
```

- 원인: @Table 에 indexes 가 없고 마이그레이션에도 없습니다. findByOrderIdOrderByCreatedAtAsc 가 이 열을 씁니다. 현재 main 호출처는 없어 low 입니다.
- 생길 수 있는 일: 정정 이력이 쌓인 뒤 주문별 조회가 테이블 전체 스캔이 됩니다.
- 해결: (order_id, created_at) 인덱스를 추가합니다.

### 496위 [low · 확신 med] order_signals 의 account_id FK 와 is_processed 필터 열에 인덱스가 없음

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/order/OrderSignal.kt:29` · 항목표 data/schema-data-model · 항목 M4

```
@Table(
    name = "order_signals",
    uniqueConstraints = [UniqueConstraint(name = "uk_order_signals_idempotency", columnNames = ["idempotency_key"])]
)   // ← indexes 없음 — (account_id, is_processed, created_at) 인덱스 없음
```

- 원인: 유일 제약은 idempotency_key 뿐입니다. findByAccountIdAndIsProcessedFalseOrderByCreatedAtAsc 가 (account_id, is_processed, created_at)을 씁니다. 현재 main 호출처는 없어 low 입니다.
- 생길 수 있는 일: 신호가 쌓이면 미처리 신호 조회가 전체 스캔이 됩니다.
- 해결: (account_id, is_processed, created_at) 인덱스를 추가합니다.

### 497위 [low · 확신 med] create 인자 7개 — 같은 BigDecimal 다섯 개가 위치로 나란히 옴

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/portfolio/DailyBalance.kt:80` · 항목표 backend/code-shape · 항목 S5

```
        fun create(
            account: Account,
            businessDate: LocalDate,
            cashBalance: BigDecimal,
            stockMarketValue: BigDecimal,
            totalAssetValue: BigDecimal,   // ← 같은 타입 BigDecimal 5개가 위치로 연속
            pnlAmount: BigDecimal,
            pnlRate: BigDecimal   // ← 인자 7개
```

- 원인: create(account, businessDate, cashBalance, stockMarketValue, totalAssetValue, pnlAmount, pnlRate) 가 7개 인자이고 BigDecimal 5개가 연속이다. refresh 도 같은 5개 BigDecimal 을 다시 받는다. detekt 기준선에도 LongParameterList 로 들어 있다.
- 생길 수 있는 일: 호출부가 이름 인자를 쓰는 동안은 안전하지만 컴파일러가 순서 바뀜을 막지 못한다. cashBalance 와 stockMarketValue 가 뒤바뀌어도 타입이 같아 컴파일되고 금액이 조용히 틀린다.
- 해결: cash·stock 두 값만 받고 totalAssetValue·pnlAmount·pnlRate 는 엔티티 안에서 계산하거나, 평가 결과를 담는 값 객체(DailyBalanceFigures) 하나로 받는다. D5 와 같은 수정으로 풀린다.

### 498위 [low · 확신 med] refresh 인자 6개·create 인자 9개 — BigDecimal 이 위치로 연속

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/portfolio/PortfolioSnapshot.kt:78` · 항목표 backend/code-shape · 항목 S5

```
    fun refresh(
        quantity: BigDecimal,
        avgBuyPrice: BigDecimal,
        closePrice: BigDecimal,   // ← 같은 타입 6개 연속
        marketValue: BigDecimal,
        weight: BigDecimal,
        unrealizedPnl: BigDecimal   // ← 인자 6개(create 는 9개)
```

- 원인: refresh(quantity, avgBuyPrice, closePrice, marketValue, weight, unrealizedPnl) 6개, create 는 account·businessDate·ticker 포함 9개이며 BigDecimal 6개가 연속이다. detekt 기준선(LongParameterList)에도 올라 있다.
- 생길 수 있는 일: avgBuyPrice 와 closePrice 처럼 같은 타입 금액이 뒤바뀌어도 컴파일이 통과해 미실현손익·평가액이 조용히 틀린다.
- 해결: quantity·avgBuyPrice·closePrice 만 받고 marketValue·unrealizedPnl 은 엔티티가 계산하도록 하거나, 인자 객체(PositionValuation)로 묶는다. weight 는 합계가 필요하므로 별도 인자로 둔다.

### 499위 [low · 확신 med] sentiment·journalType 이 닫힌 값인데 String 으로 흩어져 있고 허용값 정의가 DTO 정규식 두 곳과 KDoc 에만 있음

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/portfolio/TradingJournal.kt:53` · 항목표 backend/naming-terminology · 항목 N11

```
 * sentiment: BULLISH | BEARISH | NEUTRAL — 당시 심리 태그.   // ← KDoc 에만 있는 닫힌 값 집합
    @Column(name = "sentiment", length = 20)
    var sentiment: String? = null   // ← String 으로 저장·검증 없음
// TradingJournalCreateRequest.kt:15  @field:Pattern(regexp = "^(BULLISH|BEARISH|NEUTRAL)$")   // ← 같은 집합이 DTO 정규식으로 반복(UpdateRequest.kt:12 에도)
```

- 원인: KDoc 은 sentiment 를 BULLISH | BEARISH | NEUTRAL 로 못 박았지만 도메인은 String 으로 받아 trim·uppercase 만 한다. 허용값 검사는 presentation 의 TradingJournalCreateRequest·UpdateRequest 두 곳에서 같은 정규식으로 반복된다. journalType 은 비어 있지 않은지만 보고 허용 목록이 어디에도 없다(CS24).
- 생길 수 있는 일: 도메인을 직접 부르는 경로(배치·다른 유스케이스)는 오타 sentiment(BULISH)를 그대로 저장하고, 정규식을 고칠 때 DTO 두 곳 중 하나를 빼먹으면 입력 경로마다 허용값이 달라진다.
- 해결: Sentiment enum 을 domain.enums 에 두고 엔티티는 @Enumerated(EnumType.STRING) 으로, DTO 는 enum 으로 받는다. journalType 도 허용 목록이 있으면 enum 으로 만든다.

고친 모양(예시):
```
enum class TradingJournalSentiment { BULLISH, BEARISH, NEUTRAL }
```

### 500위 [low · 확신 med] create 인자 7개 — 문자열 4개가 위치로 연속

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/portfolio/TradingJournal.kt:76` · 항목표 backend/code-shape · 항목 S5

```
        fun create(
            account: Account,
            journalType: String,
            title: String,
            content: String,   // ← String 3개 연속
            orderId: Long? = null,
            ticker: String? = null,
            sentiment: String? = null   // ← 인자 7개
```

- 원인: create(account, journalType, title, content, orderId, ticker, sentiment) 7개이고 앞쪽 String 3개 (journalType·title·content)가 연속이다. 뒤 3개는 기본값이 있지만 인자 수는 5개를 넘는다. detekt 기준선에도 올라 있다.
- 생길 수 있는 일: journalType 과 title 이 뒤바뀌어도 타입이 같아 컴파일되고, 길이 제한이 다른 컬럼(30자 vs 200자)에 잘못 들어가 저장 시점에야 터진다.
- 해결: 애플리케이션의 CreateTradingJournalCommand 와 같은 모양의 인자 객체(또는 journalType 을 enum 으로) 하나로 받는다.

### 501위 [low · 확신 med] Kotlin 비널 필드 priceSource 가 DB 에서는 nullable

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/position/Position.kt:100` · 항목표 data/schema-data-model · 항목 M7

```
@Enumerated(EnumType.STRING)
@Column(name = "price_source", length = 20)      // ← L101 nullable = false 없음
var priceSource: PriceSource = PriceSource.UNKNOWN   // ← Kotlin 비널
```

- 원인: priceSource 는 PriceSource 비널 타입이고 기본값 UNKNOWN 이라 항상 채워지는데, @Column 에 nullable = false 가 없다. 같은 엔티티의 다른 항상 채워지는 열은 모두 nullable = false 다.
- 생길 수 있는 일: DB 에 NULL 이 들어가면 읽는 시점에 매핑이 런타임 오류를 낸다.
- 해결: nullable = false 를 추가한다.

고친 모양(예시):
```
@Column(name = "price_source", nullable = false, length = 20)
```

### 502위 [low · 확신 med] PositionExitTrigger.create 인자가 8개다

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/position/PositionExitTrigger.kt:229` · 항목표 backend/code-shape · 항목 S5

```
fun create(
    positionId: Long,
    accountId: Long,
    ticker: String,
    triggerType: TriggerType,
    triggerPercent: BigDecimal?,
    triggerPrice: BigDecimal?,
    priceBasisPolicy: PriceBasisPolicy,
    exitRatioPercent: BigDecimal? = null,
): PositionExitTrigger =  // ← 인자 8개
```

- 원인: positionId·accountId·ticker·triggerType·triggerPercent·triggerPrice·priceBasisPolicy·exitRatioPercent 8개. detekt LongParameterList 기준선에도 있다(PositionExitTrigger.Companion). triggerPercent·triggerPrice·exitRatioPercent 가 모두 BigDecimal? 라 위치가 바뀌어도 컴파일이 통과한다.
- 생길 수 있는 일: 퍼센트와 가격이 뒤바뀐 트리거가 만들어져도 validatePolicy 범위 검사에 걸리지 않는 값이면 조용히 저장된다.
- 해결: 조건(triggerPercent·triggerPrice·priceBasisPolicy·exitRatioPercent)을 ExitCondition 값 객체로 묶어 create(positionId, accountId, ticker, triggerType, condition) 로 줄인다.
- 다른 렌즈의 반증: backend/code-shape+layering+naming-terminology+tdd-purity — 유일한 호출부 PositionExitTriggerCommandService L38~L47 이 positionId = …, accountId = … 이름 인자로 부른다.

### 503위 [low · 확신 med] settlements 의 account_id FK 에 인덱스가 없음

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/settlement/Settlement.kt:28` · 항목표 data/schema-data-model · 항목 M4

```
@Table(
    name = "settlements",
    uniqueConstraints = [UniqueConstraint(name = "uk_settlements_order", columnNames = ["order_id"])]
)   // ← account_id(FK)·settled_at 인덱스 없음
```

- 원인: order_id 는 UNIQUE 로 덮이지만 account_id 는 어떤 인덱스의 선두도 아닙니다. findByAccountIdOrderBySettledAtDesc 가 이 열을 씁니다. 현재 main 호출처는 없어 low 입니다.
- 생길 수 있는 일: 정산이 쌓이면 계정별 정산 조회가 전체 스캔이 됩니다.
- 해결: (account_id, settled_at) 인덱스를 추가합니다.

### 504위 [low · 확신 med] Settlement.createFromExecution 인자 6개

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/settlement/Settlement.kt:85` · 항목표 backend/code-shape · 항목 S5

```
        fun createFromExecution(                     // ← 인자 6개
            order: Order,
            account: Account,
            execution: Execution,
            tax: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
            settledAt: Instant,
            fxRateAtSettlement: BigDecimal? = execution.fxRate,
        ): Settlement {
```

- 원인: 인자 6개(한계 5). 기본값으로 execution 을 다시 참조(fxRateAtSettlement = execution.fxRate)한다. detekt 기준선 LongParameterList(tools/Tb661de0c.txt:213).
- 생길 수 있는 일: order·account·execution 이 서로 정합해야 하는데(execution.order == order) 인자로 따로 받아 어긋난 조합을 넘겨도 컴파일을 통과한다.
- 해결: order·account 는 execution 에서 꺼내(execution.order·execution.account) 인자를 3개(execution·tax·settledAt)로 줄인다.
- 다른 렌즈의 반증: backend/code-shape+layering+naming-terminology+tdd-purity — 빈 줄을 빼면 본문 약 31줄로 40 이하

### 505위 [low · 확신 med] 파생 계보의 parent·child 전략 id 조회에 인덱스가 없음

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/strategy/StrategyDerivation.kt:20` · 항목표 data/schema-data-model · 항목 M4

```
@Table(name = "strategy_derivations")   // ← L20 indexes 없음
// fun findByChildStrategyId(childStrategyId: Long): List<StrategyDerivation>   // ← child_strategy_id 선두 인덱스 필요
```

- 원인: findByParentStrategyId·findByChildStrategyId 가 각각 FK 열로 거는데 인덱스가 없다. M3 의 (parent, child) 유일 제약은 parent 선두 인덱스만 만들어 child 단독 조회는 따로 인덱스가 필요하다.
- 생길 수 있는 일: 파생 행이 늘면 child 기준 계보 조회가 전체 스캔이 된다.
- 해결: child_strategy_id 인덱스를 추가한다(parent 는 M3 의 유일 제약이 커버).

고친 모양(예시):
```
indexes = [Index(name = "idx_strategy_derivations_child", columnList = "child_strategy_id")]
```

### 506위 [low · 확신 med] 성과 스냅샷의 strategy_id·period_start 조회에 인덱스가 없음

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/strategy/StrategyPerformanceSnapshot.kt:22` · 항목표 data/schema-data-model · 항목 M4

```
@Table(name = "strategy_performance_snapshots")   // ← L22 indexes 없음
// fun findByStrategyIdAndPeriodStartGreaterThanEqualOrderByPeriodStartAsc(strategyId, from)   // ← strategy_id + period_start
```

- 원인: StrategyPerformanceSnapshotRepository 가 strategy_id 와 period_start 범위·정렬로 조회하는데 엔티티에 인덱스가 없다. 위 M3 의 유일 인덱스(strategy_id 선두)를 넣으면 같이 해소된다.
- 생길 수 있는 일: 스냅샷이 쌓이면 전략별 기간 조회가 전체 스캔이 된다.
- 해결: M3 의 유니크 제약(strategy_id, period_start, period_end)으로 함께 해결한다.

### 507위 [low · 확신 med] versionNo — 허용 목록 밖 약어 «No»

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/strategy/StrategyVersion.kt:38` · 항목표 backend/naming-terminology · 항목 N6

```
@Column(name = "version_no", nullable = false)
var versionNo: Int = 0   // ← 약어 No (L70 create 인자, L26 유일 제약, StrategyVersionRepository.kt:8·9 메서드 이름에도)
```

- 원인: 속성·create 인자·컬럼(version_no)·유일 제약 이름에 No 약어가 쓰인다. 허용 목록(id·db·url…·min·max·avg·idx·req·res·ctx·msg) 밖이다. 팀 관용이라는 반론이 있어 confidence 는 med.
- 생길 수 있는 일: 읽는 사람마다 No 를 number·none 으로 다르게 펼쳐 조회 메서드(findTop…OrderByVersionNoDesc)와 API 필드가 versionNo/versionNumber 로 갈릴 수 있다.
- 해결: versionNumber(또는 version)로 바꾼다. 컬럼 이름 변경은 마이그레이션이 필요하므로 속성 이름만 먼저 바꿔도 된다.

고친 모양(예시):
```
@Column(name = "version_no", nullable = false)
var versionNumber: Int = 0
```

### 508위 [low · 확신 med] 포지션 청산 트리거만 쓰는 enum 이 접두 없이 공유 domain.enums 에 있음

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/enums/PriceBasisPolicy.kt:3` · 항목표 backend/naming-terminology · 항목 N12

```
enum class PriceBasisPolicy {            // ← 청산 트리거 전용인데 소속이 이름에 없음
    FIXED_PRICE,
    AVG_PRICE_AT_CREATION,
    FOLLOW_AVG_PRICE,
}
```

- 원인: PriceBasisPolicy 의 사용처는 PositionExitTrigger·UpsertPositionExitTriggerCommand·PositionExitTriggerEvaluator·PositionExitTriggerDtos 뿐이다(grep). 같은 패키지의 Order*·Settlement* enum 은 컨텍스트 접두를 갖지만 이것은 «Price Basis Policy» 로 소속이 안 보인다.
- 생길 수 있는 일: 다음 사람이 가격 기준 정책을 주문·정산 공통 개념으로 오해해 다른 컨텍스트에서 재사용한다.
- 해결: ExitTriggerPriceBasis 로 이름을 바꾸고 position 컨텍스트 아래로 옮긴다.

### 509위 [low · 확신 med] 같은 타입 id executionId·orderId 가 나란히 옴 (ExecutionFilledEvent)

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/event/ExecutionFilledEvent.kt:8` · 항목표 backend/layering · 항목 D6

```
data class ExecutionFilledEvent(
    val executionId: Long,                 // ← 같은 타입 id 가 나란히
    val orderId: Long,                     // ←
```

- 원인: 둘 다 non-null Long 이라 순서가 뒤바뀌어도 컴파일을 통과한다. 생성 지점(ExecutionProcessor L211)은 이름 붙은 인자를 쓴다.
- 생길 수 있는 일: 이벤트 소비자(알림·포지션)가 체결 id 를 주문 id 로 읽는 오류가 조용히 생긴다.
- 해결: ExecutionId·OrderId 값 클래스로 타입을 나눈다.

### 510위 [low · 확신 med] 값 집합이 정해진 mode 를 String 으로 받고 같은 String 인자 둘이 나란히 옴

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/port/CollectorSubscriptionPort.kt:4` · 항목표 backend/layering · 항목 D6

```
    /** collector-api에 WebSocket 구독 요청. mode = "KIS_PAPER" | "KIS_LIVE" */
    fun subscribe(mode: String, ticker: String) // ← 값 집합이 정해진 mode 가 String, 같은 String 둘이 나란히 (TradingMode enum 이 이미 있음)

    /** collector-api에 WebSocket 구독 해제 요청. */
    fun unsubscribe(mode: String, ticker: String)
```

- 원인: subscribe·unsubscribe 가 mode: String 과 ticker: String 을 나란히 받는다. mode 는 KDoc 에 KIS_PAPER 또는 KIS_LIVE 로 값이 정해져 있고 같은 레포에 TradingMode enum 이 이미 있다(MarketQuotePort 는 enum 을 씀). 호출부 OrderCommandService.kt:66 은 enum 을 tradingMode.name 으로 풀어 String 으로 넘기고 ExecutionProcessor.kt:250 도 mode 문자열을 넘긴다(grep 결과). 어댑터(CollectorSubscriptionAdapter.kt:21·28)는 같은 시그니처를 override 하므로 선언 자리에서 한 번만 센다.
- 생길 수 있는 일: 인자 순서를 바꿔 호출해도 컴파일이 통과하고 잘못된 URL 로 구독 요청이 나간다. 어댑터가 실패를 로그만 남기고 삼키므로 구독이 조용히 안 걸려 시세가 안 들어오는 장애로 이어질 수 있다.
- 해결: mode 를 TradingMode 로 받고 문자열 변환은 어댑터 안에서 한다.

고친 모양(예시):
```
fun subscribe(mode: TradingMode, ticker: String)
fun unsubscribe(mode: TradingMode, ticker: String)
```

### 511위 [low · 확신 med] 구독 실패 로그에 예외 객체가 빠져 스택·원인 상실

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/client/CollectorSubscriptionAdapter.kt:23` · 항목표 reliability/failure-handling · 항목 E2

```
runCatching { restTemplate.postForObject(url, null, String::class.java) }
    .onFailure { log.warn { "collector 구독 요청 실패: ticker=$ticker, mode=$mode, reason=${it.message}" } }  // ← 예외 객체 미전달, 스택 없음
```

- 원인: onFailure 에서 reason=${it.message} 문자열만 남기고 예외 객체를 넘기지 않는다. 메시지가 null 이면 'reason=null' 이 된다. 종목·모드 식별자는 들어 있다.
- 생길 수 있는 일: 연결 거부·타임아웃·4xx 를 구분할 스택이 로그에 없어 구독 실패 원인을 되짚기 어렵다. 구독이 안 되면 그 종목 시세가 안 와 지정가가 영영 체결되지 않는다.
- 해결: kotlin-logging 의 log.warn(it) { ... } 로 예외를 함께 넘긴다.
- 다른 렌즈의 반증: backend/code-shape+layering+naming-terminology+tdd-purity — KDoc 11행에 구독 실패가 주문 처리를 막지 않도록 로그만 남긴다고 명시한 best-effort 호출이다. 예외를 분기에 쓰지 않고 로그로 격리하므로 S10 이 아님(삼킴의 타당성은 failure-handling 판정)

고친 모양(예시):
```
runCatching { ... }.onFailure { log.warn(it) { "collector 구독 요청 실패: ticker=$ticker, mode=$mode" } }
```

### 512위 [low · 확신 med] KIS 응답 코드와 모드를 상수·enum 없이 문자열 리터럴로 비교함

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/kis/KisAccountBalanceAdapter.kt:41` · 항목표 backend/naming-terminology · 항목 N11

```
        val mode = if (trId == PAPER_TR_ID) "paper" else "live"      // ← 모드 문자열 리터럴
...
        if (rtCd == "0") return      // ← KIS 응답 코드를 리터럴로 비교
...
            rtCd == "1" || message.contains("인증") || message.contains("token", ignoreCase = true) ->
...
            rtCd == "7" || message.contains("권한") ->
```

- 원인: rt_cd 를 "0"·"1"·"7" 리터럴로 비교하고 모드를 "paper"/"live" 문자열로 만든다. 같은 의미의 KisAccountMode enum(LIVE·PAPER)이 이미 있고, 모드를 trId 에서 거꾸로 추정한다. PAPER_TR_ID 상수는 한 줄만 상수고 live TR 은 else 로 암묵 처리된다.
- 생길 수 있는 일: 코드 값 오타가 조용한 분기 누락이 된다(예 "7" 을 "07" 로 쓰면 권한 오류가 일반 원격 오류로 떨어진다). TR ID 와 모드 매핑이 서비스와 어댑터 두 곳에 갈라진다.
- 해결: rt_cd 는 const val 로 모으고(KisReturnCode), 모드는 KisAccountMode enum 으로 받거나 변환한다.

고친 모양(예시):
```
private object KisReturnCode {
    const val SUCCESS = "0"
    const val AUTH_FAILED = "1"
    const val FORBIDDEN = "7"
}
```

### 513위 [low · 확신 med] 클래스 이름이 Manager 로 끝나 역할을 말하지 않음

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/kis/KisExecutionWebSocketManager.kt:129` · 항목표 backend/naming-terminology · 항목 N5

```
@Component
class KisExecutionWebSocketManager( // ← Manager: 연결 수명·재연결·라우팅 중 무엇인지 이름이 말하지 않음
```

- 원인: KisExecutionWebSocketManager 는 연결 상태 추적·재연결 백오프 계산·승인키 폐기·수신 프레임 라우팅·로그 정리를 한꺼번에 하는데 Manager 라는 포괄적 접미가 그 의도를 가린다. 앞의 KisExecutionWebSocket 이 소속은 말하므로 심각도는 낮다.
- 생길 수 있는 일: 기능이 늘 때마다 «관리자»에 쌓이는 쓰레기통이 되기 쉽다.
- 해결: 역할로 나눠 이름 붙인다(예: KisExecutionWebSocketConnector, KisReconnectPolicy).

### 514위 [low · 확신 med] 모드 문자열("paper"/"live")과 성공 코드("0") 리터럴

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/kis/KisExecutionWebSocketManager.kt:144` · 항목표 backend/naming-terminology · 항목 N11

```
connectMode("paper") // ← 모드 문자열 리터럴
connectMode("live")
...
if (rtCode == null || rtCode == "0") return null // ← 성공 코드 리터럴
```

- 원인: start() 가 connectMode("paper")·connectMode("live") 리터럴로 연결을 열고, parseControlMessage 가 rtCode == "0" 을 리터럴로 비교한다(116행). KisOrderRestClient 의 같은 코드와 합쳐 흩어져 있다(CS24).
- 생길 수 있는 일: 오타가 조용한 분기 누락(연결 안 열림·성공을 오류로 판정)이 된다.
- 해결: 모드를 enum 으로 받고 KIS 응답 코드를 공용 상수로 둔다.

### 515위 [low · 확신 med] CONNECTING 상태 표시가 예외 때 풀리지 않음

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/kis/KisExecutionWebSocketManager.kt:155` · 항목표 reliability/load-concurrency · 항목 A5

```
statuses[mode] = KisWebSocketConnectionStatus.CONNECTING   // ← 이후 실패해도 되돌리지 않음
val request = KisExecutionWebSocketConnectionRequest(
    ...
    approvalKey = tokenManager.getWebSocketApprovalKey(mode),   // ← 여기서 던질 수 있음
```

- 원인: connectMode 가 먼저 상태를 CONNECTING 으로 올리고 승인키 발급을 부른다. 발급이 던지면 상태를 되돌리는 finally·catch 가 없다. 상태는 프로세스 메모리의 플래그일 뿐 다른 곳에서 중복 시도를 막는 데 쓰이지도 않는다.
- 생길 수 있는 일: 상태 조회가 영원히 CONNECTING 을 보여 오케스트레이션·운영자가 연결 단절을 알아채지 못한다.
- 해결: try/catch 로 실패 시 DISCONNECTED 로 되돌리고 재연결을 예약한다. A3 수정과 함께 처리한다.

고친 모양(예시):
```
val request = try { buildRequest(mode) } catch (e: Exception) { statuses[mode] = KisWebSocketConnectionStatus.DISCONNECTED; throw e }
```

### 516위 [low · 확신 med] WebSocket 전송 오류 로그에 예외 클래스 이름만 남음

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/kis/KisExecutionWebSocketManager.kt:190` · 항목표 reliability/failure-handling · 항목 E2

```
log.warn { "KIS WebSocket transport failure: mode=$mode, error=${error::class.simpleName}" }   // ← 원인 메시지·스택 없음
```

- 원인: handleTransportFailure 가 error::class.simpleName 만 기록하고 메시지와 스택을 버린다. 시크릿 노출을 피하려는 의도로 보이지만 원인(UnknownHost·SSL·핸드셰이크 거절)을 잃는다.
- 생길 수 있는 일: KIS 체결 통지 연결이 계속 끊길 때 이유를 로그로 알 수 없다.
- 해결: sanitizeControlMessage 를 거친 메시지와 함께 예외 객체를 넘긴다.

고친 모양(예시):
```
log.warn(error) { "KIS WebSocket transport failure: mode=$mode" }
```

### 517위 [low · 확신 med] WebSocket 연결 성공이 로그에 남지 않음

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/kis/KisExecutionWebSocketManager.kt:197` · 항목표 reliability/observability · 항목 O5

```
private fun handleConnected(mode: String) {
    reconnectAttempts[mode] = 0
    statuses[mode] = KisWebSocketConnectionStatus.CONNECTED   // ← 성공 로그 없음
}
```

- 원인: handleConnected 는 상태와 시도 횟수만 바꾸고 로그가 없다. 끊김·오류·소진은 로그하지만 연결 성공은 기록되지 않아, 로그만 봐서는 연결이 살아 있는지 알 수 없다.
- 생길 수 있는 일: 체결 통지 연결 상태를 로그로 확인할 수 없다.
- 해결: 연결 성공 시 mode 와 재시도 횟수를 INFO 로 남긴다.

고친 모양(예시):
```
log.info { "KIS WebSocket connected: mode=$mode, afterAttempts=${reconnectAttempts[mode]}" }
```

### 518위 [low · 확신 med] 외부 계좌번호 형식 오류 예외 메시지에 원문이 들어가고 실행기가 그대로 로그함

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/kis/KisOrderRestClient.kt:153` · 항목표 reliability/observability · 항목 O7

```
throw KisResponseParseException(
    "externalAccountId 형식이 잘못되었습니다. expected: XXXXXXXX-XX, actual: $raw (orderId=${order.id})"   // ← 계좌 번호 원문을 메시지에 포함
)
```

- 원인: 형식 오류일 때 던지는 KisResponseParseException 메시지에 actual: $raw(externalAccountId 원문)가 들어 있고, 실행기가 reason=${it.message} 로 ERROR 로그에 찍는다. 형식이 틀린 값이어도 실제 계좌 번호의 일부일 수 있다.
- 생길 수 있는 일: 계좌 번호 원문이 오류 로그에 남을 수 있다.
- 해결: actual 값은 길이와 형식 힌트만 남기거나 마스킹한다.

고친 모양(예시):
```
"... actual length=${raw.length}, parts=${parts.size} (orderId=${order.id})"
```

### 519위 [low · 확신 med] 공개 DTO 속성 이름 executedQty 가 허용 목록 밖 약어

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/kis/KisOrderRestClient.kt:188` · 항목표 backend/naming-terminology · 항목 N6

```
data class FillResult(
    val externalOrderId: String,
    val executedQty: BigDecimal, // ← qty 약어
    val executedPrice: BigDecimal,
```

- 원인: FillResult 의 공개 속성 executedQty 가 Qty 약어다. 다른 계층은 executedQuantity 를 쓴다. rtCd()·ccldTrId() 같은 비공개 함수 이름은 KIS 필드 이름(rt_cd·TR_ID)을 따른 것이라 외부 계약 이름으로 보고 통과시켰다.
- 생길 수 있는 일: 한 개념이 두 철자로 갈려 변환 코드에서 어긋난다.
- 해결: executedQuantity 로 바꾼다.

고친 모양(예시):
```
val executedQuantity: BigDecimal,
```

### 520위 [low · 확신 med] 주문 요청 경로의 Redis 호출에 타임아웃 설정이 없음

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/kis/KisTokenManager.kt:33` · 항목표 reliability/failure-handling · 항목 E5

```
redisTemplate.opsForValue().get(redisKey(mode))?.let { return it }   // ← 요청 경로의 Redis 호출
// application.yaml:30-33
  data:
    redis:
      host: ${REDIS_HOST:localhost}   // ← timeout·connect-timeout 설정 없음
```

- 원인: getToken 이 KIS 호출마다 redisTemplate 으로 토큰을 읽고, 주문 요청 경로 트랜잭션 안에서 불린다. application.yaml 의 spring.data.redis 에는 host·port 만 있고 timeout·connect-timeout 이 없다(내 묶음 밖 파일, 열어서 확인). Lettuce 기본값에 기대는 상태다. RestTemplate 호출은 RestTemplateConfig 의 5초·10초 타임아웃 빈을 쓰므로 통과.
- 생길 수 있는 일: Redis 가 멈추면 주문 요청 스레드가 계좌 행 락을 쥔 채 기본 제한 시간까지 대기한다.
- 해결: spring.data.redis.timeout·connect-timeout 을 명시한다(예: 2초·1초).

고친 모양(예시):
```
spring:
  data:
    redis:
      timeout: 2s
      connect-timeout: 1s
```

### 521위 [low · 확신 med] 리포지토리 위임 어댑터에 Service 접미가 붙었다

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/notification/NotificationPersistenceTxService.kt:11` · 항목표 backend/naming-terminology · 항목 N10

```
@Component
class NotificationPersistenceTxService(   // ← 리포지토리 위임 어댑터에 Service 접미
    private val repository: NotificationRepository,
```

- 원인: 이 클래스는 @Component 로 NotificationRepository 를 감싸 REQUIRES_NEW 트랜잭션만 거는 infrastructure 어댑터인데 이름이 Service 다. 유스케이스가 아니다. PTB8 에서 Service 접미는 유스케이스의 것이다.
- 생길 수 있는 일: ArchUnit·사람이 Service 이름을 유스케이스로 읽어 계층 판단이 엇나가고, 같은 이름이 application 서비스와 섞인다.
- 해결: NotificationDeliveryRecorder 처럼 역할 이름으로 바꾸거나, 유스케이스로 올린다면 application 의 @Service 로 옮긴다.

고친 모양(예시):
```
@Component
class NotificationDeliveryRecorder(private val repository: NotificationRepository)
```

### 522위 [low · 확신 med] 인자를 바꾸면서 그 인자를 돌려준다

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/notification/NotificationPersistenceTxService.kt:23` · 항목표 backend/code-shape · 항목 F3

```
fun markDelivering(notification: Notification): Notification? {
    val claimed = repository.claimForDelivery(notification.id ?: return null)
    if (claimed == 0) return null
    notification.markDelivering()   // ← 인자를 바꾸고
    return notification             // ← 그 인자를 그대로 돌려줌
```

- 원인: markDelivering 이 인자 notification 의 상태를 바꾸고 같은 객체를 반환한다(선점 실패면 null). 반환값이 선점 성공 여부 신호를 겸한다. 이름은 mark* 라 변경 동사 요건은 맞는다. 스케줄러가 반환된 claimedNotification 대신 원래 notification 을 catch 에서 쓰는 등 혼동 여지가 있다.
- 생길 수 있는 일: 호출자가 인자와 반환값을 같은 객체인 줄 모르고 둘을 따로 다루면 의도하지 않은 사본 수정이 생긴다. 지금은 같은 인스턴스라 무해하다.
- 해결: 선점 결과를 Boolean 으로 돌려주고 호출자가 같은 객체를 그대로 쓰게 한다.

고친 모양(예시):
```
fun claimForDelivery(notification: Notification): Boolean {
    val id = notification.id ?: return false
    if (repository.claimForDelivery(id) == 0) return false
    notification.markDelivering()
    return true
}
```

### 523위 [low · 확신 med] 리스너 경로는 claim 없이 발송해 스케줄러와 같은 행을 이중 발송할 수 있음

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/notification/SlackNotificationEventHandler.kt:29` · 항목표 reliability/load-concurrency · 항목 A6

```
val notification = txService.save(...)        // ← PENDING 행이 REQUIRES_NEW 로 즉시 커밋돼 스케줄러에 보임
val success = sender.send(event.message)      // ← claimForDelivery 없이 발송 — 이 사이 스케줄러가 같은 행을 claim·발송 가능
if (success) {
    txService.markDelivered(notification)     // ← merge 로 무조건 DELIVERED 덮어씀
```

- 원인: 핸들러는 PENDING 행을 REQUIRES_NEW 로 즉시 커밋한 뒤 claimForDelivery 없이 발송한다. 스케줄러는 attemptCount < maxRetries 인 PENDING 을 조회하므로 방금 만든 행도 대상이다. 핸들러가 Slack 응답을 기다리는 동안(최대 15초) 스케줄러 틱이 같은 행을 claim 해 또 보낼 수 있다. 핸들러의 markDelivered 는 엔티티를 merge 로 저장해 상태를 무조건 덮는다.
- 생길 수 있는 일: Slack 지연 시 같은 알림이 두 번 가고 최종 상태가 뒤섞인다. 금융 상태는 바뀌지 않아 영향은 중복 메시지다.
- 해결: 핸들러도 발송 전에 txService.markDelivering(notification) 로 claim 하고 null 이면 건너뛴다.

고친 모양(예시):
```
val claimed = txService.markDelivering(notification) ?: return
val success = sender.send(claimed.message)
```

### 524위 [low · 확신 med] enabled=true 인데 웹훅 URL 이 비어도 기동되고, 실패는 로그 없이 false 로만 처리

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/notification/SlackNotificationProperties.kt:6` · 항목표 reliability/security-config-hygiene · 항목 S4

```
// SlackNotificationProperties.kt:6-9
@ConfigurationProperties(prefix = "trading.notification.slack")   // ← @Validated·제약 없음
data class SlackNotificationProperties(
    val enabled: Boolean = false,
    val webhookUrl: String = "",     // ← enabled=true 여도 비어 있으면 기동됨
// SlackNotificationService.kt:31 (런타임 PUT 에서만 막음)
if (request.enabled && !current.webhookConfigured) {
// SlackWebhookNotifier.kt:17
if (properties.webhookUrl.isBlank()) return false   // ← 로그 한 줄 없이 실패 반환
```

- 원인: Properties 에 @Validated·제약이 없다. 런타임 PUT 은 SlackNotificationService.kt:31 에서 enabled 일 때 웹훅 설정을 요구하지만, 환경 변수로 시작하는 경로는 검증이 없다. webhookUrl 이 비면 Notifier 가 로그 한 줄 없이 false 를 돌려준다.
- 생길 수 있는 일: TRADING_NOTIFICATION_SLACK_ENABLED=true 만 켜고 SLACK_WEBHOOK_URL 을 빠뜨리면 기동은 성공하고 모든 알림이 이유 로그 없이 PENDING→FAILED 로 간다. 누락이 기동이 아니라 첫 운영 알림에서야 드러난다.
- 해결: Properties 를 @Validated 로 바꾸고 enabled 면 webhookUrl 이 비어 있지 않음을 시작 시 검증한다. 빈 URL 로 false 를 돌려줄 때는 WARN 한 줄을 남긴다.
- 다른 렌즈의 반증: backend/code-shape+layering+naming-terminology+tdd-purity — @ConfigurationProperties(prefix = trading.notification.slack) 의 설정 키 trading.notification.slack.enabled 에 바인딩되는 이름이다. 바꾸면 설정 키가 깨지므로 외부 계약으로 보고 통과.

고친 모양(예시):
```
@Validated
@ConfigurationProperties(prefix = "trading.notification.slack")
data class SlackNotificationProperties(...) {
    @AssertTrue(message = "enabled=true 이면 webhookUrl 필요")
    fun isWebhookPresentWhenEnabled() = !enabled || webhookUrl.isNotBlank()
}
```

### 525위 [low · 확신 med] 재시도가 30초 고정 간격이고 retryBackoffMillis 는 읽히지 않으며, 재시도 횟수 상한도 API 로 사실상 해제됨

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/notification/SlackNotificationScheduler.kt:17` · 항목표 reliability/failure-handling · 항목 E6

```
@Scheduled(fixedDelay = 30_000)   // ← 간격 고정·지터 없음. 정책의 retryBackoffMillis 는 이 묶음 어디서도 읽히지 않음
// SlackNotificationDtos.kt:36-37
@field:Min(1)
val maxRetries: Int,              // ← 상한(@Max) 없음
```

- 원인: 재시도 간격은 @Scheduled 상수 30초 고정에 지터가 없다. 정책의 retryBackoffMillis(기본 500, PUT 으로 변경 가능)는 이 묶음 어디에서도 소비되지 않는 죽은 설정이다. 영구 오류(4xx)도 Boolean false 로 합쳐져 재시도되고(E3), PUT 요청의 maxRetries 에는 상한이 없어(SlackNotificationDtos.kt:36-37) 큰 값을 넣으면 30초마다 무한에 가깝게 재시도한다.
- 생길 수 있는 일: Slack 장애 후 복구 순간 모든 인스턴스가 같은 주기로 몰려 발송하고, 폐기된 웹훅으로도 계속 재시도한다.
- 해결: retryBackoffMillis 를 실제로 쓰거나(다음 시도 시각 next_attempt_at 열과 지수 증가·지터) 설정에서 제거하고, maxRetries 에 @Max 를 둔다. 영구 오류는 재시도하지 않는다.

고친 모양(예시):
```
// Notification: nextAttemptAt = now + backoff * 2^attemptCount + jitter
// 조회: status = PENDING AND nextAttemptAt <= now
```

### 526위 [low · 확신 med] Slack 웹훅 호출의 소요 시간·결과가 기록되지 않음

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/notification/SlackWebhookNotifier.kt:19` · 항목표 reliability/observability · 항목 O5

```
return try {
    restTemplate.postForEntity(   // ← 소요 시간·성공 여부 기록 없음 (Timer·시작/끝 로그 모두 없음, 성공 시 로그 0줄)
```

- 원인: 성공 경로는 로그가 0줄이고 실패는 WARN 한 줄뿐이다. 시작·끝 로그, Micrometer Timer, RestTemplate 인터셉터 모두 없어 호출 지연을 잴 수 없다.
- 생길 수 있는 일: Slack 이 느려지는 중에도 지표가 없어, 알림 지연이 주문 스레드 지연(A3·C1)으로 번지기 전에 알아챌 수 없다.
- 해결: Timer 로 호출 시간을 재고 결과 태그(success·retryable·permanent)를 단다.

고친 모양(예시):
```
val sample = Timer.start(registry)
// ... 호출 ...
sample.stop(registry.timer("slack.webhook.call", "result", resultTag))
```

### 527위 [low · 확신 med] 설정·API 로 노출되는 Slack timeoutMillis 가 실제 호출에 연결되지 않음

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/notification/SlackWebhookNotifier.kt:20` · 항목표 reliability/failure-handling · 항목 E5

```
// SlackNotificationProperties.kt:10
val timeoutMillis: Long = 3000L,   // ← 설정·API 응답에는 3초로 노출되지만 호출부 어디서도 읽히지 않음
// SlackWebhookNotifier.kt:20
restTemplate.postForEntity(         // ← 공용 RestTemplate 사용 (RestTemplateConfig.kt:13-14 connectTimeout 5s·readTimeout 10s)
```

- 원인: timeoutMillis(기본 3000)는 Properties→ConfigStore→Result→Dtos 로 복사되어 API 응답에 나오지만, Notifier 는 공용 RestTemplate(RestTemplateConfig 의 connect 5초·read 10초)을 그대로 쓴다. 타임아웃 자체는 있어 무한 대기는 아니다. 보고하는 것은 설정값이 실제와 다르다는 점이다.
- 생길 수 있는 일: 운영자는 3초로 믿지만 최악 15초 동안 스레드가 묶인다. POST /test 는 요청 스레드에서 동기로, AFTER_COMMIT 핸들러는 CallerRuns 시 주문 스레드에서 이 시간을 쓴다.
- 해결: Slack 전용 RestTemplate 빈에 properties.timeoutMillis 를 적용하거나, 적용하지 않을 거면 timeoutMillis 를 설정·API 에서 제거한다.

고친 모양(예시):
```
@Bean("slackRestTemplate")
fun slackRestTemplate(builder: RestTemplateBuilder, props: SlackNotificationProperties) = builder
    .connectTimeout(Duration.ofMillis(props.timeoutMillis))
    .readTimeout(Duration.ofMillis(props.timeoutMillis))
    .build()
```

### 528위 [low · 확신 med] 실패 로그에 예외 객체와 알림 식별자가 없고, 스케줄러는 고정 문자열만 저장

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/notification/SlackWebhookNotifier.kt:27` · 항목표 reliability/failure-handling · 항목 E2

```
log.warn { "Slack webhook call failed: ${ex.message}" }   // ← 예외 객체(스택·cause)를 안 넘기고 알림 id·sourceRef 도 없음
// SlackNotificationScheduler.kt:35
txService.markFailed(claimedNotification, "Slack webhook returned failure", policy.maxRetries)   // ← 실패 사유 대신 고정 문자열만 저장
```

- 원인: log.warn 에 예외 객체를 넘기지 않고 ex.message 문자열만 쓰므로 스택·cause 가 사라진다. 메시지에 알림 id·sourceRef 도 없어 어떤 알림이 실패했는지 못 잇는다. 스케줄러의 실패 분기(35행)도 사유를 로그 없이 고정 문자열로 DB 에 저장한다.
- 생길 수 있는 일: 알림이 최종 FAILED 가 되어도 로그에서 어느 알림이 왜 실패했는지 되짚을 수 없다.
- 해결: send 가 실패 사유를 돌려주게 하고(E3 fix), 호출자가 알림 id·sourceRef 와 함께 로그·last_error 에 남긴다. 예외 객체는 URL 노출에 유의해 클래스·상태 코드만 남긴다(O7).
- 다른 렌즈의 반증: reliability/failure-handling+load-concurrency+observability+security-config-hygiene — WARN 로그가 있어 E1 은 아님. 같은 지점의 문제는 E3·E2·O7 로 보고

고친 모양(예시):
```
is SendResult.Retryable -> txService.markFailed(claimed, result.reason, policy.maxRetries)
// log.warn { "Slack send failed: id=${claimed.id} reason=${result.reason}" }
```

### 529위 [low · 확신 med] 상한 없는 원장 목록 메서드와 계좌 범위 없는 멱등키 조회 메서드(현재 호출 없음)

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/AccountLedgerRepository.kt:7` · 항목표 data/query · 항목 Q5

```
fun findByAccountIdOrderByCreatedAtDesc(accountId: Long): List<AccountLedger>   // ← 페이징·상한 없음, 원장은 계속 자람
fun existsByIdempotencyKey(idempotencyKey: String): Boolean   // ← account_id 없는 조회, (account_id, idempotency_key) 인덱스 선두 아님
```

- 원인: findByAccountIdOrderByCreatedAtDesc 는 페이징 없이 List 를 돌려준다. existsByIdempotencyKey 는 account_id 없이 idempotency_key 만 조회하는데 유일 인덱스는 (account_id, idempotency_key) 라 선두 열이 아니어서 인덱스를 못 탄다. 두 메서드 모두 src 전체에 호출처가 없어 지금은 잠복 상태다.
- 생길 수 있는 일: 누군가 호출하면 원장 전체를 읽거나 테이블 전체 스캔이 난다.
- 해결: 쓰지 않으면 삭제하고, 쓰려면 Pageable 과 accountId 범위를 붙인다.

### 530위 [low · 확신 med] 계정 전체 체결을 상한 없이 반환하는 파생 메서드(현재 호출처 없음)

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/ExecutionRepository.kt:10` · 항목표 data/query · 항목 Q5

```
fun findByAccountIdOrderByExecutedAtDesc(accountId: Long): List<Execution>   // ← 상한(Pageable·limit) 없음, main 호출처 없음
```

- 원인: main 소스에 호출처가 없는 잠재 결함입니다. 누군가 목록 화면에 붙이면 곧바로 무제한 조회가 됩니다.
- 생길 수 있는 일: 원장성 테이블(executions)은 계속 자라므로, 쓰이는 순간 계정 전체 체결을 통째로 당깁니다.
- 해결: 쓰지 않으면 삭제하고, 필요하면 Pageable/Slice 반환으로 바꿉니다.

### 531위 [low · 확신 med] 계정 전체 주문을 상한 없이 반환하는 파생 메서드(현재 호출처 없음)

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/OrderRepository.kt:23` · 항목표 data/query · 항목 Q5

```
fun findByAccountIdOrderByCreatedAtDesc(accountId: Long): List<Order>   // ← 상한 없음, main 호출처 없음
```

- 원인: main 소스에 호출처가 없는 잠재 결함입니다. findByAccountIdOrderByCreatedAtDesc 는 주문 이력 전체를 반환합니다.
- 생길 수 있는 일: 쓰이는 순간 주문 이력 전체를 메모리로 당깁니다.
- 해결: 쓰지 않으면 삭제하고, 필요하면 Pageable/Slice 로 바꿉니다.

### 532위 [low · 확신 med] 클래스 전체에서 쓰는 한 글자 속성 이름 o

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/OrderRepositoryImpl.kt:17` · 항목표 backend/naming-terminology · 항목 N6

```
private val o = QOrder.order   // ← 클래스 범위 한 글자 속성 이름
```

- 원인: private val o = QOrder.order 가 80여 줄의 모든 쿼리에서 쓰이는 클래스 범위 속성인데 이름이 한 글자다. CS19 는 지역 짧은 이름만 허용하고 이 이름은 함수 밖(속성)으로 나간다. ExecutionRepositoryImpl 은 QExecution.execution 을 정적 import 해 이름을 그대로 쓰는 것과 어긋난다.
- 생길 수 있는 일: o.account.tradingMode 같은 경로가 읽기 어렵고, 다른 쿼리 타입(예 Q 별칭)이 추가되면 o 가 무엇인지 헷갈린다.
- 해결: private val order = QOrder.order 로 바꾸거나 ExecutionRepositoryImpl 처럼 정적 import(QOrder.order)를 쓴다.

### 533위 [low · 확신 med] 일자별 스냅샷 조회가 파생 메서드와 Querydsl 두 곳에 있고 파생 쪽 둘은 호출처가 없다

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/PortfolioSnapshotRepository.kt:15` · 항목표 data/query · 항목 Q7

```
    fun findByAccountIdAndBusinessDateOrderByTickerAsc(   // ← 호출처 없음 — Querydsl searchByAccountIdAndBusinessDate 와 같은 조회
        accountId: Long,
        businessDate: LocalDate
    ): List<PortfolioSnapshot>
    fun findByAccountIdAndBusinessDateBetweenOrderByBusinessDateAsc(   // ← 호출처 없음

```

- 원인: findByAccountIdAndBusinessDateOrderByTickerAsc 는 PortfolioSnapshotRepositoryImpl.searchByAccountIdAndBusinessDate 와 필터·정렬이 같다. findByAccountIdAndBusinessDateBetweenOrderByBusinessDateAsc 는 호출처가 아예 없다. 호출처는 grep 으로 확인했다.
- 생길 수 있는 일: 필터가 바뀔 때 어느 사본이 정본인지 모호해지고 죽은 코드가 남는다.
- 해결: 쓰지 않는 파생 메서드를 지우거나, E2 발견의 일괄 읽기에 첫 번째 메서드를 쓰고 Querydsl 쪽을 지운다.

### 534위 [low · 확신 med] 상한 없는 리스크 이벤트 이력 목록 메서드(현재 호출 없음)

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/RiskEventRepository.kt:7` · 항목표 data/query · 항목 Q5

```
fun findByAccountIdOrderByTriggeredAtDesc(accountId: Long): List<RiskEvent>   // ← 페이징·상한 없음
```

- 원인: risk_events 는 append-only 로 계속 자라는데 findByAccountIdOrderByTriggeredAtDesc 가 List 를 상한 없이 돌려준다. src 전체에 호출처가 없다(RiskEvent.create 도 main 에서 호출처 없음).
- 생길 수 있는 일: 호출이 생기면 계좌의 이벤트 이력 전체를 한 번에 읽는다.
- 해결: 쓰지 않으면 삭제, 쓰려면 Pageable 을 받게 한다.

### 535위 [low · 확신 med] 계정 전체 정산을 상한 없이 반환하는 파생 메서드(현재 호출처 없음)

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/SettlementRepository.kt:9` · 항목표 data/query · 항목 Q5

```
fun findByAccountIdOrderBySettledAtDesc(accountId: Long): List<Settlement>   // ← 상한 없음, main 호출처 없음
```

- 원인: main 소스에 호출처가 없는 잠재 결함입니다.
- 생길 수 있는 일: 쓰이는 순간 정산 이력 전체를 메모리로 당깁니다.
- 해결: 쓰지 않으면 삭제하고, 필요하면 Pageable/Slice 로 바꿉니다.

### 536위 [low · 확신 med] 계좌·연도 단건 조회가 파생 메서드와 Querydsl 두 곳에서 따로 쓰인다

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/TaxSummaryRepository.kt:8` · 항목표 data/query · 항목 Q7

```
    fun findByAccountIdAndTaxYear(accountId: Long, taxYear: Int): Optional<TaxSummary>   // ← 사본 1 (Command 가 사용)
    fun findByAccountIdAndTaxYearBetweenOrderByTaxYearDesc(accountId: Long, fromYear: Int, toYear: Int): List<TaxSummary>   // ← 호출처 없는 사본
// TaxSummaryRepositoryImpl.kt:12  override fun findOneByAccountIdAndTaxYear(...)   // ← 사본 2 (Query 가 사용)
```

- 원인: findByAccountIdAndTaxYear 는 TaxSummaryCommandService L82 가, 같은 필터의 findOneByAccountIdAndTaxYear(Querydsl)는 TaxSummaryQueryService L27 이 쓴다. 연도 구간 조회도 findByAccountIdAndTaxYearBetweenOrderByTaxYearDesc(호출처 없음)와 searchByAccountIdAndTaxYearRange 가 중복이다.
- 생길 수 있는 일: 조회 조건이 바뀔 때 한쪽 사본만 고쳐져 조회와 재계산이 서로 다른 행을 볼 수 있다.
- 해결: 단건은 파생 메서드 하나로 통일하고 연도 구간은 한쪽만 남긴다.

### 537위 [low · 확신 med] 상한 없는 List 조회 메서드(호출처 없음)가 남아 있음

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/TradingJournalRepository.kt:9` · 항목표 data/query · 항목 Q5

```
fun findByAccountIdOrderByCreatedAtDesc(accountId: Long): List<TradingJournal>   // ← 상한 없는 목록. main·test 호출처 없음
fun findByAccountIdAndTickerOrderByCreatedAtDesc(accountId: Long, ticker: String): List<TradingJournal>   // ← 상한 없는 목록. 호출처 없음
```

- 원인: findByAccountIdOrderByCreatedAtDesc 와 findByAccountIdAndTickerOrderByCreatedAtDesc 는 Pageable·limit 없이 List 를 돌려준다(DA19). 일지는 기간이나 건수로 묶이지 않는 사용자 글이라 계좌 하나의 일지 전체(text 본문 포함)가 한 번에 올라온다. src/main·src/test 전체를 grep 해도 호출처가 없다. 쓰이는 조회는 search()(L25 호출)와 findById 뿐이다. 이 두 메서드와 Pageable 판 두 개(L11-12)는 search() 와 같은 필터를 중복해 가진 죽은 코드다.
- 생길 수 있는 일: 지금은 영향이 없다. 누군가 이 메서드를 호출하는 순간 한 요청이 한 계좌의 모든 일지를 메모리에 적재한다.
- 해결: 쓰이지 않는 파생 메서드 4개(L9-12)를 삭제하고, 일지 목록 조회는 상한이 있는 search() 하나로 둔다.

### 538위 [low · 확신 med] 계좌 생성 POST 에 멱등 키·자연 키 유일 제약이 없음

`backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/controller/AccountController.kt:43` · 항목표 reliability/failure-handling · 항목 I1

```
@PostMapping
@ResponseStatus(HttpStatus.CREATED)
fun createAccount(@Valid @RequestBody request: CreateAccountRequest): AccountResponse {  // ← 멱등 키 없음
    val account = accountCommandService.createAccount(CreateAccountCommand(...))
```

- 원인: createAccount 는 계좌와 초기 예수금 원장을 만드는데 Idempotency-Key 도 계좌명 유일 제약도 없다. 응답 유실 뒤 재시도나 이중 클릭이 계좌를 둘 만들고 각각 초기 입금 원장을 만든다. 입출금(deposit·withdraw)은 키가 있어 통과.
- 생길 수 있는 일: 같은 계좌가 중복 생성되고 모의 예수금이 두 배로 생긴다. KIS 모드에서는 같은 externalAccountId 계좌가 둘 생긴다.
- 해결: Idempotency-Key 헤더를 받거나 (tradingMode, externalAccountId) 유일 제약으로 중복을 막는다.

고친 모양(예시):
```
// 예: accounts 에 UNIQUE (trading_mode, external_account_id) WHERE external_account_id IS NOT NULL
```

### 539위 [low · 확신 med] 인증·소유 범위 없이 계좌 id 로 자동 청산 기본값 변경

`backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/controller/AccountExitTriggerDefaultController.kt:18` · 항목표 reliability/security-config-hygiene · 항목 S7

```
@PutMapping("/{accountId}/exit-trigger-default")
fun upsert(
    @PathVariable accountId: Long,  // ← 호출자 소유 검사 없음
    @Valid @RequestBody request: UpsertAccountExitTriggerDefaultRequest,
)
```

- 원인: 경로의 accountId 만으로 손절·익절 기본값을 PUT 한다. 호출자 범위 검사가 없다.
- 생길 수 있는 일: 누구나 임의 계좌의 자동 청산 조건을 바꾼다.
- 해결: 인증과 계좌 소유 범위 검사를 도입한다.

### 540위 [low · 확신 med] 인증·소유 범위 없이 계좌 id 로 리스크 정책 조회·교체

`backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/controller/RiskPolicyController.kt:25` · 항목표 reliability/security-config-hygiene · 항목 S7

```
@PostMapping
fun upsertRiskPolicy(
    @PathVariable accountId: Long,  // ← 호출자 소유 검사 없음
    @Valid @RequestBody request: UpsertRiskPolicyRequest
)
```

- 원인: 경로의 accountId 만으로 리스크 정책(최대 주문 금액·일 손실 한도)을 읽고 교체한다. 호출자 범위 검사가 없다.
- 생길 수 있는 일: 누구나 임의 계좌의 주문 한도를 풀어 과도한 주문이 가능해진다.
- 해결: 인증과 계좌 소유 범위 검사를 도입한다.

### 541위 [low · 확신 med] OFFSET 페이징에 page 상한이 없음

`backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/controller/TradingJournalController.kt:74` · 항목표 data/query · 항목 Q6

```
if (page < 0) throw InvalidPaginationException("page must be greater than or equal to 0")   // ← page 상한 검증이 없다
if (size !in 1..100) throw InvalidPaginationException("size must be between 1 and 100")
...
PageRequest.of(page, size, Sort.by(...))   // ← OFFSET = page*size 가 그대로 DB 로 간다
```

- 원인: 컨트롤러는 page 의 하한(>= 0)만 검증하고 상한이 없다. page*size 가 그대로 Querydsl 의 .offset(pageable.offset)(TradingJournalRepositoryImpl.kt:23)으로 간다. size 는 1..100 으로 제한돼 있다(L75). DA22 는 OFFSET 이면 페이지 상한을 요구한다. OFFSET 비용은 계좌의 일지 수(account_id, created_at 인덱스 범위)에 비례해 한정되므로 심각도를 낮췄다.
- 생길 수 있는 일: 임의로 큰 page 값이 매번 COUNT 쿼리와 OFFSET 스캔을 일으킨다. 일지 수가 늘수록 뒤 페이지가 느려지지만 영향은 한 계좌 범위에 그친다.
- 해결: page 에 현실적인 최댓값(예: 일지 수 상한/size)을 검증하거나, 일지가 깊어질 예정이면 (created_at, id) 키셋 페이징으로 바꾼다.

### 542위 [low · 확신 med] Boolean 속성 enabled 에 is 접두가 없다

`backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/dto/account/AccountExitTriggerDefaultDtos.kt:12` · 항목표 backend/naming-terminology · 항목 N3

```
data class UpsertAccountExitTriggerDefaultRequest(
    val enabled: Boolean,  // ← Boolean 인데 is 접두 없음(같은 패키지는 isActive)
```

- 원인: 요청 DTO(12행)와 응답 DTO(32행)의 Boolean 이 enabled 다. 같은 패키지의 AccountResponse.kt:24·RiskPolicyDto.kt:23 은 isActive 로 is 접두를 쓴다. 렌즈 N3 은 enabled 를 접두 없는 예로 든다. API 필드 이름이라 바꾸면 계약이 바뀌므로 사람이 정한다.
- 생길 수 있는 일: if (enabled) 가 명사처럼 읽히고, 한 API 안에서 활성 표현이 isActive 와 enabled 로 갈려 클라이언트·시험이 두 철자를 외워야 한다.
- 해결: 내부 이름을 isEnabled 로 바꾸고 JSON 필드 enabled 를 유지해야 하면 @JsonProperty("enabled") 를 단다. 프론트와 함께 isEnabled 로 맞추는 쪽이 더 일관된다.

고친 모양(예시):
```
data class UpsertAccountExitTriggerDefaultRequest(
    @field:JsonProperty("enabled") val isEnabled: Boolean,
    // …
)
```

### 543위 [low · 확신 med] 수량·지정가의 자릿수·부호 제한이 요청 경계에 없음 (limitPrice 는 검증 0개)

`backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/dto/order/OrderRequest.kt:19` · 항목표 reliability/security-config-hygiene · 항목 S8

```
@field:NotNull @field:DecimalMin("0.00000001") val quantity: BigDecimal,   // ← @Digits(integer, fraction) 없음
val limitPrice: BigDecimal?,   // LIMIT 주문 시 필수   // ← 부호·자릿수 검증 없음
```

- 원인: quantity 는 @DecimalMin 만 있고 @Digits 가 없습니다. limitPrice 는 @Positive·@DecimalMin·@Digits 가 모두 없어 음수·0·소수점 수십 자리가 도메인과 DB 까지 들어옵니다. 도메인 쪽 require 가 일부 막을 수 있으나(Order.kt:186 에 require 패턴이 있음) limitPrice 검증이 있는지는 열지 못했습니다.
- 생길 수 있는 일: 자릿수가 큰 값이 DB numeric 오버플로나 반올림 오류로 500 이 되고, 음수·0 가격이 도메인 검증에서 걸러지는지는 요청 경계에서 보장되지 않습니다.
- 해결: quantity 에 @Digits 를, limitPrice 에 @Positive 와 @Digits 를 추가합니다. 값 범위는 DB 열 정밀도에 맞춥니다.

고친 모양(예시):
```
@field:NotNull @field:DecimalMin("0.00000001") @field:Digits(integer = 12, fraction = 8) val quantity: BigDecimal,
@field:Positive @field:Digits(integer = 12, fraction = 4) val limitPrice: BigDecimal?,
```

### 544위 [low · 확신 med] idempotencyKey 에 길이 상한 검증이 없어 DB 열 길이(100) 초과 시 500 이 됨

`backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/dto/order/OrderRequest.kt:22` · 항목표 reliability/security-config-hygiene · 항목 S8

```
// OrderRequest.kt:22
@field:NotBlank val idempotencyKey: String,   // ← @Size(max = 100) 없음
// Order.kt:106 (grep 으로 읽은 줄)
@Column(name = "idempotency_key", nullable = false, length = 100)   // ← 열 길이 100
```

- 원인: 요청 모델은 @NotBlank 만 걸고, Order 엔티티의 idempotency_key 열은 length = 100 입니다. 길이를 넘는 키는 도메인 검증이 아니라 DB 오류로 터지고, 전역 핸들러의 최종 그물(Exception::class)로 떨어집니다.
- 생길 수 있는 일: 클라이언트의 입력 실수가 400 이 아닌 500 이 되어 5xx 오류 경보에 섞이고, 호출자는 무엇이 틀렸는지 알 수 없습니다.
- 해결: @field:Size(max = 100) 을 추가합니다.

고친 모양(예시):
```
@field:NotBlank @field:Size(max = 100) val idempotencyKey: String,
```

### 545위 [low · 확신 med] server.shutdown·tomcat 스레드 상한·종료 대기 시간이 설정되지 않음

`backend/trading-api/src/main/resources/application.yaml:60` · 항목표 reliability/load-concurrency · 항목 C6

```
server:
  port: 8080   # ← shutdown: graceful·threads.max·timeout-per-shutdown-phase 설정 없음
```

- 원인: hikari 풀 크기(10)와 connection-timeout 은 있으나 server.shutdown, server.tomcat.threads.max, spring.lifecycle.timeout-per-shutdown-phase 가 없다. server.shutdown 기본값은 즉시 종료다.
- 생길 수 있는 일: 배포·재시작 때 처리 중이던 요청과 매칭이 끊기고, 스레드 상한이 DB 풀 크기와 무관하게 기본값으로 정해진다.
- 해결: server.shutdown: graceful 과 spring.lifecycle.timeout-per-shutdown-phase 를 두고 tomcat threads.max 를 DB 풀에 맞춰 명시한다.

고친 모양(예시):
```
server:
  port: 8080
  shutdown: graceful
spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s
```

### 546위 [low · 확신 med] 구조 변경(ADD COLUMN)과 데이터 백필(UPDATE)이 한 파일에 섞임

`backend/trading-api/src/main/resources/db/migration/V10__add_transaction_tax_rate_to_fee_policies.sql:1` · 항목표 data/schema-data-model · 항목 M9

```sql
ALTER TABLE fee_policies ADD COLUMN transaction_tax_rate NUMERIC(10,6) NOT NULL DEFAULT 0;
...
UPDATE fee_policies   -- ← 같은 파일에서 구조 변경 뒤에 백필(SM12)
SET transaction_tax_rate = 0.001800
WHERE trading_mode = 'LOCAL' AND market_type IN ('KOSPI', 'KOSDAQ');
```

- 원인: 같은 파일에서 ALTER TABLE 로 열을 추가한 뒤 UPDATE 세 건으로 값을 채운다(SM12). 각 UPDATE 에는 근거 주석이 있고 데이터를 지우지는 않는다. 두 번째·세 번째 UPDATE 는 값이 새 열의 DEFAULT 0 과 같아 불필요하다. fee_policies 는 몇 행짜리 설정 테이블이고 NOT NULL DEFAULT 상수 ADD COLUMN 은 PG11+ 에서 메타데이터 변경이라, 실제 락 비용은 작다.
- 생길 수 있는 일: 큰 테이블에서 이 패턴을 따라 하면 DDL 락 안에서 백필이 돈다. 지금 테이블에서는 실질 피해가 없다.
- 해결: 이미 적용된 V10 은 고치지 않는다(SM18). 앞으로는 열 추가와 백필을 별도 마이그레이션으로 나누고, DEFAULT 와 같은 값을 쓰는 UPDATE 는 쓰지 않는다.

### 547위 [low · 확신 med] 정산 테이블을 근거 주석·보관 단계 없이 CASCADE 로 삭제

`backend/trading-api/src/main/resources/db/migration/V9__drop_receivable_settlements.sql:1` · 항목표 data/schema-data-model · 항목 M9

```sql
DROP TABLE IF EXISTS receivable_settlements CASCADE;  -- ← 근거 주석 없음 · CASCADE 로 종속 객체까지 삭제 · 보관 단계 없음
```

- 원인: V9 은 receivable_settlements(미수금 정산 이력) 테이블을 파일 안에 삭제 근거 주석도, 보관(아카이브) 단계도 없이 지운다. 게다가 CASCADE 라서 이 테이블에 걸린 FK·뷰 같은 종속 객체가 알림 없이 같이 사라진다. 대응 코드 삭제는 같은 커밋(b408236)에 있어 의도된 삭제임은 확인되므로 렌즈 규정대로 low 로 둔다. 다만 마이그레이션 파일만 보는 사람은 왜 지웠는지, 데이터를 보관했는지 알 수 없다.
- 생길 수 있는 일: 운영 DB 에서 실행되면 정산 이력(금융 상태 변경의 감사 흔적)이 복구 수단 없이 사라지고, 모르는 종속 객체도 함께 삭제된다. 나중에 감사·대사 요청이 와도 원본이 없다.
- 해결: 파일 첫 줄에 삭제 근거(b408236 에서 ReceivableSettlement 계열 코드 삭제)를 주석으로 남긴다. 보관이 필요하면 삭제 전에 아카이브 테이블로 복사한다. 종속 객체가 없음이 확인되면 CASCADE 를 뺀다. 이미 적용된 마이그레이션이라 파일 수정은 체크섬을 바꾸므로, 새 마이그레이션이나 문서로 근거를 남기는 쪽이 안전하다.

고친 모양(예시):
```sql
-- receivable_settlements 는 b408236 에서 관련 코드와 함께 제거(미수금 정산 폐지)
-- 보관 필요 시:
-- CREATE TABLE receivable_settlements_archive AS TABLE receivable_settlements;
DROP TABLE IF EXISTS receivable_settlements;
```

### 548위 [low · 확신 med] 시험 이름 4개가 행동만 말하고 상황·기대가 없음

`backend/trading-api/src/test/kotlin/com/papertrading/api/application/account/AccountQueryServiceTest.kt:55` · 항목표 backend/tdd-purity · 항목 T6

```
    fun `전체_계좌_목록을_조회한다`() {      // ← 상황·기대 없음
    fun `활성_계좌만_필터링하여_조회한다`() {      // ← 68줄
    fun `거래모드로_필터링하여_계좌를_조회한다`() {      // ← 80줄
    fun `원장을_필터_조건으로_페이징_조회한다`() {      // ← 92줄
```

- 원인: «전체 계좌 목록을 조회한다»·«활성 계좌만 필터링하여 조회한다»·«거래모드로 필터링하여 계좌를 조회한다»·«원장을 필터 조건으로 페이징 조회한다» 는 어떤 입력에서 무엇이 나와야 하는지 말하지 않는다.
- 생길 수 있는 일: 빨간 시험이 어떤 정책이 깨졌는지 알려 주지 못해 진단에 코드를 열어야 한다.
- 해결: 상황_행동_기대 세 토막으로 바꾼다. 예: 필터가_없으면_전체_계좌를_반환한다.

### 549위 [low · 확신 med] 시험 이름이 상황·기대 없이 행동만 말함

`backend/trading-api/src/test/kotlin/com/papertrading/api/application/account/KisAccountQueryServiceTest.kt:75` · 항목표 backend/tdd-purity · 항목 T6

```
    fun `정합성 mismatch를 계산한다`() {      // ← 상황·기대 없음
```

- 원인: «정합성 mismatch를 계산한다» 는 KIS 와 로컬 수량이 어떻게 다를 때 무엇이 나와야 하는지 이름에 없다. 실제 단언은 quantityMismatch·missingInKis·missingInLocal 세 가지다.
- 생길 수 있는 일: 세 목록 중 하나만 깨져도 같은 이름의 빨간 시험으로 보여 어떤 정책이 깨졌는지 모른다.
- 해결: KIS_와_로컬_수량이_다르면_수량불일치_목록에_담는다 처럼 상황·기대를 적고 단언을 시험 하나에 하나씩 나눈다.

### 550위 [low · 확신 med] 내부 알림 발행 서비스(SlackNotificationEventPublisher)를 목으로 갈음

`backend/trading-api/src/test/kotlin/com/papertrading/api/application/order/ExecutionProcessorTest.kt:55` · 항목표 backend/tdd-purity · 항목 T1

```
private val slackNotificationEventPublisher = mockk<SlackNotificationEventPublisher>().also { // ← 내부 서비스 목
    justRun { it.publishExecutionFilled(any(), any(), any(), any(), any()) }
}
```

- 원인: application.notification 의 서비스를 목으로 두고 justRun 만 건다. 외부 경계 허용 목록에는 infrastructure.notification.SlackWebhookNotifier 가 있고 이 클래스는 아니다. 영향이 작아 심각도는 낮다.
- 생길 수 있는 일: 알림 발행 시그니처 변경이 시험에서 드러나지 않는다(영향 작음).
- 해결: 알림 출력 포트의 가짜(수집형)로 바꾸고 마지막 호출 내용을 단언한다.

### 551위 [low · 확신 med] 시험 이름이 «정산 생성 없음» 인데 본문은 정산 저장 1회를 단언

`backend/trading-api/src/test/kotlin/com/papertrading/api/application/order/ExecutionProcessorTest.kt:147` · 항목표 backend/tdd-purity · 항목 T6

```
fun `KIS_LIVE SELL — availableDeposit 불변, no receivable settlement creation`() { // ← 이름: 정산 생성 없음
    ...
    verify(exactly = 1) { settlementRepository.save(any()) } // ← 본문: 정산 1회 저장
```

- 원인: KIS_LIVE·KIS_PAPER 시험 이름은 no receivable settlement creation 이지만 본문은 verify(exactly = 1) { settlementRepository.save(any()) } 로 정산이 한 번 저장됨을 단언한다(171행도 같음). 이름과 단언이 반대를 말한다(TT21).
- 생길 수 있는 일: 빨간 시험이 어떤 정책이 깨졌는지 말하지 못하고, 이름만 믿고 정산이 생성되지 않는다고 오해한다.
- 해결: 이름을 실제 단언에 맞춘다(예: «KIS_LIVE 매도는 예수금을 바꾸지 않고 정산을 1건 만든다»).

### 552위 [low · 확신 med] 예수금 불변 단언이 assertEquals 로 scale 까지 비교

`backend/trading-api/src/test/kotlin/com/papertrading/api/application/order/ExecutionProcessorTest.kt:165` · 항목표 backend/tdd-purity · 항목 T8

```
assertEquals(initialDeposit, account.availableDeposit) // ← equals — scale 민감
```

- 원인: assertEquals(initialDeposit, account.availableDeposit) 는 equals 라, 구현이 scale 을 바꿔 같은 값을 만들면(예: 1000000 → 1000000.0000) 값이 불변인데도 빨강이 된다(188행 동일). 같은 파일 137~143행은 compareTo 를 쓴다.
- 생길 수 있는 일: scale 정규화 변경이 가짜 실패를 만들고 진짜 금액 변화는 구분되지 않는다.
- 해결: 137행처럼 compareTo 로 비교한다.

고친 모양(예시):
```
assertEquals(0, initialDeposit.compareTo(account.availableDeposit))
```

### 553위 [low · 확신 med] 시험 이름이 구현(리포지토리 사용)을 말하고 정책을 말하지 않음

`backend/trading-api/src/test/kotlin/com/papertrading/api/application/order/OrderQueryServiceTest.kt:43` · 항목표 backend/tdd-purity · 항목 T6

```
fun `listOrders uses repository search`() { // ← 구현을 서술, 정책(조건에 맞는 주문만 반환)이 없음
```

- 원인: listOrders uses repository search·listExecutions uses repository search 는 상황·기대 없이 구현만 서술한다(TT21).
- 생길 수 있는 일: 실패 시 어떤 정책이 깨졌는지 이름에서 알 수 없다.
- 해결: 정책 문장으로 바꾼다(예: «조건에 맞는 주문 목록을 결과 타입으로 변환해 돌려준다»).

### 554위 [low · 확신 med] 시험 하나에 단언 10개 — DB 불변과 캐시 갱신 두 행동이 한 시험에

`backend/trading-api/src/test/kotlin/com/papertrading/api/application/position/PositionCommandServiceTest.kt:104` · 항목표 backend/tdd-purity · 항목 T12

```
assertThat(dbPosition.currentPrice).isNull()  // ← DB 불변 3개
...
assertThat(cached.returnRate).isEqualByComparingTo("0.0714")  // ← 캐시 갱신 7개, 합계 10개 단언
```

- 원인: updateCurrentPriceByTicker_updates_live_position_cache... 가 DB 가격 필드 null 3개와 캐시 필드 7개(currentPrice·priceSource·evaluationAmount·unrealizedPnl·returnRate·priceUpdatedAt·notNull)를 한 시험에서 단언한다.
- 생길 수 있는 일: 첫 실패가 나머지를 가리고, 이름 하나로 두 행동을 설명하지 못해 진단 비용이 늘어난다.
- 해결: «DB 가격 필드는 바뀌지 않는다» 와 «캐시에 평가금액·손익이 반영된다» 두 시험으로 나눈다.

고친 모양(예시):
```
@Test fun `시세 갱신은 DB 가격 필드를 바꾸지 않는다`() { ... }
@Test fun `시세 갱신은 캐시에 평가금액과 손익을 기록한다`() { ... }
```

### 555위 [low · 확신 med] 시험의 유일한 단언이 verify 호출 검증 — 결과 단언 없음

`backend/trading-api/src/test/kotlin/com/papertrading/api/application/position/PositionExitQuoteEventListenerTest.kt:18` · 항목표 backend/tdd-purity · 항목 T3

```
verify(exactly = 1) { orchestrator.onQuote("005930", BigDecimal("100"), now) }  // ← 결과 단언 없이 호출 횟수·인자만 검증
```

- 원인: verify(exactly = 1) { orchestrator.onQuote(...) } 가 이 시험의 전부다. 호출 횟수·인자만 보고 결과 상태를 단언하지 않는다.
- 생길 수 있는 일: 오케스트레이터가 인자를 어떻게 쓰든 초록이라, 리스너 연결이 실제 트리거 발동으로 이어지는지 확인하지 못한다.
- 해결: T1 과 같은 처방. 가짜 저장소를 쓰는 실제 오케스트레이터로 트리거 상태 변화를 단언한다.

고친 모양(예시):
```
assertEquals(TriggerState.TRIGGERED, armedTrigger.state)
```

### 556위 [low · 확신 med] 한 시험이 evaluate 를 두 번 호출하고 각자 단언 — 경계값과 경계 밖이 한 시험에

`backend/trading-api/src/test/kotlin/com/papertrading/api/application/position/PositionExitTriggerEvaluatorTest.kt:27` · 항목표 backend/tdd-purity · 항목 T12

```
val equality = evaluator.evaluate(position, trigger, BigDecimal("95.0000"), quoteAt)
val below = evaluator.evaluate(position, trigger, BigDecimal("94.9999"), quoteAt)  // ← 호출 2번에 각자 단언
```

- 원인: stop loss fires… 시험(27-28행)과 take profit fires… 시험(48-49행)이 같은값(equality)과 한 틱 밖(below·above)을 각각 호출해 단언한다.
- 생길 수 있는 일: 경계 비교(<=)가 틀리면 첫 단언만 빨개져 두 번째 경계가 가려진다.
- 해결: @ParameterizedTest(입력값: 95.0000, 94.9999)로 바꾼다.

고친 모양(예시):
```
@ParameterizedTest @ValueSource(strings = ["95.0000", "94.9999"])
fun `stop loss fires at or below trigger price`(quote: String) { ... }
```

### 557위 [low · 확신 med] exitRatioPercent 를 assertEquals 로 scale 까지 비교하는데 같은 파일 다른 단언은 compareTo

`backend/trading-api/src/test/kotlin/com/papertrading/api/application/position/PositionExitTriggerEvaluatorTest.kt:82` · 항목표 backend/tdd-purity · 항목 T8

```
assertEquals(0, BigDecimal("108.00000000").compareTo(decision?.effectiveTriggerPrice))  // ← 81행은 compareTo
assertEquals(BigDecimal("40.0000"), decision?.exitRatioPercent)  // ← 82행은 scale 까지 비교
```

- 원인: 31·52·81행은 compareTo 로 비교하면서 82행만 assertEquals(BigDecimal("40.0000"), ...) 로 scale 까지 비교한다. 입력값을 그대로 통과시키는 값이라 현재 위험은 낮지만 비교 방식이 파일 안에서 섞여 있다.
- 생길 수 있는 일: 트리거 값 정규화가 바뀌면 값이 맞아도 깨진다.
- 해결: compareTo 기반 단언으로 통일한다.

고친 모양(예시):
```
assertEquals(0, BigDecimal("40.0000").compareTo(decision?.exitRatioPercent))
```

### 558위 [low · 확신 med] 주문 생성 결과를 createGroupedAutoExitSellOrder 호출 횟수 verify 로 단언

`backend/trading-api/src/test/kotlin/com/papertrading/api/application/position/PositionExitTriggerOrchestratorTest.kt:67` · 항목표 backend/tdd-purity · 항목 T3

```
verify(exactly = 2) {
    autoExitOrderPlacementService.createGroupedAutoExitSellOrder(any(), any(), any(), any(), any(), any())  // ← 호출 횟수를 결과로 단언
}
```

- 원인: 주문이 몇 번 만들어졌는지(exactly 1·2·0)를 any() 6개짜리 verify 로 단언한다(67·90·138·157행). 결과 상태 단언도 함께 있어 «대신» 정도는 아니라 심각도를 낮춘다.
- 생길 수 있는 일: 인자 순서·개수가 바뀌면 시험이 깨지고, 반대로 올바른 주문이 만들어졌는지(수량·그룹 id)는 슬롯 캡처 한두 시험에만 의존한다.
- 해결: T1 처방과 같다. 기록용 가짜가 만든 주문 목록의 크기·수량을 단언한다.

고친 모양(예시):
```
assertEquals(2, fakeOrderPort.created.size)
```

### 559위 [low · 확신 med] 시험 하나가 대상 호출 2번 이상에 각각 단언을 달고 있음

`backend/trading-api/src/test/kotlin/com/papertrading/api/domain/entity/account/AccountTest.kt:72` · 항목표 backend/tdd-purity · 항목 T12

```
assertThrows(IllegalArgumentException::class.java) {
    account.addDeposit(BigDecimal.ZERO) // ← 호출 1
}
assertThrows(IllegalArgumentException::class.java) {
    account.addDeposit(BigDecimal("-1")) // ← 호출 2, 단언 따로
}
```

- 원인: 입금_금액이_0_이하면_예외를_던진다·출금_금액이_0_이하면_예외를_던진다 는 경계값 두 개를 한 시험에서 확인하고, createRiskPolicy_maxPositionRatio_경계값_0과_1은_유효하다 도 호출 두 번이며, 불변식_… 시험은 6개 연산마다 assertInvariant 를 단다.
- 생길 수 있는 일: 첫 실패가 나머지를 가리고 이름이 두 행동을 담지 못한다.
- 해결: @ParameterizedTest 와 @ValueSource(strings=["0","-1"]) 로 경계값별 시험으로 나눈다. 불변식 시나리오는 연산별 시험으로 쪼개거나 이름을 시나리오로 명시한다.

### 560위 [low · 확신 med] 시험 이름 약 9개가 상황 없이 «대상 메서드_기대»만 말함

`backend/trading-api/src/test/kotlin/com/papertrading/api/domain/entity/account/AccountTest.kt:113` · 항목표 backend/tdd-purity · 항목 T6

```
fun recordDeposit_입금과_원장을_동시에_반환한다() { // ← 상황 토막 없음
fun rename_계좌명을_변경한다() { // ← 상황 토막 없음
fun recordFee_수수료_원장을_반환한다() {
fun recordSettlement_정산입금과_원장을_동시에_반환한다() { // ← 부르는 메서드는 recordDepositWithLedger
```

- 원인: recordDeposit_입금과_원장을_동시에_반환한다·recordWithdrawal_…·recordInitialDeposit_…·rename_계좌명을_변경한다·recordFee_수수료_원장을_반환한다 등은 어떤 상황에서인지 빠져 있다. recordSettlement_ 접두는 실제로 부르는 recordDepositWithLedger 와도 이름이 어긋난다. 같은 파일의 앞쪽 시험은 상황_행동_기대를 잘 지킨다.
- 생길 수 있는 일: 빨간 시험이 어떤 정책이 깨졌는지 이름만으로 말하지 못해 진단 비용이 든다.
- 해결: «가용예수금이 충분할 때 입금하면 원장이 DEPOSIT 으로 남는다» 꼴로 상황을 넣는다.

### 561위 [low · 확신 med] 시험 하나가 같은 대상을 2번 호출하고 각각 단언 (2곳)

`backend/trading-api/src/test/kotlin/com/papertrading/api/domain/entity/feepolicy/FeePolicyTest.kt:52` · 항목표 backend/tdd-purity · 항목 T12

```
        val p0 = createPolicy(feeRate = BigDecimal.ZERO)           // ← 호출 1
        val p1 = createPolicy(feeRate = BigDecimal.ONE)            // ← 호출 2

        assertEquals(0, p0.feeRate.compareTo(BigDecimal.ZERO))
        assertEquals(0, p1.feeRate.compareTo(BigDecimal.ONE))
```

- 원인: create_feeRate_경계값_0과_1은_유효하다(L52-58)는 create 를 2번, isActiveAt_만료일시_이후이면_비활성으로_판단한다(L105-112)는 isActiveAt 을 2번 호출해 각자 단언한다.
- 생길 수 있는 일: 첫 단언이 실패하면 둘째 경계값이 가려지고, 이름이 두 행동을 담지 못한다.
- 해결: 경계값마다 시험을 나누거나 @ParameterizedTest 로 바꾼다.

### 562위 [low · 확신 med] 시험 하나가 단언 9개를 단다

`backend/trading-api/src/test/kotlin/com/papertrading/api/domain/entity/notification/NotificationTest.kt:24` · 항목표 backend/tdd-purity · 항목 T12

```
fun `create_정상_생성되면_초기_상태가_올바르게_설정된다`() {
    val notification = makeNotification()

    assertEquals(NotificationEventType.EXECUTION_FILLED, notification.eventType)   // ← 단언 1
    assertEquals("execution:1", notification.sourceRef)
    // … 단언 7개 더 …
    assertNull(notification.deliveredAt)   // ← 단언 9
```

- 원인: create 정상 시험이 필드 9개(eventType·sourceRef·title·message·status·attemptCount·isRead·lastError·deliveredAt)를 한 시험에서 확인한다. 렌즈 기준(단언 8개 이상)에 걸린다. 한 행동(초기 상태)이라는 반론은 있으나 첫 실패가 나머지를 가린다.
- 생길 수 있는 일: 단언 하나가 깨지면 나머지 필드 검증이 실행되지 않아 진단이 느려지고, 이름이 9개 기대를 다 담지 못한다.
- 해결: 입력 값 보존과 초기 전송 상태(status·attemptCount·lastError·deliveredAt·isRead)로 시험을 둘로 나눈다. assertEquals(false, …) 는 assertFalse 로 바꾼다.

고친 모양(예시):
```
@Test
fun `create_정상_생성되면_전송_상태는_PENDING이고_시도_이력이_없다`() {
    val n = makeNotification()
    assertEquals(DeliveryStatus.PENDING, n.status)
    assertEquals(0, n.attemptCount)
}
```

### 563위 [low · 확신 med] isCancellable 시험 하나가 3번 호출하고 각각 단언

`backend/trading-api/src/test/kotlin/com/papertrading/api/domain/entity/order/OrderTest.kt:220` · 항목표 backend/tdd-purity · 항목 T12

```
        assertTrue(order.isCancellable())                          // ← 호출 1
        order.applyExecution(BigDecimal("3"), BigDecimal("70000"), BigDecimal("105"))
        assertTrue(order.isCancellable())                          // ← 호출 2
        order.cancel()
        assertFalse(order.isCancellable())                         // ← 호출 3
```

- 원인: 한 시험이 PENDING·PARTIAL·CANCELLED 세 상태에서 isCancellable()을 부르고 각자 단언한다(L223·226·229).
- 생길 수 있는 일: 첫 실패가 나머지를 가리고 이름(PENDING과 PARTIAL만 true)이 CANCELLED 의 false 를 담지 못한다.
- 해결: 상태별로 세 시험(PENDING_true·PARTIAL_true·CANCELLED_false)으로 나눈다.

### 564위 [low · 확신 med] BigDecimal 단언이 assertEquals 로 scale 까지 비교하며, 정규화 시험은 이름에 드러나지 않음

`backend/trading-api/src/test/kotlin/com/papertrading/api/domain/entity/portfolio/TaxSummaryTest.kt:27` · 항목표 backend/tdd-purity · 항목 T8

```
        assertEquals(BigDecimal("1000000.0000"), summary.totalRealizedPnl)   // ← scale 까지 비교, 입력도 scale 4 라 정규화는 검증되지 않음
        assertEquals(BigDecimal("0.0000"), summary.estimatedTax)   // ← L57 ZERO → 0.0000 정규화를 이름 없이 단언
```

- 원인: L27~L29·L42~L44·L57·L81 이 assertEquals(BigDecimal(…), …) 로 scale 까지 비교한다(PTB4 는 compareTo 비교). 입력이 이미 scale 4 인 시험(L27 등)은 값 비교와 scale 비교가 구분되지 않는다. L57 은 BigDecimal.ZERO(scale 0)를 넣고 "0.0000" 을 기대해 scale 정규화를 검증하지만 시험 이름은 «정상 처리» 이다.
- 생길 수 있는 일: setScale 정규화를 빼도 L27 류는 그대로 통과해 정규화가 시험으로 보호되지 않고, 값만 같으면 되는 시험이 scale 이 달라지는 리팩터링에 빨개질 수 있다.
- 해결: 값 시험은 assertEquals(0, expected.compareTo(actual)) 또는 isEqualByComparingTo 로 바꾸고, scale 정규화는 «scale_이_다른_값을_넣으면_scale_4_로_정규화된다» 이름의 시험(입력 BigDecimal("1"))을 따로 둔다.

### 565위 [low · 확신 med] 시험 이름이 «대상 메서드 + 정상»이라 어떤 정책을 지키는지 말하지 않는다

`backend/trading-api/src/test/kotlin/com/papertrading/api/domain/entity/position/PositionTest.kt:34` · 항목표 backend/tdd-purity · 항목 T6

```
fun `lockQuantity 정상 잠금`() {  // ← 정상 + 메서드 이름
...
fun `unlockQuantity 정상 해제`() {  // ← 정상 + 메서드 이름
```

- 원인: lockQuantity 정상 잠금(L34)과 unlockQuantity 정상 해제(L55)는 상황·행동·기대 세 토막이 아니다. 한 파일 안에서 한글 백틱 이름과 영어 snake_case 이름(L89 이후)이 섞여 있다.
- 생길 수 있는 일: 빨간 시험이 «주문 가능 수량이 잠금만큼 줄어든다» 중 무엇이 깨졌는지 말하지 못해 진단 비용이 든다.
- 해결: `주문 가능 수량 10 에서 3 을 잠그면 잠금 3 주문 가능 7 이 된다` 처럼 정책을 문장으로 쓴다.

### 566위 [low · 확신 med] 시험 하나가 대상 메서드를 두 번 호출하고 각각 단언한다

`backend/trading-api/src/test/kotlin/com/papertrading/api/domain/entity/position/PositionTest.kt:47` · 항목표 backend/tdd-purity · 항목 T12

```
fun `lockQuantity 0 이하 시 예외`() {
    val p = position(qty = BigDecimal("10"))
    assertThrows<IllegalArgumentException> { p.lockQuantity(BigDecimal.ZERO) }  // ← 호출 1
    assertThrows<IllegalArgumentException> { p.lockQuantity(BigDecimal("-1")) }  // ← 호출 2
```

- 원인: `lockQuantity 0 이하 시 예외` 가 lockQuantity(0) 과 lockQuantity(-1) 을 연달아 각각 assertThrows 한다. 경계 값 둘이라 영향은 작다.
- 생길 수 있는 일: 첫 단언이 실패하면 둘째 경계(음수)는 실행되지 않아 어느 경계가 틀렸는지 가려진다.
- 해결: @ParameterizedTest 로 0 과 -1 을 나누거나 시험을 둘로 쪼갠다.

### 567위 [low · 확신 med] 시험 이름이 상황·행동·기대를 말하지 않음 (flow_works·validates_*)

`backend/trading-api/src/test/kotlin/com/papertrading/api/domain/entity/strategy/StrategyDomainEntitiesTest.kt:29` · 항목표 backend/tdd-purity · 항목 T6

```
fun strategy_review_approve_activate_flow_works() {   // ← «works» — 기대 결과가 없음
...
fun strategy_derivation_validates_parent_child_and_type() {   // ← «validates» — 입력·기대 없음 (L220, 비슷: L245·L273·L336)
```

- 원인: strategy_review_approve_activate_flow_works 는 렌즈가 지목한 «works» 꼴이고, strategy_derivation_validates_parent_child_and_type·strategy_log_validates_level_message_and_future_time·performance_snapshot_validates_period_and_ranges·strategy_version_validates_fields_and_note_updates 는 «validates …» 만 있고 어떤 입력이 어떤 결과를 내는지 이름에 없다.
- 생길 수 있는 일: 빨간 시험이 어떤 정책이 깨졌는지 말하지 못해 원인 파악에 본문을 읽어야 한다.
- 해결: 상황_행동_기대로 쪼갠 이름(한글 백틱 허용)으로 바꾸고 시험을 행동 단위로 나눈다.

고친 모양(예시):
```
@Test fun `검토 요청 후 승인하고 활성화하면 APPROVED 이고 활성 상태다`() { ... }
```

### 568위 [low · 확신 med] 시험 하나에 단언 12개, 다른 시험은 호출 3번에 각자 단언

`backend/trading-api/src/test/kotlin/com/papertrading/api/infrastructure/kis/KisExecutionNoticeParserTest.kt:15` · 항목표 backend/tdd-purity · 항목 T12

```
        assertEquals(TradingMode.KIS_PAPER, notice.mode)  // ← 한 parse 결과에 단언 12개(L23~L34)
        assertEquals("12345678-01", notice.accountScope)
        assertNull(parser.parse("0|H0STCNT0|001|MKSC_SHRN_ISCD=005930^STCK_PRPR=70000", "paper"))  // ← 호출마다 각자 단언(L54~L56)
        assertNull(parser.parse("not-a-kis-message", "paper"))
```

- 원인: 첫 시험은 parse 한 번에 assertEquals 12개(L23~L34)를 달고, 세 번째 시험(L53)은 parse 를 3번 부르며 호출마다 assertNull 을 단다. 첫 실패가 나머지를 가리고 이름이 여러 행동을 담지 못한다.
- 생길 수 있는 일: 앞 단언이 깨지면 뒤 필드의 회귀가 보이지 않고, 어떤 정책이 깨졌는지 이름으로 알 수 없다.
- 해결: 필드 묶음(식별자·수량가격·계좌범위)별 시험으로 나누고, 무시 케이스는 @ParameterizedTest 로 한 입력당 한 시험으로 만든다.

### 569위 [low · 확신 med] 시험 이름에 상황이 없고 앞선 시험과 단언이 겹침

`backend/trading-api/src/test/kotlin/com/papertrading/api/infrastructure/notification/SlackNotificationSchedulerTest.kt:158` · 항목표 backend/tdd-purity · 항목 T6

```
    fun `markFailed에 올바른 maxRetries가 전달된다`() {   // ← 상황 없이 기대만. L108 시험과 같은 verify 중복
...
        verify(exactly = 1) { txService.markFailed(notification, "Slack webhook returned failure", maxRetries) }
```

- 원인: 이름은 기대(maxRetries 전달)만 말하고 어떤 상황(전송 실패 시)인지 빠졌습니다. 같은 markFailed(…, maxRetries) 단언이 바로 위 시험(L90-110)에도 있어 사실상 중복입니다.
- 생길 수 있는 일: 빨간 시험이 어떤 정책이 깨졌는지 말하지 못하고, 중복 시험이 유지 비용만 늘립니다.
- 해결: 이름에 상황을 넣어(예: 전송 실패 시 markFailed 에 설정된 maxRetries 를 넘긴다) 합치거나 중복 시험을 삭제합니다.

### 570위 [low · 확신 med] Slack 응답값을 출처 표기 없이 손으로 적고 요청 본문 계약은 검증하지 않음

`backend/trading-api/src/test/kotlin/com/papertrading/api/infrastructure/notification/SlackWebhookNotifierTest.kt:41` · 항목표 backend/tdd-purity · 항목 T11

```
            restTemplate.postForEntity(props.webhookUrl, any<HttpEntity<Map<String, String>>>(), String::class.java)   // ← 요청 본문은 무엇이든 통과
        } returns ResponseEntity("ok", HttpStatus.OK)   // ← Slack 응답을 출처 없이 직접 적음
```

- 원인: 성공 응답을 ResponseEntity("ok", HttpStatus.OK) 로 직접 적었는데 Slack 문서·녹취 픽스처 등 출처가 시험에 없습니다. 또 요청 본문 매처가 any<HttpEntity<Map<String, String>>>() 라 어떤 본문이든 통과해 Slack 웹훅이 요구하는 JSON 형태(text 필드)와 일치하는지 시험이 보증하지 않습니다. SlackWebhookNotifier.kt 는 이 묶음 밖이라 열어 보지 못해 확신도는 med 입니다.
- 생길 수 있는 일: 시험과 코드가 같은 믿음을 복사해 둘 다 초록인 채, 운영 Slack 계약과 어긋난 본문·응답 처리가 그대로 나갈 수 있습니다.
- 해결: 본문을 capture 해 Slack 웹훅 계약({"text": 메시지})과 같은지 단언하고, 응답 값 옆에 출처(Slack incoming webhook 문서 URL)를 주석으로 남기거나 픽스처 파일로 옮깁니다.

고친 모양(예시):
```
val body = slot<HttpEntity<Map<String, String>>>()
every { restTemplate.postForEntity(props.webhookUrl, capture(body), String::class.java) } returns ResponseEntity("ok", HttpStatus.OK)  // 출처: Slack incoming webhooks 문서
assertEquals(mapOf("text" to testMessage), body.captured.body)
```

### 571위 [low · 확신 med] 금액·비율 JSON 기댓값을 Double 리터럴로 작성

`backend/trading-api/src/test/kotlin/com/papertrading/api/interfaces/rest/position/PositionExitTriggerControllerTest.kt:76` · 항목표 backend/tdd-purity · 항목 T8

```
jsonPath("$.triggerPrice") { value(95.0000) }  // ← Double 리터럴로 금액을 비교
jsonPath("$.exitRatioPercent") { value(50.0000) }
```

- 원인: jsonPath 단언이 value(95.0000)·value(50.0000)·value(8.0000) 처럼 Kotlin Double 로 비교한다(76·78·154행). 4자리 소수 문자열이 계약인데 Double 은 "95"·"95.0"·"95.0000" 을 구분하지 못하고, @Digits(integer=16) 범위에서는 정밀도 손실도 못 잡는다. 현재 값은 모두 Double 로 정확히 표현되어 위험은 낮다.
- 생길 수 있는 일: 응답 scale·정밀도가 깨져도(예: 소수 자릿수 변화) 시험은 초록이다.
- 해결: BigDecimal 문자열 비교(jsonPath(...).value("95.0000") 또는 content().json)로 바꾼다.

고친 모양(예시):
```
jsonPath("$.triggerPrice") { value(BigDecimal("95.0000")) }
```

### 572위 [low · 확신 med] 시험 이름이 상황·행동·기대 세 토막이 아니라 상태 코드 나열이다

`backend/trading-api/src/test/kotlin/com/papertrading/api/presentation/controller/KisAccountBalanceControllerIntegrationTest.kt:87` · 항목표 backend/tdd-purity · 항목 T6

```
    fun `오류 매핑 400 401 403 502 504`() {  // ← 상황·행동·기대가 없는 코드 나열
```

- 원인: 오류 매핑 400 401 403 502 504 는 어떤 입력에 어떤 응답을 기대하는 정책 문장이 아니다. 하나가 깨지면 이름만으로 어느 매핑이 깨졌는지 알 수 없다.
- 생길 수 있는 일: 빨간 시험이 어떤 정책이 깨졌는지 말하지 못해 로그를 열어 진단해야 한다.
- 해결: 오류별로 시험을 나눠 `KIS_인증_실패시_401을_반환한다` 같은 이름을 붙인다. T12 발견과 같은 수정이다.

고친 모양(예시):
```
@Test fun `KIS_인증_실패시_401과_원문_메시지를_반환한다`() { /* 호출 1번 + 단언 */ }
```

### 573위 [low · 확신 med] 시험 하나가 대상 호출 5번에 각자 단언을 단다

`backend/trading-api/src/test/kotlin/com/papertrading/api/presentation/controller/KisAccountBalanceControllerIntegrationTest.kt:94` · 항목표 backend/tdd-purity · 항목 T12

```
        mockMvc.get("/api/kis/account/balance?accountId=0&mode=LIVE").andExpect { status { isBadRequest() } }  // ← 호출 1과 자기 단언
        mockMvc.get("/api/kis/account/balance?accountId=1&mode=PAPER").andExpect {  // ← 호출 2와 자기 단언
            status { isUnauthorized() }
```

- 원인: 오류 시험이 mockMvc.get 5번(400·401·403·502·504)을 한 본문에서 하고 호출마다 상태·메시지를 단언한다. 앞 호출이 실패하면 뒤 4개는 실행되지 않아 가려진다.
- 생길 수 있는 일: 첫 실패가 나머지를 가려 한 번에 한 매핑씩만 드러나고, 이름 하나로 5개 행동을 담지 못한다.
- 해결: @ParameterizedTest 또는 오류별 시험 5개로 나눈다.

고친 모양(예시):
```
@ParameterizedTest
@MethodSource("errorMappings")
fun `서비스_오류는_정해진_상태로_매핑된다`(accountId: Long, thrown: Throwable, expected: HttpStatus) { /* 호출 1번 */ }
```

### 574위 [low · 확신 med] 시험 하나에 세 행동(주문 생성·매칭 체결·포지션 생성)과 단언 9개

`backend/trading-api/src/test/kotlin/com/papertrading/api/presentation/controller/OrderControllerIntegrationTest.kt:90` · 항목표 backend/tdd-purity · 항목 T12

```
        jsonPath("$.orderStatus") { value("PENDING") }  // ← 1단계: 주문 생성 단언 3개
        jsonPath("$.orderStatus") { value("FILLED") }  // ← 2단계: 매칭 체결 단언 3개
        jsonPath("$.quantity") { value(10) }  // ← 3단계: 포지션 생성 단언 3개
```

- 원인: 지정가 매수 시험이 주문 생성 응답(3단언), 매칭 후 FILLED 확인(3단언), 포지션 조회(3단언)를 한 시험에 담는다. 앞 단계가 깨지면 뒤 단계가 실행되지 않아 어느 행동이 깨졌는지 가려진다.
- 생길 수 있는 일: 체결 로직과 포지션 생성 로직 중 어느 쪽이 깨졌는지 시험 이름으로 알 수 없다.
- 해결: 공통 준비(계좌·시세·주문)를 헬퍼로 빼고 '체결되면 FILLED', '체결되면 포지션 생성' 시험으로 나눈다.

### 575위 [low · 확신 med] BigDecimal 필드를 Double 기댓값으로 단언해 scale 정규화 회귀를 못 잡음

`backend/trading-api/src/test/kotlin/com/papertrading/api/presentation/controller/PortfolioSnapshotControllerTest.kt:57` · 항목표 backend/tdd-purity · 항목 T8

```
            pnlRate = BigDecimal("0.200000"),
...
            jsonPath("$[0].pnlRate") { value(0.2) }   // ← Double 기댓값 — scale 6 정규화 회귀를 못 잡음
...
            jsonPath("$[0].weight") { value(0.6) }   // ← 같은 위반
```

- 원인: 픽스처는 BigDecimal("0.200000")·BigDecimal("0.600000") 이고 DTO 는 setScale(6, HALF_UP) 으로 정규화하는데, 단언은 jsonPath value(0.2)·value(0.6) 의 Double 입니다. JSON 숫자는 0.2 와 0.200000 이 같게 파싱돼 setScale 이 사라져도 초록입니다. 금액 필드인 cashBalance 등은 아예 단언하지 않습니다.
- 생길 수 있는 일: PTB4 의 scale 정규화가 깨져도 이 시험이 지켜 주지 못합니다. 비율·금액 표기가 조용히 바뀐 채 배포될 수 있습니다.
- 해결: 응답 JSON 을 문자열로 꺼내 BigDecimal 로 파싱한 뒤 compareTo 로 비교하고 scale 6 은 문자열 길이로 따로 단언한다.

고친 모양(예시):
```
val pnlRate = objectMapper.readTree(body)[0]["pnlRate"].decimalValue()
assertEquals(0, BigDecimal("0.200000").compareTo(pnlRate))
assertEquals(6, pnlRate.scale())
```

### 576위 [low · 확신 med] 시험 이름의 기대가 'correctly' 뿐

`backend/trading-api/src/test/kotlin/com/papertrading/api/presentation/dto/sse/ExecutionSseEventTest.kt:26` · 항목표 backend/tdd-purity · 항목 T6

```
    fun `from converts executionId and orderId correctly`() {  // ← 기대가 'correctly' 뿐
    fun `from converts currency correctly`() {  // ← 마찬가지
```

- 원인: 'from converts executionId and orderId correctly', 'from converts currency correctly' 는 기대가 'correctly' 라 어떤 변환 정책(값 그대로 복사)이 깨졌는지 이름이 말하지 못한다('works' 와 같은 부류). 같은 파일의 나머지 이름(plain strings·ISO instant)은 기대를 담고 있다. 또 L92·L100 시험은 L60·L40 시험과 같은 단언을 되풀이한다.
- 생길 수 있는 일: 빨간 시험을 읽고도 어떤 필드 계약이 깨졌는지 본문을 열어야 한다.
- 해결: 'from copies executionId and orderId unchanged' 처럼 기대를 이름에 적고, 중복 시험(L92·L100)은 지운다.

### 577위 [low · 확신 low] LOCAL/KIS 구분이 TradingMode·AccountType·AccountSource·KisAccountMode·executionMode 로 흩어졌을 가능성

`(repo)` · 항목표 backend/naming-terminology · 항목 N1

```
domain/entity/account/Account.kt:361:        private fun validateTradingMode(accountType: AccountType, tradingMode: TradingMode) {   // ← 비슷한 두 enum 이 한 시그니처에
domain/enums/KisAccountMode.kt:3:enum class KisAccountMode { LIVE, PAPER }   // ← 모드 개념의 세 번째 enum
application/portfolio/TaxSummaryQueryService.kt:46:    private fun ensureLocalMode(accountId: Long, tradingMode: TradingMode) {   // ← 함수 이름은 Mode, 인자는 tradingMode
```

- 원인: enum class 선언이 TradingMode·AccountType·AccountSource·KisAccountMode(LIVE, PAPER) 로 따로 있다. Account.validateTradingMode(accountType, tradingMode) 처럼 서로 다른 두 축이 한 시그니처에 들어가고, 체결 쪽은 같은 속성을 execution_mode 컬럼·executionMode 필드로 쓰는 반면 수수료 정책은 trading_mode 컬럼이다. 예외 이름은 InvalidAccountModeForTaxSummaryException 이다. 각 enum 의 구성원은 열어 보지 않아(선언 줄만 확인) 같은 개념의 철자 분열인지 서로 다른 축인지 확정하지 못했다.
- 생길 수 있는 일: 거래 방식(로컬 모의·KIS 모의·KIS 실거래) 판정은 안전 분기라서, 비슷한 이름의 enum 이 여럿이면 잘못된 enum 으로 검사해 실거래 코드가 모의 분기로 가거나 그 반대가 될 수 있다.
- 해결: TradingMode·AccountType·AccountSource·KisAccountMode 의 구성원을 한 표로 비교해 같은 축이면 하나로 합치고, 다른 축이면 이름에서 축이 드러나게(예 BrokerEnvironment) 바꾼다. 컬럼 execution_mode 는 trading_mode 와 의미가 같은지 확인한다.

### 578위 [low · 확신 low] KIS tr_id 코드가 유스케이스에 문자열 리터럴로 박혀 있고 어댑터에도 따로 있음

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/account/KisAccountQueryService.kt:22` · 항목표 backend/naming-terminology · 항목 N11

```
val trId = when (mode) {
    KisAccountMode.LIVE -> "TTTC8434R" // ← KIS 코드 리터럴(상수 아님)
    KisAccountMode.PAPER -> "VTTC8434R" // ← 어댑터에도 같은 값이 따로 있음
}
```

- 원인: KisAccountMode 를 "TTTC8434R"·"VTTC8434R" 로 번역하는 when 이 application 에 있다. infrastructure/kis/KisAccountBalanceAdapter.kt:171 에는 PAPER_TR_ID = "VTTC8434R" 상수가 따로 있다(grep 으로 확인, 파일은 열지 않음). 같은 프로토콜 코드가 두 계층에 흩어져 있다.
- 생길 수 있는 일: 코드 값을 한쪽만 바꾸면 조용히 어긋나고, KIS 프로토콜 지식이 application 으로 새어 어댑터 교체가 어렵다.
- 해결: tr_id 매핑을 KisAccountBalanceAdapter 안으로 옮기고 포트는 KisAccountMode 를 받는다.
- 다른 렌즈의 반증: backend/code-shape+layering+naming-terminology+tdd-purity — 빈 줄을 뺀 본문이 39줄이라 40줄 이하(경계에 가까움). 25줄이 결과 매핑이라 추상화 수준이 둘이긴 하나 줄 수 기준은 미달

### 579위 [low · 확신 low] 허용 목록 밖 약어 Pnl 이 속성 이름에 쓰임

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/account/result/KisBalancePositionResult.kt:11` · 항목표 backend/naming-terminology · 항목 N6

```
    val unrealizedPnl: BigDecimal,      // ← Pnl 은 허용 약어 목록 밖
```

- 원인: unrealizedPnl 의 Pnl(profit and loss)은 렌즈 허용 약어 목록(id db url uri api http json csv html sql dto uuid jwt sse ws kis vo min max avg idx req res ctx msg)에 없다. 업계 표준 약어라 레포 전체가 같은 철자를 쓰는지에 따라 사람이 허용 목록에 넣을지 정한다.
- 생길 수 있는 일: 읽는 사람마다 다르게 펼칠 여지가 있고 unrealizedProfitLoss 와 철자가 섞이면 용어 번역층이 생긴다. 파급은 작다.
- 해결: 레포 전체가 Pnl 로 통일돼 있다면 허용 목록에 추가하고, 아니면 unrealizedProfitLoss 로 통일한다.

### 580위 [low · 확신 low] 같은 타입 인자가 나란히 있어 위치 인자로 바꿔 넣어도 컴파일됨

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/account/result/LedgerResult.kt:10` · 항목표 backend/layering · 항목 D6

```
    val amount: BigDecimal,
    val balanceAfter: BigDecimal,      // ← 같은 BigDecimal 이 나란히
    val refOrderId: Long?,
    val refExecutionId: Long?,      // ← 같은 타입 id 가 나란히
```

- 원인: amount·balanceAfter 는 둘 다 BigDecimal 이고 refOrderId·refExecutionId 는 둘 다 Long? 이다. 이 결과 타입은 Querydsl Projections.constructor 로 위치 기반 생성되고(AccountLedgerRepositoryImpl.kt:23-33), 시험도 LedgerResult(1L, DEPOSIT, BigDecimal("100000"), BigDecimal("100000"), null, null, null, Instant.now()) 처럼 위치로 만든다.
- 생길 수 있는 일: select 순서나 생성자 인자 순서를 바꿔도 컴파일·실행이 통과해 amount 와 balanceAfter, 주문 id 와 체결 id 가 조용히 뒤바뀐 응답이 나갈 수 있다.
- 해결: 주문·체결 id 는 값 객체(OrderId·ExecutionId)나 최소한 이름 인자로만 생성하게 하고, 시험도 이름 인자를 쓴다.

### 581위 [low · 확신 low] 포트 이름이 값 객체처럼 읽힌다

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/notification/port/SlackNotificationConfig.kt:3` · 항목표 backend/naming-terminology · 항목 N10

```
interface SlackNotificationConfig {   // ← 포트인데 값 객체처럼 읽히는 *Config
    fun get(): SlackNotificationRuntimeConfig
    fun update(newConfig: SlackNotificationRuntimeConfig): SlackNotificationRuntimeConfig
```

- 원인: get()·update() 를 가진 저장소 포트인데 이름이 SlackNotificationConfig 다. 어댑터는 SlackNotificationConfigStore, 쓰는 쪽 변수는 policyStore 라서 포트·어댑터·변수가 서로 다른 말을 한다. 렌즈의 Spring 관례(*Config) 예외는 @Configuration 클래스용이고 이건 포트라 적용하기 어렵다.
- 생길 수 있는 일: SlackNotificationConfig 와 SlackNotificationRuntimeConfig 가 나란히 있어 어느 쪽이 값이고 어느 쪽이 포트인지 이름만으로 못 가른다.
- 해결: 포트를 SlackNotificationConfigStore 로, 어댑터를 InMemorySlackNotificationConfigStore 로 바꾸고 변수 이름도 맞춘다.

고친 모양(예시):
```
interface SlackNotificationConfigStore {
    fun get(): SlackNotificationRuntimeConfig
    fun update(newConfig: SlackNotificationRuntimeConfig): SlackNotificationRuntimeConfig
}
```

### 582위 [low · 확신 low] 같은 Long 타입 id 인자가 나란히 온다

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/notification/SlackNotificationEventPublisher.kt:44` · 항목표 backend/layering · 항목 D6

```
fun publishExecutionFailed(accountId: Long, orderId: Long, executionId: Long, message: String, occurredAt: Instant = Instant.now()) {   // ← Long id 3개 연속
```

- 원인: publishExecutionFailed 가 accountId·orderId·executionId 를 모두 Long 으로 연달아 받는다. 확인한 호출처 두 곳(ExecutionProcessor.kt:224, KisLiveOrderExecutor.kt:34)은 이름 붙은 인자를 써서 위험이 낮다. 다만 타입이 막아 주지 않는다.
- 생길 수 있는 일: 새 호출처가 위치 인자로 부르면 orderId 와 executionId 가 뒤바뀌어도 컴파일이 통과하고 알림 참조가 틀어진다.
- 해결: AccountId·OrderId·ExecutionId 값 클래스를 쓰거나 id 묶음을 값 객체 하나로 받는다.

고친 모양(예시):
```
@JvmInline value class OrderId(val value: Long)
fun publishExecutionFailed(accountId: AccountId, orderId: OrderId, executionId: ExecutionId, message: String, occurredAt: Instant)
```

### 583위 [low · 확신 low] 클래스 이름이 Processor 로 끝나 «무엇을 처리하는지» 말하지 않음

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/ExecutionProcessor.kt:39` · 항목표 backend/naming-terminology · 항목 N5

```
class ExecutionProcessor(            // ← 접미 Processor — 처리 내용을 말하지 않음
...
    fun processFill(                  // ← 함수 이름도 process
```

- 원인: ExecutionProcessor 는 Execution 으로 소속은 말하지만 Processor·processFill 의 «처리»는 체결 반영·정산 생성·구독 해제를 모두 덮는 포괄 동사다. 같은 패키지의 OrderCommandService·KisExecutionNoticeService·LocalMatchingEngine 은 역할 접미가 뚜렷하다. 소속이 드러나 렌즈 기본값 med 를 low 로 낮추고 확신도 낮다.
- 생길 수 있는 일: 새 로직이 «처리»라는 이유로 이 클래스에 계속 쌓이는 쓰레기통이 된다(이미 S1·S3 가 그 징후).
- 해결: 역할 이름으로 바꾼다: ExecutionFillService(체결 반영)/ 후속 정산은 별도 리스너.

### 584위 [low · 확신 low] «미체결(PENDING·PARTIAL)» 상태 집합을 서비스 곳곳에 인라인

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/ExecutionProcessor.kt:98` · 항목표 backend/layering · 항목 D3

```
        if (order.orderStatus !in setOf(OrderStatus.PENDING, OrderStatus.PARTIAL)) return   // ← L98 «미체결» 규칙을 서비스에 인라인
...
            accountId, listOf(OrderStatus.PENDING, OrderStatus.PARTIAL)                     // ← L247 같은 집합 또 한 번
// Order.kt:161-162
    fun isCancellable(): Boolean =
        orderStatus == OrderStatus.PENDING || orderStatus == OrderStatus.PARTIAL            // ← 도메인에 같은 규칙, 호출처 없음
```

- 원인: 같은 집합이 ExecutionProcessor L98·L247, OrderCommandService L244, Order L135·L146·L162 에 흩어져 있다. Order.isCancellable()이 같은 판정인데 호출처가 시험뿐이다(grep).
- 생길 수 있는 일: 새 상태(예: 정정 대기)를 추가하면 인라인 여섯 곳 중 하나를 빠뜨려 체결·취소가 서로 다른 상태 판정을 하게 된다. 지금은 집합이 같아 어긋남은 없다(그래서 med 기본값을 low 로 낮춤).
- 해결: Order.isOpen()(또는 OrderStatus.OPEN 집합)을 한 곳에 두고 모두 그것을 쓴다.
- 다른 렌즈의 반증: backend/code-shape+layering+naming-terminology+tdd-purity — Optional 을 null 로 풀어 elvis 로 조기 반환할 뿐 예외를 던지고 잡지 않음

### 585위 [low · 확신 low] order 컨텍스트가 notification 컨텍스트의 구체 서비스 클래스를 직접 import

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/KisLiveOrderExecutor.kt:3` · 항목표 backend/layering · 항목 L8

```
import com.papertrading.api.application.notification.SlackNotificationEventPublisher // ← 다른 컨텍스트의 구체 서비스 클래스
```

- 원인: SlackNotificationEventPublisher 는 application/notification 의 port·result 가 아닌 구체 클래스인데 order 컨텍스트가 직접 주입받는다(DD7). notification 컨텍스트에 port 하위 패키지가 따로 있어 경계가 명시 계약으로 노출되지 않은 상태다. 이 클래스를 알림 컨텍스트의 공개 진입으로 본다면 낮은 위험이다.
- 생길 수 있는 일: notification 내부 변경(발행 방식·시그니처)이 order 실행기를 같이 고친다.
- 해결: notification/port 의 발행 포트나 도메인 이벤트로 넘기고, order 는 그 계약만 안다.

### 586위 [low · 확신 low] order 컨텍스트가 notification 컨텍스트의 구체 서비스 클래스를 직접 import

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/KisPaperOrderExecutor.kt:3` · 항목표 backend/layering · 항목 L8

```
import com.papertrading.api.application.notification.SlackNotificationEventPublisher // ← 다른 컨텍스트의 구체 서비스 클래스
```

- 원인: KisLiveOrderExecutor 와 같은 결함이다. SlackNotificationEventPublisher 는 notification 컨텍스트의 port·result 가 아닌 구체 클래스다(DD7).
- 생길 수 있는 일: notification 내부 변경이 order 실행기를 같이 고친다.
- 해결: notification/port 의 발행 포트나 도메인 이벤트로 넘긴다.

### 587위 [low · 확신 low] 틱마다 종목의 활성 주문 전부를 로딩한 뒤 가격 조건을 앱에서 거름

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/LocalMatchingEngine.kt:26` · 항목표 data/query · 항목 E4

```
val orders = orderRepository.findActiveLocalOrdersByTicker(ticker)   // ← 가격 조건 없이 종목의 활성 주문 전부 로딩
for (order in orders) {
    runCatching { tryMatchOne(order, quote) }   // ← 가격 조건은 앱에서 거름
```

- 원인: findActiveLocalOrdersByTicker 는 호가 조건 없이 종목의 활성 LOCAL 주문을 모두 읽고, 체결 가능 여부(limitPrice 대 askp1·bidp1)는 Kotlin 의 determineFillPrice 에서 거릅니다. 지정가 조건은 컬럼 비교라 쿼리로 줄일 수 있습니다. 종목당 활성 주문 수가 도메인으로 묶여 있어 low 로 봅니다.
- 생길 수 있는 일: 시세 틱마다 체결 불가능한 주문까지 모두 엔티티로 올려, 주문이 쌓인 종목의 틱 처리 비용이 주문 수에 비례합니다.
- 해결: 쿼리에 시장가 또는 (BUY 이고 limit_price >= ask) 또는 (SELL 이고 limit_price <= bid) 조건을 넣어 체결 후보만 읽습니다.
- 다른 렌즈의 반증: backend/code-shape+layering+naming-terminology+tdd-purity — 주문별 실패를 격리해 나머지 주문 매칭을 계속하는 로깅 경로이고 같은 호출 사슬에서 던져 잡아 분기하지 않는다.

### 588위 [low · 확신 low] 시세 틱마다 종목의 미체결 주문 전체를 DB 에서 조회·평가

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/LocalMatchingEngine.kt:26` · 항목표 reliability/load-concurrency · 항목 C2

```
val orders = orderRepository.findActiveLocalOrdersByTicker(ticker)   // ← 틱마다 전체 조회, 상한 없음
for (order in orders) {
```

- 원인: 틱이 올 때마다 findActiveLocalOrdersByTicker 로 PENDING·PARTIAL 주문을 전부 읽고 하나씩 체결 가능 여부를 판정한다. 건수 상한·배치 크기·메모리 호가창이 없다. 종목당 주문 수가 적으면 괜찮지만 틱 빈도에 비례해 DB 조회가 일어난다.
- 생길 수 있는 일: 틱이 잦은 종목에서 미체결이 늘면 틱마다 쿼리와 평가가 쌓여 커넥션 풀을 압박한다.
- 해결: 미체결 지정가는 종목별 메모리 사본이나 가격 경계 쿼리(체결 가능한 것만)로 줄이고, 틱 병합(coalescing)으로 조회 빈도를 제한한다.
- 다른 렌즈의 반증: backend/code-shape+layering+naming-terminology+tdd-purity — 주문별 실패를 격리해 나머지 주문 매칭을 계속하는 로깅 경로이고 같은 호출 사슬에서 던져 잡아 분기하지 않는다.

고친 모양(예시):
```
orderRepository.findFillableLocalOrders(ticker, quote.askp1, quote.bidp1)   // 가격 조건을 쿼리에 포함
```

### 589위 [low · 확신 low] 매칭 실패 로그에 예외 객체가 없음

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/LocalMatchingEngine.kt:28` · 항목표 reliability/failure-handling · 항목 E2

```
runCatching { tryMatchOne(order, quote) }
    .onFailure { log.warn { "매칭 실패: orderId=${order.id}, reason=${it.message}" } }   // ← 예외 객체·스택 없음
```

- 원인: 주문별 runCatching 경계에서 orderId 와 message 만 남기고 예외 객체를 넘기지 않는다. 낙관적 락 충돌 같은 예상 실패는 괜찮지만 예상 밖 예외(NPE·제약 위반)도 스택 없이 한 줄이 된다.
- 생길 수 있는 일: LOCAL 체결 실패의 원인 분류가 안 되고 같은 메시지로 뭉쳐 보인다.
- 해결: log.warn(it) { ... } 로 예외를 함께 넘기고, 낙관적 락 예외는 별도로 낮은 수준으로 구분한다.
- 다른 렌즈의 반증: backend/code-shape+layering+naming-terminology+tdd-purity — 주문별 실패를 격리해 나머지 주문 매칭을 계속하는 로깅 경로이고 같은 호출 사슬에서 던져 잡아 분기하지 않는다.

고친 모양(예시):
```
.onFailure { log.warn(it) { "매칭 실패: orderId=${order.id}" } }
```

### 590위 [low · 확신 low] IOC/FOK 즉시 취소 경로가 방금 저장한 주문과 이미 락 건 계좌를 다시 조회

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/OrderCommandService.kt:159` · 항목표 data/query · 항목 E5

```
cancelOrder(accountId, orderId, CancelOrderCommand("시세 없음 — IOC/FOK 즉시 취소"))   // ← cancelOrder 가 order·account 를 재조회 (:240, :253)
```

- 원인: placeOrder 가 주문을 저장하고 계좌를 락한 뒤, cancelIfUnfillableIocFok→cancelOrder(:240·:253)가 같은 주문(findByIdWithOptimisticLock)과 계좌(findByIdWithLock)를 다시 읽습니다. 같은 트랜잭션이라 값은 같지만 왕복이 늘고 락 쿼리가 반복됩니다.
- 생길 수 있는 일: IOC/FOK 취소마다 조회 2회가 더 나갑니다. 비용은 작고 오류는 아닙니다.
- 해결: 이미 가진 order·account 를 넘기는 private 취소 함수로 분리해 공용으로 씁니다.
- 다른 렌즈의 반증: data/query+schema-data-model+transaction-boundary — 바깥 placeOrder 와 안쪽 cancelOrder 모두 기본 @Transactional(REQUIRED)이고 같은 범위를 기대하며 REQUIRES_NEW·readOnly 차이를 기대하지 않습니다. createGroupedAutoExitSellOrder→placeOrder(:194), createAutoExitSellOrder→placeOrder(:221)도 같습니다.

### 591위 [low · 확신 low] 매도 취소 시 포지션이 없으면 잠금 해제를 조용히 건너뜀

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/OrderCommandService.kt:272` · 항목표 reliability/failure-handling · 항목 E1

```
val position = positionRepository.findByAccountIdAndTickerWithLock(accountId2, ticker)
    .orElse(null)   // ← 없으면 null
...
position?.unlockQuantity(remainQty)   // ← 없으면 조용히 건너뜀, 로그 없음
```

- 원인: cancelOrder 의 매도 분기가 포지션을 orElse(null) 로 받고 position?.unlockQuantity 로 없으면 아무 일도 하지 않는다. 매도 주문이 있는데 포지션이 없다는 것은 불변식 위반인데 로그도 예외도 없다.
- 생길 수 있는 일: 깨진 데이터가 흔적 없이 지나간다.
- 해결: 포지션이 없으면 경고 로그와 함께 건너뛰거나 예외로 실패시킨다.

고친 모양(예시):
```
val position = positionRepository.findByAccountIdAndTickerWithLock(accountId2, ticker)
    .orElseThrow { PositionNotFoundException(ticker = ticker) }
```

### 592위 [low · 확신 low] 클래스 이름의 Tx 가 허용 약어 목록 밖

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/portfolio/SnapshotJobRunTxService.kt:12` · 항목표 backend/naming-terminology · 항목 N6

```
class SnapshotJobRunTxService(  // ← Tx 약어
```

- 원인: SnapshotJobRunTxService 의 Tx 는 허용 목록(id db url … msg)에 없는 약어다. 업계에서 흔한 약어라 low 로 둔다.
- 생길 수 있는 일: 읽는 사람이 Tx 를 Transaction 으로 펼치지만 REQUIRES_NEW 래퍼라는 의도는 이름에 안 드러난다.
- 해결: `SnapshotJobRunRecorder` 처럼 역할을 드러내는 이름으로 바꾼다.

고친 모양(예시):
```
class SnapshotJobRunRecorder(...)
```

### 593위 [low · 확신 low] 집계 결과 값에 DDD 용어 Aggregate 접미를 씀

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/portfolio/tax/TaxSettlementAggregate.kt:12` · 항목표 backend/naming-terminology · 항목 N10

```
data class TaxSettlementAggregate(  // ← 집계 결과인데 DDD 애그리거트처럼 읽힘
```

- 원인: TaxSettlementAggregate 는 SQL SUM 결과를 담는 읽기 모델인데 이 레포 규칙(PTB3 등)이 «애그리거트»를 DDD 루트 뜻으로 쓴다. 포트 결과 타입이면 Result 접미가 역할에 맞다.
- 생길 수 있는 일: 새 사람이 이것을 애그리거트 루트로 오해하거나 애그리거트 검색에 섞인다.
- 해결: `TaxSettlementSummary` 또는 `TaxSettlementResult` 로 바꾼다(포트 이전 때 함께).

고친 모양(예시):
```
data class TaxSettlementResult(val totalRealizedPnl: BigDecimal, ...)
```

### 594위 [low · 확신 low] «종료된 과세연도» 규칙이 Batch·Command 서비스에 각각 있음

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/portfolio/TaxSummaryBatchService.kt:20` · 항목표 backend/layering · 항목 D3

```
val currentYear = Instant.now(clock).atZone(ZoneOffset.UTC).year
if (taxYear.value >= currentYear) {  // ← Command 서비스 49~52행과 같은 규칙
    throw TaxYearNotClosedException(
```

- 원인: currentYear 를 구해 taxYear.value >= currentYear 를 비교하는 규칙이 TaxSummaryBatchService 와 TaxSummaryCommandService(49~52행)에 있고 TaxYear 에는 그 규칙의 메서드가 없다. TaxYear 는 도메인 클래스가 아니라 application 값 객체라 심각도는 낮춘다.
- 생길 수 있는 일: 종료 기준(UTC 연도 등)을 바꿀 때 두 곳을 같이 고쳐야 한다.
- 해결: TaxYear 에 `isClosed(now: Instant)` 같은 메서드를 두고 두 서비스가 쓴다.
- 다른 렌즈의 반증: backend/code-shape+layering+naming-terminology+tdd-purity — Instant.now(clock) 이고 Clock 을 생성자로 주입받아 시험이 고정할 수 있다(TaxSummaryCommandServiceTest 가 Clock.fixed 사용). 통과.

고친 모양(예시):
```
fun isClosed(clock: Clock): Boolean = value < Instant.now(clock).atZone(ZoneOffset.UTC).year
```

### 595위 [low · 확신 low] 활성 계좌 id 만 필요한데 Account 엔티티 전체를 읽는다

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/portfolio/TaxSummaryBatchService.kt:28` · 항목표 data/query · 항목 Q8

```
        val targetAccountIds = accountIds
            ?.filterNotNull()
            ?.distinct()
            ?.ifEmpty { accountRepository.findByIsActiveTrue().mapNotNull { it.id } }   // ← 엔티티 전체를 읽고 id 만 사용
            ?: accountRepository.findByIsActiveTrue().mapNotNull { it.id }
```

- 원인: findByIsActiveTrue() 가 활성 계좌의 엔티티 전부를 올린 뒤 mapNotNull { it.id } 로 id 만 쓴다. 이 서비스는 @Transactional 이 없어 영속성 컨텍스트 스냅샷 비용은 작지만 넓은 행이 계좌 수만큼 오간다.
- 생길 수 있는 일: 계좌 수가 늘수록 연말 일괄 시작 때 불필요한 열을 읽는다. 큰 문제는 아니다.
- 해결: id 만 돌려주는 프로젝션 메서드를 AccountRepository 에 둔다.
- 다른 렌즈의 반증: data/query+schema-data-model+transaction-boundary — 계좌마다 독립 트랜잭션·계좌 락이 필요한 단위 작업이라 일괄 읽기로 바꿀 수 없다. 루프가 부르는 것은 리포지토리가 아니라 유스케이스다. 대신 X12 로 보고했다.

고친 모양(예시):
```
@Query("select a.id from Account a where a.isActive = true")
fun findActiveAccountIds(): List<Long>
```

### 596위 [low · 확신 low] Boolean 인자 force 가 is·should 접두 없는 플래그 이름

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/portfolio/TaxSummaryCommandService.kt:46` · 항목표 backend/naming-terminology · 항목 N3

```
force: Boolean,  // ← is·should 접두 없는 Boolean 인자
```

- 원인: recalculate 의 force: Boolean 이 접두 없는 이름이다. 호출부는 force = true 처럼 이름 붙인 인자로 써서 읽기 문제는 작다.
- 생길 수 있는 일: 함수 안 `if (!force && …)` 가 명사처럼 읽히지만 호출부 이름 인자로 대부분 완화된다.
- 해결: `shouldForce` 또는 `isForced` 로 바꾸거나 강제 여부를 enum(`RecalcMode`)으로 표현한다.

고친 모양(예시):
```
enum class RecalcMode { CLOSED_YEAR_ONLY, FORCE }
```

### 597위 [low · 확신 low] 시세 틱마다 보유 계좌 수만큼 Redis 저장 왕복을 반복

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/position/PositionCommandService.kt:41` · 항목표 data/query · 항목 E2

```
snapshots.forEach {
    livePositionCachePort.save(it.evaluate(price, source, now))     // ← L42 스냅샷마다 Redis 왕복 1회
}
```

- 원인: updateCurrentPriceByTicker 는 한 종목을 가진 모든 계좌의 스냅샷을 forEach 로 돌며 계좌마다 livePositionCachePort.save 를 한 번씩 부른다. 반복 횟수를 종목 보유 계좌 수가 정한다. DB 리포지토리가 아니라 캐시 포트라 영향이 작아 severity 를 낮췄다. 포트 구현은 열어 보지 않았다.
- 생길 수 있는 일: 보유 계좌가 늘수록 시세 한 건당 Redis 왕복이 비례해 늘어 리스너 지연이 커진다.
- 해결: 포트에 saveAll(파이프라인·MSET)을 두고 한 번에 저장한다.
- 다른 렌즈의 반증: reliability/failure-handling+load-concurrency+observability+security-config-hygiene — KIS·Slack 같은 외부 의존이 아니라 평가 캐시 포트 쓰기이고, Lettuce 계측은 설정 쪽(미열람)이라 건당 타이머를 요구하지 않음.

고친 모양(예시):
```
livePositionCachePort.saveAll(snapshots.map { it.evaluate(price, source, now) })
```

### 598위 [low · 확신 low] 같은 에러 코드 문자열이 한 파일 안과 도메인에 흩어져 반복된다

`backend/trading-api/src/main/kotlin/com/papertrading/api/application/position/PositionExitTriggerCommandService.kt:108` · 항목표 backend/naming-terminology · 항목 N11

```
throw BadRequestException("TRIGGER_PRICE_REQUIRED", "FIXED_PRICE requires triggerPrice")  // ←
...
?: throw BadRequestException("TRIGGER_PERCENT_REQUIRED", "AVG_PRICE_AT_CREATION requires triggerPercent")
...
?: throw BadRequestException("TRIGGER_PERCENT_REQUIRED", "FOLLOW_AVG_PRICE requires triggerPercent")
```

- 원인: "INVALID_PRICE_BASIS_POLICY" 가 L115·L126 두 번, "TRIGGER_PERCENT_REQUIRED" 가 L120·L131 두 번, "TRIGGER_PRICE_REQUIRED" 가 L108 에 있고 같은 코드가 PositionExitTrigger L178·L191 에도 있다. 비교가 아니라 던질 때 쓰는 코드라 신호가 약해 심각도를 낮췄다.
- 생길 수 있는 일: 오타 하나가 클라이언트가 보는 에러 코드를 조용히 바꾸고, 같은 상황에 서비스와 도메인이 다른 코드를 낼 수 있다.
- 해결: 에러 코드를 enum 이나 const 로 한 곳에 모은다(D3 의 정책 검증 한 곳 모으기와 함께).

### 599위 [low · 확신 low] 같은 타입 id 두 개가 나란히 오는 생성자

`backend/trading-api/src/main/kotlin/com/papertrading/api/common/exception/CommonApiExceptions.kt:57` · 항목표 backend/layering · 항목 D6

```
class TradingJournalOwnershipMismatchException(journalId: Long, accountId: Long) :   // ← 같은 Long 타입 id 두 개 나란히
```

- 원인: TradingJournalOwnershipMismatchException(journalId: Long, accountId: Long) 의 Long id 두 개가 나란히 온다. 호출부에서 순서를 바꾸면 컴파일은 통과한다. 다만 값은 메시지에만 쓰여 영향은 진단 메시지 오기에 그친다.
- 생길 수 있는 일: 인자 순서를 바꿔 호출해도 조용히 통과해 거래 일지·계좌 id 가 뒤바뀐 로그 메시지가 남는다(금액·상태에는 영향 없음).
- 해결: 호출부에서 이름 붙은 인자로 부르거나 값 객체(JournalId·AccountId)를 도입한다. 메시지 전용이면 현 상태로 두어도 된다.

### 600위 [low · 확신 low] Order → BaseAuditEntity → BaseTimeEntity 프로젝트 상속 3단

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/order/Order.kt:37` · 항목표 backend/code-shape · 항목 S8

```
class Order protected constructor() : BaseAuditEntity() {   // ← BaseAuditEntity : BaseTimeEntity — 프로젝트 클래스 3단
```

- 원인: 렌즈가 «BaseTimeEntity → BaseAuditEntity → 엔티티는 셈»이라고 적은 바로 그 사슬이다(BaseAuditEntity : BaseTimeEntity, base/*.kt:9). 클래스를 3개로 세면 깊이 > 2. 부모 단계로만 세면 2 라 한계 이내일 수 있어 해석이 갈린다. JPA @MappedSuperclass 감사 열 믹스인이라 위험은 작다.
- 생길 수 있는 일: 감사 열을 합성이 아닌 상속으로 재사용하므로 도메인 모델이 JPA 상속 구조에 묶인다(D1 이 해결되면 함께 사라진다).
- 해결: D1 로 도메인을 순수 Kotlin 으로 옮길 때 감사 열은 infrastructure JPA 엔티티의 몫으로 돌려 상속을 없앤다.

### 601위 [low · 확신 low] KIS 주문번호(external_order_id)에 제약·인덱스가 없는데 조회는 단건(fetchOne)

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/order/Order.kt:114` · 항목표 data/schema-data-model · 항목 M3

```
@Column(name = "external_order_id", length = 100)
var externalOrderId: String? = null   // ← 유일·인덱스 없음 (조회: OrderRepositoryImpl.kt:90 o.externalOrderId.eq(...) + fetchOne)
```

- 원인: KIS 체결 통지는 externalOrderId 와 mode, 선택값 accountScope 로 주문을 찾고 fetchOne 으로 받습니다(OrderRepositoryImpl.kt:79-95). accountScope 가 비면 계정 조건이 빠집니다. KIS 주문번호는 일자·계좌 범위라 단독 UNIQUE 는 맞지 않아 (계좌, 일자, 번호) 복합 제약이 필요한지는 KIS 규격 확인이 필요합니다. 그래서 확신을 낮춥니다.
- 생길 수 있는 일: 서로 다른 계좌의 같은 번호 주문이 겹치면 NonUniqueResultException 또는 잘못된 주문에 체결이 붙을 수 있습니다.
- 해결: KIS 규격에 맞는 범위(계좌, 주문일, 번호)로 부분 UNIQUE 인덱스를 두고, accountScope 를 필수로 만듭니다.

### 602위 [low · 확신 low] 전략의 account_id 조회에 인덱스가 없음

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/strategy/Strategy.kt:30` · 항목표 data/schema-data-model · 항목 M4

```
@Table(name = "strategies")   // ← L30 indexes 없음, account_id 필터 조회 존재
```

- 원인: StrategyRepository.findByAccountIdAndIsActiveTrueAndApprovalStatus 가 account_id 로 거르는데 @Table indexes 가 없다. 계좌당 전략 수가 적어 영향은 작다.
- 생길 수 있는 일: 전략 수가 늘면 활성 전략 조회가 전체 스캔이 된다.
- 해결: (account_id, approval_status, is_active) 인덱스를 추가한다.
- 다른 렌즈의 반증: backend/code-shape+layering+naming-terminology+tdd-purity — 프로젝트 조상 클래스가 2개라 깊이 2 로 기준(> 2)을 넘지 않는다. 둘 다 감사 시각 필드용 @MappedSuperclass 이고 오버라이드가 없어 LSP 를 깨지 않는다.

고친 모양(예시):
```
@Table(name = "strategies",
    indexes = [Index(name = "idx_strategies_account_status", columnList = "account_id, approval_status, is_active")])
```

### 603위 [low · 확신 low] Strategy 클래스의 public 함수가 11개로 기준(10개) 초과

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/strategy/Strategy.kt:31` · 항목표 backend/code-shape · 항목 S4

```
class Strategy protected constructor() : BaseAuditEntity() {   // ← public 함수 11개(L99·106·110·119·127·136·145·150·164·174·176)
    fun hasExecuted(): Boolean = executionCount > 0
    fun updatePerformance(   // ← 승인 흐름과 별개인 성과 갱신이 같은 클래스에 있음
```

- 원인: rename·updateDescription·requestReview·approve·reject·activate·deactivate·cancel·markExecuted·hasExecuted·updatePerformance 11개가 모두 public 이다(companion 의 create 제외). 전부 Strategy 애그리거트 안의 상태 전이라 S3(주인공 시험)은 통과지만, 승인 흐름·실행 집계·성과 갱신이 한 클래스에 모여 표면이 넓다. detekt 기준선에도 TooManyFunctions 로 등록돼 있다(tools/Tb661de0c.txt).
- 생길 수 있는 일: 기능이 더 붙으면(예: 성과 갱신 규칙) 책임이 여럿인 클래스로 굳어 변경 때 서로 다른 전이 규칙을 함께 건드린다.
- 해결: 성과 4종(sharpeRatio·maxDrawdown·winRate·avgReturn)을 값 객체 하나로 묶어 updatePerformance 와 검증 4개를 옮기거나, 지금 규모가 의도라면 현 상태를 유지하고 기준선만 인정한다.
- 다른 렌즈의 반증: backend/code-shape+layering+naming-terminology+tdd-purity — 프로젝트 조상 클래스가 2개라 깊이 2 로 기준(> 2)을 넘지 않는다. 둘 다 감사 시각 필드용 @MappedSuperclass 이고 오버라이드가 없어 LSP 를 깨지 않는다.

고친 모양(예시):
```
data class PerformanceMetrics(val sharpeRatio: BigDecimal?, val maxDrawdown: BigDecimal?, val winRate: BigDecimal?, val avgReturn: BigDecimal?) {
    init { /* 범위 검증 4개 */ }
}
fun updatePerformance(metrics: PerformanceMetrics) { ... }
```

### 604위 [low · 확신 low] 전략 실행 횟수·승인 상태를 읽고 고쳐 쓰는데 @Version·락이 없음

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/strategy/Strategy.kt:79` · 항목표 data/schema-data-model · 항목 M2

```
@Column(name = "execution_count", nullable = false)
var executionCount: Long = 0                     // ← L79-80 @Version 없음
...
executionCount += 1                               // ← L171 읽고-고쳐-쓰기
```

- 원인: markExecuted 가 executionCount += 1 을 하고 approve·reject·activate 가 approvalStatus·isActive 를 바꾸지만 @Version 도 락도 없다. main 코드에 변경 경로가 아직 없어 확정은 못 하며 잔고·포지션 같은 금융 상태가 아니라 low.
- 생길 수 있는 일: 동시 실행·승인 요청이 서로 덮어써 실행 횟수가 줄거나 승인과 반려가 엇갈린다.
- 해결: 엔티티에 @Version var version: Long 을 추가한다.
- 다른 렌즈의 반증: backend/naming-terminology — 전략 실행 시각이지 체결 시각이 아니라 다른 개념. executedAt 은 체결 쪽에서 레포 전체 하나(filledAt 없음)

고친 모양(예시):
```
@Version
@Column(name = "version", nullable = false)
var version: Long = 0
    private set
```

### 605위 [low · 확신 low] 전략 파생 관계에 (parent, child) 유일 제약이 없음

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/strategy/StrategyDerivation.kt:20` · 항목표 data/schema-data-model · 항목 M3

```
@Entity
@Table(name = "strategy_derivations")   // ← L20 (parent_strategy_id, child_strategy_id) 유일 제약 없음
```

- 원인: 같은 부모·자식 쌍의 계보 행을 여러 번 만들 수 있다. 앙상블은 부모가 여럿일 수 있어 child 단독 유일은 아니고 쌍의 유일이 맞다. 쓰는 곳이 아직 없어 low.
- 생길 수 있는 일: 파생 계보가 중복돼 계보 조회·표시가 같은 관계를 여러 번 보여 준다.
- 해결: uniqueConstraints 로 (parent_strategy_id, child_strategy_id)를 추가한다.

고친 모양(예시):
```
@Table(name = "strategy_derivations",
    uniqueConstraints = [UniqueConstraint(name = "uk_strategy_derivations_pair", columnNames = ["parent_strategy_id", "child_strategy_id"])])
```

### 606위 [low · 확신 low] derivationType 이 enum 이 아니라 문자열 코드 값

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/strategy/StrategyDerivation.kt:36` · 항목표 backend/naming-terminology · 항목 N11

```
@Column(name = "derivation_type", nullable = false, length = 30)
lateinit var derivationType: String   // ← 코드 값이 문자열
...
private val ALLOWED_DERIVATION_TYPES = setOf("FORK", "TUNE", "ENSEMBLE")   // ← 리터럴 반복 (L60 메시지에도)
```

- 원인: FORK/TUNE/ENSEMBLE 이 String 으로 저장·비교되고 허용 목록(L49)과 오류 메시지(L60)에 같은 리터럴이 두 번 적혀 있다. 같은 패키지의 sourceType·approvalStatus 는 enum 이어서 일관성도 어긋난다(CS24). 상수 목록은 있으므로 «흩어진 반복 비교»는 아니라 confidence 를 낮춘다.
- 생길 수 있는 일: 호출부가 "fork"·"Fork"·"FORKK" 를 넘겨도 컴파일이 통과하고 런타임 require 로만 잡힌다. 새 유형을 추가할 때 허용 목록·메시지·호출부를 따로 고친다.
- 해결: DerivationType enum(FORK·TUNE·ENSEMBLE)을 만들고 @Enumerated(EnumType.STRING)으로 매핑한다.

고친 모양(예시):
```
enum class DerivationType { FORK, TUNE, ENSEMBLE }
@Enumerated(EnumType.STRING)
@Column(name = "derivation_type", nullable = false, length = 30)
lateinit var derivationType: DerivationType
```

### 607위 [low · 확신 low] logLevel 이 enum 이 아니라 문자열 코드 값 (저장소 API 까지 노출)

`backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/strategy/StrategyLog.kt:40` · 항목표 backend/naming-terminology · 항목 N11

```
@Column(name = "log_level", nullable = false, length = 10)
lateinit var logLevel: String   // ← 코드 값이 문자열
...
private val ALLOWED_LOG_LEVELS = setOf("INFO", "WARN", "ERROR")   // ← 리터럴 반복 (L74 메시지에도)
// StrategyLogRepository.kt:8  fun findByStrategyIdAndLogLevelOrderByLoggedAtDesc(strategyId: Long, logLevel: String)
```

- 원인: INFO/WARN/ERROR 가 String 이고 허용 목록(L62)과 메시지(L74)에 리터럴이 반복된다. StrategyLogRepository.findByStrategyIdAndLogLevelOrderByLoggedAtDesc(strategyId, logLevel: String) 도 문자열을 받아 호출부가 "ERROR" 리터럴을 흩뿌리게 된다.
- 생길 수 있는 일: 조회 호출부의 오타("Error")가 빈 목록으로 조용히 돌아와 오류 로그가 없는 것처럼 보인다.
- 해결: LogLevel enum 으로 올리고 저장소 메서드 인자도 enum 으로 바꾼다.

고친 모양(예시):
```
enum class LogLevel { INFO, WARN, ERROR }
fun findByStrategyIdAndLogLevelOrderByLoggedAtDesc(strategyId: Long, logLevel: LogLevel): List<StrategyLog>
```

### 608위 [low · 확신 low] 파싱 전체를 runCatching 으로 감싸 Error 까지 삼킴

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/kis/KisExecutionNoticeParser.kt:19` · 항목표 reliability/failure-handling · 항목 E3

```
fun parse(rawMessage: String, mode: String): KisExecutionNotice? = runCatching {   // ← 본문 20여 문장 전체를 Throwable 단위로 잡음
```

- 원인: runCatching 은 Throwable 을 잡는다. 20줄이 넘는 본문에서 던지는 것은 JSON 읽기와 시각 파싱 정도인데, 프로그래밍 오류와 Error 까지 같은 null 로 뭉쳐진다.
- 생길 수 있는 일: 파서 버그가 정상적인 '해당 없음' 메시지와 구분되지 않는다.
- 해결: JsonProcessingException·DateTimeParseException 만 잡도록 try 범위를 읽기·시각 파싱 호출로 좁힌다.
- 다른 렌즈의 반증: backend/code-shape+layering+naming-terminology+tdd-purity — JSON 파싱·LocalDateTime 파싱 실패 같은 외부 입력 오류를 감싼 것이라 firstOrNull·?: 한 식으로 대체할 수 없다. 오류를 삼키는 문제는 failure-handling 렌즈 몫이다.

고친 모양(예시):
```
val fields = try { parseFields(raw) } catch (e: JsonProcessingException) { return ParseResult.Failed(e) }
```

### 609위 [low · 확신 low] WebSocket 연결에 명시적 타임아웃이 없음

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/kis/KisExecutionWebSocketManager.kt:63` · 항목표 reliability/failure-handling · 항목 E5

```
private val client: StandardWebSocketClient = StandardWebSocketClient(),   // ← 타임아웃 설정 없음
...
client.execute(handler, request.url).whenComplete { _, error ->   // ← future 에 시간 제한 없음
    if (error != null) { request.onFailure(error) }
}
```

- 원인: StandardWebSocketClient 를 기본 생성하고 client.execute(...) 의 future 를 시간 제한 없이 whenComplete 로만 받는다. 컨테이너 기본값이 있을 수 있으나(미검증) 코드·설정에는 연결·핸드셰이크 제한이 없다. 핸드셰이크가 멈추면 상태는 CONNECTING 으로 남고 재연결 타이머도 없다.
- 생길 수 있는 일: KIS 서버 쪽 지연이 길면 체결 통지 연결이 CONNECTING 에서 멈춘 채 아무 신호도 없다.
- 해결: 연결 future 에 orTimeout(예: 10초)을 걸고 시간 초과를 onFailure 로 보낸다.

고친 모양(예시):
```
client.execute(handler, request.url)
    .orTimeout(10, TimeUnit.SECONDS)
    .whenComplete { _, error -> if (error != null) request.onFailure(error) }
```

### 610위 [low · 확신 low] KIS WebSocket 기본 URL 이 평문 ws:// 이고 자격 증명이 그 위로 전송됨

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/kis/KisOrderProperties.kt:17` · 항목표 reliability/security-config-hygiene · 항목 S9

```
var paperWsUrl: String = "ws://ops.koreainvestment.com:31000"   // ← 평문 ws
var liveWsUrl: String = "ws://ops.koreainvestment.com:21000"   // ← 실전도 평문 ws
```

- 원인: 모의·실전 WebSocket 기본 주소가 모두 ws:// 이다. KisExecutionWebSocketManager 의 구독 메시지(L69-86)가 approval_key 와 appKey 를 이 평문 연결로 보낸다. KIS 가 공식으로 제공하는 주소라면 선택지가 없으므로 근거 확인 후 낮게 본다(렌즈 기준 low). 실전 주소가 같은 구조라는 점은 남는다.
- 생길 수 있는 일: 중간자가 승인키를 가로채면 실시간 체결통보 구독을 가로챌 수 있다.
- 해결: KIS 가 wss 를 제공하면 wss 로 바꾸고, 아니면 공식 문서 근거와 네트워크 제한(고정 출구 IP 등)을 주석·문서에 남긴다.
- 다른 렌즈의 반증: backend/code-shape+layering+naming-terminology+tdd-purity — @ConfigurationProperties 바인딩 이름(trading.kis.execution-ws-enabled)이라 바꾸면 환경 변수·yml 외부 계약이 깨진다.

고친 모양(예시):
```
var liveWsUrl: String = "wss://ops.koreainvestment.com:21443"   // KIS 제공 여부 확인 후
```

### 611위 [low · 확신 low] 클래스 이름에 허용 목록 밖 약어 Tx 가 있다

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/notification/NotificationPersistenceTxService.kt:11` · 항목표 backend/naming-terminology · 항목 N6

```
@Component
class NotificationPersistenceTxService(   // ← Tx 약어 (허용 목록 밖)
    private val repository: NotificationRepository,
```

- 원인: NotificationPersistenceTxService 의 Tx 는 허용 약어 목록에 없다. 핸들러·스케줄러의 txService 프로퍼티도 같은 약어를 따른다. 흔한 약어라 심각도와 신뢰도를 낮춘다.
- 생길 수 있는 일: 약어를 읽는 사람마다 다르게 펼치고(Tx = transaction), 클래스가 무엇을 하는지(저장 위임)는 이름에서 안 드러난다.
- 해결: 역할을 말하는 이름으로 바꾼다(아래 N10 과 함께).

고친 모양(예시):
```
class NotificationDeliveryRecorder(
```

### 612위 [low · 확신 low] 비동기 알림 실행기의 큐 깊이·거부·대기 시간 계측이 없음

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/notification/SlackNotificationEventHandler.kt:22` · 항목표 reliability/observability · 항목 O6

```
@Async("slackNotificationExecutor")   // ← 큐 깊이·거부 횟수·처리 지연 계측 0개
fun handle(event: SlackNotificationRequestedEvent) {
```

- 원인: slackNotificationExecutor 의 큐(최대 100) 깊이, CallerRuns 로 빠진 횟수, 이벤트 발생부터 처리까지의 지연을 기록하는 로그·게이지·타이머가 하나도 없다.
- 생길 수 있는 일: 적체가 보이지 않다가 큐가 차는 순간 주문 스레드 지연(A3)으로 처음 드러난다.
- 해결: ThreadPoolTaskExecutor 큐 크기를 Gauge 로 노출하고 거부 시 카운터를 올린다.
- 다른 렌즈의 반증: reliability/failure-handling+load-concurrency+observability+security-config-hygiene — slackNotificationExecutor 는 큐 100·최대 4스레드·거부 정책이 있음(AsyncConfig.kt:14-18). CallerRuns 문제는 A3 로 보고

고친 모양(예시):
```
Gauge.builder("slack.executor.queue") { executor.threadPoolExecutor.queue.size }.register(registry)
```

### 613위 [low · 확신 low] 틱마다 PENDING 전량 조회하고 건수 상한 없이 건당 Slack 호출

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/notification/SlackNotificationScheduler.kt:21` · 항목표 reliability/load-concurrency · 항목 C2

```
val pendingNotifications = txService.findPendingForRetry(policy.maxRetries)   // ← 한도·페이징 없이 PENDING 전부 조회
for (notification in pendingNotifications) {                                  // ← 건수 상한·호출 간격 없이 건당 Slack 호출을 직렬 실행
```

- 원인: findPendingForRetry 는 한도·페이징 없이 PENDING 전부를 읽고 루프가 건마다 Slack 을 호출한다. 일괄 API 도 호출 간격도 없어 웹훅 속도 제한을 고려하지 않는다. 한 틱의 비용이 적체량에 비례한다.
- 생길 수 있는 일: Slack 장애로 PENDING 이 쌓이면 틱이 다음 틱을 넘어 길어지고, 복구 직후 초당 다수 호출로 429 가 나 FAILED 로 확정될 수 있다.
- 해결: 조회에 limit(예 50)을 두고 틱마다 그 한도까지만 처리한다.
- 다른 렌즈의 반증: data/query+schema-data-model+transaction-boundary — 각 알림이 독립 작업이고 «전체 성공/실패» 응답을 받는 호출자가 없다. 건별로 점유(claimForDelivery)→전송→결과 기록이 목적이다. 건별 예외는 catch 로 격리되고 바깥 트랜잭션이 없어 UnexpectedRollbackException 도 생기지 않는다.

고친 모양(예시):
```
fun findPendingForRetry(maxAttempts: Int, limit: Int): List<Notification> =
    repository.findByStatusAndAttemptCountLessThan(DeliveryStatus.PENDING, maxAttempts, PageRequest.of(0, limit))
```

### 614위 [low · 확신 low] 재시도 틱이 대기 건수·성공/실패 건수·소요 시간을 남기지 않음

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/notification/SlackNotificationScheduler.kt:21` · 항목표 reliability/observability · 항목 O6

```
val pendingNotifications = txService.findPendingForRetry(policy.maxRetries)   // ← 틱마다 대기 건수·성공/실패 건수·소요 시간을 남기지 않음
for (notification in pendingNotifications) {
```

- 원인: 틱마다 몇 건이 PENDING 이었고 몇 건을 보냈고 몇 건이 FAILED 가 됐는지, 틱이 얼마나 걸렸는지 기록하지 않는다. 항목별 오류만 로그에 있고 정상·최종 실패 요약이 없다.
- 생길 수 있는 일: PENDING 적체나 최종 FAILED 급증을 로그·지표로 볼 수 없다.
- 해결: 틱 끝에 pending·sent·failed 건수와 소요 ms 를 INFO 한 줄과 게이지로 남긴다.
- 다른 렌즈의 반증: data/query+schema-data-model+transaction-boundary — 각 알림이 독립 작업이고 «전체 성공/실패» 응답을 받는 호출자가 없다. 건별로 점유(claimForDelivery)→전송→결과 기록이 목적이다. 건별 예외는 catch 로 격리되고 바깥 트랜잭션이 없어 UnexpectedRollbackException 도 생기지 않는다.

고친 모양(예시):
```
log.info { "slack retry tick: pending=${pending.size} sent=$sent failed=$failed took=${elapsedMs}ms" }
```

### 615위 [low · 확신 low] 같은 일자 구간 조회가 파생 메서드와 Querydsl 두 곳에 있고 파생 쪽은 호출처가 없다

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/DailyBalanceRepository.kt:10` · 항목표 data/query · 항목 Q7

```
    fun findByAccountIdAndBusinessDateBetweenOrderByBusinessDateAsc(   // ← 호출처 없음 — searchDailyBalances(Querydsl)와 같은 조회
        accountId: Long,
        from: LocalDate,
        to: LocalDate
    ): List<DailyBalance>
```

- 원인: findByAccountIdAndBusinessDateBetweenOrderByBusinessDateAsc 는 DailyBalanceRepositoryImpl.searchDailyBalances 와 필터·정렬이 같다. grep 으로 호출처를 찾았으나 선언부 외에는 나오지 않았다. 필터(소프트 삭제·계좌 범위)가 바뀌면 한쪽만 고쳐지고 죽은 사본이 남는다.
- 생길 수 있는 일: 조회 조건 변경 때 죽은 사본이 낡은 필터로 남아 있다가 나중에 호출되면 다른 결과를 낸다.
- 해결: 쓰지 않는 파생 메서드를 지우고 조회 경로를 하나로 둔다.
- 다른 렌즈의 반증: backend/code-shape+layering+naming-terminology+tdd-purity — 이 인터페이스 자체는 어댑터 쪽이고 포트가 없다는 문제는 이를 주입받는 application 파일들의 L1 로 이미 동결 목록에 있다(T96587ad0.txt 109-133행). L3 로 두 번 올리지 않는다

### 616위 [low · 확신 low] 같은 필터의 보유 포지션 조회가 호출처 없이 중복 선언됨

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/PositionRepository.kt:30` · 항목표 data/query · 항목 Q7

```
// ticker 기준 보유 포지션 전체 조회 (QuoteEventListener 시세 갱신용)
fun findByTickerAndQuantityGreaterThan(ticker: String, minQuantity: BigDecimal): List<Position>   // ← 호출처 없음, findOpenByTickerAndMode 와 같은 필터
```

- 원인: findByTickerAndQuantityGreaterThan(ticker, minQuantity)는 QuoteEventListener 용이라고 주석에 있지만 main 코드에 호출처가 없고, 같은 필터(ticker + quantity > min)를 findOpenByTickerAndMode(Querydsl)가 쓰고 있다. 보유 포지션 필터가 두 곳에 있어 한쪽만 바뀔 수 있다.
- 생길 수 있는 일: 보유 판정 규칙이 바뀔 때 죽은 사본이 낡은 의미로 남아 다시 쓰이면 어긋난다.
- 해결: 호출처가 없으면 삭제하고 보유 조회는 PositionRepositoryImpl.findOpenByTickerAndMode 하나로 모은다.
- 다른 렌즈의 반증: data/query+schema-data-model+transaction-boundary — applySellExecutionAtomically 에 flushAutomatically = true, clearAutomatically = true 둘 다 있다

### 617위 [low · 확신 low] summarizeForTax 본문 45줄 — 쿼리 둘과 업무 규칙(통화 혼재 거부)과 결과 매핑이 한 함수

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/SettlementTaxReadRepositoryImpl.kt:17` · 항목표 backend/code-shape · 항목 S1

```
    override fun summarizeForTax(accountId: Long, yearStartInclusive: Instant, yearEndExclusive: Instant): TaxSettlementAggregate {  // ← 본문 18~65행, 빈 줄 제외 45줄(>40)
        val currencyCount = entityManager.createQuery(
        if (currencyCount > 1L) {
            throw UnsupportedSettlementCurrencyException(accountId)  // ← 통화 혼재 거부 규칙이 쿼리 어댑터 안에 있음
        val row = entityManager.createQuery(  // ← 같은 WHERE·바인딩 4개를 복사한 두 번째 쿼리
```

- 원인: 본문 18~65행에서 빈 줄 3개를 빼면 45줄이라 CS25 의 40줄을 넘는다. 줄 수의 약 19줄은 JPQL 텍스트라 코드만 세면 덜하지만, 한 함수가 통화 개수 쿼리, 통화 혼재 거부라는 업무 규칙, 집계 쿼리, 행→TaxSettlementAggregate 매핑을 모두 한다. 같은 WHERE 조건과 파라미터 바인딩 4개가 두 쿼리에 복사되어 있다. detekt LongMethod(60)는 통과했다.
- 생길 수 있는 일: 조건 하나를 바꿀 때 두 쿼리를 함께 고쳐야 하고 하나를 놓치면 통화 검사와 합계가 서로 다른 기간·모드를 본다. 세금 요약 금액이 조용히 틀릴 수 있다.
- 해결: 통화 개수 검사와 집계를 비공개 함수 둘로 나누고, 공통 바인딩(accountId·tradingMode·기간)을 한 곳에서 붙인다. 통화 혼재 거부 판단은 포트 결과(통화 목록·개수)를 받은 유스케이스(TaxSummaryCommandService 쪽)로 올리는 것을 검토한다.
- 다른 렌즈의 반증: reliability/failure-handling+load-concurrency+observability+security-config-hygiene — 상수 텍스트 블록 JPQL 이고 :accountId 등 setParameter 바인딩만 사용

### 618위 [low · 확신 low] 같은 계좌·기간 조건으로 정산 테이블을 두 번 집계

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/SettlementTaxReadRepositoryImpl.kt:18` · 항목표 data/query · 항목 E5

```
            SELECT COUNT(DISTINCT s.currency)       // ← 1번째 집계
            FROM Settlement s
            WHERE s.account.id = :accountId
              AND s.account.tradingMode = :tradingMode
              AND s.settledAt >= :yearStart
              AND s.settledAt < :yearEnd
...
            SELECT COALESCE(SUM(s.realizedPnl), 0),  // ← 2번째 집계: 같은 WHERE 를 다시 스캔
                   COALESCE(SUM(s.fee), 0),
```

- 원인: 통화 종류 수(COUNT DISTINCT)를 구하는 첫 쿼리와 합계(SUM·MIN)를 구하는 둘째 쿼리의 FROM/WHERE 가 완전히 같다. 한 SELECT 에 COUNT(DISTINCT s.currency) 를 같이 넣거나 MIN(currency) 와 MAX(currency) 를 비교하면 한 번에 끝난다.
- 생길 수 있는 일: 계좌마다 연말 일괄이 같은 기간의 정산 행을 두 번 훑는다. 정산 행이 쌓일수록 일괄 시간이 두 배가 된다.
- 해결: 두 쿼리를 하나로 합친다. 통화가 2개 이상이면 COUNT(DISTINCT currency) > 1 로 거른다.
- 다른 렌즈의 반증: reliability/failure-handling+load-concurrency+observability+security-config-hygiene — 상수 텍스트 블록 JPQL 이고 :accountId 등 setParameter 바인딩만 사용

고친 모양(예시):
```
SELECT COALESCE(SUM(s.realizedPnl), 0), COALESCE(SUM(s.fee), 0), COALESCE(SUM(s.tax), 0),
       COALESCE(MIN(s.currency), 'KRW'), COUNT(DISTINCT s.currency)
FROM Settlement s WHERE ...
```

### 619위 [low · 확신 low] 계속 자라는 전략 로그를 상한 없는 List 로 반환

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/StrategyLogRepository.kt:7` · 항목표 data/query · 항목 Q5

```
fun findByStrategyIdOrderByLoggedAtDesc(strategyId: Long): List<StrategyLog>      // ← 상한 없음, 로그는 계속 증가
fun findByStrategyIdAndLogLevelOrderByLoggedAtDesc(strategyId: Long, logLevel: String): List<StrategyLog>
```

- 원인: StrategyLog 는 실행 중 이벤트·오류를 시계열로 쌓는 append 테이블인데, 두 조회 메서드가 Pageable·limit 없이 전체 List 를 돌려준다. main 코드에 호출처가 아직 없어 영향은 미래 사용 시점부터이고, 그래서 confidence 를 낮췄다.
- 생길 수 있는 일: 전략 로그가 쌓인 뒤 이 메서드를 쓰는 화면·API 가 한 번에 전 로그를 당겨 메모리와 응답이 커진다.
- 해결: Pageable(또는 Top100 같은 상한)을 받게 하고, 깊어지면 loggedAt 기준 키셋으로 바꾼다.

고친 모양(예시):
```
fun findByStrategyIdOrderByLoggedAtDesc(strategyId: Long, pageable: Pageable): Slice<StrategyLog>
```

### 620위 [low · 확신 low] 키워드 검색이 컬럼 함수와 앞 와일드카드를 써서 인덱스를 못 탐

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/TradingJournalRepositoryImpl.kt:52` · 항목표 data/query · 항목 Q3

```
.and(
    keyword?.let {
        tradingJournal.title.containsIgnoreCase(it)   // ← lower(title) like '%kw%': 컬럼 함수 + 앞 와일드카드라 인덱스를 못 탄다
            .or(tradingJournal.content.containsIgnoreCase(it))   // ← text 본문도 같은 방식으로 전수 스캔
    }
)
```

- 원인: keyword 조건이 title 과 content 에 containsIgnoreCase 를 건다. Querydsl 은 이를 lower(col) like '%kw%' 로 바꾸므로 B-tree 를 못 쓰고, 대응하는 표현식 인덱스나 pg_trgm 인덱스도 마이그레이션(V2)에 없다(DA21). 다만 where 는 항상 account_id 동등 조건을 선두에 걸어(L45) 스캔이 한 계좌의 일지로 제한된다. 현재 컨트롤러는 keyword 를 넘기지 않아(TradingJournalController.kt:78) 이 경로는 아직 외부에 노출되지 않는다.
- 생길 수 있는 일: 키워드 검색을 열면 계좌의 모든 일지에서 text 본문(content)을 순차 스캔한다. 일지가 쌓일수록 느려지지만, 계좌 단위 사용자 메모라 규모는 제한적이다.
- 해결: 키워드 검색을 외부에 노출하기 전에 pg_trgm GIN 인덱스(lower(title)·lower(content))를 마이그레이션으로 추가하거나 PostgreSQL 전문 검색을 쓴다. 노출 계획이 없으면 쓰지 않는 keyword 분기를 둘지 재검토한다.
- 다른 렌즈의 반증: backend/code-shape+layering+naming-terminology+tdd-purity — 함수 본문 0, 람다 1단이 전부라 중첩 깊이 2 이하. buildWhere 전체 21줄이라 S1 도 아님

### 621위 [low · 확신 low] 스케줄러가 기본 단일 스레드를 다른 작업과 공유

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/scheduler/LocalMatchingScheduler.kt:23` · 항목표 reliability/load-concurrency · 항목 C1

```
@Scheduled(fixedDelay = 30_000)   // ← 기본 단일 스레드 스케줄러 공유
fun cancelExpiredGtdOrders() {
// SlackNotificationScheduler.kt:17 @Scheduled(fixedDelay = 30_000)   // ← 같은 스레드에서 외부 호출
```

- 원인: @EnableScheduling 만 켜져 있고 spring.task.scheduling.pool.size 가 설정돼 있지 않다(application.yaml 확인). 같은 30초 주기의 SlackNotificationScheduler(외부 호출, 같은 단일 스레드)가 있어 서로의 실행을 지연시킬 수 있다.
- 생길 수 있는 일: 슬랙 전송이 느리면 GTD 만료 취소가 밀린다.
- 해결: spring.task.scheduling.pool.size 를 2 이상으로 설정하거나 전용 TaskScheduler 를 둔다.
- 다른 렌즈의 반증: reliability/failure-handling+load-concurrency+observability+security-config-hygiene — cancelOrder 가 findByIdWithOptimisticLock 과 계좌 행 락을 쓰고 원장 멱등 키 cancel-unlock-$orderId 가 있어 두 번째 실행은 상태 검사 또는 락 충돌로 실패한다. 이중 효과 없음, C3 를 low 로 낮춤

고친 모양(예시):
```
spring:
  task:
    scheduling:
      pool:
        size: 4
```

### 622위 [low · 확신 low] 다중 인스턴스 단일 실행 임대가 없고 인스턴스 수 가정도 문서에 없음

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/scheduler/LocalMatchingScheduler.kt:23` · 항목표 reliability/load-concurrency · 항목 C3

```
@Scheduled(fixedDelay = 30_000)   // ← 분산 임대·단일 인스턴스 가정 없음
fun cancelExpiredGtdOrders() {
    val expired = orderRepository.findExpiredOrders(Instant.now())
```

- 원인: ShedLock·Redis 락 같은 분산 임대가 없고 단일 인스턴스 고정 근거(Dockerfile·배포 설정·문서)도 열어 본 범위에 없다. 다만 취소 처리는 계좌 행 락·낙관적 락·원장 멱등 키(cancel-unlock-$orderId)로 이중 실행의 부작용을 막아 이중 취소 위험은 낮다. 대신 실패한 쪽이 WARN 로그를 반복한다.
- 생길 수 있는 일: 인스턴스를 늘리면 같은 만료 주문을 서로 시도해 낙관적 락 실패 경고가 늘어난다. 효과 중복은 없다.
- 해결: ShedLock 을 붙이거나 단일 인스턴스 배포라는 근거를 문서에 남긴다.
- 다른 렌즈의 반증: reliability/failure-handling+load-concurrency+observability+security-config-hygiene — cancelOrder 가 findByIdWithOptimisticLock 과 계좌 행 락을 쓰고 원장 멱등 키 cancel-unlock-$orderId 가 있어 두 번째 실행은 상태 검사 또는 락 충돌로 실패한다. 이중 효과 없음, C3 를 low 로 낮춤

고친 모양(예시):
```
@SchedulerLock(name = "cancelExpiredGtdOrders", lockAtMostFor = "PT1M")
```

### 623위 [low · 확신 low] GTD 취소 실패 로그에 예외 객체가 없음

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/scheduler/LocalMatchingScheduler.kt:34` · 항목표 reliability/failure-handling · 항목 E2

```
}.onFailure {
    log.warn { "GTD 취소 실패: orderId=$orderId, reason=${it.message}" }   // ← 예외 객체 없음
}
```

- 원인: 항목별 경계에서 orderId 는 남기지만 message 만 기록한다. check() 가 던지는 예상 거절과 예상 밖 예외가 같은 형태로 섞인다.
- 생길 수 있는 일: 만료 주문 취소가 반복 실패해도 원인(락 충돌·제약 위반·NPE)을 로그로 못 가른다.
- 해결: log.warn(it) { ... } 로 예외 객체를 넘긴다.
- 다른 렌즈의 반증: reliability/failure-handling+load-concurrency+observability+security-config-hygiene — 반복 루프의 경계이며 WARN 으로 orderId 를 남기고 다음 항목으로 진행한다. 스택 부재만 E2 로 올렸다

고친 모양(예시):
```
.onFailure { log.warn(it) { "GTD 취소 실패: orderId=$orderId" } }
```

### 624위 [low · 확신 low] @Async 가 실행기를 명시하지 않아 기본 실행기에 의존

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/sse/ExecutionSseEventHandler.kt:17` · 항목표 reliability/load-concurrency · 항목 A3

```
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
@Async   // ← 실행기 이름 없음, 기본 실행기 의존
```

- 원인: @Async 에 executor 이름이 없다. AsyncConfig 는 slackNotificationExecutor 하나만 정의하므로 어느 실행기가 기본이 되는지 코드에서 알 수 없다(Spring Boot 가 단일 TaskExecutor 빈을 쓰는지는 설치 패키지로 확인하지 못함). 기본 실행기라면 큐 상한이 보장되지 않고, 슬랙 실행기라면 SSE 와 슬랙이 같은 풀을 나눈다.
- 생길 수 있는 일: SSE 브로드캐스트와 슬랙 전송이 서로 지연시키거나 큐가 무제한으로 쌓일 수 있다.
- 해결: 전용 실행기 빈을 만들고 @Async("executionSseExecutor") 로 지정한다.
- 다른 렌즈의 반증: data/query+schema-data-model+transaction-boundary — AFTER_COMMIT + @Async 라 커밋 뒤 별도 스레드에서 돌고 리스너에 @Transactional 이 없다. 발행처 ExecutionProcessor.kt:210 은 fillLocal(:55)·fillExternal(:70)의 @Transactional 이 감싸는 processFill 안이라 이벤트가 트랜잭션 없이 발행돼 버려질 일도 없다.

고친 모양(예시):
```
@Async("executionSseExecutor")
```

### 625위 [low · 확신 low] SSE 연결 수 상한이 없음

`backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/sse/ExecutionSseRegistry.kt:16` · 항목표 reliability/load-concurrency · 항목 C4

```
fun register(clientId: String, emitter: SseEmitter) {
    emitters[clientId] = emitter   // ← 연결 수 상한 확인 없음
```

- 원인: register 는 연결 수를 제한하지 않는다. 끊김 정리(onCompletion·onTimeout·onError)와 5분 타임아웃은 있어 누수는 아니다. 그러나 호출하는 컨트롤러가 인증 없이 열려 있고(@CrossOrigin("*"), ExecutionSseController.kt:15 확인) 연결 상한이 없다. clientId 는 서버가 만든 UUID(컨트롤러 L26)라 같은 id 덮어쓰기는 없다.
- 생길 수 있는 일: 연결을 계속 열면 emitter 와 broadcast 순회 비용이 무한히 커진다.
- 해결: 등록 시 emitters.size 가 상한을 넘으면 거절한다.
- 다른 렌즈의 반증: backend/code-shape+layering+naming-terminology+tdd-purity — register 는 변경 동사이고 레지스트리가 emitter 를 등록하며 콜백을 거는 것이 이름이 약속하는 행동이다 — 숨은 변경이 아님

고친 모양(예시):
```
if (emitters.size >= MAX_CLIENTS) { emitter.completeWithError(IllegalStateException("too many clients")); return }
```

### 626위 [low · 확신 low] SSE 타임아웃이 단위·의미 없는 숫자 리터럴

`backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/controller/ExecutionSseController.kt:25` · 항목표 backend/naming-terminology · 항목 N9

```
        val emitter = SseEmitter(300_000L)  // ← 단위·의미가 이름에 없는 타임아웃 리터럴(밀리초)
```

- 원인: SseEmitter 생성자 인자는 밀리초지만 이름·타입 어디에도 단위가 없어 300_000L 이 5분인지 읽는 사람이 계산해야 한다. detekt MagicNumber 도 같은 줄을 지적한다(tools/Tb661de0c.txt). 기간 단위 grep 패턴(식별자=숫자)에는 안 걸리지만 타임아웃 리터럴이라 N9 신호다.
- 생길 수 있는 일: 단위를 모른 채 값을 고치면 연결 유지 시간이 1000배 어긋난다.
- 해결: 이름 있는 상수로 단위를 드러낸다(Duration 또는 Millis 접미).
- 다른 렌즈의 반증: backend/code-shape+layering+naming-terminology+tdd-purity — F4 는 application·domain 함수 본문만 대상이고 이 값(clientId)은 어느 시험도 단언에 쓰지 않는다

고친 모양(예시):
```
private val STREAM_TIMEOUT = Duration.ofMinutes(5)
// …
val emitter = SseEmitter(STREAM_TIMEOUT.toMillis())
```

### 627위 [low · 확신 low] ticker 정규화 규칙(trim+uppercase)이 컨트롤러·리포지토리 구현·캐시 어댑터·엔티티에 흩어져 복제됨

`backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/controller/PositionController.kt:26` · 항목표 backend/layering · 항목 D3

```
positionQueryService.getPositionWithCurrentPrice(accountId, ticker.trim().uppercase())  // ← 도메인 식별자 정규화가 presentation 에 있음 (RedisLive...:19 는 trim 없이 uppercase 만)
```

- 원인: 컨트롤러가 trim().uppercase() 로 도메인 식별자를 정규화한다. 같은 규칙이 PositionRepositoryImpl.kt:22(trim+uppercase), RedisLivePositionCacheAdapter.kt:19·21·23(uppercase 만, trim 없음), Position.kt:114(trim+uppercase) 에도 있다. Ticker 값 객체가 없어 규칙 위치가 4곳이고 어댑터는 trim 이 빠져 어긋난다. (LLM-only 판단)
- 생길 수 있는 일: 컨트롤러를 거치지 않는 호출자(시세 리스너 등)가 공백이 낀 ticker 를 넘기면 캐시 키가 어긋나 조회가 조용히 비고, 규칙을 고칠 때 한 곳이 빠진다.
- 해결: 정규화를 도메인 한 곳(Ticker 값 객체 또는 Position 팩토리)에 모으고 컨트롤러·어댑터는 그 결과만 쓴다.

고친 모양(예시):
```
@JvmInline value class Ticker private constructor(val value: String) {
    companion object { fun of(raw: String) = Ticker(raw.trim().uppercase()) }
}
```

### 628위 [low · 확신 low] 같은 Long id 두 개가 나란히 위치 인자로 서비스에 전달됨

`backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/controller/TradingJournalController.kt:91` · 항목표 backend/layering · 항목 D6

```
    fun get(
        @PathVariable journalId: Long,   // ← 같은 Long id 가 나란히
        @RequestParam accountId: Long
    ): TradingJournalResponse = TradingJournalResponse.from(
        tradingJournalQueryService.get(journalId, accountId)   // ← 위치 인자 — 순서를 바꿔도 컴파일됨
```

- 원인: 핸들러 인자는 @PathVariable·@RequestParam 으로 이름이 묶여 안전하지만, 서비스 호출 tradingJournalQueryService.get(journalId, accountId) 는 위치 인자이고 서비스 시그니처도 get(journalId: Long, accountId: Long) 입니다(TradingJournalQueryService.kt:29). 두 값을 맞바꿔도 컴파일됩니다.
- 생길 수 있는 일: 인자를 바꿔 쓰는 실수가 컴파일을 통과해 엉뚱한 일지 조회 또는 계좌 불일치 오류로 이어질 수 있습니다. 소유권 검사 쪽이라 노출 범위가 큽니다.
- 해결: 호출부를 이름 붙인 인자로 바꾸거나 서비스가 TradingJournalId·AccountId 값 클래스를 받게 한다. 최소 조치는 get(journalId = journalId, accountId = accountId).

고친 모양(예시):
```
tradingJournalQueryService.get(journalId = journalId, accountId = accountId)
```

### 629위 [low · 확신 low] Boolean 속성(enabled·webhookConfigured·accepted)에 is/has 접두가 없음

`backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/dto/notification/SlackNotificationDtos.kt:12` · 항목표 backend/naming-terminology · 항목 N3

```
data class SlackNotificationConfigResponse(
    val enabled: Boolean,            // ← is 접두 없음
    val webhookConfigured: Boolean,  // ← 같은 모양
...
data class UpdateSlackNotificationConfigRequest(
    val enabled: Boolean,            // ← L35
...
data class SlackNotificationTestResponse(
    val accepted: Boolean,           // ← L58
```

- 원인: 응답·요청 DTO 의 Boolean 속성이 enabled, webhookConfigured, accepted 처럼 형용사·과거분사이고 is·has·can·should 접두가 없습니다. 렌즈가 나쁜 예로 든 enabled 와 같은 모양입니다. 이 이름은 JSON 필드 이름이기도 해서 바꾸면 응답 계약이 달라지므로, 이 렌즈 하나로 정하지 말고 계약 렌즈와 함께 사람이 정해야 합니다. 이름을 바꿀 경우 @get:JsonProperty 로 JSON 이름은 유지할 수 있습니다.
- 생길 수 있는 일: if (response.enabled) 가 명사처럼 읽혀 의도가 흐려질 수 있습니다. 비용은 낮고 읽기 혼선 수준입니다. 계약을 같이 바꾸면 프런트 호환이 깨질 위험이 있어 이름만 단독으로 바꾸면 안 됩니다.
- 해결: 코드 안 이름을 isEnabled·isWebhookConfigured·isAccepted 로 바꾸고 직렬화 이름은 유지하거나, API 계약 변경으로 함께 처리합니다.
- 다른 렌즈의 반증: backend/code-shape+layering+naming-terminology+tdd-purity — 이름에 Millis 접미사가 있어 단위가 드러남(retryBackoffMillis 도 같음) — 통과

고친 모양(예시):
```
data class SlackNotificationConfigResponse(
    @get:JsonProperty("enabled") val isEnabled: Boolean,
    @get:JsonProperty("webhookConfigured") val isWebhookConfigured: Boolean,
    // ...
)
```

### 630위 [low · 확신 low] Boolean 속성 force 에 is·should 접두가 없음

`backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/dto/portfolio/TaxSummaryDto.kt:33` · 항목표 backend/naming-terminology · 항목 N3

```
data class RecalculateTaxSummaryRequest(
    val force: Boolean = false,   // ← Boolean 인데 is/has/should 접두 없음
```

- 원인: RecalculateTaxSummaryRequest.force 는 함수 밖으로 나가는 Boolean 속성인데 접두가 없어 명사처럼 읽힙니다. 다만 이 이름은 JSON 요청 본문 필드라 바꾸면 API 계약이 바뀌므로 신뢰도를 낮게 둡니다.
- 생길 수 있는 일: if (request.force) 가 값처럼 읽혀 의도(강제 재계산 여부)를 놓치기 쉽습니다. 외부 계약이라 실제 피해는 작습니다.
- 해결: API 필드 이름을 유지해야 하면 JSON 이름만 고정하고 코드 이름은 shouldForce 로 바꾼다.

고친 모양(예시):
```
data class RecalculateTaxSummaryRequest(
    @field:JsonProperty("force") val shouldForce: Boolean = false,
)
```

### 631위 [low · 확신 low] @Scheduled 작업에 분산 단일 실행 임대가 없고 단일 인스턴스 가정이 코드에 없음

`backend/trading-api/src/main/kotlin/com/papertrading/api/TradingApiApplication.kt:11` · 항목표 reliability/load-concurrency · 항목 C3

```
@EnableScheduling   // ← 다중 인스턴스 단일 실행 임대·인스턴스 수 가정이 코드에 없음
```

- 원인: ShedLock·Redis 락·advisory lock 이 없다. 단일 인스턴스 근거는 docker-compose.yml 의 container_name: trading-api 고정뿐이다. Slack 스케줄러에는 markDelivering 점유 로직이 있다.
- 생길 수 있는 일: 인스턴스를 늘리면 GTD 만료 취소 같은 주기 작업이 중복 실행된다. 현재 배포 근거가 있어 낮게 본다.
- 해결: 단일 인스턴스 전제를 문서·설정에 적거나 ShedLock 같은 임대를 도입한다.

### 632위 [low · 확신 low] 느린 쿼리 로그·Hibernate 통계·pg_stat_statements 가 레포 어디에도 없음 (batch fetch 크기만 설정됨)

`backend/trading-api/src/main/resources/application.yaml:22` · 항목표 data/query · 항목 Q13

```
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true
        default_batch_fetch_size: 100
        jdbc:
          time_zone: UTC   # ← 느린 쿼리 로그(LOG_QUERIES_SLOWER_THAN_MS)·generate_statistics 설정이 없다
# docker-compose.yml:3
    image: postgres:16-alpine   # ← command·shared_preload_libraries(pg_stat_statements)·log_min_duration_statement 가 없다
```

- 원인: Q13 은 레포 단위로 세 가지를 묻는다. 1. default_batch_fetch_size: trading-api application.yaml:26 에 100 으로 있음(통과). 2. 느린 쿼리 로그(hibernate.session.events.log.LOG_QUERIES_SLOWER_THAN_MS)·generate_statistics: application.yaml 에 없고, yaml·properties·kt·kts·sql·md 전체를 grep 해도(docs/audits 제외) 설정이 한 줄도 없음. 3. PostgreSQL pg_stat_statements: docker-compose.yml 의 postgres 서비스는 image: postgres:16-alpine 뿐이고 command·shared_preload_libraries·log_min_duration_statement 가 없으며, 운영에서 켠다는 근거 문서도 없음. 설정을 켜지 않았다는 것은 확인했지만 운영 DB 설정이 레포 밖에 있는지는 레포만으로 알 수 없다.
- 생길 수 있는 일: 운영에서 어떤 쿼리가 느린지 볼 수단이 없다. 원장·체결·주문 이력처럼 계속 자라는 테이블에서 느려져도 사용자 제보 전까지 모르고, 이 렌즈의 다른 발견(Q3·Q6·E4)에 우선순위를 줄 근거도 못 얻는다.
- 해결: trading-api 에 느린 쿼리 임계값(예 200ms)을 Hibernate 설정으로 켠다. generate_statistics 는 부하가 있으니 프로파일로만 켠다. PostgreSQL 은 pg_stat_statements 를 preload 하고 log_min_duration_statement 를 둔다. 운영 DB 가 레포 밖에서 관리되면 그 설정 위치를 docs 에 적어 근거로 남긴다.
- 다른 렌즈의 반증: reliability/observability — show-sql: false 이고 application.yaml 에 org.hibernate.orm.jdbc.bind 설정이 0건이라 켜져 있지 않음. O11 의 세 번째 조건은 통과

고친 모양(예시):
```
# application.yaml
spring:
  jpa:
    properties:
      hibernate:
        session.events.log.LOG_QUERIES_SLOWER_THAN_MS: 200

# docker-compose.yml (postgres 서비스)
    command: ["postgres", "-c", "shared_preload_libraries=pg_stat_statements", "-c", "log_min_duration_statement=500"]
```

### 633위 [low · 확신 low] 실전 KIS WebSocket 기본 URL 이 평문 ws:// 이고 근거 주석이 없음

`backend/trading-api/src/main/resources/application.yaml:44` · 항목표 reliability/security-config-hygiene · 항목 S9

```
    live-ws-url: ${KIS_LIVE_WS_URL:ws://ops.koreainvestment.com:21000}   # ← 실전 WebSocket 기본값이 평문 ws:// (근거 없음)
```

- 원인: 모의(31000)·실전(21000) 모두 ws://ops.koreainvestment.com 이다. KIS 가 공식 제공하는 주소로 보이나 설정에 그 근거가 적혀 있지 않다. 실전 승인 키와 체결 통보가 평문 채널로 오갈 수 있다.
- 생길 수 있는 일: 경로상 중간자가 승인 키·체결 통보를 가로챌 수 있다. KIS 공식 주소라 낮게 본다.
- 해결: KIS 공식 문서상 평문만 제공된다는 근거를 주석으로 남기고, wss 가 제공되면 기본값을 wss 로 바꾼다.
- 다른 렌즈의 반증: reliability/failure-handling+load-concurrency+observability+security-config-hygiene — KIS 모의투자 공식 제공 주소로 보임. 실전 URL(44행)과 묶어 low 발견 하나로 기록

### 634위 [low · 확신 low] Actuator 설정이 없어 liveness·readiness 구분과 헬스 소비자가 없음

`backend/trading-api/src/main/resources/application.yaml:60` · 항목표 reliability/observability · 항목 O9

```
server:
  port: 8080   # ← management.endpoint.health.probes·groups 설정 없음
```

- 원인: build.gradle 에 actuator 의존은 있으나 management.* 설정이 하나도 없고 docker-compose 의 trading-api 에 healthcheck 도 없다. 기본 /actuator/health 는 단일 엔드포인트이며 노출 기본값은 health 뿐이라 include: * 문제는 없다.
- 생길 수 있는 일: 오케스트레이터가 앱 상태를 판단할 근거가 없다. 나중에 /health 를 재시작 판정에 붙이면 DB·Redis 의존이 liveness 에 섞인다.
- 해결: management.endpoint.health.probes.enabled 로 liveness·readiness 를 나누고 readiness 그룹에만 db·redis 를 넣는다.

### 635위 [low · 확신 low] Instant 필드에 대응하는 열이 시간대 없는 TIMESTAMP — Hibernate 6 기본 매핑(timestamptz)과 다를 수 있음

`backend/trading-api/src/main/resources/db/migration/V7__create_position_exit_triggers.sql:15` · 항목표 data/schema-data-model · 항목 M6

```sql
  triggered_at TIMESTAMP,                        -- ← L15 Instant 필드, 시간대 없는 타입
  last_evaluated_at TIMESTAMP,
  created_at TIMESTAMP NOT NULL DEFAULT now(),
```

- 원인: 엔티티의 triggered_at·last_evaluated_at·created_at·updated_at 은 Instant 인데 V7 은 TIMESTAMP(시간대 없음)로 만든다. Hibernate 6 은 Instant 를 PostgreSQL timestamp with time zone 으로 매핑하는 것이 기본이라, ddl-auto 로 만든 테이블(positions 등)과 마이그레이션 테이블이 같은 개념에 다른 타입을 쓸 수 있다. 실제 DB 를 보지 않았다.
- 생길 수 있는 일: 테이블마다 시간 타입이 달라 조인·비교에서 캐스트가 필요해지고, 세션 시간대에 따라 값이 어긋날 수 있다.
- 해결: 한 레포에서 시각 열 타입을 정하고(예: timestamptz) 새 마이그레이션에서 맞춘다. 팀 time.md 의 KST naive 방침과 부딪히므로 사람이 정한다.

### 636위 [low · 확신 low] 시험 이름 `compute scale 4 and expected tax` 에 상황과 기대가 없음

`backend/trading-api/src/test/kotlin/com/papertrading/api/application/portfolio/tax/TaxSummaryCalculatorTest.kt:14` · 항목표 backend/tdd-purity · 항목 T6

```
fun `compute scale 4 and expected tax`() {  // ← 상황·기대 불명확
```

- 원인: 어떤 입력에서 무엇이 기대되는지 이름에서 알 수 없다(실제로는 정상 입력에서 scale 4·세금 값 확인).
- 생길 수 있는 일: 실패했을 때 어느 정책이 깨졌는지 이름만으로 모른다.
- 해결: `KRW 정산 합계가 주어지면 scale 4 로 반올림한 과세표준과 예상세액을 돌려준다` 꼴로 쓴다.

고친 모양(예시):
```
fun `KRW 합계가 주어지면 scale 4 과세표준과 예상세액을 돌려준다`()
```

### 637위 [low · 확신 low] BigDecimal 을 assertEquals 로 scale 까지 비교

`backend/trading-api/src/test/kotlin/com/papertrading/api/application/portfolio/tax/TaxSummaryCommandServiceTest.kt:76` · 항목표 backend/tdd-purity · 항목 T8

```
assertEquals(BigDecimal("1000.0000"), summary.totalRealizedPnl)  // ← scale 까지 비교
assertEquals(BigDecimal("850.0000"), summary.taxablePnl)
```

- 원인: summary.totalRealizedPnl 등을 "1000.0000" 꼴로 assertEquals 비교한다(PTB4). scale 4 는 계산기 시험이 따로 못 박아 이 시험이 같은 scale 을 또 고정할 필요는 없다. 다만 scale 4 가 저장 계약이라 신뢰도는 낮춘다.
- 생길 수 있는 일: scale 만 바뀌어도 서비스 시험이 깨져 값 회귀와 표현 회귀가 섞인다.
- 해결: isEqualByComparingTo 로 비교하고 scale 은 계산기 시험에 맡긴다.

고친 모양(예시):
```
assertThat(summary.taxablePnl).isEqualByComparingTo("850")
```

### 638위 [low · 확신 low] 시험 이름 일부가 상황·기대를 말하지 않음 — 값을_재계산한다, 0이면_정상_처리된다

`backend/trading-api/src/test/kotlin/com/papertrading/api/domain/entity/portfolio/TaxSummaryTest.kt:33` · 항목표 backend/tdd-purity · 항목 T6

```
    fun recalculate_값을_재계산한다() {   // ← 상황·기대 없음, 메서드 이름 반복
    fun recalculate_예상세금이_0이면_정상_처리된다() {   // ← «정상 처리» 가 기대를 말하지 않음
```

- 원인: recalculate_값을_재계산한다(L33)는 대상 메서드 이름을 반복할 뿐 상황이 없고, recalculate_예상세금이_0이면_정상_처리된다(L48)의 «정상 처리»는 실제 기대(estimatedTax 가 0.0000 으로 정규화, status 유지)를 말하지 않는다. 나머지 세 시험 이름은 상황_행동_기대가 분명하다.
- 생길 수 있는 일: L48 이 빨개져도 어떤 정책(scale 정규화인지 0 허용인지)이 깨졌는지 이름만으로 알 수 없어 진단 비용이 든다.
- 해결: recalculate_세금이_0이면_estimatedTax_는_0.0000_으로_정규화된다, recalculate_새_값을_주면_세_금액이_scale_4_로_교체된다 처럼 기대를 이름에 넣는다.

### 639위 [low · 확신 low] create_필드가_정상_설정된다 — «정상» 이 기대를 말하지 않고 상황이 없음

`backend/trading-api/src/test/kotlin/com/papertrading/api/domain/entity/portfolio/TradingJournalTest.kt:15` · 항목표 backend/tdd-purity · 항목 T6

```
    fun create_필드가_정상_설정된다() {   // ← «정상 설정» 은 상황·기대가 아님
            journalType = "TRADE",   // ← 이미 대문자라 uppercase 정책은 검증되지 않음
```

- 원인: create_필드가_정상_설정된다 는 어떤 입력에서 무엇이 설정되는지(trim·uppercase 정규화 포함)를 말하지 않는다. 시험은 입력 "TRADE"·"005930"·"BULLISH" 가 이미 대문자라 정규화 정책은 이 시험이 지키지도 않는다.
- 생길 수 있는 일: 이 시험이 빨개져도 정책 위반인지 단순 대입 오류인지 이름으로 알 수 없고, trim·uppercase 정책은 시험으로 보호받지 못한다.
- 해결: create_모든_값을_주면_필드가_그대로_설정된다 와 create_앞뒤_공백과_소문자가_있으면_trim_후_대문자로_정규화된다 처럼 나눠 쓴다.

### 640위 [low · 확신 low] @WebMvcTest 슬라이스에서 내부 서비스 두 개를 @MockBean 으로 갈음 (렌즈 기본값 high 를 낮춤)

`backend/trading-api/src/test/kotlin/com/papertrading/api/interfaces/rest/position/PositionExitTriggerControllerTest.kt:38` · 항목표 backend/tdd-purity · 항목 T1

```
@MockBean lateinit var commandService: PositionExitTriggerCommandService  // ← 같은 레포 서비스를 목으로 대체 (deprecated @MockBean)
@MockBean lateinit var queryService: PositionExitTriggerQueryService
```

- 원인: PositionExitTriggerCommandService·QueryService 를 @MockBean 으로 대체한다. 컨트롤러 슬라이스의 관례라 허용 목록에는 없지만 팀이 거부한다는 근거(rules/tests.md 는 PTT1 뿐)가 없어 사람이 정할 사항으로 올린다. @MockBean 은 Spring Boot 3.4 에서 deprecated 라 ArchUnit 실행 출력 58-60행에도 경고가 있다. 청산 트리거 엔드포인트는 전체 스택 시험이 없다(PositionControllerIntegrationTest 는 포지션 조회만).
- 생길 수 있는 일: 서비스 시그니처·예외 변환이 바뀌어도 슬라이스 시험은 초록이다. 청산 트리거 API 의 DB·검증 연동은 어떤 시험도 보지 않는다.
- 해결: @MockitoBean 으로 교체하고, 청산 트리거 생성·수정·취소를 한 번은 PositionControllerIntegrationTest 방식으로 실제 DB 까지 시험한다.

고친 모양(예시):
```
@MockitoBean lateinit var commandService: PositionExitTriggerCommandService
```

### 641위 [low · 확신 low] 시험 이름의 기대가 «정상 동작한다» 로 모호하다

`backend/trading-api/src/test/kotlin/com/papertrading/api/presentation/controller/AccountControllerIntegrationTest.kt:69` · 항목표 backend/tdd-purity · 항목 T6

```
    fun `계좌_조회_입금_출금_원장조회_전체_흐름이_정상_동작한다`() {  // ← 기대가 «정상 동작» 뿐
```

- 원인: 계좌_조회_입금_출금_원장조회_전체_흐름이_정상_동작한다 는 상황은 있으나 기대(단계별 잔액·원장 3건)가 없다. 렌즈가 나쁜 이름으로 드는 «정상 동작» 꼴이다.
- 생길 수 있는 일: 빨간 시험이 입금·출금·원장 중 어느 정책이 깨졌는지 이름으로 말하지 못한다.
- 해결: T12 와 함께 입금·출금·원장 시험으로 나누고 이름에 기대 값을 쓴다.

고친 모양(예시):
```
@Test fun `입금_후_출금하면_잔액이_초기금액에서_차감된다`() { /* … */ }
```

### 642위 [low · 확신 low] 시험 하나가 입금·출금·조회·원장 4개 호출에 각자 단언을 달고 단언이 10개를 넘는다

`backend/trading-api/src/test/kotlin/com/papertrading/api/presentation/controller/AccountControllerIntegrationTest.kt:91` · 항목표 backend/tdd-purity · 항목 T12

```
        mockMvc.post("/api/v1/accounts/$accountId/deposit") {  // ← 호출 1(입금)과 자기 단언
...
        mockMvc.post("/api/v1/accounts/$accountId/withdraw") {  // ← 호출 2(출금)와 자기 단언
...
        mockMvc.get("/api/v1/accounts/$accountId/ledgers").andExpect {  // ← 호출 4(원장)와 자기 단언
```

- 원인: 한 시험에서 생성 1회 뒤 deposit·withdraw·계좌 조회·ledgers 조회를 차례로 호출하고 호출마다 단언을 단다(생성 1 + 입금 3 + 출금 3 + 조회 2 + 원장 2 = 11개). 입금이 실패하면 출금·조회·원장 정합은 확인되지 않는다.
- 생길 수 있는 일: 첫 실패가 나머지를 가리고 이름 하나가 4개 행동을 담지 못해 어느 정책이 깨졌는지 진단 비용이 든다.
- 해결: 계좌 생성은 공용 준비 함수로 빼고 입금·출금·원장 시험으로 나눈다.
- 다른 렌즈의 반증: backend/code-shape+layering+naming-terminology+tdd-purity — 요청 본문의 idempotencyKey 입력값으로만 쓰고 단언에 값을 비교하지 않는다. 시각·난수 기댓값 비교가 없어 비결정이 아니다.

고친 모양(예시):
```
private fun 계좌를_만든다(initial: Int): Long { /* post + id 반환 */ }

@Test fun `입금하면_잔액이_늘고_원장에_입금_행이_남는다`() { val id = 계좌를_만든다(500000); /* deposit + 단언 */ }
```

### 643위 [low · 확신 low] 컨트롤러 시험이 같은 레포 서비스 3개를 @MockBean 으로 갈음하고 @MockBean 은 폐기 경고

`backend/trading-api/src/test/kotlin/com/papertrading/api/presentation/controller/PortfolioSnapshotControllerTest.kt:38` · 항목표 backend/tdd-purity · 항목 T1

```
    @MockBean   // ← 같은 레포 서비스를 Mockito 가짜로 갈음(@MockBean 은 Boot 3.4+ 폐기 경고)
    lateinit var dailyBalanceQueryService: DailyBalanceQueryService

    @MockBean   // ← 같은 위반
    lateinit var portfolioSnapshotQueryService: PortfolioSnapshotQueryService

    @MockBean   // ← 같은 위반
    lateinit var snapshotJobService: SnapshotJobService
```

- 원인: DailyBalanceQueryService·PortfolioSnapshotQueryService·SnapshotJobService 는 허용 목록 밖의 내부 협력자입니다. @WebMvcTest 슬라이스에서 서비스를 가짜로 두는 것은 흔한 방식이고 가짜가 돌려주는 값은 DailyBalance.create·PortfolioSnapshot.create 로 만든 실제 엔티티라서 심각도는 high 에서 low 로 낮추고 신뢰도도 low 입니다. 레포 규칙(tests.md)에 슬라이스 목 허용 조항은 없습니다. 이 묶음에는 같은 컨트롤러를 실제 서비스·DB 로 타는 통합 시험이 없습니다. 컴파일 로그(tools/T96587ad0.txt:61-64)에 @MockBean 폐기 경고 4건이 있습니다.
- 생길 수 있는 일: given(...) 스터빙이 서비스 메서드 시그니처·인자값에 묶여 리팩터링마다 깨지고, 컨트롤러와 실제 서비스·영속 경로가 이어지는지는 이 시험이 보증하지 못합니다.
- 해결: 폐기 경고를 없애려면 @MockitoBean 으로 바꾼다. 서비스 협력은 TradingJournal 시험처럼 Testcontainers 통합 시험 하나로 덮고, 슬라이스 시험은 HTTP 매핑·오류 코드 검증에 한정한다.

고친 모양(예시):
```
import org.springframework.test.context.bean.override.mockito.MockitoBean

    @MockitoBean
    lateinit var dailyBalanceQueryService: DailyBalanceQueryService
```

### 644위 [low · 확신 low] 컨트롤러 시험이 같은 레포 서비스 3개를 @MockBean 으로 갈음하고 @MockBean 은 폐기 경고

`backend/trading-api/src/test/kotlin/com/papertrading/api/presentation/controller/TaxSummaryControllerTest.kt:40` · 항목표 backend/tdd-purity · 항목 T1

```
    @MockBean   // ← 같은 레포 서비스를 Mockito 가짜로 갈음
    lateinit var taxSummaryQueryService: TaxSummaryQueryService

    @MockBean   // ← 같은 위반
    lateinit var taxSummaryCommandService: TaxSummaryCommandService

    @MockBean   // ← 같은 위반
    lateinit var taxSummaryBatchService: TaxSummaryBatchService
```

- 원인: TaxSummaryQueryService·TaxSummaryCommandService·TaxSummaryBatchService 를 @MockBean 으로 대체합니다. PortfolioSnapshotControllerTest 와 같은 사유이며 같은 이유로 심각도 low·신뢰도 low 입니다. 컴파일 로그(tools/T96587ad0.txt:65-68)에 @MockBean 폐기 경고 4건이 있습니다.
- 생길 수 있는 일: 스터빙이 서비스 시그니처에 묶여 리팩터링마다 깨지고 세금 요약 컨트롤러의 실제 서비스·영속 경로 연결은 시험되지 않습니다.
- 해결: @MockitoBean 으로 교체하고, 서비스 협력은 실제 DB 를 쓰는 통합 시험으로 덮는다.

### 645위 [low · 확신 low] 수신 객체를 바꾸면서 돌려주는 withId 이름이 복사본을 약속함

`backend/trading-api/src/test/kotlin/com/papertrading/api/support/TestEntityUtils.kt:3` · 항목표 backend/code-shape · 항목 F3

```
fun <T : Any> T.withId(id: Long): T = apply {   // ← with* 이름인데 수신 엔티티를 제자리에서 바꾸고 같은 객체를 돌려줌
    javaClass.getDeclaredField("id").also {
        it.isAccessible = true
        it.set(this, id)
```

- 원인: fun T.withId(id) 가 apply { … it.set(this, id) } 로 수신 엔티티의 private id 필드를 리플렉션으로 바꾸고 같은 객체를 돌려준다. with* 접두는 보통 «값만 다른 새 객체»를 뜻한다. 시험 보조라 영향은 작다.
- 생길 수 있는 일: 호출자가 사본을 받는다고 믿고 원본 엔티티를 다른 시험 픽스처와 공유하면 id 가 등 뒤에서 바뀐다(시험 한정).
- 해결: 이름을 assignId 처럼 변경을 드러내게 바꾸거나, 반환 없이 제자리 변경임을 문서화한다.
- 다른 렌즈의 반증: backend/code-shape+layering+naming-terminology+tdd-purity — T3 은 리플렉션으로 읽어 단언하는 경우다. 여기는 생성형 id 에 setter 가 없어 픽스처 id 를 주입하는 쓰기 도우미이고 단언이 없다.

### 646위 [low · 확신 low] 본문이 빈 contextLoads 시험에 단언이 하나도 없음

`backend/trading-api/src/test/kotlin/com/papertrading/api/TradingApiApplicationTests.kt:28` · 항목표 backend/tdd-purity · 항목 T7

```
    @Test
    fun contextLoads() {   // ← 본문이 비어 있어 단언 0개
    }
```

- 원인: @Test contextLoads() 의 본문이 비어 있고 assertDoesNotThrow 도 없다(detekt EmptyFunctionBlock 과 일치). 컨텍스트가 못 뜨면 시험이 실패하므로 완전한 가짜 초록은 아니고, 이 시험이 지키는 약속이 코드에 한 줄도 적혀 있지 않다는 점이 문제다.
- 생길 수 있는 일: 무엇을 보장하는지 명시되지 않아 누군가 지워도 이유를 알 수 없고, 컨텍스트가 떠도 필수 빈(예 시세 리스너·실행기)이 빠진 상태를 이 시험은 잡지 못한다.
- 해결: 컨텍스트에서 꼭 있어야 하는 빈 한두 개를 단언하거나 assertDoesNotThrow 로 의도를 드러낸다.
- 다른 렌즈의 반증: backend/code-shape+layering+naming-terminology+tdd-purity — Spring 스모크 시험의 관례 이름이다. 상황은 클래스의 @ActiveProfiles(test)·Testcontainers 선언에, 기대는 이름에 있어 한 문장으로 녹아 있다고 보고 통과시켰다.

고친 모양(예시):
```
@Autowired lateinit var context: ApplicationContext

@Test
fun `테스트 프로필과 컨테이너로 애플리케이션을 띄우면 필수 빈이 등록된다`() {
    assertThat(context.getBean("jpaQueryFactory")).isNotNull()
}
```

## 3. 기준 충돌 — 항목표가 위반이라 하는데 팀 규칙이 허용(사람이 고른다)

| 항목표 | 항목 | 팀 규칙 | 내용 |
|---|---|---|---|
| backend/code-shape+layering+naming-terminology+tdd-purity | layering/D1 | rules/backend-api.md PTB1 (main 판본 Exception — PR #50 미머지) | main 판본 PTB1 Exception 은 «금액도 전이 규칙도 없는 단순 CRUD·설정 성격 컨텍스트는 JPA 엔티티를 그대로 쓴다»고 허용한다. AccountExitTriggerDefault(설정 성격)와 RiskEvent(append-only 이력)는 이 예외에 해당할 수 있는데 이 렌즈는 PR #50 판본으로 발견을 남겼다. Account·AccountLedger·RiskPolicy 는 금액이 있어 예외 대상이 아니다. 사람이 판본을 결정한다. |
| backend/code-shape+layering+naming-terminology+tdd-purity | layering/D1 | rules/backend-api.md PTB1 (main 판본 Exception — PR #50 미머지) | main 판본 Exception 은 단순 CRUD 컨텍스트의 JPA 엔티티를 domain 에 두는 것을 허용한다. Notification 은 필드만 있는 모델이 아니라 상태 전이 메서드와 생성 검증을 가져 해당 여부가 애매하다. 렌즈는 PR #50 판본으로 발견을 유지하며, 어느 쪽을 고칠지는 사람이 정한다. |
| backend/layering | D1 | rules/backend-api.md PTB1 (main 판본 Exception — PR #50 미머지) | main 판본은 단순 CRUD 컨텍스트의 JPA 엔티티를 domain 에 두는 것을 허용한다. OrderAmendment(감사 기록)·SettlementExecution(조인 엔티티)·OrderSignal(필드 중심)은 그 예외에 해당할 수 있다. 렌즈는 PR #50 판본(JPA 엔티티는 infrastructure 만)으로 판정해 발견으로 남겼다 — 사람이 어느 쪽을 따를지 정한다. |
| backend/code-shape+layering+naming-terminology+tdd-purity | T2 | rules/tests.md PTT1 Exception | 규칙은 유스케이스 단위 시험에서 포트를 가짜로 바꾸는 것을 허용한다. 이 레포는 아직 포트 대신 Spring Data 리포지토리를 주입하므로(L1·PTB8 기존 위반) 렌즈는 리포지토리 목을 T2 발견으로 본다. 포트 이전 전까지 허용할지는 사람이 정한다. |
| backend/code-shape+layering+naming-terminology+tdd-purity | layering/D1 | rules/backend-api.md PTB1 (main 판본 Exception — PR #50 미머지) | main 판본 Exception(rules/backend-api.md:18)은 금액·전이 규칙 없는 단순 CRUD·설정 컨텍스트가 JPA 엔티티를 domain 에 두는 것을 허용한다. BaseAuditEntity·BaseTimeEntity 는 그런 엔티티와 Account 같은 금액 엔티티가 함께 쓰는 공통 부모라, 단순 CRUD 만의 것이 아니다. 렌즈(PR #50 판본)는 발견으로 올렸고, 베이스 클래스에 main 판본 예외를 적용할지는 사람이 정한다. |
| backend/code-shape+layering+naming-terminology+tdd-purity | layering/D1 | rules/backend-api.md PTB1 (main 판본 Exception — PR #50 미머지) | main 판본 PTB1 Exception 은 단순 CRUD 컨텍스트의 JPA 엔티티를 domain 에 두는 것을 허용한다. 이 묶음에서는 TradingJournal(매매 일지)이 가장 CRUD 에 가깝다. 렌즈는 PR #50 판본으로 D1 발견을 유지했고, 일지·실행 기록을 단순 CRUD 로 볼지는 사람이 정한다. |
| backend/layering | L1 | rules/backend-api.md PTB1 Exception (단순 CRUD 는 JPA 엔티티를 그대로 쓴다) 와 PTB8 (유스케이스는 Spring Data 리포지토리를 직접 주입받지 않는다) | TradingJournal 은 금액·전이 규칙이 없는 단순 CRUD 일지라 PTB1 예외 대상으로 보이나 PTB8 에는 CRUD 예외가 없다. 정면 충돌은 아니고 모호하다. TradingJournalCommandService·TradingJournalQueryService 의 L1·L2 를 발견으로 남겼으니 단순 CRUD 에도 포트를 요구할지 사람이 정한다. |
| backend/tdd-purity | T2 | rules/tests.md PTT1 Exception (유스케이스 단위 시험은 포트를 가짜로 바꿔 엔진 없이 돌린다) | PTT1 이 허용하는 것은 «포트 가짜»인데 이 레포는 아직 포트 대신 Spring Data 리포지토리를 유스케이스에 주입해(PTB8 기존 위반) 이 묶음 8개 시험 파일이 T2 로 남는다. «포트 이전 전까지 허용»으로 고를지 사람이 정한다. |
| backend/code-shape+layering+naming-terminology+tdd-purity | tdd-purity/T2 | rules/tests.md PTT1 (Exception: 유스케이스 단위 시험은 포트를 가짜로) · rules/backend-api.md PTB8 | 유스케이스가 아직 포트 대신 Spring Data 리포지토리를 주입받는다(PTB8 기존 위반, ArchUnit 동결 369-377행). 그래서 mockk<PositionRepository>·mockk<OrderRepository> 는 PTT1 의 «포트 가짜»가 못 되어 T2 발견으로 남겼다. 사람이 «포트 이전 전까지 허용»으로 고르면 PositionCommandService·ExitTriggerCommandService·Orchestrator 시험의 T2 3건을 접는다. |
| backend/code-shape+layering+naming-terminology+tdd-purity | layering/D1 | rules/backend-api.md PTB1 (main 판본 Exception — PR #50 미머지) | 단순 CRUD 컨텍스트의 JPA 엔티티를 domain 에 두는 것을 main 판본은 허용한다. 필드·팩토리 위주인 StrategyLog·StrategyDerivation·StrategyPerformanceSnapshot·StrategyVersion 이 해당할 수 있어 렌즈(PR #50 판본)는 발견으로 남겼다. Strategy 는 상태 전이 메서드가 있어 단순 CRUD 가 아니므로 이 충돌 대상이 아니다. 사람이 어느 판본을 따를지 결정한다. |
| data/query+schema-data-model+transaction-boundary | schema-data-model/M6 | rules/dev-team/time.md (KST naive 컬럼 — 렌즈 Standard 가 인용한 팀 기본값. 이 세션에서 원문은 열지 않음) | V6 의 TIMESTAMP(naive)는 팀 기본값과 맞지만 이 레포는 Instant(UTC, jdbc.time_zone: UTC)를 쓰고 레포 규칙에는 시간대 조항이 없다. 발견은 «한 레포 안 혼용»으로만 올렸고, 어느 쪽으로 통일할지는 사람이 고른다. |
| data/query+schema-data-model+transaction-boundary | transaction-boundary/X7 | rules/failure-handling.md PTF6 (예수금 홀드는 주문을 받는 요청 경로 트랜잭션 안에서 즉시) | 렌즈 X7 은 Order·Account·Position 을 한 커밋에 바꾸는 것을 발견으로 보지만, 레포 규칙 PTF6 은 주문 저장과 홀드를 같은 트랜잭션에 두라고 요구합니다(OrderCommandService.kt:49-66). ExecutionProcessor 도 클래스 주석이 단일 트랜잭션을 의도로 밝힙니다. 사람이 렌즈와 규칙 중 어느 쪽을 고칠지 정합니다. |
| data/query+schema-data-model+transaction-boundary | schema-data-model/M6 | rules/dev-team/time.md (KST naive 컬럼) | 엔티티는 Instant 를 써서 timestamptz(UTC, application.yaml 의 jdbc.time_zone: UTC)로 매핑됩니다(executed_at·expire_at·settled_at·effective_from/until). 이 레포 규칙엔 시간대 조항이 없고 한 레포 안 혼용 증거도 이 묶음에서는 못 찾았습니다. timestamptz 자체의 채택 여부는 사람이 고릅니다. |
| data/query+schema-data-model+transaction-boundary | schema-data-model/M6 | rules/dev-team/time.md (KST naive 컬럼, devkit 팀 기본값) | BaseTimeEntity·BaseAuditEntity 는 Instant 로 시각을 저장하고, Hibernate 6 에서 timestamptz(UTC)로 매핑되는 것으로 추정합니다(DB 에 붙지 않아 DDL 은 미확인). application.yaml L27-28 도 jdbc.time_zone: UTC 입니다. 이 레포 규칙에는 시간대 조항이 없고 이 묶음 안에는 혼용이 없어 발견으로는 올리지 않았습니다. timestamptz 를 쓰는 것 자체는 사람이 고르게 둡니다. |

## 4. 안 본 파일 (커버리지 구멍 — «대충 봄»이 여기 드러난다)

없음 — 도착한 인스턴스는 모두 자기 범위 파일 전부에 항목별 결과를 남겼다

## 5. 안 돌린 결정적 도구 (설치·실행 뒤 재감사 대상)

| 도구 | 구분 | 인스턴스 | 이유 |
|---|---|---|---|
| grep (import 간선) | 안 돌림 | 1개 (backend/layering#repo) | per-target 행이지만 이 인스턴스는 scope=repo 라 대상 파일 묶음이 없다 — D1·L1~L5 는 files 범위 인스턴스(backend#N)가 판정한다 |
| grep (fetch join·페이징) | 안 돌림 | 1개 (data/query#repo) | manifest 의 per-target 행인데 이 인스턴스는 scope=repo 라 묶음 대상 파일이 없음 — Q13 만 판정 |
| grep (구간·함수 조건) | 안 돌림 | 1개 (data/query#repo) | manifest 의 per-target 행인데 이 인스턴스는 scope=repo 라 묶음 대상 파일이 없음 — Q13 만 판정 |
| grep (무제한 목록) | 안 돌림 | 1개 (data/query#repo) | manifest 의 per-target 행인데 이 인스턴스는 scope=repo 라 묶음 대상 파일이 없음 — Q13 만 판정 |
| grep (경계·리스너) | 안 돌림 | 1개 (data/transaction-boundary#repo) | per-target 행이라 파일 묶음용이다. 이 인스턴스는 scope 가 repo 라서 도구 호출 상한 안에서 X13 용 레포 전체 grep 으로 대신했다. |
| grep (원격 호출) | 안 돌림 | 1개 (data/transaction-boundary#repo) | per-target 행이라 파일 묶음용이다. X13 에는 원격 호출 대조가 필요 없다(X3 은 files 인스턴스 몫). |
| grep (open-in-view·수동 트랜잭션·@EnableAsync 레포 전체) | 안 돌림 | 1개 (data/transaction-boundary#repo) | grep 은 검사한 파일 수를 출력하지 않아 checked 를 읽을 수 없다. 그래서 ran:false 로 적는다. 결과는 notes 에 있다. |
| grep (catch·runCatching) | 안 돌림 | 1개 (reliability/failure-handling#repo) | src/main 전체에 한 번 실행했으나 결과가 정확히 40줄에서 잘렸고(head -40) grep 이 검사 파일 수를 보고하지 않아 checked 를 산출할 수 없음. 이 인스턴스의 체크 I7 의 근거도 아님(E1~E4 모집단이며 E 체크는 files 인스턴스 담당). |
| grep (원격 호출·타임아웃) | 안 돌림 | 1개 (reliability/failure-handling#repo) | src/main 전체에 한 번 실행했고 결과는 37줄로 완결됐으나 grep 이 검사 파일 수를 보고하지 않아 checked 를 산출할 수 없음. 이 인스턴스의 체크 I7 의 근거도 아님(E5 후보). |
| grep (실행기·스레드) | 안 돌림 | 1개 (reliability/load-concurrency#repo) | manifest per-target 행이지만 이 인스턴스는 repo 범위로 C11 한 체크만 판정한다. 이 도구는 A1·A3·C1 후보용이라 돌리지 않았다. |
| grep (공유 상태·락) | 안 돌림 | 1개 (reliability/load-concurrency#repo) | manifest per-target 행이지만 이 인스턴스는 repo 범위로 C11 한 체크만 판정한다. 이 도구는 A4·A5 후보용이라 돌리지 않았다. |
| grep (복구 경로 탐색: @Scheduled·tryMatchPendingOrders·recover·고가·저가 키워드) | 안 돌림 | 1개 (reliability/load-concurrency#repo) | 직접 돌린 ad-hoc grep 은 검사한 파일 수(checked)를 자체 출력으로 보고하지 못한다. 계약상 checked 없는 ran:true 는 미실행으로 세어지므로 ran:false 로 기록한다. 결과는 findings.evidence 와 notes 에 있다. |
| grep (시크릿 후보) | 안 돌림 | 1개 (reliability/security-config-hygiene#repo) | 매니페스트 per-target 행(T5b7501aa)이지만 S1·S2 후보용이다. 이 인스턴스의 체크는 S10·S11(레포 단위)뿐이라 대상 파일이 없다. |
| grep (원시 SQL) | 안 돌림 | 1개 (reliability/security-config-hygiene#repo) | 매니페스트 per-target 행(Tb51a40b2)이지만 S5 후보용이다. 이 인스턴스의 체크는 S10·S11(레포 단위)뿐이라 대상 파일이 없다. |
| grep (목 대상·비결정·BigDecimal 단언) | 안 돌림 | 1개 (backend#4) | tdd-purity 렌즈의 files 가 비어 있음 — 이 묶음에 src/test 파일이 없어 돌릴 대상 없음 |
| serena find_referencing_symbols / find_symbol | 안 돌림 | 1개 (backend#7) | kotlin 언어 서버가 시작 직후 종료(LanguageServerTerminatedException) — 호출처 수는 경로를 밝힌 grep 으로 대체 |
| grep (목 대상) | 안 돌림 | 1개 (backend#14) | tdd-purity 렌즈의 files 가 빈 목록이라 이 인스턴스에 판정 슬롯이 없음 |
| grep (비결정·엔진 대체) | 안 돌림 | 1개 (backend#14) | tdd-purity 렌즈의 files 가 빈 목록이라 이 인스턴스에 판정 슬롯이 없음 |
| grep (BigDecimal 단언) | 안 돌림 | 1개 (backend#14) | tdd-purity 렌즈의 files 가 빈 목록이라 이 인스턴스에 판정 슬롯이 없음 |
| serena find_symbol | 안 돌림 | 1개 (data#2) | kotlin language server 시작 실패("Language server stdout read process terminated unexpectedly"). 심볼 조회는 grep(범위·따옴표 지정)과 파일 읽기로 대신했습니다. |
| grep (fetch join·페이징) | 안 돌림 | 1개 (data#3) | data/query 의 lenses[].files 가 빈 배열이라 이 묶음의 조회 렌즈 대상 파일이 0개 — 돌릴 대상 없음 |
| grep (구간·함수 조건) | 안 돌림 | 1개 (data#3) | data/query 대상 파일 0개 (files=[]) |
| grep (무제한 목록) | 안 돌림 | 1개 (data#3) | data/query 대상 파일 0개 (files=[]) |
| grep (catch·runCatching) | 안 돌림 | 1개 (reliability#1) | 호출 상한(6회) 초과와 셸 글롭 차단으로 per-target grep 을 못 돌렸음. 범위 파일 전부를 Read 로 전문 열람해 대체 |
| grep (원격 호출·타임아웃) | 안 돌림 | 1개 (reliability#1) | 위와 같음. 전문 열람과 src/main 전체 대상 임시 grep(RestTemplate·타임아웃 사용처)으로 대체 |
| grep (실행기·스레드) | 안 돌림 | 1개 (reliability#1) | 위와 같음. 전문 열람과 src/main 전체 대상 임시 grep(RedisMessageListenerContainer·@Async·@Scheduled)으로 대체 |
| grep (공유 상태·락) | 안 돌림 | 1개 (reliability#1) | 위와 같음. 전문 열람으로 대체 |
| grep (로그 호출) | 안 돌림 | 1개 (reliability#1) | 위와 같음. 전문 열람으로 대체 |
| grep (시크릿 후보) | 안 돌림 | 1개 (reliability#1) | 위와 같음. 전문 열람으로 대체 |
| grep (원시 SQL) | 안 돌림 | 1개 (reliability#1) | 위와 같음. 전문 열람으로 대체 |
| serena find_symbol | 안 돌림 | 1개 (reliability#1) | kotlin 언어 서버 초기화 실패(LanguageServerTerminatedException). Read·grep 으로 대체 |
| grep (렌즈 Deterministic first: catch·원격 호출·로그 호출·실행기·공유 상태·시크릿·원시 SQL) | 안 돌림 | 1개 (reliability#4) | 도구 호출 상한(6회)을 다 써서 렌즈 grep 을 따로 돌리지 않았습니다. 대상 3개 파일(총 172줄)을 cat -n 으로 전부 읽어 모집단을 대신했고 후보는 0건이었습니다. manifest.json 은 앞 150줄만 읽어서 reliability 대상 per-target 행이 그 뒤에 있는지는 확인하지 못했습니다. |

## 6. 항목별 점수 (통과 ÷ (통과+위반) · 해당없음은 제외)

| 항목표 | 점수 | 통과 | 위반 |
|---|---|---|---|
| backend/code-shape | 95% | 1418 | 67 |
| backend/layering | 77% | 606 | 183 |
| backend/naming-terminology | 96% | 1484 | 59 |
| backend/tdd-purity | 79% | 391 | 107 |
| data/query | 87% | 237 | 34 |
| data/schema-data-model | 84% | 185 | 36 |
| data/transaction-boundary | 89% | 179 | 23 |
| reliability/failure-handling | 68% | 98 | 47 |
| reliability/load-concurrency | 53% | 33 | 29 |
| reliability/observability | 89% | 158 | 19 |
| reliability/security-config-hygiene | 77% | 73 | 22 |

| 항목표 | 항목 | 점수 | 통과 | 위반 | 해당없음 |
|---|---|---|---|---|---|
| backend/code-shape | F1 | 91% | 51 | 5 | 116 |
| backend/code-shape | F2 | 99% | 161 | 2 | 9 |
| backend/code-shape | F3 | 96% | 118 | 5 | 49 |
| backend/code-shape | F4 | 79% | 53 | 14 | 105 |
| backend/code-shape | S1 | 95% | 124 | 6 | 42 |
| backend/code-shape | S10 | 98% | 109 | 2 | 61 |
| backend/code-shape | S11 | 0% | 0 | 1 | 0 |
| backend/code-shape | S2 | 100% | 130 | 0 | 42 |
| backend/code-shape | S3 | 96% | 95 | 4 | 73 |
| backend/code-shape | S4 | 98% | 159 | 4 | 9 |
| backend/code-shape | S5 | 88% | 149 | 20 | 3 |
| backend/code-shape | S6 | 97% | 87 | 3 | 82 |
| backend/code-shape | S7 | 100% | 21 | 0 | 151 |
| backend/code-shape | S8 | 99% | 140 | 1 | 31 |
| backend/code-shape | S9 | 100% | 21 | 0 | 151 |
| backend/layering | D1 | 17% | 6 | 29 | 173 |
| backend/layering | D2 | 92% | 70 | 6 | 132 |
| backend/layering | D3 | 66% | 27 | 14 | 167 |
| backend/layering | D4 | 24% | 7 | 22 | 179 |
| backend/layering | D5 | 79% | 23 | 6 | 179 |
| backend/layering | D6 | 92% | 177 | 15 | 16 |
| backend/layering | L1 | 59% | 41 | 28 | 139 |
| backend/layering | L2 | 83% | 57 | 12 | 139 |
| backend/layering | L3 | 25% | 4 | 12 | 192 |
| backend/layering | L4 | 76% | 55 | 17 | 136 |
| backend/layering | L5 | 77% | 23 | 7 | 178 |
| backend/layering | L6 | 80% | 24 | 6 | 178 |
| backend/layering | L7 | 86% | 24 | 4 | 180 |
| backend/layering | L8 | 93% | 67 | 5 | 136 |
| backend/layering | L9 | 100% | 1 | 0 | 0 |
| backend/naming-terminology | N1 | 0% | 0 | 1 | 0 |
| backend/naming-terminology | N10 | 99% | 210 | 3 | 21 |
| backend/naming-terminology | N11 | 80% | 72 | 18 | 144 |
| backend/naming-terminology | N12 | 99% | 222 | 3 | 9 |
| backend/naming-terminology | N2 | 99% | 159 | 2 | 73 |
| backend/naming-terminology | N3 | 54% | 14 | 12 | 208 |
| backend/naming-terminology | N4 | 100% | 62 | 0 | 172 |
| backend/naming-terminology | N5 | 99% | 232 | 2 | 0 |
| backend/naming-terminology | N6 | 94% | 221 | 13 | 0 |
| backend/naming-terminology | N7 | 99% | 231 | 3 | 0 |
| backend/naming-terminology | N8 | 100% | 42 | 0 | 192 |
| backend/naming-terminology | N9 | 90% | 19 | 2 | 213 |
| backend/tdd-purity | T1 | 72% | 42 | 16 | 1 |
| backend/tdd-purity | T10 | 78% | 7 | 2 | 50 |
| backend/tdd-purity | T11 | 58% | 7 | 5 | 47 |
| backend/tdd-purity | T12 | 76% | 44 | 14 | 1 |
| backend/tdd-purity | T13 | – | 0 | 0 | 1 |
| backend/tdd-purity | T2 | 39% | 12 | 19 | 28 |
| backend/tdd-purity | T3 | 83% | 43 | 9 | 7 |
| backend/tdd-purity | T4 | 100% | 59 | 0 | 0 |
| backend/tdd-purity | T5 | 75% | 9 | 3 | 47 |
| backend/tdd-purity | T6 | 69% | 40 | 18 | 1 |
| backend/tdd-purity | T7 | 91% | 53 | 5 | 1 |
| backend/tdd-purity | T8 | 59% | 22 | 15 | 22 |
| backend/tdd-purity | T9 | 98% | 53 | 1 | 5 |
| data/query | E1 | 100% | 26 | 0 | 68 |
| data/query | E2 | 77% | 10 | 3 | 81 |
| data/query | E3 | 100% | 25 | 0 | 69 |
| data/query | E4 | 80% | 8 | 2 | 84 |
| data/query | E5 | 81% | 21 | 5 | 68 |
| data/query | Q1 | 100% | 5 | 0 | 89 |
| data/query | Q13 | 0% | 0 | 1 | 0 |
| data/query | Q2 | 77% | 10 | 3 | 81 |
| data/query | Q3 | 95% | 20 | 1 | 73 |
| data/query | Q4 | 100% | 3 | 0 | 91 |
| data/query | Q5 | 82% | 47 | 10 | 37 |
| data/query | Q6 | 63% | 5 | 3 | 86 |
| data/query | Q7 | 85% | 28 | 5 | 61 |
| data/query | Q8 | 97% | 29 | 1 | 64 |
| data/schema-data-model | M1 | 100% | 21 | 0 | 19 |
| data/schema-data-model | M10 | 100% | 5 | 0 | 35 |
| data/schema-data-model | M11 | 40% | 2 | 3 | 35 |
| data/schema-data-model | M12 | 0% | 0 | 1 | 0 |
| data/schema-data-model | M2 | 89% | 16 | 2 | 22 |
| data/schema-data-model | M3 | 76% | 22 | 7 | 11 |
| data/schema-data-model | M4 | 62% | 21 | 13 | 6 |
| data/schema-data-model | M5 | 100% | 19 | 0 | 21 |
| data/schema-data-model | M6 | 94% | 34 | 2 | 4 |
| data/schema-data-model | M7 | 91% | 32 | 3 | 5 |
| data/schema-data-model | M8 | 71% | 5 | 2 | 33 |
| data/schema-data-model | M9 | 73% | 8 | 3 | 29 |
| data/transaction-boundary | X1 | 97% | 35 | 1 | 40 |
| data/transaction-boundary | X10 | 81% | 17 | 4 | 55 |
| data/transaction-boundary | X11 | 83% | 5 | 1 | 70 |
| data/transaction-boundary | X12 | 67% | 4 | 2 | 71 |
| data/transaction-boundary | X13 | – | 0 | 0 | 1 |
| data/transaction-boundary | X2 | 100% | 30 | 0 | 46 |
| data/transaction-boundary | X3 | 72% | 21 | 8 | 47 |
| data/transaction-boundary | X4 | 92% | 11 | 1 | 64 |
| data/transaction-boundary | X5 | 80% | 4 | 1 | 71 |
| data/transaction-boundary | X6 | 89% | 16 | 2 | 58 |
| data/transaction-boundary | X7 | 87% | 13 | 2 | 61 |
| data/transaction-boundary | X8 | 95% | 20 | 1 | 55 |
| data/transaction-boundary | X9 | 100% | 3 | 0 | 73 |
| reliability/failure-handling | E1 | 66% | 19 | 10 | 59 |
| reliability/failure-handling | E2 | 52% | 12 | 11 | 65 |
| reliability/failure-handling | E3 | 83% | 20 | 4 | 64 |
| reliability/failure-handling | E4 | 93% | 13 | 1 | 74 |
| reliability/failure-handling | E5 | 33% | 3 | 6 | 79 |
| reliability/failure-handling | E6 | 80% | 8 | 2 | 78 |
| reliability/failure-handling | E7 | 75% | 6 | 2 | 80 |
| reliability/failure-handling | I1 | 64% | 9 | 5 | 74 |
| reliability/failure-handling | I2 | 0% | 0 | 2 | 86 |
| reliability/failure-handling | I3 | 50% | 1 | 1 | 86 |
| reliability/failure-handling | I4 | 100% | 4 | 0 | 84 |
| reliability/failure-handling | I5 | 60% | 3 | 2 | 83 |
| reliability/failure-handling | I6 | 0% | 0 | 1 | 87 |
| reliability/load-concurrency | A1 | 50% | 2 | 2 | 25 |
| reliability/load-concurrency | A2 | 0% | 0 | 4 | 25 |
| reliability/load-concurrency | A3 | 20% | 1 | 4 | 24 |
| reliability/load-concurrency | A4 | 86% | 12 | 2 | 15 |
| reliability/load-concurrency | A5 | 67% | 4 | 2 | 23 |
| reliability/load-concurrency | A6 | 86% | 12 | 2 | 15 |
| reliability/load-concurrency | A7 | – | 0 | 0 | 29 |
| reliability/load-concurrency | C1 | 0% | 0 | 3 | 26 |
| reliability/load-concurrency | C11 | 0% | 0 | 1 | 0 |
| reliability/load-concurrency | C2 | 25% | 1 | 3 | 25 |
| reliability/load-concurrency | C3 | 33% | 1 | 2 | 26 |
| reliability/load-concurrency | C4 | 0% | 0 | 3 | 26 |
| reliability/load-concurrency | C5 | – | 0 | 0 | 29 |
| reliability/load-concurrency | C6 | 0% | 0 | 1 | 28 |
| reliability/observability | O1 | 89% | 25 | 3 | 10 |
| reliability/observability | O10 | 0% | 0 | 1 | 0 |
| reliability/observability | O11 | 0% | 0 | 1 | 0 |
| reliability/observability | O12 | 100% | 18 | 0 | 20 |
| reliability/observability | O2 | 100% | 35 | 0 | 3 |
| reliability/observability | O3 | 100% | 18 | 0 | 20 |
| reliability/observability | O4 | 100% | 18 | 0 | 20 |
| reliability/observability | O5 | 43% | 3 | 4 | 31 |
| reliability/observability | O6 | 17% | 1 | 5 | 32 |
| reliability/observability | O7 | 88% | 22 | 3 | 13 |
| reliability/observability | O8 | 94% | 17 | 1 | 20 |
| reliability/observability | O9 | 50% | 1 | 1 | 36 |
| reliability/security-config-hygiene | S1 | 96% | 24 | 1 | 8 |
| reliability/security-config-hygiene | S10 | 0% | 0 | 1 | 0 |
| reliability/security-config-hygiene | S11 | 0% | 0 | 1 | 0 |
| reliability/security-config-hygiene | S2 | 0% | 0 | 1 | 32 |
| reliability/security-config-hygiene | S3 | 93% | 14 | 1 | 18 |
| reliability/security-config-hygiene | S4 | 57% | 4 | 3 | 26 |
| reliability/security-config-hygiene | S5 | 100% | 3 | 0 | 30 |
| reliability/security-config-hygiene | S6 | 100% | 9 | 0 | 24 |
| reliability/security-config-hygiene | S7 | 0% | 0 | 9 | 24 |
| reliability/security-config-hygiene | S8 | 85% | 11 | 2 | 20 |
| reliability/security-config-hygiene | S9 | 73% | 8 | 3 | 22 |

파일별 원장은 같은 폴더의 files.md · files.json (리팩터링 때 이걸 연다 — 전수조사 재실행 불필요).

## 7. 응답 모양 조사 (contract 부서 · api-shape-census)

이번 run 에는 조사 결과가 없다(contract 부서를 안 돌렸거나 openapi.json 을 못 냈다).

## 8. 사람이 정한 것

| 결정 | 선택 | 근거 |
|---|---|---|
| 리팩터링 1순위 파일 | (files.md 상위에서) |  |
| 기준 충돌 처리 | (항목표 유지 / 규칙 수정 제안) |  |
| 안 돌린 도구 | (설치 후 재감사 / 보류) |  |
| 응답 엔벨로프 규칙 | (후보 A/B/…) |  |
