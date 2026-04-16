Skill: Quant 개발 원칙

## 데이터 무결성

- Look-ahead bias 절대 금지: 시점 T의 데이터는 T 이전 정보만 사용
- 생존 편향(Survivorship bias) 주의: 현재 상장 종목만 사용하면 과거 성과 과대평가
- 점프 처리: 배당락, 액면분할은 수정주가(adjusted price) 사용
- 결측값: forward-fill 금지 (마지막 값 전파 = look-ahead). 명시적 처리 필요.

## 백테스팅 원칙

- Train/Test 분리: 팩터 개발은 train 기간만. test는 최종 검증 1회만.
- Walk-forward 검증: 과적합 방지를 위한 rolling window 검증
- 거래비용 반드시 포함: 슬리피지 + 수수료 + 세금
- 리밸런싱 시점의 시가 기준 체결 (종가 기준 체결 = look-ahead)

## 구현 원칙

- 벡터화 연산 우선: pandas/numpy loop 금지 (성능 + 버그 방지)
- 결과 재현성: random seed 고정, 환경 변수 명시
- 수식과 코드 변수명 일치: 논문/명세의 수식 기호 = 코드 변수명
- 단위 테스트: 팩터 계산 함수는 수작업 계산 결과와 검증

## 리스크 지표

- Sharpe Ratio: (연환산 수익률 - 무위험 수익률) / 연환산 변동성
- MDD(Max Drawdown): 고점 대비 최대 낙폭
- VaR: 신뢰구간 95%/99%에서 최대 손실
- Calmar Ratio: 연환산 수익률 / MDD (값이 클수록 좋음)

## 포트폴리오 제약

- 종목별 포지션 한도 명시 (기본 5~10%)
- 섹터 편중 방지 (기본 섹터당 30% 이하)
- 레버리지 한도 명시
- 숏 포지션 허용 여부 명시

## 금지

- 미래 데이터 사용 (look-ahead bias)
- 거래비용 미포함 백테스팅
- 단일 기간 백테스팅으로 전략 확정
- 과최적화 (파라미터 과다 튜닝)
- float/double로 금액·수량 계산 (Decimal 사용)
