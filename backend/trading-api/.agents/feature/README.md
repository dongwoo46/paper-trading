API 인덱스 trading-api
원칙: 최신 상태만 유지. API 추가 수정 시 즉시 갱신.

완료된 기능
ERD 설계 v1(23개 테이블): erd-design-v1.md
JPA Entity + Repository: Phase 2 완료
계좌 API (Phase 3): account.md

API 목록

계좌 관리
POST   | /api/v1/accounts                              | 계좌 생성          | 완료
GET    | /api/v1/accounts                              | 계좌 목록 조회     | 완료
GET    | /api/v1/accounts/{id}                         | 계좌 상세 조회     | 완료
PATCH  | /api/v1/accounts/{id}                         | 계좌 수정          | 완료
DELETE | /api/v1/accounts/{id}                         | 계좌 비활성화      | 완료
POST   | /api/v1/accounts/{id}/deposit                 | 입금 (멱등)        | 완료
POST   | /api/v1/accounts/{id}/withdraw                | 출금 (멱등)        | 완료
GET    | /api/v1/accounts/{id}/ledgers                 | 원장 조회          | 완료

리스크 정책
GET    | /api/v1/accounts/{id}/risk-policy             | 활성 정책 조회     | 완료
POST   | /api/v1/accounts/{id}/risk-policy             | 정책 등록/수정     | 완료

정산 예정
GET    | /api/v1/accounts/{id}/pending-settlements     | 정산 예정 조회     | 완료