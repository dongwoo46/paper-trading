# KIS Account Sync Spec (`trading-api/kis-account-sync`)

## 1. Scope
- KIS 잔고조회 API 연동: 실전 `TTTC8434R`, 모의 `VTTC8434R`
- `KisAccountQueryService`에서 KIS 잔고 스냅샷 조회 + 로컬 포지션 정합성 동기화 정보 생성
- `GET /api/kis/account/balance` 제공: 예수금/평가금액/평가손익/수익률/포지션 목록
- 응답에서 계좌 출처(`KIS`/`LOCAL`) 구분 가능하게 모델 확장

## 2. Non-Scope
- 주문/체결 처리 로직 변경
- KIS 인증 체계 전면 개편
- trading-web 화면 구현 상세(본 phase는 API 계약 중심)

## 3. Domain & Data Contract

### 3.1 Account Source
- `AccountSource`: `KIS`, `LOCAL`
- 기존 계좌/대시보드 조회 응답에 source 필드 추가 (하위호환 위해 기본값 `LOCAL` 허용)

### 3.2 Balance Snapshot
- `cashBalance`: `BigDecimal` (예수금)
- `marketValue`: `BigDecimal` (보유주식 평가금액)
- `unrealizedPnl`: `BigDecimal` (평가손익)
- `returnRate`: `BigDecimal` (수익률, `%` 단위)
- `asOf`: `OffsetDateTime`
- `positions[]`:
  - `ticker`: String
  - `quantity`: `BigDecimal`
  - `avgPrice`: `BigDecimal`
  - `currentPrice`: `BigDecimal`
  - `marketValue`: `BigDecimal`
  - `unrealizedPnl`: `BigDecimal`
  - `returnRate`: `BigDecimal`

### 3.3 Numeric Rules
- 금액/수량/수익률 계산은 `BigDecimal` 사용
- 부동소수점(`Double/Float`) 금지
- 나눗셈 시 스케일/반올림 모드 명시 (`HALF_UP`)

## 4. External Integration (KIS)
- endpoint: KIS 잔고조회(실전/모의 TR ID 분기)
- mode 분기:
  - `LIVE` -> `TTTC8434R`
  - `PAPER` -> `VTTC8434R`
- timeout/retry:
  - 단건 조회 timeout 설정
  - 일시 오류(5xx, timeout) 제한 재시도(예: 최대 2회)
- 오류 매핑:
  - 인증 실패/권한 오류 -> `KisAuthorizationException`
  - KIS 응답 오류 -> `KisRemoteCallException`

## 5. Service Design

### 5.1 `KisAccountQueryService`
- 입력: 계좌 식별자, 투자 모드(LIVE/PAPER)
- 처리:
  1. KIS adapter로 잔고/포지션 조회
  2. 로컬 포지션과 티커 기준 정합성 비교
  3. 불일치 항목을 결과 메타에 포함(감사용)
- 출력: `KisAccountBalanceResult`
  - `summary`
  - `positions`
  - `reconciliation` (missingInLocal/missingInKis/quantityMismatch)

### 5.2 Auditability
- 조회 시 correlation id 포함 structured log 기록
- 정합성 mismatch는 warn 레벨 로깅
- 상태 변경이 필요한 경우(향후 자동 동기화)는 별도 command 서비스에서 처리

## 6. API Spec

### `GET /api/kis/account/balance`
- Query:
  - `accountId` (required)
  - `mode` (`LIVE|PAPER`, required)
- 200 Response:
  - `accountId`, `source`, `mode`, `asOf`
  - `cashBalance`, `marketValue`, `unrealizedPnl`, `returnRate`
  - `positions[]`
  - `reconciliation` 요약
- Error:
  - `400` invalid mode/account
  - `401/403` KIS auth/permission
  - `502` remote call failure
  - `504` timeout

## 7. Acceptance Criteria
- LIVE/PAPER 각각 올바른 TR ID를 사용한다.
- 계산 필드가 모두 `BigDecimal` 기반으로 직렬화된다.
- API 응답에 `source=KIS|LOCAL` 구분이 노출된다.
- 정합성 비교 결과가 누락 없이 반환/로그된다.
- 실패 케이스(인증, 타임아웃, 원격 오류) 상태코드 매핑이 일관된다.

## 8. Dependencies & Risks
- KIS 샌드박스/실계좌 응답 필드 차이
- 장중 데이터 변동으로 인한 순간 불일치
- 대량 포지션 조회 시 응답 지연

## 9. Delivery Notes
- Step 2에서 TDD로 서비스/컨트롤러 구현
- Step 3에서 feature-scope 테스트와 컴파일 검증
- Step 4에서 보안/회귀/재무정합성 리뷰
- Step 5에서 summary/TODO/state/PR 준비 마감
