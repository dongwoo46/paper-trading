# Step 4: 코드 리뷰
담당 에이전트: Code Reviewer

## 작업 경로
`.worktrees/trading-api-position-service`

---

## 읽어야 할 파일 (필수)

### 명세
1. `docs/phase/trading-api/position-service/spec.md`
2. `CLAUDE.md` — 아키텍처 원칙, 절대 규칙

### 구현 파일
3. `backend/trading-api/src/main/kotlin/com/papertrading/api/application/position/PositionQueryService.kt`
4. `backend/trading-api/src/main/kotlin/com/papertrading/api/application/position/PositionCommandService.kt`
5. `backend/trading-api/src/main/kotlin/com/papertrading/api/application/position/result/PositionResult.kt`
6. `backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/controller/PositionController.kt`
7. `backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/dto/position/PositionResponse.kt`
8. `backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/dto/order/OrderResponse.kt`
9. `backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/PositionRepository.kt`
10. `backend/trading-api/src/main/kotlin/com/papertrading/api/domain/model/Position.kt`
11. `backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/redis/QuoteEventListener.kt`
12. `backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/OrderQueryService.kt`

### 테스트 파일
13. `backend/trading-api/src/test/kotlin/com/papertrading/api/application/position/PositionQueryServiceTest.kt`
14. `backend/trading-api/src/test/kotlin/com/papertrading/api/application/position/PositionCommandServiceTest.kt`
15. `backend/trading-api/src/test/kotlin/com/papertrading/api/presentation/controller/PositionControllerIntegrationTest.kt`

---

## 검토 포인트

### 1. BigDecimal 규칙 (CRITICAL)
- [ ] 모든 금액·수량 필드가 `BigDecimal` 타입인지 확인
- [ ] `double`, `float`, `Double`, `Float` 사용 절대 없음
- [ ] `BigDecimal` 비교는 `compareTo` 또는 `isEqualByComparingTo` 사용 (`==` 금지)
- [ ] `divide` 사용 시 scale + RoundingMode 명시 (무한소수 예외 방지)

### 2. 레이어 의존 방향 (CRITICAL)
- [ ] `PositionQueryService`, `PositionCommandService`가 `@Entity` 직접 반환하지 않음 (→ PositionResult 사용)
- [ ] `PositionResult`에 `@Entity`, `@Table`, `@Column` 등 JPA 어노테이션 없음
- [ ] `PositionResponse` (presentation DTO)가 도메인 모델 직접 참조하지 않음 (→ PositionResult 경유)
- [ ] `domain/` 레이어에 `@Service`, `@Repository`, `@Component` 없음
- [ ] `PositionController`가 `application/` 계층만 의존 (`infrastructure/` 직접 접근 없음)

### 3. 트랜잭션 경계
- [ ] `PositionQueryService`: `@Transactional(readOnly = true)` 적용됨
- [ ] `PositionCommandService.updateCurrentPriceByTicker`: `@Transactional` 적용됨
- [ ] `readOnly = true` 트랜잭션에서 `position.updatePrice()` 호출 시 Hibernate dirty checking이 발생하지 않는지 주의. 조회 후 응답에만 사용되고 `save()` 호출 없어야 함 (`PositionQueryService`에서)
- [ ] `PositionCommandService`는 `save()` 명시 호출 또는 dirty checking으로 저장됨 (트랜잭션 내 변경이므로 자동 flush 가능)

### 4. 동시성·데드락
- [ ] `updateCurrentPriceByTicker`에서 `findByTickerAndQuantityGreaterThan` 조회 후 대량 업데이트 시 락 없음 — 허용 (시세 업데이트는 PESSIMISTIC 락 불필요, 최신 가격으로 덮어쓰면 됨)
- [ ] `ExecutionProcessor.fill` 내 포지션 업데이트는 기존 PESSIMISTIC 락 패턴 유지 확인

### 5. Null 안전성
- [ ] `requireNotNull` 활용 (!! 연산자 사용 금지)
- [ ] `PositionResult.from(p)` 내 `p.account?.id` null 체크
- [ ] `marketQuotePort.getQuote(ticker)` null 반환 처리 (graceful degradation)

### 6. 보안
- [ ] 계좌 소유권 검증: `getPositionWithCurrentPrice(accountId, ticker)`에서 position이 해당 accountId 소유인지 확인 (`positionRepository.findByAccountIdAndTicker`로 account 조건 포함 조회 → 자동 보장)
- [ ] 다른 계좌 포지션 조회 불가 확인

### 7. DTO ↔ Entity 혼용 금지
- [ ] `PositionController`가 `Position` 엔티티를 직접 반환하지 않음
- [ ] `PositionQueryService`가 `PositionResult` (애플리케이션 계층 객체) 반환
- [ ] `dto/order/OrderResponse.kt`에 `PositionResponse`가 남아있지 않음 (완전 제거 확인)

### 8. 기존 코드 회귀
- [ ] `OrderQueryService`에서 `listPositions`, `getPosition` 제거됨
- [ ] `OrderController`가 포지션 관련 엔드포인트를 노출하지 않음
- [ ] 기존 `OrderResponse`, `ExecutionResponse`는 영향 없음

### 9. 테스트 품질
- [ ] Happy path + edge case 모두 커버됨
- [ ] 테스트명이 한국어 snake_case 또는 backtick 방식으로 명확함
- [ ] 통합 테스트에서 Testcontainers 사용 (실제 DB)
- [ ] MockK mock 설정이 실제 동작을 반영함 (over-mocking 없음)

### 10. 코드 스타일
- [ ] `val` 우선, `var` 최소화
- [ ] `@Autowired` 필드 주입 없음 (생성자 주입만)
- [ ] 로그에 시크릿·민감 정보 없음
- [ ] 불필요한 주석 없음 (코드가 자명하면 주석 제거)

---

## 검토 결과 형식

아래 형식으로 결과를 출력한다:

```
## 코드 리뷰 결과

### 🔴 Critical (즉시 수정 필요)
(없으면 "없음")

### 🟡 Warning (수정 권장)
(없으면 "없음")

### 🟢 Minor (선택적 개선)
(없으면 "없음")

### 승인 여부
[ 승인 / 조건부 승인 (Warning 수정 후) / 반려 (Critical 해결 후 재검토) ]
```

---

## Acceptance Criteria

- 🔴 Critical 없음
- 모든 CLAUDE.md 절대 원칙 준수 확인
- 레이어 의존 방향 위반 없음
- BigDecimal 규칙 위반 없음
