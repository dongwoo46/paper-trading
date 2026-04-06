# .codex 운영 가이드

## 목적
- 이 폴더는 코딩 규칙, 기능 보고서, 재발 방지 규칙을 관리한다.

## 구조
- `CODEX.md`: 전체 공통 규칙(항상 먼저 읽기)
- `.codex/feature`: 기능 정의/변경 보고서
- `.codex/rules`: 버그/장애 재발 방지 규칙
- `.codex/feature/README.md`: 현재 기준 API 인덱스 + 웹 연결 상태

## 핵심 운영 원칙
- 기능 개발 후 검증(컴파일/빌드) 성공을 확인한다.
- API 추가/수정 또는 웹 API 연결 변경이 있으면 `.codex/feature/README.md`를 즉시 갱신한다.
- `.codex/feature/README.md`는 최신 상태만 유지한다.
