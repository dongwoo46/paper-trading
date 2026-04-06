# CODEX Common Rules

## Scope
- This repo is a Python collector worker for external market data APIs.
- Current priority providers: `pykrx`, `yfinance`.

## Core Rules (Keep It Simple)
- Use Clean Architecture.
- Follow SRP for files/classes/functions.
- Always prioritize readability over clever code.
- Keep functions small, names clear, and responsibilities explicit.

## Documentation Workflow (Required)
- When creating a new feature:
  - Write a summary in `.codex/feature`.
  - Review the feature doc before merge.
- When an issue happens:
  - Record the issue in `CODEX.md` under `Issue Log`.
  - Add/adjust a prevention rule in `.codex/rules`.

## API Documentation Rule (Required)

- `.codex/feature/api.md` 는 이 프로젝트의 **API 명세 단일 출처**다.
- 다음 경우에 반드시 `api.md`를 업데이트한다:
  - collector-worker에 엔드포인트 추가/변경/삭제 시
  - 프론트엔드(`frontend/trading-web/src`)에서 새 API 경로를 호출하거나 제거할 시
- `web` 연결 여부 기준:
  - ✅ 연결됨: `frontend/trading-web/src` 코드에서 실제로 `fetchJson` 또는 `fetch`로 호출 중
  - ❌ 미연결: 백엔드에 존재하지만 프론트엔드에서 호출하지 않음
- feature 문서 작성 시 API가 포함된다면 `api.md`도 함께 갱신한다.

## Provider Rules
- Separate adapters by provider (`pykrx`, `yfinance`).
- Normalize to one common daily OHLCV schema.
- Handle retries, partial failures, and idempotent writes.

## Issue Log
- Format:
  - Date:
  - Issue:
  - Root Cause:
  - Prevention Rule:
- Date: 2026-04-05
  - Issue: `main.py` runtime failed with missing DB env (`PG_HOST`).
  - Root Cause: required PostgreSQL connection variables were not set before execution.
  - Prevention Rule: always validate and set `PG_HOST`, `PG_DATABASE`, `PG_USER`, `PG_PASSWORD` before running batch jobs.
- Date: 2026-04-06
  - Issue: yfinance 수집 중 종료일 계산에서 `AttributeError` 발생.
  - Root Cause: `datetime.date` 객체에 `.date()`를 중복 호출함.
  - Prevention Rule: `date` 타입 연산은 `datetime.timedelta`를 사용하고, date/datetime 타입 변환 테스트를 추가한다.
- Date: 2026-04-06
  - Issue: yfinance 백필 시 `date` 키 오류로 수집 실패.
  - Root Cause: yfinance 응답이 MultiIndex 컬럼일 수 있는데 단일 컬럼만 가정함.
  - Prevention Rule: yfinance 응답 정규화 단계에서 MultiIndex 컬럼을 먼저 평탄화하고 테스트한다.
