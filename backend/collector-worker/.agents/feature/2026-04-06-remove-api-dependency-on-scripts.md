# Title
API의 scripts 의존 제거

# Date
2026-04-06

# Owner
Codex

# Goal
API가 `scripts`를 재사용하지 않도록 공통 실행 로직을 `src/application`으로 이동한다.

# Scope
- 포함:
  - `src/application/daily_fetch_service.py` 추가
  - API가 application service 직접 호출
  - `scripts/fetch_daily_from_catalog.py`는 CLI 엔트리포인트 역할만 유지

# Design (Clean Architecture boundary)
- `src/application`: 공통 유스케이스/오케스트레이션
- `src/interfaces/api`: HTTP 요청/응답만 담당
- `scripts`: CLI 인자 처리만 담당

# SRP checklist
- API에서 배치 스크립트 의존 제거: Yes
- 공통 실행 로직 단일 위치화: Yes

# Readability checklist
- import 방향 단순화: Yes
- 책임 경계 명확화: Yes

# Test plan
- `python -m compileall src scripts api_main.py main.py`
- `python -c "from src.interfaces.api.app import app; print(app.title)"`

# Review result
- API/CLI 모두 application service를 호출하도록 정리 완료
