# Step 1 — Feature Spec and Step File Generation

- 담당: quant-planner
- 상태: completed
- 완료일: 2026-05-10

---

## 완료 내용

Pass A에서 코드베이스 분석 및 사용자 Q&A를 통해 설계를 확정하였고, Pass B에서 spec.md와 step-2~5.md를 작성하였다.

---

## 확정된 설계 결정 요약

| 항목 | 결정 |
|------|------|
| 데이터 소스 | pykrx `get_market_trading_volume_by_investor(detail=True)` 외 3개 API |
| 테이블 구조 | Wide 테이블 (4개 테이블 분리, 한 행 = 한 종목 전체 투자자 컬럼) |
| 조회 단위 | 날짜 기준 시장 전체 일괄 조회 (종목별 순회 아님) |
| 저장 범위 | 전 종목 저장, KOSPI/KOSDAQ 배치 분리 (market 파라미터 주입) |
| 배치 분리 | 신규 `investor_flow_schedule.py` 작성 (batch_schedule.py 수정 최소화) |
| API 추가 | quant-worker에 READ 엔드포인트 4개 추가 |
| 실행 시각 | 19:00 KST 월-금 (kr_daily 18:30과 30분 간격) |
| 기관 세부 분류 | 금융투자, 보험, 투신, 사모, 은행, 기타금융, 연기금 |
| 수집 메트릭 | 매수량, 매도량, 순매수량, 매수금액, 매도금액, 순매수금액 |
| 대상 시장 | 국내(KR) KOSPI + KOSDAQ |
| 수집 테이블 | investor_flow, short_selling, program_trading, foreign_holding |
| 금액 타입 | NUMERIC(20,0) / Python Decimal (float 금지) |
| 멱등성 | (trade_date, symbol, market) 고유키 기준 upsert |

---

## 산출물

- `docs/phase/quant-worker/investor-flow-pipeline/spec.md` — 기능 명세 및 계약
- `docs/phase/quant-worker/investor-flow-pipeline/step-2.md` — TDD 구현 지시
- `docs/phase/quant-worker/investor-flow-pipeline/step-3.md` — 테스트 검증 기준
- `docs/phase/quant-worker/investor-flow-pipeline/step-4.md` — 코드 리뷰 체크리스트
- `docs/phase/quant-worker/investor-flow-pipeline/step-5.md` — PR 생성 지시
- `docs/phase/quant-worker/investor-flow-pipeline/index.json` — 갱신 완료
