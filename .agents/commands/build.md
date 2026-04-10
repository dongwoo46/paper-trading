@.agents/roles/builder.md
@.agents/roles/tester.md
@.agents/skills/tdd.md
@.agents/skills/ddd.md

실행 모드 — 시작 전 사용자에게 묻는다
  [1] step: 각 작업 완료 후 승인 후 다음 진행
  [2] auto: 전체 자동 실행 (완료 후 결과 보고)
인자로 바로 지정 가능: /build step 또는 /build auto

실행 순서
1. design 승인 여부 확인 (없으면 design 먼저 실행 요청)
2. 작업을 독립 단위로 분해 후 목록 출력

각 작업 단위 실행 — TDD 사이클 강제 (순서 이탈 금지)
  [Red]    실패하는 테스트 작성
  [Red]    테스트 실행 → 실패 확인 (실패 미확인 시 다음 단계 진행 금지)
  [Green]  최소 구현으로 테스트 통과
  [Green]  테스트 실행 → 통과 확인
  [Refactor] 중복 제거, 가독성 개선 후 재실행 확인

step 모드: 각 작업마다 결과 보고 후 승인 대기 → 승인 시 다음 작업
auto 모드: 순서대로 자동 실행. 실패 시 즉시 중단 후 원인 보고.

실행 중 모드 전환 (언제든지)
- step 입력: 현재 작업 완료 후 step 모드로 전환
- auto 입력: 현재 작업부터 auto 모드로 전환
- stop 입력: 현재 작업 완료 후 중단, 진행 상황 보고

완료 후
빌드 검증: trading-api(./gradlew compileJava) | collector-api(./gradlew compileKotlin) | collector-worker(python -m py_compile {파일}) | trading-web(npm run build)
.agents/feature/README.md 갱신 → review 실행 권장
