# 합동 리뷰 — multi-broker-kis-toss step 3

- 대상: 미커밋 변경(기준 `a43538a`), `backend/trading-api` main 7파일(새 파일 2개 포함), 시험 7파일(새 파일 1개 포함), ktlint 기준선
- 리뷰어: 코드(`code-reviewer`) · 명세(`spec-reviewer`) · QA(`tester`). 3명 모두 결과 회수, 빠진 렌즈 없음
- 건수: 코드 4 · 명세 7 · QA 7 = 18건 → 중복 병합 후 18건(같은 자리·같은 내용 없음)
- 작성자 증거: 관련 8개 시험 클래스 154건 실패 0(`build/test-results/test/*.xml`, 2026-11-14T20:51Z), ktlintCheck·detekt·verifyLintBaselinesNotGrown 통과. 전체 스위트는 커밋 게이트·CI 몫
- 사람 결정(2026-11-15): S3S-01은 `API-Errors-Deferred`로 명시적 미룸(open-questions FLAG-11), S3S-02는 현재 구조로 확정(FLAG-12)

## Findings

| ID | file:line | 근거 조항 | 무엇이 틀렸나 | 등급 | 출처 | 회귀 | 리뷰어 |
|---|---|---|---|---|---|---|---|
| S3S-01 | GlobalExceptionHandler.kt:49-59 | AE19 | 건드린 핸들러에 `IllegalArgumentException`·`IllegalStateException` → 400 묶음 매핑이 남음. 사람이 `API-Errors-Deferred`로 미룸(FLAG-11) | blocker | 기존 | 아니오 | 명세 |
| S3S-02 | CommonApiExceptions.kt:163-216 | PTB7 | 새 A·F 하위 예외 15개가 HttpStatus를 가진 동결 기반을 상속, domain Account가 9개 참조. 사람이 현재 구조로 확정(FLAG-12) | should-fix | 이 변경 | 아니오 | 명세 |
| S3S-03 | steps/step-3.md:63 | label:stale-doc-after-change | 완료 기준 «전체 테스트 실패 0» 증거 없음(관련 8개 클래스만 실행) | should-fix | 이 변경 | 아니오 | 명세 |
| S3S-04 | steps/step-3.md:193 | label:stale-doc-after-change | §8 «limitPrice 0 이하 잠금 없이 접수» 기록이 사실과 다름(`Order.kt:184`가 잠금 전 400으로 막음) | should-fix | 이 변경 | 아니오 | 명세 |
| S3C-01 | ExecutionProcessor.kt:234 | C12 | 진입부 거부 함수의 «롤백 비의존» 주석이 롤백에 기대는 새 `ensureSellApplied` 위로 밀려 붙어 반대를 설명함 | should-fix | 이 변경 | 아니오 | 코드 |
| S3C-03 | GlobalExceptionHandler.kt:83 | AE21 | catch-all 처리기가 로그 없이 500을 돌려줌 | should-fix | 기존 | 아니오 | 코드+명세 |
| S3C-04 | ExecutionProcessor.kt:148 | label:stale-doc-after-change | step-3 문서는 K-2 `LOCKED_DEPOSIT_INSUFFICIENT`가 정상 사용에서 안 닿는다고 했지만, LOCAL 시장가 매수 뒤 시세 상승이면 닿음(잠금 부족 자체는 기존 결함) | should-fix | 기존 | 아니오 | 코드 |
| S3Q-01 | OrderCommandServiceTest.kt:340 | TT32 | 다른 계좌 주문 취소 시험이 취소 가능한 주문만 써서 404·409 검사 순서가 뒤집혀도 초록 | should-fix | 이 변경 | 아니오 | QA |
| S3Q-02 | GlobalExceptionHandlerTest.kt:36 | TT9 | 500 시험이 핸들러 메서드를 직접 불러 `@ExceptionHandler` 등록을 검증하지 않음 | should-fix | 이 변경 | 아니오 | QA |
| S3Q-03 | OrderControllerIntegrationTest.kt:305 | TT9 | 거부 경로 통합 시험이 상태·code만 보고 주문 행·예수금·잠금·포지션 불변을 단언하지 않음 | should-fix | 이 변경 | 아니오 | QA |
| S3C-02 | Account.kt:273 | C12 | 새 주석이 코드를 다시 쓰거나(273·385) 종결어미 문장(GlobalExceptionHandler.kt:24) | nit | 이 변경 | 아니오 | 코드 |
| S3S-05 | steps/step-3.md:133 | AE20 | 엔드포인트 목록에 계좌명 변경 PATCH 누락, 리스크 정책 경로 없음 | nit | 이 변경 | 아니오 | 명세 |
| S3S-06 | state.md:40 | label:stale-doc-after-change | 로드맵 3단계 상태 `todo`, step-3.md는 `in progress` | nit | 이 변경 | 아니오 | 명세 |
| S3S-07 | trading-web-visual-redesign/progress.md:8 | label:stale-doc-after-change | 다른 기능 문서 미커밋 변경, 이 단계 커밋에서 제외 필요 | nit | 범위 밖 | 아니오 | 명세 |
| S3Q-04 | OrderCommandServiceTest.kt:262 | TT32 | 같은 요청에 400·409가 겹칠 때 우선순위 시험 없음 | nit | 이 변경 | 아니오 | QA |
| S3Q-05 | OrderCommandServiceTest.kt:306 | TT48 | 시세 60초 경계 시험 없음(시계 주입 필요) | nit | 기존 | 아니오 | QA |
| S3Q-06 | AccountTest.kt:515 | TT32 | 음수·통화 2자리·비율 음수 경계 누락 | nit | 이 변경 | 아니오 | QA |
| S3Q-07 | GlobalExceptionHandlerTest.kt:36 | AE21 | 500 원문을 error 로그에 남기는 약속을 시험이 보지 않음 | nit | 이 변경 | 아니오 | QA |

## 충돌

없음. 같은 자리에 상반된 판정은 없었다.

## 리뷰어별 확인 불가

- 코드: `guards-fix`(미커밋이라 head 커밋 없음, 샌드박스가 Gradle 캐시 쓰기 차단)와 뮤테이션 도구(대상 0) 확인 불가. QA 수동 돌연변이 결과로 대신 읽음
- 명세: 리뷰 파일을 쓰지 못해 응답 본문을 오케스트레이터가 옮김
- QA: 격리 사본에서 돌연변이 7개, 4개 잡음·2개 못 잡음(S3Q-01, S3Q-02)·1개 다른 이유로 RED

<!-- devkit:standard-list hex:7b22636861696e223a22646f63732f66656174757265732f72756e6e696e672f6d756c74692d62726f6b65722d6b69732d746f73732f7265766965772f73746570332d70616e656c2e6d64222c22726f756e6473223a7b2231223a7b226361757365223a22505220eab8b0eca48020ebb88ceb9e9cecb998eba5bc20eca095ed9598eca78020ebaabbed95a8287472756e6b20ed8c90eca09520ebb688eab08029222c2268656164223a2261343335333861356537383637613134373365313163356662343266376536633966646330313365227d7d7d -->
