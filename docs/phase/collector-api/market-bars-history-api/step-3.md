# Step 3 — 테스트 작성 및 빌드 검증

## Goal

Step 1~2에서 작성한 모든 코드의 테스트가 통과하는지 검증하고, 빌드 에러가 없는지 확인한다.
누락된 엣지 케이스를 보완하고 최종 빌드를 확인한다.

## Success Criteria

- [ ] `./gradlew compileKotlin` 에러 없음
- [ ] `./gradlew test` 전체 GREEN
- [ ] 신규 테스트 파일 5개 모두 통과
- [ ] 기존 테스트(`MarketFeatureControllerTest` 등) 회귀 없음

---

## 검증 항목

### 1. 빌드 컴파일 검증

```bash
cd backend/collector-api && ./gradlew compileKotlin
```

예상: BUILD SUCCESSFUL — 에러 0, 경고 확인 후 필요 시 수정

### 2. 단위 테스트 — 서비스 레이어

실행:
```bash
cd backend/collector-api && ./gradlew test --tests "*.MarketBarQueryServiceTest"
```

검증 케이스:
- `유효한 1m 조회 — 결과 반환` → PASS
- `유효한 5m 조회 — 결과 반환` → PASS
- `잘못된 interval — 400 예외` → PASS
- `limit 0 이하 — 400 예외` → PASS
- `limit 100 초과 — 100으로 클램프 후 정상 조회` → PASS

### 3. 단위 테스트 — Redis 리포지토리 집계 로직

실행:
```bash
cd backend/collector-api && ./gradlew test --tests "*.MarketBarRedisRepositoryTest"
```

검증 케이스:
- `5m 집계 — open=첫봉, high=max, low=min, close=마지막봉, volume=합계` → PASS
- `1m bars JSON 직렬화-역직렬화 무결성` → PASS

### 4. 단위 테스트 — 컨트롤러 레이어

실행:
```bash
cd backend/collector-api && ./gradlew test --tests "*.MarketBarControllerTest"
```

검증 케이스:
- `GET bars 200 — 1m 기본 조회` → PASS
- `GET bars 200 — limit 명시` → PASS
- `GET bars 400 — 잘못된 interval` → PASS
- `GET bars 404 — 데이터 없음` → PASS
- `GET bars 200 — limit 100 초과 시 100으로 클램프` → PASS

### 5. 전체 테스트 실행 (회귀 확인)

```bash
cd backend/collector-api && ./gradlew test
```

- 기존 통과 중이던 테스트가 FAIL로 바뀌는 경우: 반드시 원인 파악 후 수정
- 새 테스트 FAIL 시: Red → Green 사이클로 재진입, Step 1/2 파일 수정

---

## 보완 테스트 — 엣지 케이스

아래 케이스가 Step 1~2 테스트에서 빠져 있다면 추가 작성한다.

### 집계 엣지 케이스

| 시나리오 | 기대 결과 |
|----------|-----------|
| 1m 바가 5개 미만(예: 3개)인데 5m 조회 | 3개로 하나의 불완전 버킷 반환 (데이터 있는 것만 집계) |
| volume = 0인 1m 바 포함 | vwap = BigDecimal.ZERO (ZeroDivisionError 없음) |
| limit=1 인 경우 | 최신 1개 반환 |
| Redis key 없음 (emptyList 반환) | 서비스/컨트롤러가 404 반환 |

파일: `src/test/kotlin/com/papertrading/collector/infra/redis/MarketBarRedisRepositoryEdgeCaseTest.kt` (필요 시 신규 작성)

---

## 실패 시 디버깅 체크리스트

1. **컴파일 에러** `Unresolved reference`:
   - 패키지 경로 확인: `com.papertrading.collector.domain.marketbar`, `application.marketbar`, `infra.redis`, `presentation.marketbar`
   - import 문 확인

2. **테스트 FAIL — 집계 결과 불일치**:
   - `startedAt` 버킷 경계 계산 로직 확인
   - `ChronoUnit.MINUTES.truncatedTo` + `(minute / N) * N` 연산 확인
   - List ordering 확인 (오래된 것부터 → 최신 순으로 정렬 후 반환)

3. **404가 아닌 200 반환** (빈 리스트):
   - 컨트롤러에서 `bars.isEmpty()` 체크 후 `ResponseStatusException(NOT_FOUND)` throw 확인

4. **BigDecimal 비교 실패** (`assertEquals` 스케일 불일치):
   - `assertEquals(BigDecimal("100"), actual)` 대신 `assertTrue(BigDecimal("100").compareTo(actual) == 0)` 사용

---

## Verification Command (최종)

```bash
# 빌드
cd backend/collector-api && ./gradlew compileKotlin

# 전체 테스트
cd backend/collector-api && ./gradlew test
```

## Commit Message (Korean)

```
test(collector): MarketBar 서비스·리포지토리·컨트롤러 단위 테스트 완성
```
