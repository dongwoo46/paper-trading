# .agents — research-service

역할: 백테스트, AI 전략 생성, 뉴스 분석, 퀀트 연구
언어/기술: Python (예정) / 상태: 미개발

## 구조
- `feature/README.md` — API 인덱스
- `feature/{기능명}.md` — 기능 보고서
- `rule/{이슈}.md` — 재발 방지 기록

---

## 개발 이정표

### Phase 1: 백테스트 DB 설계 (다음 작업)
> trading-api ERD 확정 후 진행

설계 필요 테이블: backtest_runs / backtest_trades / backtest_daily_returns

- [ ] ERD 설계 및 사용자 승인

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
