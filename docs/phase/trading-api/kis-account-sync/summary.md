# trading-api/kis-account-sync Summary

## 배경
- KIS 실계좌/모의계좌 잔고 조회를 `trading-api`에 연결해 대시보드에서 KIS 기준 잔고·평가정보를 조회할 수 있도록 한다.

## 주요 구현
- `KisAccountQueryService` 추가
  - `LIVE -> TTTC8434R`, `PAPER -> VTTC8434R` TR ID 분기
  - KIS 스냅샷과 로컬 포지션 정합성 비교(`missingInLocal`, `missingInKis`, `quantityMismatch`)
- `GET /api/kis/account/balance` 엔드포인트 추가
  - `accountId`, `mode` 입력으로 잔고 조회
  - 응답 `source`에 `KIS/LOCAL` 구분 노출
- `KisAccountBalanceAdapter.fetchBalance` 구현
  - 계좌 `externalAccountId` 기반 KIS 잔고 조회 경로 연결
  - KIS 응답 수치 필드 `BigDecimal` 매핑 및 반올림 정책 명시
- 예외 매핑 강화
  - 인증/권한/연동/타임아웃 예외를 도메인 예외로 변환
  - API 응답에서 외부 원문 메시지 노출 방지(안전 메시지 반환)

## 테스트/검증
- 단위 테스트
  - `KisAccountQueryServiceTest`
  - `KisAccountBalanceAdapterTest`
- 컨트롤러 통합 테스트
  - `KisAccountBalanceControllerIntegrationTest`
- 검증 커맨드(phase 진행 중 통과 보고됨)
  - `./gradlew test --tests "*KisAccount*"`
  - `./gradlew test --tests "*AccountBalance*Controller*"`
  - `./gradlew test --tests "*Account*"` (리뷰 재평가 단계)

## 리뷰 결과
- Step 4 재평가 `PASS`
- 기존 차단 이슈(미구현 어댑터, 외부 원문 메시지 노출) 해소 확인
- Minor 권고: `ResourceAccessException -> KisTimeoutException` 변환 경로 단위테스트 1건 추가 권장
