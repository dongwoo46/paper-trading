# .agents 운영 가이드 — research-service

## 서비스 개요

- **역할**: 백테스트, AI 전략 생성, 뉴스 분석, 퀀트 연구
- **언어/기술**: Python (예정)
- **상태**: 미개발 (설계 단계)

---

## 개발 이정표

### Phase 1: 백테스트 DB 설계 및 구현 (다음 작업)

> ⚠️ trading-api ERD 확정 후 이 서비스의 DB 설계를 진행해야 한다.

#### 백테스트 DB 설계 필요 항목

trading-api는 `strategy_versions.backtest_run_id`로 외부 참조만 한다.
실제 백테스트 데이터는 이 서비스의 DB에 저장한다.

**설계 필요 테이블:**

```
backtest_runs
├── id (UUID 권장)
├── strategy_id          -- trading-api의 strategy_id 참조 (FK 없음, 외부 참조)
├── strategy_version_id  -- trading-api의 strategy_version_id 참조
├── start_date / end_date
├── initial_capital
├── status               -- PENDING / RUNNING / COMPLETED / FAILED
├── total_return_rate
├── sharpe_ratio
├── mdd                  -- 최대 낙폭
├── win_rate
├── total_trades
├── triggered_by         -- HUMAN / AI
├── created_at
└── completed_at

backtest_trades
├── id
├── backtest_run_id FK
├── symbol / symbol_name
├── side                 -- BUY / SELL
├── quantity
├── price
├── executed_at          -- 가상 체결 시각 (과거 데이터 기준)
├── fee
├── pnl                  -- 해당 거래 손익
└── cumulative_pnl       -- 누적 손익

backtest_daily_returns
├── backtest_run_id FK
├── date
├── daily_return_rate
├── cumulative_return_rate
├── portfolio_value
└── drawdown             -- 당일 낙폭
```

**설계 원칙:**
- `backtest_runs.id`는 UUID 사용 → trading-api에 `backtest_run_id`로 전달
- 완료 시 결과 요약(수익률, 샤프, MDD, 승률)을 trading-api로 전송
- trading-api는 요약만 저장, 상세 데이터는 이 서비스에서 조회

### Phase 2: 전략 생성 엔진

- [ ] AI 전략 후보 생성 (LangChain)
- [ ] 전략 DSL → backtest 실행
- [ ] 자동 수정 루프 (LangGraph)

### Phase 3: 주문 신호 생성

- [ ] 실시간 시세 기반 전략 조건 평가
- [ ] order_signals 생성 → trading-api 전달

### Phase 4: 뉴스/공시 분석

- [ ] 뉴스 수집 및 구조화
- [ ] 시장 영향 시뮬레이션
