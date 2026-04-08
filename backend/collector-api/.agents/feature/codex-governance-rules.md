CODEX 공통 규칙/기록 체계 도입

목적/배경
작업 절차 표준화와 재발 방지 지식 축적을 위해 Codex 운영 규칙을 명문화하기 위함.

변경 범위
문서:
루트 CODEX.md 생성
.codex/feature/README.md 생성
.codex/rules/README.md 생성
규칙:
작업 전 규칙 문서 확인 의무
기능 개발 후 feature 보고서 작성 의무
버그/문제 발생 시 rules 문서화 의무
개발 후 컴파일 검증 및 실패 시 수정 반복 원칙

검증
실행 명령: .\gradlew.bat compileKotlin
결과: 성공

후속 TODO
rules 실사용 사례(버그 리포트) 1건 이상 축적
feature 템플릿에 API 샘플 요청/응답 섹션 추가 검토
