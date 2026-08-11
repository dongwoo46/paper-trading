# market-collector 제거 기록

- 날짜: 2026-08-11
- 상태: 제거 완료
- 결정: `backend/market-collector`는 `collector-api`로 대체된 초기 WebFlux/R2DBC 구현이므로 제거한다.

## 검증

- `backend/market-collector` 경로가 남지 않음
- 저장소에서 `backend/market-collector`를 가리키는 참조가 남지 않음
- `backend/collector-api` 변경 없음
- `git diff --check` 통과

## 미해결 확인

- `docker compose config --quiet`는 `backend/trading-api/.env`가 없어 실행 전 단계에서 중단됐다. 실제 Compose 검증은 로컬 환경 파일을 준비한 뒤 다시 실행해야 한다.
- `collector-api` 내부의 `spring.application.name`과 Docker 컨테이너명은 아직 `market-collector`를 사용한다. 런타임 식별자 변경은 모니터링·기존 컨테이너에 영향을 줄 수 있어 이번 제거 범위에서 제외했다.

## 복구

삭제 파일은 Git 이력에서 복구할 수 있다.
