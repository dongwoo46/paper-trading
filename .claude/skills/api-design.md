Skill: API Design (REST)

## URL 설계

- 리소스 중심 명사 사용: /orders, /accounts, /positions
- 동사 금지: /createOrder (X) → POST /orders (O)
- 계층 관계 표현: /accounts/{id}/orders, /orders/{id}/fills
- 복수형 사용: /order (X) → /orders (O)
- kebab-case: /market-data (O), /marketData (X)

## HTTP 메서드

- GET: 조회 (멱등, 부수효과 없음)
- POST: 생성
- PUT: 전체 수정 (멱등)
- PATCH: 부분 수정
- DELETE: 삭제

## 상태 코드

- 200 OK: 조회/수정 성공
- 201 Created: 생성 성공 (Location 헤더 포함)
- 204 No Content: 삭제 성공
- 400 Bad Request: 입력 유효성 오류
- 401 Unauthorized: 인증 필요
- 403 Forbidden: 권한 없음
- 404 Not Found: 리소스 없음
- 409 Conflict: 비즈니스 규칙 충돌 (잔고 부족, 중복 등)
- 500 Internal Server Error: 서버 오류

## 에러 응답 형식

```json
{
  "code": "INSUFFICIENT_BALANCE",
  "message": "보유 잔고가 부족합니다.",
  "detail": { "required": 100000, "available": 50000 }
}
```

## Request/Response 원칙

- Request: 필요한 필드만. 불필요한 필드 노출 금지.
- Response: 클라이언트가 실제로 사용하는 필드만.
- 페이지네이션: cursor 기반 우선 (offset은 대용량 시 성능 문제)
- 날짜/시간: ISO 8601 (2025-04-16T10:30:00Z)
- 금액: 문자열 또는 정수(원 단위). float 금지.

## 버저닝

- URL 버저닝: /api/v1/orders
- 하위 호환성 유지 원칙: 필드 추가는 비파괴적, 필드 제거/변경은 새 버전

## 내부 API

- 서비스 간 내부 호출: /api/internal/{리소스}
- 외부 노출 금지, 인증 별도 처리
