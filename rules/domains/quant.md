# Quant 도메인 — 수식·백테스트·신호 (paper-trading)
<!-- devkit:pointer
path: ^backend/quant-[a-z]+/.+\.py$
why: 퀀트 수식·백테스트·신호·금액 계산 Python 코드
-->
<!-- devkit:scope narrows DD13 -->
<!-- devkit:form v2 -->

이 파일은 퀀트 Python 코드의 수식 옮기기, 금액 계산, 백테스트 데이터 처리, 테이블 소유 기준을 정한다. 수식 옮김 오류, 금액 오차, 부풀려진 백테스트 성과, 생기지 않는 테이블을 막는다.

## 조항

<!-- gt:PTQ1 -->
- 퀀트 수식을 코드로 옮길 때 변수 이름은 spec 의 수식 표기와 같게 짓고, 수식과 코드가 같은지 단계별로 대조하며, 수식 계산은 손으로 구한 값과 맞추는 대상 시험으로 확인한다.
  Why: 이름이 수식과 다르면 리뷰에서 수식과 코드를 한 줄씩 맞대 볼 수 없어 옮김 오류가 숨는다(사람 결정 2026-11-22, 옛 퀀트 스킬 Implementation Rules). — enforce: review
<!-- gt:PTQ2 -->
- quant 서비스의 금액 계산(수수료, 세금, 잔고 포함)은 `Decimal` 로 하고 `float` 를 쓰지 않는다. devkit DD13 의 «Money 를 float 로 두지 않음»을 계산까지 좁힌다.
  Why: float 이진 오차가 수수료·잔고 누적과 비교에서 틀린 금액을 내고, 백테스트 spec(`docs/done/quant-worker/backtest-engine/spec.md`)도 금액·지표의 float 저장을 막는다(사람 결정 2026-11-22). — enforce: review
<!-- gt:PTQ3 -->
- 백테스트와 신호 계산은 시점 T 에 T 이전에 알 수 있던 데이터만 쓰고(룩어헤드 금지), 성과에는 슬리피지, 수수료, 세금을 늘 반영하며, 결측값, 상장폐지 종목, 서킷브레이커(거래 정지) 구간을 명시적으로 처리한다.
  Why: 미래 데이터, 비용 누락, 생존 편향은 과거 성과를 부풀려 실거래에서 못 내는 전략을 통과시킨다(사람 결정 2026-11-22, 옛 퀀트 스킬 «상장 종목만 쓰면 과거 성과가 부풀려짐»). — enforce: review
<!-- gt:PTQ4 -->
- Python 서비스가 쓰는 테이블은 그 Python 서비스가 직접 만들고, Kotlin 쪽 Flyway 마이그레이션에 기대지 않는다.
  Why: trading-api 는 Flyway 의존성 없이 `ddl-auto` 로 스키마를 만들어(`docs/TODO.md` 스키마 관리 점검 항목) 그쪽에 기대면 Python 테이블이 생기지 않고, quant 는 lifespan 에서 자기 마이그레이션을 돌린다(사람 결정 2026-11-22, service-role-refactor 결정). — enforce: review
