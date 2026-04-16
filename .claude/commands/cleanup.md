Role: Orchestrator — Cleanup + PR 담당 (Phase 마지막 Step)

## 실행 순서

1. git status, git log --oneline -10, git diff main...HEAD 확인
2. 스테이징되지 않은 변경사항 있으면 git add -A 실행
3. WIP 커밋 여러 개 있으면 squash 여부 사용자 확인 (manual 모드 시 승인 대기)
4. 커밋 메시지 작성 후 git commit 실행
   - 형식: `feat({서비스}): {기능 요약}`
   - 예: `feat(trading-api): 주문 체결 엔진 구현`
5. {feature}-summary.md 작성 (spec.md + 각 step result 기반)
6. docs/phase/{project}/{feature}/ → docs/done/{project}/{feature}/ 이동
7. state.md 활성 phase 목록에서 해당 phase 제거
8. PR 초안 작성 후 사용자 확인 대기
9. 확인 후 gh pr create 실행

## 커밋 메시지 형식

```
feat(trading-api): 주문 체결 엔진 구현

- LocalMatchingEngine: 시세 이벤트 기반 자동 체결
- OrderRepository: 주문 상태 영속화
- Redis Pub/Sub 구독 해제 로직 포함
```

타입: feat / fix / refactor / docs / test / chore

## {feature}-summary.md 작성 형식

```markdown
# {기능명}

## 핵심 기능
한 줄: 무엇을 하는 기능인지

## 고려사항
- 무엇을 중요하게 봤는지
- 어떤 제약이 있었는지

## 트레이드오프
- 선택A vs 선택B → 선택A 이유

## 구현 방식
레이어별 구현 요약

## 워크플로우
요청 → 처리 → 응답 흐름

## 주요 API
METHOD /path — 설명

## DB
테이블명 및 주요 컬럼

## 완료일 / PR
YYYY-MM-DD / #N
```

## PR 형식
제목: `feat({서비스}): {기능 요약}`
본문: Summary(변경 내용 불릿) / Test plan(체크리스트)
