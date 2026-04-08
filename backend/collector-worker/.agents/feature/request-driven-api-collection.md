Title
요청 기반 수집 API 추가 (FastAPI)

Date
2026-04-06

Owner
Codex

Goal
사용자가 API 요청을 보낼 때만 일봉 수집을 실행하도록 변경한다.

Scope
포함:
POST /collect/daily API 추가
기존 카탈로그 배치 로직 재사용
GET /health 상태 확인 API
제외:
인증/권한
비동기 큐 처리

Design (Clean Architecture boundary)
scripts/fetch_daily_from_catalog.py
실행 로직을 execute()로 분리해 CLI/API 공용 사용
src/interfaces/api/app.py
요청/응답 스키마 및 HTTP 엔드포인트 책임
api_main.py
ASGI 진입점

SRP checklist
수집 실행 로직과 HTTP 처리 분리: Yes
CLI와 API의 공통 실행 경로 유지: Yes

Readability checklist
요청 필드 기본값 명시: Yes
예외를 HTTP 코드로 명확히 매핑: Yes

Test plan
uvicorn api_main:app --host 0.0.0.0 --port 8000
GET /health
POST /collect/daily with JSON body

Review result
이제 main.py를 직접 호출하지 않아도 API 호출로 수집 실행 가능
