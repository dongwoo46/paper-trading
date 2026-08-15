# Backtest Engine Summary

## Scope

- quant-worker에 KR/US 일봉 전략용 비동기 LEAN 백테스트 실행 경로 추가
- 고정 JSON DSL 검증부터 데이터 내보내기, Docker 실행, 결과·로그 조회까지 연결

## Key Changes

- `POST /backtest-runs`와 실행 상태·결과·로그 조회 API 및 PostgreSQL run 메타데이터 추가
- RAW OHLCV를 LEAN 일봉 형식으로 내보내고 격리된 run workspace에서 공식 이미지 ENTRYPOINT로 실행
- 고정·검토된 템플릿에서 가격/기술지표 DSL을 계산하고 completed-bar 신호를 next-open MOO로 실행
- `KR_DEFAULT_V1`·`US_DEFAULT_V1` 비용 프로필과 `MOO_CLOSE_BUFFER_V1` 실행 정책을 결정적으로 snapshot
- 실제 시가 기준 slippage·fee·buying-power 전액 거절, false→true 재무장, 전량 매도, 말단 미체결 감사를 구현
- atomic PENDING claim과 symlink/path confinement로 중복 실행 및 artifact 경로 이탈 방지

## Verification

- 백테스트/API/마이그레이션 타깃: `202 passed`, warning 1건
- Ruff, Python compile, `git diff --check`: 통과
- 독립 코드 리뷰 및 스펙 리뷰: PASS, must-fix 0건
- `npm --prefix frontend/trading-web run build`: 통과

## Known Limitations

- Docker daemon 접근 제한으로 실제 LEAN container smoke는 수행하지 못하고 공식 엔진 계약과 adapter tests로 검증
- 전체 백엔드 suite는 로컬 PostgreSQL integration이 sandbox에서 차단됐고, DB 제외 실행은 임시 pandas-ta 버전의 Bollinger column 차이에서 중단됨; 새 백테스트 타깃은 모두 통과
- 실행 큐는 프로세스 내부 `ThreadPoolExecutor` 기반이므로 재시작 내구성은 후속 운영 작업 대상

## Follow-up

- `backtest-multifactor-dsl-expansion`에서 수급·펀더멘털·거시·뉴스 등 point-in-time custom data 확장
