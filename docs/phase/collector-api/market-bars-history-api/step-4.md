# Step 4 — 문서 완료 처리

## Goal

`market-bars-history-api` 피처 구현이 완료된 후 문서 상태를 정리한다.

## Success Criteria

- [ ] `docs/done/collector-api/market-bars-history-api/market-bars-history-api-summary.md` 생성
- [ ] `docs/phase/collector-api/market-bars-history-api/` → `docs/done/collector-api/market-bars-history-api/`로 이동
- [ ] `docs/TODO.md`에서 해당 항목 `[ ]` → `[x]` 변경
- [ ] `docs/state.md` 업데이트 (다음 페이즈 또는 완료 상태 반영)
- [ ] `docs/phase/collector-api/market-bars-history-api/index.json` 모든 step "done" 상태 확인

---

## 처리 순서

### Step 4-1. index.json 상태 업데이트

**파일**: `docs/phase/collector-api/market-bars-history-api/index.json`

모든 step의 status를 `"done"`으로 설정:

```json
{
  "phase": "market-bars-history-api",
  "project": "collector-api",
  "priority": "P1",
  "status": "done",
  "startedAt": "2026-05-08",
  "completedAt": "<완료 날짜>",
  "steps": [
    { "id": 1, "title": "도메인 모델 및 Redis 읽기 서비스 구현", "status": "done" },
    { "id": 2, "title": "API 컨트롤러 및 DTO 구현", "status": "done" },
    { "id": 3, "title": "테스트 작성 및 빌드 검증", "status": "done" },
    { "id": 4, "title": "문서 완료 처리", "status": "done" }
  ]
}
```

### Step 4-2. 완료 요약 문서 작성

**파일**: `docs/done/collector-api/market-bars-history-api/market-bars-history-api-summary.md`

아래 구조로 작성:

```markdown
# market-bars-history-api 완료 요약

## 개요
차트용 1m/5m/10m 바 히스토리 조회 REST API 구현.
Frontend가 Redis에 직접 접근하지 않고 collector-api를 통해 OHLCV 바 히스토리를 조회할 수 있다.

## 구현된 엔드포인트
`GET /api/market/bars/{symbol}?interval=1m|5m|10m&limit={n}`

## 주요 구현 내용
- **도메인 모델**: `MarketBar` — 모든 가격/금액 필드 BigDecimal
- **포트 인터페이스**: `MarketBarRepository.findBars(symbol, interval, limit)`
- **Redis 구현**: `MarketBarRedisRepository`
  - 1m: `LRANGE bars:1m:{symbol} -limit -1` 직접 조회
  - 5m/10m: 1m 바 조회 후 5분/10분 버킷 경계로 그룹 집계
  - vwap = tradeValue / volume (volume=0 → BigDecimal.ZERO)
- **서비스**: `MarketBarQueryService` — interval 검증, limit 클램프 [1..100]
- **컨트롤러**: `MarketBarController` — 빈 결과 → 404, 잘못된 interval → 400

## 신규 파일 목록
- `domain/marketbar/MarketBar.kt`
- `application/marketbar/port/MarketBarRepository.kt`
- `application/marketbar/service/MarketBarQueryService.kt`
- `infra/redis/MarketBarRedisRepository.kt`
- `presentation/marketbar/dto/MarketBarResponse.kt`
- `presentation/marketbar/MarketBarController.kt`
- (테스트) `MarketBarQueryServiceTest`, `MarketBarRedisRepositoryTest`, `MarketBarControllerTest`

## 검증 결과
- `./gradlew compileKotlin` PASS
- `./gradlew test` PASS (전체 테스트 회귀 없음)
```

### Step 4-3. phase 폴더 이동

`docs/phase/collector-api/market-bars-history-api/` 폴더를 `docs/done/collector-api/market-bars-history-api/`로 이동.

> 이동 전 `docs/done/collector-api/` 디렉터리가 없으면 먼저 생성한다.

```bash
# 디렉터리 생성 (필요 시)
mkdir -p docs/done/collector-api/

# 폴더 이동
mv docs/phase/collector-api/market-bars-history-api docs/done/collector-api/market-bars-history-api
```

### Step 4-4. TODO.md 업데이트

**파일**: `docs/TODO.md`

해당 항목을 `[ ]` → `[x]` 로 변경:

```
[x] collector-api: 차트용 1m/5m/10m 바 히스토리 조회 API
```

### Step 4-5. state.md 업데이트

**파일**: `docs/state.md`

- `market-bars-history-api` 완료 상태 반영
- `activeFeature`를 다음 우선순위 피처로 변경하거나 "none"으로 설정

---

## Verification

모든 문서 파일 존재 확인:
```bash
ls docs/done/collector-api/market-bars-history-api/
# 기대: index.json, step-1.md, step-2.md, step-3.md, step-4.md, market-bars-history-api-summary.md
```

TODO.md에서 체크 확인:
```bash
grep "market-bars-history-api\|바 히스토리" docs/TODO.md
# 기대: [x] 로 시작하는 라인
```

## Commit Message (Korean)

```
docs(collector): market-bars-history-api 완료 처리 및 문서 정리
```
