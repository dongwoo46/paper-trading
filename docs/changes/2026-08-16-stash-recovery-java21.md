# Stash recovery and Java 21 alignment

## 변경

- 보존된 두 stash에서 검증 명령, Serena 설정, 기획·리서치 문서, findings 원장을 복구했다.
- findings 원장은 stash 20건과 main 10건을 합쳐 30건을 모두 보존했다.
- `backend/trading-api`에 누락된 Java toolchain 21 선언을 추가했다.
- 두 Gradle wrapper에 실행 권한을 복구해 stack profile의 `./gradlew` 명령이 실제 실행되게 했다.
- 로컬 개발 환경에 Homebrew OpenJDK 21을 설치하고 `~/.zshrc`의 `JAVA_HOME`을 21로 갱신했다. 이 항목은 저장소 밖 로컬 환경 변경이다.

## 검증

- `backend/trading-api`: `./gradlew compileKotlin` — exit 0
- `backend/collector-api`: `./gradlew compileKotlin` — exit 0
- `frontend/trading-web`: `npm test -- --run` — 34 files, 152 tests 통과, exit 0
- 루트: `npm --prefix frontend/trading-web run build` — exit 0
- stack profile 및 복구 JSON/JSONL 파싱 — 통과
- `git diff --check` — 통과

## 기존 테스트 실패

- `backend/collector-api`: 142개 중 1개 실패 — `KisAccessTokenClientTest`의 rate limiter assertion. Java 21 compile은 통과했다.
- `backend/trading-api`: PostgreSQL Testcontainers 종료 후 재접속으로 persistence 테스트가 실패하고 프로세스가 종료되지 않아 중단했다. Java 21 compile은 통과했다.
- 두 실패는 stash 및 Java toolchain 변경 전 제품 테스트 경로에 있으며 이번 복구 범위에서 동작을 임의 수정하지 않았다.
