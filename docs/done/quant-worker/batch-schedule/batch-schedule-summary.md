# quant-worker batch-schedule

## Core Feature
KST 마켓/인터벌별 4개 독립 배치 스케줄러로 OHLCV 자동 수집 자동화.

## Schedule Table
| Batch ID | Market | Interval | Provider | KST Schedule |
|---|---|---|---|---|
| kr_daily | KR | 1d | pykrx | 월-금 18:30 |
| us_daily | US | 1d | yfinance | 화-토 08:30 |
| kr_weekly | KR | 1wk | pykrx | 금 19:30 |
| us_weekly | US | 1wk | yfinance | 토 10:00 |

## Retry Policy
지수 백오프: 초기 600s, 배수 2, 최대 4회, 최대 3600s.
재시도 대상: partial_success, stale_provider_window, failed.

## Notification
Slack (SLACK_WEBHOOK_URL) + 구조화 로그:
- 발송 조건: failed / partial_success / stale_provider_window / noop_empty_universe
- 최종 재시도 소진 시 별도 exhaustion 알림

## Outcome Classification
success / noop_empty_universe / partial_success / no_new_bar / stale_provider_window / failed

## Implementation
- 신규: backend/quant-worker/src/jobs/batch_schedule.py
- 수정: backend/quant-worker/src/interfaces/api/app.py (asyncio 루프 → APScheduler)
- 신규: backend/quant-worker/tests/jobs/test_batch_schedule.py (51개 테스트)

## Operational Notes
- BATCH_SCHEDULE_ENABLED=true 설정 필요
- SLACK_WEBHOOK_URL, SLACK_NOTIFICATIONS_ENABLED 환경변수 필요
- stale_provider_window 분류는 calendar 라이브러리 추가 후 구현 예정

## Test Results
- 신규: 51/51 pass
- 회귀: 70/70 pass

## Completed / PR
2026-05-10 / #TBD