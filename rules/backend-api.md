# Backend API — Kotlin·Spring 도메인 모델과 금액 (paper-trading)
<!-- devkit:pointer
path: ^backend/(trading-api|collector-api)/src/main/.+\.kt$
signal: @Entity|BigDecimal|class \w+\s*(protected )?constructor|private set|@ManyToOne|@OneToMany|@OneToOne
why: JPA 엔티티(애그리거트)나 금액 계산을 다루는 Kotlin 코드
-->
<!-- devkit:form v2 -->

> 이 레포 전용 규칙 — devkit 팀 규칙 `backend-api.md`(DD4~DD14)를 이 레포의 Kotlin·Spring·JPA 에서 지키는 방법만 적는다. 원칙 자체는 팀 규칙이 SSoT 이고 여기서 다시 쓰지 않는다.
> 금액 규칙의 상위 원칙은 루트 `CLAUDE.md` «Financial Safety»(금액에 부동소수점 금지, Kotlin 은 BigDecimal).

## 도메인 모델과 영속 모델

<!-- gt:PTB1 -->
- 금액을 바꾸거나 상태 필드에 전이 규칙이 있는 애그리거트는 순수 Kotlin 도메인 모델로 둔다. 도메인 모델 파일은 `jakarta.persistence`·`org.springframework` 를 import 하지 않고, JPA 엔티티와 그 사이 변환은 영속 어댑터에 두며, 리포지토리 포트는 도메인 모델을 주고받는다.
  Why: 팀 규칙 DD9(도메인·유스케이스가 ORM 모델을 직접 참조하지 않음)의 Kotlin·JPA 형태. 엔티티가 곧 도메인이면 지연 로딩·더티 체킹·프록시가 불변식 검사를 우회하는 경로가 된다(사람 결정 2026-11-15: 이 레포는 DD9 를 따라 분리). — enforce: review
  Exception: 금액도 전이 규칙도 없는 단순 CRUD·설정 성격 컨텍스트는 JPA 엔티티를 그대로 쓴다 — 팀 규칙 backend-api «No over-engineering»(단순 CRUD 에 전체 DDD 를 강요하지 않음)의 적용이지 이 레포가 만든 예외가 아니다.
<!-- gt:PTB2 -->
- 도메인 모델의 상태 필드는 `private set` 이고, 상태 변경은 허용된 이전 상태를 검사하는 이름 붙은 메서드로만 한다.
  Why: 팀 규칙 DD10(빈약한 모델 금지)·DD11(도메인 로직 누수 금지)의 Kotlin 형태. Kotlin `var` 는 기본이 public setter 라 서비스가 전이 검사를 건너뛰고 상태를 바꿀 수 있고, 루트 `CLAUDE.md` 의 «상태 전이는 명시적»이 깨진다. — enforce: review
<!-- gt:PTB3 -->
- 다른 애그리거트는 id 로만 참조한다. JPA 연관관계(`@ManyToOne`·`@OneToMany`·`@OneToOne`)는 같은 애그리거트 안의 자식에만 쓴다.
  Why: 팀 규칙 DD12(한 트랜잭션은 한 애그리거트)를 지키는 구조. 애그리거트를 객체로 이으면 한 트랜잭션이 여러 애그리거트를 읽고 잠근다(devkit 제안 BiocomKR/devkit#2471 의 DD15 와 같은 내용 — 그 제안이 머지되면 이 조항은 지운다). — enforce: review
<!-- gt:PTB5 -->
- 도메인 모델은 private 생성자와 팩토리, 또는 `init { require(...) }` 로 생성 시점에 불변식을 검증해 잘못된 인스턴스가 존재할 수 없게 한다.
  Why: 검증을 서비스에 두면 그 서비스를 거치지 않는 호출자(다른 유스케이스, 어댑터의 변환, 테스트 픽스처)가 건너뛴다. 팀 규칙 DD13(원시 타입 집착 금지)의 값 객체도 이 방식으로 만든다. — enforce: review

## 금액

<!-- gt:PTB4 -->
- `BigDecimal` 은 `compareTo` 로 비교하고(`==`·`equals` 금지), 문자열이나 정수로 만들며(`BigDecimal("0.1")`, `BigDecimal(0.1)` 금지), 나눗셈과 반올림에는 scale 과 `RoundingMode` 를 명시한다.
  Why: Kotlin `==` 는 `equals` 라 scale 까지 비교해 `1.0 != 1.00` 이 되고, `Double` 생성자는 이진 오차를 그대로 싣고, scale 없는 나눗셈은 무한소수에서 예외를 던진다. 셋 다 컴파일은 통과하고 금액이 조용히 틀린다. — enforce: review

<!-- devkit:rule-verified docs/eval/rule-change/2026-11-15-repo-rules-spring-ddd.md -->
