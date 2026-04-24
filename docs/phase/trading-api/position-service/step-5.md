# Step 5: Summary 작성 + PR 생성
담당 에이전트: Orchestrator (Cleanup)

## 작업 경로
`.worktrees/trading-api-position-service`

---

## 읽어야 할 파일 (필수)

1. `docs/phase/trading-api/position-service/spec.md`
2. `docs/phase/trading-api/position-service/index.json`
3. `docs/TODO.md` — Position 서비스 항목 [x] 처리용
4. `docs/state.md` — 현재 상태 업데이트용
5. 구현된 파일들 (변경 사항 파악용):
   - `backend/trading-api/src/main/kotlin/com/papertrading/api/application/position/PositionQueryService.kt`
   - `backend/trading-api/src/main/kotlin/com/papertrading/api/application/position/PositionCommandService.kt`
   - `backend/trading-api/src/main/kotlin/com/papertrading/api/application/position/result/PositionResult.kt`
   - `backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/controller/PositionController.kt`
   - `backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/dto/position/PositionResponse.kt`
   - `backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/redis/QuoteEventListener.kt`

---

## 작업 1: summary 작성

**파일 경로**: `docs/done/trading-api/position-service/position-service-summary.md`

아래 형식으로 작성한다:

```markdown
# Position Service — 구현 완료 요약

## 완료 일자
{오늘 날짜}

## 구현 범위
- PositionQueryService: 포지션 조회 + Redis 현재가 주입
- PositionCommandService: Redis 시세 수신 시 포지션 평가손익 갱신
- PositionController 리팩터링: OrderQueryService 의존 제거 → PositionQueryService 전환
- PositionResponse DTO 분리: dto/order/ → dto/position/
- QuoteEventListener 확장: 시세 수신 시 포지션 평가손익 자동 갱신
- PositionRepository 메서드 추가: findByTickerAndQuantityGreaterThan
- Position 엔티티 @Index 추가: idx_positions_ticker_qty

## 주요 설계 결정
- 포지션 청산 상태는 별도 status 컬럼 없음 (quantity=0 = 청산)
- readOnly 트랜잭션에서 Redis 시세 주입 시 DB flush 없음 (응답 전용)
- QuoteEventListener에서 PositionCommandService 호출로 DB 스냅샷 주기적 갱신

## 테스트 커버리지
- PositionQueryServiceTest (단위)
- PositionCommandServiceTest (단위)
- PositionControllerIntegrationTest (통합, Testcontainers)

## 변경된 파일 목록
{git diff --name-only 결과 기반으로 작성}

## PR
{PR URL}
```

---

## 작업 2: docs 업데이트

### 2-1. index.json 업데이트
`docs/phase/trading-api/position-service/index.json`에서:
- `current_step`: 5
- `status`: `"completed"`
- step 1~5 모두 `"status": "completed"`

### 2-2. TODO.md 업데이트
`docs/TODO.md`에서 아래 항목을 완료 처리:
```
- [ ] Position 애플리케이션 서비스 | project: trading-api | phase: position-service | priority: P0
```
→
```
- [x] Position 애플리케이션 서비스 | project: trading-api | phase: position-service | priority: P0 | done: {오늘날짜} | pr: #{PR번호}
```

### 2-3. state.md 업데이트
`docs/state.md`에 position-service 완료 기록 추가.

---

## 작업 3: PR 생성

### 빌드 최종 검증 (PR 전 필수)
```bash
cd .worktrees/trading-api-position-service/backend/trading-api
./gradlew test
```
모든 테스트 통과 확인 후 PR 생성.

### PR 생성 명령
```bash
cd .worktrees/trading-api-position-service
gh pr create \
  --title "feat(trading-api): Position 애플리케이션 서비스 구현" \
  --body "$(cat <<'EOF'
## Summary
- PositionQueryService / PositionCommandService 신설로 포지션 전담 서비스 계층 분리
- Redis 시세 수신 시 포지션 평가손익(unrealizedPnl, returnRate) 자동 갱신
- PositionController가 OrderQueryService 의존 제거 → PositionQueryService 전환
- PositionResponse DTO를 dto/order/에서 dto/position/으로 분리

## 변경 사항
- 신규: PositionQueryService, PositionCommandService, PositionResult, PositionResponse
- 수정: PositionController, QuoteEventListener, OrderQueryService, PositionRepository, Position(@Index)
- 삭제: OrderResponse.kt 내 PositionResponse 클래스

## Test plan
- [ ] `./gradlew test` 전체 통과 확인
- [ ] PositionQueryServiceTest — Redis 시세 주입, graceful degradation
- [ ] PositionCommandServiceTest — ticker 기준 일괄 시세 갱신
- [ ] PositionControllerIntegrationTest — 포지션 없는 계좌 빈 배열, 404 처리

🤖 Generated with [Claude Code](https://claude.com/claude-code)
EOF
)"
```

---

## Acceptance Criteria

- `docs/done/trading-api/position-service/position-service-summary.md` 생성 완료
- `docs/TODO.md` Position 항목 `[x]` 처리 완료 (pr 번호 포함)
- `docs/state.md` 최신화
- `index.json` status: "completed"
- PR 생성 완료 및 URL 반환