# Quant 도메인 — 수식·백테스트·신호 (paper-trading)
<!-- devkit:pointer
path: ^backend/quant-[a-z]+/.+\.py$
why: 퀀트 수식·백테스트·신호·금액 계산 Python 코드
-->

> 아래 줄은 옛 문서에서 원문 그대로 옮겼다(2026-11-21 규칙 이사). 절 제목이 출처다.

## 옮겨 온 줄 — 옛 하네스(.claude/_disabled·.codex)

- 퀀트 수식을 코드로 옮길 때 변수 이름은 spec 의 수식 표기와 같게 짓고, 수식과 코드가 같은지 단계별로 대조한다.
- quant-worker 의 금액 계산은 `Decimal` 로 하고 `float` 를 쓰지 않는다.

- 백테스트와 신호 계산은 시점 T 에 T 이전에 알 수 있던 데이터만 쓴다(룩어헤드 편향 금지).

- 백테스트 성과에는 슬리피지, 수수료, 세금을 늘 반영한다.

- 백테스트는 결측값, 상장폐지 종목, 서킷브레이커(거래 정지) 구간을 명시적으로 처리한다.

- Use `Decimal` for monetary calculations and verify quant math with targeted tests.

## 옮겨 온 줄 — docs/TODO.md

- Python 서비스가 사용하는 테이블은 Python 서비스가 직접 생성 (Kotlin Flyway 의존 금지)
