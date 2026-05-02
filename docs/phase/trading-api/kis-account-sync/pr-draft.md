# PR Draft — trading-api/kis-account-sync

## 배경
- KIS 실계좌/모의계좌 잔고를 trading-api에서 직접 조회하고, 로컬 포지션과 정합성 비교 결과를 함께 제공하기 위해 구현.

## 변경사항
- `KisAccountQueryService` 추가 및 TR ID 분기(LIVE/PAPER) 적용
- `GET /api/kis/account/balance` API 추가
- `KisAccountBalanceAdapter` 잔고조회 구현(계좌 파싱/헤더/응답 매핑)
- `AccountSource` 응답 모델 확장(`KIS`, `LOCAL`)
- KIS 예외 매핑 정리 및 외부 원문 에러 메시지 비노출 처리
- 관련 단위/통합 테스트 추가 및 보강

## 테스트
- `./gradlew test --tests "*KisAccount*"`
- `./gradlew test --tests "*AccountBalance*Controller*"`
- `./gradlew test --tests "*Account*"`

## 리스크
- KIS 응답 스키마 변동 시 필드 매핑 영향 가능
- 외부 API 타임아웃/일시 오류 시 운영 환경에서 재시도/관측성 정책 조정 필요

## 롤백 플랜
1. 해당 PR revert
2. `/api/kis/account/balance` 호출 경로 비활성화(필요 시 라우팅 차단)
3. 기존 LOCAL 기반 조회만 사용

## 체크리스트
- [x] 보안: 외부 원문 에러 메시지 응답 비노출
- [x] 재무정합성: 수치 계산/직렬화 `BigDecimal` 사용
- [x] 테스트: 핵심 시나리오(정상/오류/회귀) 검증
- [ ] 모니터링: 운영 로그/알림 임계치 최종 점검(후속)
